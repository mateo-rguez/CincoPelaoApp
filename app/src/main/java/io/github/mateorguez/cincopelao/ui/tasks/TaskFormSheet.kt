package io.github.mateorguez.cincopelao.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.local.entity.Task
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formulario (hoja inferior) para crear o editar una tarea.
 *
 * @param task la tarea a editar, o null para crear una nueva.
 * @param parentCandidates tareas del mismo bloque que pueden ser su tarea madre.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormSheet(
    task: Task?,
    subject: Subject?,
    parentCandidates: List<Task>,
    onSave: (title: String, description: String, dueDateMillis: Long?, parentTaskId: Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val beingEdited = task != null

    // Estado del formulario: se inicializa una sola vez con los datos de la tarea
    var title by remember { mutableStateOf(task?.title.orEmpty()) }
    var description by remember { mutableStateOf(task?.description.orEmpty()) }
    var selectedDateMillis by remember { mutableStateOf(task?.dueDate) }
    var selectedParentId by remember { mutableStateOf(task?.parentTaskId) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
    var showDatePicker by remember { mutableStateOf(false) }
    var expandedParentMenu by remember { mutableStateOf(false) }

    val availableParents = parentCandidates.filter { it.taskId != task?.taskId && it.parentTaskId == null && !it.archived }
    val selectedParent = parentCandidates.find { it.taskId == selectedParentId }

    val formatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Hay cambios si algo difiere de lo que había al abrir el formulario
    val formHasChanges by remember {
        derivedStateOf {
            title != task?.title.orEmpty() ||
                description != task?.description.orEmpty() ||
                selectedDateMillis != task?.dueDate ||
                selectedParentId != task?.parentTaskId
        }
    }
    val formHasChangesState by rememberUpdatedState(formHasChanges)

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        // Con cambios sin guardar, deslizar hacia abajo no cierra el formulario
        confirmValueChange = { !(it == SheetValue.Hidden && formHasChangesState) }
    )

    var showDiscardDialog by remember { mutableStateOf(false) }

    fun close() {
        coroutineScope.launch {
            sheetState.hide()
            onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = when {
                    subject == null && !beingEdited -> "Nueva tarea general"
                    subject != null && !beingEdited -> "Nueva tarea (${subject.abbrev})"
                    subject == null -> "Editar tarea general"
                    else -> "Editar tarea (${subject.abbrev})"
                },
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título de la tarea") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción (opcional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 200.dp),
                maxLines = 5,
                singleLine = false,
                keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Sentences)
            )

            // Fecha (solo lectura; se elige con el selector)
            OutlinedTextField(
                value = selectedDateMillis?.let { formatter.format(Date(it)) } ?: "",
                onValueChange = {},
                label = { Text("Fecha (opcional)") },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Elegir fecha")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            )

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            selectedDateMillis = datePickerState.selectedDateMillis
                            showDatePicker = false
                        }) {
                            Text("Aceptar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancelar")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            // Tarea madre
            ExposedDropdownMenuBox(
                expanded = expandedParentMenu,
                onExpandedChange = { expandedParentMenu = !expandedParentMenu }
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = selectedParent?.title ?: "",
                    onValueChange = {},
                    label = { Text("Tarea madre (opcional)") },
                    placeholder = { Text("Sin tarea madre") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedParentMenu) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expandedParentMenu,
                    onDismissRequest = { expandedParentMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Sin tarea madre") },
                        onClick = {
                            selectedParentId = null
                            expandedParentMenu = false
                        }
                    )
                    availableParents.forEach { candidate ->
                        DropdownMenuItem(
                            text = { Text(candidate.title) },
                            onClick = {
                                selectedParentId = candidate.taskId
                                expandedParentMenu = false
                            }
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { if (formHasChanges) showDiscardDialog = true else close() }) {
                    Text("Cancelar")
                }

                TextButton(
                    enabled = title.isNotBlank(),
                    onClick = {
                        onSave(title, description, selectedDateMillis, selectedParentId)
                        close()
                    }
                ) {
                    Text("Guardar")
                }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { /* tocar fuera del diálogo no hace nada */ },
            title = { Text("Descartar cambios") },
            text = { Text("Tienes cambios sin guardar. ¿Descartar?") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    close()
                }) {
                    Text("Descartar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Seguir editando")
                }
            }
        )
    }
}
