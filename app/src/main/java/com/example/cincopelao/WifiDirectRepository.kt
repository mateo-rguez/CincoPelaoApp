package com.example.cincopelao

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.MacAddress
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

interface WifiDirectRepository {
    fun getDeviceName(onResult: (String) -> Unit)
    val groupPassphrase: StateFlow<String?>
    val SSID: StateFlow<String?>
    val deviceName: StateFlow<String?>
    fun startGroupOwnerWithPassword(onStarted: (Boolean) -> Unit)
    fun discoverPeers(onPeersAvailable: (Collection<WifiP2pDevice>) -> Unit)
    fun connectToPeer(deviceAddress: String, passphrase: String, onConnected: (Boolean) -> Unit)
    fun disconnect(onDone: () -> Unit)
    // ---
    fun requestConnectionInfo(onInfo: (android.net.wifi.p2p.WifiP2pInfo?) -> Unit)
    val socketConnected: StateFlow<Boolean>
    val receivedData: StateFlow<List<String>>
    val receivedImages: StateFlow<List<ByteArray>>
    fun startServerSocket(port: Int = 8988)
    fun connectToServerSocket(port: Int = 8988, timeoutMs: Int = 5000)
    fun stopSocket()
    fun send(bytes: ByteArray)
}

@Singleton
class WifiDirectRepositoryImpl @Inject constructor(
    private val context: Context
): WifiDirectRepository {

    // Conexión WiFi Direct P2P --------------------------------------------------------------------

    private val _groupPassphrase = MutableStateFlow<String?>(null)
    override val groupPassphrase: StateFlow<String?> get() = _groupPassphrase

    private val _SSID = MutableStateFlow<String?>(null)
    override val SSID: StateFlow<String?> get() = _SSID

    private val _deviceName = MutableStateFlow<String?>(null)
    override val deviceName: StateFlow<String?> get() = _deviceName

    private val wifiP2pManager =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private val channel = wifiP2pManager.initialize(context, context.mainLooper, null)

    // Obtener nombre del dispositivo
    override fun getDeviceName(onResult: (String) -> Unit) {
        wifiP2pManager.requestDeviceInfo(channel) { device ->
            onResult(device?.deviceName ?: "Unknown")
            _deviceName.value = device?.deviceName ?: "Error (reintentar)"
        }
    }

    @SuppressLint("MissingPermission")
    override fun startGroupOwnerWithPassword(onStarted: (Boolean) -> Unit) {
        wifiP2pManager.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // Espera unos ms antes de pedir la info del grupo
                Handler(Looper.getMainLooper()).postDelayed({
                    wifiP2pManager.requestGroupInfo(channel) { group ->
                        if (group != null) {
                            _groupPassphrase.value = group.passphrase
                            _SSID.value = group.networkName
                            Toast.makeText(context, "WiFi P2P Listo PW: ${_groupPassphrase.value}", Toast.LENGTH_SHORT).show()
                            Log.d("WifiDirectRepo", "Grupo creado: SSID=${group.networkName}, Pass=${group.passphrase}")
                        } else {
                            Log.e("WifiDirectRepo", "requestGroupInfo devolvió null (no hay grupo)")
                        }
                    }
                }, 500) // 500ms suele ser suficiente
                onStarted(true)
            }

            override fun onFailure(reason: Int) {
                Toast.makeText(context, "WiFi P2P Error: $reason", Toast.LENGTH_SHORT).show()
                onStarted(false)
            }
        })
    }


    // Escanear dispositivos cercanos
    @SuppressLint("MissingPermission")
    override fun discoverPeers(onPeersAvailable: (Collection<WifiP2pDevice>) -> Unit) {
        wifiP2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                wifiP2pManager.requestPeers(channel) { peers ->
                    onPeersAvailable(peers.deviceList)
                }
            }

            override fun onFailure(reason: Int) {
                onPeersAvailable(emptyList())
            }
        })
    }

    @SuppressLint("MissingPermission")
    override fun connectToPeer(deviceAddress: String, passphrase: String, onConnected: (Boolean) -> Unit) {
        val config = WifiP2pConfig.Builder()
            .setDeviceAddress(MacAddress.fromString(deviceAddress))
            .build()

        Log.d("WifiDirectRepo", "Intentando conectar a $deviceAddress")

        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION) {
                    val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                    if (networkInfo?.isConnected == true) {
                        // Ya estamos conectados, pedimos la info del grupo
                        wifiP2pManager.requestConnectionInfo(channel) { info ->
                            if (info.groupFormed && info.groupOwnerAddress != null) {
                                // Host listo, abrir socket
                                Log.d("WifiDirectRepo", "Grupo P2P formado. Host: ${info.groupOwnerAddress.hostAddress}")
                                connectToServerSocket(8988)
                                _deviceName.value = deviceName.value // opcional, actualizar estado
                                onConnected(true)
                                context?.unregisterReceiver(this) // ya no necesitamos el receiver
                            }
                        }
                    }
                }
            }
        }

        context.registerReceiver(receiver, intentFilter)

        // Iniciamos la conexión al peer
        wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("WifiDirectRepo", "Conexión iniciada correctamente con $deviceAddress")
            }

            override fun onFailure(reason: Int) {
                val message = when (reason) {
                    WifiP2pManager.P2P_UNSUPPORTED -> "Wi-Fi Direct no soportado"
                    WifiP2pManager.BUSY -> "El sistema está ocupado"
                    WifiP2pManager.ERROR -> "Error interno"
                    else -> "Fallo desconocido ($reason)"
                }
                Log.e("WifiDirectRepo", "Error conectando: $message")
                try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
                onConnected(false)
            }
        })
    }

    // Cancelar conexión o grupo
    override fun disconnect(onDone: () -> Unit) {
        wifiP2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                onDone()
            }

            override fun onFailure(reason: Int) {
                onDone()
            }
        })
    }

    // Gestión de Socket TCP -----------------------------------------------------------------------

    private val _receivedData = MutableStateFlow<List<String>>(emptyList())
    override val receivedData: StateFlow<List<String>> = _receivedData

    private val _receivedImages = MutableStateFlow<List<ByteArray>>(emptyList())
    override val receivedImages: StateFlow<List<ByteArray>> = _receivedImages

    override fun requestConnectionInfo(onInfo: (android.net.wifi.p2p.WifiP2pInfo?) -> Unit) {
        wifiP2pManager.requestConnectionInfo(channel) { info -> onInfo(info) }
    }

    private val _socketConnected = MutableStateFlow(false)
    override val socketConnected: StateFlow<Boolean> = _socketConnected.asStateFlow()

    private var serverJob: kotlinx.coroutines.Job? = null
    private var clientJob: kotlinx.coroutines.Job? = null
    private var serverSocket: java.net.ServerSocket? = null
    private var activeSocket: java.net.Socket? = null

    override fun startServerSocket(port: Int) {
        if (serverJob != null) return
        serverJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                serverSocket = java.net.ServerSocket(port)
                android.util.Log.d("WifiDirectRepo", "SOCKET MONDATO")
                val socket = serverSocket!!.accept()
                activeSocket = socket
                _socketConnected.value = true
                readLoop(socket)
            } catch (e: Exception) {
                android.util.Log.e("WifiDirectRepo", "Server socket error", e)
                _socketConnected.value = false
            }
        }
    }

    override fun connectToServerSocket(port: Int, timeoutMs: Int) {
        Log.d("WifiDirectRepo", "Intentando montar socket")
        clientJob?.cancel()
        clientJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val host = suspendCancellableCoroutine<String?> { cont ->
                    // Request connection info y espera a que el grupo P2P esté formado
                    val listener = WifiP2pManager.ConnectionInfoListener { info ->
                        if (info.groupFormed) {
                            val hostAddress = info.groupOwnerAddress?.hostAddress
                            if (!hostAddress.isNullOrBlank()) {
                                Log.d("WifiDirectRepo", "Grupo P2P formado. Host: $hostAddress")
                                cont.resume(hostAddress) {}
                            } else {
                                Log.d("WifiDirectRepo", "Grupo formado, pero hostAddress aún null")
                            }
                        } else {
                            Log.d("WifiDirectRepo", "Grupo P2P no formado todavía")
                        }
                    }

                    wifiP2pManager.requestConnectionInfo(channel, listener)
                }

                if (host.isNullOrBlank()) {
                    Log.e("WifiDirectRepo", "No se pudo obtener host del GO")
                    _socketConnected.value = false
                    return@launch
                }

                Log.d("WifiDirectRepo", "Conectando socket a $host:$port")
                val socket = Socket()
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                activeSocket = socket
                _socketConnected.value = true
                Log.d("WifiDirectRepo", "SOCKET CONECTADO a $host:$port")

                readLoop(socket) // tu función de lectura

            } catch (e: Exception) {
                Log.e("WifiDirectRepo", "Error al conectar socket", e)
                _socketConnected.value = false
            }
        }
    }

    private fun readLoop(socket: java.net.Socket) {
        try {
            val input = DataInputStream(BufferedInputStream(socket.getInputStream()))
            while (!socket.isClosed) {
                // Leer primeros 4 bytes: tamaño del paquete
                val totalLength = try { input.readInt() } catch (e: Exception) { break }
                if (totalLength <= 0) continue

                // Leer to el paquete
                val packet = ByteArray(totalLength)
                input.readFully(packet)

                // Separar header JSON, siguientes 4 bytes
                val jsonLength = ByteBuffer.wrap(packet.copyOfRange(0, 4)).int

                // Extraer JSON
                val jsonBytes = packet.copyOfRange(4, 4 + jsonLength)
                val jsonString = String(jsonBytes, Charsets.UTF_8)

                // Extraer imagen, si hay
                val imageBytes = packet.copyOfRange(4 + jsonLength, packet.size)

                // Guardar mensaje
                _receivedData.value = _receivedData.value + jsonString

                // Guardar imagen en bytes
                _receivedImages.value = _receivedImages.value + imageBytes

            }
        } catch (e: Exception) {
            android.util.Log.e("WifiDirectRepo", "Error en readLoop", e)
        }
    }

    override fun send(bytes: ByteArray) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val out = activeSocket?.getOutputStream() ?: return@launch
                val header = ByteBuffer.allocate(4).putInt(bytes.size).array()
                out.write(header)
                out.write(bytes)
                out.flush()
            } catch (e: Exception) {
                android.util.Log.e("WifiDirectRepo", "Send error", e)
                _socketConnected.value = false
            }
        }
    }

    override fun stopSocket() {
        try { activeSocket?.close() } catch (_: Exception) {}
        try { serverSocket?.close() } catch (_: Exception) {}
        activeSocket = null
        serverSocket = null
        serverJob?.cancel(); serverJob = null
        clientJob?.cancel(); clientJob = null
        _socketConnected.value = false
    }
}