package io.github.mateorguez.cincopelao.sharing.protocol

import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.local.entity.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SharePayloadTest {

    private val subjects = listOf(Subject(subjectId = 1, name = "Cálculo"), Subject(subjectId = 2, name = "Física"))
    private val events = listOf(
        Event(eventId = 10, subjectId = 1, name = "Parcial", type = "Examen", isPuntuable = true, weight = 40f, score = 7.5f, date = 0),
        Event(eventId = 11, subjectId = 2, name = "Práctica", type = "Entrega", noPuntuableDone = true, date = 0)
    )
    private val tasks = listOf(
        Task(taskId = 100, title = "Ejercicios", subjectId = 1),
        Task(taskId = 101, title = "Apartado a", subjectId = 1, parentTaskId = 100, isCompleted = true, completedAt = 5),
        Task(taskId = 200, title = "Comprar folios"),
        Task(taskId = 201, title = "Subtarea general", parentTaskId = 200),
        Task(taskId = 300, title = "Vieja", subjectId = 2, isCompleted = true, archived = true)
    )

    private fun build(selection: ShareSelection) = buildSharePayload(subjects, events, tasks, selection)

    @Test
    fun `tareas de asignatura sin asignaturas no se pueden pedir`() {
        val selection = ShareSelection(subjectTasks = true, generalTasks = true)

        assertFalse(selection.normalized().subjectTasks)
        val payload = build(selection)
        assertTrue(payload.subjects.isEmpty())
        assertEquals(listOf(200L, 201L), payload.tasks.map { it.taskId })
    }

    @Test
    fun `asignaturas con sus tareas`() {
        val payload = build(ShareSelection(subjectsAndEvents = true, subjectTasks = true))

        assertEquals(2, payload.subjects.size)
        assertEquals(2, payload.events.size)
        assertEquals(listOf(100L, 101L), payload.tasks.map { it.taskId })
    }

    @Test
    fun `asignaturas sin tareas`() {
        val payload = build(ShareSelection(subjectsAndEvents = true))

        assertTrue(payload.tasks.isEmpty())
    }

    @Test
    fun `solo tareas generales, con sus subtareas`() {
        val payload = build(ShareSelection(generalTasks = true))

        assertTrue(payload.subjects.isEmpty())
        assertTrue(payload.events.isEmpty())
        assertEquals(listOf(200L, 201L), payload.tasks.map { it.taskId })
    }

    @Test
    fun `una subtarea va con su raiz aunque su propia asignatura no coincida`() {
        val odd = tasks + Task(taskId = 202, title = "Rara", subjectId = 1, parentTaskId = 200)

        val payload = buildSharePayload(subjects, events, odd, ShareSelection(generalTasks = true))

        assertTrue(payload.tasks.any { it.taskId == 202L })
    }

    @Test
    fun `no se comparten notas ni progreso personal`() {
        val payload = build(ShareSelection(subjectsAndEvents = true, subjectTasks = true, generalTasks = true))

        assertTrue(payload.events.all { it.score == null && !it.noPuntuableDone })
        assertTrue(payload.events.first().isPuntuable)
        assertEquals(40f, payload.events.first().weight)
        assertTrue(payload.tasks.all { !it.isCompleted && it.completedAt == null })
    }

    @Test
    fun `las tareas archivadas no se envian`() {
        val payload = build(ShareSelection(subjectsAndEvents = true, subjectTasks = true, generalTasks = true))

        assertNull(payload.tasks.find { it.taskId == 300L })
    }

    @Test
    fun `una seleccion vacia o solo de horario no envia datos`() {
        assertTrue(ShareSelection().isEmpty)
        val payload = build(ShareSelection(schedule = true))

        assertEquals(DataToSend(), payload)
    }

    @Test
    fun `un ciclo de madres no cuelga la clasificacion`() {
        val cycle = listOf(
            Task(taskId = 1, title = "a", parentTaskId = 2),
            Task(taskId = 2, title = "b", parentTaskId = 1)
        )

        val payload = buildSharePayload(emptyList(), emptyList(), cycle, ShareSelection(generalTasks = true))

        assertEquals(2, payload.tasks.size)
    }

    @Test
    fun `el resumen cuenta cada tipo`() {
        val summary = build(ShareSelection(subjectsAndEvents = true, subjectTasks = true, generalTasks = true)).summary(1234)

        assertEquals(PayloadSummary(subjects = 2, events = 2, subjectTasks = 2, generalTasks = 2, scheduleBytes = 1234), summary)
    }
}
