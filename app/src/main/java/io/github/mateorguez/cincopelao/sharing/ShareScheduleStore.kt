package io.github.mateorguez.cincopelao.sharing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.mateorguez.cincopelao.data.instances.InstanceRepository
import io.github.mateorguez.cincopelao.data.prefs.getScheduleState
import io.github.mateorguez.cincopelao.data.prefs.saveScheduleState
import io.github.mateorguez.cincopelao.ui.schedule.ScheduleFileLoader
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Lleva el horario (imagen o PDF ya renderizado) de una instancia a PNG y al revés. */
@Singleton
class ShareScheduleStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val instances: InstanceRepository
) {
    /** PNG del horario de la instancia activa, o null si no tiene. */
    suspend fun exportCurrent(): ByteArray? = export(instances.currentInstanceId.value)

    /** PNG del horario de [instanceId], o null si no tiene (o ya no se puede abrir). */
    suspend fun export(instanceId: String): ByteArray? {
        val uri = getScheduleState(context, instanceId).first().first ?: return null
        val bitmap = ScheduleFileLoader.load(context, Uri.parse(uri)) ?: return null
        return withContext(Dispatchers.Default) {
            ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.toByteArray()
            }
        }
    }

    /**
     * Guarda [png] como horario de [instanceId]. Devuelve false si no es una imagen válida
     * (se comprueba antes de escribir nada en disco).
     */
    suspend fun import(instanceId: String, png: ByteArray): Boolean {
        val valid = withContext(Dispatchers.Default) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(png, 0, png.size, bounds)
            bounds.outWidth > 0 && bounds.outHeight > 0
        }
        if (!valid) return false
        val file = withContext(Dispatchers.IO) {
            val directory = File(context.filesDir, "SCHEDULE_$instanceId").apply { mkdirs() }
            File(directory, "$instanceId.png").apply { writeBytes(png) }
        }
        saveScheduleState(context, instanceId, Uri.fromFile(file).toString(), zoom = 1f, offset = Offset.Zero)
        return true
    }
}
