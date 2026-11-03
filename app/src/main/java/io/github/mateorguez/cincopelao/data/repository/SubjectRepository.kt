package io.github.mateorguez.cincopelao.data.repository

import androidx.room.withTransaction
import io.github.mateorguez.cincopelao.data.instances.InstanceRepository
import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.local.entity.Task
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

/** Todo lo que se borra junto con una asignatura, para poder deshacerlo. */
data class DeletedSubject(
    val subject: Subject,
    val events: List<Event>,
    val tasks: List<Task>
)

/** Asignaturas de la instancia activa. */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SubjectRepository @Inject constructor(
    private val instances: InstanceRepository
) {
    private val db get() = instances.currentDb()

    val subjects: Flow<List<Subject>> =
        instances.currentDatabase.flatMapLatest { it.subjectDao().getAllSubjects() }

    fun subject(id: Int): Flow<Subject?> =
        instances.currentDatabase.flatMapLatest { it.subjectDao().getSubjectById(id) }

    /** Crea o edita una asignatura junto con sus eventos (en una transacción). */
    suspend fun saveWithEvents(isEditing: Boolean, subject: Subject, events: List<Event>) {
        db.subjectDao().insertSubjectWithEvents(isEditing, subject, events)
    }

    /**
     * Borra la asignatura. Sus eventos y tareas se borran en cascada; se devuelven
     * para poder restaurarlos con [restore].
     */
    suspend fun delete(subject: Subject): DeletedSubject {
        val db = db
        return db.withTransaction {
            val events = db.eventDao().getEventsForSubjectOnce(subject.subjectId)
            val allTasks = db.taskDao().getAllTasksOnce()
            val ownIds = allTasks.filter { it.subjectId == subject.subjectId.toLong() }.mapTo(HashSet()) { it.taskId }
            val tasks = allTasks.withDescendantsOf(ownIds)
            db.subjectDao().delete(subject)
            DeletedSubject(subject, events, tasks)
        }
    }

    /** Deshace un [delete]: vuelve a insertar la asignatura, sus eventos y sus tareas. */
    suspend fun restore(deleted: DeletedSubject) {
        val db = db
        db.withTransaction {
            db.subjectDao().insert(deleted.subject)
            db.eventDao().insertEvents(deleted.events)
            deleted.tasks.parentsFirst().forEach { db.taskDao().insertTask(it) }
        }
    }
}
