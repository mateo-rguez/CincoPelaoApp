package io.github.mateorguez.cincopelao.ui.instances

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.mateorguez.cincopelao.data.instances.DatabaseInstance
import io.github.mateorguez.cincopelao.data.instances.InstanceRepository
import io.github.mateorguez.cincopelao.data.prefs.getScheduleState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Resumen de una instancia en una línea: «Teleco · 6 asignaturas · 12 tareas · horario».
 * Lo que está vacío no se muestra.
 */
internal fun instanceSummaryText(titulation: String?, subjects: Int, pendingTasks: Int, hasSchedule: Boolean): String =
    buildList {
        titulation?.takeIf { it.isNotBlank() }?.let { add(it.trim()) }
        if (subjects > 0) add(if (subjects == 1) "1 asignatura" else "$subjects asignaturas")
        if (pendingTasks > 0) add(if (pendingTasks == 1) "1 tarea" else "$pendingTasks tareas")
        if (hasSchedule) add("horario")
    }.ifEmpty { listOf("Vacía") }.joinToString(" · ")

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InstancesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: InstanceRepository
) : ViewModel() {

    val instances: StateFlow<List<DatabaseInstance>> = repository.instances
    val currentInstanceId: StateFlow<String> = repository.currentInstanceId

    /** Resumen de cada instancia por id; se recalcula al crear, renombrar o borrar. */
    val summaries: StateFlow<Map<String, String>> = repository.instances
        .mapLatest { list -> list.associate { it.id to summaryOf(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private suspend fun summaryOf(instance: DatabaseInstance): String {
        val db = repository.database(instance.id)
        val hasSchedule = getScheduleState(context, instance.id).first().first != null
        return instanceSummaryText(instance.titulation, db.subjectDao().count(), db.taskDao().countPending(), hasSchedule)
    }

    fun select(instance: DatabaseInstance) = repository.select(instance.id)

    fun create(name: String, titulation: String?) {
        repository.create(name, titulation = titulation?.takeIf { it.isNotBlank() })
    }

    fun update(instance: DatabaseInstance, name: String, titulation: String?) =
        repository.update(instance.id, name, titulation)

    fun delete(instance: DatabaseInstance) {
        viewModelScope.launch { repository.delete(instance.id) }
    }
}
