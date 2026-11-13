package com.example.core

import java.util.Calendar
import java.util.TimeZone

data class JalaliDate(
    val year: Int,
    val month: Int,
    val day: Int
) : Comparable<JalaliDate> {
    override fun compareTo(other: JalaliDate): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }

    fun formatCanonical(): String {
        return "%04d/%02d/%02d".format(java.util.Locale.US, year, month, day)
    }

    fun formatPersian(): String {
        return PersianCurrencyEngine.toPersianDigits(formatCanonical())
    }
}

object JalaliCalendarEngine {

    val MONTH_NAMES = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    val WEEKDAY_NAMES = listOf(
        "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه"
    )

    val WEEKDAY_SHORT = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

    private val LEAP_REMAINDERS_33 = setOf(1, 5, 9, 13, 17, 22, 26, 30)

    /**
     * Checks whether a given Solar Hijri (Jalali) year is a leap year.
     * Uses the official 33-year Iranian astronomical cycle remainders (1, 5, 9, 13, 17, 22, 26, 30).
     */
    fun isJalaliLeapYear(year: Int): Boolean {
        return year.mod(33) in LEAP_REMAINDERS_33
    }

    /**
     * Returns the exact number of days in a Jalali month (1..12).
     */
    fun getJalaliMonthLength(year: Int, month: Int): Int {
        return when (month) {
            in 1..6 -> 31
            in 7..11 -> 30
            12 -> if (isJalaliLeapYear(year)) 30 else 29
            else -> 30
        }
    }

    fun getMonthName(month: Int): String {
        return MONTH_NAMES.getOrElse((month - 1).coerceIn(0, 11)) { "فروردین" }
    }

    /**
     * Parses a "YYYY/MM/DD" or "YYYY-MM-DD" string (with either Persian or English digits) into JalaliDate.
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
     * Converts a Gregorian date (year, month 1..12, day 1..31) to JalaliDate.
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
     * Converts a JalaliDate to continuous day count (Julian Day Number equivalent) for exact day difference.
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
     * Adds months to a Jalali date string ("YYYY/MM/DD"), clamping end-of-month accurately.
     * E.g., "1405/06/31" + 1 month -> "1405/07/30", and "1405/06/31" + 6 months -> "1405/12/29" (or 30 in leap year).
     */
    fun addJalaliMonths(jalaliDateStr: String, monthsToAdd: Int, anchorDay: Int? = null): String {
        val start = parseJalali(jalaliDateStr)
        val totalMonths = (start.year * 12) + (start.month - 1) + monthsToAdd
        val targetYear = totalMonths.floorDiv(12)
        val targetMonth = (totalMonths.mod(12)) + 1
        val desiredDay = anchorDay ?: start.day
        val maxDay = getJalaliMonthLength(targetYear, targetMonth)
        val clampedDay = desiredDay.coerceIn(1, maxDay)
        return JalaliDate(targetYear, targetMonth, clampedDay).formatCanonical()
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
     * Returns today's Jalali date string in canonical "YYYY/MM/DD" format.
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
        return if (d.month == 12) {
            JalaliDate(d.year + 1, 1, 1).formatCanonical()
        } else {
            JalaliDate(d.year, d.month + 1, 1).formatCanonical()
        }
    }
}
