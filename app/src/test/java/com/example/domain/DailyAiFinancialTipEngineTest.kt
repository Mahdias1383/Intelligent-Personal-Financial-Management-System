package com.example.domain

import com.example.core.CurrencyUnit
import com.example.data.CounterpartyEntity
import com.example.data.EntityCategory
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.ObligationCategory
import com.example.data.ObligationEntity
import com.example.data.PaymentInterval
import com.example.data.ScheduleMode
import com.example.data.TrustPriority
import com.example.data.UserPreferencesState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyAiFinancialTipEngineTest {

    private fun sampleCounterparty() = CounterpartyEntity(
        id = 1L,
        name = "بانک رسالت",
        category = EntityCategory.BANK.name,
        avatarColorHex = 0xFF35B8ABL,
        createdAtJalali = "1405/01/01"
    )

    private fun sampleObligation() = ObligationEntity(
        id = 10L,
        counterpartyId = 1L,
        title = "وام خرید خودرو",
        category = ObligationCategory.INSTALLMENT_LOAN.name,
        scheduleMode = ScheduleMode.AUTO_SCHEDULE.name,
        principalAmountToman = 100_000_000L,
        profitOrFeeToman = 24_000_000L,
        installmentCount = 12,
        paymentInterval = PaymentInterval.MONTHLY.name,
        firstDueDateJalali = "1405/07/01",
        trustPriority = TrustPriority.HIGH.name,
        createdAtJalali = "1405/06/01"
    )

    @Test
    fun buildSnapshotAndPrompt_capturesSpendingPatternsAndDebtMetrics() {
        val today = "1405/08/15"
        val installments = listOf(
            InstallmentEntity(
                id = 1L,
                obligationId = 10L,
                installmentNumber = 1,
                title = "قسط ۱",
                dueDateJalali = "1405/08/01",
                amountToman = 10_000_000L,
                paidAmountToman = 10_000_000L,
                status = InstallmentStatus.PAID.name,
                paidDateJalali = "1405/08/01"
            ),
            InstallmentEntity(
                id = 2L,
                obligationId = 10L,
                installmentNumber = 2,
                title = "قسط ۲",
                dueDateJalali = "1405/08/10",
                amountToman = 10_000_000L,
                paidAmountToman = 0L,
                status = InstallmentStatus.OVERDUE.name
            ),
            InstallmentEntity(
                id = 3L,
                obligationId = 10L,
                installmentNumber = 3,
                title = "قسط ۳",
                dueDateJalali = "1405/08/20",
                amountToman = 10_000_000L,
                paidAmountToman = 0L,
                status = InstallmentStatus.PENDING.name
            )
        )
        val recurring = RecurringCashflowHealthEngine.defaultSampleRecurringItems()
        val prefs = UserPreferencesState(
            monthlyIncomeToman = 58_000_000L,
            livingExpensePercent = 50,
            autoPaymentScheduleEnabled = true,
            preferredAutoPayDayOfMonth = 3
        )

        val snapshot = DailyAiFinancialTipEngine.buildSnapshot(
            obligations = listOf(sampleObligation()),
            installments = installments,
            counterparties = listOf(sampleCounterparty()),
            subRecords = emptyList(),
            recurringItems = recurring,
            dtiReport = null,
            savingsSummary = null,
            preferences = prefs,
            todayJalali = today
        )

        assertEquals(1, snapshot.overdueCount)
        assertEquals(10_000_000L, snapshot.overdueAmountToman)
        assertEquals(1, snapshot.dueNext7DaysCount)
        assertEquals(10_000_000L, snapshot.recentPaidLast30DaysToman)
        assertEquals(27_000_000L, snapshot.totalRecurringNonLoanExpenseToman)
        assertEquals(3_500_000L, snapshot.discretionaryExpenseToman)
        assertEquals("وام خرید خودرو", snapshot.highestInterestDebtTitleFa)
        assertEquals(3, snapshot.preferredAutoPayDay)

        val prompt = DailyAiFinancialTipEngine.buildGeminiPrompt(snapshot, CurrencyUnit.TOMAN)
        assertTrue(prompt.contains("وام خرید خودرو"))
        assertTrue(prompt.contains("1405/08/15"))
        assertTrue(prompt.contains("تومان"))
        assertTrue(prompt.contains("PROTECTIVE"))
    }

    @Test
    fun serializeAndDeserializeCachedTip_roundTripsCleanlyAndValidatesTodayOnly() {
        val original = DailyFinancialTipResult(
            headlineFa = "اولویت امروز: تسویه قسط معوقه | بدون جریمه",
            spendingPatternInsightFa = "مخارج اختیاری این ماه ۳.۵ میلیون تومان است.",
            dailyTipFa = "با انتقال بخشی از بودجه اشتراک‌ها، قسط معوق بانک رسالت را تسویه کنید.",
            concreteActionStepFa = "پرداخت فوری قسط معوق پیش از پایان هفته",
            focusBadgeFa = "تسویه معوقه",
            estimatedImpactFa = "جلوگیری از جریمه دیرکرد",
            urgencyTone = TipUrgencyTone.PROTECTIVE,
            generatedDateJalali = "1405/08/15",
            profileFingerprint = "1405/08/15-20000000"
        )

        val serialized = DailyAiFinancialTipEngine.serializeCachedTip(original)
        val restored = DailyAiFinancialTipEngine.deserializeCachedTip(serialized)

        assertNotNull(restored)
        assertEquals("اولویت امروز: تسویه قسط معوقه   بدون جریمه", restored!!.headlineFa)
        assertEquals(original.dailyTipFa, restored.dailyTipFa)
        assertEquals(TipUrgencyTone.PROTECTIVE, restored.urgencyTone)
        assertEquals("1405/08/15", restored.generatedDateJalali)

        assertTrue(DailyAiFinancialTipEngine.isCacheValidForToday(restored, "1405/08/15"))
        assertFalse(DailyAiFinancialTipEngine.isCacheValidForToday(restored, "1405/08/16"))
    }

    @Test
    fun parseUrgencyTone_mapsAllEnumValuesSafely() {
        assertEquals(TipUrgencyTone.PROTECTIVE, DailyAiFinancialTipEngine.parseUrgencyTone("PROTECTIVE"))
        assertEquals(TipUrgencyTone.MOMENTUM, DailyAiFinancialTipEngine.parseUrgencyTone("momentum"))
        assertEquals(TipUrgencyTone.OPTIMIZATION, DailyAiFinancialTipEngine.parseUrgencyTone("UNKNOWN"))
    }
}
