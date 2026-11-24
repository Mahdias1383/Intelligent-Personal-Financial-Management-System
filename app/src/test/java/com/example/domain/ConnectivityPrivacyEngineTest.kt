package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectivityPrivacyEngineTest {

    private fun sampleSnapshot(): SpendingAndDebtSnapshot = SpendingAndDebtSnapshot(
        todayJalali = "1404/02/15",
        monthNameFa = "اردیبهشت",
        monthlyIncomeToman = 45_000_000L,
        livingExpensePercent = 48,
        totalRecurringNonLoanExpenseToman = 14_500_000L,
        essentialExpenseToman = 10_000_000L,
        discretionaryExpenseToman = 4_500_000L,
        topSpendingCategoriesFa = listOf(
            "اجاره خانه سعادت‌آباد (ضروری): ۱۲٬۰۰۰٬۰۰۰ تومان",
            "کافه و رستوران نیاوران (اختیاری): ۲٬۵۰۰٬۰۰۰ تومان"
        ),
        recentPaidLast30DaysToman = 8_200_000L,
        recentPaidInstallmentsCount = 3,
        currentMonthDebtDueToman = 9_500_000L,
        totalDebtRemainingToman = 185_000_000L,
        dtiPercent = 36,
        overdueCount = 1,
        overdueAmountToman = 2_400_000L,
        dueNext7DaysCount = 2,
        dueNext7DaysAmountToman = 4_100_000L,
        highestInterestDebtTitleFa = "وام خرید خودرو بانک ملت شعبه ونک",
        highestInterestAnnualRatePercent = 23.0,
        highestInterestRemainingToman = 95_000_000L,
        savingsGoalTitleFa = "سفر کیش تابستان ۱۴۰۴",
        savingsProgressPercent = 62,
        activeDebtCount = 4,
        preferredAutoPayDay = 5,
        profileFingerprint = "fp_test_01"
    )

    @Test
    fun defaultOfflinePolicy_disablesAllOnlineFeaturesAndDefaultsToStrictOffline() {
        val policy = ConnectivityPrivacyEngine.defaultOfflinePolicy()

        assertFalse(policy.masterOnlineAccessEnabled)
        assertFalse(policy.aiFinancialTipsEnabled)
        assertFalse(policy.cloudSyncEnabled)
        assertFalse(policy.autoRefreshAiTipOnLaunch)
        assertTrue(policy.wifiOnlyForOnlineFeatures)
        assertTrue(policy.anonymizeAiPayloads)
        assertEquals(3, policy.dailyAiRequestLimit)
        assertEquals(5, policy.cloudSyncMinChangesThreshold)
        assertTrue(ConnectivityPrivacyEngine.isFullyOffline(policy))
        assertFalse(ConnectivityPrivacyEngine.effectiveAiTipsEnabled(policy))
        assertFalse(ConnectivityPrivacyEngine.effectiveCloudSyncEnabled(policy))
    }

    @Test
    fun evaluateAiTipAccess_enforcesStrictPermissionAndThresholdPrecedence() {
        val today = "1404/02/15"

        // 1. Default policy blocked by master offline kill-switch
        val defaultDecision = ConnectivityPrivacyEngine.evaluateAiTipAccess(
            policy = ConnectivityPrivacyEngine.defaultOfflinePolicy(),
            isNetworkAvailable = true,
            isWifiConnected = true,
            todayJalali = today
        )
        assertTrue(defaultDecision is OnlineFeatureGateDecision.Blocked)
        assertEquals(
            OnlineGateBlockReason.MASTER_OFFLINE_LOCKED,
            (defaultDecision as OnlineFeatureGateDecision.Blocked).reason
        )

        // 2. Master enabled, AI tips still disabled
        val masterOnly = ConnectivityPrivacyPolicy(
            masterOnlineAccessEnabled = true,
            aiFinancialTipsEnabled = false
        )
        val featureBlocked = ConnectivityPrivacyEngine.evaluateAiTipAccess(
            policy = masterOnly,
            isNetworkAvailable = true,
            isWifiConnected = true,
            todayJalali = today
        )
        assertEquals(
            OnlineGateBlockReason.FEATURE_DISABLED,
            (featureBlocked as OnlineFeatureGateDecision.Blocked).reason
        )

        // 3. Enabled, but no network connectivity
        val aiEnabledWifiOnly = masterOnly.copy(
            aiFinancialTipsEnabled = true,
            wifiOnlyForOnlineFeatures = true,
            dailyAiRequestLimit = 2
        )
        val noNet = ConnectivityPrivacyEngine.evaluateAiTipAccess(
            policy = aiEnabledWifiOnly,
            isNetworkAvailable = false,
            isWifiConnected = false,
            todayJalali = today
        )
        assertEquals(
            OnlineGateBlockReason.NO_NETWORK,
            (noNet as OnlineFeatureGateDecision.Blocked).reason
        )

        // 4. Cellular network while Wi-Fi-only guardrail is active
        val wifiRequired = ConnectivityPrivacyEngine.evaluateAiTipAccess(
            policy = aiEnabledWifiOnly,
            isNetworkAvailable = true,
            isWifiConnected = false,
            todayJalali = today
        )
        assertEquals(
            OnlineGateBlockReason.WIFI_ONLY_REQUIRED,
            (wifiRequired as OnlineFeatureGateDecision.Blocked).reason
        )

        // 5. Daily quota exhausted
        val quotaExhausted = aiEnabledWifiOnly.copy(
            aiRequestsUsedToday = 2,
            aiRequestsCounterDateJalali = today
        )
        val quotaBlocked = ConnectivityPrivacyEngine.evaluateAiTipAccess(
            policy = quotaExhausted,
            isNetworkAvailable = true,
            isWifiConnected = true,
            todayJalali = today
        )
        assertEquals(
            OnlineGateBlockReason.DAILY_QUOTA_EXCEEDED,
            (quotaBlocked as OnlineFeatureGateDecision.Blocked).reason
        )

        // 6. All gates satisfied -> Allowed with exact remaining quota
        val allowed = ConnectivityPrivacyEngine.evaluateAiTipAccess(
            policy = aiEnabledWifiOnly.copy(aiRequestsUsedToday = 1, aiRequestsCounterDateJalali = today),
            isNetworkAvailable = true,
            isWifiConnected = true,
            todayJalali = today
        )
        assertTrue(allowed is OnlineFeatureGateDecision.Allowed)
        assertEquals(1, (allowed as OnlineFeatureGateDecision.Allowed).remainingQuotaToday)
    }

    @Test
    fun recordAiRequestUsage_incrementsSameDayAndResetsOnNewJalaliDay() {
        val day1 = "1404/02/15"
        val day2 = "1404/02/16"
        val base = ConnectivityPrivacyPolicy(
            masterOnlineAccessEnabled = true,
            aiFinancialTipsEnabled = true,
            dailyAiRequestLimit = 3
        )

        val afterFirst = ConnectivityPrivacyEngine.recordAiRequestUsage(base, todayJalali = day1)
        assertEquals(1, ConnectivityPrivacyEngine.usedDailyAiRequests(afterFirst, day1))
        assertEquals(2, ConnectivityPrivacyEngine.remainingDailyAiQuota(afterFirst, day1))

        val afterSecond = ConnectivityPrivacyEngine.recordAiRequestUsage(afterFirst, todayJalali = day1)
        assertEquals(2, ConnectivityPrivacyEngine.usedDailyAiRequests(afterSecond, day1))
        assertEquals(1, ConnectivityPrivacyEngine.remainingDailyAiQuota(afterSecond, day1))

        // Next day resets usage to 0 before recording
        assertEquals(0, ConnectivityPrivacyEngine.usedDailyAiRequests(afterSecond, day2))
        assertEquals(3, ConnectivityPrivacyEngine.remainingDailyAiQuota(afterSecond, day2))
        val firstOnDay2 = ConnectivityPrivacyEngine.recordAiRequestUsage(afterSecond, todayJalali = day2)
        assertEquals(1, firstOnDay2.aiRequestsUsedToday)
        assertEquals(day2, firstOnDay2.aiRequestsCounterDateJalali)
    }

    @Test
    fun anonymizeSnapshotForPrivacy_stripsPersonalNamesWhilePreservingMetricsAndTags() {
        val original = sampleSnapshot()
        val sanitized = ConnectivityPrivacyEngine.anonymizeSnapshotForPrivacy(original)

        // Personal titles stripped
        assertFalse(sanitized.highestInterestDebtTitleFa!!.contains("بانک ملت"))
        assertFalse(sanitized.savingsGoalTitleFa.contains("کیش"))
        assertTrue(sanitized.topSpendingCategoriesFa.none { it.contains("سعادت‌آباد") || it.contains("نیاوران") })

        // Essential/discretionary tags and exact numbers preserved
        assertTrue(sanitized.topSpendingCategoriesFa[0].contains("ضروری"))
        assertTrue(sanitized.topSpendingCategoriesFa[0].contains("۱۲٬۰۰۰٬۰۰۰"))
        assertTrue(sanitized.topSpendingCategoriesFa[1].contains("اختیاری"))
        assertEquals(original.monthlyIncomeToman, sanitized.monthlyIncomeToman)
        assertEquals(original.totalDebtRemainingToman, sanitized.totalDebtRemainingToman)
        assertEquals(original.dtiPercent, sanitized.dtiPercent)
        assertEquals(original.highestInterestAnnualRatePercent, sanitized.highestInterestAnnualRatePercent, 0.001)
    }

    @Test
    fun computePrivacyPosture_scoresStrictOfflineHighestAndReflectsHardeningFlags() {
        val strictOffline = ConnectivityPrivacyEngine.computePrivacyPosture(
            policy = ConnectivityPrivacyEngine.defaultOfflinePolicy(),
            hasPinLock = true,
            privacyMaskEnabled = false
        )
        assertEquals(PrivacyPostureLevel.STRICT_OFFLINE, strictOffline.level)
        assertEquals(100, strictOffline.score)
        assertEquals(0, strictOffline.activeOnlineFeaturesCount)

        val permissiveOnline = ConnectivityPrivacyEngine.computePrivacyPosture(
            policy = ConnectivityPrivacyPolicy(
                masterOnlineAccessEnabled = true,
                aiFinancialTipsEnabled = true,
                cloudSyncEnabled = true,
                wifiOnlyForOnlineFeatures = false,
                anonymizeAiPayloads = false,
                autoRefreshAiTipOnLaunch = true
            ),
            hasPinLock = false,
            privacyMaskEnabled = false
        )
        assertEquals(PrivacyPostureLevel.SELECTIVE_ONLINE, permissiveOnline.level)
        assertEquals(2, permissiveOnline.activeOnlineFeaturesCount)
        assertTrue(permissiveOnline.score < strictOffline.score)

        val hardenedOnline = ConnectivityPrivacyEngine.computePrivacyPosture(
            policy = ConnectivityPrivacyPolicy(
                masterOnlineAccessEnabled = true,
                aiFinancialTipsEnabled = true,
                cloudSyncEnabled = false,
                wifiOnlyForOnlineFeatures = true,
                anonymizeAiPayloads = true,
                autoRefreshAiTipOnLaunch = false,
                preventScreenshotsAndRecents = true,
                hideAmountsInWidget = true
            ),
            hasPinLock = true,
            privacyMaskEnabled = true
        )
        assertEquals(PrivacyPostureLevel.CONTROLLED_PRIVACY, hardenedOnline.level)
        assertTrue(hardenedOnline.score > permissiveOnline.score)
    }

    @Test
    fun serializeAndDeserializePolicy_preservesAllFieldsAndFallsBackSafely() {
        val custom = ConnectivityPrivacyPolicy(
            masterOnlineAccessEnabled = true,
            aiFinancialTipsEnabled = true,
            cloudSyncEnabled = true,
            wifiOnlyForOnlineFeatures = false,
            anonymizeAiPayloads = false,
            autoRefreshAiTipOnLaunch = true,
            dailyAiRequestLimit = 10,
            aiRequestsUsedToday = 4,
            aiRequestsCounterDateJalali = "1404/02/15",
            cloudSyncMinChangesThreshold = 20,
            preventScreenshotsAndRecents = true,
            hideAmountsInWidget = true
        )

        val encoded = ConnectivityPrivacyEngine.serializePolicy(custom)
        val decoded = ConnectivityPrivacyEngine.deserializePolicy(encoded)
        assertEquals(custom, decoded)

        assertEquals(
            ConnectivityPrivacyEngine.defaultOfflinePolicy(),
            ConnectivityPrivacyEngine.deserializePolicy(null)
        )
        assertEquals(
            ConnectivityPrivacyEngine.defaultOfflinePolicy(),
            ConnectivityPrivacyEngine.deserializePolicy("   ")
        )
    }
}
