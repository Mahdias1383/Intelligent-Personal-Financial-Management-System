package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.CurrencyUnit
import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.data.CounterpartyEntity
import com.example.data.EntityCategory
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.ObligationEntity
import com.example.data.TrustPriority
import com.example.domain.AlertUrgencyBucket
import com.example.domain.EnrichedInstallmentAlert
import com.example.ui.TarazinoUiState
import com.example.domain.MonthlySavingsGoalEngine
import com.example.ui.components.AnimatedOdometerMoneyText
import com.example.ui.components.CounterpartyMonogramAvatar
import com.example.ui.components.DashboardDebtCategoryTagFilterSection
import com.example.ui.components.DebtPaidVsOutstandingRingCard
import com.example.ui.components.FormattedMoneyText
import com.example.ui.components.LiquidWaveHealthOrb
import com.example.ui.components.MetallicSmartChipEmblem
import com.example.ui.components.MonthlySavingsGoalDashboardSummarySection
import com.example.ui.components.TarazinoAutoSizeText
import com.example.ui.components.TarazinoGlassCard
import com.example.ui.components.TarazinoIconBadge
import com.example.ui.components.TarazinoLivePulseDot
import com.example.ui.components.TarazinoLuminousProgressBar
import com.example.ui.components.TarazinoMetalVaultCard
import com.example.ui.components.TarazinoMiniBrandCard
import com.example.ui.components.TarazinoQuickActionTile
import com.example.ui.components.TarazinoSectionHeader
import com.example.ui.components.TarazinoSlideToSettleBar
import com.example.ui.components.TarazinoStaggeredReveal
import com.example.ui.components.TarazinoStatusPill
import com.example.ui.components.formatCompactPersianToman
import com.example.ui.components.tarazinoAmbientBackdrop
import com.example.ui.components.tarazinoPressScale
import com.example.ui.components.tourSpotlightTarget
import com.example.ui.theme.LocalTarazinoColors

private data class CategoryShareSlice(
    val category: EntityCategory,
    val labelFa: String,
    val subProvidersPreviewFa: String,
    val remainingToman: Long,
    val shareRatio: Float,
    val color: Color
)

@Composable
fun DashboardScreen(
    uiState: TarazinoUiState,
    onQuickPay: (InstallmentEntity) -> Unit,
    onSnoozeTomorrow: (InstallmentEntity) -> Unit,
    onOpenNewObligationWizard: () -> Unit,
    onOpenCategoryInTree: (EntityCategory) -> Unit,
    onOpenCounterpartyInTree: (CounterpartyEntity) -> Unit,
    onOpenObligationInTree: (ObligationEntity, EntityCategory?) -> Unit,
    onOpenIntelligenceTab: () -> Unit,
    onOpenFullA4Report: () -> Unit,
    onQuickSavingsDepositDelta: (Long) -> Unit = {},
    onUpdateMonthlySavingsGoal: (String, Long, Long) -> Unit = { _, _, _ -> },
    tourScrollIndex: Int? = null
) {
    val ext = LocalTarazinoColors.current
    val unit = uiState.preferences.currencyUnit
    val masked = uiState.preferences.privacyMaskEnabled
    val listState = rememberLazyListState()

    var showAllAlertsModal by remember { mutableStateOf(false) }
    var heroModeIndex by remember { mutableIntStateOf(0) } // 0 = This Month, 1 = Total Remaining, 2 = Receivables
    var isVaultCardFlipped by remember { mutableStateOf(false) }
    var selectedDonutCategory by remember { mutableStateOf<EntityCategory?>(null) }

    // Genuine animated entrance sweep for charts
    var hasAnimatedIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        hasAnimatedIn = true
    }

    LaunchedEffect(tourScrollIndex) {
        if (tourScrollIndex != null && tourScrollIndex >= 0) {
            listState.animateScrollToItem(tourScrollIndex)
        }
    }

    val totalPayable = uiState.totalDebtPayableToman
    val totalPaid = uiState.totalDebtPaidToman
    val totalRem = uiState.totalDebtRemainingToman
    val currentMonthDue = uiState.dtiHealthReport?.currentMonthDebtDueToman ?: 0L
    val receivablesRem = uiState.totalReceivableRemainingToman

    val progressRatio = if (totalPayable > 0L) {
        (totalPaid.toFloat() / totalPayable.toFloat()).coerceIn(0f, 1f)
    } else 1f
    val donutSweepScale by animateFloatAsState(
        targetValue = if (hasAnimatedIn) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessVeryLow),
        label = "DonutSweepEntrance"
    )
    val progressPercent = (progressRatio * 100).toInt()

    // Calculate Category Share Slices for Interactive Donut Chart
    val categorySlices = remember(uiState.counterparties, uiState.obligations, uiState.installments) {
        val ordered = listOf(
            EntityCategory.BNPL_PLATFORM,
            EntityCategory.BANK,
            EntityCategory.INSTITUTION,
            EntityCategory.PERSON
        )
        val rawTotals = ordered.map { cat ->
            val catProviders = uiState.counterparties.filter { it.categoryEnum == cat }
            val providerIds = catProviders.map { it.id }.toSet()
            val debtObIds = uiState.obligations
                .filter { it.counterpartyId in providerIds && !it.categoryEnum.isReceivable }
                .map { it.id }
                .toSet()
            val rem = uiState.installments
                .filter { it.obligationId in debtObIds }
                .sumOf { it.remainingToman }
            val namesPreview = catProviders.take(3).joinToString("، ") {
                it.name.substringBefore(" (")
            }.ifBlank { "بدون زیرمجموعه" }
            Triple(cat, rem, namesPreview)
        }
        val grandTotal = rawTotals.sumOf { it.second }.coerceAtLeast(1L)
        rawTotals.map { (cat, rem, preview) ->
            CategoryShareSlice(
                category = cat,
                labelFa = cat.labelFa,
                subProvidersPreviewFa = preview,
                remainingToman = rem,
                shareRatio = (rem.toFloat() / grandTotal.toFloat()).coerceIn(0f, 1f),
                color = Color(cat.defaultColorHex)
            )
        }
    }

    val top3UrgentAlerts = remember(uiState.smartAlertQueue) {
        uiState.smartAlertQueue.take(3)
    }
    val urgentCount = remember(uiState.smartAlertQueue) {
        uiState.smartAlertQueue.count {
            it.bucket == AlertUrgencyBucket.OVERDUE || it.bucket == AlertUrgencyBucket.DUE_TODAY
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // =========================================================================
        // 1. INTERACTIVE 3D-TILTABLE & 3D-FLIPPABLE PLATINUM VAULT CARD
        //    (Drag to tilt in 3D with live specular flare • Tap flip for Back Vault Stats)
        // =========================================================================
        item {
            val heroModes = listOf(
                uiState.dtiHealthReport?.currentMonthYearLabelFa?.substringBefore(" ") ?: "این ماه",
                "کل بدهی",
                "مطالبات"
            )
            val displayedAmount = when (heroModeIndex) {
                0 -> currentMonthDue
                1 -> totalRem
                else -> receivablesRem
            }
            val displayedSubtitle = when (heroModeIndex) {
                0 -> "مجموع اقساط مانده در ماه جاری"
                1 -> "کل تعهدات و اقساط پرداخت‌نشده"
                else -> "مجموع طلب‌های دریافت‌نشده از دیگران"
            }

            // Dynamic Theme-Synced Aura Morphing per Mode (Institutional Private-Banking Finishes)
            val cardAccentPrimary = when (heroModeIndex) {
                0 -> ext.heroAccent
                1 -> Color(0xFFDEB14B) // Sovereign Bullion Champagne Gold
                else -> Color(0xFF4DA6F0) // Imperial Sapphire Slate
            }
            val cardAccentSecondary = when (heroModeIndex) {
                0 -> MaterialTheme.colorScheme.secondary
                1 -> Color(0xFFD9884B)
                else -> Color(0xFFA794E8)
            }
            val cardBaseStart = when (heroModeIndex) {
                0 -> ext.heroStart
                1 -> if (ext.isDark) Color(0xFF2C2215) else Color(0xFF3B2B16)
                else -> if (ext.isDark) Color(0xFF102446) else Color(0xFF0E3254)
            }
            val cardBaseMid = when (heroModeIndex) {
                0 -> ext.heroMid
                1 -> if (ext.isDark) Color(0xFF1E1914) else Color(0xFF282019)
                else -> if (ext.isDark) Color(0xFF101C36) else Color(0xFF102640)
            }
            val cardBaseEnd = when (heroModeIndex) {
                0 -> ext.heroEnd
                1 -> if (ext.isDark) Color(0xFF13110E) else Color(0xFF1A1715)
                else -> if (ext.isDark) Color(0xFF0B1224) else Color(0xFF0E1B2E)
            }

            val freedomDateShort = uiState.payoffSimulation?.projectedBaselineDateJalali?.let {
                JalaliCalendarEngine.formatJalaliShortText(it)
            } ?: "تسویه کامل!"
            val healthScore = uiState.dtiHealthReport?.obligationCoverageScore ?: 100
            val paidInstCount = uiState.installments.count { it.statusEnum == InstallmentStatus.PAID }

            TarazinoStaggeredReveal(index = 0) {
                TarazinoMetalVaultCard(
                    accentPrimary = cardAccentPrimary,
                    accentSecondary = cardAccentSecondary,
                    baseStart = cardBaseStart,
                    baseMid = cardBaseMid,
                    baseEnd = cardBaseEnd,
                    isFlipped = isVaultCardFlipped,
                    modifier = Modifier.tourSpotlightTarget("dashboard_monthly_summary"),
                backContent = {
                    // BACK OF THE 3D BLACK VAULT CARD
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 18.dp)
                    ) {
                        Spacer(modifier = Modifier.height(14.dp))
                        // Authentic Obsidian Magnetic Security Stripe
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(30.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF020406),
                                            Color(0xFF141B24),
                                            Color(0xFF020406)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "TARAZINO ULTRA • PRIVATE VAULT",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                letterSpacing = 1.6.sp,
                                color = cardAccentPrimary.copy(alpha = 0.65f),
                                modifier = Modifier.padding(horizontal = 18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = cardAccentPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    TarazinoAutoSizeText(
                                        text = "شناسنامه خزانه و آزادی مالی",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        minFontSize = 10.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = cardAccentPrimary.copy(alpha = 0.18f),
                                    border = BorderStroke(0.8.dp, cardAccentPrimary.copy(alpha = 0.55f)),
                                    modifier = Modifier.tarazinoPressScale(scaleDown = 0.92f) {
                                        isVaultCardFlipped = false
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FlipCameraAndroid,
                                            contentDescription = "بازگشت به روی کارت",
                                            tint = cardAccentPrimary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "روی کارت",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = cardAccentPrimary
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = 0.06f),
                                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.12f)),
                                    modifier = Modifier.weight(1.25f)
                                ) {
                                    Column(modifier = Modifier.padding(11.dp)) {
                                        TarazinoAutoSizeText(
                                            text = "تاریخ آزادی از بدهی",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ext.heroOnSurfaceMuted,
                                            maxLines = 1,
                                            minFontSize = 8.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        TarazinoAutoSizeText(
                                            text = freedomDateShort,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = cardAccentPrimary,
                                            maxLines = 1,
                                            minFontSize = 10.sp
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = 0.06f),
                                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.12f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(11.dp)) {
                                        TarazinoAutoSizeText(
                                            text = "امتیاز سلامت مالی",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ext.heroOnSurfaceMuted,
                                            maxLines = 1,
                                            minFontSize = 8.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        TarazinoAutoSizeText(
                                            text = "${PersianCurrencyEngine.toPersianDigits(healthScore.toString())} / ۱۰۰",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFE8C67D),
                                            maxLines = 1,
                                            minFontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TarazinoAutoSizeText(
                                    text = "${PersianCurrencyEngine.toPersianDigits(uiState.obligations.size.toString())} پرونده فعال • ${PersianCurrencyEngine.toPersianDigits(paidInstCount.toString())} قسط تسویه",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ext.heroOnSurface.copy(alpha = 0.88f),
                                    maxLines = 1,
                                    minFontSize = 8.5.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "آفلاین و امن",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = cardAccentPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            ) {
                // FRONT OF THE 3D PLATINUM VAULT CARD
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    // Top Row: Mode Pill Switcher + 3D Flip Trigger (Clean spacing, zero horizontal collision)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.075f),
                            border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.16f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                heroModes.forEachIndexed { idx, label ->
                                    val selected = idx == heroModeIndex
                                    val tabBg by animateColorAsState(
                                        targetValue = if (selected) cardAccentPrimary else Color.Transparent,
                                        label = "HeroModeBg_$idx"
                                    )
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(tabBg)
                                            .tarazinoPressScale(scaleDown = 0.93f) { heroModeIndex = idx }
                                            .padding(horizontal = 4.dp, vertical = 5.dp)
                                    ) {
                                        TarazinoAutoSizeText(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                            color = if (selected) Color(0xFF041C19) else ext.heroOnSurface.copy(alpha = 0.88f),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            minFontSize = 8.5.sp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.09f),
                            border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.22f)),
                            modifier = Modifier
                                .size(32.dp)
                                .tarazinoPressScale(scaleDown = 0.88f) {
                                    isVaultCardFlipped = true
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FlipCameraAndroid,
                                    contentDescription = "چرخش سه‌بعدی پشت کارت",
                                    tint = Color.White.copy(alpha = 0.88f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Focal Balance Row with True Per-Digit Rolling Odometer Animation + 60fps Liquid Wave Orb
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            AnimatedContent(
                                targetState = displayedSubtitle,
                                transitionSpec = {
                                    fadeIn(tween(220)) togetherWith fadeOut(tween(130))
                                },
                                label = "HeroSubtitleFade"
                            ) { sub ->
                                TarazinoAutoSizeText(
                                    text = sub,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ext.heroOnSurfaceMuted,
                                    maxLines = 1,
                                    minFontSize = 9.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            AnimatedOdometerMoneyText(
                                amountToman = displayedAmount,
                                unit = unit,
                                privacyMasked = masked,
                                style = MaterialTheme.typography.displayLarge,
                                numberColor = Color.White,
                                unitColor = cardAccentPrimary.copy(alpha = 0.95f),
                                modifier = Modifier.fillMaxWidth()
                            )
                            AnimatedVisibility(visible = !masked && displayedAmount > 0L) {
                                TarazinoAutoSizeText(
                                    text = PersianCurrencyEngine.amountToPersianWords(displayedAmount, unit),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.68f),
                                    maxLines = 2,
                                    minFontSize = 8.5.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // 60fps Animated Dual-Wave Liquid Settlement Orb
                        LiquidWaveHealthOrb(
                            progress = if (hasAnimatedIn) progressRatio else 0f,
                            accentColor = cardAccentPrimary,
                            centerPrimaryText = "${PersianCurrencyEngine.toPersianDigits(progressPercent.toString())}٪",
                            centerSubText = "تسویه",
                            size = 62.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Translucent Smoked-Glass Vault Footer Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(0.8.dp, Color.White.copy(alpha = 0.13f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 13.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            TarazinoAutoSizeText(
                                text = "تسویه‌شده تا امروز",
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.heroOnSurfaceMuted,
                                maxLines = 1,
                                minFontSize = 8.5.sp
                            )
                            FormattedMoneyText(
                                amountToman = totalPaid,
                                unit = unit,
                                privacyMasked = masked,
                                style = MaterialTheme.typography.labelLarge,
                                numberColor = Color(0xFF34D399),
                                unitColor = Color(0xFFA7F3D0),
                                minFontSize = 9.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(26.dp)
                                .background(Color.White.copy(alpha = 0.14f))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(1f)
                        ) {
                            TarazinoAutoSizeText(
                                text = "کل بدهی مانده",
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.heroOnSurfaceMuted,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                minFontSize = 8.5.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                            FormattedMoneyText(
                                amountToman = totalRem,
                                unit = unit,
                                privacyMasked = masked,
                                style = MaterialTheme.typography.labelLarge,
                                numberColor = Color(0xFFDAB978),
                                unitColor = Color(0xFFF7E7C4),
                                minFontSize = 9.sp,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            }
        }

        // =========================================================================
        // 1.5. SHAMSI QUICK DEBT, MONTHLY SAVINGS GOAL & UPCOMING DEADLINES SUMMARY CARD
        // =========================================================================
        item {
            TarazinoStaggeredReveal(index = 1) {
                ShamsiQuickDebtAndDeadlineSummaryCard(
                    uiState = uiState,
                    currencyUnit = unit,
                    privacyMasked = masked,
                    onQuickPay = onQuickPay,
                    onOpenAllAlerts = { showAllAlertsModal = true },
                    onOpenObligationInTree = onOpenObligationInTree,
                    onQuickSavingsDepositDelta = onQuickSavingsDepositDelta,
                    onUpdateMonthlySavingsGoal = onUpdateMonthlySavingsGoal
                )
            }
        }

        // =========================================================================
        // 1.7. PAID VS. OUTSTANDING DEBT PROGRESS RING CHART
        // =========================================================================
        item {
            val debtObIds = remember(uiState.obligations) {
                uiState.obligations.filter { !it.categoryEnum.isReceivable }.map { it.id }.toSet()
            }
            val paidDebtInstCount = remember(uiState.installments, debtObIds) {
                uiState.installments.count { it.obligationId in debtObIds && it.statusEnum == InstallmentStatus.PAID }
            }
            val outstandingDebtInstCount = remember(uiState.installments, debtObIds) {
                uiState.installments.count { it.obligationId in debtObIds && it.statusEnum != InstallmentStatus.PAID }
            }

            TarazinoStaggeredReveal(index = 2) {
                DebtPaidVsOutstandingRingCard(
                    paidDebtToman = totalPaid,
                    outstandingDebtToman = totalRem,
                    paidInstallmentsCount = paidDebtInstCount,
                    outstandingInstallmentsCount = outstandingDebtInstCount,
                    currencyUnit = unit,
                    privacyMasked = masked
                )
            }
        }

        // =========================================================================
        // 2. BLUBANK-STYLE 4-COLUMN SQUIRCLE QUICK ACTIONS STRIP
        // =========================================================================
        item {
            val mostUrgent = uiState.smartAlertQueue.firstOrNull()
            TarazinoStaggeredReveal(index = 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TarazinoQuickActionTile(
                    label = "ثبت قسط",
                    icon = Icons.Default.AddCircle,
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = onOpenNewObligationWizard,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_add_obligation_btn")
                )
                TarazinoQuickActionTile(
                    label = "تسویه فوری",
                    icon = Icons.Default.Bolt,
                    accentColor = ext.warning,
                    badgeCount = urgentCount,
                    onClick = {
                        if (mostUrgent != null) {
                            onQuickPay(mostUrgent.installment)
                        } else {
                            showAllAlertsModal = true
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_bar_pay_urgent_btn")
                )
                TarazinoQuickActionTile(
                    label = "بودجه و هوش",
                    icon = Icons.Default.Insights,
                    accentColor = ext.info,
                    onClick = onOpenIntelligenceTab,
                    modifier = Modifier.weight(1f)
                )
                TarazinoQuickActionTile(
                    label = "گزارش A4",
                    icon = Icons.Default.PictureAsPdf,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    onClick = onOpenFullA4Report,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_open_a4_report_btn")
                )
            }
            }
        }

        // =========================================================================
        // 2.2. APPLE WALLET × REVOLUT PHYSICAL MINI-CARD DECK (کیف کارت طرف‌های پرداخت)
        // =========================================================================
        if (uiState.counterparties.isNotEmpty()) {
            item {
                TarazinoStaggeredReveal(index = 2) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "کیف کارت طرف‌های پرداخت",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Text(
                            text = "${PersianCurrencyEngine.toPersianDigits(uiState.counterparties.size.toString())} کارت فعال",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        uiState.counterparties.forEach { cp ->
                            val cpObs = uiState.obligations.filter { it.counterpartyId == cp.id }
                            val cpObIds = cpObs.map { it.id }.toSet()
                            val cpInsts = uiState.installments.filter { it.obligationId in cpObIds }
                            val cpTotalPayable = cpInsts.sumOf { it.effectiveTotalToman }
                            val cpTotalPaid = cpInsts.sumOf { it.paidAmountToman }
                            val cpRem = cpInsts.sumOf { it.remainingToman }
                            val cpPending = cpInsts.count { it.statusEnum != InstallmentStatus.PAID }
                            val cpProg = if (cpTotalPayable > 0L) {
                                (cpTotalPaid.toFloat() / cpTotalPayable.toFloat()).coerceIn(0f, 1f)
                            } else 1f
                            val last4 = cp.cardNumber.filter { it.isDigit() }.takeLast(4).ifBlank { null }

                            TarazinoMiniBrandCard(
                                name = cp.name,
                                categoryBadgeFa = cp.categoryEnum.badgeFa,
                                remainingToman = cpRem,
                                progressRatio = cpProg,
                                pendingCount = cpPending,
                                lastFourDigits = last4,
                                accentColor = Color(cp.avatarColorHex),
                                currencyUnit = unit,
                                privacyMasked = masked,
                                onClick = { onOpenCounterpartyInTree(cp) },
                                modifier = Modifier.testTag("dashboard_quick_cp_${cp.id}")
                            )
                        }
                    }
                }
                }
            }
        }

        // =========================================================================
        // 2.5. INTERACTIVE TAP-TO-INSPECT 7-DAY JALALI CASHFLOW PULSE STRIP
        // =========================================================================
        item {
            TarazinoStaggeredReveal(index = 3) {
                WeeklyJalaliCashflowStrip(
                    alerts = uiState.smartAlertQueue,
                    currencyUnit = unit,
                    privacyMasked = masked,
                    onQuickPay = onQuickPay,
                    onOpenCalendarTab = onOpenIntelligenceTab
                )
            }
        }

        // =========================================================================
        // 3. INTERACTIVE ANIMATED DONUT CHART (CATEGORY BREAKDOWN)
        // =========================================================================
        item {
            val activeSlice = categorySlices.find { it.category == selectedDonutCategory }

            TarazinoGlassCard(
                cornerRadius = 26.dp,
                modifier = Modifier
                    .tourSpotlightTarget("dashboard_category_donut")
                    .testTag("dashboard_category_donut_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TarazinoSectionHeader(
                        title = "ترکیب تعهدات بر اساس دسته",
                        subtitle = "لمس هر دسته برای بزرگ‌نمایی یا ورود به پوشه",
                        icon = Icons.Default.DonutLarge,
                        accentColor = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Interactive Expanding Donut Ring (Properly inset so stroke NEVER overflows bounds!)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(116.dp)
                                .clickable { selectedDonutCategory = null }
                        ) {
                            Canvas(modifier = Modifier.size(114.dp)) {
                                val baseStroke = 13.dp.toPx()
                                val selectedStroke = 17.dp.toPx()
                                val inset = selectedStroke / 2f + 1.dp.toPx()
                                val arcSize = Size(
                                    (size.width - inset * 2f).coerceAtLeast(10f),
                                    (size.height - inset * 2f).coerceAtLeast(10f)
                                )
                                val arcTopLeft = Offset(inset, inset)

                                drawArc(
                                    color = Color.Gray.copy(alpha = 0.13f),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = arcTopLeft,
                                    size = arcSize,
                                    style = Stroke(width = baseStroke)
                                )
                                if (totalRem > 0L) {
                                    var currentStartAngle = -90f
                                    categorySlices.forEach { slice ->
                                        val rawSweep = (slice.shareRatio * 360f)
                                            .coerceAtLeast(if (slice.remainingToman > 0L) 9f else 0f)
                                        val sweep = rawSweep * donutSweepScale
                                        val isFocused = selectedDonutCategory == slice.category
                                        val alpha = if (selectedDonutCategory == null || isFocused) 1f else 0.30f
                                        if (sweep > 0f) {
                                            drawArc(
                                                color = slice.color.copy(alpha = alpha),
                                                startAngle = currentStartAngle,
                                                sweepAngle = (sweep - 4f).coerceAtLeast(2f),
                                                useCenter = false,
                                                topLeft = arcTopLeft,
                                                size = arcSize,
                                                style = Stroke(
                                                    width = if (isFocused) selectedStroke else baseStroke,
                                                    cap = StrokeCap.Round
                                                )
                                            )
                                            currentStartAngle += sweep
                                        }
                                    }
                                }
                            }

                            AnimatedContent(
                                targetState = activeSlice,
                                label = "DonutCenterContent"
                            ) { focused ->
                                if (focused != null) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                    ) {
                                        TarazinoAutoSizeText(
                                            text = formatCompactPersianToman(
                                                focused.remainingToman * unit.multiplierFromToman,
                                                masked
                                            ),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = focused.color,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            minFontSize = 8.5.sp
                                        )
                                        Text(
                                            text = "${PersianCurrencyEngine.toPersianDigits((focused.shareRatio * 100).toInt().toString())}٪",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                    ) {
                                        TarazinoAutoSizeText(
                                            text = PersianCurrencyEngine.toPersianDigits(uiState.obligations.size.toString()),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            minFontSize = 13.sp
                                        )
                                        Text(
                                            text = "پرونده",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // Crisp Interactive Category Legend List (Clean unified surfaces + jewel focal accents)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            categorySlices.forEach { slice ->
                                val pct = (slice.shareRatio * 100).toInt()
                                val isSel = selectedDonutCategory == slice.category
                                val borderW by animateDpAsState(
                                    targetValue = if (isSel) 1.4.dp else 0.85.dp,
                                    label = "SliceBorder_${slice.category.name}"
                                )
                                val rowSurfaceColor by animateColorAsState(
                                    targetValue = if (isSel) {
                                        slice.color.copy(alpha = if (ext.isDark) 0.16f else 0.10f)
                                    } else {
                                        ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.72f else 0.85f)
                                    },
                                    label = "SliceBg_${slice.category.name}"
                                )
                                val rowBorderColor by animateColorAsState(
                                    targetValue = if (isSel) {
                                        slice.color.copy(alpha = 0.78f)
                                    } else {
                                        ext.cardBorderTop
                                    },
                                    label = "SliceBorderCol_${slice.category.name}"
                                )
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = rowSurfaceColor,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    border = BorderStroke(borderW, rowBorderColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .tarazinoPressScale(scaleDown = 0.96f) {
                                            if (isSel) {
                                                onOpenCategoryInTree(slice.category)
                                            } else {
                                                selectedDonutCategory = slice.category
                                            }
                                        }
                                        .testTag("donut_legend_${slice.category.name}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(slice.color.copy(alpha = 0.20f))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(slice.color)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                TarazinoAutoSizeText(
                                                    text = slice.category.badgeFa,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    minFontSize = 9.5.sp
                                                )
                                                TarazinoAutoSizeText(
                                                    text = if (isSel) "لمس مجدد برای باز کردن" else slice.subProvidersPreviewFa,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isSel) slice.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    minFontSize = 8.5.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = slice.color.copy(alpha = if (ext.isDark) 0.16f else 0.12f),
                                            border = BorderStroke(0.65.dp, slice.color.copy(alpha = 0.35f))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "${PersianCurrencyEngine.toPersianDigits(pct.toString())}٪",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = slice.color,
                                                    maxLines = 1
                                                )
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                    contentDescription = null,
                                                    tint = slice.color,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 3.5. INTERACTIVE DEBT CATEGORY & CUSTOM TAG FILTER VIEW
        //      (Filter Dashboard by 'Bank Loan', 'Credit Card', 'Personal', 'Check & Housing', 'Receivable' or #tags)
        // =========================================================================
        item {
            DashboardDebtCategoryTagFilterSection(
                obligations = uiState.obligations,
                installments = uiState.installments,
                counterparties = uiState.counterparties,
                alerts = uiState.smartAlertQueue,
                currencyUnit = unit,
                privacyMasked = masked,
                onQuickPay = onQuickPay,
                onOpenObligationInTree = onOpenObligationInTree
            )
        }

        // =========================================================================
        // 4. TOP 3 URGENT INSTALLMENTS RADAR WITH KINETIC SLIDE-TO-SETTLE BARS
        // =========================================================================
        item {
            TarazinoSectionHeader(
                title = "۳ سررسید فوری پیش‌رو",
                subtitle = "برای تسویه، دکمه سبز را به چپ بکشید یا لمس کنید",
                icon = Icons.Default.NotificationsActive,
                accentColor = MaterialTheme.colorScheme.secondary,
                trailing = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (urgentCount > 0) {
                            TarazinoLivePulseDot(
                                color = ext.danger,
                                size = 7.dp
                            )
                        }
                        if (uiState.smartAlertQueue.size > 3) {
                            Surface(
                                shape = RoundedCornerShape(11.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(11.dp))
                                    .tarazinoPressScale(scaleDown = 0.94f) { showAllAlertsModal = true }
                                    .testTag("dashboard_view_all_alerts_btn")
                            ) {
                                Text(
                                    text = "همه (${PersianCurrencyEngine.toPersianDigits(uiState.smartAlertQueue.size.toString())}) ‹",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            )
        }

        if (top3UrgentAlerts.isEmpty()) {
            item {
                TarazinoGlassCard(
                    accentBorderColor = ext.positive,
                    modifier = Modifier.tourSpotlightTarget("dashboard_urgent_alerts")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                    ) {
                        TarazinoIconBadge(
                            icon = Icons.Default.CheckCircle,
                            accentColor = ext.positive,
                            size = 50.dp,
                            iconSize = 26.dp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "عالی! هیچ قسط فوری یا معوقه‌ای ندارید.",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "تمامی تعهدات نزدیک شما با موفقیت مدیریت شده‌اند.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(top3UrgentAlerts, key = { "top_alert_${it.installment.id}" }) { alert ->
                val isFirstAlert = alert.installment.id == top3UrgentAlerts.first().installment.id
                SmartAlertRadarCard(
                    alert = alert,
                    currencyUnit = unit,
                    privacyMasked = masked,
                    onQuickPay = { onQuickPay(alert.installment) },
                    onSnoozeTomorrow = { onSnoozeTomorrow(alert.installment) },
                    onOpenInTree = {
                        onOpenObligationInTree(alert.obligation, alert.counterparty?.categoryEnum)
                    },
                    modifier = if (isFirstAlert) {
                        Modifier.tourSpotlightTarget("dashboard_urgent_alerts")
                    } else Modifier
                )
            }
        }

        // =========================================================================
        // 5. LIVING LIQUID-ORB FINANCIAL HEALTH & SAFE CAPACITY CARD
        // =========================================================================
        uiState.dtiHealthReport?.let { dti ->
            item {
                val statusColor = Color(dti.statusColorHex)
                val capacityRatio = if (dti.safeMaxTotalDebtBudgetToman > 0L) {
                    (dti.safeNewInstallmentCapacityToman.toFloat() / dti.safeMaxTotalDebtBudgetToman.toFloat())
                        .coerceIn(0f, 1f)
                } else 0f

                TarazinoGlassCard(
                    cornerRadius = 24.dp,
                    accentBorderColor = statusColor,
                    onClick = onOpenIntelligenceTab
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                TarazinoIconBadge(
                                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                                    accentColor = statusColor,
                                    size = 42.dp,
                                    iconSize = 20.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "سقف امن افزودن قسط جدید",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    FormattedMoneyText(
                                        amountToman = dti.safeNewInstallmentCapacityToman,
                                        unit = unit,
                                        privacyMasked = masked,
                                        style = MaterialTheme.typography.titleMedium,
                                        numberColor = statusColor,
                                        unitColor = statusColor.copy(alpha = 0.78f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Living Liquid Sphere showing Financial Health Score / 100
                            LiquidWaveHealthOrb(
                                progress = (dti.obligationCoverageScore / 100f).coerceIn(0.08f, 1f),
                                accentColor = statusColor,
                                centerPrimaryText = PersianCurrencyEngine.toPersianDigits(dti.obligationCoverageScore.toString()),
                                centerSubText = "از ۱۰۰",
                                size = 58.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TarazinoLuminousProgressBar(
                            progress = capacityRatio,
                            color = statusColor,
                            height = 7.dp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TarazinoAutoSizeText(
                                text = "نقدینگی آزاد: ${
                                    PersianCurrencyEngine.formatAmount(
                                        dti.freeCashflowToman,
                                        unit,
                                        privacyMasked = masked
                                    )
                                }",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                minFontSize = 8.5.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "بودجه و پیش‌بینی ML ‹",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }

                        // Proactive ML Deficit Early-Warning Strip right on the Dashboard
                        uiState.mlForecastReport?.let { forecast ->
                            val hasDeficit = forecast.deficitMonths.isNotEmpty()
                            val stripColor = if (hasDeficit) ext.danger else ext.positive
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = stripColor.copy(alpha = if (ext.isDark) 0.14f else 0.09f),
                                border = BorderStroke(0.8.dp, stripColor.copy(alpha = 0.36f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dashboard_ml_deficit_alert_strip")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        TarazinoLivePulseDot(
                                            color = stripColor,
                                            size = 6.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        TarazinoAutoSizeText(
                                            text = if (hasDeficit) {
                                                val firstDef = forecast.firstDeficitMonth?.monthShortFa ?: ""
                                                "هشدار ML: ${
                                                    PersianCurrencyEngine.toPersianDigits(forecast.deficitMonths.size.toString())
                                                } ماه کسری در پیش است (نزدیک‌ترین: $firstDef)"
                                            } else {
                                                "پیش‌بینی ML: ۱۲ ماه آینده بدون کسری بودجه (امتیاز ${
                                                    PersianCurrencyEngine.toPersianDigits(forecast.averageProjectedHealthScore.toString())
                                                })"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = stripColor,
                                            maxLines = 2,
                                            minFontSize = 9.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        // Modal Sheet for Viewing All Alerts & Filtering by Bucket
    if (showAllAlertsModal) {
        AllAlertsModalDialog(
            alerts = uiState.smartAlertQueue,
            currencyUnit = unit,
            privacyMasked = masked,
            onDismiss = { showAllAlertsModal = false },
            onQuickPay = onQuickPay,
            onSnoozeTomorrow = onSnoozeTomorrow,
            onOpenAlertInTree = { alert ->
                showAllAlertsModal = false
                onOpenObligationInTree(alert.obligation, alert.counterparty?.categoryEnum)
            }
        )
    }
}

@Composable
fun ShamsiQuickDebtAndDeadlineSummaryCard(
    uiState: TarazinoUiState,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    onQuickPay: (InstallmentEntity) -> Unit,
    onOpenAllAlerts: () -> Unit,
    onOpenObligationInTree: (ObligationEntity, EntityCategory?) -> Unit,
    onQuickSavingsDepositDelta: (Long) -> Unit = {},
    onUpdateMonthlySavingsGoal: (String, Long, Long) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val ext = LocalTarazinoColors.current
    val todayJalali = remember { JalaliCalendarEngine.todayJalali() }
    val parsedToday = remember(todayJalali) { JalaliCalendarEngine.parseJalali(todayJalali) }
    val todayVerboseFa = remember(todayJalali) { JalaliCalendarEngine.formatJalaliVerbose(todayJalali) }
    val seasonNameFa = remember(parsedToday) { JalaliCalendarEngine.getSeasonName(parsedToday.month) }
    val monthLength = remember(parsedToday) {
        JalaliCalendarEngine.getJalaliMonthLength(parsedToday.year, parsedToday.month)
    }
    val daysRemainingInMonth = (monthLength - parsedToday.day).coerceAtLeast(0)
    val decadeLabelFa = when (parsedToday.decadeOfMonth) {
        1 -> "دهه اول ${parsedToday.monthNameFa}"
        2 -> "دهه دوم ${parsedToday.monthNameFa}"
        else -> "دهه سوم ${parsedToday.monthNameFa}"
    }

    val activeDebtObligationsCount = remember(uiState.obligations, uiState.installments) {
        val unpaidObIds = uiState.installments
            .filter { it.remainingToman > 0L && it.statusEnum != InstallmentStatus.PAID }
            .map { it.obligationId }
            .toSet()
        uiState.obligations.count { !it.categoryEnum.isReceivable && it.id in unpaidObIds }
    }

    val currentMonthRemainingCount = remember(uiState.installments, uiState.obligations, parsedToday) {
        val debtObIds = uiState.obligations.filter { !it.categoryEnum.isReceivable }.map { it.id }.toSet()
        uiState.installments.count { inst ->
            inst.obligationId in debtObIds &&
                inst.remainingToman > 0L &&
                inst.statusEnum != InstallmentStatus.PAID &&
                JalaliCalendarEngine.isInJalaliMonth(inst.dueDateJalali, parsedToday.year, parsedToday.month)
        }
    }

    val upcomingDeadlines = remember(uiState.smartAlertQueue) {
        uiState.smartAlertQueue
            .filter { !it.obligation.categoryEnum.isReceivable }
            .take(3)
    }

    TarazinoGlassCard(
        cornerRadius = 24.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_shamsi_quick_summary_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Shamsi Calendar Header Bar with Today's Date, Decade & Remaining Month Days
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    TarazinoIconBadge(
                        icon = Icons.Default.Alarm,
                        accentColor = MaterialTheme.colorScheme.primary,
                        size = 38.dp,
                        iconSize = 19.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        TarazinoAutoSizeText(
                            text = "خلاصه بدهی‌های فعال و سررسیدهای شمسی",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            minFontSize = 10.5.sp
                        )
                        TarazinoAutoSizeText(
                            text = "$todayVerboseFa • $seasonNameFa ($decadeLabelFa)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            minFontSize = 8.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = if (ext.isDark) 0.16f else 0.12f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.40f))
                ) {
                    Text(
                        text = "${PersianCurrencyEngine.toPersianDigits(daysRemainingInMonth.toString())} روز تا پایان ماه",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 2. Quick View Twin KPI Tiles: Total Active Debts & Current Shamsi Month Remaining Installments
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.72f else 0.88f),
                    border = BorderStroke(0.8.dp, ext.cardBorderTop),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("shamsi_summary_total_active_debt_tile")
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TarazinoAutoSizeText(
                                text = "کل بدهی‌های فعال",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                minFontSize = 8.5.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${PersianCurrencyEngine.toPersianDigits(activeDebtObligationsCount.toString())} پرونده",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        FormattedMoneyText(
                            amountToman = uiState.totalDebtRemainingToman,
                            unit = currencyUnit,
                            privacyMasked = privacyMasked,
                            style = MaterialTheme.typography.titleSmall,
                            numberColor = MaterialTheme.colorScheme.onSurface,
                            unitColor = MaterialTheme.colorScheme.primary,
                            minFontSize = 9.5.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.72f else 0.88f),
                    border = BorderStroke(0.8.dp, ext.cardBorderTop),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("shamsi_summary_current_month_tile")
                ) {
                    val monthRemainingToman = uiState.dtiHealthReport?.currentMonthRemainingDueToman ?: 0L
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TarazinoAutoSizeText(
                                text = "مانده اقساط ${parsedToday.monthNameFa}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                minFontSize = 8.5.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${PersianCurrencyEngine.toPersianDigits(currentMonthRemainingCount.toString())} قسط",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        FormattedMoneyText(
                            amountToman = monthRemainingToman,
                            unit = currencyUnit,
                            privacyMasked = privacyMasked,
                            style = MaterialTheme.typography.titleSmall,
                            numberColor = MaterialTheme.colorScheme.onSurface,
                            unitColor = MaterialTheme.colorScheme.secondary,
                            minFontSize = 9.5.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 2.5. Monthly Savings Goal Tracker & Progress within the Dashboard Summary
            val savingsSummary = remember(
                uiState.monthlySavingsSummary,
                uiState.preferences.monthlySavingsGoalTitle,
                uiState.preferences.monthlySavingsTargetToman,
                uiState.preferences.monthlySavingsSavedToman,
                uiState.preferences.monthlyIncomeToman,
                uiState.preferences.livingExpensePercent,
                uiState.dtiHealthReport?.currentMonthDebtDueToman,
                todayJalali
            ) {
                uiState.monthlySavingsSummary ?: MonthlySavingsGoalEngine.evaluateMonthlySavingsGoal(
                    goalTitleFa = uiState.preferences.monthlySavingsGoalTitle,
                    targetAmountToman = uiState.preferences.monthlySavingsTargetToman,
                    savedAmountToman = uiState.preferences.monthlySavingsSavedToman,
                    monthlyIncomeToman = uiState.preferences.monthlyIncomeToman,
                    livingExpensePercent = uiState.preferences.livingExpensePercent,
                    currentMonthDebtDueToman = uiState.dtiHealthReport?.currentMonthDebtDueToman ?: 0L,
                    todayJalali = todayJalali
                )
            }

            MonthlySavingsGoalDashboardSummarySection(
                summary = savingsSummary,
                currencyUnit = currencyUnit,
                privacyMasked = privacyMasked,
                onQuickDepositDelta = onQuickSavingsDepositDelta,
                onUpdateSavingsGoal = onUpdateMonthlySavingsGoal
            )

            // 3. Upcoming Installment Deadlines Timeline (Powered by JalaliCalendarEngine)
            if (upcomingDeadlines.isNotEmpty()) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    thickness = 0.75.dp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "نزدیک‌ترین سررسیدهای تقویم شمسی:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "مشاهده همه ‹",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .tarazinoPressScale(onClick = onOpenAllAlerts)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("shamsi_summary_view_all_deadlines_btn")
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    upcomingDeadlines.forEach { item ->
                        val dueDateFormatted = JalaliCalendarEngine.formatJalaliShortText(item.installment.dueDateJalali)
                        val relativeStatusFa = JalaliCalendarEngine.formatRelativeDueStatusFa(
                            todayJalali = todayJalali,
                            targetDateJalali = item.installment.dueDateJalali
                        )
                        val statusColor = Color(item.bucket.colorHex)

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.58f else 0.76f),
                            border = BorderStroke(0.75.dp, statusColor.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .tarazinoPressScale(scaleDown = 0.97f) {
                                    onOpenObligationInTree(item.obligation, item.counterparty?.categoryEnum)
                                }
                                .testTag("shamsi_deadline_row_${item.installment.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(statusColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        TarazinoAutoSizeText(
                                            text = item.installment.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            minFontSize = 9.sp
                                        )
                                        TarazinoAutoSizeText(
                                            text = "$dueDateFormatted • $relativeStatusFa",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = statusColor,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            minFontSize = 8.5.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FormattedMoneyText(
                                        amountToman = item.installment.remainingToman,
                                        unit = currencyUnit,
                                        privacyMasked = privacyMasked,
                                        style = MaterialTheme.typography.labelMedium,
                                        numberColor = MaterialTheme.colorScheme.onSurface,
                                        minFontSize = 8.5.sp,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.widthIn(max = 120.dp)
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(9.dp),
                                        color = ext.positive.copy(alpha = if (ext.isDark) 0.20f else 0.14f),
                                        border = BorderStroke(0.7.dp, ext.positive.copy(alpha = 0.45f)),
                                        modifier = Modifier.tarazinoPressScale(scaleDown = 0.92f) {
                                            onQuickPay(item.installment)
                                        }
                                    ) {
                                        Text(
                                            text = "تسویه",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = ext.positive,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyJalaliCashflowStrip(
    alerts: List<EnrichedInstallmentAlert>,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    onQuickPay: (InstallmentEntity) -> Unit,
    onOpenCalendarTab: () -> Unit
) {
    val ext = LocalTarazinoColors.current
    val today = remember { JalaliCalendarEngine.todayJalali() }
    var selectedOffset by remember { mutableStateOf<Int?>(null) }

    val weekDays = remember(today, alerts) {
        (0..6).map { offset ->
            val dateStr = JalaliCalendarEngine.addJalaliDays(today, offset)
            val parsed = JalaliCalendarEngine.parseJalali(dateStr)
            val weekdayIdx = JalaliCalendarEngine.getPersianWeekdayIndex(dateStr)
            val dayAlerts = alerts.filter { it.installment.dueDateJalali == dateStr }
            Triple(
                Triple(dateStr, parsed.day, JalaliCalendarEngine.WEEKDAY_SHORT[weekdayIdx]),
                offset,
                dayAlerts
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = ext.glassSurface,
        border = BorderStroke(
            0.95.dp,
            Brush.verticalGradient(listOf(ext.cardBorderTop, ext.cardBorderBottom))
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "نبض ۷ روزه سررسیدها",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "تقویم کامل ‹",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .tarazinoPressScale(onClick = onOpenCalendarTab)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                weekDays.forEach { (dateMeta, offset, dayAlerts) ->
                    val (_, dayNum, shortDay) = dateMeta
                    val isToday = offset == 0
                    val isSelected = selectedOffset == offset
                    val hasDue = dayAlerts.isNotEmpty()
                    val dotColor = when {
                        hasDue && isToday -> ext.danger
                        hasDue -> ext.warning
                        else -> ext.positive.copy(alpha = 0.45f)
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            hasDue -> ext.warning.copy(alpha = 0.13f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
                        },
                        border = BorderStroke(
                            width = if (isSelected || isToday) 1.2.dp else 0.7.dp,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                                hasDue -> ext.warning.copy(alpha = 0.45f)
                                else -> Color.Transparent
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .tarazinoPressScale(scaleDown = 0.90f) {
                                selectedOffset = if (isSelected) null else offset
                            }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp)
                        ) {
                            Text(
                                text = shortDay,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else if (isToday) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = PersianCurrencyEngine.toPersianDigits(dayNum.toString()),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected || isToday || hasDue) FontWeight.Black else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(if (hasDue) 6.dp else 4.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else dotColor)
                            )
                        }
                    }
                }
            }

            // Interactive Inline Drawer revealing the tapped day's exact installments!
            AnimatedVisibility(
                visible = selectedOffset != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                selectedOffset?.let { off ->
                    val chosen = weekDays.getOrNull(off)
                    if (chosen != null) {
                        val (meta, _, dayAlerts) = chosen
                        val dateVerbose = JalaliCalendarEngine.formatJalaliShortText(meta.first)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
                                .border(0.8.dp, ext.cardBorderTop, RoundedCornerShape(14.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (off == 0) "امروز • $dateVerbose" else dateVerbose,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (dayAlerts.isEmpty()) "بدون سررسید"
                                    else "${PersianCurrencyEngine.toPersianDigits(dayAlerts.size.toString())} قسط",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (dayAlerts.isEmpty()) {
                                Text(
                                    text = "روز آزاد و بدون تعهد مالی — خیال آسوده ✨",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.positive,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                dayAlerts.forEach { a ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            TarazinoAutoSizeText(
                                                text = a.installment.title,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                minFontSize = 9.5.sp
                                            )
                                            FormattedMoneyText(
                                                amountToman = a.installment.remainingToman,
                                                unit = currencyUnit,
                                                privacyMasked = privacyMasked,
                                                style = MaterialTheme.typography.labelSmall,
                                                numberColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                minFontSize = 8.5.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = ext.positive,
                                            modifier = Modifier.tarazinoPressScale(scaleDown = 0.92f) {
                                                onQuickPay(a.installment)
                                            }
                                        ) {
                                            Text(
                                                text = "تسویه",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = ext.onPositive,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartAlertRadarCard(
    alert: EnrichedInstallmentAlert,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    onQuickPay: () -> Unit,
    onSnoozeTomorrow: () -> Unit,
    onOpenInTree: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTarazinoColors.current
    val bucketColor = Color(alert.bucket.colorHex)
    val isCritical = alert.obligation.priorityEnum == TrustPriority.CRITICAL ||
        alert.counterparty?.priorityEnum == TrustPriority.CRITICAL

    val countdownTextFa = when {
        alert.daysUntilDue < 0 -> "${PersianCurrencyEngine.toPersianDigits((-alert.daysUntilDue).toString())} روز معوق!"
        alert.daysUntilDue == 0 -> "سررسید امروز!"
        else -> "${PersianCurrencyEngine.toPersianDigits(alert.daysUntilDue.toString())} روز مانده"
    }

    TarazinoGlassCard(
        cornerRadius = 22.dp,
        accentBorderColor = if (isCritical || alert.bucket == AlertUrgencyBucket.OVERDUE) bucketColor else null,
        onClick = onOpenInTree,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        val cpColor = Color(alert.counterparty?.avatarColorHex ?: 0xFF00E5A0)
                        CounterpartyMonogramAvatar(
                            name = alert.counterparty?.name ?: "ت",
                            color = cpColor,
                            size = 42.dp
                        )
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(bucketColor)
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(11.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        TarazinoAutoSizeText(
                            text = alert.installment.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            minFontSize = 10.5.sp
                        )
                        TarazinoAutoSizeText(
                            text = "${alert.counterparty?.name ?: "طرف‌حساب"} • ${PersianCurrencyEngine.toPersianDigits(alert.installment.dueDateJalali)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            minFontSize = 8.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                FormattedMoneyText(
                    amountToman = alert.installment.remainingToman,
                    unit = currencyUnit,
                    privacyMasked = privacyMasked,
                    style = MaterialTheme.typography.titleSmall,
                    numberColor = MaterialTheme.colorScheme.onSurface,
                    minFontSize = 9.5.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(max = 152.dp)
                )
            }

            Spacer(modifier = Modifier.height(7.dp))

            // Dedicated non-colliding status pill strip below title & amount
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TarazinoStatusPill(
                    text = countdownTextFa,
                    accentColor = bucketColor
                )
                if (isCritical) {
                    TarazinoStatusPill(
                        text = "اولویت حیاتی",
                        accentColor = ext.danger,
                        icon = Icons.Default.PriorityHigh
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Kinetic CRED/Revolut Slide-to-Settle Track + Compact Snooze Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TarazinoSlideToSettleBar(
                    labelFa = "بکشید برای تسویه",
                    accentColor = ext.positive,
                    onConfirmSettle = onQuickPay,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_pay_btn_${alert.installment.id}")
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .tarazinoPressScale(scaleDown = 0.93f, onClick = onSnoozeTomorrow)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 11.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "یادآوری فردا",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "فردا",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AllAlertsModalDialog(
    alerts: List<EnrichedInstallmentAlert>,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    onDismiss: () -> Unit,
    onQuickPay: (InstallmentEntity) -> Unit,
    onSnoozeTomorrow: (InstallmentEntity) -> Unit,
    onOpenAlertInTree: (EnrichedInstallmentAlert) -> Unit
) {
    val ext = LocalTarazinoColors.current
    var selectedBucket by remember { mutableStateOf<AlertUrgencyBucket?>(null) }
    val filtered = remember(alerts, selectedBucket) {
        if (selectedBucket == null) alerts else alerts.filter { it.bucket == selectedBucket }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .tarazinoAmbientBackdrop(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    auroraPrimary = ext.auroraPrimary,
                    auroraSecondary = ext.auroraSecondary
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Surface(
                    color = ext.glassSurface,
                    border = BorderStroke(0.8.dp, ext.cardBorderTop),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            TarazinoIconBadge(
                                icon = Icons.Default.NotificationsActive,
                                accentColor = MaterialTheme.colorScheme.secondary,
                                size = 40.dp,
                                iconSize = 20.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                TarazinoAutoSizeText(
                                    text = "تمام سررسیدها و اقساط پیش‌رو",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    minFontSize = 11.5.sp
                                )
                                TarazinoAutoSizeText(
                                    text = "${PersianCurrencyEngine.toPersianDigits(alerts.size.toString())} قسط فعال در صف یادآوری",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    minFontSize = 9.sp
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "بستن")
                        }
                    }
                }

                // Sleek Custom Glass Bucket Filter Pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    val allSelected = selectedBucket == null
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (allSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.tarazinoPressScale(scaleDown = 0.94f) { selectedBucket = null }
                    ) {
                        Text(
                            text = "همه (${PersianCurrencyEngine.toPersianDigits(alerts.size.toString())})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (allSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                    AlertUrgencyBucket.entries.forEach { b ->
                        val c = alerts.count { it.bucket == b }
                        val sel = selectedBucket == b
                        val bColor = Color(b.colorHex)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (sel) bColor else bColor.copy(alpha = 0.13f),
                            border = BorderStroke(0.8.dp, bColor.copy(alpha = 0.42f)),
                            modifier = Modifier.tarazinoPressScale(scaleDown = 0.94f) { selectedBucket = b }
                        ) {
                            Text(
                                text = "${b.labelFa} (${PersianCurrencyEngine.toPersianDigits(c.toString())})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (sel) Color.White else bColor,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filtered, key = { "modal_alert_${it.installment.id}" }) { alert ->
                        SmartAlertRadarCard(
                            alert = alert,
                            currencyUnit = currencyUnit,
                            privacyMasked = privacyMasked,
                            onQuickPay = { onQuickPay(alert.installment) },
                            onSnoozeTomorrow = { onSnoozeTomorrow(alert.installment) },
                            onOpenInTree = { onOpenAlertInTree(alert) }
                        )
                    }
                }
            }
        }
    }
}
