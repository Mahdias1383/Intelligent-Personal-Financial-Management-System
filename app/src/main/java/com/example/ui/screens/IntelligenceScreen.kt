package com.example.ui.screens

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.JalaliCalendarEngine
import com.example.core.JalaliDate
import com.example.core.PersianCurrencyEngine
import com.example.domain.CalendarHeatmapDay
import com.example.ui.TarazinoUiState
import com.example.ui.components.MoneyInputField
import kotlin.math.roundToLong

@Composable
fun IntelligenceScreen(
    uiState: TarazinoUiState,
    onUpdateExtraPayoffBudget: (Long) -> Unit,
    onChangeHeatmapMonth: (Int) -> Unit,
    onUpdateIncomeAndAlertDays: (Long, Int) -> Unit
) {
    val unit = uiState.preferences.currencyUnit
    val masked = uiState.preferences.privacyMaskEnabled
    val dti = uiState.dtiHealthReport
    val sim = uiState.payoffSimulation

    var selectedStrategyTab by remember { mutableIntStateOf(0) } // 0 = Avalanche, 1 = Snowball
    var selectedHeatmapDay by remember(uiState.heatmapDays) {
        mutableStateOf(
            uiState.heatmapDays.firstOrNull { it.items.isNotEmpty() }
                ?: uiState.heatmapDays.firstOrNull { it.isToday }
                ?: uiState.heatmapDays.firstOrNull()
        )
    }
    var selectedBarMonthIndex by remember { mutableIntStateOf(0) }
    var showIncomeConfigModal by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("intelligence_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Cashflow Stress, DTI Radar & Decade Crunch Card
        if (dti != null) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
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
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "رادار سلامت مالی و فشار نقدینگی (DTI)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "تحلیل ماه جاری: ${dti.currentMonthYearLabelFa}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { showIncomeConfigModal = true }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "تنظیم درآمد ماهانه"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

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
                                label = "نسبت اقساط (DTI)",
                                value = "${PersianCurrencyEngine.toPersianDigits(dti.dtiPercent.toString())}٪",
                                color = Color(dti.statusColorHex),
                                modifier = Modifier.weight(1f)
                            )
                            MetricMiniCard(
                                label = "امتیاز سلامت",
                                value = "${PersianCurrencyEngine.toPersianDigits(dti.obligationCoverageScore.toString())}/۱۰۰",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Peak Crunch Windows (دهه‌های اوج فشار مالی ماه شمسی)
                        Text(
                            text = "تحلیل دهه‌های اوج فشار مالی در ماه شمسی:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

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
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
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

        // 2. Interactive Early Payoff Strategy Simulator (Snowball vs. Avalanche)
        if (sim != null) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoGraph,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "شبیه‌ساز تسویه زودهنگام: گلوله برفی در برابر بهمن",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "اگر ماهانه بودجه اضافه‌ای برای تسویه بدهی‌ها کنار بگذارم...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "بودجه مازاد ماهانه: ${
                                PersianCurrencyEngine.formatAmount(
                                    uiState.extraPayoffBudgetToman,
                                    unit,
                                    privacyMasked = masked
                                )
                            } (${PersianCurrencyEngine.amountToPersianWords(uiState.extraPayoffBudgetToman, unit)})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Slider(
                            value = uiState.extraPayoffBudgetToman.toFloat(),
                            onValueChange = { raw ->
                                val stepped = ((raw / 500_000f).roundToLong() * 500_000L).coerceIn(0L, 25_000_000L)
                                onUpdateExtraPayoffBudget(stepped)
                            },
                            valueRange = 0f..25_000_000f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("extra_payoff_budget_slider")
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            listOf(
                                "بدون بودجه اضافه" to 0L,
                                "۱ میلیون تومان" to 1_000_000L,
                                "۳ میلیون تومان" to 3_000_000L,
                                "۵ میلیون تومان" to 5_000_000L,
                                "۱۰ میلیون تومان" to 10_000_000L
                            ).forEach { (lbl, amt) ->
                                AssistChip(
                                    onClick = { onUpdateExtraPayoffBudget(amt) },
                                    label = { Text(lbl) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Comparative Metrics Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricMiniCard(
                                label = "زمان تسویه عادی",
                                value = "${PersianCurrencyEngine.toPersianDigits(sim.baselineMonthsToClear.toString())} ماه",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            MetricMiniCard(
                                label = "با بودجه مازاد",
                                value = "${PersianCurrencyEngine.toPersianDigits(sim.avalancheMonthsToClear.toString())} ماه",
                                color = Color(0xFF10B981),
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
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "تاریخ دقیق شمسی آزادی کامل از بدهی: «${
                                        JalaliCalendarEngine.formatJalaliVerbose(sim.projectedAcceleratedDateJalali)
                                    }»",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = sim.recommendationFa,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Strategy Switcher: Avalanche vs Snowball
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedStrategyTab == 0,
                                onClick = { selectedStrategyTab = 0 },
                                label = { Text("استراتژی بهمن (حداقل هزینه و ریسک)") }
                            )
                            FilterChip(
                                selected = selectedStrategyTab == 1,
                                onClick = { selectedStrategyTab = 1 },
                                label = { Text("استراتژی گلوله برفی (حذف سریع بدهی‌های کوچک)") }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val steps = if (selectedStrategyTab == 0) sim.avalancheOrder else sim.snowballOrder
                        steps.forEachIndexed { idx, step ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                    .padding(10.dp),
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
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text(
                                            text = PersianCurrencyEngine.toPersianDigits((idx + 1).toString()),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = step.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${step.counterpartyName} • تسویه کامل در ماه ${
                                                PersianCurrencyEngine.toPersianDigits(step.estimatedClearMonthIndex.toString())
                                            }",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = PersianCurrencyEngine.formatAmount(
                                        step.remainingToman,
                                        unit,
                                        privacyMasked = masked
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Monthly Jalali Calendar Due-Date Heatmap (نقشه حرارتی ۳۰/۳۱ روزه تقویم شمسی)
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
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
                            Text(
                                text = "نقشه حرارتی سررسیدهای تقویم شمسی",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { onChangeHeatmapMonth(1) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "ماه بعد"
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Weekday Header
                    Row(modifier = Modifier.fillMaxWidth()) {
                        JalaliCalendarEngine.WEEKDAY_SHORT.forEach { w ->
                            Text(
                                text = w,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val firstWeekdayOffset = remember(uiState.heatmapYear, uiState.heatmapMonth) {
                        JalaliCalendarEngine.getPersianWeekdayIndex(
                            JalaliDate(uiState.heatmapYear, uiState.heatmapMonth, 1)
                        )
                    }
                    val days = uiState.heatmapDays
                    val totalCells = firstWeekdayOffset + days.size
                    val rowsCount = (totalCells + 6) / 7

                    for (r in 0 until rowsCount) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            for (c in 0 until 7) {
                                val idx = r * 7 + c - firstWeekdayOffset
                                val dayItem = days.getOrNull(idx)
                                if (dayItem != null) {
                                    HeatmapDayCell(
                                        dayItem = dayItem,
                                        isSelected = selectedHeatmapDay?.dateJalali == dayItem.dateJalali,
                                        onClick = { selectedHeatmapDay = dayItem },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f).size(36.dp))
                                }
                            }
                        }
                    }

                    // Selected Heatmap Day Details
                    selectedHeatmapDay?.let { selDay ->
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "تعهدات روز ${JalaliCalendarEngine.formatJalaliVerbose(selDay.dateJalali)}:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (selDay.items.isEmpty()) {
                            Text(
                                text = "در این تاریخ هیچ سررسید یا قسطی ثبت نشده است.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            selDay.items.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${item.installment.title} (${item.counterparty?.name ?: ""})",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = PersianCurrencyEngine.formatAmount(
                                            item.installment.effectiveTotalToman,
                                            unit,
                                            privacyMasked = masked
                                        ),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. 12-Month Forward Installment Load Bar Chart
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "نمودار بار اقساط ۱۲ ماه آینده",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "برای مشاهده جزئیات هر ماه، روی ستون آن ضربه بزنید",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val maxMonthLoad = uiState.forward12MonthBars.maxOfOrNull { it.totalDebtToman }?.coerceAtLeast(1L) ?: 1L

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        uiState.forward12MonthBars.forEachIndexed { idx, bar ->
                            val ratio = (bar.totalDebtToman.toFloat() / maxMonthLoad.toFloat()).coerceIn(0.06f, 1f)
                            val isSelected = selectedBarMonthIndex == idx

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier
                                    .width(46.dp)
                                    .clickable { selectedBarMonthIndex = idx }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(26.dp)
                                        .height((95 * ratio).dp)
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(
                                            if (isSelected) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary
                                        )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = bar.monthLabelFa,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    uiState.forward12MonthBars.getOrNull(selectedBarMonthIndex)?.let { selBar ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${selBar.monthLabelFa} (${PersianCurrencyEngine.toPersianDigits(selBar.count.toString())} قسط)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "مجموع: ${
                                        PersianCurrencyEngine.formatAmount(
                                            selBar.totalDebtToman,
                                            unit,
                                            privacyMasked = masked
                                        )
                                    }",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showIncomeConfigModal) {
        var tempIncome by remember { mutableLongStateOf(uiState.preferences.monthlyIncomeToman) }
        var tempDays by remember { mutableIntStateOf(uiState.preferences.alertAdvanceDays) }

        AlertDialog(
            onDismissRequest = { showIncomeConfigModal = false },
            title = { Text("تنظیم درآمد ماهانه و بازه هشدار سررسید", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MoneyInputField(
                        labelFa = "میانگین درآمد ماهانه شما",
                        amountToman = tempIncome,
                        onAmountChange = { tempIncome = it },
                        currencyUnit = unit
                    )

                    Text("هشدار زودهنگام چند روز قبل از سررسید فعال شود؟", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 3, 7, 14).forEach { d ->
                            FilterChip(
                                selected = tempDays == d,
                                onClick = { tempDays = d },
                                label = { Text("${PersianCurrencyEngine.toPersianDigits(d.toString())} روز قبل") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateIncomeAndAlertDays(tempIncome, tempDays)
                        showIncomeConfigModal = false
                    }
                ) {
                    Text("ذخیره تنظیمات")
                }
            },
            dismissButton = {
                TextButton(onClick = { showIncomeConfigModal = false }) {
                    Text("انصراف")
                }
            }
        )
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
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = 10.dp, horizontal = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HeatmapDayCell(
    dayItem: CalendarHeatmapDay,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasUnpaid = dayItem.unpaidDebtToman > 0L
    val hasPaid = dayItem.paidDebtToman > 0L
    val hasReceivable = dayItem.receivableToman > 0L

    val bgColor = when {
        hasUnpaid -> Color(0xFFEF4444).copy(alpha = 0.25f)
        hasReceivable -> Color(0xFF06B6D4).copy(alpha = 0.25f)
        hasPaid -> Color(0xFF10B981).copy(alpha = 0.22f)
        dayItem.isToday -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    val dotColor = when {
        hasUnpaid -> Color(0xFFEF4444)
        hasReceivable -> Color(0xFF06B6D4)
        hasPaid -> Color(0xFF10B981)
        else -> Color.Transparent
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else if (dayItem.isToday) 1.2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = PersianCurrencyEngine.toPersianDigits(dayItem.dayOfMonth.toString()),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (hasUnpaid || isSelected || dayItem.isToday) FontWeight.Bold else FontWeight.Normal
            )
            if (dotColor != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }
    }
}
