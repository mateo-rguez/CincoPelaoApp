package io.github.mateorguez.cincopelao.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Reconciliación de recordatorios: qué alarmas poner y quitar y qué horas se conservan. */
class ReminderSyncTest {

    private val day = 24L * 60 * 60 * 1000
    private val now = 1_800_000_000_000L // una fecha cualquiera

    /** Recordatorio de un elemento dentro de [inDays] días, avisado [offset] días antes, de 0 a 24 h. */
    private fun planned(key: String, inDays: Int = 10, offset: Int = 7, title: String = "Aviso") =
        PlannedReminder(key, now + inDays * day, offset, 0, 24, title, "mensaje")

    private fun reconcile(planned: List<PlannedReminder>, stored: List<ScheduledReminder> = emptyList()) =
        ReminderSync.reconcile(planned, stored.associateBy { it.key }, now, Random(42))

    @Test
    fun `los recordatorios nuevos se sortean, se guardan y se ponen`() {
        val result = reconcile(listOf(planned("a"), planned("b")))

        assertEquals(2, result.drawn)
        assertEquals(setOf("a", "b"), result.scheduled.map { it.key }.toSet())
        assertEquals(result.scheduled.associate { it.key to it.time }, result.toArm.associate { it.reminder.key to it.time })
        assertTrue(result.toArm.all { it.time > now })
        assertTrue(result.toCancel.isEmpty())
    }

    @Test
    fun `si no cambia, conserva su hora y vuelve a poner la alarma`() {
        val reminder = planned("a")
        val stored = ScheduledReminder("a", reminder.spec, now + 3 * day + 12_345)

        val result = reconcile(listOf(reminder), listOf(stored))

        assertEquals(0, result.drawn)
        assertEquals(listOf(stored), result.scheduled)
        assertEquals(listOf(ReminderAlarm(reminder, stored.time)), result.toArm)
    }

    @Test
    fun `si cambia (otra fecha, otro texto), se vuelve a sortear`() {
        val old = planned("a")
        val stored = ScheduledReminder("a", old.spec, now + 3 * day)
        val changed = planned("a", title = "Otro título")

        val result = reconcile(listOf(changed), listOf(stored))

        assertEquals(1, result.drawn)
        assertEquals(changed.spec, result.scheduled.single().spec)
    }

    @Test
    fun `el que ya salto no se vuelve a poner, pero se recuerda`() {
        val reminder = planned("a")
        val shown = ScheduledReminder("a", reminder.spec, now - 60_000)

        val result = reconcile(listOf(reminder), listOf(shown))

        assertEquals(listOf(shown), result.scheduled)
        assertTrue(result.toArm.isEmpty())
        assertEquals(0, result.drawn)
    }

    @Test
    fun `se quitan los que ya no tocan`() {
        val gone = ScheduledReminder("borrado", "x", now + day)

        val result = reconcile(listOf(planned("a")), listOf(gone))

        assertEquals(listOf("borrado"), result.toCancel)
        assertTrue(result.scheduled.none { it.key == "borrado" })
    }

    @Test
    fun `si su franja ya termino, no se pone y se quita la que hubiera`() {
        // Aviso 7 días antes de algo que es dentro de 2 días: ese día ya pasó
        val late = planned("a", inDays = 2, offset = 7)
        val stored = ScheduledReminder("a", "spec antigua", now + day)

        val result = reconcile(listOf(late), listOf(stored))

        assertEquals(listOf("a"), result.toCancel)
        assertTrue(result.scheduled.isEmpty())
        assertTrue(result.toArm.isEmpty())
    }

    @Test
    fun `se guarda y se lee igual, con claves que llevan dos puntos`() {
        val reminder = ScheduledReminder("a65e2cf5-4d5f:event:4:7", "-12345", now)
        assertEquals(reminder, ScheduledReminder.decode(reminder.encode()))
        assertNull(ScheduledReminder.decode("basura"))
        assertNull(ScheduledReminder.decode("a\tb\tno-es-un-numero"))
    }
}
