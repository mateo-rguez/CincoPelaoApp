package io.github.mateorguez.cincopelao.data.prefs

import javax.inject.Singleton
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class NotiPrefsManager @Inject constructor(@ApplicationContext context: Context) {

    private val dataStore = context.dataStore

    // --- Claves para eventos ---
    private val EVENTS_DAYS = stringPreferencesKey("events_days")
    private val EVENTS_START_HOUR = intPreferencesKey("events_start_hour")
    private val EVENTS_END_HOUR = intPreferencesKey("events_end_hour")

    // --- Claves para tareas ---
    private val TASKS_DAYS = stringPreferencesKey("tasks_days")
    private val TASKS_START_HOUR = intPreferencesKey("tasks_start_hour")
    private val TASKS_END_HOUR = intPreferencesKey("tasks_end_hour")

    // --- Valores por defecto ---
    private fun getDefaultDays(): List<Int> = listOf(7, 3, 1)
    private fun getDefaultStartHour() = 8
    private fun getDefaultEndHour() = 22

    // --- Obtener datos de eventos ---
    val eventDaysFlow: Flow<List<Int>> = context.dataStore.data.map { prefs ->
        prefs[EVENTS_DAYS]?.split(",")?.mapNotNull { it.toIntOrNull() } ?: getDefaultDays()
    }

    val eventStartHourFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[EVENTS_START_HOUR] ?: getDefaultStartHour()
    }

    val eventEndHourFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[EVENTS_END_HOUR] ?: getDefaultEndHour()
    }

    // --- Guardar datos de eventos ---
    suspend fun saveEventConfig(days: List<Int>, startHour: Int, endHour: Int) {
        dataStore.edit { prefs ->
            prefs[EVENTS_DAYS] = days.joinToString(",")
            prefs[EVENTS_START_HOUR] = startHour
            prefs[EVENTS_END_HOUR] = endHour
        }
    }

    // --- Obtener datos de tareas ---
    val taskDaysFlow: Flow<List<Int>> = context.dataStore.data.map { prefs ->
        prefs[TASKS_DAYS]?.split(",")?.mapNotNull { it.toIntOrNull() } ?: getDefaultDays()
    }

    val taskStartHourFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[TASKS_START_HOUR] ?: getDefaultStartHour()
    }

    val taskEndHourFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[TASKS_END_HOUR] ?: getDefaultEndHour()
    }

    // --- Guardar datos de tareas ---
    suspend fun saveTaskConfig(days: List<Int>, startHour: Int, endHour: Int) {
        dataStore.edit { prefs ->
            prefs[TASKS_DAYS] = days.joinToString(",")
            prefs[TASKS_START_HOUR] = startHour
            prefs[TASKS_END_HOUR] = endHour
        }
    }

    suspend fun initializeDefaultsIfNeeded() {
        val prefsSnapshot = dataStore.data.first()

        val alreadyInitialized = prefsSnapshot.contains(EVENTS_DAYS) &&
                prefsSnapshot.contains(EVENTS_START_HOUR) &&
                prefsSnapshot.contains(EVENTS_END_HOUR) &&
                prefsSnapshot.contains(TASKS_DAYS) &&
                prefsSnapshot.contains(TASKS_START_HOUR) &&
                prefsSnapshot.contains(TASKS_END_HOUR)

        if (!alreadyInitialized) {
            dataStore.edit { prefs ->
                // Valores por defecto para eventos
                prefs[EVENTS_DAYS] = getDefaultDays().joinToString(",")
                prefs[EVENTS_START_HOUR] = getDefaultStartHour()
                prefs[EVENTS_END_HOUR] = getDefaultEndHour()

                // Valores por defecto para tareas
                prefs[TASKS_DAYS] = getDefaultDays().joinToString(",")
                prefs[TASKS_START_HOUR] = getDefaultStartHour()
                prefs[TASKS_END_HOUR] = getDefaultEndHour()
            }
        }
    }
}