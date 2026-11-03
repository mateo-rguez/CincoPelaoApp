package io.github.mateorguez.cincopelao.data.repository

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.mateorguez.cincopelao.data.instances.InstanceRepository
import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.local.entity.Task
import io.github.mateorguez.cincopelao.sharing.protocol.DataToSend
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests de los repositorios contra una base de datos Room real (en un Android simulado
 * por Robolectric), para comprobar transacciones, cascadas y claves foráneas.
 */
// Application simple: la app real arranca servicios (recordatorios, mantenimiento) que aquí no queremos
@Config(application = Application::class)
@RunWith(RobolectricTestRunner::class)
class RepositoriesTest {

    private lateinit var instances: InstanceRepository
    private lateinit var subjects: SubjectRepository
    private lateinit var events: EventRepository
    private lateinit var tasks: TaskRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        instances = InstanceRepository(context)
        subjects = SubjectRepository(instances)
        events = EventRepository(instances)
        tasks = TaskRepository(instances)
    }

    @After
    fun tearDown() {
        instances.currentDb().close()
    }

    private suspend fun allTasks() = tasks.tasks.first()
    private suspend fun task(title: String) = allTasks().single { it.title == title }

    private suspend fun createSubject(): Subject {
        subjects.saveWithEvents(
            isEditing = false,
            subject = Subject(name = "Cálculo", course = "1º", abbrev = "CAL"),
            events = listOf(Event(subjectId = 0, name = "Parcial", type = "Examen", date = 1L))
        )
        return subjects.subjects.first().single()
    }

    @Test
    fun `borrar una asignatura y deshacerlo restaura sus eventos y tareas`() = runBlocking {
        val subject = createSubject()
        tasks.insert(Task(title = "madre", subjectId = subject.subjectId.toLong()))
        tasks.insert(Task(title = "hija", subjectId = subject.subjectId.toLong(), parentTaskId = task("madre").taskId))

        val deleted = subjects.delete(subject)

        assertTrue(subjects.subjects.first().isEmpty())
        assertTrue(events.events.first().isEmpty())
        assertTrue(allTasks().isEmpty())

        subjects.restore(deleted)

        assertEquals(listOf(subject), subjects.subjects.first())
        assertEquals(1, events.events.first().size)
        assertEquals(task("madre").taskId, task("hija").parentTaskId)
    }

    @Test
    fun `completar todas las subtareas completa la madre y desmarcar una la descompleta`() = runBlocking {
        tasks.insert(Task(title = "madre"))
        val madre = task("madre").taskId
        tasks.insert(Task(title = "a", parentTaskId = madre))
        tasks.insert(Task(title = "b", parentTaskId = madre))

        tasks.update(task("a").copy(isCompleted = true))
        assertFalse(task("madre").isCompleted)

        tasks.update(task("b").copy(isCompleted = true))
        assertTrue(task("madre").isCompleted)

        tasks.update(task("a").copy(isCompleted = false))
        assertFalse(task("madre").isCompleted)
        assertNull(task("madre").completedAt)
    }

    @Test
    fun `borrar conservando subtareas las deja como tareas independientes`() = runBlocking {
        tasks.insert(Task(title = "madre"))
        tasks.insert(Task(title = "hija", parentTaskId = task("madre").taskId))

        tasks.deleteKeepingSubtasks(task("madre"))

        assertEquals(listOf("hija"), allTasks().map { it.title })
        assertNull(task("hija").parentTaskId)
    }

    @Test
    fun `borrar con subtareas y deshacerlo restaura toda la jerarquia`() = runBlocking {
        tasks.insert(Task(title = "madre"))
        tasks.insert(Task(title = "hija", parentTaskId = task("madre").taskId))
        tasks.insert(Task(title = "nieta", parentTaskId = task("hija").taskId))

        val deleted = tasks.deleteWithSubtasks(task("madre"))
        assertTrue(allTasks().isEmpty())

        tasks.restore(deleted.reversed()) // aunque lleguen desordenadas

        assertEquals(3, allTasks().size)
        assertEquals(task("hija").taskId, task("nieta").parentTaskId)
    }

    @Test
    fun `el mantenimiento archiva las tareas completadas antes de hoy`() = runBlocking {
        tasks.insert(Task(title = "vieja", isCompleted = true, completedAt = 1_000L))
        tasks.insert(Task(title = "nueva", isCompleted = true, completedAt = System.currentTimeMillis()))

        tasks.runMaintenance()

        assertTrue(task("vieja").archived)
        assertFalse(task("nueva").archived)
    }

    @Test
    fun `importar tareas sin sus asignaturas funciona y activa la nueva instancia`() = runBlocking {
        val data = DataToSend(
            tasks = listOf(
                Task(taskId = 6, title = "hija", parentTaskId = 5, subjectId = 9),
                Task(taskId = 5, title = "madre", subjectId = 9)
            )
        )

        val id = instances.importAsNewInstance("Set recibido", data)

        assertEquals(id, instances.currentInstanceId.value)
        assertEquals(2, allTasks().size)
        assertTrue(allTasks().all { it.subjectId == null })
        assertEquals(5L, task("hija").parentTaskId)
    }

    @Test
    fun `editar una instancia inexistente o borrar la activa no hace nada`() = runBlocking {
        val before = instances.instances.value

        instances.update("no-existe", "x", null)
        instances.delete(instances.currentInstanceId.value)

        assertEquals(before, instances.instances.value)
    }
}
