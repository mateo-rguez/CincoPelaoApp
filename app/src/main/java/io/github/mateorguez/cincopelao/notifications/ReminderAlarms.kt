package io.github.mateorguez.cincopelao.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log

/**
 * Pone y quita las alarmas de los recordatorios en [AlarmManager].
 *
 * Antes se usaba WorkManager, pero se apoya en el JobScheduler y algunas ROMs (ColorOS en el
 * Realme del autor) congelan esos trabajos con la app cerrada: los avisos llegaban todos de golpe
 * al abrirla. Las alarmas son lo que usan despertadores y calendarios, y las ROMs sí las respetan.
 *
 * Son exactas si se puede: Android 10–11 no piden permiso, Android 12 da SCHEDULE_EXACT_ALARM al
 * instalar y desde Android 13 se usa USE_EXACT_ALARM, que también se concede solo. Si aun así no
 * se puede (permiso retirado a mano, ROM que lo bloquea), se pone una alarma inexacta, que el
 * sistema puede retrasar algo pero sigue avisando.
 */
class ReminderAlarms(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun arm(alarm: ReminderAlarm) {
        val reminder = alarm.reminder
        val intent = intentFor(reminder.key)
            .putExtra(ReminderReceiver.EXTRA_TITLE, reminder.title)
            .putExtra(ReminderReceiver.EXTRA_MESSAGE, reminder.message)
            .putExtra(ReminderReceiver.EXTRA_ID, reminder.key.hashCode())
        val operation = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.time, operation)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.time, operation)
            }
        } catch (e: SecurityException) {
            // Se retiró el permiso entre la comprobación y la llamada
            Log.w(TAG, "Sin permiso de alarma exacta; se pone inexacta", e)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.time, operation)
        }
    }

    fun cancel(key: String) {
        val operation = PendingIntent.getBroadcast(
            context, 0, intentFor(key), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(operation)
        operation.cancel()
    }

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /**
     * La clave va en la URI del intent: así cada recordatorio tiene su propia alarma (los extras
     * no cuentan al comparar intents) sin depender de que no choquen los hashCode.
     */
    private fun intentFor(key: String) = Intent(context, ReminderReceiver::class.java)
        .setAction(ReminderReceiver.ACTION_SHOW)
        .setData(Uri.Builder().scheme(URI_SCHEME).opaquePart(key).build())

    private companion object {
        const val TAG = "ReminderAlarms"
        const val URI_SCHEME = "cincopelao-reminder"
    }
}
