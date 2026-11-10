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
    /** Iconos, botones de texto y datos destacados sobre el fondo; legible en los dos modos. */
    val accent: Color,
    /** Iconos de la barra inferior sin seleccionar (sobre `primary`). */
    val navBarUnselected: Color,
    /** Degradado de las tarjetas de asignatura. */
    val subjectCardStart: Color,
    val subjectCardEnd: Color,
    /** Tono del borde y del degradado de las tarjetas de tareas (se aplica con transparencia). */
    val taskCardTint: Color,
    /** Calendario: borde del día de hoy, fondo y borde del día elegido. */
    val calendarToday: Color,
    val calendarSelectedBackground: Color,
    val calendarSelectedBorder: Color,
    /** Línea de tiempo del calendario: tarjetas de eventos y de tareas. */
    val timelineEventContainer: Color,
    val timelineTaskContainer: Color,
    /** Brillo de la cabecera de una asignatura según su nota media (ya con transparencia). */
    val glowNoScore: Color,
    val glowPerfect: Color,
    val glowPass: Color,
    val glowRegular: Color,
    val glowFail: Color,
)

/** Colores propios de la paleta activa. Lo publica [CincoPelaoTheme]. */
val LocalAppColors = staticCompositionLocalOf { classicLightAppColors }
