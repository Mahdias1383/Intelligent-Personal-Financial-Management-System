package com.example.domain

import com.example.data.CounterpartyEntity
import com.example.data.EntityCategory
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.ObligationCategory
import com.example.data.ObligationEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DebtSnowballCalculatorEngineTest {

    @Test
    fun `calculateSnowballPlan strictly prioritizes smallest balances first and rolls over freed minimum payments`() {
        // Deliberately unsorted input where the largest debt has the highest APR
        val accounts = listOf(
            SnowballDebtAccount(
                id = "large_bank",
                titleFa = "وام کلان بانکی",
                counterpartyFa = "بانک ملی",
                balanceToman = 30_000_000L,
                minimumMonthlyPaymentToman = 3_000_000L,
                annualInterestRatePercent = 24.0,
                categoryTag = DebtCategoryTag.BANK_LOAN
            ),
            SnowballDebtAccount(
                id = "tiny_personal",
                titleFa = "قرض کوچک شخصی",
                counterpartyFa = "دوست",
                balanceToman = 4_000_000L,
                minimumMonthlyPaymentToman = 1_000_000L,
                annualInterestRatePercent = 0.0,
                categoryTag = DebtCategoryTag.PERSONAL
            ),
            SnowballDebtAccount(
                id = "mid_bnpl",
                titleFa = "خرید اقساطی اسنپ‌پی",
                counterpartyFa = "اسنپ‌پی",
                balanceToman = 10_000_000L,
                minimumMonthlyPaymentToman = 2_000_000L,
                annualInterestRatePercent = 0.0,
                categoryTag = DebtCategoryTag.CREDIT_CARD
            )
        )

        // Extra snowball budget = 2M/month, total minimums = 6M/month => total commitment = 8M/month
        val plan = DebtSnowballCalculatorEngine.calculateSnowballPlan(
            accounts = accounts,
            extraMonthlyBudgetToman = 2_000_000L,
            startDateJalali = "1403/09/01"
        )

        assertEquals(3, plan.prioritizedMilestones.size)
        // Strict smallest-balance order: 4M -> 10M -> 30M
        assertEquals("tiny_personal", plan.prioritizedMilestones[0].account.id)
        assertEquals(1, plan.prioritizedMilestones[0].priorityRank)
        assertEquals("mid_bnpl", plan.prioritizedMilestones[1].account.id)
        assertEquals(2, plan.prioritizedMilestones[1].priorityRank)
        assertEquals("large_bank", plan.prioritizedMilestones[2].account.id)
        assertEquals(3, plan.prioritizedMilestones[2].priorityRank)

        // First Quick Win: 4M balance with 1M min + 2M extra = 3M/month => eliminated in Month 2!
        val firstWin = plan.firstQuickWinMilestone
        assertNotNull(firstWin)
        assertEquals("tiny_personal", firstWin!!.account.id)
        assertEquals(2, firstWin.payoffMonthIndex)
        assertEquals(3_000_000L, firstWin.initialAttackPaymentToman)
        assertEquals(1_000_000L, firstWin.rolloverFreedMonthlyToman)
        assertEquals(3_000_000L, firstWin.cumulativeSnowballPowerToman)

        // Second target receives rolled-over 1M minimum + 2M extra = 3M snowball on top of its 2M minimum (5M attack)
        val secondMilestone = plan.prioritizedMilestones[1]
        assertEquals(5_000_000L, secondMilestone.initialAttackPaymentToman)
        assertTrue(secondMilestone.payoffMonthIndex > firstWin.payoffMonthIndex)

        // Total payoff must finish much faster than minimum-only baseline
        assertTrue(plan.snowballMonthsToDebtFree < plan.minimumOnlyMonthsToDebtFree)
        assertTrue(plan.monthsSavedWithSnowball > 0)
        assertEquals(0L, plan.monthlyLedger.last().remainingTotalDebtToman)
        assertEquals(3, plan.monthlyLedger.last().cumulativeClearedCount)
        assertTrue(plan.momentumScorePercent in 25..100)
        assertTrue(plan.actionableStrategySummaryFa.contains("قرض کوچک شخصی"))
    }

    @Test
    fun `extractAccountsFromPortfolio filters out receivables and paid debts and sorts smallest balance first`() {
        val counterparties = listOf(
            CounterpartyEntity(id = 1L, name = "بانک سپه", category = EntityCategory.BANK.name),
            CounterpartyEntity(id = 2L, name = "رضا", category = EntityCategory.PERSON.name)
        )
        val obligations = listOf(
            ObligationEntity(
                id = 10L,
                counterpartyId = 1L,
                title = "وام بانکی بزرگ",
                category = ObligationCategory.INSTALLMENT_LOAN.name,
                principalAmountToman = 50_000_000L,
                firstDueDateJalali = "1403/09/01"
            ),
            ObligationEntity(
                id = 20L,
                counterpartyId = 2L,
                title = "قرض کوچک رضا",
                category = ObligationCategory.PERSONAL_DEBT.name,
                principalAmountToman = 6_000_000L,
                firstDueDateJalali = "1403/09/05"
            ),
            ObligationEntity(
                id = 30L,
                counterpartyId = 2L,
                title = "طلب از همکار",
                category = ObligationCategory.PERSONAL_RECEIVABLE.name,
                principalAmountToman = 10_000_000L,
                firstDueDateJalali = "1403/09/10"
            )
        )
        val installments = listOf(
            InstallmentEntity(
                id = 101L,
                obligationId = 10L,
                installmentNumber = 1,
                title = "قسط ۱",
                dueDateJalali = "1403/09/01",
                amountToman = 25_000_000L,
                paidAmountToman = 0L,
                status = InstallmentStatus.PENDING.name
            ),
            InstallmentEntity(
                id = 102L,
                obligationId = 10L,
                installmentNumber = 2,
                title = "قسط ۲",
                dueDateJalali = "1403/10/01",
                amountToman = 25_000_000L,
                paidAmountToman = 0L,
                status = InstallmentStatus.PENDING.name
            ),
            InstallmentEntity(
                id = 201L,
                obligationId = 20L,
                installmentNumber = 1,
                title = "قسط قرض رضا",
                dueDateJalali = "1403/09/05",
                amountToman = 6_000_000L,
                paidAmountToman = 2_000_000L,
                status = InstallmentStatus.PARTIAL.name
            ),
            InstallmentEntity(
                id = 301L,
                obligationId = 30L,
                installmentNumber = 1,
                title = "طلب",
                dueDateJalali = "1403/09/10",
                amountToman = 10_000_000L,
                paidAmountToman = 0L,
                status = InstallmentStatus.PENDING.name
            )
        )

        val extracted = DebtSnowballCalculatorEngine.extractAccountsFromPortfolio(
            obligations = obligations,
            installments = installments,
            counterparties = counterparties
        )

        // Receivable (id=30) must be excluded; remaining 2 debts sorted smallest balance first (4M then 50M)
        assertEquals(2, extracted.size)
        assertEquals("ob_20", extracted[0].id)
        assertEquals(4_000_000L, extracted[0].balanceToman)
        assertEquals("ob_10", extracted[1].id)
        assertEquals(50_000_000L, extracted[1].balanceToman)
    }

    @Test
    fun `calculateSnowballPlan handles empty debt list gracefully`() {
        val report = DebtSnowballCalculatorEngine.calculateSnowballPlan(
            accounts = emptyList(),
            extraMonthlyBudgetToman = 5_000_000L,
            startDateJalali = "1403/09/01"
        )

        assertEquals(0, report.totalDebtsCount)
        assertEquals(0, report.snowballMonthsToDebtFree)
        assertEquals(100, report.momentumScorePercent)
        assertTrue(report.prioritizedMilestones.isEmpty())
    }
}
