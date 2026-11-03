package io.github.mateorguez.cincopelao.ui.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.OutDateStyle
import com.kizitonwose.calendar.core.daysOfWeek
import io.github.mateorguez.cincopelao.ui.theme.CalendarInactiveText
import io.github.mateorguez.cincopelao.ui.theme.CalendarSelectedDayDark
import io.github.mateorguez.cincopelao.ui.theme.LocalDarkTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

/** Pestaña de calendario: mes con los días marcados y, debajo, la línea de tiempo de lo próximo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDarkTheme = LocalDarkTheme.current

    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(500) }
    val endMonth = remember { currentMonth.plusMonths(500) }
    var selection by remember { mutableStateOf<CalendarDay?>(null) }
    val daysOfWeek = remember { daysOfWeek() }
    val today = remember { LocalDate.now() }
    var isSheetVisible by remember { mutableStateOf(false) }

    selection?.takeIf { isSheetVisible }?.let { day ->
        DayDetailsSheet(
            date = day.date,
            state = uiState,
            onDismiss = { isSheetVisible = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        val state = rememberCalendarState(
            startMonth = startMonth,
            endMonth = endMonth,
            firstVisibleMonth = currentMonth,
            firstDayOfWeek = daysOfWeek.first(),
            outDateStyle = OutDateStyle.EndOfGrid,
        )
        val coroutineScope = rememberCoroutineScope()
        val visibleMonth by remember { derivedStateOf { state.firstVisibleMonth } }
        LaunchedEffect(visibleMonth) { selection = null }

        CalendarHeader(
            calendarState = state,
            onPreviousClick = {
                coroutineScope.launch { state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.minusMonths(1)) }
            },
            onNextClick = {
                coroutineScope.launch { state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.plusMonths(1)) }
            }
        )

        MonthHeader(daysOfWeek = daysOfWeek)

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
                    itemBackgroundColor = if (isDarkTheme) CalendarSelectedDayDark else Color.Cyan,
                    selectedItemColor = if (isDarkTheme) Color.Cyan else Color.Black,
                    inactiveTextColor = CalendarInactiveText,
                    normalTextColor = MaterialTheme.colorScheme.onBackground,
                    highlighted = day.date in uiState.highlightedDates,
                )
            }
        )

        EventsAndTasksTimeline(uiState.timeline, modifier = Modifier.heightIn(max = 9999.dp))
    }
}

/** Hoja inferior con los eventos y tareas de un día. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailsSheet(date: LocalDate, state: CalendarUiState, onDismiss: () -> Unit) {
    val isDarkTheme = LocalDarkTheme.current
    val sectionColor = if (!isDarkTheme) MaterialTheme.colorScheme.primary else Color.Blue
    val events = remember(date, state.events) { state.eventsOn(date) }
    val tasks = remember(date, state.tasks) { state.tasksOn(date) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 400.dp)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(text = "Detalles del día", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(4.dp))

            Text(
                text = date.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale.getDefault()))
                    .replaceFirstChar { it.uppercaseChar() },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            if (events.isNotEmpty()) {
                Text(text = "Eventos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = sectionColor)
                Spacer(Modifier.height(8.dp))
                events.forEach { event ->
                    Text(
                        text = "${state.subject(event.subjectId)?.name ?: "Asignatura"}: ${event.name}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            if (tasks.isNotEmpty()) {
                Text(text = "Tareas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = sectionColor)
                Spacer(Modifier.height(8.dp))
                tasks.forEach { task ->
                    Text(
                        text = "${state.subject(task.subjectId)?.name?.plus(": ").orEmpty()}${task.title}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            if (events.isEmpty() && tasks.isEmpty()) {
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
