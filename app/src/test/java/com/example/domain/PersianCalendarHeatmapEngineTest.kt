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

class PersianCalendarHeatmapEngineTest {

    private val counterparties = listOf(
        CounterpartyEntity(id = 1L, name = "بانک ملی", category = EntityCategory.BANK.name),
        CounterpartyEntity(id = 2L, name = "اسنپ‌پی", category = EntityCategory.BNPL_PLATFORM.name),
        CounterpartyEntity(id = 3L, name = "دوست", category = EntityCategory.PERSON.name)
    )

    private val obligations = listOf(
        ObligationEntity(
            id = 10L,
            counterpartyId = 1L,
            title = "وام مسکن",
            category = ObligationCategory.INSTALLMENT_LOAN.name,
            principalAmountToman = 100_000_000L,
            firstDueDateJalali = "1403/09/14"
        ),
        ObligationEntity(
            id = 20L,
            counterpartyId = 2L,
            title = "خرید اسنپ‌پی",
            category = ObligationCategory.BNPL_PURCHASE.name,
            principalAmountToman = 20_000_000L,
            firstDueDateJalali = "1403/09/15"
        ),
        ObligationEntity(
            id = 30L,
            counterpartyId = 3L,
            title = "قرض شخصی",
            category = ObligationCategory.PERSONAL_DEBT.name,
            principalAmountToman = 5_000_000L,
            firstDueDateJalali = "1403/09/03"
        )
    )

    private val installments = listOf(
        // Light day in Decade 1: Day 3 -> 2M
        InstallmentEntity(
            id = 1L,
            obligationId = 30L,
            installmentNumber = 1,
            title = "قسط سبک",
            dueDateJalali = "1403/09/03",
            amountToman = 2_000_000L,
            paidAmountToman = 0L,
            status = InstallmentStatus.PENDING.name
        ),
        // High-Pressure cluster in Decade 2: Day 14 (12M) + Day 15 (10M + 6M = 16M) = 28M total!
        InstallmentEntity(
            id = 2L,
            obligationId = 10L,
            installmentNumber = 1,
            title = "قسط وام مسکن",
            dueDateJalali = "1403/09/14",
            amountToman = 12_000_000L,
            paidAmountToman = 0L,
            status = InstallmentStatus.PENDING.name
        ),
        InstallmentEntity(
            id = 3L,
            obligationId = 20L,
            installmentNumber = 1,
            title = "قسط ۱ اسنپ‌پی",
            dueDateJalali = "1403/09/15",
            amountToman = 10_000_000L,
            paidAmountToman = 0L,
            status = InstallmentStatus.PENDING.name
        ),
        InstallmentEntity(
            id = 4L,
            obligationId = 20L,
            installmentNumber = 2,
            title = "قسط ۲ اسنپ‌پی",
            dueDateJalali = "1403/09/15",
            amountToman = 6_000_000L,
            paidAmountToman = 0L,
            status = InstallmentStatus.PENDING.name
        ),
        // Paid installment on Day 25 (Decade 3) -> should not contribute to unpaid heat pressure
        InstallmentEntity(
            id = 5L,
            obligationId = 30L,
            installmentNumber = 2,
            title = "قسط تسویه‌شده",
            dueDateJalali = "1403/09/25",
            amountToman = 3_000_000L,
            paidAmountToman = 3_000_000L,
            status = InstallmentStatus.PAID.name
        )
    )

    @Test
    fun `buildPersianMonthHeatmap classifies thermal intensities and highlights high-pressure periods`() {
        val report = PersianCalendarHeatmapEngine.buildPersianMonthHeatmap(
            year = 1403,
            month = 9, // Azar has 30 days
            installments = installments,
            obligations = obligations,
            counterparties = counterparties,
            monthlyIncomeToman = 80_000_000L,
            todayJalali = "1403/09/01"
        )

        // Azar 1403 has 30 days
        assertEquals(30, report.daysInMonth)
        assertEquals(30, report.cells.size)
        assertEquals(30_000_000L, report.totalUpcomingUnpaidToman)
        assertEquals(3_000_000L, report.totalPaidDebtToman)
        assertEquals(4, report.upcomingInstallmentsCount)

        // Peak day must be Day 15 (16M unpaid across 2 installments -> CRITICAL)
        assertNotNull(report.peakDayCell)
        assertEquals(15, report.peakDayCell!!.dayOfMonth)
        assertEquals(HeatmapIntensityLevel.CRITICAL, report.peakDayCell!!.intensityLevel)
        assertTrue(report.peakDayCell!!.isInHighPressurePeriod)

        // Day 14 (12M) must also be high/critical and part of the high-pressure period
        val day14 = report.cells.first { it.dayOfMonth == 14 }
        assertTrue(day14.intensityLevel.rank >= HeatmapIntensityLevel.HIGH.rank)
        assertTrue(day14.isInHighPressurePeriod)

        // Day 3 (2M) should be LOW intensity
        val day3 = report.cells.first { it.dayOfMonth == 3 }
        assertEquals(HeatmapIntensityLevel.LOW, day3.intensityLevel)

        // Day 25 (only paid debt) should have NONE unpaid intensity
        val day25 = report.cells.first { it.dayOfMonth == 25 }
        assertEquals(HeatmapIntensityLevel.NONE, day25.intensityLevel)
        assertEquals(3_000_000L, day25.paidDebtToman)

        // High-Pressure Period detection must isolate the Day 14..15 cluster (28M = 93% of month!)
        assertTrue(report.highPressurePeriods.isNotEmpty())
        val topPeriod = report.highPressurePeriods.maxByOrNull { it.totalUpcomingDebtToman }!!
        assertEquals(14, topPeriod.startDay)
        assertEquals(15, topPeriod.endDay)
        assertEquals(28_000_000L, topPeriod.totalUpcomingDebtToman)
        assertEquals(93, topPeriod.shareOfMonthlyUpcomingPercent)

        // Decade 2 (11..20) must be identified as the peak decade
        assertEquals(2, report.peakDecadeIndex)
        assertEquals(3, report.decadeSummaries.size)
        assertTrue(report.decadeSummaries[1].isPeakDecade)
    }

    @Test
    fun `buildPersianMonthHeatmap handles 31-day Farvardin and leap vs common Esfand accurately`() {
        val farvardin = PersianCalendarHeatmapEngine.buildPersianMonthHeatmap(
            year = 1403,
            month = 1,
            installments = emptyList(),
            obligations = emptyList(),
            counterparties = emptyList(),
            todayJalali = "1403/01/01"
        )
        assertEquals(31, farvardin.daysInMonth)
        assertEquals(0, farvardin.highPressureDaysCount)
        assertTrue(farvardin.highPressurePeriods.isEmpty())

        // 1403 is a Jalali leap year (Esfand = 30 days), 1404 is common (Esfand = 29 days)
        val esfandLeap = PersianCalendarHeatmapEngine.buildPersianMonthHeatmap(
            year = 1403,
            month = 12,
            installments = emptyList(),
            obligations = emptyList(),
            counterparties = emptyList(),
            todayJalali = "1403/12/01"
        )
        val esfandCommon = PersianCalendarHeatmapEngine.buildPersianMonthHeatmap(
            year = 1404,
            month = 12,
            installments = emptyList(),
            obligations = emptyList(),
            counterparties = emptyList(),
            todayJalali = "1404/12/01"
        )
        assertEquals(30, esfandLeap.daysInMonth)
        assertEquals(29, esfandCommon.daysInMonth)
    }
}
