package io.github.mateorguez.cincopelao.ui.tasks

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Días naturales desde [now] hasta la fecha indicada (negativo si ya pasó). */
fun daysUntil(timestampMillis: Long, now: Long = System.currentTimeMillis()): Int {
    fun startOfDay(millis: Long) = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    // round y no división entera, para que los días de cambio de hora (23 o 25 h) cuenten como un día
    return Math.round((startOfDay(timestampMillis) - startOfDay(now)) / (1000.0 * 60 * 60 * 24)).toInt()
}

/** Texto corto para la fecha límite de una tarea: "Hoy", "Mañana", "En 3 días"... */
fun dueDateLabel(dueDate: Long, now: Long = System.currentTimeMillis()): String {
    val daysLeft = daysUntil(dueDate, now)
    return when {
        daysLeft < 0 -> "Para hace ${-daysLeft} días"
        daysLeft == 0 -> "Hoy"
        daysLeft == 1 -> "Mañana"
        else -> "En $daysLeft días"
    }
}

/** Cuándo se completó una tarea: "hoy", "hace 3 días" o "el 5 nov". */
fun completedLabel(completedAt: Long?, now: Long = System.currentTimeMillis()): String {
    if (completedAt == null) return "fecha desconocida"
    val daysAgo = -daysUntil(completedAt, now)
    return when {
        daysAgo < 1 -> "hoy"
        daysAgo < 7 -> "hace $daysAgo día${if (daysAgo > 1) "s" else ""}"
        else -> "el " + SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(completedAt))
    }
}
