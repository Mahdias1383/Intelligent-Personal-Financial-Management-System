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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.CurrencyUnit
import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.domain.CalendarHeatmapDay
import com.example.ui.TarazinoUiState
import com.example.ui.components.FormattedMoneyText
import com.example.ui.components.LiquidWaveHealthOrb
import com.example.ui.components.MoneyInputField
import com.example.ui.components.TarazinoGlassCard
import com.example.ui.components.TarazinoIconBadge
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
    tourScrollIndex: Int? = null
) {
    val unit = uiState.preferences.currencyUnit
    val masked = uiState.preferences.privacyMaskEnabled
    val listState = rememberLazyListState()

    var selectedHeatmapDay by remember { mutableStateOf<CalendarHeatmapDay?>(null) }
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
                val capAccent = if (hasCapacity) Color(0xFF10B981) else Color(0xFFEF4444)
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
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "سقف امن قسط جدید ماهانه",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
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
                                        unitColor = capAccent.copy(alpha = 0.8f)
                                    )
                                    if (!masked && dti.safeNewInstallmentCapacityToman > 0L) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = PersianCurrencyEngine.amountToPersianWords(
                                                dti.safeNewInstallmentCapacityToman,
                                                unit
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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
                                color = Color(0xFF38BDF8),
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
                                    color = Color(0xFF10B981),
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
                                    color = Color(0xFF38BDF8),
                                    dotColor = Color(0xFF38BDF8),
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${slice.labelFa} (${slice.rangeLabelFa})",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isPeak) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isPeak) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            TarazinoStatusPill(
                                                text = "اوج فشار",
                                                accentColor = Color(0xFFF59E0B)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${PersianCurrencyEngine.formatAmount(slice.amountToman, unit, privacyMasked = masked)} (${
                                            PersianCurrencyEngine.toPersianDigits(slice.sharePercent.toString())
                                        }٪)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(5.dp))
                                LinearProgressIndicator(
                                    progress = { (slice.sharePercent / 100f).coerceIn(0f, 1f) },
                                    color = if (isPeak) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(4.dp))
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
                                        tint = Color(0xFFF59E0B),
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
        // 2. INTERACTIVE JALALI MONTHLY CALENDAR HEATMAP
        // =========================================================================
        item {
            TarazinoGlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    TarazinoSectionHeader(
                        title = "تقویم حرارتی سررسیدها (شمسی)",
                        subtitle = "برای مشاهده جزئیات اقساط هر روز، روی خانه آن روز بزنید",
                        icon = Icons.Default.CalendarMonth,
                        accentColor = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Month Stepper Bar
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onChangeHeatmapMonth(-1) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "ماه قبل"
                                )
                            }
                            Text(
                                text = "${JalaliCalendarEngine.getMonthName(uiState.heatmapMonth)} ${
                                    PersianCurrencyEngine.toPersianDigits(uiState.heatmapYear.toString())
                                }",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { onChangeHeatmapMonth(1) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "ماه بعد"
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Compact 4-Dot Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendDotLabel("پرفشار", Color(0xFFEF4444))
                        LegendDotLabel("دارای قسط", Color(0xFFF59E0B))
                        LegendDotLabel("طلب", Color(0xFF06B6D4))
                        LegendDotLabel("تسویه‌شده", Color(0xFF10B981))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 7-Column Weekday Header
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (w in 0..6) {
                            Text(
                                text = JalaliCalendarEngine.WEEKDAY_SHORT[w],
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (w == 6) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val firstDayStr = "%04d/%02d/01".format(
                        java.util.Locale.US,
                        uiState.heatmapYear,
                        uiState.heatmapMonth
                    )
                    val firstDayOffset = JalaliCalendarEngine.getPersianWeekdayIndex(firstDayStr)
                    val totalSlots = firstDayOffset + uiState.heatmapDays.size
                    val rowCount = (totalSlots + 6) / 7

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (r in 0 until rowCount) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (c in 0..6) {
                                    val slot = r * 7 + c
                                    val dayIdx = slot - firstDayOffset
                                    if (dayIdx in uiState.heatmapDays.indices) {
                                        val dayItem = uiState.heatmapDays[dayIdx]
                                        val hasUnpaid = dayItem.unpaidDebtToman > 0L
                                        val hasPaid = dayItem.paidDebtToman > 0L
                                        val hasRec = dayItem.receivableToman > 0L
                                        val isSelectedDay = selectedHeatmapDay?.dateJalali == dayItem.dateJalali

                                        val cellColor = when {
                                            dayItem.unpaidDebtToman >= 10_000_000L -> Color(0xFFEF4444).copy(alpha = 0.28f)
                                            hasUnpaid -> Color(0xFFF59E0B).copy(alpha = 0.24f)
                                            hasRec -> Color(0xFF06B6D4).copy(alpha = 0.24f)
                                            hasPaid -> Color(0xFF10B981).copy(alpha = 0.20f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        }

                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(11.dp))
                                                .background(cellColor)
                                                .then(
                                                    when {
                                                        isSelectedDay -> Modifier.border(
                                                            2.dp,
                                                            MaterialTheme.colorScheme.secondary,
                                                            RoundedCornerShape(11.dp)
                                                        )
                                                        dayItem.isToday -> Modifier.border(
                                                            1.5.dp,
                                                            MaterialTheme.colorScheme.primary,
                                                            RoundedCornerShape(11.dp)
                                                        )
                                                        else -> Modifier
                                                    }
                                                )
                                                .clickable {
                                                    if (dayItem.items.isNotEmpty()) {
                                                        selectedHeatmapDay = dayItem
                                                    }
                                                }
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = PersianCurrencyEngine.toPersianDigits(dayItem.dayOfMonth.toString()),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = if (dayItem.items.isNotEmpty() || dayItem.isToday) FontWeight.Bold else FontWeight.Normal
                                                )
                                                if (dayItem.items.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                when {
                                                                    hasUnpaid -> Color(0xFFEF4444)
                                                                    hasRec -> Color(0xFF06B6D4)
                                                                    else -> Color(0xFF10B981)
                                                                }
                                                            )
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = selectedHeatmapDay != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        selectedHeatmapDay?.let { sel ->
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "تعهدات روز ${JalaliCalendarEngine.formatJalaliVerbose(sel.dateJalali)}:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                sel.items.forEach { alert ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${alert.installment.title} (${alert.counterparty?.name ?: ""})",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            FormattedMoneyText(
                                                amountToman = alert.installment.remainingToman,
                                                unit = unit,
                                                privacyMasked = masked,
                                                style = MaterialTheme.typography.labelLarge,
                                                numberColor = MaterialTheme.colorScheme.primary
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
        // 3. INTERACTIVE DIRECT-TAP 12-MONTH FORWARD INSTALLMENT LOAD BAR CHART
        // =========================================================================
        if (uiState.forward12MonthBars.isNotEmpty()) {
            item {
                val ext = LocalTarazinoColors.current
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
                                Column {
                                    Text(
                                        text = "ماه ${focusedBar.monthLabelFa}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    val focusedDisplayTotal = if (unit == CurrencyUnit.RIAL) focusedBar.totalDebtToman * 10L else focusedBar.totalDebtToman
                                    Text(
                                        text = "کل بار ماه: ${formatCompactPersianToman(focusedDisplayTotal, masked)} ${if (masked) "" else unit.labelFa}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "مانده",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formatCompactPersianToman(
                                                if (unit == CurrencyUnit.RIAL) focusedBar.unpaidToman * 10L else focusedBar.unpaidToman,
                                                masked
                                            ),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFF59E0B)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "پرداخت‌شده",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formatCompactPersianToman(
                                                if (unit == CurrencyUnit.RIAL) focusedBar.paidToman * 10L else focusedBar.paidToman,
                                                masked
                                            ),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF10B981)
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
                                            listOf(Color(0xFFFCD34D), Color(0xFFF59E0B))
                                        } else {
                                            listOf(
                                                Color(0xFFF59E0B).copy(alpha = 0.62f),
                                                Color(0xFFF59E0B).copy(alpha = 0.28f)
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
                                                listOf(Color(0xFF34D399), Color(0xFF059669))
                                            } else {
                                                listOf(Color(0xFF10B981), Color(0xFF047857))
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
                                color = Color(0xFFF59E0B),
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
                                    accentColor = Color(0xFFF59E0B),
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
                                    accentColor = Color(0xFF10B981),
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
                                        Column {
                                            Text(
                                                text = step.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${step.counterpartyName} • تسویه کامل در ماه ${
                                                    PersianCurrencyEngine.toPersianDigits(step.estimatedClearMonthIndex.toString())
                                                }",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    FormattedMoneyText(
                                        amountToman = step.remainingToman,
                                        unit = unit,
                                        privacyMasked = masked,
                                        style = MaterialTheme.typography.labelLarge,
                                        numberColor = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
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
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
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
                        Text(
                            text = PersianCurrencyEngine.formatAmount(
                                (newIncome * newLivingPct.roundToInt()) / 100L,
                                unit
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                color = Color(0xFFF59E0B)
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
                            Text("انصراف", fontWeight = FontWeight.SemiBold)
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
                            Text("ذخیره بودجه", fontWeight = FontWeight.Bold)
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
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(172.dp)
    ) {
        Canvas(
            modifier = Modifier
                .size(210.dp)
                .padding(top = 8.dp)
        ) {
            val strokePx = 15.dp.toPx()
            val arcDiameter = size.minDimension - strokePx
            val topLeft = Offset((size.width - arcDiameter) / 2f, strokePx / 2f)
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
                    style = Stroke(width = strokePx + 9.dp.toPx(), cap = StrokeCap.Round)
                )

                // Active gradient arc
                drawArc(
                    brush = Brush.horizontalGradient(
                        listOf(
                            statusColor.copy(alpha = 0.75f),
                            statusColor,
                            Color(0xFF34D399)
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
                    radius = 5.5.dp.toPx(),
                    center = Offset(tipX, tipY)
                )
                drawCircle(
                    color = statusColor,
                    radius = 3.dp.toPx(),
                    center = Offset(tipX, tipY)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 28.dp)
        ) {
            Text(
                text = "شاخص سلامت مالی",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = PersianCurrencyEngine.toPersianDigits(score.toString()),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = statusColor
                )
                Text(
                    text = "/ ۱۰۰",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 7.dp)
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
            .height(64.dp)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (selected) Icons.Default.CheckCircle else icon,
                contentDescription = null,
                tint = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = titleFa,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) accentColor else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitleFa,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
