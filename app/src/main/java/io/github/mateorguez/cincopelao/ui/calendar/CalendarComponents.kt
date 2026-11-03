package io.github.mateorguez.cincopelao.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kizitonwose.calendar.compose.CalendarLayoutInfo
import com.kizitonwose.calendar.compose.CalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.previousMonth
import io.github.mateorguez.cincopelao.ui.theme.DelightMedium
import io.github.mateorguez.cincopelao.ui.theme.DelightRegular
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.flow.filterNotNull

// Piezas del calendario mensual: cabecera con el mes, días de la semana y cada día.

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
                contentDescription = "Mes anterior",
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
                    fontFamily = DelightMedium
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
                contentDescription = "Mes siguiente",
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
                    fontFamily = DelightRegular,
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

// Utilidades del calendario de Kizitonwose ------------------------------

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
