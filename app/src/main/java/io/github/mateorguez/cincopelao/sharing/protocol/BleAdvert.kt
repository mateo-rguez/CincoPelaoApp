package io.github.mateorguez.cincopelao.sharing.protocol

import java.util.UUID

/**
 * Formato del anuncio BLE del receptor.
 *
 * El paquete de anuncio (31 bytes) lleva el UUID del servicio, y la respuesta al escaneo lleva
 * como "service data" `[versión del protocolo][nombre visible en UTF-8]`. Así el emisor ve el
 * nombre y si es compatible sin conectarse; solo lee la baliza GATT del receptor que elige.
 */
object BleAdvert {
    val SERVICE_UUID: UUID = UUID.fromString("e4a2c8aa-62f1-4b9c-8d39-57c8d908b33f")
    val BEACON_CHARACTERISTIC_UUID: UUID = UUID.fromString("e4a2c8ab-62f1-4b9c-8d39-57c8d908b33f")

    /**
     * Bytes libres para el nombre: 31 − 2 (cabecera AD) − 16 (UUID de 128 bits) − 1 (versión).
     */
    const val MAX_NAME_BYTES = 12

    fun encodeServiceData(displayName: String): ByteArray =
        byteArrayOf(ShareProtocol.VERSION.toByte()) + truncateUtf8(displayName.trim(), MAX_NAME_BYTES)

    /** Devuelve (versión, nombre) o null si el anuncio no trae datos. */
    fun decodeServiceData(bytes: ByteArray?): Pair<Int, String>? {
        if (bytes == null || bytes.isEmpty()) return null
        return (bytes[0].toInt() and 0xFF) to bytes.decodeToString(1, bytes.size)
    }

    /** Recorta a [maxBytes] bytes de UTF-8 sin partir un carácter (ni un emoji) por la mitad. */
    fun truncateUtf8(text: String, maxBytes: Int): ByteArray {
        val result = StringBuilder()
        var used = 0
        var i = 0
        while (i < text.length) {
            val codePoint = text.codePointAt(i)
            val chars = Character.charCount(codePoint)
            val size = text.substring(i, i + chars).toByteArray().size
            if (used + size > maxBytes) break
            result.append(text, i, i + chars)
            used += size
            i += chars
        }
        return result.toString().toByteArray()
    }
}
