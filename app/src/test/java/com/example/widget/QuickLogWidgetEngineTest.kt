package com.example.widget

import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.ObligationCategory
import com.example.data.ObligationEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickLogWidgetEngineTest {

    @Test
    fun buildSnapshot_prioritizesOverdueDebtInstallmentAndExcludesReceivables() {
        val obligations = listOf(
            ObligationEntity(
                id = 1L,
                counterpartyId = 10L,
                title = "وام مسکن",
                category = ObligationCategory.INSTALLMENT_LOAN.name,
                principalAmountToman = 50_000_000L,
                firstDueDateJalali = "1403/09/15"
            ),
            ObligationEntity(
                id = 2L,
                counterpartyId = 11L,
                title = "قسط معوقه اسنپ‌پی",
                category = ObligationCategory.BNPL_PURCHASE.name,
                principalAmountToman = 8_000_000L,
                firstDueDateJalali = "1403/08/25"
            ),
            ObligationEntity(
                id = 3L,
                counterpartyId = 12L,
                title = "طلب از دوست",
                category = ObligationCategory.PERSONAL_RECEIVABLE.name,
                principalAmountToman = 20_000_000L,
                firstDueDateJalali = "1403/08/20"
            )
        )
        val installments = listOf(
            InstallmentEntity(
                id = 101L,
                obligationId = 1L,
                installmentNumber = 1,
                title = "قسط ۱ وام",
                dueDateJalali = "1403/09/15",
                amountToman = 5_000_000L,
                paidAmountToman = 0L,
                status = InstallmentStatus.PENDING.name
            ),
            InstallmentEntity(
                id = 201L,
                obligationId = 2L,
                installmentNumber = 1,
                title = "قسط ۱ اسنپ‌پی",
                dueDateJalali = "1403/08/25",
                amountToman = 2_000_000L,
                paidAmountToman = 0L,
                status = InstallmentStatus.OVERDUE.name
            ),
            InstallmentEntity(
                id = 301L,
                obligationId = 3L,
                installmentNumber = 1,
                title = "طلب",
                dueDateJalali = "1403/08/20",
                amountToman = 20_000_000L,
                paidAmountToman = 0L,
                status = InstallmentStatus.PENDING.name
            )
        )

        val snapshot = QuickLogWidgetEngine.buildSnapshot(
            obligations = obligations,
            installments = installments,
            todayJalali = "1403/09/01"
        )

        assertEquals(7_000_000L, snapshot.totalRemainingDebtToman)
        assertEquals(2, snapshot.activeUnpaidDebtCount)
        assertEquals(1, snapshot.urgentCount)
        assertEquals(201L, snapshot.nextDueInstallmentId)
        assertEquals("قسط معوقه اسنپ‌پی", snapshot.nextDueObligationTitleFa)
        assertEquals(2_000_000L, snapshot.nextDueRemainingToman)
    }

    @Test
    fun buildSnapshot_returnsGracefulDebtFreeStateWhenAllPaid() {
        val snapshot = QuickLogWidgetEngine.buildSnapshot(
            obligations = emptyList(),
            installments = emptyList(),
            todayJalali = "1403/09/01"
        )
        assertEquals(0L, snapshot.totalRemainingDebtToman)
        assertNull(snapshot.nextDueInstallmentId)
    }

    @Test
    fun createQuickExpenseBundle_supportsImmediatePaidExpenseAndUnpaidDebt() {
        val paidExpense = QuickLogWidgetEngine.createQuickExpenseBundle(
            counterpartyId = 5L,
            title = "خرید سوپرمارکت",
            amountToman = 850_000L,
            markAsPaidImmediately = true,
            dateJalali = "1403/09/01"
        )
        assertEquals(InstallmentStatus.PAID, paidExpense.installment.statusEnum)
        assertEquals(850_000L, paidExpense.installment.paidAmountToman)
        assertEquals(0L, paidExpense.installment.remainingToman)

        val unpaidDebt = QuickLogWidgetEngine.createQuickExpenseBundle(
            counterpartyId = 5L,
            title = "بدهی جدید تعمیر خودرو",
            amountToman = 3_500_000L,
            markAsPaidImmediately = false,
            dateJalali = "1403/09/01"
        )
        assertEquals(InstallmentStatus.PENDING, unpaidDebt.installment.statusEnum)
        assertEquals(0L, unpaidDebt.installment.paidAmountToman)
        assertEquals(3_500_000L, unpaidDebt.installment.remainingToman)
    }

    @Test
    fun applyQuickDebtPayment_handlesPartialAndFullSettlementCorrectly() {
        val inst = InstallmentEntity(
            id = 50L,
            obligationId = 1L,
            installmentNumber = 1,
            title = "قسط ۱",
            dueDateJalali = "1403/09/10",
            amountToman = 4_000_000L,
            paidAmountToman = 0L,
            status = InstallmentStatus.PENDING.name
        )

        val partial = QuickLogWidgetEngine.applyQuickDebtPayment(
            installment = inst,
            paymentAmountToman = 1_500_000L,
            todayJalali = "1403/09/01"
        )
        assertEquals(InstallmentStatus.PARTIAL, partial.statusEnum)
        assertEquals(1_500_000L, partial.paidAmountToman)
        assertEquals(2_500_000L, partial.remainingToman)

        val settled = QuickLogWidgetEngine.applyQuickDebtPayment(
            installment = partial,
            paymentAmountToman = 2_500_000L,
            todayJalali = "1403/09/02"
        )
        assertEquals(InstallmentStatus.PAID, settled.statusEnum)
        assertEquals(4_000_000L, settled.paidAmountToman)
        assertEquals(0L, settled.remainingToman)
    }

    @Test
    fun validateInput_rejectsZeroAmountAndMissingFields() {
        assertTrue(
            QuickLogWidgetEngine.validateInput(
                mode = WidgetQuickLogMode.EXPENSE,
                title = "خوراکی",
                amountToman = 0L,
                selectedInstallmentId = null
            ) is QuickLogValidationResult.Invalid
        )
        assertTrue(
            QuickLogWidgetEngine.validateInput(
                mode = WidgetQuickLogMode.EXPENSE,
                title = "   ",
                amountToman = 100_000L,
                selectedInstallmentId = null
            ) is QuickLogValidationResult.Invalid
        )
        assertTrue(
            QuickLogWidgetEngine.validateInput(
                mode = WidgetQuickLogMode.DEBT_PAYMENT,
                title = "",
                amountToman = 500_000L,
                selectedInstallmentId = null
            ) is QuickLogValidationResult.Invalid
        )
        assertTrue(
            QuickLogWidgetEngine.validateInput(
                mode = WidgetQuickLogMode.DEBT_PAYMENT,
                title = "",
                amountToman = 500_000L,
                selectedInstallmentId = 12L
            ) is QuickLogValidationResult.Valid
        )
    }
}
