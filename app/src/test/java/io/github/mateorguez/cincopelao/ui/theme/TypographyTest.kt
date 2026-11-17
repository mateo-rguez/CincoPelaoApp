package io.github.mateorguez.cincopelao.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import io.github.mateorguez.cincopelao.data.prefs.TitleFont
import org.junit.Assert.assertEquals
import org.junit.Test

class TypographyTest {

    @Test
    fun `los estilos de titulo usan la letra elegida y el resto la del texto`() {
        TitleFont.entries.forEach { font ->
            val t = appTypography(font)
            val title = titleFontFamily(font)
            listOf(
                t.displayLarge, t.displayMedium, t.displaySmall,
                t.headlineLarge, t.headlineMedium, t.headlineSmall,
                t.titleLarge, t.titleMedium,
            ).forEach {
                assertEquals(title, it.fontFamily)
                assertEquals(FontWeight.Medium, it.fontWeight)
            }
            listOf(
                t.titleSmall, t.bodyLarge, t.bodyMedium, t.bodySmall,
                t.labelLarge, t.labelMedium, t.labelSmall,
            ).forEach { assertEquals(BodyFont, it.fontFamily) }
        }
    }

    @Test
    fun `el texto junta el espaciado entre letras de Material y los titulos lo conservan`() {
        val material = Typography()
        val t = appTypography(TitleFont.DELIGHT)
        assertEquals(material.bodyLarge.letterSpacing * BODY_LETTER_SPACING, t.bodyLarge.letterSpacing)
        assertEquals(material.labelMedium.letterSpacing * BODY_LETTER_SPACING, t.labelMedium.letterSpacing)
        assertEquals(material.titleLarge.letterSpacing, t.titleLarge.letterSpacing)
    }

    @Test
    fun `cada letra de titulos es distinta y tiene nombre propio`() {
        assertEquals(TitleFont.entries.size, TitleFont.entries.map { titleFontFamily(it) }.toSet().size)
        assertEquals(TitleFont.entries.size, TitleFont.entries.map { it.displayName }.toSet().size)
    }
}
