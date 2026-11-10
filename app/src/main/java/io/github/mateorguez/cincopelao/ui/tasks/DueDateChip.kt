package io.github.mateorguez.cincopelao.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.mateorguez.cincopelao.ui.theme.appColors

/** Opacidad del fondo de la etiqueta de la fecha (el mismo tono que su texto, muy suave). */
const val DUE_CHIP_ALPHA = 0.15f

/**
 * Etiqueta con la fecha límite de una tarea: roja si venció, naranja si es hoy o mañana y
 * neutra si queda tiempo. Una tarea completada la muestra apagada, ya no apremia.
 */
@Composable
fun DueDateChip(dueDate: Long, isCompleted: Boolean, modifier: Modifier = Modifier) {
    val urgency = dueUrgency(dueDate)
    val tone = when {
        isCompleted -> Color.Gray
        urgency == DueUrgency.OVERDUE -> MaterialTheme.appColors.dueOverdue
        urgency == DueUrgency.SOON -> MaterialTheme.appColors.dueSoon
        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
    }
    val background = if (urgency == DueUrgency.LATER || isCompleted) {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f)
    } else {
        tone.copy(alpha = DUE_CHIP_ALPHA)
    }
    Row(
        modifier = modifier
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = if (urgency == DueUrgency.OVERDUE && !isCompleted) Icons.Outlined.ErrorOutline else Icons.Outlined.Event,
            contentDescription = null,
            tint = tone,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = dueDateLabel(dueDate),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (urgency == DueUrgency.LATER || isCompleted) FontWeight.Normal else FontWeight.SemiBold,
            color = tone
        )
    }
}
