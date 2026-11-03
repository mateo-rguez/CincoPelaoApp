package io.github.mateorguez.cincopelao.ui.calendar

import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.local.entity.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TimelineTest {

    private val zone = ZoneId.of("Europe/Madrid")
    private val today = LocalDate.of(2026, 11, 10)
    private fun millis(date: LocalDate, hour: Int = 12) = date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    private val subject = Subject(subjectId = 1, name = "Cálculo")

    @Test
    fun `agrupa por dia con cabecera, en orden y sin lo pasado`() {
        val events = listOf(
            Event(eventId = 1, subjectId = 1, name = "Final", type = "Examen", date = millis(today.plusDays(5))),
            Event(eventId = 2, subjectId = 1, name = "Viejo", type = "Examen", date = millis(today.minusDays(1)))
        )
        val tasks = listOf(
            Task(taskId = 7, title = "Entrega", dueDate = millis(today)),
            Task(taskId = 8, title = "Sin fecha"),
            Task(taskId = 9, title = "Mismo día", dueDate = millis(today.plusDays(5), hour = 9))
        )

        val timeline = buildTimeline(events, tasks, listOf(subject), today, zone)

        assertEquals(
            listOf("header-$today", "task-7", "header-${today.plusDays(5)}", "event-1", "task-9"),
            timeline.map { it.key }
        )
    }

    @Test
    fun `un evento cuya asignatura no ha llegado aun no rompe nada`() {
        val events = listOf(Event(eventId = 1, subjectId = 99, name = "Final", type = "Examen", date = millis(today)))

        val item = buildTimeline(events, emptyList(), emptyList(), today, zone)[1] as TimelineEntry.EventItem

        assertNull(item.subject)
    }
}
