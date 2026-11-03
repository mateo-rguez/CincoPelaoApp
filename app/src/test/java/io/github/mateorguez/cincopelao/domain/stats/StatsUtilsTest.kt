package io.github.mateorguez.cincopelao.domain.stats

import io.github.mateorguez.cincopelao.data.local.entity.Event
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests de caracterización de [StatsUtils]: fijan el comportamiento actual
 * para poder refactorizar sin cambiarlo por accidente.
 *
 * Convenciones del modelo: `score` va de 0 a 10, `weight` es un porcentaje (0-100)
 * y `minScore` también va de 0 a 100 (se compara como `score >= minScore / 10`).
 */
class StatsUtilsTest {

    private val delta = 1e-4f

    private fun graded(score: Float?, weight: Float, minScore: Float? = null) = Event(
        subjectId = 1,
        name = "evento",
        type = "Examen",
        isPuntuable = true,
        weight = weight,
        score = score,
        minScore = minScore,
        date = 0L
    )

    private fun ungraded(done: Boolean) = Event(
        subjectId = 1,
        name = "práctica",
        type = "Práctica",
        isPuntuable = false,
        noPuntuableDone = done,
        date = 0L
    )

    @Test
    fun `nota ponderada y estadisticas basicas con todo calificado`() {
        val stats = StatsUtils.calculateStats(listOf(graded(8f, 40f), graded(6f, 60f)))

        assertEquals(6.8f, stats.currentWeighted, delta) // 8*0.4 + 6*0.6
        assertEquals(7f, stats.average, delta)
        assertEquals(6f, stats.minScore, delta)
        assertEquals(8f, stats.maxScore, delta)
        assertEquals(2f, stats.range, delta)
        assertEquals(7f, stats.median, delta)
        assertEquals(1f, stats.stdDeviation, delta)
        assertEquals(-2f, stats.trend, delta) // de 8 a 6 en un paso
        assertEquals(2, stats.eventCount)
        assertEquals(2, stats.eventCountDone)
        assertEquals(0, stats.eventCountLeft)
        assertEquals(100f, stats.eventCompletedPerc, delta)
        assertEquals(100f, stats.scoreCompletedPerc, delta)
    }

    @Test
    fun `eventos pendientes cuentan como 5 en finalScoring5 y como 10 en maxPotential`() {
        val stats = StatsUtils.calculateStats(listOf(graded(8f, 40f), graded(null, 60f)))

        assertEquals(3.2f, stats.currentWeighted, delta)
        assertEquals(6.2f, stats.finalScoring5, delta) // 3.2 + 5*0.6
        assertEquals(9.2f, stats.maxPotential, delta)  // 3.2 + 10*0.6
        assertEquals(1, stats.eventCountLeft)
        assertEquals(50f, stats.eventCompletedPerc, delta)
        assertEquals(40f, stats.scoreCompletedPerc, delta)
    }

    @Test
    fun `una nota por debajo del minimo no suma y cuenta como truncada`() {
        // minScore = 40 equivale a un 4 sobre 10
        val stats = StatsUtils.calculateStats(listOf(graded(3f, 50f, minScore = 40f), graded(9f, 50f)))

        assertEquals(4.5f, stats.currentWeighted, delta) // solo suma el 9
        assertEquals(1, stats.eventCountTrunc)
        assertEquals(0, stats.eventCountSusp)
        assertEquals(1, stats.eventCountSobre)
    }

    @Test
    fun `clasificacion por tramos de nota en sus limites`() {
        val stats = StatsUtils.calculateStats(
            listOf(
                graded(4.99f, 20f), // suspenso
                graded(5f, 20f),    // suficiente
                graded(6f, 20f),    // aceptable (6 <= x < 7)
                graded(7f, 20f),    // notable (7 <= x < 9)
                graded(9f, 20f)     // sobresaliente
            )
        )

        assertEquals(1, stats.eventCountSusp)
        assertEquals(1, stats.eventCountSufi)
        assertEquals(1, stats.eventCountAcep)
        assertEquals(1, stats.eventCountNotable)
        assertEquals(1, stats.eventCountSobre)
        assertEquals(0, stats.eventCountTrunc)
    }

    @Test
    fun `los eventos no puntuables se cuentan aparte`() {
        val stats = StatsUtils.calculateStats(
            listOf(ungraded(done = true), ungraded(done = false), graded(5f, 100f))
        )

        assertEquals(2, stats.noPuntCount)
        assertEquals(1, stats.noPuntDoneCount)
        assertEquals(1, stats.eventCount)
        assertEquals(1, stats.eventCountSufi)
    }

    @Test
    fun `mediana con numero par e impar de notas`() {
        assertEquals(7f, StatsUtils.median(listOf(4f, 10f, 6f, 8f)), delta)
        assertEquals(6f, StatsUtils.median(listOf(8f, null, 4f, 6f)), delta)
        assertEquals(0f, StatsUtils.median(emptyList()), delta)
    }

    @Test
    fun `la tendencia respeta la posicion de los eventos sin nota`() {
        // Puntos (0, 5) y (2, 7): pendiente 1
        assertEquals(1f, StatsUtils.calculateTrend(listOf(5f, null, 7f)), delta)
        assertEquals(0f, StatsUtils.calculateTrend(listOf(5f)), delta)
    }

    // --- Casos límite (antes provocaban un crash o devolvían NaN) ---

    @Test
    fun `un evento puntuable sin peso no provoca un crash y no suma a la nota`() {
        val sinPeso = graded(7f, 0f).copy(weight = null)
        val stats = StatsUtils.calculateStats(listOf(sinPeso, graded(8f, 100f)))

        assertEquals(8f, stats.currentWeighted, delta)
        assertEquals(2, stats.eventCount)
    }

    @Test
    fun `sin eventos puntuables las estadisticas valen 0 y no NaN`() {
        val stats = StatsUtils.calculateStats(listOf(ungraded(done = true)))

        assertEquals(0f, stats.average, delta)
        assertEquals(0f, stats.stdDeviation, delta)
        assertEquals(0f, stats.eventCompletedPerc, delta)
    }
}
