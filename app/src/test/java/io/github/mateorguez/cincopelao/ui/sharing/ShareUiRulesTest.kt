package io.github.mateorguez.cincopelao.ui.sharing

import io.github.mateorguez.cincopelao.sharing.ble.ScanSighting
import io.github.mateorguez.cincopelao.sharing.protocol.ShareProtocol
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareUiRulesTest {

    private fun sighting(address: String, rssi: Int, time: Long, version: Int = ShareProtocol.VERSION) =
        ScanSighting(address, rssi, txPower = null, protocolVersion = version, displayName = "R-$address", timestampMillis = time)

    @Test
    fun `un receptor nuevo se anade y uno conocido se actualiza sin cambiar de sitio`() {
        val list = emptyList<NearbyReceiver>()
            .withSighting(sighting("B", -60, 0))
            .withSighting(sighting("A", -70, 0))
            .withSighting(sighting("B", -30, 100))

        assertEquals(listOf("B", "A"), list.map { it.address })
        assertEquals(2, list[0].sightings)
        assertEquals(100, list[0].lastSeenMillis)
    }

    @Test
    fun `el RSSI se suaviza`() {
        val list = emptyList<NearbyReceiver>().withSighting(sighting("A", -60, 0)).withSighting(sighting("A", -30, 1))

        assertEquals(-50, list.single().rssi)
    }

    @Test
    fun `los receptores que dejan de verse desaparecen`() {
        val list = emptyList<NearbyReceiver>().withSighting(sighting("A", -60, 0)).withSighting(sighting("B", -60, 4_000))

        assertEquals(listOf("B"), list.withoutStale(nowMillis = 6_000).map { it.address })
    }

    @Test
    fun `un receptor con otra version se marca como incompatible`() {
        val list = emptyList<NearbyReceiver>().withSighting(sighting("A", -60, 0, version = 9))

        assertFalse(list.single().isCompatible)
    }

    @Test
    fun `la consola se limita a las ultimas lineas`() {
        var log = emptyList<LogLine>()
        repeat(250) { log = log.appendCapped(LogLine(it.toLong(), "BLE", "línea $it")) }

        assertEquals(MAX_LOG_LINES, log.size)
        assertEquals("línea 249", log.last().text)
    }

    @Test
    fun `formato de una linea de la consola`() {
        assertEquals("00:00:01.250 TCP  → HELLO", LogLine(1_250, "TCP", "→ HELLO").format(ZoneOffset.UTC))
    }

    @Test
    fun `las filas tecnicas se actualizan sin perder el orden`() {
        val info = emptyMap<TechSection, Map<String, String>>()
            .with(TechSection.BLE, "MAC", "1")
            .with(TechSection.BLE, "RSSI", "2")
            .with(TechSection.BLE, "MAC", "3")

        assertEquals(listOf("MAC" to "3", "RSSI" to "2"), info.getValue(TechSection.BLE).toList())
    }

    @Test
    fun `bytes y velocidad legibles`() {
        assertEquals("512 B", formatBytes(512))
        assertEquals("1,5 KB", formatBytes(1536))
        assertEquals("2,00 MB", formatBytes(2 * 1024 * 1024))
        assertEquals("—", formatRate(0.0))
        assertTrue(formatRate(1_000_000.0).endsWith("8,0 Mbit/s"))
    }

    @Test
    fun `canales y bandas Wi-Fi`() {
        assertEquals(1, wifiChannel(2412))
        assertEquals(6, wifiChannel(2437))
        assertEquals(14, wifiChannel(2484))
        assertEquals(36, wifiChannel(5180))
        assertEquals(149, wifiChannel(5745))
        assertEquals(5, wifiChannel(5975))
        assertNull(wifiChannel(1000))
        assertEquals("5 GHz · canal 149 (5745 MHz)", describeFrequency(5745))
        assertEquals("2,4 GHz", wifiBand(2437))
    }

    @Test
    fun `barras y distancia a partir del RSSI`() {
        assertEquals(4, signalBars(-40))
        assertEquals(2, signalBars(-75))
        assertEquals(0, signalBars(-100))
        assertEquals(1.0, estimateDistanceMeters(-59), 0.001)
        assertEquals(10.0, estimateDistanceMeters(-79), 0.001)
        assertEquals("~1,0 m", formatDistance(1.0))
        assertEquals("~50 cm", formatDistance(0.5))
    }

    @Test
    fun `hash abreviado`() {
        assertEquals("01234567 89abcdef…", shortHash("0123456789abcdef0123"))
    }
}
