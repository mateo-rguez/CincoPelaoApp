package io.github.mateorguez.cincopelao.data.prefs

/** Cuándo usar los colores oscuros. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Paleta de colores de la app; cada una tiene variante clara y oscura. */
enum class AppPalette { CLASSIC }

/** Apariencia elegida por el usuario. */
data class ThemeSettings(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val palette: AppPalette = AppPalette.CLASSIC,
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
         * Valores desconocidos (p. ej. una paleta que ya no existe) vuelven al de por defecto.
         */
        fun fromStored(mode: String?, palette: String?, legacyDarkMode: Boolean?): ThemeSettings {
            val parsedMode = ThemeMode.entries.firstOrNull { it.name == mode }
                ?: when (legacyDarkMode) {
                    true -> ThemeMode.DARK
                    false -> ThemeMode.LIGHT
                    null -> ThemeMode.SYSTEM
                }
            val parsedPalette = AppPalette.entries.firstOrNull { it.name == palette } ?: AppPalette.CLASSIC
            return ThemeSettings(parsedMode, parsedPalette)
        }
    }
}
