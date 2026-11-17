package io.github.mateorguez.cincopelao.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class AnimatedColumnItemsTest {

    @Test
    fun `sin cambios el orden es el actual`() {
        assertEquals(listOf(1, 2, 3), keepRemovedInPlace(listOf(1, 2, 3), listOf(1, 2, 3)))
    }

    @Test
    fun `el que se quita queda en su sitio mientras sale`() {
        assertEquals(listOf(1, 2, 3), keepRemovedInPlace(listOf(1, 2, 3), listOf(1, 3)))
        assertEquals(listOf(1, 2, 3), keepRemovedInPlace(listOf(1, 2, 3), listOf(2, 3)))
        assertEquals(listOf(1, 2, 3), keepRemovedInPlace(listOf(1, 2, 3), listOf(1, 2)))
    }

    @Test
    fun `varios seguidos que salen conservan su orden`() {
        assertEquals(listOf(1, 2, 3, 4), keepRemovedInPlace(listOf(1, 2, 3, 4), listOf(1, 4)))
        assertEquals(listOf(1, 2), keepRemovedInPlace(listOf(1, 2), emptyList()))
    }

    @Test
    fun `los nuevos aparecen en su posición actual`() {
        assertEquals(listOf(1, 5, 3), keepRemovedInPlace(listOf(1, 3), listOf(1, 5, 3)))
        // Sale el 2 y entra el 5 en otra posición
        assertEquals(listOf(5, 1, 2, 3), keepRemovedInPlace(listOf(1, 2, 3), listOf(5, 1, 3)))
    }
}
