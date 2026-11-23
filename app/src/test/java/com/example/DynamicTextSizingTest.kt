package com.example

import com.example.ui.components.findFittingFontSizeSp
import com.example.ui.components.formatCompactPersianToman
import com.example.ui.components.ltrIsolateDigits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicTextSizingTest {

    @Test
    fun `fast path returns max font size in a single probe when text fits`() {
        var probes = 0
        val fitted = findFittingFontSizeSp(
            maxFontSizeSp = 22f,
            minFontSizeSp = 10f,
            precisionSp = 0.25f
        ) { _ ->
            probes++
            true
        }
        assertEquals(22f, fitted, 0.001f)
        assertEquals(1, probes)
    }

    @Test
    fun `floor guard returns min font size when text never fits`() {
        var probes = 0
        val fitted = findFittingFontSizeSp(
            maxFontSizeSp = 20f,
            minFontSizeSp = 9.5f,
            precisionSp = 0.25f
        ) { _ ->
            probes++
            false
        }
        assertEquals(9.5f, fitted, 0.001f)
        assertEquals(2, probes) // 1 probe at max + 1 probe at min
    }

    @Test
    fun `binary search converges within precision for 15-digit Rial figure on 320dp mini screen`() {
        // Simulate 21 chars ("۹۹۹,۹۹۹,۹۹۹,۹۹۹,۹۹۰ ریال") in a 240dp box at 1.12f large font scale
        // Approx glyph width per sp = 0.62dp * 1.12 = 0.6944dp -> 21 chars need 14.5824dp per sp
        // Fitting threshold = 240 / 14.5824 = 16.458sp
        val containerDp = 240f
        val charCount = 21
        val fontScale = 1.12f
        val thresholdSp = containerDp / (charCount * 0.62f * fontScale)
        var probes = 0

        val fitted = findFittingFontSizeSp(
            maxFontSizeSp = 32f,
            minFontSizeSp = 10f,
            precisionSp = 0.25f
        ) { candidateSp ->
            probes++
            candidateSp <= thresholdSp
        }

        assertTrue("Fitted font size ($fitted) must not exceed threshold ($thresholdSp)", fitted <= thresholdSp)
        assertTrue(
            "Fitted font size ($fitted) should be within 0.25sp of threshold ($thresholdSp)",
            thresholdSp - fitted <= 0.25f
        )
        assertTrue("Binary search must finish in <= 12 total probes (was $probes)", probes <= 12)
    }

    @Test
    fun `binary search scales cleanly across 320dp 360dp 412dp and 600dp containers`() {
        val widths = listOf(120f, 160f, 220f, 340f)
        val results = widths.map { widthDp ->
            findFittingFontSizeSp(maxFontSizeSp = 26f, minFontSizeSp = 8.5f) { sp ->
                sp * 11.5f <= widthDp
            }
        }
        // Strictly non-decreasing as available width grows
        for (i in 0 until results.lastIndex) {
            assertTrue(
                "Wider container should allow >= font size: ${results[i]} vs ${results[i + 1]}",
                results[i + 1] >= results[i]
            )
        }
        assertEquals(26f, results.last(), 0.001f)
    }

    @Test
    fun `degenerate bounds clamp safely without infinite loop`() {
        val equalBounds = findFittingFontSizeSp(maxFontSizeSp = 14f, minFontSizeSp = 14f) { true }
        assertEquals(14f, equalBounds, 0.001f)

        val invertedBounds = findFittingFontSizeSp(maxFontSizeSp = 12f, minFontSizeSp = 18f) { false }
        assertEquals(12f, invertedBounds, 0.001f)

        val subZeroBounds = findFittingFontSizeSp(maxFontSizeSp = 0f, minFontSizeSp = -5f) { true }
        assertEquals(1f, subZeroBounds, 0.001f)
    }

    @Test
    fun `ltrIsolateDigits wraps numeric runs with Unicode LRI and PDI markers`() {
        val isolated = ltrIsolateDigits("−۱۲,۵۰۰,۰۰۰")
        assertTrue(isolated.startsWith("\u2066"))
        assertTrue(isolated.endsWith("\u2069"))
        assertEquals("−۱۲,۵۰۰,۰۰۰", isolated.substring(1, isolated.length - 1))
    }

    @Test
    fun `formatCompactPersianToman formats thousands millions billions trillions negatives and privacy mask`() {
        assertEquals("•••", formatCompactPersianToman(125_000_000L, privacyMasked = true))
        assertEquals("۴۵۰ ه", formatCompactPersianToman(450_000L, privacyMasked = false))
        assertEquals("۲۵ م", formatCompactPersianToman(25_000_000L, privacyMasked = false))
        assertEquals("۱۲.۵ م", formatCompactPersianToman(12_500_000L, privacyMasked = false))
        assertEquals("۲.۵ م‌د", formatCompactPersianToman(2_500_000_000L, privacyMasked = false))
        assertEquals("۱.۲ همت", formatCompactPersianToman(1_200_000_000_000L, privacyMasked = false))
        assertEquals("−۱۵ م", formatCompactPersianToman(-15_000_000L, privacyMasked = false))
    }
}
