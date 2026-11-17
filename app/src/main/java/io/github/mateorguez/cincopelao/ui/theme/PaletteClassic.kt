package io.github.mateorguez.cincopelao.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

// Clásico: el azul de siempre. Cada modo define todos sus colores (ver Palettes.kt).
// Los tonos de Material que no se eligieron a propósito (secundario, terciario, superficies…) son
// los de la paleta base de Material 3, escritos aquí para que la paleta no dependa de nada más.

private val AzulFuerte = Color(0xFF2823BC)
private val AzulOscuro = Color(0xFF11122F)
/** Botones y controles en oscuro: el azul de siempre aclarado para que se lea sobre fondo oscuro. */
private val AzulClaro = Color(0xFF9AA6FF)
/** Acento de compartir y día de hoy en oscuro: AzulFuerte apenas contrasta con AzulOscuro. */
private val Lila = Color(0xFF9D99FF)
private val Blanco = Color(0xFFFFFFFF)
private val Negro = Color(0xFF000000)

internal val classicLight = PaletteColors(
    // Material: botones, casillas, diálogos, hojas, menús, avisos…
    scheme = ColorScheme(
        primary = AzulFuerte,
        onPrimary = Blanco,
        primaryContainer = Color(0xFFEADDFF),
        onPrimaryContainer = Color(0xFF21005D),
        inversePrimary = Color(0xFFD0BCFF),
        secondary = Color(0xFF625B71),
        onSecondary = Blanco,
        secondaryContainer = Color(0xFFE8DEF8),
        onSecondaryContainer = Color(0xFF1D192B),
        tertiary = Color(0xFF7D5260),
        onTertiary = Blanco,
        tertiaryContainer = Color(0xFFFFD8E4),
        onTertiaryContainer = Color(0xFF31111D),
        background = Blanco,
        onBackground = Negro,
        surface = Blanco,
        onSurface = Negro,
        surfaceVariant = Color(0xFFE7E0EC),
        onSurfaceVariant = Color(0xFF49454F),
        surfaceTint = AzulFuerte,
        inverseSurface = Color(0xFF322F35),
        inverseOnSurface = Color(0xFFF5EFF7),
        error = Color(0xFFB3261E),
        onError = Blanco,
        errorContainer = Color(0xFFF9DEDC),
        onErrorContainer = Color(0xFF410E0B),
        outline = Color(0xFF79747E),
        outlineVariant = Color(0xFFCAC4D0),
        scrim = Negro,
        surfaceBright = Color(0xFFFEF7FF),
        surfaceDim = Color(0xFFDED8E1),
        surfaceContainer = Color(0xFFF3EDF7),
        surfaceContainerHigh = Color(0xFFECE6F0),
        surfaceContainerHighest = Color(0xFFE6E0E9),
        surfaceContainerLow = Color(0xFFF7F2FA),
        surfaceContainerLowest = Blanco,
    ),
    // Propios de la app (ver AppColors)
    app = AppColors(
        bar = AzulFuerte,
        onBar = Blanco,
        accent = AzulFuerte,
        shareAccent = AzulFuerte,
        navBarUnselected = Color(0xFFBEC0FF),
        subjectCardStart = Color(0xFFE4D4FF),
        subjectCardEnd = Color(0xFF76EAEE),
        taskCardTint = Color(0xFF001794),
        taskCard = Color(0xFFF1F2FB),
        dueOverdue = Color(0xFF9F1D17),
        dueSoon = Color(0xFF884000),
        calendarToday = AzulFuerte,
        calendarDayBackground = Color(0xFFECEDF8),
        calendarSelectedBorder = AzulOscuro,
        timelineExamPoint = Color(0xFFAD1457),
        timelineEventPoint = Color(0xFF1458B0),
        timelineTaskPoint = Color(0xFF216A27),
        glowNoScore = AzulFuerte.copy(alpha = 0.5f),
        glowPerfect = AzulFuerte.copy(alpha = 0.5f),
        glowPass = Color(0xFFFFFF00).copy(alpha = 0.5f),
        glowRegular = Color(0xFFEF6D05).copy(alpha = 0.5f),
        glowFail = Color(0xFFFF0000).copy(alpha = 0.5f),
    )
)

internal val classicDark = PaletteColors(
    // Material: botones, casillas, diálogos, hojas, menús, avisos…
    scheme = ColorScheme(
        primary = AzulClaro,
        onPrimary = AzulOscuro,
        primaryContainer = Color(0xFF4F378B),
        onPrimaryContainer = Color(0xFFEADDFF),
        inversePrimary = Color(0xFF6750A4),
        secondary = Color(0xFFCCC2DC),
        onSecondary = Color(0xFF332D41),
        secondaryContainer = Color(0xFF4A4458),
        onSecondaryContainer = Color(0xFFE8DEF8),
        tertiary = Color(0xFFEFB8C8),
        onTertiary = Color(0xFF492532),
        tertiaryContainer = Color(0xFF633B48),
        onTertiaryContainer = Color(0xFFFFD8E4),
        background = AzulOscuro,
        onBackground = Blanco,
        surface = Color(0xFF121212),
        onSurface = Blanco,
        surfaceVariant = Color(0xFF49454F),
        onSurfaceVariant = Color(0xFFCAC4D0),
        surfaceTint = AzulClaro,
        inverseSurface = Color(0xFFE6E0E9),
        inverseOnSurface = Color(0xFF322F35),
        error = Color(0xFFF2B8B5),
        onError = Color(0xFF601410),
        errorContainer = Color(0xFF8C1D18),
        onErrorContainer = Color(0xFFF9DEDC),
        outline = Color(0xFF938F99),
        outlineVariant = Color(0xFF49454F),
        scrim = Negro,
        surfaceBright = Color(0xFF3B383E),
        surfaceDim = Color(0xFF141218),
        surfaceContainer = Color(0xFF211F26),
        surfaceContainerHigh = Color(0xFF2B2930),
        surfaceContainerHighest = Color(0xFF36343B),
        surfaceContainerLow = Color(0xFF1D1B20),
        surfaceContainerLowest = Color(0xFF0F0D13),
    ),
    // Propios de la app (ver AppColors)
    app = AppColors(
        bar = AzulFuerte,
        onBar = Blanco,
        accent = Blanco,
        shareAccent = Lila,
        navBarUnselected = Color(0xFFBEC0FF),
        subjectCardStart = Color(0xFF4F378B),
        subjectCardEnd = Color(0xFF4A4458),
        taskCardTint = Blanco,
        taskCard = Color(0xFF22233E),
        dueOverdue = Color(0xFFFF9E96),
        dueSoon = Color(0xFFFFB74D),
        calendarToday = Lila,
        calendarDayBackground = Color(0xFF3A3767),
        calendarSelectedBorder = Blanco,
        timelineExamPoint = Color(0xFFF48FB1),
        timelineEventPoint = Color(0xFF90CAF9),
        timelineTaskPoint = Color(0xFFA5D6A7),
        glowNoScore = AzulFuerte.copy(alpha = 0.3f),
        glowPerfect = AzulFuerte.copy(alpha = 0.7f),
        glowPass = Color(0xFF0000FF).copy(alpha = 0.3f),
        glowRegular = Color(0xFFC57009).copy(alpha = 0.3f),
        glowFail = Color(0xFFFF0000).copy(alpha = 0.5f),
    )
)
