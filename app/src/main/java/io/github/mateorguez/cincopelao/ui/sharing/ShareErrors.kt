package io.github.mateorguez.cincopelao.ui.sharing

import io.github.mateorguez.cincopelao.sharing.ble.BleException
import io.github.mateorguez.cincopelao.sharing.protocol.ShareProtocolException
import io.github.mateorguez.cincopelao.sharing.wifidirect.WifiDirectException
import java.io.IOException
import kotlinx.coroutines.TimeoutCancellationException

/** Mensaje para el usuario a partir de un fallo de la sesión. */
fun describeShareError(e: Throwable): String = when (e) {
    is ShareProtocolException, is BleException, is WifiDirectException -> e.message ?: "Error de conexión"
    is TimeoutCancellationException -> "El otro móvil ha tardado demasiado en responder"
    is SecurityException -> "Falta algún permiso de dispositivos cercanos"
    is IOException -> "Se ha perdido la conexión con el otro móvil"
    else -> "Error inesperado (${e::class.simpleName})"
}
