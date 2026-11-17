package io.github.mateorguez.cincopelao.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colores con significado propio de la app, además del `ColorScheme` de Material. Cada paleta
 * define todos para claro y oscuro (sin valores por defecto: una paleta nueva tiene que decidir
 * cada uno), así las pantallas no tienen que preguntar por el modo.
 */
@Immutable
data class AppColors(
    /**
     * Barras superiores, barra inferior y cabecera del menú, con su texto encima. No es el
     * `primary` de Material: ese es el acento de botones, casillas y selectores, y en oscuro
     * tiene que verse sobre los diálogos.
     */
    val bar: Color,
    val onBar: Color,
    /** Iconos, botones de texto y datos destacados sobre el fondo; legible en los dos modos. */
    val accent: Color,
    /** Iconos y datos de las pantallas de compartir (puede ser el mismo que el acento). */
    val shareAccent: Color,
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
    /**
     * Texto apagado: tareas completadas, eventos sin nota o por hacer, fechas sin urgencia y
     * recuentos secundarios. Legible (4,5:1) sobre el fondo y sobre las tarjetas de tareas.
     */
    val muted: Color,
    /** Algo hecho o al alcance: eventos no puntuables hechos, «Asequible», envío completado. */
    val done: Color,
    /**
     * Calificaciones, de peor a mejor: etiquetas de los eventos (texto del tono sobre ese tono
     * suave), barra del Reparto, puntos de la gráfica y calculadora de la nota objetivo.
     */
    val gradeBelowMinimum: Color,
    val gradeFail: Color,
    val gradePass: Color,
    val gradeFair: Color,
    val gradeGood: Color,
    val gradeExcellent: Color,
    /** Calendario: borde del día de hoy, fondo de las casillas de los días y borde del elegido. */
    val calendarToday: Color,
    val calendarDayBackground: Color,
    val calendarSelectedBorder: Color,
    /**
     * Colores de cada tipo en el calendario (etiquetas de la línea de tiempo y puntos de la
     * cuadrícula y de la hoja del día): exámenes, otros eventos y tareas.
     */
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
val LocalAppColors = staticCompositionLocalOf { classicLight.app }
