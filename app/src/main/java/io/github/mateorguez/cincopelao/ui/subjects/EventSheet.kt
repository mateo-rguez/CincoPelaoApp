package io.github.mateorguez.cincopelao.ui.subjects

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.ui.common.ChoicePill
import io.github.mateorguez.cincopelao.ui.theme.DelightMedium
import io.github.mateorguez.cincopelao.ui.theme.appColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val EVENT_TYPES = listOf("Examen", "Proyecto", "Entregable", "Trabajo", "Presentación", "Ejercicio")
private val EVENT_MODALITIES = listOf("Teoría", "Prácticas", "Seminario", "Taller")

/**
 * Hoja inferior para crear ([event] == null) o editar un evento de una asignatura. Se usa en el
 * formulario de la asignatura y en su pantalla de detalle.
 *
 * Tipo y modalidad se eligen con pastillas (la modalidad es opcional: se quita pulsándola otra
 * vez); el interruptor «Cuenta para la nota» despliega la ponderación y la nota mínima. Los
 * errores se marcan en cada campo al intentar guardar.
 *
 * @param onDelete si no es null, se muestra «Eliminar».
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EventSheet(
    subjectId: Long,
    event: Event?,
    onDismiss: () -> Unit,
    onSave: (Event) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val accent = MaterialTheme.appColors.accent
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(event?.name.orEmpty()) }
    var type by remember { mutableStateOf(event?.type.orEmpty()) }
    var modal by remember { mutableStateOf(event?.modal?.takeIf { it in EVENT_MODALITIES }) }
    var isPuntuable by remember { mutableStateOf(event?.isPuntuable ?: true) }
    var weight by remember { mutableStateOf(event?.weight?.let(::inputNumber).orEmpty()) }
    // La nota mínima se guarda en % (40) y se escribe sobre 10 (4), como se muestra en el detalle
    var minScore by remember {
        mutableStateOf(event?.minScore?.takeIf { it > 0f }?.let { inputNumber(it / 10f) }.orEmpty())
    }
    var dateMillis by remember { mutableStateOf(event?.date?.takeIf { it > 0 }) }

    // Valores al abrir: si nada ha cambiado, la hoja se cierra sin preguntar
    val initial = remember { listOf(name, type, modal, isPuntuable, weight, minScore, dateMillis) }
    val hasChanges = listOf(name, type, modal, isPuntuable, weight, minScore, dateMillis) != initial
    val currentHasChanges by rememberUpdatedState(hasChanges)
    var showDiscardDialog by remember { mutableStateOf(false) }

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

    var showErrors by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val errors = if (showErrors) invalidEventFields(name, type, dateMillis, isPuntuable, weight, minScore) else emptySet()

    /** Cierra con la animación de la hoja y después avisa. */
    fun close(then: () -> Unit = {}) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            then()
            onDismiss()
        }
    }

    fun save() {
        if (invalidEventFields(name, type, dateMillis, isPuntuable, weight, minScore).isNotEmpty()) {
            showErrors = true
            return
        }
        val edited = (event ?: Event(subjectId = subjectId, name = "", type = "", date = 0L)).copy(
            subjectId = subjectId,
            name = name.trim(),
            type = type,
            modal = modal,
            isPuntuable = isPuntuable,
            weight = if (isPuntuable) weight.trim().toFloat() else null,
            minScore = if (isPuntuable) (parseNumber(minScore) ?: 0f) * 10f else 0f,
            date = dateMillis ?: 0L,
        )
        close { onSave(edited) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // El botón atrás lo gestiona el BackHandler de dentro (el de la hoja no pregunta)
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false)
    ) {
        BackHandler { if (hasChanges) showDiscardDialog = true else close() }
        // La hoja es otra ventana: su foco se gestiona con el FocusManager de dentro, no con el de la pantalla
        val focusManager = LocalFocusManager.current
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (event == null) "Nuevo evento" else "Editar evento",
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = DelightMedium
            )

            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= MAX_EVENT_NAME_LENGTH) name = it },
                label = { Text("Nombre") },
                placeholder = { Text("Cuest. 1, Práctica 2…") },
                isError = EventField.Name in errors,
                supportingText = {
                    Text(if (EventField.Name in errors) "Ponle un nombre" else "${name.length}/$MAX_EVENT_NAME_LENGTH")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )

            FieldGroup("Tipo", error = "Elige un tipo".takeIf { EventField.Type in errors }) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EVENT_TYPES.forEach { option ->
                        ChoicePill(
                            label = option,
                            selected = type == option,
                            onClick = {
                                focusManager.clearFocus()
                                type = option
                            },
                            isError = EventField.Type in errors,
                            horizontalPadding = 14.dp
                        )
                    }
                }
            }

            FieldGroup("Modalidad (opcional)") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EVENT_MODALITIES.forEach { option ->
                        ChoicePill(
                            label = option,
                            selected = modal == option,
                            onClick = {
                                focusManager.clearFocus()
                                modal = if (modal == option) null else option
                            },
                            horizontalPadding = 14.dp
                        )
                    }
                }
            }

            FieldGroup("Fecha", error = "Elige la fecha".takeIf { EventField.Date in errors }) {
                val dateFormatter = remember { SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale.getDefault()) }
                ChoicePill(
                    label = dateMillis?.let { dateFormatter.format(Date(it)).replaceFirstChar(Char::uppercase) } ?: "Elegir fecha",
                    selected = false,
                    onClick = {
                        // Si no, al cerrar el selector el foco vuelve al nombre y se abre el teclado
                        focusManager.clearFocus()
                        showDatePicker = true
                    },
                    icon = Icons.Outlined.Event,
                    isError = EventField.Date in errors,
                    horizontalPadding = 14.dp
                )
            }

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            focusManager.clearFocus()
                            isPuntuable = !isPuntuable
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cuenta para la nota", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = if (isPuntuable) "Con ponderación y nota" else "Solo se marca como hecho",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = isPuntuable, onCheckedChange = {
                        focusManager.clearFocus()
                        isPuntuable = it
                    })
                }
                AnimatedVisibility(
                    visible = isPuntuable,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { input ->
                                // Solo cifras; el rango (1–100) se comprueba al guardar
                                if (input.length <= 3 && input.all(Char::isDigit)) weight = input
                            },
                            label = { Text("Ponderación") },
                            suffix = { Text("%") },
                            isError = EventField.Weight in errors,
                            supportingText = { Text("De 1 a 100") }, // siempre visible: con error solo cambia a rojo
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = minScore,
                            onValueChange = { input ->
                                if (input.length <= 5 && input.all { it.isDigit() || it == ',' || it == '.' }) minScore = input
                            },
                            label = { Text("Nota mínima") },
                            placeholder = { Text("0") },
                            suffix = { Text("/ 10") },
                            isError = EventField.MinScore in errors,
                            supportingText = { Text("Opcional · 0 a 10") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    TextButton(onClick = { close(onDelete) }) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { if (hasChanges) showDiscardDialog = true else close() }) {
                    Text("Cancelar", color = accent)
                }
                TextButton(onClick = ::save) { Text("Guardar", color = accent) }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(if (event == null) "¿Descartar el evento?" else "¿Descartar los cambios?") },
            text = { Text("Lo que has escrito en el evento se perderá.") },
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

/** Título pequeño encima de un grupo de opciones, con el error debajo si lo hay. */
@Composable
private fun FieldGroup(title: String, error: String? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

