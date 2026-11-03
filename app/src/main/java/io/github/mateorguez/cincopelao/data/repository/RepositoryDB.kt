package io.github.mateorguez.cincopelao.data.repository

import io.github.mateorguez.cincopelao.data.local.AppDatabase
import io.github.mateorguez.cincopelao.data.local.DatabaseManager
import io.github.mateorguez.cincopelao.data.local.dao.EventDao
import io.github.mateorguez.cincopelao.data.local.dao.SubjectDao
import io.github.mateorguez.cincopelao.data.local.dao.TaskDao
import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.local.entity.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class RepositoryDB(
    private val subjectDao: SubjectDao,
    private val eventDao: EventDao,
    private val taskDao: TaskDao
) {

    private val currentDb: AppDatabase?
        get() = DatabaseManager.currentDb

    // ASIGNATURAS --------------------------------

    val allSubjects: Flow<List<Subject>>
        get() = currentDb?.subjectDao()?.getAllSubjects() ?: emptyFlow()

    suspend fun insertSubject(subject: Subject) {
        currentDb?.subjectDao()?.insert(subject)
    }

    suspend fun deleteSubject(subject: Subject) {
        currentDb?.subjectDao()?.delete(subject)
    }

    fun getSubjectById(id: Int): Flow<Subject> = currentDb?.subjectDao()?.getSubjectById(id) ?: emptyFlow()

    suspend fun insertSubjectWithEvents(isEditing: Boolean, subject: Subject, events: List<Event>) {
        currentDb?.subjectDao()?.insertSubjectWithEvents(isEditing, subject, events)
    }

    // EVENTOS --------------------------------

    val allEvents: Flow<List<Event>>
        get() = currentDb?.eventDao()?.getAllEvents() ?: emptyFlow()

    fun getEventsForSubject(subjectId: Int): Flow<List<Event>> =
        currentDb?.eventDao()?.getEventsForSubject(subjectId) ?: emptyFlow()

    suspend fun insertEvent(event: Event) = currentDb?.eventDao()?.insertEvent(event)

    suspend fun insertEvents(events: List<Event>) = currentDb?.eventDao()?.insertEvents(events)

    suspend fun deleteEvent(event: Event) = currentDb?.eventDao()?.deleteEvent(event)

    suspend fun updateEvent(event: Event) {
        currentDb?.eventDao()?.updateEvent(event)
    }

    // TAREAS --------------------------------

    val allTasks: Flow<List<Task>>
        get() = currentDb?.taskDao()?.getAllTasks() ?: emptyFlow()

    fun getTasksBySubject(subjectId: Long): Flow<List<Task>> =
        currentDb?.taskDao()?.getTasksBySubject(subjectId) ?: emptyFlow()

    fun getGeneralTasks(): Flow<List<Task>> =
        currentDb?.taskDao()?.getGeneralTasks() ?: emptyFlow()

    fun getSubTasks(parentId: Long): Flow<List<Task>> =
        currentDb?.taskDao()?.getSubTasks(parentId) ?: emptyFlow()

    suspend fun insertTask(task: Task) {
        currentDb?.taskDao()?.insertTask(task)
    }

    suspend fun updateTask(task: Task) {
        currentDb?.taskDao()?.updateTask(task)
    }

    suspend fun deleteTask(task: Task) {
        currentDb?.taskDao()?.deleteTask(task)
    }

}