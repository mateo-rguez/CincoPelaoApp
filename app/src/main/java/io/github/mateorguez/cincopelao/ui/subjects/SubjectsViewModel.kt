package io.github.mateorguez.cincopelao.ui.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mateorguez.cincopelao.data.instances.InstanceRepository
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.repository.DeletedSubject
import io.github.mateorguez.cincopelao.data.repository.EventRepository
import io.github.mateorguez.cincopelao.data.repository.TaskRepository
import io.github.mateorguez.cincopelao.data.repository.SubjectRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubjectsUiState(
    val subjects: List<SubjectSummary> = emptyList(),
    /** Titulación de la instancia activa, que se muestra junto al curso. */
    val titulation: String? = null
)

@HiltViewModel
class SubjectsViewModel @Inject constructor(
    private val subjects: SubjectRepository,
    events: EventRepository,
    tasks: TaskRepository,
    instances: InstanceRepository
) : ViewModel() {

    val uiState: StateFlow<SubjectsUiState> = combine(
        subjects.subjects, events.events, tasks.tasks, instances.currentInstance
    ) { list, allEvents, allTasks, instance ->
        SubjectsUiState(subjectSummaries(list, allEvents, allTasks), instance?.titulation)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubjectsUiState())

    /** Borra la asignatura con sus eventos y tareas; devuelve lo borrado para poder deshacerlo. */
    suspend fun delete(subject: Subject): DeletedSubject = subjects.delete(subject)

    fun undoDelete(deleted: DeletedSubject) = viewModelScope.launch { subjects.restore(deleted) }
}
