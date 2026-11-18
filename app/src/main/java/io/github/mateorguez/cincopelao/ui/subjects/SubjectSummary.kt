package io.github.mateorguez.cincopelao.ui.subjects

import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.local.entity.Task
import io.github.mateorguez.cincopelao.domain.stats.GradeCalculator
import io.github.mateorguez.cincopelao.ui.tasks.daysUntil

/** Lo que muestra la tarjeta de una asignatura en la lista. */
data class SubjectSummary(
    val subject: Subject,
    /** Texto de la pastilla: la abreviatura o, si no tiene, las iniciales del nombre. */
    val badge: String,
    /** Media de las notas de los eventos puntuables ya calificados; null si no hay ninguna. */
    val average: Float?,
    /** Último evento puntuable calificado (por fecha); null si no hay ninguno. */
    val lastGraded: Event?,
    /**
     * Nota final (ponderada) cuando ya están hechos todos los eventos y hay alguno con nota;
     * mientras quede alguno, null.
     */
    val finalGrade: Float?,
    /** Siguiente evento sin hacer, de hoy en adelante; null si no queda ninguno. */
    val nextEvent: Event?,
    /** Tareas principales sin completar ni archivar (las subtareas cuentan dentro de su madre). */
    val pendingTasks: Int
)

/** Resume cada asignatura con sus eventos y tareas. Lógica pura, sin Android. */
fun subjectSummaries(
    subjects: List<Subject>,
    events: List<Event>,
    tasks: List<Task>,
    now: Long = System.currentTimeMillis()
): List<SubjectSummary> {
    val eventsBySubject = events.groupBy { it.subjectId }
    val pendingBySubject = tasks
        .filter { it.subjectId != null && it.parentTaskId == null && !it.isCompleted && !it.archived }
        .groupingBy { it.subjectId }
        .eachCount()
    return subjects.map { subject ->
        val own = eventsBySubject[subject.subjectId].orEmpty()
        val graded = own.filter { it.isPuntuable && it.score != null }
        SubjectSummary(
            subject = subject,
            badge = subjectBadge(subject),
            average = GradeCalculator.meanGrade(own),
            lastGraded = graded.maxByOrNull { it.date },
            finalGrade = if (graded.isNotEmpty() && own.all { it.isDone() }) GradeCalculator.weightedGrade(own) else null,
            nextEvent = own
                .filter { daysUntil(it.date, now) >= 0 && !it.isDone() }
                .minByOrNull { it.date },
            pendingTasks = pendingBySubject[subject.subjectId] ?: 0
        )
    }
}

/** Un evento ya hecho no es «próximo» aunque sea hoy: tiene nota o se marcó como hecho. */
private fun Event.isDone(): Boolean = score != null || noPuntuableDone

/** Abreviatura en mayúsculas o, si está vacía, iniciales de las palabras con contenido (máx. 3). */
fun subjectBadge(subject: Subject): String {
    subject.abbrev.trim().takeIf { it.isNotEmpty() }?.let { return it.uppercase().take(4) }
    val words = subject.name.split(' ', '-').filter { it.isNotBlank() }
    val significant = words.filter { it.length > 2 }.ifEmpty { words }
    return when {
        significant.isEmpty() -> "?"
        significant.size == 1 -> significant[0].take(2).uppercase()
        else -> significant.take(3).joinToString("") { it.take(1).uppercase() }
    }
}

/** Media global de la instancia y de cuántas asignaturas sale. */
data class OverallAverage(val value: Float, val subjects: Int)

/**
 * Media de las asignaturas con alguna nota, contando cada una igual: su nota final si ya está
 * terminada y, si no, la media de sus notas. null si ninguna tiene nota.
 */
fun overallAverage(summaries: List<SubjectSummary>): OverallAverage? {
    val grades = summaries.mapNotNull { it.finalGrade ?: it.average }
    return if (grades.isEmpty()) null else OverallAverage(grades.average().toFloat(), grades.size)
}
