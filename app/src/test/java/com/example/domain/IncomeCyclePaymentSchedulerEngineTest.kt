package com.example.domain

import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.ObligationCategory
import com.example.data.ObligationEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomeCyclePaymentSchedulerEngineTest {

    private fun sampleObligation(id: Long, title: String) = ObligationEntity(
        id = id,
        counterpartyId = 1L,
        title = title,
        category = ObligationCategory.INSTALLMENT_LOAN.name,
        principalAmountToman = 60_000_000L,
        profitOrFeeToman = 10_000_000L,
        installmentCount = 12,
        firstDueDateJalali = "1405/09/10"
    )

    @Test
    fun singleSalaryOnDay1_suggestsOptimalDayWithSettlementBufferBeforeDueDates() {
        val recurring = listOf(
            RecurringCashflowItem(
                id = 1L,
                titleFa = "حقوق ماهانه",
                category = RecurringCashflowCategory.SALARY,
                monthlyAmountToman = 50_000_000L,
                dayOfShamsiMonth = 1
            ),
            RecurringCashflowItem(
                id = 2L,
                titleFa = "مخارج خانوار",
                category = RecurringCashflowCategory.GROCERIES_LIVING,
                monthlyAmountToman = 15_000_000L,
                dayOfShamsiMonth = 10
            )
        )
        val obligations = listOf(sampleObligation(1L, "وام بانک ملت"))
        val installments = listOf(
            InstallmentEntity(
                id = 101L,
                obligationId = 1L,
                installmentNumber = 3,
                title = "قسط ۳ بانک ملت",
                dueDateJalali = "1405/09/08",
                amountToman = 6_000_000L,
                status = InstallmentStatus.PENDING.name
            )
        )

        val report = IncomeCyclePaymentSchedulerEngine.buildScheduleRecommendation(
            recurringItems = recurring,
            historicalIncomeDeposits = emptyList(),
            obligations = obligations,
            installments = installments,
            fallbackMonthlyIncomeToman = 50_000_000L,
            todayJalali = "1405/09/01"
        )

        // Should recommend day 3 or 4 (+2..+3 days after Day 1 payday, safely before Day 8 due date)
        assertTrue(
            "Expected optimal day between 2 and 5, got ${report.primaryRecommendedDay}",
            report.primaryRecommendedDay in 2..5
        )
        assertEquals(30, report.dailyEvaluations.size)
        assertEquals(100, report.onTimeCoveragePercent)
        assertTrue(report.projectedBufferOnBestDayToman > 30_000_000L)
    }

    @Test
    fun dualIncomeCycles_detectsTwoClustersAndSupportsSplitBiCycleMode() {
        val recurring = listOf(
            RecurringCashflowItem(
                id = 1L,
                titleFa = "حقوق ثابت",
                category = RecurringCashflowCategory.SALARY,
                monthlyAmountToman = 45_000_000L,
                dayOfShamsiMonth = 1
            ),
            RecurringCashflowItem(
                id = 2L,
                titleFa = "درآمد مشاوره نیمه‌ماه",
                category = RecurringCashflowCategory.FREELANCE_BUSINESS,
                monthlyAmountToman = 15_000_000L,
                dayOfShamsiMonth = 15
            )
        )
        val obligations = listOf(
            sampleObligation(1L, "وام اول ماه"),
            sampleObligation(2L, "قسط نیمه دوم")
        )
        val installments = listOf(
            InstallmentEntity(
                id = 201L,
                obligationId = 1L,
                installmentNumber = 1,
                title = "قسط ۱",
                dueDateJalali = "1405/09/07",
                amountToman = 5_000_000L,
                status = InstallmentStatus.PENDING.name
            ),
            InstallmentEntity(
                id = 202L,
                obligationId = 2L,
                installmentNumber = 1,
                title = "قسط ۲",
                dueDateJalali = "1405/09/22",
                amountToman = 4_000_000L,
                status = InstallmentStatus.PENDING.name
            )
        )

        val report = IncomeCyclePaymentSchedulerEngine.buildScheduleRecommendation(
            recurringItems = recurring,
            historicalIncomeDeposits = emptyList(),
            obligations = obligations,
            installments = installments,
            fallbackMonthlyIncomeToman = 60_000_000L,
            preferredMode = PaymentScheduleStrategyMode.SPLIT_BI_CYCLE,
            todayJalali = "1405/09/01"
        )

        assertEquals(2, report.detectedPaydays.size)
        assertNotNull(report.secondaryRecommendedDay)
        assertTrue(
            "Expected secondary day in 16..19, got ${report.secondaryRecommendedDay}",
            (report.secondaryRecommendedDay ?: 0) in 16..19
        )
        assertEquals(PaymentScheduleStrategyMode.SPLIT_BI_CYCLE, report.effectiveMode)
        assertTrue(report.scheduledActions.any { it.usesSecondaryCycle })
    }

    @Test
    fun lateMonthHistoricalIncomeCycles_wrapAroundEndOfMonthCleanly() {
        val userHistory = listOf(
            HistoricalIncomeDeposit(1L, "حقوق مهر", 55_000_000L, "1405/07/29"),
            HistoricalIncomeDeposit(2L, "حقوق آبان", 56_000_000L, "1405/08/29"),
            HistoricalIncomeDeposit(3L, "حقوق آذر", 55_000_000L, "1405/09/28")
        )
        val obligations = listOf(sampleObligation(1L, "وام مسکن"))
        val installments = listOf(
            InstallmentEntity(
                id = 301L,
                obligationId = 1L,
                installmentNumber = 5,
                title = "قسط مسکن",
                dueDateJalali = "1405/10/05",
                amountToman = 9_000_000L,
                status = InstallmentStatus.PENDING.name
            )
        )

        val report = IncomeCyclePaymentSchedulerEngine.buildScheduleRecommendation(
            recurringItems = emptyList(),
            historicalIncomeDeposits = userHistory,
            obligations = obligations,
            installments = installments,
            fallbackMonthlyIncomeToman = 55_000_000L,
            todayJalali = "1405/09/29"
        )

        // Payday around 29th -> +2..+3 days wraps to day 1 or 2 of next Shamsi month, before Day 5 due date!
        assertTrue(
            "Expected wrap-around recommended day in {30, 1, 2, 3}, got ${report.primaryRecommendedDay}",
            report.primaryRecommendedDay in setOf(30, 1, 2, 3)
        )
        assertEquals(29, report.detectedPaydays.first().medianDayOfMonth)
    }

    @Test
    fun serializeAndDeserializeHistoricalDeposits_roundTripsCleanly() {
        val original = listOf(
            HistoricalIncomeDeposit(10L, "حقوق آبان", 48_500_000L, "1405/08/01", true),
            HistoricalIncomeDeposit(11L, "پاداش پروژه", 12_000_000L, "1405/08/16", true)
        )
        val json = IncomeCyclePaymentSchedulerEngine.serializeHistoricalDeposits(original)
        val restored = IncomeCyclePaymentSchedulerEngine.deserializeHistoricalDeposits(json)

        assertEquals(2, restored.size)
        assertEquals("حقوق آبان", restored[0].titleFa)
        assertEquals(48_500_000L, restored[0].amountToman)
        assertEquals("1405/08/01", restored[0].receivedDateJalali)
        assertEquals(16, restored[1].dayOfShamsiMonth)
    }
}
