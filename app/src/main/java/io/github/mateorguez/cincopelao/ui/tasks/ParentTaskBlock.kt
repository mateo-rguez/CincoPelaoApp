package io.github.mateorguez.cincopelao.ui.tasks

import io.github.mateorguez.cincopelao.ui.theme.appColors
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.mateorguez.cincopelao.data.local.entity.Task
import io.github.mateorguez.cincopelao.ui.common.AnimatedColumnItems

/** Tarjeta de una tarea con subtareas: cabecera con el progreso y la lista de subtareas. */
@Composable
fun ParentTaskBlock(
    parentTask: Task,
    subtasks: List<Task>,
    onCheckedChange: (Task, Boolean) -> Unit,
    showInfoSheet: (Task) -> Unit,
    onArchive: () -> Unit,
) {
    TaskCardFrame(contentPadding = 12.dp) {
        // Cabecera: título + resumen
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showInfoSheet(parentTask) }
                .animateContentSize()
        ) {
            Column(modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text(text = parentTask.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    parentTask.dueDate?.let { DueDateChip(dueDate = it, isCompleted = parentTask.isCompleted) }
                    SubtaskProgress(done = subtasks.count { it.isCompleted }, total = subtasks.size)
                }
            }

            AnimatedVisibility(
                visible = parentTask.isCompleted,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                ArchiveButton(onArchive)
            }
        }

        SubtaskDivider()

        // Subtareas: al crearlas o borrarlas se despliegan y pliegan; el divisor va encima de
        // cada una salvo la primera, así se pliega junto con su subtarea
        val sortedSubtasks = subtasks.sortedBy { it.dueDate ?: Long.MAX_VALUE }
        Column {
            AnimatedColumnItems(sortedSubtasks, key = { it.taskId }) { subtask, isFirst ->
                Column {
                    if (!isFirst) SubtaskDivider()
                    TaskRow(
                        task = subtask,
                        modifier = Modifier,
                        onCheckedChange = { checked -> onCheckedChange(subtask, checked) },
                        onClick = { showInfoSheet(subtask) },
                        onArchive = { /* las subtareas se archivan con su madre */ }
                    )
                }
            }
        }
    }
}

/** Separador suave entre la tarea madre y sus subtareas, y entre subtarea y subtarea. */
@Composable
private fun SubtaskDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f), thickness = 1.dp)
}

/** "2/5" y una barra fina que se llena con el color de los controles de la paleta. */
@Composable
private fun SubtaskProgress(done: Int, total: Int) {
    val progress by animateFloatAsState(
        targetValue = if (total == 0) 0f else done.toFloat() / total,
        label = "progreso"
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.width(56.dp).height(5.dp).clip(RoundedCornerShape(50)),
            trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
        Text(text = "$done/$total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.appColors.muted)
    }
}
