package io.github.mateorguez.cincopelao.ui.subjects

import android.os.Bundle
import android.widget.Space
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import io.github.mateorguez.cincopelao.data.local.entity.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.github.mateorguez.cincopelao.R
import io.github.mateorguez.cincopelao.data.prefs.getDarkModeFlow
import io.github.mateorguez.cincopelao.ui.MainViewModel
import io.github.mateorguez.cincopelao.ui.theme.CincoPelaoTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddEditSubjectActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            val context = LocalContext.current
            val isDarkTheme by context.getDarkModeFlow().collectAsState(initial = false)

            val subjectId = intent.getIntExtra("subjectId", -1)
            val isEditMode = subjectId != -1
            val addSubjectViewModel: AddEditSubjectViewModel = hiltViewModel()
            addSubjectViewModel.setEditMode(isEditMode)
            if (isEditMode) {
                val mainViewModel: MainViewModel = hiltViewModel()
                lifecycleScope.launch {
                    val subject = mainViewModel.getSubjectById(subjectId)
                    val events = mainViewModel.getEventsForSubject(subjectId)
                    addSubjectViewModel.setSubject(subject)
                    addSubjectViewModel.setEvents(events)
                }
            }

            CincoPelaoTheme(darkTheme = isDarkTheme) {
                AddSubjectScreen(onClose = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubjectTopBar(onMenuClick: () -> Unit) {
    TopAppBar(
        title = {
            val addSubjectViewModel: AddEditSubjectViewModel = hiltViewModel()
            val editMode = addSubjectViewModel.isEditMode.collectAsState().value
            if (editMode) {
                Text(text = "Editar Asignatura", fontFamily = FontFamily(Font(R.font.delight_medium)))
            } else {
                Text(text = "Añadir Asignatura", fontFamily = FontFamily(Font(R.font.delight_medium)))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
        }
    )
}

@Composable
fun AddSubjectScreen(onClose: () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val addSubjectViewModel: AddEditSubjectViewModel = hiltViewModel()
    val editMode = addSubjectViewModel.isEditMode.collectAsState().value
    Scaffold(
        topBar = {
            AddSubjectTopBar(onMenuClick = onClose)
        },
        floatingActionButton = {
            SaveButton(isEditing = editMode, onDone = onClose)
        },
        floatingActionButtonPosition = FabPosition.End,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        AddSubjectForm(
            modifier = Modifier
                .padding(innerPadding)
                .padding(24.dp),
            snackbarHostState = snackbarHostState,
            scope = scope
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubjectForm(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    val addSubjectViewModel: AddEditSubjectViewModel = hiltViewModel()
    val events by addSubjectViewModel.events.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        SubjectNameInput()
        Row {
            CursoDropdown(modifier = Modifier.weight(0.45f).padding(end = 7.dp))
            AbbreviationTextField(modifier = Modifier.weight(0.55f))
        }

        Spacer(Modifier.height(10.dp))

        AddEventButton()
        if (events.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            EventListCardStack(events = events, onDelete = { eventToDelete ->
                onDeleteEvent(
                    event = eventToDelete,
                    viewModel = addSubjectViewModel,
                    snackbarHostState = snackbarHostState,
                    scope = scope
                )
            })
        }
    }
}

@Composable
fun SubjectNameInput() {

    val viewModel: AddEditSubjectViewModel = hiltViewModel()
    val subject by viewModel.subject.collectAsState()

    OutlinedTextField(
        value = subject.name,
        onValueChange = {
            if (it.length <= 50) {
                viewModel.updateName(it)
            }
        },
        label = { Text("Nombre de la asignatura") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CursoDropdown(modifier: Modifier = Modifier) {

    val viewModel: AddEditSubjectViewModel = hiltViewModel()
    val subject by viewModel.subject.collectAsState()
    var selectedCurso = subject.course

    val cursos = listOf("1º", "2º", "3º", "4º", "5º", "6º")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedCurso,
            onValueChange = {},
            readOnly = true,
            label = { Text("Curso") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            cursos.forEach { curso ->
                DropdownMenuItem(
                    text = { Text(curso) },
                    onClick = {
                        viewModel.updateCurso(curso)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun AbbreviationTextField(modifier: Modifier = Modifier) {
    val viewModel: AddEditSubjectViewModel = hiltViewModel()
    val subject by viewModel.subject.collectAsState()

    OutlinedTextField(
        value = subject.abbrev ?: "",
        onValueChange = { input ->
            val abbrev = input
                .uppercase()
                .take(6)
            viewModel.updateAbbrev(abbrev)
        },
        label = { Text("Abrev.") },
        modifier = modifier,
        singleLine = true
    )
}

@Composable
fun AddEventButton() {
    var showAddEventDialog by remember { mutableStateOf(false) }

    val addSubjectViewModel: AddEditSubjectViewModel = hiltViewModel()
    val subject by addSubjectViewModel.subject.collectAsState()

    Button(
        onClick = { showAddEventDialog = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Event, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Añadir evento")
    }

    if (showAddEventDialog) {
        EventDialog(
            subjectId = subject.subjectId,
            onDismiss = { showAddEventDialog = false },
            onEventCreated = { event -> addSubjectViewModel.addEvent(event) },
            onEventUpdated = { },
            event = null
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDialog(
    subjectId: Int,
    event: Event?,
    onDismiss: () -> Unit,
    onEventCreated: (Event) -> Unit,
    onEventUpdated: (Event) -> Unit,
) {
    val context = LocalContext.current
    val isDarkTheme by context.getDarkModeFlow().collectAsState(initial = false)

    val initialName = event?.name ?: ""
    val initialType = event?.type ?: ""
    val initialModal = event?.modal
    val initialIsPuntuable = event?.isPuntuable ?: false
    val initialWeight = event?.weight?.toString() ?: ""
    val initialDateMillis = event?.date
    val initialMinScore = event?.minScore?.toString() ?: ""

    var name by remember { mutableStateOf(initialName) }
    var type by remember { mutableStateOf(initialType) }
    var modal by remember { mutableStateOf<String?>(initialModal) }
    var isPuntuable by remember { mutableStateOf(initialIsPuntuable) }
    var weight by remember { mutableStateOf(initialWeight) }
    var minScore by remember { mutableStateOf(initialMinScore) }

    var showError by remember { mutableStateOf(false) }
    val showDatePicker = remember { mutableStateOf(false) }

    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis
    )
    val dateMillis = dateState.selectedDateMillis

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
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    )
                )

                EventTypeDropdown(
                    selectedType = type,
                    onTypeSelected = {
                        type = it
                        showError = false
                    },
                    isError = showError && type.isBlank()
                )

                Box(modifier = Modifier.padding(bottom = 7.dp)) {
                    EventModalDropdown(
                        selectedModal = modal ?: "",
                        onTypeSelected = {
                            modal = it
                            showError = false
                        },
                        isError = showError
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
                    onValueChange = {
                        val intValue = it.trim().toIntOrNull()
                        if (intValue == null) {
                            weight = ""
                            showError = false
                        } else if (intValue in 1..100) {
                            weight = it
                            showError = false
                        }
                    },
                    label = { Text("Ponderación (%)") },
                    enabled = isPuntuable,
                    isError = showError && isPuntuable && weight.toFloatOrNull() == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                )

                OutlinedTextField(
                    value = minScore,
                    onValueChange = {
                        val value = it.trim().toFloatOrNull()
                        if (value == null) {
                            minScore = ""
                        } else {
                            minScore = it
                        }
                        showError = false
                    },
                    label = { Text("Nota mínima (%) (p. def. 0)") },
                    enabled = isPuntuable,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                )

                val formatterDisplay = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

                Box(modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker.value = true }
                ) {
                    OutlinedTextField(
                        value = dateMillis?.let { formatterDisplay.format(Date(it)) } ?: "",
                        onValueChange = {},
                        label = { Text("Fecha") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker.value = true }) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Seleccionar fecha"
                                )
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
                val weightFloat = weight.toFloatOrNull()
                val isValid = name.isNotBlank()
                        && type.isNotBlank()
                        && (!isPuntuable || weightFloat != null)
                        && dateMillis != null

                val minScoreFloat = minScore.toFloatOrNull() ?: 0f
                val saveModal = if (modal == "Ninguna") null else modal

                if (isValid && event == null) {
                    val event = Event(
                        subjectId = subjectId,
                        name = name,
                        type = type,
                        modal = saveModal,
                        isPuntuable = isPuntuable,
                        weight = if (isPuntuable) weightFloat else null,
                        date = dateMillis ?: Long.MIN_VALUE,
                        minScore = minScoreFloat
                    )
                    onEventCreated(event)
                    onDismiss()
                } else if (isValid && event != null){
                    val newEvent = event.copy(
                        subjectId = subjectId,
                        name = name,
                        type = type,
                        modal = saveModal,
                        isPuntuable = isPuntuable,
                        weight = if (isPuntuable) weightFloat else null,
                        date = dateMillis ?: Long.MIN_VALUE,
                        minScore = minScoreFloat
                    )
                    onEventUpdated(newEvent)
                    onDismiss()
                } else {
                    showError = true
                }
            }) {
                Text("Guardar", color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss,) {
                Text("Cancelar", color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary)
            }
        }
    )

    if (showDatePicker.value) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton = {
                Button(onClick = { showDatePicker.value = false }) {
                    Text(text = "OK")
                }
            },
            dismissButton = {
                Button(onClick = { showDatePicker.value = false }) {
                    Text(text = "Cancel")
                }
            }
        ) {
            DatePicker(state = dateState, showModeToggle = true)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventTypeDropdown(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    isError: Boolean = false
) {
    val options = listOf("Examen", "Proyecto", "Entregable", "Trabajo", "Presentación", "Ejercicio")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedType,
            onValueChange = {},
            label = { Text("Tipo") },
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            isError = isError
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption) },
                    onClick = {
                        onTypeSelected(selectionOption)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventModalDropdown(
    selectedModal: String,
    onTypeSelected: (String) -> Unit,
    isError: Boolean = false
) {
    val options = listOf("Teoría", "Prácticas", "Seminario", "Taller", "Ninguna (por defecto)")
    var expanded by remember { mutableStateOf(false) }

    val displayText = when (selectedModal) {
        "Teoría", "Prácticas", "Seminario", "Taller" -> selectedModal
        "Ninguna" -> "Ninguna"
        else -> "" // cuando no se ha seleccionado nada aún
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            label = { Text("Modalidad") },
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            },
            placeholder = { if (displayText.isBlank()) Text("Selecciona modalidad") },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            isError = isError
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption) },
                    onClick = {
                        val normalized = when (selectionOption) {
                            "Ninguna (por defecto)" -> "Ninguna"
                            else -> selectionOption
                        }
                        onTypeSelected(normalized)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun EventListCardStack(
    events: List<Event>,
    onDelete: (Event) -> Unit,
) {

    val context = LocalContext.current
    val isDarkTheme by context.getDarkModeFlow().collectAsState(initial = false)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        events.forEach { event ->

            val formattedDate = event.date?.takeIf { it > 0 }?.let {
                SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(it))
            } ?: "Sin fecha"

            var showEditEventDialog by remember { mutableStateOf(false) }
            val addSubjectViewModel: AddEditSubjectViewModel = hiltViewModel()
            val subject by addSubjectViewModel.subject.collectAsState()

            val idx = events.indexOfFirst { it.eventId == event.eventId }

            if (showEditEventDialog) {
                EventDialog(
                    subjectId = subject.subjectId,
                    onDismiss = { showEditEventDialog = false },
                    onEventCreated = { event -> addSubjectViewModel.addEvent(event) },
                    onEventUpdated = { event -> addSubjectViewModel.updateEvent(idx, event)},
                    event = event
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
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, fontFamily = FontFamily(Font(R.font.delight_regular)))){
                                    append(event.name)
                                }
                                event.weight?.let {
                                    append("  ")
                                    withStyle(
                                        style = SpanStyle(
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Normal
                                        )
                                    ) {
                                        append("${it}%")
                                    }
                                }
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(modifier = Modifier.height(30.dp)){
                            IconButton(onClick = { showEditEventDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar evento",
                                    tint = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                            IconButton(onClick = { onDelete(event) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar evento",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${event.type}${if (!event.modal.isNullOrBlank()) " (${event.modal})" else ""} - $formattedDate",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = buildString {
                            if (event.score != null) append("Nota: ${event.score} (Mínima: ${event.minScore})")
                            else append("Nota: N/A (Mínima: ${event.minScore})")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

fun onDeleteEvent(
    event: Event,
    viewModel: AddEditSubjectViewModel,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    viewModel.removeEvent(event)

    scope.launch {
        val result = snackbarHostState.showSnackbar(
            message = "Evento '${event.name}' eliminado",
            actionLabel = "Deshacer",
            duration = SnackbarDuration.Short
        )

        if (result == SnackbarResult.ActionPerformed) {
            viewModel.addEvent(event)
        }
    }
}

@Composable
fun SaveButton(isEditing: Boolean, onDone: () -> Unit) {
    val addSubjectViewModel: AddEditSubjectViewModel = hiltViewModel()
    val mainViewModel: MainViewModel = hiltViewModel()

    val subject by addSubjectViewModel.subject.collectAsState()
    val events by addSubjectViewModel.events.collectAsState()

    var showIncompleteDialog by remember { mutableStateOf(false) }
    var showPercentageDialog by remember { mutableStateOf(false) }

    var continueBTW by remember { mutableStateOf(false) }

    if (showIncompleteDialog) {
        AlertDialog(
            onDismissRequest = { showIncompleteDialog = false },
            title = { Text("Formulario incompleto") },
            text = { Text("Por favor, rellena todos los campos antes de guardar. Una asignatura debe tener como mínimo un evento.") },
            confirmButton = {
                TextButton(onClick = { showIncompleteDialog = false }) {
                    Text("Aceptar")
                }
            }
        )
    }

    if (showPercentageDialog) {
        AlertDialog(
            onDismissRequest = { showPercentageDialog = false },
            title = { Text("Advertencia") },
            text = { Text("La suma de las ponderaciones de todos los eventos no es 100%, ¿Estás seguro de querer continuar? Luego puedes editarlos en cualquier momento.") },
            dismissButton = {
                TextButton(onClick = { showPercentageDialog = false }) {
                    Text("Cancelar")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    continueBTW = true
                    showPercentageDialog = false
                }) {
                    Text("Continuar de todas formas")
                }
            }
        )
    }

    Button(
        onClick = {
            if (addSubjectViewModel.isFormValid() == 0 || continueBTW) {
                mainViewModel.insertSubjectWithEvents(isEditing, subject, events)
                addSubjectViewModel.resetForm()
                onDone()
            } else if (addSubjectViewModel.isFormValid() == 1) {
                showIncompleteDialog = true
            } else {
                showPercentageDialog = true
            }
        }
    ) {
        Text("Guardar")
    }
}