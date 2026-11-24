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

class DebtTaggingAndCategorizationEngineTest {

    private val sampleCounterparties = listOf(
        CounterpartyEntity(
            id = 1L,
            name = "بانک مهر ایران",
            category = EntityCategory.BANK.name
        ),
        CounterpartyEntity(
            id = 2L,
            name = "اسنپ‌پی (SnappPay)",
            category = EntityCategory.BNPL_PLATFORM.name
        ),
        CounterpartyEntity(
            id = 3L,
            name = "رضا محمدی",
            category = EntityCategory.PERSON.name
        )
    )

    private val sampleObligations = listOf(
        ObligationEntity(
            id = 101L,
            counterpartyId = 1L,
            title = "وام قرض‌الحسنه ازدواج",
            category = ObligationCategory.INSTALLMENT_LOAN.name,
            principalAmountToman = 100_000_000L,
            firstDueDateJalali = "1403/09/01",
            notes = "#وام_بانکی #ازدواج"
        ),
        ObligationEntity(
            id = 102L,
            counterpartyId = 2L,
            title = "خرید اقساطی لپ‌تاپ با کارت اعتباری",
            category = ObligationCategory.BNPL_PURCHASE.name,
            principalAmountToman = 40_000_000L,
            firstDueDateJalali = "1403/09/10",
            notes = "#کارت_اعتباری #دیجیتال"
        ),
        ObligationEntity(
            id = 103L,
            counterpartyId = 3L,
            title = "قرض شخصی از رضا",
            category = ObligationCategory.PERSONAL_DEBT.name,
            principalAmountToman = 20_000_000L,
            firstDueDateJalali = "1403/09/15",
            notes = "#شخصی #اضطراری"
        )
    )

    private val sampleInstallments = listOf(
        InstallmentEntity(
            id = 1001L,
            obligationId = 101L,
            installmentNumber = 1,
            title = "قسط ۱ وام ازدواج",
            dueDateJalali = "1403/09/05",
            amountToman = 10_000_000L,
            paidAmountToman = 10_000_000L,
            status = InstallmentStatus.PAID.name
        ),
        InstallmentEntity(
            id = 1002L,
            obligationId = 101L,
            installmentNumber = 2,
            title = "قسط ۲ وام ازدواج",
            dueDateJalali = "1403/09/25",
            amountToman = 90_000_000L,
            paidAmountToman = 0L,
            status = InstallmentStatus.PENDING.name
        ),
        InstallmentEntity(
            id = 1003L,
            obligationId = 102L,
            installmentNumber = 1,
            title = "قسط ۱ کارت اعتباری",
            dueDateJalali = "1403/09/10",
            amountToman = 40_000_000L,
            paidAmountToman = 10_000_000L,
            status = InstallmentStatus.PARTIAL.name
        ),
        InstallmentEntity(
            id = 1004L,
            obligationId = 103L,
            installmentNumber = 1,
            title = "بازپرداخت قرض شخصی",
            dueDateJalali = "1403/09/20",
            amountToman = 20_000_000L,
            paidAmountToman = 0L,
            status = InstallmentStatus.PENDING.name
        )
    )

    @Test
    fun `resolvePrimaryCategoryTag classifies Bank Loan, Credit Card, and Personal accurately`() {
        val cpMap = sampleCounterparties.associateBy { it.id }

        val bankTag = DebtTaggingAndCategorizationEngine.resolvePrimaryCategoryTag(
            obligation = sampleObligations[0],
            counterparty = cpMap[1L]
        )
        val creditCardTag = DebtTaggingAndCategorizationEngine.resolvePrimaryCategoryTag(
            obligation = sampleObligations[1],
            counterparty = cpMap[2L]
        )
        val personalTag = DebtTaggingAndCategorizationEngine.resolvePrimaryCategoryTag(
            obligation = sampleObligations[2],
            counterparty = cpMap[3L]
        )

        assertEquals(DebtCategoryTag.BANK_LOAN, bankTag)
        assertEquals(DebtCategoryTag.CREDIT_CARD, creditCardTag)
        assertEquals(DebtCategoryTag.PERSONAL, personalTag)
    }

    @Test
    fun `evaluateCategorizedPortfolio filters obligations by Credit Card tag and calculates breakdown`() {
        val creditReport = DebtTaggingAndCategorizationEngine.evaluateCategorizedPortfolio(
            obligations = sampleObligations,
            installments = sampleInstallments,
            counterparties = sampleCounterparties,
            selectedFilterTag = DebtCategoryTag.CREDIT_CARD,
            todayJalali = "1403/09/12"
        )

        assertEquals(1, creditReport.filteredObligations.size)
        assertEquals(102L, creditReport.filteredObligations.first().obligation.id)
        assertEquals(30_000_000L, creditReport.filteredTotalRemainingToman)
        assertEquals(10_000_000L, creditReport.filteredTotalPaidToman)

        val bankBreakdown = creditReport.categoryBreakdowns.first { it.tag == DebtCategoryTag.BANK_LOAN }
        val creditBreakdown = creditReport.categoryBreakdowns.first { it.tag == DebtCategoryTag.CREDIT_CARD }
        val personalBreakdown = creditReport.categoryBreakdowns.first { it.tag == DebtCategoryTag.PERSONAL }

        assertEquals(90_000_000L, bankBreakdown.remainingToman)
        assertEquals(30_000_000L, creditBreakdown.remainingToman)
        assertEquals(20_000_000L, personalBreakdown.remainingToman)
        assertEquals(DebtCategoryTag.BANK_LOAN, creditReport.dominantCategoryTag)
    }

    @Test
    fun `serializeTagOverrides and deserializeTagOverrides preserve category overrides and custom tags`() {
        val catOverrides = mapOf(
            101L to DebtCategoryTag.CREDIT_CARD,
            103L to DebtCategoryTag.PERSONAL
        )
        val customTags = mapOf(
            101L to listOf("کارت اعتباری", "فوری"),
            102L to listOf("لپ‌تاپ", "کار")
        )

        val serialized = DebtTaggingAndCategorizationEngine.serializeTagOverrides(
            categoryOverrides = catOverrides,
            customTagsMap = customTags
        )
        val (restoredCats, restoredCustom) = DebtTaggingAndCategorizationEngine.deserializeTagOverrides(serialized)

        assertEquals(DebtCategoryTag.CREDIT_CARD, restoredCats[101L])
        assertEquals(DebtCategoryTag.PERSONAL, restoredCats[103L])
        assertTrue(restoredCustom[101L].orEmpty().contains("فوری"))
        assertTrue(restoredCustom[102L].orEmpty().contains("لپ‌تاپ"))
    }
}
