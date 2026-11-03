package io.github.mateorguez.cincopelao.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mateorguez.cincopelao.data.local.entity.Task
import io.github.mateorguez.cincopelao.data.repository.SubjectRepository
import io.github.mateorguez.cincopelao.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Una tarea archivada tal como se muestra: con sus subtareas archivadas y su asignatura. */
data class ArchivedTaskItem(
    val task: Task,
    val subtasks: List<Task>,
    val subjectAbbrev: String?
)

data class CompletedTasksUiState(
    /** false hasta que llegan los datos (para no mostrar "no hay tareas" mientras se cargan). */
    val isLoaded: Boolean = false,
    /** Tareas archivadas de primer nivel, de la completada más recientemente a la más antigua. */
    val items: List<ArchivedTaskItem> = emptyList(),
    val archivedTasks: List<Task> = emptyList(),
    val allTasks: List<Task> = emptyList()
) {
    fun task(id: Long): Task? = allTasks.find { it.taskId == id }
}

@HiltViewModel
class CompletedTasksViewModel @Inject constructor(
    subjects: SubjectRepository,
    private val tasks: TaskRepository
) : ViewModel() {

    val uiState: StateFlow<CompletedTasksUiState> = combine(subjects.subjects, tasks.tasks) { subjectList, taskList ->
        val abbrevById = subjectList.associate { it.subjectId.toLong() to it.abbrev }
        val archived = taskList.filter { it.archived }
        val items = archived
            .sortedByDescending { it.completedAt ?: Long.MAX_VALUE }
            .mapNotNull { task ->
                val subtasks = archived.filter { it.parentTaskId == task.taskId }
                // Las subtareas archivadas se muestran dentro de su tarea madre
                if (subtasks.isEmpty() && task.parentTaskId != null) null
                else ArchivedTaskItem(task, subtasks, task.subjectId?.let { abbrevById[it] })
            }
        CompletedTasksUiState(isLoaded = true, items = items, archivedTasks = archived, allTasks = taskList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompletedTasksUiState())

    /** Devuelve la tarea (y sus subtareas) a la lista de pendientes. */
    fun restore(item: ArchivedTaskItem) = viewModelScope.launch {
        tasks.setArchived(listOf(item.task) + item.subtasks, archived = false)
    }

    /** Borra la tarea con sus subtareas y las devuelve para poder deshacerlo con [undoDelete]. */
    suspend fun deleteWithSubtasks(task: Task): List<Task> = tasks.deleteWithSubtasks(task)

    fun undoDelete(deleted: List<Task>) = viewModelScope.launch { tasks.restore(deleted) }

    fun deleteAllArchived() = viewModelScope.launch { tasks.deleteAll(uiState.value.archivedTasks) }

    fun delete(task: Task) = viewModelScope.launch { tasks.delete(task) }

    fun deleteKeepingSubtasks(task: Task) = viewModelScope.launch { tasks.deleteKeepingSubtasks(task) }
}
