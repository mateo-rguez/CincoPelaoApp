package io.github.mateorguez.cincopelao.ui.tasks

import java.util.Calendar

/** Días naturales desde hoy hasta la fecha indicada (negativo si ya pasó). */
fun daysUntil(timestampMillis: Long): Int {
    fun Calendar.atStartOfDay() = apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val today = Calendar.getInstance().atStartOfDay()
    val due = Calendar.getInstance().apply { timeInMillis = timestampMillis }.atStartOfDay()
    return ((due.timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
}

/** Texto corto para la fecha límite de una tarea: "Hoy", "Mañana", "En 3 días"... */
fun dueDateLabel(dueDate: Long): String {
    val daysLeft = daysUntil(dueDate)
    return when {
        daysLeft < 0 -> "Para hace ${-daysLeft} días"
        daysLeft == 0 -> "Hoy"
        daysLeft == 1 -> "Mañana"
        else -> "En $daysLeft días"
    }
}
