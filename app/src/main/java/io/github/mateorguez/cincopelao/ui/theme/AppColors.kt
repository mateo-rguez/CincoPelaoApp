package io.github.mateorguez.cincopelao.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colores con significado propio de la app, además del `ColorScheme` de Material. Cada paleta
 * define los suyos para claro y oscuro, así las pantallas no tienen que preguntar por el modo.
 */
@Immutable
data class AppColors(
    /**
     * Barras superiores, barra inferior y cabecera del menú, con su texto encima. No es el
     * `primary` de Material: ese es el acento de botones, casillas y selectores, y en oscuro
     * tiene que verse sobre los diálogos.
     */
    val bar: Color,
    val onBar: Color = Color.White,
    /** Iconos, botones de texto y datos destacados sobre el fondo; legible en los dos modos. */
    val accent: Color,
    /** Iconos y datos de las pantallas de compartir; por defecto, el acento. */
    val shareAccent: Color = accent,
    /** Iconos de la barra inferior sin seleccionar (sobre `primary`). */
    val navBarUnselected: Color,
    /** Degradado de las tarjetas de asignatura. */
    val subjectCardStart: Color,
    val subjectCardEnd: Color,
    /** Tono de la sombra y del brillo del borde de las tarjetas de tareas (con transparencia). */
    val taskCardTint: Color,
    /** Fondo de las tarjetas de tareas: un poco distinto del de la pantalla para que destaquen. */
    val taskCard: Color,
    /** Etiqueta de la fecha límite: vencida y para hoy o mañana (texto; el fondo es el mismo tono suave). */
    val dueOverdue: Color,
    val dueSoon: Color,
    /** Calendario: borde del día de hoy, fondo de las casillas de los días y borde del elegido. */
    val calendarToday: Color,
    val calendarDayBackground: Color,
    val calendarSelectedBorder: Color,
    /** Línea de tiempo del calendario: tarjetas de eventos y de tareas. */
    val timelineEventContainer: Color,
    val timelineTaskContainer: Color,
    /** Marcadores de la línea de tiempo (sobre el fondo): exámenes, otros eventos y tareas. */
    val timelineExamPoint: Color,
    val timelineEventPoint: Color,
    val timelineTaskPoint: Color,
    /** Brillo de la cabecera de una asignatura según su nota media (ya con transparencia). */
    val glowNoScore: Color,
    val glowPerfect: Color,
    val glowPass: Color,
    val glowRegular: Color,
    val glowFail: Color,
)

/** Colores propios de la paleta activa. Lo publica [CincoPelaoTheme]. */
val LocalAppColors = staticCompositionLocalOf { classicLightAppColors }
