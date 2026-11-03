package io.github.mateorguez.cincopelao.data.repository

import io.github.mateorguez.cincopelao.data.instances.InstanceRepository
import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.local.entity.Task
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Acceso a los datos de la instancia activa. Los Flow se re-suscriben solos
 * a la nueva base de datos cuando cambia la instancia.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class RepositoryDB @Inject constructor(
    private val instances: InstanceRepository
) {
    private val db get() = instances.currentDb()

    // ASIGNATURAS --------------------------------

    val allSubjects: Flow<List<Subject>> =
        instances.currentDatabase.flatMapLatest { it.subjectDao().getAllSubjects() }

    suspend fun insertSubject(subject: Subject) {
        db.subjectDao().insert(subject)
    }

    suspend fun deleteSubject(subject: Subject) {
        db.subjectDao().delete(subject)
    }

    fun getSubjectById(id: Int): Flow<Subject> =
        instances.currentDatabase.flatMapLatest { it.subjectDao().getSubjectById(id) }

    suspend fun insertSubjectWithEvents(isEditing: Boolean, subject: Subject, events: List<Event>) {
        db.subjectDao().insertSubjectWithEvents(isEditing, subject, events)
    }

    // EVENTOS --------------------------------

    val allEvents: Flow<List<Event>> =
        instances.currentDatabase.flatMapLatest { it.eventDao().getAllEvents() }

    fun getEventsForSubject(subjectId: Int): Flow<List<Event>> =
        instances.currentDatabase.flatMapLatest { it.eventDao().getEventsForSubject(subjectId) }

    suspend fun insertEvent(event: Event) = db.eventDao().insertEvent(event)

    suspend fun insertEvents(events: List<Event>) = db.eventDao().insertEvents(events)

    suspend fun deleteEvent(event: Event) = db.eventDao().deleteEvent(event)

    suspend fun updateEvent(event: Event) {
        db.eventDao().updateEvent(event)
    }

    // TAREAS --------------------------------

    val allTasks: Flow<List<Task>> =
        instances.currentDatabase.flatMapLatest { it.taskDao().getAllTasks() }

    fun getTasksBySubject(subjectId: Long): Flow<List<Task>> =
        instances.currentDatabase.flatMapLatest { it.taskDao().getTasksBySubject(subjectId) }

    fun getGeneralTasks(): Flow<List<Task>> =
        instances.currentDatabase.flatMapLatest { it.taskDao().getGeneralTasks() }

    fun getSubTasks(parentId: Long): Flow<List<Task>> =
        instances.currentDatabase.flatMapLatest { it.taskDao().getSubTasks(parentId) }

    suspend fun insertTask(task: Task) {
        db.taskDao().insertTask(task)
    }

    suspend fun updateTask(task: Task) {
        db.taskDao().updateTask(task)
    }

    suspend fun deleteTask(task: Task) {
        db.taskDao().deleteTask(task)
    }
}
