package io.github.mateorguez.cincopelao.data.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.mateorguez.cincopelao.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ajustes de apariencia (modo y paleta). Se empiezan a cargar al arrancar la app, así que al abrir
 * cualquier pantalla ya están disponibles y no hay un fotograma con los colores equivocados.
 */
@Singleton
class ThemeRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope scope: CoroutineScope
) {
    /** null mientras se leen por primera vez. */
    val settings: StateFlow<ThemeSettings?> = context.getThemeSettingsFlow()
        .map<ThemeSettings, ThemeSettings?> { it }
        .stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun setMode(mode: ThemeMode) = context.setThemeMode(mode)

    suspend fun setPalette(palette: AppPalette) = context.setThemePalette(palette)
}
