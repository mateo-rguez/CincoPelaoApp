package io.github.mateorguez.cincopelao.sharing.protocol

import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ReceiverBeaconTest {

    private val credentials = GroupCredentials.generate(Random(1))
    private val beacon = ReceiverBeacon(
        displayName = "Mateo",
        networkName = credentials.networkName,
        passphrase = credentials.passphrase
    )

    @Test
    fun `la baliza se codifica y decodifica igual y cabe en una lectura GATT`() {
        val bytes = beacon.encode()

        assertTrue(bytes.size <= ReceiverBeacon.MAX_BYTES)
        assertEquals(beacon, ReceiverBeacon.decode(bytes))
    }

    @Test
    fun `una baliza de otra version se rechaza`() {
        try {
            ReceiverBeacon.decode(beacon.copy(protocolVersion = 99).encode())
            fail()
        } catch (e: ShareProtocolException.IncompatibleVersion) {
            assertEquals(99, e.peerVersion)
        }
    }

    @Test
    fun `una baliza ilegible o con datos de red falsos se rechaza`() {
        val bad = listOf(
            "basura".toByteArray(),
            ByteArray(600),
            beacon.copy(networkName = "MiWifiDeCasa").encode(),
            beacon.copy(passphrase = "corta").encode(),
            beacon.copy(port = 0).encode()
        )
        bad.forEach {
            try {
                ReceiverBeacon.decode(it)
                fail("Debería rechazarse: ${it.decodeToString()}")
            } catch (_: ShareProtocolException) {
            }
        }
    }

    @Test
    fun `las credenciales generadas cumplen lo que exige Android`() {
        repeat(200) { seed ->
            val c = GroupCredentials.generate(Random(seed.toLong()))
            assertTrue(c.networkName, GroupCredentials.isValidNetworkName(c.networkName))
            assertTrue(c.passphrase, GroupCredentials.isValidPassphrase(c.passphrase))
        }
    }

    @Test
    fun `cada sesion tiene credenciales distintas`() {
        assertNotEquals(GroupCredentials.generate(), GroupCredentials.generate())
    }

    @Test
    fun `validacion de nombres de red`() {
        assertTrue(GroupCredentials.isValidNetworkName("DIRECT-ab-CincoPelao"))
        assertFalse(GroupCredentials.isValidNetworkName("DIRECT-a"))
        assertFalse(GroupCredentials.isValidNetworkName("direct-ab"))
        assertFalse(GroupCredentials.isValidNetworkName("DIRECT-ab-" + "x".repeat(30)))
    }
}
