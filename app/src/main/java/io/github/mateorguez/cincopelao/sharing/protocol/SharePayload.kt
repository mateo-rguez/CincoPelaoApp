package io.github.mateorguez.cincopelao.sharing.protocol

import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.local.entity.Task

/**
 * Corrige una selección incoherente: las tareas de asignatura sin sus asignaturas quedarían
 * colgando (antes se podían pedir y la importación las convertía en generales sin avisar).
 */
fun ShareSelection.normalized(): ShareSelection =
    if (subjectTasks && !subjectsAndEvents) copy(subjectTasks = false) else this

/**
 * Construye el paquete que se envía según [selection].
 *
 * - El progreso personal no se comparte: los eventos van sin nota y sin marcar como hechos,
 *   y las tareas van como pendientes.
 * - Las tareas archivadas (completadas de días anteriores) no se envían.
 * - Una tarea se clasifica (de asignatura o general) según su tarea raíz, para que una familia
 *   de subtareas viaje siempre entera.
 */
fun buildSharePayload(
    subjects: List<Subject>,
    events: List<Event>,
    tasks: List<Task>,
    selection: ShareSelection
): DataToSend {
    val wanted = selection.normalized()
    val sentSubjects = if (wanted.subjectsAndEvents) subjects else emptyList()
    val sentEvents = if (wanted.subjectsAndEvents) {
        events.map { it.copy(score = null, noPuntuableDone = false) }
    } else {
        emptyList()
    }

    val active = tasks.filterNot { it.archived }
    val byId = active.associateBy { it.taskId }
    val sentTasks = active
        .filter { task ->
            val subjectId = rootOf(task, byId).subjectId
            if (subjectId == null) wanted.generalTasks else wanted.subjectTasks
        }
        .map { it.copy(isCompleted = false, completedAt = null) }

    return DataToSend(subjects = sentSubjects, events = sentEvents, tasks = sentTasks)
}

/** Sube por las madres hasta la raíz (con tope por si hubiera un ciclo). */
private fun rootOf(task: Task, byId: Map<Long, Task>): Task {
    var current = task
    repeat(byId.size) {
        val parent = current.parentTaskId?.let(byId::get) ?: return current
        current = parent
    }
    return current
}

/** Resumen de un paquete para mostrarlo en pantalla: "3 asignaturas · 12 eventos · 5 tareas". */
data class PayloadSummary(val subjects: Int, val events: Int, val subjectTasks: Int, val generalTasks: Int, val scheduleBytes: Int)

fun DataToSend.summary(scheduleBytes: Int = 0): PayloadSummary = PayloadSummary(
    subjects = subjects.size,
    events = events.size,
    subjectTasks = tasks.count { it.subjectId != null },
    generalTasks = tasks.count { it.subjectId == null },
    scheduleBytes = scheduleBytes
)
