package com.example.notification

import com.example.core.CurrencyUnit
import com.example.data.CounterpartyEntity
import com.example.data.EntityCategory
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.ObligationCategory
import com.example.data.ObligationEntity
import com.example.data.ScheduleMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShamsiInstallmentNotificationSystemTest {

    private fun sampleObligation(
        id: Long = 1L,
        title: String = "وام مسکن بانک مسکن",
        counterpartyId: Long = 10L,
        category: String = ObligationCategory.INSTALLMENT_LOAN.name
    ) = ObligationEntity(
        id = id,
        counterpartyId = counterpartyId,
        title = title,
        category = category,
        scheduleMode = ScheduleMode.AUTO_SCHEDULE.name,
        principalAmountToman = 120_000_000L,
        profitOrFeeToman = 24_000_000L,
        installmentCount = 12,
        firstDueDateJalali = "1403/01/15"
    )

    private fun sampleCounterparty(
        id: Long = 10L,
        name: String = "بانک مسکن"
    ) = CounterpartyEntity(
        id = id,
        name = name,
        category = EntityCategory.BANK.name
    )

    private fun sampleInstallment(
        id: Long,
        obligationId: Long = 1L,
        installmentNumber: Int,
        dueDateJalali: String,
        amountToman: Long = 12_000_000L,
        paidAmountToman: Long = 0L,
        status: String = InstallmentStatus.PENDING.name,
        snoozedUntilJalali: String = ""
    ) = InstallmentEntity(
        id = id,
        obligationId = obligationId,
        installmentNumber = installmentNumber,
        title = "قسط $installmentNumber",
        dueDateJalali = dueDateJalali,
        amountToman = amountToman,
        paidAmountToman = paidAmountToman,
        status = status,
        snoozedUntilJalali = snoozedUntilJalali
    )

    @Test
    fun `buildShamsiDueNotificationSummary identifies overdue due-today and upcoming Shamsi installments`() {
        val todayJalali = "1403/09/15"
        val obligations = listOf(sampleObligation(id = 1L, title = "وام مسکن"))
        val counterparties = listOf(sampleCounterparty(id = 10L, name = "بانک مسکن"))

        val installments = listOf(
            // Overdue by 5 days (1403/09/10)
            sampleInstallment(
                id = 101L,
                installmentNumber = 1,
                dueDateJalali = "1403/09/10",
                amountToman = 5_000_000L
            ),
            // Due today (1403/09/15)
            sampleInstallment(
                id = 102L,
                installmentNumber = 2,
                dueDateJalali = "1403/09/15",
                amountToman = 7_500_000L
            ),
            // Upcoming in 4 days (1403/09/19) within 7-day window
            sampleInstallment(
                id = 103L,
                installmentNumber = 3,
                dueDateJalali = "1403/09/19",
                amountToman = 10_000_000L
            ),
            // Far in future (1403/10/15) outside 7-day window -> excluded
            sampleInstallment(
                id = 104L,
                installmentNumber = 4,
                dueDateJalali = "1403/10/15",
                amountToman = 10_000_000L
            ),
            // Already paid -> excluded
            sampleInstallment(
                id = 105L,
                installmentNumber = 5,
                dueDateJalali = "1403/09/12",
                amountToman = 6_000_000L,
                paidAmountToman = 6_000_000L,
                status = InstallmentStatus.PAID.name
            )
        )

        val summary = ShamsiInstallmentReminderEvaluator.buildShamsiDueNotificationSummary(
            installments = installments,
            obligations = obligations,
            counterparties = counterparties,
            todayJalali = todayJalali,
            alertAdvanceDays = 7,
            currencyUnit = CurrencyUnit.TOMAN,
            privacyMasked = false
        )

        assertTrue(summary.shouldNotify)
        assertEquals(1, summary.overdueCount)
        assertEquals(1, summary.dueTodayCount)
        assertEquals(1, summary.upcomingCount)
        assertEquals(3, summary.totalAlertCount)
        assertEquals(22_500_000L, summary.totalAlertAmountToman)

        // Verify ordering: overdue first (-5), then due today (0), then upcoming (+4)
        assertEquals(listOf(-5, 0, 4), summary.items.map { it.diffDays })
        assertTrue(summary.notificationTitle.contains("یادآور سررسید شمسی"))
        assertTrue(summary.notificationShortText.contains("وام مسکن"))
        assertEquals(3, summary.expandedLines.size)
    }

    @Test
    fun `buildShamsiDueNotificationSummary returns non-notifying summary when no installments are within advance window`() {
        val todayJalali = "1403/09/01"
        val obligations = listOf(sampleObligation(id = 1L))
        val counterparties = listOf(sampleCounterparty(id = 10L))
        val installments = listOf(
            sampleInstallment(
                id = 201L,
                installmentNumber = 1,
                dueDateJalali = "1403/09/20",
                amountToman = 8_000_000L
            )
        )

        val summary = ShamsiInstallmentReminderEvaluator.buildShamsiDueNotificationSummary(
            installments = installments,
            obligations = obligations,
            counterparties = counterparties,
            todayJalali = todayJalali,
            alertAdvanceDays = 7
        )

        assertFalse(summary.shouldNotify)
        assertEquals(0, summary.totalAlertCount)
        assertEquals(0L, summary.totalAlertAmountToman)
        assertTrue(summary.expandedLines.isEmpty())
    }
}
