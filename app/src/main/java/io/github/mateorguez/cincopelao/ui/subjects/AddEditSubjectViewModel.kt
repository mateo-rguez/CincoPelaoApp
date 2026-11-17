package io.github.mateorguez.cincopelao.ui.subjects

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mateorguez.cincopelao.data.instances.InstanceRepository
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
    /** En edición, false hasta que se cargan la asignatura y sus eventos. */
    val loaded: Boolean = true,
    val subject: Subject = Subject(),
    /** Eventos ordenados por fecha. */
    val events: List<EditableEvent> = emptyList(),
    /** Titulación de la instancia activa, para la vista previa de la tarjeta. */
    val titulation: String? = null,
    /** Lo que falta; solo se rellena tras intentar guardar, para no marcar errores de entrada. */
    val missing: Set<SubjectField> = emptySet(),
    /** Si hay algo distinto de lo guardado (o de un formulario vacío, al crear). */
    val hasChanges: Boolean = false,
) {
    val weightTotal: Float get() = weightTotal(events.map { it.event })
}

enum class SubjectFormValidation { Valid, Incomplete, WeightsNotHundred }

/** Una asignatura necesita nombre, curso, abreviatura y al menos un evento; los pesos deben sumar ~100%. */
fun validateSubjectForm(subject: Subject, events: List<Event>): SubjectFormValidation = when {
    missingSubjectFields(subject, events).isNotEmpty() -> SubjectFormValidation.Incomplete
    weightStatus(weightTotal(events)) != WeightStatus.Complete -> SubjectFormValidation.WeightsNotHundred
    else -> SubjectFormValidation.Valid
}

@HiltViewModel
class AddEditSubjectViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val subjects: SubjectRepository,
    private val eventsRepository: EventRepository,
    instances: InstanceRepository,
) : ViewModel() {

    /** Id de la asignatura a editar, o -1 para crear una nueva (llega en los extras del intent). */
    private val subjectId: Long = savedStateHandle[AddEditSubjectActivity.EXTRA_SUBJECT_ID] ?: -1L

    private var nextKey = 0L

    /** Lo guardado al abrir, para saber si hay cambios. */
    private var original: Pair<Subject, List<Event>> = Subject() to emptyList()

    /** Mientras sea false, la abreviatura sigue al nombre; deja de hacerlo al escribirla a mano. */
    private var abbrevTouched = subjectId != -1L

    private val _uiState = MutableStateFlow(
        AddEditSubjectUiState(isEditMode = subjectId != -1L, loaded = subjectId == -1L)
    )
    val uiState: StateFlow<AddEditSubjectUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val titulation = instances.currentInstance.first()?.titulation?.takeIf { it.isNotBlank() }
            _uiState.update { it.copy(titulation = titulation) }
        }
        // Se carga una sola vez: al girar la pantalla el ViewModel sobrevive y no se pisan los cambios
        if (subjectId != -1L) {
            viewModelScope.launch {
                val subject = subjects.subject(subjectId).first() ?: return@launch
                val events = eventsRepository.eventsForSubject(subjectId).first()
                original = subject to events
                _uiState.update {
                    it.copy(loaded = true, subject = subject, events = events.map(::editable).sortedByDate())
                }
            }
        }
    }

    private fun editable(event: Event) = EditableEvent(nextKey++, event)
    private fun List<EditableEvent>.sortedByDate() = sortedBy { it.event.date }

    /** Aplica un cambio y recalcula lo que depende de él (cambios pendientes y errores ya mostrados). */
    private fun edit(change: (AddEditSubjectUiState) -> AddEditSubjectUiState) = _uiState.update { state ->
        val next = change(state)
        val events = next.events.map { it.event }
        next.copy(
            hasChanges = next.subject != original.first || events.sortedBy { it.date } != original.second.sortedBy { it.date },
            missing = if (state.missing.isEmpty()) emptySet() else missingSubjectFields(next.subject, events)
        )
    }

    fun updateName(name: String) = edit { state ->
        val abbrev = if (abbrevTouched) state.subject.abbrev else suggestAbbrev(name)
        state.copy(subject = state.subject.copy(name = name, abbrev = abbrev))
    }

    fun updateCourse(course: String) = edit { it.copy(subject = it.subject.copy(course = course)) }

    fun updateAbbrev(abbrev: String) {
        // Si se borra entera, vuelve a seguir al nombre
        abbrevTouched = abbrev.isNotEmpty()
        edit { it.copy(subject = it.subject.copy(abbrev = abbrev)) }
    }

    fun addEvent(event: Event) = edit { it.copy(events = (it.events + editable(event)).sortedByDate()) }

    fun updateEvent(key: Long, event: Event) = edit { state ->
        state.copy(events = state.events.map { if (it.key == key) it.copy(event = event) else it }.sortedByDate())
    }

    /** Quita el evento del formulario y lo devuelve para poder deshacerlo con [restoreEvent]. */
    fun removeEvent(key: Long): EditableEvent? {
        val removed = _uiState.value.events.find { it.key == key } ?: return null
        edit { state -> state.copy(events = state.events.filterNot { it.key == key }) }
        return removed
    }

    fun restoreEvent(removed: EditableEvent) = edit { it.copy(events = (it.events + removed).sortedByDate()) }

    /** Comprueba el formulario; si falta algo, lo marca en pantalla. */
    fun validate(): SubjectFormValidation {
        val state = _uiState.value
        val events = state.events.map { it.event }
        val result = validateSubjectForm(state.subject, events)
        _uiState.update { it.copy(missing = missingSubjectFields(state.subject, events)) }
        return result
    }

    /** Guarda la asignatura con sus eventos. Es suspend para que la pantalla espere antes de cerrarse. */
    suspend fun save() {
        val state = _uiState.value
        subjects.saveWithEvents(state.isEditMode, state.subject, state.events.map { it.event })
    }
}
