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
import io.github.mateorguez.cincopelao.ui.subjects.subjectBadge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

fun Long.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

/** Elemento de la línea de tiempo: un evento o una tarea, con su asignatura si la tiene. */
sealed class TimelineItem {
    abstract val subject: Subject?

    data class EventItem(val event: Event, override val subject: Subject?) : TimelineItem()
    data class TaskItem(val task: Task, override val subject: Subject?) : TimelineItem()

    /** Clave estable para la lista. */
    val key: String
        get() = when (this) {
            is EventItem -> "event-${event.eventId}"
            is TaskItem -> "task-${task.taskId}"
        }
}

/** Un día de la línea de tiempo con lo que hay en él. */
data class TimelineDay(val date: LocalDate, val items: List<TimelineItem>)

/**
 * Línea de tiempo desde [today]: eventos y tareas (con fecha) agrupados por día y en orden
 * cronológico; dentro de un día, primero los eventos.
 */
fun buildTimeline(
    events: List<Event>,
    tasks: List<Task>,
    subjects: List<Subject>,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault()
): List<TimelineDay> {
    val subjectById = subjects.associateBy { it.subjectId }
    val dated: List<Pair<LocalDate, TimelineItem>> =
        events.map { it.date.toLocalDate(zone) to TimelineItem.EventItem(it, subjectById[it.subjectId]) } +
            tasks.mapNotNull { task ->
                task.dueDate?.let { it.toLocalDate(zone) to TimelineItem.TaskItem(task, task.subjectId?.let(subjectById::get)) }
            }
    return dated
        .filter { (date, _) -> !date.isBefore(today) }
        .sortedBy { (date, _) -> date }
        .groupBy({ it.first }, { it.second })
        .map { (date, items) -> TimelineDay(date, items) }
}

/**
 * Cuánto falta para un día: "Hoy", "Mañana", "En 6 días" hasta dos semanas, luego en semanas
 * ("En 3 sem.") hasta dos meses y después en meses ("En 3 meses"), redondeando.
 */
fun relativeDayLabel(date: LocalDate, today: LocalDate): String =
    when (val days = ChronoUnit.DAYS.between(today, date)) {
        0L -> "Hoy"
        1L -> "Mañana"
        in 2L..13L -> "En $days días"
        in 14L..59L -> "En ${Math.round(days / 7.0)} sem."
        else -> {
            // Desde 60 días siempre salen al menos 2 meses
            val months = Math.round(days / 30.44)
            "En $months meses"
        }
    }

/** Cabecera de un día en la línea de tiempo: "Martes 17 nov". */
fun dayHeaderLabel(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    "${date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)} ${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.SHORT, locale)}"
        .replace(".", "")
        .replaceFirstChar { it.titlecase(locale) }

/** Tipo de un elemento de la línea de tiempo: decide el color de su etiqueta. */
enum class TimelineMarker { EXAM, DELIVERABLE, OTHER_EVENT, TASK }

fun TimelineItem.marker(): TimelineMarker = when (this) {
    is TimelineItem.TaskItem -> TimelineMarker.TASK
    is TimelineItem.EventItem -> when (event.type) {
        "Examen" -> TimelineMarker.EXAM
        "Entregable", "Proyecto", "Trabajo" -> TimelineMarker.DELIVERABLE
        else -> TimelineMarker.OTHER_EVENT
    }
}

/** Etiqueta del tipo en la tarjeta: "Examen · Teoría" para un evento, "Tarea" para una tarea. */
fun TimelineItem.typeLabel(): String = when (this) {
    is TimelineItem.TaskItem -> "Tarea"
    is TimelineItem.EventItem -> listOfNotNull(event.type, event.modal?.takeIf { it.isNotBlank() }).joinToString(" · ")
}

/** Línea de la asignatura: "PSI · Programación de Sistemas Inteligentes"; null si no tiene. */
fun TimelineItem.subjectLine(): String? = subject?.let { "${subjectBadge(it)} · ${it.name}" }

data class CalendarUiState(
    val subjects: List<Subject> = emptyList(),
    val events: List<Event> = emptyList(),
    /** Tareas pendientes (ni completadas ni archivadas). */
    val tasks: List<Task> = emptyList(),
    /** Días con algún evento o tarea, para marcarlos en el calendario. */
    val highlightedDates: Set<LocalDate> = emptySet(),
    val timeline: List<TimelineDay> = emptyList()
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
