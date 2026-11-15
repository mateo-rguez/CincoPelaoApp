package io.github.mateorguez.cincopelao.ui.schedule

import android.graphics.Bitmap
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.launch

// Imagen con zoom por pellizco, desplazamiento y doble toque. La imagen sigue al dedo 1:1 con
// cualquier zoom, el pellizco amplía hacia donde están los dedos y el doble toque hacia el punto
// tocado (las cuentas, con tests, en ZoomMath.kt).

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 10f // mismo límite que al restaurar el zoom guardado
private const val DOUBLE_TAP_ZOOM = 2.5f

@Composable
fun ZoomableImage(
    bitmap: Bitmap,
    initialZoom: Float = 1f,
    initialOffset: Offset = Offset.Zero,
    onTransformChange: (Float, Offset) -> Unit
) {
    var zoom by remember { mutableStateOf(initialZoom.coerceIn(MIN_ZOOM, MAX_ZOOM)) }
    var offset by remember { mutableStateOf(initialOffset) }
    var viewport by remember { mutableStateOf(Size.Zero) }
    val imageSize = remember(bitmap) { Size(bitmap.width.toFloat(), bitmap.height.toFloat()) }
    val scope = rememberCoroutineScope()

    /** Aplica un zoom y un desplazamiento, sin dejar huecos, y avisa para guardarlos. */
    fun apply(newZoom: Float, newOffset: Offset) {
        zoom = newZoom
        offset = clampOffset(newOffset, newZoom, fittedSize(imageSize, viewport), viewport)
        onTransformChange(zoom, offset)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewport = it.toSize() }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoomChange, _ ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val newZoom = (zoom * zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)
                    apply(newZoom, zoomAround(offset, zoom, newZoom, centroid - center) + pan)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tap ->
                        // Con zoom vuelve a verlo entero; sin zoom amplía hacia el punto tocado
                        val focus = tap - Offset(size.width / 2f, size.height / 2f)
                        val startZoom = zoom
                        val startOffset = offset
                        val target = if (startZoom > MIN_ZOOM + 0.01f) MIN_ZOOM else DOUBLE_TAP_ZOOM
                        scope.launch {
                            animate(startZoom, target, animationSpec = tween(250)) { value, _ ->
                                apply(value, zoomAround(startOffset, startZoom, value, focus))
                            }
                        }
                    }
                )
            }
            .graphicsLayer {
                scaleX = zoom
                scaleY = zoom
                translationX = offset.x
                translationY = offset.y
            }
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Horario",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}
