package io.github.mateorguez.cincopelao.notifications

import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.local.entity.Task
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs
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

    private val eventTemplates = listOf(
        "Recuerda que tienes el evento de abbrev",
        "Queda poco para el evento",
        "No te olvides del tipo de abbrev",
        "Vete pensando en lo de abbrev"
    )

    private val taskTemplates = listOf(
        "Recuerda que tienes que tarea",
        "Queda poco para tarea",
        "No te olvides de tarea para abbrev",
        "Vete pensando en tarea"
    )

    private const val MISSING_SUBJECT = "asignatura eliminada? ta mal hecho esto entonces"

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
            eventSettings.daysBefore.distinct().map { offset ->
                val key = "$instanceId:event:${event.eventId}:$offset"
                val title = pick(eventTemplates, key)
                    .replace("evento", event.name)
                    .replace("tipo", event.type.lowercase())
                    .replace("abbrev", abbrevById[event.subjectId] ?: MISSING_SUBJECT)
                PlannedReminder(key, event.date, offset, eventSettings.startHour, eventSettings.endHour, title, reminderMessageFor(offset))
            }
        }

        val taskReminders = tasks
            .filter { !it.isCompleted && !it.archived && (it.dueDate ?: 0L) > now }
            .flatMap { task ->
                taskSettings.daysBefore.distinct().map { offset ->
                    val key = "$instanceId:task:${task.taskId}:$offset"
                    val title = pick(taskTemplates, key)
                        .replace("tarea", task.title.lowercase())
                        .replace("abbrev", task.subjectId?.let { abbrevById[it] } ?: MISSING_SUBJECT)
                    PlannedReminder(key, task.dueDate!!, offset, taskSettings.startHour, taskSettings.endHour, title, reminderMessageFor(offset))
                }
            }

        return eventReminders + taskReminders
    }

    /** Elige una plantilla de forma determinista: el mismo recordatorio siempre usa la misma. */
    private fun pick(templates: List<String>, key: String) = templates[abs(key.hashCode() % templates.size)]

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

    /** Texto del recordatorio según los días que faltan. */
    internal fun reminderMessageFor(offsetDays: Int): String = when {
        offsetDays == 1 -> "Es mañana, ¿Preparado? 💪"
        offsetDays <= 3 -> "Quedan $offsetDays unidades de día, tocó cookear 🐐"
        offsetDays < 7 -> "Faltan $offsetDays días, habrá que ir espabilando 🙄"
        else -> "Aún quedan $offsetDays días, hay tiempo 🥱"
    }
}
