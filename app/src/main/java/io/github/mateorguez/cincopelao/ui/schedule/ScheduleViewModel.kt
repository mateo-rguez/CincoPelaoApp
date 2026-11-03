package io.github.mateorguez.cincopelao.ui.schedule

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.mateorguez.cincopelao.data.instances.InstanceRepository
import io.github.mateorguez.cincopelao.data.prefs.getScheduleState
import io.github.mateorguez.cincopelao.data.prefs.saveScheduleState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ScheduleUiState {
    data object Loading : ScheduleUiState
    data object Empty : ScheduleUiState

    /** [version] cambia con cada archivo cargado, para reiniciar el zoom de la vista. */
    data class Loaded(val bitmap: Bitmap, val zoom: Float, val offset: Offset, val version: Int) : ScheduleUiState
}

@OptIn(FlowPreview::class)
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val instances: InstanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScheduleUiState>(ScheduleUiState.Loading)
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    /** Mensajes de error para mostrar una sola vez. */
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private var currentUri: String? = null
    private var version = 0

    /** Último zoom/desplazamiento; se guarda cuando el gesto se detiene, no en cada movimiento. */
    private val transform = MutableStateFlow<Pair<Float, Offset>?>(null)

    init {
        viewModelScope.launch {
            instances.currentInstanceId.collectLatest { instanceId -> loadSaved(instanceId) }
        }
        viewModelScope.launch {
            transform.filterNotNull().debounce(SAVE_DELAY_MS).collect { (zoom, offset) ->
                // El estado también se actualiza (sin cambiar version): al volver a la pestaña,
                // la vista se recrea con el último zoom y no con el que tenía al cargarse
                _uiState.update { state ->
                    if (state is ScheduleUiState.Loaded) state.copy(zoom = zoom, offset = offset) else state
                }
                saveScheduleState(context, instances.currentInstanceId.value, currentUri, zoom, offset)
            }
        }
    }

    private suspend fun loadSaved(instanceId: String) {
        _uiState.value = ScheduleUiState.Loading
        val (uriString, zoom, offset) = getScheduleState(context, instanceId).first()
        currentUri = uriString
        val bitmap = uriString?.let { ScheduleFileLoader.load(context, Uri.parse(it)) }
        _uiState.value = if (bitmap != null) ScheduleUiState.Loaded(bitmap, zoom, offset, ++version) else ScheduleUiState.Empty
    }

    /** Importa un archivo elegido por el usuario y lo guarda como horario de la instancia activa. */
    fun import(uri: Uri) = viewModelScope.launch {
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (ScheduleFileLoader.isPdf(context, uri) &&
                ScheduleFileLoader.pdfPageCount(context, uri) > ScheduleFileLoader.MAX_PDF_PAGES
            ) {
                _messages.send("El PDF tiene más de ${ScheduleFileLoader.MAX_PDF_PAGES} páginas. Selecciona uno más corto.")
                return@launch
            }
            val bitmap = ScheduleFileLoader.load(context, uri)
            if (bitmap == null) {
                _messages.send("No se pudo cargar el archivo")
                return@launch
            }
            currentUri = uri.toString()
            _uiState.value = ScheduleUiState.Loaded(bitmap, 1f, Offset.Zero, ++version)
            saveScheduleState(context, instances.currentInstanceId.value, currentUri, 1f, Offset.Zero)
        } catch (e: Exception) {
            _messages.send("Error al cargar archivo: ${e.message}")
        }
    }

    fun onTransformChange(zoom: Float, offset: Offset) {
        transform.value = zoom to offset
    }

    private companion object {
        const val SAVE_DELAY_MS = 400L
    }
}
