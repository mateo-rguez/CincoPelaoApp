package io.github.mateorguez.cincopelao.ui.subjects

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.mateorguez.cincopelao.ui.tasks.DUE_CHIP_ALPHA
import io.github.mateorguez.cincopelao.ui.tasks.DueUrgency
import io.github.mateorguez.cincopelao.ui.tasks.TaskCardFrame
import io.github.mateorguez.cincopelao.ui.tasks.dueDateLabel
import io.github.mateorguez.cincopelao.ui.tasks.dueUrgency
import io.github.mateorguez.cincopelao.ui.theme.DelightRegular
import io.github.mateorguez.cincopelao.ui.theme.appColors

/** Margen horizontal de la tarjeta; la barra de acciones lo comparte para quedar alineada. */
private val CardHorizontalPadding = 12.dp

/**
 * Tarjeta de una asignatura: pastilla con su abreviatura, nombre y curso, y debajo dos etiquetas
 * que se alternan: una con la nota (media ↔ último examen, o la nota final si ya está todo hecho)
 * y otra con lo siguiente que hacer (próximo evento ↔ tareas pendientes). Una pulsación abre la
 * asignatura; una pulsación larga despliega editar/eliminar.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubjectCard(
    summary: SubjectSummary,
    titulation: String?,
    /** Cambia cada pocos segundos (a la vez en todas las tarjetas) para alternar las etiquetas. */
    showAlternate: Boolean,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val subject = summary.subject
    var showDeleteDialog by remember { mutableStateOf(false) }
    val appColors = MaterialTheme.appColors

    Column(modifier = Modifier.animateContentSize()) {
        TaskCardFrame(
            outerPadding = PaddingValues(horizontal = CardHorizontalPadding, vertical = 6.dp),
            contentPadding = 14.dp,
            modifier = Modifier.combinedClickable(
                onClick = {
                    if (isExpanded) onCollapse()
                    else context.startActivity(SubjectActivity.intent(context, subject.subjectId))
                },
                onLongClick = { if (!isExpanded) onExpand() },
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SubjectBadge(summary.badge)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = DelightRegular,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val courseLine = listOf(subject.course, titulation.orEmpty())
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")
                    if (courseLine.isNotEmpty()) {
                        Text(
                            text = courseLine,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = DelightRegular,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RotatingChip(gradeChips(summary), showAlternate)
                RotatingChip(nextChips(summary), showAlternate, Modifier.weight(1f, fill = false))
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.zIndex(-1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CardHorizontalPadding)
                    .offset(y = (-12).dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    onCollapse()
                    context.startActivity(AddEditSubjectActivity.editIntent(context, subject.subjectId))
                }) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp), tint = appColors.accent)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar", color = appColors.accent)
                }
                TextButton(onClick = {
                    onCollapse()
                    showDeleteDialog = true
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Eliminar asignatura") },
                text = { Text("¿Estás seguro de que deseas eliminar '${subject.name}'?") },
                confirmButton = {
                    TextButton(onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancelar", color = appColors.accent)
                    }
                }
            )
        }
    }
}

/** Pastilla cuadrada con la abreviatura, con el degradado de asignaturas de la paleta. */
@Composable
private fun SubjectBadge(text: String) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(colors.subjectCardStart, colors.subjectCardEnd))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = if (text.length > 3) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

/** Contenido de una etiqueta. */
private data class ChipContent(val icon: ImageVector, val text: String, val tone: Color? = null)

/** Etiquetas de la nota: la final, fija, si ya está todo hecho; si no, media ↔ último examen. */
@Composable
private fun gradeChips(summary: SubjectSummary): List<ChipContent> {
    val colors = MaterialTheme.appColors
    fun Float.text() = "%.1f".format(this)
    fun Float.failTone(otherwise: Color?) = if (this < 5f) colors.dueOverdue else otherwise
    summary.finalGrade?.let { grade ->
        return listOf(ChipContent(Icons.Outlined.WorkspacePremium, "Nota final " + grade.text(), grade.failTone(colors.accent)))
    }
    val average = summary.average ?: return listOf(ChipContent(Icons.Outlined.Grade, "Sin notas"))
    val chips = mutableListOf(ChipContent(Icons.Outlined.Grade, "Media " + average.text(), average.failTone(null)))
    summary.lastGraded?.score?.let { score ->
        // La nota delante: si el nombre es largo, se recorta el nombre y no la nota
        chips += ChipContent(Icons.Outlined.History, "Último ${score.text()} · ${summary.lastGraded.name}", score.failTone(null))
    }
    return chips
}

/** Etiquetas de lo siguiente que hacer: próximo evento ↔ tareas pendientes, o «Todo al día». */
@Composable
private fun nextChips(summary: SubjectSummary): List<ChipContent> {
    val chips = mutableListOf<ChipContent>()
    summary.nextEvent?.let { event ->
        val soon = dueUrgency(event.date) == DueUrgency.SOON
        chips += ChipContent(
            icon = if (soon) Icons.Outlined.ErrorOutline else Icons.Outlined.Event,
            text = "${event.name} · " + dueDateLabel(event.date).replaceFirstChar { it.lowercase() },
            tone = if (soon) MaterialTheme.appColors.dueSoon else null
        )
    }
    if (summary.pendingTasks > 0) {
        val n = summary.pendingTasks
        chips += ChipContent(Icons.Outlined.TaskAlt, if (n == 1) "1 tarea pendiente" else "$n tareas pendientes")
    }
    return chips.ifEmpty { listOf(ChipContent(Icons.Outlined.Done, "Todo al día")) }
}

/** Muestra la primera etiqueta o, si hay dos y toca alternar, la segunda; cambia deslizando hacia arriba. */
@Composable
private fun RotatingChip(chips: List<ChipContent>, showAlternate: Boolean, modifier: Modifier = Modifier) {
    val shown = if (showAlternate && chips.size > 1) chips[1] else chips[0]
    AnimatedContent(
        targetState = shown,
        transitionSpec = {
            (slideInVertically(tween(450)) { it } + fadeIn(tween(450)))
                .togetherWith(slideOutVertically(tween(450)) { -it } + fadeOut(tween(300)))
                .using(SizeTransform(clip = true))
        },
        modifier = modifier,
        label = "SubjectChip"
    ) { chip ->
        InfoChip(chip.icon, chip.text, chip.tone)
    }
}

/** Etiqueta redondeada; con [tone] se tiñe de ese color, sin él es neutra. */
@Composable
private fun InfoChip(icon: ImageVector, text: String, tone: Color? = null) {
    val onBackground = MaterialTheme.colorScheme.onBackground
    val content = tone ?: onBackground.copy(alpha = 0.75f)
    val background = tone?.copy(alpha = DUE_CHIP_ALPHA) ?: onBackground.copy(alpha = 0.07f)
    Row(
        modifier = Modifier
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (tone != null) FontWeight.SemiBold else FontWeight.Normal,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
