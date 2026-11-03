package com.example.cincopelao

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class TransmitViewModel @Inject constructor(
    private val bleRepository: BleGattRepository,
    private val wifiDirectRepository: WifiDirectRepository
) : ViewModel() {

    // Bluetooth Low Energy ------------------------------------------------------------------------

    private val _state = MutableStateFlow(TransmitState())
    val state: StateFlow<TransmitState> = _state.asStateFlow()

    private var scanJob: Job? = null

    fun startScanning() {
        _state.update { it.copy(scanning = true) }

        bleRepository.startScanning(
            onDataReceived = { deviceName, pass, id, macBle ->
                val now = System.currentTimeMillis()
                _state.update { current ->
                    val updated = current.devices.toMutableList()
                    val idx = updated.indexOfFirst { it.deviceId == id }
                    if (idx >= 0) {
                        updated[idx] = updated[idx].copy(deviceName = deviceName, passphrase = pass, macBle = macBle, lastSeen = now)
                    } else {
                        updated.add(DeviceInfo(deviceName, pass, id, macBle, now))
                    }
                    current.copy(devices = updated)
                }
            },
            onDeviceSeen = { macBle ->
                val now = System.currentTimeMillis()
                _state.update { current ->
                    val updated = current.devices.toMutableList()
                    val idx = updated.indexOfFirst { it.macBle == macBle }
                    if (idx >= 0) {
                        // 🔹 Solo refrescamos el lastSeen, sin tocar los demás datos
                        updated[idx] = updated[idx].copy(lastSeen = now)
                    }
                    current.copy(devices = updated)
                }
            },
            onDeviceDisconnected = { deviceId ->
                _state.update { current ->
                    current.copy(devices = current.devices.filter { it.deviceId != deviceId })
                }
            }
        )

        val deviceTimeout = 2000L
        scanJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val now = System.currentTimeMillis()
                _state.update { current ->
                    current.copy(devices = current.devices.filter { now - it.lastSeen < deviceTimeout })
                }
            }
        }
    }

    fun stopScanning() {
        bleRepository.stop()
        _state.update { it.copy(scanning = false) }
        scanJob?.cancel()
    }

    // WiFi Direct ---------------------------------------------------------------------------------

    private val _connectionStatus = MutableStateFlow("Idle")
    val connectionStatus: StateFlow<String> = _connectionStatus

    val socketConnected: StateFlow<Boolean> = wifiDirectRepository.socketConnected
    val receivedMessages: StateFlow<List<String>> = wifiDirectRepository.receivedData

    fun connectToDevice(ssid: String, passphrase: String) {

        val maxAttempts = 5
        val delayMs = 2000L

        _connectionStatus.value = "Iniciando búsqueda de peers..."
        Log.d("WiFiDirect", "Iniciando búsqueda de peers...")

        var attempt = 0

        fun tryDiscover() {
            attempt++
            _connectionStatus.value = "Buscando peers (intento $attempt/$maxAttempts)..."
            Log.d("WiFiDirect", "Intento $attempt de $maxAttempts: buscando peers...")

            wifiDirectRepository.discoverPeers { peers ->
                if (peers.isEmpty()) {
                    _connectionStatus.value = "No se encontraron peers en este intento."
                    Log.d("WiFiDirect", "No se encontraron peers en este intento.")
                } else {
                    _connectionStatus.value = "Peers encontrados: ${peers.joinToString { it.deviceName }}"
                    Log.d("WiFiDirect", "Peers encontrados: ${peers.joinToString { it.deviceName }}")
                }

                val targetPeer = peers.firstOrNull { it.deviceName.startsWith(ssid) }
                if (targetPeer != null) {
                    _connectionStatus.value = "Intentando conectar a ${targetPeer.deviceName}..."
                    Log.d("WiFiDirect", "Intentando conectar a ${targetPeer.deviceName} con address ${targetPeer.deviceAddress}...")
                    wifiDirectRepository.connectToPeer(targetPeer.deviceAddress, passphrase) { success ->
                        _connectionStatus.value =
                            if (success) {
                                Log.d("WiFiDirect", "Conexión exitosa a ${targetPeer.deviceName}")
                                "Conectado a ${targetPeer.deviceName}"
                            } else {
                                Log.d("WiFiDirect", "Error conectando a ${targetPeer.deviceName}")
                                "Error conectando a ${targetPeer.deviceName}"
                            }
                    }
                } else {
                    if (attempt < maxAttempts) {
                        _connectionStatus.value = "Peer no encontrado, reintentando en ${delayMs / 1000} segundos..."
                        Log.d("WiFiDirect", "Peer no encontrado. Reintentando en ${delayMs / 1000} segundos...")
                        Handler(Looper.getMainLooper()).postDelayed({ tryDiscover() }, delayMs)
                    } else {
                        _connectionStatus.value = "No se encontró un peer que comience con: $ssid después de $maxAttempts intentos."
                        Log.d("WiFiDirect", "No se encontró un peer que comience con: $ssid después de $maxAttempts intentos.")
                    }
                }
            }
        }

        tryDiscover()
    }

    fun send(bytes: ByteArray) {
        wifiDirectRepository.send(bytes)
    }

    fun stopConnection() {
        wifiDirectRepository.disconnect {
            _connectionStatus.value = "Conexión detenida"
        }
    }
}

data class TransmitState(
    val scanning: Boolean = false,
    val devices: List<DeviceInfo> = emptyList()
)

data class DeviceInfo(
    val deviceName: String,
    val passphrase: String,
    val deviceId: String,
    val macBle: String,
    val lastSeen: Long,
)
