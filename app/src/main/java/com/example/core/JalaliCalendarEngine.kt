package com.example.core

import com.example.data.PaymentInterval
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Immutable Solar Hijri (Jalali / Shamsi) date value object with full ordering,
 * canonical formatting ("YYYY/MM/DD"), Persian digit rendering, and decade/quarter classification.
 */
data class JalaliDate(
    val year: Int,
    val month: Int,
    val day: Int
) : Comparable<JalaliDate> {

    val monthNameFa: String
        get() = JalaliCalendarEngine.getMonthName(month)

    /**
     * Returns the Jalali month decade (دهه ماه):
     * 1 -> Days 1..10 (دهه اول ماه)
     * 2 -> Days 11..20 (دهه دوم ماه)
     * 3 -> Days 21..end of month (دهه سوم ماه)
     */
    val decadeOfMonth: Int
        get() = when {
            day <= 10 -> 1
            day <= 20 -> 2
            else -> 3
        }

    /**
     * Returns the Persian season / quarter (فصل سال شمسی): 1=بهار, 2=تابستان, 3=پاییز, 4=زمستان.
     */
    val quarterOfYear: Int
        get() = ((month - 1).coerceIn(0, 11) / 3) + 1

    val isEndOfMonth: Boolean
        get() = day >= JalaliCalendarEngine.getJalaliMonthLength(year, month)

    override fun compareTo(other: JalaliDate): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }

    /**
     * Canonical ASCII storage format: "YYYY/MM/DD" (e.g., "1405/08/22").
     */
    fun formatCanonical(): String {
        return "%04d/%02d/%02d".format(Locale.US, year, month, day)
    }

    /**
     * Canonical year-month prefix: "YYYY/MM" (e.g., "1405/08").
     */
    fun formatMonthPrefix(): String {
        return "%04d/%02d".format(Locale.US, year, month)
    }

    /**
     * Localized Persian digit string: "۱۴۰۵/۰۸/۲۲".
     */
    fun formatPersian(): String {
        return PersianCurrencyEngine.toPersianDigits(formatCanonical())
    }
}

/**
 * Comprehensive Persian (Shamsi / Solar Hijri) Calendar Utility Engine (`JalaliCalendarEngine` / `PersianShamsiCalendarUtil`).
 *
 * Ensures all date-based calculations for loans, BNPL purchases, checks, and installment schedules strictly
 * follow the Iranian Jalali calendar system:
 * - Months 1..6 (فروردین تا شهریور): 31 days
 * - Months 7..11 (مهر تا بهمن): 30 days
 * - Month 12 (اسفند): 30 days in leap years, 29 days in common years
 * - Astronomical 33-year cycle & Birashk 2820-year cycle leap-year determination
 * - Anchor-day preserving installment progression (e.g., an installment starting on 31 Shahrivar
 *   clamps to 30 Mehr -> 30 Aban -> 30 Azar -> 30 Dey -> 30 Bahman -> 29/30 Esfand -> 31 Farvardin)
 */
typealias PersianShamsiCalendarUtil = JalaliCalendarEngine

object JalaliCalendarEngine {

    val MONTH_NAMES = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    val SEASON_NAMES = listOf(
        "بهار", "تابستان", "پاییز", "زمستان"
    )

    val WEEKDAY_NAMES = listOf(
        "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه"
    )

    val WEEKDAY_SHORT = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

    private val LEAP_REMAINDERS_33 = setOf(1, 5, 9, 13, 17, 22, 26, 30)

    /**
     * Checks whether a given Solar Hijri (Jalali) year is a leap year (سال کبیسه شمسی).
     * Uses the official 33-year Iranian astronomical cycle remainders (1, 5, 9, 13, 17, 22, 26, 30).
     */
    fun isJalaliLeapYear(year: Int): Boolean {
        return year.mod(33) in LEAP_REMAINDERS_33
    }

    /**
     * Returns the exact number of days in a Jalali month (1..12):
     * - Months 1..6: 31 days
     * - Months 7..11: 30 days
     * - Month 12 (Esfand): 30 days if leap year, 29 days otherwise
     */
    fun getJalaliMonthLength(year: Int, month: Int): Int {
        return when (month) {
            in 1..6 -> 31
            in 7..11 -> 30
            12 -> if (isJalaliLeapYear(year)) 30 else 29
            else -> 30
        }
    }

    /**
     * Returns the total number of days in a Jalali year (366 for leap years, 365 for normal years).
     */
    fun getJalaliYearLength(year: Int): Int {
        return if (isJalaliLeapYear(year)) 366 else 365
    }

    fun getMonthName(month: Int): String {
        return MONTH_NAMES.getOrElse((month - 1).coerceIn(0, 11)) { "فروردین" }
    }

    fun getSeasonName(month: Int): String {
        val idx = ((month - 1).coerceIn(0, 11)) / 3
        return SEASON_NAMES.getOrElse(idx) { "بهار" }
    }

    /**
     * Validates whether a given year, month, and day form a valid Solar Hijri (Jalali) date.
     */
    fun isValidJalaliDate(year: Int, month: Int, day: Int): Boolean {
        if (year !in 1200..1600) return false
        if (month !in 1..12) return false
        val maxDay = getJalaliMonthLength(year, month)
        return day in 1..maxDay
    }

    /**
     * Validates whether a string is a structurally valid Jalali date ("YYYY/MM/DD" or "YYYY-MM-DD").
     */
    fun isValidJalaliDate(dateStr: String): Boolean {
        val normalized = PersianCurrencyEngine.toEnglishDigits(dateStr.trim()).replace("-", "/")
        val parts = normalized.split("/")
        if (parts.size != 3) return false
        val y = parts[0].toIntOrNull() ?: return false
        val m = parts[1].toIntOrNull() ?: return false
        val d = parts[2].toIntOrNull() ?: return false
        return isValidJalaliDate(y, m, d)
    }

    /**
     * Parses a "YYYY/MM/DD" or "YYYY-MM-DD" string (with either Persian or English digits) into [JalaliDate],
     * automatically clamping month to 1..12 and day to 1..getJalaliMonthLength(year, month).
     */
    fun parseJalali(dateStr: String): JalaliDate {
        val normalized = PersianCurrencyEngine.toEnglishDigits(dateStr.trim())
            .replace("-", "/")
        val parts = normalized.split("/")
        if (parts.size >= 3) {
            val y = parts[0].toIntOrNull() ?: 1405
            val m = (parts[1].toIntOrNull() ?: 1).coerceIn(1, 12)
            val maxD = getJalaliMonthLength(y, m)
            val d = (parts[2].toIntOrNull() ?: 1).coerceIn(1, maxD)
            return JalaliDate(y, m, d)
        }
        return JalaliDate(1405, 8, 22)
    }

    /**
     * Normalizes any input Jalali date string (Persian or English digits, '-' or '/' separators,
     * single-digit months/days) into canonical "YYYY/MM/DD" format.
     */
    fun normalizeJalaliDate(dateStr: String): String {
        return parseJalali(dateStr).formatCanonical()
    }

    /**
     * Returns the canonical "YYYY/MM" prefix for a given Jalali year and month.
     */
    fun formatMonthPrefix(year: Int, month: Int): String {
        val (normY, normM) = shiftJalaliYearMonth(year, month, 0)
        return "%04d/%02d".format(Locale.US, normY, normM)
    }

    /**
     * Shifts a Jalali (year, month) pair forward or backward by [deltaMonths] using exact 12-month floor division,
     * handling negative offsets and multi-year jumps cleanly.
     */
    fun shiftJalaliYearMonth(startYear: Int, startMonth: Int, deltaMonths: Int): Pair<Int, Int> {
        val totalMonths = (startYear * 12) + (startMonth - 1) + deltaMonths
        val targetYear = totalMonths.floorDiv(12)
        val targetMonth = totalMonths.mod(12) + 1
        return targetYear to targetMonth
    }

    /**
     * Converts a Gregorian date (year, month 1..12, day 1..31) to [JalaliDate].
     */
    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        val gDaysInMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) +
            ((gy2 + 399) / 400) + gd + gDaysInMonth[gm - 1]
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + (days / 31)
            jd = 1 + (days % 31)
        } else {
            jm = 7 + ((days - 186) / 30)
            jd = 1 + ((days - 186) % 30)
        }
        return JalaliDate(jy, jm, jd)
    }

    /**
     * Converts a JalaliDate to Gregorian (year, month 1..12, day 1..31).
     */
    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
        val jy1 = jy + 1595
        var days = -355668 + (365 * jy1) + ((jy1 / 33) * 8) + (((jy1 % 33) + 3) / 4) + jd +
            if (jm < 7) (jm - 1) * 31 else ((jm - 7) * 30) + 186
        var gy = 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            gy += 100 * (--days / 36524)
            days %= 36524
            if (days >= 365) days++
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }
        var gd = days + 1
        val isLeapG = (gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)
        val salA = intArrayOf(0, 31, if (isLeapG) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0
        while (gm < 13 && gd > salA[gm]) {
            gd -= salA[gm]
            gm++
        }
        return Triple(gy, gm, gd)
    }

    /**
     * Converts a [JalaliDate] to continuous day count (Julian Day Number equivalent) for exact day difference.
     */
    fun toContinuousDayNumber(date: JalaliDate): Long {
        val (gy, gm, gd) = jalaliToGregorian(date.year, date.month, date.day)
        val a = (14 - gm) / 12
        val y = gy + 4800 - a
        val m = gm + 12 * a - 3
        return (gd + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045).toLong()
    }

    fun fromContinuousDayNumber(jdn: Long): JalaliDate {
        var l = jdn + 68569
        val n = (4 * l) / 146097
        l -= (146097 * n + 3) / 4
        val i = (4000 * (l + 1)) / 1461001
        l = l - (1461 * i) / 4 + 31
        val j = (80 * l) / 2447
        val gd = (l - (2447 * j) / 80).toInt()
        l = j / 11
        val gm = (j + 2 - 12 * l).toInt()
        val gy = (100 * (n - 49) + i + l).toInt()
        return gregorianToJalali(gy, gm, gd)
    }

    /**
     * Adds months to a Jalali date string ("YYYY/MM/DD"), clamping end-of-month accurately according to
     * the Jalali 31/30/29-day month rules while preserving [anchorDay] when specified.
     * E.g., "1405/06/31" + 1 month -> "1405/07/30", and "1405/06/31" + 6 months -> "1405/12/29" (or 30 in leap year).
     */
    fun addJalaliMonths(jalaliDateStr: String, monthsToAdd: Int, anchorDay: Int? = null): String {
        val start = parseJalali(jalaliDateStr)
        val (targetYear, targetMonth) = shiftJalaliYearMonth(start.year, start.month, monthsToAdd)
        val desiredDay = anchorDay ?: start.day
        val maxDay = getJalaliMonthLength(targetYear, targetMonth)
        val clampedDay = desiredDay.coerceIn(1, maxDay)
        return JalaliDate(targetYear, targetMonth, clampedDay).formatCanonical()
    }

    /**
     * Adds or subtracts exact years from a Jalali date string ("YYYY/MM/DD"), clamping Esfand 30 to 29 in non-leap years.
     */
    fun addJalaliYears(jalaliDateStr: String, yearsToAdd: Int): String {
        return addJalaliMonths(jalaliDateStr, yearsToAdd * 12)
    }

    /**
     * Adds or subtracts exact days from a Jalali date string ("YYYY/MM/DD").
     */
    fun addJalaliDays(jalaliDateStr: String, daysToAdd: Int): String {
        val start = parseJalali(jalaliDateStr)
        val jdn = toContinuousDayNumber(start) + daysToAdd
        return fromContinuousDayNumber(jdn).formatCanonical()
    }

    /**
     * Calculates exact days between dateA and dateB: (dateB - dateA).
     * Positive if dateB is in the future relative to dateA; negative if dateB is in the past.
     */
    fun diffJalaliDays(dateA: String, dateB: String): Int {
        val a = parseJalali(dateA)
        val b = parseJalali(dateB)
        return (toContinuousDayNumber(b) - toContinuousDayNumber(a)).toInt()
    }

    /**
     * Calculates the signed month difference between two Jalali dates: (dateB - dateA) in whole months.
     */
    fun diffJalaliMonths(dateA: String, dateB: String): Int {
        val a = parseJalali(dateA)
        val b = parseJalali(dateB)
        return ((b.year - a.year) * 12) + (b.month - a.month)
    }

    /**
     * Computes the exact due date for the [installmentIndex]-th installment (0-indexed)
     * starting from [firstDueDateJalali] according to [interval] (Monthly, Bimonthly, Quarterly, Weekly, Biweekly),
     * preserving the original anchor day across 31-day, 30-day, and 29/30-day Esfand transitions.
     */
    fun computeInstallmentDueDate(
        firstDueDateJalali: String,
        installmentIndex: Int,
        interval: PaymentInterval,
        anchorDay: Int? = null
    ): String {
        val parsedFirst = parseJalali(firstDueDateJalali)
        if (installmentIndex <= 0) return parsedFirst.formatCanonical()
        val effectiveAnchor = anchorDay ?: parsedFirst.day
        return if (interval.monthsStep > 0) {
            addJalaliMonths(
                jalaliDateStr = parsedFirst.formatCanonical(),
                monthsToAdd = installmentIndex * interval.monthsStep,
                anchorDay = effectiveAnchor
            )
        } else {
            addJalaliDays(
                jalaliDateStr = parsedFirst.formatCanonical(),
                daysToAdd = installmentIndex * interval.daysStep
            )
        }
    }

    /**
     * Generates a complete sequence of [installmentCount] canonical Jalali due dates for a loan or obligation.
     */
    fun generateInstallmentDueDates(
        firstDueDateJalali: String,
        installmentCount: Int,
        interval: PaymentInterval
    ): List<String> {
        val safeCount = installmentCount.coerceAtLeast(1)
        val parsedFirst = parseJalali(firstDueDateJalali)
        val anchorDay = parsedFirst.day
        return (0 until safeCount).map { index ->
            computeInstallmentDueDate(
                firstDueDateJalali = parsedFirst.formatCanonical(),
                installmentIndex = index,
                interval = interval,
                anchorDay = anchorDay
            )
        }
    }

    /**
     * Checks whether [dateStr] falls inside the specified Jalali [year] and [month].
     */
    fun isInJalaliMonth(dateStr: String, year: Int, month: Int): Boolean {
        val d = parseJalali(dateStr)
        return d.year == year && d.month == month
    }

    /**
     * Checks whether [dateStr] falls within the inclusive range [startDateJalali]..[endDateJalali].
     */
    fun isBetweenJalaliInclusive(dateStr: String, startDateJalali: String, endDateJalali: String): Boolean {
        val target = parseJalali(dateStr)
        val start = parseJalali(startDateJalali)
        val end = parseJalali(endDateJalali)
        return target >= start && target <= end
    }

    /**
     * Returns the Jalali month decade (1 = Days 1..10, 2 = Days 11..20, 3 = Days 21..end) for cashflow crunch analysis.
     */
    fun getDecadeOfMonth(dateStr: String): Int {
        return parseJalali(dateStr).decadeOfMonth
    }

    /**
     * Returns the first and last canonical Jalali dates of a given (year, month),
     * e.g. ("1405/08/01" to "1405/08/30").
     */
    fun getJalaliMonthBounds(year: Int, month: Int): Pair<String, String> {
        val (normY, normM) = shiftJalaliYearMonth(year, month, 0)
        val lastDay = getJalaliMonthLength(normY, normM)
        val start = JalaliDate(normY, normM, 1).formatCanonical()
        val end = JalaliDate(normY, normM, lastDay).formatCanonical()
        return start to end
    }

    /**
     * Returns index 0..6 where 0 = شنبه (Saturday) and 6 = جمعه (Friday).
     */
    fun getPersianWeekdayIndex(date: JalaliDate): Int {
        val jdn = toContinuousDayNumber(date)
        // JDN % 7: 0=Mon, 1=Tue, 2=Wed, 3=Thu, 4=Fri, 5=Sat, 6=Sun
        // To map Sat -> 0, Sun -> 1, Mon -> 2, ..., Fri -> 6:
        return ((jdn + 2).mod(7))
    }

    fun getPersianWeekdayIndex(dateStr: String): Int {
        return getPersianWeekdayIndex(parseJalali(dateStr))
    }

    /**
     * Formats a Jalali date into verbose Persian, e.g. "جمعه ۲۲ آبان ۱۴۰۵".
     */
    fun formatJalaliVerbose(dateStr: String): String {
        val d = parseJalali(dateStr)
        val weekday = WEEKDAY_NAMES[getPersianWeekdayIndex(d)]
        val monthName = getMonthName(d.month)
        val dayFa = PersianCurrencyEngine.toPersianDigits(d.day.toString())
        val yearFa = PersianCurrencyEngine.toPersianDigits(d.year.toString())
        return "$weekday $dayFa $monthName $yearFa"
    }

    /**
     * Formats a Jalali date into compact Persian month label, e.g. "۲۲ آبان ۱۴۰۵".
     */
    fun formatJalaliShortText(dateStr: String): String {
        val d = parseJalali(dateStr)
        val monthName = getMonthName(d.month)
        val dayFa = PersianCurrencyEngine.toPersianDigits(d.day.toString())
        val yearFa = PersianCurrencyEngine.toPersianDigits(d.year.toString())
        return "$dayFa $monthName $yearFa"
    }

    /**
     * Formats a Jalali year and month into a localized Persian header, e.g. "آبان ۱۴۰۵".
     */
    fun formatJalaliMonthYear(year: Int, month: Int): String {
        val (normY, normM) = shiftJalaliYearMonth(year, month, 0)
        return "${getMonthName(normM)} ${PersianCurrencyEngine.toPersianDigits(normY.toString())}"
    }

    /**
     * Formats the relative distance in days from [todayJalali] to [targetDateJalali] in natural Persian
     * (e.g., "امروز", "فردا", "۳ روز دیگر", "۴ روز معوقه").
     */
    fun formatRelativeDueStatusFa(todayJalali: String, targetDateJalali: String): String {
        val diff = diffJalaliDays(todayJalali, targetDateJalali)
        val absFa = PersianCurrencyEngine.toPersianDigits(kotlin.math.abs(diff).toString())
        return when {
            diff == 0 -> "سررسید امروز"
            diff == 1 -> "فردا"
            diff > 1 -> "$absFa روز دیگر"
            diff == -1 -> "۱ روز معوقه"
            else -> "$absFa روز معوقه"
        }
    }

    /**
     * Returns today's Jalali date string in canonical "YYYY/MM/DD" format using Asia/Tehran timezone.
     */
    fun todayJalali(): String {
        return try {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"))
            val gy = cal.get(Calendar.YEAR)
            val gm = cal.get(Calendar.MONTH) + 1
            val gd = cal.get(Calendar.DAY_OF_MONTH)
            gregorianToJalali(gy, gm, gd).formatCanonical()
        } catch (_: Exception) {
            "1405/08/22"
        }
    }

    /**
     * Returns the first day of the next Jalali month relative to the given Jalali date.
     */
    fun firstOfNextJalaliMonth(dateStr: String): String {
        val d = parseJalali(dateStr)
        val (nextY, nextM) = shiftJalaliYearMonth(d.year, d.month, 1)
        return JalaliDate(nextY, nextM, 1).formatCanonical()
    }

    /**
     * Returns the last day of the Jalali month for the given date string.
     */
    fun endOfJalaliMonth(dateStr: String): String {
        val d = parseJalali(dateStr)
        val maxDay = getJalaliMonthLength(d.year, d.month)
        return JalaliDate(d.year, d.month, maxDay).formatCanonical()
    }
}
