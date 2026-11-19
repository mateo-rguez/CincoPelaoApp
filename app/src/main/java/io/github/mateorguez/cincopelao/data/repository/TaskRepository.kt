package io.github.mateorguez.cincopelao.data.repository

import androidx.room.withTransaction
import io.github.mateorguez.cincopelao.data.instances.InstanceRepository
import io.github.mateorguez.cincopelao.data.local.AppDatabase
import io.github.mateorguez.cincopelao.data.local.entity.Task
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tareas de la instancia activa.
 *
 * Todas las escrituras mantienen la regla de que una tarea con subtareas está completada
 * solo si lo están todas (ver [reconcileWithSubtasks]), y las operaciones compuestas
 * se hacen en una única transacción.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class TaskRepository @Inject constructor(
    private val instances: InstanceRepository
) {
    private val db get() = instances.currentDb()

    val tasks: Flow<List<Task>> =
        instances.currentDatabase.flatMapLatest { it.taskDao().getAllTasks() }

    fun generalTasks(): Flow<List<Task>> =
        instances.currentDatabase.flatMapLatest { it.taskDao().getGeneralTasks() }

    fun tasksBySubject(subjectId: Long): Flow<List<Task>> =
        instances.currentDatabase.flatMapLatest { it.taskDao().getTasksBySubject(subjectId) }

    fun subtasks(parentId: Long): Flow<List<Task>> =
        instances.currentDatabase.flatMapLatest { it.taskDao().getSubTasks(parentId) }

    suspend fun insert(task: Task) {
        val db = db
        db.withTransaction {
            db.taskDao().insertTask(task)
            task.parentTaskId?.let { db.syncParent(it) }
        }
    }

    suspend fun update(task: Task) {
        val db = db
        db.withTransaction {
            val previousParent = db.taskDao().getTaskById(task.taskId)?.parentTaskId
            db.taskDao().updateTask(task)
            setOfNotNull(previousParent, task.parentTaskId, task.taskId).forEach { db.syncParent(it) }
        }
    }

    /**
     * Marca como hecha una tarea de cualquier instancia (desde el botón de su aviso), junto con
     * sus subtareas pendientes: «ya la hice» vale para la tarea entera. Devuelve false si la
     * tarea o la instancia ya no existen.
     */
    suspend fun completeFromReminder(instanceId: String, taskId: Long, now: Long = System.currentTimeMillis()): Boolean {
        if (instances.instances.value.none { it.id == instanceId }) return false
        val db = instances.database(instanceId)
        return db.withTransaction {
            val task = db.taskDao().getTaskById(taskId) ?: return@withTransaction false
            (listOf(task) + db.taskDao().getSubTasksOnce(taskId))
                .filter { !it.isCompleted }
                .forEach { db.taskDao().updateTask(it.copy(isCompleted = true, completedAt = now)) }
            task.parentTaskId?.let { db.syncParent(it) }
            true
        }
    }

    /** Borra la tarea; sus subtareas se borran en cascada. */
    suspend fun delete(task: Task) {
        val db = db
        db.withTransaction {
            db.taskDao().deleteTask(task)
            task.parentTaskId?.let { db.syncParent(it) }
        }
    }

    /** Borra la tarea pero conserva sus subtareas como tareas independientes. */
    suspend fun deleteKeepingSubtasks(task: Task) {
        val db = db
        db.withTransaction {
            db.taskDao().getSubTasksOnce(task.taskId).forEach {
                db.taskDao().updateTask(it.copy(parentTaskId = null))
            }
            db.taskDao().deleteTask(task)
            task.parentTaskId?.let { db.syncParent(it) }
        }
    }

    /** Borra la tarea y sus subtareas y las devuelve para poder deshacerlo con [restore]. */
    suspend fun deleteWithSubtasks(task: Task): List<Task> {
        val db = db
        return db.withTransaction {
            val deleted = db.taskDao().getAllTasksOnce().withDescendantsOf(setOf(task.taskId))
            db.taskDao().deleteTask(task)
            task.parentTaskId?.let { db.syncParent(it) }
            deleted
        }
    }

    /** Archiva o desarchiva varias tareas a la vez (p. ej. una tarea madre y sus subtareas). */
    suspend fun setArchived(tasks: List<Task>, archived: Boolean) {
        val db = db
        db.withTransaction { tasks.forEach { db.taskDao().updateTask(it.copy(archived = archived)) } }
    }

    suspend fun deleteAll(tasks: List<Task>) {
        val db = db
        db.withTransaction { tasks.forEach { db.taskDao().deleteTask(it) } }
    }

    /** Vuelve a insertar tareas borradas, siempre las madres antes que sus subtareas. */
    suspend fun restore(tasks: List<Task>) {
        val db = db
        db.withTransaction {
            tasks.parentsFirst().forEach { db.taskDao().insertTask(it) }
            tasks.mapNotNullTo(HashSet()) { it.parentTaskId }.forEach { db.syncParent(it) }
        }
    }

    /**
     * Mantenimiento: archiva las tareas completadas antes de hoy y corrige cualquier
     * tarea madre que no sea coherente con sus subtareas.
     */
    suspend fun runMaintenance() {
        val db = db
        db.withTransaction {
            val all = db.taskDao().getAllTasksOnce()
            tasksToArchive(all, startOfToday()).forEach { db.taskDao().updateTask(it.copy(archived = true)) }
            all.mapNotNullTo(HashSet()) { it.parentTaskId }.forEach { db.syncParent(it) }
        }
    }

    private suspend fun AppDatabase.syncParent(parentId: Long) {
        val parent = taskDao().getTaskById(parentId) ?: return
        val subtasks = taskDao().getSubTasksOnce(parentId)
        reconcileWithSubtasks(parent, subtasks, System.currentTimeMillis())?.let { taskDao().updateTask(it) }
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
