package io.github.mateorguez.cincopelao.ui.tasks

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class TaskDatesTest {

    private lateinit var previousZone: TimeZone
    private lateinit var previousLocale: Locale

    @Before
    fun setUp() {
        previousZone = TimeZone.getDefault()
        previousLocale = Locale.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Madrid"))
        Locale.setDefault(Locale.forLanguageTag("es-ES"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(previousZone)
        Locale.setDefault(previousLocale)
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int = 12) =
        Calendar.getInstance().apply { clear(); set(year, month - 1, day, hour, 0) }.timeInMillis

    private val now = at(2026, 11, 10, 18)

    @Test
    fun `dias naturales aunque falten menos de 24 horas`() {
        assertEquals(1, daysUntil(at(2026, 11, 11, 9), now))
        assertEquals(0, daysUntil(at(2026, 11, 10, 23), now))
        assertEquals(-1, daysUntil(at(2026, 11, 9, 23), now))
    }

    @Test
    fun `cuenta bien a traves del cambio de hora`() {
        // El 25/10/2026 tiene 25 horas en España
        assertEquals(2, daysUntil(at(2026, 10, 26), at(2026, 10, 24)))
    }

    @Test
    fun `texto de la fecha limite`() {
        assertEquals("Hoy", dueDateLabel(at(2026, 11, 10, 20), now))
        assertEquals("Mañana", dueDateLabel(at(2026, 11, 11), now))
        assertEquals("En 5 días", dueDateLabel(at(2026, 11, 15), now))
        assertEquals("Venció ayer", dueDateLabel(at(2026, 11, 9), now))
        assertEquals("Venció hace 2 días", dueDateLabel(at(2026, 11, 8), now))
    }

    @Test
    fun `urgencia de la fecha limite`() {
        assertEquals(DueUrgency.OVERDUE, dueUrgency(at(2026, 11, 9, 23), now))
        assertEquals(DueUrgency.SOON, dueUrgency(at(2026, 11, 10, 8), now)) // hoy, aunque la hora ya pasó
        assertEquals(DueUrgency.SOON, dueUrgency(at(2026, 11, 11), now))
        assertEquals(DueUrgency.LATER, dueUrgency(at(2026, 11, 12), now))
    }

    @Test
    fun `texto de cuando se completo`() {
        assertEquals("hoy", completedLabel(at(2026, 11, 10, 9), now))
        assertEquals("hace 1 día", completedLabel(at(2026, 11, 9, 23), now))
        assertEquals("hace 3 días", completedLabel(at(2026, 11, 7), now))
        assertEquals("fecha desconocida", completedLabel(null, now))
    }

    @Test
    fun `fechas antiguas se muestran como fecha y no como el objeto formateador`() {
        val label = completedLabel(at(2026, 10, 5), now)
        assertEquals("el 5 oct", label)
        assertFalse(label.contains("SimpleDateFormat"))
    }
}
