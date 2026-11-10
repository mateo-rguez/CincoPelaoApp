package io.github.mateorguez.cincopelao.ui.tasks

import io.github.mateorguez.cincopelao.ui.theme.appColors
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mateorguez.cincopelao.data.local.entity.Task
import io.github.mateorguez.cincopelao.ui.theme.DelightRegular
import kotlinx.coroutines.launch

/** Pestaña de tareas completadas (archivadas): se pueden restaurar o borrar. */
@Composable
fun CompletedTasksScreen(
    snackbarHostState: SnackbarHostState,
    viewModel: CompletedTasksViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var infoTaskId by remember { mutableStateOf<Long?>(null) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    /** Borra con opción de deshacer desde el snackbar. */
    fun deleteWithUndo(task: Task, message: String) {
        scope.launch {
            val deleted = viewModel.deleteWithSubtasks(task)
            val result = snackbarHostState.showSnackbar(message, actionLabel = "Deshacer", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(deleted)
        }
    }

    infoTaskId?.let { state.task(it) }?.let { task ->
        TaskInfoSheet(
            task = task,
            enableCheckbox = false,
            showEdit = false,
            onDismiss = { infoTaskId = null },
            onEdit = {},
            onDelete = { taskToDelete = task },
            onCheckbox = {}
        )
    }

    AnimatedVisibility(
        visible = state.isLoaded && state.items.isEmpty(),
        enter = fadeIn(tween(500)) + slideInVertically { it / 2 },
        exit = fadeOut(tween(300)) + slideOutVertically { it / 2 }
    ) {
        EmptyCompletedTasks()
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), thickness = 0.5.dp)
        }

        item {
            CompletedTasksHeader(
                showDeleteAll = state.archivedTasks.isNotEmpty(),
                onDeleteAll = { showDeleteAllDialog = true }
            )
        }

        items(state.items, key = { it.task.taskId }) { item ->
            Box(modifier = Modifier.animateItem()) {
                if (item.subtasks.isNotEmpty()) {
                    ArchivedParentTaskBlock(
                        item = item,
                        showInfoSheet = { infoTaskId = it.taskId },
                        onRestore = { viewModel.restore(item) },
                        onDelete = { deleteWithUndo(item.task, "Tarea y subtareas eliminadas") }
                    )
                } else {
                    TaskCardFrame(outerPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp), cornerRadius = 12.dp) {
                        ArchivedTaskRow(
                            task = item.task,
                            subjectAbbrev = item.subjectAbbrev,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            onClick = { infoTaskId = item.task.taskId },
                            onRestore = { viewModel.restore(item) },
                            onDelete = { deleteWithUndo(item.task, "Tarea eliminada") }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Eliminar todas las tareas archivadas") },
            text = { Text("¿Estás seguro de que quieres eliminar todas las tareas archivadas?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAllArchived()
                    showDeleteAllDialog = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("Cancelar") }
            }
        )
    }

    taskToDelete?.let { task ->
        DeleteTaskDialog(
            hasSubtasks = state.allTasks.any { it.parentTaskId == task.taskId },
            onDeleteOnlyThis = { viewModel.deleteKeepingSubtasks(task) },
            onDeleteAll = { viewModel.delete(task) },
            onDismiss = { taskToDelete = null }
        )
    }
}

@Composable
private fun EmptyCompletedTasks() {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "😘", fontSize = 64.sp, modifier = Modifier.padding(bottom = 16.dp))
            Text(
                text = "No hay tareas completadas por ahora.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CompletedTasksHeader(showDeleteAll: Boolean, onDeleteAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 12.dp)
            .height(43.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Tareas Completadas",
            fontFamily = DelightRegular,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(1f))
        if (showDeleteAll) {
            TintedIconButton(Icons.Default.DeleteSweep, "Eliminar todas", onDeleteAll)
        }
    }
}

/** Botón de icono con el color de acento del tema (blanco en modo oscuro). */
@Composable
private fun TintedIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.appColors.accent
        )
    }
}

/** Línea secundaria de una tarea archivada: "[ABR] Completada hace 2 días". */
private fun completedSubtitle(task: Task, subjectAbbrev: String?): String =
    "${subjectAbbrev?.let { "[$it] " } ?: ""}Completada ${completedLabel(task.completedAt)}"

@Composable
private fun ArchivedParentTaskBlock(
    item: ArchivedTaskItem,
    showInfoSheet: (Task) -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    TaskCardFrame(outerPadding = PaddingValues(4.dp), cornerRadius = 10.dp, contentPadding = 12.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showInfoSheet(item.task) }
                .animateContentSize()
        ) {
            Column(modifier = Modifier.weight(1f).height(48.dp)) {
                Text(text = item.task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = completedSubtitle(item.task, item.subjectAbbrev),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            TintedIconButton(Icons.AutoMirrored.Filled.Undo, "Restaurar tarea", onRestore)
            TintedIconButton(Icons.Default.Delete, "Eliminar tarea", onDelete)
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), thickness = 0.5.dp)

        Column {
            val sortedSubtasks = item.subtasks.sortedBy { it.dueDate ?: Long.MAX_VALUE }
            sortedSubtasks.forEachIndexed { index, subtask ->
                ArchivedTaskRow(
                    task = subtask,
                    subjectAbbrev = null,
                    modifier = Modifier.padding(horizontal = 0.dp),
                    onClick = { showInfoSheet(subtask) },
                    onRestore = {},
                    onDelete = {}
                )
                if (index < sortedSubtasks.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), thickness = 0.5.dp)
                }
            }
        }
    }
}

/** Fila de una tarea archivada; las subtareas no tienen botones (se gestionan con su madre). */
@Composable
private fun ArchivedTaskRow(
    task: Task,
    modifier: Modifier,
    subjectAbbrev: String?,
    onClick: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(modifier)
            .clickable { onClick() }
            .animateContentSize()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(task.title)
                Text(text = completedSubtitle(task, subjectAbbrev), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }

        if (task.parentTaskId == null) {
            Row {
                TintedIconButton(Icons.AutoMirrored.Filled.Undo, "Restaurar tarea", onRestore)
                TintedIconButton(Icons.Default.Delete, "Eliminar tarea", onDelete)
            }
        }
    }
}
