package io.github.mateorguez.cincopelao.ui.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.local.entity.Task
import io.github.mateorguez.cincopelao.ui.theme.DelightRegular

/** Formulario abierto: la tarea a editar (o null si es nueva) y el bloque en el que se crea. */
private data class TaskFormTarget(val task: Task?, val subject: Subject?)

/** Pestaña de tareas pendientes: un bloque de tareas generales y uno por asignatura. */
@Composable
fun TasksScreen(viewModel: TasksViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Bloques plegados/desplegados por el usuario (por defecto, desplegados si tienen tareas)
    val expandedGroups = remember { mutableStateMapOf<Long, Boolean>() }
    var formTarget by remember { mutableStateOf<TaskFormTarget?>(null) }
    var infoTaskId by remember { mutableStateOf<Long?>(null) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }

    // Ficha de información de una tarea (se refresca sola si la tarea cambia)
    infoTaskId?.let { state.task(it) }?.let { task ->
        TaskInfoSheet(
            task = task,
            enableCheckbox = !state.hasSubtasks(task),
            showEdit = true,
            onDismiss = { infoTaskId = null },
            onEdit = {
                val subject = state.groups.firstOrNull { it.subject?.subjectId == task.subjectId }?.subject
                formTarget = TaskFormTarget(task, subject)
            },
            onDelete = { taskToDelete = task },
            onCheckbox = { checked -> viewModel.setCompleted(task, checked) }
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { SectionDivider() }

        itemsIndexed(state.groups, key = { _, group -> group.key }) { index, group ->
            TaskGroupSection(
                group = group,
                isExpanded = expandedGroups[group.key] ?: group.tasks.isNotEmpty(),
                onToggleExpanded = { expandedGroups[group.key] = !(expandedGroups[group.key] ?: group.tasks.isNotEmpty()) },
                onAddTask = { formTarget = TaskFormTarget(null, group.subject) },
                onCheckedChange = viewModel::setCompleted,
                onShowInfo = { infoTaskId = it.taskId },
                onArchive = viewModel::archive
            )
            // Separador debajo de las generales y entre asignaturas (no después de la última)
            if (index == 0 || index < state.groups.lastIndex) SectionDivider()
        }

        item { SectionDivider() }
    }

    formTarget?.let { target ->
        TaskFormSheet(
            task = target.task,
            subject = target.subject,
            parentCandidates = state.allTasks.filter { it.subjectId == target.subject?.subjectId },
            onSave = { title, description, dueDate, parentTaskId ->
                viewModel.save(target.task, target.subject, title, description, dueDate, parentTaskId)
                formTarget = null
            },
            onDismiss = { formTarget = null }
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
private fun SectionDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), thickness = 0.5.dp)
}

/** Bloque plegable con las tareas de una asignatura (o las generales). */
@Composable
private fun TaskGroupSection(
    group: TaskGroup,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onAddTask: () -> Unit,
    onCheckedChange: (Task, Boolean) -> Unit,
    onShowInfo: (Task) -> Unit,
    onArchive: (Task, List<Task>) -> Unit
) {
    val subject = group.subject
    Column(modifier = Modifier.clip(RoundedCornerShape(12.dp))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded)
                .padding(vertical = 2.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = subject?.name ?: "Tareas Generales",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = DelightRegular),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onAddTask) {
                Icon(Icons.Default.Add, contentDescription = if (subject == null) "Añadir tarea general" else "Añadir tarea")
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = "Expandir o colapsar"
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column {
                if (group.tasks.isEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        Text(
                            text = if (subject == null) "No hay tareas generales." else "No hay tareas para ${subject.name}.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    group.tasks.forEach { task ->
                        val subtasks = group.tasks.filter { it.parentTaskId == task.taskId }
                        when {
                            subtasks.isNotEmpty() -> ParentTaskBlock(
                                parentTask = task,
                                subtasks = subtasks,
                                onCheckedChange = onCheckedChange,
                                showInfoSheet = onShowInfo,
                                onArchive = { onArchive(task, subtasks) }
                            )
                            // Las subtareas se muestran dentro de su tarea madre
                            task.parentTaskId == null -> TaskCardFrame {
                                TaskRow(
                                    task = task,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    onCheckedChange = { checked -> onCheckedChange(task, checked) },
                                    onClick = { onShowInfo(task) },
                                    onArchive = { onArchive(task, emptyList()) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Confirmación de borrado; si la tarea tiene subtareas, permite conservarlas. */
@Composable
fun DeleteTaskDialog(
    hasSubtasks: Boolean,
    onDeleteOnlyThis: () -> Unit,
    onDeleteAll: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar tarea") },
        text = {
            Text(
                if (hasSubtasks) "Esta tarea tiene subtareas. ¿Qué te gustaría hacer?"
                else "¿Estás seguro de que quieres eliminar esta tarea?"
            )
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.End) {
                if (hasSubtasks) {
                    TextButton(onClick = { onDeleteOnlyThis(); onDismiss() }) { Text("Eliminar solo esta") }
                    TextButton(onClick = { onDeleteAll(); onDismiss() }) { Text("Eliminar tarea y subtareas") }
                } else {
                    TextButton(onClick = { onDeleteAll(); onDismiss() }) { Text("Eliminar") }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
        dismissButton = {}
    )
}
