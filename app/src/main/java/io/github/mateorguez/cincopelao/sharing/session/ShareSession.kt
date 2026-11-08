package io.github.mateorguez.cincopelao.sharing.session

import io.github.mateorguez.cincopelao.sharing.protocol.DataToSend
import io.github.mateorguez.cincopelao.sharing.protocol.FrameInfo
import io.github.mateorguez.cincopelao.sharing.protocol.ShareConnection
import io.github.mateorguez.cincopelao.sharing.protocol.ShareMessage
import io.github.mateorguez.cincopelao.sharing.protocol.ShareProtocolException
import io.github.mateorguez.cincopelao.sharing.protocol.ShareSelection
import io.github.mateorguez.cincopelao.sharing.protocol.normalized
import java.security.MessageDigest

/** El otro móvil, tal y como se ha presentado, y el código que deben ver igual los dos. */
data class Peer(val hello: ShareMessage.Hello, val verificationCode: String)

/** Cifras de una transferencia para el panel técnico. */
data class TransferReport(val frame: FrameInfo, val durationNanos: Long) {
    /** Bytes por segundo (0 si fue instantánea). */
    val bytesPerSecond: Double
        get() = if (durationNanos <= 0) 0.0 else frame.wireBytes * 1_000_000_000.0 / durationNanos
}

class ReceivedPayload(val data: DataToSend, val schedule: ByteArray?, val report: TransferReport)

/** Recibe (etiqueta, texto) para la consola de detalles técnicos. */
typealias ShareLogger = (tag: String, message: String) -> Unit

/**
 * Código de emparejamiento de 4 cifras: sale de los nonces de los dos [ShareMessage.Hello], así que
 * solo coincide en las dos pantallas si los dos móviles están hablando entre sí.
 */
fun verificationCode(nonceA: String, nonceB: String): String {
    val (first, second) = listOf(nonceA, nonceB).sorted()
    val digest = MessageDigest.getInstance("SHA-256").digest("$first:$second".toByteArray())
    val number = ((digest[0].toInt() and 0xFF) shl 24) or ((digest[1].toInt() and 0xFF) shl 16) or
        ((digest[2].toInt() and 0xFF) shl 8) or (digest[3].toInt() and 0xFF)
    return "%04d".format((number.toLong() and 0xFFFFFFFFL) % 10_000)
}

/** Lo que se espera a que el otro cierre tras el último mensaje antes de deshacer el grupo. */
const val CLOSE_GRACE_MILLIS = 3_000L

private suspend fun ShareConnection.closeGracefully(log: ShareLogger) {
    if (awaitPeerClose(CLOSE_GRACE_MILLIS)) log("TCP", "FIN recibido: el otro extremo ha cerrado")
    else log("TCP", "El otro extremo no cerró en ${CLOSE_GRACE_MILLIS / 1000} s; se cierra igualmente")
}

private fun FrameInfo.describe(arrow: String) = "$arrow $type ${wireBytes} B crc=%08X".format(crc32)

private fun unexpected(expected: String, message: ShareMessage): Nothing =
    if (message is ShareMessage.Error) throw ShareProtocolException.Rejected(message.reason)
    else throw ShareProtocolException.Unexpected(expected, message::class.simpleName ?: "?")

/**
 * Lado emisor ("Transmitir"): saluda, espera la petición, envía los datos y espera la confirmación.
 * No sabe nada de BLE ni de Wi‑Fi Direct: solo necesita una [ShareConnection] abierta.
 */
class SenderSession(
    private val connection: ShareConnection,
    private val local: ShareMessage.Hello,
    private val log: ShareLogger = { _, _ -> },
    private val clock: () -> Long = System::nanoTime
) {
    suspend fun handshake(): Peer {
        log("TCP", connection.send(local).describe("→"))
        val frame = connection.receive()
        log("TCP", frame.info.describe("←"))
        val hello = frame.message as? ShareMessage.Hello ?: unexpected("HELLO", frame.message)
        return Peer(hello, verificationCode(local.nonce, hello.nonce))
    }

    /** Espera a que el receptor diga qué quiere. La selección llega ya corregida. */
    suspend fun awaitRequest(): ShareSelection {
        val frame = connection.receive()
        log("TCP", frame.info.describe("←"))
        val request = frame.message as? ShareMessage.Request ?: unexpected("REQUEST", frame.message)
        return request.selection.normalized()
    }

    /** Envía los datos y espera el ACK; falla si la suma SHA-256 del receptor no coincide. */
    suspend fun send(data: DataToSend, schedule: ByteArray?, onProgress: (Long, Long) -> Unit = { _, _ -> }): TransferReport {
        val start = clock()
        val sent = connection.send(ShareMessage.Payload(data, schedule), onProgress)
        log("TCP", sent.describe("→"))
        val frame = connection.receive()
        val report = TransferReport(sent, clock() - start)
        log("TCP", frame.info.describe("←"))
        val ack = frame.message as? ShareMessage.Ack ?: unexpected("ACK", frame.message)
        if (ack.sha256 != sent.sha256) {
            throw ShareProtocolException.Malformed("el receptor calculó otra suma SHA-256")
        }
        log("SHA", "sha256 verificado ${sent.sha256.take(16)}…")
        return report
    }

    /** Rechaza la petición avisando al otro lado y espera a que lo haya leído y cerrado. */
    suspend fun decline(reason: String) {
        log("TCP", connection.send(ShareMessage.Error(reason)).describe("→"))
        connection.closeGracefully(log)
    }
}

/**
 * Lado receptor ("Recibir"): espera el saludo, contesta, pide los datos, los recibe y confirma.
 */
class ReceiverSession(
    private val connection: ShareConnection,
    private val local: ShareMessage.Hello,
    private val log: ShareLogger = { _, _ -> },
    private val clock: () -> Long = System::nanoTime
) {
    suspend fun handshake(): Peer {
        val frame = connection.receive()
        log("TCP", frame.info.describe("←"))
        val hello = frame.message as? ShareMessage.Hello ?: unexpected("HELLO", frame.message)
        log("TCP", connection.send(local).describe("→"))
        return Peer(hello, verificationCode(local.nonce, hello.nonce))
    }

    /** Pide [selection] y espera los datos. Si el emisor la rechaza, lanza [ShareProtocolException.Rejected]. */
    suspend fun request(selection: ShareSelection, onProgress: (Long, Long) -> Unit = { _, _ -> }): ReceivedPayload {
        log("TCP", connection.send(ShareMessage.Request(selection.normalized())).describe("→"))
        // El cronómetro arranca con el primer bloque: antes, el emisor puede estar decidiendo si acepta
        var start = 0L
        val frame = connection.receive { done, total ->
            if (start == 0L) start = clock()
            onProgress(done, total)
        }
        val report = TransferReport(frame.info, if (start == 0L) 0 else clock() - start)
        log("TCP", frame.info.describe("←"))
        val payload = frame.message as? ShareMessage.Payload ?: unexpected("PAYLOAD", frame.message)
        log("TCP", connection.send(ShareMessage.Ack(frame.info.sha256)).describe("→"))
        // El emisor cierra al leer el ACK; hasta entonces no se puede deshacer el grupo
        connection.closeGracefully(log)
        return ReceivedPayload(payload.data, payload.schedule, report)
    }
}
