package io.github.mateorguez.cincopelao.sharing.protocol

import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas con dos sockets TCP reales en el mismo ordenador: lo más parecido a dos móviles sin tenerlos. */
class ShareConnectionTest {

    private fun connectedPair(): Pair<ShareConnection, ShareConnection> {
        ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { server ->
            val client = Socket(InetAddress.getLoopbackAddress(), server.localPort)
            val accepted = server.accept()
            return ShareConnection(client) to ShareConnection(accepted)
        }
    }

    @Test
    fun `un paquete grande cruza el socket entero`() = runBlocking {
        val (a, b) = connectedPair()
        val image = ByteArray(5 * 1024 * 1024) { (it * 31).toByte() }

        val sender = launch { a.send(ShareMessage.Payload(DataToSend(), image)) }
        val received = withTimeout(10_000) { b.receive() }
        sender.join()

        assertArrayEquals(image, (received.message as ShareMessage.Payload).schedule)
        a.close(); b.close()
    }

    @Test
    fun `cancelar una espera cierra la conexion en vez de quedarse colgada`() = runBlocking {
        val (a, b) = connectedPair()

        val waiting = async { runCatching { b.receive() } }
        delay(200)
        waiting.cancel()
        withTimeout(2_000) { waiting.join() }

        assertTrue(b.isClosed)
        a.close()
    }

    @Test
    fun `cancelar un accept propaga la cancelacion y no un error de red`() = runBlocking {
        // Bug encontrado en el móvil: "Cancelar" en Recibir mostraba "Se ha perdido la conexión"
        val server = ServerSocket(0, 1, InetAddress.getLoopbackAddress())
        var caught: Throwable? = null
        val job = launch {
            try {
                cancellableBlocking(onCancel = server::close) { server.accept() }
            } catch (e: Throwable) {
                caught = e
                throw e
            }
        }
        delay(200)
        job.cancelAndJoin()

        assertTrue("Se esperaba cancelación y llegó $caught", caught is CancellationException)
        assertTrue(server.isClosed)
    }

    @Test
    fun `si el otro cuelga a mitad, se informa de la trama cortada`() = runBlocking {
        val (a, b) = connectedPair()
        val reader = async { runCatching { b.receive() } }
        a.close()

        val error = withTimeout(2_000) { reader.await() }.exceptionOrNull()
        assertTrue("Error: $error", error is ShareProtocolException.Truncated)
        b.close()
    }

    @Test
    fun `los mensajes van y vuelven en los dos sentidos`() = runBlocking {
        val (a, b) = connectedPair()

        a.send(ShareMessage.Ack("x"))
        assertEquals(ShareMessage.Ack("x"), b.receive().message)
        b.send(ShareMessage.Error("y"))
        assertEquals(ShareMessage.Error("y"), a.receive().message)
        a.close(); b.close()
    }
}
