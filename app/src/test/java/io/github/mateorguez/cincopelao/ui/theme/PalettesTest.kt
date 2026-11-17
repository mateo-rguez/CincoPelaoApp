package io.github.mateorguez.cincopelao.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import io.github.mateorguez.cincopelao.data.prefs.AppPalette
import io.github.mateorguez.cincopelao.ui.tasks.DUE_CHIP_ALPHA
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/** Comprueba que el texto se lee en todas las paletas, en claro y en oscuro (WCAG AA: 4,5:1). */
class PalettesTest {

    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance().toDouble()
        val lb = b.luminance().toDouble()
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun forEachPalette(block: (String, PaletteColors) -> Unit) {
        for (palette in AppPalette.entries) for (dark in listOf(false, true)) {
            block("${palette.name} ${if (dark) "oscuro" else "claro"}", paletteColors(palette, dark))
        }
    }

    private fun check(failures: MutableList<String>, what: String, fg: Color, bg: Color, min: Double = 4.5) {
        val ratio = contrast(fg, bg)
        if (ratio < min) failures += "$what: %.2f < %.1f".format(ratio, min)
    }

    @Test
    fun `texto principal, barras y acento con contraste suficiente`() {
        val failures = mutableListOf<String>()
        forEachPalette { name, c ->
            check(failures, "$name texto/fondo", c.scheme.onBackground, c.scheme.background)
            check(failures, "$name texto/superficie", c.scheme.onSurface, c.scheme.surface)
            check(failures, "$name barras", c.app.onBar, c.app.bar)
            // Botones de texto, día elegido del selector de fecha, casillas…: sobre los diálogos
            // (surfaceContainerHigh) y sobre el fondo, y con su contenido encima legible
            check(failures, "$name controles/diálogo", c.scheme.primary, c.scheme.surfaceContainerHigh)
            check(failures, "$name controles/fondo", c.scheme.primary, c.scheme.background)
            check(failures, "$name sobre controles", c.scheme.onPrimary, c.scheme.primary)
            check(failures, "$name contenedor", c.scheme.onPrimaryContainer, c.scheme.primaryContainer)
            check(failures, "$name acento/fondo", c.app.accent, c.scheme.background)
            // Colores de cada tipo en el calendario: la etiqueta de la tarjeta de la línea de
            // tiempo (texto del tono sobre ese tono suave) y los puntos (gráficos, 3:1) sobre el
            // fondo y sobre la casilla de un día
            for ((label, tone) in listOf(
                "examen" to c.app.timelineExamPoint,
                "evento" to c.app.timelineEventPoint,
                "tarea" to c.app.timelineTaskPoint
            )) {
                check(failures, "$name etiqueta $label", tone, tone.copy(alpha = DUE_CHIP_ALPHA).compositeOver(c.app.taskCard))
                check(failures, "$name punto $label/fondo", tone, c.scheme.background, min = 3.0)
                check(failures, "$name punto $label/día", tone, c.app.calendarDayBackground, min = 3.0)
            }
            check(failures, "$name texto/tarjeta de tarea", c.scheme.onBackground, c.app.taskCard)
            // Etiqueta de la fecha: texto del tono sobre ese mismo tono suave, encima de la tarjeta
            for ((label, tone) in listOf("vencida" to c.app.dueOverdue, "pronto" to c.app.dueSoon)) {
                check(failures, "$name fecha $label", tone, tone.copy(alpha = DUE_CHIP_ALPHA).compositeOver(c.app.taskCard))
            }
            // Iconos de la barra inferior: elementos gráficos grandes, basta 3:1
            check(failures, "$name barra inferior", c.app.navBarUnselected, c.app.bar, min = 3.0)
            // Texto apagado (completadas, sin nota, fechas sin urgencia): sobre el fondo y las tarjetas
            check(failures, "$name apagado/fondo", c.app.muted, c.scheme.background)
            check(failures, "$name apagado/tarjeta de tarea", c.app.muted, c.app.taskCard)
            // Estados y notas: como texto sobre el fondo (calculadora) y como etiqueta de un evento
            // (texto del tono sobre ese tono suave, encima del fondo); de paso, barras y puntos
            for ((label, tone) in listOf(
                "sin nota" to c.app.muted,
                "hecho" to c.app.done,
                "bajo mínima" to c.app.gradeBelowMinimum,
                "suspenso" to c.app.gradeFail,
                "suficiente" to c.app.gradePass,
                "aceptable" to c.app.gradeFair,
                "notable" to c.app.gradeGood,
                "sobresaliente" to c.app.gradeExcellent,
                "pronto (evento)" to c.app.dueSoon,
            )) {
                check(failures, "$name $label/fondo", tone, c.scheme.background)
                check(failures, "$name etiqueta $label", tone, tone.copy(alpha = DUE_CHIP_ALPHA).compositeOver(c.scheme.background))
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }
}
