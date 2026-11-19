package io.github.mateorguez.cincopelao.notifications

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** El aviso de una tarea lleva el botón «hecha»; el de un evento, no. */
@Config(application = Application::class)
@RunWith(RobolectricTestRunner::class)
class ReminderReceiverTest {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val manager get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @Before
    fun setUp() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationUtils.createNotificationChannel(context)
    }

    private fun show(key: String) = ReminderReceiver().onReceive(
        context,
        Intent(ReminderReceiver.ACTION_SHOW)
            .putExtra(ReminderReceiver.EXTRA_TITLE, "Título")
            .putExtra(ReminderReceiver.EXTRA_MESSAGE, "Mensaje")
            .putExtra(ReminderReceiver.EXTRA_ID, key.hashCode())
            .putExtra(ReminderReceiver.EXTRA_KEY, key)
    )

    @Test
    fun `el aviso de una tarea lleva el boton de hecha`() {
        show("inst:task:7:1")

        val notification = shadowOf(manager).allNotifications.single()
        val action = notification.actions.single()
        assertTrue(action.title.toString() in ReminderPhrases.doneActions)

        val intent = shadowOf(action.actionIntent).savedIntent
        assertEquals(TaskDoneReceiver.ACTION_DONE, intent.action)
        assertEquals("inst", intent.getStringExtra(TaskDoneReceiver.EXTRA_INSTANCE))
        assertEquals(7L, intent.getLongExtra(TaskDoneReceiver.EXTRA_TASK, -1))
    }

    @Test
    fun `el aviso de un evento no lleva botones`() {
        show("inst:event:5:1")

        val notification = shadowOf(manager).allNotifications.single()
        assertTrue(notification.actions.isNullOrEmpty())
    }
}
