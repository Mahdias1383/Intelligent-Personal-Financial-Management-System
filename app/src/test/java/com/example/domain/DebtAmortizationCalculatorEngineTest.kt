package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebtAmortizationCalculatorEngineTest {

    @Test
    fun `compound annuity amortization calculates monthly payment, declining principal balance, and exact Shamsi due dates`() {
        val result = DebtAmortizationCalculatorEngine.calculateAmortization(
            principalToman = 100_000_000L,
            annualInterestRatePercent = 23.0,
            durationMonths = 12,
            startDateJalali = "1405/06/31",
            formulaMode = AmortizationFormulaMode.COMPOUND_ANNUITY
        )

        assertEquals(12, result.schedule.size)
        assertEquals(100_000_000L, result.principalToman)
        // Sum of all principal portions must equal exact principal (100,000,000 Toman)
        assertEquals(100_000_000L, result.schedule.sumOf { it.principalPortionToman })
        // Remaining principal after final installment must be 0
        assertEquals(0L, result.schedule.last().remainingPrincipalAfterToman)
        assertTrue(result.monthlyPaymentToman > 8_333_333L)
        assertTrue(result.totalInterestToman > 0L)
        assertEquals(result.principalToman + result.totalInterestToman, result.totalPayableToman)

        // Verify Shamsi calendar clamping across 31-day, 30-day, 29-day Esfand, and back to 31-day spring
        assertEquals("1405/06/31", result.schedule[0].dueDateJalali)
        assertEquals("1405/07/30", result.schedule[1].dueDateJalali)
        assertEquals("1405/12/29", result.schedule[6].dueDateJalali)
        assertEquals("1406/01/31", result.schedule[7].dueDateJalali)
        assertEquals("1406/05/31", result.endDateJalali)
    }

    @Test
    fun `flat simple fee mode calculates Qarz-al-Hasaneh 4 percent annual fee accurately`() {
        val result = DebtAmortizationCalculatorEngine.calculateAmortization(
            principalToman = 120_000_000L,
            annualInterestRatePercent = 4.0,
            durationMonths = 12,
            startDateJalali = "1405/08/01",
            formulaMode = AmortizationFormulaMode.FLAT_SIMPLE
        )

        // 4% of 120,000,000 for 12 months = 4,800,000 total interest
        assertEquals(4_800_000L, result.totalInterestToman)
        assertEquals(124_800_000L, result.totalPayableToman)
        assertEquals(10_400_000L, result.monthlyPaymentToman)
        assertEquals(120_000_000L, result.schedule.sumOf { it.principalPortionToman })
        assertEquals(0L, result.schedule.last().remainingPrincipalAfterToman)
    }

    @Test
    fun `zero interest rate loan distributes exact principal across Shamsi months with zero remainder loss`() {
        val result = DebtAmortizationCalculatorEngine.calculateAmortization(
            principalToman = 50_000_000L,
            annualInterestRatePercent = 0.0,
            durationMonths = 3,
            startDateJalali = "1405/11/30",
            formulaMode = AmortizationFormulaMode.COMPOUND_ANNUITY
        )

        assertEquals(0L, result.totalInterestToman)
        assertEquals(50_000_000L, result.totalPayableToman)
        assertEquals(listOf(16_666_666L, 16_666_666L, 16_666_668L), result.schedule.map { it.paymentAmountToman })
        assertEquals(listOf("1405/11/30", "1405/12/29", "1406/01/30"), result.schedule.map { it.dueDateJalali })
    }
}
