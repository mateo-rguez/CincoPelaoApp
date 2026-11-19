package io.github.mateorguez.cincopelao.data.backup

import androidx.room.withTransaction
import io.github.mateorguez.cincopelao.data.instances.InstanceRepository
import io.github.mateorguez.cincopelao.sharing.ShareScheduleStore
import io.github.mateorguez.cincopelao.sharing.protocol.DataToSend
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Exporta instancias a un archivo de copia y las importa como instancias nuevas. */
@Singleton
class BackupRepository @Inject constructor(
    private val instances: InstanceRepository,
    private val schedules: ShareScheduleStore
) {
    /** Lee las instancias [instanceIds] (en ese orden) con todos sus datos. */
    suspend fun collect(instanceIds: List<String>): List<BackupInstance> {
        val byId = instances.instances.value.associateBy { it.id }
        return instanceIds.mapNotNull { byId[it] }.map { instance ->
            val db = instances.database(instance.id)
            // En una transacción, para que asignaturas, eventos y tareas sean de un mismo momento
            val data = db.withTransaction {
                DataToSend(
                    subjects = db.subjectDao().getAllSubjectsOnce(),
                    events = db.eventDao().getAllEventsOnce(),
                    tasks = db.taskDao().getAllTasksOnce()
                )
            }
            BackupInstance(instance.name, instance.titulation?.takeIf { it.isNotBlank() }, data, schedules.export(instance.id))
        }
    }

    /** Escribe la copia de [backup] en [out]. */
    suspend fun write(out: OutputStream, backup: List<BackupInstance>, appVersion: String) =
        withContext(Dispatchers.IO) {
            BackupFormat.write(out, backup, createdAt = System.currentTimeMillis(), appVersion = appVersion)
            out.flush()
        }

    /** Lee una copia; lanza [BackupFormatException] si no lo es o está dañada. */
    suspend fun read(input: InputStream): BackupContents = withContext(Dispatchers.IO) { BackupFormat.read(input) }

    /**
     * Importa [selected] como instancias nuevas, sin cambiar la activa ni tocar las que hay.
     * Devuelve los nombres con los que han entrado. Si una falla, las anteriores se quedan.
     */
    suspend fun import(selected: List<BackupInstance>): List<String> {
        val taken = instances.instances.value.map { it.name }.toMutableList()
        return selected.map { instance ->
            val name = importedName(instance.name, taken)
            taken += name
            val id = instances.importAsNewInstance(name, instance.data, instance.titulation, select = false)
            instance.schedule?.let { schedules.import(id, it) }
            name
        }
    }
}
