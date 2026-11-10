package io.github.mateorguez.cincopelao.ui.appearance

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.mateorguez.cincopelao.data.prefs.AppPalette
import io.github.mateorguez.cincopelao.ui.theme.appColors
import io.github.mateorguez.cincopelao.ui.theme.displayName
import io.github.mateorguez.cincopelao.ui.theme.paletteColors

/**
 * Miniatura de la app pintada con [palette] (barra superior, una tarjeta de asignatura, una
 * tarea y la barra inferior), en el modo que se ve ahora, para comparar paletas de un vistazo.
 */
@Composable
fun PalettePreviewCard(
    palette: AppPalette,
    dark: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = paletteColors(palette, dark)
    val scheme = colors.scheme
    val app = colors.app
    val shape = RoundedCornerShape(16.dp)
    val borderWidth by animateDpAsState(if (selected) 3.dp else 1.dp, label = "borde")

    Column(
        modifier = modifier
            .clip(shape)
            .border(
                borderWidth,
                if (selected) MaterialTheme.appColors.accent else MaterialTheme.colorScheme.outlineVariant,
                shape
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .background(scheme.background)
    ) {
        // Barra superior
        Box(
            Modifier
                .fillMaxWidth()
                .height(22.dp)
                .background(scheme.primary)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(Modifier.width(36.dp).height(5.dp).clip(CircleShape).background(scheme.onPrimary))
        }

        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Tarjeta de asignatura con su degradado
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.horizontalGradient(listOf(app.subjectCardStart, app.subjectCardEnd)))
            )
            // Una tarea: casilla con el acento y una línea de texto
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(RoundedCornerShape(2.dp)).background(app.accent))
                Spacer(Modifier.width(6.dp))
                Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(scheme.onBackground.copy(alpha = 0.7f)))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(RoundedCornerShape(2.dp)).background(app.timelineTaskContainer))
                Spacer(Modifier.width(6.dp))
                Box(Modifier.fillMaxWidth(0.6f).height(5.dp).clip(CircleShape).background(scheme.onBackground.copy(alpha = 0.4f)))
            }
        }

        // Barra inferior con un icono elegido y dos sin elegir
        Row(
            Modifier
                .fillMaxWidth()
                .height(18.dp)
                .background(scheme.primary),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(scheme.onPrimary))
            Box(Modifier.size(6.dp).clip(CircleShape).background(app.navBarUnselected))
            Box(Modifier.size(6.dp).clip(CircleShape).background(app.navBarUnselected))
        }

        // Nombre (con los colores de la app, no de la paleta, para que se lea igual en todas)
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                palette.displayName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Elegida",
                    tint = MaterialTheme.appColors.accent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
