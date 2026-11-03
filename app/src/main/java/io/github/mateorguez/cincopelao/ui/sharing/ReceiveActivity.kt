package io.github.mateorguez.cincopelao.ui.sharing

import io.github.mateorguez.cincopelao.ui.theme.DelightRegular
import io.github.mateorguez.cincopelao.ui.theme.DelightMedium
import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.os.ParcelUuid
import android.provider.ContactsContract.Data
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import androidx.room.withTransaction
import dagger.hilt.android.AndroidEntryPoint
import io.github.mateorguez.cincopelao.MainActivity
import io.github.mateorguez.cincopelao.R
import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.prefs.saveScheduleState
import io.github.mateorguez.cincopelao.sharing.protocol.DataToSend
import io.github.mateorguez.cincopelao.ui.theme.CincoPelaoAppTheme
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@AndroidEntryPoint
class ReceiveActivity : ComponentActivity() {
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
                    Manifest.permission.ACCESS_FINE_LOCATION,
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

            val receiveViewModel: ReceiveViewModel = hiltViewModel()
            val connected by receiveViewModel.socketConnected.collectAsState()

            val deviceId = remember { mutableStateOf("") }
            val deviceName by receiveViewModel.deviceName.collectAsState()
            val SSID by receiveViewModel.SSID.collectAsState()
            val passphrase by receiveViewModel.passphrase.collectAsState()
            val messages by receiveViewModel.receivedMessages.collectAsState()
            val images by receiveViewModel.receivedImages.collectAsState()

            LaunchedEffect(Unit) {
                receiveViewModel.startAvailableMode()
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            val state by receiveViewModel.state.collectAsState()

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> {
                            if (state.advertisingStarted) {
                                receiveViewModel.startAdvertising(deviceName = (deviceName), passphrase = (passphrase ?: "No definida"), deviceId = deviceId.value)
                            }
                        }
                        Lifecycle.Event.ON_PAUSE -> receiveViewModel.stopAdvertising()
                        Lifecycle.Event.ON_DESTROY -> {
                            receiveViewModel.stopAdvertising()
                            receiveViewModel.stopAvailableMode()
                        }
                        else -> {}
                    }
                }

                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    receiveViewModel.stopAdvertising()
                    receiveViewModel.stopAvailableMode()
                }
            }

            CincoPelaoAppTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Recibir Set",
                                    fontFamily = DelightMedium
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                                actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Menú")
                                }
                            },
                        )
                    },
                    content = { paddingValues ->
                        Box(modifier = Modifier.padding(paddingValues)) {
                            ReceiveScreen(
                                connected = connected,
                                advertisingStarted = state.advertisingStarted,
                                passphrase = passphrase,
                                messages = messages,
                                images = images,
                                onStartAdvertising = { name ->
                                    deviceId.value = name
                                    receiveViewModel.startAdvertising(deviceName = (deviceName), passphrase = (passphrase ?: "No definida"), deviceId = deviceId.value)
                                },
                                onSend = { msg -> receiveViewModel.send(msg) },
                                onImport = receiveViewModel::importAsNewInstance,
                                onLastSteps = {
                                    val intent = Intent(context, MainActivity::class.java)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(intent)
                                    finish()
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
fun ReceiveScreen(
    connected: Boolean,
    advertisingStarted: Boolean,
    passphrase: String?,
    messages: List<String>,
    images: List<ByteArray>,
    onStartAdvertising: (name: String) -> Unit,
    onSend: (bytes: ByteArray) -> Unit,
    onImport: suspend (name: String, data: DataToSend) -> String,
    onLastSteps: () -> Unit,
    onFinish: () -> Unit
) {
    var showErrorDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1000)
        if (passphrase == null) {
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

    var inputName by remember { mutableStateOf("") }

    var bitmap: Bitmap? = null

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

    val context = LocalContext.current
    var showSaveDialog by remember { mutableStateOf(false) }
    var setName by remember { mutableStateOf("") }
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(text = "Guardar Set") },
            text = {
                Column {
                    Text("Este set se guardará en una nueva instancia de la base de datos de la aplicación, puedes intercambiar entre tus instancias desde el menú principal.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Introduce un nombre para este set de datos:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = setName,
                        onValueChange = { newValue ->
                            val filtered = newValue.filter { it.isLetterOrDigit() }
                            setName = filtered
                        },
                        placeholder = { Text("Nombre del set") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val data = Json.decodeFromString<DataToSend>(messages[0])

                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val newInstanceId = onImport(setName, data)

                                if (bitmap != null) {
                                    val directory = File(context.filesDir, "SCHEDULE_$newInstanceId")
                                    if (!directory.exists()) directory.mkdirs()
                                    val file = File(directory, "$newInstanceId.png")
                                    FileOutputStream(file).use { out ->
                                        bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, out)
                                    }
                                    val fileUri = Uri.fromFile(file)
                                    saveScheduleState(
                                        context = context,
                                        instanceId = newInstanceId,
                                        fileUriString = fileUri.toString(),
                                        zoom = 1f,
                                        offset = Offset(0f, 0f)
                                    )
                                }

                                withContext(Dispatchers.Main) {
                                    showSaveDialog = false
                                    onLastSteps()
                                }
                            } catch (e: Exception) {
                                // TODO(Fase 6): avisar al usuario de que la importación ha fallado.
                                Log.e("ReceiveScreen", "No se pudo importar el set recibido", e)
                            }
                        }
                    }

                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                Button(onClick = { showSaveDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
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
                advertisingStarted -> Icons.AutoMirrored.Filled.BluetoothSearching
                else -> Icons.Default.Bluetooth
            },
            contentDescription = if (connected) "Conectado" else "Esperando",
            modifier = Modifier.size(120.dp),
            tint = when {
                connected -> MaterialTheme.colorScheme.secondary
                advertisingStarted -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        Text(
            text = when {
                showCheck -> "Dispositivos conectados correctamente"
                connected -> "Canal operativo"
                advertisingStarted -> "Advirtiendo..."
                else -> "Pulsa iniciar para advertir"
            },
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            fontFamily = DelightRegular
        )

        if (messages.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 16.dp, horizontal = 8.dp)
            ) {
                items(messages) { msg ->

                    val data = Json.decodeFromString<DataToSend>(msg)

                    val subjects = data.subjects
                    val events = data.events
                    val tasks = data.tasks

                    bitmap = if (images.isNotEmpty()) {
                        byteArrayToBitmap(images.first())
                    } else null

                    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

                    if (data != null) {
                        // Título general
                        Text(
                            "Datos Recibidos",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(8.dp))

                        // --- Asignaturas ---
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    "Asignaturas",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(8.dp))

                                subjects.forEach { subject ->
                                    Text("• ${subject.name} (${subject.course})")
                                    val subjectEvents = events.filter { it.subjectId == subject.subjectId }
                                    subjectEvents.forEach { event ->
                                        Text(
                                            "   - ${event.name} (${event.type}) en ${dateFormat.format(Date(event.date))}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                }
                            }
                        }

                        // --- Tareas ---
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    "Tareas",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(8.dp))

                                tasks.forEach { task ->
                                    val status = if (task.isCompleted) "✔️" else "⏳"
                                    val due = task.dueDate?.let { dateFormat.format(Date(it)) } ?: "Sin fecha"
                                    Text("• $status ${task.title} (vence: $due)", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        if (bitmap != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Image(
                                    bitmap = bitmap!!.asImageBitmap(),
                                    contentDescription = "Horario recibido",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        val saveButtonClicked = remember { mutableStateOf(false) }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    showSaveDialog = true
                                    saveButtonClicked.value = true
                                    },
                                modifier = Modifier.padding(top = 16.dp).align(Alignment.CenterHorizontally),
                                enabled = !saveButtonClicked.value
                            ) {
                                Text("Guardar Set")
                            }
                        }

                    }

                }
            }
        } else if (!connected) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 16.dp)) {
                Text(
                    text = "Nombre de dispositivo:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                OutlinedTextField(
                    value = inputName,
                    onValueChange = { newValue ->
                        inputName = newValue.take(15)
                    },
                    placeholder = { Text("Máx. 10 caracteres") },
                    singleLine = true,
                    enabled = !advertisingStarted
                )

                Text(
                    text = "Passphrase: " + (passphrase ?: "No definida"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Button(
                onClick = { onStartAdvertising(inputName) },
                modifier = Modifier.padding(top = 16.dp),
                enabled = inputName.isNotBlank() && !advertisingStarted
            ) {
                Text("Iniciar Advertising")
            }
        } else {
            val options = listOf("Asignaturas y Eventos", "Tareas", "Horarios")
            val selectedItems = remember { mutableStateListOf<String>() }
            val sendListClicked = remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text("Selecciona lo que quieres recibir:", style = MaterialTheme.typography.titleMedium)

                Spacer(Modifier.height(12.dp))

                options.forEach { option ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 5.dp)
                            .clickable {
                                if (!sendListClicked.value) {
                                    if (selectedItems.contains(option)) {
                                        selectedItems.remove(option)
                                    } else {
                                        selectedItems.add(option)
                                    }
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedItems.contains(option))
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Checkbox(
                                checked = selectedItems.contains(option),
                                enabled = !sendListClicked.value,
                                onCheckedChange = { checked ->
                                    if (checked) selectedItems.add(option) else selectedItems.remove(option)
                                }
                            )
                            Text(
                                option,
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            val message = selectedItems.joinToString(",")
                            val bytes = message.toByteArray(Charsets.UTF_8)
                            val packet = ByteBuffer.allocate(4 + bytes.size)
                                .putInt(bytes.size)  // tamaño del mensaje
                                .put(bytes)
                                .array()
                            onSend(packet)
                            sendListClicked.value = true
                            Log.d("ChecklistScreen", "Enviado: $message")
                        },
                        enabled = selectedItems.isNotEmpty() && !sendListClicked.value
                    ) {
                        Text(if (!sendListClicked.value) "Solicitar selección" else "Esperando respuesta")
                    }
                }
            }
        }
    }
}

fun byteArrayToBitmap(bytes: ByteArray): Bitmap {
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}