package io.github.mateorguez.cincopelao.notifications

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.mateorguez.cincopelao.data.instances.InstanceRepository
import io.github.mateorguez.cincopelao.data.prefs.NotiPrefsManager
import io.github.mateorguez.cincopelao.data.prefs.dataStore
import io.github.mateorguez.cincopelao.data.repository.EventRepository
import io.github.mateorguez.cincopelao.data.repository.SubjectRepository
import io.github.mateorguez.cincopelao.data.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Mantiene los recordatorios programados en WorkManager sincronizados con los datos.
 *
 * Observa las asignaturas, eventos, tareas y ajustes de la instancia activa y, en cada cambio,
 * reconcilia: programa los recordatorios que faltan, cancela los que sobran (tareas completadas,
 * elementos borrados, fechas cambiadas...) y no toca los que siguen igual, para que su hora
 * aleatoria no se vuelva a sortear. La interfaz ya no tiene que cancelar ni reprogramar nada.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Singleton
class ReminderManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val instances: InstanceRepository,
    private val subjects: SubjectRepository,
    private val events: EventRepository,
    private val tasks: TaskRepository,
    private val prefs: NotiPrefsManager
) {
    private val workManager get() = WorkManager.getInstance(context)

    fun start(scope: CoroutineScope) {
        scope.launch {
            cleanUpLegacyScheduling()
            plannedReminders()
                .debounce(DEBOUNCE_MS)
                .collectLatest { planned ->
                    try {
                        reconcile(planned)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sincronizando recordatorios", e)
                    }
                }
        }
    }

    private fun settings(): Flow<Pair<ReminderSettings, ReminderSettings>> = combine(
        prefs.eventDaysFlow, prefs.eventStartHourFlow, prefs.eventEndHourFlow,
        prefs.taskDaysFlow, combine(prefs.taskStartHourFlow, prefs.taskEndHourFlow, ::Pair)
    ) { eDays, eStart, eEnd, tDays, (tStart, tEnd) ->
        ReminderSettings(eDays, eStart, eEnd) to ReminderSettings(tDays, tStart, tEnd)
    }

    private fun plannedReminders(): Flow<List<PlannedReminder>> =
        instances.currentInstanceId.flatMapLatest { instanceId ->
            combine(subjects.subjects, events.events, tasks.tasks, settings()) { subjects, events, tasks, (eventSettings, taskSettings) ->
                ReminderPlanner.plan(instanceId, subjects, events, tasks, eventSettings, taskSettings, System.currentTimeMillis())
            }
        }

    private suspend fun reconcile(planned: List<PlannedReminder>) {
        // Recordatorios que ya existen (pendientes o ya mostrados), por clave
        val existing: Map<String, String?> = workManager.getWorkInfosByTagFlow(TAG_REMINDER).first()
            .filter { it.state != WorkInfo.State.CANCELLED && it.state != WorkInfo.State.FAILED }
            .mapNotNull { info -> info.tagValue(KEY_PREFIX)?.let { it to info.tagValue(SPEC_PREFIX) } }
            .toMap()

        val now = System.currentTimeMillis()
        var scheduled = 0
        var cancelled = 0

        for (reminder in planned) {
            if (existing[reminder.key] == reminder.spec) continue // sin cambios (o ya se mostró)

            val time = ReminderPlanner.computeNotifyTime(
                reminder.targetTime, reminder.offsetDays, reminder.startHour, reminder.endHour, now, Random.Default
            )
            if (time == null) {
                if (reminder.key in existing) { workManager.cancelUniqueWork(workName(reminder.key)); cancelled++ }
                continue
            }
            workManager.enqueueUniqueWork(workName(reminder.key), ExistingWorkPolicy.REPLACE, request(reminder, time - now))
            scheduled++
            Log.d(TAG, "Programado '${reminder.title}' para el ${DATE_FORMAT.format(time)}")
        }

        val plannedKeys = planned.mapTo(HashSet()) { it.key }
        (existing.keys - plannedKeys).forEach { workManager.cancelUniqueWork(workName(it)); cancelled++ }

        Log.d(TAG, "Recordatorios: ${planned.size} previstos, $scheduled programados, $cancelled cancelados")
    }

    private fun request(reminder: PlannedReminder, delayMs: Long) =
        OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putString(NotificationWorker.KEY_TITLE, reminder.title)
                    .putString(NotificationWorker.KEY_MESSAGE, reminder.message)
                    .putInt(NotificationWorker.KEY_ID, reminder.key.hashCode())
                    .build()
            )
            .addTag(TAG_REMINDER)
            .addTag(KEY_PREFIX + reminder.key)
            .addTag(SPEC_PREFIX + reminder.spec)
            .build()

    /** Elimina lo que dejaba el sistema anterior: sus trabajos y sus marcas en DataStore. */
    private suspend fun cleanUpLegacyScheduling() {
        workManager.cancelAllWorkByTag(LEGACY_TAG)
        context.dataStore.edit { prefs ->
            LEGACY_KEYS.forEach { prefs.remove(stringPreferencesKey(it)) }
        }
    }

    private fun WorkInfo.tagValue(prefix: String) = tags.firstOrNull { it.startsWith(prefix) }?.removePrefix(prefix)

    private fun workName(key: String) = "reminder:$key"

    private companion object {
        const val TAG = "ReminderManager"
        const val TAG_REMINDER = "reminder"
        const val KEY_PREFIX = "reminder-key:"
        const val SPEC_PREFIX = "reminder-spec:"
        const val DEBOUNCE_MS = 500L
        const val LEGACY_TAG = "NOTIFICATION_WORK"
        val LEGACY_KEYS = listOf("scheduled_events_ids", "scheduled_tasks_ids")
        val DATE_FORMAT = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    }
}
