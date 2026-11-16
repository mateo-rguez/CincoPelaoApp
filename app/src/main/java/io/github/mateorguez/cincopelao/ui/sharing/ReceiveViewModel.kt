package io.github.mateorguez.cincopelao.ui.sharing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mateorguez.cincopelao.data.instances.InstanceRepository
import io.github.mateorguez.cincopelao.sharing.ShareEnvironment
import io.github.mateorguez.cincopelao.sharing.ShareScheduleStore
import io.github.mateorguez.cincopelao.sharing.ble.AdvertiseEvent
import io.github.mateorguez.cincopelao.sharing.ble.BleTransport
import io.github.mateorguez.cincopelao.sharing.protocol.BleAdvert
import io.github.mateorguez.cincopelao.sharing.protocol.GroupCredentials
import io.github.mateorguez.cincopelao.sharing.protocol.PayloadSummary
import io.github.mateorguez.cincopelao.sharing.protocol.ReceiverBeacon
import io.github.mateorguez.cincopelao.sharing.protocol.ShareConnection
import io.github.mateorguez.cincopelao.sharing.protocol.ShareProtocol
import io.github.mateorguez.cincopelao.sharing.protocol.ShareSelection
import io.github.mateorguez.cincopelao.sharing.protocol.normalized
import io.github.mateorguez.cincopelao.sharing.protocol.summary
import io.github.mateorguez.cincopelao.sharing.session.Peer
import io.github.mateorguez.cincopelao.sharing.session.ReceivedPayload
import io.github.mateorguez.cincopelao.sharing.session.ReceiverSession
import io.github.mateorguez.cincopelao.sharing.session.SharePhase
import io.github.mateorguez.cincopelao.sharing.session.ShareRole
import io.github.mateorguez.cincopelao.sharing.session.TransferReport
import io.github.mateorguez.cincopelao.sharing.session.progressOf
import io.github.mateorguez.cincopelao.sharing.wifidirect.WifiDirectTransport
import java.net.ServerSocket
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReceiveUiState(
    val phase: SharePhase = SharePhase.Idle,
    /** Nombre con el que se anuncia este móvil. */
    val displayName: String = "",
    val peer: Peer? = null,
    val selection: ShareSelection = ShareSelection(subjectsAndEvents = true, subjectTasks = true, generalTasks = true, schedule = true),
    val received: PayloadSummary? = null,
    val report: TransferReport? = null,
    val instanceName: String = "",
    val importError: String? = null,
    val tech: TechInfo = emptyMap(),
    val log: List<LogLine> = emptyList()
) {
    val displayNameBytes: Int get() = displayName.trim().toByteArray().size
    val canStart: Boolean get() = displayName.isNotBlank() && displayNameBytes <= BleAdvert.MAX_NAME_BYTES
}

/**
 * Contador bajo el nombre visible: «5/12 caracteres». El límite real son bytes (lo que cabe en el
 * anuncio BLE), así que si hay letras que ocupan más (tildes, ñ) se avisa de que cuentan doble.
 */
internal fun displayNameCounter(name: String): String {
    val trimmed = name.trim()
    val bytes = trimmed.toByteArray().size
    val base = "$bytes/${BleAdvert.MAX_NAME_BYTES} caracteres"
    return if (bytes > trimmed.length) "$base · las tildes y la ñ cuentan doble" else base
}

/**
 * Recibir: crea un grupo Wi‑Fi Direct, abre el socket, se anuncia por BLE y espera al emisor.
 * Los datos recibidos son de esta sesión y de nadie más (antes se acumulaban en un singleton y
 * se importaba siempre el primer mensaje).
 */
@HiltViewModel
class ReceiveViewModel @Inject constructor(
    private val env: ShareEnvironment,
    private val ble: BleTransport,
    private val wifi: WifiDirectTransport,
    private val instances: InstanceRepository,
    private val schedule: ShareScheduleStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiveUiState())
    val uiState: StateFlow<ReceiveUiState> = _uiState.asStateFlow()

    private var sessionJob: Job? = null
    private var selectionRequest: CompletableDeferred<ShareSelection>? = null
    private var pending: ReceivedPayload? = null

    init {
        val hello = env.hello()
        tech(TechSection.DEVICE, "Modelo", hello.model)
        tech(TechSection.DEVICE, "Android", hello.androidVersion)
        tech(TechSection.DEVICE, "App", "Cinco Pelao ${hello.appVersion} · protocolo v${ShareProtocol.VERSION}")
    }

    fun missingPermissions(): List<String> = env.missingPermissions(ShareRole.RECEIVE)

    fun refreshSetup() {
        if (sessionJob?.isActive == true) return
        val issues = env.setupIssues(ShareRole.RECEIVE)
        val phase = _uiState.value.phase
        when {
            issues.isNotEmpty() && (phase is SharePhase.Idle || phase is SharePhase.NeedsSetup) ->
                setPhase(SharePhase.NeedsSetup(issues))
            issues.isEmpty() && phase is SharePhase.NeedsSetup -> setPhase(SharePhase.Idle)
        }
    }

    fun setDisplayName(name: String) = _uiState.update { it.copy(displayName = name.take(24)) }

    fun setSelection(selection: ShareSelection) = _uiState.update { it.copy(selection = selection.normalized()) }

    fun setInstanceName(name: String) = _uiState.update { it.copy(instanceName = name.take(40), importError = null) }

    /** Se hace visible y espera a un emisor. */
    fun start() {
        val state = _uiState.value
        if (!state.canStart || sessionJob?.isActive == true) return
        sessionJob = viewModelScope.launch { runSession(state.displayName.trim()) }
    }

    /** Pide al emisor la selección actual. */
    fun requestSelection() {
        val selection = _uiState.value.selection.normalized()
        if (!selection.isEmpty) selectionRequest?.complete(selection)
    }

    /** Corta lo que esté en curso y vuelve al principio (conserva el nombre visible). */
    fun restart() {
        sessionJob?.cancel()
        sessionJob = null
        pending = null
        _uiState.update {
            ReceiveUiState(displayName = it.displayName, tech = it.tech.filterKeys { s -> s == TechSection.DEVICE }, log = it.log)
        }
        log("APP", "— nueva sesión —")
        refreshSetup()
    }

    /** Guarda lo recibido como una instancia nueva (que pasa a ser la activa). */
    fun import() {
        val data = pending ?: return
        val name = _uiState.value.instanceName.trim()
        if (name.isEmpty() || _uiState.value.phase != SharePhase.Received) return
        setPhase(SharePhase.Importing)
        viewModelScope.launch {
            try {
                val id = instances.importAsNewInstance(name, data.data)
                log("DB", "Instancia «$name» creada (${data.data.subjects.size} asig., ${data.data.events.size} ev., ${data.data.tasks.size} tareas)")
                data.schedule?.let { png ->
                    if (schedule.import(id, png)) log("DB", "Horario guardado (${formatBytes(png.size.toLong())})")
                    else log("ERR", "El horario recibido no es una imagen válida: se descarta")
                }
                pending = null
                setPhase(SharePhase.Done)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log("ERR", "Importación: ${e::class.simpleName}: ${e.message}")
                _uiState.update {
                    it.copy(phase = SharePhase.Received, importError = "No se ha podido guardar: ${e.message ?: "error de la base de datos"}")
                }
            }
        }
    }

    private suspend fun runSession(displayName: String) {
        val credentials = GroupCredentials.generate()
        var server: ServerSocket? = null
        var connection: ShareConnection? = null
        try {
            setPhase(SharePhase.Advertising)
            log("P2P", "Creando grupo ${credentials.networkName} · banda AUTO · WPA2")
            val created = wifi.createGroup(credentials)
            val group = created.info
            if (created.credentials.networkName != credentials.networkName) {
                log("P2P", "El sistema ha usado otro nombre de red: ${created.credentials.networkName}")
            }
            tech(TechSection.WIFI, "SSID", group.networkName)
            group.frequencyMhz?.let { tech(TechSection.WIFI, "Frecuencia", describeFrequency(it)) }
            group.interfaceName?.let { tech(TechSection.WIFI, "Interfaz", it) }
            tech(TechSection.WIFI, "MAC Wi‑Fi Direct", describeMac(group.ownerMac))
            tech(TechSection.WIFI, "IP (dueño del grupo)", group.ownerIp ?: group.localIp ?: "?")
            log("P2P", "Grupo listo · dueño ${group.ownerIp ?: "?"}${group.frequencyMhz?.let { " · $it MHz" } ?: ""}")

            val socketServer = wifi.openServer(ShareProtocol.PORT)
            server = socketServer
            tech(TechSection.TCP, "Escuchando", "0.0.0.0:${ShareProtocol.PORT}")
            log("TCP", "Escuchando en el puerto ${ShareProtocol.PORT}")

            val beacon = ReceiverBeacon(
                displayName = displayName,
                networkName = created.credentials.networkName,
                passphrase = created.credentials.passphrase
            )
            tech(TechSection.BLE, "Baliza GATT", "${beacon.encode().size} B")
            val socket = coroutineScope {
                val advertising = launch {
                    ble.advertise(displayName, beacon).collect { event ->
                        when (event) {
                            is AdvertiseEvent.Started -> {
                                tech(TechSection.BLE, "Anuncio", "${event.mode} (cada ~100 ms) · potencia ${event.txPower}")
                                log("BLE", "Anunciándose como «$displayName» (${event.mode}, TX ${event.txPower})")
                            }
                            is AdvertiseEvent.ClientConnected -> log("GATT", "${event.address} conectado")
                            is AdvertiseEvent.BeaconRead -> log("GATT", "${event.address} lee la baliza (offset ${event.offset}, ${event.bytes} B)")
                            is AdvertiseEvent.ClientDisconnected -> log("GATT", "${event.address} desconectado")
                        }
                    }
                }
                val accepted = wifi.accept(socketServer)
                advertising.cancel()
                accepted
            }
            log("BLE", "Anuncio detenido")
            socketServer.close() // solo se atiende a un emisor por sesión
            tech(TechSection.TCP, "Emisor", "${socket.inetAddress.hostAddress}:${socket.port}")
            log("TCP", "Conexión entrante de ${socket.inetAddress.hostAddress}:${socket.port}")
            val conn = ShareConnection(socket)
            connection = conn

            setPhase(SharePhase.Handshaking)
            val session = ReceiverSession(conn, env.hello(), log = ::log)
            val peer = session.handshake()
            _uiState.update { it.copy(peer = peer) }
            tech(TechSection.PROTOCOL, "Otro móvil", "${peer.hello.model} · Android ${peer.hello.androidVersion}")
            tech(TechSection.PROTOCOL, "Su app", "${peer.hello.appVersion} · protocolo v${peer.hello.protocolVersion}")
            tech(TechSection.PROTOCOL, "Código", peer.verificationCode)

            setPhase(SharePhase.Choosing)
            val selection = CompletableDeferred<ShareSelection>().also { selectionRequest = it }.await()
            log("APP", "Pidiendo: ${describeSelection(selection)}")

            setPhase(SharePhase.AwaitingApproval)
            var lastPercent = -1
            val received = session.request(selection) { done, total ->
                val percent = (progressOf(done, total) * 100).toInt()
                if (percent != lastPercent) {
                    lastPercent = percent
                    setPhase(SharePhase.Transferring(percent / 100f))
                }
            }
            pending = received
            val report = received.report
            tech(TechSection.PROTOCOL, "Recibido", "${formatBytes(report.frame.wireBytes.toLong())} en ${formatMillis(report.durationNanos)}")
            tech(TechSection.PROTOCOL, "Velocidad", formatRate(report.bytesPerSecond))
            tech(TechSection.PROTOCOL, "CRC32", "%08X ✓".format(report.frame.crc32))
            tech(TechSection.PROTOCOL, "SHA-256", shortHash(report.frame.sha256, groups = 4))
            _uiState.update {
                it.copy(
                    received = received.data.summary(received.schedule?.size ?: 0),
                    report = report,
                    instanceName = it.instanceName.ifBlank { peer.hello.deviceName.take(40) }
                )
            }
            setPhase(SharePhase.Received)
        } catch (e: TimeoutCancellationException) {
            fail(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Si la sesión ya se ha cancelado (Cancelar, salir), el error es consecuencia de cortarla
            currentCoroutineContext().ensureActive()
            fail(e)
        } finally {
            selectionRequest = null
            connection?.close()
            server?.close()
            withContext(NonCancellable) { wifi.removeGroupQuietly() }
        }
    }

    private fun setPhase(phase: SharePhase) = _uiState.update { it.copy(phase = phase) }

    private fun tech(section: TechSection, label: String, value: String) =
        _uiState.update { it.copy(tech = it.tech.with(section, label, value)) }

    private fun log(tag: String, text: String) =
        _uiState.update { it.copy(log = it.log.appendCapped(LogLine(System.currentTimeMillis(), tag, text))) }

    private fun fail(e: Throwable) {
        log("ERR", "${e::class.simpleName}: ${e.message}")
        setPhase(SharePhase.Failed(describeShareError(e)))
    }
}
