package io.github.mateorguez.cincopelao.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mateorguez.cincopelao.data.prefs.NotiPrefsManager
import io.github.mateorguez.cincopelao.notifications.ReminderSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ReminderKind { Events, Tasks }

data class NotificationsUiState(
    /** null hasta que se leen los ajustes guardados. */
    val events: ReminderSettings? = null,
    val tasks: ReminderSettings? = null
) {
    fun settings(kind: ReminderKind) = if (kind == ReminderKind.Events) events else tasks
}

/**
 * Ajustes de los recordatorios. Cada cambio se guarda al momento (los avisos se reprograman
 * solos al cambiar los ajustes), así que no hay "cambios sin guardar".
 */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val prefs: NotiPrefsManager
) : ViewModel() {

    val uiState: StateFlow<NotificationsUiState> = combine(
        combine(prefs.eventDaysFlow, prefs.eventStartHourFlow, prefs.eventEndHourFlow, ::ReminderSettings),
        combine(prefs.taskDaysFlow, prefs.taskStartHourFlow, prefs.taskEndHourFlow, ::ReminderSettings)
    ) { events, tasks -> NotificationsUiState(events.normalized(), tasks.normalized()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationsUiState())

    private fun ReminderSettings.normalized() = copy(daysBefore = daysBefore.distinct().sortedDescending())

    private fun change(kind: ReminderKind, change: (ReminderSettings) -> ReminderSettings) {
        val current = uiState.value.settings(kind) ?: return
        val updated = change(current).normalized()
        if (updated == current) return
        viewModelScope.launch {
            when (kind) {
                ReminderKind.Events -> prefs.saveEventConfig(updated.daysBefore, updated.startHour, updated.endHour)
                ReminderKind.Tasks -> prefs.saveTaskConfig(updated.daysBefore, updated.startHour, updated.endHour)
            }
        }
    }

    /** Activa o desactiva un día de antelación. */
    fun toggleDay(kind: ReminderKind, day: Int) = change(kind) { s ->
        s.copy(daysBefore = if (day in s.daysBefore) s.daysBefore - day else s.daysBefore + day)
    }

    /** Añade un día escrito a mano (de 1 a [MAX_REMINDER_DAYS]). */
    fun addDay(kind: ReminderKind, day: Int) {
        if (day in 1..MAX_REMINDER_DAYS) change(kind) { it.copy(daysBefore = it.daysBefore + day) }
    }

    /** Franja horaria: al menos una hora, dentro del día (la hora de fin puede ser 24). */
    fun setHours(kind: ReminderKind, start: Int, end: Int) {
        val from = start.coerceIn(0, 23)
        val to = end.coerceIn(from + 1, 24)
        change(kind) { it.copy(startHour = from, endHour = to) }
    }
}
