package com.example.domain

import com.example.data.CounterpartyEntity
import com.example.data.EntityCategory
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.ObligationCategory
import com.example.data.ObligationEntity
import com.example.data.TrustPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebtPayoffStrategyRankingEngineTest {

    private val counterparties = listOf(
        CounterpartyEntity(id = 1L, name = "دیجی‌پی", category = EntityCategory.BNPL_PLATFORM.name),
        CounterpartyEntity(id = 2L, name = "بانک ملی", category = EntityCategory.BANK.name),
        CounterpartyEntity(id = 3L, name = "دوست", category = EntityCategory.PERSON.name)
    )

    private fun sampleObligationsAndInstallments(): Pair<List<ObligationEntity>, List<InstallmentEntity>> {
        // 1. High APR (BNPL ~33%+) + Urgent (due in 3 days)
        val highAprUrgent = ObligationEntity(
            id = 10L,
            counterpartyId = 1L,
            title = "کارت اعتباری پرهزینه",
            category = ObligationCategory.BNPL_PURCHASE.name,
            principalAmountToman = 30_000_000L,
            profitOrFeeToman = 6_000_000L,
            installmentCount = 12,
            firstDueDateJalali = "1403/09/04",
            trustPriority = TrustPriority.HIGH.name
        )
        // 2. Zero APR personal loan, due in 25 days (Low urgency, 0% interest)
        val zeroAprRelaxed = ObligationEntity(
            id = 20L,
            counterpartyId = 3L,
            title = "قرض خانوادگی بدون سود",
            category = ObligationCategory.PERSONAL_DEBT.name,
            principalAmountToman = 20_000_000L,
            profitOrFeeToman = 0L,
            installmentCount = 10,
            firstDueDateJalali = "1403/09/26",
            trustPriority = TrustPriority.NORMAL.name
        )
        // 3. Moderate APR bank loan, OVERDUE by 5 days with late fee & CRITICAL trust priority
        val overdueCritical = ObligationEntity(
            id = 30L,
            counterpartyId = 2L,
            title = "چک / وام معوقه بانکی",
            category = ObligationCategory.INSTALLMENT_LOAN.name,
            principalAmountToman = 40_000_000L,
            profitOrFeeToman = 7_000_000L,
            installmentCount = 12,
            firstDueDateJalali = "1403/08/26",
            trustPriority = TrustPriority.CRITICAL.name
        )
        // 4. Receivable (must be excluded!)
        val receivable = ObligationEntity(
            id = 40L,
            counterpartyId = 3L,
            title = "طلب از همکار",
            category = ObligationCategory.PERSONAL_RECEIVABLE.name,
            principalAmountToman = 15_000_000L,
            firstDueDateJalali = "1403/09/02"
        )

        val installments = mutableListOf<InstallmentEntity>()
        // 12 unpaid installments for highAprUrgent (3M each = 36M total)
        for (i in 0 until 12) {
            installments += InstallmentEntity(
                id = 100L + i,
                obligationId = 10L,
                installmentNumber = i + 1,
                title = "قسط ${i + 1}",
                dueDateJalali = com.example.core.JalaliCalendarEngine.addJalaliMonths("1403/09/04", i),
                amountToman = 3_000_000L,
                paidAmountToman = 0L,
                status = InstallmentStatus.PENDING.name
            )
        }
        // 10 unpaid installments for zeroAprRelaxed (2M each = 20M total)
        for (i in 0 until 10) {
            installments += InstallmentEntity(
                id = 200L + i,
                obligationId = 20L,
                installmentNumber = i + 1,
                title = "قسط ${i + 1}",
                dueDateJalali = com.example.core.JalaliCalendarEngine.addJalaliMonths("1403/09/26", i),
                amountToman = 2_000_000L,
                paidAmountToman = 0L,
                status = InstallmentStatus.PENDING.name
            )
        }
        // 12 unpaid installments for overdueCritical (first is OVERDUE with late fee)
        for (i in 0 until 12) {
            installments += InstallmentEntity(
                id = 300L + i,
                obligationId = 30L,
                installmentNumber = i + 1,
                title = "قسط ${i + 1}",
                dueDateJalali = com.example.core.JalaliCalendarEngine.addJalaliMonths("1403/08/26", i),
                amountToman = 3_900_000L,
                paidAmountToman = 0L,
                lateFeeToman = if (i == 0) 350_000L else 0L,
                status = if (i == 0) InstallmentStatus.OVERDUE.name else InstallmentStatus.PENDING.name
            )
        }
        // Receivable installment (should be ignored)
        installments += InstallmentEntity(
            id = 400L,
            obligationId = 40L,
            installmentNumber = 1,
            title = "طلب",
            dueDateJalali = "1403/09/02",
            amountToman = 15_000_000L,
            paidAmountToman = 0L,
            status = InstallmentStatus.PENDING.name
        )

        return listOf(highAprUrgent, zeroAprRelaxed, overdueCritical, receivable) to installments
    }

    @Test
    fun buildStrategyReport_combinedModeRanksOverdueHighRateAboveZeroRateRelaxed() {
        val (obs, insts) = sampleObligationsAndInstallments()
        val report = DebtPayoffStrategyRankingEngine.buildStrategyReport(
            obligations = obs,
            installments = insts,
            counterparties = counterparties,
            rankingMode = PayoffRankingMode.INTEREST_AND_URGENCY,
            extraMonthlyBudgetToman = 3_000_000L,
            todayJalali = "1403/09/01"
        )

        // Excludes receivable -> 3 ranked debts
        assertEquals(3, report.rankedDebts.size)
        assertEquals(1, report.rankedDebts[0].rank)
        assertEquals(2, report.rankedDebts[1].rank)
        assertEquals(3, report.rankedDebts[2].rank)

        // Zero-APR relaxed debt must be ranked last (#3)
        assertEquals(20L, report.rankedDebts.last().obligation.id)
        // Top ranked debt must have strictly higher composite score than last
        assertTrue(report.rankedDebts.first().compositePriorityScore > report.rankedDebts.last().compositePriorityScore)
        // Overdue critical debt must be flagged CRITICAL
        val overdueItem = report.rankedDebts.first { it.obligation.id == 30L }
        assertEquals(DebtUrgencyLevel.CRITICAL, overdueItem.urgencyLevel)
        assertTrue(overdueItem.overdueInstallmentCount >= 1)

        // Extra monthly budget must yield positive interest savings and fewer months vs minimum-only
        assertTrue(report.totalEstimatedInterestSavedVsBaselineToman > 0L)
        assertTrue(report.monthsSavedVsMinimumOnly > 0)
        assertTrue(report.interestSavedByOrderVsReverseToman > 0L)
    }

    @Test
    fun buildStrategyReport_maxInterestModeStrictlyPrioritizesHighestAprFirst() {
        val (obs, insts) = sampleObligationsAndInstallments()
        val report = DebtPayoffStrategyRankingEngine.buildStrategyReport(
            obligations = obs,
            installments = insts,
            counterparties = counterparties,
            rankingMode = PayoffRankingMode.MAX_INTEREST_SAVINGS,
            extraMonthlyBudgetToman = 4_000_000L,
            todayJalali = "1403/09/01"
        )

        assertEquals(3, report.rankedDebts.size)
        assertTrue(report.rankedDebts[0].effectiveAnnualRatePercent >= report.rankedDebts[1].effectiveAnnualRatePercent)
        assertTrue(report.rankedDebts[1].effectiveAnnualRatePercent >= report.rankedDebts[2].effectiveAnnualRatePercent)
        assertEquals(10L, report.rankedDebts[0].obligation.id)
        assertEquals(20L, report.rankedDebts[2].obligation.id)
        assertTrue(report.interestSavedByOrderVsReverseToman > 0L)
    }

    @Test
    fun buildStrategyReport_urgencyFirstModePrioritizesOverdueDebtFirst() {
        val (obs, insts) = sampleObligationsAndInstallments()
        val report = DebtPayoffStrategyRankingEngine.buildStrategyReport(
            obligations = obs,
            installments = insts,
            counterparties = counterparties,
            rankingMode = PayoffRankingMode.URGENCY_FIRST,
            extraMonthlyBudgetToman = 2_000_000L,
            todayJalali = "1403/09/01"
        )

        assertEquals(30L, report.rankedDebts.first().obligation.id)
        assertEquals(DebtUrgencyLevel.CRITICAL, report.rankedDebts.first().urgencyLevel)
    }

    @Test
    fun buildStrategyReport_noActiveDebtsReturnsGracefulEmptyReport() {
        val report = DebtPayoffStrategyRankingEngine.buildStrategyReport(
            obligations = emptyList(),
            installments = emptyList(),
            counterparties = emptyList(),
            todayJalali = "1403/09/01"
        )

        assertTrue(report.rankedDebts.isEmpty())
        assertEquals(0L, report.totalRemainingDebtToman)
        assertEquals(0, report.projectedDebtFreeMonths)
    }
}
