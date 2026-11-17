package io.github.mateorguez.cincopelao.ui.theme

import androidx.compose.material3.ColorScheme
import io.github.mateorguez.cincopelao.data.prefs.AppPalette

/**
 * Colores completos de una paleta en un modo: los de Material y los propios de la app.
 *
 * Cómo añadir una paleta: copiar un archivo `PaletteXxx.kt`, cambiar los valores de sus dos
 * modos (claro y oscuro), añadirla a [AppPalette], a [paletteColors] y a [displayName], y pasar
 * `PalettesTest`, que comprueba el contraste de textos, etiquetas y gráficos. Las pantallas solo
 * leen estas variables (`MaterialTheme.colorScheme` y `MaterialTheme.appColors`): ningún color
 * se decide fuera de las paletas ni depende de si el modo es oscuro.
 */
data class PaletteColors(val scheme: ColorScheme, val app: AppColors)

fun paletteColors(palette: AppPalette, dark: Boolean): PaletteColors = when (palette) {
    AppPalette.CLASSIC -> if (dark) classicDark else classicLight
    AppPalette.GRAPHITE -> if (dark) graphiteDark else graphiteLight
    AppPalette.CATPPUCCIN -> if (dark) catppuccinDark else catppuccinLight
    AppPalette.ROSE_PINE -> if (dark) rosePineDark else rosePineLight
    AppPalette.GRUVBOX -> if (dark) gruvboxDark else gruvboxLight
    AppPalette.FLEXOKI -> if (dark) flexokiDark else flexokiLight
    AppPalette.TOKYO_NIGHT -> if (dark) tokyoNightDark else tokyoNightLight
    AppPalette.KANAGAWA -> if (dark) kanagawaDark else kanagawaLight
    AppPalette.CHALKBOARD -> if (dark) chalkboardDark else chalkboardLight
    AppPalette.NOTEBOOK -> if (dark) notebookDark else notebookLight
    AppPalette.LIBRARY -> if (dark) libraryDark else libraryLight
}

/** Nombre visible de cada paleta. */
val AppPalette.displayName: String
    get() = when (this) {
        AppPalette.CLASSIC -> "Clásico"
        AppPalette.GRAPHITE -> "Grafito"
        AppPalette.CATPPUCCIN -> "Catppuccin"
        AppPalette.ROSE_PINE -> "Rosé Pine"
        AppPalette.GRUVBOX -> "Gruvbox"
        AppPalette.FLEXOKI -> "Flexoki"
        AppPalette.TOKYO_NIGHT -> "Tokyo Night"
        AppPalette.KANAGAWA -> "Kanagawa"
        AppPalette.CHALKBOARD -> "Pizarra"
        AppPalette.NOTEBOOK -> "Cuaderno"
        AppPalette.LIBRARY -> "Biblioteca"
    }
