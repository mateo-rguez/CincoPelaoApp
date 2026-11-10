package io.github.mateorguez.cincopelao.ui.subjects

import io.github.mateorguez.cincopelao.ui.theme.appColors
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.ui.theme.CincoPelaoAppTheme
import io.github.mateorguez.cincopelao.ui.theme.DelightMedium
import io.github.mateorguez.cincopelao.ui.theme.DelightRegular
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Alta o edición de una asignatura con sus eventos. */
@AndroidEntryPoint
class AddEditSubjectActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CincoPelaoAppTheme {
                AddEditSubjectScreen(onClose = { finish() })
            }
        }
    }

    companion object {
        /** Extra con el id (Long) de la asignatura a editar; sin él, se crea una nueva. */
        const val EXTRA_SUBJECT_ID = "subjectId"

        fun editIntent(context: Context, subjectId: Long): Intent =
            Intent(context, AddEditSubjectActivity::class.java).putExtra(EXTRA_SUBJECT_ID, subjectId)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSubjectScreen(onClose: () -> Unit, viewModel: AddEditSubjectViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showIncompleteDialog by remember { mutableStateOf(false) }
    var showWeightsDialog by remember { mutableStateOf(false) }

    /** Espera a que termine de guardarse antes de cerrar (si no, el guardado podía cancelarse). */
    fun saveAndClose() {
        scope.launch {
            viewModel.save()
            onClose()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "Editar Asignatura" else "Añadir Asignatura", fontFamily = DelightMedium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.appColors.bar,
                    titleContentColor = MaterialTheme.appColors.onBar,
                    navigationIconContentColor = MaterialTheme.appColors.onBar,
                    actionIconContentColor = MaterialTheme.appColors.onBar
                ),
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            Button(onClick = {
                when (viewModel.validate()) {
                    SubjectFormValidation.Valid -> saveAndClose()
                    SubjectFormValidation.Incomplete -> showIncompleteDialog = true
                    SubjectFormValidation.WeightsNotHundred -> showWeightsDialog = true
                }
            }) {
                Text("Guardar")
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(24.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedTextField(
                value = state.subject.name,
                onValueChange = { if (it.length <= 50) viewModel.updateName(it) },
                label = { Text("Nombre de la asignatura") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row {
                CourseDropdown(
                    selected = state.subject.course,
                    onSelected = viewModel::updateCourse,
                    modifier = Modifier.weight(0.45f).padding(end = 7.dp)
                )
                OutlinedTextField(
                    value = state.subject.abbrev,
                    onValueChange = { viewModel.updateAbbrev(it.uppercase().take(6)) },
                    label = { Text("Abrev.") },
                    modifier = Modifier.weight(0.55f),
                    singleLine = true
                )
            }

            Spacer(Modifier.height(10.dp))

            AddEventButton(subject = state.subject, onEventCreated = viewModel::addEvent)

            if (state.events.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                EditableEventList(
                    subject = state.subject,
                    events = state.events,
                    onUpdate = viewModel::updateEvent,
                    onDelete = { item ->
                        val removed = viewModel.removeEvent(item.key) ?: return@EditableEventList
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Evento '${item.event.name}' eliminado",
                                actionLabel = "Deshacer",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) viewModel.restoreEvent(removed)
                        }
                    }
                )
            }
        }
    }

    if (showIncompleteDialog) {
        AlertDialog(
            onDismissRequest = { showIncompleteDialog = false },
            title = { Text("Formulario incompleto") },
            text = { Text("Por favor, rellena todos los campos antes de guardar. Una asignatura debe tener como mínimo un evento.") },
            confirmButton = {
                TextButton(onClick = { showIncompleteDialog = false }) { Text("Aceptar") }
            }
        )
    }

    if (showWeightsDialog) {
        AlertDialog(
            onDismissRequest = { showWeightsDialog = false },
            title = { Text("Advertencia") },
            text = { Text("La suma de las ponderaciones de todos los eventos no es 100%, ¿Estás seguro de querer continuar? Luego puedes editarlos en cualquier momento.") },
            dismissButton = {
                TextButton(onClick = { showWeightsDialog = false }) { Text("Cancelar") }
            },
            confirmButton = {
                TextButton(onClick = {
                    showWeightsDialog = false
                    saveAndClose()
                }) { Text("Continuar de todas formas") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseDropdown(selected: String, onSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    val courses = listOf("1º", "2º", "3º", "4º", "5º", "6º")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Curso") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            courses.forEach { course ->
                DropdownMenuItem(
                    text = { Text(course) },
                    onClick = {
                        onSelected(course)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AddEventButton(subject: Subject, onEventCreated: (Event) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    Button(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Event, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Añadir evento")
    }

    if (showDialog) {
        EventDialog(
            subjectId = subject.subjectId,
            event = null,
            onDismiss = { showDialog = false },
            onEventCreated = onEventCreated,
            onEventUpdated = {}
        )
    }
}

/** Tarjetas de los eventos del formulario, con editar y eliminar. */
@Composable
private fun EditableEventList(
    subject: Subject,
    events: List<EditableEvent>,
    onUpdate: (key: Long, Event) -> Unit,
    onDelete: (EditableEvent) -> Unit,
) {
    val accentColor = MaterialTheme.appColors.accent
    val dateFormatter = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        events.forEach { item ->
            // key: el estado de cada tarjeta (diálogo abierto) va con su evento aunque cambie el orden
            key(item.key) {
                val event = item.event
                var showEditDialog by remember { mutableStateOf(false) }

                if (showEditDialog) {
                    EventDialog(
                        subjectId = subject.subjectId,
                        event = event,
                        onDismiss = { showEditDialog = false },
                        onEventCreated = {},
                        onEventUpdated = { onUpdate(item.key, it) }
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, fontFamily = DelightRegular)) {
                                        append(event.name)
                                    }
                                    event.weight?.let {
                                        append("  ")
                                        withStyle(
                                            SpanStyle(
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Normal
                                            )
                                        ) { append("${formatNumber(it)}%") }
                                    }
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Row(modifier = Modifier.height(30.dp)) {
                                IconButton(onClick = { showEditDialog = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar evento", tint = accentColor, modifier = Modifier.size(25.dp))
                                }
                                IconButton(onClick = { onDelete(item) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar evento", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(25.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        val formattedDate = event.date.takeIf { it > 0 }?.let { dateFormatter.format(Date(it)) } ?: "Sin fecha"
                        Text(
                            text = "${event.type}${if (!event.modal.isNullOrBlank()) " (${event.modal})" else ""} - $formattedDate",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Nota: ${event.score?.let(::formatNumber) ?: "N/A"} (Mínima: ${event.minScore?.let(::formatNumber) ?: "N/A"})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
