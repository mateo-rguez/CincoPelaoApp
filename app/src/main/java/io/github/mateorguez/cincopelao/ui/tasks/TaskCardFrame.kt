package io.github.mateorguez.cincopelao.ui.tasks

import io.github.mateorguez.cincopelao.ui.theme.appColors
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Margen alrededor de cada tarjeta: deja sitio a la sombra y separa una tarjeta de otra. */
val TaskCardOuterPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp)

/**
 * Tarjeta de tareas: fondo propio de la paleta, sombra suave teñida con su tono y un brillo
 * fino en el borde que se desvanece en diagonal.
 */
@Composable
fun TaskCardFrame(
    outerPadding: PaddingValues = TaskCardOuterPadding,
    cornerRadius: Dp = 16.dp,
    contentPadding: Dp = 0.dp,
    /** Se aplica dentro del recorte de la tarjeta (p. ej. para hacerla pulsable). */
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val colors = MaterialTheme.appColors
    val tint = colors.taskCardTint
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(outerPadding)
            .shadow(elevation = 6.dp, shape = shape, ambientColor = tint.copy(alpha = 0.4f), spotColor = tint.copy(alpha = 0.5f))
            .background(colors.taskCard, shape)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(tint.copy(alpha = 0.25f), tint.copy(alpha = 0.03f))),
                shape = shape
            )
            .clip(shape)
            .then(modifier)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}
