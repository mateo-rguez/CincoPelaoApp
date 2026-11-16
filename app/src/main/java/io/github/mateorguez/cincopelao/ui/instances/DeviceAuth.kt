package io.github.mateorguez.cincopelao.ui.instances

import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager.Authenticators
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal

/**
 * Pide al usuario que demuestre que es el dueño del móvil (huella, cara, PIN, patrón o
 * contraseña: lo que tenga configurado) antes de algo irreversible, y llama a [onSuccess] si lo
 * consigue. Si cancela o falla, no pasa nada.
 *
 * Usa el BiometricPrompt del sistema (sin librerías ni permisos que pedir). Si el móvil no tiene
 * ningún bloqueo, no hay nada que pedir y se continúa directamente.
 */
fun requestDeviceAuth(context: Context, title: String, subtitle: String, onSuccess: () -> Unit) {
    val keyguard = context.getSystemService(KeyguardManager::class.java)
    if (keyguard?.isDeviceSecure != true) {
        onSuccess()
        return
    }

    val builder = BiometricPrompt.Builder(context)
        .setTitle(title)
        .setSubtitle(subtitle)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // WEAK incluye el desbloqueo facial de muchos móviles; si no hay biometría, PIN o patrón
        builder.setAllowedAuthenticators(Authenticators.BIOMETRIC_WEAK or Authenticators.DEVICE_CREDENTIAL)
    } else {
        @Suppress("DEPRECATION") // en Android 10 es la única forma de admitir PIN o patrón
        builder.setDeviceCredentialAllowed(true)
    }

    builder.build().authenticate(
        CancellationSignal(),
        context.mainExecutor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
        }
    )
}
