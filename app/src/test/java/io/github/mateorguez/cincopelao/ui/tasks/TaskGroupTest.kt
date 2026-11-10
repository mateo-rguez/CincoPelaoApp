package io.github.mateorguez.cincopelao.ui.tasks

import io.github.mateorguez.cincopelao.data.local.entity.Task
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskGroupTest {

    @Test
    fun `cuenta las tareas pendientes sin contar subtareas ni completadas`() {
        val tasks = listOf(
            Task(taskId = 1, title = "Suelta"),
            Task(taskId = 2, title = "Hecha", isCompleted = true),
            Task(taskId = 3, title = "Con subtareas"),
            Task(taskId = 4, title = "Subtarea pendiente", parentTaskId = 3),
            Task(taskId = 5, title = "Subtarea hecha", parentTaskId = 3, isCompleted = true),
        )
        assertEquals(2, TaskGroup(subject = null, tasks = tasks).pendingCount)
        assertEquals(0, TaskGroup(subject = null, tasks = emptyList()).pendingCount)
    }

    @Test
    fun `titulo del bloque con la abreviatura si el nombre no cabe`() {
        assertEquals("Señales y Sistemas", groupTitle("Señales y Sistemas", "SyS", nameFits = true))
        assertEquals("SyS", groupTitle("Señales y Sistemas", "SyS", nameFits = false))
        // Sin abreviatura se queda el nombre (con puntos suspensivos)
        assertEquals("Señales y Sistemas", groupTitle("Señales y Sistemas", " ", nameFits = false))
    }
}
