package io.github.mateorguez.cincopelao.notifications

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import io.github.mateorguez.cincopelao.data.instances.InstanceRepository
import io.github.mateorguez.cincopelao.data.local.entity.Task
import io.github.mateorguez.cincopelao.data.prefs.NotiPrefsManager
import io.github.mateorguez.cincopelao.data.repository.EventRepository
import io.github.mateorguez.cincopelao.data.repository.SubjectRepository
import io.github.mateorguez.cincopelao.data.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/**
 * Comprueba que [ReminderManager] mantiene WorkManager sincronizado con los datos:
 * programa, conserva lo que no cambia y cancela lo que ya no hace falta.
 */
@Config(application = Application::class)
@RunWith(RobolectricTestRunner::class)
class ReminderManagerTest {

    private lateinit var context: Context
    private lateinit var instances: InstanceRepository
    private lateinit var tasks: TaskRepository
    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build()
        )
        instances = InstanceRepository(context)
        tasks = TaskRepository(instances)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        ReminderManager(
            context, instances, SubjectRepository(instances), EventRepository(instances), tasks, NotiPrefsManager(context)
        ).start(scope)
    }

    @After
    fun tearDown() {
        scope.cancel()
        instances.currentDb().close()
    }

    /** Ids de los trabajos de recordatorio pendientes. */
    private suspend fun pending(): Set<UUID> =
        WorkManager.getInstance(context).getWorkInfosByTagFlow("reminder").first()
            .filter { it.state == WorkInfo.State.ENQUEUED }
            .mapTo(HashSet()) { it.id }

    /** Espera a que el número de recordatorios pendientes sea [expected] (el manager reacciona con retardo). */
    private suspend fun awaitPending(expected: Int): Set<UUID> {
        repeat(50) {
            val current = pending()
            if (current.size == expected) return current
            delay(100)
        }
        return pending()
    }

    private suspend fun task(title: String) = tasks.tasks.first().single { it.title == title }

    @Test
    fun `programa, conserva y cancela recordatorios segun cambian las tareas`() = runBlocking {
        val inTenDays = System.currentTimeMillis() + 10L * 24 * 60 * 60 * 1000

        // Una tarea con fecha: un recordatorio por cada día de antelación por defecto (7, 3 y 1)
        tasks.insert(Task(title = "Entrega", dueDate = inTenDays))
        val first = awaitPending(3)
        assertEquals(3, first.size)

        // Un cambio que no afecta a sus avisos (otra tarea sin fecha) no los reprograma
        tasks.insert(Task(title = "Sin fecha"))
        delay(1_500)
        assertEquals(first, pending())

        // Completarla cancela sus recordatorios
        tasks.update(task("Entrega").copy(isCompleted = true, completedAt = System.currentTimeMillis()))
        assertEquals(0, awaitPending(0).size)

        // Volver a dejarla pendiente los vuelve a programar; borrarla los cancela
        tasks.update(task("Entrega").copy(isCompleted = false, completedAt = null))
        assertEquals(3, awaitPending(3).size)
        tasks.delete(task("Entrega"))
        assertEquals(0, awaitPending(0).size)
    }
}
