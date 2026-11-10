package io.github.mateorguez.cincopelao.ui.calendar

import io.github.mateorguez.cincopelao.ui.theme.appColors
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pushpal.jetlime.EventPosition
import com.pushpal.jetlime.ItemsList
import com.pushpal.jetlime.JetLimeColumn
import com.pushpal.jetlime.JetLimeDefaults
import com.pushpal.jetlime.JetLimeEvent
import com.pushpal.jetlime.JetLimeEventDefaults
import com.pushpal.jetlime.JetLimeEventStyle
import java.time.format.DateTimeFormatter
import java.util.Locale

// Línea de tiempo de próximos eventos y tareas.

@Composable
fun EventsAndTasksTimeline(
    entries: List<TimelineEntry>,
    modifier: Modifier = Modifier
) {
    JetLimeColumn(
        modifier = modifier.padding(16.dp),
        itemsList = ItemsList(entries),
        key = { _, item -> item.key },
        style = JetLimeDefaults.columnStyle(
            contentDistance = 16.dp,
            itemSpacing = 12.dp,
            lineThickness = 2.dp,
            lineBrush = JetLimeDefaults.lineSolidBrush(color = Color.LightGray),
        )
    ) { index, item, position ->

        val appColors = MaterialTheme.appColors
        val eventContainerColor = appColors.timelineEventContainer
        val taskContainerColor = appColors.timelineTaskContainer

        when (item) {
            is TimelineEntry.HeaderItem -> {
                Text(
                    text = item.date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = appColors.accent,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            is TimelineEntry.EventItem -> {
                val event = item.event
                val subject = item.subject
                JetLimeEvent(
                    style = eventPointStyle(position)
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
                                text = event.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )

                            Spacer(Modifier.height(2.dp))

                            Text(
                                text = listOfNotNull(event.type, subject?.name).joinToString(" - "),
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
                    style = taskPointStyle(position)
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
                                text = (task.dueDate ?: 0L).toLocalDate()
                                    .format(DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = task.title,
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

/** Punto de la línea de tiempo para las tareas (color de la paleta). */
@Composable
fun taskPointStyle(position: EventPosition): JetLimeEventStyle {
    // Estilo por defecto cambiando solo el color del punto
    val default = JetLimeEventDefaults.eventStyle(position)
    return JetLimeEventDefaults.eventStyle(
        position = position,
        pointType = default.pointType,
        pointColor = MaterialTheme.appColors.timelineTaskPoint,
        pointFillColor = default.pointFillColor,
        pointRadius = default.pointRadius,
        pointAnimation = default.pointAnimation,
        pointStrokeWidth = default.pointStrokeWidth,
        pointStrokeColor = default.pointStrokeColor,
    )
}

/** Punto de la línea de tiempo para los eventos (color de la paleta). */
@Composable
fun eventPointStyle(position: EventPosition): JetLimeEventStyle {
    val default = JetLimeEventDefaults.eventStyle(position)
    return JetLimeEventDefaults.eventStyle(
        position = position,
        pointType = default.pointType,
        pointColor = MaterialTheme.appColors.timelineEventPoint,
        pointFillColor = default.pointFillColor,
        pointRadius = default.pointRadius,
        pointAnimation = default.pointAnimation,
        pointStrokeWidth = default.pointStrokeWidth,
        pointStrokeColor = default.pointStrokeColor,
    )
}
