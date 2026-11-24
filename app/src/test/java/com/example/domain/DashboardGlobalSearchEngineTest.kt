package com.example.domain

import com.example.data.CounterpartyEntity
import com.example.data.EntityCategory
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.ObligationCategory
import com.example.data.ObligationEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardGlobalSearchEngineTest {

    private val counterparties = listOf(
        CounterpartyEntity(id = 1L, name = "بانک ملت", category = EntityCategory.BANK.name),
        CounterpartyEntity(id = 2L, name = "اسنپ‌پی", category = EntityCategory.BNPL_PLATFORM.name),
        CounterpartyEntity(id = 3L, name = "محمد کریمی", category = EntityCategory.PERSON.name)
    )

    private val obligations = listOf(
        ObligationEntity(
            id = 10L,
            counterpartyId = 1L,
            title = "وام مسکن بانک ملت",
            category = ObligationCategory.INSTALLMENT_LOAN.name,
            principalAmountToman = 100_000_000L,
            firstDueDateJalali = "1403/09/10",
            notes = "#مسکن #وام_بانکی"
        ),
        ObligationEntity(
            id = 20L,
            counterpartyId = 2L,
            title = "خرید اقساطی لپ‌تاپ",
            category = ObligationCategory.BNPL_PURCHASE.name,
            principalAmountToman = 20_000_000L,
            firstDueDateJalali = "1403/09/14",
            notes = "#کالای_دیجیتال"
        ),
        ObligationEntity(
            id = 30L,
            counterpartyId = 3L,
            title = "قرض شخصی سفر",
            category = ObligationCategory.PERSONAL_DEBT.name,
            principalAmountToman = 8_000_000L,
            firstDueDateJalali = "1403/10/05",
            notes = "#سفر #شخصی"
        )
    )

    private val installments = listOf(
        // Overdue Bank Loan installment (before today 1403/09/12)
        InstallmentEntity(
            id = 101L,
            obligationId = 10L,
            installmentNumber = 1,
            title = "قسط ۱ وام مسکن",
            dueDateJalali = "1403/09/08",
            amountToman = 12_000_000L,
            paidAmountToman = 0L,
            status = InstallmentStatus.OVERDUE.name
        ),
        // Upcoming BNPL installment within 7 days (1403/09/14)
        InstallmentEntity(
            id = 201L,
            obligationId = 20L,
            installmentNumber = 1,
            title = "قسط ۱ لپ‌تاپ",
            dueDateJalali = "1403/09/14",
            amountToman = 5_000_000L,
            paidAmountToman = 0L,
            status = InstallmentStatus.PENDING.name
        ),
        // Future Personal Debt installment next month (1403/10/05)
        InstallmentEntity(
            id = 301L,
            obligationId = 30L,
            installmentNumber = 1,
            title = "قسط ۱ سفر",
            dueDateJalali = "1403/10/05",
            amountToman = 8_000_000L,
            paidAmountToman = 0L,
            status = InstallmentStatus.PENDING.name
        )
    )

    @Test
    fun `searchAndFilter finds debts by Persian or Arabic normalized name and counterparty`() {
        // Using Arabic Kaf (ك) and Yeh (ي) to verify resilient Persian normalization
        val result = DashboardGlobalSearchEngine.searchAndFilter(
            obligations = obligations,
            installments = installments,
            counterparties = counterparties,
            criteria = DashboardSearchCriteria(query = "بانك ملت"),
            todayJalali = "1403/09/12"
        )

        assertEquals(1, result.totalMatchedDebtsCount)
        assertEquals(10L, result.matches.first().obligation.id)
        assertEquals(12_000_000L, result.totalMatchedRemainingToman)
        assertTrue(result.matches.first().matchReasonsFa.contains("تطابق نام"))
    }

    @Test
    fun `searchAndFilter filters accurately by category tag and custom hashtag`() {
        val byCategory = DashboardGlobalSearchEngine.searchAndFilter(
            obligations = obligations,
            installments = installments,
            counterparties = counterparties,
            criteria = DashboardSearchCriteria(selectedCategoryTag = DebtCategoryTag.CREDIT_CARD),
            todayJalali = "1403/09/12"
        )
        assertEquals(1, byCategory.totalMatchedDebtsCount)
        assertEquals(20L, byCategory.matches.first().obligation.id)

        val byCustomTag = DashboardGlobalSearchEngine.searchAndFilter(
            obligations = obligations,
            installments = installments,
            counterparties = counterparties,
            criteria = DashboardSearchCriteria(selectedCustomTag = "سفر"),
            todayJalali = "1403/09/12"
        )
        assertEquals(1, byCustomTag.totalMatchedDebtsCount)
        assertEquals(30L, byCustomTag.matches.first().obligation.id)
    }

    @Test
    fun `searchAndFilter filters accurately by Shamsi due date range presets and custom bounds`() {
        // Overdue preset (before 1403/09/12) -> only Bank Loan (1403/09/08)
        val overdueRes = DashboardGlobalSearchEngine.searchAndFilter(
            obligations = obligations,
            installments = installments,
            counterparties = counterparties,
            criteria = DashboardSearchCriteria(dueDatePreset = DueDateRangePreset.OVERDUE),
            todayJalali = "1403/09/12"
        )
        assertEquals(1, overdueRes.totalMatchedDebtsCount)
        assertEquals(10L, overdueRes.matches.first().obligation.id)

        // Next 7 days preset (1403/09/12 .. 1403/09/19) -> only BNPL (1403/09/14)
        val next7Res = DashboardGlobalSearchEngine.searchAndFilter(
            obligations = obligations,
            installments = installments,
            counterparties = counterparties,
            criteria = DashboardSearchCriteria(dueDatePreset = DueDateRangePreset.NEXT_7_DAYS),
            todayJalali = "1403/09/12"
        )
        assertEquals(1, next7Res.totalMatchedDebtsCount)
        assertEquals(20L, next7Res.matches.first().obligation.id)

        // Custom range (even with reversed bounds 1403/10/10 .. 1403/10/01) -> only Personal Debt (1403/10/05)
        val customRes = DashboardGlobalSearchEngine.searchAndFilter(
            obligations = obligations,
            installments = installments,
            counterparties = counterparties,
            criteria = DashboardSearchCriteria(
                dueDatePreset = DueDateRangePreset.CUSTOM_RANGE,
                customStartDateJalali = "1403/10/10",
                customEndDateJalali = "1403/10/01"
            ),
            todayJalali = "1403/09/12"
        )
        assertEquals(1, customRes.totalMatchedDebtsCount)
        assertEquals(30L, customRes.matches.first().obligation.id)
        assertEquals("1403/10/01", customRes.effectiveStartDateJalali)
        assertEquals("1403/10/10", customRes.effectiveEndDateJalali)
    }

    @Test
    fun `searchAndFilter combines text query, tag, and date range cleanly`() {
        val combined = DashboardGlobalSearchEngine.searchAndFilter(
            obligations = obligations,
            installments = installments,
            counterparties = counterparties,
            criteria = DashboardSearchCriteria(
                query = "لپ تاپ",
                selectedCategoryTag = DebtCategoryTag.CREDIT_CARD,
                dueDatePreset = DueDateRangePreset.THIS_SHAMSI_MONTH
            ),
            todayJalali = "1403/09/12"
        )

        assertEquals(1, combined.totalMatchedDebtsCount)
        assertEquals(20L, combined.matches.first().obligation.id)
        assertEquals(3, combined.activeFilterCount)
    }
}
