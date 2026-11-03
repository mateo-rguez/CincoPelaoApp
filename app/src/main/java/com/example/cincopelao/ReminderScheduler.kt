package com.example.cincopelao

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import androidx.datastore.preferences.core.edit

object ReminderScheduler {

    val eventMessages = listOf(
        "Recuerda que tienes el evento de abbrev",
        "Queda poco para el evento",
        "No te olvides del tipo de abbrev",
        "Vete pensando en lo de abbrev"
    )

    val taskMessages = listOf(
        "Recuerda que tienes que tarea",
        "Queda poco para tarea",
        "No te olvides de tarea para abbrev",
        "Vete pensando en tarea"
    )

    fun scheduleAllReminders(
        context: Context,
        events: List<Event>,
        tasks: List<Task>,
        subjects: List<Subject>,
        prefs: NotiPrefsManager
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val eventDays = prefs.eventDaysFlow.first()
            val eventStartHour = prefs.eventStartHourFlow.first()
            val eventEndHour = prefs.eventEndHourFlow.first()

            val taskDays = prefs.taskDaysFlow.first()
            val taskStartHour = prefs.taskStartHourFlow.first()
            val taskEndHour = prefs.taskEndHourFlow.first()

            // Programar eventos
            events.forEach { event ->
                eventDays.forEach { offset ->
                    val key = "${event.eventId}-$offset"

                    val template = eventMessages.random()
                    val title = template.replace("evento", event.name).replace("tipo", event.type.toLowerCase()).replace("abbrev", subjects.find { it.subjectId == event.subjectId }?.abbrev ?: "asignatura eliminada? ta mal hecho esto entonces")
                    val message = reminderMessageFor(offset)

                    if (!isScheduledEvent(context, key)) {
                        scheduleReminderIfValid(
                            context,
                            targetTime = event.date,
                            offsetDays = offset,
                            title = title,
                            message = message,
                            id = event.eventId + offset * 10000,
                            startHour = eventStartHour,
                            endHour = eventEndHour
                        )
                        markScheduledEvent(context, key)
                    }
                }
            }

            // Programar tareas
            tasks.forEach { task ->
                task.dueDate?.let { dueDate ->
                    taskDays.forEach { offset ->
                        val key = "${task.taskId}-$offset"

                        val template = taskMessages.random()
                        val title = template.replace("tarea", task.title.toLowerCase()).replace("abbrev", subjects.find { it.subjectId.toLong() == task.subjectId }?.abbrev ?: "asignatura eliminada? ta mal hecho esto entonces")
                        val message = reminderMessageFor(offset)

                        if (!isScheduledTask(context, key)) {
                            scheduleReminderIfValid(
                                context,
                                targetTime = dueDate,
                                offsetDays = offset,
                                title = title,
                                message = message,
                                id = task.taskId.toInt() + offset * 10000,
                                startHour = taskStartHour,
                                endHour = taskEndHour
                            )
                            markScheduledTask(context, key)
                        }
                    }
                }
            }
        }
    }

    private fun scheduleReminderIfValid(
        context: Context,
        targetTime: Long,
        offsetDays: Int,
        title: String,
        message: String,
        id: Int,
        startHour: Int,
        endHour: Int
    ) {
        val now = System.currentTimeMillis()
        val notifyTime = computeNotifyTime(
            targetTime = targetTime,
            offsetDays = offsetDays,
            startHour = startHour,
            endHour = endHour,
            now = now,
            random = Random.Default
        ) ?: return

        val delay = notifyTime - now
        val data = Data.Builder()
            .putString("title", title)
            .putString("message", message)
            .putInt("id", id)
            .build()

        val request = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInputData(data)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag("NOTIFICATION_WORK")
            .build()

        WorkManager.getInstance(context).enqueue(request)
        Log.d(
            "ReminderScheduler",
            "Notificación programada: $title el ${
                java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(notifyTime)
            }"
        )
    }


    /**
     * Calcula el instante en que debe saltar un recordatorio: un momento aleatorio entre
     * [startHour] y [endHour] del día situado [offsetDays] días antes de [targetTime].
     *
     * Devuelve `null` si ese momento ya ha pasado y no se puede programar.
     * Es una función pura (no lee el reloj ni el Random del sistema) para poder probarla.
     */
    internal fun computeNotifyTime(
        targetTime: Long,
        offsetDays: Int,
        startHour: Int,
        endHour: Int,
        now: Long,
        random: Random,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long? {
        // Día base: X días antes del target
        val notifyBaseTime = targetTime - TimeUnit.DAYS.toMillis(offsetDays.toLong())

        // Ajustamos el calendario a la medianoche local del día de la notificación
        val calendar = Calendar.getInstance(timeZone).apply {
            timeInMillis = notifyBaseTime
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val today = Calendar.getInstance(timeZone).apply { timeInMillis = now }

        // Generamos hora aleatoria dentro del rango configurado
        var notifyTime = calendar.timeInMillis + generateRandomMillisInRange(startHour, endHour, random)

        // Si el día de la notificación es hoy y la hora aleatoria ya pasó...
        if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
            notifyTime < now
        ) {
            val currentHour = today.get(Calendar.HOUR_OF_DAY)

            // Si estamos dentro de la ventana horaria, recalculamos desde la hora actual hasta el endHour
            if (currentHour < endHour) {
                // TODO(Fase 4): esto calcula la medianoche en UTC, no en la zona local,
                //  así que la hora resultante sale desplazada (1-2 h en España).
                notifyTime = now - now % TimeUnit.DAYS.toMillis(1) +
                        generateRandomMillisInRange(currentHour, endHour, random)
            } else {
                // Si ya hemos pasado la ventana horaria, no programamos nada
                return null
            }
        }

        // Si aún así sigue quedando en el pasado, no programamos nada
        return if (notifyTime < now) null else notifyTime
    }

    /** Texto del recordatorio según los días que faltan. */
    internal fun reminderMessageFor(offsetDays: Int): String = when {
        offsetDays == 1 -> "Es mañana, ¿Preparado? 💪"
        offsetDays <= 3 -> "Quedan $offsetDays unidades de día, tocó cookear 🐐"
        offsetDays < 7 -> "Faltan $offsetDays días, habrá que ir espabilando 🙄"
        else -> "Aún quedan $offsetDays días, hay tiempo 🥱"
    }

    private fun generateRandomMillisInRange(startHour: Int, endHour: Int, random: Random): Long {
        val randomHour = random.nextInt(startHour, endHour)
        val randomMinute = random.nextInt(0, 60)
        return TimeUnit.HOURS.toMillis(randomHour.toLong()) +
                TimeUnit.MINUTES.toMillis(randomMinute.toLong())
    }

    // --- Funciones de marcado y comprobación ---
    private val SCHEDULED_EVENTS_IDS = stringPreferencesKey("scheduled_events_ids")
    private val SCHEDULED_TASKS_IDS = stringPreferencesKey("scheduled_tasks_ids")

    suspend fun markScheduledEvent(context: Context, id: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[SCHEDULED_EVENTS_IDS]?.split(",")?.toMutableSet() ?: mutableSetOf()
            current.add(id)
            prefs[SCHEDULED_EVENTS_IDS] = current.joinToString(",")
        }
    }

    suspend fun markScheduledTask(context: Context, id: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[SCHEDULED_TASKS_IDS]?.split(",")?.toMutableSet() ?: mutableSetOf()
            current.add(id)
            prefs[SCHEDULED_TASKS_IDS] = current.joinToString(",")
        }
    }

    suspend fun isScheduledEvent(context: Context, id: String): Boolean {
        val prefsSnapshot = context.dataStore.data.first()
        val current = prefsSnapshot[SCHEDULED_EVENTS_IDS]?.split(",")?.toSet() ?: emptySet()
        return current.contains(id)
    }

    suspend fun isScheduledTask(context: Context, id: String): Boolean {
        val prefsSnapshot = context.dataStore.data.first()
        val current = prefsSnapshot[SCHEDULED_TASKS_IDS]?.split(",")?.toSet() ?: emptySet()
        return current.contains(id)
    }

    suspend fun clearAllScheduledReminders(context: Context) {
        context.dataStore.edit { prefs ->
            prefs.remove(SCHEDULED_EVENTS_IDS)
            prefs.remove(SCHEDULED_TASKS_IDS)
        }
    }
}

fun scheduleReminders(context: Context, prefs: NotiPrefsManager, events: List<Event>, tasks: List<Task>, subjects: List<Subject>) {
    CoroutineScope(Dispatchers.IO).launch {
        ReminderScheduler.scheduleAllReminders(
            context = context,
            events = events,
            tasks = tasks,
            subjects = subjects,
            prefs = prefs
        )
    }
}