package io.github.mateorguez.cincopelao.ui.sharing

import io.github.mateorguez.cincopelao.data.instances.InstanceRepository
import io.github.mateorguez.cincopelao.sharing.protocol.DataToSend
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mateorguez.cincopelao.sharing.ble.BleGattRepository
import io.github.mateorguez.cincopelao.sharing.wifidirect.WifiDirectRepository
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ReceiveViewModel @Inject constructor(
    private val bleRepository: BleGattRepository,
    private val wifiDirectRepository: WifiDirectRepository,
    private val instanceRepository: InstanceRepository
) : ViewModel() {

    /** Guarda los datos recibidos como una instancia nueva y la activa. Devuelve su id. */
    suspend fun importAsNewInstance(name: String, data: DataToSend): String =
        instanceRepository.importAsNewInstance(name, data)

    init {
        wifiDirectRepository.getDeviceName { name ->
            _deviceName.value = name
        }
    }

    // Bluetooth Low Energy ------------------------------------------------------------------------

    private val _state = MutableStateFlow(ReceiveState())
    val state: StateFlow<ReceiveState> = _state

    fun startAdvertising(deviceName: String, passphrase: String, deviceId: String) {
        _state.update { it.copy(advertisingStarted = true) }
        viewModelScope.launch {
            bleRepository.startServer(
                deviceName = deviceName,
                passphrase = passphrase,
                deviceId = deviceId
            )
        }
    }

    fun stopAdvertising() {
        _state.update { it.copy(advertisingStarted = false) }
        viewModelScope.launch {
            bleRepository.stopServer()
        }
    }

    // WiFi Direct ---------------------------------------------------------------------------------

    private val _deviceName = MutableStateFlow("Unknown")
    val deviceName: StateFlow<String> = _deviceName

    private val _isGroupOwnerStarted = MutableStateFlow(false)
    val isGroupOwnerStarted: StateFlow<Boolean> = _isGroupOwnerStarted

    private val _connectionStatus = MutableStateFlow("Idle")
    val connectionStatus: StateFlow<String> = _connectionStatus

    val socketConnected: StateFlow<Boolean> = wifiDirectRepository.socketConnected
    val passphrase: StateFlow<String?> = wifiDirectRepository.groupPassphrase
    val SSID: StateFlow<String?> = wifiDirectRepository.SSID
    val receivedMessages: StateFlow<List<String>> = wifiDirectRepository.receivedData
    val receivedImages: StateFlow<List<ByteArray>> = wifiDirectRepository.receivedImages

    fun startAvailableMode() {
        wifiDirectRepository.startGroupOwnerWithPassword() { success ->
            _isGroupOwnerStarted.value = success
            if (success) {
                wifiDirectRepository.startServerSocket(port = 8988)
                _connectionStatus.value = "Esperando conexiones..."
            } else {
                _connectionStatus.value = "Error iniciando modo disponible"
            }
        }
    }

    fun send(bytes: ByteArray) {
        wifiDirectRepository.send(bytes)
    }

    fun stopAvailableMode() {
        wifiDirectRepository.stopSocket()
        wifiDirectRepository.disconnect {
            _connectionStatus.value = "Modo disponible detenido"
        }
    }
}

data class ReceiveState(
    val connected: Boolean = false,
    val advertisingStarted: Boolean = false
)