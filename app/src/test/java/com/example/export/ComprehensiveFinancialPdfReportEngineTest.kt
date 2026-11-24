package com.example.export

import com.example.core.CurrencyUnit
import com.example.data.CounterpartyEntity
import com.example.data.EntityCategory
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.ObligationCategory
import com.example.data.ObligationEntity
import com.example.domain.DebtCategoryTag
import com.example.domain.RecurringCashflowCategory
import com.example.domain.RecurringCashflowItem
import com.example.domain.RecurringCashflowType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComprehensiveFinancialPdfReportEngineTest {

    private val counterparties = listOf(
        CounterpartyEntity(
            id = 1L,
            name = "بانک مهر ایران",
            category = EntityCategory.BANK.name
        ),
        CounterpartyEntity(
            id = 2L,
            name = "اسنپ‌پی",
            category = EntityCategory.BNPL_PLATFORM.name
        )
    )

    private val obligations = listOf(
        ObligationEntity(
            id = 10L,
            counterpartyId = 1L,
            title = "وام مسکن جوانان",
            category = ObligationCategory.INSTALLMENT_LOAN.name,
            principalAmountToman = 120_000_000L,
            firstDueDateJalali = "1403/09/01"
        ),
        ObligationEntity(
            id = 20L,
            counterpartyId = 2L,
            title = "خرید اقساطی گوشی",
            category = ObligationCategory.BNPL_PURCHASE.name,
            principalAmountToman = 30_000_000L,
            firstDueDateJalali = "1403/09/10"
        )
    )

    private val installments = listOf(
        InstallmentEntity(
            id = 100L,
            obligationId = 10L,
            installmentNumber = 1,
            title = "قسط ۱ وام مسکن",
            dueDateJalali = "1403/09/05",
            amountToman = 40_000_000L,
            paidAmountToman = 40_000_000L,
            status = InstallmentStatus.PAID.name
        ),
        InstallmentEntity(
            id = 101L,
            obligationId = 10L,
            installmentNumber = 2,
            title = "قسط ۲ وام مسکن",
            dueDateJalali = "1403/10/05",
            amountToman = 80_000_000L,
            paidAmountToman = 0L,
            status = InstallmentStatus.PENDING.name
        ),
        InstallmentEntity(
            id = 200L,
            obligationId = 20L,
            installmentNumber = 1,
            title = "قسط ۱ گوشی",
            dueDateJalali = "1403/09/15",
            amountToman = 30_000_000L,
            paidAmountToman = 10_000_000L,
            status = InstallmentStatus.PARTIAL.name
        )
    )

    private val recurringItems = listOf(
        RecurringCashflowItem(
            id = 1L,
            titleFa = "حقوق ماهانه",
            category = RecurringCashflowCategory.SALARY,
            monthlyAmountToman = 85_000_000L,
            dayOfShamsiMonth = 1,
            isActive = true
        ),
        RecurringCashflowItem(
            id = 2L,
            titleFa = "اجاره و شارژ",
            category = RecurringCashflowCategory.HOUSING_UTILITIES,
            monthlyAmountToman = 15_000_000L,
            dayOfShamsiMonth = 5,
            isActive = true
        )
    )

    @Test
    fun `buildComprehensiveSummaryReport aggregates debts, savings progress, and cashflow accurately`() {
        val report = ComprehensiveFinancialPdfReportEngine.buildComprehensiveSummaryReport(
            obligations = obligations,
            installments = installments,
            counterparties = counterparties,
            recurringCashflowItems = recurringItems,
            monthlyIncomeToman = 85_000_000L,
            livingExpensePercent = 40,
            savingsGoalTitleFa = "ذخیره طلا",
            savingsTargetToman = 10_000_000L,
            savingsSavedToman = 8_500_000L,
            currencyUnit = CurrencyUnit.TOMAN,
            todayJalali = "1403/09/12",
            fixedDocumentId = "TRZ-FIN-140309"
        )

        assertEquals("TRZ-FIN-140309", report.documentId)
        assertEquals(2, report.totalActiveDebtsCount)
        assertEquals(150_000_000L, report.totalDebtPayableToman)
        assertEquals(50_000_000L, report.totalDebtPaidToman)
        assertEquals(100_000_000L, report.totalDebtRemainingToman)
        assertEquals(33, report.overallDebtSettlementPercent)

        // Savings progress assertions
        assertEquals("ذخیره طلا", report.savingsSummary.goalTitleFa)
        assertEquals(10_000_000L, report.savingsSummary.targetToman)
        assertEquals(8_500_000L, report.savingsSummary.savedToman)
        assertEquals(1_500_000L, report.savingsSummary.remainingToTargetToman)
        assertEquals(85, report.savingsSummary.progressPercent)

        // Cashflow assertions
        assertEquals(85_000_000L, report.cashflowSummary.recurringIncomeToman)
        assertEquals(15_000_000L, report.cashflowSummary.recurringNonLoanExpenseToman)

        // Category breakdowns assertions
        val bankRow = report.categoryBreakdowns.first { it.tag == DebtCategoryTag.BANK_LOAN }
        val creditRow = report.categoryBreakdowns.first { it.tag == DebtCategoryTag.CREDIT_CARD }
        assertEquals(80_000_000L, bankRow.remainingToman)
        assertEquals(20_000_000L, creditRow.remainingToman)
        assertEquals(3, report.executiveNotesFa.size)
    }

    @Test
    fun `toA4PreviewPayload includes savings progress row, cashflow row, and obligation rows`() {
        val report = ComprehensiveFinancialPdfReportEngine.buildComprehensiveSummaryReport(
            obligations = obligations,
            installments = installments,
            counterparties = counterparties,
            recurringCashflowItems = recurringItems,
            savingsGoalTitleFa = "صندوق اضطراری",
            savingsTargetToman = 20_000_000L,
            savingsSavedToman = 15_000_000L,
            todayJalali = "1403/09/12",
            fixedDocumentId = "TRZ-FIN-999"
        )

        val payload = ComprehensiveFinancialPdfReportEngine.toA4PreviewPayload(report)
        assertEquals("TRZ-FIN-999", payload.trackingId)
        assertEquals(4, payload.rows.size) // 1 savings row + 1 cashflow row + 2 obligation rows
        assertTrue(payload.rows[0].title.contains("صندوق اضطراری"))
        assertTrue(payload.subtitleFa.contains("۷۵٪"))
    }
}
