package io.github.mateorguez.cincopelao.sharing.session

/**
 * En qué punto está una sesión de compartición. Lo usan las dos pantallas para el texto y el
 * icono animado; cada lado solo pasa por las fases que le tocan.
 */
sealed interface SharePhase {
    /** Faltan permisos o radios (Bluetooth, Wi‑Fi, ubicación). */
    data class NeedsSetup(val missing: List<SetupIssue>) : SharePhase

    /** Receptor: el usuario aún no ha empezado (elige el nombre visible). */
    data object Idle : SharePhase

    /** Receptor: grupo Wi‑Fi Direct creado y anunciándose por BLE. */
    data object Advertising : SharePhase

    /** Emisor: buscando receptores por BLE. */
    data object Scanning : SharePhase

    /** Emisor: leyendo la baliza GATT del receptor elegido. */
    data object ReadingBeacon : SharePhase

    /** Emisor: uniéndose al grupo Wi‑Fi Direct y abriendo el socket. */
    data object ConnectingWifi : SharePhase

    /** Los dos: intercambiando HELLO. */
    data object Handshaking : SharePhase

    /** Receptor: eligiendo qué pedir. Emisor: esperando la petición. */
    data object Choosing : SharePhase

    /** Emisor: el receptor ha pedido algo y hay que aceptar. Receptor: esperando que el emisor acepte. */
    data object AwaitingApproval : SharePhase

    /** Los dos: datos en camino. [progress] entre 0 y 1. */
    data class Transferring(val progress: Float) : SharePhase

    /** Receptor: guardando los datos en una instancia nueva. */
    data object Importing : SharePhase

    data object Done : SharePhase

    data class Failed(val message: String) : SharePhase
}

/** Lo que falta para poder compartir. */
enum class SetupIssue(val description: String) {
    PERMISSIONS("Conceder los permisos de dispositivos cercanos"),
    BLUETOOTH_OFF("Activar el Bluetooth"),
    WIFI_OFF("Activar el Wi‑Fi"),
    LOCATION_OFF("Activar la ubicación (Android la exige para Wi‑Fi Direct en esta versión)"),
    UNSUPPORTED("Este móvil no tiene Bluetooth LE o Wi‑Fi Direct")
}

/** Progreso de 0 a 1 a partir de bytes, sin dividir entre cero. */
fun progressOf(done: Long, total: Long): Float = if (total <= 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)
