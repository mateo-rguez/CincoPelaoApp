package io.github.mateorguez.cincopelao.ui.theme

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mateorguez.cincopelao.data.prefs.ThemeRepository
import io.github.mateorguez.cincopelao.data.prefs.ThemeSettings
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Da a cada Activity los ajustes de apariencia para montar el tema. */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    repository: ThemeRepository
) : ViewModel() {

    /** Ajustes elegidos; null mientras se cargan por primera vez. */
    val settings: StateFlow<ThemeSettings?> = repository.settings
}
