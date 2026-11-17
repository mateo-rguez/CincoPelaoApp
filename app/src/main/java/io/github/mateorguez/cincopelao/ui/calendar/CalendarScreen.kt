package io.github.mateorguez.cincopelao.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mateorguez.cincopelao.ui.theme.appColors
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.launch

/** Pestaña de calendario: mes con los días marcados y, debajo, la línea de tiempo de lo próximo. */
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val today = remember { LocalDate.now() }
    val currentMonth = remember { YearMonth.from(today) }
    val firstDay = remember { firstDayOfWeek() }
    val weekDays = remember(firstDay) { weekDays(firstDay) }
    var selection by remember { mutableStateOf<LocalDate?>(null) }
    var isSheetVisible by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(initialPage = CurrentMonthPage) { MonthPageCount }
    val coroutineScope = rememberCoroutineScope()
    // Cambia a mitad del deslizamiento, así el nombre del mes acompaña al gesto
    val visibleMonth = monthForPage(pagerState.currentPage, currentMonth)
    LaunchedEffect(pagerState.settledPage) { selection = null }

    selection?.takeIf { isSheetVisible }?.let { date ->
        DayDetailsSheet(
            date = date,
            today = today,
            items = uiState.itemsOn(date),
            onDismiss = { isSheetVisible = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        CalendarHeader(
            month = visibleMonth,
            onPreviousClick = {
                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
            },
            onNextClick = {
                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            }
        )

        WeekDaysHeader(weekDays)

        // Todos los meses tienen 6 semanas: el calendario no cambia de alto
        HorizontalPager(
            state = pagerState,
            pageSpacing = 16.dp,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            MonthGrid(
                month = monthForPage(page, currentMonth),
                firstDay = firstDay,
                today = today,
                selected = selection,
                itemsByDate = uiState.itemsByDate,
                onDayClick = { date ->
                    selection = date
                    isSheetVisible = true
                }
            )
        }

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
                style = MaterialTheme.typography.bodyLarge,
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
