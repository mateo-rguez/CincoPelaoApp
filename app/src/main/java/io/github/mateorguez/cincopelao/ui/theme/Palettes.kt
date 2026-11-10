package io.github.mateorguez.cincopelao.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import io.github.mateorguez.cincopelao.data.prefs.AppPalette

/** Colores completos de una paleta en un modo: los de Material y los propios de la app. */
data class PaletteColors(val scheme: ColorScheme, val app: AppColors)

fun paletteColors(palette: AppPalette, dark: Boolean): PaletteColors = when (palette) {
    AppPalette.CLASSIC -> if (dark) classicDark else classicLight
    AppPalette.GRAPHITE -> if (dark) graphiteDark else graphiteLight
}

/** Nombre visible de cada paleta. */
val AppPalette.displayName: String
    get() = when (this) {
        AppPalette.CLASSIC -> "Clásico"
        AppPalette.GRAPHITE -> "Grafito"
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
    calendarDayBackground = Color.Cyan,
    calendarSelectedBorder = Color.Black,
    timelineEventContainer = TimelineEventContainerLight,
    timelineTaskContainer = TimelineTaskContainerLight,
    timelineEventPoint = TimelineEventPoint,
    timelineTaskPoint = TimelineTaskPoint,
    glowNoScore = AzulFuerte.copy(alpha = 0.5f),
    glowPerfect = AzulFuerte.copy(alpha = 0.5f),
    glowPass = Color.Yellow.copy(alpha = 0.5f),
    glowRegular = GlowRegularLight.copy(alpha = 0.5f),
    glowFail = Color.Red.copy(alpha = 0.5f),
)

private val classicDarkAppColors = AppColors(
    // Texto e iconos en blanco, como siempre (el autor lo prefiere al lila). La compartición
    // conserva su lila: AzulFuerte apenas contrasta con el fondo AzulOscuro.
    accent = Blanco,
    shareAccent = ShareAccentDark,
    navBarUnselected = NavBarUnselected,
    subjectCardStart = classicDarkScheme.primaryContainer,
    subjectCardEnd = classicDarkScheme.secondaryContainer,
    taskCardTint = Blanco,
    calendarToday = ShareAccentDark,
    calendarDayBackground = CalendarSelectedDayDark,
    calendarSelectedBorder = Color.Cyan,
    timelineEventContainer = TimelineEventContainerDark,
    timelineTaskContainer = TimelineTaskContainerDark,
    timelineEventPoint = TimelineEventPoint,
    timelineTaskPoint = TimelineTaskPoint,
    glowNoScore = AzulFuerte.copy(alpha = 0.3f),
    glowPerfect = AzulFuerte.copy(alpha = 0.7f),
    glowPass = Color.Blue.copy(alpha = 0.3f),
    glowRegular = GlowRegularDark.copy(alpha = 0.3f),
    glowFail = GlowFailDark.copy(alpha = 0.5f),
)

private val classicLight = PaletteColors(classicLightScheme, classicLightAppColors)
private val classicDark = PaletteColors(classicDarkScheme, classicDarkAppColors)

// Paletas con nombre ------------------------------------------------------------------------------
//
// Todas siguen el mismo patrón: `primary` es el color de las barras (con texto blanco encima),
// `accent` el de iconos y botones de texto sobre el fondo, y las tarjetas de la línea de tiempo
// llevan texto blanco, así que su fondo es oscuro en los dos modos.

/** Monta una paleta a partir de sus tonos principales; el resto se deduce igual en todas. */
private fun namedPalette(
    dark: Boolean,
    primary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    secondaryContainer: Color,
    onSecondaryContainer: Color,
    tertiary: Color,
    background: Color,
    onBackground: Color,
    surface: Color,
    surfaceVariant: Color,
    onSurfaceVariant: Color,
    outline: Color,
    accent: Color,
    navBarUnselected: Color,
    subjectCardStart: Color,
    subjectCardEnd: Color,
    timelineEvent: Color,
    timelineTask: Color,
    timelineEventPoint: Color,
    timelineTaskPoint: Color,
): PaletteColors {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val scheme = base.copy(
        primary = primary,
        onPrimary = Blanco,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = accent,
        onSecondary = if (dark) Negro else Blanco,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onBackground,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surfaceVariant.copy(alpha = 0.5f).compositeOver(surface),
        surfaceContainerHigh = surfaceVariant.copy(alpha = 0.7f).compositeOver(surface),
        surfaceContainerHighest = surfaceVariant,
        outline = outline,
        outlineVariant = surfaceVariant,
    )
    val app = AppColors(
        accent = accent,
        navBarUnselected = navBarUnselected,
        subjectCardStart = subjectCardStart,
        subjectCardEnd = subjectCardEnd,
        taskCardTint = if (dark) Blanco else primary,
        calendarToday = accent,
        calendarDayBackground = secondaryContainer,
        calendarSelectedBorder = onPrimaryContainer,
        timelineEventContainer = timelineEvent,
        timelineTaskContainer = timelineTask,
        timelineEventPoint = timelineEventPoint,
        timelineTaskPoint = timelineTaskPoint,
        glowNoScore = primary.copy(alpha = if (dark) 0.3f else 0.5f),
        glowPerfect = accent.copy(alpha = 0.5f),
        glowPass = GlowPass.copy(alpha = if (dark) 0.3f else 0.5f),
        glowRegular = (if (dark) GlowRegularDark else GlowRegularLight).copy(alpha = if (dark) 0.3f else 0.5f),
        glowFail = (if (dark) GlowFailDark else Color.Red).copy(alpha = 0.5f),
    )
    return PaletteColors(scheme, app)
}

// Grafito: grises con acento azul petróleo
private val graphiteLight = namedPalette(
    dark = false,
    primary = Color(0xFF37474F), primaryContainer = Color(0xFFCFD8DC), onPrimaryContainer = Color(0xFF102027),
    secondaryContainer = Color(0xFFE0E7EA), onSecondaryContainer = Color(0xFF151D20), tertiary = Color(0xFF00696F),
    background = Color(0xFFF8F9FA), onBackground = Color(0xFF191C1D), surface = Color(0xFFF8F9FA),
    surfaceVariant = Color(0xFFDCE3E6), onSurfaceVariant = Color(0xFF40484B), outline = Color(0xFF70787B),
    accent = Color(0xFF00697A), navBarUnselected = Color(0xFFB0BEC5),
    subjectCardStart = Color(0xFFDCE3E6), subjectCardEnd = Color(0xFFB8E4EA),
    timelineEvent = Color(0xFF37474F), timelineTask = Color(0xFF00696F),
    timelineEventPoint = Color(0xFF37474F), timelineTaskPoint = Color(0xFF00897B),
)
private val graphiteDark = namedPalette(
    dark = true,
    primary = Color(0xFF2B3940), primaryContainer = Color(0xFF37474F), onPrimaryContainer = Color(0xFFCFD8DC),
    secondaryContainer = Color(0xFF3A4448), onSecondaryContainer = Color(0xFFE0E7EA), tertiary = Color(0xFF80D4DC),
    background = Color(0xFF111415), onBackground = Color(0xFFE1E3E4), surface = Color(0xFF191C1D),
    surfaceVariant = Color(0xFF40484B), onSurfaceVariant = Color(0xFFC0C8CB), outline = Color(0xFF8A9295),
    accent = Color(0xFF5FD4E6), navBarUnselected = Color(0xFFB0BEC5),
    subjectCardStart = Color(0xFF37474F), subjectCardEnd = Color(0xFF1F4A50),
    timelineEvent = Color(0xFF2E3D44), timelineTask = Color(0xFF00474D),
    timelineEventPoint = Color(0xFFB0BEC5), timelineTaskPoint = Color(0xFF5FD4E6),
)
