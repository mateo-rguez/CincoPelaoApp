package io.github.mateorguez.cincopelao.ui.calendar

import io.github.mateorguez.cincopelao.ui.theme.appColors
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mateorguez.cincopelao.ui.subjects.InfoChip
import io.github.mateorguez.cincopelao.ui.tasks.TaskCardFrame
import java.time.LocalDate

// Línea de tiempo de próximos eventos y tareas, como un árbol: un tronco común pasa por el punto
// de cada día (con su fecha y cuánto falta) y de él sale una rama curva, del mismo color, que
// llega hasta el borde de la tarjeta de cada elemento.
//
// Letra: la fecha del día a 18 (es lo que manda), el nombre de cada elemento a 16 y lo
// secundario de la tarjeta (etiqueta y asignatura) a 12.

/** Posición del tronco desde el borde izquierdo. */
private val TrunkX = 10.dp
/** Radio de la curva con la que cada rama sale del tronco. */
private val BranchRadius = 12.dp
/** Ancho de la vía: la rama sigue recta desde la curva hasta aquí, donde empieza la tarjeta. */
private val RailWidth = 40.dp
private val HeaderDotSize = 14.dp
private val HeaderHeight = 40.dp
/** Hueco entre la fecha de un día y su primera tarjeta. */
private val HeaderGap = 8.dp
/** Altura de la rama desde el borde superior de la tarjeta: la de la etiqueta del tipo. */
private val BranchY = 25.dp
private val CardGap = 12.dp

@Composable
fun EventsAndTasksTimeline(
    days: List<TimelineDay>,
    today: LocalDate,
    modifier: Modifier = Modifier
) {
    if (days.isEmpty()) {
        Text(
            text = "Nada pendiente de hoy en adelante.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = modifier.fillMaxWidth().padding(vertical = 24.dp)
        )
        return
    }
    // Opaco: donde la rama se solapa con el tronco no se nota la diferencia
    val lineColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
        .compositeOver(MaterialTheme.colorScheme.background)
    Column(modifier) {
        days.forEachIndexed { dayIndex, day ->
            key(day.date) {
                DayHeader(date = day.date, today = today, lineColor = lineColor, isFirst = dayIndex == 0)
                day.items.forEachIndexed { index, item ->
                    key(item.key) {
                        TimelineRow(
                            item = item,
                            lineColor = lineColor,
                            isLast = dayIndex == days.lastIndex && index == day.items.lastIndex
                        )
                    }
                }
            }
        }
    }
}

/** Tramo vertical de la línea en el centro de la vía, de [startY] a [endY] (null = hasta el borde). */
private fun Modifier.railLine(color: Color, startY: Dp?, endY: Dp?): Modifier = drawBehind {
    val x = size.width / 2
    drawLine(
        color = color,
        start = Offset(x, startY?.toPx() ?: 0f),
        end = Offset(x, endY?.toPx() ?: size.height),
        strokeWidth = 2.5.dp.toPx()
    )
}

/** Cabecera de un día: su punto en el tronco, "Martes 17 nov" y cuánto falta. */
@Composable
private fun DayHeader(date: LocalDate, today: LocalDate, lineColor: Color, isFirst: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().height(HeaderHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(TrunkX * 2)
                .fillMaxHeight()
                // El tronco es común: entra desde arriba salvo en el primer día
                .railLine(lineColor, startY = if (isFirst) HeaderHeight / 2 else null, endY = null),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(HeaderDotSize).background(MaterialTheme.colorScheme.onBackground, CircleShape))
        }
        // El texto empieza donde las tarjetas: el día queda por encima de sus elementos
        Text(
            text = dayHeaderLabel(date),
            style = HeaderStyle(),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = RailWidth - TrunkX * 2)
        )
        Text(
            text = " · ${relativeDayLabel(date, today)}",
            style = HeaderStyle(),
            color = MaterialTheme.appColors.accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    // Hueco hasta la primera tarjeta, con el tronco pasando por él
    Box(Modifier.width(TrunkX * 2).height(HeaderGap).railLine(lineColor, startY = null, endY = null))
}

/** Letra de la fecha de un día: algo mayor que el nombre de los elementos, para que mande. */
@Composable
private fun HeaderStyle() = MaterialTheme.typography.titleMedium.copy(
    fontSize = 18.sp,
    lineHeight = 24.sp,
    fontWeight = FontWeight.Bold
)

/** Un elemento: el tramo de tronco, la rama curva hasta el borde de su tarjeta y la tarjeta. */
@Composable
private fun TimelineRow(item: TimelineItem, lineColor: Color, isLast: Boolean) {
    val color = item.markerColor()
    Row(Modifier.height(IntrinsicSize.Min)) {
        Box(
            modifier = Modifier
                .width(RailWidth)
                .fillMaxHeight()
                .drawBehind {
                    val stroke = 2.5.dp.toPx()
                    val x = TrunkX.toPx()
                    val y = BranchY.toPx()
                    val r = BranchRadius.toPx()
                    // El tronco sigue hacia abajo salvo en el último elemento, donde acaba en su rama
                    drawLine(lineColor, Offset(x, 0f), Offset(x, if (isLast) y - r else size.height), stroke)
                    val branch = Path().apply {
                        moveTo(x, y - r)
                        // Cuarto de curva (aprox. de círculo) y recta hasta el borde de la tarjeta
                        cubicTo(x, y - r * 0.45f, x + r * 0.45f, y, x + r, y)
                        lineTo(size.width, y)
                    }
                    drawPath(branch, lineColor, style = Stroke(width = stroke, cap = StrokeCap.Butt))
                }
        )
        TimelineCard(item, color, Modifier.weight(1f).padding(bottom = CardGap))
    }
}

/** Color de un elemento según su tipo: exámenes, otros eventos o tareas, de la paleta. */
@Composable
fun TimelineItem.markerColor(): Color = marker().color()

/** Color de cada tipo: el mismo en la línea de tiempo, la cuadrícula y la hoja del día. */
@Composable
fun TimelineMarker.color(): Color = when (this) {
    TimelineMarker.EXAM -> MaterialTheme.appColors.timelineExamPoint
    TimelineMarker.DELIVERABLE, TimelineMarker.OTHER_EVENT -> MaterialTheme.appColors.timelineEventPoint
    TimelineMarker.TASK -> MaterialTheme.appColors.timelineTaskPoint
}

/** Tarjeta de un evento o una tarea: etiqueta del tipo, nombre y asignatura. */
@Composable
fun TimelineCard(item: TimelineItem, color: Color, modifier: Modifier = Modifier) {
    Box(modifier) {
        TaskCardFrame(outerPadding = PaddingValues(0.dp), contentPadding = 14.dp) {
            InfoChip(icon = null, text = item.typeLabel(), tone = color)
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
                item.subjectLine()?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
