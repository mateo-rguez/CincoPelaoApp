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
 * Preferencia de modo oscuro. Se empieza a cargar al arrancar la app, así que al abrir
 * cualquier pantalla ya está disponible y no hay un fotograma en el tema equivocado.
 */
@Singleton
class ThemeRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope scope: CoroutineScope
) {
    /** null mientras se lee por primera vez. */
    val isDarkTheme: StateFlow<Boolean?> = context.getDarkModeFlow()
        .map<Boolean, Boolean?> { it }
        .stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun setDarkTheme(enabled: Boolean) = context.setDarkMode(enabled)
}
