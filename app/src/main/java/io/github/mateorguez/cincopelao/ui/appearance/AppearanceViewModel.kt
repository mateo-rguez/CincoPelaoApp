package io.github.mateorguez.cincopelao.ui.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mateorguez.cincopelao.data.prefs.AppPalette
import io.github.mateorguez.cincopelao.data.prefs.ThemeMode
import io.github.mateorguez.cincopelao.data.prefs.ThemeRepository
import io.github.mateorguez.cincopelao.data.prefs.ThemeSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Modo y paleta elegidos. Los cambios se guardan al momento y toda la app los ve. */
@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val repository: ThemeRepository
) : ViewModel() {

    val uiState: StateFlow<ThemeSettings> = repository.settings
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repository.settings.value ?: ThemeSettings())

    fun setMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setMode(mode) }
    }

    fun setPalette(palette: AppPalette) {
        viewModelScope.launch { repository.setPalette(palette) }
    }
}
