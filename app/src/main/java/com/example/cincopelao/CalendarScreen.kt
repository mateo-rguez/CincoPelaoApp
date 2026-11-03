package com.example.cincopelao

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.materialIcon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kizitonwose.calendar.compose.CalendarLayoutInfo
import com.kizitonwose.calendar.compose.CalendarState
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.VerticalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.OutDateStyle
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.nextMonth
import com.kizitonwose.calendar.core.previousMonth
import com.pushpal.jetlime.EventPointAnimation
import com.pushpal.jetlime.EventPointType
import com.pushpal.jetlime.EventPosition
import com.pushpal.jetlime.ItemsList
import com.pushpal.jetlime.JetLimeColumn
import com.pushpal.jetlime.JetLimeDefaults
import com.pushpal.jetlime.JetLimeEvent
import com.pushpal.jetlime.JetLimeEventDefaults
import com.pushpal.jetlime.JetLimeEventStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen() {
    val mainViewModel: MainViewModel = hiltViewModel()
    val subjects = mainViewModel.allSubjects.collectAsState(initial = emptyList())
    val events = mainViewModel.allEvents.collectAsState(initial = emptyList())
    val tasks = mainViewModel.allTasks.collectAsState(initial = emptyList()).value
        .filter { it.isCompleted == false && it.archived == false }

    val eventDates = remember(events.value) {
        events.value.mapNotNull { it.date?.toLocalDate() }.toSet()
    }
    val taskDates = remember(tasks) {
        tasks.mapNotNull { it.dueDate?.toLocalDate() }.toSet()
    }
    val highlightedDates = remember(eventDates, taskDates) {
        eventDates union taskDates
    }

    val context = LocalContext.current
    val isDarkTheme by context.getDarkModeFlow().collectAsState(initial = false)

    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(500) }
    val endMonth = remember { currentMonth.plusMonths(500) }
    var selection by remember { mutableStateOf<CalendarDay?>(null) }
    val daysOfWeek = remember { daysOfWeek() }
    val today = remember { LocalDate.now() }
    val bottomSheetState = rememberModalBottomSheetState()
    var isSheetVisible by remember { mutableStateOf(false) }

    val selectedEvents = remember(selection, events.value) {
        events.value.filter { it.date.toLocalDate() == selection?.date }
    }
    val selectedTasks = remember(selection, tasks) {
        tasks.filter { it.dueDate?.toLocalDate() == selection?.date }
    }

    if (isSheetVisible && selection != null) {
        ModalBottomSheet(
            onDismissRequest = { isSheetVisible = false },
            sheetState = bottomSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 400.dp)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                // Título principal con fecha
                Text(
                    text = "Detalles del día",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = selection?.date?.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale.getDefault()))
                        ?.replaceFirstChar { it.uppercaseChar() } ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))

                // Eventos
                if (selectedEvents.isNotEmpty()) {
                    Text(
                        text = "Eventos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (!isDarkTheme) MaterialTheme.colorScheme.primary else Color.Blue
                    )

                    Spacer(Modifier.height(8.dp))

                    selectedEvents.forEach { event ->
                        val subject = subjects.value.find { it.subjectId == event.subjectId }
                        Text(
                            text = "${subject?.name ?: "Asignatura"}: ${event.name ?: "Sin título"}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                }

                // Tareas
                if (selectedTasks.isNotEmpty()) {
                    Text(
                        text = "Tareas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (!isDarkTheme) MaterialTheme.colorScheme.primary else Color.Blue
                    )

                    Spacer(Modifier.height(8.dp))

                    selectedTasks.forEach { task ->
                        val subject = subjects.value.find { it.subjectId.toLong() == task.subjectId }
                        Text(
                            text = "${subject?.name?.plus(": ") ?: ""}${task.title ?: "Sin título"}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                // Si no hay contenido
                if (selectedEvents.isEmpty() && selectedTasks.isEmpty()) {
                    Text(
                        text = "No hay eventos ni tareas para este día.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }

    }

    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(scroll)
    ) {
        val state = rememberCalendarState(
            startMonth = startMonth,
            endMonth = endMonth,
            firstVisibleMonth = currentMonth,
            firstDayOfWeek = daysOfWeek.first(),
            outDateStyle = OutDateStyle.EndOfGrid,
        )
        val coroutineScope = rememberCoroutineScope()
        val visibleMonth by remember {
            derivedStateOf { state.firstVisibleMonth }
        }
        LaunchedEffect(visibleMonth) {
            selection = null
        }

        // Toolbar with month name and arrows
        CalendarHeader(
            calendarState = state,
            onPreviousClick = {
                coroutineScope.launch {
                    state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.minusMonths(1))
                }
            },
            onNextClick = {
                coroutineScope.launch {
                    state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.plusMonths(1))
                }
            }
        )

        // Days of week header
        MonthHeader(
            modifier = Modifier,
            daysOfWeek = daysOfWeek
        )

        // Calendar itself
        HorizontalCalendar(
            modifier = Modifier.wrapContentWidth(),
            state = state,
            dayContent = { day ->
                Day(
                    day = day,
                    today = today,
                    isSelected = selection?.date == day.date,
                    onClick = {
                        selection = day
                        isSheetVisible = true
                    },
                    todayColor = if (isDarkTheme) MaterialTheme.colorScheme.primary else Color.Blue,
                    itemBackgroundColor = if (isDarkTheme) Color(0xFF3A3767) else Color.Cyan,
                    selectedItemColor = if (isDarkTheme) Color.Cyan else Color.Black,
                    inactiveTextColor = Color(0xFF666666),
                    normalTextColor = MaterialTheme.colorScheme.onBackground,
                    highlighted = highlightedDates.contains(day.date),
                )
            }
        )

        // Vertical Timeline -------------------------

        val timeLineEvents: List<TimelineEntry.EventItem> = events.value
            .filter { !it.date.toLocalDate().isBefore(LocalDate.now()) }
            .map { event ->
                val subject = subjects.value.find { it.subjectId == event.subjectId }
                TimelineEntry.EventItem(
                    id = event.eventId.toString(),
                    event = event,
                    subject = subject!!
                )
            }

        val timeLineTasks: List<TimelineEntry.TaskItem> = tasks
            .filter { it.dueDate != null && !it.dueDate.toLocalDate().isBefore(today) }
            .map { task ->
                val subject = subjects.value.find { it.subjectId.toLong() == task.subjectId } // o null si no existe
                TimelineEntry.TaskItem(
                    id = task.taskId.toString(),
                    task = task,
                    subject = subject
                )
            }

        val timeLineEntries = buildTimeline(timeLineEvents, timeLineTasks)

        EventsAndTasksTimeline(timeLineEntries, modifier = Modifier.heightIn(max = 9999.dp))

    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CalendarHeader(
    calendarState: CalendarState,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit
) {
    val visibleMonth = rememberFirstMostVisibleMonth(calendarState, viewportPercent = 50f).yearMonth

    var currentHeaderMonth by remember { mutableStateOf(visibleMonth) }
    var previousMonth by remember { mutableStateOf(visibleMonth) }

    // Detecta cambio manual (swipe) y actualiza estado
    LaunchedEffect(visibleMonth) {
        if (visibleMonth != currentHeaderMonth) {
            previousMonth = currentHeaderMonth
            currentHeaderMonth = visibleMonth
        }
    }

    val direction = if (currentHeaderMonth > previousMonth) 1 else -1

    val monthTitle = currentHeaderMonth.format(
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    ).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {
            val prev = currentHeaderMonth.minusMonths(1)
            previousMonth = currentHeaderMonth
            currentHeaderMonth = prev
            onPreviousClick()
        }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous month",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.weight(1f))


        AnimatedContent(
            targetState = monthTitle,
            transitionSpec = {
                val animSpec = tween<IntOffset>(300)
                if (direction > 0) {
                    slideInHorizontally(animSpec) { it } + fadeIn() togetherWith
                            slideOutHorizontally(animSpec) { -it } + fadeOut()
                } else {
                    slideInHorizontally(animSpec) { -it } + fadeIn() togetherWith
                            slideOutHorizontally(animSpec) { it } + fadeOut()
                }.using(SizeTransform(clip = false))
            }
        ) { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily(Font(R.font.delight_medium))
                ),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(onClick = {
            val next = currentHeaderMonth.plusMonths(1)
            previousMonth = currentHeaderMonth
            currentHeaderMonth = next
            onNextClick()
        }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Next month",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}


@Composable
fun MonthHeader(
    modifier: Modifier = Modifier,
    daysOfWeek: List<DayOfWeek> = emptyList()
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 5.dp)
    ) {
        for (dayOfWeek in daysOfWeek) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight(),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onBackground,
                text = dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()).uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily(Font(R.font.delight_regular)),
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
fun Day(
    day: CalendarDay,
    today: LocalDate,
    isSelected: Boolean = false,
    onClick: (CalendarDay) -> Unit = {},
    itemBackgroundColor: Color = Color.DarkGray,
    todayColor: Color = Color.Cyan,
    selectedItemColor: Color = Color.Green,
    inactiveTextColor: Color = Color.Gray,
    normalTextColor: Color = Color.White,
    highlighted: Boolean = false,
) {
    val isToday = day.date == today

    val borderModifier = when {
        isSelected -> Modifier.border(2.dp, selectedItemColor, shape = RoundedCornerShape(4.dp))
        isToday -> Modifier.border(2.dp, todayColor, shape = RoundedCornerShape(4.dp))
        else -> Modifier
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .then(borderModifier)
            .padding(1.dp)
            .background(color = itemBackgroundColor)
            .clickable(enabled = day.position == DayPosition.MonthDate, onClick = { onClick(day) }),
    ) {

        if (highlighted && day.position == DayPosition.MonthDate) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .size(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onBackground,
                        shape = CircleShape
                    )
            )
        }

        val textColor = when (day.position) {
            DayPosition.MonthDate -> normalTextColor
            DayPosition.InDate, DayPosition.OutDate -> inactiveTextColor
        }

        Text(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp),
            text = day.date.dayOfMonth.toString(),
            color = textColor,
            fontSize = 14.sp,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

// Vertical Timeline ---------------------------------------------

sealed class TimelineEntry {
    data class HeaderItem(val date: LocalDate) : TimelineEntry()
    data class EventItem(
        val id: String,
        val event: Event,
        val subject: Subject
    ) : TimelineEntry()
    data class TaskItem(
        val id: String,
        val task: Task,
        val subject: Subject?
    ) : TimelineEntry()
}

@Composable
fun EventsAndTasksTimeline(
    entries: List<TimelineEntry>,
    modifier: Modifier = Modifier
) {
    JetLimeColumn(
        modifier = modifier.padding(16.dp),
        itemsList = ItemsList(entries),
        key = { _, item ->
            when (item) {
                is TimelineEntry.HeaderItem -> "header-${item.date}"
                is TimelineEntry.EventItem -> "event-${item.id}"
                is TimelineEntry.TaskItem -> "task-${item.id}"
            }
        },
        style = JetLimeDefaults.columnStyle(
            contentDistance = 16.dp,
            itemSpacing = 12.dp,
            lineThickness = 2.dp,
            lineBrush = JetLimeDefaults.lineSolidBrush(color = Color.LightGray),
        )
    ) { index, item, position ->

        val context = LocalContext.current
        val isDarkTheme by context.getDarkModeFlow().collectAsState(initial = false)

        val eventContainerColor = if (isDarkTheme) Color(0xFF1D206B) else Color(0xFF6057EA)
        val taskContainerColor = if (isDarkTheme) Color(0xFF1A3A2B) else Color(0xFF7C981D)

        when (item) {
            is TimelineEntry.HeaderItem -> {
                Text(
                    text = item.date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.Blue,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            is TimelineEntry.EventItem -> {
                val event = item.event
                val subject = item.subject
                JetLimeEvent(
                    style = blueTaskStyle(position)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = eventContainerColor
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = event.date.toLocalDate()
                                    .format(DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = event.name ?: "Evento sin título",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )

                            Spacer(Modifier.height(2.dp))

                            Text(
                                text = "${event.type} - ${subject.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )

                            if (!event.modal.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Modalidad: ${event.modal}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                }
            }

            is TimelineEntry.TaskItem -> {
                val task = item.task
                val subject = item.subject
                JetLimeEvent(
                    style = greenEventStyle(position)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = taskContainerColor
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = task.dueDate!!.toLocalDate()
                                    .format(DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = task.title ?: "Tarea sin título",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )

                            if (subject != null) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Tarea de ${subject.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }

                }
            }
        }
    }
}

fun buildTimeline(events: List<TimelineEntry.EventItem>, tasks: List<TimelineEntry.TaskItem>): List<TimelineEntry> {
    val allItems = (events + tasks)
        .sortedBy {
            when (it) {
                is TimelineEntry.EventItem -> it.event.date.toLocalDate()
                is TimelineEntry.TaskItem -> it.task.dueDate!!.toLocalDate()
                else -> LocalDate.now()
            }
        }

    return allItems
        .groupBy {
            when (it) {
                is TimelineEntry.EventItem -> it.event.date.toLocalDate()
                is TimelineEntry.TaskItem -> it.task.dueDate!!.toLocalDate()
                else -> LocalDate.now()
            }
        }
        .toSortedMap()
        .flatMap { (date, items) ->
            listOf(TimelineEntry.HeaderItem(date)) + items
        }
}

@Composable
fun greenEventStyle(position: EventPosition): JetLimeEventStyle {
    // Usamos los estilos por defecto y reemplazamos parcialmente lo necesario
    val default = JetLimeEventDefaults.eventStyle(position)
    return JetLimeEventDefaults.eventStyle(
        position = position,
        pointType = default.pointType,
        pointColor = Color(0xFF4CAF50),
        pointFillColor = default.pointFillColor,
        pointRadius = default.pointRadius,
        pointAnimation = default.pointAnimation,
        pointStrokeWidth = default.pointStrokeWidth,
        pointStrokeColor = default.pointStrokeColor,
    )
}

@Composable
fun blueTaskStyle(position: EventPosition): JetLimeEventStyle {
    val default = JetLimeEventDefaults.eventStyle(position)
    return JetLimeEventDefaults.eventStyle(
        position = position,
        pointType = default.pointType,
        pointColor = Color(0xFF2196F3), // Azul
        pointFillColor = default.pointFillColor,
        pointRadius = default.pointRadius,
        pointAnimation = default.pointAnimation,
        pointStrokeWidth = default.pointStrokeWidth,
        pointStrokeColor = default.pointStrokeColor,
    )
}

// Kititonwose Calendar Utils ------------------------------------

@Composable
fun rememberFirstMostVisibleMonth(
    state: CalendarState,
    viewportPercent: Float = 50f,
): CalendarMonth {
    val visibleMonth = remember(state) { mutableStateOf(state.firstVisibleMonth) }
    LaunchedEffect(state) {
        snapshotFlow { state.layoutInfo.firstMostVisibleMonth(viewportPercent) }
            .filterNotNull()
            .collect { month -> visibleMonth.value = month }
    }
    return visibleMonth.value
}

private fun CalendarLayoutInfo.firstMostVisibleMonth(viewportPercent: Float = 50f): CalendarMonth? {
    return if (visibleMonthsInfo.isEmpty()) {
        null
    } else {
        val viewportSize = (viewportEndOffset + viewportStartOffset) * viewportPercent / 100f
        visibleMonthsInfo.firstOrNull { itemInfo ->
            if (itemInfo.offset < 0) {
                itemInfo.offset + itemInfo.size >= viewportSize
            } else {
                itemInfo.size - itemInfo.offset >= viewportSize
            }
        }?.month
    }
}

// --------------------------------------------------------------