package io.github.mateorguez.cincopelao.ui.subjects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.ui.theme.LocalDarkTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val EVENT_TYPES = listOf("Examen", "Proyecto", "Entregable", "Trabajo", "Presentación", "Ejercicio")
private val EVENT_MODALITIES = listOf("Teoría", "Prácticas", "Seminario", "Taller")
private const val NO_MODALITY = "Ninguna"

/**
 * Diálogo para crear ([event] == null) o editar un evento de una asignatura.
 * Se usa en el formulario de la asignatura y en su pantalla de detalle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDialog(
    subjectId: Int,
    event: Event?,
    onDismiss: () -> Unit,
    onEventCreated: (Event) -> Unit,
    onEventUpdated: (Event) -> Unit,
) {
    val accentColor = if (LocalDarkTheme.current) Color.White else MaterialTheme.colorScheme.primary

    var name by remember { mutableStateOf(event?.name.orEmpty()) }
    var type by remember { mutableStateOf(event?.type.orEmpty()) }
    var modal by remember { mutableStateOf(event?.modal) }
    var isPuntuable by remember { mutableStateOf(event?.isPuntuable ?: false) }
    var weight by remember { mutableStateOf(event?.weight?.let(::formatNumber).orEmpty()) }
    var minScore by remember { mutableStateOf(event?.minScore?.let(::formatNumber).orEmpty()) }
    var dateMillis by remember { mutableStateOf(event?.date?.takeIf { it > 0 }) }

    var showError by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (event == null) "Nuevo evento" else "Editar evento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        if (it.length <= 10) {
                            name = it
                            showError = false
                        }
                    },
                    label = { Text("Nombre del evento") },
                    isError = showError && name.isBlank(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )

                OptionsDropdown(
                    label = "Tipo",
                    selected = type,
                    options = EVENT_TYPES,
                    onSelected = {
                        type = it
                        showError = false
                    },
                    isError = showError && type.isBlank()
                )

                Box(modifier = Modifier.padding(bottom = 7.dp)) {
                    // La modalidad es opcional, así que nunca se marca como error
                    OptionsDropdown(
                        label = "Modalidad",
                        selected = modal.orEmpty(),
                        options = EVENT_MODALITIES + "$NO_MODALITY (por defecto)",
                        placeholder = "Selecciona modalidad",
                        onSelected = { modal = if (it.startsWith(NO_MODALITY)) NO_MODALITY else it }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isPuntuable,
                        onCheckedChange = {
                            isPuntuable = it
                            showError = false
                        }
                    )
                    Text("Es puntuable")
                }

                OutlinedTextField(
                    value = weight,
                    onValueChange = { input ->
                        // Solo enteros de 1 a 100; una entrada no válida se ignora en vez de borrar el campo
                        val value = input.trim()
                        if (value.isEmpty() || value.toIntOrNull()?.let { it in 1..100 } == true) {
                            weight = value
                            showError = false
                        }
                    },
                    label = { Text("Ponderación (%)") },
                    enabled = isPuntuable,
                    isError = showError && isPuntuable && weight.toFloatOrNull() == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                )

                OutlinedTextField(
                    value = minScore,
                    onValueChange = { input ->
                        val value = input.trim().replace(',', '.')
                        if (value.isEmpty() || value.toFloatOrNull() != null) {
                            minScore = value
                            showError = false
                        }
                    },
                    label = { Text("Nota mínima (%) (p. def. 0)") },
                    enabled = isPuntuable,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done)
                )

                val dateFormatter = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
                Box(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                    OutlinedTextField(
                        value = dateMillis?.let { dateFormatter.format(Date(it)) }.orEmpty(),
                        onValueChange = {},
                        label = { Text("Fecha") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(imageVector = Icons.Default.DateRange, contentDescription = "Seleccionar fecha")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isError = showError && dateMillis == null
                    )
                }

                if (showError) {
                    Text(
                        text = "Por favor, completa todos los campos obligatorios.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val weightValue = weight.toFloatOrNull()
                val date = dateMillis
                val isValid = name.isNotBlank() && type.isNotBlank() && (!isPuntuable || weightValue != null) && date != null
                if (!isValid) {
                    showError = true
                    return@TextButton
                }
                val edited = (event ?: Event(subjectId = subjectId, name = "", type = "", date = 0L)).copy(
                    subjectId = subjectId,
                    name = name,
                    type = type,
                    modal = modal.takeUnless { it == NO_MODALITY },
                    isPuntuable = isPuntuable,
                    weight = if (isPuntuable) weightValue else null,
                    date = date!!,
                    minScore = minScore.toFloatOrNull() ?: 0f
                )
                if (event == null) onEventCreated(edited) else onEventUpdated(edited)
                onDismiss()
            }) {
                Text("Guardar", color = accentColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = accentColor)
            }
        }
    )

    if (showDatePicker) {
        // El selector tiene su propio estado: la fecha solo se aplica al pulsar Aceptar
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    pickerState.selectedDateMillis?.let { dateMillis = it }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                Button(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = pickerState, showModeToggle = true)
        }
    }
}

/** Muestra 40.0 como "40" y 37.5 como "37.5". */
private fun formatNumber(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else value.toString()

/** Desplegable de solo lectura con una lista fija de opciones. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionsDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    placeholder: String? = null,
    isError: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            placeholder = placeholder?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            isError = isError
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
