package io.github.mateorguez.cincopelao.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.local.entity.Task
import io.github.mateorguez.cincopelao.data.repository.EventRepository
import io.github.mateorguez.cincopelao.data.repository.SubjectRepository
import io.github.mateorguez.cincopelao.data.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

fun Long.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

/** Elemento de la línea de tiempo: una cabecera de día, un evento o una tarea. */
sealed class TimelineEntry {
    data class HeaderItem(val date: LocalDate) : TimelineEntry()
    data class EventItem(val event: Event, val subject: Subject?) : TimelineEntry()
    data class TaskItem(val task: Task, val subject: Subject?) : TimelineEntry()

    /** Clave estable para la lista. */
    val key: String
        get() = when (this) {
            is HeaderItem -> "header-$date"
            is EventItem -> "event-${event.eventId}"
            is TaskItem -> "task-${task.taskId}"
        }
}

/**
 * Línea de tiempo desde [today]: eventos y tareas (con fecha) agrupados por día,
 * con una cabecera por día y en orden cronológico.
 */
fun buildTimeline(
    events: List<Event>,
    tasks: List<Task>,
    subjects: List<Subject>,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault()
): List<TimelineEntry> {
    val subjectById = subjects.associateBy { it.subjectId }
    val dated: List<Pair<LocalDate, TimelineEntry>> =
        events.map { it.date.toLocalDate(zone) to TimelineEntry.EventItem(it, subjectById[it.subjectId]) } +
            tasks.mapNotNull { task ->
                task.dueDate?.let { it.toLocalDate(zone) to TimelineEntry.TaskItem(task, task.subjectId?.let(subjectById::get)) }
            }
    return dated
        .filter { (date, _) -> !date.isBefore(today) }
        .sortedBy { (date, _) -> date }
        .groupBy({ it.first }, { it.second })
        .flatMap { (date, items) -> listOf(TimelineEntry.HeaderItem(date)) + items }
}

data class CalendarUiState(
    val subjects: List<Subject> = emptyList(),
    val events: List<Event> = emptyList(),
    /** Tareas pendientes (ni completadas ni archivadas). */
    val tasks: List<Task> = emptyList(),
    /** Días con algún evento o tarea, para marcarlos en el calendario. */
    val highlightedDates: Set<LocalDate> = emptySet(),
    val timeline: List<TimelineEntry> = emptyList()
) {
    fun subject(id: Long?): Subject? = id?.let { subjects.find { s -> s.subjectId == it } }
    fun eventsOn(date: LocalDate): List<Event> = events.filter { it.date.toLocalDate() == date }
    fun tasksOn(date: LocalDate): List<Task> = tasks.filter { it.dueDate?.toLocalDate() == date }
}

@HiltViewModel
class CalendarViewModel @Inject constructor(
    subjects: SubjectRepository,
    events: EventRepository,
    tasks: TaskRepository
) : ViewModel() {

    /** Asignaturas, eventos y tareas llegan juntos, así nunca se pinta un evento sin su asignatura. */
    val uiState: StateFlow<CalendarUiState> =
        combine(subjects.subjects, events.events, tasks.tasks) { subjectList, eventList, taskList ->
            val pending = taskList.filter { !it.isCompleted && !it.archived }
            CalendarUiState(
                subjects = subjectList,
                events = eventList,
                tasks = pending,
                highlightedDates = eventList.map { it.date.toLocalDate() }.toSet() +
                    pending.mapNotNull { it.dueDate?.toLocalDate() },
                timeline = buildTimeline(eventList, pending, subjectList, LocalDate.now())
            )
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())
}
