package com.example.ui.theme

import com.example.data.TarazinoThemeMode
import kotlin.math.pow

/**
 * Pure domain helper powering the **Deep Teal Light <-> High-Contrast Late-Night Dark**
 * theme switcher and providing WCAG 2.1 luminance contrast verification.
 */
object ThemeSwitcherEngine {

    /**
     * Canonical "Deep Teal" Light Mode (`مروارید صبح` — #0F6B66 Deep Persian Teal on crisp Alabaster).
     */
    val DEEP_TEAL_LIGHT_MODE: TarazinoThemeMode = TarazinoThemeMode.ALABASTER_LIGHT

    /**
     * Dedicated High-Contrast Dark Mode engineered for late-night financial management
     * (true OLED dark canvas, razor-sharp borders, WCAG AAA luminous teal & warm gold typography).
     */
    val HIGH_CONTRAST_DARK_MODE: TarazinoThemeMode = TarazinoThemeMode.HIGH_CONTRAST_NIGHT_DARK

    fun isDarkThemeMode(mode: TarazinoThemeMode): Boolean = when (mode) {
        TarazinoThemeMode.ALABASTER_LIGHT,
        TarazinoThemeMode.PERSIAN_TURQUOISE_LIGHT -> false
        TarazinoThemeMode.OBSIDIAN_EMERALD_DARK,
        TarazinoThemeMode.HIGH_CONTRAST_NIGHT_DARK,
        TarazinoThemeMode.DEEP_SAPPHIRE,
        TarazinoThemeMode.MIDNIGHT_GOLD,
        TarazinoThemeMode.VELVET_AMETHYST -> true
    }

    fun isHighContrastNightMode(mode: TarazinoThemeMode): Boolean =
        mode == HIGH_CONTRAST_DARK_MODE

    /**
     * One-tap switcher between **Deep Teal Light Mode** and **High-Contrast Late-Night Dark Mode**:
     * - From any Light mode -> switches directly to [HIGH_CONTRAST_DARK_MODE].
     * - From any Dark mode -> switches directly to [DEEP_TEAL_LIGHT_MODE].
     */
    fun toggleDeepTealLightAndHighContrastDark(current: TarazinoThemeMode): TarazinoThemeMode {
        return if (isDarkThemeMode(current)) {
            DEEP_TEAL_LIGHT_MODE
        } else {
            HIGH_CONTRAST_DARK_MODE
        }
    }

    fun nextToggleActionLabelFa(current: TarazinoThemeMode): String {
        return if (isDarkThemeMode(current)) {
            "تغییر به حالت روشن (فیروزه تیره)"
        } else {
            "تغییر به حالت تاریک پُرکنتراست شبانه"
        }
    }

    /**
     * Computes exact WCAG 2.1 contrast ratio (`1.0..21.0`) between two ARGB hex colors.
     */
    fun contrastRatio(foregroundArgb: Long, backgroundArgb: Long): Double {
        val l1 = relativeLuminance(foregroundArgb)
        val l2 = relativeLuminance(backgroundArgb)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(argb: Long): Double {
        val r = ((argb shr 16) and 0xFF) / 255.0
        val g = ((argb shr 8) and 0xFF) / 255.0
        val b = (argb and 0xFF) / 255.0

        fun linearize(channel: Double): Double =
            if (channel <= 0.03928) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)

        return 0.2126 * linearize(r) + 0.7152 * linearize(g) + 0.0722 * linearize(b)
    }
}
