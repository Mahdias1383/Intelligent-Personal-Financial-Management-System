package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.CurrencyUnit
import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.data.CounterpartyEntity
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.ObligationEntity
import com.example.domain.EnrichedHeatmapDayCell
import com.example.domain.HeatmapIntensityLevel
import com.example.domain.HighPressurePeriodWindow
import com.example.domain.PersianCalendarHeatmapEngine
import com.example.domain.PersianDecadeHeatSummary
import com.example.domain.PersianMonthHeatmapReport
import com.example.ui.theme.LocalTarazinoColors

private enum class HeatmapFilterMode(val labelFa: String) {
    ALL("همه روزها"),
    HIGH_PRESSURE_ONLY("دوره‌های پرفشار 🔥"),
    UPCOMING_UNPAID("اقساط پیش‌رو"),
    PAID_ONLY("تسویه‌شده")
}

/**
 * Interactive Persian (Shamsi) Calendar-Based Installment Intensity Heatmap.
 *
 * Visually encodes upcoming installment payment intensity across all 29/30/31 days of the Persian month
 * using a 5-tier thermal gradient, automatically detects and spotlights High-Pressure Periods
 * (`دوره‌های اوج فشار`), breaks down pressure by Persian Decade (`دهه اول، دوم، سوم`),
 * and opens a detailed day inspector drawer on tap.
 */
@Composable
fun PersianInstallmentHeatmapSection(
    heatmapYear: Int,
    heatmapMonth: Int,
    installments: List<InstallmentEntity>,
    obligations: List<ObligationEntity>,
    counterparties: List<CounterpartyEntity>,
    monthlyIncomeToman: Long,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    onChangeHeatmapMonth: (Int) -> Unit,
    onQuickPay: ((InstallmentEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val ext = LocalTarazinoColors.current
    val tealAccent = ext.positive // Deep Persian Teal (#35B8AB / #0F6B66)
    val goldAccent = MaterialTheme.colorScheme.secondary // Champagne Gold (#DAB978 / #B58538)

    val report: PersianMonthHeatmapReport = remember(
        heatmapYear,
        heatmapMonth,
        installments,
        obligations,
        counterparties,
        monthlyIncomeToman
    ) {
        PersianCalendarHeatmapEngine.buildPersianMonthHeatmap(
            year = heatmapYear,
            month = heatmapMonth,
            installments = installments,
            obligations = obligations,
            counterparties = counterparties,
            monthlyIncomeToman = monthlyIncomeToman
        )
    }

    var filterMode by remember { mutableStateOf(HeatmapFilterMode.ALL) }
    var selectedDayNumber by remember(heatmapYear, heatmapMonth) {
        mutableStateOf<Int?>(null)
    }
    var focusedPeriodId by remember(heatmapYear, heatmapMonth) {
        mutableStateOf<String?>(null)
    }
    var focusedDecadeIndex by remember(heatmapYear, heatmapMonth) {
        mutableStateOf<Int?>(null)
    }

    val selectedCell: EnrichedHeatmapDayCell? = remember(selectedDayNumber, report) {
        selectedDayNumber?.let { d -> report.cells.firstOrNull { it.dayOfMonth == d } }
    }
    val focusedPeriod: HighPressurePeriodWindow? = remember(focusedPeriodId, report) {
        focusedPeriodId?.let { id -> report.highPressurePeriods.firstOrNull { it.id == id } }
    }

    TarazinoGlassCard(
        cornerRadius = 26.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("persian_calendar_heatmap_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            // 1. Header with High-Pressure Day Counter Badge
            TarazinoSectionHeader(
                title = "نقشه حرارتی تقویم اقساط (ماه شمسی)",
                subtitle = "شدت پرداخت‌های پیش‌رو و شناسایی خودکار دوره‌های اوج فشار",
                icon = Icons.Default.CalendarMonth,
                accentColor = tealAccent,
                trailing = {
                    val hasHotDays = report.highPressureDaysCount > 0
                    val badgeColor = if (hasHotDays) ext.danger else tealAccent
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = badgeColor.copy(alpha = if (ext.isDark) 0.17f else 0.12f),
                        border = BorderStroke(0.9.dp, badgeColor.copy(alpha = 0.45f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (hasHotDays) Icons.Default.LocalFireDepartment else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (hasHotDays) {
                                    "${PersianCurrencyEngine.toPersianDigits(report.highPressureDaysCount.toString())} روز پرفشار"
                                } else {
                                    "ماه آرام"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = badgeColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            )

            // 2. Persian Month Stepper Bar + Monthly Upcoming Load Readout
            Surface(
                shape = RoundedCornerShape(15.dp),
                color = ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.62f else 0.85f),
                border = BorderStroke(0.85.dp, ext.cardBorderTop),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onChangeHeatmapMonth(-1) },
                        modifier = Modifier.testTag("heatmap_prev_month_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "ماه شمسی قبل"
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = report.monthYearLabelFa,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (report.totalUpcomingUnpaidToman > 0L) {
                                "اقساط پیش‌رو: ${
                                    PersianCurrencyEngine.formatAmount(
                                        report.totalUpcomingUnpaidToman,
                                        currencyUnit,
                                        privacyMasked = privacyMasked
                                    )
                                } (${PersianCurrencyEngine.toPersianDigits(report.upcomingInstallmentsCount.toString())} قسط)"
                            } else {
                                "بدون قسط پرداخت‌نشده در این ماه"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (report.totalUpcomingUnpaidToman > 0L) goldAccent else tealAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = { onChangeHeatmapMonth(1) },
                        modifier = Modifier.testTag("heatmap_next_month_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "ماه شمسی بعد"
                        )
                    }
                }
            }

            // 3. Interactive Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                HeatmapFilterMode.entries.forEach { mode ->
                    val isSelected = filterMode == mode
                    val chipAccent = when (mode) {
                        HeatmapFilterMode.ALL -> MaterialTheme.colorScheme.primary
                        HeatmapFilterMode.HIGH_PRESSURE_ONLY -> ext.danger
                        HeatmapFilterMode.UPCOMING_UNPAID -> goldAccent
                        HeatmapFilterMode.PAID_ONLY -> tealAccent
                    }
                    Surface(
                        shape = RoundedCornerShape(11.dp),
                        color = if (isSelected) {
                            chipAccent.copy(alpha = if (ext.isDark) 0.22f else 0.14f)
                        } else {
                            ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.50f else 0.75f)
                        },
                        border = BorderStroke(
                            width = if (isSelected) 1.25.dp else 0.75.dp,
                            color = if (isSelected) chipAccent else ext.cardBorderTop
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(11.dp))
                            .clickable {
                                filterMode = mode
                                focusedDecadeIndex = null
                                focusedPeriodId = null
                            }
                            .testTag("heatmap_filter_${mode.name}")
                    ) {
                        Text(
                            text = mode.labelFa,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) chipAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // 4. Detected High-Pressure Period Spotlight Cards (دوره‌های اوج فشار)
            if (report.highPressurePeriods.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    report.highPressurePeriods.forEach { period ->
                        val isPeriodFocused = focusedPeriodId == period.id
                        val periodAccent = Color(period.peakIntensityLevel.accentColorHex)
                        Surface(
                            shape = RoundedCornerShape(15.dp),
                            color = Color.Transparent,
                            border = BorderStroke(
                                width = if (isPeriodFocused) 1.4.dp else 0.9.dp,
                                color = periodAccent.copy(alpha = if (isPeriodFocused) 0.85f else 0.45f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(15.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            periodAccent.copy(alpha = if (ext.isDark) 0.20f else 0.13f),
                                            periodAccent.copy(alpha = if (ext.isDark) 0.08f else 0.05f)
                                        )
                                    )
                                )
                                .clickable {
                                    focusedDecadeIndex = null
                                    focusedPeriodId = if (isPeriodFocused) null else period.id
                                    selectedDayNumber = period.startDay
                                }
                                .testTag("heatmap_high_pressure_period_${period.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WarningAmber,
                                            contentDescription = null,
                                            tint = periodAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "بازه پرفشار: ${period.rangeLabelFa}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = periodAccent.copy(alpha = 0.20f)
                                    ) {
                                        Text(
                                            text = "${PersianCurrencyEngine.toPersianDigits(period.shareOfMonthlyUpcomingPercent.toString())}٪ بار ماه",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = periodAccent,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${period.severityBadgeFa} • ${PersianCurrencyEngine.toPersianDigits(period.installmentsCount.toString())} قسط",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = PersianCurrencyEngine.formatAmount(
                                            period.totalUpcomingDebtToman,
                                            currencyUnit,
                                            privacyMasked = privacyMasked
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = periodAccent
                                    )
                                }

                                AnimatedVisibility(visible = isPeriodFocused) {
                                    Text(
                                        text = period.advisoryFa,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.padding(top = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Continuous 5-Step Thermal Intensity Scale Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.50f else 0.72f))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "طیف شدت فشار:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeatmapIntensityLevel.entries.forEach { level ->
                        val swatchColor = heatFillColorForLevel(
                            level = level,
                            isDark = ext.isDark,
                            defaultEmpty = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(swatchColor)
                                    .border(
                                        0.7.dp,
                                        Color(level.accentColorHex).copy(alpha = 0.65f),
                                        RoundedCornerShape(3.dp)
                                    )
                            )
                            Text(
                                text = level.shortLabelFa,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 6. 7-Column Persian Weekday Header (شنبه تا جمعه)
            Row(modifier = Modifier.fillMaxWidth()) {
                for (w in 0..6) {
                    Text(
                        text = JalaliCalendarEngine.WEEKDAY_SHORT[w],
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (w == 6) ext.danger else MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 7. 29/30/31-Day Thermal Calendar Heatmap Grid
            val totalSlots = report.firstDayWeekdayOffset + report.cells.size
            val rowCount = (totalSlots + 6) / 7

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.testTag("persian_heatmap_calendar_grid")
            ) {
                for (r in 0 until rowCount) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        for (c in 0..6) {
                            val slot = r * 7 + c
                            val dayIdx = slot - report.firstDayWeekdayOffset
                            if (dayIdx in report.cells.indices) {
                                val cell = report.cells[dayIdx]
                                val isSelected = selectedDayNumber == cell.dayOfMonth
                                val matchesFilter = when (filterMode) {
                                    HeatmapFilterMode.ALL -> true
                                    HeatmapFilterMode.HIGH_PRESSURE_ONLY ->
                                        cell.isInHighPressurePeriod ||
                                            cell.intensityLevel.rank >= HeatmapIntensityLevel.HIGH.rank
                                    HeatmapFilterMode.UPCOMING_UNPAID -> cell.upcomingUnpaidDebtToman > 0L
                                    HeatmapFilterMode.PAID_ONLY -> cell.paidDebtToman > 0L && cell.upcomingUnpaidDebtToman == 0L
                                }
                                val matchesFocusSpan = when {
                                    focusedPeriod != null ->
                                        cell.dayOfMonth in focusedPeriod.startDay..focusedPeriod.endDay
                                    focusedDecadeIndex != null ->
                                        cell.decadeOfMonth == focusedDecadeIndex
                                    else -> true
                                }
                                val isDimmed = !(matchesFilter && matchesFocusSpan)

                                PersianHeatmapDayTile(
                                    cell = cell,
                                    isSelected = isSelected,
                                    isDimmed = isDimmed,
                                    isDark = ext.isDark,
                                    tealAccent = tealAccent,
                                    goldAccent = goldAccent,
                                    dangerAccent = ext.danger,
                                    currencyUnit = currencyUnit,
                                    privacyMasked = privacyMasked,
                                    onClick = {
                                        selectedDayNumber = if (isSelected) null else cell.dayOfMonth
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 8. 3-Column Persian Decade Intensity Strip (دهه اول، دهه دوم، دهه سوم)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                report.decadeSummaries.forEach { dec ->
                    val isDecFocused = focusedDecadeIndex == dec.decadeIndex
                    PersianDecadeHeatCard(
                        decade = dec,
                        isFocused = isDecFocused,
                        currencyUnit = currencyUnit,
                        privacyMasked = privacyMasked,
                        onClick = {
                            focusedPeriodId = null
                            focusedDecadeIndex = if (isDecFocused) null else dec.decadeIndex
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 9. Expandable Selected Day Inspector Drawer
            AnimatedVisibility(
                visible = selectedCell != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                selectedCell?.let { cell ->
                    SelectedHeatmapDayInspectorCard(
                        cell = cell,
                        currencyUnit = currencyUnit,
                        privacyMasked = privacyMasked,
                        tealAccent = tealAccent,
                        goldAccent = goldAccent,
                        onQuickPay = onQuickPay,
                        onDismiss = { selectedDayNumber = null }
                    )
                }
            }

            // 10. Executive Summary Footer
            Surface(
                shape = RoundedCornerShape(13.dp),
                color = tealAccent.copy(alpha = if (ext.isDark) 0.11f else 0.07f),
                border = BorderStroke(0.75.dp, tealAccent.copy(alpha = 0.28f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = report.executiveSummaryFa,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun PersianHeatmapDayTile(
    cell: EnrichedHeatmapDayCell,
    isSelected: Boolean,
    isDimmed: Boolean,
    isDark: Boolean,
    tealAccent: Color,
    goldAccent: Color,
    dangerAccent: Color,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseFill = when {
        cell.upcomingUnpaidDebtToman > 0L -> heatFillColorForLevel(
            level = cell.intensityLevel,
            isDark = isDark,
            defaultEmpty = MaterialTheme.colorScheme.surfaceVariant
        )
        cell.paidDebtToman > 0L -> tealAccent.copy(alpha = if (isDark) 0.22f else 0.16f)
        cell.receivableToman > 0L -> Color(0xFF47B0C4).copy(alpha = if (isDark) 0.22f else 0.16f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.30f else 0.38f)
    }

    val animatedBg by animateColorAsState(
        targetValue = if (isDimmed) baseFill.copy(alpha = baseFill.alpha * 0.28f) else baseFill,
        label = "HeatCellBg_${cell.dayOfMonth}"
    )

    val borderStrokeColor = when {
        isSelected -> goldAccent
        cell.isToday -> MaterialTheme.colorScheme.primary
        cell.isInHighPressurePeriod && !isDimmed -> dangerAccent.copy(alpha = 0.85f)
        cell.intensityLevel == HeatmapIntensityLevel.CRITICAL && !isDimmed -> dangerAccent.copy(alpha = 0.78f)
        cell.intensityLevel == HeatmapIntensityLevel.HIGH && !isDimmed -> Color(0xFFE08B4F).copy(alpha = 0.65f)
        else -> Color.Transparent
    }
    val borderWidth = when {
        isSelected -> 2.dp
        cell.isToday -> 1.6.dp
        cell.isInHighPressurePeriod && !isDimmed -> 1.35.dp
        borderStrokeColor != Color.Transparent -> 1.dp
        else -> 0.dp
    }

    val textColor = when {
        isDimmed -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f)
        cell.intensityLevel == HeatmapIntensityLevel.CRITICAL -> if (isDark) Color.White else Color(0xFF7A1C1C)
        cell.weekdayIndex == 6 && cell.upcomingUnpaidDebtToman == 0L -> dangerAccent
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(animatedBg)
            .then(
                if (borderWidth > 0.dp) {
                    Modifier.border(borderWidth, borderStrokeColor, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .testTag("heatmap_day_cell_${cell.dayOfMonth}")
    ) {
        // Subtle corner fire/peak dot for high-pressure cluster days
        if (cell.isInHighPressurePeriod && !isDimmed) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(dangerAccent)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 2.dp)
        ) {
            Text(
                text = PersianCurrencyEngine.toPersianDigits(cell.dayOfMonth.toString()),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (cell.totalInstallmentsCount > 0 || cell.isToday) FontWeight.ExtraBold else FontWeight.Medium,
                color = textColor,
                fontSize = 12.sp,
                maxLines = 1
            )

            when {
                cell.upcomingUnpaidDebtToman > 0L -> {
                    val compactVal = formatCompactPersianToman(
                        cell.upcomingUnpaidDebtToman * currencyUnit.multiplierFromToman,
                        privacyMasked
                    )
                    Text(
                        text = compactVal,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDimmed) textColor else Color(cell.intensityLevel.accentColorHex),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                    // Payment count intensity pips (up to 3 dots)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(top = 1.dp)
                    ) {
                        val pips = cell.upcomingInstallmentsCount.coerceIn(1, 3)
                        repeat(pips) {
                            Box(
                                modifier = Modifier
                                    .size(3.5.dp)
                                    .clip(CircleShape)
                                    .background(Color(cell.intensityLevel.accentColorHex).copy(alpha = if (isDimmed) 0.3f else 1f))
                            )
                        }
                    }
                }
                cell.paidDebtToman > 0L -> {
                    Box(
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(tealAccent.copy(alpha = if (isDimmed) 0.3f else 0.9f))
                    )
                }
                cell.receivableToman > 0L -> {
                    Box(
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF47B0C4).copy(alpha = if (isDimmed) 0.3f else 0.9f))
                    )
                }
            }
        }
    }
}

@Composable
private fun PersianDecadeHeatCard(
    decade: PersianDecadeHeatSummary,
    isFocused: Boolean,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTarazinoColors.current
    val accent = Color(decade.intensityLevel.accentColorHex)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isFocused) {
            accent.copy(alpha = if (ext.isDark) 0.22f else 0.14f)
        } else {
            ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.58f else 0.85f)
        },
        border = BorderStroke(
            width = if (isFocused || decade.isPeakDecade) 1.2.dp else 0.8.dp,
            color = if (isFocused || decade.isPeakDecade) accent.copy(alpha = 0.72f) else ext.cardBorderTop
        ),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("heatmap_decade_card_${decade.decadeIndex}")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = decade.titleFa,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "${PersianCurrencyEngine.toPersianDigits(decade.shareOfMonthlyUpcomingPercent.toString())}٪",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = accent,
                    fontSize = 10.sp
                )
            }

            // Thermal intensity progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                val frac = (decade.shareOfMonthlyUpcomingPercent / 100f).coerceIn(0f, 1f)
                if (frac > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(frac.coerceAtLeast(0.08f))
                            .clip(RoundedCornerShape(3.dp))
                            .background(accent)
                    )
                }
            }

            TarazinoAutoSizeText(
                text = if (decade.upcomingDebtToman > 0L) {
                    formatCompactPersianToman(
                        decade.upcomingDebtToman * currencyUnit.multiplierFromToman,
                        privacyMasked
                    ) + if (decade.isPeakDecade) " • اوج" else ""
                } else {
                    "آرام"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (decade.isPeakDecade) FontWeight.Bold else FontWeight.Medium,
                color = if (decade.isPeakDecade) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                minFontSize = 8.sp
            )
        }
    }
}

@Composable
private fun SelectedHeatmapDayInspectorCard(
    cell: EnrichedHeatmapDayCell,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    tealAccent: Color,
    goldAccent: Color,
    onQuickPay: ((InstallmentEntity) -> Unit)?,
    onDismiss: () -> Unit
) {
    val ext = LocalTarazinoColors.current
    val levelColor = Color(cell.intensityLevel.accentColorHex)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = levelColor.copy(alpha = if (ext.isDark) 0.13f else 0.08f),
        border = BorderStroke(1.1.dp, levelColor.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("heatmap_selected_day_inspector")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Today,
                        contentDescription = null,
                        tint = levelColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = JalaliCalendarEngine.formatJalaliVerbose(cell.dateJalali),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = levelColor.copy(alpha = 0.20f)
                    ) {
                        Text(
                            text = cell.intensityLevel.labelFa,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = levelColor,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن جزئیات روز",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            if (cell.items.isEmpty()) {
                Text(
                    text = "در این روز هیچ قسط یا سررسیدی ثبت نشده است.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "جمع سررسیدهای پیش‌رو: ${
                            PersianCurrencyEngine.formatAmount(
                                cell.upcomingUnpaidDebtToman,
                                currencyUnit,
                                privacyMasked = privacyMasked
                            )
                        }",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (cell.upcomingUnpaidDebtToman > 0L) levelColor else tealAccent
                    )
                    if (cell.shareOfMonthlyUpcomingPercent > 0) {
                        Text(
                            text = "${PersianCurrencyEngine.toPersianDigits(cell.shareOfMonthlyUpcomingPercent.toString())}٪ از کل ماه",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = goldAccent
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                cell.items.forEach { alert ->
                    val isPaid = alert.installment.statusEnum == InstallmentStatus.PAID
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.72f else 0.92f),
                        border = BorderStroke(0.75.dp, ext.cardBorderTop),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = alert.installment.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${alert.counterparty?.name ?: alert.obligation.title} • ${
                                        if (isPaid) "تسویه‌شده" else alert.bucket.badgeFa
                                    }",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPaid) tealAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                FormattedMoneyText(
                                    amountToman = if (isPaid) alert.installment.paidAmountToman else alert.installment.remainingToman,
                                    unit = currencyUnit,
                                    privacyMasked = privacyMasked,
                                    style = MaterialTheme.typography.labelMedium,
                                    numberColor = if (isPaid) tealAccent else levelColor,
                                    minFontSize = 9.sp,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.widthIn(max = 132.dp)
                                )
                                if (!isPaid && onQuickPay != null) {
                                    Text(
                                        text = "ثبت پرداخت ✓",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = tealAccent,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onQuickPay(alert.installment) }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
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

private fun heatFillColorForLevel(
    level: HeatmapIntensityLevel,
    isDark: Boolean,
    defaultEmpty: Color
): Color {
    return when (level) {
        HeatmapIntensityLevel.NONE -> defaultEmpty.copy(alpha = if (isDark) 0.28f else 0.36f)
        HeatmapIntensityLevel.LOW -> Color(0xFF35B8AB).copy(alpha = if (isDark) 0.24f else 0.18f)
        HeatmapIntensityLevel.MODERATE -> Color(0xFFDAB978).copy(alpha = if (isDark) 0.30f else 0.24f)
        HeatmapIntensityLevel.HIGH -> Color(0xFFE08B4F).copy(alpha = if (isDark) 0.36f else 0.30f)
        HeatmapIntensityLevel.CRITICAL -> Color(0xFFD96B6B).copy(alpha = if (isDark) 0.44f else 0.36f)
    }
}
