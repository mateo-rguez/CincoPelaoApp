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
                    val message = when {
                        offset == 1 -> "Es mañana, ¿Preparado? 💪"
                        offset <= 3 -> "Quedan $offset unidades de día, tocó cookear 🐐"
                        offset < 7 -> "Faltan $offset días, habrá que ir espabilando 🙄"
                        else -> "Aún quedan $offset días, hay tiempo 🥱"
                    }

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
                        val message = when {
                            offset == 1 -> "Es mañana, ¿Preparado? 💪"
                            offset <= 3 -> "Quedan $offset unidades de día, tocó cookear 🐐"
                            offset < 7 -> "Faltan $offset días, habrá que ir espabilando 🙄"
                            else -> "Aún quedan $offset días, hay tiempo 🥱"
                        }

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
        // Día base: X días antes del target
        val notifyBaseTime = targetTime - TimeUnit.DAYS.toMillis(offsetDays.toLong())

        // Ajustamos el calendario a la medianoche local del día de la notificación
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = notifyBaseTime
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        // Generamos hora aleatoria dentro del rango configurado
        var randomMillis = generateRandomMillisInRange(startHour, endHour)
        var notifyTime = calendar.timeInMillis + randomMillis

        val now = System.currentTimeMillis()

        // Si el día de la notificación es hoy y la hora aleatoria ya pasó...
        if (calendar.get(java.util.Calendar.YEAR) == java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) &&
            calendar.get(java.util.Calendar.DAY_OF_YEAR) == java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR) &&
            notifyTime < now
        ) {
            val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

            // Si estamos dentro de la ventana horaria, recalculamos desde la hora actual hasta el endHour
            if (currentHour < endHour) {
                randomMillis = generateRandomMillisInRange(currentHour, endHour)
                notifyTime = System.currentTimeMillis() - System.currentTimeMillis() % TimeUnit.DAYS.toMillis(1) + randomMillis
            } else {
                // Si ya hemos pasado la ventana horaria, salimos y no programamos nada
                return
            }
        }

        // Si aún así sigue quedando en el pasado, no programamos nada
        if (notifyTime < now) return

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


    private fun generateRandomMillisInRange(startHour: Int, endHour: Int): Long {
        val randomHour = Random.nextInt(startHour, endHour)
        val randomMinute = Random.nextInt(0, 60)
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