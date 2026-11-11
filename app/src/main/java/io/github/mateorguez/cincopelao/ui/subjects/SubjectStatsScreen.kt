package io.github.mateorguez.cincopelao.ui.subjects

import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
            StatsSectionHeader(Icons.AutoMirrored.Filled.ShowChart, "Evolución de notas", first = true)

            GradeChart(
                data = state.gradedEvents.map { it.score },
                minScores = state.gradedEvents.map { it.minScore },
                labels = state.gradedEvents.map { it.name },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
            )

            // Menos aire que el resto de secciones: debajo de los nombres del eje ya queda hueco
            StatsSectionHeader(Icons.Default.Lightbulb, "Datos", top = 20.dp)

            val stats = state.stats
            val insights = state.insights
            if (stats != null && insights != null) SubjectDataSection(stats, insights)

            StatsSectionHeader(Icons.Default.Calculate, "Cálculos")

            GradeTargetCalculator(state.gradedEvents)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Encabezado de una sección de la pantalla (icono y título). Todas llevan la misma separación con
 * lo anterior, salvo la primera, que va pegada a la barra.
 */
@Composable
private fun StatsSectionHeader(icon: ImageVector, text: String, first: Boolean = false, top: Dp = 36.dp) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = if (first) 4.dp else top, bottom = 16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.titleLarge,
            fontFamily = DelightMedium,
            color = MaterialTheme.colorScheme.onBackground
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
        StatsGroupTitle("Nota objetivo", first = true) {
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
