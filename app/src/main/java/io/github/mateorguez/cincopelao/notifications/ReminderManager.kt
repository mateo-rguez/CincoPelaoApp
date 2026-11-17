package io.github.mateorguez.cincopelao.notifications

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.mateorguez.cincopelao.data.instances.InstanceRepository
import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.local.entity.Task
import io.github.mateorguez.cincopelao.data.prefs.NotiPrefsManager
import io.github.mateorguez.cincopelao.data.prefs.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/** Recordatorios programados (clave, spec y hora sorteada), para conservarlos entre ejecuciones. */
private val Context.reminderStore by preferencesDataStore(name = "reminder_alarms")

/**
 * Mantiene las alarmas de los recordatorios sincronizadas con los datos.
 *
 * Observa las asignaturas, eventos, tareas y ajustes de todas las instancias y, en cada cambio,
 * reconcilia ([ReminderSync]): pone las alarmas que faltan, quita las que sobran (tareas
 * completadas, elementos borrados, fechas cambiadas...) y conserva la hora de las que siguen igual,
 * para que no se vuelva a sortear. La interfaz no tiene que programar ni cancelar nada.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Singleton
class ReminderManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val instances: InstanceRepository,
    private val prefs: NotiPrefsManager
) {
    private val alarms = ReminderAlarms(context)
    private val store get() = context.reminderStore
    private val mutex = Mutex()

    fun start(scope: CoroutineScope) {
        scope.launch {
            cleanUpLegacyScheduling()
            migrateFromWorkManager()
            plannedReminders()
                .debounce(DEBOUNCE_MS)
                .collectLatest { planned -> sync(planned) }
        }
    }

    /** Sincroniza ya con los datos actuales (tras reiniciar el móvil o actualizar la app). */
    suspend fun syncNow() {
        migrateFromWorkManager()
        sync(plannedReminders().first())
    }

    private fun settings(): Flow<Pair<ReminderSettings, ReminderSettings>> = combine(
        prefs.eventDaysFlow, prefs.eventStartHourFlow, prefs.eventEndHourFlow,
        prefs.taskDaysFlow, combine(prefs.taskStartHourFlow, prefs.taskEndHourFlow, ::Pair)
    ) { eDays, eStart, eEnd, tDays, (tStart, tEnd) ->
        ReminderSettings(eDays, eStart, eEnd) to ReminderSettings(tDays, tStart, tEnd)
    }

    /** Datos de una instancia necesarios para planificar sus recordatorios. */
    private data class InstanceData(val id: String, val subjects: List<Subject>, val events: List<Event>, val tasks: List<Task>)

    /** Datos de todas las instancias; se re-suscribe al crear o borrar instancias. */
    private fun allInstancesData(): Flow<List<InstanceData>> =
        instances.instances
            .map { list -> list.map { it.id } }
            .distinctUntilChanged()
            .flatMapLatest { ids ->
                if (ids.isEmpty()) return@flatMapLatest flowOf(emptyList())
                combine(ids.map { id ->
                    val db = instances.database(id)
                    combine(db.subjectDao().getAllSubjects(), db.eventDao().getAllEvents(), db.taskDao().getAllTasks()) { s, e, t ->
                        InstanceData(id, s, e, t)
                    }
                }) { it.toList() }
            }

    private fun plannedReminders(): Flow<List<PlannedReminder>> =
        combine(allInstancesData(), settings()) { data, (eventSettings, taskSettings) ->
            val now = System.currentTimeMillis()
            data.flatMap { ReminderPlanner.plan(it.id, it.subjects, it.events, it.tasks, eventSettings, taskSettings, now) }
        }.retryWhen { cause, _ ->
            // P. ej. si se borra una instancia mientras se leía su base de datos
            Log.w(TAG, "Reintentando la observación de recordatorios", cause)
            delay(RETRY_DELAY_MS)
            true
        }

    /**
     * Aplica una reconciliación. No se deja cancelar a medias (llega otro cambio, `collectLatest`
     * cancela): las alarmas y lo guardado deben quedar de acuerdo.
     */
    private suspend fun sync(planned: List<PlannedReminder>) = withContext(NonCancellable) {
        mutex.withLock {
            try {
                val stored = readScheduled()
                val now = System.currentTimeMillis()
                val result = ReminderSync.reconcile(planned, stored, now, Random.Default)

                result.toCancel.forEach(alarms::cancel)
                result.toArm.forEach(alarms::arm)
                store.edit { it[SCHEDULED_KEY] = result.scheduled.mapTo(HashSet()) { s -> s.encode() } }

                val drawnKeys = result.scheduled.filter { stored[it.key]?.spec != it.spec }.mapTo(HashSet()) { it.key }
                result.toArm.filter { it.reminder.key in drawnKeys }.forEach {
                    Log.d(TAG, "Programado '${it.reminder.title}' para el ${DATE_FORMAT.format(it.time)}")
                }
                Log.d(
                    TAG,
                    "Recordatorios: ${planned.size} previstos, ${result.toArm.size} alarmas " +
                        "(${result.drawn} nuevas), ${result.toCancel.size} quitadas, " +
                        "exactas=${alarms.canScheduleExact()}"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error sincronizando recordatorios", e)
            }
        }
    }

    private suspend fun readScheduled(): Map<String, ScheduledReminder> =
        store.data.first()[SCHEDULED_KEY].orEmpty()
            .mapNotNull(ScheduledReminder::decode)
            .associateBy { it.key }

    /**
     * Paso único desde la versión con WorkManager: se quedan las horas ya sorteadas de los avisos
     * pendientes, los que ya saltaron se marcan como pasados (para que no se repitan hoy) y se
     * quitan los trabajos, que si no avisarían dos veces.
     */
    private suspend fun migrateFromWorkManager(): Unit = mutex.withLock {
        // Con el cerrojo: al actualizar la app, el arranque y ReminderBootReceiver llegan a la vez
        if (store.data.first()[MIGRATED_KEY] == true) return@withLock
        try {
            val workManager = WorkManager.getInstance(context)
            val migrated = workManager.getWorkInfosByTagFlow(LEGACY_REMINDER_TAG).first().mapNotNull { info ->
                val key = info.tagValue(LEGACY_KEY_PREFIX) ?: return@mapNotNull null
                val spec = info.tagValue(LEGACY_SPEC_PREFIX) ?: return@mapNotNull null
                when (info.state) {
                    WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> ScheduledReminder(key, spec, info.nextScheduleTimeMillis)
                    WorkInfo.State.SUCCEEDED, WorkInfo.State.RUNNING -> ScheduledReminder(key, spec, ALREADY_SHOWN)
                    else -> null
                }
            }
            workManager.cancelAllWorkByTag(LEGACY_REMINDER_TAG)
            store.edit { prefs ->
                val current = prefs[SCHEDULED_KEY].orEmpty().mapNotNull(ScheduledReminder::decode).associateBy { it.key }
                prefs[SCHEDULED_KEY] = (migrated.associateBy { it.key } + current).values.mapTo(HashSet()) { it.encode() }
                prefs[MIGRATED_KEY] = true
            }
            Log.d(TAG, "Migrados ${migrated.size} recordatorios desde WorkManager")
        } catch (e: IllegalStateException) {
            // WorkManager sin inicializar (p. ej. en tests): no hay nada que migrar
            store.edit { it[MIGRATED_KEY] = true }
        }
    }

    /** Elimina lo que dejaba el sistema anterior a WorkManager: sus trabajos y sus marcas en DataStore. */
    private suspend fun cleanUpLegacyScheduling() {
        runCatching { WorkManager.getInstance(context).cancelAllWorkByTag(LEGACY_TAG) }
        context.dataStore.edit { prefs ->
            LEGACY_KEYS.forEach { prefs.remove(stringPreferencesKey(it)) }
        }
    }

    private fun WorkInfo.tagValue(prefix: String) = tags.firstOrNull { it.startsWith(prefix) }?.removePrefix(prefix)

    private companion object {
        const val TAG = "ReminderManager"
        const val DEBOUNCE_MS = 500L
        const val RETRY_DELAY_MS = 1_000L
        /** Hora de los avisos que ya saltaron: en el pasado, así no se vuelven a poner. */
        const val ALREADY_SHOWN = 0L
        val SCHEDULED_KEY: Preferences.Key<Set<String>> = stringSetPreferencesKey("scheduled")
        val MIGRATED_KEY: Preferences.Key<Boolean> = booleanPreferencesKey("migrated_from_work_manager")

        // Versión con WorkManager (hasta noviembre de 2026)
        const val LEGACY_REMINDER_TAG = "reminder"
        const val LEGACY_KEY_PREFIX = "reminder-key:"
        const val LEGACY_SPEC_PREFIX = "reminder-spec:"
        // Versión anterior a esa
        const val LEGACY_TAG = "NOTIFICATION_WORK"
        val LEGACY_KEYS = listOf("scheduled_events_ids", "scheduled_tasks_ids")

        val DATE_FORMAT = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    }
}
