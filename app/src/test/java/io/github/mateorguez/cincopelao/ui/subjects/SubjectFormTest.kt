package io.github.mateorguez.cincopelao.ui.subjects

import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubjectFormTest {

    @Test
    fun `la abreviatura sugerida usa las iniciales sin palabras vacías ni tildes`() {
        assertEquals("PSI", suggestAbbrev("Programación de Sistemas Inteligentes"))
        assertEquals("RIM", suggestAbbrev("Redes Inalámbricas y Móviles"))
        assertEquals("SM", suggestAbbrev("Servicios Multimedia"))
        assertEquals("FIS", suggestAbbrev("Física"))
        assertEquals("", suggestAbbrev("  "))
        // Muchas palabras: se corta en 6 letras
        assertEquals("UDTCCS", suggestAbbrev("Uno Dos Tres Cuatro Cinco Seis Siete"))
    }

    @Test
    fun `los números se muestran con coma y sin decimales basura`() {
        assertEquals("8,38", formatNumber(8.379999f))
        assertEquals("40", formatNumber(40f))
        assertEquals("4,5", formatNumber(4.5f))
        assertEquals("0", formatNumber(0f))
        assertEquals("4.5", inputNumber(4.5f))
        assertEquals(4.5f, parseNumber("4,5"))
        assertNull(parseNumber("abc"))
    }

    @Test
    fun `estado de la suma de ponderaciones`() {
        val events = listOf(event(60f), event(30f), event(null))
        assertEquals(90f, weightTotal(events))
        assertEquals(WeightStatus.Under, weightStatus(90f))
        assertEquals(WeightStatus.Complete, weightStatus(99f))
        assertEquals(WeightStatus.Complete, weightStatus(100f))
        assertEquals(WeightStatus.Over, weightStatus(105f))
    }

    @Test
    fun `campos que faltan en la asignatura`() {
        val complete = Subject(name = "Cálculo", course = "1º", abbrev = "CAL")
        assertTrue(missingSubjectFields(complete, listOf(event(100f))).isEmpty())
        assertEquals(
            setOf(SubjectField.Name, SubjectField.Course, SubjectField.Abbrev, SubjectField.Events),
            missingSubjectFields(Subject(), emptyList())
        )
    }

    @Test
    fun `validación del evento`() {
        val day = 1L
        assertTrue(invalidEventFields("Cuest. 1", "Examen", day, true, "10", "").isEmpty())
        assertTrue(invalidEventFields("Cuest. 1", "Examen", day, true, "10", "4,5").isEmpty())
        // Sin nota no se miran ponderación ni mínima
        assertTrue(invalidEventFields("Foro", "Ejercicio", day, false, "", "x").isEmpty())
        assertEquals(
            setOf(EventField.Name, EventField.Type, EventField.Date, EventField.Weight),
            invalidEventFields(" ", "", null, true, "", "")
        )
        assertEquals(setOf(EventField.Weight), invalidEventFields("a", "Examen", day, true, "0", ""))
        assertEquals(setOf(EventField.Weight), invalidEventFields("a", "Examen", day, true, "101", ""))
        assertEquals(setOf(EventField.MinScore), invalidEventFields("a", "Examen", day, true, "10", "11"))
    }

    @Test
    fun `línea de resumen del evento`() {
        val exam = Event(
            subjectId = 0, name = "Cuest. 2", type = "Examen", modal = "Teoría",
            isPuntuable = true, weight = 10f, score = 8.379999f, minScore = 40f, date = 1L
        )
        assertEquals("Examen · Teoría · 27 oct 2026 · mín. 4 · nota 8,38", eventSummaryLine(exam, "27 oct 2026"))
        val forum = Event(subjectId = 0, name = "Foro", type = "Ejercicio", date = 1L)
        assertEquals("Ejercicio · 8 ene", eventSummaryLine(forum, "8 ene"))
    }

    @Test
    fun `fecha corta sin año si es de este año`() {
        val es = java.util.Locale.forLanguageTag("es-ES")
        fun at(y: Int, m: Int, d: Int) = java.util.Calendar.getInstance().apply { set(y, m, d, 12, 0) }.timeInMillis
        val now = at(2026, java.util.Calendar.NOVEMBER, 17)
        assertEquals("27 oct", shortEventDate(at(2026, java.util.Calendar.OCTOBER, 27), now, es))
        assertEquals("8 ene 2027", shortEventDate(at(2027, java.util.Calendar.JANUARY, 8), now, es))
        assertEquals("Sin fecha", shortEventDate(0L, now, es))
    }

    private fun event(weight: Float?) =
        Event(subjectId = 0, name = "e", type = "Examen", isPuntuable = weight != null, weight = weight, date = 1L)
}
