package io.github.mateorguez.cincopelao.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import io.github.mateorguez.cincopelao.ui.tasks.DUE_CHIP_ALPHA
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// «De tu móvil» (Material You): la única paleta calculada. Android genera el ColorScheme a partir
// del fondo de pantalla (Android 12+) y de él salen los colores propios de la app. Los que tienen
// significado (notas, fechas, tipos del calendario) parten de los de Grafito, se acercan un poco
// al tono del fondo para que no desentonen y se ajustan de luz hasta leerse sobre el fondo.

/** Si el móvil puede generar colores a partir del fondo de pantalla (Android 12 o posterior). */
val systemPaletteAvailable: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** Esquema de Material generado por el sistema, o null si el móvil no lo permite. */
@Composable
@ReadOnlyComposable
fun systemColorScheme(dark: Boolean): ColorScheme? {
    if (!systemPaletteAvailable) return null
    val context = LocalContext.current
    return if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}

/**
 * Paleta completa a partir del esquema que da el sistema. Pura: los tests la prueban con
 * esquemas de varios fondos.
 */
fun systemPalette(generated: ColorScheme, dark: Boolean): PaletteColors {
    val base = if (dark) graphiteDark.app else graphiteLight.app
    // El acento de controles tiene que verse sobre los diálogos y el fondo (casi siempre ya lo hace)
    val primary = readable(generated.primary, generated.surfaceContainerHigh, dark)
        .let { readable(it, generated.background, dark) }
    val scheme = generated.copy(primary = primary)
    val background = scheme.background
    val taskCard = if (dark) scheme.surfaceContainerHigh else scheme.surfaceContainer
    val bar = if (dark) scheme.primaryContainer else scheme.primary
    val onBar = if (dark) scheme.onPrimaryContainer else scheme.onPrimary
    val hue = primary.hue()

    /** Tono fijo acercado al del fondo, legible como texto y como etiqueta sobre [surface]. */
    fun meaning(fixed: Color, surface: Color = background): Color {
        val tone = fixed.harmonized(hue)
        return adjustUntil(tone, darker = !dark) { c ->
            contrast(c, background) >= MIN_TEXT &&
                contrast(c, c.copy(alpha = DUE_CHIP_ALPHA).compositeOver(surface)) >= MIN_TEXT
        }
    }

    /** Punto del calendario: gráfico (3:1) sobre el fondo y la casilla, y etiqueta en la tarjeta. */
    fun point(fixed: Color, day: Color): Color {
        val tone = fixed.harmonized(hue)
        return adjustUntil(tone, darker = !dark) { c ->
            contrast(c, background) >= MIN_GRAPHIC && contrast(c, day) >= MIN_GRAPHIC &&
                contrast(c, c.copy(alpha = DUE_CHIP_ALPHA).compositeOver(taskCard)) >= MIN_TEXT
        }
    }

    val calendarDay = scheme.surfaceContainerHighest
    val accent = readable(primary, background, dark)
    val muted = readable(scheme.onSurfaceVariant, taskCard, dark).let { readable(it, background, dark) }

    return PaletteColors(
        scheme = scheme,
        app = AppColors(
            bar = bar,
            onBar = onBar,
            accent = accent,
            shareAccent = accent,
            navBarUnselected = adjustUntil(onBar.copy(alpha = 0.7f).compositeOver(bar), darker = bar.luminance() > 0.5f) {
                contrast(it, bar) >= MIN_GRAPHIC
            },
            subjectCardStart = scheme.primaryContainer,
            subjectCardEnd = scheme.tertiaryContainer,
            taskCardTint = if (dark) Color.White else primary,
            taskCard = taskCard,
            dueOverdue = meaning(base.dueOverdue, taskCard),
            dueSoon = meaning(base.dueSoon, taskCard),
            muted = muted,
            done = meaning(base.done),
            gradeBelowMinimum = meaning(base.gradeBelowMinimum),
            gradeFail = meaning(base.gradeFail),
            gradePass = meaning(base.gradePass),
            gradeFair = meaning(base.gradeFair),
            gradeGood = meaning(base.gradeGood),
            gradeExcellent = meaning(base.gradeExcellent),
            calendarToday = accent,
            calendarDayBackground = calendarDay,
            calendarSelectedBorder = scheme.onSurface,
            timelineExamPoint = point(base.timelineExamPoint, calendarDay),
            timelineEventPoint = point(base.timelineEventPoint, calendarDay),
            timelineTaskPoint = point(base.timelineTaskPoint, calendarDay),
            glowNoScore = bar.copy(alpha = base.glowNoScore.alpha),
            glowPerfect = accent.copy(alpha = base.glowPerfect.alpha),
            glowPass = base.glowPass.harmonized(hue),
            glowRegular = base.glowRegular.harmonized(hue),
            glowFail = base.glowFail.harmonized(hue),
        )
    )
}

private const val MIN_TEXT = 4.6
private const val MIN_GRAPHIC = 3.1

/** Contraste WCAG entre dos colores opacos. */
internal fun contrast(a: Color, b: Color): Double {
    val la = a.luminance().toDouble()
    val lb = b.luminance().toDouble()
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
}

/** [color] oscurecido (fondo claro) o aclarado (fondo oscuro) lo justo para leerse sobre [bg]. */
private fun readable(color: Color, bg: Color, dark: Boolean): Color =
    adjustUntil(color, darker = !dark) { contrast(it, bg) >= MIN_TEXT }

/** Cambia la luz de [color] en pasos pequeños hasta que cumpla [ok] (o llegue al extremo). */
private fun adjustUntil(color: Color, darker: Boolean, ok: (Color) -> Boolean): Color {
    if (ok(color)) return color
    val (h, s, l) = color.hsl()
    var light = l
    repeat(60) {
        light = (if (darker) light - 0.02f else light + 0.02f).coerceIn(0f, 1f)
        val candidate = Color.hsl(h, s, light, color.alpha)
        if (ok(candidate)) return candidate
    }
    return Color.hsl(h, s, light, color.alpha)
}

/**
 * Gira el tono hacia [targetHue] la mitad de la diferencia, como mucho 15°: se nota el fondo de
 * pantalla, pero el rojo sigue siendo rojo y el verde, verde.
 */
private fun Color.harmonized(targetHue: Float): Color {
    val (h, s, l) = hsl()
    var diff = targetHue - h
    if (diff > 180f) diff -= 360f
    if (diff < -180f) diff += 360f
    val step = min(abs(diff) * 0.5f, 15f) * (if (diff < 0) -1 else 1)
    return Color.hsl((h + step + 360f) % 360f, s, l, alpha)
}

private fun Color.hue(): Float = hsl()[0]

/** Tono (0–360), saturación y luz (0–1). */
private fun Color.hsl(): FloatArray {
    val maxC = max(red, max(green, blue))
    val minC = min(red, min(green, blue))
    val l = (maxC + minC) / 2f
    val d = maxC - minC
    if (d == 0f) return floatArrayOf(0f, 0f, l)
    val s = d / (1f - abs(2f * l - 1f))
    val h = when (maxC) {
        red -> 60f * (((green - blue) / d) % 6f)
        green -> 60f * ((blue - red) / d + 2f)
        else -> 60f * ((red - green) / d + 4f)
    }
    return floatArrayOf((h + 360f) % 360f, s.coerceIn(0f, 1f), l)
}
