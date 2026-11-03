package io.github.mateorguez.cincopelao.ui.subjects

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.repository.EventRepository
import io.github.mateorguez.cincopelao.data.repository.SubjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Un evento dentro del formulario. [key] lo identifica mientras se edita: los eventos nuevos
 * aún no tienen eventId (todos valen 0), así que no sirve para distinguirlos.
 */
data class EditableEvent(val key: Long, val event: Event)

data class AddEditSubjectUiState(
    val isEditMode: Boolean = false,
    val subject: Subject = Subject(),
    /** Eventos ordenados por fecha. */
    val events: List<EditableEvent> = emptyList()
)

enum class SubjectFormValidation { Valid, Incomplete, WeightsNotHundred }

/** Una asignatura necesita nombre, curso, abreviatura y al menos un evento; los pesos deben sumar ~100%. */
fun validateSubjectForm(subject: Subject, events: List<Event>): SubjectFormValidation = when {
    subject.name.isBlank() || subject.course.isBlank() || subject.abbrev.isBlank() || events.isEmpty() ->
        SubjectFormValidation.Incomplete
    events.mapNotNull { it.weight }.sum() !in 99.0..100.0 -> SubjectFormValidation.WeightsNotHundred
    else -> SubjectFormValidation.Valid
}

@HiltViewModel
class AddEditSubjectViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val subjects: SubjectRepository,
    private val eventsRepository: EventRepository
) : ViewModel() {

    /** Id de la asignatura a editar, o -1 para crear una nueva (llega en los extras del intent). */
    private val subjectId: Int = savedStateHandle[AddEditSubjectActivity.EXTRA_SUBJECT_ID] ?: -1

    private var nextKey = 0L

    private val _uiState = MutableStateFlow(AddEditSubjectUiState(isEditMode = subjectId != -1))
    val uiState: StateFlow<AddEditSubjectUiState> = _uiState.asStateFlow()

    init {
        // Se carga una sola vez: al girar la pantalla el ViewModel sobrevive y no se pisan los cambios
        if (subjectId != -1) {
            viewModelScope.launch {
                val subject = subjects.subject(subjectId).first() ?: return@launch
                val events = eventsRepository.eventsForSubject(subjectId).first()
                _uiState.update { it.copy(subject = subject, events = events.map(::editable).sortedByDate()) }
            }
        }
    }

    private fun editable(event: Event) = EditableEvent(nextKey++, event)
    private fun List<EditableEvent>.sortedByDate() = sortedBy { it.event.date }

    fun updateName(name: String) = _uiState.update { it.copy(subject = it.subject.copy(name = name)) }
    fun updateCourse(course: String) = _uiState.update { it.copy(subject = it.subject.copy(course = course)) }
    fun updateAbbrev(abbrev: String) = _uiState.update { it.copy(subject = it.subject.copy(abbrev = abbrev)) }

    fun addEvent(event: Event) = _uiState.update { it.copy(events = (it.events + editable(event)).sortedByDate()) }

    fun updateEvent(key: Long, event: Event) = _uiState.update { state ->
        state.copy(events = state.events.map { if (it.key == key) it.copy(event = event) else it }.sortedByDate())
    }

    /** Quita el evento del formulario y lo devuelve para poder deshacerlo con [restoreEvent]. */
    fun removeEvent(key: Long): EditableEvent? {
        val removed = _uiState.value.events.find { it.key == key } ?: return null
        _uiState.update { state -> state.copy(events = state.events.filterNot { it.key == key }) }
        return removed
    }

    fun restoreEvent(removed: EditableEvent) = _uiState.update { it.copy(events = (it.events + removed).sortedByDate()) }

    fun validate(): SubjectFormValidation = with(_uiState.value) { validateSubjectForm(subject, events.map { it.event }) }

    /** Guarda la asignatura con sus eventos. Es suspend para que la pantalla espere antes de cerrarse. */
    suspend fun save() {
        val state = _uiState.value
        subjects.saveWithEvents(state.isEditMode, state.subject, state.events.map { it.event })
    }
}
