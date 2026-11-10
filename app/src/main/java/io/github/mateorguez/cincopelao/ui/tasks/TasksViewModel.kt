package io.github.mateorguez.cincopelao.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.local.entity.Task
import io.github.mateorguez.cincopelao.data.repository.SubjectRepository
import io.github.mateorguez.cincopelao.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Un bloque de la pantalla de tareas: las generales (subject == null) o las de una asignatura. */
data class TaskGroup(
    val subject: Subject?,
    /** Tareas sin archivar, ordenadas por fecha límite (sin fecha al final). */
    val tasks: List<Task>
) {
    val key: Long get() = subject?.subjectId ?: GENERAL_KEY

    /** Tareas por hacer: las que no son subtareas y no están completadas (una con subtareas cuenta una vez). */
    val pendingCount: Int get() = tasks.count { it.parentTaskId == null && !it.isCompleted }

    companion object {
        const val GENERAL_KEY = -1L
    }
}

/** Título de un bloque de tareas: el nombre completo si cabe; si no, la abreviatura (si la hay). */
fun groupTitle(name: String, abbrev: String, nameFits: Boolean): String =
    if (nameFits || abbrev.isBlank()) name else abbrev

data class TasksUiState(
    val groups: List<TaskGroup> = emptyList(),
    /** Todas las tareas (también archivadas), para consultar una concreta. */
    val allTasks: List<Task> = emptyList()
) {
    fun task(id: Long): Task? = allTasks.find { it.taskId == id }
    fun hasSubtasks(task: Task): Boolean = allTasks.any { it.parentTaskId == task.taskId && !it.archived }
}

@HiltViewModel
class TasksViewModel @Inject constructor(
    subjects: SubjectRepository,
    private val tasks: TaskRepository
) : ViewModel() {

    /** Se calcula a partir de una sola consulta de tareas (antes había una por asignatura). */
    val uiState: StateFlow<TasksUiState> = combine(subjects.subjects, tasks.tasks) { subjectList, taskList ->
        val active = taskList.filterNot { it.archived }.sortedBy { it.dueDate ?: Long.MAX_VALUE }
        val bySubject = active.groupBy { it.subjectId }
        TasksUiState(
            groups = listOf(TaskGroup(null, bySubject[null].orEmpty())) +
                subjectList.map { TaskGroup(it, bySubject[it.subjectId].orEmpty()) },
            allTasks = taskList
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksUiState())

    fun setCompleted(task: Task, completed: Boolean) = viewModelScope.launch {
        tasks.update(task.copy(isCompleted = completed, completedAt = if (completed) System.currentTimeMillis() else null))
    }

    /** Archiva la tarea junto con sus subtareas. */
    fun archive(task: Task, subtasks: List<Task> = emptyList()) = viewModelScope.launch {
        tasks.setArchived(listOf(task) + subtasks, archived = true)
    }

    /** Crea una tarea nueva o guarda los cambios de [existing]. */
    fun save(existing: Task?, subject: Subject?, title: String, description: String, dueDate: Long?, parentTaskId: Long?) =
        viewModelScope.launch {
            if (existing != null) {
                tasks.update(existing.copy(title = title, description = description, dueDate = dueDate, parentTaskId = parentTaskId))
            } else {
                tasks.insert(
                    Task(
                        title = title,
                        description = description,
                        dueDate = dueDate,
                        subjectId = subject?.subjectId,
                        parentTaskId = parentTaskId
                    )
                )
            }
        }

    /** Borra la tarea con sus subtareas. */
    fun delete(task: Task) = viewModelScope.launch { tasks.delete(task) }

    /** Borra la tarea pero conserva sus subtareas como tareas independientes. */
    fun deleteKeepingSubtasks(task: Task) = viewModelScope.launch { tasks.deleteKeepingSubtasks(task) }
}
