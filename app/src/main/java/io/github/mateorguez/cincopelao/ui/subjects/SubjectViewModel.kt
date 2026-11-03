package io.github.mateorguez.cincopelao.ui.subjects

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.repository.EventRepository
import io.github.mateorguez.cincopelao.data.repository.SubjectRepository
import io.github.mateorguez.cincopelao.domain.stats.GradeCalculator
import io.github.mateorguez.cincopelao.domain.stats.GradedEvent
import io.github.mateorguez.cincopelao.domain.stats.Stats
import io.github.mateorguez.cincopelao.domain.stats.StatsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado del detalle de una asignatura y de su pantalla de estadísticas. */
data class SubjectDetailUiState(
    val subject: Subject? = null,
    val events: List<Event> = emptyList(),
    /** Nota ponderada actual (null si aún no hay notas). */
    val weightedGrade: Float? = null,
    /** Media simple de las notas (null si aún no hay notas). */
    val meanGrade: Float? = null,
    /** Eventos puntuables, para la gráfica y la calculadora. */
    val gradedEvents: List<GradedEvent> = emptyList(),
    val stats: Stats? = null
) {
    val hasGradedEvents: Boolean get() = gradedEvents.isNotEmpty()
}

@HiltViewModel
class SubjectViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    subjects: SubjectRepository,
    private val events: EventRepository
) : ViewModel() {

    /** Llega en los extras del intent que abre SubjectActivity. */
    private val subjectId: Int = savedStateHandle[SubjectActivity.EXTRA_SUBJECT_ID] ?: -1

    val uiState: StateFlow<SubjectDetailUiState> =
        combine(subjects.subject(subjectId), events.eventsForSubject(subjectId)) { subject, eventList ->
            SubjectDetailUiState(
                subject = subject,
                events = eventList,
                weightedGrade = GradeCalculator.weightedGrade(eventList),
                meanGrade = GradeCalculator.meanGrade(eventList),
                gradedEvents = GradeCalculator.gradedEvents(eventList),
                stats = StatsUtils.calculateStats(eventList)
            )
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubjectDetailUiState())

    fun updateEvent(updated: Event) = viewModelScope.launch { events.update(updated) }
}
