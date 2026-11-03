package io.github.mateorguez.cincopelao.data.repository

import io.github.mateorguez.cincopelao.data.local.entity.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskRulesTest {

    private fun task(id: Long, parent: Long? = null, done: Boolean = false, completedAt: Long? = null, archived: Boolean = false) =
        Task(taskId = id, title = "t$id", parentTaskId = parent, isCompleted = done, completedAt = completedAt, archived = archived)

    @Test
    fun `la madre se completa cuando todas sus subtareas lo estan`() {
        val result = reconcileWithSubtasks(task(1), listOf(task(2, 1, done = true), task(3, 1, done = true)), now = 500)

        assertTrue(result!!.isCompleted)
        assertEquals(500L, result.completedAt)
    }

    @Test
    fun `la madre se descompleta si alguna subtarea no esta completada`() {
        val result = reconcileWithSubtasks(task(1, done = true, completedAt = 100), listOf(task(2, 1, done = true), task(3, 1)), now = 500)

        assertFalse(result!!.isCompleted)
        assertNull(result.completedAt)
    }

    @Test
    fun `si ya es coherente o no tiene subtareas no se cambia`() {
        assertNull(reconcileWithSubtasks(task(1), listOf(task(2, 1)), now = 500))
        assertNull(reconcileWithSubtasks(task(1, done = true), emptyList(), now = 500))
    }

    @Test
    fun `se archivan solo las completadas antes de hoy y no archivadas`() {
        val today = 1_000L
        val tasks = listOf(
            task(1, done = true, completedAt = 999),               // ayer -> se archiva
            task(2, done = true, completedAt = 1_000),             // hoy -> no
            task(3, done = true, completedAt = 10, archived = true), // ya archivada -> no
            task(4, done = false)                                  // pendiente -> no
        )

        assertEquals(listOf(1L), tasksToArchive(tasks, today).map { it.taskId })
    }

    @Test
    fun `descendientes a cualquier profundidad`() {
        val tasks = listOf(task(1), task(2, 1), task(3, 2), task(4), task(5, 4))

        assertEquals(setOf(1L, 2L, 3L), tasks.withDescendantsOf(setOf(1L)).map { it.taskId }.toSet())
    }
}
