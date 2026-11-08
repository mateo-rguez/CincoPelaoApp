package io.github.mateorguez.cincopelao.sharing.protocol

import kotlinx.serialization.Serializable

/** Qué datos pide el receptor. */
@Serializable
data class ShareSelection(
    val subjectsAndEvents: Boolean = false,
    /** Tareas que pertenecen a una asignatura: solo tienen sentido si también se envían las asignaturas. */
    val subjectTasks: Boolean = false,
    /** Tareas sin asignatura. */
    val generalTasks: Boolean = false,
    val schedule: Boolean = false
) {
    val isEmpty: Boolean get() = !subjectsAndEvents && !subjectTasks && !generalTasks && !schedule
}
