package io.github.mateorguez.cincopelao.ui.sharing

import java.util.Locale
import kotlin.math.pow

// Formato de los datos técnicos que se enseñan durante la compartición. Funciones puras.

private val es = Locale.forLanguageTag("es")

fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(es, "%.1f KB", bytes / 1024.0)
    else -> String.format(es, "%.2f MB", bytes / (1024.0 * 1024.0))
}

fun formatRate(bytesPerSecond: Double): String =
    if (bytesPerSecond <= 0) "—" else "${formatBytes(bytesPerSecond.toLong())}/s · " +
        String.format(es, "%.1f Mbit/s", bytesPerSecond * 8 / 1_000_000)

fun formatMillis(nanos: Long): String = String.format(es, "%.0f ms", nanos / 1_000_000.0)

/** Canal Wi‑Fi a partir de la frecuencia, o null si no es una frecuencia Wi‑Fi conocida. */
fun wifiChannel(frequencyMhz: Int): Int? = when (frequencyMhz) {
    2484 -> 14
    in 2412..2472 -> (frequencyMhz - 2407) / 5
    in 5160..5885 -> (frequencyMhz - 5000) / 5
    in 5955..7115 -> (frequencyMhz - 5950) / 5
    else -> null
}

fun wifiBand(frequencyMhz: Int): String = when {
    frequencyMhz < 3000 -> "2,4 GHz"
    frequencyMhz < 5925 -> "5 GHz"
    else -> "6 GHz"
}

/** "5 GHz · canal 149 (5745 MHz)". */
fun describeFrequency(frequencyMhz: Int): String {
    val channel = wifiChannel(frequencyMhz)?.let { " · canal $it" } ?: ""
    return "${wifiBand(frequencyMhz)}$channel ($frequencyMhz MHz)"
}

/** Calidad de la señal de 0 (casi nada) a 4 barras. */
fun signalBars(rssi: Int): Int = when {
    rssi >= -55 -> 4
    rssi >= -67 -> 3
    rssi >= -80 -> 2
    rssi >= -90 -> 1
    else -> 0
}

/**
 * Distancia aproximada con el modelo de pérdida en espacio libre: d = 10^((Tx − RSSI) / (10·n)).
 * [txPower] es el RSSI esperado a 1 m (−59 dBm es un valor típico de un móvil) y n = 2 en
 * espacio abierto. Es una estimación: paredes y cuerpos la cambian mucho.
 */
fun estimateDistanceMeters(rssi: Int, txPower: Int = -59, pathLossExponent: Double = 2.0): Double =
    10.0.pow((txPower - rssi) / (10 * pathLossExponent))

fun formatDistance(meters: Double): String = when {
    meters < 1 -> String.format(es, "~%.0f cm", meters * 100)
    meters < 10 -> String.format(es, "~%.1f m", meters)
    else -> String.format(es, "~%.0f m", meters)
}

/** Separa un SHA-256 en grupos de 8 para leerlo mejor: "3fa2b1c0 9e…". */
fun shortHash(sha256: String, groups: Int = 2): String =
    sha256.chunked(8).take(groups).joinToString(" ") + "…"
