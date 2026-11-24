package com.example.domain

import com.example.core.JalaliCalendarEngine
import kotlin.math.roundToInt

enum class RecurringCashflowType(
    val labelFa: String,
    val shortLabelFa: String,
    val accentColorHex: Long
) {
    INCOME(
        labelFa = "درآمد دوره‌ای ماهانه",
        shortLabelFa = "درآمد ثابت",
        accentColorHex = 0xFF35B8AB
    ),
    EXPENSE(
        labelFa = "هزینه دوره‌ای غیرقسطی",
        shortLabelFa = "هزینه جاری",
        accentColorHex = 0xFFDAB978
    )
}

enum class RecurringCashflowCategory(
    val labelFa: String,
    val type: RecurringCashflowType,
    val defaultIsEssential: Boolean,
    val colorHex: Long
) {
    SALARY(
        labelFa = "حقوق و دستمزد ماهانه",
        type = RecurringCashflowType.INCOME,
        defaultIsEssential = true,
        colorHex = 0xFF35B8AB
    ),
    FREELANCE_BUSINESS(
        labelFa = "درآمد پروژه، کسب‌وکار یا سود سپرده",
        type = RecurringCashflowType.INCOME,
        defaultIsEssential = false,
        colorHex = 0xFF53C9BD
    ),
    RENTAL_DIVIDEND(
        labelFa = "اجاره‌بهای دریافتی یا سود سهام",
        type = RecurringCashflowType.INCOME,
        defaultIsEssential = false,
        colorHex = 0xFF47B0C4
    ),
    HOUSING_UTILITIES(
        labelFa = "مسکن، شارژ ساختمان و قبوض",
        type = RecurringCashflowType.EXPENSE,
        defaultIsEssential = true,
        colorHex = 0xFFDAB978
    ),
    GROCERIES_LIVING(
        labelFa = "خوراک، پوشاک و مخارج روزمره خانوار",
        type = RecurringCashflowType.EXPENSE,
        defaultIsEssential = true,
        colorHex = 0xFFC99B68
    ),
    SUBSCRIPTIONS_INSURANCE(
        labelFa = "بیمه، اینترنت، اشتراک‌ها و آموزش",
        type = RecurringCashflowType.EXPENSE,
        defaultIsEssential = false,
        colorHex = 0xFF689EB8
    ),
    TRANSPORT_HEALTH(
        labelFa = "حمل‌ونقل، خودرو و سلامت",
        type = RecurringCashflowType.EXPENSE,
        defaultIsEssential = true,
        colorHex = 0xFF4DA3B8
    );

    companion object {
        fun fromName(raw: String): RecurringCashflowCategory {
            return entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: GROCERIES_LIVING
        }
    }
}

data class RecurringCashflowItem(
    val id: Long,
    val titleFa: String,
    val category: RecurringCashflowCategory,
    val monthlyAmountToman: Long,
    val dayOfShamsiMonth: Int = 1,
    val isEssential: Boolean = category.defaultIsEssential,
    val isActive: Boolean = true
) {
    val type: RecurringCashflowType
        get() = category.type
}

data class EnrichedRecurringCashflowItem(
    val item: RecurringCashflowItem,
    val nextOccurrenceDateJalali: String,
    val daysUntilNextOccurrence: Int,
    val shareOfIncomePercent: Int
)

data class HolisticFinancialHealthSummary(
    val shamsiYear: Int,
    val shamsiMonth: Int,
    val shamsiMonthNameFa: String,
    val enrichedItems: List<EnrichedRecurringCashflowItem>,
    val totalRecurringIncomeToman: Long,
    val totalRecurringNonLoanExpenseToman: Long,
    val essentialNonLoanExpenseToman: Long,
    val discretionaryNonLoanExpenseToman: Long,
    val currentMonthLoanInstallmentsToman: Long,
    val monthlySavingsTargetToman: Long,
    val netDisposableBeforeLoansToman: Long,
    val netFreeCashflowAfterLoansToman: Long,
    val unallocatedSurplusAfterSavingsToman: Long,
    val nonLoanExpenseToIncomePercent: Int,
    val loanDebtToIncomePercent: Int,
    val totalCommitmentsToIncomePercent: Int,
    val holisticHealthScore: Int,
    val healthStatusBadgeFa: String,
    val healthStatusColorHex: Long
)

/**
 * Pure domain engine for tracking recurring monthly incomes and non-loan expenses
 * and computing a holistic personal financial health report alongside loan obligations.
 */
object RecurringCashflowHealthEngine {

    fun defaultSampleRecurringItems(): List<RecurringCashflowItem> {
        return listOf(
            RecurringCashflowItem(
                id = 1L,
                titleFa = "حقوق و مزایای ماهانه شرکت",
                category = RecurringCashflowCategory.SALARY,
                monthlyAmountToman = 48_000_000L,
                dayOfShamsiMonth = 1,
                isEssential = true,
                isActive = true
            ),
            RecurringCashflowItem(
                id = 2L,
                titleFa = "درآمد مشاوره فنی و سود صندوق درآمد ثابت",
                category = RecurringCashflowCategory.FREELANCE_BUSINESS,
                monthlyAmountToman = 10_000_000L,
                dayOfShamsiMonth = 15,
                isEssential = false,
                isActive = true
            ),
            RecurringCashflowItem(
                id = 3L,
                titleFa = "خوراک، خرید روزمره و مایحتاج خانوار",
                category = RecurringCashflowCategory.GROCERIES_LIVING,
                monthlyAmountToman = 15_500_000L,
                dayOfShamsiMonth = 5,
                isEssential = true,
                isActive = true
            ),
            RecurringCashflowItem(
                id = 4L,
                titleFa = "شارژ ساختمان، قبوض برق/گاز و اینترنت فیبر نوری",
                category = RecurringCashflowCategory.HOUSING_UTILITIES,
                monthlyAmountToman = 4_200_000L,
                dayOfShamsiMonth = 10,
                isEssential = true,
                isActive = true
            ),
            RecurringCashflowItem(
                id = 5L,
                titleFa = "هزینه سوخت، سرویس خودرو و حمل‌ونقل شهری",
                category = RecurringCashflowCategory.TRANSPORT_HEALTH,
                monthlyAmountToman = 3_800_000L,
                dayOfShamsiMonth = 12,
                isEssential = true,
                isActive = true
            ),
            RecurringCashflowItem(
                id = 6L,
                titleFa = "بیمه تکمیلی، اشتراک‌های آموزشی و باشگاه",
                category = RecurringCashflowCategory.SUBSCRIPTIONS_INSURANCE,
                monthlyAmountToman = 3_500_000L,
                dayOfShamsiMonth = 20,
                isEssential = false,
                isActive = true
            )
        )
    }

    fun evaluateHolisticHealth(
        items: List<RecurringCashflowItem>,
        currentMonthLoanInstallmentsToman: Long,
        monthlySavingsTargetToman: Long = 0L,
        todayJalali: String = JalaliCalendarEngine.todayJalali()
    ): HolisticFinancialHealthSummary {
        val parsedToday = JalaliCalendarEngine.parseJalali(todayJalali)
        val currentMonthLength = JalaliCalendarEngine.getJalaliMonthLength(parsedToday.year, parsedToday.month)

        val activeItems = items.filter { it.isActive && it.monthlyAmountToman > 0L }

        val totalIncome = activeItems
            .filter { it.type == RecurringCashflowType.INCOME }
            .sumOf { it.monthlyAmountToman }

        val expenseItems = activeItems.filter { it.type == RecurringCashflowType.EXPENSE }
        val totalNonLoanExpense = expenseItems.sumOf { it.monthlyAmountToman }
        val essentialExpense = expenseItems.filter { it.isEssential }.sumOf { it.monthlyAmountToman }
        val discretionaryExpense = (totalNonLoanExpense - essentialExpense).coerceAtLeast(0L)

        val safeLoans = currentMonthLoanInstallmentsToman.coerceAtLeast(0L)
        val safeSavingsTarget = monthlySavingsTargetToman.coerceAtLeast(0L)

        val netDisposableBeforeLoans = totalIncome - totalNonLoanExpense
        val netFreeCashflowAfterLoans = netDisposableBeforeLoans - safeLoans
        val unallocatedSurplus = netFreeCashflowAfterLoans - safeSavingsTarget

        val incomeDenominator = totalIncome.coerceAtLeast(1L).toDouble()
        val nonLoanExpensePct = if (totalIncome > 0L) {
            ((totalNonLoanExpense.toDouble() / incomeDenominator) * 100.0).roundToInt().coerceIn(0, 999)
        } else {
            0
        }
        val loanDebtPct = if (totalIncome > 0L) {
            ((safeLoans.toDouble() / incomeDenominator) * 100.0).roundToInt().coerceIn(0, 999)
        } else {
            0
        }
        val totalCommitmentsPct = if (totalIncome > 0L) {
            (((totalNonLoanExpense + safeLoans).toDouble() / incomeDenominator) * 100.0).roundToInt().coerceIn(0, 999)
        } else {
            0
        }

        // Compute Holistic Financial Health Score (0..100)
        val holisticScore = when {
            totalIncome <= 0L -> if (totalNonLoanExpense + safeLoans == 0L) 80 else 25
            else -> {
                val freeRatio = netFreeCashflowAfterLoans.toDouble() / incomeDenominator
                val debtRatio = safeLoans.toDouble() / incomeDenominator
                val expenseRatio = totalNonLoanExpense.toDouble() / incomeDenominator
                val rawScore = 100.0 -
                    (debtRatio * 65.0) -
                    ((expenseRatio - 0.35).coerceAtLeast(0.0) * 55.0) +
                    (freeRatio.coerceIn(-0.5, 0.4) * 35.0)
                rawScore.roundToInt().coerceIn(10, 100)
            }
        }

        val (statusBadgeFa, statusColorHex) = when {
            netFreeCashflowAfterLoans < 0L -> "کسری بودجه ماهانه" to 0xFFD96B6B
            totalCommitmentsPct > 80 -> "فشار هزینه‌های جاری و اقساط" to 0xFFDAB978
            holisticScore >= 75 -> "تراز عالی و مازاد نقدینگی ✨" to 0xFF35B8AB
            else -> "تراز متعادل و پایدار" to 0xFF4DA3B8
        }

        val enriched = items.map { item ->
            val clampedDayThisMonth = item.dayOfShamsiMonth.coerceIn(1, currentMonthLength)
            val candidateThisMonth = "%04d/%02d/%02d".format(
                java.util.Locale.US,
                parsedToday.year,
                parsedToday.month,
                clampedDayThisMonth
            )
            val nextDateJalali = if (clampedDayThisMonth >= parsedToday.day) {
                candidateThisMonth
            } else {
                JalaliCalendarEngine.addJalaliMonths(candidateThisMonth, 1)
            }
            val daysUntil = JalaliCalendarEngine.diffJalaliDays(todayJalali, nextDateJalali).coerceAtLeast(0)
            val sharePct = if (totalIncome > 0L) {
                ((item.monthlyAmountToman.toDouble() / incomeDenominator) * 100.0).roundToInt().coerceIn(0, 999)
            } else {
                0
            }

            EnrichedRecurringCashflowItem(
                item = item,
                nextOccurrenceDateJalali = nextDateJalali,
                daysUntilNextOccurrence = daysUntil,
                shareOfIncomePercent = sharePct
            )
        }.sortedWith(
            compareBy<EnrichedRecurringCashflowItem> { it.item.type.ordinal }
                .thenByDescending { it.item.monthlyAmountToman }
        )

        return HolisticFinancialHealthSummary(
            shamsiYear = parsedToday.year,
            shamsiMonth = parsedToday.month,
            shamsiMonthNameFa = parsedToday.monthNameFa,
            enrichedItems = enriched,
            totalRecurringIncomeToman = totalIncome,
            totalRecurringNonLoanExpenseToman = totalNonLoanExpense,
            essentialNonLoanExpenseToman = essentialExpense,
            discretionaryNonLoanExpenseToman = discretionaryExpense,
            currentMonthLoanInstallmentsToman = safeLoans,
            monthlySavingsTargetToman = safeSavingsTarget,
            netDisposableBeforeLoansToman = netDisposableBeforeLoans,
            netFreeCashflowAfterLoansToman = netFreeCashflowAfterLoans,
            unallocatedSurplusAfterSavingsToman = unallocatedSurplus,
            nonLoanExpenseToIncomePercent = nonLoanExpensePct,
            loanDebtToIncomePercent = loanDebtPct,
            totalCommitmentsToIncomePercent = totalCommitmentsPct,
            holisticHealthScore = holisticScore,
            healthStatusBadgeFa = statusBadgeFa,
            healthStatusColorHex = statusColorHex
        )
    }

    private fun escapeField(raw: String): String {
        return raw.replace("|", " ").replace("\n", " ").trim()
    }

    fun serializeItems(items: List<RecurringCashflowItem>): String {
        if (items.isEmpty()) return "EMPTY"
        return items.joinToString(separator = "\n") { item ->
            listOf(
                item.id.toString(),
                escapeField(item.titleFa),
                item.category.name,
                item.monthlyAmountToman.toString(),
                item.dayOfShamsiMonth.toString(),
                item.isEssential.toString(),
                item.isActive.toString()
            ).joinToString(separator = "|")
        }
    }

    fun deserializeItems(serialized: String?): List<RecurringCashflowItem> {
        if (serialized == null) return defaultSampleRecurringItems()
        if (serialized.isBlank() || serialized == "EMPTY") return emptyList()
        return try {
            val lines = serialized.lines().filter { it.isNotBlank() }
            val result = mutableListOf<RecurringCashflowItem>()
            for ((idx, line) in lines.withIndex()) {
                val parts = line.split("|")
                if (parts.size >= 7) {
                    val cat = RecurringCashflowCategory.fromName(parts[2])
                    result += RecurringCashflowItem(
                        id = parts[0].toLongOrNull() ?: (idx + 1L),
                        titleFa = parts[1].ifBlank { cat.labelFa },
                        category = cat,
                        monthlyAmountToman = (parts[3].toLongOrNull() ?: 0L).coerceAtLeast(0L),
                        dayOfShamsiMonth = (parts[4].toIntOrNull() ?: 1).coerceIn(1, 31),
                        isEssential = parts[5].toBooleanStrictOrNull() ?: cat.defaultIsEssential,
                        isActive = parts[6].toBooleanStrictOrNull() ?: true
                    )
                }
            }
            if (result.isEmpty() && serialized != "EMPTY") defaultSampleRecurringItems() else result
        } catch (_: Exception) {
            defaultSampleRecurringItems()
        }
    }
}
