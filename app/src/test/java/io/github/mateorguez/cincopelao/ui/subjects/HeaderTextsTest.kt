package io.github.mateorguez.cincopelao.ui.subjects

import io.github.mateorguez.cincopelao.data.prefs.HeaderGrade
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class HeaderTextsTest {

    private lateinit var previous: Locale

    @Before
    fun spanish() {
        previous = Locale.getDefault()
        Locale.setDefault(Locale("es", "ES"))
    }

    @After
    fun restore() = Locale.setDefault(previous)

    @Test
    fun `ponderada en grande y la media en la frase`() {
        val t = headerTexts(weighted = 7.4f, mean = 7.25f, shown = HeaderGrade.WEIGHTED)
        assertEquals("7,40", t.big)
        assertEquals("Tu nota media es buena: 7,25", t.status)
    }

    @Test
    fun `media en grande y la ponderada en la frase`() {
        val t = headerTexts(weighted = 7.4f, mean = 7.25f, shown = HeaderGrade.MEAN)
        assertEquals("7,25", t.big)
        assertEquals("Tu media es buena. Ponderada: 7,40", t.status)
    }

    @Test
    fun `nota entera con un decimal`() {
        assertEquals("7,0", headerTexts(7f, 7f, HeaderGrade.MEAN).big)
    }

    @Test
    fun `la valoracion sigue a la media en los dos modos`() {
        assertEquals("¡Excelente! Tu nota media es de 9,50", headerTexts(6f, 9.5f, HeaderGrade.WEIGHTED).status)
        assertEquals("¡Excelente media! Ponderada: 6,00", headerTexts(6f, 9.5f, HeaderGrade.MEAN).status)
        assertEquals("Tu media es baja. Ponderada: 3,00", headerTexts(3f, 3f, HeaderGrade.MEAN).status)
    }

    @Test
    fun `etiqueta con y sin la pista del toque`() {
        assertEquals("Nota media", headerLabel(HeaderGrade.MEAN, tapHint = false))
        assertEquals("Nota ponderada · toca para cambiar", headerLabel(HeaderGrade.WEIGHTED, tapHint = true))
    }

    @Test
    fun `sin notas`() {
        for (shown in HeaderGrade.entries) {
            val t = headerTexts(null, null, shown)
            assertEquals("Sin nota", t.big)
            assertEquals("¡Suerte con los exámenes!", t.status)
        }
    }
}
