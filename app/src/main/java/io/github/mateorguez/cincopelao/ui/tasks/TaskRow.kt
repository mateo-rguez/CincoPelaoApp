package io.github.mateorguez.cincopelao.ui.tasks

import io.github.mateorguez.cincopelao.ui.theme.appColors
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import io.github.mateorguez.cincopelao.data.local.entity.Task

/** Fila de una tarea: casilla, título (tachado animado al completarla) y fecha límite. */
@Composable
fun TaskRow(
    task: Task,
    modifier: Modifier,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onArchive: () -> Unit
) {
    val textColor by animateColorAsState(
        targetValue = if (task.isCompleted) Color.Gray else MaterialTheme.colorScheme.onBackground
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(modifier)
            .clickable { onClick() }
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.isCompleted, onCheckedChange = onCheckedChange)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                ScribbleStrikeThroughText(text = task.title, isChecked = task.isCompleted)
                task.dueDate?.let {
                    Text(text = dueDateLabel(it), style = MaterialTheme.typography.bodySmall, color = textColor)
                }
            }
        }

        AnimatedVisibility(
            visible = task.isCompleted && task.parentTaskId == null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            ArchiveButton(onArchive)
        }
    }
}

/** Botón de archivar una tarea completada. */
@Composable
fun ArchiveButton(onArchive: () -> Unit) {
    IconButton(onClick = onArchive) {
        Icon(
            imageVector = Icons.Default.Archive,
            contentDescription = "Archivar tarea",
            tint = MaterialTheme.appColors.accent
        )
    }
}

/** Texto que, al marcarse, se tacha con un trazo ondulado animado línea a línea. */
@Composable
fun ScribbleStrikeThroughText(
    text: String,
    isChecked: Boolean,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val animationProgress by animateFloatAsState(
        targetValue = if (isChecked) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )
    val textColor by animateColorAsState(
        targetValue = if (isChecked) Color.Gray else MaterialTheme.colorScheme.onBackground
    )
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Box(modifier = modifier) {
        Text(
            text = text,
            style = textStyle,
            color = textColor,
            onTextLayout = { textLayoutResult = it },
            modifier = Modifier.align(Alignment.CenterStart)
        )

        Canvas(modifier = Modifier.matchParentSize()) {
            val layoutResult = textLayoutResult ?: return@Canvas

            val waveAmplitude = 10f
            val waveLength = 50f
            val delayPerLine = 1f
            val totalDuration = 1f + delayPerLine * (layoutResult.lineCount - 1)

            for (lineIndex in 0 until layoutResult.lineCount) {
                val start = delayPerLine * lineIndex
                val lineProgress = ((animationProgress * totalDuration) - start).coerceIn(0f, 1f)

                val lineStart = layoutResult.getLineLeft(lineIndex)
                val lineEnd = layoutResult.getLineRight(lineIndex)
                val lineTop = layoutResult.getLineTop(lineIndex)
                val lineBottom = layoutResult.getLineBottom(lineIndex)
                val heightCenter = (lineTop + lineBottom) / 2f
                val length = (lineEnd - lineStart) * lineProgress

                val path = Path().apply {
                    moveTo(lineStart, heightCenter)
                    var x = lineStart
                    var up = true
                    while (x < lineStart + length) {
                        val controlY = if (up) heightCenter - waveAmplitude else heightCenter + waveAmplitude
                        val nextX = (x + waveLength).coerceAtMost(lineStart + length)
                        quadraticTo(x + waveLength / 2, controlY, nextX, heightCenter)
                        x = nextX
                        up = !up
                    }
                }

                drawPath(path = path, color = textColor, style = Stroke(width = 4f, cap = StrokeCap.Round))
            }
        }
    }
}
