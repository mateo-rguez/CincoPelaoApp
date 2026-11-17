package io.github.mateorguez.cincopelao.notifications

import kotlin.random.Random

/**
 * Un recordatorio ya programado, tal como se guarda entre ejecuciones: su hora sorteada se
 * conserva mientras su [spec] no cambie. Si [time] ya pasó, el aviso ya saltó (o se perdió) y no
 * se vuelve a poner.
 */
data class ScheduledReminder(val key: String, val spec: String, val time: Long) {

    fun encode(): String = listOf(key, spec, time.toString()).joinToString(SEPARATOR)

    companion object {
        private const val SEPARATOR = "\t"

        fun decode(stored: String): ScheduledReminder? {
            val parts = stored.split(SEPARATOR)
            if (parts.size != 3) return null
            val time = parts[2].toLongOrNull() ?: return null
            return ScheduledReminder(parts[0], parts[1], time)
        }
    }
}

/** Una alarma que hay que poner: qué recordatorio y a qué hora. */
data class ReminderAlarm(val reminder: PlannedReminder, val time: Long)

/**
 * Resultado de reconciliar: el nuevo estado que guardar, las alarmas que poner y las que quitar.
 * [toArm] incluye también las que ya estaban puestas y siguen igual: volver a ponerlas no cambia
 * nada y así reaparecen si el sistema las borró (reinicio, forzar detención...).
 */
data class ReminderSyncResult(
    val scheduled: List<ScheduledReminder>,
    val toArm: List<ReminderAlarm>,
    val toCancel: List<String>,
    /** Recordatorios nuevos o cambiados a los que se les ha sorteado hora. */
    val drawn: Int
)

/** Lógica pura de la reconciliación de recordatorios, sin Android, para poder probarla. */
object ReminderSync {

    fun reconcile(
        planned: List<PlannedReminder>,
        stored: Map<String, ScheduledReminder>,
        now: Long,
        random: Random
    ): ReminderSyncResult {
        val scheduled = mutableListOf<ScheduledReminder>()
        val toArm = mutableListOf<ReminderAlarm>()
        val toCancel = mutableListOf<String>()
        var drawn = 0

        for (reminder in planned) {
            val previous = stored[reminder.key]
            if (previous != null && previous.spec == reminder.spec) {
                // Sin cambios: misma hora; si ya pasó, ya avisó y no se repite
                scheduled += previous
                if (previous.time > now) toArm += ReminderAlarm(reminder, previous.time)
                continue
            }

            val time = ReminderPlanner.computeNotifyTime(
                reminder.targetTime, reminder.offsetDays, reminder.startHour, reminder.endHour, now, random
            )
            if (time == null) {
                // Su franja ya terminó: no toca avisar (y si había alarma, se quita)
                if (previous != null) toCancel += reminder.key
                continue
            }
            scheduled += ScheduledReminder(reminder.key, reminder.spec, time)
            toArm += ReminderAlarm(reminder, time)
            drawn++
        }

        val plannedKeys = planned.mapTo(HashSet()) { it.key }
        toCancel += stored.keys.filter { it !in plannedKeys }

        return ReminderSyncResult(scheduled, toArm, toCancel, drawn)
    }
}
