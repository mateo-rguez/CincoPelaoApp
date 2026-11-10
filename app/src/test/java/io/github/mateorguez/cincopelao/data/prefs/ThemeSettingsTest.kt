package io.github.mateorguez.cincopelao.data.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeSettingsTest {

    @Test
    fun `sin nada guardado sigue al sistema con la paleta clasica`() {
        assertEquals(ThemeSettings(ThemeMode.SYSTEM, AppPalette.CLASSIC), ThemeSettings.fromStored(null, null, null))
    }

    @Test
    fun `el interruptor antiguo de modo oscuro se respeta`() {
        assertEquals(ThemeMode.DARK, ThemeSettings.fromStored(null, null, legacyDarkMode = true).mode)
        assertEquals(ThemeMode.LIGHT, ThemeSettings.fromStored(null, null, legacyDarkMode = false).mode)
    }

    @Test
    fun `el modo elegido manda sobre el interruptor antiguo`() {
        assertEquals(ThemeMode.SYSTEM, ThemeSettings.fromStored("SYSTEM", null, legacyDarkMode = true).mode)
    }

    @Test
    fun `valores desconocidos vuelven a los de por defecto`() {
        assertEquals(ThemeSettings(), ThemeSettings.fromStored("SEPIA", "NO_EXISTE", null))
    }

    @Test
    fun `modo oscuro segun el sistema`() {
        assertTrue(ThemeSettings(ThemeMode.SYSTEM).isDark(systemDark = true))
        assertFalse(ThemeSettings(ThemeMode.SYSTEM).isDark(systemDark = false))
        assertTrue(ThemeSettings(ThemeMode.DARK).isDark(systemDark = false))
        assertFalse(ThemeSettings(ThemeMode.LIGHT).isDark(systemDark = true))
    }
}
