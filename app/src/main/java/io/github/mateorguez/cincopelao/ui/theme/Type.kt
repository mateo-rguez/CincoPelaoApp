package io.github.mateorguez.cincopelao.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.github.mateorguez.cincopelao.R
import io.github.mateorguez.cincopelao.data.prefs.TitleFont

// Tipografía de la app: dos papeles con nombre, y las fuentes van dentro de la app para que se vean
// igual en todos los móviles (la del sistema cambia según el fabricante).
//  - Texto: DM Sans, fija. Cuerpo, listas, botones, etiquetas y la barra de navegación.
//  - Títulos: la que elija el usuario en Apariencia ([TitleFont]). Barras superiores, cabeceras,
//    nombres de asignatura y de evento, y cifras destacadas como la nota.
// Las pantallas no ponen `fontFamily` a mano: eligen un estilo de `MaterialTheme.typography` y
// este decide la letra. Los estilos display, headline, titleLarge y titleMedium son de título; el
// resto (titleSmall incluido, que Material usa en pestañas) son de texto.
// Las fuentes de Google (licencia OFL, en `assets/licencias`) son instancias estáticas de cada peso
// recortadas al alfabeto latino; si falta algún carácter, Android usa el del sistema.

/** Letra del texto normal. */
val BodyFont = FontFamily(
    Font(R.font.dm_sans_regular, FontWeight.Normal),
    Font(R.font.dm_sans_medium, FontWeight.Medium),
    Font(R.font.dm_sans_semibold, FontWeight.SemiBold),
    Font(R.font.dm_sans_bold, FontWeight.Bold),
)

private val Delight = FontFamily(
    Font(R.font.delight_regular, FontWeight.Normal),
    Font(R.font.delight_medium, FontWeight.Medium),
    Font(R.font.delight_semibold, FontWeight.SemiBold),
    Font(R.font.delight_bold, FontWeight.Bold),
)

private val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
)

private val SourceSerif = FontFamily(
    Font(R.font.source_serif_regular, FontWeight.Normal),
    Font(R.font.source_serif_medium, FontWeight.Medium),
    Font(R.font.source_serif_semibold, FontWeight.SemiBold),
    Font(R.font.source_serif_bold, FontWeight.Bold),
)

private val Fraunces = FontFamily(
    Font(R.font.fraunces_regular, FontWeight.Normal),
    Font(R.font.fraunces_medium, FontWeight.Medium),
    Font(R.font.fraunces_semibold, FontWeight.SemiBold),
    Font(R.font.fraunces_bold, FontWeight.Bold),
)

/** Familia de una letra de títulos. */
fun titleFontFamily(font: TitleFont): FontFamily = when (font) {
    TitleFont.DELIGHT -> Delight
    TitleFont.JAKARTA -> PlusJakartaSans
    TitleFont.SOURCE_SERIF -> SourceSerif
    TitleFont.FRAUNCES -> Fraunces
}

/** Nombre que se muestra en Apariencia. */
val TitleFont.displayName: String
    get() = when (this) {
        TitleFont.DELIGHT -> "Delight"
        TitleFont.JAKARTA -> "Jakarta"
        TitleFont.SOURCE_SERIF -> "Source Serif"
        TitleFont.FRAUNCES -> "Fraunces"
    }

/** Cómo es cada letra, para Apariencia. */
val TitleFont.description: String
    get() = when (this) {
        TitleFont.DELIGHT -> "Sobria y seria"
        TitleFont.JAKARTA -> "Moderna"
        TitleFont.SOURCE_SERIF -> "Académica"
        TitleFont.FRAUNCES -> "Con carácter"
    }

/**
 * Estilos de Material con la letra de títulos elegida. Los títulos van en peso medio (como iba
 * Delight Medium en las barras); los tamaños y los interlineados son los de Material.
 */
fun appTypography(titleFont: TitleFont): Typography {
    val base = Typography()
    val title = titleFontFamily(titleFont)
    fun TextStyle.asTitle() = copy(fontFamily = title, fontWeight = FontWeight.Medium)
    fun TextStyle.asBody() = copy(fontFamily = BodyFont)
    return Typography(
        displayLarge = base.displayLarge.asTitle(),
        displayMedium = base.displayMedium.asTitle(),
        displaySmall = base.displaySmall.asTitle(),
        headlineLarge = base.headlineLarge.asTitle(),
        headlineMedium = base.headlineMedium.asTitle(),
        headlineSmall = base.headlineSmall.asTitle(),
        titleLarge = base.titleLarge.asTitle(),
        titleMedium = base.titleMedium.asTitle(),
        titleSmall = base.titleSmall.asBody(),
        bodyLarge = base.bodyLarge.asBody(),
        bodyMedium = base.bodyMedium.asBody(),
        bodySmall = base.bodySmall.asBody(),
        labelLarge = base.labelLarge.asBody(),
        labelMedium = base.labelMedium.asBody(),
        labelSmall = base.labelSmall.asBody(),
    )
}
