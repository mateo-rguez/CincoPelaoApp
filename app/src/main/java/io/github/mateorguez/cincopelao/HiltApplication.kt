package io.github.mateorguez.cincopelao

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.github.mateorguez.cincopelao.data.local.DatabaseManager

@HiltAndroidApp
class HiltApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DatabaseManager.initialize(this)
    }
}