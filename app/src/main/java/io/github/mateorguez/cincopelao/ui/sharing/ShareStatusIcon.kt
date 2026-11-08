package io.github.mateorguez.cincopelao.ui.sharing

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.mateorguez.cincopelao.sharing.session.SharePhase

/** Icono de la fase actual. */
@Composable
fun ShareStatusIcon(phase: SharePhase, modifier: Modifier = Modifier) {
    val icon = when (phase) {
        is SharePhase.NeedsSetup, SharePhase.Idle -> Icons.Default.SettingsSuggest
        SharePhase.Advertising -> Icons.Default.Bluetooth
        SharePhase.Scanning, SharePhase.ReadingBeacon -> Icons.AutoMirrored.Filled.BluetoothSearching
        SharePhase.ConnectingWifi, SharePhase.Handshaking -> Icons.Default.Wifi
        SharePhase.Choosing, SharePhase.AwaitingApproval -> Icons.AutoMirrored.Filled.FactCheck
        is SharePhase.Transferring, SharePhase.Importing -> Icons.Default.SwapVert
        SharePhase.Received, SharePhase.Done -> Icons.Default.CheckCircle
        is SharePhase.Failed -> Icons.Default.Error
    }
    Crossfade(targetState = icon, label = "icono de fase") {
        Icon(it, contentDescription = null, modifier = modifier.size(120.dp), tint = MaterialTheme.colorScheme.primary)
    }
}
