package io.github.mateorguez.cincopelao.ui.schedule

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Convierte el archivo del horario (imagen o PDF) en un bitmap. Todo se hace fuera del hilo principal. */
object ScheduleFileLoader {

    /** Máximo de páginas de un PDF de horario (cada página ocupa bastante memoria). */
    const val MAX_PDF_PAGES = 5

    private const val TAG = "ScheduleFileLoader"
    private const val PDF_SCALE = 3f
    private const val PDF_PAGE_SPACING_PX = 20

    fun isPdf(context: Context, uri: Uri): Boolean =
        context.contentResolver.getType(uri) == "application/pdf" ||
            uri.lastPathSegment?.endsWith(".pdf", ignoreCase = true) == true

    suspend fun pdfPageCount(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
            PdfRenderer(fd).use { it.pageCount }
        } ?: 0
    }

    /** Devuelve el bitmap del horario, o null si no se puede leer. */
    suspend fun load(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            if (isPdf(context, uri)) loadPdf(context, uri) else loadImage(context, uri)
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo cargar el horario $uri", e)
            null
        }
    }

    private fun loadImage(context: Context, uri: Uri): Bitmap =
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))

    /** Pinta todas las páginas una debajo de otra en un solo bitmap. */
    private fun loadPdf(context: Context, uri: Uri): Bitmap? =
        context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
            PdfRenderer(fd).use { renderer ->
                val pageCount = renderer.pageCount
                if (pageCount == 0) return@use null

                val (pageWidth, pageHeight) = renderer.openPage(0).use {
                    (it.width * PDF_SCALE).toInt() to (it.height * PDF_SCALE).toInt()
                }
                val result = Bitmap.createBitmap(
                    pageWidth,
                    pageCount * pageHeight + (pageCount - 1) * PDF_PAGE_SPACING_PX,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(result)
                val matrix = Matrix().apply { setScale(PDF_SCALE, PDF_SCALE) }

                for (i in 0 until pageCount) {
                    renderer.openPage(i).use { page ->
                        val pageBitmap = Bitmap.createBitmap(pageWidth, pageHeight, Bitmap.Config.ARGB_8888)
                        pageBitmap.eraseColor(Color.WHITE)
                        page.render(pageBitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        canvas.drawBitmap(pageBitmap, 0f, (i * (pageHeight + PDF_PAGE_SPACING_PX)).toFloat(), null)
                        pageBitmap.recycle() // se libera ya en vez de esperar al recolector
                    }
                }
                result
            }
        }
}
