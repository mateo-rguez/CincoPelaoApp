package io.github.mateorguez.cincopelao.sharing.protocol

import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.data.local.entity.Subject
import io.github.mateorguez.cincopelao.data.local.entity.Task
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FrameCodecTest {

    private fun roundTrip(message: ShareMessage): ShareMessage {
        val out = ByteArrayOutputStream()
        FrameCodec.write(out, message)
        return FrameCodec.read(ByteArrayInputStream(out.toByteArray())).message
    }

    private fun encoded(message: ShareMessage): ByteArray =
        ByteArrayOutputStream().also { FrameCodec.write(it, message) }.toByteArray()

    private inline fun <reified T : Throwable> assertThrows(block: () -> Unit): T {
        try {
            block()
        } catch (e: Throwable) {
            if (e is T) return e
            throw AssertionError("Se esperaba ${T::class.simpleName} y saltó ${e::class.simpleName}: ${e.message}", e)
        }
        fail("Se esperaba ${T::class.simpleName} y no saltó nada")
        throw IllegalStateException()
    }

    private val hello = ShareMessage.Hello(
        deviceName = "Móvil de Mateo", model = "RMX3363", androidVersion = "14", appVersion = "2.0", nonce = "abc123"
    )

    @Test
    fun `los mensajes de control llegan iguales`() {
        assertEquals(hello, roundTrip(hello))
        val request = ShareMessage.Request(ShareSelection(subjectsAndEvents = true, generalTasks = true))
        assertEquals(request, roundTrip(request))
        assertEquals(ShareMessage.Ack("ff00"), roundTrip(ShareMessage.Ack("ff00")))
        assertEquals(ShareMessage.Error("no"), roundTrip(ShareMessage.Error("no")))
    }

    @Test
    fun `el paquete de datos llega con asignaturas, eventos, tareas e imagen`() {
        val data = DataToSend(
            subjects = listOf(Subject(subjectId = 1, name = "Cálculo", course = "1º", abbrev = "CAL")),
            events = listOf(Event(eventId = 2, subjectId = 1, name = "Parcial", type = "Examen", weight = 40f, date = 1_700_000_000_000)),
            tasks = listOf(Task(taskId = 3, title = "Ejercicios 🧮", subjectId = 1))
        )
        val image = ByteArray(100_000) { (it % 251).toByte() }

        val received = roundTrip(ShareMessage.Payload(data, image)) as ShareMessage.Payload

        assertEquals(data, received.data)
        assertArrayEquals(image, received.schedule)
    }

    @Test
    fun `un paquete sin horario llega sin imagen`() {
        val received = roundTrip(ShareMessage.Payload(DataToSend(), null)) as ShareMessage.Payload

        assertNull(received.schedule)
    }

    @Test
    fun `el progreso avanza por bloques hasta el total`() {
        val payload = ShareMessage.Payload(DataToSend(), ByteArray(50_000))
        val sent = mutableListOf<Long>()
        val bytes = ByteArrayOutputStream().also { out -> FrameCodec.write(out, payload) { done, _ -> sent += done } }.toByteArray()
        val received = mutableListOf<Long>()
        val frame = FrameCodec.read(ByteArrayInputStream(bytes)) { done, _ -> received += done }

        assertTrue(sent.size > 1)
        assertEquals(sent.last(), frame.info.bodyBytes.toLong())
        assertEquals(sent, received)
        assertEquals(bytes.size, frame.info.wireBytes)
    }

    @Test
    fun `emisor y receptor calculan la misma suma SHA-256`() {
        val out = ByteArrayOutputStream()
        val sentInfo = FrameCodec.write(out, ShareMessage.Payload(DataToSend(), ByteArray(10) { 7 }))
        val receivedInfo = FrameCodec.read(ByteArrayInputStream(out.toByteArray())).info

        assertEquals(sentInfo, receivedInfo)
        assertEquals(64, receivedInfo.sha256.length)
    }

    @Test
    fun `datos que no son de Cinco Pelao se rechazan`() {
        val http = "GET / HTTP/1.1\r\n\r\n".toByteArray()

        assertThrows<ShareProtocolException.NotCincoPelao> { FrameCodec.read(ByteArrayInputStream(http)) }
    }

    @Test
    fun `otra version del protocolo se rechaza diciendo cual es`() {
        val bytes = encoded(hello).also { it[4] = 9 }

        val e = assertThrows<ShareProtocolException.IncompatibleVersion> { FrameCodec.read(ByteArrayInputStream(bytes)) }
        assertEquals(9, e.peerVersion)
    }

    @Test
    fun `un tipo desconocido se rechaza`() {
        val bytes = encoded(hello).also { it[5] = 42 }

        assertThrows<ShareProtocolException.UnknownType> { FrameCodec.read(ByteArrayInputStream(bytes)) }
    }

    @Test
    fun `una longitud enorme se rechaza antes de reservar memoria`() {
        val bytes = ByteArrayOutputStream().also {
            DataOutputStream(it).apply {
                writeInt(ShareProtocol.MAGIC); writeByte(ShareProtocol.VERSION); writeByte(3); writeInt(Int.MAX_VALUE)
            }
        }.toByteArray()

        val e = assertThrows<ShareProtocolException.TooLarge> { FrameCodec.read(ByteArrayInputStream(bytes)) }
        assertEquals(ShareProtocol.MAX_PAYLOAD_BYTES, e.limit)
    }

    @Test
    fun `un mensaje de control no puede ser tan grande como un paquete`() {
        val bytes = ByteArrayOutputStream().also {
            DataOutputStream(it).apply {
                writeInt(ShareProtocol.MAGIC); writeByte(ShareProtocol.VERSION); writeByte(1); writeInt(1_000_000)
            }
        }.toByteArray()

        assertThrows<ShareProtocolException.TooLarge> { FrameCodec.read(ByteArrayInputStream(bytes)) }
    }

    @Test
    fun `un byte cambiado por el camino se detecta con el CRC`() {
        val bytes = encoded(hello)
        bytes[FrameCodec.HEADER_BYTES + 3] = (bytes[FrameCodec.HEADER_BYTES + 3] + 1).toByte()

        assertThrows<ShareProtocolException.Corrupted> { FrameCodec.read(ByteArrayInputStream(bytes)) }
    }

    @Test
    fun `una trama cortada a medias se detecta`() {
        val bytes = encoded(ShareMessage.Payload(DataToSend(), ByteArray(40_000)))

        for (cut in listOf(2, FrameCodec.HEADER_BYTES, bytes.size / 2, bytes.size - 1)) {
            assertThrows<ShareProtocolException.Truncated> {
                FrameCodec.read(ByteArrayInputStream(bytes.copyOf(cut)))
            }
        }
    }

    @Test
    fun `un JSON invalido con CRC correcto se rechaza como mal formado`() {
        val body = "{esto no es json".toByteArray()
        val crc = java.util.zip.CRC32().apply { update(body) }.value
        val bytes = ByteArrayOutputStream().also {
            DataOutputStream(it).apply {
                writeInt(ShareProtocol.MAGIC); writeByte(ShareProtocol.VERSION); writeByte(1)
                writeInt(body.size); write(body); writeInt(crc.toInt())
            }
        }.toByteArray()

        assertThrows<ShareProtocolException.Malformed> { FrameCodec.read(ByteArrayInputStream(bytes)) }
    }

    @Test
    fun `un paquete con longitud de JSON imposible se rechaza`() {
        val body = java.nio.ByteBuffer.allocate(8).putInt(1000).putInt(0).array()
        val crc = java.util.zip.CRC32().apply { update(body) }.value
        val bytes = ByteArrayOutputStream().also {
            DataOutputStream(it).apply {
                writeInt(ShareProtocol.MAGIC); writeByte(ShareProtocol.VERSION); writeByte(3)
                writeInt(body.size); write(body); writeInt(crc.toInt())
            }
        }.toByteArray()

        assertThrows<ShareProtocolException.Malformed> { FrameCodec.read(ByteArrayInputStream(bytes)) }
    }

    @Test
    fun `varios mensajes seguidos se leen uno a uno`() {
        val out = ByteArrayOutputStream()
        FrameCodec.write(out, hello)
        FrameCodec.write(out, ShareMessage.Ack("1"))
        val input = ByteArrayInputStream(out.toByteArray())

        assertEquals(hello, FrameCodec.read(input).message)
        assertEquals(ShareMessage.Ack("1"), FrameCodec.read(input).message)
    }
}
