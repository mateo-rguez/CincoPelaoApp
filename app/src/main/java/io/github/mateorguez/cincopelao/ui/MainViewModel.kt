package io.github.mateorguez.cincopelao.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mateorguez.cincopelao.data.instances.DatabaseInstance
import io.github.mateorguez.cincopelao.data.instances.InstanceRepository
import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.local.entity.Task
import io.github.mateorguez.cincopelao.data.repository.DeletedSubject
import io.github.mateorguez.cincopelao.data.repository.EventRepository
import io.github.mateorguez.cincopelao.data.repository.SubjectRepository
import io.github.mateorguez.cincopelao.data.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val subjects: SubjectRepository,
    private val events: EventRepository,
    private val tasks: TaskRepository,
    instanceRepository: InstanceRepository
) : ViewModel() {

    // INSTANCIA ACTIVA --------------------------------

    val currentInstanceId: StateFlow<String> = instanceRepository.currentInstanceId

    val currentInstance: StateFlow<DatabaseInstance?> = instanceRepository.currentInstance.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        instanceRepository.instances.value.find { it.id == instanceRepository.currentInstanceId.value }
    )

    // ASIGNATURAS --------------------------------

    val allSubjects: Flow<List<Subject>> = subjects.subjects

    /** Borra la asignatura con sus eventos y tareas; devuelve lo borrado para poder deshacerlo. */
    suspend fun deleteSubject(subject: Subject): DeletedSubject = subjects.delete(subject)

    fun restoreSubject(deleted: DeletedSubject) = viewModelScope.launch {
        subjects.restore(deleted)
    }

    fun insertSubjectWithEvents(isEditing: Boolean, subject: Subject, events: List<Event>) {
        viewModelScope.launch {
            subjects.saveWithEvents(isEditing, subject, events)
        }
    }

    suspend fun getSubjectById(id: Int): Subject? = subjects.subject(id).first()

    // EVENTOS --------------------------------

    val allEvents: Flow<List<Event>> = events.events

    suspend fun getEventsForSubject(subjectId: Int): List<Event> =
        events.eventsForSubject(subjectId).first()

    fun getEventsForSubjectFlow(subjectId: Int): Flow<List<Event>> = events.eventsForSubject(subjectId)

    fun insertEvent(event: Event) = viewModelScope.launch { events.insert(event) }

    fun insertEvents(list: List<Event>) = viewModelScope.launch { events.insertAll(list) }

    fun deleteEvent(event: Event) = viewModelScope.launch { events.delete(event) }

    // TAREAS --------------------------------

    val allTasks: Flow<List<Task>> = tasks.tasks

    fun getTasksBySubject(subjectId: Long): Flow<List<Task>> = tasks.tasksBySubject(subjectId)

    fun getGeneralTasks(): Flow<List<Task>> = tasks.generalTasks()

    fun getSubTasks(parentId: Long): Flow<List<Task>> = tasks.subtasks(parentId)

    fun insertTask(task: Task) = viewModelScope.launch { tasks.insert(task) }

    fun updateTask(task: Task) = viewModelScope.launch { tasks.update(task) }

    /** Borra la tarea; sus subtareas se borran con ella. */
    fun deleteTask(task: Task) = viewModelScope.launch { tasks.delete(task) }

    /** Borra la tarea pero conserva sus subtareas como tareas independientes. */
    fun deleteTaskKeepingSubtasks(task: Task) = viewModelScope.launch { tasks.deleteKeepingSubtasks(task) }

    /** Borra la tarea y sus subtareas; devuelve lo borrado para poder deshacerlo. */
    suspend fun deleteTaskWithSubtasks(task: Task): List<Task> = tasks.deleteWithSubtasks(task)

    fun deleteTasks(list: List<Task>) = viewModelScope.launch { tasks.deleteAll(list) }

    /** Deshace un borrado: reinserta las tareas en el orden correcto (madres primero). */
    fun restoreTasks(list: List<Task>) = viewModelScope.launch { tasks.restore(list) }
}
