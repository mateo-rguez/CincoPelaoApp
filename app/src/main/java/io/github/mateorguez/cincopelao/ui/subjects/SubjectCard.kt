package io.github.mateorguez.cincopelao.ui.subjects

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import io.github.mateorguez.cincopelao.ui.common.requestDeviceAuth
import io.github.mateorguez.cincopelao.ui.tasks.DUE_CHIP_ALPHA
import io.github.mateorguez.cincopelao.ui.tasks.DueUrgency
import io.github.mateorguez.cincopelao.ui.tasks.TaskCardFrame
import io.github.mateorguez.cincopelao.ui.tasks.dueDateLabel
import io.github.mateorguez.cincopelao.ui.tasks.dueUrgency
import io.github.mateorguez.cincopelao.ui.theme.DelightRegular
import io.github.mateorguez.cincopelao.ui.theme.appColors

/** Lo que muestra la franja de abajo de la tarjeta. */
private enum class CardStage { CLOSED, ACTIONS, CONFIRM_DELETE }

/**
 * Tarjeta de una asignatura: pastilla con su abreviatura, nombre y curso, y debajo dos etiquetas
 * que se alternan: una con la nota (media ↔ último examen, o la nota final si ya está todo hecho)
 * y otra con lo siguiente que hacer (próximo evento ↔ tareas pendientes). Una pulsación abre la
 * asignatura; el botón «⋮» despliega dentro de la tarjeta una franja con editar y eliminar, y
 * eliminar se confirma en esa misma franja.
 */
@Composable
fun SubjectCard(
    summary: SubjectSummary,
    titulation: String?,
    /** Cambia cada pocos segundos (a la vez en todas las tarjetas) para alternar las etiquetas. */
    showAlternate: Boolean,
    /** Si la franja de acciones está desplegada (la lista solo deja una abierta a la vez). */
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val subject = summary.subject
    var confirmingDelete by remember { mutableStateOf(false) }
    LaunchedEffect(menuOpen) { if (!menuOpen) confirmingDelete = false }
    val stage = when {
        !menuOpen -> CardStage.CLOSED
        confirmingDelete -> CardStage.CONFIRM_DELETE
        else -> CardStage.ACTIONS
    }

    TaskCardFrame(
        outerPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        contentPadding = 14.dp,
        modifier = Modifier.clickable {
            // Con la franja abierta, tocar la tarjeta solo la cierra
            if (menuOpen) onMenuOpenChange(false)
            else context.startActivity(SubjectActivity.intent(context, subject.subjectId))
        }
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
            MenuToggle(
                open = menuOpen,
                onClick = { onMenuOpenChange(!menuOpen) },
                modifier = Modifier.align(Alignment.Top)
            )
        }

        // En una columna propia: el espaciado del marco no debe añadir hueco con la franja cerrada
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RotatingChip(gradeChips(summary), showAlternate)
                RotatingChip(nextChips(summary), showAlternate, Modifier.weight(1f, fill = false))
            }
            AnimatedContent(
                targetState = stage,
                transitionSpec = {
                    (fadeIn(tween(150, delayMillis = 90)) togetherWith fadeOut(tween(90)))
                        .using(SizeTransform(clip = true) { _, _ -> spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow) })
                },
                label = "SubjectCardStage"
            ) { shown ->
                when (shown) {
                    CardStage.CLOSED -> Spacer(Modifier.fillMaxWidth())
                    CardStage.ACTIONS -> ActionsStrip(
                        onEdit = {
                            onMenuOpenChange(false)
                            context.startActivity(AddEditSubjectActivity.editIntent(context, subject.subjectId))
                        },
                        onDelete = { confirmingDelete = true }
                    )
                    CardStage.CONFIRM_DELETE -> ConfirmDeleteStrip(
                        name = subject.name,
                        onCancel = { confirmingDelete = false },
                        onConfirm = {
                            onMenuOpenChange(false)
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Botón «⋮» que pasa a «✕» con la franja abierta. Pequeño y pegado al borde derecho y arriba,
 * para no ensanchar ni alargar la fila del nombre.
 */
@Composable
private fun MenuToggle(open: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .offset(x = 8.dp, y = (-6).dp)
            .size(28.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = open, animationSpec = tween(200), label = "SubjectMenuIcon") { isOpen ->
            Icon(
                if (isOpen) Icons.Default.Close else Icons.Default.MoreVert,
                contentDescription = if (isOpen) "Cerrar opciones" else "Opciones",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** Divisor de la franja, del mismo tono que los de las filas de eventos. */
@Composable
private fun StripDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(top = 12.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        thickness = 1.dp
    )
}

/** Franja con Editar y Eliminar, cada uno centrado en su mitad. */
@Composable
private fun ActionsStrip(onEdit: () -> Unit, onDelete: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        StripDivider()
        // El margen inferior de la tarjeta ya separa: los botones solo suben un poco
        Row(Modifier.fillMaxWidth().padding(top = 6.dp).offset(y = 4.dp)) {
            StripButton(Icons.Default.Edit, "Editar", MaterialTheme.appColors.accent, onEdit, Modifier.weight(1f))
            StripButton(Icons.Default.Delete, "Eliminar", MaterialTheme.colorScheme.error, onDelete, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StripButton(icon: ImageVector, text: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = color)
    }
}

/** Confirmación del borrado en la propia franja, con desbloqueo del móvil; después queda «Deshacer» en el aviso. */
@Composable
private fun ConfirmDeleteStrip(name: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        StripDivider()
        Spacer(Modifier.height(10.dp))
        Text(
            "¿Eliminar «${name.trim()}»?",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Se borrarán también sus eventos y tareas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        val context = LocalContext.current
        Row(Modifier.align(Alignment.End).offset(x = 8.dp, y = 4.dp)) {
            TextButton(onClick = onCancel) { Text("Cancelar", color = MaterialTheme.appColors.accent) }
            // Como en Instancias, se pide además el desbloqueo del móvil (huella, cara, PIN o patrón)
            TextButton(onClick = { requestDeviceAuth(context, "Eliminar asignatura", "Confirma que quieres eliminar «${name.trim()}»", onConfirm) }) {
                Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Pastilla cuadrada con la abreviatura, con el degradado de asignaturas de la paleta. */
@Composable
internal fun SubjectBadge(text: String) {
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
