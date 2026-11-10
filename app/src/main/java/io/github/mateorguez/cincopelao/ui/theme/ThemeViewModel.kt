package io.github.mateorguez.cincopelao.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mateorguez.cincopelao.data.prefs.ThemeMode
import io.github.mateorguez.cincopelao.data.prefs.ThemeRepository
import io.github.mateorguez.cincopelao.data.prefs.ThemeSettings
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Da a cada Activity los ajustes de apariencia para montar el tema. */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val repository: ThemeRepository
) : ViewModel() {

    /** Ajustes elegidos; null mientras se cargan por primera vez. */
    val settings: StateFlow<ThemeSettings?> = repository.settings

    /** Interruptor del menú lateral: fija Claro u Oscuro. */
    fun setMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setMode(mode) }
    }
}
