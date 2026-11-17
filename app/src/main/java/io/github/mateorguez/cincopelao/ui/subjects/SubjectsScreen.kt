package io.github.mateorguez.cincopelao.ui.subjects

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
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
import androidx.compose.ui.unit.dp
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
                    Icon(
                        Icons.Default.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(110.dp)
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
                    SubjectCard(
                        summary = summary,
                        titulation = state.titulation,
                        showAlternate = showAlternate,
                        menuOpen = openMenuId == subject.subjectId,
                        onMenuOpenChange = { open -> openMenuId = if (open) subject.subjectId else null },
                        onDelete = {
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
                    )
                }
            }
        }
    }
}
