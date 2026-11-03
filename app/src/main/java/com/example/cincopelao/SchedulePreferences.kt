package com.example.cincopelao

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Persistencia del horario importado (archivo, zoom y desplazamiento) por instancia.

object ScheduleSettingsKeys {
    fun fileUriKey(instanceId: String) = stringPreferencesKey("file_uri_$instanceId")
    fun zoomKey(instanceId: String) = floatPreferencesKey("zoom_$instanceId")
    fun offsetXKey(instanceId: String) = floatPreferencesKey("offset_x_$instanceId")
    fun offsetYKey(instanceId: String) = floatPreferencesKey("offset_y_$instanceId")
}

suspend fun saveScheduleState(
    context: Context,
    instanceId: String,
    fileUriString: String?,
    zoom: Float,
    offset: Offset
) {
    context.dataStore.edit { prefs ->
        fileUriString?.let { prefs[ScheduleSettingsKeys.fileUriKey(instanceId)] = it }
        prefs[ScheduleSettingsKeys.zoomKey(instanceId)] = zoom
        prefs[ScheduleSettingsKeys.offsetXKey(instanceId)] = offset.x
        prefs[ScheduleSettingsKeys.offsetYKey(instanceId)] = offset.y
    }
}

fun getScheduleState(context: Context, instanceId: String): Flow<Triple<String?, Float, Offset>> {
    return context.dataStore.data.map { prefs ->
        val uriString = prefs[ScheduleSettingsKeys.fileUriKey(instanceId)]
        val zoom = prefs[ScheduleSettingsKeys.zoomKey(instanceId)] ?: 1f
        val offsetX = prefs[ScheduleSettingsKeys.offsetXKey(instanceId)] ?: 0f
        val offsetY = prefs[ScheduleSettingsKeys.offsetYKey(instanceId)] ?: 0f
        Triple(uriString, zoom, Offset(offsetX, offsetY))
    }
}
