package com.example.domain

import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine

enum class OnlineGateBlockReason {
    MASTER_OFFLINE_LOCKED,
    FEATURE_DISABLED,
    NO_NETWORK,
    WIFI_ONLY_REQUIRED,
    DAILY_QUOTA_EXCEEDED
}

sealed interface OnlineFeatureGateDecision {
    data class Allowed(val remainingQuotaToday: Int) : OnlineFeatureGateDecision
    data class Blocked(
        val reason: OnlineGateBlockReason,
        val messageFa: String
    ) : OnlineFeatureGateDecision
}

enum class PrivacyPostureLevel(val badgeFa: String) {
    STRICT_OFFLINE("آفلاین مطلق و امن"),
    CONTROLLED_PRIVACY("حریم خصوصی بالا"),
    SELECTIVE_ONLINE("اتصال انتخابی فعال")
}

data class PrivacyPostureReport(
    val score: Int,
    val level: PrivacyPostureLevel,
    val headlineFa: String,
    val subtitleFa: String,
    val activeOnlineFeaturesCount: Int,
    val totalOnlineFeaturesCount: Int = 2
)

/**
 * Centralized user-controlled Connectivity & Privacy policy.
 * Defaults strictly to offline-first ("Disabled" for all online features) so zero network traffic
 * or external data transmission occurs unless the user explicitly opts in.
 */
data class ConnectivityPrivacyPolicy(
    val masterOnlineAccessEnabled: Boolean = false,
    val aiFinancialTipsEnabled: Boolean = false,
    val cloudSyncEnabled: Boolean = false,
    val wifiOnlyForOnlineFeatures: Boolean = true,
    val anonymizeAiPayloads: Boolean = true,
    val autoRefreshAiTipOnLaunch: Boolean = false,
    val dailyAiRequestLimit: Int = 3,
    val aiRequestsUsedToday: Int = 0,
    val aiRequestsCounterDateJalali: String = "",
    val cloudSyncMinChangesThreshold: Int = 5,
    val preventScreenshotsAndRecents: Boolean = false,
    val hideAmountsInWidget: Boolean = false
)

/**
 * Pure Kotlin domain engine for evaluating offline/online permission gates, daily request quotas,
 * Wi-Fi-only thresholds, AI payload anonymization, privacy posture scoring, and serialization.
 */
object ConnectivityPrivacyEngine {

    val AI_DAILY_QUOTA_OPTIONS: List<Int> = listOf(1, 3, 5, 10)
    val CLOUD_SYNC_THRESHOLD_OPTIONS: List<Int> = listOf(1, 5, 10, 20)

    fun defaultOfflinePolicy(): ConnectivityPrivacyPolicy = ConnectivityPrivacyPolicy()

    fun isFullyOffline(policy: ConnectivityPrivacyPolicy): Boolean {
        return !policy.masterOnlineAccessEnabled ||
            (!policy.aiFinancialTipsEnabled && !policy.cloudSyncEnabled)
    }

    fun effectiveAiTipsEnabled(policy: ConnectivityPrivacyPolicy): Boolean {
        return policy.masterOnlineAccessEnabled && policy.aiFinancialTipsEnabled
    }

    fun effectiveCloudSyncEnabled(policy: ConnectivityPrivacyPolicy): Boolean {
        return policy.masterOnlineAccessEnabled && policy.cloudSyncEnabled
    }

    fun usedDailyAiRequests(
        policy: ConnectivityPrivacyPolicy,
        todayJalali: String = JalaliCalendarEngine.todayJalali()
    ): Int {
        return if (policy.aiRequestsCounterDateJalali == todayJalali) {
            policy.aiRequestsUsedToday.coerceAtLeast(0)
        } else {
            0
        }
    }

    fun remainingDailyAiQuota(
        policy: ConnectivityPrivacyPolicy,
        todayJalali: String = JalaliCalendarEngine.todayJalali()
    ): Int {
        val limit = policy.dailyAiRequestLimit.coerceIn(1, 50)
        val used = usedDailyAiRequests(policy, todayJalali)
        return (limit - used).coerceAtLeast(0)
    }

    fun evaluateAiTipAccess(
        policy: ConnectivityPrivacyPolicy,
        isNetworkAvailable: Boolean,
        isWifiConnected: Boolean,
        todayJalali: String = JalaliCalendarEngine.todayJalali()
    ): OnlineFeatureGateDecision {
        if (!policy.masterOnlineAccessEnabled) {
            return OnlineFeatureGateDecision.Blocked(
                reason = OnlineGateBlockReason.MASTER_OFFLINE_LOCKED,
                messageFa = "حالت کاملاً آفلاین فعال است. برای دریافت نکته مالی هوشمند، دسترسی آنلاین را در تنظیمات «اتصال و حریم خصوصی» فعال کنید."
            )
        }
        if (!policy.aiFinancialTipsEnabled) {
            return OnlineFeatureGateDecision.Blocked(
                reason = OnlineGateBlockReason.FEATURE_DISABLED,
                messageFa = "سرویس «نکته مالی روزانه هوشمند» غیرفعال است. برای استفاده، مجوز این قابلیت را در تنظیمات «اتصال و حریم خصوصی» روشن کنید."
            )
        }
        if (!isNetworkAvailable) {
            return OnlineFeatureGateDecision.Blocked(
                reason = OnlineGateBlockReason.NO_NETWORK,
                messageFa = "دستگاه به اینترنت متصل نیست. داده‌های شما همچنان به صورت کامل و آفلاین روی گوشی در دسترس است."
            )
        }
        if (policy.wifiOnlyForOnlineFeatures && !isWifiConnected) {
            return OnlineFeatureGateDecision.Blocked(
                reason = OnlineGateBlockReason.WIFI_ONLY_REQUIRED,
                messageFa = "محدودیت «فقط روی وای‌فای» فعال است. برای جلوگیری از مصرف اینترنت همراه، به وای‌فای متصل شوید یا این شرط را تغییر دهید."
            )
        }
        val remaining = remainingDailyAiQuota(policy, todayJalali)
        if (remaining <= 0) {
            val limitFa = PersianCurrencyEngine.toPersianDigits(policy.dailyAiRequestLimit.toString())
            return OnlineFeatureGateDecision.Blocked(
                reason = OnlineGateBlockReason.DAILY_QUOTA_EXCEEDED,
                messageFa = "سقف مجاز روزانه درخواست هوشمند ($limitFa بار در روز) تکمیل شده است. می‌توانید این آستانه را در تنظیمات «اتصال و حریم خصوصی» افزایش دهید."
            )
        }
        return OnlineFeatureGateDecision.Allowed(remainingQuotaToday = remaining)
    }

    fun recordAiRequestUsage(
        policy: ConnectivityPrivacyPolicy,
        todayJalali: String = JalaliCalendarEngine.todayJalali()
    ): ConnectivityPrivacyPolicy {
        val currentUsed = usedDailyAiRequests(policy, todayJalali)
        return policy.copy(
            aiRequestsUsedToday = currentUsed + 1,
            aiRequestsCounterDateJalali = todayJalali
        )
    }

    /**
     * Strips personally identifiable names/titles (specific loan names, counterparties, savings title,
     * custom expense titles) from the snapshot before sending to the cloud AI model while preserving
     * exact aggregated financial ratios and figures.
     */
    fun anonymizeSnapshotForPrivacy(snapshot: SpendingAndDebtSnapshot): SpendingAndDebtSnapshot {
        val anonymizedCategories = snapshot.topSpendingCategoriesFa.mapIndexed { index, raw ->
            val rankFa = PersianCurrencyEngine.toPersianDigits((index + 1).toString())
            val essentialTag = when {
                raw.contains("ضروری") -> "ضروری"
                raw.contains("اختیاری") -> "اختیاری"
                else -> "جاری"
            }
            val amountSuffix = raw.substringAfter(":", "").trim()
            if (amountSuffix.isNotEmpty()) {
                "سرفصل هزینه $rankFa ($essentialTag): $amountSuffix"
            } else {
                "سرفصل هزینه $rankFa ($essentialTag)"
            }
        }

        return snapshot.copy(
            topSpendingCategoriesFa = anonymizedCategories,
            highestInterestDebtTitleFa = snapshot.highestInterestDebtTitleFa?.let { "پرهزینه‌ترین پرونده بدهی" },
            savingsGoalTitleFa = "هدف پس‌انداز ماهانه"
        )
    }

    fun computePrivacyPosture(
        policy: ConnectivityPrivacyPolicy,
        hasPinLock: Boolean,
        privacyMaskEnabled: Boolean
    ): PrivacyPostureReport {
        val activeOnline = listOf(
            effectiveAiTipsEnabled(policy),
            effectiveCloudSyncEnabled(policy)
        ).count { it }

        var score = 100
        if (policy.masterOnlineAccessEnabled) {
            if (policy.aiFinancialTipsEnabled) score -= 12
            if (policy.cloudSyncEnabled) score -= 10
            if (!policy.anonymizeAiPayloads && policy.aiFinancialTipsEnabled) score -= 14
            if (!policy.wifiOnlyForOnlineFeatures && activeOnline > 0) score -= 6
            if (policy.autoRefreshAiTipOnLaunch && policy.aiFinancialTipsEnabled) score -= 5
        }
        if (!hasPinLock) score -= 8
        if (policy.preventScreenshotsAndRecents) score += 5
        if (policy.hideAmountsInWidget) score += 4
        if (privacyMaskEnabled) score += 4

        val clampedScore = score.coerceIn(25, 100)
        val level = when {
            isFullyOffline(policy) -> PrivacyPostureLevel.STRICT_OFFLINE
            clampedScore >= 78 -> PrivacyPostureLevel.CONTROLLED_PRIVACY
            else -> PrivacyPostureLevel.SELECTIVE_ONLINE
        }

        val headline = when (level) {
            PrivacyPostureLevel.STRICT_OFFLINE -> "حالت ۱۰۰٪ آفلاین و خصوصی فعال است"
            PrivacyPostureLevel.CONTROLLED_PRIVACY -> "اتصال آنلاین کنترل‌شده با حریم خصوصی بالا"
            PrivacyPostureLevel.SELECTIVE_ONLINE -> "برخی قابلیت‌های آنلاین با مجوز شما فعال است"
        }

        val subtitle = when (level) {
            PrivacyPostureLevel.STRICT_OFFLINE ->
                "هیچ داده‌ای از دستگاه خارج نمی‌شود و تمام محاسبات و ثبت‌ها کاملاً محلی انجام می‌شود."
            PrivacyPostureLevel.CONTROLLED_PRIVACY ->
                "تنها قابلیت‌های تاییدشده توسط شما با رعایت آستانه‌ها و ناشناس‌سازی داده اجرا می‌شوند."
            PrivacyPostureLevel.SELECTIVE_ONLINE ->
                "برای افزایش امنیت، ناشناس‌سازی داده، محدودیت وای‌فای و قفل پین پیشنهاد می‌شود."
        }

        return PrivacyPostureReport(
            score = clampedScore,
            level = level,
            headlineFa = headline,
            subtitleFa = subtitle,
            activeOnlineFeaturesCount = activeOnline
        )
    }

    fun serializePolicy(policy: ConnectivityPrivacyPolicy): String {
        return listOf(
            "master=${if (policy.masterOnlineAccessEnabled) 1 else 0}",
            "ai=${if (policy.aiFinancialTipsEnabled) 1 else 0}",
            "cloud=${if (policy.cloudSyncEnabled) 1 else 0}",
            "wifi=${if (policy.wifiOnlyForOnlineFeatures) 1 else 0}",
            "anon=${if (policy.anonymizeAiPayloads) 1 else 0}",
            "autolaunch=${if (policy.autoRefreshAiTipOnLaunch) 1 else 0}",
            "ailimit=${policy.dailyAiRequestLimit.coerceIn(1, 50)}",
            "aiused=${policy.aiRequestsUsedToday.coerceAtLeast(0)}",
            "aidate=${policy.aiRequestsCounterDateJalali.replace(";", "").replace("=", "")}",
            "syncthresh=${policy.cloudSyncMinChangesThreshold.coerceIn(1, 100)}",
            "secure=${if (policy.preventScreenshotsAndRecents) 1 else 0}",
            "widgetmask=${if (policy.hideAmountsInWidget) 1 else 0}"
        ).joinToString(";")
    }

    fun deserializePolicy(raw: String?): ConnectivityPrivacyPolicy {
        if (raw.isNullOrBlank()) return defaultOfflinePolicy()
        val map = mutableMapOf<String, String>()
        raw.split(";").forEach { entry ->
            val idx = entry.indexOf('=')
            if (idx > 0) {
                map[entry.substring(0, idx).trim()] = entry.substring(idx + 1).trim()
            }
        }
        fun boolVal(key: String, default: Boolean): Boolean {
            return when (map[key]) {
                "1", "true" -> true
                "0", "false" -> false
                else -> default
            }
        }
        fun intVal(key: String, default: Int, min: Int, max: Int): Int {
            return map[key]?.toIntOrNull()?.coerceIn(min, max) ?: default
        }
        return ConnectivityPrivacyPolicy(
            masterOnlineAccessEnabled = boolVal("master", false),
            aiFinancialTipsEnabled = boolVal("ai", false),
            cloudSyncEnabled = boolVal("cloud", false),
            wifiOnlyForOnlineFeatures = boolVal("wifi", true),
            anonymizeAiPayloads = boolVal("anon", true),
            autoRefreshAiTipOnLaunch = boolVal("autolaunch", false),
            dailyAiRequestLimit = intVal("ailimit", 3, 1, 50),
            aiRequestsUsedToday = intVal("aiused", 0, 0, 1000),
            aiRequestsCounterDateJalali = map["aidate"] ?: "",
            cloudSyncMinChangesThreshold = intVal("syncthresh", 5, 1, 100),
            preventScreenshotsAndRecents = boolVal("secure", false),
            hideAmountsInWidget = boolVal("widgetmask", false)
        )
    }
}
