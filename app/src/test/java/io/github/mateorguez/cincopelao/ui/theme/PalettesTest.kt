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
            check(failures, "$name barras", c.scheme.onPrimary, c.scheme.primary)
            check(failures, "$name contenedor", c.scheme.onPrimaryContainer, c.scheme.primaryContainer)
            check(failures, "$name acento/fondo", c.app.accent, c.scheme.background)
            check(failures, "$name línea de tiempo (eventos)", Color.White, c.app.timelineEventContainer)
            check(failures, "$name línea de tiempo (tareas)", Color.White, c.app.timelineTaskContainer)
            check(failures, "$name texto/tarjeta de tarea", c.scheme.onBackground, c.app.taskCard)
            // Etiqueta de la fecha: texto del tono sobre ese mismo tono suave, encima de la tarjeta
            for ((label, tone) in listOf("vencida" to c.app.dueOverdue, "pronto" to c.app.dueSoon)) {
                check(failures, "$name fecha $label", tone, tone.copy(alpha = DUE_CHIP_ALPHA).compositeOver(c.app.taskCard))
            }
            // Iconos de la barra inferior: elementos gráficos grandes, basta 3:1
            check(failures, "$name barra inferior", c.app.navBarUnselected, c.scheme.primary, min = 3.0)
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }
}
