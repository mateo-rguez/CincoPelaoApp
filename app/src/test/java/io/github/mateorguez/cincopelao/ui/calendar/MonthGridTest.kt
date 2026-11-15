package io.github.mateorguez.cincopelao.ui.calendar

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

class MonthGridTest {

    @Test
    fun `siempre seis semanas de lunes a domingo`() {
        // Noviembre de 2026 empieza en domingo y acaba en lunes: necesita las 6
        val november = monthGridDays(YearMonth.of(2026, 11), DayOfWeek.MONDAY)
        assertEquals(42, november.size)
        assertEquals(LocalDate.of(2026, 10, 26), november.first())
        assertEquals(LocalDate.of(2026, 12, 6), november.last())

        // Diciembre de 2026 ocupa 5: la de relleno va al final
        val december = monthGridDays(YearMonth.of(2026, 12), DayOfWeek.MONDAY)
        assertEquals(LocalDate.of(2026, 11, 30), december.first())
        assertEquals(LocalDate.of(2027, 1, 10), december.last())

        // Febrero de 2027 cabe en 4 semanas justas: una de relleno antes y otra después
        val february = monthGridDays(YearMonth.of(2027, 2), DayOfWeek.MONDAY)
        assertEquals(42, february.size)
        assertEquals(LocalDate.of(2027, 1, 25), february.first())
        assertEquals(LocalDate.of(2027, 3, 7), february.last())
    }

    @Test
    fun `la semana empieza segun el idioma`() {
        assertEquals(DayOfWeek.MONDAY, firstDayOfWeek(Locale.forLanguageTag("es-ES")))
        assertEquals(DayOfWeek.SUNDAY, weekDays(DayOfWeek.MONDAY).last())
        assertEquals(LocalDate.of(2026, 11, 1), monthGridDays(YearMonth.of(2026, 11), DayOfWeek.SUNDAY).first())
    }

    @Test
    fun `cada pagina es un mes`() {
        val now = YearMonth.of(2026, 11)
        assertEquals(now, monthForPage(CurrentMonthPage, now))
        assertEquals(YearMonth.of(2027, 1), monthForPage(CurrentMonthPage + 2, now))
        assertEquals(YearMonth.of(2025, 10), monthForPage(CurrentMonthPage - 13, now))
    }
}
