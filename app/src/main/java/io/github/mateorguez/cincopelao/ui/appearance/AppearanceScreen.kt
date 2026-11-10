package io.github.mateorguez.cincopelao.ui.appearance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mateorguez.cincopelao.data.prefs.AppPalette
import io.github.mateorguez.cincopelao.data.prefs.ThemeMode
import io.github.mateorguez.cincopelao.ui.theme.LocalDarkTheme
import io.github.mateorguez.cincopelao.ui.theme.appColors

private val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "Sistema"
        ThemeMode.LIGHT -> "Claro"
        ThemeMode.DARK -> "Oscuro"
    }

private val ThemeMode.icon: ImageVector
    get() = when (this) {
        ThemeMode.SYSTEM -> Icons.Default.PhoneAndroid
        ThemeMode.LIGHT -> Icons.Default.LightMode
        ThemeMode.DARK -> Icons.Default.DarkMode
    }

/** Modo claro/oscuro y paleta de colores. Los cambios se ven al momento. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(viewModel: AppearanceViewModel = hiltViewModel()) {
    val settings by viewModel.uiState.collectAsStateWithLifecycle()
    // Las miniaturas se pintan en el modo que se ve ahora (con «Sistema», el del móvil)
    val dark = LocalDarkTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SectionTitle("Modo")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = settings.mode == mode,
                    onClick = { viewModel.setMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                    icon = { SegmentedButtonDefaults.Icon(settings.mode == mode) { Icon(mode.icon, null) } },
                    label = { Text(mode.label) }
                )
            }
        }
        Text(
            text = if (settings.mode == ThemeMode.SYSTEM) {
                "Claro u oscuro según el ajuste del móvil."
            } else {
                "Siempre en modo ${settings.mode.label.lowercase()}, aunque el móvil cambie."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
        )

        Spacer(Modifier.height(28.dp))

        SectionTitle("Colores")
        // Rejilla de dos columnas; con pocos elementos no hace falta una LazyGrid
        AppPalette.entries.chunked(2).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                row.forEach { palette ->
                    PalettePreviewCard(
                        palette = palette,
                        dark = dark,
                        selected = settings.palette == palette,
                        onClick = { viewModel.setPalette(palette) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.appColors.accent,
        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
    )
}
