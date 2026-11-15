package io.github.mateorguez.cincopelao.ui.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

// Lógica del calendario mensual propio: qué días se pintan y qué mes corresponde a cada página.

/** Páginas del calendario: 500 meses hacia atrás y 500 hacia delante del actual. */
const val MonthPageCount = 1001
const val CurrentMonthPage = MonthPageCount / 2

fun monthForPage(page: Int, current: YearMonth): YearMonth =
    current.plusMonths((page - CurrentMonthPage).toLong())

/** Primer día de la semana según el idioma (lunes en España). */
fun firstDayOfWeek(locale: Locale = Locale.getDefault()): DayOfWeek = WeekFields.of(locale).firstDayOfWeek

/** Los siete días de la semana empezando por [first]. */
fun weekDays(first: DayOfWeek): List<DayOfWeek> = List(7) { first.plus(it.toLong()) }

/**
 * Días de la cuadrícula de [month]: siempre 6 semanas desde [first], con los días de los meses
 * vecinos que las completan. Así todos los meses miden lo mismo y el calendario no cambia de
 * alto al pasar de uno a otro. Las semanas de relleno se reparten: un mes de 5 semanas añade
 * una al final y uno de 4 (un febrero que empieza en lunes), una antes y otra después.
 */
fun monthGridDays(month: YearMonth, first: DayOfWeek): List<LocalDate> {
    val firstWeek = month.atDay(1).with(TemporalAdjusters.previousOrSame(first))
    val lastWeekEnd = month.atEndOfMonth().with(TemporalAdjusters.nextOrSame(first.minus(1)))
    val weeks = (ChronoUnit.DAYS.between(firstWeek, lastWeekEnd) + 1) / 7
    val start = firstWeek.minusWeeks((6 - weeks) / 2)
    return List(42) { start.plusDays(it.toLong()) }
}
