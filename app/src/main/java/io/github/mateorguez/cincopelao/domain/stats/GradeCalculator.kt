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

/** Cómo de exigente es la nota que hace falta en un evento pendiente. */
enum class Requirement {
    /** Basta con la nota mínima del evento (o con cualquier nota, si no tiene). */
    MINIMUM,
    /** Hace falta algo más, hasta un 7. */
    REACHABLE,
    /** Hace falta más de un 7. */
    DEMANDING,
    /** Hace falta más de un 10. */
    IMPOSSIBLE
}

/** Resumen de la calculadora para una nota objetivo. */
sealed interface TargetVerdict {
    /** Hace falta [average] de media en los [pending] eventos que quedan. */
    data class Needs(val average: Float, val pending: Int) : TargetVerdict
    /** Ya se llega con lo hecho; si [minimums], aún hay que sacar las mínimas. */
    data class Reached(val minimums: Boolean) : TargetVerdict
    /** No se llega ni con un 10 en todo lo que queda; lo máximo es [maxReachable]. */
    data class Impossible(val maxReachable: Float) : TargetVerdict
    /** No queda nada por hacer: la nota final es [finalGrade]. */
    data class NothingLeft(val finalGrade: Float) : TargetVerdict
}

/** Margen para que los redondeos de coma flotante no cambien de tramo una nota justa. */
private const val EPSILON = 1e-3f

/** Qué supone sacar [required] en un evento con nota mínima [minScore] (de 0 a 100). */
fun requirement(required: Float, minScore: Float?): Requirement = when {
    required > 10f + EPSILON -> Requirement.IMPOSSIBLE
    required <= (minScore ?: 0f) / 10f + EPSILON -> Requirement.MINIMUM
    required > 7f + EPSILON -> Requirement.DEMANDING
    else -> Requirement.REACHABLE
}

/** Resumen de lo que hace falta para llegar a [target] con los [events] dados. */
fun targetVerdict(events: List<GradedEvent>, target: Float, plan: TargetPlan): TargetVerdict {
    val pending = events.filter { it.score == null }
    if (pending.isEmpty()) return TargetVerdict.NothingLeft(plan.currentGrade)
    val remainingWeight = pending.sumOf { it.weight.toDouble() }.toFloat() / 100f
    val maxReachable = plan.currentGrade + remainingWeight * 10f
    val needed = target - plan.currentGrade
    return when {
        target > maxReachable + EPSILON -> TargetVerdict.Impossible(maxReachable)
        needed <= EPSILON -> TargetVerdict.Reached(minimums = pending.any { (it.minScore ?: 0f) > 0f })
        else -> TargetVerdict.Needs(needed / remainingWeight, pending.size)
    }
}

