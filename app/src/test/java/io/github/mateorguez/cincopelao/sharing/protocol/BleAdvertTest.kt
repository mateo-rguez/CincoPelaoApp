package io.github.mateorguez.cincopelao.sharing.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BleAdvertTest {

    @Test
    fun `el nombre y la version viajan en el anuncio`() {
        val bytes = BleAdvert.encodeServiceData("Mateo")

        assertEquals(ShareProtocol.VERSION to "Mateo", BleAdvert.decodeServiceData(bytes))
    }

    @Test
    fun `un nombre largo se recorta y el anuncio cabe en 31 bytes`() {
        val bytes = BleAdvert.encodeServiceData("Un nombre larguísimo de verdad")

        assertTrue(bytes.size <= 1 + BleAdvert.MAX_NAME_BYTES)
        assertEquals(31, 2 + 16 + 1 + BleAdvert.MAX_NAME_BYTES)
    }

    @Test
    fun `no se parte un caracter multibyte ni un emoji`() {
        assertEquals("ñññññ", BleAdvert.truncateUtf8("ññññññ", 11).decodeToString())
        assertEquals("ab🎓", BleAdvert.truncateUtf8("ab🎓🎓", 7).decodeToString())
        assertEquals("ab", BleAdvert.truncateUtf8("ab🎓", 5).decodeToString())
    }

    @Test
    fun `un anuncio sin datos no se decodifica`() {
        assertNull(BleAdvert.decodeServiceData(null))
        assertNull(BleAdvert.decodeServiceData(ByteArray(0)))
    }
}
