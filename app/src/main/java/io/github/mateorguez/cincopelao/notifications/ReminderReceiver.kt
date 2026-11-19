package io.github.mateorguez.cincopelao.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.mateorguez.cincopelao.data.repository.TaskRepository
import io.github.mateorguez.cincopelao.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/** Muestra un recordatorio cuando suena su alarma (la programa [ReminderAlarms]). */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SHOW) return
        val key = intent.getStringExtra(EXTRA_KEY)
        NotificationUtils.showNotification(
            context,
            title = intent.getStringExtra(EXTRA_TITLE) ?: "Recordatorio",
            message = intent.getStringExtra(EXTRA_MESSAGE).orEmpty(),
            id = intent.getIntExtra(EXTRA_ID, 0),
            // Solo los avisos de tareas llevan el botón de marcarla como hecha
            doneAction = key?.let { k ->
                ReminderPlanner.taskOf(k)?.let { task -> DoneAction(task, ReminderPhrases.pick(ReminderPhrases.doneActions, emptyMap(), "$k:hecha")) }
            }
        )
    }

    companion object {
        const val ACTION_SHOW = "io.github.mateorguez.cincopelao.SHOW_REMINDER"
        const val EXTRA_TITLE = "title"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_ID = "id"
        const val EXTRA_KEY = "key"
    }
}

/**
 * Botón «hecha» de un aviso de tarea: la marca como completada (con sus subtareas) sin abrir la
 * app, quita el aviso y cancela los recordatorios que quedaban de esa tarea.
 */
@AndroidEntryPoint
class TaskDoneReceiver : BroadcastReceiver() {

    @Inject lateinit var tasks: TaskRepository
    @Inject lateinit var reminderManager: ReminderManager
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DONE) return
        val instanceId = intent.getStringExtra(EXTRA_INSTANCE) ?: return
        val taskId = intent.getLongExtra(EXTRA_TASK, -1L).takeIf { it >= 0 } ?: return
        NotificationManagerCompat.from(context).cancel(intent.getIntExtra(ReminderReceiver.EXTRA_ID, 0))

        val pending = goAsync()
        applicationScope.launch {
            try {
                tasks.completeFromReminder(instanceId, taskId)
                // Sin esperar a la sincronización habitual (va con retardo): con la app cerrada,
                // el sistema puede matar el proceso antes y quedarían puestas las alarmas que
                // tenía la tarea para otros días
                withTimeoutOrNull(SYNC_TIMEOUT_MS) { reminderManager.syncNow() }
            } catch (e: Exception) {
                Log.e(TAG, "No se ha podido marcar la tarea $taskId como hecha", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_DONE = "io.github.mateorguez.cincopelao.TASK_DONE"
        const val EXTRA_INSTANCE = "instance"
        const val EXTRA_TASK = "task"
        private const val TAG = "TaskDoneReceiver"
        private const val SYNC_TIMEOUT_MS = 8_000L
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
