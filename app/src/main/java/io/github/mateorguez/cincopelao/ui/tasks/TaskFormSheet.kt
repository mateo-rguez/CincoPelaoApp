package io.github.mateorguez.cincopelao.ui.tasks

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.local.entity.Task
import io.github.mateorguez.cincopelao.ui.common.ChoicePill
import io.github.mateorguez.cincopelao.ui.common.FieldGroup
import io.github.mateorguez.cincopelao.ui.theme.appColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Largo máximo del nombre de una tarea madre en su pastilla. */
private const val PARENT_LABEL_LENGTH = 28

/**
 * Tareas que pueden ser madre de [task]: solo hay un nivel de subtareas, así que valen las
 * tareas de primer nivel sin archivar (y nunca ella misma). Si [task] ya tiene subtareas, no
 * puede pasar a ser subtarea de otra: no hay ninguna.
 */
internal fun parentOptions(task: Task?, candidates: List<Task>): List<Task> {
    if (task != null && candidates.any { it.parentTaskId == task.taskId }) return emptyList()
    return candidates.filter { it.taskId != task?.taskId && it.parentTaskId == null && !it.archived }
}

/** «Nueva tarea · PSI», «Editar tarea general»… */
internal fun taskFormTitle(isNew: Boolean, subjectAbbrev: String?): String {
    val action = if (isNew) "Nueva tarea" else "Editar tarea"
    return if (subjectAbbrev == null) "$action general" else "$action · $subjectAbbrev"
}

/** Recorta un texto largo con «…» para que quepa en una pastilla. */
internal fun ellipsize(text: String, max: Int): String =
    if (text.length <= max) text else text.take(max - 1).trimEnd() + "…"

/**
 * Formulario (hoja inferior) para crear o editar una tarea, con el mismo estilo que el de
 * eventos: fecha en una pastilla (con × para quitarla) y tarea madre en pastillas.
 *
 * @param task la tarea a editar, o null para crear una nueva.
 * @param parentCandidates tareas del mismo bloque que pueden ser su tarea madre.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskFormSheet(
    task: Task?,
    subject: Subject?,
    parentCandidates: List<Task>,
    onSave: (title: String, description: String, dueDateMillis: Long?, parentTaskId: Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val accent = MaterialTheme.appColors.accent
    val scope = rememberCoroutineScope()

    // Estado del formulario: se inicializa una sola vez con los datos de la tarea
    var title by remember { mutableStateOf(task?.title.orEmpty()) }
    var description by remember { mutableStateOf(task?.description.orEmpty()) }
    var dateMillis by remember { mutableStateOf(task?.dueDate) }
    var parentId by remember { mutableStateOf(task?.parentTaskId) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val parents = remember(task, parentCandidates) { parentOptions(task, parentCandidates) }

    val hasChanges = title != task?.title.orEmpty() ||
        description != task?.description.orEmpty() ||
        dateMillis != task?.dueDate ||
        parentId != task?.parentTaskId
    val currentHasChanges by rememberUpdatedState(hasChanges)

    // Arrastrar hacia abajo o tocar fuera con cambios no cierra: pregunta antes
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { value ->
            if (value == SheetValue.Hidden && currentHasChanges) {
                showDiscardDialog = true
                false
            } else {
                true
            }
        }
    )

    /** Cierra con la animación de la hoja y después avisa. */
    fun close(then: () -> Unit = {}) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            then()
            onDismiss()
        }
    }

    fun save() {
        if (title.isBlank()) return
        close { onSave(title.trim(), description.trim(), dateMillis, parentId) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // El botón atrás lo gestiona el BackHandler de dentro (el de la hoja no pregunta)
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false)
    ) {
        BackHandler { if (hasChanges) showDiscardDialog = true else close() }
        // La hoja es otra ventana: su foco se gestiona con el FocusManager de dentro
        val focusManager = LocalFocusManager.current
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = taskFormTitle(isNew = task == null, subjectAbbrev = subject?.abbrev),
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título") },
                maxLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción (opcional)") },
                maxLines = 5,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 200.dp)
            )

            FieldGroup("Fecha (opcional)") {
                val dateFormatter = remember { SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale.getDefault()) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChoicePill(
                        label = dateMillis?.let { dateFormatter.format(Date(it)).replaceFirstChar(Char::uppercase) } ?: "Elegir fecha",
                        selected = false,
                        onClick = {
                            // Si no, al cerrar el selector el foco vuelve al título y se abre el teclado
                            focusManager.clearFocus()
                            showDatePicker = true
                        },
                        icon = Icons.Outlined.Event,
                        horizontalPadding = 14.dp
                    )
                    if (dateMillis != null) {
                        IconButton(onClick = { dateMillis = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Quitar fecha", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (parents.isNotEmpty()) {
                FieldGroup("Tarea madre (opcional)") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoicePill(
                            label = "Ninguna",
                            selected = parentId == null,
                            onClick = {
                                focusManager.clearFocus()
                                parentId = null
                            },
                            horizontalPadding = 14.dp
                        )
                        parents.forEach { candidate ->
                            ChoicePill(
                                label = ellipsize(candidate.title, PARENT_LABEL_LENGTH),
                                selected = parentId == candidate.taskId,
                                onClick = {
                                    focusManager.clearFocus()
                                    parentId = candidate.taskId
                                },
                                horizontalPadding = 14.dp,
                                modifier = Modifier.widthIn(max = 280.dp)
                            )
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { if (hasChanges) showDiscardDialog = true else close() }) {
                    Text("Cancelar", color = accent)
                }
                TextButton(onClick = ::save, enabled = title.isNotBlank()) {
                    Text("Guardar", color = if (title.isNotBlank()) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(if (task == null) "¿Descartar la tarea?" else "¿Descartar los cambios?") },
            text = { Text("Lo que has escrito en la tarea se perderá.") },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Seguir editando", color = accent) }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    close()
                }) { Text("Descartar", color = MaterialTheme.colorScheme.error) }
            }
        )
    }

    if (showDatePicker) {
        // El selector tiene su propio estado: la fecha solo se aplica al pulsar Aceptar
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { dateMillis = it }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = pickerState, showModeToggle = true)
        }
    }
}
