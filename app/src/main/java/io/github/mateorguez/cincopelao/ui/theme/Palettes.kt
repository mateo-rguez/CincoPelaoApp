package io.github.mateorguez.cincopelao.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.mateorguez.cincopelao.data.prefs.AppPalette

/**
 * Colores completos de una paleta en un modo: los de Material y los propios de la app.
 *
 * Cómo añadir una paleta: copiar un archivo `PaletteXxx.kt`, cambiar los valores de sus dos
 * modos (claro y oscuro), añadirla a [AppPalette], a [paletteColors] y a [displayName], y pasar
 * `PalettesTest`, que comprueba el contraste de textos, etiquetas y gráficos. Las pantallas solo
 * leen estas variables (`MaterialTheme.colorScheme` y `MaterialTheme.appColors`): ningún color
 * se decide fuera de las paletas ni depende de si el modo es oscuro.
 *
 * La excepción es «De tu móvil» (`PaletteSystem.kt`): sus colores se calculan a partir de los
 * que genera Android con el fondo de pantalla.
 */
data class PaletteColors(val scheme: ColorScheme, val app: AppColors)

/**
 * Colores de [palette]. Para «De tu móvil» hace falta el esquema que genera Android
 * ([generated]); sin él (Android 11 o anterior) se usa Clásico.
 */
fun paletteColors(palette: AppPalette, dark: Boolean, generated: ColorScheme? = null): PaletteColors = when (palette) {
    AppPalette.SYSTEM -> generated?.let { systemPalette(it, dark) } ?: paletteColors(AppPalette.CLASSIC, dark)
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

/** Colores de [palette] en este móvil (con el esquema del sistema si hace falta). */
@Composable
fun rememberPaletteColors(palette: AppPalette, dark: Boolean): PaletteColors {
    val generated = if (palette == AppPalette.SYSTEM) systemColorScheme(dark) else null
    return remember(palette, dark, generated) { paletteColors(palette, dark, generated) }
}

/** Paletas que se pueden elegir en este móvil: «De tu móvil» necesita Android 12. */
val availablePalettes: List<AppPalette>
    get() = AppPalette.entries.filter { it != AppPalette.SYSTEM || systemPaletteAvailable }

/** Nombre visible de cada paleta. */
val AppPalette.displayName: String
    get() = when (this) {
        AppPalette.SYSTEM -> "De tu móvil"
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
