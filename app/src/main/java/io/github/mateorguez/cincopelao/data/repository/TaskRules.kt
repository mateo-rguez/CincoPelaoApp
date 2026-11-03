package io.github.mateorguez.cincopelao.data.repository

import io.github.mateorguez.cincopelao.data.local.entity.Task

/**
 * Una tarea con subtareas está completada si y solo si lo están todas sus subtareas.
 * Devuelve la tarea madre corregida, o null si ya es coherente (o no tiene subtareas).
 */
internal fun reconcileWithSubtasks(parent: Task, subtasks: List<Task>, now: Long): Task? {
    if (subtasks.isEmpty()) return null
    val allCompleted = subtasks.all { it.isCompleted }
    return when {
        parent.isCompleted && !allCompleted -> parent.copy(isCompleted = false, completedAt = null)
        !parent.isCompleted && allCompleted -> parent.copy(isCompleted = true, completedAt = now)
        else -> null
    }
}

/** Tareas completadas antes de hoy que todavía no están archivadas. */
internal fun tasksToArchive(tasks: List<Task>, startOfToday: Long): List<Task> =
    tasks.filter { task ->
        task.isCompleted && !task.archived && task.completedAt?.let { it < startOfToday } == true
    }

/** Las tareas indicadas más todas sus subtareas, a cualquier profundidad. */
internal fun List<Task>.withDescendantsOf(rootIds: Set<Long>): List<Task> {
    val byParent = groupBy { it.parentTaskId }
    val result = LinkedHashMap<Long, Task>()
    val queue = ArrayDeque(filter { it.taskId in rootIds })
    while (queue.isNotEmpty()) {
        val task = queue.removeFirst()
        if (result.put(task.taskId, task) == null) {
            byParent[task.taskId]?.let(queue::addAll)
        }
    }
    return result.values.toList()
}
