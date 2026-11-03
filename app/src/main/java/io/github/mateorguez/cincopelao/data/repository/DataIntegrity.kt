package io.github.mateorguez.cincopelao.data.repository

import io.github.mateorguez.cincopelao.data.local.entity.Task
import io.github.mateorguez.cincopelao.sharing.protocol.DataToSend

/**
 * Ordena las tareas para que cada tarea madre vaya antes que sus subtareas,
 * de forma que se puedan insertar sin violar la clave foránea `parentTaskId`.
 *
 * Las tareas cuya madre no está en la lista se tratan como raíz. Si hubiera un ciclo
 * (no debería), se rompe quitando la madre de la tarea que lo cierra.
 */
internal fun List<Task>.parentsFirst(): List<Task> {
    val ids = mapTo(HashSet()) { it.taskId }
    val pending = map { if (it.parentTaskId != null && it.parentTaskId !in ids) it.copy(parentTaskId = null) else it }
        .toMutableList()
    val placed = HashSet<Long>()
    val result = ArrayList<Task>(size)
    while (pending.isNotEmpty()) {
        val ready = pending.filter { it.parentTaskId == null || it.parentTaskId in placed }
        if (ready.isEmpty()) {
            val breaker = pending.removeAt(0).copy(parentTaskId = null)
            result += breaker
            placed += breaker.taskId
            continue
        }
        pending.removeAll(ready.toSet())
        result += ready
        ready.mapTo(placed) { it.taskId }
    }
    return result
}

/**
 * Limpia un paquete recibido para que se pueda guardar sin errores de claves foráneas:
 * - Los eventos de asignaturas que no vienen en el paquete se descartan (no existen sin asignatura).
 * - Las tareas de asignaturas que no vienen pasan a ser tareas generales.
 * - Las subtareas cuya madre no viene pasan a ser tareas independientes.
 */
internal fun DataToSend.withoutDanglingReferences(): DataToSend {
    val subjectIds = subjects.mapTo(HashSet()) { it.subjectId }
    val taskIds = tasks.mapTo(HashSet()) { it.taskId }
    return copy(
        events = events.filter { it.subjectId in subjectIds },
        tasks = tasks.map { task ->
            task.copy(
                subjectId = task.subjectId?.takeIf { it.toInt() in subjectIds },
                parentTaskId = task.parentTaskId?.takeIf { it in taskIds }
            )
        }
    )
}
