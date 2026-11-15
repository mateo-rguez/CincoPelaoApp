package io.github.mateorguez.cincopelao.ui.schedule

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.max
import kotlin.math.min

// Cuentas del zoom del horario. La imagen se escala desde el centro de la vista y luego se
// desplaza [offset] píxeles de pantalla: un punto a distancia d del centro acaba en d·zoom + offset.

/** Tamaño que ocupa la imagen ajustada a la vista sin recortar (como ContentScale.Fit). */
fun fittedSize(image: Size, viewport: Size): Size {
    if (image.width <= 0f || image.height <= 0f) return viewport
    val ratio = min(viewport.width / image.width, viewport.height / image.height)
    return Size(image.width * ratio, image.height * ratio)
}

/**
 * Desplazamiento permitido con [zoom]: la imagen ampliada no deja huecos dentro de la vista, y
 * en el eje en el que aún cabe entera se queda centrada.
 */
fun clampOffset(offset: Offset, zoom: Float, content: Size, viewport: Size): Offset {
    val maxX = max(0f, (content.width * zoom - viewport.width) / 2f)
    val maxY = max(0f, (content.height * zoom - viewport.height) / 2f)
    return Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
}

/**
 * Desplazamiento al pasar de [from] a [to] de zoom sin que se mueva lo que hay bajo [focus]
 * (el punto del pellizco o del doble toque, medido desde el centro de la vista).
 */
fun zoomAround(offset: Offset, from: Float, to: Float, focus: Offset): Offset {
    val factor = to / from
    return focus * (1f - factor) + offset * factor
}
