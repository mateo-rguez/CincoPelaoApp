package io.github.mateorguez.cincopelao.notifications

import java.util.Calendar
import java.util.TimeZone
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

/**
 * Tests de [ReminderScheduler.computeNotifyTime] y [ReminderScheduler.reminderMessageFor].
 *
 * La hora del aviso es aleatoria, así que cada test se repite con muchas semillas
 * y comprueba que el resultado cae siempre dentro de la ventana esperada.
 */
class ReminderSchedulerTest {

    private val madrid: TimeZone = TimeZone.getTimeZone("Europe/Madrid")
    private val seeds = 0 until 300

    /** Instante en hora local de Madrid. */
    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance(madrid).apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }.timeInMillis

    private fun compute(target: Long, offsetDays: Int, now: Long, seed: Int, start: Int = 8, end: Int = 22) =
        ReminderScheduler.computeNotifyTime(
            targetTime = target,
            offsetDays = offsetDays,
            startHour = start,
            endHour = end,
            now = now,
            random = Random(seed),
            timeZone = madrid
        )

    @Test
    fun `el aviso cae en la ventana horaria del dia correspondiente`() {
        val now = at(2026, 11, 10, 12)
        val examen = at(2026, 11, 20, 9)

        for (seed in seeds) {
            val result = compute(examen, offsetDays = 3, now = now, seed = seed)
            assertNotNull(result)
            // 3 días antes del examen: el 17, entre las 8:00 y las 22:00
            assertTrue(result!! >= at(2026, 11, 17, 8))
            assertTrue(result < at(2026, 11, 17, 22))
        }
    }

    @Test
    fun `no se programa nada si el dia del aviso ya paso`() {
        val now = at(2026, 11, 10, 12)
        val examenManana = at(2026, 11, 11, 9)

        for (seed in seeds) {
            assertNull(compute(examenManana, offsetDays = 7, now = now, seed = seed))
        }
    }

    @Test
    fun `si el aviso es hoy y ya paso la ventana horaria no se programa`() {
        val now = at(2026, 11, 10, 23)
        val examenManana = at(2026, 11, 11, 9)

        for (seed in seeds) {
            assertNull(compute(examenManana, offsetDays = 1, now = now, seed = seed))
        }
    }

    @Test
    fun `un aviso programado nunca queda en el pasado`() {
        val now = at(2026, 11, 10, 15)
        val examenManana = at(2026, 11, 11, 9)

        for (seed in seeds) {
            val result = compute(examenManana, offsetDays = 1, now = now, seed = seed)
            if (result != null) assertTrue(result >= now)
        }
    }

    @Ignore("BUG: al recalcular para hoy se usa la medianoche UTC; en España el aviso se sale de la ventana")
    @Test
    fun `si el aviso es hoy se recalcula dentro de la ventana local`() {
        val now = at(2026, 11, 10, 21, 30)
        val examenManana = at(2026, 11, 11, 9)

        for (seed in seeds) {
            val result = compute(examenManana, offsetDays = 1, now = now, seed = seed)
            assertNotNull(result)
            assertTrue(result!! < at(2026, 11, 10, 22))
        }
    }

    @Ignore("BUG: Random.nextInt(start, end) lanza IllegalArgumentException si start >= end")
    @Test
    fun `una ventana horaria vacia no provoca un crash`() {
        compute(at(2026, 11, 20, 9), offsetDays = 3, now = at(2026, 11, 10, 12), seed = 0, start = 10, end = 10)
    }

    @Test
    fun `el mensaje depende de los dias que faltan`() {
        assertEquals("Es mañana, ¿Preparado? 💪", ReminderScheduler.reminderMessageFor(1))
        assertEquals("Quedan 3 unidades de día, tocó cookear 🐐", ReminderScheduler.reminderMessageFor(3))
        assertEquals("Faltan 5 días, habrá que ir espabilando 🙄", ReminderScheduler.reminderMessageFor(5))
        assertEquals("Aún quedan 7 días, hay tiempo 🥱", ReminderScheduler.reminderMessageFor(7))
    }
}
