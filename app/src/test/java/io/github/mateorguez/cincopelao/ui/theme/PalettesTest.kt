package io.github.mateorguez.cincopelao.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import io.github.mateorguez.cincopelao.data.prefs.AppPalette
import io.github.mateorguez.cincopelao.ui.tasks.DUE_CHIP_ALPHA
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/**
 * Comprueba que el texto se lee en todas las paletas, en claro y en oscuro (WCAG AA: 4,5:1).
 * «De tu móvil» se prueba con esquemas parecidos a los que genera Android con fondos de pantalla
 * de distintos colores (y uno gris), ya que no se sabe qué fondo tendrá cada móvil.
 */
class PalettesTest {

    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance().toDouble()
        val lb = b.luminance().toDouble()
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun forEachPalette(block: (String, PaletteColors) -> Unit) {
        for (dark in listOf(false, true)) {
            val mode = if (dark) "oscuro" else "claro"
            for (palette in AppPalette.entries) {
                if (palette == AppPalette.SYSTEM) {
                    for ((label, hue, chroma) in wallpapers) {
                        block("SYSTEM $label $mode", paletteColors(palette, dark, generatedScheme(hue, chroma, dark)))
                    }
                } else {
                    block("${palette.name} $mode", paletteColors(palette, dark))
                }
            }
        }
    }

    /** Fondos de pantalla de prueba: tono dominante (0–360) y cuánto color tiene. */
    private val wallpapers = (0 until 360 step 30).map { Triple("tono $it", it.toFloat(), 0.45f) } +
        listOf(Triple("gris", 220f, 0.04f), Triple("muy saturado", 280f, 0.9f))

    /** Aproximación a Material You: paletas tonales de un tono (luz = tono de Material). */
    private fun generatedScheme(hue: Float, chroma: Float, dark: Boolean): ColorScheme {
        fun t(h: Float, s: Float, tone: Int) = Color.hsl((h + 360f) % 360f, s, tone / 100f)
        fun p(tone: Int) = t(hue, chroma, tone)
        fun sec(tone: Int) = t(hue, chroma * 0.35f, tone)
        fun ter(tone: Int) = t(hue + 60f, chroma * 0.8f, tone)
        fun n(tone: Int) = t(hue, chroma * 0.1f, tone)
        fun nv(tone: Int) = t(hue, chroma * 0.2f, tone)
        return if (!dark) lightColorScheme(
            primary = p(40), onPrimary = p(100), primaryContainer = p(90), onPrimaryContainer = p(10),
            secondary = sec(40), onSecondary = sec(100), secondaryContainer = sec(90), onSecondaryContainer = sec(10),
            tertiary = ter(40), onTertiary = ter(100), tertiaryContainer = ter(90), onTertiaryContainer = ter(10),
            background = n(98), onBackground = n(10), surface = n(98), onSurface = n(10),
            surfaceVariant = nv(90), onSurfaceVariant = nv(30), outline = nv(50), outlineVariant = nv(80),
            surfaceContainerLowest = n(100), surfaceContainerLow = n(96), surfaceContainer = n(94),
            surfaceContainerHigh = n(92), surfaceContainerHighest = n(90),
        ) else darkColorScheme(
            primary = p(80), onPrimary = p(20), primaryContainer = p(30), onPrimaryContainer = p(90),
            secondary = sec(80), onSecondary = sec(20), secondaryContainer = sec(30), onSecondaryContainer = sec(90),
            tertiary = ter(80), onTertiary = ter(20), tertiaryContainer = ter(30), onTertiaryContainer = ter(90),
            background = n(6), onBackground = n(90), surface = n(6), onSurface = n(90),
            surfaceVariant = nv(30), onSurfaceVariant = nv(80), outline = nv(60), outlineVariant = nv(30),
            surfaceContainerLowest = n(4), surfaceContainerLow = n(10), surfaceContainer = n(12),
            surfaceContainerHigh = n(17), surfaceContainerHighest = n(22),
        )
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
