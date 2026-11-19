package io.github.mateorguez.cincopelao.notifications

import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.local.entity.Task
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.max
import kotlin.random.Random

/** Configuración de avisos para eventos o para tareas. */
data class ReminderSettings(
    val daysBefore: List<Int>,
    val startHour: Int,
    val endHour: Int
)

/**
 * Un recordatorio que debería existir.
 *
 * [key] identifica el recordatorio de forma estable (instancia, tipo, id y días de antelación),
 * así eventos y tareas con el mismo id no se pisan. [spec] resume todo lo que afecta al aviso:
 * si no cambia, el recordatorio ya programado se conserva tal cual.
 */
data class PlannedReminder(
    val key: String,
    val targetTime: Long,
    val offsetDays: Int,
    val startHour: Int,
    val endHour: Int,
    val title: String,
    val message: String
) {
    val spec: String get() = listOf(targetTime, offsetDays, startHour, endHour, title, message).joinToString("|").hashCode().toString()
}

/** Lógica pura de recordatorios: qué avisos tocan y a qué hora. Sin Android, para poder probarla. */
object ReminderPlanner {

    /**
     * Recordatorios que deberían existir: eventos futuros y tareas futuras pendientes
     * (ni completadas ni archivadas), uno por cada día de antelación configurado.
     */
    fun plan(
        instanceId: String,
        subjects: List<Subject>,
        events: List<Event>,
        tasks: List<Task>,
        eventSettings: ReminderSettings,
        taskSettings: ReminderSettings,
        now: Long
    ): List<PlannedReminder> {
        val abbrevById = subjects.associate { it.subjectId to it.abbrev }

        val eventReminders = events.filter { it.date > now }.flatMap { event ->
            val values = buildMap {
                put("nombre", event.name.trim())
                abbrevById[event.subjectId]?.let { put("asig", it) }
                ReminderPhrases.typeWithArticle(event.type)?.let { put("el_tipo", it) }
            }
            eventSettings.daysBefore.distinct().map { offset ->
                val key = "$instanceId:event:${event.eventId}:$offset"
                PlannedReminder(
                    key, event.date, offset, eventSettings.startHour, eventSettings.endHour,
                    title = ReminderPhrases.pick(ReminderPhrases.eventTitles, values, key),
                    message = reminderMessageFor(offset, key)
                )
            }
        }

        val taskReminders = tasks.mapNotNull { task ->
            val due = task.dueDate
            if (task.isCompleted || task.archived || due == null || due <= now) null else task to due
        }.flatMap { (task, due) ->
            val values = buildMap {
                put("nombre", ReminderPhrases.lowerFirst(task.title))
                task.subjectId?.let { abbrevById[it] }?.let { put("asig", it) }
            }
            taskSettings.daysBefore.distinct().map { offset ->
                val key = "$instanceId:task:${task.taskId}:$offset"
                PlannedReminder(
                    key, due, offset, taskSettings.startHour, taskSettings.endHour,
                    title = ReminderPhrases.pick(ReminderPhrases.taskTitles, values, key),
                    message = reminderMessageFor(offset, key)
                )
            }
        }

        return eventReminders + taskReminders
    }

    /**
     * Calcula el instante en que debe saltar un recordatorio: un momento aleatorio dentro de la
     * franja [startHour, endHour) del día situado [offsetDays] días antes de [targetTime], en hora local.
     * Si ese día es hoy y la franja ya ha empezado, el aviso cae entre ahora y el final de la franja.
     *
     * Devuelve `null` si la franja de ese día ya ha terminado.
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
        // Una franja vacía o invertida se trata como una franja de una hora desde startHour
        val start = startHour.coerceIn(0, 23)
        val end = endHour.coerceIn(start + 1, 24)

        fun at(hour: Int) = Calendar.getInstance(timeZone).apply {
            timeInMillis = targetTime
            add(Calendar.DAY_OF_YEAR, -offsetDays)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // set (y no add) para que los días de cambio de hora sigan siendo horas "de reloj"
            if (hour >= 24) add(Calendar.DAY_OF_YEAR, 1) else set(Calendar.HOUR_OF_DAY, hour)
        }.timeInMillis

        val windowStart = at(start)
        val windowEnd = at(end)
        if (now >= windowEnd) return null

        val from = max(windowStart, now)
        return from + random.nextLong(windowEnd - from)
    }

    /** Texto del recordatorio según los días que faltan; [key] elige la frase de forma estable. */
    internal fun reminderMessageFor(offsetDays: Int, key: String): String =
        ReminderPhrases.pick(ReminderPhrases.messagesFor(offsetDays), mapOf("dias" to "$offsetDays"), "$key:msg")
}
