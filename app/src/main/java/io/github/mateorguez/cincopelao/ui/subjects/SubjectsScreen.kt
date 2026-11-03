package io.github.mateorguez.cincopelao.ui.subjects

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mateorguez.cincopelao.ui.theme.SubjectCardGradientStart
import io.github.mateorguez.cincopelao.ui.theme.SubjectCardGradientEnd
import io.github.mateorguez.cincopelao.ui.theme.DelightRegular
import io.github.mateorguez.cincopelao.ui.theme.LocalDarkTheme
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.mateorguez.cincopelao.R
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Pestaña de asignaturas. Una pulsación abre la asignatura; una pulsación larga muestra editar/eliminar. */
@Composable
fun SubjectsScreen(
    snackbarHostState: SnackbarHostState,
    viewModel: SubjectsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var expandedCardId by remember { mutableStateOf<Int?>(null) }

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
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(state.subjects, key = { it.subjectId }) { subject ->
                SubjectCard(
                    subject = subject,
                    titulation = state.titulation,
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
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubjectCard(
    subject: Subject,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onDelete: () -> Unit,
    titulation: String?,
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    val isDarkTheme = LocalDarkTheme.current

    val cardGradient = if (isDarkTheme) {
        Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.secondaryContainer
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                SubjectCardGradientStart,
                SubjectCardGradientEnd
            )
        )
    }


    Column(
        modifier = Modifier
            .animateContentSize()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .combinedClickable(
                    onClick = {
                        if (isExpanded) onCollapse()
                        else {
                            context.startActivity(SubjectActivity.intent(context, subject.subjectId))
                        }
                    },
                    onLongClick = {
                        if (!isExpanded) onExpand()
                    },
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(cardGradient)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = DelightRegular
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (!titulation.isNullOrBlank()) {
                            "${subject.course} $titulation"
                        } else {
                            subject.course
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = DelightRegular
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.zIndex(-1f)
        ) {

            val transitionOffset = (-8).dp
            val context = LocalContext.current

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = transitionOffset)
                    .height(60.dp)
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)) // Solo esquinas inferiores redondeadas
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(
                        top = 16.dp,
                        bottom = 8.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    onCollapse()
                    val intent = Intent(context, AddEditSubjectActivity::class.java)
                    intent.putExtra("subjectId", subject.subjectId)
                    context.startActivity(intent)
                }) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp), tint = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar", color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary)
                }

                TextButton(onClick = {
                    onCollapse()
                    showDeleteDialog = true
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Eliminar asignatura") },
                text = { Text("¿Estás seguro de que deseas eliminar '${subject.name}'?") },
                confirmButton = {
                    TextButton(onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        // onPrimary (blanco) no se veía sobre el diálogo en modo claro
                        Text("Cancelar", color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    }
}
