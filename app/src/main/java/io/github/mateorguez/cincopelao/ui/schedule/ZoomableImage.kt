package io.github.mateorguez.cincopelao.ui.schedule

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale

// Imagen con zoom por pellizco, desplazamiento y doble toque.

@Composable
fun ZoomableImage(
    bitmap: Bitmap,
    initialZoom: Float = 1f,
    initialOffset: Offset = Offset.Zero,
    onTransformChange: (Float, Offset) -> Unit
) {
    var scale by remember { mutableStateOf(initialZoom.coerceIn(1f, 10f)) }
    var offset by remember { mutableStateOf(initialOffset) }

    val minScale = 1f
    val maxScale = 10f // mismo límite que al restaurar el zoom guardado

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoomChange, _ ->
                    val newScaleRaw = scale * zoomChange
                    val newScale = newScaleRaw.coerceIn(minScale, maxScale)
                    val scaleFactor = newScale / scale

                    // Límites del offset para no mover la imagen fuera del viewport
                    val maxOffsetX = (size.width / 2) * (newScale - 1)
                    val minOffsetX = -maxOffsetX
                    val maxOffsetY = (size.height / 2) * (newScale - 1)
                    val minOffsetY = -maxOffsetY

                    // Ajuste para zoom centrado en el punto del pellizco
                    val offsetXChange = (centroid.x - offset.x) * (scaleFactor - 1)
                    val offsetYChange = (centroid.y - offset.y) * (scaleFactor - 1)

                    // Aquí ajustamos el pan dividiéndolo por newScale para que el desplazamiento sea proporcional al zoom
                    val newOffsetX = (offset.x + pan.x / newScale*2.5f + offsetXChange)
                        .coerceIn(minOffsetX, maxOffsetX)
                    val newOffsetY = (offset.y + pan.y / newScale*2.5f + offsetYChange)
                        .coerceIn(minOffsetY, maxOffsetY)

                    scale = newScale
                    offset = Offset(newOffsetX, newOffsetY)
                    onTransformChange(scale, offset)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale != 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2f
                        }
                        onTransformChange(scale, offset)
                    }
                )
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}
