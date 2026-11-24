package com.example.ui.theme

import com.example.data.TarazinoThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeSwitcherEngineTest {

    @Test
    fun toggleDeepTealLightAndHighContrastDark_roundTripsCleanly() {
        val fromLight = ThemeSwitcherEngine.toggleDeepTealLightAndHighContrastDark(
            TarazinoThemeMode.ALABASTER_LIGHT
        )
        assertEquals(TarazinoThemeMode.HIGH_CONTRAST_NIGHT_DARK, fromLight)
        assertTrue(ThemeSwitcherEngine.isDarkThemeMode(fromLight))
        assertTrue(ThemeSwitcherEngine.isHighContrastNightMode(fromLight))

        val backToLight = ThemeSwitcherEngine.toggleDeepTealLightAndHighContrastDark(fromLight)
        assertEquals(TarazinoThemeMode.ALABASTER_LIGHT, backToLight)
        assertFalse(ThemeSwitcherEngine.isDarkThemeMode(backToLight))
        assertFalse(ThemeSwitcherEngine.isHighContrastNightMode(backToLight))
    }

    @Test
    fun toggleFromLegacyDarkThemes_switchesToDeepTealLightAndThenHighContrastDark() {
        val toLight = ThemeSwitcherEngine.toggleDeepTealLightAndHighContrastDark(
            TarazinoThemeMode.OBSIDIAN_EMERALD_DARK
        )
        assertEquals(TarazinoThemeMode.ALABASTER_LIGHT, toLight)

        val toHighContrastDark = ThemeSwitcherEngine.toggleDeepTealLightAndHighContrastDark(toLight)
        assertEquals(TarazinoThemeMode.HIGH_CONTRAST_NIGHT_DARK, toHighContrastDark)
    }

    @Test
    fun highContrastNightDarkTokens_meetWcagAaaContrastRatiosForLateNightReadability() {
        val bg = 0xFF020506L
        val surface = 0xFF0A1114L
        val onBg = 0xFFFFFFFFL
        val mutedText = 0xFFCBE0E6L
        val primaryTeal = 0xFF45E6D5L
        val secondaryGold = 0xFFF5D28AL
        val dangerCoral = 0xFFFF7B7BL

        assertTrue(
            "Primary white text on OLED bg must exceed WCAG AAA (7:1)",
            ThemeSwitcherEngine.contrastRatio(onBg, bg) >= 18.0
        )
        assertTrue(
            "Muted frost text on card surface must exceed WCAG AAA (7:1)",
            ThemeSwitcherEngine.contrastRatio(mutedText, surface) >= 10.0
        )
        assertTrue(
            "Luminous Teal primary on card surface must exceed WCAG AAA (7:1)",
            ThemeSwitcherEngine.contrastRatio(primaryTeal, surface) >= 10.0
        )
        assertTrue(
            "Champagne Gold secondary on card surface must exceed WCAG AAA (7:1)",
            ThemeSwitcherEngine.contrastRatio(secondaryGold, surface) >= 10.0
        )
        assertTrue(
            "Alert coral danger on card surface must exceed WCAG AAA (7:1)",
            ThemeSwitcherEngine.contrastRatio(dangerCoral, surface) >= 7.0
        )
    }

    @Test
    fun deepTealLightTokens_meetHighContrastReadabilityOnLightSurface() {
        val surface = 0xFFFFFFFFL
        val primaryDeepTeal = 0xFF0F6B66L
        val onBg = 0xFF0A1D22L

        assertTrue(
            "Deep Teal primary on white card must exceed WCAG AA+ (6:1)",
            ThemeSwitcherEngine.contrastRatio(primaryDeepTeal, surface) >= 6.0
        )
        assertTrue(
            "Primary ink on white card must exceed WCAG AAA (14:1)",
            ThemeSwitcherEngine.contrastRatio(onBg, surface) >= 14.0
        )
    }
}
