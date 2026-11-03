package io.github.mateorguez.cincopelao.ui.subjects

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class AddEditSubjectViewModel @Inject constructor() : ViewModel() {

    val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    fun setEditMode(editMode: Boolean) {
        _isEditMode.value = editMode
    }

    fun resetForm() {
        _subject.value = Subject()
        _events.value = emptyList<Event>()
    }

    fun isFormValid(): Int {
        val currentSubject = subject.value
        val currentEvents = events.value

        // Chequeamos datos obligatorios
        if (currentSubject.name.isNullOrBlank() || currentSubject.course.isNullOrBlank() || currentSubject.abbrev.isNullOrBlank() || currentEvents.isEmpty()) {
            return 1
        }

        // Sumamos los weights de los eventos (ignoramos los null)
        val totalWeight = currentEvents.mapNotNull { it.weight }.sum()

        if (totalWeight !in 99.0..100.0) {
            return 2
        }

        return 0
    }

    // ASIGNATURAS --------------------------------

    val _subject = MutableStateFlow(Subject())
    val subject: StateFlow<Subject> = _subject.asStateFlow()

    fun updateName(name: String) {
        _subject.update { it.copy(name = name) }
    }

    fun updateCurso(curso: String) {
        _subject.update { it.copy(course = curso) }
    }

    fun updateAbbrev(abbrev: String) {
        _subject.update { it.copy(abbrev = abbrev) }
    }

    fun setSubject(subject: Subject) {
        _subject.update { subject }
    }

    // EVENTOS --------------------------------

    val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    fun addEvent(event: Event) {
        _events.update { (it + event).sortedBy { e -> e.date ?: Long.MIN_VALUE } }
    }

    fun removeEvent(event: Event) {
        _events.update { (it - event).sortedBy { e -> e.date ?: Long.MIN_VALUE } }
    }

    fun setEvents(events: List<Event>) {
        _events.update { events }
    }

    fun updateEvent(index: Int, updatedEvent: Event) {
        _events.update { currentList ->
            currentList.toMutableList().apply {
                if (index in indices) this[index] = updatedEvent
            }.sortedBy { it.date ?: Long.MIN_VALUE }
        }
    }
}