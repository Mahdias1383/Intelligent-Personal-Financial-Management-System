package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.domain.CalendarHeatmapDay
import com.example.ui.TarazinoUiState
import com.example.ui.components.MoneyInputField
import com.example.ui.components.tourSpotlightTarget
import kotlin.math.roundToInt

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
    var showBudgetConfigModal by remember { mutableStateOf(false) }
    var selectedStrategyTab by remember { mutableIntStateOf(0) } // 0 = Avalanche (بهمن), 1 = Snowball (گلوله برفی)

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
        // 1. Monthly Budget Planner & Safe New Installment Capacity Card (پنل برنامه‌ریزی بودجه ماهانه و سقف قسط جدید)
        uiState.dtiHealthReport?.let { dti ->
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .tourSpotlightTarget("intel_budget_planner")
                        .testTag("intel_budget_planner_card")
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
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "برنامه‌ریز بودجه و سقف مجاز قسط جدید",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "تحلیل ماه جاری: ${dti.currentMonthYearLabelFa} • ${dti.statusBadgeFa}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(dti.statusColorHex)
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { showBudgetConfigModal = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("edit_budget_planner_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "تنظیم درآمد و بودجه",
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "تنظیم بودجه",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Highlight Box: Safe New Monthly Installment Capacity (سقف مجاز اقساط جدید در ماه)
                        val hasCapacity = dti.safeNewInstallmentCapacityToman > 0L
                        val capAccent = if (hasCapacity) Color(0xFF10B981) else Color(0xFFEF4444)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = capAccent.copy(alpha = 0.12f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, capAccent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "سقف مجاز افزودن قسط جدید در ماه:",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = PersianCurrencyEngine.formatAmount(
                                            dti.safeNewInstallmentCapacityToman,
                                            unit,
                                            privacyMasked = masked
                                        ),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = capAccent
                                    )
                                }
                                if (!masked && dti.safeNewInstallmentCapacityToman > 0L) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "معادل حروفی: ${
                                            PersianCurrencyEngine.amountToPersianWords(
                                                dti.safeNewInstallmentCapacityToman,
                                                unit
                                            )
                                        }",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3-Column Metrics Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricMiniCard(
                                label = "درآمد ماهانه",
                                value = PersianCurrencyEngine.formatAmount(dti.monthlyIncomeToman, unit, privacyMasked = masked),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            MetricMiniCard(
                                label = "مخارج زندگی (${PersianCurrencyEngine.toPersianDigits(dti.livingExpensePercent.toString())}٪)",
                                value = PersianCurrencyEngine.formatAmount(dti.essentialLivingExpenseToman, unit, privacyMasked = masked),
                                color = Color(0xFF38BDF8),
                                modifier = Modifier.weight(1f)
                            )
                            MetricMiniCard(
                                label = "بار اقساط (${PersianCurrencyEngine.toPersianDigits(dti.dtiPercent.toString())}٪)",
                                value = PersianCurrencyEngine.formatAmount(dti.currentMonthDebtDueToman, unit, privacyMasked = masked),
                                color = Color(dti.statusColorHex),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Peak Crunch Windows (دهه‌های اوج فشار مالی ماه شمسی)
                        Text(
                            text = "توزیع فشار اقساط در دهه‌های ماه شمسی:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        dti.decadeSlices.forEach { slice ->
                            val isPeak = slice.decadeIndex == dti.peakDecadeIndex && slice.amountToman > 0L
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${slice.labelFa} (${slice.rangeLabelFa})" +
                                            if (isPeak) " • اوج فشار" else "",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isPeak) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${PersianCurrencyEngine.formatAmount(slice.amountToman, unit, privacyMasked = masked)} (${
                                            PersianCurrencyEngine.toPersianDigits(slice.sharePercent.toString())
                                        }٪)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
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
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
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
                                        text = "توصیه‌های موتور تصمیم‌یار هوشمند ترازینو:",
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

        // 2. Interactive Jalali Monthly Calendar Heatmap (تقویم حرارتی ۳۰/۳۱ روزه ماه شمسی)
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "تقویم حرارتی سررسیدها (Shamsi Heatmap)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "روی هر روز دارای قسط بزنید تا جزئیات تعهدات آن روز باز شود",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Month Stepper
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // 7-Column Weekday Header
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (w in 0..6) {
                            Text(
                                text = JalaliCalendarEngine.WEEKDAY_SHORT[w],
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
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

                                        val cellColor = when {
                                            dayItem.unpaidDebtToman >= 10_000_000L -> Color(0xFFEF4444).copy(alpha = 0.28f)
                                            hasUnpaid -> Color(0xFFF59E0B).copy(alpha = 0.25f)
                                            hasRec -> Color(0xFF06B6D4).copy(alpha = 0.25f)
                                            hasPaid -> Color(0xFF10B981).copy(alpha = 0.22f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        }

                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(cellColor)
                                                .then(
                                                    if (dayItem.isToday) {
                                                        Modifier.border(
                                                            1.5.dp,
                                                            MaterialTheme.colorScheme.primary,
                                                            RoundedCornerShape(10.dp)
                                                        )
                                                    } else Modifier
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
                                                    fontWeight = if (dayItem.items.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
                                                )
                                                if (dayItem.items.isNotEmpty()) {
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

                    selectedHeatmapDay?.let { sel ->
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "تعهدات روز ${JalaliCalendarEngine.formatJalaliVerbose(sel.dateJalali)}:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        sel.items.forEach { alert ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${alert.installment.title} (${alert.counterparty?.name ?: ""})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = PersianCurrencyEngine.formatAmount(
                                        alert.installment.remainingToman,
                                        unit,
                                        privacyMasked = masked
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. 12-Month Forward Installment Load Bar Chart (نمودار بار اقساط ۱۲ ماه آینده)
        if (uiState.forward12MonthBars.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "چشم‌انداز بار اقساط ۱۲ ماه آینده",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "مقایسه تعهدات پرداخت‌شده (سبز) و مانده هر ماه (نارنجی)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val maxMonthly = uiState.forward12MonthBars.maxOfOrNull { it.totalDebtToman }
                            ?.coerceAtLeast(1L) ?: 1L

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        ) {
                            val barCount = uiState.forward12MonthBars.size
                            val slotWidth = size.width / barCount
                            val barWidth = (slotWidth * 0.56f).coerceAtMost(28.dp.toPx())

                            uiState.forward12MonthBars.forEachIndexed { idx, bar ->
                                val x = size.width - ((idx + 1) * slotWidth) + (slotWidth - barWidth) / 2f
                                val totalRatio = (bar.totalDebtToman.toFloat() / maxMonthly.toFloat()).coerceIn(0.04f, 1f)
                                val paidRatio = (bar.paidToman.toFloat() / maxMonthly.toFloat()).coerceIn(0f, totalRatio)

                                val totalH = size.height * totalRatio
                                val paidH = size.height * paidRatio

                                drawRoundRect(
                                    color = Color(0xFFF59E0B).copy(alpha = 0.78f),
                                    topLeft = Offset(x, size.height - totalH),
                                    size = Size(barWidth, totalH),
                                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                )
                                if (paidH > 0f) {
                                    drawRoundRect(
                                        color = Color(0xFF10B981),
                                        topLeft = Offset(x, size.height - paidH),
                                        size = Size(barWidth, paidH),
                                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            uiState.forward12MonthBars.take(6).forEach { bar ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = bar.monthLabelFa,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = PersianCurrencyEngine.formatAmount(
                                            bar.unpaidToman,
                                            unit,
                                            includeUnit = false,
                                            privacyMasked = masked
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Interactive Early Payoff Simulator: Snowball vs. Avalanche (Fixed Symmetrical Segmented Selector!)
        uiState.payoffSimulation?.let { sim ->
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .tourSpotlightTarget("intel_payoff_simulator")
                        .testTag("intel_payoff_simulator_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RocketLaunch,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "شبیه‌ساز تسویه زودهنگام (بهمن در برابر گلوله برفی)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "محاسبه تاریخ دقیق آزادی از بدهی با افزودن بودجه مازاد ماهانه",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "بودجه مازاد ماهانه برای تسویه سریع‌تر: ${
                                PersianCurrencyEngine.formatAmount(
                                    uiState.extraPayoffBudgetToman,
                                    unit,
                                    privacyMasked = masked
                                )
                            }",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

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

                        Spacer(modifier = Modifier.height(8.dp))

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

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
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

                        // FIXED SYMMETRICAL 2-COLUMN SEGMENTED STRATEGY SELECTOR (No multi-line wrapping!)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
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

                        Spacer(modifier = Modifier.height(10.dp))

                        val targetList = if (selectedStrategyTab == 0) sim.avalancheOrder else sim.snowballOrder
                        targetList.forEachIndexed { idx, step ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
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
                                Text(
                                    text = PersianCurrencyEngine.formatAmount(step.remainingToman, unit, privacyMasked = masked),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
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

        AlertDialog(
            onDismissRequest = { showBudgetConfigModal = false },
            title = {
                Text("تنظیم بودجه ماهانه و سقف مجاز اقساط", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MoneyInputField(
                        labelFa = "درآمد ماهانه خالص شما",
                        amountToman = newIncome,
                        currencyUnit = unit,
                        onAmountChange = { newIncome = it }
                    )

                    Text(
                        text = "سهم مخارج ضروری زندگی از درآمد: ${
                            PersianCurrencyEngine.toPersianDigits(newLivingPct.roundToInt().toString())
                        }٪ (${
                            PersianCurrencyEngine.formatAmount(
                                (newIncome * newLivingPct.roundToInt()) / 100L,
                                unit
                            )
                        })",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = newLivingPct,
                        onValueChange = { newLivingPct = it },
                        valueRange = 25f..75f,
                        steps = 9
                    )

                    Text(
                        text = "بازه هشدار زودهنگام قبل از سررسید: ${
                            PersianCurrencyEngine.toPersianDigits(newAlertDays.roundToInt().toString())
                        } روز",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = newAlertDays,
                        onValueChange = { newAlertDays = it },
                        valueRange = 1f..20f,
                        steps = 18
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateBudgetPlannerSettings(
                            newIncome,
                            newLivingPct.roundToInt(),
                            newAlertDays.roundToInt()
                        )
                        showBudgetConfigModal = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("ذخیره برنامه بودجه", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetConfigModal = false }) {
                    Text("انصراف")
                }
            }
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
