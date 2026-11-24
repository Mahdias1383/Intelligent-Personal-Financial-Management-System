package com.example.notification

import com.example.core.CurrencyUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalThresholdNotificationSystemTest {

    @Test
    fun `evaluateThresholdAlerts triggers savings alert when reaching or exceeding 80 percent`() {
        val summary = GoalThresholdNotificationEvaluator.evaluateThresholdAlerts(
            savingsGoalTitleFa = "ذخیره طلا",
            savingsCurrentToman = 8_200_000L,
            savingsTargetToman = 10_000_000L,
            expenseCurrentToman = 12_000_000L,
            expenseBudgetCapToman = 25_000_000L,
            savingsThresholdPercent = 80,
            expenseThresholdPercent = 80,
            todayJalali = "1403/09/15",
            currencyUnit = CurrencyUnit.TOMAN
        )

        assertTrue(summary.shouldNotify)
        assertTrue(summary.isSavingsThresholdExceeded)
        assertFalse(summary.isExpenseThresholdExceeded)
        assertEquals(82, summary.savingsProgressPercent)
        assertEquals(1, summary.triggeredAlerts.size)
        assertEquals(
            GoalThresholdAlertKind.SAVINGS_GOAL_THRESHOLD_REACHED,
            summary.triggeredAlerts.first().kind
        )
        assertTrue(summary.notificationTitleFa.contains("۸۲٪"))
    }

    @Test
    fun `evaluateThresholdAlerts triggers expense alert when reaching or exceeding 90 percent`() {
        val summary = GoalThresholdNotificationEvaluator.evaluateThresholdAlerts(
            savingsGoalTitleFa = "پس‌انداز ماهانه",
            savingsCurrentToman = 4_000_000L,
            savingsTargetToman = 10_000_000L,
            expenseCurrentToman = 27_500_000L,
            expenseBudgetCapToman = 30_000_000L,
            savingsThresholdPercent = 80,
            expenseThresholdPercent = 90,
            todayJalali = "1403/09/18",
            currencyUnit = CurrencyUnit.TOMAN
        )

        assertTrue(summary.shouldNotify)
        assertFalse(summary.isSavingsThresholdExceeded)
        assertTrue(summary.isExpenseThresholdExceeded)
        assertEquals(92, summary.expenseUsagePercent)
        assertEquals(1, summary.triggeredAlerts.size)
        val alert = summary.triggeredAlerts.first()
        assertEquals(GoalThresholdAlertKind.EXPENSE_CAP_THRESHOLD_EXCEEDED, alert.kind)
        assertTrue(alert.isCritical)
        assertEquals(90, alert.thresholdPercentage)
    }

    @Test
    fun `evaluateThresholdAlerts stays quiet when both savings and expenses are below threshold`() {
        val summary = GoalThresholdNotificationEvaluator.evaluateThresholdAlerts(
            savingsGoalTitleFa = "پس‌انداز اضطراری",
            savingsCurrentToman = 6_000_000L,
            savingsTargetToman = 10_000_000L,
            expenseCurrentToman = 15_000_000L,
            expenseBudgetCapToman = 30_000_000L,
            savingsThresholdPercent = 80,
            expenseThresholdPercent = 80,
            todayJalali = "1403/09/10"
        )

        assertFalse(summary.shouldNotify)
        assertFalse(summary.isSavingsThresholdExceeded)
        assertFalse(summary.isExpenseThresholdExceeded)
        assertTrue(summary.triggeredAlerts.isEmpty())
    }

    @Test
    fun `evaluateThresholdAlerts reports both alerts when savings and expenses both exceed thresholds`() {
        val summary = GoalThresholdNotificationEvaluator.evaluateThresholdAlerts(
            savingsGoalTitleFa = "صندوق سفر",
            savingsCurrentToman = 9_500_000L,
            savingsTargetToman = 10_000_000L,
            expenseCurrentToman = 28_000_000L,
            expenseBudgetCapToman = 25_000_000L,
            savingsThresholdPercent = 90,
            expenseThresholdPercent = 80,
            todayJalali = "1403/09/25"
        )

        assertTrue(summary.shouldNotify)
        assertTrue(summary.isSavingsThresholdExceeded)
        assertTrue(summary.isExpenseThresholdExceeded)
        assertEquals(2, summary.triggeredAlerts.size)
        assertEquals(2, summary.expandedLinesFa.size)
    }
}
