package io.github.mateorguez.cincopelao.ui.sharing

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mateorguez.cincopelao.sharing.protocol.BleAdvert
import io.github.mateorguez.cincopelao.sharing.protocol.PayloadSummary
import io.github.mateorguez.cincopelao.sharing.protocol.ShareSelection
import io.github.mateorguez.cincopelao.sharing.session.SharePhase

@Composable
fun ReceiveScreen(
    onFinish: () -> Unit,
    onOpenSettings: (SettingsTarget) -> Unit,
    viewModel: ReceiveViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.refreshSetup()
    }
    LaunchedEffect(Unit) {
        val missing = viewModel.missingPermissions()
        if (missing.isNotEmpty()) permissions.launch(missing.toTypedArray())
    }
    LifecycleResumeEffect(Unit) {
        viewModel.refreshSetup()
        onPauseOrDispose { }
    }

    val peerName = state.peer?.hello?.deviceName ?: "el emisor"
    val phase = state.phase
    val (title, subtitle) = when (phase) {
        is SharePhase.NeedsSetup -> "Falta algo" to null
        SharePhase.Idle -> "Recibir datos" to "Elige el nombre con el que te verá el otro móvil"
        SharePhase.Advertising -> "Visible como «${state.displayName.trim()}»" to "Esperando a que otro móvil se conecte desde Transmitir"
        SharePhase.Handshaking -> "Conectando" to "Comprobando que los dos habláis el mismo protocolo"
        SharePhase.Choosing -> "Conectado con «$peerName»" to "¿Qué quieres recibir?"
        SharePhase.AwaitingApproval -> "Petición enviada" to "Esperando a que «$peerName» la acepte"
        is SharePhase.Transferring -> "Recibiendo" to null
        SharePhase.Received -> "Datos recibidos" to "Integridad comprobada (CRC32 y SHA-256). Ponles nombre para guardarlos"
        SharePhase.Importing -> "Guardando…" to null
        SharePhase.Done -> "¡Guardado!" to "Se ha creado la instancia «${state.instanceName.trim()}» y ya está activa"
        is SharePhase.Failed -> "No se ha podido recibir" to phase.message
        SharePhase.Scanning, SharePhase.ReadingBeacon, SharePhase.ConnectingWifi -> "" to null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PhaseHeader(icon = { ShareStatusIcon(phase) }, title = title, subtitle = subtitle)

        when (phase) {
            is SharePhase.NeedsSetup -> SetupCard(
                issues = phase.missing,
                onGrantPermissions = { permissions.launch(viewModel.missingPermissions().toTypedArray()) },
                onOpenAppSettings = { onOpenSettings(SettingsTarget.APP) },
                onFix = { onOpenSettings(SettingsTarget.of(it)) }
            )
            SharePhase.Idle -> {
                OutlinedTextField(
                    value = state.displayName,
                    onValueChange = viewModel::setDisplayName,
                    label = { Text("Nombre visible") },
                    singleLine = true,
                    isError = state.displayNameBytes > BleAdvert.MAX_NAME_BYTES,
                    supportingText = {
                        Text(
                            "${state.displayNameBytes}/${BleAdvert.MAX_NAME_BYTES} bytes (lo que cabe en un anuncio BLE)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = viewModel::start, enabled = state.canStart) { Text("Hacerme visible") }
            }
            SharePhase.Advertising, SharePhase.Handshaking, SharePhase.AwaitingApproval ->
                SecondaryButton("Cancelar", viewModel::restart)
            SharePhase.Choosing -> {
                state.peer?.let { VerificationCode(it.verificationCode) }
                SelectionCard(state.selection, viewModel::setSelection)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryButton("Cancelar", viewModel::restart)
                    Button(onClick = viewModel::requestSelection, enabled = !state.selection.isEmpty) { Text("Pedir") }
                }
            }
            is SharePhase.Transferring -> TransferProgress(phase.progress)
            SharePhase.Received -> {
                state.received?.let { ReceivedCard(it) }
                OutlinedTextField(
                    value = state.instanceName,
                    onValueChange = viewModel::setInstanceName,
                    label = { Text("Nombre de la instancia nueva") },
                    singleLine = true,
                    isError = state.importError != null,
                    supportingText = state.importError?.let { error -> { Text(error) } },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryButton("Descartar", onFinish)
                    Button(onClick = viewModel::import, enabled = state.instanceName.isNotBlank()) { Text("Guardar") }
                }
            }
            SharePhase.Done -> Button(onClick = onFinish) { Text("Ir a la app") }
            is SharePhase.Failed -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton("Salir", onFinish)
                Button(onClick = viewModel::restart) { Text("Reintentar") }
            }
            else -> Unit
        }

        TechDetailsCard(state.tech, state.log)
    }
}

@Composable
private fun SelectionCard(selection: ShareSelection, onChange: (ShareSelection) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            SelectionRow("Asignaturas y eventos", null, selection.subjectsAndEvents) {
                onChange(selection.copy(subjectsAndEvents = it, subjectTasks = it && selection.subjectTasks))
            }
            SelectionRow(
                "Tareas de asignaturas",
                if (!selection.subjectsAndEvents) "Necesita las asignaturas" else null,
                selection.subjectTasks,
                enabled = selection.subjectsAndEvents
            ) { onChange(selection.copy(subjectTasks = it)) }
            SelectionRow("Tareas generales", null, selection.generalTasks) {
                onChange(selection.copy(generalTasks = it))
            }
            SelectionRow("Horario", null, selection.schedule) { onChange(selection.copy(schedule = it)) }
        }
    }
}

@Composable
private fun SelectionRow(label: String, hint: String?, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(horizontal = 8.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange, enabled = enabled)
        Column {
            Text(
                label,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            if (hint != null) {
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ReceivedCard(summary: PayloadSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(
                "Asignaturas" to "${summary.subjects}",
                "Eventos" to "${summary.events}",
                "Tareas de asignaturas" to "${summary.subjectTasks}",
                "Tareas generales" to "${summary.generalTasks}",
                "Horario" to if (summary.scheduleBytes > 0) "PNG ${formatBytes(summary.scheduleBytes.toLong())}" else "—"
            ).forEach { (label, value) ->
                Row(Modifier.fillMaxWidth()) {
                    Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(value, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}
