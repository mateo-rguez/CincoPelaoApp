package io.github.mateorguez.cincopelao

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.github.mateorguez.cincopelao.data.prefs.NotiPrefsManager
import io.github.mateorguez.cincopelao.data.prefs.ThemeRepository
import io.github.mateorguez.cincopelao.data.repository.TaskMaintenance
import io.github.mateorguez.cincopelao.di.ApplicationScope
import io.github.mateorguez.cincopelao.notifications.NotificationUtils
import io.github.mateorguez.cincopelao.notifications.ReminderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class HiltApplication : Application() {

    @Inject lateinit var taskMaintenance: TaskMaintenance
    @Inject lateinit var reminderManager: ReminderManager
    @Inject lateinit var notiPrefs: NotiPrefsManager
    @Inject lateinit var themeRepository: ThemeRepository // se crea ya para empezar a leer la apariencia
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // El canal debe existir antes de que un recordatorio intente mostrarse
        NotificationUtils.createNotificationChannel(this)
        applicationScope.launch { notiPrefs.initializeDefaultsIfNeeded() }
        taskMaintenance.start(applicationScope)
        reminderManager.start(applicationScope)
    }
}
