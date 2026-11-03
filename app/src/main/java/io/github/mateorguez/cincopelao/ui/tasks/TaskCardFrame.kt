package io.github.mateorguez.cincopelao.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mateorguez.cincopelao.ui.theme.LocalDarkTheme
import io.github.mateorguez.cincopelao.ui.theme.TaskCardTintLight

/** Colores del borde degradado de las tarjetas de tareas. */
@Composable
fun taskCardBorderColors(): List<Color> =
    if (LocalDarkTheme.current) {
        listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.05f))
    } else {
        listOf(TaskCardTintLight.copy(alpha = 0.3f), TaskCardTintLight.copy(alpha = 0.05f))
    }

/**
 * Marco "de cristal" de las tarjetas de tareas: una capa difuminada con borde degradado
 * detrás y, encima, el contenido con un borde fino.
 */
@Composable
fun TaskCardFrame(
    outerPadding: PaddingValues,
    cornerRadius: Dp,
    contentPadding: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(outerPadding)
                .clip(shape)
                .background(Color.White.copy(alpha = 0.05f))
                .blur(16.dp)
                .border(width = 1.dp, brush = Brush.linearGradient(colors = taskCardBorderColors()), shape = shape)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(outerPadding)
                .border(width = 1.dp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), shape = shape)
                .clip(shape)
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}
