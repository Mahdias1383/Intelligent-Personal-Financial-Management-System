package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurringCashflowHealthEngineTest {

    @Test
    fun `evaluateHolisticHealth aggregates recurring incomes non-loan expenses and loan installments accurately`() {
        val items = listOf(
            RecurringCashflowItem(
                id = 1L,
                titleFa = "حقوق ثابت",
                category = RecurringCashflowCategory.SALARY,
                monthlyAmountToman = 50_000_000L,
                dayOfShamsiMonth = 1,
                isEssential = true,
                isActive = true
            ),
            RecurringCashflowItem(
                id = 2L,
                titleFa = "پروژه جانبی",
                category = RecurringCashflowCategory.FREELANCE_BUSINESS,
                monthlyAmountToman = 10_000_000L,
                dayOfShamsiMonth = 15,
                isEssential = false,
                isActive = true
            ),
            RecurringCashflowItem(
                id = 3L,
                titleFa = "خوراک و مایحتاج",
                category = RecurringCashflowCategory.GROCERIES_LIVING,
                monthlyAmountToman = 18_000_000L,
                dayOfShamsiMonth = 5,
                isEssential = true,
                isActive = true
            ),
            RecurringCashflowItem(
                id = 4L,
                titleFa = "اشتراک‌ها و باشگاه",
                category = RecurringCashflowCategory.SUBSCRIPTIONS_INSURANCE,
                monthlyAmountToman = 6_000_000L,
                dayOfShamsiMonth = 20,
                isEssential = false,
                isActive = true
            ),
            // Inactive item -> excluded from totals
            RecurringCashflowItem(
                id = 5L,
                titleFa = "هزینه متوقف‌شده",
                category = RecurringCashflowCategory.TRANSPORT_HEALTH,
                monthlyAmountToman = 5_000_000L,
                dayOfShamsiMonth = 12,
                isEssential = false,
                isActive = false
            )
        )

        val summary = RecurringCashflowHealthEngine.evaluateHolisticHealth(
            items = items,
            currentMonthLoanInstallmentsToman = 12_000_000L,
            monthlySavingsTargetToman = 8_000_000L,
            todayJalali = "1403/09/10"
        )

        assertEquals(60_000_000L, summary.totalRecurringIncomeToman)
        assertEquals(24_000_000L, summary.totalRecurringNonLoanExpenseToman)
        assertEquals(18_000_000L, summary.essentialNonLoanExpenseToman)
        assertEquals(6_000_000L, summary.discretionaryNonLoanExpenseToman)
        assertEquals(12_000_000L, summary.currentMonthLoanInstallmentsToman)

        // Net disposable before loans: 60M - 24M = 36M
        assertEquals(36_000_000L, summary.netDisposableBeforeLoansToman)
        // Net free cashflow after loans: 36M - 12M = 24M
        assertEquals(24_000_000L, summary.netFreeCashflowAfterLoansToman)
        // Surplus after 8M savings target: 24M - 8M = 16M
        assertEquals(16_000_000L, summary.unallocatedSurplusAfterSavingsToman)

        // Percentages: 24M / 60M = 40%, 12M / 60M = 20%, Total commitments = 60%
        assertEquals(40, summary.nonLoanExpenseToIncomePercent)
        assertEquals(20, summary.loanDebtToIncomePercent)
        assertEquals(60, summary.totalCommitmentsToIncomePercent)
        assertTrue(summary.holisticHealthScore >= 75)
    }

    @Test
    fun `serializeItems and deserializeItems preserve all recurring cashflow records`() {
        val sample = RecurringCashflowHealthEngine.defaultSampleRecurringItems()
        val encoded = RecurringCashflowHealthEngine.serializeItems(sample)
        val restored = RecurringCashflowHealthEngine.deserializeItems(encoded)

        assertEquals(sample.size, restored.size)
        assertEquals(sample[0].titleFa, restored[0].titleFa)
        assertEquals(sample[0].monthlyAmountToman, restored[0].monthlyAmountToman)
        assertEquals(sample[0].category, restored[0].category)
    }
}
