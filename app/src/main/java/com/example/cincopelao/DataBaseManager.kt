package com.example.cincopelao

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.room.Room
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object DatabaseManager {
    // Lista de instancias creadas
    val instances = mutableStateListOf<DatabaseInstance>()

    // Base de datos actual (principal)
    var currentDb: AppDatabase? by mutableStateOf(null)
        private set

    // Devuelve todas las instancias
    fun getAllInstances(): List<DatabaseInstance> = instances.toList()

    // Devuelve la instancia actualmente activa
    fun getCurrentInstance(): DatabaseInstance? = instances.find { it.db == currentDb }

    // Inicializa currentDb al arrancar
    fun initialize(context: Context) {

        loadInstances(context)

        if (instances.isEmpty()) {
            val db = AppDatabase.getDatabase(context)

            val instance = DatabaseInstance(
                name = "Instancia por defecto",
                db = db,
                id = "cinco_pelao_db"
            )

            instances.add(instance)
            setCurrentDb(context, instance)
            saveInstances(context)
        } else {
            val prefs = context.getSharedPreferences("db_manager", Context.MODE_PRIVATE)
            val currentId = prefs.getString("current_db_id", null)
            val savedInstance = instances.find { it.id == currentId }

            currentDb = savedInstance?.db ?: instances.first().db
        }
    }

    fun createDatabase(context: Context, dbName: String): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            dbName
        ).build()
    }

    fun createInstance(context: Context, name: String, setCurrent: Boolean) {
        val id = UUID.randomUUID().toString()
        val newDb = createDatabase(context, id)

        val instance = DatabaseInstance(name = name, db = newDb, id = id)
        instances.add(instance)
        if (setCurrent) setCurrentDb(context, instance)

        saveInstances(context)
    }

    fun renameInstance(context: Context, oldInstance: DatabaseInstance, newName: String, newTitulation: String?) {
        val index = instances.indexOf(oldInstance)
        if (index != -1) {
            instances[index] = instances[index].copy(name = newName)
        }
        instances[index] = instances[index].copy(titulation = newTitulation)
        saveInstances(context)
    }

    fun deleteInstanceFromList(instanceId: String) {
        instances.removeIf { it.id == instanceId }
    }

    // Crea o importa una nueva instancia
    suspend fun importAsNewDatabase(
        context: Context,
        name: String,
        subjects: List<Subject>,
        events: List<Event>,
        tasks: List<Task>
    ): String {
        val id = UUID.randomUUID().toString()
        val newDb = createDatabase(context, id)

        val instance = DatabaseInstance(name = name, db = newDb, id = id)
        instances.add(instance)
        setCurrentDb(context, instance)

        saveInstances(context)

        newDb.withTransaction {
            subjects.forEach { subj ->
                newDb.subjectDao().insert(subj)
            }

            events.forEach { ev ->
                newDb.eventDao().insertEvent(ev)
            }

            tasks.filter { it.parentTaskId == null }.forEach { task ->
                newDb.taskDao().insertTask(task)
            }

            tasks.filter { it.parentTaskId != null }.forEach { task ->
                newDb.taskDao().insertTask(task)
            }
        }

        return id
    }


    // Cambia la base de datos principal
    fun setCurrentDb(context: Context, instance: DatabaseInstance) {
        currentDb = instance.db
        val prefs = context.getSharedPreferences("db_manager", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("current_db_id", instance.id)
            .apply()
    }

    fun saveInstances(context: Context) {
        val prefs = context.getSharedPreferences("db_manager", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        val jsonArray = JSONArray()
        instances.forEach { instance ->
            val obj = JSONObject()
            obj.put("id", instance.id)
            obj.put("name", instance.name)
            obj.put("titulation", instance.titulation)
            jsonArray.put(obj)
        }

        editor.putString("db_instances_json", jsonArray.toString())
        editor.apply()
    }

    fun loadInstances(context: Context) {
        val prefs = context.getSharedPreferences("db_manager", Context.MODE_PRIVATE)
        val jsonString = prefs.getString("db_instances_json", null)

        instances.clear()

        if (jsonString.isNullOrEmpty()) {
            val oldNames = prefs.getStringSet("db_instances", emptySet()) ?: emptySet()
            if (oldNames.isNotEmpty()) {
                oldNames.forEach { dbName ->
                    val id = UUID.randomUUID().toString()

                    // Renombramos físicamente el archivo para mantener datos
                    val oldFile = context.getDatabasePath(dbName)
                    val newFile = context.getDatabasePath(id)

                    if (oldFile.exists()) {
                        oldFile.renameTo(newFile)
                    }

                    val db = createDatabase(context, id)
                    instances.add(DatabaseInstance(id = id, name = dbName, db = db))
                }
                saveInstances(context)
            }
            return
        }

        // Si ya hay JSON, cargamos normalmente
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val id = obj.getString("id")
            val name = obj.getString("name")
            val titulation = obj.optString("titulation", "")

            val db = createDatabase(context, id)
            instances.add(DatabaseInstance(id = id, name = name, db = db, titulation = titulation))
        }

        if (instances.isNotEmpty() && currentDb == null) {
            currentDb = instances.first().db
        }
    }

    data class DatabaseInstance(
        val name: String,
        val db: AppDatabase,
        val id: String,
        val titulation: String? = null
    )
}