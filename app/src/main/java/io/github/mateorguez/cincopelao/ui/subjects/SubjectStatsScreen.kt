package io.github.mateorguez.cincopelao.ui.subjects

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mateorguez.cincopelao.domain.stats.GradeCalculator
import io.github.mateorguez.cincopelao.domain.stats.GradedEvent
import io.github.mateorguez.cincopelao.domain.stats.Requirement
import io.github.mateorguez.cincopelao.domain.stats.Stats
import io.github.mateorguez.cincopelao.domain.stats.StatsUtils
import io.github.mateorguez.cincopelao.domain.stats.TargetVerdict
import io.github.mateorguez.cincopelao.domain.stats.requirement
import io.github.mateorguez.cincopelao.domain.stats.targetVerdict
import io.github.mateorguez.cincopelao.ui.theme.DelightMedium
import io.github.mateorguez.cincopelao.ui.theme.StatusDone
import io.github.mateorguez.cincopelao.ui.theme.StatusFail
import io.github.mateorguez.cincopelao.ui.theme.StatusFair
import io.github.mateorguez.cincopelao.ui.theme.appColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectStatsScreen(
    state: SubjectDetailUiState,
    onBackClick: () -> Unit
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas - ${state.subject?.abbrev.orEmpty()}") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.appColors.bar,
                    titleContentColor = MaterialTheme.appColors.onBar,
                    navigationIconContentColor = MaterialTheme.appColors.onBar
                ),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Evolución de notas",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            LineChart(
                data = state.gradedEvents.map { it.score },
                minScores = state.gradedEvents.map { it.minScore },
                labels = state.gradedEvents.map { it.name },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Datos",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            state.stats?.let { StatsTable(it) }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Cálculos",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            GradeTargetCalculator(state.gradedEvents)
        }
    }
}

/**
 * Evolución de las notas: una curva suave por cada tramo de eventos con nota (los pendientes la
 * cortan) con un degradado debajo. Un punto bajo la nota mínima de su evento sale en rojo. Con un
 * solo evento se dibuja una línea horizontal a su altura de lado a lado.
 */
@Composable
fun LineChart(
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
    val belowMinimumColor = StatusFail
    val gridColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val density = LocalDensity.current
    val yLabelPaint = remember(labelColor, density) {
        Paint().apply {
            color = labelColor
            isAntiAlias = true
            textSize = with(density) { 10.sp.toPx() }
        }
    }
    val xLabelPaint = remember(labelColor, density) {
        Paint().apply {
            color = labelColor
            isAntiAlias = true
            textSize = with(density) { 12.sp.toPx() }
            textAlign = Paint.Align.CENTER
        }
    }

    val maxValue = (data.filterNotNull().maxOrNull() ?: 10f).coerceAtLeast(10f)
    val singleEvent = data.size == 1

    Canvas(modifier = modifier) {
        val left = 16.dp.toPx()
        val top = 12.dp.toPx()
        val width = size.width - left
        val height = size.height - 48.dp.toPx() - top
        val bottom = top + height
        val pointGap = width / (data.size - 1).coerceAtLeast(1)

        fun xOf(index: Int) = if (singleEvent) left + width / 2 else left + index * pointGap
        fun yOf(value: Float) = bottom - value / maxValue * height

        // Rejilla horizontal con las notas a la izquierda y vertical en cada evento
        for (i in 0..maxValue.toInt()) {
            val y = yOf(i.toFloat())
            drawLine(gridColor, Offset(left, y), Offset(left + width, y), strokeWidth = 1f)
            drawContext.canvas.nativeCanvas.drawText(i.toString(), 2.dp.toPx(), y + 4f, yLabelPaint)
        }
        if (!singleEvent) {
            data.indices.forEach { i -> drawLine(gridColor, Offset(xOf(i), top), Offset(xOf(i), bottom), strokeWidth = 1f) }
        }
        drawLine(axisColor, Offset(left, top), Offset(left, bottom), strokeWidth = 3f)
        drawLine(axisColor, Offset(left, bottom), Offset(left + width, bottom), strokeWidth = 3f)

        // Tramos de eventos seguidos con nota
        val values = data.mapIndexed { i, v -> if (v == null) null else animated[i].value }
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

        if (!singleEvent) {
            values.forEachIndexed { i, v ->
                if (v == null) return@forEachIndexed
                val minimum = minScores.getOrNull(i)
                val belowMinimum = minimum != null && v < minimum / 10
                drawCircle(if (belowMinimum) belowMinimumColor else lineColor, radius = 8f, center = Offset(xOf(i), yOf(v)))
            }
        }

        // Nombres de los eventos, girados para que quepan
        val labelY = bottom + 20.dp.toPx()
        labels.forEachIndexed { i, label ->
            drawContext.canvas.nativeCanvas.apply {
                save()
                rotate(-45f, xOf(i), labelY)
                drawText(label, xOf(i), labelY, xLabelPaint)
                restore()
            }
        }
    }
}

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

@Composable
fun StatsTable(stats: Stats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small)
            .padding(8.dp)
    ) {

        val stdLabel = StatsUtils.regularityLabel(stats.stdDeviation)
        val trendLabel = StatsUtils.trendLabel(stats.trend)

        TableRow(label = "Número de eventos puntuables", value = stats.eventCount.toString())
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))

        TableRow(label = "Número de eventos completados", value = stats.eventCountDone.toString())
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))

        TableRow(label = "Número de eventos pendientes", value = stats.eventCountLeft.toString())
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))

        TableRow(label = "Nº de eventos bajo nota mínima", value = stats.eventCountTrunc.toString())
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))

        TableRow(label = "No puntuables hechos", value = "${stats.noPuntDoneCount} de ${stats.noPuntCount}")
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))

        TableRow(label = "Nota ponderada actual", value = String.format("%.2f", stats.currentWeighted))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))

        val averageValue = if (stats.average.isNaN()) {
            "N/A"
        } else {
            String.format("%.2f", stats.average)
        }

        TableRow(label = "Promedio", value = averageValue)
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))

        TableRow(label = "Nota mínima", value = String.format("%.2f", stats.minScore))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))

        TableRow(label = "Nota máxima", value = String.format("%.2f", stats.maxScore))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))

        TableRow(label = "Proyección con 5 en restantes", value = String.format("%.2f", stats.finalScoring5))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))

        TableRow(label = "Nota final máxima potencial", value = String.format("%.2f", stats.maxPotential))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))

        TableRow(label = "Tendencia (RLS)", value = "${String.format("%+.2f", stats.trend)} ($trendLabel)")
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))

        TableRow(label = "Porcentaje de eventos completados", value = String.format("%.1f", stats.eventCompletedPerc) + "%")
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))

        TableRow(label = "Porcentaje barrido de nota final", value = String.format("%.1f", stats.scoreCompletedPerc) + "%")
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))

        val stdValue = if (stats.stdDeviation.isNaN()) {
            "N/A"
        } else {
            String.format("%.2f", stats.stdDeviation) + " ($stdLabel)"
        }

        TableRow(label = "Desviación típica", value = "$stdValue")
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))

        TableRow(label = "Mediana", value = String.format("%.2f", stats.median))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))

        TableRow(label = "Rango", value = String.format("%.2f", stats.range))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))

        TableRow(label = "Suspensos", value = stats.eventCountSusp.toString())
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))

        TableRow(label = "Suficientes", value = stats.eventCountSufi.toString())
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))

        TableRow(label = "Aceptables", value = stats.eventCountAcep.toString())
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))

        TableRow(label = "Notables", value = stats.eventCountNotable.toString())
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))

        TableRow(label = "Sobresalientes", value = stats.eventCountSobre.toString())
    }
}

@Composable
fun TableRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Calculadora de nota objetivo: se elige la nota final que se quiere con un slider de medio en medio
 * punto y debajo sale un resumen y lo que hace falta en cada evento pendiente, con un color según
 * lo exigente que sea.
 */
@Composable
fun GradeTargetCalculator(events: List<GradedEvent>) {
    var target by rememberSaveable { mutableFloatStateOf(5f) }
    val plan = remember(events, target) { GradeCalculator.requiredScores(events, target) }
    val verdict = remember(events, target, plan) { targetVerdict(events, target, plan) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Nota objetivo",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 24.sp),
                fontFamily = DelightMedium,
                modifier = Modifier.weight(1f)
            )
            Text("%.2f".format(target), style = MaterialTheme.typography.headlineSmall, fontFamily = DelightMedium)
        }
        // Un punto por cada medio punto de nota
        Slider(value = target, onValueChange = { target = it }, valueRange = 0f..10f, steps = 19)

        Text(
            text = verdictText(verdict, target),
            style = MaterialTheme.typography.bodyLarge,
            color = if (verdict is TargetVerdict.Impossible) StatusFail else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
        )

        events.forEachIndexed { i, event ->
            TargetRow(event, plan.required.getOrNull(i))
            if (i < events.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f), thickness = 1.dp)
            }
        }
    }
}

private fun verdictText(verdict: TargetVerdict, target: Float): String = when (verdict) {
    is TargetVerdict.Needs -> {
        val events = if (verdict.pending == 1) "el evento que queda" else "los ${verdict.pending} eventos que quedan"
        "Para un %.2f necesitas un %.2f de media en %s.".format(target, verdict.average, events)
    }
    is TargetVerdict.Reached ->
        if (verdict.minimums) "Ya llegas al %.2f: solo te falta sacar las mínimas.".format(target)
        else "Ya llegas al %.2f con lo que llevas.".format(target)
    is TargetVerdict.Impossible -> "No se puede llegar al %.2f: como mucho sacarías un %.2f.".format(target, verdict.maxReachable)
    is TargetVerdict.NothingLeft -> "No queda nada por hacer: tu nota final es %.2f.".format(verdict.finalGrade)
}

/**
 * Un evento en la calculadora: los hechos con su nota en gris (en rojo si no llegó a la mínima) y
 * los pendientes con la nota que hace falta, coloreada, y una palabra que dice lo exigente que es.
 */
@Composable
private fun TargetRow(event: GradedEvent, required: Float?) {
    val minimum = (event.minScore ?: 0f) / 10f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(vertical = 4.dp)
    ) {
        Text(
            event.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        val score = event.score
        if (score != null) {
            val belowMinimum = score < minimum
            Icon(
                Icons.Default.Check,
                contentDescription = "Hecho",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "%.2f".format(score),
                style = MaterialTheme.typography.titleMedium,
                fontFamily = DelightMedium,
                color = if (belowMinimum) StatusFail else MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (required != null) {
            val level = requirement(required, event.minScore)
            val (caption, color) = when (level) {
                Requirement.MINIMUM -> (if (minimum > 0f) "Basta la mínima" else "Cualquier nota") to StatusDone
                Requirement.REACHABLE -> "Asequible" to StatusDone
                Requirement.DEMANDING -> "Exigente" to StatusFair
                Requirement.IMPOSSIBLE -> "Imposible" to StatusFail
            }
            // La palabra a la izquierda de la nota, sobre la misma base; el par se centra en la fila
            Row {
                Text(
                    caption,
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                    modifier = Modifier.alignByBaseline()
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (level == Requirement.MINIMUM && minimum == 0f) "—" else "%.2f".format(required),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = DelightMedium,
                    color = color,
                    modifier = Modifier.alignByBaseline()
                )
            }
        }
    }
}
