package io.github.mateorguez.cincopelao.ui.calendar

import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.local.entity.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class TimelineTest {

    private val zone = ZoneId.of("Europe/Madrid")
    private val today = LocalDate.of(2026, 11, 10)
    private fun millis(date: LocalDate, hour: Int = 12) = date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    private val subject = Subject(subjectId = 1, name = "Cálculo")

    @Test
    fun `agrupa por dia, en orden y sin lo pasado`() {
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

        assertEquals(listOf(today, today.plusDays(5)), timeline.map { it.date })
        assertEquals(listOf(listOf("task-7"), listOf("event-1", "task-9")), timeline.map { day -> day.items.map { it.key } })
    }

    @Test
    fun `un evento cuya asignatura no ha llegado aun no rompe nada`() {
        val events = listOf(Event(eventId = 1, subjectId = 99, name = "Final", type = "Examen", date = millis(today)))

        val item = buildTimeline(events, emptyList(), emptyList(), today, zone).single().items.single()

        assertNull(item.subject)
        assertNull(item.subjectLine())
    }

    @Test
    fun `cuanto falta para cada dia`() {
        assertEquals("Hoy", relativeDayLabel(today, today))
        assertEquals("Mañana", relativeDayLabel(today.plusDays(1), today))
        assertEquals("En 6 días", relativeDayLabel(today.plusDays(6), today))
        assertEquals("En 13 días", relativeDayLabel(today.plusDays(13), today))
        assertEquals("En 2 sem.", relativeDayLabel(today.plusDays(14), today))
        assertEquals("En 3 sem.", relativeDayLabel(today.plusDays(20), today))
        assertEquals("En 8 sem.", relativeDayLabel(today.plusDays(56), today))
        assertEquals("En 2 meses", relativeDayLabel(today.plusDays(60), today))
        assertEquals("En 4 meses", relativeDayLabel(today.plusDays(120), today))
    }

    @Test
    fun `cabecera de cada dia`() {
        val es = Locale.forLanguageTag("es-ES")
        assertEquals("Martes 10 nov", dayHeaderLabel(today, es))
        assertEquals("Miércoles 2 sept", dayHeaderLabel(LocalDate.of(2026, 9, 2), es))
    }

    @Test
    fun `forma y etiqueta segun el tipo`() {
        fun event(type: String, modal: String? = null) =
            TimelineItem.EventItem(Event(subjectId = 1, name = "x", type = type, modal = modal, date = 0), subject)

        assertEquals(TimelineMarker.EXAM, event("Examen").marker())
        assertEquals(TimelineMarker.DELIVERABLE, event("Proyecto").marker())
        assertEquals(TimelineMarker.OTHER_EVENT, event("Debate").marker())
        assertEquals(TimelineMarker.TASK, TimelineItem.TaskItem(Task(title = "t"), null).marker())

        assertEquals("Examen · Teoría", event("Examen", "Teoría").typeLabel())
        assertEquals("Examen", event("Examen", " ").typeLabel())
        assertEquals("Tarea", TimelineItem.TaskItem(Task(title = "t"), subject).typeLabel())
        assertEquals("CÁ · Cálculo", event("Examen").subjectLine())
    }
}
