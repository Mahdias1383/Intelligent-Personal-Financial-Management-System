package com.example

import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.ObligationCategory
import com.example.data.ObligationEntity
import com.example.domain.FinancialForecastMlEngine
import com.example.domain.ForecastRiskTier
import com.example.domain.ForecastScenarioMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialForecastMlEngineTest {

    private fun sampleObligation(id: Long = 1L, isReceivable: Boolean = false): ObligationEntity {
        return ObligationEntity(
            id = id,
            counterpartyId = 1L,
            title = if (isReceivable) "طلب" else "وام آزمایشی",
            category = if (isReceivable) ObligationCategory.PERSONAL_RECEIVABLE.name else ObligationCategory.INSTALLMENT_LOAN.name,
            principalAmountToman = 50_000_000L,
            firstDueDateJalali = "1405/08/15"
        )
    }

    private fun sampleInstallment(
        id: Long,
        obligationId: Long = 1L,
        dueDate: String,
        amount: Long,
        paid: Long = 0L,
        lateFee: Long = 0L,
        status: InstallmentStatus = InstallmentStatus.PENDING
    ): InstallmentEntity {
        return InstallmentEntity(
            id = id,
            obligationId = obligationId,
            installmentNumber = id.toInt(),
            title = "قسط $id",
            dueDateJalali = dueDate,
            amountToman = amount,
            paidAmountToman = paid,
            lateFeeToman = lateFee,
            status = status.name
        )
    }

    @Test
    fun detectsUpcomingDeficitMonthsAndRecommendsPositiveReserve() {
        val obligations = listOf(sampleObligation(1L))
        // Income 50M, living 50% (25M) => max debt budget = 25M/mo.
        // Month +1 (1405/09) has 38M scheduled => guaranteed deficit of >= 13M!
        val installments = listOf(
            sampleInstallment(1, dueDate = "1405/08/15", amount = 10_000_000L),
            sampleInstallment(2, dueDate = "1405/09/15", amount = 38_000_000L),
            sampleInstallment(3, dueDate = "1405/10/15", amount = 12_000_000L)
        )

        val report = FinancialForecastMlEngine.generateForecast(
            installments = installments,
            obligations = obligations,
            monthlyIncomeToman = 50_000_000L,
            livingExpensePercent = 50,
            todayJalali = "1405/08/05",
            horizonCount = 12
        )

        assertEquals(12, report.horizonMonths.size)
        assertTrue("Expected at least one deficit month", report.deficitMonths.isNotEmpty())
        assertNotNull(report.worstDeficitMonth)
        assertEquals(9, report.worstDeficitMonth?.month)
        assertTrue(report.worstDeficitMonth!!.projectedNetBalanceToman < 0L)
        assertTrue(report.worstDeficitMonth!!.deficitProbability >= 0.65f)
        assertTrue(report.recommendedMonthlyReserveToman > 0L)
    }

    @Test
    fun healthyLowDebtPortfolioProducesZeroDeficitsAndOrderedConfidenceBands() {
        val obligations = listOf(sampleObligation(1L))
        val installments = listOf(
            sampleInstallment(1, dueDate = "1405/08/15", amount = 5_000_000L),
            sampleInstallment(2, dueDate = "1405/09/15", amount = 5_000_000L),
            sampleInstallment(3, dueDate = "1405/10/15", amount = 5_000_000L)
        )

        val report = FinancialForecastMlEngine.generateForecast(
            installments = installments,
            obligations = obligations,
            monthlyIncomeToman = 80_000_000L,
            livingExpensePercent = 45,
            todayJalali = "1405/08/05",
            horizonCount = 12
        )

        assertTrue("Expected zero deficit months", report.deficitMonths.isEmpty())
        assertTrue(report.averageProjectedHealthScore >= 75)
        report.horizonMonths.forEach { pt ->
            assertTrue(pt.upperConfidenceNetToman >= pt.projectedNetBalanceToman)
            assertTrue(pt.projectedNetBalanceToman >= pt.lowerConfidenceNetToman)
            assertEquals(ForecastRiskTier.HEALTHY, pt.riskTier)
        }
    }

    @Test
    fun overdueCarryoverAndStressScenarioIncreaseDeficitProbability() {
        val obligations = listOf(sampleObligation(1L))
        val cleanInstallments = listOf(
            sampleInstallment(1, dueDate = "1405/08/20", amount = 18_000_000L),
            sampleInstallment(2, dueDate = "1405/09/20", amount = 18_000_000L)
        )
        val overdueInstallments = cleanInstallments + listOf(
            sampleInstallment(
                id = 99,
                dueDate = "1405/07/10",
                amount = 15_000_000L,
                lateFee = 1_500_000L,
                status = InstallmentStatus.OVERDUE
            )
        )

        val cleanReport = FinancialForecastMlEngine.generateForecast(
            installments = cleanInstallments,
            obligations = obligations,
            monthlyIncomeToman = 50_000_000L,
            livingExpensePercent = 50,
            todayJalali = "1405/08/05"
        )
        val overdueReport = FinancialForecastMlEngine.generateForecast(
            installments = overdueInstallments,
            obligations = obligations,
            monthlyIncomeToman = 50_000_000L,
            livingExpensePercent = 50,
            todayJalali = "1405/08/05"
        )
        val stressReport = FinancialForecastMlEngine.generateForecast(
            installments = cleanInstallments,
            obligations = obligations,
            monthlyIncomeToman = 50_000_000L,
            livingExpensePercent = 50,
            todayJalali = "1405/08/05",
            scenario = ForecastScenarioMode.STRESS_SHOCK
        )

        assertTrue(
            "Overdue rollover should raise near-term deficit probability",
            overdueReport.horizonMonths[0].deficitProbability > cleanReport.horizonMonths[0].deficitProbability
        )
        assertTrue(
            "Income stress shock should lower projected net balance",
            stressReport.horizonMonths[0].projectedNetBalanceToman < cleanReport.horizonMonths[0].projectedNetBalanceToman
        )
    }
}
