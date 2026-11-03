package io.github.mateorguez.cincopelao.ui.subjects

import io.github.mateorguez.cincopelao.domain.stats.Stats
import io.github.mateorguez.cincopelao.domain.stats.GradedEvent
import io.github.mateorguez.cincopelao.domain.stats.GradeCalculator
import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.mateorguez.cincopelao.data.local.entity.Event
import io.github.mateorguez.cincopelao.domain.stats.StatsUtils.calculateStats
import kotlin.math.max
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
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
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

@Composable
fun LineChart(
    data: List<Float?>,
    minScores: List<Float?>,
    modifier: Modifier = Modifier,
    labels: List<String> = emptyList()
) {

    val animatedData = remember(data) {
        if (data.size == 1) {
            listOf(Animatable(0f))
        } else {
            data.map { Animatable(0f) }
        }
    }

    LaunchedEffect(data) {
        if (data.size == 1 && data[0] != null) {
            animatedData[0].animateTo(
                targetValue = data[0]!!,
                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
            )
        } else {
            data.forEachIndexed { index, target ->
                if (target != null) {
                    launch {
                        animatedData[index].animateTo(
                            targetValue = target,
                            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
                        )
                    }
                }
            }
        }
    }

    val animatedDataValues = data.mapIndexed { index, value ->
        if (value == null) null else animatedData[index].value
    }

    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val maxValue = (data.filterNotNull().maxOrNull() ?: 10f).coerceAtLeast(10f)
    val minValue = 0f
    val yStep = 1f
    val yLines = (minValue.toInt()..maxValue.toInt())

    val labelColorArgb = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    Canvas(modifier = modifier) {
        val paddingLeft = 16.dp.toPx()
        val paddingBottom = 48.dp.toPx()
        val paddingTop = 12.dp.toPx()
        val width = size.width - paddingLeft
        val height = size.height - paddingBottom - paddingTop

        val pointGap = width / (data.size - 1).coerceAtLeast(1)


        // CANVAS PARA CUANDO SOLO HAY UN PUNTUABLE

        if (
            data.size == 1 &&
            minScores.size == 1 &&
            labels.size == 1 &&
            data[0] != null
        ) {
            val animatedValue = animatedData[0].value
            val y = paddingTop + height - (animatedValue - minValue) / (maxValue - minValue) * height

            // Dibuja ejes y líneas horizontales igual que antes
            drawLine(
                color = axisColor,
                start = Offset(paddingLeft, paddingTop),
                end = Offset(paddingLeft, paddingTop + height),
                strokeWidth = 3f
            )
            drawLine(
                color = axisColor,
                start = Offset(paddingLeft, paddingTop + height),
                end = Offset(paddingLeft + width, paddingTop + height),
                strokeWidth = 3f
            )

            for (i in yLines) {
                val yStepLine = paddingTop + height - (i - minValue) / (maxValue - minValue) * height

                drawLine(
                    color = gridColor,
                    start = Offset(paddingLeft, yStepLine),
                    end = Offset(paddingLeft + width, yStepLine),
                    strokeWidth = 1f
                )

                drawContext.canvas.nativeCanvas.drawText(
                    i.toString(),
                    2.dp.toPx(),
                    yStepLine + 4f,
                    Paint().apply {
                        color = labelColorArgb
                        textSize = 10.sp.toPx()
                    }
                )
            }

            // Dibuja la sombra degradada bajo la línea
            val path = Path().apply {
                moveTo(paddingLeft, y)
                lineTo(paddingLeft + width, y)
                lineTo(paddingLeft + width, paddingTop + height)
                lineTo(paddingLeft, paddingTop + height)
                close()
            }

            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.3f), Color.Transparent),
                    startY = paddingTop,
                    endY = paddingTop + height
                )
            )

            // Dibuja la línea animada principal
            drawLine(
                color = lineColor,
                start = Offset(paddingLeft, y),
                end = Offset(paddingLeft + width, y),
                strokeWidth = 4f
            )

            // Etiqueta X
            val x = paddingLeft + width / 2
            val label = labels[0]
            val labelY = paddingTop + height + 20.dp.toPx()
            drawContext.canvas.nativeCanvas.apply {
                save()
                rotate(-45f, x, labelY)
                drawText(
                    label,
                    x,
                    labelY,
                    Paint().apply {
                        color = labelColorArgb
                        textSize = 12.sp.toPx()
                        textAlign = Paint.Align.CENTER
                    }
                )
                restore()
            }

            return@Canvas
        }

        //CANVAS PARA CUANDO HAY MAS DE UN PUNTUABLE

        // Líneas horizontales de referencia + etiquetas Y
        for (i in yLines) {
            val y = paddingTop + height - (i - minValue) / (maxValue - minValue) * height

            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y),
                end = Offset(paddingLeft + width, y),
                strokeWidth = 1f
            )

            // Etiquetas
            drawContext.canvas.nativeCanvas.drawText(
                i.toString(),
                2.dp.toPx(),
                y + 4f,
                Paint().apply {
                    color = labelColorArgb
                    textSize = 10.sp.toPx()
                }
            )
        }

        // Lineas verticales
        for (i in data.indices) {
            val x = paddingLeft + i * pointGap

            drawLine(
                color = gridColor,
                start = Offset(x, paddingTop),
                end = Offset(x, paddingTop + height),
                strokeWidth = 1f
            )
        }

        // Ejes X e Y
        drawLine(
            color = axisColor,
            start = Offset(paddingLeft, paddingTop),
            end = Offset(paddingLeft, paddingTop + height),
            strokeWidth = 3f
        )
        drawLine(
            color = axisColor,
            start = Offset(paddingLeft, paddingTop + height),
            end = Offset(paddingLeft + width, paddingTop + height),
            strokeWidth = 3f
        )

        val pointsSegments = mutableListOf<List<Offset>>()
        var currentSegment = mutableListOf<Offset>()

        animatedDataValues.forEachIndexed { index, value ->
            val x = paddingLeft + pointGap * index

            if (value != null) {
                val y = paddingTop + height - (value - minValue) / (maxValue - minValue) * height
                currentSegment.add(Offset(x, y))
            } else {
                if (currentSegment.isNotEmpty()) {
                    pointsSegments.add(currentSegment)
                    currentSegment = mutableListOf()
                }
            }
        }

        if (currentSegment.isNotEmpty()) {
            pointsSegments.add(currentSegment)
        }

        // Línea principal
        fun catmullRomToBezier(points: List<Offset>): Path {
            val path = Path()
            if (points.size < 2) return path

            path.moveTo(points.first().x, points.first().y)

            for (i in 0 until points.size - 1) {
                val p0 = if (i == 0) points[i] else points[i - 1]
                val p1 = points[i]
                val p2 = points[i + 1]
                val p3 = if (i + 2 < points.size) points[i + 2] else p2

                val cp1 = Offset(
                    p1.x + (p2.x - p0.x) / 6f,
                    p1.y + (p2.y - p0.y) / 6f
                )
                val cp2 = Offset(
                    p2.x - (p3.x - p1.x) / 6f,
                    p2.y - (p3.y - p1.y) / 6f
                )

                path.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p2.x, p2.y)
            }

            return path
        }

        pointsSegments.forEach { segment ->
            val smoothPath = catmullRomToBezier(segment)
            drawPath(
                path = smoothPath,
                color = lineColor,
                style = Stroke(width = 4f)
            )

            // También podés dibujar la sombra si querés, por cada segmento
            val shadowPath = smoothPath.apply {
                lineTo(segment.last().x, paddingTop + height)
                lineTo(segment.first().x, paddingTop + height)
                close()
            }

            drawPath(
                path = shadowPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.3f), Color.Transparent),
                    startY = paddingTop,
                    endY = paddingTop + height
                )
            )
        }

        // Puntos con animación suave
        animatedDataValues.forEachIndexed { index, value ->
            if (value != null) {
                val x = paddingLeft + pointGap * index
                val y = paddingTop + height - (value - minValue) / (maxValue - minValue) * height

                val circleColor = if (minScores.getOrNull(index) != null && value < minScores[index]!! / 10) {
                    Color.Red
                } else {
                    pointColor
                }

                drawCircle(
                    color = circleColor,
                    radius = 8f,
                    center = Offset(x, y)
                )
            }
        }

        // Etiquetas X rotadas
        if (labels.isNotEmpty()) {
            labels.forEachIndexed { index, label ->
                val x = paddingLeft + pointGap * index
                val y = paddingTop + height + 20.dp.toPx()
                drawContext.canvas.nativeCanvas.apply {
                    save()
                    rotate(-45f, x, y)
                    drawText(
                        label,
                        x,
                        y,
                        Paint().apply {
                            color = labelColorArgb
                            textSize = 12.sp.toPx()
                            textAlign = Paint.Align.CENTER
                        }
                    )
                    restore()
                }
            }
        }
    }
}

@Composable
fun StatsTable(stats: Stats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small)
            .padding(8.dp)
    ) {

        val stdLabel = when {
            stats.stdDeviation <= 0.9f -> "Muy regular"
            stats.stdDeviation <= 1.5f -> "Regular"
            stats.stdDeviation <= 2.5f -> "Irregular"
            else -> "Caos"
        }
        val trendLabel = when {
            stats.trend > 2.0f -> "Muy Creciente"
            stats.trend > 0.15f -> "Creciente"
            stats.trend < -2.0f -> "Muy Decreciente"
            stats.trend < -0.15f -> "Decreciente"
            else -> "Estable"
        }

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

@Composable
fun GradeTargetCalculator(events: List<GradedEvent>) {
    var targetGrade by remember { mutableStateOf(5f) }
    val plan = remember(events, targetGrade) { GradeCalculator.requiredScores(events, targetGrade) }
    val actualGrade = plan.currentGrade
    val labels = events.map { it.name }

    Column(modifier = Modifier) {
        Text("Nota actual: ${"%.2f".format(actualGrade)}", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(8.dp))
        Text("Define tu nota objetivo:")
        Slider(
            value = targetGrade,
            onValueChange = { targetGrade = it },
            valueRange = 0f..10f,
            steps = 19
        )
        Text("Nota objetivo: ${"%.2f".format(targetGrade)}", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(16.dp))
        Text("Notas necesarias en los eventos restantes:")

        LazyColumn(
            modifier = Modifier
                .heightIn(max = 500.dp)
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small)
                .padding(8.dp)
        ) {
            item {
                // Cabecera de tabla
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Evento",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(0.5f),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Estado / Requisito",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(0.5f),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), thickness = 1.dp)
            }

            itemsIndexed(labels) { i, label ->
                val score = events[i].score
                val required = plan.required[i]
                val minScore = (events[i].minScore ?: 0f) / 10f

                val statusText = if (score == null) {
                    when {
                        required != null && required > 10f -> "(Imposible) ${"%.2f".format(required)}"
                        required != null && required <= minScore -> "(Mínima) ${"%.2f".format(required)}"
                        else -> "${"%.2f".format(required ?: 0f)}"
                    }
                } else {
                    "(Completado) $score"
                }

                val textColor = when {
                    score != null -> MaterialTheme.colorScheme.onSurfaceVariant
                    required != null && (required <= minScore || required > 10f) -> Color.Red
                    else -> MaterialTheme.colorScheme.onSurface
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(0.5f),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(0.5f),
                        textAlign = TextAlign.End,
                        color = textColor
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), thickness = 0.5.dp)
            }
        }

    }
}
