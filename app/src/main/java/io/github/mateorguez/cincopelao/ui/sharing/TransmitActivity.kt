package io.github.mateorguez.cincopelao.ui.sharing

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dagger.hilt.android.AndroidEntryPoint
import io.github.mateorguez.cincopelao.R
import io.github.mateorguez.cincopelao.data.prefs.getDarkModeFlow
import io.github.mateorguez.cincopelao.data.prefs.getScheduleState
import io.github.mateorguez.cincopelao.sharing.protocol.DataToSend
import io.github.mateorguez.cincopelao.ui.MainViewModel
import io.github.mateorguez.cincopelao.ui.schedule.loadImageAsBitmap
import io.github.mateorguez.cincopelao.ui.schedule.loadPdfAsBitmap
import io.github.mateorguez.cincopelao.ui.theme.CincoPelaoTheme
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@AndroidEntryPoint
class TransmitActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.NEARBY_WIFI_DEVICES,
                    Manifest.permission.INTERNET
                ),
                1
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                1
            )
        }

        setContent {

            val context = LocalContext.current
            val isDarkTheme by context.getDarkModeFlow().collectAsState(initial = false)

            val transmitViewModel: TransmitViewModel = hiltViewModel()
            val connected by transmitViewModel.socketConnected.collectAsState()
            val messages by transmitViewModel.receivedMessages.collectAsState()

            val mainViewModel: MainViewModel = hiltViewModel()

            val lifecycleOwner = LocalLifecycleOwner.current
            val state by transmitViewModel.state.collectAsState()

            val instanceId by mainViewModel.currentInstanceId.collectAsState()

            val allSubjects = mainViewModel.allSubjects.collectAsState(initial = emptyList())
            val allEventsRaw = mainViewModel.allEvents.collectAsState(initial = emptyList())
            val allEvents = allEventsRaw.value.map { event ->
                if (event.score != null) {
                    event.copy(score = null)
                } else if (event.noPuntuableDone) {
                    event.copy(noPuntuableDone = false)
                } else {
                    event
                }
            }
            val allTasks = mainViewModel.allTasks.collectAsState(initial = emptyList())
            var bitmap by remember { mutableStateOf<Bitmap?>(null) }
            LaunchedEffect(instanceId) {
                getScheduleState(context, instanceId).firstOrNull()?.let { (uriString, _, _) ->
                    uriString?.let {
                        val uri = Uri.parse(uriString)
                        val mimeType = context.contentResolver.getType(uri) ?: ""
                        val isPdf = mimeType == "application/pdf"

                        bitmap = if (isPdf) {
                            loadPdfAsBitmap(context, uri)
                        } else {
                            loadImageAsBitmap(context, uri)
                        }
                    }
                }
            }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> transmitViewModel.startScanning()
                        Lifecycle.Event.ON_PAUSE -> transmitViewModel.stopScanning()
                        Lifecycle.Event.ON_DESTROY -> transmitViewModel.stopScanning()
                        else -> {}
                    }
                }

                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    transmitViewModel.stopScanning()
                }
            }

            CincoPelaoTheme(darkTheme = isDarkTheme) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(text = "Transmitir Set", fontFamily = FontFamily(Font(R.font.delight_medium))) },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                                actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            navigationIcon = {
                                IconButton(onClick = { finish() } ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Menú")
                                }
                            },
                        )
                    },
                    content = { paddingValues ->
                        Box(modifier = Modifier.padding(paddingValues)) {
                            TransmitScreen(
                                connected = connected,
                                scanning = state.scanning,
                                devices = state.devices,
                                messages = messages,
                                onDeviceClick = { deviceName, passphrase ->
                                    transmitViewModel.connectToDevice(deviceName, passphrase)
                                },
                                onSendInfo = { subjectsEvents, tasks, schedule ->
                                    val data = DataToSend(
                                        subjects = if (subjectsEvents) allSubjects.value else emptyList(),
                                        events = if (subjectsEvents) allEvents else emptyList(),
                                        tasks = if (tasks) allTasks.value else emptyList()
                                    )
                                    val jsonString = Json.encodeToString(DataToSend.serializer(), data)
                                    val jsonBytes = jsonString.toByteArray()
                                    val scheduleBytes = if (schedule && bitmap != null) bitmapToByteArray(bitmap!!) else ByteArray(0)
                                    val header = ByteBuffer.allocate(4).putInt(jsonBytes.size).array()
                                    val packet = header + jsonBytes + scheduleBytes
                                    transmitViewModel.send(packet)
                                },
                                onFinish = { finish() }
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TransmitScreen(
    connected: Boolean,
    scanning: Boolean,
    devices: List<DeviceInfo>,
    messages: List<String>,
    onDeviceClick: (String, String) -> Unit,
    onSendInfo: (Boolean, Boolean, Boolean) -> Unit,
    onFinish: () -> Unit
) {

    var showErrorDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1000)
        if (!scanning) {
            showErrorDialog = true
        }
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = {
                Text(text = "Ups...")
            },
            text = {
                Column {
                    Text("Parece que los componentes no se han inicializado correctamente. Por favor, para usra esta función revise las siguientes instrucciones:")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• Conceder los permisos requeridos")
                    Text("• Activar el WiFi")
                    Text("• Activar el Bluetooth")
                    Text("• Dejar pasar unos segundos después y reintentar")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Si tras seguir estas instrucciones el error persiste, simplemente cierra la aplicación por completo y vuelve a intentar")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showErrorDialog = false
                        onFinish()
                    }
                ) {
                    Text("Aceptar")
                }
            }
        )
    }

    var showCheck by remember { mutableStateOf(false) }
    LaunchedEffect(connected) {
        if (connected) {
            showCheck = true
            kotlinx.coroutines.delay(2000)
            showCheck = false
        } else {
            showCheck = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Icon(
            imageVector = when {
                showCheck -> Icons.Default.CheckCircleOutline
                connected -> Icons.Default.Wifi
                else -> Icons.AutoMirrored.Filled.BluetoothSearching
            },
            contentDescription = "Escaneando",
            modifier = Modifier.size(120.dp),
            tint = when {
                connected -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.primary
            }
        )

        Text(
            text = when {
                showCheck -> "Dispositivos conectados correctamente"
                connected -> "Canal operativo"
                scanning -> "Escaneando dispositivos en modo recepción..."
                else -> "Escaneo detenido"
            },
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily(Font(R.font.delight_regular))
        )

        if (devices.isEmpty()) {
            Text(
                text = "No se han encontrado dispositivos aún...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = FontFamily(Font(R.font.delight_regular))
            )
        } else {
            if (!connected) {
                LazyColumn {
                    items(devices) { device ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text("${device.deviceId}", style = MaterialTheme.typography.titleLarge.copy(fontFamily =
                                    FontFamily(Font(R.font.delight_medium)
                                    )))
                                Spacer(modifier = Modifier.height(5.dp))
                                Text("Device: ${device.deviceName}", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Passphrase: ${device.passphrase}", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("MAC BLE: ${device.macBle}", style = MaterialTheme.typography.bodyMedium)

                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { onDeviceClick(device.deviceName, device.passphrase) },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Conectar")
                                }
                            }
                        }
                    }
                }
            } else {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Elementos solicitados",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(16.dp))

                    val firstMessage = messages.firstOrNull()
                    val items = firstMessage?.split(",")?.map { it.trim() } ?: emptyList()

                    items.forEach { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (item) {
                                        "Asignaturas y Eventos" -> Icons.Default.Event
                                        "Tareas" -> Icons.Default.Task
                                        "Horarios" -> Icons.Default.Schedule
                                        else -> Icons.Default.Info
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    item,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }

                    val onSendClicked = remember { mutableStateOf(false) }

                    if (items.isNotEmpty()) {
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = {
                                val subjectsEvents = items.contains("Asignaturas y Eventos")
                                val tasks = items.contains("Tareas")
                                val schedules = items.contains("Horarios")

                                onSendInfo(subjectsEvents, tasks, schedules)
                                onSendClicked.value = true
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            enabled = !onSendClicked.value,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Enviar información")
                        }
                    }

                    if (onSendClicked.value) {
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { onFinish() },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Terminar")
                        }
                    }
                }

            }
        }
    }
}

fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}