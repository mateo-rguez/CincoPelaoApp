package io.github.mateorguez.cincopelao.ui.notifications

import io.github.mateorguez.cincopelao.notifications.ReminderSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderTextsTest {

    @Test
    fun `pastillas fijas mas las anadidas a mano, en orden`() {
        val chips = dayChips(listOf(7, 3, 1, 10))
        assertEquals(listOf(1, 2, 3, 5, 7, 10, 14), chips.map { it.day })
        assertEquals(listOf(1, 3, 7, 10), chips.filter { it.selected }.map { it.day })
    }

    @Test
    fun `frase con varios dias, uno solo o ninguno`() {
        assertEquals(
            "Te avisaremos 7, 3 y 1 días antes de cada evento, a una hora al azar entre las 8:00 y las 19:00.",
            reminderSummary(ReminderKind.Events, ReminderSettings(listOf(1, 7, 3), 8, 19))
        )
        assertEquals(
            "Te avisaremos 1 día antes de cada tarea, a una hora al azar entre las 9:00 y las 24:00.",
            reminderSummary(ReminderKind.Tasks, ReminderSettings(listOf(1), 9, 24))
        )
        assertEquals(
            "Te avisaremos 2 días antes de cada tarea, a una hora al azar entre las 0:00 y las 1:00.",
            reminderSummary(ReminderKind.Tasks, ReminderSettings(listOf(2), 0, 1))
        )
        assertEquals("No recibirás avisos de las tareas.", reminderSummary(ReminderKind.Tasks, ReminderSettings(emptyList(), 8, 22)))
    }

    @Test
    fun `se resaltan los dias y las horas`() {
        val parts = reminderSummaryParts(ReminderKind.Events, ReminderSettings(listOf(3, 1), 8, 24))
        assertEquals(listOf("3 y 1 días antes", "8:00", "24:00"), parts.filter { it.highlight }.map { it.text })
    }
}
