package io.github.mateorguez.cincopelao.ui.subjects

import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.local.entity.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubjectSummaryTest {

    private val day = 24L * 60 * 60 * 1000
    // Mediodía, para que sumar o restar días no cruce medianoche por la hora
    private val now = java.util.Calendar.getInstance().apply {
        set(2026, 10, 11, 12, 0, 0)
    }.timeInMillis

    private val subject = Subject(subjectId = 1, name = "Análisis de Circuitos", course = "1º")

    private fun event(name: String, offsetDays: Int, score: Float? = null, puntuable: Boolean = true, done: Boolean = false) =
        Event(subjectId = 1, name = name, type = "Examen", isPuntuable = puntuable, weight = 50f,
            score = score, noPuntuableDone = done, date = now + offsetDays * day)

    @Test
    fun `el próximo evento es el más cercano sin hacer, incluido hoy`() {
        val events = listOf(
            event("Pasado", -3),
            event("Lejano", 10),
            event("Hoy hecho", 0, score = 8f),
            event("Hoy", 0),
            event("Práctica hecha", 1, puntuable = false, done = true)
        )
        val summary = subjectSummaries(listOf(subject), events, emptyList(), now).single()
        assertEquals("Hoy", summary.nextEvent?.name)
    }

    @Test
    fun `sin eventos futuros no hay próximo y sin notas no hay media`() {
        val summary = subjectSummaries(listOf(subject), listOf(event("Pasado", -1)), emptyList(), now).single()
        assertNull(summary.nextEvent)
        assertNull(summary.average)
    }

    @Test
    fun `la media es la de los eventos puntuables con nota`() {
        val events = listOf(event("A", -5, score = 6f), event("B", -2, score = 9f), event("C", 4))
        val summary = subjectSummaries(listOf(subject), events, emptyList(), now).single()
        assertEquals(7.5f, summary.average!!, 0.001f)
    }

    @Test
    fun `el último examen es el calificado más reciente`() {
        val events = listOf(event("Primero", -9, score = 4f), event("Segundo", -2, score = 8f), event("Futuro", 3))
        val summary = subjectSummaries(listOf(subject), events, emptyList(), now).single()
        assertEquals("Segundo", summary.lastGraded?.name)
    }

    @Test
    fun `hay nota final solo con todos los eventos hechos`() {
        val pending = listOf(event("A", -5, score = 6f), event("B", 2))
        assertNull(subjectSummaries(listOf(subject), pending, emptyList(), now).single().finalGrade)

        // Pesos del 50 %: 6·0,5 + 9·0,5; la práctica no puntuable cuenta si está hecha
        val done = listOf(event("A", -5, score = 6f), event("B", -1, score = 9f), event("Práctica", -3, puntuable = false, done = true))
        assertEquals(7.5f, subjectSummaries(listOf(subject), done, emptyList(), now).single().finalGrade!!, 0.001f)

        val practicePending = done.dropLast(1) + event("Práctica", -3, puntuable = false)
        assertNull(subjectSummaries(listOf(subject), practicePending, emptyList(), now).single().finalGrade)
    }

    @Test
    fun `solo cuentan las tareas principales pendientes de la asignatura`() {
        val tasks = listOf(
            Task(taskId = 1, title = "Pendiente", subjectId = 1),
            Task(taskId = 2, title = "Subtarea", subjectId = 1, parentTaskId = 1),
            Task(taskId = 3, title = "Hecha", subjectId = 1, isCompleted = true),
            Task(taskId = 4, title = "Archivada", subjectId = 1, archived = true),
            Task(taskId = 5, title = "Otra asignatura", subjectId = 2),
            Task(taskId = 6, title = "General")
        )
        val summary = subjectSummaries(listOf(subject), emptyList(), tasks, now).single()
        assertEquals(1, summary.pendingTasks)
    }

    @Test
    fun `la pastilla usa la abreviatura o las iniciales`() {
        assertEquals("ACIR", subjectBadge(subject.copy(abbrev = "acir")))
        assertEquals("AC", subjectBadge(subject))
        assertEquals("AO", subjectBadge(Subject(name = "Arquitectura de Ordenadores")))
        assertEquals("BE", subjectBadge(Subject(name = "bebi")))
        assertEquals("DE", subjectBadge(Subject(name = "de")))
        assertEquals("?", subjectBadge(Subject(name = "  ")))
    }

    private fun summary(id: Long, average: Float?, finalGrade: Float? = null) = SubjectSummary(
        subject = subject.copy(subjectId = id), badge = "", average = average, lastGraded = null,
        finalGrade = finalGrade, nextEvent = null, pendingTasks = 0
    )

    @Test
    fun `la media global cuenta cada asignatura igual y prefiere su nota final`() {
        val overall = overallAverage(listOf(
            summary(1, average = 8f),
            summary(2, average = 9f, finalGrade = 6f), // terminada: cuenta su nota final
            summary(3, average = null),                // sin notas: no cuenta
        ))
        assertEquals(7f, overall?.value ?: -1f, 0.001f)
        assertEquals(2, overall?.subjects)
    }

    @Test
    fun `sin notas no hay media global`() {
        assertNull(overallAverage(listOf(summary(1, average = null))))
        assertNull(overallAverage(emptyList()))
    }
}
