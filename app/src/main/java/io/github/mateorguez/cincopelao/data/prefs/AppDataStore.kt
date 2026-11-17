package io.github.mateorguez.cincopelao.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore de preferencias de la app y ajustes de apariencia.

val Context.dataStore by preferencesDataStore(name = "settings")

/** Interruptor de modo oscuro de versiones anteriores; solo se lee para migrarlo. */
private val LEGACY_DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
private val THEME_PALETTE_KEY = stringPreferencesKey("theme_palette")
private val THEME_TITLE_FONT_KEY = stringPreferencesKey("theme_title_font")

fun Context.getThemeSettingsFlow(): Flow<ThemeSettings> = dataStore.data.map { preferences ->
    ThemeSettings.fromStored(
        mode = preferences[THEME_MODE_KEY],
        palette = preferences[THEME_PALETTE_KEY],
        legacyDarkMode = preferences[LEGACY_DARK_MODE_KEY],
        titleFont = preferences[THEME_TITLE_FONT_KEY],
    )
}

suspend fun Context.setThemeMode(mode: ThemeMode) {
    dataStore.edit { preferences ->
        preferences[THEME_MODE_KEY] = mode.name
        preferences.remove(LEGACY_DARK_MODE_KEY) // ya migrado
    }
}

suspend fun Context.setThemePalette(palette: AppPalette) {
    dataStore.edit { preferences -> preferences[THEME_PALETTE_KEY] = palette.name }
}

suspend fun Context.setThemeTitleFont(font: TitleFont) {
    dataStore.edit { preferences -> preferences[THEME_TITLE_FONT_KEY] = font.name }
}
