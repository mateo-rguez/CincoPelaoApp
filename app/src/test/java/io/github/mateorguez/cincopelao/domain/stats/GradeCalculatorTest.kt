package io.github.mateorguez.cincopelao.domain.stats

import io.github.mateorguez.cincopelao.data.local.entity.Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GradeCalculatorTest {

    private val delta = 1e-3f

    private fun event(score: Float?, weight: Float?, minScore: Float? = null, puntuable: Boolean = true) =
        Event(subjectId = 1, name = "e", type = "Examen", isPuntuable = puntuable, score = score, weight = weight, minScore = minScore, date = 0L)

    @Test
    fun `nota ponderada solo con eventos que alcanzan la minima`() {
        val events = listOf(event(8f, 40f), event(3f, 30f, minScore = 40f), event(null, 30f))
        assertEquals(3.2f, GradeCalculator.weightedGrade(events)!!, delta)
    }

    @Test
    fun `sin notas no hay nota ponderada ni media`() {
        assertNull(GradeCalculator.weightedGrade(listOf(event(null, 100f))))
        assertNull(GradeCalculator.meanGrade(listOf(event(null, 100f))))
    }

    @Test
    fun `la media ignora los no puntuables`() {
        val events = listOf(event(6f, 50f), event(8f, 50f), event(null, null, puntuable = false))
        assertEquals(7f, GradeCalculator.meanGrade(events)!!, delta)
    }

    @Test
    fun `un evento puntuable sin peso cuenta con peso 0 y no rompe nada`() {
        val graded = GradeCalculator.gradedEvents(listOf(event(7f, null), event(null, 100f)))
        assertEquals(0f, graded[0].weight, delta)
        GradeCalculator.requiredScores(graded, target = 5f)
    }

    @Test
    fun `lo que falta se reparte a partes iguales entre los pendientes`() {
        // Llevo un 8 en el 40%; para un 5 me faltan 1.8 puntos en el 60% restante -> un 3 en cada uno
        val plan = GradeCalculator.requiredScores(
            listOf(GradedEvent("a", 8f, 40f, null), GradedEvent("b", null, 30f, null), GradedEvent("c", null, 30f, null)),
            target = 5f
        )
        assertEquals(3.2f, plan.currentGrade, delta)
        assertNull(plan.required[0])
        assertEquals(3f, plan.required[1]!!, delta)
        assertEquals(3f, plan.required[2]!!, delta)
    }

    @Test
    fun `una nota minima alta fija ese evento y reparte el resto entre los demas`() {
        // Sin mínimas haría falta un 5 en cada uno; con mínima 7 en "b", "b" pide un 7 y "c" un 3
        val plan = GradeCalculator.requiredScores(
            listOf(GradedEvent("b", null, 50f, 70f), GradedEvent("c", null, 50f, null)),
            target = 5f
        )
        assertEquals(7f, plan.required[0]!!, delta)
        assertEquals(3f, plan.required[1]!!, delta)
    }

    // --- Exigencia de cada evento y resumen de la calculadora ---

    @Test
    fun `la exigencia va por tramos y la minima manda por debajo`() {
        assertEquals(Requirement.MINIMUM, requirement(4f, minScore = 40f))
        assertEquals(Requirement.MINIMUM, requirement(0f, minScore = null))
        assertEquals(Requirement.REACHABLE, requirement(4.08f, minScore = 40f))
        assertEquals(Requirement.REACHABLE, requirement(7f, minScore = null))
        assertEquals(Requirement.DEMANDING, requirement(7.01f, minScore = null))
        assertEquals(Requirement.DEMANDING, requirement(10f, minScore = null))
        assertEquals(Requirement.IMPOSSIBLE, requirement(10.01f, minScore = null))
    }

    private val halfDone = listOf(GradedEvent("a", 8f, 50f, null), GradedEvent("b", null, 50f, 40f))

    private fun verdict(events: List<GradedEvent>, target: Float) =
        targetVerdict(events, target, GradeCalculator.requiredScores(events, target))

    @Test
    fun `el resumen da la media necesaria en lo que queda`() {
        // Llevo 4 de 5 posibles; para un 7 me faltan 3 en el 50% -> un 6 de media en 1 evento
        val v = verdict(halfDone, 7f) as TargetVerdict.Needs
        assertEquals(6f, v.average, delta)
        assertEquals(1, v.pending)
    }

    @Test
    fun `si ya se llega el resumen avisa de las minimas pendientes`() {
        assertEquals(TargetVerdict.Reached(minimums = true), verdict(halfDone, 4f))
        assertEquals(TargetVerdict.Reached(minimums = false), verdict(listOf(GradedEvent("a", 8f, 50f, null), GradedEvent("b", null, 50f, null)), 3f))
    }

    @Test
    fun `si no se llega ni con dieces el resumen da el maximo posible`() {
        val v = verdict(listOf(GradedEvent("a", 2f, 50f, null), GradedEvent("b", null, 50f, null)), 8f) as TargetVerdict.Impossible
        assertEquals(6f, v.maxReachable, delta)
    }

    @Test
    fun `sin pendientes el resumen da la nota final`() {
        assertEquals(TargetVerdict.NothingLeft(4f), verdict(listOf(GradedEvent("a", 8f, 50f, null)), 5f))
    }
}
