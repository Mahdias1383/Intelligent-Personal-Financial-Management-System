package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineCurrencyDebtConverterEngineTest {

    @Test
    fun `convertForeignToToman and convertTomanToForeign work accurately with user manual rate`() {
        val manualUsdRate = 75_000L
        val toman = OfflineCurrencyDebtConverterEngine.convertForeignToToman(
            foreignAmount = 400.0,
            manualRateToman = manualUsdRate
        )
        assertEquals(30_000_000L, toman)

        val usdBack = OfflineCurrencyDebtConverterEngine.convertTomanToForeign(
            amountToman = 30_000_000L,
            manualRateToman = manualUsdRate
        )
        assertEquals(400.0, usdBack, 0.001)
    }

    @Test
    fun `evaluateConversion calculates cross-currency equivalents using user manual rates`() {
        val customRates = mapOf(
            ForeignDebtCurrency.USD to 70_000L,
            ForeignDebtCurrency.EUR to 80_000L,
            ForeignDebtCurrency.AED to 20_000L,
            ForeignDebtCurrency.GOLD_GRAM to 4_000_000L,
            ForeignDebtCurrency.USDT to 70_000L,
            ForeignDebtCurrency.TRY to 2_000L
        )

        val result = OfflineCurrencyDebtConverterEngine.evaluateConversion(
            sourceCurrency = ForeignDebtCurrency.USD,
            sourceForeignAmount = 800.0,
            manualRates = customRates
        )

        assertEquals(56_000_000L, result.equivalentToman)
        assertEquals(560_000_000L, result.equivalentRial)

        val eurRow = result.crossConversions.first { it.currency == ForeignDebtCurrency.EUR }
        // 56,000,000 Toman / 80,000 Toman/EUR = 700 EUR
        assertEquals(700.0, eurRow.convertedForeignAmount, 0.001)

        val goldRow = result.crossConversions.first { it.currency == ForeignDebtCurrency.GOLD_GRAM }
        // 56,000,000 Toman / 4,000,000 Toman/gram = 14.0 grams
        assertEquals(14.0, goldRow.convertedForeignAmount, 0.001)
    }

    @Test
    fun `evaluatePortfolioSummary computes FX delta between entry rate and current manual rate`() {
        val manualRates = OfflineCurrencyDebtConverterEngine.defaultManualRatesMap().toMutableMap().apply {
            put(ForeignDebtCurrency.USD, 75_000L)
            put(ForeignDebtCurrency.GOLD_GRAM, 5_000_000L)
        }

        val positions = listOf(
            ForeignCurrencyDebtPosition(
                id = 1L,
                titleFa = "بدهی دلاری",
                counterpartyNameFa = "شرکت واردات",
                currency = ForeignDebtCurrency.USD,
                totalForeignAmount = 1000.0,
                paidForeignAmount = 400.0, // 600 USD remaining
                entryExchangeRateToman = 60_000L // Entry value = 36,000,000 Toman; Current value = 45,000,000 Toman
            ),
            ForeignCurrencyDebtPosition(
                id = 2L,
                titleFa = "قرض طلا",
                counterpartyNameFa = "صندوق خانواده",
                currency = ForeignDebtCurrency.GOLD_GRAM,
                totalForeignAmount = 2.0,
                paidForeignAmount = 0.0, // 2 grams remaining
                entryExchangeRateToman = 4_000_000L // Entry value = 8,000,000 Toman; Current value = 10,000,000 Toman
            )
        )

        val summary = OfflineCurrencyDebtConverterEngine.evaluatePortfolioSummary(
            positions = positions,
            manualRates = manualRates,
            domesticRemainingDebtToman = 20_000_000L
        )

        assertEquals(2, summary.activePositionsCount)
        assertEquals(55_000_000L, summary.totalForeignDebtEquivalentToman)
        assertEquals(44_000_000L, summary.totalForeignDebtEntryToman)
        assertEquals(11_000_000L, summary.totalFxDeltaToman)
        assertEquals(75_000_000L, summary.combinedGrandTotalDebtToman)

        val usdEvaluated = summary.evaluatedPositions.first { it.position.id == 1L }
        assertEquals(25, usdEvaluated.fxChangePercent) // (75k - 60k) / 60k = +25%
    }

    @Test
    fun `serialize and deserialize rates and positions preserve offline data`() {
        val originalRates = OfflineCurrencyDebtConverterEngine.defaultManualRatesMap().toMutableMap().apply {
            put(ForeignDebtCurrency.USD, 81_200L)
            put(ForeignDebtCurrency.AED, 22_150L)
        }
        val ratesJson = OfflineCurrencyDebtConverterEngine.serializeRatesToJson(originalRates)
        val restoredRates = OfflineCurrencyDebtConverterEngine.deserializeRatesFromJson(ratesJson)
        assertEquals(81_200L, restoredRates[ForeignDebtCurrency.USD])
        assertEquals(22_150L, restoredRates[ForeignDebtCurrency.AED])

        val samplePositions = OfflineCurrencyDebtConverterEngine.defaultSampleForeignDebts()
        val posJson = OfflineCurrencyDebtConverterEngine.serializePositionsToJson(samplePositions)
        val restoredPositions = OfflineCurrencyDebtConverterEngine.deserializePositionsFromJson(posJson)
        assertEquals(samplePositions.size, restoredPositions.size)
        assertEquals(samplePositions.first().titleFa, restoredPositions.first().titleFa)
        assertFalse(restoredPositions.first().isSettled)
        assertTrue(restoredPositions.first().remainingForeignAmount > 0.0)
    }
}
