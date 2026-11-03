package io.github.mateorguez.cincopelao.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val title = inputData.getString("title") ?: "Recordatorio"
        val message = inputData.getString("message") ?: ""
        val id = inputData.getInt("id", 0)

        NotificationUtils.showNotification(applicationContext, title, message, id)
        return Result.success()
    }
}