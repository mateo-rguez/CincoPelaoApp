package io.github.mateorguez.cincopelao.ui.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.mateorguez.cincopelao.data.local.entity.Task

/** Tarjeta de una tarea con subtareas: cabecera con el progreso y la lista de subtareas. */
@Composable
fun ParentTaskBlock(
    parentTask: Task,
    subtasks: List<Task>,
    onCheckedChange: (Task, Boolean) -> Unit,
    showInfoSheet: (Task) -> Unit,
    onArchive: () -> Unit,
) {
    TaskCardFrame(outerPadding = PaddingValues(4.dp), cornerRadius = 10.dp, contentPadding = 12.dp) {
        // Cabecera: título + resumen
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showInfoSheet(parentTask) }
                .animateContentSize()
        ) {
            Column(modifier = Modifier.weight(1f).height(48.dp)) {
                Text(text = parentTask.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                val completed = subtasks.count { it.isCompleted }
                val progress = "$completed de ${subtasks.size} completadas"
                Text(
                    text = parentTask.dueDate?.let { "${dueDateLabel(it)}, $progress" } ?: progress,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            AnimatedVisibility(
                visible = parentTask.isCompleted,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                ArchiveButton(onArchive)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), thickness = 0.5.dp)

        // Subtareas
        Column {
            val sortedSubtasks = subtasks.sortedBy { it.dueDate ?: Long.MAX_VALUE }
            sortedSubtasks.forEachIndexed { index, subtask ->
                TaskRow(
                    task = subtask,
                    modifier = Modifier.padding(horizontal = 0.dp),
                    onCheckedChange = { checked -> onCheckedChange(subtask, checked) },
                    onClick = { showInfoSheet(subtask) },
                    onArchive = { /* las subtareas se archivan con su madre */ }
                )
                if (index < sortedSubtasks.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), thickness = 0.5.dp)
                }
            }
        }
    }
}
