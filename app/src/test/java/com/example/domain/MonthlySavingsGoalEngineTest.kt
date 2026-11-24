package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthlySavingsGoalEngineTest {

    @Test
    fun `evaluateMonthlySavingsGoal computes accurate progress remaining and daily rate in Shamsi month`() {
        // 1403/09/10 -> Azar (30-day month), 10 days elapsed, 20 days remaining
        val summary = MonthlySavingsGoalEngine.evaluateMonthlySavingsGoal(
            goalTitleFa = "ذخیره طلا و صندوق درآمد ثابت",
            targetAmountToman = 10_000_000L,
            savedAmountToman = 4_000_000L,
            monthlyIncomeToman = 60_000_000L,
            livingExpensePercent = 50,
            currentMonthDebtDueToman = 12_000_000L,
            todayJalali = "1403/09/10"
        )

        assertEquals("ذخیره طلا و صندوق درآمد ثابت", summary.goalTitleFa)
        assertEquals(1403, summary.shamsiYear)
        assertEquals(9, summary.shamsiMonth)
        assertEquals("آذر", summary.shamsiMonthNameFa)
        assertEquals(30, summary.monthLengthDays)
        assertEquals(10, summary.daysElapsedInMonth)
        assertEquals(20, summary.daysRemainingInMonth)

        assertEquals(10_000_000L, summary.targetAmountToman)
        assertEquals(4_000_000L, summary.savedAmountToman)
        assertEquals(6_000_000L, summary.remainingToTargetToman)
        assertEquals(0L, summary.surplusOverTargetToman)
        assertEquals(0.40f, summary.progressRatio, 0.001f)
        assertEquals(40, summary.progressPercent)
        assertFalse(summary.isTargetAchieved)

        // 6,000,000 remaining over 20 remaining days = 300,000 Toman/day
        assertEquals(300_000L, summary.requiredDailySavingsToman)

        // Free cashflow: 60M - 30M (50% living) - 12M (debt) = 18M
        assertEquals(18_000_000L, summary.estimatedMonthlyFreeCashflowToman)
        assertEquals(SavingsGoalHealthStatus.ON_TRACK, summary.status)
    }

    @Test
    fun `evaluateMonthlySavingsGoal marks target achieved and calculates surplus when saved exceeds target`() {
        val summary = MonthlySavingsGoalEngine.evaluateMonthlySavingsGoal(
            goalTitleFa = "پس‌انداز اضطراری",
            targetAmountToman = 8_000_000L,
            savedAmountToman = 9_500_000L,
            monthlyIncomeToman = 50_000_000L,
            livingExpensePercent = 50,
            currentMonthDebtDueToman = 10_000_000L,
            todayJalali = "1403/09/25"
        )

        assertTrue(summary.isTargetAchieved)
        assertEquals(0L, summary.remainingToTargetToman)
        assertEquals(1_500_000L, summary.surplusOverTargetToman)
        assertEquals(1.0f, summary.progressRatio, 0.001f)
        assertEquals(119, summary.progressPercent)
        assertEquals(0L, summary.requiredDailySavingsToman)
        assertEquals(SavingsGoalHealthStatus.ACHIEVED, summary.status)
    }
}
