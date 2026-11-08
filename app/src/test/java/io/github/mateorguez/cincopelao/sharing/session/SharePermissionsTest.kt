package io.github.mateorguez.cincopelao.sharing.session

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharePermissionsTest {

    @Test
    fun `Android 10 y 11 solo piden ubicacion`() {
        for (sdk in 29..30) {
            for (role in ShareRole.entries) {
                assertEquals(listOf(Manifest.permission.ACCESS_FINE_LOCATION), SharePermissions.required(sdk, role))
            }
        }
    }

    @Test
    fun `Android 12 pide Bluetooth segun el papel y ubicacion para Wi-Fi Direct`() {
        assertEquals(
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION),
            SharePermissions.required(31, ShareRole.TRANSMIT)
        )
        assertEquals(
            listOf(Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION),
            SharePermissions.required(32, ShareRole.RECEIVE)
        )
    }

    @Test
    fun `Android 13 en adelante usa dispositivos cercanos en vez de ubicacion`() {
        for (sdk in 33..36) {
            val transmit = SharePermissions.required(sdk, ShareRole.TRANSMIT)
            assertTrue(Manifest.permission.NEARBY_WIFI_DEVICES in transmit)
            assertFalse(Manifest.permission.ACCESS_FINE_LOCATION in transmit)
        }
    }

    @Test
    fun `nunca se pide INTERNET en tiempo de ejecucion`() {
        for (sdk in 29..36) for (role in ShareRole.entries) {
            assertFalse(Manifest.permission.INTERNET in SharePermissions.required(sdk, role))
        }
    }

    @Test
    fun `la ubicacion del sistema solo hace falta hasta Android 12L`() {
        assertTrue(SharePermissions.needsLocationServices(32))
        assertFalse(SharePermissions.needsLocationServices(33))
    }
}
