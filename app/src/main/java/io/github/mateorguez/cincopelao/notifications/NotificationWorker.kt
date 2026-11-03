package io.github.mateorguez.cincopelao.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** Muestra un recordatorio programado por [ReminderManager]. */
class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: "Recordatorio"
        val message = inputData.getString(KEY_MESSAGE) ?: ""
        val id = inputData.getInt(KEY_ID, 0)

        NotificationUtils.showNotification(applicationContext, title, message, id)
        return Result.success()
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_MESSAGE = "message"
        const val KEY_ID = "id"
    }
}
