package com.example.domain

import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import java.net.URI
import java.util.Locale

enum class OnlineGateBlockReason {
    MASTER_OFFLINE_LOCKED,
    FEATURE_DISABLED,
    MISSING_OR_INVALID_INPUTS,
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
 * User-selectable execution backend for the optional online AI Daily Financial Tip feature.
 */
enum class AiProviderMode(
    val labelFa: String,
    val shortLabelFa: String,
    val subtitleFa: String,
    val defaultModelName: String
) {
    FIREBASE_AI_BUILTIN(
        labelFa = "سرویس داخلی آماده",
        shortLabelFa = "داخلی آماده",
        subtitleFa = "متصل به زیرساخت داخلی برنامه بدون نیاز به وارد کردن کلید API",
        defaultModelName = "gemini-3.8-flash"
    ),
    CUSTOM_GEMINI_API(
        labelFa = "کلید اختصاصی Gemini",
        shortLabelFa = "کلید Gemini",
        subtitleFa = "اتصال مستقیم با کلید API شخصی شما و مدل انتخابی Gemini",
        defaultModelName = "gemini-2.5-flash"
    ),
    OPENAI_COMPATIBLE_ENDPOINT(
        labelFa = "سرور / API سازگار با OpenAI",
        shortLabelFa = "سرور اختصاصی",
        subtitleFa = "اتصال به آدرس HTTPS و کلید دسترسی سرویس دلخواه شما",
        defaultModelName = "gpt-4o-mini"
    )
}

/**
 * User-configured inputs for optional online capabilities.
 * Secrets (`customAiApiKey`, `cloudSyncAuthToken`) are stored exclusively in encrypted local
 * Android Keystore storage and never written to plaintext preferences or exported backups.
 */
data class OnlineServiceInputsState(
    val aiProviderMode: AiProviderMode = AiProviderMode.FIREBASE_AI_BUILTIN,
    val customAiEndpointUrl: String = "",
    val customAiModelName: String = "",
    val customAiApiKey: String = "",
    val cloudSyncEndpointUrl: String = "",
    val cloudSyncAuthToken: String = "",
    val cloudSyncWorkspaceLabel: String = "",
    val lastCloudSyncJalali: String = ""
)

data class OnlineInputValidationResult(
    val isValid: Boolean,
    val errorFa: String? = null
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
 * Pure Kotlin domain engine for evaluating offline/online permission gates, validating user-entered
 * online endpoints and credentials, enforcing daily request quotas, Wi-Fi-only thresholds,
 * AI payload anonymization, privacy posture scoring, and serialization.
 */
object ConnectivityPrivacyEngine {

    val AI_DAILY_QUOTA_OPTIONS: List<Int> = listOf(1, 3, 5, 10)
    val CLOUD_SYNC_THRESHOLD_OPTIONS: List<Int> = listOf(1, 5, 10, 20)
    const val DEFAULT_GEMINI_BASE_URL: String = "https://generativelanguage.googleapis.com"

    fun defaultOfflinePolicy(): ConnectivityPrivacyPolicy = ConnectivityPrivacyPolicy()

    fun defaultOnlineInputs(): OnlineServiceInputsState = OnlineServiceInputsState()

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

    /**
     * Validates a user-entered HTTPS endpoint URL:
     * - Must use strict `https://` scheme (rejects `http://`, `file://`, `javascript:`, etc.)
     * - Must contain a valid non-empty public host with no whitespace or embedded credentials
     * - Blocks loopback, private RFC1918, and link-local/cloud metadata addresses
     */
    fun validateHttpsEndpointUrl(rawUrl: String): OnlineInputValidationResult {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) {
            return OnlineInputValidationResult(false, "آدرس سرور نباید خالی باشد.")
        }
        if (trimmed.any { it.isWhitespace() || it.isISOControl() }) {
            return OnlineInputValidationResult(false, "آدرس سرور نباید شامل فاصله یا کاراکتر نامعتبر باشد.")
        }
        val uri = runCatching { URI(trimmed) }.getOrNull()
            ?: return OnlineInputValidationResult(false, "فرمت آدرس سرور معتبر نیست.")

        if (!uri.scheme.equals("https", ignoreCase = true)) {
            return OnlineInputValidationResult(false, "برای حفظ امنیت، آدرس سرور باید با \u2066https://\u2069 شروع شود.")
        }
        if (!uri.userInfo.isNullOrBlank()) {
            return OnlineInputValidationResult(false, "نام کاربری یا رمز عبور نباید داخل آدرس URL قرار گیرد.")
        }
        val host = uri.host?.lowercase(Locale.ROOT)?.trim().orEmpty()
        if (host.isEmpty() || !host.contains('.') || host.startsWith('.') || host.endsWith('.')) {
            return OnlineInputValidationResult(false, "نام دامنه سرور معتبر نیست.")
        }
        if (isDisallowedPrivateOrLocalHost(host)) {
            return OnlineInputValidationResult(false, "استفاده از آدرس‌های محلی یا شبکه خصوصی مجاز نیست.")
        }
        return OnlineInputValidationResult(true)
    }

    private fun isDisallowedPrivateOrLocalHost(host: String): Boolean {
        val normalized = host.removePrefix("[").removeSuffix("]")
        if (
            normalized == "localhost" ||
            normalized.endsWith(".localhost") ||
            normalized.endsWith(".local") ||
            normalized == "0.0.0.0" ||
            normalized == "::1" ||
            normalized.startsWith("fe80:") ||
            normalized.startsWith("fc") ||
            normalized.startsWith("fd")
        ) {
            return true
        }
        val ipv4Parts = normalized.split('.')
        if (ipv4Parts.size == 4 && ipv4Parts.all { it.toIntOrNull() in 0..255 }) {
            val a = ipv4Parts[0].toInt()
            val b = ipv4Parts[1].toInt()
            return when {
                a == 127 -> true // loopback
                a == 10 -> true // RFC1918
                a == 0 -> true
                a == 169 && b == 254 -> true // link-local / metadata
                a == 172 && b in 16..31 -> true // RFC1918
                a == 192 && b == 168 -> true // RFC1918
                a == 100 && b in 64..127 -> true // CGNAT
                else -> false
            }
        }
        return false
    }

    fun validateAiInputs(inputs: OnlineServiceInputsState): OnlineInputValidationResult {
        return when (inputs.aiProviderMode) {
            AiProviderMode.FIREBASE_AI_BUILTIN -> OnlineInputValidationResult(true)

            AiProviderMode.CUSTOM_GEMINI_API -> {
                if (inputs.customAiApiKey.trim().length < 10) {
                    return OnlineInputValidationResult(
                        false,
                        "برای اتصال اختصاصی \u2066Gemini\u2069، وارد کردن کلید \u2066API\u2069 معتبر الزامی است."
                    )
                }
                val endpoint = inputs.customAiEndpointUrl.trim()
                if (endpoint.isNotEmpty()) {
                    val urlCheck = validateHttpsEndpointUrl(endpoint)
                    if (!urlCheck.isValid) return urlCheck
                }
                val model = inputs.customAiModelName.trim()
                if (model.isNotEmpty() && !isValidModelIdentifier(model)) {
                    return OnlineInputValidationResult(
                        false,
                        "نام مدل فقط می‌تواند شامل حروف انگلیسی، عدد، نقطه و خط تیره باشد."
                    )
                }
                OnlineInputValidationResult(true)
            }

            AiProviderMode.OPENAI_COMPATIBLE_ENDPOINT -> {
                val urlCheck = validateHttpsEndpointUrl(inputs.customAiEndpointUrl)
                if (!urlCheck.isValid) return urlCheck
                if (inputs.customAiApiKey.trim().length < 8) {
                    return OnlineInputValidationResult(
                        false,
                        "برای سرور اختصاصی هوش مصنوعی، وارد کردن کلید دسترسی (\u2066API Key\u2069) الزامی است."
                    )
                }
                val model = inputs.customAiModelName.trim()
                if (model.isEmpty() || !isValidModelIdentifier(model)) {
                    return OnlineInputValidationResult(
                        false,
                        "نام مدل هوش مصنوعی (مثلاً \u2066gpt-4o-mini\u2069) را به صورت معتبر وارد کنید."
                    )
                }
                OnlineInputValidationResult(true)
            }
        }
    }

    fun validateCloudSyncInputs(inputs: OnlineServiceInputsState): OnlineInputValidationResult {
        val urlCheck = validateHttpsEndpointUrl(inputs.cloudSyncEndpointUrl)
        if (!urlCheck.isValid) return urlCheck
        if (inputs.cloudSyncAuthToken.trim().length < 8) {
            return OnlineInputValidationResult(
                false,
                "برای همگام‌سازی ابری، وارد کردن توکن احراز هویت معتبر (حداقل ۸ کاراکتر) الزامی است."
            )
        }
        return OnlineInputValidationResult(true)
    }

    private fun isValidModelIdentifier(model: String): Boolean {
        if (model.length > 80) return false
        return model.all { ch ->
            ch in 'a'..'z' || ch in 'A'..'Z' || ch in '0'..'9' ||
                ch == '-' || ch == '_' || ch == '.' || ch == '/' || ch == ':'
        }
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
        todayJalali: String = JalaliCalendarEngine.todayJalali(),
        inputs: OnlineServiceInputsState = defaultOnlineInputs()
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
        val inputValidation = validateAiInputs(inputs)
        if (!inputValidation.isValid) {
            return OnlineFeatureGateDecision.Blocked(
                reason = OnlineGateBlockReason.MISSING_OR_INVALID_INPUTS,
                messageFa = inputValidation.errorFa
                    ?: "ورودی‌های مورد نیاز سرویس هوش مصنوعی در تنظیمات «اتصال و حریم خصوصی» تکمیل نشده است."
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

    fun evaluateCloudSyncAccess(
        policy: ConnectivityPrivacyPolicy,
        inputs: OnlineServiceInputsState,
        isNetworkAvailable: Boolean,
        isWifiConnected: Boolean
    ): OnlineFeatureGateDecision {
        if (!policy.masterOnlineAccessEnabled) {
            return OnlineFeatureGateDecision.Blocked(
                reason = OnlineGateBlockReason.MASTER_OFFLINE_LOCKED,
                messageFa = "حالت کاملاً آفلاین فعال است. برای همگام‌سازی ابری، ابتدا کلید اصلی دسترسی آنلاین را روشن کنید."
            )
        }
        if (!policy.cloudSyncEnabled) {
            return OnlineFeatureGateDecision.Blocked(
                reason = OnlineGateBlockReason.FEATURE_DISABLED,
                messageFa = "قابلیت همگام‌سازی ابری غیرفعال است. ابتدا این بخش را فعال کرده و آدرس سرور و توکن خود را وارد کنید."
            )
        }
        val inputValidation = validateCloudSyncInputs(inputs)
        if (!inputValidation.isValid) {
            return OnlineFeatureGateDecision.Blocked(
                reason = OnlineGateBlockReason.MISSING_OR_INVALID_INPUTS,
                messageFa = inputValidation.errorFa
                    ?: "آدرس سرور و توکن همگام‌سازی ابری به درستی تنظیم نشده است."
            )
        }
        if (!isNetworkAvailable) {
            return OnlineFeatureGateDecision.Blocked(
                reason = OnlineGateBlockReason.NO_NETWORK,
                messageFa = "اتصال اینترنت برقرار نیست. داده‌های شما به صورت کامل در حافظه محلی ذخیره شده‌اند."
            )
        }
        if (policy.wifiOnlyForOnlineFeatures && !isWifiConnected) {
            return OnlineFeatureGateDecision.Blocked(
                reason = OnlineGateBlockReason.WIFI_ONLY_REQUIRED,
                messageFa = "محدودیت «فقط روی وای‌فای» فعال است. برای ارسال پشتیبان ابری به وای‌فای متصل شوید یا این شرط را تغییر دهید."
            )
        }
        return OnlineFeatureGateDecision.Allowed(remainingQuotaToday = 1)
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
