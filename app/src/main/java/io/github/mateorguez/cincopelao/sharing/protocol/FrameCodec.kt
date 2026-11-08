package io.github.mateorguez.cincopelao.sharing.protocol

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.zip.CRC32
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Convierte [ShareMessage] en tramas binarias y al revés.
 *
 * ```
 * ┌────────┬─────────┬──────┬──────────┬────────────────┬────────┐
 * │ "5PLO" │ versión │ tipo │ longitud │ cuerpo         │ CRC32  │
 * │ 4 B    │ 1 B     │ 1 B  │ 4 B      │ longitud bytes │ 4 B    │
 * └────────┴─────────┴──────┴──────────┴────────────────┴────────┘
 * ```
 * El cuerpo es JSON salvo en [ShareMessage.Payload]: `[longitud JSON 4 B][JSON][PNG del horario]`.
 *
 * La longitud se comprueba contra un límite por tipo **antes** de reservar memoria, así que un
 * mensaje malicioso o corrupto no puede pedir un array de 2 GB.
 *
 * Funciones bloqueantes: llamarlas fuera del hilo principal.
 */
object FrameCodec {

    /** Tamaño de la cabecera (magic + versión + tipo + longitud). */
    const val HEADER_BYTES = 10
    const val TRAILER_BYTES = 4

    private const val TYPE_HELLO = 1
    private const val TYPE_REQUEST = 2
    private const val TYPE_PAYLOAD = 3
    private const val TYPE_ACK = 4
    private const val TYPE_ERROR = 5

    internal val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Escribe [message] y devuelve los datos de la trama. [onProgress] recibe (bytes del cuerpo enviados, total). */
    fun write(
        output: OutputStream,
        message: ShareMessage,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ): FrameInfo {
        val type = typeOf(message)
        val body = encodeBody(message)
        require(body.size <= limitFor(type)) { "Mensaje demasiado grande: ${body.size} bytes" }

        val crc = CRC32().apply { update(body) }.value
        val out = DataOutputStream(output)
        out.writeInt(ShareProtocol.MAGIC)
        out.writeByte(ShareProtocol.VERSION)
        out.writeByte(type)
        out.writeInt(body.size)
        var sent = 0
        while (sent < body.size) {
            val n = minOf(ShareProtocol.CHUNK_BYTES, body.size - sent)
            out.write(body, sent, n)
            sent += n
            onProgress(sent.toLong(), body.size.toLong())
        }
        out.writeInt(crc.toInt())
        out.flush()
        return FrameInfo(nameOf(type), body.size, crc, sha256(body))
    }

    /** Lee una trama completa y la valida. Lanza [ShareProtocolException] si algo no cuadra. */
    fun read(
        input: InputStream,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ): ReceivedFrame {
        val data = DataInputStream(input)
        try {
            if (data.readInt() != ShareProtocol.MAGIC) throw ShareProtocolException.NotCincoPelao()
            val version = data.readUnsignedByte()
            if (version != ShareProtocol.VERSION) throw ShareProtocolException.IncompatibleVersion(version)
            val type = data.readUnsignedByte()
            if (type !in TYPE_HELLO..TYPE_ERROR) throw ShareProtocolException.UnknownType(type)
            val length = data.readInt()
            val limit = limitFor(type)
            if (length < 0 || length > limit) throw ShareProtocolException.TooLarge(length, limit)

            val body = ByteArray(length)
            var read = 0
            while (read < length) {
                val n = data.read(body, read, minOf(ShareProtocol.CHUNK_BYTES, length - read))
                if (n < 0) throw EOFException()
                read += n
                onProgress(read.toLong(), length.toLong())
            }
            val expectedCrc = data.readInt().toLong() and 0xFFFFFFFFL
            val actualCrc = CRC32().apply { update(body) }.value
            if (expectedCrc != actualCrc) throw ShareProtocolException.Corrupted(expectedCrc, actualCrc)

            return ReceivedFrame(decodeBody(type, body), FrameInfo(nameOf(type), length, actualCrc, sha256(body)))
        } catch (e: EOFException) {
            throw ShareProtocolException.Truncated()
        }
    }

    private fun typeOf(message: ShareMessage): Int = when (message) {
        is ShareMessage.Hello -> TYPE_HELLO
        is ShareMessage.Request -> TYPE_REQUEST
        is ShareMessage.Payload -> TYPE_PAYLOAD
        is ShareMessage.Ack -> TYPE_ACK
        is ShareMessage.Error -> TYPE_ERROR
    }

    private fun nameOf(type: Int): String = when (type) {
        TYPE_HELLO -> "HELLO"
        TYPE_REQUEST -> "REQUEST"
        TYPE_PAYLOAD -> "PAYLOAD"
        TYPE_ACK -> "ACK"
        else -> "ERROR"
    }

    private fun limitFor(type: Int): Int =
        if (type == TYPE_PAYLOAD) ShareProtocol.MAX_PAYLOAD_BYTES else ShareProtocol.MAX_CONTROL_BYTES

    private fun encodeBody(message: ShareMessage): ByteArray = when (message) {
        is ShareMessage.Hello -> json.encodeToString(ShareMessage.Hello.serializer(), message).toByteArray()
        is ShareMessage.Request -> json.encodeToString(ShareMessage.Request.serializer(), message).toByteArray()
        is ShareMessage.Ack -> json.encodeToString(ShareMessage.Ack.serializer(), message).toByteArray()
        is ShareMessage.Error -> json.encodeToString(ShareMessage.Error.serializer(), message).toByteArray()
        is ShareMessage.Payload -> {
            val dataJson = json.encodeToString(DataToSend.serializer(), message.data).toByteArray()
            val schedule = message.schedule ?: ByteArray(0)
            ByteBuffer.allocate(4 + dataJson.size + schedule.size)
                .putInt(dataJson.size)
                .put(dataJson)
                .put(schedule)
                .array()
        }
    }

    private fun decodeBody(type: Int, body: ByteArray): ShareMessage = try {
        when (type) {
            TYPE_HELLO -> json.decodeFromString(ShareMessage.Hello.serializer(), body.decodeToString())
            TYPE_REQUEST -> json.decodeFromString(ShareMessage.Request.serializer(), body.decodeToString())
            TYPE_ACK -> json.decodeFromString(ShareMessage.Ack.serializer(), body.decodeToString())
            TYPE_ERROR -> json.decodeFromString(ShareMessage.Error.serializer(), body.decodeToString())
            else -> decodePayload(body)
        }
    } catch (e: SerializationException) {
        throw ShareProtocolException.Malformed(e.message ?: "JSON inválido")
    } catch (e: IllegalArgumentException) {
        throw ShareProtocolException.Malformed(e.message ?: "JSON inválido")
    }

    private fun decodePayload(body: ByteArray): ShareMessage.Payload {
        if (body.size < 4) throw ShareProtocolException.Malformed("paquete sin cabecera")
        val jsonLength = ByteBuffer.wrap(body, 0, 4).int
        if (jsonLength < 0 || jsonLength > body.size - 4) {
            throw ShareProtocolException.Malformed("longitud de JSON fuera de rango ($jsonLength)")
        }
        val data = json.decodeFromString(DataToSend.serializer(), body.decodeToString(4, 4 + jsonLength))
        val scheduleStart = 4 + jsonLength
        val schedule = if (scheduleStart < body.size) body.copyOfRange(scheduleStart, body.size) else null
        return ShareMessage.Payload(data, schedule)
    }

    internal fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}

/** Datos de una trama, para el panel de detalles técnicos y para comprobar la integridad. */
data class FrameInfo(val type: String, val bodyBytes: Int, val crc32: Long, val sha256: String) {
    /** Bytes totales en el cable, contando cabecera y CRC. */
    val wireBytes: Int get() = FrameCodec.HEADER_BYTES + bodyBytes + FrameCodec.TRAILER_BYTES
}

class ReceivedFrame(val message: ShareMessage, val info: FrameInfo)
