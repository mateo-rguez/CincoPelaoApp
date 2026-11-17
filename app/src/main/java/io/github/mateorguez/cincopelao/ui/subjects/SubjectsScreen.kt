package io.github.mateorguez.cincopelao.ui.subjects

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import io.github.mateorguez.cincopelao.ui.theme.appColors
import io.github.mateorguez.cincopelao.ui.common.EmptyState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.mateorguez.cincopelao.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Cada cuánto alternan las etiquetas de las tarjetas (nota y lo siguiente que hacer). */
private const val CHIP_ROTATION_MILLIS = 5_000L

/** Pestaña de asignaturas. Una pulsación abre la asignatura; el «⋮» de cada tarjeta, editar/eliminar. */
@Composable
fun SubjectsScreen(
    snackbarHostState: SnackbarHostState,
    onAddSubject: () -> Unit,
    onOpenSharing: () -> Unit,
    /** Avisa de si no hay ninguna asignatura, para ocultar el «＋» flotante (repetiría el botón). */
    onEmptyChange: (Boolean) -> Unit,
    viewModel: SubjectsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isEmpty = state.loaded && state.subjects.isEmpty()
    LaunchedEffect(isEmpty) { onEmptyChange(isEmpty) }
    val scope = rememberCoroutineScope()
    // Un solo reloj para todas las tarjetas: sus etiquetas cambian a la vez
    var showAlternate by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(CHIP_ROTATION_MILLIS)
            showAlternate = !showAlternate
        }
    }

    when {
        !state.loaded -> Unit // aún sin leer: ni lista ni aviso
        state.subjects.isEmpty() -> {
            EmptyState(
                title = "Aún no hay asignaturas",
                text = "Añade la primera o recíbelas de un compañero.",
                illustration = {
                    // Ilustración blanca sobre transparente: se tiñe con el color de la paleta
                    Icon(
                        painterResource(R.drawable.empty_subjects),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(145.dp)
                    )
                }
            ) {
                // Los dos botones igual de anchos: tanto como el más largo
                Column(Modifier.width(IntrinsicSize.Max)) {
                    Button(onClick = onAddSubject, modifier = Modifier.fillMaxWidth()) { Text("Añadir asignatura") }
                    Spacer(Modifier.height(8.dp))
                    // Secundario: fondo suave de acento, lleva a la pestaña de compartir
                    val accent = MaterialTheme.appColors.accent
                    FilledTonalButton(
                        onClick = onOpenSharing,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = accent.copy(alpha = 0.15f),
                            contentColor = accent
                        )
                    ) { Text("Recibir de un compañero") }
                }
            }
        }
        else -> {
            // Solo una tarjeta con la franja de acciones abierta; atrás la cierra
            var openMenuId by rememberSaveable { mutableStateOf<Long?>(null) }
            BackHandler(enabled = openMenuId != null) { openMenuId = null }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // Abajo deja sitio al botón de añadir para que no tape la última tarjeta
                contentPadding = PaddingValues(top = 10.dp, bottom = 88.dp)
            ) {
                items(state.subjects, key = { it.subject.subjectId }) { summary ->
                    val subject = summary.subject
                    // Al confirmar el borrado la tarjeta se encoge y desvanece, y solo entonces se
                    // borra (las demás suben a su sitio con animateItem); al deshacer, vuelve a aparecer
                    val visible = remember { MutableTransitionState(true) }
                    LaunchedEffect(visible.isIdle, visible.currentState) {
                        if (visible.isIdle && !visible.currentState) {
                            scope.launch {
                                val deleted = viewModel.delete(subject)
                                val result = snackbarHostState.showSnackbar(
                                    message = "${subject.name} ha sido eliminada",
                                    actionLabel = "Deshacer",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(deleted)
                            }
                        }
                    }
                    AnimatedVisibility(
                        visibleState = visible,
                        enter = fadeIn(),
                        exit = scaleOut(tween(250), targetScale = 0.85f) + fadeOut(tween(250)) +
                            shrinkVertically(tween(300, delayMillis = 150)),
                        modifier = Modifier.animateItem()
                    ) {
                        SubjectCard(
                            summary = summary,
                            titulation = state.titulation,
                            showAlternate = showAlternate,
                            menuOpen = openMenuId == subject.subjectId,
                            onMenuOpenChange = { open -> openMenuId = if (open) subject.subjectId else null },
                            onDelete = {
                                openMenuId = null
                                visible.targetState = false
                            }
                        )
                    }
                }
            }
        }
    }
}
