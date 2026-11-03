package io.github.mateorguez.cincopelao.ui.schedule

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mateorguez.cincopelao.R
import kotlinx.coroutines.launch

/** Pestaña de horario: muestra la imagen o PDF importado, con zoom; si no hay, invita a importarlo. */
@Composable
fun ScheduleScreen(viewModel: ScheduleViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::import)
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }

    Box(Modifier.fillMaxSize()) {
        when (val s = state) {
            ScheduleUiState.Loading -> Unit
            ScheduleUiState.Empty -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(20.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.schedule),
                        contentDescription = "Logo de horario",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(300.dp).clipToBounds()
                    )
                    Text(
                        "Aún no has importado tu horario",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Puedes importar una imagen o PDF con tu horario para visualizarlo aquí.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = {
                        launcher.launch(arrayOf("image/*", "application/pdf"))
                    }) {
                        Text("Importar Horario")
                    }
                }
            }
            is ScheduleUiState.Loaded -> {
                // key: con cada archivo nuevo, la vista empieza con su propio zoom
                key(s.version) {
                    ZoomableImage(
                        bitmap = s.bitmap,
                        initialZoom = s.zoom,
                        initialOffset = s.offset,
                        onTransformChange = viewModel::onTransformChange
                    )
                }

                FloatingActionButton(
                    onClick = { launcher.launch(arrayOf("image/*", "application/pdf")) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Cambiar archivo")
                }
            }
        }
    }
}
