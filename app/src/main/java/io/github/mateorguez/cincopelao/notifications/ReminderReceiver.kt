package io.github.mateorguez.cincopelao.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import io.github.mateorguez.cincopelao.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/** Muestra un recordatorio cuando suena su alarma (la programa [ReminderAlarms]). */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SHOW) return
        NotificationUtils.showNotification(
            context,
            title = intent.getStringExtra(EXTRA_TITLE) ?: "Recordatorio",
            message = intent.getStringExtra(EXTRA_MESSAGE).orEmpty(),
            id = intent.getIntExtra(EXTRA_ID, 0)
        )
    }

    companion object {
        const val ACTION_SHOW = "io.github.mateorguez.cincopelao.SHOW_REMINDER"
        const val EXTRA_TITLE = "title"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_ID = "id"
    }
}

/**
 * Vuelve a poner las alarmas cuando el sistema las borra: al reiniciar el móvil, al actualizar la
 * app y cuando cambia el permiso de alarmas exactas. Espera a que termine la sincronización para
 * que el sistema no cierre el proceso a medias.
 */
@AndroidEntryPoint
class ReminderBootReceiver : BroadcastReceiver() {

    @Inject lateinit var reminderManager: ReminderManager
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        applicationScope.launch {
            try {
                // Un receptor tiene unos 10 s; el resto se hará la próxima vez que se abra la app
                withTimeoutOrNull(SYNC_TIMEOUT_MS) { reminderManager.syncNow() }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val SYNC_TIMEOUT_MS = 8_000L
    }
}
