package com.example.core

import com.example.data.PaymentInterval
import com.example.domain.InstallmentScheduleEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JalaliCalendarEngineTest {

    @Test
    fun `leap year detection follows Iranian Solar Hijri 33-year astronomical cycle`() {
        // 1403 and 1408 are leap years (Esfand has 30 days); 1404, 1405, 1406, 1407 are common years (Esfand has 29 days)
        assertTrue(JalaliCalendarEngine.isJalaliLeapYear(1403))
        assertFalse(JalaliCalendarEngine.isJalaliLeapYear(1404))
        assertFalse(JalaliCalendarEngine.isJalaliLeapYear(1405))
        assertFalse(JalaliCalendarEngine.isJalaliLeapYear(1406))
        assertFalse(JalaliCalendarEngine.isJalaliLeapYear(1407))
        assertTrue(JalaliCalendarEngine.isJalaliLeapYear(1408))

        assertEquals(30, JalaliCalendarEngine.getJalaliMonthLength(1403, 12))
        assertEquals(29, JalaliCalendarEngine.getJalaliMonthLength(1405, 12))
        assertEquals(366, JalaliCalendarEngine.getJalaliYearLength(1403))
        assertEquals(365, JalaliCalendarEngine.getJalaliYearLength(1405))
    }

    @Test
    fun `month lengths follow 31 days for months 1 to 6 and 30 days for months 7 to 11`() {
        for (m in 1..6) {
            assertEquals(31, JalaliCalendarEngine.getJalaliMonthLength(1405, m))
        }
        for (m in 7..11) {
            assertEquals(30, JalaliCalendarEngine.getJalaliMonthLength(1405, m))
        }
    }

    @Test
    fun `gregorian to jalali and jalali to gregorian round-trip conversion is exact`() {
        val sampleDates = listOf(
            "1403/12/30", // Leap year Esfand 30
            "1404/01/01", // Nowruz
            "1405/06/31", // End of summer (31 Shahrivar)
            "1405/07/01", // 1 Mehr
            "1405/08/22",
            "1405/12/29", // Common year Esfand 29
            "1406/01/01"
        )
        for (jalaliStr in sampleDates) {
            val parsed = JalaliCalendarEngine.parseJalali(jalaliStr)
            val (gy, gm, gd) = JalaliCalendarEngine.jalaliToGregorian(parsed.year, parsed.month, parsed.day)
            val backToJalali = JalaliCalendarEngine.gregorianToJalali(gy, gm, gd)
            assertEquals(jalaliStr, backToJalali.formatCanonical())
        }
    }

    @Test
    fun `loan installment schedule starting on 31st clamps across 30-day and 29-day months and restores 31st in spring`() {
        // Loan starting on 1405/06/31 (31 Shahrivar 1405) for 8 monthly installments
        val dueDates = JalaliCalendarEngine.generateInstallmentDueDates(
            firstDueDateJalali = "1405/06/31",
            installmentCount = 8,
            interval = PaymentInterval.MONTHLY
        )

        val expected = listOf(
            "1405/06/31", // Shahrivar (31 days)
            "1405/07/30", // Mehr (30 days)
            "1405/08/30", // Aban (30 days)
            "1405/09/30", // Azar (30 days)
            "1405/10/30", // Dey (30 days)
            "1405/11/30", // Bahman (30 days)
            "1405/12/29", // Esfand 1405 (non-leap, 29 days)
            "1406/01/31"  // Farvardin 1406 (restores anchor day 31!)
        )
        assertEquals(expected, dueDates)
    }

    @Test
    fun `installment schedule engine preserves exact total toman sum and follows Jalali intervals`() {
        val installments = InstallmentScheduleEngine.generateAutoInstallments(
            obligationId = 42L,
            titlePrefix = "وام قرض‌الحسنه",
            netPayableToman = 100_000_000L,
            installmentCount = 3,
            firstDueDateJalali = "1405/11/30",
            interval = PaymentInterval.MONTHLY,
            todayJalali = "1405/11/01"
        )

        assertEquals(3, installments.size)
        // Sum must equal 100,000,000 with zero rounding loss (33,333,333 + 33,333,333 + 33,333,334)
        assertEquals(100_000_000L, installments.sumOf { it.amountToman })
        assertEquals("1405/11/30", installments[0].dueDateJalali)
        assertEquals("1405/12/29", installments[1].dueDateJalali) // Esfand 1405 clamps to 29
        assertEquals("1406/01/30", installments[2].dueDateJalali) // Farvardin 1406 restores anchor day 30
    }

    @Test
    fun `weekly and biweekly installment intervals cross Jalali month and year boundaries accurately`() {
        // Starting on 1405/12/25 (Esfand 1405 has 29 days), adding 7 days -> 1406/01/03
        val weeklyDates = JalaliCalendarEngine.generateInstallmentDueDates(
            firstDueDateJalali = "1405/12/25",
            installmentCount = 3,
            interval = PaymentInterval.WEEKLY
        )
        assertEquals(listOf("1405/12/25", "1406/01/03", "1406/01/10"), weeklyDates)
        assertEquals(7, JalaliCalendarEngine.diffJalaliDays(weeklyDates[0], weeklyDates[1]))
        assertEquals(14, JalaliCalendarEngine.diffJalaliDays(weeklyDates[0], weeklyDates[2]))
    }

    @Test
    fun `decade of month and month bounds follow Shamsi rules`() {
        assertEquals(1, JalaliCalendarEngine.getDecadeOfMonth("1405/08/01"))
        assertEquals(1, JalaliCalendarEngine.getDecadeOfMonth("1405/08/10"))
        assertEquals(2, JalaliCalendarEngine.getDecadeOfMonth("1405/08/11"))
        assertEquals(2, JalaliCalendarEngine.getDecadeOfMonth("1405/08/20"))
        assertEquals(3, JalaliCalendarEngine.getDecadeOfMonth("1405/08/21"))
        assertEquals(3, JalaliCalendarEngine.getDecadeOfMonth("1405/06/31"))

        assertEquals("1405/06/01" to "1405/06/31", JalaliCalendarEngine.getJalaliMonthBounds(1405, 6))
        assertEquals("1405/12/01" to "1405/12/29", JalaliCalendarEngine.getJalaliMonthBounds(1405, 12))
        assertEquals("1403/12/01" to "1403/12/30", JalaliCalendarEngine.getJalaliMonthBounds(1403, 12))
    }

    @Test
    fun `persian digits and date validation work for valid and invalid Jalali dates`() {
        assertTrue(JalaliCalendarEngine.isValidJalaliDate("۱۴۰۵/۰۶/۳۱"))
        assertFalse(JalaliCalendarEngine.isValidJalaliDate("1405/07/31")) // Mehr only has 30 days
        assertFalse(JalaliCalendarEngine.isValidJalaliDate("1405/12/30")) // 1405 is not a leap year
        assertTrue(JalaliCalendarEngine.isValidJalaliDate("1403/12/30"))  // 1403 is a leap year
        assertEquals("1405/08/05", JalaliCalendarEngine.normalizeJalaliDate("۱۴۰۵-۸-۵"))
    }
}
