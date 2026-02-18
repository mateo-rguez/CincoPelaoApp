package com.example.cincopelao

import android.content.Context
import java.util.UUID

object DeviceIdManager {
    private const val PREF_KEY = "device_uuid"
    private const val PREFS_NAME = "app_prefs"

    fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var id = prefs.getString(PREF_KEY, null)
        if (id == null) {
            id = UUID.randomUUID().toString().take(8)
            prefs.edit().putString(PREF_KEY, id).apply()
        }
        return id.take(4)
    }
}