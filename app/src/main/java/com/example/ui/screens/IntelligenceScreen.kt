package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.data.InstallmentEntity
import com.example.ui.TarazinoUiState
import com.example.ui.components.DebtAmortizationCalculatorSection
import com.example.ui.components.DebtSnowballCalculatorSection
import com.example.ui.components.FinancialHealthRadarChartCard
import com.example.ui.components.FormattedMoneyText
import com.example.ui.components.LiquidWaveHealthOrb
import com.example.ui.components.MlFinancialForecastSection
import com.example.ui.components.MoneyInputField
import com.example.ui.components.OfflineCurrencyDebtConverterSection
import com.example.ui.components.PersianInstallmentHeatmapSection
import com.example.ui.components.RecurringCashflowHealthSection
import com.example.ui.components.TarazinoAutoSizeText
import com.example.ui.components.TarazinoGlassCard
import com.example.ui.components.TarazinoIconBadge
import com.example.ui.components.TarazinoLuminousProgressBar
import com.example.ui.components.TarazinoSectionHeader
import com.example.ui.components.TarazinoStatusPill
import com.example.ui.components.formatCompactPersianToman
import com.example.ui.components.tarazinoPressScale
import com.example.ui.components.tourSpotlightTarget
import com.example.ui.theme.LocalTarazinoColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun IntelligenceScreen(
    uiState: TarazinoUiState,
    onUpdateExtraPayoffBudget: (Long) -> Unit,
    onChangeHeatmapMonth: (Int) -> Unit,
    onUpdateBudgetPlannerSettings: (monthlyIncomeToman: Long, livingExpensePercent: Int, alertDays: Int) -> Unit,
    onQuickPayInstallment: ((InstallmentEntity) -> Unit)? = null,
    tourScrollIndex: Int? = null
) {
    val unit = uiState.preferences.currencyUnit
    val masked = uiState.preferences.privacyMaskEnabled
    val ext = LocalTarazinoColors.current
    val listState = rememberLazyListState()

    var selectedForwardBarIndex by remember { mutableIntStateOf(0) }
    var showBudgetConfigModal by remember { mutableStateOf(false) }
    var selectedStrategyTab by remember { mutableIntStateOf(0) } // 0 = Avalanche, 1 = Snowball

    LaunchedEffect(tourScrollIndex) {
        if (tourScrollIndex != null && tourScrollIndex >= 0) {
            listState.animateScrollToItem(tourScrollIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("intelligence_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // =========================================================================
        // 1. MONTHLY BUDGET PLANNER & SAFE NEW INSTALLMENT CAPACITY
        // =========================================================================
        uiState.dtiHealthReport?.let { dti ->
            item {
                val hasCapacity = dti.safeNewInstallmentCapacityToman > 0L
                val capAccent = if (hasCapacity) ext.positive else ext.danger
                val statusColor = Color(dti.statusColorHex)

                val incomeSafe = dti.monthlyIncomeToman.coerceAtLeast(1L).toFloat()
                val livingFrac by animateFloatAsState(
                    targetValue = (dti.essentialLivingExpenseToman / incomeSafe).coerceIn(0f, 1f),
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "LivingBar"
                )
                val debtFrac by animateFloatAsState(
                    targetValue = (dti.currentMonthDebtDueToman / incomeSafe).coerceIn(0f, (1f - livingFrac).coerceAtLeast(0f)),
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "DebtBar"
                )
                val safeCapFrac = (1f - livingFrac - debtFrac).coerceIn(0f, 1f)

                TarazinoGlassCard(
                    modifier = Modifier
                        .animateContentSize()
                        .tourSpotlightTarget("intel_budget_planner")
                        .testTag("intel_budget_planner_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        TarazinoSectionHeader(
                            title = "برنامه‌ریز بودجه و سقف قسط جدید",
                            subtitle = "${dti.currentMonthYearLabelFa} • ${dti.statusBadgeFa}",
                            icon = Icons.Default.AccountBalanceWallet,
                            accentColor = MaterialTheme.colorScheme.primary,
                            trailing = {
                                OutlinedButton(
                                    onClick = { showBudgetConfigModal = true },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("edit_budget_planner_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "تنظیم درآمد و بودجه",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "تنظیم بودجه",
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Signature 220-Degree Semi-Circular Financial Health Speedometer Gauge
                        FinancialHealthSpeedometerGauge(
                            score = dti.obligationCoverageScore,
                            statusBadgeFa = dti.statusBadgeFa,
                            dtiPercent = dti.dtiPercent,
                            statusColor = statusColor
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Highlight Capacity Spotlight Panel with 60fps Liquid Wave Orb
                        val safeCapPct = if (dti.safeMaxTotalDebtBudgetToman > 0L) {
                            ((dti.safeNewInstallmentCapacityToman.toFloat() / dti.safeMaxTotalDebtBudgetToman.toFloat()) * 100f)
                                .toInt().coerceIn(0, 100)
                        } else 0
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = capAccent.copy(alpha = 0.11f),
                            border = BorderStroke(1.dp, capAccent.copy(alpha = 0.38f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        TarazinoAutoSizeText(
                                            text = "سقف امن قسط جدید",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            minFontSize = 10.sp,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        TarazinoStatusPill(
                                            text = if (hasCapacity) "ظرفیت باز" else "تکمیل",
                                            accentColor = capAccent
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    FormattedMoneyText(
                                        amountToman = dti.safeNewInstallmentCapacityToman,
                                        unit = unit,
                                        privacyMasked = masked,
                                        style = MaterialTheme.typography.headlineMedium,
                                        numberColor = capAccent,
                                        unitColor = capAccent.copy(alpha = 0.8f),
                                        minFontSize = 13.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    if (!masked && dti.safeNewInstallmentCapacityToman > 0L) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                        TarazinoAutoSizeText(
                                            text = PersianCurrencyEngine.amountToPersianWords(
                                                dti.safeNewInstallmentCapacityToman,
                                                unit
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            minFontSize = 8.5.sp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                LiquidWaveHealthOrb(
                                    progress = (safeCapPct / 100f).coerceIn(0.06f, 1f),
                                    accentColor = capAccent,
                                    centerPrimaryText = "${PersianCurrencyEngine.toPersianDigits(safeCapPct.toString())}٪",
                                    centerSubText = "ظرفیت",
                                    size = 60.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Visual Stacked Horizontal Budget Allocation Bar
                        Text(
                            text = "نوار تفکیک بودجه ماهانه:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                        ) {
                            val totalW = size.width
                            val livingW = totalW * livingFrac
                            val debtW = totalW * debtFrac
                            val capW = (totalW - livingW - debtW).coerceAtLeast(0f)

                            // Draw right-to-left visually for Persian readers
                            drawRect(
                                color = ext.info,
                                topLeft = Offset(totalW - livingW, 0f),
                                size = Size(livingW, size.height)
                            )
                            drawRect(
                                color = statusColor,
                                topLeft = Offset(totalW - livingW - debtW, 0f),
                                size = Size(debtW, size.height)
                            )
                            if (capW > 0f) {
                                drawRect(
                                    color = ext.positive,
                                    topLeft = Offset(0f, 0f),
                                    size = Size(capW, size.height)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Spacious 2x2 Budget Metrics Grid (Zero truncation on compact phones)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricMiniCard(
                                    label = "درآمد ماهانه",
                                    value = PersianCurrencyEngine.formatAmount(dti.monthlyIncomeToman, unit, privacyMasked = masked),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    dotColor = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricMiniCard(
                                    label = "معیشت (${PersianCurrencyEngine.toPersianDigits(dti.livingExpensePercent.toString())}٪)",
                                    value = PersianCurrencyEngine.formatAmount(dti.essentialLivingExpenseToman, unit, privacyMasked = masked),
                                    color = ext.info,
                                    dotColor = ext.info,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricMiniCard(
                                    label = "اقساط این ماه (${PersianCurrencyEngine.toPersianDigits(dti.dtiPercent.toString())}٪)",
                                    value = PersianCurrencyEngine.formatAmount(dti.currentMonthDebtDueToman, unit, privacyMasked = masked),
                                    color = statusColor,
                                    dotColor = statusColor,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricMiniCard(
                                    label = "حاشیه امن (${PersianCurrencyEngine.toPersianDigits((safeCapFrac * 100f).roundToInt().toString())}٪)",
                                    value = PersianCurrencyEngine.formatAmount(dti.safeNewInstallmentCapacityToman, unit, privacyMasked = masked),
                                    color = capAccent,
                                    dotColor = capAccent,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Peak Crunch Windows (دهه‌های اوج فشار مالی ماه شمسی)
                        Text(
                            text = "توزیع فشار اقساط در دهه‌های ماه شمسی:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        dti.decadeSlices.forEach { slice ->
                            val isPeak = slice.decadeIndex == dti.peakDecadeIndex && slice.amountToman > 0L
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        TarazinoAutoSizeText(
                                            text = "${slice.labelFa} (${slice.rangeLabelFa})",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isPeak) ext.warning else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            minFontSize = 9.sp,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (isPeak) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            TarazinoStatusPill(
                                                text = "اوج فشار",
                                                accentColor = ext.warning
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TarazinoAutoSizeText(
                                        text = "${PersianCurrencyEngine.formatAmount(slice.amountToman, unit, privacyMasked = masked)} (${
                                            PersianCurrencyEngine.toPersianDigits(slice.sharePercent.toString())
                                        }٪)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.End,
                                        maxLines = 1,
                                        minFontSize = 8.5.sp,
                                        modifier = Modifier.widthIn(max = 160.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(5.dp))
                                TarazinoLuminousProgressBar(
                                    progress = (slice.sharePercent / 100f).coerceIn(0f, 1f),
                                    color = if (isPeak) ext.warning else MaterialTheme.colorScheme.primary,
                                    height = 7.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Smart Heuristic Persian Advisories
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            border = BorderStroke(
                                0.8.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = ext.warning,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "توصیه‌های موتور تصمیم‌یار ترازینو:",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                dti.smartPersianAdvisories.forEach { tip ->
                                    Text(
                                        text = "• $tip",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 1A. D3 FINANCIAL HEALTH RADAR (DEBT CATEGORIES & MONTHLY BUDGET CAPACITY IMPACT)
        // =========================================================================
        item {
            FinancialHealthRadarChartCard(
                obligations = uiState.obligations,
                installments = uiState.installments,
                counterparties = uiState.counterparties,
                monthlyIncomeToman = uiState.preferences.monthlyIncomeToman,
                livingExpensePercent = uiState.preferences.livingExpensePercent,
                currencyUnit = unit,
                privacyMasked = masked
            )
        }

        // =========================================================================
        // 1B. MACHINE LEARNING FINANCIAL HEALTH FORECASTING & DEFICIT WARNINGS
        // =========================================================================
        uiState.mlForecastReport?.let { mlReport ->
            item {
                MlFinancialForecastSection(
                    baselineReport = mlReport,
                    installments = uiState.installments,
                    obligations = uiState.obligations,
                    monthlyIncomeToman = uiState.preferences.monthlyIncomeToman,
                    livingExpensePercent = uiState.preferences.livingExpensePercent,
                    unit = unit,
                    privacyMasked = masked
                )
            }
        }

        // =========================================================================
        // 1.9. RECURRING MONTHLY INCOME & NON-LOAN EXPENSE TRACKER (Holistic Financial Health)
        // =========================================================================
        item {
            RecurringCashflowHealthSection(
                currencyUnit = unit,
                privacyMasked = masked,
                currentMonthLoanInstallmentsToman = uiState.dtiHealthReport?.currentMonthDebtDueToman ?: 0L,
                monthlySavingsTargetToman = uiState.preferences.monthlySavingsTargetToman,
                onSyncWithBudgetPlanner = { totalIncomeToman, livingExpensePercent ->
                    onUpdateBudgetPlannerSettings(
                        totalIncomeToman,
                        livingExpensePercent,
                        uiState.preferences.alertAdvanceDays
                    )
                }
            )
        }

        // =========================================================================
        // 2. INTERACTIVE PERSIAN (SHAMSI) INSTALLMENT INTENSITY CALENDAR HEATMAP
        //    (5-Tier Thermal Heat Scale, High-Pressure Period Detection & Decade Strips)
        // =========================================================================
        item {
            PersianInstallmentHeatmapSection(
                heatmapYear = uiState.heatmapYear,
                heatmapMonth = uiState.heatmapMonth,
                installments = uiState.installments,
                obligations = uiState.obligations,
                counterparties = uiState.counterparties,
                monthlyIncomeToman = uiState.preferences.monthlyIncomeToman,
                currencyUnit = unit,
                privacyMasked = masked,
                onChangeHeatmapMonth = onChangeHeatmapMonth,
                onQuickPay = onQuickPayInstallment
            )
        }

        // =========================================================================
        // 3. INTERACTIVE DIRECT-TAP 12-MONTH FORWARD INSTALLMENT LOAD BAR CHART
        // =========================================================================
        if (uiState.forward12MonthBars.isNotEmpty()) {
            item {
                val safeBarIdx = selectedForwardBarIndex.coerceIn(0, uiState.forward12MonthBars.lastIndex)
                val focusedBar = uiState.forward12MonthBars[safeBarIdx]

                var chartEntered by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { chartEntered = true }
                val barEntrance by animateFloatAsState(
                    targetValue = if (chartEntered) 1f else 0f,
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessVeryLow),
                    label = "ForwardBarEntrance"
                )

                TarazinoGlassCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        TarazinoSectionHeader(
                            title = "چشم‌انداز بار اقساط ۱۲ ماه آینده",
                            subtitle = "مستقیماً روی هر ستون نمودار یا نام ماه لمس کنید",
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            accentColor = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Focused Month Readout Card
                        Surface(
                            shape = RoundedCornerShape(15.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(0.9.dp, ext.cardBorderTop),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 13.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    TarazinoAutoSizeText(
                                        text = "ماه ${focusedBar.monthLabelFa}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        minFontSize = 10.sp
                                    )
                                    val focusedDisplayTotal = if (unit == CurrencyUnit.RIAL) focusedBar.totalDebtToman * 10L else focusedBar.totalDebtToman
                                    TarazinoAutoSizeText(
                                        text = "کل بار ماه: ${formatCompactPersianToman(focusedDisplayTotal, masked)} ${if (masked) "" else unit.labelFa}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        minFontSize = 8.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "مانده",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                        TarazinoAutoSizeText(
                                            text = formatCompactPersianToman(
                                                if (unit == CurrencyUnit.RIAL) focusedBar.unpaidToman * 10L else focusedBar.unpaidToman,
                                                masked
                                            ),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = ext.warning,
                                            maxLines = 1,
                                            minFontSize = 9.5.sp
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "پرداخت‌شده",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                        TarazinoAutoSizeText(
                                            text = formatCompactPersianToman(
                                                if (unit == CurrencyUnit.RIAL) focusedBar.paidToman * 10L else focusedBar.paidToman,
                                                masked
                                            ),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = ext.positive,
                                            maxLines = 1,
                                            minFontSize = 9.5.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val maxMonthly = uiState.forward12MonthBars.maxOfOrNull { it.totalDebtToman }
                            ?.coerceAtLeast(1L) ?: 1L
                        val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        val primaryHighlight = MaterialTheme.colorScheme.primary

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(164.dp)
                                .pointerInput(uiState.forward12MonthBars) {
                                    detectTapGestures { offset ->
                                        val count = uiState.forward12MonthBars.size
                                        if (count > 0 && size.width > 0) {
                                            val slotW = size.width.toFloat() / count.toFloat()
                                            // RTL mapping: index 0 is on the far right
                                            val rtlIdx = ((size.width - offset.x) / slotW)
                                                .toInt()
                                                .coerceIn(0, count - 1)
                                            selectedForwardBarIndex = rtlIdx
                                        }
                                    }
                                }
                        ) {
                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            for (lineIdx in 0..3) {
                                val y = size.height * (lineIdx / 3f)
                                drawLine(
                                    color = gridColor,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = dashEffect
                                )
                            }

                            val barCount = uiState.forward12MonthBars.size
                            val slotWidth = size.width / barCount
                            val barWidth = (slotWidth * 0.54f).coerceAtMost(22.dp.toPx())

                            uiState.forward12MonthBars.forEachIndexed { idx, bar ->
                                val isFocused = idx == safeBarIdx
                                val slotLeft = size.width - ((idx + 1) * slotWidth)
                                val x = slotLeft + (slotWidth - barWidth) / 2f

                                if (isFocused) {
                                    drawRoundRect(
                                        color = primaryHighlight.copy(alpha = 0.12f),
                                        topLeft = Offset(slotLeft + 2.dp.toPx(), 0f),
                                        size = Size((slotWidth - 4.dp.toPx()).coerceAtLeast(barWidth), size.height),
                                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                                    )
                                }

                                val totalRatio = (bar.totalDebtToman.toFloat() / maxMonthly.toFloat()).coerceIn(0.05f, 1f)
                                val paidRatio = (bar.paidToman.toFloat() / maxMonthly.toFloat()).coerceIn(0f, totalRatio)

                                val totalH = size.height * totalRatio * barEntrance
                                val paidH = size.height * paidRatio * barEntrance

                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        if (isFocused) {
                                            listOf(ext.warning, ext.warning.copy(alpha = 0.80f))
                                        } else {
                                            listOf(
                                                ext.warning.copy(alpha = 0.62f),
                                                ext.warning.copy(alpha = 0.28f)
                                            )
                                        }
                                    ),
                                    topLeft = Offset(x, size.height - totalH),
                                    size = Size(barWidth, totalH),
                                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                                )
                                if (paidH > 0f) {
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            if (isFocused) {
                                                listOf(ext.positive, ext.positive.copy(alpha = 0.80f))
                                            } else {
                                                listOf(ext.positive.copy(alpha = 0.78f), ext.positive.copy(alpha = 0.48f))
                                            }
                                        ),
                                        topLeft = Offset(x, size.height - paidH),
                                        size = Size(barWidth, paidH),
                                        cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Interactive Compact Horizontal Month Selector Pills
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            uiState.forward12MonthBars.forEachIndexed { idx, bar ->
                                val isSel = idx == safeBarIdx
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSel) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    },
                                    modifier = Modifier.tarazinoPressScale(
                                        scaleDown = 0.94f,
                                        onClick = { selectedForwardBarIndex = idx }
                                    )
                                ) {
                                    Text(
                                        text = bar.monthLabelFa,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSel) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 4. INTERACTIVE EARLY PAYOFF SIMULATOR (AVALANCHE VS. SNOWBALL)
        // =========================================================================
        uiState.payoffSimulation?.let { sim ->
            item {
                TarazinoGlassCard(
                    modifier = Modifier
                        .tourSpotlightTarget("intel_payoff_simulator")
                        .testTag("intel_payoff_simulator_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        TarazinoSectionHeader(
                            title = "شبیه‌ساز تسویه زودهنگام بدهی‌ها",
                            subtitle = "مقایسه استراتژی بهمن (حداقل هزینه) و گلوله برفی (حذف سریع)",
                            icon = Icons.Default.RocketLaunch,
                            accentColor = MaterialTheme.colorScheme.secondary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "بودجه مازاد ماهانه:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            FormattedMoneyText(
                                amountToman = uiState.extraPayoffBudgetToman,
                                unit = unit,
                                privacyMasked = masked,
                                style = MaterialTheme.typography.titleSmall,
                                numberColor = MaterialTheme.colorScheme.primary
                            )
                        }

                        var sliderValue by remember(uiState.extraPayoffBudgetToman) {
                            mutableFloatStateOf(uiState.extraPayoffBudgetToman.toFloat())
                        }
                        Slider(
                            value = sliderValue,
                            onValueChange = {
                                sliderValue = it
                                val stepped = ((it.toLong() / 500_000L) * 500_000L)
                                onUpdateExtraPayoffBudget(stepped)
                            },
                            valueRange = 0f..25_000_000f,
                            steps = 49,
                            modifier = Modifier.testTag("payoff_extra_budget_slider")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricMiniCard(
                                label = "زمان تسویه عادی",
                                value = "${PersianCurrencyEngine.toPersianDigits(sim.baselineMonthsToClear.toString())} ماه",
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            MetricMiniCard(
                                label = "با بودجه مازاد",
                                value = "${PersianCurrencyEngine.toPersianDigits(sim.avalancheMonthsToClear.toString())} ماه",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            MetricMiniCard(
                                label = "صرفه‌جویی زمانی",
                                value = "${PersianCurrencyEngine.toPersianDigits(sim.monthsSavedAvalanche.toString())} ماه زودتر!",
                                color = ext.warning,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                            border = BorderStroke(
                                0.8.dp,
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "تاریخ دقیق شمسی آزادی کامل از بدهی: «${
                                        JalaliCalendarEngine.formatJalaliVerbose(sim.projectedAcceleratedDateJalali)
                                    }»",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = sim.recommendationFa,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Symmetrical 2-Column Segmented Strategy Selector
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                StrategySegmentButton(
                                    selected = selectedStrategyTab == 0,
                                    titleFa = "استراتژی بهمن",
                                    subtitleFa = "حداقل ریسک و هزینه",
                                    icon = Icons.Default.Shield,
                                    accentColor = ext.warning,
                                    onClick = { selectedStrategyTab = 0 },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("strategy_segment_avalanche")
                                )
                                StrategySegmentButton(
                                    selected = selectedStrategyTab == 1,
                                    titleFa = "استراتژی گلوله برفی",
                                    subtitleFa = "حذف سریع خرده‌بدهی",
                                    icon = Icons.Default.Speed,
                                    accentColor = ext.positive,
                                    onClick = { selectedStrategyTab = 1 },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("strategy_segment_snowball")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val targetList = if (selectedStrategyTab == 0) sim.avalancheOrder else sim.snowballOrder
                        targetList.forEachIndexed { idx, step ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text(
                                                text = PersianCurrencyEngine.toPersianDigits((idx + 1).toString()),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            TarazinoAutoSizeText(
                                                text = step.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                minFontSize = 10.sp
                                            )
                                            TarazinoAutoSizeText(
                                                text = "${step.counterpartyName} • تسویه کامل در ماه ${
                                                    PersianCurrencyEngine.toPersianDigits(step.estimatedClearMonthIndex.toString())
                                                }",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                minFontSize = 8.5.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    FormattedMoneyText(
                                        amountToman = step.remainingToman,
                                        unit = unit,
                                        privacyMasked = masked,
                                        style = MaterialTheme.typography.labelLarge,
                                        numberColor = MaterialTheme.colorScheme.onSurface,
                                        minFontSize = 9.sp,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.widthIn(max = 148.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 5. DEDICATED DEBT SNOWBALL CALCULATOR MODULE (ماشین‌حساب استراتژی گلوله برفی)
        // =========================================================================
        item {
            DebtSnowballCalculatorSection(
                obligations = uiState.obligations,
                installments = uiState.installments,
                counterparties = uiState.counterparties,
                initialExtraBudgetToman = uiState.extraPayoffBudgetToman,
                currencyUnit = unit,
                privacyMasked = masked
            )
        }

        // =========================================================================
        // 6. SHAMSI DEBT AMORTIZATION CALCULATOR (ماشین‌حساب استهلاک وام و اقساط شمسی)
        // =========================================================================
        item {
            DebtAmortizationCalculatorSection(
                currencyUnit = unit,
                privacyMasked = masked
            )
        }

        // =========================================================================
        // 7. OFFLINE MULTI-CURRENCY DEBT CONVERTER & TRACKER (مبدل آفلاین ارز و بدهی چندارزی)
        // =========================================================================
        item {
            OfflineCurrencyDebtConverterSection(
                currencyUnit = unit,
                privacyMasked = masked,
                domesticRemainingDebtToman = uiState.totalDebtRemainingToman
            )
        }
    }

    if (showBudgetConfigModal) {
        var newIncome by remember { mutableLongStateOf(uiState.preferences.monthlyIncomeToman) }
        var newLivingPct by remember { mutableFloatStateOf(uiState.preferences.livingExpensePercent.toFloat()) }
        var newAlertDays by remember { mutableFloatStateOf(uiState.preferences.alertAdvanceDays.toFloat()) }

        Dialog(
            onDismissRequest = { showBudgetConfigModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            TarazinoGlassCard(
                cornerRadius = 28.dp,
                accentBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .imePadding()
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TarazinoIconBadge(
                            icon = Icons.Default.AccountBalanceWallet,
                            accentColor = MaterialTheme.colorScheme.primary,
                            size = 42.dp,
                            iconSize = 21.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "تنظیم بودجه ماهانه",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "محاسبه دقیق سقف قسط جدید و امتیاز سلامت",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    MoneyInputField(
                        labelFa = "درآمد ماهانه خالص شما",
                        amountToman = newIncome,
                        currencyUnit = unit,
                        onAmountChange = { newIncome = it }
                    )

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "سهم مخارج ضروری زندگی",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${PersianCurrencyEngine.toPersianDigits(newLivingPct.roundToInt().toString())}٪",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        TarazinoAutoSizeText(
                            text = PersianCurrencyEngine.formatAmount(
                                (newIncome * newLivingPct.roundToInt()) / 100L,
                                unit
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            minFontSize = 8.5.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Slider(
                            value = newLivingPct,
                            onValueChange = { newLivingPct = it },
                            valueRange = 25f..75f,
                            steps = 9
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "بازه هشدار پیش از سررسید",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${PersianCurrencyEngine.toPersianDigits(newAlertDays.roundToInt().toString())} روز",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = ext.warning
                            )
                        }
                        Slider(
                            value = newAlertDays,
                            onValueChange = { newAlertDays = it },
                            valueRange = 1f..20f,
                            steps = 18
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showBudgetConfigModal = false },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("انصراف", fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                        Button(
                            onClick = {
                                onUpdateBudgetPlannerSettings(
                                    newIncome,
                                    newLivingPct.roundToInt(),
                                    newAlertDays.roundToInt()
                                )
                                showBudgetConfigModal = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1.4f)
                        ) {
                            Text("ذخیره بودجه", fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinancialHealthSpeedometerGauge(
    score: Int,
    statusBadgeFa: String,
    dtiPercent: Int,
    statusColor: Color
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(score) { entered = true }

    val animatedFraction by animateFloatAsState(
        targetValue = if (entered) (score / 100f).coerceIn(0f, 1f) else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessVeryLow),
        label = "HealthSpeedometerSweep"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxWidth()
            .height(206.dp)
    ) {
        Canvas(
            modifier = Modifier
                .size(width = 210.dp, height = 168.dp)
                .padding(top = 6.dp)
        ) {
            val strokePx = 14.dp.toPx()
            val auraExtraPx = 8.dp.toPx()
            val safeInset = (strokePx + auraExtraPx) / 2f
            val arcDiameter = (minOf(size.width, size.height * 1.15f) - safeInset * 2f).coerceAtLeast(80f)
            val topLeft = Offset((size.width - arcDiameter) / 2f, safeInset)
            val arcSize = Size(arcDiameter, arcDiameter)
            val startAngle = 160f
            val totalSweep = 220f
            val activeSweep = totalSweep * animatedFraction

            // Background subtle track
            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Soft outer aura glow
            if (activeSweep > 2f) {
                drawArc(
                    color = statusColor.copy(alpha = 0.22f),
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx + auraExtraPx, cap = StrokeCap.Round)
                )

                // Active gradient arc
                drawArc(
                    brush = Brush.horizontalGradient(
                        listOf(
                            statusColor.copy(alpha = 0.65f),
                            statusColor,
                            statusColor.copy(alpha = 0.95f)
                        )
                    ),
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                // Glowing endpoint indicator dot
                val endRad = ((startAngle + activeSweep) * PI / 180.0)
                val radius = arcDiameter / 2f
                val cx = topLeft.x + radius
                val cy = topLeft.y + radius
                val tipX = cx + (radius * cos(endRad)).toFloat()
                val tipY = cy + (radius * sin(endRad)).toFloat()

                drawCircle(
                    color = Color.White,
                    radius = 5.dp.toPx(),
                    center = Offset(tipX, tipY)
                )
                drawCircle(
                    color = statusColor,
                    radius = 2.8.dp.toPx(),
                    center = Offset(tipX, tipY)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp)
        ) {
            Text(
                text = "شاخص سلامت مالی",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = PersianCurrencyEngine.toPersianDigits(score.toString()),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = statusColor,
                    maxLines = 1
                )
                Text(
                    text = "/ ۱۰۰",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            TarazinoStatusPill(
                text = "$statusBadgeFa • DTI ${PersianCurrencyEngine.toPersianDigits(dtiPercent.toString())}٪",
                accentColor = statusColor
            )
        }
    }
}

@Composable
private fun LegendDotLabel(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StrategySegmentButton(
    selected: Boolean,
    titleFa: String,
    subtitleFa: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.18f) else Color.Transparent,
        label = "SegmentBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
        label = "SegmentBorder"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        modifier = modifier
            .border(
                width = if (selected) 1.4.dp else 0.85.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (selected) Icons.Default.CheckCircle else icon,
                    contentDescription = null,
                    tint = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                TarazinoAutoSizeText(
                    text = titleFa,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) accentColor else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    minFontSize = 9.5.sp,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            TarazinoAutoSizeText(
                text = subtitleFa,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                minFontSize = 8.5.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MetricMiniCard(
    label: String,
    value: String,
    color: Color,
    dotColor: Color? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .border(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(13.dp))
            .padding(horizontal = 9.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            TarazinoAutoSizeText(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                minFontSize = 8.sp,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        TarazinoAutoSizeText(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            minFontSize = 8.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
