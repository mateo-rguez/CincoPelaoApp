package io.github.mateorguez.cincopelao.ui.sharing

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayNameCounterTest {

    @Test
    fun `cuenta los caracteres sin espacios de los extremos`() {
        assertEquals("5/12 caracteres", displayNameCounter("  Mateo "))
    }

    @Test
    fun `avisa cuando hay letras que ocupan doble`() {
        assertEquals("6/12 caracteres · las tildes y la ñ cuentan doble", displayNameCounter("Iñigo"))
    }
}
