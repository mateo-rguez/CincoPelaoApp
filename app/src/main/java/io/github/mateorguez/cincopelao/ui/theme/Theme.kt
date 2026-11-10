package io.github.mateorguez.cincopelao.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.mateorguez.cincopelao.data.prefs.ThemeSettings

private val DarkColorScheme = darkColorScheme(
    primary = AzulFuerte,
    onPrimary = Blanco,
    background = AzulOscuro,
    onBackground = Blanco,
    surface = SurfaceDark,
    onSurface = Blanco,
    primaryContainer = PrimaryContainerDark,
)

private val LightColorScheme = lightColorScheme(
    primary = AzulFuerte,
    onPrimary = Blanco,
    background = Blanco,
    onBackground = Negro,
    surface = Blanco,
    onSurface = Negro,
    primaryContainer = PrimaryContainerLight,
)

/** Si la app está en modo oscuro. Lo publica [CincoPelaoTheme] para que las pantallas no lo lean de DataStore. */
val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun CincoPelaoTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
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
    val darkTheme = (settings ?: ThemeSettings()).isDark(isSystemInDarkTheme())
    CincoPelaoTheme(darkTheme = darkTheme, content = content)
}
