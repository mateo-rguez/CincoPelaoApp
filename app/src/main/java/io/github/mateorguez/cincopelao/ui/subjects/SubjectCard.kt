package io.github.mateorguez.cincopelao.ui.subjects

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.unit.sp
import io.github.mateorguez.cincopelao.ui.tasks.DUE_CHIP_ALPHA
import io.github.mateorguez.cincopelao.ui.tasks.DueUrgency
import io.github.mateorguez.cincopelao.ui.tasks.TaskCardFrame
import io.github.mateorguez.cincopelao.ui.tasks.dueDateLabel
import io.github.mateorguez.cincopelao.ui.tasks.dueUrgency
import io.github.mateorguez.cincopelao.ui.theme.DelightRegular
import io.github.mateorguez.cincopelao.ui.theme.appColors

/** Margen horizontal de la tarjeta; la barra de acciones lo comparte para quedar alineada. */
/**
 * Tarjeta de una asignatura: pastilla con su abreviatura, nombre y curso, y debajo dos etiquetas
 * que se alternan: una con la nota (media ↔ último examen, o la nota final si ya está todo hecho)
 * y otra con lo siguiente que hacer (próximo evento ↔ tareas pendientes). Una pulsación abre la
 * asignatura; el botón «⋮» abre el menú con editar y eliminar.
 */
@Composable
fun SubjectCard(
    summary: SubjectSummary,
    titulation: String?,
    /** Cambia cada pocos segundos (a la vez en todas las tarjetas) para alternar las etiquetas. */
    showAlternate: Boolean,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val subject = summary.subject
    var showDeleteDialog by remember { mutableStateOf(false) }

    TaskCardFrame(
        outerPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        contentPadding = 14.dp,
        modifier = Modifier.clickable { context.startActivity(SubjectActivity.intent(context, subject.subjectId)) }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubjectBadge(summary.badge)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject.name,
                    // Interlineado más apretado que el de titleMedium (24 sp): si no, la separación
                    // entre las líneas del nombre era mayor que la que hay hasta el curso
                    style = MaterialTheme.typography.titleMedium.copy(lineHeight = 20.sp),
                    fontFamily = DelightRegular,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val courseLine = listOf(subject.course, titulation.orEmpty())
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                if (courseLine.isNotEmpty()) {
                    // Medido en pantalla: con esto el hueco hasta el curso iguala al de las líneas del nombre
                    Spacer(modifier = Modifier.height(1.5.dp))
                    Text(
                        text = courseLine,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = DelightRegular,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            SubjectMenu(
                modifier = Modifier.align(Alignment.Top),
                onEdit = { context.startActivity(AddEditSubjectActivity.editIntent(context, subject.subjectId)) },
                onDelete = { showDeleteDialog = true }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RotatingChip(gradeChips(summary), showAlternate)
            RotatingChip(nextChips(summary), showAlternate, Modifier.weight(1f, fill = false))
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
                    Text("Cancelar", color = MaterialTheme.appColors.accent)
                }
            }
        )
    }
}

/**
 * Botón «⋮» con el menú de editar y eliminar. Pequeño y pegado al borde derecho y arriba, para no
 * ensanchar ni alargar la fila del nombre.
 */
@Composable
private fun SubjectMenu(onEdit: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    // El Box ancla el menú al botón; el offset lo mete en el margen derecho de la tarjeta
    Box(modifier = modifier.offset(x = 8.dp, y = (-6).dp)) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable { open = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Opciones",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Editar") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.appColors.accent) },
                onClick = {
                    open = false
                    onEdit()
                }
            )
            DropdownMenuItem(
                text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                onClick = {
                    open = false
                    onDelete()
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
