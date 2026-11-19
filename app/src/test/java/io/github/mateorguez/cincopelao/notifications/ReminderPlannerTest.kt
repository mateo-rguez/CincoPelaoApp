package io.github.mateorguez.cincopelao.notifications

import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.local.entity.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone
import kotlin.random.Random

class ReminderPlannerTest {

    private val madrid: TimeZone = TimeZone.getTimeZone("Europe/Madrid")
    private val seeds = 0 until 300

    /** Instante en hora local de Madrid. */
    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance(madrid).apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }.timeInMillis

    private fun compute(target: Long, offsetDays: Int, now: Long, seed: Int, start: Int = 8, end: Int = 22) =
        ReminderPlanner.computeNotifyTime(
            targetTime = target,
            offsetDays = offsetDays,
            startHour = start,
            endHour = end,
            now = now,
            random = Random(seed),
            timeZone = madrid
        )

    // --- Hora del aviso -------------------------------------------------------------------------

    @Test
    fun `el aviso cae en la ventana horaria del dia correspondiente`() {
        val now = at(2026, 11, 10, 12)
        val examen = at(2026, 11, 20, 9)

        for (seed in seeds) {
            val result = compute(examen, offsetDays = 3, now = now, seed = seed)
            assertNotNull(result)
            assertTrue(result!! >= at(2026, 11, 17, 8))
            assertTrue(result < at(2026, 11, 17, 22))
        }
    }

    @Test
    fun `no se programa nada si el dia del aviso ya paso`() {
        val now = at(2026, 11, 10, 12)
        for (seed in seeds) {
            assertNull(compute(at(2026, 11, 11, 9), offsetDays = 7, now = now, seed = seed))
        }
    }

    @Test
    fun `si el aviso es hoy y ya paso la ventana horaria no se programa`() {
        val now = at(2026, 11, 10, 23)
        for (seed in seeds) {
            assertNull(compute(at(2026, 11, 11, 9), offsetDays = 1, now = now, seed = seed))
        }
    }

    @Test
    fun `si el aviso es hoy cae entre ahora y el final de la ventana local`() {
        val now = at(2026, 11, 10, 21, 30)
        for (seed in seeds) {
            val result = compute(at(2026, 11, 11, 9), offsetDays = 1, now = now, seed = seed)
            assertNotNull(result)
            assertTrue(result!! >= now)
            assertTrue(result < at(2026, 11, 10, 22))
        }
    }

    @Test
    fun `una ventana vacia se trata como una hora desde el inicio`() {
        val now = at(2026, 11, 10, 12)
        for (seed in seeds) {
            val result = compute(at(2026, 11, 20, 9), offsetDays = 3, now = now, seed = seed, start = 10, end = 10)
            assertNotNull(result)
            assertTrue(result!! >= at(2026, 11, 17, 10))
            assertTrue(result < at(2026, 11, 17, 11))
        }
    }

    @Test
    fun `la ventana respeta la hora local en el cambio de hora`() {
        // 25/10/2026 es el día del cambio a horario de invierno en España (ese día tiene 25 horas)
        val now = at(2026, 10, 1, 12)
        for (seed in seeds) {
            val result = compute(at(2026, 10, 26, 9), offsetDays = 1, now = now, seed = seed)
            assertNotNull(result)
            assertTrue(result!! >= at(2026, 10, 25, 8))
            assertTrue(result < at(2026, 10, 25, 22))
        }
    }

    @Test
    fun `el mensaje sale de la lista de su tramo de dias con el numero puesto`() {
        val tramos = mapOf(
            1 to ReminderPhrases.messagesTomorrow,
            2 to ReminderPhrases.messagesSoon,
            3 to ReminderPhrases.messagesSoon,
            5 to ReminderPhrases.messagesThisWeek,
            7 to ReminderPhrases.messagesLater,
            30 to ReminderPhrases.messagesLater
        )
        for ((dias, lista) in tramos) {
            val posibles = lista.map { it.replace("{dias}", "$dias") }
            repeat(20) { i ->
                val mensaje = ReminderPlanner.reminderMessageFor(dias, "clave$i")
                assertTrue(mensaje, mensaje in posibles)
            }
        }
    }

    // --- Qué recordatorios tocan ----------------------------------------------------------------

    private val settings = ReminderSettings(daysBefore = listOf(7, 3, 1), startHour = 8, endHour = 22)
    private val now = at(2026, 11, 10, 12)
    private val future = at(2026, 11, 20, 9)
    private val subject = Subject(subjectId = 1, name = "Cálculo", abbrev = "CAL")

    private fun plan(events: List<Event> = emptyList(), tasks: List<Task> = emptyList()) =
        ReminderPlanner.plan("inst", listOf(subject), events, tasks, settings, settings, now)

    @Test
    fun `un recordatorio por cada dia de antelacion`() {
        val reminders = plan(events = listOf(Event(eventId = 5, subjectId = 1, name = "Parcial", type = "Examen", date = future)))

        assertEquals(listOf(7, 3, 1), reminders.map { it.offsetDays })
        assertTrue(reminders.all { "CAL" in it.title || "Parcial" in it.title })
    }

    @Test
    fun `eventos y tareas con el mismo id no comparten clave`() {
        val reminders = plan(
            events = listOf(Event(eventId = 5, subjectId = 1, name = "Parcial", type = "Examen", date = future)),
            tasks = listOf(Task(taskId = 5, title = "Entrega", dueDate = future))
        )

        assertEquals(reminders.size, reminders.map { it.key }.toSet().size)
    }

    @Test
    fun `no hay recordatorios de tareas completadas, archivadas, sin fecha o pasadas ni de eventos pasados`() {
        val reminders = plan(
            events = listOf(Event(eventId = 1, subjectId = 1, name = "Viejo", type = "Examen", date = now - 1)),
            tasks = listOf(
                Task(taskId = 1, title = "hecha", dueDate = future, isCompleted = true),
                Task(taskId = 2, title = "archivada", dueDate = future, archived = true),
                Task(taskId = 3, title = "sin fecha"),
                Task(taskId = 4, title = "pasada", dueDate = now - 1)
            )
        )

        assertTrue(reminders.isEmpty())
    }

    @Test
    fun `el mismo dato produce siempre el mismo recordatorio y cambiar la fecha cambia su spec`() {
        val task = Task(taskId = 9, title = "Entrega", dueDate = future)

        val first = plan(tasks = listOf(task))
        assertEquals(first, plan(tasks = listOf(task)))

        val moved = plan(tasks = listOf(task.copy(dueDate = future + 86_400_000)))
        assertEquals(first.map { it.key }, moved.map { it.key })
        assertNotEquals(first.map { it.spec }, moved.map { it.spec })
    }

    // --- Frases ---------------------------------------------------------------------------------

    private fun titles(reminders: List<PlannedReminder>) = reminders.map { it.title }

    @Test
    fun `una tarea sin asignatura no usa frases con asignatura`() {
        val tasks = (1L..40L).map { Task(taskId = it, title = "Entregar la memoria", dueDate = future) }
        val result = titles(plan(tasks = tasks))

        assertTrue(result.none { "{" in it || "asignatura" in it || "null" in it })
        assertTrue(result.all { "entregar la memoria" in it })
    }

    @Test
    fun `los nombres no se sustituyen dentro de si mismos`() {
        val events = (1L..40L).map { Event(eventId = it, subjectId = 1, name = "Prototipo {asig}", type = "Proyecto", date = future) }
        val result = titles(plan(events = events))

        assertTrue(result.none { "Protoproyecto" in it || "Protoel" in it })
        assertTrue(result.filter { "Proto" in it }.all { "Prototipo {asig}" in it })
    }

    @Test
    fun `el tipo lleva su articulo y uno desconocido no se usa`() {
        assertEquals("la presentación", ReminderPhrases.typeWithArticle("Presentación"))
        assertEquals("el examen", ReminderPhrases.typeWithArticle("Examen"))
        assertNull(ReminderPhrases.typeWithArticle("Cosa rara"))

        val raros = (1L..40L).map { Event(eventId = it, subjectId = 1, name = "Algo", type = "Cosa rara", date = future) }
        assertTrue(titles(plan(events = raros)).none { "cosa rara" in it || "{" in it })
    }

    @Test
    fun `la primera letra pasa a minuscula salvo en siglas`() {
        assertEquals("entregar práctica", ReminderPhrases.lowerFirst("Entregar práctica"))
        assertEquals("TFG: capítulo 2", ReminderPhrases.lowerFirst("TFG: capítulo 2"))
        assertEquals("", ReminderPhrases.lowerFirst("  "))
    }

    @Test
    fun `cada lista tiene frases que sirven sin asignatura ni tipo`() {
        val basicos = setOf("nombre", "dias")
        val listas = listOf(
            ReminderPhrases.eventTitles, ReminderPhrases.taskTitles, ReminderPhrases.messagesTomorrow,
            ReminderPhrases.messagesSoon, ReminderPhrases.messagesThisWeek, ReminderPhrases.messagesLater
        )
        for (lista in listas) {
            assertTrue(lista.any { basicos.containsAll(ReminderPhrases.placeholdersOf(it)) })
        }
    }

    @Test
    fun `las frases solo usan huecos conocidos`() {
        val conocidos = setOf("nombre", "asig", "el_tipo", "dias")
        val todas = ReminderPhrases.eventTitles + ReminderPhrases.taskTitles + ReminderPhrases.messagesTomorrow +
            ReminderPhrases.messagesSoon + ReminderPhrases.messagesThisWeek + ReminderPhrases.messagesLater
        for (frase in todas) assertTrue(frase, conocidos.containsAll(ReminderPhrases.placeholdersOf(frase)))
        for (frase in ReminderPhrases.taskTitles + ReminderPhrases.eventTitles) {
            assertTrue(frase, "{dias}" !in frase)
        }
    }

    // --- Botón «hecha» --------------------------------------------------------------------------

    @Test
    fun `la clave de un aviso de tarea lleva a su tarea y la de un evento no`() {
        val reminders = plan(
            events = listOf(Event(eventId = 5, subjectId = 1, name = "Parcial", type = "Examen", date = future)),
            tasks = listOf(Task(taskId = 7, title = "Entrega", dueDate = future))
        )
        val (tareas, eventos) = reminders.partition { ":task:" in it.key }

        assertTrue(tareas.all { ReminderPlanner.taskOf(it.key) == ReminderTaskRef("inst", 7) })
        assertTrue(eventos.all { ReminderPlanner.taskOf(it.key) == null })
        assertNull(ReminderPlanner.taskOf("basura"))
        assertNull(ReminderPlanner.taskOf("inst:task:x:1"))
        assertEquals(ReminderTaskRef("cinco_pelao_db", 3), ReminderPlanner.taskOf("cinco_pelao_db:task:3:1"))
    }

    @Test
    fun `los textos del boton no tienen huecos`() {
        assertTrue(ReminderPhrases.doneActions.all { ReminderPhrases.placeholdersOf(it).isEmpty() })
        assertTrue(ReminderPhrases.doneActions.all { it.length <= 20 })
    }
}
