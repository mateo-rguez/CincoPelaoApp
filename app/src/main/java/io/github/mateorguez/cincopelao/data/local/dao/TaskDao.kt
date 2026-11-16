package io.github.mateorguez.cincopelao.data.local.dao

import androidx.room.*
import io.github.mateorguez.cincopelao.data.local.entity.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // Insertar una nueva tarea
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    // Actualizar una tarea
    @Update
    suspend fun updateTask(task: Task)

    // Borrar una tarea
    @Delete
    suspend fun deleteTask(task: Task)

    // Obtener todas las tareas
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    fun getAllTasks(): Flow<List<Task>>

    // Obtener tareas generales (sin asignatura)
    @Query("SELECT * FROM tasks WHERE subjectId IS NULL ORDER BY dueDate ASC")
    fun getGeneralTasks(): Flow<List<Task>>

    // Obtener tareas por asignatura
    @Query("SELECT * FROM tasks WHERE subjectId = :subjectId ORDER BY dueDate ASC")
    fun getTasksBySubject(subjectId: Long): Flow<List<Task>>

    // Obtener una tarea específica por ID
    /** Tareas pendientes (sin contar subtareas ni archivadas), para el resumen de cada instancia. */
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0 AND archived = 0 AND parentTaskId IS NULL")
    suspend fun countPending(): Int

    @Query("SELECT * FROM tasks WHERE taskId = :taskId")
    suspend fun getTaskById(taskId: Long): Task?

    // Obtener subtareas de una tarea
    @Query("SELECT * FROM tasks WHERE parentTaskId = :parentId ORDER BY dueDate ASC")
    fun getSubTasks(parentId: Long): Flow<List<Task>>

    // Consultas puntuales (sin Flow) para operaciones del repositorio
    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksOnce(): List<Task>

    @Query("SELECT * FROM tasks WHERE parentTaskId = :parentId")
    suspend fun getSubTasksOnce(parentId: Long): List<Task>

    // Obtener tareas completadas
    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY dueDate ASC")
    fun getCompletedTasks(): Flow<List<Task>>

    // Marcar tarea como completada
    @Query("UPDATE tasks SET isCompleted = 1 WHERE taskId = :taskId")
    suspend fun markTaskAsCompleted(taskId: Long)

    // Marcar tarea como incompleta
    @Query("UPDATE tasks SET isCompleted = 0 WHERE taskId = :taskId")
    suspend fun markTaskAsIncomplete(taskId: Long)
}