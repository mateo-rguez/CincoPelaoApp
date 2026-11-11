package io.github.mateorguez.cincopelao.ui.subjects

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreStateTest {

    private val changes = mutableListOf<Float?>()
    private fun state(initial: String = "") = ScoreState(initial) { changes += it }

    @Test
    fun `bajar desde vacío no hace nada`() {
        val score = state()
        score.modify(-0.1f)
        assertEquals("", score.text)
        assertTrue(changes.isEmpty())
    }

    @Test
    fun `subir desde vacío empieza en el paso`() {
        val score = state()
        score.modify(1f)
        assertEquals(1f, changes.single()!!, 0.0001f)
    }

    @Test
    fun `bajar desde cero borra la nota y no pasa de 0 a 10`() {
        val score = state("0,00")
        score.modify(-1f)
        assertEquals("", score.text)
        assertNull(changes.single())

        val high = state("9,50")
        high.modify(1f)
        assertEquals(10f, changes.last()!!, 0.0001f)
    }

    @Test
    fun `al escribir solo se aceptan notas de 0 a 10`() {
        val score = state()
        score.type("11")
        assertEquals("", score.text)
        score.type("7,5")
        assertEquals("7,5", score.text)
        assertEquals(7.5f, changes.last()!!, 0.0001f)
        score.type("")
        assertNull(changes.last())
    }
}
