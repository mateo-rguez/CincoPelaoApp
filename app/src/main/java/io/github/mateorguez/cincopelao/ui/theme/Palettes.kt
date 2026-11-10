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
    AppPalette.FOREST -> if (dark) forestDark else forestLight
    AppPalette.CORAL -> if (dark) coralDark else coralLight
    AppPalette.VIOLET -> if (dark) violetDark else violetLight
    AppPalette.GRAPHITE -> if (dark) graphiteDark else graphiteLight
}

/** Nombre visible de cada paleta. */
val AppPalette.displayName: String
    get() = when (this) {
        AppPalette.CLASSIC -> "Clásico"
        AppPalette.FOREST -> "Bosque"
        AppPalette.CORAL -> "Coral"
        AppPalette.VIOLET -> "Violeta"
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
    calendarDayBackground = CalendarSelectedDayDark,
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
        glowNoScore = primary.copy(alpha = if (dark) 0.3f else 0.5f),
        glowPerfect = accent.copy(alpha = 0.5f),
        glowPass = GlowPass.copy(alpha = if (dark) 0.3f else 0.5f),
        glowRegular = (if (dark) GlowRegularDark else GlowRegularLight).copy(alpha = if (dark) 0.3f else 0.5f),
        glowFail = (if (dark) GlowFailDark else Color.Red).copy(alpha = 0.5f),
    )
    return PaletteColors(scheme, app)
}

// Bosque: verdes
private val forestLight = namedPalette(
    dark = false,
    primary = Color(0xFF2E6B3F), primaryContainer = Color(0xFFB7F1BE), onPrimaryContainer = Color(0xFF00210B),
    secondaryContainer = Color(0xFFD3E8D1), onSecondaryContainer = Color(0xFF0F1F11), tertiary = Color(0xFF39656D),
    background = Color(0xFFF6FBF3), onBackground = Color(0xFF181D18), surface = Color(0xFFF6FBF3),
    surfaceVariant = Color(0xFFDDE5DA), onSurfaceVariant = Color(0xFF414941), outline = Color(0xFF717970),
    accent = Color(0xFF2E6B3F), navBarUnselected = Color(0xFFA9D3B1),
    subjectCardStart = Color(0xFFD3E8D1), subjectCardEnd = Color(0xFFB5E3DA),
    timelineEvent = Color(0xFF2E6B3F), timelineTask = Color(0xFF39656D),
)
private val forestDark = namedPalette(
    dark = true,
    primary = Color(0xFF2E6B3F), primaryContainer = Color(0xFF14522A), onPrimaryContainer = Color(0xFFB7F1BE),
    secondaryContainer = Color(0xFF3A4B3A), onSecondaryContainer = Color(0xFFD3E8D1), tertiary = Color(0xFFA1CED7),
    background = Color(0xFF101510), onBackground = Color(0xFFDFE4DC), surface = Color(0xFF181D18),
    surfaceVariant = Color(0xFF414941), onSurfaceVariant = Color(0xFFC1C9BE), outline = Color(0xFF8B9389),
    accent = Color(0xFF9CD5A3), navBarUnselected = Color(0xFFA9D3B1),
    subjectCardStart = Color(0xFF14522A), subjectCardEnd = Color(0xFF1F4D4A),
    timelineEvent = Color(0xFF1E4D2B), timelineTask = Color(0xFF1F4A52),
)

// Coral: cálida
private val coralLight = namedPalette(
    dark = false,
    primary = Color(0xFFA8432F), primaryContainer = Color(0xFFFFDAD2), onPrimaryContainer = Color(0xFF3E0500),
    secondaryContainer = Color(0xFFFFDBC9), onSecondaryContainer = Color(0xFF2C160B), tertiary = Color(0xFF6E5D0E),
    background = Color(0xFFFFF8F6), onBackground = Color(0xFF231918), surface = Color(0xFFFFF8F6),
    surfaceVariant = Color(0xFFF5DDD8), onSurfaceVariant = Color(0xFF534340), outline = Color(0xFF857370),
    accent = Color(0xFFA8432F), navBarUnselected = Color(0xFFFFC2B5),
    subjectCardStart = Color(0xFFFFDBC9), subjectCardEnd = Color(0xFFFFC7D1),
    timelineEvent = Color(0xFFA8432F), timelineTask = Color(0xFF6E5D0E),
)
private val coralDark = namedPalette(
    dark = true,
    primary = Color(0xFF8E3A2A), primaryContainer = Color(0xFF73342A), onPrimaryContainer = Color(0xFFFFDAD2),
    secondaryContainer = Color(0xFF5D4037), onSecondaryContainer = Color(0xFFFFDBC9), tertiary = Color(0xFFDDC66F),
    background = Color(0xFF1A1110), onBackground = Color(0xFFF1DFDB), surface = Color(0xFF231918),
    surfaceVariant = Color(0xFF534340), onSurfaceVariant = Color(0xFFD8C2BE), outline = Color(0xFFA08C88),
    accent = Color(0xFFFFB4A5), navBarUnselected = Color(0xFFFFC2B5),
    subjectCardStart = Color(0xFF73342A), subjectCardEnd = Color(0xFF6A3048),
    timelineEvent = Color(0xFF6E2A1E), timelineTask = Color(0xFF4F4410),
)

// Violeta: lavanda
private val violetLight = namedPalette(
    dark = false,
    primary = Color(0xFF6750A4), primaryContainer = Color(0xFFEADDFF), onPrimaryContainer = Color(0xFF21005D),
    secondaryContainer = Color(0xFFE8DEF8), onSecondaryContainer = Color(0xFF1D192B), tertiary = Color(0xFF7D5260),
    background = Color(0xFFFEF7FF), onBackground = Color(0xFF1D1B20), surface = Color(0xFFFEF7FF),
    surfaceVariant = Color(0xFFE7E0EC), onSurfaceVariant = Color(0xFF49454F), outline = Color(0xFF79747E),
    accent = Color(0xFF6750A4), navBarUnselected = Color(0xFFCFC0F5),
    subjectCardStart = Color(0xFFE8DEF8), subjectCardEnd = Color(0xFFC9D7FF),
    timelineEvent = Color(0xFF6750A4), timelineTask = Color(0xFF7D5260),
)
private val violetDark = namedPalette(
    dark = true,
    primary = Color(0xFF5B4597), primaryContainer = Color(0xFF4F378B), onPrimaryContainer = Color(0xFFEADDFF),
    secondaryContainer = Color(0xFF4A4458), onSecondaryContainer = Color(0xFFE8DEF8), tertiary = Color(0xFFEFB8C8),
    background = Color(0xFF141218), onBackground = Color(0xFFE6E0E9), surface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFF49454F), onSurfaceVariant = Color(0xFFCAC4D0), outline = Color(0xFF938F99),
    accent = Color(0xFFD0BCFF), navBarUnselected = Color(0xFFCFC0F5),
    subjectCardStart = Color(0xFF4F378B), subjectCardEnd = Color(0xFF633B48),
    timelineEvent = Color(0xFF3F2C78), timelineTask = Color(0xFF5A3341),
)

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
)
