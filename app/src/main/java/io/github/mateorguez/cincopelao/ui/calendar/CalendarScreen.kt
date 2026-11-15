package io.github.mateorguez.cincopelao.ui.calendar

import io.github.mateorguez.cincopelao.ui.theme.appColors
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.OutDateStyle
import com.kizitonwose.calendar.core.daysOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.launch

/** Pestaña de calendario: mes con los días marcados y, debajo, la línea de tiempo de lo próximo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appColors = MaterialTheme.appColors

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
            today = today,
            items = uiState.itemsOn(day.date),
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
                    todayColor = appColors.calendarToday,
                    itemBackgroundColor = appColors.calendarDayBackground,
                    selectedItemColor = appColors.calendarSelectedBorder,
                    // Los días de otros meses, atenuados pero legibles sobre cualquier fondo
                    inactiveTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                    normalTextColor = MaterialTheme.colorScheme.onBackground,
                    dots = uiState.itemsByDate[day.date]?.let { gridDots(it) },
                )
            }
        )

        EventsAndTasksTimeline(uiState.timeline, today = today, modifier = Modifier.padding(top = 24.dp))
    }
}

/** Hoja inferior con los eventos y tareas de un día, en filas separadas por líneas finas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailsSheet(date: LocalDate, today: LocalDate, items: List<TimelineItem>, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 320.dp)
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 32.dp)
        ) {
            Text(
                text = buildAnnotatedString {
                    append(dayHeaderLabel(date))
                    withStyle(SpanStyle(color = MaterialTheme.appColors.accent)) {
                        append(" · ${relativeDayLabel(date, today)}")
                    }
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(16.dp))

            if (items.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🌤️", fontSize = 40.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Día libre",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "No hay eventos ni tareas para este día.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items.forEachIndexed { index, item ->
                    key(item.key) {
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        DayItemRow(item)
                    }
                }
            }
        }
    }
}

/** Una fila de la hoja: punto del color del tipo, nombre y "Examen · Teoría · PSI". */
@Composable
private fun DayItemRow(item: TimelineItem) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        // Centrado con la primera línea (el nombre)
        Box(Modifier.height(24.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(10.dp).background(item.markerColor(), CircleShape))
        }
        Spacer(Modifier.width(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = when (item) {
                    is TimelineItem.EventItem -> item.event.name
                    is TimelineItem.TaskItem -> item.task.title
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = item.shortLine(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
