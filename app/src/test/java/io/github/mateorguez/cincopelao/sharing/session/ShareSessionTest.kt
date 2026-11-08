package io.github.mateorguez.cincopelao.sharing.session

import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.local.entity.Task
import io.github.mateorguez.cincopelao.sharing.protocol.DataToSend
import io.github.mateorguez.cincopelao.sharing.protocol.ShareConnection
import io.github.mateorguez.cincopelao.sharing.protocol.ShareMessage
import io.github.mateorguez.cincopelao.sharing.protocol.ShareProtocolException
import io.github.mateorguez.cincopelao.sharing.protocol.ShareSelection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Una sesión entera, emisor contra receptor, por sockets TCP locales. */
class ShareSessionTest {

    private fun hello(name: String, nonce: String) =
        ShareMessage.Hello(deviceName = name, model = "Test", androidVersion = "14", appVersion = "2.0", nonce = nonce)

    private fun connectedPair(): Pair<ShareConnection, ShareConnection> {
        ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { server ->
            val client = Socket(InetAddress.getLoopbackAddress(), server.localPort)
            return ShareConnection(client) to ShareConnection(server.accept())
        }
    }

    private val data = DataToSend(
        subjects = listOf(Subject(subjectId = 1, name = "Cálculo")),
        tasks = listOf(Task(taskId = 1, title = "Ejercicios", subjectId = 1))
    )

    @Test
    fun `sesion completa de principio a fin`() = runBlocking {
        val (senderConn, receiverConn) = connectedPair()
        val senderLog = mutableListOf<String>()
        val sender = SenderSession(senderConn, hello("Emisor", "n1"), log = { _, m -> senderLog += m })
        val receiver = ReceiverSession(receiverConn, hello("Receptor", "n2"))
        val image = ByteArray(300_000) { it.toByte() }
        val asked = ShareSelection(subjectsAndEvents = true, subjectTasks = true, schedule = true)

        val senderSide = async {
            val peer = sender.handshake()
            val selection = sender.awaitRequest()
            Triple(peer, selection, sender.send(data, image))
        }
        val receiverPeer = receiver.handshake()
        val progress = mutableListOf<Long>()
        val received = withTimeout(10_000) { receiver.request(asked) { done, _ -> progress += done } }
        val (senderPeer, selection, report) = senderSide.await()

        assertEquals("Receptor", senderPeer.hello.deviceName)
        assertEquals("Emisor", receiverPeer.hello.deviceName)
        assertEquals(senderPeer.verificationCode, receiverPeer.verificationCode)
        assertEquals(asked, selection)
        assertEquals(data, received.data)
        assertArrayEquals(image, received.schedule)
        assertEquals(report.frame.sha256, received.report.frame.sha256)
        assertTrue(progress.size > 1)
        assertTrue(senderLog.any { it.startsWith("→ PAYLOAD") })
        senderConn.close(); receiverConn.close()
    }

    @Test
    fun `una peticion incoherente llega corregida al emisor`() = runBlocking {
        val (senderConn, receiverConn) = connectedPair()
        val sender = SenderSession(senderConn, hello("E", "a"))
        val receiver = ReceiverSession(receiverConn, hello("R", "b"))

        val selection = async { sender.handshake(); sender.awaitRequest() }
        receiver.handshake()
        val pending = async { runCatching { receiver.request(ShareSelection(subjectTasks = true, generalTasks = true)) } }

        assertFalse(selection.await().subjectTasks)
        sender.decline("cancelado")
        pending.await()
        senderConn.close(); receiverConn.close()
    }

    @Test
    fun `si el emisor rechaza, el receptor recibe el motivo`() = runBlocking {
        val (senderConn, receiverConn) = connectedPair()
        val sender = SenderSession(senderConn, hello("E", "a"))
        val receiver = ReceiverSession(receiverConn, hello("R", "b"))

        val senderSide = async { sender.handshake(); sender.awaitRequest(); sender.decline("El emisor ha cancelado") }
        receiver.handshake()
        val error = runCatching { receiver.request(ShareSelection(generalTasks = true)) }.exceptionOrNull()
        senderSide.await()

        assertTrue(error is ShareProtocolException.Rejected)
        assertEquals("El emisor ha cancelado", error?.message)
        senderConn.close(); receiverConn.close()
    }

    @Test
    fun `un ACK con otra suma se detecta`() = runBlocking {
        val (senderConn, fakeReceiver) = connectedPair()
        val sender = SenderSession(senderConn, hello("E", "a"))

        val fake = async {
            fakeReceiver.receive()
            fakeReceiver.send(hello("R", "b"))
            fakeReceiver.send(ShareMessage.Request(ShareSelection(generalTasks = true)))
            fakeReceiver.receive()
            fakeReceiver.send(ShareMessage.Ack("0000"))
        }
        sender.handshake()
        sender.awaitRequest()
        val error = runCatching { sender.send(data, null) }.exceptionOrNull()
        fake.await()

        assertTrue("Error: $error", error is ShareProtocolException.Malformed)
        senderConn.close(); fakeReceiver.close()
    }

    @Test
    fun `un mensaje fuera de orden se rechaza`() = runBlocking {
        val (senderConn, fakeReceiver) = connectedPair()
        val sender = SenderSession(senderConn, hello("E", "a"))

        val fake = async { fakeReceiver.receive(); fakeReceiver.send(ShareMessage.Ack("x")) }
        val error = runCatching { sender.handshake() }.exceptionOrNull()
        fake.await()

        assertTrue(error is ShareProtocolException.Unexpected)
        senderConn.close(); fakeReceiver.close()
    }

    @Test
    fun `el codigo de emparejamiento es simetrico, de 4 cifras y depende de los nonces`() {
        assertEquals(verificationCode("a", "b"), verificationCode("b", "a"))
        assertTrue(Regex("\\d{4}").matches(verificationCode("x", "y")))
        val codes = (1..50).map { verificationCode("n$it", "m") }.toSet()
        assertTrue(codes.size > 40)
    }

    @Test
    fun `el informe calcula la velocidad`() {
        val report = TransferReport(
            io.github.mateorguez.cincopelao.sharing.protocol.FrameInfo("PAYLOAD", 999_986, 0, ""),
            durationNanos = 500_000_000
        )

        assertEquals(2_000_000.0, report.bytesPerSecond, 0.1)
        assertEquals(0.0, report.copy(durationNanos = 0).bytesPerSecond, 0.0)
    }

    @Test
    fun `progreso sin dividir entre cero`() {
        assertEquals(0f, progressOf(5, 0))
        assertEquals(0.5f, progressOf(5, 10))
        assertEquals(1f, progressOf(20, 10))
    }
}
