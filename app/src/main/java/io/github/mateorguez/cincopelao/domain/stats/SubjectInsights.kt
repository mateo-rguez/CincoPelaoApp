package io.github.mateorguez.cincopelao.domain.stats

/** Una nota con el evento en que se sacó. */
data class NamedScore(val name: String, val score: Float)

/** Un evento pendiente con su peso en la nota final (en porcentaje). */
data class PendingWeight(val name: String, val weight: Float)

/** Datos de una asignatura que no salen de [Stats]: van con el nombre del evento o son proyecciones. */
data class SubjectInsights(
    /** Mejor nota (la primera, si se repite), o null sin notas. */
    val best: NamedScore?,
    /** Peor nota (la primera, si se repite), o null sin notas. */
    val worst: NamedScore?,
    /** El evento pendiente que más pesa (el primero, si empatan), o null si no queda ninguno con peso. */
    val heaviestPending: PendingWeight?,
    /** Nota final si en todo lo que queda se saca la media actual, o null sin notas. */
    val projectionIfSteady: Float?,
    /** Cuántos eventos tienen nota. */
    val scoredCount: Int
)

/** Calcula los [SubjectInsights] de los eventos puntuables de una asignatura. Lógica pura. */
fun subjectInsights(events: List<GradedEvent>): SubjectInsights {
    val scored = events.mapNotNull { e -> e.score?.let { NamedScore(e.name, it) } }
    val average = scored.map { it.score }.average().toFloat().takeIf { scored.isNotEmpty() }

    // Como el resto de proyecciones: una nota por debajo de la mínima de su evento no suma
    val projection = average?.let { avg ->
        events.sumOf { e ->
            val value = e.score ?: avg
            if (value >= (e.minScore ?: 0f) / 10f) (value * e.weight / 100f).toDouble() else 0.0
        }.toFloat()
    }

    return SubjectInsights(
        best = scored.maxByOrNull { it.score },
        worst = scored.minByOrNull { it.score },
        heaviestPending = events.filter { it.score == null && it.weight > 0f }
            .maxByOrNull { it.weight }
            ?.let { PendingWeight(it.name, it.weight) },
        projectionIfSteady = projection,
        scoredCount = scored.size
    )
}
