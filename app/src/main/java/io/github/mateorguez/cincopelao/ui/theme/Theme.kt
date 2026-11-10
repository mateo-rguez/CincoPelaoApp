package io.github.mateorguez.cincopelao.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.mateorguez.cincopelao.data.prefs.AppPalette
import io.github.mateorguez.cincopelao.data.prefs.ThemeSettings

/**
 * Si la app está en modo oscuro. Lo publica [CincoPelaoTheme]. Para colores, mejor
 * [appColors] o el `colorScheme`: así cada paleta decide sus tonos.
 */
val LocalDarkTheme = staticCompositionLocalOf { false }

/** Colores propios de la paleta activa (acento, tarjetas, calendario…). */
val MaterialTheme.appColors: AppColors
    @Composable @ReadOnlyComposable get() = LocalAppColors.current

@Composable
fun CincoPelaoTheme(
    darkTheme: Boolean,
    palette: AppPalette = AppPalette.CLASSIC,
    content: @Composable () -> Unit
) {
    val colors = paletteColors(palette, darkTheme)
    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalAppColors provides colors.app,
    ) {
        MaterialTheme(
            colorScheme = colors.scheme,
            typography = Typography,
            content = content
        )
    }
}

/** Tema de la app con la apariencia que haya elegido el usuario. Lo usa cada Activity. */
@Composable
fun CincoPelaoAppTheme(content: @Composable () -> Unit) {
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val settings by themeViewModel.settings.collectAsState()
    val current = settings ?: ThemeSettings()
    CincoPelaoTheme(
        darkTheme = current.isDark(isSystemInDarkTheme()),
        palette = current.palette,
        content = content
    )
}
