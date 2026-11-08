package io.github.mateorguez.cincopelao.sharing.protocol

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Canal de mensajes sobre un socket (o cualquier par de streams).
 *
 * Las lecturas de un socket no se pueden interrumpir, así que si la corrutina que espera un
 * mensaje se cancela, la conexión se cierra para desbloquearla: cancelar = colgar.
 */
class ShareConnection(
    input: InputStream,
    output: OutputStream,
    private val closeable: Closeable,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : Closeable {

    constructor(socket: Socket) : this(socket.getInputStream(), socket.getOutputStream(), socket)

    private val input = BufferedInputStream(input, ShareProtocol.CHUNK_BYTES)
    private val output = BufferedOutputStream(output, ShareProtocol.CHUNK_BYTES)
    private val closed = AtomicBoolean(false)

    val isClosed: Boolean get() = closed.get()

    suspend fun send(message: ShareMessage, onProgress: (Long, Long) -> Unit = { _, _ -> }): FrameInfo =
        blocking { FrameCodec.write(output, message, onProgress) }

    suspend fun receive(onProgress: (Long, Long) -> Unit = { _, _ -> }): ReceivedFrame =
        blocking { FrameCodec.read(input, onProgress) }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            runCatching { closeable.close() }
        }
    }

    /** Ejecuta [block] en el hilo de E/S; si la corrutina se cancela mientras tanto, cierra la conexión. */
    private suspend fun <T> blocking(block: () -> T): T = coroutineScope {
        val finished = AtomicBoolean(false)
        // UNDISPATCHED: el vigilante entra en el try ya, aunque la cancelación llegue enseguida
        val watchdog = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                awaitCancellation()
            } finally {
                if (!finished.get()) close()
            }
        }
        try {
            withContext(io) { block() }
        } finally {
            finished.set(true)
            watchdog.cancel()
        }
    }
}
