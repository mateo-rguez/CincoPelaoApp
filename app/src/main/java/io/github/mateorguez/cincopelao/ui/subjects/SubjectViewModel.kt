package io.github.mateorguez.cincopelao.ui.subjects

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.prefs.HeaderGrade
import io.github.mateorguez.cincopelao.data.prefs.SubjectDisplayRepository
import io.github.mateorguez.cincopelao.data.repository.EventRepository
import io.github.mateorguez.cincopelao.data.repository.SubjectRepository
import io.github.mateorguez.cincopelao.domain.stats.GradeCalculator
import io.github.mateorguez.cincopelao.domain.stats.GradedEvent
import io.github.mateorguez.cincopelao.domain.stats.Stats
import io.github.mateorguez.cincopelao.domain.stats.StatsUtils
import io.github.mateorguez.cincopelao.domain.stats.SubjectInsights
import io.github.mateorguez.cincopelao.domain.stats.subjectInsights
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

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
    val stats: Stats? = null,
    /** Datos con nombre de evento y proyecciones, para la sección «Datos» de las estadísticas. */
    val insights: SubjectInsights? = null,
    /** Qué nota va en grande en la cabecera (ajuste común a todas las asignaturas). */
    val headerGrade: HeaderGrade = HeaderGrade.WEIGHTED,
    /** Sube cada vez que hay que enseñar un momento la etiqueta de la nota grande. */
    val headerLabelPulse: Int = 0,
    /** Si la etiqueta de esta vez recuerda que se puede tocar (algunas aperturas, al azar). */
    val headerTapHint: Boolean = false,
) {
    val hasGradedEvents: Boolean get() = gradedEvents.isNotEmpty()
}

@HiltViewModel
class SubjectViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    subjects: SubjectRepository,
    private val events: EventRepository,
    private val display: SubjectDisplayRepository,
) : ViewModel() {

    /** Llega en los extras del intent que abre SubjectActivity. */
    private val subjectId: Long = savedStateHandle[SubjectActivity.EXTRA_SUBJECT_ID] ?: -1L

    /** Pulso de la etiqueta y si lleva la pista del toque. */
    private val headerLabel = MutableStateFlow(LabelPulse(0, tapHint = false))

    val uiState: StateFlow<SubjectDetailUiState> =
        combine(
            subjects.subject(subjectId),
            events.eventsForSubject(subjectId),
            display.headerGrade,
            headerLabel,
        ) { subject, eventList, headerGrade, label ->
            SubjectDetailUiState(
                subject = subject,
                events = eventList,
                weightedGrade = GradeCalculator.weightedGrade(eventList),
                meanGrade = GradeCalculator.meanGrade(eventList),
                gradedEvents = GradeCalculator.gradedEvents(eventList),
                stats = StatsUtils.calculateStats(eventList),
                insights = subjectInsights(GradeCalculator.gradedEvents(eventList)),
                headerGrade = headerGrade,
                headerLabelPulse = label.count,
                headerTapHint = label.tapHint,
            )
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubjectDetailUiState())

    init {
        // Al abrir se enseña siempre qué nota es; algunas veces, también que se puede cambiar
        headerLabel.value = LabelPulse(1, tapHint = Random.nextDouble() < HEADER_TAP_HINT_CHANCE)
    }

    fun updateEvent(updated: Event) = viewModelScope.launch { events.update(updated) }

    /** Intercambia la nota grande de la cabecera (para todas las asignaturas) y lo indica. */
    fun toggleHeaderGrade() = viewModelScope.launch {
        display.setHeaderGrade(uiState.value.headerGrade.other)
        headerLabel.value = LabelPulse(headerLabel.value.count + 1, tapHint = false)
    }

    private data class LabelPulse(val count: Int, val tapHint: Boolean)
}
