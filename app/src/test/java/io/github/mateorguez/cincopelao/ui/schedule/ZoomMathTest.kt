package io.github.mateorguez.cincopelao.ui.schedule

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test

class ZoomMathTest {

    private val viewport = Size(1000f, 2000f)

    /** Dónde acaba en pantalla (desde el centro) un punto de la imagen a [d] del centro. */
    private fun screen(d: Offset, zoom: Float, offset: Offset) = d * zoom + offset

    @Test
    fun `la imagen ancha ocupa todo el ancho`() {
        assertEquals(Size(1000f, 625f), fittedSize(Size(1600f, 1000f), viewport))
        assertEquals(viewport, fittedSize(Size(0f, 0f), viewport))
    }

    @Test
    fun `sin zoom no se mueve y con zoom no deja huecos`() {
        val content = Size(1000f, 625f)
        assertEquals(Offset.Zero, clampOffset(Offset(300f, 300f), 1f, content, viewport))
        // A 4x mide 4000 x 2500: puede moverse 1500 a los lados y 250 arriba y abajo
        assertEquals(Offset(1500f, -250f), clampOffset(Offset(9999f, -9999f), 4f, content, viewport))
        // A 2x mide 1250 de alto, menos que la vista: se queda centrada en vertical
        assertEquals(Offset(100f, 0f), clampOffset(Offset(100f, 400f), 2f, content, viewport))
    }

    @Test
    fun `el punto del pellizco no se mueve al ampliar`() {
        val focus = Offset(200f, -300f)
        val offset = Offset(50f, 80f)
        // Punto de la imagen que está bajo el dedo con zoom 2
        val underFinger = (focus - offset) / 2f

        val newOffset = zoomAround(offset, from = 2f, to = 5f, focus = focus)

        val after = screen(underFinger, 5f, newOffset)
        assertEquals(focus.x, after.x, 0.001f)
        assertEquals(focus.y, after.y, 0.001f)
    }
}
