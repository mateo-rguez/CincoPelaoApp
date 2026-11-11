package io.github.mateorguez.cincopelao.ui.subjects

import org.junit.Assert.assertEquals
import org.junit.Test

class GradeChartTest {

    // Cada letra mide 1: así el hueco se cuenta en letras
    private val measure: (String) -> Float = { it.length.toFloat() }

    @Test
    fun `un nombre que cabe queda igual`() {
        assertEquals("Foro Moovi", ellipsizeToWidth("Foro Moovi", room = 10f, measure = measure))
    }

    @Test
    fun `uno que no cabe se recorta con puntos suspensivos contandolos en el hueco`() {
        assertEquals("Trabajo…", ellipsizeToWidth("Trabajo colaborativo", room = 8f, measure = measure))
    }

    @Test
    fun `no se deja un espacio antes de los puntos suspensivos`() {
        assertEquals("Foro…", ellipsizeToWidth("Foro Moovi", room = 6f, measure = measure))
    }

    @Test
    fun `si todos caben van en una fila y sin recortar`() {
        val layout = chartLabelLayout(listOf("Cuest", "Debate"), slot = 6f, measure = measure)
        assertEquals(listOf(ChartLabel("Cuest", 0), ChartLabel("Debate", 0)), layout)
    }

    @Test
    fun `si alguno no cabe se alternan en dos filas con el doble de hueco`() {
        val layout = chartLabelLayout(listOf("Entrega 1", "Cuest. 3", "Foro"), slot = 5f, measure = measure)
        assertEquals(listOf(ChartLabel("Entrega 1", 0), ChartLabel("Cuest. 3", 1), ChartLabel("Foro", 0)), layout)
    }

    @Test
    fun `en dos filas el que no cabe ni con el doble se recorta`() {
        val layout = chartLabelLayout(listOf("Trabajo colaborativo", "a"), slot = 4f, measure = measure)
        assertEquals(ChartLabel("Trabajo…", 0), layout[0])
    }

    @Test
    fun `suspende por debajo de 5 o de la minima del evento`() {
        assertEquals(true, isFailing(4.99f, minScore = null))
        assertEquals(false, isFailing(5f, minScore = null))
        assertEquals(true, isFailing(5.5f, minScore = 60f))
        assertEquals(false, isFailing(6f, minScore = 60f))
    }
}
