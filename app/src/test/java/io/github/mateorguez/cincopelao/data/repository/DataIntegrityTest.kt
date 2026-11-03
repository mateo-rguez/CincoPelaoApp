package io.github.mateorguez.cincopelao.data.repository

import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.local.entity.Task
import io.github.mateorguez.cincopelao.sharing.protocol.DataToSend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataIntegrityTest {

    private fun task(id: Long, parent: Long? = null, subject: Long? = null) =
        Task(taskId = id, title = "t$id", parentTaskId = parent, subjectId = subject)

    /** Comprueba que ninguna tarea aparece antes que su madre. */
    private fun assertParentsBeforeChildren(tasks: List<Task>) {
        val seen = HashSet<Long>()
        tasks.forEach { t ->
            t.parentTaskId?.let { assertTrue("La madre $it debe ir antes que ${t.taskId}", it in seen) }
            seen += t.taskId
        }
    }

    @Test
    fun `las madres van antes que sus subtareas aunque vengan desordenadas`() {
        val ordered = listOf(task(3, parent = 2), task(2, parent = 1), task(1), task(4, parent = 1)).parentsFirst()

        assertEquals(4, ordered.size)
        assertParentsBeforeChildren(ordered)
    }

    @Test
    fun `una subtarea cuya madre no esta en la lista se trata como raiz`() {
        val ordered = listOf(task(2, parent = 99)).parentsFirst()

        assertNull(ordered.single().parentTaskId)
    }

    @Test
    fun `un ciclo se rompe en lugar de bloquearse`() {
        val ordered = listOf(task(1, parent = 2), task(2, parent = 1)).parentsFirst()

        assertEquals(2, ordered.size)
        assertParentsBeforeChildren(ordered)
    }

    @Test
    fun `tareas de asignaturas que no vienen pasan a ser generales`() {
        val data = DataToSend(tasks = listOf(task(1, subject = 7)))

        assertNull(data.withoutDanglingReferences().tasks.single().subjectId)
    }

    @Test
    fun `se conservan las referencias que si vienen en el paquete`() {
        val data = DataToSend(
            subjects = listOf(Subject(subjectId = 7, name = "Cálculo")),
            events = listOf(Event(subjectId = 7, name = "Parcial", type = "Examen", date = 0L)),
            tasks = listOf(task(1, subject = 7), task(2, parent = 1, subject = 7))
        )

        val clean = data.withoutDanglingReferences()

        assertEquals(data, clean)
    }

    @Test
    fun `eventos sin su asignatura se descartan y subtareas sin madre se independizan`() {
        val data = DataToSend(
            events = listOf(Event(subjectId = 7, name = "Parcial", type = "Examen", date = 0L)),
            tasks = listOf(task(2, parent = 1))
        )

        val clean = data.withoutDanglingReferences()

        assertTrue(clean.events.isEmpty())
        assertNull(clean.tasks.single().parentTaskId)
    }
}
