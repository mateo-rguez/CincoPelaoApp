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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import io.github.mateorguez.cincopelao.ui.subjects.InfoChip
import io.github.mateorguez.cincopelao.ui.tasks.TaskCardFrame
import io.github.mateorguez.cincopelao.ui.theme.DelightMedium
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// Línea de tiempo de próximos eventos y tareas: la fecha a la izquierda, una línea vertical con
// un marcador por elemento (su forma dice qué es) y la tarjeta a la derecha.

private val DateColumnWidth = 68.dp
private val RailWidth = 28.dp
private val MarkerSize = 20.dp
/** Distancia del borde superior de la tarjeta al marcador, para que quede a la altura de la etiqueta. */
private val MarkerTop = 15.dp
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
    Column(modifier) {
        days.forEachIndexed { dayIndex, day ->
            key(day.date) {
                // La altura intrínseca deja que la columna de tarjetas ocupe todo el día aunque la
                // fecha sea más alta, así la línea no se corta.
                Row(Modifier.height(IntrinsicSize.Min)) {
                    DayLabel(date = day.date, today = today, modifier = Modifier.width(DateColumnWidth).padding(top = 6.dp))
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        day.items.forEachIndexed { index, item ->
                            val isLastOfDay = index == day.items.lastIndex
                            key(item.key) {
                                TimelineRow(
                                    item = item,
                                    isFirst = dayIndex == 0 && index == 0,
                                    isLast = dayIndex == days.lastIndex && isLastOfDay,
                                    modifier = if (isLastOfDay) Modifier.weight(1f) else Modifier
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Número del día, día de la semana con el mes y cuánto falta. */
@Composable
private fun DayLabel(date: LocalDate, today: LocalDate, modifier: Modifier = Modifier) {
    val locale = Locale.getDefault()
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = DelightMedium),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "${date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)} · ${date.format(DateTimeFormatter.ofPattern("MMM", locale))}"
                .replace(".", "").uppercase(locale),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            text = relativeDayLabel(date, today),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.appColors.accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Un elemento: el tramo de línea con su marcador y la tarjeta. */
@Composable
private fun TimelineRow(item: TimelineItem, isFirst: Boolean, isLast: Boolean, modifier: Modifier = Modifier) {
    val color = item.markerColor()
    val lineColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
    Row(modifier.height(IntrinsicSize.Min)) {
        Box(
            modifier = Modifier
                .width(RailWidth)
                .fillMaxHeight()
                .drawBehind {
                    // La línea empieza en el primer marcador y acaba en el último
                    val markerCenter = (MarkerTop + MarkerSize / 2).toPx()
                    val x = size.width / 2
                    drawLine(
                        color = lineColor,
                        start = Offset(x, if (isFirst) markerCenter else 0f),
                        end = Offset(x, if (isLast) markerCenter else size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                },
            contentAlignment = Alignment.TopCenter
        ) {
            TimelineMarkerIcon(item.marker(), color, Modifier.padding(top = MarkerTop))
        }
        TimelineCard(item, color, Modifier.weight(1f).padding(bottom = CardGap))
    }
}

/** Color de un elemento: el de los eventos o el de las tareas de la paleta. */
@Composable
fun TimelineItem.markerColor(): Color = when (this) {
    is TimelineItem.EventItem -> MaterialTheme.appColors.timelineEventPoint
    is TimelineItem.TaskItem -> MaterialTheme.appColors.timelineTaskPoint
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
                    style = MaterialTheme.typography.bodyLarge,
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

/** Marcador con la forma de su tipo; las tareas llevan además un ✓. */
@Composable
fun TimelineMarkerIcon(marker: TimelineMarker, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(MarkerSize)
            .background(color, marker.shape),
        contentAlignment = Alignment.Center
    ) {
        if (marker == TimelineMarker.TASK) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.background,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// Formas de Material 3 (las de MaterialShapes), hechas con graphics-shapes ------------------

private val ExamShape: Shape = PolygonShape(
    RoundedPolygon.star(numVerticesPerRadius = 10, innerRadius = 0.72f, rounding = CornerRounding(0.12f))
)
private val DeliverableShape: Shape = PolygonShape(
    RoundedPolygon.rectangle(width = 2f, height = 2f, rounding = CornerRounding(0.55f))
)
private val OtherEventShape: Shape = PolygonShape(
    RoundedPolygon.star(numVerticesPerRadius = 6, innerRadius = 0.78f, rounding = CornerRounding(0.4f))
)

private val TimelineMarker.shape: Shape
    get() = when (this) {
        TimelineMarker.EXAM -> ExamShape
        TimelineMarker.DELIVERABLE -> DeliverableShape
        TimelineMarker.OTHER_EVENT -> OtherEventShape
        TimelineMarker.TASK -> CircleShape
    }

/** Un [RoundedPolygon] como `Shape` de Compose, escalado al tamaño de lo que recorta. */
private class PolygonShape(polygon: RoundedPolygon) : Shape {
    private val unitPath = polygon.normalized().toPath()

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = android.graphics.Path(unitPath).apply {
            transform(android.graphics.Matrix().apply { setScale(size.width, size.height) })
        }
        return Outline.Generic(path.asComposePath())
    }
}
