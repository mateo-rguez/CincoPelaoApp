package io.github.mateorguez.cincopelao.data.backup

import io.github.mateorguez.cincopelao.sharing.protocol.DataToSend
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/*
 * Archivo de copia de seguridad: un ZIP con el mismo contenido que se envía al compartir
 * (`DataToSend` en JSON + PNG del horario), una carpeta por instancia:
 *
 *   copia.json            índice: formato, versión, fecha y resumen de cada instancia
 *   1/datos.json          asignaturas, eventos y tareas (con notas y tareas hechas)
 *   1/horario.png         horario, si la instancia tiene
 *   2/…
 *
 * Al contrario que la compartición, la copia guarda todo: notas, eventos hechos, tareas
 * completadas y archivadas.
 */

/** Índice de la copia (`copia.json`). */
@Serializable
data class BackupManifest(
    val format: String = BackupFormat.FORMAT,
    val version: Int = BackupFormat.VERSION,
    val createdAt: Long,
    val appVersion: String,
    val instances: List<BackupInstanceInfo>
)

/** Resumen de una instancia dentro de la copia, para mostrarla antes de importar. */
@Serializable
data class BackupInstanceInfo(
    val folder: String,
    val name: String,
    val titulation: String? = null,
    val subjects: Int = 0,
    val events: Int = 0,
    val tasks: Int = 0,
    val hasSchedule: Boolean = false
)

/** Una instancia con todos sus datos, tal como entra o sale de la copia. */
class BackupInstance(
    val name: String,
    val titulation: String?,
    val data: DataToSend,
    val schedule: ByteArray?
) {
    fun info(folder: String) = BackupInstanceInfo(
        folder = folder,
        name = name,
        titulation = titulation,
        subjects = data.subjects.size,
        events = data.events.size,
        tasks = data.tasks.size,
        hasSchedule = schedule != null
    )
}

/** Copia ya leída: su índice y las instancias en el mismo orden. */
class BackupContents(val manifest: BackupManifest, val instances: List<BackupInstance>)

/** El archivo no es una copia de Cinco Pelao o está dañado; [message] se muestra tal cual. */
class BackupFormatException(message: String) : Exception(message)

object BackupFormat {
    const val FORMAT = "cincopelao-copia"
    const val VERSION = 1
    const val MIME_TYPE = "application/zip"

    private const val MANIFEST = "copia.json"
    private const val DATA = "datos.json"
    private const val SCHEDULE = "horario.png"

    /** Topes al leer, para que un archivo raro no agote la memoria. */
    private const val MAX_MANIFEST_BYTES = 1L * 1024 * 1024
    private const val MAX_DATA_BYTES = 32L * 1024 * 1024
    private const val MAX_SCHEDULE_BYTES = 32L * 1024 * 1024
    private const val MAX_TOTAL_BYTES = 256L * 1024 * 1024
    private const val MAX_ENTRIES = 1000

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Nombre de archivo propuesto: «CincoPelao-2026-11-19.zip». */
    fun suggestedFileName(date: String) = "CincoPelao-$date.zip"

    /** Escribe la copia en [out] (no lo cierra). */
    fun write(out: OutputStream, instances: List<BackupInstance>, createdAt: Long, appVersion: String) {
        val zip = ZipOutputStream(out)
        val infos = instances.mapIndexed { i, instance -> instance.info(folder = "${i + 1}") }
        val manifest = BackupManifest(createdAt = createdAt, appVersion = appVersion, instances = infos)

        zip.entry(MANIFEST, json.encodeToString(BackupManifest.serializer(), manifest).toByteArray())
        instances.zip(infos).forEach { (instance, info) ->
            zip.entry("${info.folder}/$DATA", json.encodeToString(DataToSend.serializer(), instance.data).toByteArray())
            instance.schedule?.let { zip.entry("${info.folder}/$SCHEDULE", it) }
        }
        zip.finish()
    }

    private fun ZipOutputStream.entry(name: String, bytes: ByteArray) {
        putNextEntry(ZipEntry(name))
        write(bytes)
        closeEntry()
    }

    /** Lee una copia completa de [input] (no lo cierra). */
    fun read(input: InputStream): BackupContents {
        val entries = readEntries(input)

        val manifestBytes = entries[MANIFEST] ?: throw BackupFormatException("No es una copia de Cinco Pelao")
        val manifest = decode("el índice") { json.decodeFromString(BackupManifest.serializer(), manifestBytes.decodeToString()) }
        if (manifest.format != FORMAT) throw BackupFormatException("No es una copia de Cinco Pelao")
        if (manifest.version > VERSION) {
            throw BackupFormatException("La copia es de una versión más nueva de la app; actualízala para importarla")
        }

        val instances = manifest.instances.map { info ->
            val dataBytes = entries["${info.folder}/$DATA"]
                ?: throw BackupFormatException("Faltan los datos de «${info.name}»")
            val data = decode("los datos de «${info.name}»") {
                json.decodeFromString(DataToSend.serializer(), dataBytes.decodeToString())
            }
            BackupInstance(info.name, info.titulation, data, entries["${info.folder}/$SCHEDULE"])
        }
        return BackupContents(manifest, instances)
    }

    private fun <T> decode(what: String, block: () -> T): T = try {
        block()
    } catch (e: SerializationException) {
        throw BackupFormatException("La copia está dañada: no se pueden leer $what")
    } catch (e: IllegalArgumentException) {
        throw BackupFormatException("La copia está dañada: no se pueden leer $what")
    }

    /** Entradas del ZIP que interesan (las demás se saltan), con sus topes de tamaño. */
    private fun readEntries(input: InputStream): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()
        var total = 0L
        var count = 0
        try {
            val zip = ZipInputStream(input)
            while (true) {
                val entry = zip.nextEntry ?: break
                if (++count > MAX_ENTRIES) throw BackupFormatException("La copia tiene demasiados archivos")
                val limit = when {
                    entry.isDirectory -> null
                    entry.name == MANIFEST -> MAX_MANIFEST_BYTES
                    entry.name.endsWith("/$DATA") -> MAX_DATA_BYTES
                    entry.name.endsWith("/$SCHEDULE") -> MAX_SCHEDULE_BYTES
                    else -> null
                }
                if (limit != null) {
                    val bytes = zip.readLimited(limit, entry.name)
                    total += bytes.size
                    if (total > MAX_TOTAL_BYTES) throw BackupFormatException("La copia es demasiado grande")
                    result[entry.name] = bytes
                }
                zip.closeEntry()
            }
        } catch (e: ZipException) {
            throw BackupFormatException("El archivo no es un ZIP válido")
        } catch (e: IOException) {
            throw BackupFormatException("No se ha podido leer el archivo: ${e.message ?: "error de lectura"}")
        }
        if (result.isEmpty()) throw BackupFormatException("No es una copia de Cinco Pelao")
        return result
    }

    private fun InputStream.readLimited(limit: Long, name: String): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        var read = 0L
        while (true) {
            val n = read(buffer)
            if (n < 0) break
            read += n
            if (read > limit) throw BackupFormatException("La copia está dañada: «$name» es demasiado grande")
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }
}

/**
 * Nombre con el que entra una instancia importada: el suyo si está libre y, si no,
 * «Nombre (2)», «Nombre (3)»… Los nombres ya usados en la misma importación cuentan.
 */
fun importedName(name: String, taken: Collection<String>): String {
    val base = name.trim().ifEmpty { "Instancia importada" }
    val used = taken.map { it.trim().lowercase() }.toSet()
    if (base.lowercase() !in used) return base
    var n = 2
    while ("$base ($n)".lowercase() in used) n++
    return "$base ($n)"
}
