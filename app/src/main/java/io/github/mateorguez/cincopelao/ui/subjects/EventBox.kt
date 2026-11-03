package io.github.mateorguez.cincopelao.ui.subjects

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.ui.theme.DelightMedium
import io.github.mateorguez.cincopelao.ui.theme.StatusBelowMinimum
import io.github.mateorguez.cincopelao.ui.theme.StatusDone
import io.github.mateorguez.cincopelao.ui.theme.StatusExcellent
import io.github.mateorguez.cincopelao.ui.theme.StatusFail
import io.github.mateorguez.cincopelao.ui.theme.StatusFair
import io.github.mateorguez.cincopelao.ui.theme.StatusGood
import io.github.mateorguez.cincopelao.ui.theme.StatusPass
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Tarjeta de un evento con su nota (o casilla de hecho) y su etiqueta de estado.

@Composable
fun EventBox(
    event: Event,
    subjectId: Long,
    onScoreChange: (Float?) -> Unit,
    onDoneChange: (Boolean) -> Unit,
    onEventUpdate: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditEventDialog by remember { mutableStateOf(false) }

    if (showEditEventDialog) {
        EventDialog(
            subjectId = subjectId,
            onDismiss = { showEditEventDialog = false },
            onEventCreated = { },
            onEventUpdated = { newEvent ->
                onEventUpdate(newEvent)
            },
            event = event
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp, 5.dp, 20.dp, 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(0.45f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = DelightMedium
                        ),
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.clickable{showEditEventDialog = true}
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { showEditEventDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                event.weight?.let {
                    Text("Ponderación: ${"%.0f".format(it)}%", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Nota mínima: ${
                            if (event.minScore == null || event.minScore == 0f) "N/A"
                            else "${"%.0f%%".format(event.minScore)}"
                        }",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                val formattedDate = remember(event.date) {
                    val formatter = SimpleDateFormat("d MMM", Locale.getDefault())
                    val raw = formatter.format(Date(event.date))
                    raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                }

                Text("${event.type} ${event.modal ?: ""}", style = MaterialTheme.typography.bodySmall)
                Text("Fecha: $formattedDate", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(8.dp))

                val (statusText, statusColor) = getEventStatus(event)

                if(event.isPuntuable) {
                    EventStatusTag(
                        text = statusText,
                        backgroundColor = statusColor
                    )
                }
            }

            if (event.isPuntuable) {
                Column(
                    modifier = Modifier.weight(0.55f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val formattedScore = event.score?.let { String.format("%.2f", it) } ?: ""
                    var scoreText by remember { mutableStateOf(formattedScore) }

                    fun modifyScore(delta: Float) {
                        val current = scoreText.replace(',', '.').toFloatOrNull()
                        val updated: Float? = when {
                            current == null -> (delta).coerceIn(0f, 10f)
                            current == 0f && delta < 0f -> null
                            else -> (current + delta).coerceIn(0f, 10f)
                        }
                        if (updated == null) {
                            scoreText = ""
                            onScoreChange(null)
                        } else {
                            scoreText = "%.2f".format(updated)
                            onScoreChange(updated)
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { modifyScore(1.0f) }) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir 1")
                        }
                        IconButton(onClick = { modifyScore(0.1f) }) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir 0.1")
                        }
                        IconButton(onClick = { modifyScore(0.01f) }) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir 0.01")
                        }
                    }

                    OutlinedTextField(
                        value = scoreText,
                        onValueChange = { input ->
                            val sanitized = input.replace(',', '.')
                            val floatValue = sanitized.toFloatOrNull()
                            if (input.isBlank()) {
                                scoreText = ""
                                onScoreChange(null)
                            } else if (floatValue != null && floatValue in 0f..10f) {
                                scoreText = input
                                onScoreChange(floatValue)
                            }
                        },
                        placeholder = { Text("Nota") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = {
                            val floatValue = scoreText.replace(',', '.').toFloatOrNull()
                            if (floatValue == 0f) {
                                IconButton(
                                    onClick = {
                                        scoreText = ""
                                        onScoreChange(null)
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Clear,
                                        contentDescription = "Limpiar Nota")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { modifyScore(-1.0f) }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Bajar 1")
                        }
                        IconButton(onClick = { modifyScore(-0.1f) }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Bajar 0.1")
                        }
                        IconButton(onClick = { modifyScore(-0.01f) }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Bajar 0.01")
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val (statusText, statusColor) = getEventStatus(event)
                        var checked by remember { mutableStateOf(event.noPuntuableDone) }
                        EventStatusTag(
                            text = statusText,
                            backgroundColor = statusColor
                        )

                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                checked = it
                                onDoneChange(it)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EventStatusTag(
    text: String,
    backgroundColor: Color,
    contentColor: Color = Color.White
) {
    // Animar el color de fondo y del texto
    val animatedBackgroundColor by animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = tween(durationMillis = 500)
    )

    val animatedContentColor by animateColorAsState(
        targetValue = contentColor,
        animationSpec = tween(durationMillis = 200)
    )

    // Animar el cambio de texto con AnimatedContent
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            fadeIn(tween(200)) togetherWith fadeOut(tween(200))
        },
        label = "StatusTextTransition"
    ) { animatedText ->
        Box(
            modifier = Modifier
                .background(
                    color = animatedBackgroundColor,
                    shape = RoundedCornerShape(50)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = animatedText,
                color = animatedContentColor,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

fun getEventStatus(event: Event): Pair<String, Color> {
    return if (!event.isPuntuable) {
        if (event.noPuntuableDone) {
            "Hecho" to StatusDone
        } else {
            "Por hacer" to Color.Gray
        }
    } else {
        when {
            event.score == null -> "Sin nota" to Color.Gray
            event.minScore != null && event.score < event.minScore / 10 -> "Nota mínima" to StatusBelowMinimum
            event.score < 5 -> "Suspenso" to StatusFail
            event.score < 6 -> "Suficiente" to StatusPass
            event.score < 7 -> "Aceptable" to StatusFair
            event.score < 9 -> "Notable" to StatusGood
            event.score >= 9 -> "Sobresaliente" to StatusExcellent
            else -> "Bien" to StatusFair
        }
    }
}
