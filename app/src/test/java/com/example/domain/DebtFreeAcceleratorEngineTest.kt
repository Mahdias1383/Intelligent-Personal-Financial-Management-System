package com.example.domain

import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.ObligationCategory
import com.example.data.ObligationEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebtFreeAcceleratorEngineTest {

    @Test
    fun simulateAcceleration_extraMonthlyPaymentReducesMonthsAndSavesInterest() {
        val baseReport = DebtFreeAcceleratorEngine.simulateAcceleration(
            totalRemainingDebtToman = 120_000_000L,
            baseMonthlyPaymentToman = 6_000_000L,
            extraMonthlyPaymentToman = 0L,
            annualInterestRatePercent = 18.0,
            startDateJalali = "1403/09/01"
        )
        val acceleratedReport = DebtFreeAcceleratorEngine.simulateAcceleration(
            totalRemainingDebtToman = 120_000_000L,
            baseMonthlyPaymentToman = 6_000_000L,
            extraMonthlyPaymentToman = 4_000_000L,
            annualInterestRatePercent = 18.0,
            startDateJalali = "1403/09/01"
        )

        assertEquals(0, baseReport.monthsSaved)
        assertEquals(0L, baseReport.totalInterestSavedToman)

        assertTrue(
            "Extra monthly payment must strictly reduce months to debt-free",
            acceleratedReport.acceleratedMonthsToDebtFree < acceleratedReport.baselineMonthsToDebtFree
        )
        assertEquals(
            acceleratedReport.baselineMonthsToDebtFree - acceleratedReport.acceleratedMonthsToDebtFree,
            acceleratedReport.monthsSaved
        )
        assertTrue(
            "Extra monthly payment on 18% APR debt must save positive interest",
            acceleratedReport.totalInterestSavedToman > 0L
        )
        assertEquals(10_000_000L, acceleratedReport.newTotalMonthlyPaymentToman)
        assertTrue(acceleratedReport.accelerationPercent in 1..99)
    }

    @Test
    fun simulateAcceleration_oneTimeLumpSumFurtherAcceleratesDebtFreeDate() {
        val withoutLump = DebtFreeAcceleratorEngine.simulateAcceleration(
            totalRemainingDebtToman = 100_000_000L,
            baseMonthlyPaymentToman = 5_000_000L,
            extraMonthlyPaymentToman = 2_000_000L,
            annualInterestRatePercent = 15.0,
            oneTimeLumpSumBoostToman = 0L,
            startDateJalali = "1403/09/01"
        )
        val withLump = DebtFreeAcceleratorEngine.simulateAcceleration(
            totalRemainingDebtToman = 100_000_000L,
            baseMonthlyPaymentToman = 5_000_000L,
            extraMonthlyPaymentToman = 2_000_000L,
            annualInterestRatePercent = 15.0,
            oneTimeLumpSumBoostToman = 25_000_000L,
            startDateJalali = "1403/09/01"
        )

        assertTrue(
            "One-time lump sum boost must finish sooner than recurring extra alone",
            withLump.acceleratedMonthsToDebtFree < withoutLump.acceleratedMonthsToDebtFree
        )
        assertTrue(
            "One-time lump sum boost must save more interest",
            withLump.totalInterestSavedToman > withoutLump.totalInterestSavedToman
        )
    }

    @Test
    fun simulateAcceleration_trajectoryAndQuickStepComparisonsAreMonotonicAndZeroOut() {
        val report = DebtFreeAcceleratorEngine.simulateAcceleration(
            totalRemainingDebtToman = 90_000_000L,
            baseMonthlyPaymentToman = 4_500_000L,
            extraMonthlyPaymentToman = 3_000_000L,
            annualInterestRatePercent = 12.0,
            startDateJalali = "1403/09/01"
        )

        assertTrue(report.trajectoryPoints.isNotEmpty())
        assertEquals(0, report.trajectoryPoints.first().monthIndex)
        assertEquals(90_000_000L, report.trajectoryPoints.first().baselineRemainingToman)
        assertEquals(0L, report.trajectoryPoints.last().baselineRemainingToman)
        assertEquals(0L, report.trajectoryPoints[report.acceleratedMonthsToDebtFree].acceleratedRemainingToman)

        assertEquals(4, report.quickStepComparisons.size)
        for (i in 1 until report.quickStepComparisons.size) {
            val prev = report.quickStepComparisons[i - 1]
            val curr = report.quickStepComparisons[i]
            assertTrue(
                "Higher extra monthly preset should finish in <= months",
                curr.monthsToDebtFree <= prev.monthsToDebtFree
            )
            assertTrue(
                "Higher extra monthly preset should save >= months",
                curr.monthsSaved >= prev.monthsSaved
            )
        }
    }

    @Test
    fun derivePortfolioBaseline_sumsOnlyUnpaidDebtObligationsAndExcludesReceivables() {
        val bankLoan = ObligationEntity(
            id = 1L,
            title = "وام مسکن",
            counterpartyId = 10L,
            category = ObligationCategory.INSTALLMENT_LOAN.name,
            principalAmountToman = 40_000_000L,
            firstDueDateJalali = "1403/01/01"
        )
        val personalDebt = ObligationEntity(
            id = 2L,
            title = "قرض شخصی",
            counterpartyId = 11L,
            category = ObligationCategory.PERSONAL_DEBT.name,
            principalAmountToman = 10_000_000L,
            firstDueDateJalali = "1403/05/01"
        )
        val receivable = ObligationEntity(
            id = 3L,
            title = "طلب از دوست",
            counterpartyId = 12L,
            category = ObligationCategory.PERSONAL_RECEIVABLE.name,
            principalAmountToman = 50_000_000L,
            firstDueDateJalali = "1403/02/01"
        )

        val installments = listOf(
            // Bank loan: 2 paid, 2 unpaid (12M each = 24M remaining, total payable 48M vs 40M principal -> positive APR)
            InstallmentEntity(id = 1, obligationId = 1L, installmentNumber = 1, title = "قسط ۱", dueDateJalali = "1403/07/01", amountToman = 12_000_000L, paidAmountToman = 12_000_000L, status = InstallmentStatus.PAID.name),
            InstallmentEntity(id = 2, obligationId = 1L, installmentNumber = 2, title = "قسط ۲", dueDateJalali = "1403/08/01", amountToman = 12_000_000L, paidAmountToman = 12_000_000L, status = InstallmentStatus.PAID.name),
            InstallmentEntity(id = 3, obligationId = 1L, installmentNumber = 3, title = "قسط ۳", dueDateJalali = "1403/09/01", amountToman = 12_000_000L, paidAmountToman = 0L, status = InstallmentStatus.PENDING.name),
            InstallmentEntity(id = 4, obligationId = 1L, installmentNumber = 4, title = "قسط ۴", dueDateJalali = "1403/10/01", amountToman = 12_000_000L, paidAmountToman = 0L, status = InstallmentStatus.PENDING.name),
            // Personal debt: 2 unpaid (5M each = 10M remaining)
            InstallmentEntity(id = 5, obligationId = 2L, installmentNumber = 1, title = "قسط ۱ قرض", dueDateJalali = "1403/09/15", amountToman = 5_000_000L, paidAmountToman = 0L, status = InstallmentStatus.PENDING.name),
            InstallmentEntity(id = 6, obligationId = 2L, installmentNumber = 2, title = "قسط ۲ قرض", dueDateJalali = "1403/10/15", amountToman = 5_000_000L, paidAmountToman = 0L, status = InstallmentStatus.PENDING.name),
            // Receivable: must be ignored
            InstallmentEntity(id = 7, obligationId = 3L, installmentNumber = 1, title = "طلب", dueDateJalali = "1403/09/20", amountToman = 50_000_000L, paidAmountToman = 0L, status = InstallmentStatus.PENDING.name)
        )

        val baseline = DebtFreeAcceleratorEngine.derivePortfolioBaseline(
            obligations = listOf(bankLoan, personalDebt, receivable),
            installments = installments
        )

        assertEquals(2, baseline.activeDebtsCount)
        assertEquals(34_000_000L, baseline.totalRemainingDebtToman)
        assertEquals(17_000_000L, baseline.baseMonthlyPaymentToman)
        assertTrue(baseline.weightedAnnualRatePercent > 0.0)
    }

    @Test
    fun simulateAcceleration_zeroDebtReturnsGracefulZeroState() {
        val report = DebtFreeAcceleratorEngine.simulateAcceleration(
            totalRemainingDebtToman = 0L,
            baseMonthlyPaymentToman = 0L,
            extraMonthlyPaymentToman = 2_000_000L,
            startDateJalali = "1403/09/01"
        )

        assertEquals(0, report.baselineMonthsToDebtFree)
        assertEquals(0, report.acceleratedMonthsToDebtFree)
        assertEquals(0, report.monthsSaved)
        assertTrue(report.trajectoryPoints.isEmpty())
    }
}
