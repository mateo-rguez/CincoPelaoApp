package io.github.mateorguez.cincopelao.ui.appearance

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.mateorguez.cincopelao.data.prefs.TitleFont
import io.github.mateorguez.cincopelao.ui.theme.appColors
import io.github.mateorguez.cincopelao.ui.theme.displayName
import io.github.mateorguez.cincopelao.ui.theme.titleFontFamily

/**
 * Muestra de una letra de títulos: un nombre de asignatura y una nota escritos con ella, y debajo
 * su nombre (en la letra del texto, para que se lea igual en todas).
 */
@Composable
fun TitleFontCard(
    font: TitleFont,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    val borderWidth by animateDpAsState(if (selected) 3.dp else 1.dp, label = "borde")
    val family = titleFontFamily(font)

    Column(
        modifier = modifier
            .clip(shape)
            .border(
                borderWidth,
                if (selected) MaterialTheme.appColors.accent else MaterialTheme.colorScheme.outlineVariant,
                shape
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            "Álgebra",
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = family),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "7,42",
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = family),
            color = MaterialTheme.appColors.accent,
        )
        Spacer(Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 6.dp)
        ) {
            Text(
                font.displayName,
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
