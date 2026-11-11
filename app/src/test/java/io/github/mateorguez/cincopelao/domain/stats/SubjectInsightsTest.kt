package io.github.mateorguez.cincopelao.domain.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubjectInsightsTest {

    private val delta = 1e-3f

    @Test
    fun `mejor y peor nota llevan el nombre de su evento`() {
        val insights = subjectInsights(
            listOf(GradedEvent("a", 9f, 10f, null), GradedEvent("b", 8.38f, 10f, null), GradedEvent("c", null, 80f, null))
        )
        assertEquals(NamedScore("a", 9f), insights.best)
        assertEquals(NamedScore("b", 8.38f), insights.worst)
        assertEquals(2, insights.scoredCount)
    }

    @Test
    fun `lo que mas pesa de lo pendiente es el primero con mas peso`() {
        val insights = subjectInsights(
            listOf(
                GradedEvent("hecho", 5f, 50f, null),
                GradedEvent("x", null, 20f, null),
                GradedEvent("y", null, 30f, null),
                GradedEvent("z", null, 30f, null),
                GradedEvent("sinPeso", null, 0f, null)
            )
        )
        assertEquals(PendingWeight("y", 30f), insights.heaviestPending)
    }

    @Test
    fun `si sigues asi saca la media actual en todo lo que queda`() {
        // Media 8: 8·0,2 + 8·0,8 = 8
        val insights = subjectInsights(listOf(GradedEvent("a", 7f, 10f, null), GradedEvent("b", 9f, 10f, null), GradedEvent("c", null, 80f, null)))
        assertEquals(8f, insights.projectionIfSteady!!, delta)
    }

    @Test
    fun `si la media no llega a la minima de un pendiente ese evento no suma`() {
        // Media 4 y "b" pide un 5: solo cuenta "a" -> 4·0,5 = 2
        val insights = subjectInsights(listOf(GradedEvent("a", 4f, 50f, null), GradedEvent("b", null, 50f, 50f)))
        assertEquals(2f, insights.projectionIfSteady!!, delta)
    }

    @Test
    fun `sin notas no hay mejor peor ni proyeccion`() {
        val insights = subjectInsights(listOf(GradedEvent("a", null, 100f, null)))
        assertNull(insights.best)
        assertNull(insights.worst)
        assertNull(insights.projectionIfSteady)
        assertEquals(PendingWeight("a", 100f), insights.heaviestPending)
        assertEquals(0, insights.scoredCount)
    }
}
