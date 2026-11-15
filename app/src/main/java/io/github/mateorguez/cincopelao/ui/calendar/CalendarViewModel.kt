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
import kotlin.math.abs
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
 * Todo lo que hay cada día, también lo pasado: eventos y tareas con fecha, y dentro de un día
 * primero los eventos. De aquí salen la línea de tiempo, los puntos de la cuadrícula y la hoja
 * de cada día.
 */
fun itemsByDate(
    events: List<Event>,
    tasks: List<Task>,
    subjects: List<Subject>,
    zone: ZoneId = ZoneId.systemDefault()
): Map<LocalDate, List<TimelineItem>> {
    val subjectById = subjects.associateBy { it.subjectId }
    val dated: List<Pair<LocalDate, TimelineItem>> =
        events.map { it.date.toLocalDate(zone) to TimelineItem.EventItem(it, subjectById[it.subjectId]) } +
            tasks.mapNotNull { task ->
                task.dueDate?.let { it.toLocalDate(zone) to TimelineItem.TaskItem(task, task.subjectId?.let(subjectById::get)) }
            }
    return dated.groupBy({ it.first }, { it.second })
}

/** Línea de tiempo desde [today]: los días con algo, en orden cronológico. */
fun buildTimeline(byDate: Map<LocalDate, List<TimelineItem>>, today: LocalDate): List<TimelineDay> =
    byDate
        .filterKeys { !it.isBefore(today) }
        .toSortedMap()
        .map { (date, items) -> TimelineDay(date, items) }

fun buildTimeline(
    events: List<Event>,
    tasks: List<Task>,
    subjects: List<Subject>,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault()
): List<TimelineDay> = buildTimeline(itemsByDate(events, tasks, subjects, zone), today)

/** Puntos de un día en la cuadrícula: uno por elemento, hasta [max]; [more] si no caben todos. */
data class GridDots(val markers: List<TimelineMarker>, val more: Boolean)

fun gridDots(items: List<TimelineItem>, max: Int = 4): GridDots =
    GridDots(items.take(max).map { it.marker() }, more = items.size > max)

/**
 * Cuánto falta para un día: "Hoy", "Mañana", "En 6 días" hasta dos semanas, luego en semanas
 * ("En 3 sem.") hasta dos meses y después en meses ("En 3 meses"), redondeando. Para un día
 * pasado (se abre desde la cuadrícula), lo mismo hacia atrás: "Ayer", "Hace 6 días"…
 */
fun relativeDayLabel(date: LocalDate, today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(today, date)
    val distance = abs(days)
    val amount = when (distance) {
        0L -> return "Hoy"
        1L -> return if (days > 0) "Mañana" else "Ayer"
        in 2L..13L -> "$distance días"
        in 14L..59L -> "${Math.round(distance / 7.0)} sem."
        // Desde 60 días siempre salen al menos 2 meses
        else -> "${Math.round(distance / 30.44)} meses"
    }
    return if (days > 0) "En $amount" else "Hace $amount"
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

/** Línea corta para la hoja del día: "Examen · Teoría · PSI", "Tarea" si no tiene asignatura. */
fun TimelineItem.shortLine(): String = listOfNotNull(typeLabel(), subject?.let(::subjectBadge)).joinToString(" · ")

data class CalendarUiState(
    /** Eventos y tareas pendientes (ni completadas ni archivadas) de cada día. */
    val itemsByDate: Map<LocalDate, List<TimelineItem>> = emptyMap(),
    val timeline: List<TimelineDay> = emptyList()
) {
    fun itemsOn(date: LocalDate): List<TimelineItem> = itemsByDate[date].orEmpty()
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
            val byDate = itemsByDate(eventList, pending, subjectList)
            CalendarUiState(itemsByDate = byDate, timeline = buildTimeline(byDate, LocalDate.now()))
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())
}
