package io.github.mateorguez.cincopelao.data.instances

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.mateorguez.cincopelao.data.local.AppDatabase
import io.github.mateorguez.cincopelao.data.repository.parentsFirst
import io.github.mateorguez.cincopelao.data.repository.withoutDanglingReferences
import io.github.mateorguez.cincopelao.sharing.protocol.DataToSend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** Una instancia es un conjunto independiente de datos (p. ej. una carrera o un curso). */
data class DatabaseInstance(
    val id: String,
    val name: String,
    val titulation: String? = null
)

/**
 * Gestiona las instancias: cuáles existen, cuál está activa y la base de datos de cada una.
 *
 * Sustituye al antiguo `DatabaseManager` global. Mantiene el mismo formato de guardado
 * (SharedPreferences "db_manager") para conservar los datos existentes.
 */
@Singleton
class InstanceRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Bases de datos ya construidas, por id de instancia. Se crean bajo demanda. */
    private val databases = ConcurrentHashMap<String, AppDatabase>()

    private val _instances = MutableStateFlow(loadInstances())
    val instances: StateFlow<List<DatabaseInstance>> = _instances.asStateFlow()

    private val _currentInstanceId: MutableStateFlow<String>
    val currentInstanceId: StateFlow<String>

    init {
        if (_instances.value.isEmpty()) {
            _instances.value = listOf(DatabaseInstance(id = DEFAULT_ID, name = DEFAULT_NAME))
            saveInstances()
        }
        val savedId = prefs.getString(KEY_CURRENT_ID, null)
        val initialId = _instances.value.find { it.id == savedId }?.id ?: _instances.value.first().id
        _currentInstanceId = MutableStateFlow(initialId)
        currentInstanceId = _currentInstanceId.asStateFlow()
    }

    /** Instancia activa (o null si se acaba de borrar de la lista). */
    val currentInstance: Flow<DatabaseInstance?> =
        combine(_instances, _currentInstanceId) { list, id -> list.find { it.id == id } }
            .distinctUntilChanged()

    /** Base de datos de la instancia activa; emite de nuevo al cambiar de instancia. */
    val currentDatabase: Flow<AppDatabase> = _currentInstanceId.map { database(it) }

    /** Base de datos de la instancia activa en este momento. */
    fun currentDb(): AppDatabase = database(_currentInstanceId.value)

    /** Base de datos de una instancia concreta (activa o no). */
    fun database(id: String): AppDatabase =
        databases.getOrPut(id) { AppDatabase.build(context, id) }

    fun select(instanceId: String) {
        if (_instances.value.none { it.id == instanceId }) return
        _currentInstanceId.value = instanceId
        prefs.edit { putString(KEY_CURRENT_ID, instanceId) }
    }

    fun create(name: String, setCurrent: Boolean = false): DatabaseInstance {
        val instance = DatabaseInstance(id = UUID.randomUUID().toString(), name = name)
        _instances.update { it + instance }
        saveInstances()
        if (setCurrent) select(instance.id)
        return instance
    }

    /** Cambia nombre y titulación. Si la instancia ya no existe, no hace nada. */
    fun update(instanceId: String, name: String, titulation: String?) {
        _instances.update { list ->
            list.map { if (it.id == instanceId) it.copy(name = name, titulation = titulation) else it }
        }
        saveInstances()
    }

    /** Borra una instancia con todos sus datos. La instancia activa no se puede borrar. */
    suspend fun delete(instanceId: String) {
        if (instanceId == _currentInstanceId.value) return
        // Primero se quita de la lista, para que quien la esté observando deje de hacerlo
        _instances.update { list -> list.filterNot { it.id == instanceId } }
        saveInstances()
        withContext(Dispatchers.IO) {
            databases.remove(instanceId)?.close()
            context.deleteDatabase(instanceId)
            File(context.filesDir, "SCHEDULE_$instanceId").deleteRecursively()
        }
    }

    /**
     * Crea una instancia nueva con los datos recibidos y la activa.
     * Si la importación falla, no queda ninguna instancia a medias.
     */
    suspend fun importAsNewInstance(name: String, data: DataToSend): String {
        val id = UUID.randomUUID().toString()
        val clean = data.withoutDanglingReferences()
        val db = database(id)
        try {
            db.withTransaction {
                clean.subjects.forEach { db.subjectDao().insert(it) }
                db.eventDao().insertEvents(clean.events)
                clean.tasks.parentsFirst().forEach { db.taskDao().insertTask(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error importando la instancia '$name'", e)
            withContext(Dispatchers.IO) {
                databases.remove(id)?.close()
                context.deleteDatabase(id)
            }
            throw e
        }
        _instances.update { it + DatabaseInstance(id = id, name = name) }
        saveInstances()
        select(id)
        return id
    }

    private fun loadInstances(): List<DatabaseInstance> {
        val json = prefs.getString(KEY_INSTANCES_JSON, null) ?: return emptyList()
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            DatabaseInstance(
                id = obj.getString("id"),
                name = obj.getString("name"),
                titulation = obj.optString("titulation", "")
            )
        }
    }

    private fun saveInstances() {
        val array = JSONArray()
        _instances.value.forEach { instance ->
            array.put(
                JSONObject()
                    .put("id", instance.id)
                    .put("name", instance.name)
                    .put("titulation", instance.titulation)
            )
        }
        prefs.edit { putString(KEY_INSTANCES_JSON, array.toString()) }
    }

    private companion object {
        const val TAG = "InstanceRepository"
        const val PREFS_NAME = "db_manager"
        const val KEY_INSTANCES_JSON = "db_instances_json"
        const val KEY_CURRENT_ID = "current_db_id"
        const val DEFAULT_ID = "cinco_pelao_db"
        const val DEFAULT_NAME = "Instancia por defecto"
    }
}
