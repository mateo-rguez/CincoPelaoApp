package io.github.mateorguez.cincopelao.ui.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.mateorguez.cincopelao.domain.stats.Stats
import io.github.mateorguez.cincopelao.domain.stats.StatsUtils
import io.github.mateorguez.cincopelao.domain.stats.SubjectInsights
import io.github.mateorguez.cincopelao.ui.theme.DelightMedium
import io.github.mateorguez.cincopelao.ui.theme.appColors

/**
 * Sección «Datos» de las estadísticas, en cuatro bloques: progreso, nota final (tres casillas con
 * la horquilla de lo que se puede sacar: con 5, si sigues así y máximo), tus notas y reparto por calificación. Lo que no tiene
 * nada que contar (no puntuables, tendencia con pocas notas…) no sale.
 */
@Composable
fun SubjectDataSection(stats: Stats, insights: SubjectInsights) {
    Column {
        StatsGroupTitle("Progreso", first = true)
        InPlayBar(stats.scoreCompletedPerc)
        DataRow("Eventos hechos", "${stats.eventCountDone} de ${stats.eventCount}")
        insights.heaviestPending?.let {
            RowDivider()
            DataRow("Lo que más pesa", "${it.name} · ${displayPercent(it.weight)}")
        }
        if (stats.noPuntCount > 0) {
            RowDivider()
            DataRow("No puntuables hechos", "${stats.noPuntDoneCount} de ${stats.noPuntCount}")
        }

        StatsGroupTitle("Nota final")
        if (stats.eventCountLeft == 0) {
            // Sin nada pendiente las tres casillas dirían lo mismo
            ProjectionTile(stats.currentWeighted, "nota final", Modifier.fillMaxWidth())
        } else {
            // Las tres del mismo alto (el de la más alta) aunque un texto ocupe dos líneas
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                val tile = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                ProjectionTile(stats.finalScoring5, "con 5 en todo", tile)
                ProjectionTile(insights.projectionIfSteady, "si sigues así", tile)
                ProjectionTile(stats.maxPotential, "máximo posible", tile)
            }
        }

        StatsGroupTitle("Tus notas")
        if (insights.scoredCount == 0) {
            Text(
                "Aún no hay notas.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            DataRow("Nota ponderada actual", grade(stats.currentWeighted))
            RowDivider()
            DataRow("Promedio", grade(stats.average))
            insights.best?.let {
                RowDivider()
                DataRow(if (insights.scoredCount == 1) "Nota" else "Mejor", "${it.name} · ${grade(it.score)}")
            }
            if (insights.scoredCount >= 2) {
                insights.worst?.let {
                    RowDivider()
                    DataRow("Peor", "${it.name} · ${grade(it.score)}")
                }
                RowDivider()
                DataRow(
                    "Regularidad",
                    "${StatsUtils.regularityLabel(stats.stdDeviation)} · σ ${grade(stats.stdDeviation)}"
                )
            }
            // Con dos notas la «recta» es solo la diferencia entre ellas: no dice nada
            if (insights.scoredCount >= 3) {
                RowDivider()
                TrendRow(stats.trend)
            }

            StatsGroupTitle("Reparto")
            GradeDistribution(stats)
        }
    }
}

/**
 * Título de un bloque dentro de una sección («Progreso», «Nota objetivo»…), con algo opcional a la
 * derecha centrado con él. El primero de cada sección va pegado a su encabezado; el resto, separado
 * lo mismo de lo anterior.
 */
@Composable
internal fun StatsGroupTitle(
    text: String,
    first: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (first) 0.dp else 24.dp, bottom = 8.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = DelightMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

@Composable
private fun RowDivider() =
    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f), thickness = 1.dp)

/** Fila de dato: etiqueta a la izquierda y valor a la derecha, que se corta si no cabe. */
@Composable
private fun DataRow(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = DelightMedium,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Barra con la parte de la nota final que ya está en juego (eventos con nota). */
@Composable
private fun InPlayBar(percent: Float) {
    val fraction = (percent / 100f).coerceIn(0f, 1f)
    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        Text(
            "${displayPercent(percent)} de la nota ya en juego",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        ) {
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.appColors.accent)
                )
            }
        }
    }
}

/** Casilla con una nota final posible y qué supone. */
@Composable
private fun ProjectionTile(value: Float?, caption: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f), shape)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), shape)
            // Arriba y no centrado: así los números de las tres casillas quedan a la misma altura
            .padding(horizontal = 6.dp, vertical = 16.dp)
    ) {
        Text(
            value?.let { grade(it) } ?: "—",
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = DelightMedium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            caption,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

/** Tendencia con una flecha que sube, baja o va recta. */
@Composable
private fun TrendRow(trend: Float) {
    val label = StatsUtils.trendLabel(trend)
    // Mismos cortes que «Estable» en StatsUtils.trendLabel
    val icon = when {
        trend > 0.15f -> Icons.AutoMirrored.Filled.TrendingUp
        trend < -0.15f -> Icons.AutoMirrored.Filled.TrendingDown
        else -> Icons.AutoMirrored.Filled.TrendingFlat
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(vertical = 4.dp)
    ) {
        Text("Tendencia", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            "$label · ${"%+.2f".format(trend)}",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = DelightMedium
        )
    }
}

/** Una calificación del reparto: nombre, cuántas notas y su color (el de las etiquetas del detalle). */
private data class GradeBand(val name: String, val count: Int, val color: Color)

/** Barra apilada con cuántas notas caen en cada calificación y una leyenda con las que tienen alguna. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GradeDistribution(stats: Stats) {
    val colors = MaterialTheme.appColors
    val bands = listOf(
        GradeBand("Bajo mínima", stats.eventCountTrunc, colors.gradeBelowMinimum),
        GradeBand("Suspenso", stats.eventCountSusp, colors.gradeFail),
        GradeBand("Suficiente", stats.eventCountSufi, colors.gradePass),
        GradeBand("Aceptable", stats.eventCountAcep, colors.gradeFair),
        GradeBand("Notable", stats.eventCountNotable, colors.gradeGood),
        GradeBand("Sobresaliente", stats.eventCountSobre, colors.gradeExcellent)
    ).filter { it.count > 0 }

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(CircleShape)
    ) {
        bands.forEach { band ->
            Box(
                modifier = Modifier
                    .weight(band.count.toFloat())
                    .height(12.dp)
                    .background(band.color)
            )
        }
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 10.dp)
    ) {
        bands.forEach { band ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(band.color)
                )
                Spacer(Modifier.width(6.dp))
                Text("${band.name} ${band.count}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun grade(value: Float): String = "%.2f".format(value)

/** Porcentaje sin decimales si es entero y con uno si no («20 %», «12,5 %»). */
private fun displayPercent(value: Float): String =
    (if (value % 1f == 0f) "%.0f".format(value) else "%.1f".format(value)) + " %"
