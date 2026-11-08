package io.github.mateorguez.cincopelao.sharing.wifidirect

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.mateorguez.cincopelao.sharing.protocol.GroupCredentials
import io.github.mateorguez.cincopelao.sharing.protocol.ReceiverBeacon
import io.github.mateorguez.cincopelao.sharing.protocol.cancellableBlocking
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

/** Datos del grupo Wi‑Fi Direct para el panel técnico. Nunca incluye la contraseña. */
data class GroupInfo(
    val networkName: String,
    val frequencyMhz: Int?,
    val interfaceName: String?,
    val ownerMac: String?,
    val ownerIp: String?,
    val localIp: String?,
    val isOwner: Boolean
)

/** Grupo creado por el receptor con las credenciales que de verdad usa el sistema. */
class CreatedGroup(val info: GroupInfo, val credentials: GroupCredentials)

class WifiDirectException(message: String) : Exception(message)

/**
 * Wi‑Fi Direct de la compartición.
 *
 * - El receptor crea un grupo con nombre y contraseña propios (generados en cada sesión) y es su
 *   dueño (192.168.49.1).
 * - El emisor se une con esos datos, leídos por BLE: no hace falta buscar al otro móvil ni que
 *   nadie acepte una invitación (antes se buscaba por nombre y se lanzaba una conexión normal).
 *
 * Los permisos se comprueban en la pantalla antes de llamar aquí.
 */
@SuppressLint("MissingPermission")
@Singleton
class WifiDirectTransport @Inject constructor(@ApplicationContext private val context: Context) {

    private val manager: WifiP2pManager? = context.getSystemService(WifiP2pManager::class.java)
    private var channel: WifiP2pManager.Channel? = null

    private fun p2p(): Pair<WifiP2pManager, WifiP2pManager.Channel> {
        val m = manager ?: throw WifiDirectException("Este móvil no tiene Wi‑Fi Direct")
        val c = channel ?: m.initialize(context, Looper.getMainLooper()) { channel = null }
            ?: throw WifiDirectException("No se pudo iniciar Wi‑Fi Direct")
        channel = c
        return m to c
    }

    /**
     * Receptor: crea el grupo y espera a que esté listo. Si el sistema no respeta el nombre o la
     * contraseña pedidos (algún fabricante), devuelve los que usa de verdad, que son los que hay
     * que publicar por BLE.
     */
    suspend fun createGroup(credentials: GroupCredentials): CreatedGroup {
        val (m, c) = p2p()
        removeGroupQuietly() // un grupo que se quedó abierto haría fallar la creación (BUSY)
        val config = WifiP2pConfig.Builder()
            .setNetworkName(credentials.networkName)
            .setPassphrase(credentials.passphrase)
            .setGroupOperatingBand(WifiP2pConfig.GROUP_OWNER_BAND_AUTO)
            .enablePersistentMode(false)
            .build()
        action("crear el grupo") { m.createGroup(c, config, it) }

        // El grupo tarda un poco en aparecer; antes se esperaban 500 ms a ciegas
        repeat(40) {
            val group = requestGroup()
            if (group != null && group.isGroupOwner && !group.networkName.isNullOrEmpty()) {
                val actual = GroupCredentials(
                    networkName = group.networkName,
                    passphrase = group.passphrase?.takeIf { it.isNotEmpty() } ?: credentials.passphrase
                )
                return CreatedGroup(describe(group, requestConnectionInfo()), actual)
            }
            delay(250)
        }
        throw WifiDirectException("El grupo Wi‑Fi Direct no llegó a crearse")
    }

    /** Emisor: se une al grupo del receptor y espera a tener IP. */
    suspend fun joinGroup(beacon: ReceiverBeacon, timeoutMillis: Long = 30_000): GroupInfo {
        val (m, c) = p2p()
        removeGroupQuietly()
        val config = WifiP2pConfig.Builder()
            .setNetworkName(beacon.networkName)
            .setPassphrase(beacon.passphrase)
            .enablePersistentMode(false)
            .build()

        val changes = callbackFlow {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val info = IntentCompat.getParcelableExtra(intent, WifiP2pManager.EXTRA_WIFI_P2P_INFO, WifiP2pInfo::class.java)
                    val group = IntentCompat.getParcelableExtra(intent, WifiP2pManager.EXTRA_WIFI_P2P_GROUP, WifiP2pGroup::class.java)
                    trySend(info to group)
                }
            }
            ContextCompat.registerReceiver(
                context, receiver,
                IntentFilter(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            // Se conecta con el receptor ya registrado para no perder el aviso
            m.connect(c, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() = Unit
                override fun onFailure(reason: Int) {
                    close(WifiDirectException("No se pudo unir al grupo (${reasonText(reason)})"))
                }
            })
            awaitClose { runCatching { context.unregisterReceiver(receiver) } }
        }

        return try {
            withTimeout(timeoutMillis) {
                // El aviso es "pegajoso": se ignora el estado anterior y otros grupos. Algunas
                // versiones no adjuntan el grupo; entonces basta con que se haya formado como cliente
                val (info, group) = changes.first { (info, group) ->
                    info?.groupFormed == true && !info.isGroupOwner && info.groupOwnerAddress != null &&
                        (group?.networkName.isNullOrEmpty() || group?.networkName == beacon.networkName)
                }
                describe(group, info)
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            runCatching { action("cancelar la conexión") { m.cancelConnect(c, it) } }
            throw WifiDirectException("No se pudo unir al grupo Wi‑Fi Direct en ${timeoutMillis / 1000} s")
        }
    }

    /** Dirección MAC de Wi‑Fi Direct de este móvil (Android suele aleatorizarla). */
    suspend fun ownDeviceAddress(): String? {
        val (m, c) = runCatching { p2p() }.getOrNull() ?: return null
        return suspendCancellableCoroutine { cont -> m.requestDeviceInfo(c) { cont.resume(it?.deviceAddress) } }
    }

    /** Deshace el grupo (receptor) o sale de él (emisor). No falla nunca. */
    suspend fun removeGroupQuietly() {
        val (m, c) = runCatching { p2p() }.getOrNull() ?: return
        runCatching { action("cancelar la conexión") { m.cancelConnect(c, it) } }
        runCatching { action("deshacer el grupo") { m.removeGroup(c, it) } }
    }

    // Sockets ------------------------------------------------------------------------------------

    /** Receptor: abre el puerto antes de anunciarse, para que el emisor no llegue antes que él. */
    fun openServer(port: Int): ServerSocket = ServerSocket().apply {
        reuseAddress = true
        bind(InetSocketAddress(port))
    }

    /** Espera una conexión; cancelar cierra el ServerSocket. */
    suspend fun accept(server: ServerSocket): Socket = cancellableBlocking(onCancel = server::close) {
        server.accept().apply { tcpNoDelay = true }
    }

    /** Emisor: se conecta al socket del receptor, con reintentos mientras la red termina de montarse. */
    suspend fun connect(host: String, port: Int, attempts: Int = 5): Socket {
        var last: Exception? = null
        repeat(attempts) { attempt ->
            val socket = Socket()
            try {
                cancellableBlocking(onCancel = socket::close) { socket.connect(InetSocketAddress(host, port), 5_000) }
                socket.tcpNoDelay = true
                return socket
            } catch (e: java.io.IOException) {
                socket.close()
                last = e
                if (attempt < attempts - 1) delay(1_000)
            }
        }
        throw WifiDirectException("No se pudo abrir el socket con $host:$port (${last?.message})")
    }

    // Utilidades ---------------------------------------------------------------------------------

    private suspend fun requestGroup(): WifiP2pGroup? {
        val (m, c) = p2p()
        return suspendCancellableCoroutine { cont -> m.requestGroupInfo(c) { cont.resume(it) } }
    }

    private suspend fun requestConnectionInfo(): WifiP2pInfo? {
        val (m, c) = p2p()
        return suspendCancellableCoroutine { cont -> m.requestConnectionInfo(c) { cont.resume(it) } }
    }

    private fun describe(group: WifiP2pGroup?, info: WifiP2pInfo?): GroupInfo {
        val iface = group?.`interface`
        return GroupInfo(
            networkName = group?.networkName.orEmpty(),
            frequencyMhz = group?.frequency?.takeIf { it > 0 },
            interfaceName = iface,
            ownerMac = group?.owner?.deviceAddress,
            ownerIp = info?.groupOwnerAddress?.hostAddress,
            localIp = iface?.let(::ipv4Of),
            isOwner = group?.isGroupOwner == true
        )
    }

    private fun ipv4Of(interfaceName: String): String? = runCatching {
        NetworkInterface.getByName(interfaceName)?.inetAddresses?.toList()
            ?.firstOrNull { it is Inet4Address }?.hostAddress
    }.getOrNull()

    private suspend fun action(what: String, call: (WifiP2pManager.ActionListener) -> Unit) =
        suspendCancellableCoroutine { cont ->
            call(object : WifiP2pManager.ActionListener {
                override fun onSuccess() = cont.resume(Unit)
                override fun onFailure(reason: Int) =
                    cont.resumeWithException(WifiDirectException("No se pudo $what (${reasonText(reason)})"))
            })
        }

    private fun reasonText(reason: Int): String = when (reason) {
        WifiP2pManager.P2P_UNSUPPORTED -> "Wi‑Fi Direct no soportado"
        WifiP2pManager.BUSY -> "el Wi‑Fi está ocupado"
        WifiP2pManager.ERROR -> "error interno"
        WifiP2pManager.NO_SERVICE_REQUESTS -> "sin peticiones de servicio"
        else -> "código $reason"
    }
}
