package io.github.mateorguez.cincopelao.ui.notifications

import io.github.mateorguez.cincopelao.notifications.ReminderSettings

// Lógica pura de la pantalla de notificaciones: qué pastillas de días se muestran y la frase que
// resume los ajustes.

/** Días que siempre se ofrecen como pastillas, estén o no activados. */
val PresetReminderDays = listOf(1, 2, 3, 5, 7, 14)

/** Máximo de días de antelación que se puede añadir a mano. */
const val MAX_REMINDER_DAYS = 365

data class DayChip(val day: Int, val selected: Boolean)

/** Las pastillas fijas y las que se añadieron a mano, en orden, marcadas si están activadas. */
fun dayChips(selected: List<Int>, presets: List<Int> = PresetReminderDays): List<DayChip> =
    (presets + selected).distinct().sorted().map { DayChip(it, it in selected) }

fun dayLabel(day: Int): String = if (day == 1) "1 día" else "$day días"

fun hourLabel(hour: Int): String = "$hour:00"

/** Un trozo de la frase resumen; los importantes (días y horas) se resaltan. */
data class SummaryPart(val text: String, val highlight: Boolean = false)

/**
 * "Te avisaremos **7, 3 y 1 días antes** de cada evento, a una hora al azar entre las **8:00** y
 * las **24:00**." Sin días: "No recibirás avisos de los eventos."
 */
fun reminderSummaryParts(kind: ReminderKind, settings: ReminderSettings): List<SummaryPart> {
    val what = if (kind == ReminderKind.Events) "evento" else "tarea"
    val days = settings.daysBefore.distinct().sortedDescending()
    if (days.isEmpty()) {
        return listOf(SummaryPart("No recibirás avisos de ${if (kind == ReminderKind.Events) "los eventos" else "las tareas"}."))
    }
    val list = when (days.size) {
        1 -> days.single().toString()
        else -> days.dropLast(1).joinToString(", ") + " y " + days.last()
    }
    val unit = if (days.size == 1 && days.single() == 1) "día" else "días"
    return listOf(
        SummaryPart("Te avisaremos "),
        SummaryPart("$list $unit antes", highlight = true),
        SummaryPart(" de cada $what, a una hora al azar entre las "),
        SummaryPart(hourLabel(settings.startHour), highlight = true),
        SummaryPart(" y las "),
        SummaryPart(hourLabel(settings.endHour), highlight = true),
        SummaryPart(".")
    )
}

fun reminderSummary(kind: ReminderKind, settings: ReminderSettings): String =
    reminderSummaryParts(kind, settings).joinToString("") { it.text }
