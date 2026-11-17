package io.github.mateorguez.cincopelao.ui.subjects

import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/*
 * Reglas puras del formulario de asignatura y del de evento (con tests en SubjectFormTest).
 */

/** Cursos que se pueden elegir. */
val SUBJECT_COURSES = listOf("1º", "2º", "3º", "4º", "5º", "6º")

/** Longitud máxima de la abreviatura y del nombre de un evento. */
const val MAX_ABBREV_LENGTH = 6
const val MAX_EVENT_NAME_LENGTH = 10

/** Palabras que no cuentan para la abreviatura («Programación de Sistemas» → «PS»). */
private val ABBREV_STOPWORDS = setOf("de", "del", "la", "las", "el", "los", "y", "e", "en", "a", "al", "con", "para", "por")

/**
 * Abreviatura sugerida a partir del nombre: iniciales de las palabras con significado, sin
 * tildes y en mayúsculas («Redes Inalámbricas y Móviles» → «RIM»); con una sola palabra, sus
 * tres primeras letras («Física» → «FIS»). Vacía si no hay nombre.
 */
fun suggestAbbrev(name: String): String {
    val words = Normalizer.normalize(name, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { it.isNotBlank() }
    val significant = words.filter { it.lowercase() !in ABBREV_STOPWORDS }.ifEmpty { words }
    val abbrev = when (significant.size) {
        0 -> ""
        1 -> significant[0].take(3)
        else -> significant.joinToString("") { it.take(1) }
    }
    return abbrev.uppercase().take(MAX_ABBREV_LENGTH)
}

/** Suma de las ponderaciones de los eventos puntuables. */
fun weightTotal(events: List<Event>): Float = events.mapNotNull { it.weight }.sum()

/** Cómo va la suma de ponderaciones respecto al 100 %. */
enum class WeightStatus { Under, Complete, Over }

fun weightStatus(total: Float): WeightStatus = when {
    total > 100.001f -> WeightStatus.Over
    total >= 99f -> WeightStatus.Complete
    else -> WeightStatus.Under
}

/** Campos del formulario de asignatura que pueden faltar. */
enum class SubjectField { Name, Course, Abbrev, Events }

/** Lo que falta para poder guardar la asignatura; vacío si está completa. */
fun missingSubjectFields(subject: Subject, events: List<Event>): Set<SubjectField> = buildSet {
    if (subject.name.isBlank()) add(SubjectField.Name)
    if (subject.course.isBlank()) add(SubjectField.Course)
    if (subject.abbrev.isBlank()) add(SubjectField.Abbrev)
    if (events.isEmpty()) add(SubjectField.Events)
}

/**
 * Número para mostrar: como mucho dos decimales, sin ceros de sobra y con coma
 * (40 → «40», 8.379999 → «8,38», 4.5 → «4,5»).
 */
fun formatNumber(value: Float): String = plainNumber(value).replace('.', ',')

/** Como [formatNumber] pero con punto, para rellenar un campo de texto que luego se lee con toFloat. */
fun inputNumber(value: Float): String = plainNumber(value)

private fun plainNumber(value: Float): String =
    BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

/** Lee un número escrito con coma o con punto; null si no lo es. */
fun parseNumber(text: String): Float? = text.trim().replace(',', '.').toFloatOrNull()

/** Campos del formulario de evento que pueden estar mal. */
enum class EventField { Name, Type, Date, Weight, MinScore }

/**
 * Comprueba el formulario de un evento: nombre, tipo y fecha siempre; si cuenta para la nota,
 * ponderación entera de 1 a 100 y nota mínima (opcional) de 0 a 10.
 */
fun invalidEventFields(
    name: String,
    type: String,
    dateMillis: Long?,
    isPuntuable: Boolean,
    weight: String,
    minScore: String,
): Set<EventField> = buildSet {
    if (name.isBlank()) add(EventField.Name)
    if (type.isBlank()) add(EventField.Type)
    if (dateMillis == null) add(EventField.Date)
    if (isPuntuable) {
        if (weight.trim().toIntOrNull()?.let { it in 1..100 } != true) add(EventField.Weight)
        if (minScore.isNotBlank() && parseNumber(minScore)?.let { it in 0f..10f } != true) add(EventField.MinScore)
    }
}

/** Una línea con lo que define a un evento: «Examen · Teoría · 24 nov · mín. 4 · nota 8,38». */
fun eventSummaryLine(event: Event, formattedDate: String): String = buildList {
    if (event.type.isNotBlank()) add(event.type)
    event.modal?.takeIf { it.isNotBlank() }?.let { add(it) }
    add(formattedDate)
    event.minScore?.takeIf { event.isPuntuable && it > 0f }?.let { add("mín. " + formatNumber(it / 10f)) }
    event.score?.let { add("nota " + formatNumber(it)) }
}.joinToString(" · ")

/**
 * Fecha corta de un evento: sin año si es del año de [now] («27 oct») y con él si no
 * («8 ene 2027»); «Sin fecha» si no tiene.
 */
fun shortEventDate(millis: Long, now: Long, locale: Locale = Locale.getDefault()): String {
    if (millis <= 0) return "Sin fecha"
    fun year(t: Long) = Calendar.getInstance().apply { timeInMillis = t }.get(Calendar.YEAR)
    val pattern = if (year(millis) == year(now)) "d MMM" else "d MMM yyyy"
    return SimpleDateFormat(pattern, locale).format(Date(millis))
}
