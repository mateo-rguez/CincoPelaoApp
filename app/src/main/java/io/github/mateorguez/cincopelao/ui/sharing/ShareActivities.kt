package io.github.mateorguez.cincopelao.ui.sharing

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import io.github.mateorguez.cincopelao.sharing.session.SetupIssue
import io.github.mateorguez.cincopelao.ui.theme.CincoPelaoAppTheme
import io.github.mateorguez.cincopelao.ui.theme.DelightMedium

/** Ajustes del sistema a los que se puede mandar al usuario para resolver un [SetupIssue]. */
enum class SettingsTarget {
    APP, BLUETOOTH, WIFI, LOCATION;

    companion object {
        fun of(issue: SetupIssue): SettingsTarget = when (issue) {
            SetupIssue.BLUETOOTH_OFF -> BLUETOOTH
            SetupIssue.WIFI_OFF -> WIFI
            SetupIssue.LOCATION_OFF -> LOCATION
            SetupIssue.PERMISSIONS, SetupIssue.UNSUPPORTED -> APP
        }
    }
}

private fun ComponentActivity.openSettings(target: SettingsTarget) {
    val intent = when (target) {
        SettingsTarget.APP -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        SettingsTarget.BLUETOOTH -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        SettingsTarget.WIFI -> Intent(Settings.Panel.ACTION_WIFI)
        SettingsTarget.LOCATION -> Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    }
    runCatching { startActivity(intent) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    CincoPelaoAppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = title, fontFamily = DelightMedium) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) { content() }
        }
    }
}

/** Enviar datos a otro móvil. Al cerrarla, el ViewModel corta la sesión y deja el grupo Wi‑Fi Direct. */
@AndroidEntryPoint
class TransmitActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShareScaffold(title = "Transmitir", onBack = ::finish) {
                TransmitScreen(onFinish = ::finish, onOpenSettings = ::openSettings)
            }
        }
    }
}

/** Recibir datos de otro móvil y guardarlos como una instancia nueva. */
@AndroidEntryPoint
class ReceiveActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShareScaffold(title = "Recibir", onBack = ::finish) {
                ReceiveScreen(onFinish = ::finish, onOpenSettings = ::openSettings)
            }
        }
    }
}
