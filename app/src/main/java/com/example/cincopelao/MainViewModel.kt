package com.example.cincopelao

import com.example.cincopelao.Subject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: RepositoryDB
) : ViewModel() {

    init {
        manageTasks()
    }

    // ASIGNATURAS --------------------------------

    val allSubjects = repository.allSubjects

    fun deleteSubject(subject: Subject) = viewModelScope.launch {
        repository.deleteSubject(subject)
    }

    fun insertSubjectWithEvents(isEditing: Boolean, subject: Subject, events: List<Event>) {
        viewModelScope.launch {
            repository.insertSubjectWithEvents(isEditing, subject, events)
        }
    }

    suspend fun getSubjectById(id: Int): Subject {
        return repository.getSubjectById(id).first()
    }

    // EVENTOS --------------------------------

    val allEvents = repository.allEvents

    suspend fun getEventsForSubject(subjectId: Int): List<Event> =
        repository.getEventsForSubject(subjectId).first()

    fun getEventsForSubjectFlow(subjectId: Int): Flow<List<Event>> {
        return repository.getEventsForSubject(subjectId)
    }

    fun insertEvent(event: Event) = viewModelScope.launch {
        repository.insertEvent(event)
    }

    fun insertEvents(events: List<Event>) = viewModelScope.launch {
        repository.insertEvents(events)
    }

    fun deleteEvent(event: Event) = viewModelScope.launch {
        repository.deleteEvent(event)
    }

    // TAREAS --------------------------------

    val allTasks = repository.allTasks

    fun getTasksBySubject(subjectId: Long): Flow<List<Task>> {
        return repository.getTasksBySubject(subjectId)
    }

    fun getGeneralTasks(): Flow<List<Task>> {
        return repository.getGeneralTasks()
    }

    fun getSubTasks(parentId: Long): Flow<List<Task>> {
        return repository.getSubTasks(parentId)
    }

    fun insertTask(task: Task) = viewModelScope.launch {
        repository.insertTask(task)
    }

    fun updateTask(task: Task) = viewModelScope.launch {
        repository.updateTask(task)
    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        repository.deleteTask(task)
    }

    private fun manageTasks() {
        viewModelScope.launch {
            archiveOldCompletedTasks()
        }
        validateParentTaskCompletion()
        startAutoArchivingLoop()
    }

    private fun validateParentTaskCompletion() {
        viewModelScope.launch {
            repository.allTasks.collect { tasks ->
                val tasksByParent = tasks.groupBy { it.parentTaskId }

                tasks.forEach { task ->
                    val subtasks = tasksByParent[task.taskId] ?: emptyList()

                    if (subtasks.isNotEmpty()) {
                        val allSubtasksCompleted = subtasks.all { it.isCompleted }

                        when {
                            // está marcada como completada pero alguna sub no lo está → desmarcar
                            task.isCompleted && !allSubtasksCompleted -> {
                                updateTask(task.copy(isCompleted = false, completedAt = null))
                            }

                            // no está completada y todas las subtareas sí → marcar y poner timestamp
                            !task.isCompleted && allSubtasksCompleted -> {
                                updateTask(task.copy(isCompleted = true, completedAt = System.currentTimeMillis()))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startAutoArchivingLoop() {
        viewModelScope.launch {
            while (isActive) {
                val now = Calendar.getInstance()
                val nextMidnight = Calendar.getInstance().apply {
                    timeInMillis = now.timeInMillis
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val millisUntilMidnight = nextMidnight.timeInMillis - now.timeInMillis

                delay(millisUntilMidnight)
                archiveOldCompletedTasks()
            }
        }
    }

    private suspend fun archiveOldCompletedTasks() {
        val tasks = repository.allTasks.firstOrNull() ?: emptyList()

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val tasksToArchive = tasks.filter { task ->
            task.isCompleted &&
                    !task.archived &&
                    task.completedAt?.let { it < today } == true
        }

        tasksToArchive.forEach { task ->
            updateTask(task.copy(archived = true))
        }
    }
}