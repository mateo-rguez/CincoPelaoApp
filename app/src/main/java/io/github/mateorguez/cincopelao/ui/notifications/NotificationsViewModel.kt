package io.github.mateorguez.cincopelao.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mateorguez.cincopelao.data.prefs.NotiPrefsManager
import io.github.mateorguez.cincopelao.notifications.ReminderSettings
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ReminderKind { Events, Tasks }

/** Ajustes de un tipo de recordatorio: lo que se está editando y lo que hay guardado. */
data class ReminderSettingsForm(val edited: ReminderSettings, val saved: ReminderSettings) {
    val hasUnsavedChanges: Boolean
        get() = edited.daysBefore.sortedDescending() != saved.daysBefore.sortedDescending() ||
            edited.startHour != saved.startHour ||
            edited.endHour != saved.endHour
}

data class NotificationsUiState(
    /** null hasta que se leen los ajustes guardados. */
    val events: ReminderSettingsForm? = null,
    val tasks: ReminderSettingsForm? = null
) {
    fun form(kind: ReminderKind) = if (kind == ReminderKind.Events) events else tasks
}

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val prefs: NotiPrefsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    /** Mensajes para mostrar una vez (confirmaciones y validaciones). */
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    init {
        // Se lee una sola vez: al girar la pantalla el ViewModel sobrevive con los cambios sin guardar
        viewModelScope.launch {
            val events = combine(prefs.eventDaysFlow, prefs.eventStartHourFlow, prefs.eventEndHourFlow, ::ReminderSettings).first()
            val tasks = combine(prefs.taskDaysFlow, prefs.taskStartHourFlow, prefs.taskEndHourFlow, ::ReminderSettings).first()
            _uiState.value = NotificationsUiState(
                events = ReminderSettingsForm(events.sorted(), events.sorted()),
                tasks = ReminderSettingsForm(tasks.sorted(), tasks.sorted())
            )
        }
    }

    private fun ReminderSettings.sorted() = copy(daysBefore = daysBefore.distinct().sortedDescending())

    private fun edit(kind: ReminderKind, change: (ReminderSettings) -> ReminderSettings) = _uiState.update { state ->
        when (kind) {
            ReminderKind.Events -> state.copy(events = state.events?.let { it.copy(edited = change(it.edited).sorted()) })
            ReminderKind.Tasks -> state.copy(tasks = state.tasks?.let { it.copy(edited = change(it.edited).sorted()) })
        }
    }

    fun addDay(kind: ReminderKind, day: Int) {
        if (day > 0) edit(kind) { it.copy(daysBefore = it.daysBefore + day) }
    }

    fun removeDay(kind: ReminderKind, day: Int) = edit(kind) { it.copy(daysBefore = it.daysBefore - day) }

    fun setStartHour(kind: ReminderKind, hour: Int) {
        val current = _uiState.value.form(kind)?.edited ?: return
        if (hour >= current.endHour) {
            _messages.trySend("La hora de inicio no puede ser igual o posterior a la de fin")
        } else {
            edit(kind) { it.copy(startHour = hour) }
        }
    }

    fun setEndHour(kind: ReminderKind, hour: Int) {
        val current = _uiState.value.form(kind)?.edited ?: return
        if (hour <= current.startHour) {
            _messages.trySend("La hora de fin no puede ser anterior o igual a la de inicio")
        } else {
            edit(kind) { it.copy(endHour = hour) }
        }
    }

    fun save(kind: ReminderKind) = viewModelScope.launch {
        val settings = _uiState.value.form(kind)?.edited ?: return@launch
        when (kind) {
            ReminderKind.Events -> prefs.saveEventConfig(settings.daysBefore, settings.startHour, settings.endHour)
            ReminderKind.Tasks -> prefs.saveTaskConfig(settings.daysBefore, settings.startHour, settings.endHour)
        }
        _uiState.update { state ->
            when (kind) {
                ReminderKind.Events -> state.copy(events = state.events?.copy(saved = settings))
                ReminderKind.Tasks -> state.copy(tasks = state.tasks?.copy(saved = settings))
            }
        }
        _messages.send(if (kind == ReminderKind.Events) "Configuración de eventos guardada" else "Configuración de tareas guardada")
    }
}
