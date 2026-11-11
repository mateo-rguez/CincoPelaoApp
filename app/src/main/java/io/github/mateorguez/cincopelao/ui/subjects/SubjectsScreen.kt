package io.github.mateorguez.cincopelao.ui.subjects

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mateorguez.cincopelao.ui.theme.DelightRegular
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Cada cuánto alternan las etiquetas de las tarjetas (nota y lo siguiente que hacer). */
private const val CHIP_ROTATION_MILLIS = 5_000L

/** Pestaña de asignaturas. Una pulsación abre la asignatura; una pulsación larga muestra editar/eliminar. */
@Composable
fun SubjectsScreen(
    snackbarHostState: SnackbarHostState,
    viewModel: SubjectsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var expandedCardId by remember { mutableStateOf<Long?>(null) }

    // Un solo reloj para todas las tarjetas: sus etiquetas cambian a la vez
    var showAlternate by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(CHIP_ROTATION_MILLIS)
            showAlternate = !showAlternate
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Tocar fuera de la tarjeta desplegada la pliega
            .pointerInput(expandedCardId) {
                if (expandedCardId != null) {
                    detectTapGestures(onTap = { expandedCardId = null })
                }
            }
    ) {
        if (state.subjects.isEmpty()) {
            Text(
                text = "Aún no hay asignaturas.\nPulsa + para añadir la primera.",
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = DelightRegular,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.Center).padding(32.dp)
            )
        }
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
                    isExpanded = expandedCardId == subject.subjectId,
                    onExpand = { expandedCardId = subject.subjectId },
                    onCollapse = { expandedCardId = null },
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
