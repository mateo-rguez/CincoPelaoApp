package io.github.mateorguez.cincopelao.sharing.session

import android.Manifest

/** Papel del móvil en la compartición. */
enum class ShareRole { TRANSMIT, RECEIVE }

/**
 * Permisos de tiempo de ejecución que hacen falta según la versión de Android (minSdk 29).
 *
 * - Android 12+ (31): Bluetooth tiene permisos propios (buscar, anunciar, conectar).
 * - Android 13+ (33): Wi‑Fi Direct usa NEARBY_WIFI_DEVICES en lugar de la ubicación.
 * - Hasta Android 12L (32), Wi‑Fi Direct necesita ubicación precisa; hasta Android 11 (30),
 *   también el escaneo BLE.
 *
 * INTERNET no está aquí: es un permiso normal que se concede al instalar.
 */
object SharePermissions {

    fun required(sdkInt: Int, role: ShareRole): List<String> = buildList {
        if (sdkInt >= 31) {
            add(if (role == ShareRole.TRANSMIT) Manifest.permission.BLUETOOTH_SCAN else Manifest.permission.BLUETOOTH_ADVERTISE)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (sdkInt >= 33) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    /** Hasta Android 12L, Wi‑Fi Direct no funciona con la ubicación del sistema apagada. */
    fun needsLocationServices(sdkInt: Int): Boolean = sdkInt <= 32

    /** Nombre corto para explicar al usuario qué permiso falta. */
    fun label(permission: String): String = when (permission) {
        Manifest.permission.BLUETOOTH_SCAN -> "Buscar dispositivos Bluetooth"
        Manifest.permission.BLUETOOTH_ADVERTISE -> "Hacerse visible por Bluetooth"
        Manifest.permission.BLUETOOTH_CONNECT -> "Conectar por Bluetooth"
        Manifest.permission.NEARBY_WIFI_DEVICES -> "Dispositivos Wi‑Fi cercanos"
        Manifest.permission.ACCESS_FINE_LOCATION -> "Ubicación (Wi‑Fi Direct la exige en esta versión)"
        else -> permission.substringAfterLast('.')
    }
}
