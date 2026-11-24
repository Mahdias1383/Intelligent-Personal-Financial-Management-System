package com.example.domain

import com.example.core.CurrencyUnit
import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.data.CounterpartyEntity
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.LedgerSubRecordEntity
import com.example.data.ObligationEntity
import com.example.data.SubRecordType
import com.example.data.UserPreferencesState
import kotlin.math.roundToInt

enum class TipUrgencyTone(
    val labelFa: String,
    val badgeColorHex: Long
) {
    PROTECTIVE("اولویت حفاظتی", 0xFFD96B6B),
    OPTIMIZATION("فرصت بهینه‌سازی", 0xFFDAB978),
    MOMENTUM("مسیر متعادل و رو‌به‌رشد", 0xFF35B8AB)
}

data class SpendingAndDebtSnapshot(
    val todayJalali: String,
    val monthNameFa: String,
    val monthlyIncomeToman: Long,
    val livingExpensePercent: Int,
    val totalRecurringNonLoanExpenseToman: Long,
    val essentialExpenseToman: Long,
    val discretionaryExpenseToman: Long,
    val topSpendingCategoriesFa: List<String>,
    val recentPaidLast30DaysToman: Long,
    val recentPaidInstallmentsCount: Int,
    val currentMonthDebtDueToman: Long,
    val totalDebtRemainingToman: Long,
    val dtiPercent: Int,
    val overdueCount: Int,
    val overdueAmountToman: Long,
    val dueNext7DaysCount: Int,
    val dueNext7DaysAmountToman: Long,
    val highestInterestDebtTitleFa: String?,
    val highestInterestAnnualRatePercent: Double,
    val highestInterestRemainingToman: Long,
    val savingsGoalTitleFa: String,
    val savingsProgressPercent: Int,
    val activeDebtCount: Int,
    val preferredAutoPayDay: Int,
    val profileFingerprint: String
)

data class DailyFinancialTipResult(
    val headlineFa: String,
    val spendingPatternInsightFa: String,
    val dailyTipFa: String,
    val concreteActionStepFa: String,
    val focusBadgeFa: String,
    val estimatedImpactFa: String,
    val urgencyTone: TipUrgencyTone,
    val generatedDateJalali: String,
    val profileFingerprint: String
)

sealed interface DailyAiTipUiState {
    data object Idle : DailyAiTipUiState
    data object Loading : DailyAiTipUiState
    data class Success(
        val tip: DailyFinancialTipResult,
        val isCachedFromToday: Boolean
    ) : DailyAiTipUiState

    data class Error(val messageFa: String) : DailyAiTipUiState
}

/**
 * Pure Kotlin domain engine for building a structured spending & debt snapshot,
 * formatting the personalized Gemini prompt, and caching same-day tips without
 * depending on Android framework JSON classes so JVM unit tests run cleanly.
 */
object DailyAiFinancialTipEngine {

    fun buildSnapshot(
        obligations: List<ObligationEntity>,
        installments: List<InstallmentEntity>,
        counterparties: List<CounterpartyEntity>,
        subRecords: List<LedgerSubRecordEntity>,
        recurringItems: List<RecurringCashflowItem>,
        dtiReport: DtiHealthReport?,
        savingsSummary: MonthlySavingsGoalSummary?,
        preferences: UserPreferencesState,
        todayJalali: String = JalaliCalendarEngine.todayJalali()
    ): SpendingAndDebtSnapshot {
        val parsedToday = JalaliCalendarEngine.parseJalali(todayJalali)
        val debtObligations = obligations.filter { !it.categoryEnum.isReceivable }
        val debtObIds = debtObligations.map { it.id }.toSet()
        val debtInstallments = installments.filter { it.obligationId in debtObIds }

        val totalDebtRemaining = debtInstallments.sumOf { it.remainingToman }
        val currentMonthDebtDue = dtiReport?.currentMonthDebtDueToman ?: 0L
        val dtiPct = dtiReport?.dtiPercent?.coerceIn(0, 999)
            ?: if (preferences.monthlyIncomeToman > 0L) {
                ((currentMonthDebtDue.toDouble() / preferences.monthlyIncomeToman.toDouble()) * 100.0)
                    .roundToInt()
                    .coerceIn(0, 999)
            } else {
                0
            }

        // Overdue and upcoming 7-day debt pressure
        var overdueCount = 0
        var overdueAmount = 0L
        var dueNext7Count = 0
        var dueNext7Amount = 0L
        debtInstallments.filter { it.statusEnum != InstallmentStatus.PAID && it.remainingToman > 0L }
            .forEach { inst ->
                val diff = JalaliCalendarEngine.diffJalaliDays(todayJalali, inst.dueDateJalali)
                if (diff < 0) {
                    overdueCount++
                    overdueAmount += inst.remainingToman
                } else if (diff in 0..7) {
                    dueNext7Count++
                    dueNext7Amount += inst.remainingToman
                }
            }

        // Recent 30-day outflow from paid installments + recent ledger sub-record charges/payments
        val recentPaidInstallments = debtInstallments.filter { inst ->
            inst.paidAmountToman > 0L && inst.paidDateJalali.isNotBlank() &&
                JalaliCalendarEngine.diffJalaliDays(inst.paidDateJalali, todayJalali) in 0..30
        }
        val recentSubPayments = subRecords.filter { sr ->
            sr.recordTypeEnum == SubRecordType.PAYMENT && sr.dateJalali.isNotBlank() &&
                JalaliCalendarEngine.diffJalaliDays(sr.dateJalali, todayJalali) in 0..30
        }.sumOf { it.amountToman }
        val recentPaidTotal = recentPaidInstallments.sumOf { it.paidAmountToman } + recentSubPayments

        // Recurring non-loan spending patterns
        val activeExpenses = recurringItems.filter {
            it.isActive && it.type == RecurringCashflowType.EXPENSE && it.monthlyAmountToman > 0L
        }
        val totalNonLoanExpense = activeExpenses.sumOf { it.monthlyAmountToman }
        val essentialExpense = activeExpenses.filter { it.isEssential }.sumOf { it.monthlyAmountToman }
        val discretionaryExpense = (totalNonLoanExpense - essentialExpense).coerceAtLeast(0L)

        val topSpendingCategories = activeExpenses
            .sortedByDescending { it.monthlyAmountToman }
            .take(3)
            .map { item ->
                val tag = if (item.isEssential) "ضروری" else "اختیاری"
                "${item.titleFa} ($tag): ${formatTomanBrief(item.monthlyAmountToman)}"
            }

        // Highest-APR active debt using DebtPayoffStrategyRankingEngine
        val payoffPlan = DebtPayoffStrategyRankingEngine.buildStrategyReport(
            obligations = obligations,
            installments = installments,
            counterparties = counterparties,
            todayJalali = todayJalali
        )
        val highestAprDebt = payoffPlan.rankedDebts.maxByOrNull { it.effectiveAnnualRatePercent }
        val activeDebtCount = payoffPlan.rankedDebts.size

        val savingsPct = savingsSummary?.progressPercent?.coerceIn(0, 100)
            ?: if (preferences.monthlySavingsTargetToman > 0L) {
                ((preferences.monthlySavingsSavedToman * 100L) / preferences.monthlySavingsTargetToman)
                    .toInt()
                    .coerceIn(0, 100)
            } else {
                0
            }

        val fingerprint = listOf(
            todayJalali,
            totalDebtRemaining.toString(),
            currentMonthDebtDue.toString(),
            totalNonLoanExpense.toString(),
            overdueCount.toString(),
            activeDebtCount.toString(),
            preferences.monthlyIncomeToman.toString()
        ).joinToString("-")

        return SpendingAndDebtSnapshot(
            todayJalali = todayJalali,
            monthNameFa = parsedToday.monthNameFa,
            monthlyIncomeToman = preferences.monthlyIncomeToman,
            livingExpensePercent = preferences.livingExpensePercent,
            totalRecurringNonLoanExpenseToman = totalNonLoanExpense,
            essentialExpenseToman = essentialExpense,
            discretionaryExpenseToman = discretionaryExpense,
            topSpendingCategoriesFa = topSpendingCategories,
            recentPaidLast30DaysToman = recentPaidTotal,
            recentPaidInstallmentsCount = recentPaidInstallments.size,
            currentMonthDebtDueToman = currentMonthDebtDue,
            totalDebtRemainingToman = totalDebtRemaining,
            dtiPercent = dtiPct,
            overdueCount = overdueCount,
            overdueAmountToman = overdueAmount,
            dueNext7DaysCount = dueNext7Count,
            dueNext7DaysAmountToman = dueNext7Amount,
            highestInterestDebtTitleFa = highestAprDebt?.obligation?.title,
            highestInterestAnnualRatePercent = highestAprDebt?.effectiveAnnualRatePercent ?: 0.0,
            highestInterestRemainingToman = highestAprDebt?.remainingBalanceToman ?: 0L,
            savingsGoalTitleFa = preferences.monthlySavingsGoalTitle,
            savingsProgressPercent = savingsPct,
            activeDebtCount = activeDebtCount,
            preferredAutoPayDay = if (preferences.autoPaymentScheduleEnabled) {
                preferences.preferredAutoPayDayOfMonth
            } else {
                0
            },
            profileFingerprint = fingerprint
        )
    }

    fun buildGeminiPrompt(
        snapshot: SpendingAndDebtSnapshot,
        currencyUnit: CurrencyUnit
    ): String {
        fun fmt(toman: Long): String = PersianCurrencyEngine.formatAmount(toman, currencyUnit)
        val topCategoriesText = if (snapshot.topSpendingCategoriesFa.isEmpty()) {
            "ثبت‌نشده (برآورد هزینه زندگی ${snapshot.livingExpensePercent}% درآمد)"
        } else {
            snapshot.topSpendingCategoriesFa.joinToString(" | ")
        }
        val highestAprText = if (snapshot.highestInterestDebtTitleFa != null) {
            "${snapshot.highestInterestDebtTitleFa} با نرخ سود سالانه حدود ${"%.1f".format(java.util.Locale.US, snapshot.highestInterestAnnualRatePercent)}% و مانده ${fmt(snapshot.highestInterestRemainingToman)}"
        } else {
            "بدهی فعال با سود بالا وجود ندارد"
        }
        val autoPayText = if (snapshot.preferredAutoPayDay in 1..30) {
            "روز ${snapshot.preferredAutoPayDay} هر ماه شمسی"
        } else {
            "تنظیم‌نشده"
        }

        return buildString {
            appendLine("تو مشاور هوشمند مالی شخصی در اپلیکیشن فارسی «ترازینو» هستی.")
            appendLine("بر اساس داده‌های واقعی مخارج اخیر و پروفایل بدهی کاربر در تاریخ ${snapshot.todayJalali} (${snapshot.monthNameFa})، یک تحلیل دقیق از الگوی خرج‌کرد و یک «نکته مالی روزانه» کاملاً شخصی‌سازی‌شده، کاربردی و غیرکلیشه‌ای به زبان فارسی روان و محترمانه تولید کن.")
            appendLine()
            appendLine("داده‌های پروفایل مالی امروز کاربر (واحد نمایش: ${currencyUnit.labelFa}):")
            appendLine("- درآمد ماهانه: ${fmt(snapshot.monthlyIncomeToman)}")
            appendLine("- مجموع مخارج دوره‌ای غیرقسطی ماهانه: ${fmt(snapshot.totalRecurringNonLoanExpenseToman)} (ضروری: ${fmt(snapshot.essentialExpenseToman)} | اختیاری: ${fmt(snapshot.discretionaryExpenseToman)})")
            appendLine("- بیشترین سرفصل‌های هزینه اخیر: $topCategoriesText")
            appendLine("- پرداخت‌های ثبت‌شده در ۳۰ روز اخیر: ${fmt(snapshot.recentPaidLast30DaysToman)} (${snapshot.recentPaidInstallmentsCount} قسط)")
            appendLine("- مجموع اقساط سررسید این ماه: ${fmt(snapshot.currentMonthDebtDueToman)} (نسبت قسط به درآمد DTI: ${snapshot.dtiPercent}%)")
            appendLine("- کل بدهی مانده فعلی: ${fmt(snapshot.totalDebtRemainingToman)} در ${snapshot.activeDebtCount} پرونده فعال")
            appendLine("- اقساط معوقه: ${snapshot.overdueCount} مورد به مبلغ ${fmt(snapshot.overdueAmountToman)}")
            appendLine("- اقساط پیش‌رو در ۷ روز آینده: ${snapshot.dueNext7DaysCount} مورد به مبلغ ${fmt(snapshot.dueNext7DaysAmountToman)}")
            appendLine("- پرهزینه‌ترین بدهی از نظر نرخ سود: $highestAprText")
            appendLine("- پیشرفت هدف پس‌انداز ماهانه («${snapshot.savingsGoalTitleFa}»): ${snapshot.savingsProgressPercent}%")
            appendLine("- روز منتخب زمان‌بند خودکار پرداخت قسط: $autoPayText")
            appendLine()
            appendLine("قواعد الزامی پاسخ:")
            appendLine("1. تمام متن‌ها به فارسی طبیعی، موجز، دقیق و بدون ایموجی اضافه باشند و ارقام را با واحد ${currencyUnit.labelFa} بیان کن.")
            appendLine("2. به اعداد واقعی همین کاربر (مثل نسبت DTI، سرفصل مخارج اختیاری/ضروری، بدهی معوق یا گران‌ترین وام) مستقیماً اشاره کن و توصیه کلیشه‌ای نده.")
            appendLine("3. فیلد urgencyTone را دقیقاً یکی از سه مقدار PROTECTIVE (اگر معوقه یا DTI بالای ۴۵% است)، OPTIMIZATION (اگر فرصت کاهش مخارج اختیاری یا سود وام هست)، یا MOMENTUM (اگر روند تسویه و پس‌انداز مطلوب است) برگردان.")
            appendLine("4. خروجی را دقیقاً مطابق اسکیمای JSON تعریف‌شده برگردان.")
        }
    }

    fun parseUrgencyTone(raw: String?): TipUrgencyTone {
        return when (raw?.trim()?.uppercase()) {
            "PROTECTIVE" -> TipUrgencyTone.PROTECTIVE
            "MOMENTUM" -> TipUrgencyTone.MOMENTUM
            else -> TipUrgencyTone.OPTIMIZATION
        }
    }

    fun isCacheValidForToday(
        cached: DailyFinancialTipResult?,
        todayJalali: String
    ): Boolean {
        return cached != null &&
            cached.generatedDateJalali == todayJalali &&
            cached.headlineFa.isNotBlank() &&
            cached.dailyTipFa.isNotBlank()
    }

    private fun escape(raw: String): String {
        return raw.replace("|", " ").replace("\n", " ").trim()
    }

    fun serializeCachedTip(tip: DailyFinancialTipResult): String {
        return listOf(
            escape(tip.headlineFa),
            escape(tip.spendingPatternInsightFa),
            escape(tip.dailyTipFa),
            escape(tip.concreteActionStepFa),
            escape(tip.focusBadgeFa),
            escape(tip.estimatedImpactFa),
            tip.urgencyTone.name,
            escape(tip.generatedDateJalali),
            escape(tip.profileFingerprint)
        ).joinToString(separator = "|")
    }

    fun deserializeCachedTip(raw: String?): DailyFinancialTipResult? {
        if (raw.isNullOrBlank()) return null
        val parts = raw.split("|")
        if (parts.size < 9) return null
        val headline = parts[0].trim()
        val tip = parts[2].trim()
        if (headline.isBlank() || tip.isBlank()) return null
        return DailyFinancialTipResult(
            headlineFa = headline,
            spendingPatternInsightFa = parts[1].trim(),
            dailyTipFa = tip,
            concreteActionStepFa = parts[3].trim(),
            focusBadgeFa = parts[4].trim().ifBlank { "تحلیل روزانه" },
            estimatedImpactFa = parts[5].trim(),
            urgencyTone = parseUrgencyTone(parts[6]),
            generatedDateJalali = parts[7].trim(),
            profileFingerprint = parts[8].trim()
        )
    }

    private fun formatTomanBrief(amountToman: Long): String {
        return PersianCurrencyEngine.formatAmount(amountToman, CurrencyUnit.TOMAN)
    }
}
