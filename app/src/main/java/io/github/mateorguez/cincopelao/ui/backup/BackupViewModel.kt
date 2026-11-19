package io.github.mateorguez.cincopelao.ui.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.mateorguez.cincopelao.data.backup.BackupContents
import io.github.mateorguez.cincopelao.data.backup.BackupFormat
import io.github.mateorguez.cincopelao.data.backup.BackupFormatException
import io.github.mateorguez.cincopelao.data.backup.BackupInstanceInfo
import io.github.mateorguez.cincopelao.data.backup.BackupRepository
import io.github.mateorguez.cincopelao.data.instances.InstanceRepository
import io.github.mateorguez.cincopelao.data.prefs.getScheduleState
import io.github.mateorguez.cincopelao.ui.instances.instanceSummaryText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Una instancia de la app en la lista de exportar. */
data class ExportItem(val id: String, val name: String, val summary: String)

/** Copia abierta para importar: qué trae y qué instancias se han marcado (por posición). */
data class ImportPreview(
    val fileName: String,
    val createdAt: String,
    val instances: List<BackupInstanceInfo>,
    val selected: Set<Int>
)

/** Resultado de la última operación, en una tarjeta bajo su sección. */
data class BackupMessage(val text: String, val isError: Boolean)

enum class BackupWork { EXPORTING, READING, IMPORTING }

data class BackupUiState(
    val exportItems: List<ExportItem> = emptyList(),
    val exportSelected: Set<String> = emptySet(),
    val exportMessage: BackupMessage? = null,
    val importPreview: ImportPreview? = null,
    val importMessage: BackupMessage? = null,
    val work: BackupWork? = null
) {
    val canExport get() = work == null && exportItems.any { it.id in exportSelected }
    val exportCount get() = exportItems.count { it.id in exportSelected }
}

/** Resumen de una instancia de la copia: «Teleco · 6 asignaturas · 14 eventos · 9 tareas · horario». */
internal fun backupInfoSummary(info: BackupInstanceInfo): String = buildList {
    info.titulation?.takeIf { it.isNotBlank() }?.let { add(it.trim()) }
    if (info.subjects > 0) add(if (info.subjects == 1) "1 asignatura" else "${info.subjects} asignaturas")
    if (info.events > 0) add(if (info.events == 1) "1 evento" else "${info.events} eventos")
    if (info.tasks > 0) add(if (info.tasks == 1) "1 tarea" else "${info.tasks} tareas")
    if (info.hasSchedule) add("horario")
}.ifEmpty { listOf("Vacía") }.joinToString(" · ")

/** «Se han importado «A» y «B»». */
internal fun importedText(names: List<String>): String {
    val quoted = names.map { "«$it»" }
    val list = if (quoted.size == 1) quoted.single() else quoted.dropLast(1).joinToString(", ") + " y " + quoted.last()
    val verb = if (names.size == 1) "Se ha importado" else "Se han importado"
    return "$verb $list. Las tienes en Instancias."
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val instances: InstanceRepository,
    private val backup: BackupRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState())

    /** Copia abierta (con todos sus datos); la vista previa solo lleva su resumen. */
    private var openedBackup: BackupContents? = null

    /** Ids ya vistos, para marcar por defecto las instancias que aparezcan nuevas. */
    private var knownIds = emptySet<String>()

    private val exportItems = instances.instances.mapLatest { list ->
        list.map { instance ->
            val db = instances.database(instance.id)
            val hasSchedule = getScheduleState(context, instance.id).first().first != null
            ExportItem(
                instance.id,
                instance.name,
                instanceSummaryText(instance.titulation, db.subjectDao().count(), db.taskDao().countPending(), hasSchedule)
            )
        }
    }

    val uiState: StateFlow<BackupUiState> = combine(_state, exportItems) { state, items ->
        state.copy(exportItems = items)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackupUiState())

    init {
        // Todas marcadas por defecto, también las que se creen mientras la pantalla está abierta
        viewModelScope.launch {
            instances.instances.collect { list ->
                val ids = list.map { it.id }.toSet()
                _state.update { it.copy(exportSelected = (it.exportSelected intersect ids) + (ids - knownIds)) }
                knownIds = ids
            }
        }
    }

    fun toggleExport(id: String) = _state.update {
        it.copy(exportSelected = if (id in it.exportSelected) it.exportSelected - id else it.exportSelected + id, exportMessage = null)
    }

    fun suggestedFileName(): String =
        BackupFormat.suggestedFileName(SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date()))

    /** Escribe la copia en el archivo que ha creado el selector del sistema. */
    fun export(uri: Uri) {
        val state = uiState.value
        val ids = state.exportItems.map { it.id }.filter { it in state.exportSelected }
        if (ids.isEmpty() || state.work != null) return
        _state.update { it.copy(work = BackupWork.EXPORTING, exportMessage = null) }
        viewModelScope.launch {
            val message = try {
                val data = backup.collect(ids)
                val out = context.contentResolver.openOutputStream(uri, "wt")
                    ?: throw IllegalStateException("no se puede abrir el archivo")
                out.use { backup.write(it, data, appVersion()) }
                val n = data.size
                BackupMessage(
                    "${if (n == 1) "1 instancia guardada" else "$n instancias guardadas"} en «${displayName(uri) ?: "el archivo"}».",
                    isError = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error exportando la copia", e)
                // Que no quede un archivo a medias que parezca una copia buena
                runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }
                BackupMessage("No se ha podido guardar la copia: ${e.message ?: "error de escritura"}", isError = true)
            }
            _state.update { it.copy(work = null, exportMessage = message) }
        }
    }

    /** Abre una copia elegida en el selector y muestra lo que trae, todo marcado. */
    fun open(uri: Uri) {
        if (_state.value.work != null) return
        _state.update { it.copy(work = BackupWork.READING, importMessage = null, importPreview = null) }
        viewModelScope.launch {
            try {
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw BackupFormatException("No se ha podido abrir el archivo")
                val contents = input.use { backup.read(it) }
                if (contents.instances.isEmpty()) throw BackupFormatException("La copia no tiene ninguna instancia")
                openedBackup = contents
                val infos = contents.manifest.instances
                _state.update {
                    it.copy(
                        work = null,
                        importPreview = ImportPreview(
                            fileName = displayName(uri) ?: "Copia",
                            createdAt = DATE_FORMAT.format(Date(contents.manifest.createdAt)),
                            instances = infos,
                            selected = infos.indices.toSet()
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error leyendo la copia", e)
                val text = (e as? BackupFormatException)?.message ?: "No se ha podido leer el archivo: ${e.message ?: "error"}"
                _state.update { it.copy(work = null, importMessage = BackupMessage(text, isError = true)) }
            }
        }
    }

    fun toggleImport(index: Int) = _state.update { state ->
        val preview = state.importPreview ?: return@update state
        val selected = if (index in preview.selected) preview.selected - index else preview.selected + index
        state.copy(importPreview = preview.copy(selected = selected))
    }

    fun cancelImport() {
        openedBackup = null
        _state.update { it.copy(importPreview = null) }
    }

    /** Añade las instancias marcadas como nuevas. */
    fun importSelected() {
        val preview = _state.value.importPreview ?: return
        val contents = openedBackup ?: return
        val chosen = contents.instances.filterIndexed { i, _ -> i in preview.selected }
        if (chosen.isEmpty() || _state.value.work != null) return
        _state.update { it.copy(work = BackupWork.IMPORTING, importMessage = null) }
        viewModelScope.launch {
            val message = try {
                BackupMessage(importedText(backup.import(chosen)), isError = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error importando la copia", e)
                BackupMessage("No se ha podido importar: ${e.message ?: "error de la base de datos"}", isError = true)
            }
            if (!message.isError) openedBackup = null
            _state.update {
                it.copy(work = null, importMessage = message, importPreview = if (message.isError) it.importPreview else null)
            }
        }
    }

    private fun displayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

    private fun appVersion(): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"

    private companion object {
        const val TAG = "BackupViewModel"
        val DATE_FORMAT = SimpleDateFormat("d 'de' MMMM 'de' yyyy, HH:mm", Locale.forLanguageTag("es-ES"))
    }
}
