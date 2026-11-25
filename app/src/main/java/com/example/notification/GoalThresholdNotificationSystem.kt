package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.core.CurrencyUnit
import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.domain.RecurringCashflowHealthEngine
import com.example.domain.RecurringCashflowType
import kotlin.math.abs
import kotlin.math.roundToInt

enum class GoalThresholdAlertKind(
    val labelFa: String,
    val badgeColorHex: Long
) {
    SAVINGS_GOAL_THRESHOLD_REACHED(
        labelFa = "عبور از آستانه هدف پس‌انداز ماهانه",
        badgeColorHex = 0xFF35B8AB
    ),
    EXPENSE_CAP_THRESHOLD_EXCEEDED(
        labelFa = "عبور از آستانه سقف هزینه ماهانه",
        badgeColorHex = 0xFFD96B6B
    )
}

data class GoalThresholdAlertItem(
    val kind: GoalThresholdAlertKind,
    val titleFa: String,
    val messageFa: String,
    val currentAmountToman: Long,
    val goalLimitAmountToman: Long,
    val actualPercentage: Int,
    val thresholdPercentage: Int,
    val isCritical: Boolean
)

data class GoalThresholdEvaluationSummary(
    val todayJalali: String,
    val shamsiMonthNameFa: String,
    val savingsThresholdPercent: Int,
    val expenseThresholdPercent: Int,
    val savingsGoalTitleFa: String,
    val savingsCurrentToman: Long,
    val savingsTargetToman: Long,
    val savingsProgressPercent: Int,
    val isSavingsThresholdExceeded: Boolean,
    val expenseCurrentToman: Long,
    val expenseBudgetCapToman: Long,
    val expenseUsagePercent: Int,
    val isExpenseThresholdExceeded: Boolean,
    val triggeredAlerts: List<GoalThresholdAlertItem>,
    val notificationTitleFa: String,
    val notificationShortTextFa: String,
    val expandedLinesFa: List<String>
) {
    val shouldNotify: Boolean
        get() = triggeredAlerts.isNotEmpty()
}

/**
 * Pure domain evaluator that checks whether the user has reached or exceeded a configured
 * percentage threshold (e.g., 80% or 90%) of their defined monthly savings goal or
 * monthly expense budget goal.
 */
object GoalThresholdNotificationEvaluator {

    val SUPPORTED_THRESHOLD_PERCENTS = listOf(75, 80, 90, 100)

    fun evaluateThresholdAlerts(
        savingsGoalTitleFa: String,
        savingsCurrentToman: Long,
        savingsTargetToman: Long,
        expenseCurrentToman: Long,
        expenseBudgetCapToman: Long,
        savingsThresholdPercent: Int = 80,
        expenseThresholdPercent: Int = 80,
        todayJalali: String = JalaliCalendarEngine.todayJalali(),
        currencyUnit: CurrencyUnit = CurrencyUnit.TOMAN,
        privacyMasked: Boolean = false
    ): GoalThresholdEvaluationSummary {
        val parsedToday = JalaliCalendarEngine.parseJalali(todayJalali)
        val monthNameFa = parsedToday.monthNameFa

        val safeSavingsThreshold = savingsThresholdPercent.coerceIn(50, 100)
        val safeExpenseThreshold = expenseThresholdPercent.coerceIn(50, 100)

        val safeSavingsCurrent = savingsCurrentToman.coerceAtLeast(0L)
        val safeSavingsTarget = savingsTargetToman.coerceAtLeast(0L)
        val savingsPct = if (safeSavingsTarget > 0L) {
            ((safeSavingsCurrent.toDouble() / safeSavingsTarget.toDouble()) * 100.0)
                .roundToInt()
                .coerceIn(0, 999)
        } else {
            0
        }

        val safeExpenseCurrent = expenseCurrentToman.coerceAtLeast(0L)
        val safeExpenseCap = expenseBudgetCapToman.coerceAtLeast(0L)
        val expensePct = if (safeExpenseCap > 0L) {
            ((safeExpenseCurrent.toDouble() / safeExpenseCap.toDouble()) * 100.0)
                .roundToInt()
                .coerceIn(0, 999)
        } else {
            0
        }

        val isSavingsExceeded = safeSavingsTarget > 0L && savingsPct >= safeSavingsThreshold
        val isExpenseExceeded = safeExpenseCap > 0L && expensePct >= safeExpenseThreshold

        val alerts = mutableListOf<GoalThresholdAlertItem>()

        if (isExpenseExceeded) {
            val formattedCurrent = PersianCurrencyEngine.formatAmount(
                amountToman = safeExpenseCurrent,
                unit = currencyUnit,
                includeUnit = true,
                privacyMasked = privacyMasked
            )
            val formattedCap = PersianCurrencyEngine.formatAmount(
                amountToman = safeExpenseCap,
                unit = currencyUnit,
                includeUnit = true,
                privacyMasked = privacyMasked
            )
            val diff = safeExpenseCap - safeExpenseCurrent
            val tailNote = if (diff >= 0L) {
                val remFmt = PersianCurrencyEngine.formatAmount(
                    amountToman = diff,
                    unit = currencyUnit,
                    includeUnit = true,
                    privacyMasked = privacyMasked
                )
                "ظرفیت باقی‌مانده تا سقف بودجه: $remFmt"
            } else {
                val overFmt = PersianCurrencyEngine.formatAmount(
                    amountToman = abs(diff),
                    unit = currencyUnit,
                    includeUnit = true,
                    privacyMasked = privacyMasked
                )
                "عبور از سقف بودجه ماهانه به میزان $overFmt"
            }

            alerts += GoalThresholdAlertItem(
                kind = GoalThresholdAlertKind.EXPENSE_CAP_THRESHOLD_EXCEEDED,
                titleFa = "هشدار بودجه هزینه $monthNameFa (${PersianCurrencyEngine.toPersianDigits(expensePct.toString())}٪ از سقف)",
                messageFa = "هزینه‌های ماهانه شما ($formattedCurrent) از آستانه ${
                    PersianCurrencyEngine.toPersianDigits(safeExpenseThreshold.toString())
                }٪ سقف هدف ($formattedCap) عبور کرده است. $tailNote",
                currentAmountToman = safeExpenseCurrent,
                goalLimitAmountToman = safeExpenseCap,
                actualPercentage = expensePct,
                thresholdPercentage = safeExpenseThreshold,
                isCritical = true
            )
        }

        if (isSavingsExceeded) {
            val cleanTitle = savingsGoalTitleFa.ifBlank { "پس‌انداز ماهانه" }
            val formattedSaved = PersianCurrencyEngine.formatAmount(
                amountToman = safeSavingsCurrent,
                unit = currencyUnit,
                includeUnit = true,
                privacyMasked = privacyMasked
            )
            val formattedTarget = PersianCurrencyEngine.formatAmount(
                amountToman = safeSavingsTarget,
                unit = currencyUnit,
                includeUnit = true,
                privacyMasked = privacyMasked
            )
            val remToTarget = (safeSavingsTarget - safeSavingsCurrent).coerceAtLeast(0L)
            val tailNote = if (remToTarget == 0L) {
                "هدف پس‌انداز ماه $monthNameFa به طور کامل محقق شد ✨"
            } else {
                val remFmt = PersianCurrencyEngine.formatAmount(
                    amountToman = remToTarget,
                    unit = currencyUnit,
                    includeUnit = true,
                    privacyMasked = privacyMasked
                )
                "تنها $remFmt تا تحقق ۱۰۰٪ هدف باقی مانده است."
            }

            alerts += GoalThresholdAlertItem(
                kind = GoalThresholdAlertKind.SAVINGS_GOAL_THRESHOLD_REACHED,
                titleFa = "تحقق ${PersianCurrencyEngine.toPersianDigits(savingsPct.toString())}٪ از هدف «$cleanTitle»",
                messageFa = "ذخیره شما ($formattedSaved) از مرز ${
                    PersianCurrencyEngine.toPersianDigits(safeSavingsThreshold.toString())
                }٪ هدف ماهانه ($formattedTarget) عبور کرد! $tailNote",
                currentAmountToman = safeSavingsCurrent,
                goalLimitAmountToman = safeSavingsTarget,
                actualPercentage = savingsPct,
                thresholdPercentage = safeSavingsThreshold,
                isCritical = false
            )
        }

        if (alerts.isEmpty()) {
            return GoalThresholdEvaluationSummary(
                todayJalali = todayJalali,
                shamsiMonthNameFa = monthNameFa,
                savingsThresholdPercent = safeSavingsThreshold,
                expenseThresholdPercent = safeExpenseThreshold,
                savingsGoalTitleFa = savingsGoalTitleFa,
                savingsCurrentToman = safeSavingsCurrent,
                savingsTargetToman = safeSavingsTarget,
                savingsProgressPercent = savingsPct,
                isSavingsThresholdExceeded = false,
                expenseCurrentToman = safeExpenseCurrent,
                expenseBudgetCapToman = safeExpenseCap,
                expenseUsagePercent = expensePct,
                isExpenseThresholdExceeded = false,
                triggeredAlerts = emptyList(),
                notificationTitleFa = "پایش خودکار آستانه پس‌انداز و هزینه ($monthNameFa)",
                notificationShortTextFa = "پس‌انداز: ${
                    PersianCurrencyEngine.toPersianDigits(savingsPct.toString())
                }٪ (آستانه ${
                    PersianCurrencyEngine.toPersianDigits(safeSavingsThreshold.toString())
                }٪) • هزینه: ${
                    PersianCurrencyEngine.toPersianDigits(expensePct.toString())
                }٪ (آستانه ${
                    PersianCurrencyEngine.toPersianDigits(safeExpenseThreshold.toString())
                }٪)",
                expandedLinesFa = emptyList()
            )
        }

        val notifTitle = if (alerts.size == 1) {
            alerts.first().titleFa
        } else {
            "هشدار هوشمند اهداف مالی $monthNameFa (${PersianCurrencyEngine.toPersianDigits(alerts.size.toString())} مورد)"
        }
        val notifShort = alerts.first().messageFa
        val expandedLines = alerts.map { "• ${it.titleFa}: ${it.messageFa}" }

        return GoalThresholdEvaluationSummary(
            todayJalali = todayJalali,
            shamsiMonthNameFa = monthNameFa,
            savingsThresholdPercent = safeSavingsThreshold,
            expenseThresholdPercent = safeExpenseThreshold,
            savingsGoalTitleFa = savingsGoalTitleFa,
            savingsCurrentToman = safeSavingsCurrent,
            savingsTargetToman = safeSavingsTarget,
            savingsProgressPercent = savingsPct,
            isSavingsThresholdExceeded = isSavingsExceeded,
            expenseCurrentToman = safeExpenseCurrent,
            expenseBudgetCapToman = safeExpenseCap,
            expenseUsagePercent = expensePct,
            isExpenseThresholdExceeded = isExpenseExceeded,
            triggeredAlerts = alerts,
            notificationTitleFa = notifTitle,
            notificationShortTextFa = notifShort,
            expandedLinesFa = expandedLines
        )
    }
}

/**
 * Manages user-configurable goal threshold percentages (e.g., 80% or 90%),
 * evaluates live savings and expense totals from offline storage, and posts
 * automated local Android notifications when thresholds are crossed.
 */
object GoalThresholdNotificationManager {

    const val CHANNEL_ID = "tarazino_goal_threshold_alerts"
    const val CHANNEL_NAME_FA = "هشدار آستانه هدف پس‌انداز و سقف هزینه"
    const val CHANNEL_DESC_FA = "اعلان خودکار هنگام عبور از درصد تعیین‌شده (۸۰٪ یا ۹۰٪) هدف پس‌انداز یا سقف هزینه‌های ماهانه"
    const val NOTIFICATION_ID_GOAL_THRESHOLD = 140302

    private const val PREFS_NAME = "tarazino_prefs_v1"
    private const val KEY_AUTO_THRESHOLD_ENABLED = "goal_threshold_auto_notifications_enabled"
    private const val KEY_SAVINGS_THRESHOLD_PCT = "goal_savings_threshold_percent"
    private const val KEY_EXPENSE_THRESHOLD_PCT = "goal_expense_threshold_percent"
    private const val KEY_RECURRING_CASHFLOW_ITEMS_V1 = "recurring_non_loan_cashflow_items_v1"

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isAutoThresholdNotificationEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_THRESHOLD_ENABLED, true)
    }

    fun setAutoThresholdNotificationEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_THRESHOLD_ENABLED, enabled).apply()
    }

    fun getSavingsThresholdPercent(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_SAVINGS_THRESHOLD_PCT, 80).coerceIn(50, 100)
    }

    fun setSavingsThresholdPercent(context: Context, percent: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_SAVINGS_THRESHOLD_PCT, percent.coerceIn(50, 100)).apply()
    }

    fun getExpenseThresholdPercent(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_EXPENSE_THRESHOLD_PCT, 80).coerceIn(50, 100)
    }

    fun setExpenseThresholdPercent(context: Context, percent: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_EXPENSE_THRESHOLD_PCT, percent.coerceIn(50, 100)).apply()
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME_FA,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC_FA
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun evaluateFromPreferences(context: Context): GoalThresholdEvaluationSummary {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savingsTitle = prefs.getString("monthly_savings_goal_title", "هدف پس‌انداز ماهانه")
            ?: "هدف پس‌انداز ماهانه"
        val savingsTarget = prefs.getLong("monthly_savings_target_toman", 0L)
        val savingsSaved = prefs.getLong("monthly_savings_saved_toman", 0L)

        val monthlyIncome = prefs.getLong("monthly_income_toman", 0L)
        val livingPercent = prefs.getInt("living_expense_percent", 50).coerceIn(20, 80)
        val expenseBudgetCap = (monthlyIncome * livingPercent) / 100L

        val serializedItems = prefs.getString(KEY_RECURRING_CASHFLOW_ITEMS_V1, null)
        val recurringItems = RecurringCashflowHealthEngine.deserializeItems(serializedItems)
        val activeNonLoanExpenses = recurringItems
            .filter { it.isActive && it.type == RecurringCashflowType.EXPENSE }
            .sumOf { it.monthlyAmountToman }

        val currencyName = prefs.getString("currency_unit", CurrencyUnit.TOMAN.name) ?: CurrencyUnit.TOMAN.name
        val currencyUnit = runCatching { CurrencyUnit.valueOf(currencyName) }.getOrDefault(CurrencyUnit.TOMAN)
        val privacyMasked = prefs.getBoolean("privacy_mask_enabled", false)

        val savingsThreshold = getSavingsThresholdPercent(context)
        val expenseThreshold = getExpenseThresholdPercent(context)

        return GoalThresholdNotificationEvaluator.evaluateThresholdAlerts(
            savingsGoalTitleFa = savingsTitle,
            savingsCurrentToman = savingsSaved,
            savingsTargetToman = savingsTarget,
            expenseCurrentToman = activeNonLoanExpenses,
            expenseBudgetCapToman = expenseBudgetCap,
            savingsThresholdPercent = savingsThreshold,
            expenseThresholdPercent = expenseThreshold,
            todayJalali = JalaliCalendarEngine.todayJalali(),
            currencyUnit = currencyUnit,
            privacyMasked = privacyMasked
        )
    }

    fun evaluateAndNotifyIfThresholdCrossed(
        context: Context,
        forceEvenIfBelowThreshold: Boolean = false
    ): Boolean {
        ensureNotificationChannel(context)
        if (!forceEvenIfBelowThreshold && !isAutoThresholdNotificationEnabled(context)) {
            return false
        }
        val summary = evaluateFromPreferences(context)
        return postThresholdNotification(
            context = context,
            summary = summary,
            forceEvenIfBelowThreshold = forceEvenIfBelowThreshold
        )
    }

    fun postThresholdNotification(
        context: Context,
        summary: GoalThresholdEvaluationSummary,
        forceEvenIfBelowThreshold: Boolean = false
    ): Boolean {
        ensureNotificationChannel(context)
        if (!hasNotificationPermission(context)) return false
        if (!summary.shouldNotify && !forceEvenIfBelowThreshold) return false

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val contentIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_GOAL_THRESHOLD,
            launchIntent,
            pendingFlags
        )

        val bigText = if (summary.expandedLinesFa.isNotEmpty()) {
            summary.expandedLinesFa.joinToString(separator = "\n")
        } else {
            summary.notificationShortTextFa
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(summary.notificationTitleFa)
            .setContentText(summary.notificationShortTextFa)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(summary.notificationTitleFa)
                    .bigText(bigText)
                    .setSummaryText("پایش اهداف مالی • ${summary.shamsiMonthNameFa}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)

        return try {
            NotificationManagerCompat.from(context).notify(
                NOTIFICATION_ID_GOAL_THRESHOLD,
                builder.build()
            )
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
