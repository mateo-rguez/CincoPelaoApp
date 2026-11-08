package io.github.mateorguez.cincopelao.sharing.protocol

import kotlinx.serialization.Serializable

/**
 * Mensajes que se intercambian los dos móviles por el socket TCP, en este orden:
 *
 * ```
 *  Transmitir                         Recibir
 *      │ ── Hello ──────────────────────► │
 *      │ ◄───────────────────── Hello ─── │
 *      │ ◄─────────────────── Request ─── │   (lo que el receptor quiere)
 *      │ ── Payload ────────────────────► │   (o Error si el emisor lo rechaza)
 *      │ ◄─────────────────────── Ack ─── │   (el receptor confirma la suma SHA-256)
 * ```
 */
sealed interface ShareMessage {

    /** Presentación: quién soy y con qué versión hablo. [nonce] sirve para el código de emparejamiento. */
    @Serializable
    data class Hello(
        val protocolVersion: Int = ShareProtocol.VERSION,
        val deviceName: String,
        val model: String,
        val androidVersion: String,
        val appVersion: String,
        val nonce: String
    ) : ShareMessage

    /** El receptor pide qué datos quiere recibir. */
    @Serializable
    data class Request(val selection: ShareSelection) : ShareMessage

    /** Los datos: asignaturas, eventos y tareas en JSON y, si se pidió, la imagen del horario (PNG). */
    class Payload(val data: DataToSend, val schedule: ByteArray?) : ShareMessage

    /** El receptor confirma que ha recibido el paquete entero; [sha256] es la suma del cuerpo recibido. */
    @Serializable
    data class Ack(val sha256: String) : ShareMessage

    /** Cualquiera de los dos cancela la sesión explicando por qué. */
    @Serializable
    data class Error(val reason: String) : ShareMessage
}
