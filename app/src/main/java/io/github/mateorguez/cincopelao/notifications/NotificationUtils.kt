package io.github.mateorguez.cincopelao.notifications

import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.mateorguez.cincopelao.MainActivity
import io.github.mateorguez.cincopelao.R

/** Botón «hecha» de un aviso de tarea, con su texto. */
data class DoneAction(val task: ReminderTaskRef, val label: String)

object NotificationUtils {

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "CincoPelao Notifications"
            val descriptionText = "Recordatorios de tareas y eventos"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("CINCOPELAO_CHANNEL", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(context: Context, title: String, message: String, id: Int = 0, doneAction: DoneAction? = null) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, "CINCOPELAO_CHANNEL")
            .setSmallIcon(R.drawable.cinco_pelao_vector)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        doneAction?.let { action ->
            val done = Intent(context, TaskDoneReceiver::class.java)
                .setAction(TaskDoneReceiver.ACTION_DONE)
                // Un intent distinto por aviso, para que cada botón lleve su tarea
                .setData(Uri.Builder().scheme("cincopelao-done").opaquePart("${action.task.instanceId}:${action.task.taskId}:$id").build())
                .putExtra(TaskDoneReceiver.EXTRA_INSTANCE, action.task.instanceId)
                .putExtra(TaskDoneReceiver.EXTRA_TASK, action.task.taskId)
                .putExtra(ReminderReceiver.EXTRA_ID, id)
            val donePending = PendingIntent.getBroadcast(
                context, id, done, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(R.drawable.cinco_pelao_vector, action.label, donePending)
        }

        // Desde Android 13 hace falta el permiso POST_NOTIFICATIONS; sin él no se muestra nada
        if (!hasNotificationPermission(context)) return

        with(NotificationManagerCompat.from(context)) {
            notify(id, builder.build())
        }
    }

    fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}
