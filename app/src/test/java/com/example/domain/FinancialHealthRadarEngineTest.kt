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
import kotlin.math.PI

class FinancialHealthRadarEngineTest {

    private val counterparties = listOf(
        CounterpartyEntity(
            id = 1L,
            name = "بانک ملت",
            category = EntityCategory.BANK.name,
            avatarColorHex = 0xFF35B8AB
        ),
        CounterpartyEntity(
            id = 2L,
            name = "اسنپ‌پی",
            category = EntityCategory.BNPL_PLATFORM.name,
            avatarColorHex = 0xFF4DA3B8
        ),
        CounterpartyEntity(
            id = 3L,
            name = "علی رضایی (دوست)",
            category = EntityCategory.PERSON.name,
            avatarColorHex = 0xFFDAB978
        )
    )

    private val obligations = listOf(
        ObligationEntity(
            id = 10L,
            counterpartyId = 1L,
            title = "وام مسکن بانک ملت",
            category = ObligationCategory.INSTALLMENT_LOAN.name,
            principalAmountToman = 100_000_000L,
            firstDueDateJalali = "1403/08/01",
            notes = "#وام_بانکی"
        ),
        ObligationEntity(
            id = 20L,
            counterpartyId = 2L,
            title = "خرید اقساطی اسنپ‌پی",
            category = ObligationCategory.BNPL_PURCHASE.name,
            principalAmountToman = 20_000_000L,
            firstDueDateJalali = "1403/09/01",
            notes = "#کارت_اعتباری"
        ),
        ObligationEntity(
            id = 30L,
            counterpartyId = 3L,
            title = "قرض شخصی از علی",
            category = ObligationCategory.PERSONAL_DEBT.name,
            principalAmountToman = 40_000_000L,
            firstDueDateJalali = "1403/09/01",
            notes = "#شخصی"
        )
    )

    private val installments = listOf(
        // Bank Loan: 40M paid last month, 60M remaining due in Azar 1403 (1403/09) -> monthly due = 15M
        InstallmentEntity(
            id = 101L,
            obligationId = 10L,
            installmentNumber = 1,
            title = "قسط ۱ وام",
            dueDateJalali = "1403/08/15",
            amountToman = 40_000_000L,
            paidAmountToman = 40_000_000L,
            status = InstallmentStatus.PAID.name
        ),
        InstallmentEntity(
            id = 102L,
            obligationId = 10L,
            installmentNumber = 2,
            title = "قسط ۲ وام",
            dueDateJalali = "1403/09/15",
            amountToman = 15_000_000L,
            paidAmountToman = 0L,
            status = InstallmentStatus.PENDING.name
        ),
        InstallmentEntity(
            id = 103L,
            obligationId = 10L,
            installmentNumber = 3,
            title = "قسط ۳ وام",
            dueDateJalali = "1403/10/15",
            amountToman = 45_000_000L,
            paidAmountToman = 0L,
            status = InstallmentStatus.PENDING.name
        ),
        // Credit Card / BNPL: 10M remaining due in 1403/09
        InstallmentEntity(
            id = 201L,
            obligationId = 20L,
            installmentNumber = 1,
            title = "قسط ۱ اسنپ‌پی",
            dueDateJalali = "1403/09/10",
            amountToman = 10_000_000L,
            paidAmountToman = 0L,
            status = InstallmentStatus.PENDING.name
        ),
        // Personal Debt: 30M total remaining (5M due in 1403/09, 25M due in 1403/10)
        InstallmentEntity(
            id = 301L,
            obligationId = 30L,
            installmentNumber = 1,
            title = "قسط ۱ قرض شخصی",
            dueDateJalali = "1403/09/20",
            amountToman = 5_000_000L,
            paidAmountToman = 0L,
            status = InstallmentStatus.PENDING.name
        ),
        InstallmentEntity(
            id = 302L,
            obligationId = 30L,
            installmentNumber = 2,
            title = "قسط ۲ قرض شخصی",
            dueDateJalali = "1403/10/20",
            amountToman = 25_000_000L,
            paidAmountToman = 0L,
            status = InstallmentStatus.PENDING.name
        )
    )

    @Test
    fun `evaluateFinancialHealthRadar computes category debt distribution and monthly budget capacity impact`() {
        // Monthly income = 100M, Living Expense = 40% (40M) => Monthly Debt Budget Capacity = 60M
        val report = FinancialHealthRadarEngine.evaluateFinancialHealthRadar(
            obligations = obligations,
            installments = installments,
            counterparties = counterparties,
            monthlyIncomeToman = 100_000_000L,
            livingExpensePercent = 40,
            todayJalali = "1403/09/12"
        )

        assertEquals(5, report.axes.size)
        assertEquals(60_000_000L, report.totalMonthlyDebtBudgetCapacityToman)

        // Total remaining debt = 60M (Bank Loan) + 10M (Credit Card) + 30M (Personal) = 100M
        assertEquals(100_000_000L, report.totalRemainingDebtToman)

        // Total current month (1403/09) installments = 15M (Bank) + 10M (Credit Card) + 5M (Personal) = 30M
        assertEquals(30_000_000L, report.totalCurrentMonthInstallmentsToman)
        assertEquals(30_000_000L, report.remainingSafeBudgetCapacityToman)
        assertEquals(50, report.overallBudgetCapacityUtilizationPercent)

        val bankAxis = report.axes.first { it.categoryTag == DebtCategoryTag.BANK_LOAN }
        val creditAxis = report.axes.first { it.categoryTag == DebtCategoryTag.CREDIT_CARD }
        val personalAxis = report.axes.first { it.categoryTag == DebtCategoryTag.PERSONAL }
        val housingAxis = report.axes.first { it.categoryTag == DebtCategoryTag.CHECK_AND_HOUSING }
        val bufferAxis = report.axes.first { it.axisId == "BUDGET_CAPACITY_BUFFER" }

        // Debt distribution shares: Bank = 60%, Credit Card = 10%, Personal = 30%, Housing = 0%
        assertEquals(60, bankAxis.debtDistributionPercent)
        assertEquals(10, creditAxis.debtDistributionPercent)
        assertEquals(30, personalAxis.debtDistributionPercent)
        assertEquals(0, housingAxis.debtDistributionPercent)

        // Monthly budget capacity impacts (out of 60M capacity):
        // Bank = 15M / 60M = 25%
        // Credit Card = 10M / 60M = 17%
        // Personal = 5M / 60M = 8%
        // Free Buffer = 30M / 60M = 50%
        assertEquals(25, bankAxis.budgetCapacityImpactPercent)
        assertEquals(17, creditAxis.budgetCapacityImpactPercent)
        assertEquals(8, personalAxis.budgetCapacityImpactPercent)
        assertEquals(50, bufferAxis.budgetCapacityImpactPercent)

        assertEquals(DebtCategoryTag.BANK_LOAN, report.dominantDebtCategory)
        assertEquals(DebtCategoryTag.BANK_LOAN, report.highestBudgetImpactCategory)
        assertTrue(report.executiveInsightFa.contains("وام بانکی"))
    }

    @Test
    fun `computePolarVertex and findNearestTappedAxisIndex map D3 polar radar coordinates accurately`() {
        val report = FinancialHealthRadarEngine.evaluateFinancialHealthRadar(
            obligations = obligations,
            installments = installments,
            counterparties = counterparties,
            monthlyIncomeToman = 100_000_000L,
            livingExpensePercent = 40,
            todayJalali = "1403/09/12"
        )

        // Top vertex (-90 deg = -PI/2) at radius 100 around center (200, 200) -> (200, 100)
        val (topX, topY) = FinancialHealthRadarEngine.computePolarVertex(
            angleRadians = -PI / 2.0,
            normalizedRadius = 1.0f,
            maxRadiusPx = 100f,
            centerX = 200f,
            centerY = 200f
        )
        assertEquals(200f, topX, 0.5f)
        assertEquals(100f, topY, 0.5f)

        // Tap near axis 0 (top vertex at 200, 115) should select index 0 (BANK_LOAN)
        val tappedIdx = FinancialHealthRadarEngine.findNearestTappedAxisIndex(
            axes = report.axes,
            tapX = 202f,
            tapY = 115f,
            centerX = 200f,
            centerY = 200f,
            maxRadiusPx = 100f
        )
        assertNotNull(tappedIdx)
        assertEquals(0, tappedIdx)
    }
}
