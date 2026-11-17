package io.github.mateorguez.cincopelao.ui.subjects

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.ui.common.ChoicePill
import io.github.mateorguez.cincopelao.ui.tasks.TaskCardFrame
import io.github.mateorguez.cincopelao.ui.theme.CincoPelaoAppTheme
import io.github.mateorguez.cincopelao.ui.theme.appColors
import kotlinx.coroutines.launch

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

/** Margen lateral común: el contenido de la tarjeta, la cabecera de eventos y las filas quedan alineados. */
private val FormEdge = 24.dp

/** Qué evento se está editando en la hoja: uno nuevo o el de esa clave. */
private sealed interface SheetTarget {
    data object New : SheetTarget
    data class Existing(val key: Long) : SheetTarget
}

/**
 * Formulario de asignatura: arriba, la tarjeta tal y como saldrá en la lista, que se despliega en
 * los campos (nombre, abreviatura y curso); debajo, los eventos en filas con la suma de
 * ponderaciones. Cada evento se crea o edita en una hoja inferior ([EventSheet]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSubjectScreen(onClose: () -> Unit, viewModel: AddEditSubjectViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var sheetTarget by remember { mutableStateOf<SheetTarget?>(null) }
    var showWeightsDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Tarjeta de la asignatura desplegada: al crear empieza abierta; se abre sola si falta un dato
    var headerExpanded by rememberSaveable { mutableStateOf(!state.isEditMode) }
    val headerMissing = state.missing.any { it != SubjectField.Events }
    LaunchedEffect(headerMissing) { if (headerMissing) headerExpanded = true }

    /** Abre la hoja de un evento soltando el foco y plegando la tarjeta, para dejar sitio a los eventos. */
    fun openSheet(target: SheetTarget) {
        focusManager.clearFocus()
        headerExpanded = false
        sheetTarget = target
    }

    /** Espera a que termine de guardarse antes de cerrar (si no, el guardado podía cancelarse). */
    fun saveAndClose() {
        if (saving) return
        saving = true
        scope.launch {
            viewModel.save()
            onClose()
        }
    }

    fun trySave() {
        when (viewModel.validate()) {
            SubjectFormValidation.Valid -> saveAndClose()
            SubjectFormValidation.WeightsNotHundred -> showWeightsDialog = true
            SubjectFormValidation.Incomplete -> scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar("Falta algún dato: está marcado en rojo")
            }
        }
    }

    fun back() {
        if (state.hasChanges) showDiscardDialog = true else onClose()
    }
    BackHandler(onBack = ::back)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "Editar asignatura" else "Nueva asignatura") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.appColors.bar,
                    titleContentColor = MaterialTheme.appColors.onBar,
                    navigationIconContentColor = MaterialTheme.appColors.onBar,
                    actionIconContentColor = MaterialTheme.appColors.onBar
                ),
                navigationIcon = {
                    IconButton(onClick = ::back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(onClick = ::trySave, enabled = state.loaded && !saving) {
                        Text("Guardar", color = MaterialTheme.appColors.onBar, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (!state.loaded) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp, bottom = 32.dp)
        ) {
            SubjectHeaderCard(
                subject = state.subject,
                titulation = state.titulation,
                expanded = headerExpanded,
                onToggle = { headerExpanded = !headerExpanded },
                missing = state.missing,
                onNameChange = viewModel::updateName,
                onAbbrevChange = viewModel::updateAbbrev,
                onCourseChange = viewModel::updateCourse
            )

            Spacer(Modifier.height(20.dp))
            EventsHeader(total = state.weightTotal, hasEvents = state.events.isNotEmpty(), onAdd = { openSheet(SheetTarget.New) })
            Spacer(Modifier.height(8.dp))

            if (state.events.isEmpty()) {
                NoEvents(isError = SubjectField.Events in state.missing)
            } else {
                HorizontalDivider(color = DividerColor())
                state.events.forEach { item ->
                    key(item.key) {
                        EventFormRow(item, onClick = { openSheet(SheetTarget.Existing(item.key)) })
                    }
                }
            }
        }
    }

    sheetTarget?.let { target ->
        val editing = (target as? SheetTarget.Existing)?.let { t -> state.events.find { it.key == t.key } }
        EventSheet(
            subjectId = state.subject.subjectId,
            event = editing?.event,
            onDismiss = { sheetTarget = null },
            onSave = { event ->
                if (editing != null) viewModel.updateEvent(editing.key, event) else viewModel.addEvent(event)
            },
            onDelete = editing?.let { item ->
                {
                    val removed = viewModel.removeEvent(item.key)
                    if (removed != null) scope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        val result = snackbarHostState.showSnackbar(
                            message = "«${item.event.name}» eliminado",
                            actionLabel = "Deshacer",
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) viewModel.restoreEvent(removed)
                    }
                }
            }
        )
    }

    if (showWeightsDialog) {
        AlertDialog(
            onDismissRequest = { showWeightsDialog = false },
            title = { Text("Las ponderaciones suman ${formatNumber(state.weightTotal)} %") },
            text = { Text("Normalmente suman 100 %. Puedes guardarla así y cambiarlas cuando quieras.") },
            dismissButton = {
                TextButton(onClick = { showWeightsDialog = false }) { Text("Revisar", color = MaterialTheme.appColors.accent) }
            },
            confirmButton = {
                TextButton(onClick = {
                    showWeightsDialog = false
                    saveAndClose()
                }) { Text("Guardar igualmente", color = MaterialTheme.appColors.accent) }
            }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("¿Descartar los cambios?") },
            text = { Text(if (state.isEditMode) "Lo que has cambiado en esta asignatura se perderá." else "La asignatura no se guardará.") },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Seguir editando", color = MaterialTheme.appColors.accent) }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onClose()
                }) { Text("Descartar", color = MaterialTheme.colorScheme.error) }
            }
        )
    }
}

@Composable
private fun DividerColor() = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)

/**
 * La tarjeta de la asignatura como se verá en la lista, que se actualiza al escribir. Al tocarla
 * se despliegan los campos.
 */
@Composable
private fun SubjectHeaderCard(
    subject: Subject,
    titulation: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
    missing: Set<SubjectField>,
    onNameChange: (String) -> Unit,
    onAbbrevChange: (String) -> Unit,
    onCourseChange: (String) -> Unit,
) {
    TaskCardFrame(
        outerPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        contentPadding = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = FormEdge - 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SubjectBadge(subjectBadge(subject))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject.name.ifBlank { "Nombre de la asignatura" },
                    style = MaterialTheme.typography.titleMedium.copy(lineHeight = 20.sp),
                    fontWeight = FontWeight.Normal,
                    color = if (subject.name.isBlank()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(1.5.dp))
                Text(
                    text = listOf(subject.course, titulation.orEmpty()).filter { it.isNotBlank() }
                        .joinToString(" · ").ifEmpty { "Sin curso" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.Edit,
                contentDescription = if (expanded) "Plegar" else "Editar datos",
                tint = MaterialTheme.appColors.accent,
                modifier = Modifier.size(20.dp)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(start = FormEdge - 12.dp, end = FormEdge - 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = subject.name,
                    onValueChange = { if (it.length <= 50) onNameChange(it) },
                    label = { Text("Nombre") },
                    isError = SubjectField.Name in missing,
                    supportingText = if (SubjectField.Name in missing) ({ Text("Escribe el nombre") }) else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subject.abbrev,
                    onValueChange = { onAbbrevChange(it.uppercase().filterNot(Char::isWhitespace).take(MAX_ABBREV_LENGTH)) },
                    label = { Text("Abreviatura") },
                    isError = SubjectField.Abbrev in missing,
                    supportingText = {
                        Text(if (SubjectField.Abbrev in missing) "Pon una abreviatura" else "Sale en el cuadro de la tarjeta; hasta $MAX_ABBREV_LENGTH letras")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth()
                )
                val courseError = SubjectField.Course in missing
                Text(
                    "Curso",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (courseError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SUBJECT_COURSES.forEach { course ->
                        ChoicePill(
                            label = course,
                            selected = subject.course == course,
                            onClick = { onCourseChange(course) },
                            isError = courseError,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (courseError) {
                    Text("Elige el curso", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

/** Título «Eventos», suma de ponderaciones con su barra y botón de añadir. */
@Composable
private fun EventsHeader(total: Float, hasEvents: Boolean, onAdd: () -> Unit) {
    val status = weightStatus(total)
    val tone by animateColorAsState(
        when (status) {
            WeightStatus.Under -> MaterialTheme.colorScheme.primary
            WeightStatus.Complete -> MaterialTheme.appColors.accent
            WeightStatus.Over -> MaterialTheme.colorScheme.error
        },
        label = "ponderación"
    )
    Column(modifier = Modifier.padding(horizontal = FormEdge)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Eventos", style = MaterialTheme.typography.titleLarge)
                if (hasEvents) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (status == WeightStatus.Complete) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = tone, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = "Ponderación ${formatNumber(total)} % de 100 %",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (status == WeightStatus.Complete) tone else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (status != WeightStatus.Complete) {
                        Text(
                            text = if (status == WeightStatus.Under) "Faltan ${formatNumber(100f - total)} %"
                            else "Sobran ${formatNumber(total - 100f)} %",
                            style = MaterialTheme.typography.bodySmall,
                            color = tone
                        )
                    }
                }
            }
            OutlinedButton(onClick = onAdd, contentPadding = PaddingValues(start = 12.dp, end = 16.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.appColors.accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Añadir", color = MaterialTheme.appColors.accent)
            }
        }
        if (hasEvents) {
            Spacer(Modifier.height(8.dp))
            val fraction by animateFloatAsState((total / 100f).coerceIn(0f, 1f), label = "barra")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(tone)
                )
            }
        }
    }
}

/** Aviso cuando aún no hay eventos; en rojo si se intentó guardar así. */
@Composable
private fun NoEvents(isError: Boolean) {
    Text(
        text = "Aún no hay eventos. Añade al menos uno: exámenes, prácticas, entregas…",
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = FormEdge, vertical = 12.dp)
    )
}

/**
 * Un evento en una fila: nombre y, debajo, tipo, modalidad, fecha, nota mínima y nota; a la
 * derecha, su ponderación. Tocarla abre la hoja para editarlo o eliminarlo.
 */
@Composable
private fun EventFormRow(item: EditableEvent, onClick: () -> Unit) {
    val event = item.event
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = FormEdge, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val date = remember(event.date) { shortEventDate(event.date, System.currentTimeMillis()) }
                Text(
                    text = eventSummaryLine(event, date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = event.weight?.let { formatNumber(it) + " %" } ?: "Sin nota",
                style = if (event.weight != null) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelMedium,
                color = if (event.weight != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(color = DividerColor())
    }
}
