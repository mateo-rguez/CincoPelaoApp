package io.github.mateorguez.cincopelao.data.repository

import android.util.Log
import io.github.mateorguez.cincopelao.data.instances.InstanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ejecuta el mantenimiento de tareas ([TaskRepository.runMaintenance]) al arrancar la app,
 * al cambiar de instancia y cada medianoche mientras la app sigue viva.
 *
 * Antes lo hacía cada MainViewModel, así que podía haber varias copias funcionando a la vez.
 */
@Singleton
class TaskMaintenance @Inject constructor(
    private val tasks: TaskRepository,
    private val instances: InstanceRepository
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            instances.currentInstanceId.collectLatest {
                while (true) {
                    try {
                        tasks.runMaintenance()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error en el mantenimiento de tareas", e)
                    }
                    delay(millisUntilNextMidnight())
                }
            }
        }
    }

    private fun millisUntilNextMidnight(): Long {
        val now = Calendar.getInstance()
        val nextMidnight = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return nextMidnight.timeInMillis - now.timeInMillis
    }

    private companion object {
        const val TAG = "TaskMaintenance"
    }
}
