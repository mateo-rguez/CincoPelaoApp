package io.github.mateorguez.cincopelao

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.github.mateorguez.cincopelao.data.repository.TaskMaintenance
import io.github.mateorguez.cincopelao.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

@HiltAndroidApp
class HiltApplication : Application() {

    @Inject lateinit var taskMaintenance: TaskMaintenance
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        taskMaintenance.start(applicationScope)
    }
}
