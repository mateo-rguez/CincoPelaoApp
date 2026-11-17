package io.github.mateorguez.cincopelao.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mateorguez.cincopelao.ui.theme.appColors

/** Diámetro del círculo de la ilustración. */
val EmptyStateCircleSize = 180.dp

/**
 * Pantalla vacía o de bienvenida: ilustración en un círculo suave, título, texto y acciones, todo
 * centrado. La comparten el Horario sin horario, Asignaturas sin asignaturas y la pestaña de
 * compartir, para que se vean iguales y se cambien en un solo sitio.
 *
 * Con [waves], salen ondas del círculo (como el icono de «Anunciándose» de compartir). Se
 * dibujan por fuera del círculo sin ocupar sitio, así que no cambian la maquetación.
 */
@Composable
fun EmptyState(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.appColors.accent,
    waves: Boolean = false,
    /** Hueco entre el texto y las acciones. */
    actionsSpacing: Dp = 24.dp,
    /** Márgenes arriba y abajo (a los lados siempre 32 dp). */
    topPadding: Dp = 24.dp,
    bottomPadding: Dp = 24.dp,
    illustration: @Composable BoxScope.() -> Unit,
    actions: @Composable ColumnScope.() -> Unit = {}
) {
    // Centrado en vertical, pero con scroll si no cabe (pantallas bajas o letra grande)
    BoxWithConstraints(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .fillMaxWidth()
                .padding(start = 32.dp, end = 32.dp, top = topPadding, bottom = bottomPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(EmptyStateCircleSize)
                    .then(if (waves) Modifier.waves(accent) else Modifier)
                    .background(accent.copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center,
                content = illustration
            )
            Spacer(Modifier.height(28.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(actionsSpacing))
            actions()
        }
    }
}

/** Dos ondas que nacen en el borde del círculo y se desvanecen al alejarse. */
@Composable
private fun Modifier.waves(color: Color): Modifier {
    val t by rememberInfiniteTransition(label = "ondas").animateFloat(
        0f, 1f, infiniteRepeatable(tween(3_200, easing = LinearEasing)), label = "fase"
    )
    return drawBehind {
        val radius = size.minDimension / 2
        repeat(2) { i ->
            val phase = (t + i / 2f) % 1f
            drawCircle(
                color = color.copy(alpha = (1f - phase) * 0.35f),
                radius = radius * (1f + 0.22f * phase),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
