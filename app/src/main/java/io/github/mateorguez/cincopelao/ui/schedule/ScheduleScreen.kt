package io.github.mateorguez.cincopelao.ui.schedule

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mateorguez.cincopelao.R
import io.github.mateorguez.cincopelao.ui.common.EmptyState

private val ScheduleMimeTypes = arrayOf("image/*", "application/pdf")

/** Pestaña de horario: muestra la imagen o PDF importado, con zoom; si no hay, invita a importarlo. */
@Composable
fun ScheduleScreen(viewModel: ScheduleViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::import)
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }

    Box(Modifier.fillMaxSize()) {
        when (val s = state) {
            ScheduleUiState.Loading -> Unit
            ScheduleUiState.Empty -> EmptySchedule(onImport = { launcher.launch(ScheduleMimeTypes) })
            is ScheduleUiState.Loaded -> {
                // key: con cada archivo nuevo, la vista empieza con su propio zoom
                key(s.version) {
                    ZoomableImage(
                        bitmap = s.bitmap,
                        initialZoom = s.zoom,
                        initialOffset = s.offset,
                        onTransformChange = viewModel::onTransformChange
                    )
                }
                ScheduleMenu(
                    onChange = { launcher.launch(ScheduleMimeTypes) },
                    onRemove = viewModel::remove
                )
            }
        }
    }
}

/** Sin horario: reloj en el círculo común de las pantallas vacías y el botón de importar. */
@Composable
private fun EmptySchedule(onImport: () -> Unit) {
    EmptyState(
        title = "Aún no has importado tu horario",
        text = "Puedes importar una imagen o PDF con tu horario para visualizarlo aquí.",
        illustration = {
            Icon(
                painter = painterResource(id = R.drawable.schedule),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(120.dp)
            )
        }
    ) {
        Button(onClick = onImport) { Text("Importar Horario") }
    }
}

/** Qué muestra el botón de opciones: cerrado, el menú o la confirmación de quitar. */
private enum class MenuStage { CLOSED, MENU, CONFIRM_REMOVE }

/**
 * Botón «⋮» que crece hasta convertirse en el menú (cambiar archivo o quitar el horario) y, al
 * quitar, en la confirmación, todo en la misma tarjeta. Su icono pasa a ser una ✕ en el mismo
 * sitio. Se cierra tocando fuera o con Atrás.
 */
@Composable
private fun ScheduleMenu(onChange: () -> Unit, onRemove: () -> Unit) {
    var stage by remember { mutableStateOf(MenuStage.CLOSED) }
    val open = stage != MenuStage.CLOSED
    BackHandler(enabled = open) { stage = MenuStage.CLOSED }

    Box(Modifier.fillMaxSize()) {
        // Velo sobre el horario mientras el menú está abierto; tocarlo lo cierra
        val scrim by animateFloatAsState(if (open) 0.32f else 0f, label = "velo")
        if (scrim > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = scrim))
                    .pointerInput(Unit) { detectTapGestures { stage = MenuStage.CLOSED } }
            )
        }

        val corner by animateDpAsState(if (open) 28.dp else 16.dp, label = "esquinas")
        Surface(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            shape = RoundedCornerShape(corner),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shadowElevation = 6.dp
        ) {
            AnimatedContent(
                targetState = stage,
                contentAlignment = Alignment.BottomEnd,
                transitionSpec = {
                    (fadeIn(tween(150, delayMillis = 90)) togetherWith fadeOut(tween(90)))
                        .using(SizeTransform(clip = true) { _, _ ->
                            spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                        })
                },
                label = "menú"
            ) { shown ->
                when (shown) {
                    MenuStage.CLOSED -> Box(
                        modifier = Modifier.size(56.dp).clickable { stage = MenuStage.MENU },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones del horario")
                    }
                    MenuStage.MENU -> Column(Modifier.width(248.dp).padding(top = 8.dp)) {
                        MenuRow(Icons.Default.FileOpen, "Cambiar archivo") {
                            stage = MenuStage.CLOSED
                            onChange()
                        }
                        MenuRow(Icons.Default.Delete, "Quitar horario") { stage = MenuStage.CONFIRM_REMOVE }
                        CloseCorner { stage = MenuStage.CLOSED }
                    }
                    MenuStage.CONFIRM_REMOVE -> Column(Modifier.width(280.dp).padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 8.dp)) {
                        Text("¿Quitar el horario?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Dejará de mostrarse en esta instancia. El archivo de tu móvil no se borra.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.align(Alignment.End)) {
                            TextButton(onClick = { stage = MenuStage.MENU }) {
                                Text("Cancelar", color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            TextButton(onClick = {
                                stage = MenuStage.CLOSED
                                onRemove()
                            }) {
                                Text("Quitar", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Una opción del menú: icono y texto en una fila que se pulsa entera. */
@Composable
private fun MenuRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

/** ✕ abajo a la derecha, en el mismo sitio que el icono del botón cerrado. */
@Composable
private fun ColumnScope.CloseCorner(onClick: () -> Unit) {
    Box(
        modifier = Modifier.align(Alignment.End).size(56.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Close, contentDescription = "Cerrar opciones")
    }
}
