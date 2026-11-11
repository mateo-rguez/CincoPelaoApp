package io.github.mateorguez.cincopelao.ui.subjects

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mateorguez.cincopelao.ui.theme.StatusFail
import kotlinx.coroutines.launch

/**
 * Evolución de las notas.
 *
 * - Una curva suave por cada tramo de eventos con nota (los pendientes la cortan), con un degradado
 *   debajo y la nota encima de cada punto. Un punto suspenso (menos de 5 o bajo la mínima de su
 *   evento) sale en rojo; la línea es siempre del color de la gráfica.
 * - Los eventos pendientes son círculos huecos sobre el eje, con el nombre más apagado.
 * - Una línea discontinua marca el 5.
 * - Los nombres van en horizontal; si no caben en una fila se alternan en dos alturas y, si ni así,
 *   se recortan con «…» (ver [chartLabelLayout]).
 *
 * Con un solo evento se dibuja una línea horizontal a su altura de lado a lado.
 */
@Composable
fun GradeChart(
    data: List<Float?>,
    minScores: List<Float?>,
    modifier: Modifier = Modifier,
    labels: List<String> = emptyList()
) {
    // Cada nota sube desde 0 al entrar y al cambiar
    val animated = remember(data) { data.map { Animatable(0f) } }
    LaunchedEffect(data) {
        data.forEachIndexed { i, target ->
            if (target != null) {
                launch { animated[i].animateTo(target, tween(durationMillis = 1000, easing = FastOutSlowInEasing)) }
            }
        }
    }

    val lineColor = MaterialTheme.colorScheme.primary
    val failColor = StatusFail
    val gridColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
    val passLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val pendingColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val backgroundColor = MaterialTheme.colorScheme.background
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val pendingLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f).toArgb()
    val valueColor = MaterialTheme.colorScheme.onBackground.toArgb()
    val density = LocalDensity.current

    fun paint(size: Float, color: Int, align: Paint.Align = Paint.Align.LEFT) = Paint().apply {
        this.color = color
        isAntiAlias = true
        textSize = with(density) { size.sp.toPx() }
        textAlign = align
    }
    val yLabelPaint = remember(labelColor, density) { paint(10f, labelColor) }
    val xLabelPaint = remember(labelColor, density) { paint(12f, labelColor, Paint.Align.CENTER) }
    val pendingLabelPaint = remember(pendingLabelColor, density) { paint(12f, pendingLabelColor, Paint.Align.CENTER) }
    val valuePaint = remember(valueColor, density) { paint(11f, valueColor, Paint.Align.CENTER) }

    val maxValue = (data.filterNotNull().maxOrNull() ?: 10f).coerceAtLeast(10f)
    val singleEvent = data.size == 1

    Canvas(modifier = modifier) {
        val left = 22.dp.toPx()
        // Margen a los lados para que el primer y el último punto (y su nota) no queden pegados
        val inset = 14.dp.toPx()
        val top = 20.dp.toPx()
        val labelLine = 16.dp.toPx()
        val width = size.width - left
        val pointGap = (width - 2 * inset) / (data.size - 1).coerceAtLeast(1)

        // Los nombres se colocan antes para saber cuánto alto necesitan
        val layout = chartLabelLayout(
            labels = labels,
            slot = if (singleEvent) width else pointGap - 4.dp.toPx(),
            measure = { xLabelPaint.measureText(it) }
        )
        val rows = if (layout.any { it.row == 1 }) 2 else 1
        // Lo justo bajo la última fila para que no se corten las letras que bajan (g, p…)
        val height = size.height - top - 4.dp.toPx() - rows * labelLine
        val bottom = top + height

        fun xOf(index: Int) = if (singleEvent) left + width / 2 else left + inset + index * pointGap
        fun yOf(value: Float) = bottom - value / maxValue * height

        // Rejilla horizontal con las notas a la izquierda y vertical en cada evento
        for (i in 0..maxValue.toInt()) {
            val y = yOf(i.toFloat())
            drawLine(gridColor, Offset(left, y), Offset(left + width, y), strokeWidth = 1f)
            val text = i.toString()
            drawContext.canvas.nativeCanvas.drawText(text, left - 6.dp.toPx() - yLabelPaint.measureText(text), y + 4f, yLabelPaint)
        }
        if (!singleEvent) {
            data.indices.forEach { i -> drawLine(gridColor, Offset(xOf(i), top), Offset(xOf(i), bottom), strokeWidth = 1f) }
        }

        // El aprobado
        drawLine(
            passLineColor,
            Offset(left, yOf(5f)),
            Offset(left + width, yOf(5f)),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
        )

        drawLine(axisColor, Offset(left, top), Offset(left, bottom), strokeWidth = 3f)
        drawLine(axisColor, Offset(left, bottom), Offset(left + width, bottom), strokeWidth = 3f)

        // Tramos de eventos seguidos con nota
        val values = data.mapIndexed { i, v -> if (v == null) null else animated[i].value }
        // Color de cada punto: rojo si suspende
        fun colorOf(i: Int): Color {
            val score = data[i] ?: return lineColor
            return if (isFailing(score, minScores.getOrNull(i))) failColor else lineColor
        }
        val segments: List<List<Offset>> = if (singleEvent) {
            values[0]?.let { listOf(listOf(Offset(left, yOf(it)), Offset(left + width, yOf(it)))) }.orEmpty()
        } else {
            values.foldIndexed(mutableListOf(mutableListOf<Offset>())) { i, acc, v ->
                if (v != null) acc.last().add(Offset(xOf(i), yOf(v)))
                else if (acc.last().isNotEmpty()) acc.add(mutableListOf())
                acc
            }.filter { it.isNotEmpty() }
        }

        val fill = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.3f), Color.Transparent), startY = top, endY = bottom)
        segments.forEach { segment ->
            val curve = smoothPath(segment)
            drawPath(curve, lineColor, style = Stroke(width = 4f))
            curve.lineTo(segment.last().x, bottom)
            curve.lineTo(segment.first().x, bottom)
            curve.close()
            drawPath(curve, fill)
        }

        // Puntos con su nota encima; los pendientes, huecos sobre el eje
        val radius = 5.dp.toPx()
        values.forEachIndexed { i, v ->
            val x = xOf(i)
            val target = data[i]
            if (v == null || target == null) {
                // Relleno con el fondo para que el eje no lo atraviese
                drawCircle(backgroundColor, radius = radius, center = Offset(x, bottom))
                drawCircle(pendingColor, radius = radius, center = Offset(x, bottom), style = Stroke(width = 2.dp.toPx()))
                return@forEachIndexed
            }
            val center = Offset(x, yOf(v))
            drawCircle(colorOf(i), radius = radius, center = center)
            drawContext.canvas.nativeCanvas.drawText("%.2f".format(target), x, center.y - radius - 5.dp.toPx(), valuePaint)
        }

        // Nombres de los eventos, sin salirse por los lados
        layout.forEachIndexed { i, label ->
            val paint = if (data.getOrNull(i) == null) pendingLabelPaint else xLabelPaint
            val half = paint.measureText(label.text) / 2
            val x = xOf(i).coerceIn(half, size.width - half)
            val y = bottom + labelLine * (label.row + 1)
            drawContext.canvas.nativeCanvas.drawText(label.text, x, y, paint)
        }
    }
}

/** Nombre de un evento ya colocado en el eje: texto (quizá recortado) y fila (0 arriba, 1 abajo). */
internal data class ChartLabel(val text: String, val row: Int)

/**
 * Coloca los nombres bajo el eje. Si todos caben en su hueco ([slot]) van en una fila; si no, se
 * alternan en dos (cada uno dispone entonces del doble) y el que aún no quepa se recorta con «…».
 * [measure] da el ancho de un texto. Lógica pura.
 */
internal fun chartLabelLayout(labels: List<String>, slot: Float, measure: (String) -> Float): List<ChartLabel> {
    if (labels.all { measure(it) <= slot }) return labels.map { ChartLabel(it, 0) }
    val room = slot * 2
    return labels.mapIndexed { i, label -> ChartLabel(ellipsizeToWidth(label, room, measure), i % 2) }
}

/** [text] tal cual si cabe en [room]; si no, recortado con «…» hasta caber. [measure] da su ancho. Pura. */
internal fun ellipsizeToWidth(text: String, room: Float, measure: (String) -> Float): String {
    if (measure(text) <= room) return text
    var end = text.length
    while (end > 1 && measure(text.take(end).trimEnd() + "…") > room) end--
    return text.take(end).trimEnd() + "…"
}

/** Suspende una nota si no llega al 5 o a la mínima de su evento ([minScore], de 0 a 100). Pura. */
internal fun isFailing(score: Float, minScore: Float?): Boolean =
    score < 5f || (minScore != null && score < minScore / 10f)

/** Curva suave (Catmull-Rom pasada a Bézier) que pasa por todos los [points]. */
private fun smoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.size < 2) return path
    path.moveTo(points.first().x, points.first().y)
    for (i in 0 until points.size - 1) {
        val p0 = points[maxOf(i - 1, 0)]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points[minOf(i + 2, points.size - 1)]
        path.cubicTo(
            p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f,
            p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f,
            p2.x, p2.y
        )
    }
    return path
}
