package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.domain.AiProviderMode
import com.example.domain.ConnectivityPrivacyEngine
import com.example.domain.ConnectivityPrivacyPolicy
import com.example.domain.OnlineServiceInputsState
import com.example.domain.PrivacyPostureLevel
import com.example.ui.components.TarazinoAutoSizeText
import com.example.ui.components.TarazinoGlassCard
import com.example.ui.components.TarazinoLuminousProgressBar
import com.example.ui.components.TarazinoSectionHeader
import com.example.ui.components.TarazinoStatusPill
import com.example.ui.components.tarazinoAmbientBackdrop
import com.example.ui.theme.LocalTarazinoColors

/**
 * Centralized "Connectivity & Privacy" (اتصال، سرویس‌های آنلاین و حریم خصوصی) full-screen settings vault.
 * All online capabilities default strictly to Disabled for a 100% offline-first experience.
 * Users who explicitly opt in can toggle each internet-dependent section and enter the required
 * credentials, endpoints, and thresholds directly here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectivityPrivacyScreen(
    policy: ConnectivityPrivacyPolicy,
    onlineInputs: OnlineServiceInputsState,
    hasPinLock: Boolean,
    privacyMaskEnabled: Boolean,
    isCloudSyncRunning: Boolean,
    onUpdatePolicy: ((ConnectivityPrivacyPolicy) -> ConnectivityPrivacyPolicy) -> Unit,
    onSaveOnlineAiInputs: (AiProviderMode, String, String, String) -> Unit,
    onSaveCloudSyncInputs: (String, String, String) -> Unit,
    onTriggerCloudBackupSyncNow: () -> Unit,
    onClearStoredOnlineSecrets: () -> Unit,
    onTogglePrivacyMask: () -> Unit,
    onClearOnlineAiCache: () -> Unit,
    onResetStrictOffline: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val ext = LocalTarazinoColors.current
    val today = remember { JalaliCalendarEngine.todayJalali() }
    val posture = remember(policy, hasPinLock, privacyMaskEnabled) {
        ConnectivityPrivacyEngine.computePrivacyPosture(
            policy = policy,
            hasPinLock = hasPinLock,
            privacyMaskEnabled = privacyMaskEnabled
        )
    }
    val isFullyOffline = remember(policy) { ConnectivityPrivacyEngine.isFullyOffline(policy) }
    val aiEffectiveEnabled = remember(policy) { ConnectivityPrivacyEngine.effectiveAiTipsEnabled(policy) }
    val cloudEffectiveEnabled = remember(policy) { ConnectivityPrivacyEngine.effectiveCloudSyncEnabled(policy) }
    val usedAiToday = remember(policy, today) { ConnectivityPrivacyEngine.usedDailyAiRequests(policy, today) }
    val remainingAiToday = remember(policy, today) { ConnectivityPrivacyEngine.remainingDailyAiQuota(policy, today) }
    val hasStoredSecrets = onlineInputs.customAiApiKey.isNotBlank() || onlineInputs.cloudSyncAuthToken.isNotBlank()

    // Local draft states for AI and Cloud Sync user-entered inputs
    var draftAiMode by remember(onlineInputs.aiProviderMode) { mutableStateOf(onlineInputs.aiProviderMode) }
    var draftAiEndpoint by remember(onlineInputs.customAiEndpointUrl) { mutableStateOf(onlineInputs.customAiEndpointUrl) }
    var draftAiModel by remember(onlineInputs.customAiModelName) { mutableStateOf(onlineInputs.customAiModelName) }
    var draftAiKey by remember(onlineInputs.customAiApiKey) { mutableStateOf(onlineInputs.customAiApiKey) }
    var showAiKey by remember { mutableStateOf(false) }

    var draftCloudEndpoint by remember(onlineInputs.cloudSyncEndpointUrl) { mutableStateOf(onlineInputs.cloudSyncEndpointUrl) }
    var draftCloudToken by remember(onlineInputs.cloudSyncAuthToken) { mutableStateOf(onlineInputs.cloudSyncAuthToken) }
    var draftCloudWorkspace by remember(onlineInputs.cloudSyncWorkspaceLabel) { mutableStateOf(onlineInputs.cloudSyncWorkspaceLabel) }
    var showCloudToken by remember { mutableStateOf(false) }

    LaunchedEffect(onlineInputs) {
        draftAiMode = onlineInputs.aiProviderMode
        draftAiEndpoint = onlineInputs.customAiEndpointUrl
        draftAiModel = onlineInputs.customAiModelName
        draftAiKey = onlineInputs.customAiApiKey
        draftCloudEndpoint = onlineInputs.cloudSyncEndpointUrl
        draftCloudToken = onlineInputs.cloudSyncAuthToken
        draftCloudWorkspace = onlineInputs.cloudSyncWorkspaceLabel
    }

    val postureAccent = com.example.ui.components.rememberSpringColorAnimatable(
        targetColor = when (posture.level) {
            PrivacyPostureLevel.STRICT_OFFLINE -> ext.positive
            PrivacyPostureLevel.CONTROLLED_PRIVACY -> MaterialTheme.colorScheme.primary
            PrivacyPostureLevel.SELECTIVE_ONLINE -> ext.warning
        }
    ).value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .tarazinoAmbientBackdrop(
                backgroundColor = MaterialTheme.colorScheme.background,
                auroraPrimary = ext.auroraPrimary,
                auroraSecondary = ext.auroraSecondary,
                auroraTertiary = ext.auroraTertiary
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("connectivity_privacy_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازگشت به صفحه قبل"
                            )
                        }
                    },
                    title = {
                        Column {
                            TarazinoAutoSizeText(
                                text = "اتصال و حریم خصوصی",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                minFontSize = 14.sp
                            )
                            Text(
                                text = "تنظیم بخش‌های آنلاین، ورود کلیدها و مجوزهای آفلاین",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        Box(modifier = Modifier.padding(end = 12.dp)) {
                            TarazinoStatusPill(
                                text = if (isFullyOffline) "آفلاین مطلق" else "اتصال انتخابی",
                                accentColor = postureAccent,
                                icon = if (isFullyOffline) Icons.Default.CloudOff else Icons.Default.GppGood
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp)
                        .testTag("connectivity_privacy_screen"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Hero Privacy Posture & Master Offline Kill-Switch Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.15.dp,
                                Brush.linearGradient(
                                    listOf(
                                        postureAccent.copy(alpha = if (ext.isDark) 0.7f else 0.45f),
                                        ext.cardBorderBottom
                                    )
                                )
                            ),
                            shadowElevation = if (ext.isDark) 0.dp else 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("connectivity_privacy_hero_card")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                postureAccent.copy(alpha = if (ext.isDark) 0.16f else 0.08f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(postureAccent.copy(alpha = 0.18f))
                                        ) {
                                            Icon(
                                                imageVector = if (isFullyOffline) Icons.Default.CloudOff else Icons.Default.Shield,
                                                contentDescription = "وضعیت حریم خصوصی",
                                                tint = postureAccent,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            TarazinoAutoSizeText(
                                                text = posture.headlineFa,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                minFontSize = 11.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = posture.subtitleFa,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = postureAccent.copy(alpha = 0.16f),
                                        border = BorderStroke(0.9.dp, postureAccent.copy(alpha = 0.45f))
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = PersianCurrencyEngine.toPersianDigits(posture.score.toString()),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = postureAccent
                                            )
                                            Text(
                                                text = "امتیاز امنیت",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                TarazinoLuminousProgressBar(
                                    progress = posture.score / 100f,
                                    color = postureAccent,
                                    height = 6.dp
                                )

                                // 3-Tile Live Status Summary Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PostureMetricMiniBox(
                                        label = "وضعیت شبکه",
                                        value = if (isFullyOffline) "آفلاین کامل" else "انتخابی",
                                        accent = postureAccent,
                                        modifier = Modifier.weight(1f)
                                    )
                                    PostureMetricMiniBox(
                                        label = "بخش‌های آنلاین",
                                        value = "${PersianCurrencyEngine.toPersianDigits(posture.activeOnlineFeaturesCount.toString())} از ${PersianCurrencyEngine.toPersianDigits(posture.totalOnlineFeaturesCount.toString())}",
                                        accent = if (posture.activeOnlineFeaturesCount == 0) ext.positive else MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    PostureMetricMiniBox(
                                        label = "سهمیه امروز",
                                        value = if (aiEffectiveEnabled) {
                                            "${PersianCurrencyEngine.toPersianDigits(usedAiToday.toString())} از ${PersianCurrencyEngine.toPersianDigits(policy.dailyAiRequestLimit.toString())}"
                                        } else {
                                            "غیرفعال"
                                        },
                                        accent = if (aiEffectiveEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                HorizontalDivider(color = ext.cardBorderTop)

                                // Master Online Kill-Switch (Defaults to FALSE = Full Offline Vault)
                                PrivacySwitchRow(
                                    icon = if (policy.masterOnlineAccessEnabled) Icons.Default.GppGood else Icons.Default.CloudOff,
                                    accentColor = if (policy.masterOnlineAccessEnabled) MaterialTheme.colorScheme.primary else ext.positive,
                                    title = "فعال‌سازی دسترسی بخش‌های نیازمند اینترنت",
                                    subtitle = if (policy.masterOnlineAccessEnabled) {
                                        "فعال — اکنون می‌توانید هر بخش آنلاین را روشن کرده و ورودی‌های مورد نیاز آن را تنظیم کنید."
                                    } else {
                                        "غیرفعال (پیش‌فرض ۱۰۰٪ آفلاین) — تمام بخش‌های نیازمند اینترنت مسدود هستند و هیچ ارتباطی برقرار نمی‌شود."
                                    },
                                    checked = policy.masterOnlineAccessEnabled,
                                    onCheckedChange = { enabled ->
                                        onUpdatePolicy { it.copy(masterOnlineAccessEnabled = enabled) }
                                    },
                                    testTagName = "toggle_master_online_access"
                                )
                            }
                        }
                    }

                    // 2. Online Features Toggles + User-Entered Required Inputs Card
                    item {
                        TarazinoGlassCard(
                            accentBorderColor = if (policy.masterOnlineAccessEnabled) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            } else {
                                null
                            }
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TarazinoSectionHeader(
                                    title = "بخش‌های نیازمند اینترنت و ورودی‌های کاربر",
                                    subtitle = "به صورت پیش‌فرض غیرفعال؛ در صورت نیاز فعال کرده و کلید یا آدرس سرور را ست کنید",
                                    icon = Icons.Default.Key,
                                    accentColor = MaterialTheme.colorScheme.primary
                                )

                                AnimatedVisibility(
                                    visible = !policy.masterOnlineAccessEnabled,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(13.dp),
                                        color = ext.positive.copy(alpha = 0.12f),
                                        border = BorderStroke(0.8.dp, ext.positive.copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "برنامه اکنون در حالت ۱۰۰٪ آفلاین قرار دارد. برای فعال‌سازی و تنظیم ورودی‌های هوش مصنوعی یا همگام‌سازی ابری، ابتدا کلید اصلی بالا را روشن کنید.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ext.positive,
                                            fontWeight = FontWeight.SemiBold,
                                            lineHeight = 18.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                                        )
                                    }
                                }

                                // --- SECTION A: AI Financial Tips Toggle & User Inputs ---
                                PrivacySwitchRow(
                                    icon = Icons.Default.AutoAwesome,
                                    accentColor = MaterialTheme.colorScheme.secondary,
                                    title = "نکته مالی روزانه هوشمند (هوش مصنوعی آنلاین)",
                                    subtitle = if (aiEffectiveEnabled) {
                                        "فعال (${onlineInputs.aiProviderMode.shortLabelFa}) • باقیمانده سهمیه امروز: ${PersianCurrencyEngine.toPersianDigits(remainingAiToday.toString())} درخواست"
                                    } else {
                                        "غیرفعال — هیچ داده‌ای از مخارج یا بدهی به سرویس هوش مصنوعی ارسال نمی‌شود."
                                    },
                                    checked = policy.aiFinancialTipsEnabled,
                                    enabled = policy.masterOnlineAccessEnabled,
                                    onCheckedChange = { enabled ->
                                        onUpdatePolicy { it.copy(aiFinancialTipsEnabled = enabled) }
                                    },
                                    testTagName = "toggle_ai_financial_tips"
                                )

                                AnimatedVisibility(
                                    visible = aiEffectiveEnabled,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    val candidateAiInputs = onlineInputs.copy(
                                        aiProviderMode = draftAiMode,
                                        customAiEndpointUrl = draftAiEndpoint,
                                        customAiModelName = draftAiModel,
                                        customAiApiKey = draftAiKey
                                    )
                                    val aiValidation = ConnectivityPrivacyEngine.validateAiInputs(candidateAiInputs)

                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                                        border = BorderStroke(0.9.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("ai_online_inputs_panel")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "تنظیم منبع و ورودی‌های هوش مصنوعی",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                TarazinoStatusPill(
                                                    text = if (aiValidation.isValid) "آماده اجرا" else "نیاز به تکمیل",
                                                    accentColor = if (aiValidation.isValid) ext.positive else ext.warning
                                                )
                                            }

                                            // 3-way AI provider mode selector
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                AiProviderMode.entries.forEach { mode ->
                                                    val selected = draftAiMode == mode
                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = if (selected) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.surface
                                                        },
                                                        border = BorderStroke(
                                                            width = if (selected) 1.2.dp else 0.8.dp,
                                                            color = if (selected) {
                                                                MaterialTheme.colorScheme.primary
                                                            } else {
                                                                ext.cardBorderTop
                                                            }
                                                        ),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .heightIn(min = 48.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .clickable {
                                                                draftAiMode = mode
                                                                if (draftAiModel.isBlank()) {
                                                                    draftAiModel = mode.defaultModelName
                                                                }
                                                            }
                                                            .testTag("ai_provider_mode_${mode.name.lowercase()}")
                                                    ) {
                                                        Box(
                                                            contentAlignment = Alignment.Center,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                                                        ) {
                                                            TarazinoAutoSizeText(
                                                                text = mode.shortLabelFa,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                                color = if (selected) {
                                                                    MaterialTheme.colorScheme.onPrimary
                                                                } else {
                                                                    MaterialTheme.colorScheme.onSurface
                                                                },
                                                                textAlign = TextAlign.Center,
                                                                maxLines = 1,
                                                                minFontSize = 9.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Text(
                                                text = draftAiMode.subtitleFa,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 17.sp
                                            )

                                            if (draftAiMode != AiProviderMode.FIREBASE_AI_BUILTIN) {
                                                // API Key Input (Encrypted at rest in Android Keystore)
                                                OutlinedTextField(
                                                    value = draftAiKey,
                                                    onValueChange = { draftAiKey = it },
                                                    label = {
                                                        Text(
                                                            text = if (draftAiMode == AiProviderMode.CUSTOM_GEMINI_API) {
                                                                "کلید اختصاصی \u2066Gemini API Key\u2069 *"
                                                            } else {
                                                                "کلید دسترسی (\u2066API Key / Bearer\u2069) *"
                                                            }
                                                        )
                                                    },
                                                    placeholder = {
                                                        Text(
                                                            text = if (draftAiMode == AiProviderMode.CUSTOM_GEMINI_API) {
                                                                "AIza..."
                                                            } else {
                                                                "sk-..."
                                                            },
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                textDirection = TextDirection.Ltr
                                                            )
                                                        )
                                                    },
                                                    singleLine = true,
                                                    visualTransformation = if (showAiKey) {
                                                        VisualTransformation.None
                                                    } else {
                                                        PasswordVisualTransformation()
                                                    },
                                                    trailingIcon = {
                                                        IconButton(
                                                            onClick = { showAiKey = !showAiKey },
                                                            modifier = Modifier.size(48.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = if (showAiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                                contentDescription = if (showAiKey) "مخفی کردن کلید" else "نمایش کلید"
                                                            )
                                                        }
                                                    },
                                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                        textDirection = TextDirection.Ltr
                                                    ),
                                                    shape = RoundedCornerShape(13.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag("input_custom_ai_api_key")
                                                )

                                                // Model Name Input
                                                OutlinedTextField(
                                                    value = draftAiModel,
                                                    onValueChange = { draftAiModel = it },
                                                    label = {
                                                        Text(
                                                            text = if (draftAiMode == AiProviderMode.OPENAI_COMPATIBLE_ENDPOINT) {
                                                                "نام مدل هوش مصنوعی *"
                                                            } else {
                                                                "نام مدل (اختیاری — پیش‌فرض \u2066${draftAiMode.defaultModelName}\u2069)"
                                                            }
                                                        )
                                                    },
                                                    placeholder = {
                                                        Text(
                                                            text = draftAiMode.defaultModelName,
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                textDirection = TextDirection.Ltr
                                                            )
                                                        )
                                                    },
                                                    singleLine = true,
                                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                        textDirection = TextDirection.Ltr
                                                    ),
                                                    shape = RoundedCornerShape(13.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag("input_custom_ai_model")
                                                )

                                                // Endpoint URL Input (Optional for Gemini, Required for OpenAI-compatible)
                                                OutlinedTextField(
                                                    value = draftAiEndpoint,
                                                    onValueChange = { draftAiEndpoint = it },
                                                    label = {
                                                        Text(
                                                            text = if (draftAiMode == AiProviderMode.OPENAI_COMPATIBLE_ENDPOINT) {
                                                                "آدرس کامل سرور (فقط \u2066HTTPS\u2069) *"
                                                            } else {
                                                                "آدرس پایه اختصاصی (اختیاری — فقط \u2066HTTPS\u2069)"
                                                            }
                                                        )
                                                    },
                                                    placeholder = {
                                                        Text(
                                                            text = if (draftAiMode == AiProviderMode.OPENAI_COMPATIBLE_ENDPOINT) {
                                                                "https://api.openai.com/v1/chat/completions"
                                                            } else {
                                                                ConnectivityPrivacyEngine.DEFAULT_GEMINI_BASE_URL
                                                            },
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                textDirection = TextDirection.Ltr
                                                            )
                                                        )
                                                    },
                                                    singleLine = true,
                                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                        textDirection = TextDirection.Ltr
                                                    ),
                                                    shape = RoundedCornerShape(13.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag("input_custom_ai_endpoint")
                                                )
                                            }

                                            if (!aiValidation.isValid && aiValidation.errorFa != null) {
                                                Surface(
                                                    shape = RoundedCornerShape(11.dp),
                                                    color = ext.warning.copy(alpha = 0.14f),
                                                    border = BorderStroke(0.8.dp, ext.warning.copy(alpha = 0.4f)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = aiValidation.errorFa,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = ext.warning,
                                                        fontWeight = FontWeight.SemiBold,
                                                        lineHeight = 17.sp,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        onSaveOnlineAiInputs(
                                                            draftAiMode,
                                                            draftAiEndpoint,
                                                            draftAiModel,
                                                            draftAiKey
                                                        )
                                                    },
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 48.dp)
                                                        .testTag("save_ai_inputs_btn")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Save,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    TarazinoAutoSizeText(
                                                        text = "ذخیره ورودی‌های هوش مصنوعی",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        minFontSize = 10.sp
                                                    )
                                                }
                                            }

                                            OutlinedButton(
                                                onClick = onClearOnlineAiCache,
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 48.dp)
                                                    .testTag("clear_online_ai_cache_btn")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteSweep,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "پاکسازی حافظه موقت نکات آنلاین ذخیره‌شده",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = ext.cardBorderTop.copy(alpha = 0.7f))

                                // --- SECTION B: Cloud Sync Opt-In Toggle & User Server Inputs ---
                                PrivacySwitchRow(
                                    icon = Icons.Default.CloudSync,
                                    accentColor = ext.info,
                                    title = "همگام‌سازی ابری و پشتیبان‌گیری روی سرور شخصی",
                                    subtitle = if (cloudEffectiveEnabled) {
                                        val lastSync = onlineInputs.lastCloudSyncJalali.ifBlank { "هنوز انجام نشده" }
                                        "فعال (آستانه هر ${PersianCurrencyEngine.toPersianDigits(policy.cloudSyncMinChangesThreshold.toString())} تغییر) • آخرین ارسال: $lastSync"
                                    } else {
                                        "غیرفعال — تمامی پرونده‌ها، اقساط و سوابق منحصراً در حافظه داخلی دستگاه نگهداری می‌شوند."
                                    },
                                    checked = policy.cloudSyncEnabled,
                                    enabled = policy.masterOnlineAccessEnabled,
                                    onCheckedChange = { enabled ->
                                        onUpdatePolicy { it.copy(cloudSyncEnabled = enabled) }
                                    },
                                    testTagName = "toggle_cloud_sync"
                                )

                                AnimatedVisibility(
                                    visible = cloudEffectiveEnabled,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    val candidateCloudInputs = onlineInputs.copy(
                                        cloudSyncEndpointUrl = draftCloudEndpoint,
                                        cloudSyncAuthToken = draftCloudToken,
                                        cloudSyncWorkspaceLabel = draftCloudWorkspace
                                    )
                                    val cloudValidation = ConnectivityPrivacyEngine.validateCloudSyncInputs(candidateCloudInputs)

                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                                        border = BorderStroke(0.9.dp, ext.info.copy(alpha = 0.30f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("cloud_sync_inputs_panel")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "ورودی‌های سرور همگام‌سازی ابری",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                TarazinoStatusPill(
                                                    text = if (cloudValidation.isValid) "آماده ارسال" else "نیاز به تنظیم",
                                                    accentColor = if (cloudValidation.isValid) ext.positive else ext.warning
                                                )
                                            }

                                            OutlinedTextField(
                                                value = draftCloudEndpoint,
                                                onValueChange = { draftCloudEndpoint = it },
                                                label = { Text("آدرس سرور / وب‌هوک پشتیبان‌گیری (فقط \u2066HTTPS\u2069) *") },
                                                placeholder = {
                                                    Text(
                                                        text = "https://backup.example.com/api/tarazino/sync",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            textDirection = TextDirection.Ltr
                                                        )
                                                    )
                                                },
                                                singleLine = true,
                                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                    textDirection = TextDirection.Ltr
                                                ),
                                                shape = RoundedCornerShape(13.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("input_cloud_sync_endpoint")
                                            )

                                            OutlinedTextField(
                                                value = draftCloudToken,
                                                onValueChange = { draftCloudToken = it },
                                                label = { Text("توکن احراز هویت (\u2066Bearer Token\u2069) *") },
                                                placeholder = {
                                                    Text(
                                                        text = "توکن امنیتی سرور شخصی شما",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                },
                                                singleLine = true,
                                                visualTransformation = if (showCloudToken) {
                                                    VisualTransformation.None
                                                } else {
                                                    PasswordVisualTransformation()
                                                },
                                                trailingIcon = {
                                                    IconButton(
                                                        onClick = { showCloudToken = !showCloudToken },
                                                        modifier = Modifier.size(48.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (showCloudToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                            contentDescription = if (showCloudToken) "مخفی کردن توکن" else "نمایش توکن"
                                                        )
                                                    }
                                                },
                                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                    textDirection = TextDirection.Ltr
                                                ),
                                                shape = RoundedCornerShape(13.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("input_cloud_sync_token")
                                            )

                                            OutlinedTextField(
                                                value = draftCloudWorkspace,
                                                onValueChange = { draftCloudWorkspace = it },
                                                label = { Text("نام دستگاه یا فضای کاری (اختیاری)") },
                                                placeholder = { Text("مثلاً: گوشی شخصی من") },
                                                singleLine = true,
                                                shape = RoundedCornerShape(13.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("input_cloud_sync_workspace")
                                            )

                                            if (!cloudValidation.isValid && cloudValidation.errorFa != null) {
                                                Surface(
                                                    shape = RoundedCornerShape(11.dp),
                                                    color = ext.warning.copy(alpha = 0.14f),
                                                    border = BorderStroke(0.8.dp, ext.warning.copy(alpha = 0.4f)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = cloudValidation.errorFa,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = ext.warning,
                                                        fontWeight = FontWeight.SemiBold,
                                                        lineHeight = 17.sp,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                                    )
                                                }
                                            } else if (onlineInputs.lastCloudSyncJalali.isNotBlank()) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = ext.positive,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = "آخرین پشتیبان‌گیری موفق: ${PersianCurrencyEngine.toPersianDigits(onlineInputs.lastCloudSyncJalali)}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = ext.positive,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    onSaveCloudSyncInputs(
                                                        draftCloudEndpoint,
                                                        draftCloudToken,
                                                        draftCloudWorkspace
                                                    )
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 48.dp)
                                                    .testTag("save_cloud_sync_inputs_btn")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Save,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                TarazinoAutoSizeText(
                                                    text = "ذخیره ورودی‌های سرور ابری",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    minFontSize = 10.sp
                                                )
                                            }

                                            OutlinedButton(
                                                onClick = onTriggerCloudBackupSyncNow,
                                                enabled = !isCloudSyncRunning &&
                                                    ConnectivityPrivacyEngine.validateCloudSyncInputs(onlineInputs).isValid,
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 48.dp)
                                                    .testTag("trigger_cloud_sync_now_btn")
                                            ) {
                                                if (isCloudSyncRunning) {
                                                    CircularProgressIndicator(
                                                        strokeWidth = 2.dp,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "در حال ارسال پشتیبان به سرور...",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.CloudUpload,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "ارسال پشتیبان و همگام‌سازی الان",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Thresholds & Network Guardrails Card
                    item {
                        TarazinoGlassCard {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TarazinoSectionHeader(
                                    title = "آستانه‌ها و محدودیت‌های شبکه",
                                    subtitle = "تعیین سقف مصرف، محدودیت وای‌فای و شرط اجرای آنلاین",
                                    icon = Icons.Default.Tune,
                                    accentColor = MaterialTheme.colorScheme.secondary
                                )

                                PrivacySwitchRow(
                                    icon = Icons.Default.Wifi,
                                    accentColor = ext.info,
                                    title = "اجرای قابلیت‌های آنلاین فقط روی وای‌فای",
                                    subtitle = "در صورت اتصال با اینترنت همراه، ارسال درخواست آنلاین مسدود می‌شود.",
                                    checked = policy.wifiOnlyForOnlineFeatures,
                                    onCheckedChange = { wifiOnly ->
                                        onUpdatePolicy { it.copy(wifiOnlyForOnlineFeatures = wifiOnly) }
                                    },
                                    testTagName = "toggle_wifi_only"
                                )

                                HorizontalDivider(color = ext.cardBorderTop.copy(alpha = 0.7f))

                                PrivacySwitchRow(
                                    icon = Icons.Default.AutoAwesome,
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    title = "به‌روزرسانی خودکار نکته هوشمند هنگام ورود",
                                    subtitle = if (policy.autoRefreshAiTipOnLaunch) {
                                        "در صورت فعال بودن هوش مصنوعی، در شروع روز یک بار به صورت خودکار اجرا می‌شود."
                                    } else {
                                        "غیرفعال — هیچ درخواستی بدون لمس دکمه بروزرسانی توسط شما ارسال نمی‌شود."
                                    },
                                    checked = policy.autoRefreshAiTipOnLaunch,
                                    enabled = policy.masterOnlineAccessEnabled && policy.aiFinancialTipsEnabled,
                                    onCheckedChange = { autoLaunch ->
                                        onUpdatePolicy { it.copy(autoRefreshAiTipOnLaunch = autoLaunch) }
                                    },
                                    testTagName = "toggle_auto_refresh_ai_launch"
                                )

                                HorizontalDivider(color = ext.cardBorderTop.copy(alpha = 0.7f))

                                // Daily AI Request Quota Threshold Selector
                                ThresholdOptionSelectorBlock(
                                    title = "سقف مجاز درخواست هوش مصنوعی در هر روز",
                                    subtitle = "مصرف امروز: ${PersianCurrencyEngine.toPersianDigits(usedAiToday.toString())} از ${PersianCurrencyEngine.toPersianDigits(policy.dailyAiRequestLimit.toString())} درخواست",
                                    options = ConnectivityPrivacyEngine.AI_DAILY_QUOTA_OPTIONS,
                                    selectedValue = policy.dailyAiRequestLimit,
                                    labelFormatter = { v -> "${PersianCurrencyEngine.toPersianDigits(v.toString())} بار" },
                                    onSelect = { newLimit ->
                                        onUpdatePolicy { it.copy(dailyAiRequestLimit = newLimit) }
                                    },
                                    testTagPrefix = "ai_quota_option"
                                )

                                HorizontalDivider(color = ext.cardBorderTop.copy(alpha = 0.7f))

                                // Cloud Sync Minimum Changes Threshold Selector
                                ThresholdOptionSelectorBlock(
                                    title = "آستانه حداقل تغییر برای همگام‌سازی ابری",
                                    subtitle = "حداقل تعداد ثبت یا ویرایش قسط پیش از ارسال بسته همگام‌سازی",
                                    options = ConnectivityPrivacyEngine.CLOUD_SYNC_THRESHOLD_OPTIONS,
                                    selectedValue = policy.cloudSyncMinChangesThreshold,
                                    labelFormatter = { v -> "${PersianCurrencyEngine.toPersianDigits(v.toString())} تغییر" },
                                    onSelect = { newThreshold ->
                                        onUpdatePolicy { it.copy(cloudSyncMinChangesThreshold = newThreshold) }
                                    },
                                    testTagPrefix = "cloud_sync_threshold"
                                )
                            }
                        }
                    }

                    // 4. Data Minimization & Device Privacy Permissions Card
                    item {
                        TarazinoGlassCard(
                            accentBorderColor = ext.positive.copy(alpha = 0.32f)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TarazinoSectionHeader(
                                    title = "حریم خصوصی داده‌ها و محافظت صفحه",
                                    subtitle = "ناشناس‌سازی، منع اسکرین‌شات و مخفی‌سازی مبالغ حساس",
                                    icon = Icons.Default.PhonelinkLock,
                                    accentColor = ext.positive
                                )

                                PrivacySwitchRow(
                                    icon = Icons.Default.Shield,
                                    accentColor = ext.positive,
                                    title = "ناشناس‌سازی داده‌های ارسالی به هوش مصنوعی",
                                    subtitle = "نام اشخاص، بانک‌ها، عنوان وام‌ها و سرفصل‌های شخصی پیش از ارسال حذف شده و فقط نسبت‌های عددی ارسال می‌شوند.",
                                    checked = policy.anonymizeAiPayloads,
                                    onCheckedChange = { anonymize ->
                                        onUpdatePolicy { it.copy(anonymizeAiPayloads = anonymize) }
                                    },
                                    testTagName = "toggle_anonymize_ai_payloads"
                                )

                                HorizontalDivider(color = ext.cardBorderTop.copy(alpha = 0.7f))

                                PrivacySwitchRow(
                                    icon = Icons.Default.ScreenshotMonitor,
                                    accentColor = ext.warning,
                                    title = "مسدودسازی اسکرین‌شات و پیش‌نمایش برنامه‌های اخیر",
                                    subtitle = "از ثبت تصویر صفحه، ضبط ویدیو و نمایش محتوای مالی در نمای برنامه‌های اخیر گوشی جلوگیری می‌کند.",
                                    checked = policy.preventScreenshotsAndRecents,
                                    onCheckedChange = { prevent ->
                                        onUpdatePolicy { it.copy(preventScreenshotsAndRecents = prevent) }
                                    },
                                    testTagName = "toggle_prevent_screenshots"
                                )

                                HorizontalDivider(color = ext.cardBorderTop.copy(alpha = 0.7f))

                                PrivacySwitchRow(
                                    icon = Icons.Default.VisibilityOff,
                                    accentColor = MaterialTheme.colorScheme.secondary,
                                    title = "مخفی‌سازی ارقام مالی در ویجت صفحه اصلی",
                                    subtitle = "مبالغ بدهی در ویجت صفحه اصلی به صورت ستاره‌دار (•••,•••) نمایش داده می‌شوند.",
                                    checked = policy.hideAmountsInWidget,
                                    onCheckedChange = { hide ->
                                        onUpdatePolicy { it.copy(hideAmountsInWidget = hide) }
                                    },
                                    testTagName = "toggle_hide_widget_amounts"
                                )

                                HorizontalDivider(color = ext.cardBorderTop.copy(alpha = 0.7f))

                                PrivacySwitchRow(
                                    icon = Icons.Default.Lock,
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    title = "پوشش حریم خصوصی ارقام داخل برنامه",
                                    subtitle = "مخفی‌سازی فوری تمامی مبالغ ریالی و تومانی در داشبورد و گزارش‌ها در محیط‌های عمومی.",
                                    checked = privacyMaskEnabled,
                                    onCheckedChange = { onTogglePrivacyMask() },
                                    testTagName = "toggle_in_app_privacy_mask"
                                )
                            }
                        }
                    }

                    // 5. Instant Strict Offline Reset & Secret Cleanup Card
                    item {
                        TarazinoGlassCard(
                            accentBorderColor = ext.positive.copy(alpha = 0.4f)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "بازگشت فوری به حالت آفلاین مطلق و مدیریت کلیدها",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "کلیدهای API و توکن‌های واردشده با رمزنگاری داخلی دستگاه ذخیره می‌شوند و هرگز در فایل‌های پشتیبان JSON قرار نمی‌گیرند.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 19.sp
                                )

                                if (hasStoredSecrets) {
                                    OutlinedButton(
                                        onClick = onClearStoredOnlineSecrets,
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, ext.warning.copy(alpha = 0.65f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("clear_online_secrets_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteSweep,
                                            contentDescription = null,
                                            tint = ext.warning,
                                            modifier = Modifier.size(19.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        TarazinoAutoSizeText(
                                            text = "پاکسازی کلیدهای API و توکن‌های ذخیره‌شده",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = ext.warning,
                                            maxLines = 1,
                                            minFontSize = 10.sp
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = onResetStrictOffline,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, ext.positive.copy(alpha = 0.65f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("reset_strict_offline_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RestartAlt,
                                        contentDescription = null,
                                        tint = ext.positive,
                                        modifier = Modifier.size(19.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    TarazinoAutoSizeText(
                                        text = "بازنشانی به حالت ۱۰۰٪ آفلاین (غیرفعال‌سازی همه)",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ext.positive,
                                        maxLines = 1,
                                        minFontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun PostureMetricMiniBox(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(13.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(0.75.dp, accent.copy(alpha = 0.28f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            TarazinoAutoSizeText(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                textAlign = TextAlign.Center,
                maxLines = 1,
                minFontSize = 9.sp
            )
        }
    }
}

@Composable
private fun PrivacySwitchRow(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTagName: String,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.52f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 4.dp)
            .heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.14f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled,
            modifier = Modifier.testTag(testTagName)
        )
    }
}

@Composable
private fun ThresholdOptionSelectorBlock(
    title: String,
    subtitle: String,
    options: List<Int>,
    selectedValue: Int,
    labelFormatter: (Int) -> String,
    onSelect: (Int) -> Unit,
    testTagPrefix: String
) {
    val ext = LocalTarazinoColors.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selectedValue
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    },
                    border = BorderStroke(
                        width = if (isSelected) 1.2.dp else 0.8.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            ext.cardBorderTop
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect(option) }
                        .testTag("${testTagPrefix}_$option")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp)
                    ) {
                        TarazinoAutoSizeText(
                            text = labelFormatter(option),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            minFontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
