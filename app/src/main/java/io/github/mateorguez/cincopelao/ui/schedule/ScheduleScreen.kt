package io.github.mateorguez.cincopelao.ui.schedule

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.mateorguez.cincopelao.R
import io.github.mateorguez.cincopelao.data.prefs.getScheduleState
import io.github.mateorguez.cincopelao.data.prefs.saveScheduleState
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@Composable
fun ScheduleScreen(instanceId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var zoom by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var currentFileUriString by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { selectedUri ->
        selectedUri?.let { uri ->
            scope.launch {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    val mimeType = context.contentResolver.getType(uri) ?: ""
                    val isPdf = mimeType == "application/pdf"

                    if (isPdf) {
                        context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                            val renderer = PdfRenderer(fd)
                            val pageCount = renderer.pageCount
                            renderer.close()
                            val maxPagesAllowed = 5
                            if (pageCount > maxPagesAllowed) {
                                Toast.makeText(context,
                                    "El PDF tiene más de $maxPagesAllowed páginas. Selecciona uno más corto.",
                                    Toast.LENGTH_LONG).show()
                                return@launch
                            }
                        }
                    }

                    currentFileUriString = uri.toString()

                    bitmap = if (isPdf) {
                        loadPdfAsBitmap(context, uri) ?: throw Exception("No se pudo renderizar el PDF")
                    } else {
                        loadImageAsBitmap(context, uri) ?: throw Exception("No se pudo cargar la imagen")
                    }

                    zoom = 1f
                    offset = Offset.Zero

                    saveScheduleState(context, instanceId, currentFileUriString, zoom, offset)

                } catch (e: Exception) {
                    e.printStackTrace()
                    bitmap = null
                    Toast.makeText(context, "Error al cargar archivo: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LaunchedEffect(instanceId) {
        getScheduleState(context, instanceId).firstOrNull()?.let { (uriString, savedZoom, savedOffset) ->
            currentFileUriString = uriString
            uriString?.let {
                val uri = Uri.parse(uriString)
                val mimeType = context.contentResolver.getType(uri) ?: ""
                val isPdf = mimeType == "application/pdf"

                bitmap = if (isPdf) {
                    loadPdfAsBitmap(context, uri)
                } else {
                    loadImageAsBitmap(context, uri)
                }
                zoom = savedZoom
                offset = savedOffset
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (bitmap == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(20.dp))
                Icon(
                    painter = painterResource(id = R.drawable.schedule),
                    contentDescription = "Logo de horario",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(300.dp).clipToBounds()
                )
                Text(
                    "Aún no has importado tu horario",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Puedes importar una imagen o PDF con tu horario para visualizarlo aquí.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = {
                    launcher.launch(arrayOf("image/*", "application/pdf"))
                }) {
                    Text("Importar Horario")
                }
            }
        } else {
            ZoomableImage(
                bitmap = bitmap!!,
                initialZoom = zoom,
                initialOffset = offset,
                onTransformChange = { newZoom, newOffset ->
                    zoom = newZoom
                    offset = newOffset
                    scope.launch {
                        saveScheduleState(context, instanceId, currentFileUriString!!, newZoom!!, newOffset!!)
                    }
                }
            )

            FloatingActionButton(
                onClick = {
                    launcher.launch(arrayOf("image/*", "application/pdf"))
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Cambiar archivo")
            }
        }
    }
}

fun loadImageAsBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun loadPdfAsBitmap(context: Context, uri: Uri, spacingPx: Int = 20): Bitmap? {
    return try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { parcelFd ->
            val renderer = PdfRenderer(parcelFd)
            val pageCount = renderer.pageCount

            if (pageCount == 0) return null

            val scale = 3f

            // Obtenemos dimensiones base de la primera página
            val tempPage = renderer.openPage(0)
            val pageWidth = (tempPage.width * scale).toInt()
            val pageHeight = (tempPage.height * scale).toInt()
            tempPage.close()

            val totalWidth = pageWidth
            val totalHeight = pageCount * pageHeight + (pageCount - 1) * spacingPx

            val resultBitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(resultBitmap)
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR) // Fondo completamente transparente para los espaciados

            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)

                val pageBitmap = Bitmap.createBitmap(pageWidth, pageHeight, Bitmap.Config.ARGB_8888)
                val pageCanvas = Canvas(pageBitmap)
                pageCanvas.drawColor(Color.WHITE)

                val matrix = Matrix().apply {
                    setScale(scale, scale)
                }

                page.render(pageBitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                val top = i * (pageHeight + spacingPx)
                canvas.drawBitmap(pageBitmap, 0f, top.toFloat(), null)

                page.close()
            }

            renderer.close()
            resultBitmap
        }
    } catch (e: Exception) {
        Log.e("PDF", "Error al cargar PDF", e)
        null
    }
}

@Composable
fun ZoomableImage(
    bitmap: Bitmap,
    initialZoom: Float = 1f,
    initialOffset: Offset = Offset.Zero,
    onTransformChange: (Float, Offset) -> Unit
) {
    var scale by remember { mutableStateOf(initialZoom.coerceIn(1f, 4f)) }
    var offset by remember { mutableStateOf(initialOffset) }

    val minScale = 1f
    val maxScale = 10f

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
