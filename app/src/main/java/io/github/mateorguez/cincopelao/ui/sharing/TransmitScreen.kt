package io.github.mateorguez.cincopelao.ui.sharing

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.SignalCellular0Bar
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularAlt1Bar
import androidx.compose.material.icons.filled.SignalCellularAlt2Bar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mateorguez.cincopelao.sharing.protocol.PayloadSummary
import io.github.mateorguez.cincopelao.sharing.protocol.ShareSelection
import io.github.mateorguez.cincopelao.sharing.session.SharePhase
import io.github.mateorguez.cincopelao.ui.theme.DelightMedium

@Composable
fun TransmitScreen(
    onFinish: () -> Unit,
    onOpenSettings: (SettingsTarget) -> Unit,
    viewModel: TransmitViewModel = hiltViewModel()
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
        onPauseOrDispose { viewModel.onPause() }
    }

    TransmitContent(
        state = state,
        onGrantPermissions = { permissions.launch(viewModel.missingPermissions().toTypedArray()) },
        onOpenSettings = onOpenSettings,
        onConnect = viewModel::connect,
        onApprove = viewModel::approve,
        onDecline = viewModel::decline,
        onRestart = viewModel::restart,
        onFinish = onFinish
    )
}

@Composable
private fun TransmitContent(
    state: TransmitUiState,
    onGrantPermissions: () -> Unit,
    onOpenSettings: (SettingsTarget) -> Unit,
    onConnect: (NearbyReceiver) -> Unit,
    onApprove: () -> Unit,
    onDecline: () -> Unit,
    onRestart: () -> Unit,
    onFinish: () -> Unit
) {
    val targetName = state.target?.displayName ?: "el receptor"
    val phase = state.phase
    val (title, subtitle) = when (phase) {
        is SharePhase.NeedsSetup -> "Falta algo" to null
        SharePhase.Idle -> "Preparando…" to null
        SharePhase.Scanning -> "Buscando receptores" to "Pide al otro móvil que abra Recibir y se haga visible"
        SharePhase.ReadingBeacon -> "Contactando con «$targetName»" to "Leyendo su baliza por Bluetooth LE"
        SharePhase.ConnectingWifi -> "Conectando por Wi‑Fi Direct" to "Uniéndose a la red de «$targetName»"
        SharePhase.Handshaking -> "Presentándose" to "Comprobando que los dos habláis el mismo protocolo"
        SharePhase.Choosing -> "Conectado con «$targetName»" to "Esperando a que elija qué quiere recibir"
        SharePhase.AwaitingApproval -> "«$targetName» te pide" to "Revisa la petición y decide si la envías"
        is SharePhase.Transferring -> "Enviando" to null
        SharePhase.Done -> "¡Enviado!" to "El otro móvil ha confirmado que los datos han llegado íntegros"
        is SharePhase.Failed -> "No se ha podido enviar" to phase.message
        SharePhase.Received, SharePhase.Importing, SharePhase.Advertising -> "" to null
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
                onGrantPermissions = onGrantPermissions,
                onOpenAppSettings = { onOpenSettings(SettingsTarget.APP) },
                onFix = { onOpenSettings(SettingsTarget.of(it)) }
            )
            SharePhase.Scanning -> ReceiverList(state.receivers, onConnect)
            SharePhase.ReadingBeacon, SharePhase.ConnectingWifi, SharePhase.Handshaking ->
                SecondaryButton("Cancelar", onRestart)
            SharePhase.Choosing -> {
                state.peer?.let { VerificationCode(it.verificationCode) }
                SecondaryButton("Cancelar", onRestart)
            }
            SharePhase.AwaitingApproval -> {
                state.peer?.let { VerificationCode(it.verificationCode) }
                val request = state.request
                val offer = state.offer
                if (request != null && offer != null) RequestCard(request, offer)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryButton("Rechazar", onDecline)
                    Button(onClick = onApprove) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Enviar")
                    }
                }
            }
            is SharePhase.Transferring -> TransferProgress(phase.progress)
            SharePhase.Done -> {
                state.report?.let {
                    Text(
                        "${formatBytes(it.frame.wireBytes.toLong())} en ${formatMillis(it.durationNanos)} · ${formatRate(it.bytesPerSecond)}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryButton("Enviar a otro", onRestart)
                    Button(onClick = onFinish) { Text("Terminar") }
                }
            }
            is SharePhase.Failed -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton("Salir", onFinish)
                Button(onClick = onRestart) { Text("Volver a buscar") }
            }
            else -> Unit
        }

        TechDetailsCard(state.tech, state.log)
    }
}

@Composable
private fun ReceiverList(receivers: List<NearbyReceiver>, onConnect: (NearbyReceiver) -> Unit) {
    if (receivers.isEmpty()) {
        Text(
            "Todavía no hay nadie cerca en modo Recibir…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        receivers.forEach { receiver ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                onClick = { onConnect(receiver) },
                enabled = receiver.isCompatible
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        signalIcon(signalBars(receiver.rssi)),
                        contentDescription = "Señal",
                        tint = shareAccent(),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(receiver.displayName, style = MaterialTheme.typography.titleMedium, fontFamily = DelightMedium)
                        Text(
                            "${receiver.rssi} dBm · ${formatDistance(estimateDistanceMeters(receiver.rssi, receiver.txPower ?: -59))}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        Text(
                            receiver.address,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!receiver.isCompatible) {
                            Text(
                                "Usa el protocolo v${receiver.protocolVersion}: actualizad las dos apps",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    if (receiver.isCompatible) TextButton(onClick = { onConnect(receiver) }, colors = ButtonDefaults.textButtonColors(contentColor = shareAccent())) { Text("Conectar") }
                }
            }
        }
    }
}

private fun signalIcon(bars: Int) = when (bars) {
    4 -> Icons.Default.SignalCellular4Bar
    3 -> Icons.Default.SignalCellularAlt
    2 -> Icons.Default.SignalCellularAlt2Bar
    1 -> Icons.Default.SignalCellularAlt1Bar
    else -> Icons.Default.SignalCellular0Bar
}

@Composable
private fun RequestCard(request: ShareSelection, offer: PayloadSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (request.subjectsAndEvents) RequestRow("Asignaturas y eventos", "${offer.subjects} · ${offer.events}")
            if (request.subjectTasks) RequestRow("Tareas de asignaturas", "${offer.subjectTasks}")
            if (request.generalTasks) RequestRow("Tareas generales", "${offer.generalTasks}")
            if (request.schedule) {
                RequestRow("Horario", if (offer.scheduleBytes > 0) "PNG ${formatBytes(offer.scheduleBytes.toLong())}" else "no tienes")
            }
            Text(
                "Las notas y lo que tengas completado no se comparten.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun RequestRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(value, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}
