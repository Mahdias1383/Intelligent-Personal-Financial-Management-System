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

class DebtConsolidationEngineTest {

    @Test
    fun simulateConsolidation_lowInterestLoanForHighInterestDebtsIsStronglyAdvantageous() {
        val highCostDebts = listOf(
            ConsolidatableDebtItem(
                id = 1L,
                titleFa = "کارت اعتباری اول",
                counterpartyFa = "دیجی‌پی",
                categoryTag = DebtCategoryTag.CREDIT_CARD,
                remainingPrincipalToman = 30_000_000L,
                remainingScheduledPayableToman = 39_600_000L,
                remainingEmbeddedInterestToman = 9_600_000L,
                currentMonthlyPaymentToman = 3_300_000L,
                remainingMonths = 12,
                effectiveAnnualRatePercent = 34.0
            ),
            ConsolidatableDebtItem(
                id = 2L,
                titleFa = "خرید اقساطی دوم",
                counterpartyFa = "اسنپ‌پی",
                categoryTag = DebtCategoryTag.CREDIT_CARD,
                remainingPrincipalToman = 40_000_000L,
                remainingScheduledPayableToman = 53_000_000L,
                remainingEmbeddedInterestToman = 13_000_000L,
                currentMonthlyPaymentToman = 3_533_333L,
                remainingMonths = 15,
                effectiveAnnualRatePercent = 30.0
            )
        )

        val report = DebtConsolidationEngine.simulateConsolidation(
            allDebts = highCostDebts,
            selectedDebtIds = setOf(1L, 2L),
            consolidationAnnualRatePercent = 12.0,
            consolidationTermMonths = 18,
            originationFeePercent = 1.0,
            monthlyIncomeToman = 35_000_000L,
            startDateJalali = "1403/09/01"
        )

        assertEquals(ConsolidationVerdict.STRONGLY_ADVANTAGEOUS, report.verdict)
        assertTrue("Net total savings must be positive", report.netTotalSavingsToman > 0L)
        assertTrue("Monthly payment should drop meaningfully", report.monthlyCashflowReliefToman > 0L)
        assertTrue("Rate spread must be positive", report.rateSpreadPercent > 15.0)
        assertNotNull("Break-even months should be calculated when fee > 0", report.breakEvenMonths)
        assertTrue(report.breakEvenMonths!! in 1..18)
        assertNotNull(report.monthlyDtiBeforePercent)
        assertNotNull(report.monthlyDtiAfterPercent)
        assertTrue(report.monthlyDtiAfterPercent!! < report.monthlyDtiBeforePercent!!)
    }

    @Test
    fun simulateConsolidation_highRateLoanForZeroInterestDebtIsNotAdvantageous() {
        val zeroInterestDebt = listOf(
            ConsolidatableDebtItem(
                id = 10L,
                titleFa = "قرض خانوادگی بدون سود",
                counterpartyFa = "خانواده",
                categoryTag = DebtCategoryTag.PERSONAL,
                remainingPrincipalToman = 50_000_000L,
                remainingScheduledPayableToman = 50_000_000L,
                remainingEmbeddedInterestToman = 0L,
                currentMonthlyPaymentToman = 2_500_000L,
                remainingMonths = 20,
                effectiveAnnualRatePercent = 0.0
            )
        )

        val report = DebtConsolidationEngine.simulateConsolidation(
            allDebts = zeroInterestDebt,
            selectedDebtIds = setOf(10L),
            consolidationAnnualRatePercent = 23.0,
            consolidationTermMonths = 18,
            originationFeePercent = 2.0,
            startDateJalali = "1403/09/01"
        )

        assertEquals(ConsolidationVerdict.NOT_ADVANTAGEOUS, report.verdict)
        assertTrue("Consolidating 0% debt into 23% loan must yield negative net savings", report.netTotalSavingsToman < 0L)
        assertTrue("Rate spread must be negative", report.rateSpreadPercent < 0.0)
    }

    @Test
    fun simulateConsolidation_longerTermLowersMonthlyBillButCostsMoreTotalIsCashflowTradeoff() {
        val shortLowRateDebt = listOf(
            ConsolidatableDebtItem(
                id = 20L,
                titleFa = "وام کوتاه‌مدت کم‌بهره",
                counterpartyFa = "صندوق",
                categoryTag = DebtCategoryTag.BANK_LOAN,
                remainingPrincipalToman = 40_000_000L,
                remainingScheduledPayableToman = 41_600_000L,
                remainingEmbeddedInterestToman = 1_600_000L,
                currentMonthlyPaymentToman = 5_200_000L,
                remainingMonths = 8,
                effectiveAnnualRatePercent = 6.0
            )
        )

        val report = DebtConsolidationEngine.simulateConsolidation(
            allDebts = shortLowRateDebt,
            selectedDebtIds = setOf(20L),
            consolidationAnnualRatePercent = 18.0,
            consolidationTermMonths = 36,
            originationFeePercent = 0.0,
            startDateJalali = "1403/09/01"
        )

        assertEquals(ConsolidationVerdict.CASHFLOW_RELIEF_TRADEOFF, report.verdict)
        assertTrue("Monthly bill drops due to longer term", report.monthlyCashflowReliefToman > 0L)
        assertTrue("Total cost increases due to higher APR & longer term", report.netTotalSavingsToman < 0L)
    }

    @Test
    fun recommendOptimalSubsetIds_excludesLowRateDebtsBelowNewLoanApr() {
        val sample = DebtConsolidationEngine.sampleScenarioDebts()
        // Sample has 34%, 28.5%, 23%, and 4% debts
        val optimalAt14 = DebtConsolidationEngine.recommendOptimalSubsetIds(
            allDebts = sample,
            consolidationAnnualRatePercent = 14.0
        )

        assertEquals(3, optimalAt14.size)
        assertTrue(-101L in optimalAt14)
        assertTrue(-102L in optimalAt14)
        assertTrue(-103L in optimalAt14)
        assertTrue("4% Qarz-al-Hasanah loan should NOT be selected for a 14% consolidation loan", -104L !in optimalAt14)
    }

    @Test
    fun extractConsolidatableDebts_excludesReceivablesAndComputesPrincipalAndApr() {
        val counterparties = listOf(
            CounterpartyEntity(id = 1L, name = "دیجی‌پی", category = EntityCategory.BNPL_PLATFORM.name),
            CounterpartyEntity(id = 2L, name = "دوست", category = EntityCategory.PERSON.name)
        )
        val obligations = listOf(
            ObligationEntity(
                id = 100L,
                counterpartyId = 1L,
                title = "خرید اقساطی لپ‌تاپ",
                category = ObligationCategory.BNPL_PURCHASE.name,
                principalAmountToman = 40_000_000L,
                profitOrFeeToman = 8_000_000L,
                installmentCount = 4,
                firstDueDateJalali = "1403/07/01"
            ),
            ObligationEntity(
                id = 200L,
                counterpartyId = 2L,
                title = "طلب از دوست",
                category = ObligationCategory.PERSONAL_RECEIVABLE.name,
                principalAmountToman = 20_000_000L,
                firstDueDateJalali = "1403/08/01"
            )
        )
        val installments = listOf(
            InstallmentEntity(id = 1, obligationId = 100L, installmentNumber = 1, title = "قسط ۱", dueDateJalali = "1403/07/01", amountToman = 12_000_000L, paidAmountToman = 12_000_000L, status = InstallmentStatus.PAID.name),
            InstallmentEntity(id = 2, obligationId = 100L, installmentNumber = 2, title = "قسط ۲", dueDateJalali = "1403/08/01", amountToman = 12_000_000L, paidAmountToman = 0L, status = InstallmentStatus.PENDING.name),
            InstallmentEntity(id = 3, obligationId = 100L, installmentNumber = 3, title = "قسط ۳", dueDateJalali = "1403/09/01", amountToman = 12_000_000L, paidAmountToman = 0L, status = InstallmentStatus.PENDING.name),
            InstallmentEntity(id = 4, obligationId = 100L, installmentNumber = 4, title = "قسط ۴", dueDateJalali = "1403/10/01", amountToman = 12_000_000L, paidAmountToman = 0L, status = InstallmentStatus.PENDING.name),
            InstallmentEntity(id = 5, obligationId = 200L, installmentNumber = 1, title = "طلب", dueDateJalali = "1403/09/01", amountToman = 20_000_000L, paidAmountToman = 0L, status = InstallmentStatus.PENDING.name)
        )

        val extracted = DebtConsolidationEngine.extractConsolidatableDebts(
            obligations = obligations,
            installments = installments,
            counterparties = counterparties
        )

        assertEquals(1, extracted.size)
        val item = extracted.first()
        assertEquals(100L, item.id)
        assertEquals(36_000_000L, item.remainingScheduledPayableToman)
        assertEquals(3, item.remainingMonths)
        assertEquals(12_000_000L, item.currentMonthlyPaymentToman)
        assertTrue(item.remainingEmbeddedInterestToman > 0L)
        assertTrue(item.effectiveAnnualRatePercent > 0.0)
    }
}
