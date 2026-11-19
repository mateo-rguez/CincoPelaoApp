package io.github.mateorguez.cincopelao.ui.tasks

import io.github.mateorguez.cincopelao.data.local.entity.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskFormTest {

    private val madre = Task(taskId = 1, title = "Memoria")
    private val hija = Task(taskId = 2, title = "Índice", parentTaskId = 1)
    private val suelta = Task(taskId = 3, title = "Suelta")
    private val archivada = Task(taskId = 4, title = "Vieja", archived = true)
    private val todas = listOf(madre, hija, suelta, archivada)

    @Test
    fun `una tarea nueva puede colgar de cualquier tarea de primer nivel sin archivar`() {
        assertEquals(listOf(madre, suelta), parentOptions(null, todas))
    }

    @Test
    fun `una tarea no puede ser madre de si misma`() {
        assertEquals(listOf(madre), parentOptions(suelta, todas))
        assertEquals(listOf(madre, suelta), parentOptions(hija, todas))
    }

    @Test
    fun `una tarea con subtareas no puede pasar a ser subtarea`() {
        assertTrue(parentOptions(madre, todas).isEmpty())
    }

    @Test
    fun `titulo del formulario`() {
        assertEquals("Nueva tarea general", taskFormTitle(isNew = true, subjectAbbrev = null))
        assertEquals("Nueva tarea · PSI", taskFormTitle(isNew = true, subjectAbbrev = "PSI"))
        assertEquals("Editar tarea · PSI", taskFormTitle(isNew = false, subjectAbbrev = "PSI"))
    }

    @Test
    fun `los nombres largos se recortan con puntos suspensivos`() {
        assertEquals("corto", ellipsize("corto", 10))
        assertEquals("Meter a l…", ellipsize("Meter a los demás en gh", 10))
        assertEquals(10, ellipsize("Meter a los demás en gh", 10).length)
    }
}
