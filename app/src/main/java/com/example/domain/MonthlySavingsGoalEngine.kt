package com.example.domain

import com.example.core.JalaliCalendarEngine
import kotlin.math.roundToInt

enum class SavingsGoalHealthStatus(
    val labelFa: String,
    val badgeColorHex: Long
) {
    ACHIEVED("هدف محقق شد ✨", 0xFF35B8AB),
    ON_TRACK("در مسیر هدف", 0xFF35B8AB),
    NEEDS_BOOST("نیاز به تقویت", 0xFFDAB978),
    STRETCH_GOAL("فراتر از جریان نقدی آزاد", 0xFFD96B6B)
}

data class MonthlySavingsGoalSummary(
    val goalTitleFa: String,
    val shamsiYear: Int,
    val shamsiMonth: Int,
    val shamsiMonthNameFa: String,
    val daysElapsedInMonth: Int,
    val daysRemainingInMonth: Int,
    val monthLengthDays: Int,
    val targetAmountToman: Long,
    val savedAmountToman: Long,
    val remainingToTargetToman: Long,
    val surplusOverTargetToman: Long,
    val progressRatio: Float,
    val progressPercent: Int,
    val timeElapsedRatio: Float,
    val requiredDailySavingsToman: Long,
    val estimatedMonthlyFreeCashflowToman: Long,
    val recommendedTargetToman: Long,
    val isTargetAchieved: Boolean,
    val status: SavingsGoalHealthStatus
)

/**
 * Pure domain engine for calculating and tracking monthly savings goals
 * aligned with the Persian (Shamsi / Jalali) calendar month.
 */
object MonthlySavingsGoalEngine {

    fun evaluateMonthlySavingsGoal(
        goalTitleFa: String,
        targetAmountToman: Long,
        savedAmountToman: Long,
        monthlyIncomeToman: Long,
        livingExpensePercent: Int,
        currentMonthDebtDueToman: Long,
        todayJalali: String = JalaliCalendarEngine.todayJalali()
    ): MonthlySavingsGoalSummary {
        val parsedToday = JalaliCalendarEngine.parseJalali(todayJalali)
        val monthLength = JalaliCalendarEngine.getJalaliMonthLength(parsedToday.year, parsedToday.month)
        val daysElapsed = parsedToday.day.coerceIn(1, monthLength)
        val daysRemaining = (monthLength - daysElapsed).coerceAtLeast(0)

        val safeTarget = targetAmountToman.coerceAtLeast(0L)
        val safeSaved = savedAmountToman.coerceAtLeast(0L)
        val remainingToTarget = (safeTarget - safeSaved).coerceAtLeast(0L)
        val surplus = (safeSaved - safeTarget).coerceAtLeast(0L)

        val progressRatio = when {
            safeTarget <= 0L -> if (safeSaved > 0L) 1f else 0f
            else -> (safeSaved.toFloat() / safeTarget.toFloat()).coerceIn(0f, 1f)
        }
        val rawPercent = when {
            safeTarget <= 0L -> if (safeSaved > 0L) 100 else 0
            else -> ((safeSaved.toDouble() / safeTarget.toDouble()) * 100.0).roundToInt().coerceIn(0, 999)
        }

        val timeElapsedRatio = (daysElapsed.toFloat() / monthLength.toFloat()).coerceIn(0f, 1f)

        val requiredDaily = when {
            remainingToTarget <= 0L -> 0L
            daysRemaining <= 0 -> remainingToTarget
            else -> (remainingToTarget + daysRemaining - 1) / daysRemaining
        }

        val safeIncome = monthlyIncomeToman.coerceAtLeast(0L)
        val livingCost = (safeIncome * livingExpensePercent.coerceIn(0, 100)) / 100L
        val freeCashflow = (safeIncome - livingCost - currentMonthDebtDueToman.coerceAtLeast(0L)).coerceAtLeast(0L)

        // Recommend 20% of income or 60% of free cashflow after debt & living costs
        val recommendedTarget = when {
            freeCashflow > 0L -> ((freeCashflow * 60L) / 100L).coerceAtLeast(1_000_000L)
            safeIncome > 0L -> ((safeIncome * 15L) / 100L).coerceAtLeast(1_000_000L)
            else -> 5_000_000L
        }

        val isAchieved = safeTarget > 0L && safeSaved >= safeTarget

        val status = when {
            isAchieved -> SavingsGoalHealthStatus.ACHIEVED
            safeTarget > freeCashflow && freeCashflow > 0L && remainingToTarget > freeCashflow ->
                SavingsGoalHealthStatus.STRETCH_GOAL
            progressRatio + 0.08f >= timeElapsedRatio ->
                SavingsGoalHealthStatus.ON_TRACK
            else ->
                SavingsGoalHealthStatus.NEEDS_BOOST
        }

        return MonthlySavingsGoalSummary(
            goalTitleFa = goalTitleFa.ifBlank { "پس‌انداز و ذخیره طلایی ماه" },
            shamsiYear = parsedToday.year,
            shamsiMonth = parsedToday.month,
            shamsiMonthNameFa = parsedToday.monthNameFa,
            daysElapsedInMonth = daysElapsed,
            daysRemainingInMonth = daysRemaining,
            monthLengthDays = monthLength,
            targetAmountToman = safeTarget,
            savedAmountToman = safeSaved,
            remainingToTargetToman = remainingToTarget,
            surplusOverTargetToman = surplus,
            progressRatio = progressRatio,
            progressPercent = rawPercent,
            timeElapsedRatio = timeElapsedRatio,
            requiredDailySavingsToman = requiredDaily,
            estimatedMonthlyFreeCashflowToman = freeCashflow,
            recommendedTargetToman = recommendedTarget,
            isTargetAchieved = isAchieved,
            status = status
        )
    }
}
