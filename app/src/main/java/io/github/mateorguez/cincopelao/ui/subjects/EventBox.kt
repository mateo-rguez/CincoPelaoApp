package io.github.mateorguez.cincopelao.ui.subjects

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import io.github.mateorguez.cincopelao.ui.tasks.TaskCheckbox
import io.github.mateorguez.cincopelao.ui.tasks.daysUntil
import io.github.mateorguez.cincopelao.ui.tasks.dueDateLabel
import io.github.mateorguez.cincopelao.ui.theme.AppColors
import io.github.mateorguez.cincopelao.ui.theme.appColors
import io.github.mateorguez.cincopelao.ui.tasks.DUE_CHIP_ALPHA
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mateorguez.cincopelao.data.local.entity.Event
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fila de un evento, con un separador debajo.
 *
 * Un evento puntuable se reparte en tres franjas, y en cada una lo de la izquierda queda centrado
 * con lo de la derecha, tenga el evento poco o mucho contenido:
 * - nombre (con lápiz para editarlo) ↔ flechas de subir 1, 0,1 y 0,01;
 * - etiquetas con peso, nota mínima, tipo y fecha ↔ caja de la nota;
 * - estado («Notable», «En 6 días»…) ↔ flechas de bajar.
 *
 * Uno no puntuable tiene nombre y etiquetas a la izquierda y estado con casilla a la derecha.
 */
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
        EventSheet(
            subjectId = subjectId,
            event = event,
            onDismiss = { showEditEventDialog = false },
            onSave = onEventUpdate
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Abajo algo más: el título tiene aire propio por encima de las letras y la etiqueta de
        // estado no, así que con márgenes iguales el de abajo parecía menor
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 15.dp)) {
            if (event.isPuntuable) {
                PuntuableEvent(event, onScoreChange, onEdit = { showEditEventDialog = true })
            } else {
                NoPuntuableEvent(event, onDoneChange, onEdit = { showEditEventDialog = true })
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f), thickness = 1.dp)
    }
}

/** Separación entre la columna de la información y la de la nota. */
private val ColumnGap = 12.dp

/** Las tres franjas de un evento puntuable (ver [EventBox]). */
@Composable
private fun PuntuableEvent(event: Event, onScoreChange: (Float?) -> Unit, onEdit: () -> Unit) {
    val score = rememberScoreState(event.score, onScoreChange)

    // Cada franja es una fila con dos mitades centradas entre sí
    @Composable
    fun Band(left: @Composable () -> Unit, right: @Composable () -> Unit) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ColumnGap)
        ) {
            Box(modifier = Modifier.weight(1f)) { left() }
            Box(modifier = Modifier.weight(1f)) { right() }
        }
    }

    Band(left = { EventTitle(event.name, onEdit) }, right = { ScoreArrows(up = true, score::modify) })
    Band(left = { EventInfoChips(event) }, right = { ScoreField(score) })
    Band(
        left = {
            val (text, color) = puntuableStatus(event)
            EventStatusTag(text = text, tone = color)
        },
        right = { ScoreArrows(up = false, score::modify) }
    )
}

/** Estado de un evento puntuable; mientras no tenga nota y no haya pasado, cuánto falta. */
@Composable
private fun puntuableStatus(event: Event): Pair<String, Color> {
    val daysLeft = daysUntil(event.date)
    return if (event.score == null && daysLeft >= 0) {
        dueDateLabel(event.date) to (if (daysLeft <= 1) MaterialTheme.appColors.dueSoon else MaterialTheme.appColors.muted)
    } else {
        getEventStatus(event, MaterialTheme.appColors)
    }
}

@Composable
private fun NoPuntuableEvent(event: Event, onDoneChange: (Boolean) -> Unit, onEdit: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ColumnGap)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EventTitle(event.name, onEdit)
            EventInfoChips(event)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val (statusText, statusColor) = getEventStatus(event, MaterialTheme.appColors)
            var checked by remember { mutableStateOf(event.noPuntuableDone) }
            EventStatusTag(text = statusText, tone = statusColor)
            TaskCheckbox(
                checked = checked,
                onCheckedChange = {
                    checked = it
                    onDoneChange(it)
                }
            )
        }
    }
}

/** Nombre del evento con un lápiz pequeño; los dos abren la edición. */
@Composable
private fun EventTitle(name: String, onEdit: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 24.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f, fill = false)
                .clickable(onClick = onEdit)
        )
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable(onClick = onEdit),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "Editar",
                tint = MaterialTheme.appColors.accent,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

/** Peso, nota mínima, tipo y fecha corta del evento, como etiquetas. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventInfoChips(event: Event) {
    val formattedDate = remember(event.date) {
        SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(event.date))
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        event.weight?.let { InfoChip(null, "Peso " + displayNumber(it) + " %") }
        event.minScore?.takeIf { it > 0f }?.let { InfoChip(null, "Mín. " + displayNumber(it / 10f)) }
        if (event.type.isNotBlank()) InfoChip(null, event.type)
        InfoChip(Icons.Outlined.Event, formattedDate)
    }
}

/** Número sin decimales si es entero y con uno si no («10», «4,5»). */
private fun displayNumber(value: Float): String =
    if (value % 1f == 0f) "%.0f".format(value) else "%.1f".format(value)

/** Texto de la caja de la nota y cómo cambiarlo; avisa de cada cambio con [onScoreChange]. */
internal class ScoreState(initial: String, private val onScoreChange: (Float?) -> Unit) {
    var text by mutableStateOf(initial)

    /** Sube o baja la nota; bajar desde vacío no hace nada y bajar desde 0 la borra. */
    fun modify(delta: Float) {
        val current = text.replace(',', '.').toFloatOrNull()
        if (current == null && delta < 0f) return
        val updated: Float? = when {
            current == null -> delta.coerceIn(0f, 10f)
            current == 0f && delta < 0f -> null
            else -> (current + delta).coerceIn(0f, 10f)
        }
        set(updated?.let { "%.2f".format(it) } ?: "", updated)
    }

    /** Lo escrito a mano: se acepta vacío o un número de 0 a 10. */
    fun type(input: String) {
        val value = input.replace(',', '.').toFloatOrNull()
        when {
            input.isBlank() -> set("", null)
            value != null && value in 0f..10f -> set(input, value)
        }
    }

    fun clear() = set("", null)

    val isZero: Boolean get() = text.replace(',', '.').toFloatOrNull() == 0f

    private fun set(newText: String, value: Float?) {
        text = newText
        onScoreChange(value)
    }
}

@Composable
private fun rememberScoreState(score: Float?, onScoreChange: (Float?) -> Unit): ScoreState {
    // La función cambia si se edita el evento (lleva dentro una copia de él): usar siempre la última
    val latestOnChange by rememberUpdatedState(onScoreChange)
    return remember { ScoreState(score?.let { "%.2f".format(it) } ?: "") { latestOnChange(it) } }
}

private val ScoreSteps = listOf(1.0f, 0.1f, 0.01f)

/** Tres flechas que suben (o bajan) la nota 1, 0,1 y 0,01. */
@Composable
private fun ScoreArrows(up: Boolean, onStep: (Float) -> Unit) {
    val tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        ScoreSteps.forEach { step ->
            IconButton(onClick = { onStep(if (up) step else -step) }, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = if (up) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = (if (up) "Subir " else "Bajar ") + step,
                    tint = tint
                )
            }
        }
    }
}

/** Caja de la nota; se puede escribir y, con 0, sale una equis para borrarla. */
@Composable
private fun ScoreField(score: ScoreState) {
    val accent = MaterialTheme.appColors.accent
    OutlinedTextField(
        value = score.text,
        onValueChange = score::type,
        placeholder = {
            Text(
                "Sin nota",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            )
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.titleLarge.copy(
            textAlign = TextAlign.Center
        ),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent,
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            cursorColor = accent,
            focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
            unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        trailingIcon = if (score.isZero) {
            {
                IconButton(onClick = score::clear) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar nota")
                }
            }
        } else null,
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Etiqueta del estado de un evento: el texto en el tono de su estado sobre ese mismo tono suave,
 * como las etiquetas de fecha de las tareas (cada paleta da tonos que se leen así).
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EventStatusTag(text: String, tone: Color) {
    // Al cambiar de estado, el tono pasa de uno a otro
    val animatedTone by animateColorAsState(
        targetValue = tone,
        animationSpec = tween(durationMillis = 500)
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
                    color = animatedTone.copy(alpha = DUE_CHIP_ALPHA),
                    shape = RoundedCornerShape(50)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = animatedText,
                color = animatedTone,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/** Texto y tono del estado de un evento ya calificado (o no puntuable), con los colores de la paleta. */
fun getEventStatus(event: Event, colors: AppColors): Pair<String, Color> {
    return if (!event.isPuntuable) {
        if (event.noPuntuableDone) {
            "Hecho" to colors.done
        } else {
            "Por hacer" to colors.muted
        }
    } else {
        when {
            event.score == null -> "Sin nota" to colors.muted
            event.minScore != null && event.score < event.minScore / 10 -> "Nota mínima" to colors.gradeBelowMinimum
            event.score < 5 -> "Suspenso" to colors.gradeFail
            event.score < 6 -> "Suficiente" to colors.gradePass
            event.score < 7 -> "Aceptable" to colors.gradeFair
            event.score < 9 -> "Notable" to colors.gradeGood
            event.score >= 9 -> "Sobresaliente" to colors.gradeExcellent
            else -> "Bien" to colors.gradeFair
        }
    }
}
