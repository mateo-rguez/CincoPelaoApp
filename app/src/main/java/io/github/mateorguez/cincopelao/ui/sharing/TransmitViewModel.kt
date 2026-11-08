package io.github.mateorguez.cincopelao.ui.sharing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mateorguez.cincopelao.data.repository.EventRepository
import io.github.mateorguez.cincopelao.data.repository.SubjectRepository
import io.github.mateorguez.cincopelao.data.repository.TaskRepository
import io.github.mateorguez.cincopelao.sharing.ShareEnvironment
import io.github.mateorguez.cincopelao.sharing.ShareScheduleStore
import io.github.mateorguez.cincopelao.sharing.ble.BleTransport
import io.github.mateorguez.cincopelao.sharing.protocol.BleAdvert
import io.github.mateorguez.cincopelao.sharing.protocol.DataToSend
import io.github.mateorguez.cincopelao.sharing.protocol.PayloadSummary
import io.github.mateorguez.cincopelao.sharing.protocol.ShareConnection
import io.github.mateorguez.cincopelao.sharing.protocol.ShareProtocol
import io.github.mateorguez.cincopelao.sharing.protocol.ShareSelection
import io.github.mateorguez.cincopelao.sharing.protocol.buildSharePayload
import io.github.mateorguez.cincopelao.sharing.protocol.summary
import io.github.mateorguez.cincopelao.sharing.session.Peer
import io.github.mateorguez.cincopelao.sharing.session.SenderSession
import io.github.mateorguez.cincopelao.sharing.session.SharePhase
import io.github.mateorguez.cincopelao.sharing.session.ShareRole
import io.github.mateorguez.cincopelao.sharing.session.TransferReport
import io.github.mateorguez.cincopelao.sharing.session.progressOf
import io.github.mateorguez.cincopelao.sharing.wifidirect.WifiDirectException
import io.github.mateorguez.cincopelao.sharing.wifidirect.WifiDirectTransport
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TransmitUiState(
    val phase: SharePhase = SharePhase.Idle,
    val receivers: List<NearbyReceiver> = emptyList(),
    val target: NearbyReceiver? = null,
    val peer: Peer? = null,
    val request: ShareSelection? = null,
    /** Lo que se enviaría si se acepta la petición. */
    val offer: PayloadSummary? = null,
    val report: TransferReport? = null,
    val tech: TechInfo = emptyMap(),
    val log: List<LogLine> = emptyList()
)

/**
 * Transmitir: busca receptores por BLE, lee la baliza del elegido, se une a su grupo Wi‑Fi Direct
 * y le envía lo que pida si el usuario acepta. Todo vive en [sessionJob]: cancelarlo (o salir de
 * la pantalla) cierra el socket y deja el grupo.
 */
@HiltViewModel
class TransmitViewModel @Inject constructor(
    private val env: ShareEnvironment,
    private val ble: BleTransport,
    private val wifi: WifiDirectTransport,
    private val subjects: SubjectRepository,
    private val events: EventRepository,
    private val tasks: TaskRepository,
    private val schedule: ShareScheduleStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransmitUiState())
    val uiState: StateFlow<TransmitUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null
    private var sessionJob: Job? = null
    private var approval: CompletableDeferred<Boolean>? = null

    init {
        describeDevice()
    }

    fun missingPermissions(): List<String> = env.missingPermissions(ShareRole.TRANSMIT)

    /** Comprueba permisos y radios; si todo está bien y no hay sesión, (re)empieza a buscar. */
    fun refreshSetup() {
        if (sessionJob?.isActive == true) return
        val issues = env.setupIssues(ShareRole.TRANSMIT)
        if (issues.isNotEmpty()) {
            stopScan()
            setPhase(SharePhase.NeedsSetup(issues))
            return
        }
        val phase = _uiState.value.phase
        if (phase is SharePhase.Idle || phase is SharePhase.NeedsSetup || (phase is SharePhase.Scanning && scanJob?.isActive != true)) {
            startScan()
        }
    }

    /** Al salir de la pantalla se deja de escanear (la sesión, si la hay, sigue). */
    fun onPause() {
        if (sessionJob?.isActive != true) stopScan()
    }

    fun connect(receiver: NearbyReceiver) {
        if (sessionJob?.isActive == true || !receiver.isCompatible) return
        stopScan()
        _uiState.update { it.copy(target = receiver, phase = SharePhase.ReadingBeacon) }
        sessionJob = viewModelScope.launch { runSession(receiver) }
    }

    fun approve() {
        approval?.complete(true)
    }

    fun decline() {
        approval?.complete(false)
    }

    /** Corta la sesión en curso y vuelve a buscar. */
    fun restart() {
        sessionJob?.cancel()
        sessionJob = null
        _uiState.update {
            TransmitUiState(tech = it.tech.filterKeys { s -> s == TechSection.DEVICE }, log = it.log)
        }
        log("APP", "— nueva sesión —")
        refreshSetup()
    }

    // Escaneo -------------------------------------------------------------------------------------

    private fun startScan() {
        scanJob?.cancel()
        _uiState.update { it.copy(phase = SharePhase.Scanning, receivers = emptyList()) }
        log("BLE", "Escaneo LOW_LATENCY · filtro de servicio ${BleAdvert.SERVICE_UUID.toString().take(8)}…")
        scanJob = viewModelScope.launch {
            launch {
                while (isActive) {
                    delay(1_000)
                    val now = System.currentTimeMillis()
                    val before = _uiState.value.receivers
                    val after = before.withoutStale(now)
                    (before - after.toSet()).forEach { log("BLE", "${it.address} ha dejado de anunciarse") }
                    _uiState.update { it.copy(receivers = it.receivers.withoutStale(now)) }
                }
            }
            try {
                ble.scan().collect { sighting ->
                    if (_uiState.value.receivers.none { it.address == sighting.address }) {
                        log("BLE", "Visto «${sighting.displayName}» ${sighting.address} rssi=${sighting.rssi} dBm v${sighting.protocolVersion}")
                    }
                    _uiState.update { it.copy(receivers = it.receivers.withSighting(sighting)) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    private fun stopScan() {
        if (scanJob?.isActive == true) log("BLE", "Escaneo detenido")
        scanJob?.cancel()
        scanJob = null
    }

    // Sesión --------------------------------------------------------------------------------------

    private suspend fun runSession(receiver: NearbyReceiver) {
        var connection: ShareConnection? = null
        try {
            tech(TechSection.BLE, "Receptor", "«${receiver.displayName}»")
            tech(TechSection.BLE, "MAC BLE", "${receiver.address} (aleatoria)")
            tech(TechSection.BLE, "RSSI", "${receiver.rssi} dBm · ${formatDistance(estimateDistanceMeters(receiver.rssi, receiver.txPower ?: -59))}")
            log("GATT", "Conectando a ${receiver.address} · pidiendo MTU 517")
            val read = ble.readBeacon(receiver.address)
            val beacon = read.beacon
            tech(TechSection.BLE, "MTU", "${read.mtu} bytes")
            tech(TechSection.BLE, "Lectura GATT", "${beacon.encode().size} B en ${read.elapsedMillis} ms")
            log("GATT", "Baliza leída (MTU ${read.mtu}) en ${read.elapsedMillis} ms · GATT cerrado")

            setPhase(SharePhase.ConnectingWifi)
            log("P2P", "Uniéndose a ${beacon.networkName} (WPA2, Android 10+ sin invitación)")
            val group = wifi.joinGroup(beacon)
            describeGroup(group.networkName, group.frequencyMhz, group.interfaceName, group.ownerMac, group.localIp, group.ownerIp)
            wifi.ownDeviceAddress()?.let { tech(TechSection.DEVICE, "MAC Wi‑Fi Direct", it) }
            log("P2P", "Grupo formado · IP ${group.localIp ?: "?"} → dueño ${group.ownerIp ?: "?"}")

            val host = group.ownerIp ?: throw WifiDirectException("No se conoce la IP del receptor")
            log("TCP", "Conectando a $host:${beacon.port}")
            val socket = wifi.connect(host, beacon.port)
            tech(TechSection.TCP, "Local", "${socket.localAddress.hostAddress}:${socket.localPort}")
            tech(TechSection.TCP, "Remoto", "${socket.inetAddress.hostAddress}:${socket.port}")
            tech(TechSection.TCP, "Opciones", "TCP_NODELAY · bloques de ${ShareProtocol.CHUNK_BYTES / 1024} KB")
            log("TCP", "Conectado (TCP_NODELAY)")
            val conn = ShareConnection(socket)
            connection = conn

            setPhase(SharePhase.Handshaking)
            val session = SenderSession(conn, env.hello(), log = ::log)
            val peer = session.handshake()
            _uiState.update { it.copy(peer = peer) }
            describePeer(peer)

            setPhase(SharePhase.Choosing)
            val request = session.awaitRequest()
            val (data, png) = prepare(request)
            val offer = data.summary(png?.size ?: 0)
            _uiState.update { it.copy(request = request, offer = offer) }
            log("APP", "Petición: ${describeSelection(request)}")

            setPhase(SharePhase.AwaitingApproval)
            val accepted = CompletableDeferred<Boolean>().also { approval = it }.await()
            if (!accepted) {
                session.decline("El otro móvil ha rechazado el envío")
                log("APP", "Envío rechazado")
                setPhase(SharePhase.Failed("Has rechazado el envío"))
                return
            }

            setPhase(SharePhase.Transferring(0f))
            var lastPercent = -1
            val report = session.send(data, png) { done, total ->
                val percent = (progressOf(done, total) * 100).toInt()
                if (percent != lastPercent) {
                    lastPercent = percent
                    setPhase(SharePhase.Transferring(percent / 100f))
                }
            }
            describeReport(report)
            _uiState.update { it.copy(report = report) }
            setPhase(SharePhase.Done)
        } catch (e: TimeoutCancellationException) {
            fail(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fail(e)
        } finally {
            approval = null
            connection?.close()
            withContext(NonCancellable) { wifi.removeGroupQuietly() }
        }
    }

    /** Monta el paquete con los datos actuales de la instancia activa. */
    private suspend fun prepare(selection: ShareSelection): Pair<DataToSend, ByteArray?> {
        val data = buildSharePayload(subjects.subjects.first(), events.events.first(), tasks.tasks.first(), selection)
        val png = if (selection.schedule) schedule.exportCurrent() else null
        if (selection.schedule && png == null) log("APP", "No hay horario en esta instancia: se envía sin él")
        return data to png
    }

    // Detalles técnicos ---------------------------------------------------------------------------

    private fun describeDevice() {
        val hello = env.hello()
        tech(TechSection.DEVICE, "Modelo", hello.model)
        tech(TechSection.DEVICE, "Android", hello.androidVersion)
        tech(TechSection.DEVICE, "App", "Cinco Pelao ${hello.appVersion} · protocolo v${ShareProtocol.VERSION}")
    }

    private fun describeGroup(ssid: String, freq: Int?, iface: String?, ownerMac: String?, localIp: String?, ownerIp: String?) {
        tech(TechSection.WIFI, "SSID", ssid)
        freq?.let { tech(TechSection.WIFI, "Frecuencia", describeFrequency(it)) }
        iface?.let { tech(TechSection.WIFI, "Interfaz", it) }
        ownerMac?.let { tech(TechSection.WIFI, "MAC del dueño", it) }
        tech(TechSection.WIFI, "IP propia", localIp ?: "?")
        tech(TechSection.WIFI, "IP del dueño", ownerIp ?: "?")
    }

    private fun describePeer(peer: Peer) {
        tech(TechSection.PROTOCOL, "Otro móvil", "${peer.hello.model} · Android ${peer.hello.androidVersion}")
        tech(TechSection.PROTOCOL, "Su app", "${peer.hello.appVersion} · protocolo v${peer.hello.protocolVersion}")
        tech(TechSection.PROTOCOL, "Código", peer.verificationCode)
    }

    private fun describeReport(report: TransferReport) {
        tech(TechSection.PROTOCOL, "Enviado", "${formatBytes(report.frame.wireBytes.toLong())} en ${formatMillis(report.durationNanos)}")
        tech(TechSection.PROTOCOL, "Velocidad", formatRate(report.bytesPerSecond))
        tech(TechSection.PROTOCOL, "CRC32", "%08X".format(report.frame.crc32))
        tech(TechSection.PROTOCOL, "SHA-256", shortHash(report.frame.sha256, groups = 4))
    }

    // Utilidades ----------------------------------------------------------------------------------

    private fun setPhase(phase: SharePhase) = _uiState.update { it.copy(phase = phase) }

    private fun tech(section: TechSection, label: String, value: String) =
        _uiState.update { it.copy(tech = it.tech.with(section, label, value)) }

    private fun log(tag: String, text: String) =
        _uiState.update { it.copy(log = it.log.appendCapped(LogLine(System.currentTimeMillis(), tag, text))) }

    private fun fail(e: Throwable) {
        val message = describeShareError(e)
        log("ERR", "${e::class.simpleName}: ${e.message}")
        setPhase(SharePhase.Failed(message))
    }
}

/** "asignaturas y eventos, tareas generales, horario". */
fun describeSelection(selection: ShareSelection): String = buildList {
    if (selection.subjectsAndEvents) add("asignaturas y eventos")
    if (selection.subjectTasks) add("tareas de asignaturas")
    if (selection.generalTasks) add("tareas generales")
    if (selection.schedule) add("horario")
}.joinToString(", ").ifEmpty { "nada" }
