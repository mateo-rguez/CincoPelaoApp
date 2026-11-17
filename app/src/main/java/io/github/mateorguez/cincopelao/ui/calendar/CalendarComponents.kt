package io.github.mateorguez.cincopelao.ui.calendar

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mateorguez.cincopelao.ui.theme.appColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// Piezas del calendario mensual: cabecera con el mes, días de la semana y la cuadrícula de días.

/** Hueco entre las celdas de la cuadrícula, en las dos direcciones. */
private val CellGap = 4.dp
private val CellShape = RoundedCornerShape(8.dp)

/** Mes visible entre dos flechas; al cambiar, el nombre entra por el lado hacia el que se va. */
@Composable
fun CalendarHeader(month: YearMonth, onPreviousClick: () -> Unit, onNextClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Mes anterior",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(Modifier.weight(1f))
        AnimatedContent(
            targetState = month,
            transitionSpec = {
                val spec = tween<IntOffset>(300)
                val forward = targetState > initialState
                (slideInHorizontally(spec) { if (forward) it else -it } + fadeIn() togetherWith
                    slideOutHorizontally(spec) { if (forward) -it else it } + fadeOut())
                    .using(SizeTransform(clip = false))
            },
            label = "mes"
        ) { shown ->
            Text(
                text = shown.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
                    .replaceFirstChar { it.titlecase(Locale.getDefault()) },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onNextClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Mes siguiente",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

/** Iniciales de los días de la semana ("LUN", "MAR"…), alineadas con las columnas. */
@Composable
fun WeekDaysHeader(days: List<DayOfWeek>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(CellGap)
    ) {
        days.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).replace(".", "").uppercase(),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                maxLines = 1
            )
        }
    }
}

/** Cuadrícula de un mes: 6 semanas, con los días de los meses vecinos atenuados. */
@Composable
fun MonthGrid(
    month: YearMonth,
    firstDay: DayOfWeek,
    today: LocalDate,
    selected: LocalDate?,
    itemsByDate: Map<LocalDate, List<TimelineItem>>,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val days = remember(month, firstDay) { monthGridDays(month, firstDay) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CellGap)) {
        days.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                week.forEach { date ->
                    val inMonth = YearMonth.from(date) == month
                    DayCell(
                        date = date,
                        inMonth = inMonth,
                        isToday = date == today,
                        isSelected = inMonth && date == selected,
                        dots = if (inMonth) itemsByDate[date]?.let { gridDots(it) } else null,
                        onClick = { onDayClick(date) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Un día: casilla redondeada con el número arriba y los puntos abajo. Hoy lleva un borde del
 * acento; el día elegido, un relleno suave y un borde fino. Los días de otros meses, atenuados y
 * sin pulsar.
 */
@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    dots: GridDots?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appColors = MaterialTheme.appColors
    val onBackground = MaterialTheme.colorScheme.onBackground
    val base = appColors.calendarDayBackground
    val background = when {
        isSelected -> appColors.calendarSelectedBorder.copy(alpha = 0.22f).compositeOver(base)
        inMonth -> base
        else -> base.copy(alpha = 0.5f)
    }
    val border = when {
        isToday && inMonth -> Modifier.border(2.dp, appColors.calendarToday, CellShape)
        isSelected -> Modifier.border(1.dp, appColors.calendarSelectedBorder, CellShape)
        else -> Modifier
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CellShape)
            .background(background)
            .then(border)
            .clickable(enabled = inMonth, onClick = onClick)
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 5.dp),
            color = if (inMonth) onBackground else onBackground.copy(alpha = 0.45f),
            fontSize = 14.sp,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
        )
        if (dots != null) {
            DayDots(dots, Modifier.align(Alignment.BottomCenter).padding(bottom = 7.dp))
        }
    }
}

/** Un punto del color de su tipo por cada evento o tarea del día y "+" si hay más. */
@Composable
private fun DayDots(dots: GridDots, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dots.markers.forEach { marker ->
            Box(Modifier.size(5.dp).background(marker.color(), CircleShape))
        }
        if (dots.more) {
            Text(
                text = "+",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 10.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
