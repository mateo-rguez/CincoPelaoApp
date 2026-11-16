package io.github.mateorguez.cincopelao.ui.schedule

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.mateorguez.cincopelao.data.instances.InstanceRepository
import io.github.mateorguez.cincopelao.data.prefs.getScheduleState
import io.github.mateorguez.cincopelao.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarda en memoria el bitmap del último horario cargado.
 *
 * Renderizar un PDF a 3× tarda bastante (y reserva decenas de MB), y antes se hacía la primera vez
 * que se abría la pestaña, justo durante la animación de entrada: la pantalla salía vacía y el
 * horario aparecía de golpe, a tirones. Ahora se precarga al abrir la app ([preload]) y la pestaña
 * lo encuentra ya hecho. Solo se guarda uno (el de la instancia activa): es el mismo bitmap que ya
 * vivía en el ViewModel mientras la app estaba abierta, así que la memoria apenas cambia.
 */
@Singleton
class ScheduleImageCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val instances: InstanceRepository,
    @ApplicationScope private val scope: CoroutineScope
) {
    // Si la pestaña pide el horario mientras se precarga, espera a esa carga en vez de repetirla
    private val mutex = Mutex()
    @Volatile private var cached: Pair<String, Bitmap>? = null

    /** Empieza a cargar en segundo plano el horario de la instancia activa. */
    fun preload() {
        scope.launch {
            val uri = getScheduleState(context, instances.currentInstanceId.value).first().first ?: return@launch
            get(uri)
        }
    }

    /** El bitmap del horario [uri]: el guardado si es el mismo archivo; si no, lo carga. */
    suspend fun get(uri: String): Bitmap? = mutex.withLock {
        cached?.takeIf { it.first == uri }?.second
            ?: ScheduleFileLoader.load(context, Uri.parse(uri))?.also { put(uri, it) }
    }

    /** Guarda un bitmap recién importado, para no volver a leer el archivo. */
    fun put(uri: String, bitmap: Bitmap) {
        // Sube la imagen a la GPU en segundo plano: si no, se hace al pintar el primer fotograma
        bitmap.prepareToDraw()
        cached = uri to bitmap
    }

    fun clear() {
        cached = null
    }
}
