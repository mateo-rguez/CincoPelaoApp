package io.github.mateorguez.cincopelao.ui.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.mateorguez.cincopelao.ui.tasks.DUE_CHIP_ALPHA

/** Etiqueta redondeada con icono opcional; con [tone] se tiñe de ese color, sin él es neutra. */
@Composable
internal fun InfoChip(icon: ImageVector?, text: String, tone: Color? = null, modifier: Modifier = Modifier) {
    val onBackground = MaterialTheme.colorScheme.onBackground
    val content = tone ?: onBackground.copy(alpha = 0.75f)
    val background = tone?.copy(alpha = DUE_CHIP_ALPHA) ?: onBackground.copy(alpha = 0.07f)
    Row(
        modifier = modifier
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (tone != null) FontWeight.SemiBold else FontWeight.Normal,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
