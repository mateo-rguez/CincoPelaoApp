package com.example.cincopelao

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore de preferencias de la app y preferencia de modo oscuro.

val Context.dataStore by preferencesDataStore(name = "settings")
val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
fun Context.getDarkModeFlow(): Flow<Boolean> = dataStore.data
    .map { preferences ->
        preferences[DARK_MODE_KEY] ?: false
    }
suspend fun Context.setDarkMode(isDark: Boolean) {
    dataStore.edit { preferences ->
        preferences[DARK_MODE_KEY] = isDark
    }
}
