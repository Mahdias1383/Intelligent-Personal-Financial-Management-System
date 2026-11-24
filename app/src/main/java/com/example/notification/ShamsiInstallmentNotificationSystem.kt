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
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.core.CurrencyUnit
import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.data.CounterpartyEntity
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.ObligationEntity
import com.example.data.TarazinoDatabase
import java.util.concurrent.TimeUnit

data class ShamsiDueAlertLine(
    val installmentId: Long,
    val obligationId: Long,
    val title: String,
    val counterpartyName: String,
    val dueDateJalali: String,
    val dueDateVerboseFa: String,
    val diffDays: Int,
    val relativeStatusFa: String,
    val remainingAmountToman: Long
)

data class ShamsiDueNotificationSummary(
    val todayJalali: String,
    val todayVerboseFa: String,
    val alertAdvanceDays: Int,
    val overdueCount: Int,
    val dueTodayCount: Int,
    val upcomingCount: Int,
    val totalAlertCount: Int,
    val totalAlertAmountToman: Long,
    val notificationTitle: String,
    val notificationShortText: String,
    val expandedLines: List<String>,
    val items: List<ShamsiDueAlertLine>
) {
    val shouldNotify: Boolean
        get() = totalAlertCount > 0
}

/**
 * Pure domain evaluator that uses [JalaliCalendarEngine] (Shamsi calendar utility)
 * to calculate overdue, due-today, and upcoming installment due dates and format
 * localized Persian notification payloads.
 */
object ShamsiInstallmentReminderEvaluator {

    fun buildShamsiDueNotificationSummary(
        installments: List<InstallmentEntity>,
        obligations: List<ObligationEntity>,
        counterparties: List<CounterpartyEntity>,
        todayJalali: String = JalaliCalendarEngine.todayJalali(),
        alertAdvanceDays: Int = 7,
        currencyUnit: CurrencyUnit = CurrencyUnit.TOMAN,
        privacyMasked: Boolean = false
    ): ShamsiDueNotificationSummary {
        val safeAdvanceDays = alertAdvanceDays.coerceIn(1, 30)
        val obligationMap = obligations.associateBy { it.id }
        val counterpartyMap = counterparties.associateBy { it.id }

        val alertItems = mutableListOf<ShamsiDueAlertLine>()

        for (inst in installments) {
            if (inst.status == InstallmentStatus.PAID.name) continue
            val remaining = inst.remainingToman
            if (remaining <= 0L) continue

            val obligation = obligationMap[inst.obligationId] ?: continue
            // Focus on debt/payable obligations (not receivables)
            if (obligation.categoryEnum.isReceivable) continue

            val dueDateEffective = inst.snoozedUntilJalali.takeIf { it.isNotBlank() } ?: inst.dueDateJalali
            val diffDays = JalaliCalendarEngine.diffJalaliDays(todayJalali, dueDateEffective)

            if (diffDays <= safeAdvanceDays) {
                val counterpartyName = counterpartyMap[obligation.counterpartyId]?.name ?: "نامشخص"
                val relativeStatusFa = JalaliCalendarEngine.formatRelativeDueStatusFa(todayJalali, dueDateEffective)
                val dueVerbose = JalaliCalendarEngine.formatJalaliShortText(dueDateEffective)

                alertItems += ShamsiDueAlertLine(
                    installmentId = inst.id,
                    obligationId = obligation.id,
                    title = "${obligation.title} (قسط ${PersianCurrencyEngine.toPersianDigits(inst.installmentNumber.toString())})",
                    counterpartyName = counterpartyName,
                    dueDateJalali = dueDateEffective,
                    dueDateVerboseFa = dueVerbose,
                    diffDays = diffDays,
                    relativeStatusFa = relativeStatusFa,
                    remainingAmountToman = remaining
                )
            }
        }

        val sorted = alertItems.sortedWith(
            compareBy<ShamsiDueAlertLine> { it.diffDays }
                .thenByDescending { it.remainingAmountToman }
        )

        val overdueCount = sorted.count { it.diffDays < 0 }
        val dueTodayCount = sorted.count { it.diffDays == 0 }
        val upcomingCount = sorted.count { it.diffDays > 0 }
        val totalCount = sorted.size
        val totalAmount = sorted.sumOf { it.remainingAmountToman }
        val todayVerboseFa = JalaliCalendarEngine.formatJalaliVerbose(todayJalali)

        if (totalCount == 0) {
            return ShamsiDueNotificationSummary(
                todayJalali = todayJalali,
                todayVerboseFa = todayVerboseFa,
                alertAdvanceDays = safeAdvanceDays,
                overdueCount = 0,
                dueTodayCount = 0,
                upcomingCount = 0,
                totalAlertCount = 0,
                totalAlertAmountToman = 0L,
                notificationTitle = "وضعیت سررسید اقساط شمسی ($todayVerboseFa)",
                notificationShortText = "هیچ قسط معوق یا سررسید نزدیکی تا ${PersianCurrencyEngine.toPersianDigits(safeAdvanceDays.toString())} روز آینده وجود ندارد.",
                expandedLines = emptyList(),
                items = emptyList()
            )
        }

        val formattedTotalAmount = PersianCurrencyEngine.formatAmount(
            amountToman = totalAmount,
            unit = currencyUnit,
            includeUnit = true,
            privacyMasked = privacyMasked
        )

        val titleParts = mutableListOf<String>()
        if (overdueCount > 0) {
            titleParts += "${PersianCurrencyEngine.toPersianDigits(overdueCount.toString())} قسط معوق"
        }
        if (dueTodayCount > 0) {
            titleParts += "${PersianCurrencyEngine.toPersianDigits(dueTodayCount.toString())} سررسید امروز"
        }
        if (upcomingCount > 0) {
            titleParts += "${PersianCurrencyEngine.toPersianDigits(upcomingCount.toString())} قسط پیش‌رو"
        }

        val notificationTitle = "یادآور سررسید شمسی: ${titleParts.joinToString(" و ")}"
        val nearest = sorted.first()

        val notificationShortText =
            "نزدیک‌ترین: ${nearest.title} (${nearest.relativeStatusFa} - ${nearest.dueDateVerboseFa}) • مجموع: $formattedTotalAmount"

        val expandedLines = sorted.take(5).map { item ->
            val itemAmount = PersianCurrencyEngine.formatAmount(
                amountToman = item.remainingAmountToman,
                unit = currencyUnit,
                includeUnit = true,
                privacyMasked = privacyMasked
            )
            "• ${item.title} | ${item.dueDateVerboseFa} (${item.relativeStatusFa}) | $itemAmount"
        }

        return ShamsiDueNotificationSummary(
            todayJalali = todayJalali,
            todayVerboseFa = todayVerboseFa,
            alertAdvanceDays = safeAdvanceDays,
            overdueCount = overdueCount,
            dueTodayCount = dueTodayCount,
            upcomingCount = upcomingCount,
            totalAlertCount = totalCount,
            totalAlertAmountToman = totalAmount,
            notificationTitle = notificationTitle,
            notificationShortText = notificationShortText,
            expandedLines = expandedLines,
            items = sorted
        )
    }
}

/**
 * Manages the Android NotificationChannel, runtime POST_NOTIFICATIONS permission check,
 * notification dispatch, and Jetpack WorkManager background scheduling.
 */
object ShamsiInstallmentNotificationManager {

    const val CHANNEL_ID = "tarazino_shamsi_due_alerts"
    const val CHANNEL_NAME_FA = "یادآور سررسید اقساط شمسی"
    const val CHANNEL_DESC_FA = "اعلان خودکار سررسید اقساط، وام‌ها و تعهدات مالی بر اساس تقویم جلالی (شمسی)"
    const val PERIODIC_WORK_NAME = "tarazino_shamsi_due_periodic_worker"
    const val IMMEDIATE_WORK_NAME = "tarazino_shamsi_due_immediate_worker"
    const val NOTIFICATION_ID_DUE_SUMMARY = 140301

    private const val PREFS_NAME = "tarazino_prefs_v1"
    private const val KEY_NOTIFICATION_ENABLED = "shamsi_bg_notifications_enabled"
    private const val KEY_LAST_CHECK_JALALI = "shamsi_bg_last_check_jalali"

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

    fun isBackgroundReminderEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true)
    }

    fun setBackgroundReminderEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, enabled).apply()
        if (enabled) {
            ensureChannelAndSchedulePeriodicWork(context)
        } else {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
        }
    }

    fun getLastCheckJalali(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_CHECK_JALALI, null) ?: JalaliCalendarEngine.todayJalali()
    }

    fun recordLastCheckJalali(context: Context, todayJalali: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_CHECK_JALALI, todayJalali).apply()
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
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

    fun ensureChannelAndSchedulePeriodicWork(context: Context) {
        ensureNotificationChannel(context)
        if (!isBackgroundReminderEnabled(context)) return

        val periodicRequest = PeriodicWorkRequestBuilder<ShamsiInstallmentReminderWorker>(
            12,
            TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }

    fun triggerImmediateWorkerCheck(context: Context) {
        ensureNotificationChannel(context)
        val oneTimeRequest = OneTimeWorkRequestBuilder<ShamsiInstallmentReminderWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
    }

    suspend fun evaluateFromDatabase(context: Context): ShamsiDueNotificationSummary {
        val db = TarazinoDatabase.getInstance(context)
        val dao = db.tarazinoDao()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val alertAdvanceDays = prefs.getInt("alert_advance_days", 7).coerceIn(1, 30)
        val currencyName = prefs.getString("currency_unit", CurrencyUnit.TOMAN.name) ?: CurrencyUnit.TOMAN.name
        val currencyUnit = runCatching { CurrencyUnit.valueOf(currencyName) }.getOrDefault(CurrencyUnit.TOMAN)
        val privacyMasked = prefs.getBoolean("privacy_mask_enabled", false)
        val todayJalali = JalaliCalendarEngine.todayJalali()

        val installments = dao.getAllInstallmentsOnce()
        val obligations = dao.getAllObligationsOnce()
        val counterparties = dao.getAllCounterpartiesOnce()

        val summary = ShamsiInstallmentReminderEvaluator.buildShamsiDueNotificationSummary(
            installments = installments,
            obligations = obligations,
            counterparties = counterparties,
            todayJalali = todayJalali,
            alertAdvanceDays = alertAdvanceDays,
            currencyUnit = currencyUnit,
            privacyMasked = privacyMasked
        )
        recordLastCheckJalali(context, todayJalali)
        return summary
    }

    fun postSummaryNotification(
        context: Context,
        summary: ShamsiDueNotificationSummary,
        forceEvenIfEmpty: Boolean = false
    ): Boolean {
        ensureNotificationChannel(context)
        if (!hasNotificationPermission(context)) return false
        if (!summary.shouldNotify && !forceEvenIfEmpty) return false

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val contentIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_DUE_SUMMARY,
            launchIntent,
            pendingIntentFlags
        )

        val bigText = if (summary.expandedLines.isNotEmpty()) {
            buildString {
                appendLine(summary.notificationShortText)
                summary.expandedLines.forEach { line ->
                    appendLine(line)
                }
            }.trim()
        } else {
            summary.notificationShortText
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(summary.notificationTitle)
            .setContentText(summary.notificationShortText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(summary.notificationTitle)
                    .bigText(bigText)
                    .setSummaryText("تقویم شمسی • ${summary.todayVerboseFa}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)

        return try {
            NotificationManagerCompat.from(context).notify(
                NOTIFICATION_ID_DUE_SUMMARY,
                builder.build()
            )
            true
        } catch (_: SecurityException) {
            false
        }
    }
}

/**
 * Jetpack WorkManager background worker that evaluates upcoming and overdue
 * installment due dates calculated by [JalaliCalendarEngine] and triggers local notifications.
 */
class ShamsiInstallmentReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            if (!ShamsiInstallmentNotificationManager.isBackgroundReminderEnabled(applicationContext)) {
                return Result.success()
            }
            val summary = ShamsiInstallmentNotificationManager.evaluateFromDatabase(applicationContext)
            if (summary.shouldNotify) {
                ShamsiInstallmentNotificationManager.postSummaryNotification(
                    context = applicationContext,
                    summary = summary,
                    forceEvenIfEmpty = false
                )
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
