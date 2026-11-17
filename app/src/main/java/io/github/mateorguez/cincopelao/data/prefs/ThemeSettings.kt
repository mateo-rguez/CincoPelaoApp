package io.github.mateorguez.cincopelao.data.prefs

/** Cuándo usar los colores oscuros. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Paleta de colores de la app; cada una tiene variante clara y oscura. */
enum class AppPalette {
    CLASSIC, GRAPHITE, CATPPUCCIN, ROSE_PINE, GRUVBOX, FLEXOKI, TOKYO_NIGHT, KANAGAWA,
    CHALKBOARD, NOTEBOOK, LIBRARY, RISO,
}

/**
 * Letra de los títulos y las cifras destacadas. El texto normal usa siempre la misma (DM Sans):
 * solo cambia el carácter de la app, no la lectura.
 */
enum class TitleFont { DELIGHT, JAKARTA, SOURCE_SERIF, FRAUNCES }

/** Apariencia elegida por el usuario. */
data class ThemeSettings(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val palette: AppPalette = AppPalette.CLASSIC,
    val titleFont: TitleFont = TitleFont.DELIGHT,
) {
    /** Si hay que pintar en oscuro, dado el modo actual del sistema. */
    fun isDark(systemDark: Boolean): Boolean = when (mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    companion object {
        /**
         * Lee los ajustes guardados. [legacyDarkMode] es el interruptor de modo oscuro de versiones
         * anteriores: si el usuario lo había tocado y aún no ha elegido modo, se respeta.
         * Valores desconocidos (p. ej. una paleta o una letra que ya no existe) vuelven al de por defecto.
         */
        fun fromStored(
            mode: String?,
            palette: String?,
            legacyDarkMode: Boolean?,
            titleFont: String? = null,
        ): ThemeSettings {
            val parsedMode = ThemeMode.entries.firstOrNull { it.name == mode }
                ?: when (legacyDarkMode) {
                    true -> ThemeMode.DARK
                    false -> ThemeMode.LIGHT
                    null -> ThemeMode.SYSTEM
                }
            val parsedPalette = AppPalette.entries.firstOrNull { it.name == palette } ?: AppPalette.CLASSIC
            val parsedTitleFont = TitleFont.entries.firstOrNull { it.name == titleFont } ?: TitleFont.DELIGHT
            return ThemeSettings(parsedMode, parsedPalette, parsedTitleFont)
        }
    }
}
