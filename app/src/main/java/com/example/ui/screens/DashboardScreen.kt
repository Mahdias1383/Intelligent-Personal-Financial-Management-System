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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.components.CounterpartyMonogramAvatar
import com.example.ui.components.FormattedMoneyText
import com.example.ui.components.LiquidWaveHealthOrb
import com.example.ui.components.MetallicSmartChipEmblem
import com.example.ui.components.TarazinoGlassCard
import com.example.ui.components.TarazinoIconBadge
import com.example.ui.components.TarazinoMetalVaultCard
import com.example.ui.components.TarazinoMiniBrandCard
import com.example.ui.components.TarazinoQuickActionTile
import com.example.ui.components.TarazinoSectionHeader
import com.example.ui.components.TarazinoSlideToSettleBar
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // =========================================================================
        // 1. INTERACTIVE 3D-TILTABLE & 3D-FLIPPABLE PLATINUM VAULT CARD
        //    (Drag to tilt in 3D with live specular flare • Tap flip for Back Vault Stats)
        // =========================================================================
        item {
            val heroModes = listOf(
                uiState.dtiHealthReport?.let { "ماه ${it.currentMonthYearLabelFa.substringBefore(" ")}" } ?: "این ماه",
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

            // Dynamic CRED / Revolut Ultra Aura Morphing per Mode
            val cardAccentPrimary = when (heroModeIndex) {
                0 -> Color(0xFF00E5A0) // Velvet Electric Mint
                1 -> Color(0xFFE8C67D) // Brushed Champagne Gold
                else -> Color(0xFF22D3EE) // Arctic Sapphire
            }
            val cardAccentSecondary = when (heroModeIndex) {
                0 -> Color(0xFF38BDF8)
                1 -> Color(0xFFFF6B8B)
                else -> Color(0xFF818CF8)
            }
            val cardBaseStart = when (heroModeIndex) {
                0 -> Color(0xFF051C19)
                1 -> Color(0xFF1A140A)
                else -> Color(0xFF061628)
            }
            val cardBaseEnd = when (heroModeIndex) {
                0 -> Color(0xFF05080D)
                1 -> Color(0xFF0A0910)
                else -> Color(0xFF050A16)
            }

            val freedomDateShort = uiState.payoffSimulation?.projectedBaselineDateJalali?.let {
                JalaliCalendarEngine.formatJalaliShortText(it)
            } ?: "تسویه کامل!"
            val healthScore = uiState.dtiHealthReport?.obligationCoverageScore ?: 100
            val paidInstCount = uiState.installments.count { it.statusEnum == InstallmentStatus.PAID }

            TarazinoMetalVaultCard(
                accentPrimary = cardAccentPrimary,
                accentSecondary = cardAccentSecondary,
                baseStart = cardBaseStart,
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = cardAccentPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "شناسنامه خزانه و افق آزادی مالی",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }

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
                                        Text(
                                            text = "تاریخ آزادی از بدهی",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF9EB3C4)
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = freedomDateShort,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = cardAccentPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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
                                        Text(
                                            text = "امتیاز سلامت مالی",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF9EB3C4)
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "${PersianCurrencyEngine.toPersianDigits(healthScore.toString())} / ۱۰۰",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFE8C67D)
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
                                Text(
                                    text = "${PersianCurrencyEngine.toPersianDigits(uiState.obligations.size.toString())} پرونده فعال • ${PersianCurrencyEngine.toPersianDigits(paidInstCount.toString())} قسط تسویه‌شده",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFD5E1EC)
                                )
                                Text(
                                    text = "آفلاین و رمزنگاری‌شده",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = cardAccentPrimary
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
                    // Top Row: Mode Pill Switcher + 3D Flip Trigger + Smart Chip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.075f),
                            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.16f))
                        ) {
                            Row(
                                modifier = Modifier.padding(3.dp),
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
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(tabBg)
                                            .tarazinoPressScale(scaleDown = 0.93f) { heroModeIndex = idx }
                                            .padding(horizontal = 9.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                            color = if (selected) Color(0xFF031915) else Color(0xFFD5E1EC),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.09f),
                                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.22f)),
                                modifier = Modifier
                                    .size(30.dp)
                                    .tarazinoPressScale(scaleDown = 0.88f) {
                                        isVaultCardFlipped = true
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.FlipCameraAndroid,
                                        contentDescription = "چرخش سه‌بعدی پشت کارت",
                                        tint = Color.White.copy(alpha = 0.88f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            MetallicSmartChipEmblem(accentColor = cardAccentPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Focal Balance Row with Vertical Rolling Digit Animation + 60fps Liquid Wave Orb
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedContent(
                            targetState = Triple(displayedSubtitle, displayedAmount, cardAccentPrimary),
                            transitionSpec = {
                                (slideInVertically(tween(240)) { it / 4 } + fadeIn(tween(240))) togetherWith
                                    (slideOutVertically(tween(150)) { -it / 4 } + fadeOut(tween(150)))
                            },
                            label = "HeroBalanceCrossfade",
                            modifier = Modifier.weight(1f)
                        ) { (subtitleText, amt, accent) ->
                            Column {
                                Text(
                                    text = subtitleText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF9EB3C4)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                FormattedMoneyText(
                                    amountToman = amt,
                                    unit = unit,
                                    privacyMasked = masked,
                                    style = MaterialTheme.typography.displayLarge,
                                    numberColor = Color.White,
                                    unitColor = accent.copy(alpha = 0.95f)
                                )
                                if (!masked && amt > 0L) {
                                    Text(
                                        text = PersianCurrencyEngine.amountToPersianWords(amt, unit),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.68f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
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
                        Column {
                            Text(
                                text = "تسویه‌شده تا امروز",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF9EB3C4)
                            )
                            FormattedMoneyText(
                                amountToman = totalPaid,
                                unit = unit,
                                privacyMasked = masked,
                                style = MaterialTheme.typography.labelLarge,
                                numberColor = Color(0xFF00E5A0),
                                unitColor = Color(0xFFA7F3D0)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(26.dp)
                                .background(Color.White.copy(alpha = 0.14f))
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "کل بدهی مانده",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF9EB3C4)
                            )
                            FormattedMoneyText(
                                amountToman = totalRem,
                                unit = unit,
                                privacyMasked = masked,
                                style = MaterialTheme.typography.labelLarge,
                                numberColor = Color(0xFF38BDF8),
                                unitColor = Color(0xFFBAE6FD)
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 2. BLUBANK-STYLE 4-COLUMN SQUIRCLE QUICK ACTIONS STRIP
        // =========================================================================
        item {
            val mostUrgent = uiState.smartAlertQueue.firstOrNull()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TarazinoQuickActionTile(
                    label = "ثبت قسط",
                    icon = Icons.Default.AddCircle,
                    accentColor = Color(0xFF00E5A0),
                    onClick = onOpenNewObligationWizard,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_add_obligation_btn")
                )
                TarazinoQuickActionTile(
                    label = "تسویه فوری",
                    icon = Icons.Default.Bolt,
                    accentColor = Color(0xFFE8C67D),
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
                    accentColor = Color(0xFF22D3EE),
                    onClick = onOpenIntelligenceTab,
                    modifier = Modifier.weight(1f)
                )
                TarazinoQuickActionTile(
                    label = "گزارش A4",
                    icon = Icons.Default.PictureAsPdf,
                    accentColor = Color(0xFFA78BFA),
                    onClick = onOpenFullA4Report,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_open_a4_report_btn")
                )
            }
        }

        // =========================================================================
        // 2.2. APPLE WALLET × REVOLUT PHYSICAL MINI-CARD DECK (کیف کارت طرف‌های پرداخت)
        // =========================================================================
        if (uiState.counterparties.isNotEmpty()) {
            item {
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

        // =========================================================================
        // 2.5. INTERACTIVE TAP-TO-INSPECT 7-DAY JALALI CASHFLOW PULSE STRIP
        // =========================================================================
        item {
            WeeklyJalaliCashflowStrip(
                alerts = uiState.smartAlertQueue,
                currencyUnit = unit,
                privacyMasked = masked,
                onQuickPay = onQuickPay,
                onOpenCalendarTab = onOpenIntelligenceTab
            )
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
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Interactive Expanding Donut Ring
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(138.dp)
                                .clickable { selectedDonutCategory = null }
                        ) {
                            Canvas(modifier = Modifier.size(124.dp)) {
                                val baseStroke = 15.dp.toPx()
                                val selectedStroke = 21.dp.toPx()

                                drawArc(
                                    color = Color.Gray.copy(alpha = 0.13f),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
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
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = formatCompactPersianToman(focused.remainingToman, masked),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = focused.color
                                        )
                                        Text(
                                            text = "${PersianCurrencyEngine.toPersianDigits((focused.shareRatio * 100).toInt().toString())}٪",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = PersianCurrencyEngine.toPersianDigits(uiState.obligations.size.toString()),
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "پرونده فعال",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Crisp Interactive Category Legend List
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            categorySlices.forEach { slice ->
                                val pct = (slice.shareRatio * 100).toInt()
                                val isSel = selectedDonutCategory == slice.category
                                val borderW by animateDpAsState(
                                    targetValue = if (isSel) 1.5.dp else 0.8.dp,
                                    label = "SliceBorder_${slice.category.name}"
                                )
                                Surface(
                                    shape = RoundedCornerShape(13.dp),
                                    color = slice.color.copy(alpha = if (isSel) 0.19f else 0.08f),
                                    border = BorderStroke(
                                        borderW,
                                        slice.color.copy(alpha = if (isSel) 0.72f else 0.22f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(13.dp))
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
                                                modifier = Modifier
                                                    .size(9.dp)
                                                    .clip(CircleShape)
                                                    .background(slice.color)
                                            )
                                            Spacer(modifier = Modifier.width(7.dp))
                                            Column {
                                                Text(
                                                    text = slice.category.badgeFa,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (isSel) "لمس مجدد برای باز کردن" else slice.subProvidersPreviewFa,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isSel) slice.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${PersianCurrencyEngine.toPersianDigits(pct.toString())}٪",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = slice.color
                                            )
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                                contentDescription = null,
                                                tint = slice.color,
                                                modifier = Modifier.size(16.dp)
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

        // =========================================================================
        // 4. TOP 3 URGENT INSTALLMENTS RADAR WITH KINETIC SLIDE-TO-SETTLE BARS
        // =========================================================================
        item {
            TarazinoSectionHeader(
                title = "۳ سررسید فوری پیش‌رو",
                subtitle = "برای تسویه، دکمه سبز را به چپ بکشید یا لمس کنید",
                icon = Icons.Default.NotificationsActive,
                accentColor = MaterialTheme.colorScheme.secondary,
                trailing = if (uiState.smartAlertQueue.size > 3) {
                    {
                        Surface(
                            shape = RoundedCornerShape(11.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)),
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
                } else null
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

                        LinearProgressIndicator(
                            progress = { capacityRatio },
                            color = statusColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "نقدینگی آزاد: ${
                                    PersianCurrencyEngine.formatAmount(
                                        dti.freeCashflowToman,
                                        unit,
                                        privacyMasked = masked
                                    )
                                }",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "برنامه‌ریز بودجه و تقویم ‹",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
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
                        hasDue && isToday -> Color(0xFFFF4D67)
                        hasDue -> Color(0xFFF5B742)
                        else -> Color(0xFF00E5A0).copy(alpha = 0.40f)
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            hasDue -> Color(0xFFF5B742).copy(alpha = 0.13f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
                        },
                        border = BorderStroke(
                            width = if (isSelected || isToday) 1.2.dp else 0.7.dp,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                                hasDue -> Color(0xFFF5B742).copy(alpha = 0.45f)
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
                                            Text(
                                                text = a.installment.title,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = PersianCurrencyEngine.formatAmount(
                                                    a.installment.remainingToman,
                                                    currencyUnit,
                                                    privacyMasked = privacyMasked
                                                ),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                                color = Color(0xFF022019),
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
                    Column {
                        Text(
                            text = alert.installment.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${alert.counterparty?.name ?: "طرف‌حساب"} • ${PersianCurrencyEngine.toPersianDigits(alert.installment.dueDateJalali)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    FormattedMoneyText(
                        amountToman = alert.installment.remainingToman,
                        unit = currencyUnit,
                        privacyMasked = privacyMasked,
                        style = MaterialTheme.typography.titleSmall,
                        numberColor = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TarazinoStatusPill(
                            text = countdownTextFa,
                            accentColor = bucketColor
                        )
                        if (isCritical) {
                            TarazinoStatusPill(
                                text = "حیاتی",
                                accentColor = Color(0xFFFF4D67),
                                icon = Icons.Default.PriorityHigh
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(11.dp))

            // Kinetic CRED/Revolut Slide-to-Settle Track + Compact Snooze Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TarazinoSlideToSettleBar(
                    labelFa = "بکشید برای تسویه سریع",
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
            Column(modifier = Modifier.fillMaxSize()) {
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TarazinoIconBadge(
                                icon = Icons.Default.NotificationsActive,
                                accentColor = MaterialTheme.colorScheme.secondary,
                                size = 40.dp,
                                iconSize = 20.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "تمام سررسیدها و اقساط پیش‌رو",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "${PersianCurrencyEngine.toPersianDigits(alerts.size.toString())} قسط فعال در صف یادآوری",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                color = if (sel) Color(0xFF041C19) else bColor,
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
