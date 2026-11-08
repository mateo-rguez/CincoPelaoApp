package io.github.mateorguez.cincopelao.ui.sharing

import io.github.mateorguez.cincopelao.sharing.ble.ScanSighting
import io.github.mateorguez.cincopelao.sharing.protocol.ShareProtocol
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Modelos de las pantallas de compartición y sus reglas (puras, con tests).

/** Un receptor cercano en la lista de Transmitir. */
data class NearbyReceiver(
    val address: String,
    val displayName: String,
    val rssi: Int,
    val txPower: Int?,
    val protocolVersion: Int,
    val lastSeenMillis: Long,
    val sightings: Int
) {
    val isCompatible: Boolean get() = protocolVersion == ShareProtocol.VERSION
}

/** Sin anuncios durante este tiempo, el receptor se quita de la lista (anuncia ~10 veces por segundo). */
const val STALE_RECEIVER_MILLIS = 3_000L

/**
 * Añade o actualiza un avistamiento. El RSSI se suaviza para que la lista no baile.
 *
 * Un receptor que vuelve a anunciarse estrena dirección BLE aleatoria: si llega uno con el mismo
 * nombre y otra dirección, sustituye al antiguo en su sitio en vez de salir duplicado.
 */
fun List<NearbyReceiver>.withSighting(s: ScanSighting): List<NearbyReceiver> {
    val sameAddress = find { it.address == s.address }
    val sameName = if (sameAddress == null) find { it.displayName == s.displayName } else null
    val existing = sameAddress
    val updated = if (existing == null) {
        NearbyReceiver(s.address, s.displayName, s.rssi, s.txPower, s.protocolVersion, s.timestampMillis, 1)
    } else {
        existing.copy(
            displayName = s.displayName,
            rssi = (existing.rssi * 2 + s.rssi) / 3,
            txPower = s.txPower ?: existing.txPower,
            protocolVersion = s.protocolVersion,
            lastSeenMillis = s.timestampMillis,
            sightings = existing.sightings + 1
        )
    }
    // Se mantiene el orden de aparición para que la lista no salte mientras se pulsa
    return when {
        sameAddress != null -> map { if (it.address == s.address) updated else it }
        sameName != null -> map { if (it.address == sameName.address) updated else it }
        else -> this + updated
    }
}

/** Quita los receptores que hace más de [timeoutMillis] que no se ven (han dejado de anunciarse). */
fun List<NearbyReceiver>.withoutStale(nowMillis: Long, timeoutMillis: Long = STALE_RECEIVER_MILLIS): List<NearbyReceiver> =
    filter { nowMillis - it.lastSeenMillis <= timeoutMillis }

/** Una línea de la consola técnica. */
data class LogLine(val timeMillis: Long, val tag: String, val text: String) {
    fun format(zone: ZoneId = ZoneId.systemDefault()): String =
        "${TIME.format(Instant.ofEpochMilli(timeMillis).atZone(zone))} ${tag.padEnd(4)} $text"

    private companion object {
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    }
}

const val MAX_LOG_LINES = 200

fun List<LogLine>.appendCapped(line: LogLine, max: Int = MAX_LOG_LINES): List<LogLine> =
    (this + line).takeLast(max)

/** Secciones del panel de detalles técnicos. */
enum class TechSection(val title: String) {
    DEVICE("Este móvil"),
    BLE("Bluetooth LE"),
    WIFI("Wi‑Fi Direct"),
    TCP("Socket TCP"),
    PROTOCOL("Protocolo")
}

typealias TechInfo = Map<TechSection, Map<String, String>>

/** Añade o cambia una fila manteniendo el orden en que aparecieron. */
fun TechInfo.with(section: TechSection, label: String, value: String): TechInfo =
    this + (section to ((this[section] ?: emptyMap()) + (label to value)))
