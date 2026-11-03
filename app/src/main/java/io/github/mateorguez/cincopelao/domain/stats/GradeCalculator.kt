package io.github.mateorguez.cincopelao.domain.stats

import io.github.mateorguez.cincopelao.data.local.entity.Event
import kotlin.math.max

/** Evento puntuable reducido a lo que importa para las notas. */
data class GradedEvent(
    val name: String,
    /** Nota de 0 a 10, o null si aún no se ha hecho. */
    val score: Float?,
    /** Peso en la nota final, en porcentaje (0-100). */
    val weight: Float,
    /** Nota mínima exigida, de 0 a 100 (se compara como score >= minScore / 10). */
    val minScore: Float?
)

/** Resultado de la calculadora de nota objetivo. */
data class TargetPlan(
    /** Nota ponderada que ya se lleva con los eventos hechos. */
    val currentGrade: Float,
    /** Nota necesaria en cada evento pendiente (null en los ya hechos), en el mismo orden. */
    val required: List<Float?>
)

/** Cálculos de notas de una asignatura. Lógica pura, sin Android. */
object GradeCalculator {

    /** Eventos puntuables en orden, con peso 0 si no lo tienen. */
    fun gradedEvents(events: List<Event>): List<GradedEvent> =
        events.filter { it.isPuntuable }.map { GradedEvent(it.name, it.score, it.weight ?: 0f, it.minScore) }

    /** Nota ponderada actual: suma de nota × peso de los eventos con nota que alcanzan la mínima. */
    fun weightedGrade(events: List<Event>): Float? {
        val scored = events.filter { it.score != null && it.weight != null && it.score >= (it.minScore ?: 0f) / 10 }
        if (scored.isEmpty()) return null
        return (scored.sumOf { (it.score!! * it.weight!!).toDouble() } / 100).toFloat()
    }

    /** Media simple de las notas de los eventos puntuables. */
    fun meanGrade(events: List<Event>): Float? {
        val scores = events.filter { it.isPuntuable }.mapNotNull { it.score }
        return if (scores.isEmpty()) null else scores.average().toFloat()
    }

    /**
     * Qué nota hace falta en cada evento pendiente para llegar a [target].
     *
     * Se reparte lo que falta a partes iguales entre los pendientes. Si en alguno la nota mínima
     * exigida es mayor que esa nota, se le asigna la mínima y el resto se reparte entre los demás.
     */
    fun requiredScores(events: List<GradedEvent>, target: Float): TargetPlan {
        val scores = events.map { it.score }
        val weights = events.map { it.weight }
        val minScores = events.map { (it.minScore ?: 0f) / 10f }

        val currentGrade = events.sumOf { e -> (e.score?.let { it * e.weight / 100 } ?: 0f).toDouble() }.toFloat()

        val remainingWeightSum = events.filter { it.score == null }.sumOf { it.weight.toDouble() }.toFloat() / 100f
        val neededGrade = target - currentGrade
        val remainingTarget = if (remainingWeightSum > 0) neededGrade / remainingWeightSum else 0f

        val adjusted = scores.mapIndexed { i, score -> if (score == null) max(minScores[i], remainingTarget) else null }

        // Los eventos cuya mínima supera el reparto base quedan fijados en su mínima
        val fixedWeight = adjusted.mapIndexed { i, s -> if (s != null && s > remainingTarget) weights[i] / 100 else 0f }.sum()
        val fixedContribution = adjusted.mapIndexed { i, s -> if (s != null && s > remainingTarget) s * weights[i] / 100 else 0f }.sum()

        val remainingToDistribute = (neededGrade - fixedContribution).coerceAtLeast(0f)
        val weightToDistribute = remainingWeightSum - fixedWeight

        val required = adjusted.mapIndexed { i, s ->
            if (s != null && s <= remainingTarget) {
                val proportion = if (weightToDistribute > 0) weights[i] / weightToDistribute else 0f
                max(minScores[i], proportion * remainingToDistribute / weights[i])
            } else s
        }
        return TargetPlan(currentGrade, required)
    }
}
