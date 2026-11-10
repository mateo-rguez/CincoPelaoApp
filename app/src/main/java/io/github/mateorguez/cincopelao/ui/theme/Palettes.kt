package io.github.mateorguez.cincopelao.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import io.github.mateorguez.cincopelao.data.prefs.AppPalette

/** Colores completos de una paleta en un modo: los de Material y los propios de la app. */
data class PaletteColors(val scheme: ColorScheme, val app: AppColors)

fun paletteColors(palette: AppPalette, dark: Boolean): PaletteColors = when (palette) {
    AppPalette.CLASSIC -> if (dark) classicDark else classicLight
}

// Clásico: el azul de siempre --------------------------------------------------------------------

private val classicLightScheme = lightColorScheme(
    primary = AzulFuerte,
    onPrimary = Blanco,
    background = Blanco,
    onBackground = Negro,
    surface = Blanco,
    onSurface = Negro,
    primaryContainer = PrimaryContainerLight,
)

private val classicDarkScheme = darkColorScheme(
    primary = AzulFuerte,
    onPrimary = Blanco,
    background = AzulOscuro,
    onBackground = Blanco,
    surface = SurfaceDark,
    onSurface = Blanco,
    primaryContainer = PrimaryContainerDark,
)

internal val classicLightAppColors = AppColors(
    accent = AzulFuerte,
    navBarUnselected = NavBarUnselected,
    subjectCardStart = SubjectCardGradientStart,
    subjectCardEnd = SubjectCardGradientEnd,
    taskCardTint = TaskCardTintLight,
    calendarToday = Color.Blue,
    calendarSelectedBackground = Color.Cyan,
    calendarSelectedBorder = Color.Black,
    timelineEventContainer = TimelineEventContainerLight,
    timelineTaskContainer = TimelineTaskContainerLight,
    glowNoScore = AzulFuerte.copy(alpha = 0.5f),
    glowPerfect = AzulFuerte.copy(alpha = 0.5f),
    glowPass = Color.Yellow.copy(alpha = 0.5f),
    glowRegular = GlowRegularLight.copy(alpha = 0.5f),
    glowFail = Color.Red.copy(alpha = 0.5f),
)

private val classicDarkAppColors = AppColors(
    // AzulFuerte apenas contrasta con el fondo AzulOscuro: en oscuro el acento es un lila claro
    accent = ShareAccentDark,
    navBarUnselected = NavBarUnselected,
    subjectCardStart = classicDarkScheme.primaryContainer,
    subjectCardEnd = classicDarkScheme.secondaryContainer,
    taskCardTint = Blanco,
    calendarToday = ShareAccentDark,
    calendarSelectedBackground = CalendarSelectedDayDark,
    calendarSelectedBorder = Color.Cyan,
    timelineEventContainer = TimelineEventContainerDark,
    timelineTaskContainer = TimelineTaskContainerDark,
    glowNoScore = AzulFuerte.copy(alpha = 0.3f),
    glowPerfect = AzulFuerte.copy(alpha = 0.7f),
    glowPass = Color.Blue.copy(alpha = 0.3f),
    glowRegular = GlowRegularDark.copy(alpha = 0.3f),
    glowFail = GlowFailDark.copy(alpha = 0.5f),
)

private val classicLight = PaletteColors(classicLightScheme, classicLightAppColors)
private val classicDark = PaletteColors(classicDarkScheme, classicDarkAppColors)
