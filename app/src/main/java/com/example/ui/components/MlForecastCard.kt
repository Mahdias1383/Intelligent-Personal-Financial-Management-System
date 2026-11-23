package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.CurrencyUnit
import com.example.core.PersianCurrencyEngine
import com.example.data.InstallmentEntity
import com.example.data.ObligationEntity
import com.example.domain.FinancialForecastMlEngine
import com.example.domain.FinancialForecastReport
import com.example.domain.ForecastRiskTier
import com.example.domain.ForecastScenarioMode
import com.example.domain.ForecastTrendDirection
import com.example.domain.MonthlyMlForecastPoint
import com.example.ui.theme.LocalTarazinoColors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Interactive Machine Learning Financial Health & Deficit Early-Warning Studio.
 * Projects 6M/12M monthly net cashflow with 80% confidence bands, logistic deficit probabilities,
 * live what-if stress scenarios, and tappable per-month diagnostic breakdown.
 */
@Composable
fun MlFinancialForecastSection(
    baselineReport: FinancialForecastReport,
    installments: List<InstallmentEntity>,
    obligations: List<ObligationEntity>,
    monthlyIncomeToman: Long,
    livingExpensePercent: Int,
    unit: CurrencyUnit,
    privacyMasked: Boolean,
    modifier: Modifier = Modifier
) {
    val ext = LocalTarazinoColors.current
    var scenarioIndex by remember { mutableIntStateOf(0) }
    var horizonTabIndex by remember { mutableIntStateOf(0) } // 0 = 6M, 1 = 12M
    val activeScenario = ForecastScenarioMode.entries[scenarioIndex.coerceIn(0, ForecastScenarioMode.entries.lastIndex)]

    val report = remember(
        baselineReport,
        activeScenario,
        installments,
        obligations,
        monthlyIncomeToman,
        livingExpensePercent
    ) {
        if (activeScenario == ForecastScenarioMode.BASELINE) {
            baselineReport
        } else {
            FinancialForecastMlEngine.generateForecast(
                installments = installments,
                obligations = obligations,
                monthlyIncomeToman = monthlyIncomeToman,
                livingExpensePercent = livingExpensePercent,
                scenario = activeScenario
            )
        }
    }

    val visiblePoints = remember(report, horizonTabIndex) {
        if (horizonTabIndex == 0) report.horizonMonths.take(6) else report.horizonMonths
    }

    // Default focus on earliest deficit month if one exists, otherwise month 0
    val defaultSelection = remember(visiblePoints) {
        visiblePoints.indexOfFirst { it.riskTier == ForecastRiskTier.DEFICIT }
            .takeIf { it >= 0 } ?: 0
    }
    var selectedIdx by remember(visiblePoints) { mutableIntStateOf(defaultSelection) }
    val focusedMonth = visiblePoints.getOrNull(selectedIdx.coerceIn(0, (visiblePoints.size - 1).coerceAtLeast(0)))

    val trendIcon = when (report.trendDirection) {
        ForecastTrendDirection.IMPROVING -> Icons.AutoMirrored.Filled.TrendingUp
        ForecastTrendDirection.STABLE -> Icons.AutoMirrored.Filled.TrendingFlat
        ForecastTrendDirection.DETERIORATING -> Icons.AutoMirrored.Filled.TrendingDown
    }
    val trendColor = when (report.trendDirection) {
        ForecastTrendDirection.IMPROVING -> ext.positive
        ForecastTrendDirection.STABLE -> ext.info
        ForecastTrendDirection.DETERIORATING -> ext.danger
    }

    val hasDeficits = report.deficitMonths.isNotEmpty()

    TarazinoGlassCard(
        accentBorderColor = if (hasDeficits) ext.danger else MaterialTheme.colorScheme.primary,
        modifier = modifier
            .animateContentSize()
            .testTag("ml_forecast_module_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with ML Model Confidence Pill
            TarazinoSectionHeader(
                title = "پیش‌بینی هوشمند سلامت مالی (ML)",
                subtitle = "رگرسیون Ridge و طبقه‌بندی احتمال کسری ماهانه",
                icon = Icons.Default.AutoGraph,
                accentColor = if (hasDeficits) ext.danger else MaterialTheme.colorScheme.primary,
                trailing = {
                    TarazinoStatusPill(
                        text = "دقت ${PersianCurrencyEngine.toPersianDigits(report.modelConfidencePercent.toString())}٪",
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 1. Proactive Deficit Early-Warning Alert Banner (or Green All-Clear Banner)
            val alertColor = when {
                hasDeficits -> ext.danger
                report.tightMonths.isNotEmpty() -> ext.warning
                else -> ext.positive
            }
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = alertColor.copy(alpha = if (ext.isDark) 0.13f else 0.09f),
                border = BorderStroke(1.dp, alertColor.copy(alpha = 0.42f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ml_deficit_warning_banner")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
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
                                imageVector = if (hasDeficits) Icons.Default.WarningAmber else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = alertColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    hasDeficits -> "هشدار زودهنگام کسری بودجه"
                                    report.tightMonths.isNotEmpty() -> "هشدار ماه‌های با نقدینگی محدود"
                                    else -> "پیش‌بینی سلامت مالی پایدار"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = alertColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = trendIcon,
                                contentDescription = null,
                                tint = trendColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = report.trendDirection.labelFa,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = trendColor,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = report.modelSummaryFa,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (hasDeficits) {
                        Spacer(modifier = Modifier.height(10.dp))
                        // Upcoming Deficit Month Tappable Warning Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            report.deficitMonths.forEach { defMonth ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = ext.danger.copy(alpha = 0.20f),
                                    border = BorderStroke(0.85.dp, ext.danger.copy(alpha = 0.55f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            if (defMonth.horizonIndex >= 6 && horizonTabIndex == 0) {
                                                horizonTabIndex = 1
                                            }
                                            selectedIdx = defMonth.horizonIndex
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(ext.danger)
                                        )
                                        Text(
                                            text = "${defMonth.monthShortFa} (${
                                                PersianCurrencyEngine.toPersianDigits((defMonth.deficitProbability * 100).roundToInt().toString())
                                            }٪)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ext.danger,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        if (report.recommendedMonthlyReserveToman > 0L) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = if (ext.isDark) 0.55f else 0.88f),
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
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
                                        Icon(
                                            imageVector = Icons.Default.Savings,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "ذخیره احتیاطی ماهانه برای رفع کسری:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    FormattedMoneyText(
                                        amountToman = report.recommendedMonthlyReserveToman,
                                        unit = unit,
                                        privacyMasked = privacyMasked,
                                        style = MaterialTheme.typography.labelLarge,
                                        numberColor = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Interactive What-If Scenario Selector + 6M / 12M Horizon Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TarazinoSegmentedControl(
                    options = listOf("۶ ماه آینده", "۱۲ ماه آینده"),
                    selectedIndex = horizonTabIndex,
                    onSelectIndex = { horizonTabIndex = it },
                    compact = true,
                    testTagPrefix = "ml_horizon_seg",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ForecastScenarioMode.entries.forEachIndexed { idx, mode ->
                    val active = idx == scenarioIndex
                    Surface(
                        shape = RoundedCornerShape(11.dp),
                        color = if (active) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
                        },
                        border = BorderStroke(
                            width = if (active) 1.2.dp else 0.8.dp,
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(11.dp))
                            .tarazinoPressScale(scaleDown = 0.95f) { scenarioIndex = idx }
                            .testTag("ml_scenario_chip_${mode.name}")
                    ) {
                        Text(
                            text = mode.labelFa,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Interactive ML Net Cashflow & Confidence-Band Trajectory Chart
            if (visiblePoints.isNotEmpty()) {
                MlCashflowTrajectoryChart(
                    points = visiblePoints,
                    selectedIndex = selectedIdx.coerceIn(0, visiblePoints.lastIndex),
                    onSelectIndex = { selectedIdx = it },
                    positiveColor = ext.positive,
                    warningColor = ext.warning,
                    dangerColor = ext.danger,
                    lineColor = MaterialTheme.colorScheme.primary,
                    gridColor = MaterialTheme.colorScheme.outlineVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Month Axis Labels synced to Chart Points
                Row(modifier = Modifier.fillMaxWidth()) {
                    visiblePoints.forEachIndexed { idx, pt ->
                        val isSel = idx == selectedIdx
                        val labelColor = when {
                            pt.riskTier == ForecastRiskTier.DEFICIT -> ext.danger
                            isSel -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text(
                            text = if (visiblePoints.size > 6) pt.monthShortFa.take(3) else pt.monthShortFa,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSel || pt.riskTier == ForecastRiskTier.DEFICIT) FontWeight.ExtraBold else FontWeight.Normal,
                            color = labelColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { selectedIdx = idx }
                                .padding(vertical = 2.dp)
                        )
                    }
                }
            }

            // 4. Selected Month Diagnostic Inspector Card
            focusedMonth?.let { month ->
                Spacer(modifier = Modifier.height(12.dp))
                val tierColor = when (month.riskTier) {
                    ForecastRiskTier.DEFICIT -> ext.danger
                    ForecastRiskTier.TIGHT -> ext.warning
                    ForecastRiskTier.HEALTHY -> ext.positive
                }
                AnimatedContent(
                    targetState = month,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
                    label = "MlMonthInspector"
                ) { m ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (ext.isDark) 0.48f else 0.62f),
                        border = BorderStroke(1.dp, tierColor.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "تحلیل مدل برای ${m.monthYearLabelFa}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                TarazinoStatusPill(
                                    text = m.riskTier.labelFa,
                                    accentColor = tierColor
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Projected Health Score Progress Bar + Deficit Probability
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "امتیاز سلامت پیش‌بینی‌شده: ${
                                        PersianCurrencyEngine.toPersianDigits(m.projectedHealthScore.toString())
                                    } از ۱۰۰",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = tierColor
                                )
                                Text(
                                    text = "احتمال کسری: ${
                                        PersianCurrencyEngine.toPersianDigits((m.deficitProbability * 100f).roundToInt().toString())
                                    }٪",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (m.deficitProbability >= 0.5f) ext.danger else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            TarazinoLuminousProgressBar(
                                progress = (m.projectedHealthScore / 100f).coerceIn(0f, 1f),
                                color = tierColor,
                                height = 6.5.dp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // 3-Column Financial Metric Breakdown
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MlMiniMetricCell(
                                    labelFa = "اقساط سررسید",
                                    valueText = PersianCurrencyEngine.formatAmount(m.scheduledDebtToman, unit, privacyMasked = privacyMasked),
                                    valueColor = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                MlMiniMetricCell(
                                    labelFa = "حاشیه ریسک ML",
                                    valueText = PersianCurrencyEngine.formatAmount(m.behavioralRiskBufferToman, unit, privacyMasked = privacyMasked),
                                    valueColor = if (m.behavioralRiskBufferToman > 0L) ext.warning else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                val netPrefix = if (m.projectedNetBalanceToman < 0L) "−" else "+"
                                val netAbsFormatted = PersianCurrencyEngine.formatAmount(
                                    abs(m.projectedNetBalanceToman),
                                    unit,
                                    privacyMasked = privacyMasked
                                )
                                MlMiniMetricCell(
                                    labelFa = if (m.projectedNetBalanceToman < 0L) "کسری پیش‌بینی" else "تراز مازاد",
                                    valueText = if (privacyMasked) netAbsFormatted else "$netPrefix$netAbsFormatted",
                                    valueColor = if (m.projectedNetBalanceToman < 0L) ext.danger else ext.positive,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "علت کلیدی: ${m.primaryDriverFa}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 5. Actionable ML Mitigation Recommendations
            if (report.actionableRecommendationsFa.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (ext.isDark) 0.30f else 0.42f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Insights,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "راهکارهای پیشنهادی مدل پیش‌بینی:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        report.actionableRecommendationsFa.forEach { rec ->
                            Text(
                                text = "• $rec",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MlMiniMetricCell(
    labelFa: String,
    valueText: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(11.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = labelFa,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Interactive Canvas Trajectory Chart displaying projected monthly net cashflow,
 * shaded 80% ML confidence interval band, zero break-even threshold with crimson deficit zone,
 * and tappable monthly risk nodes.
 */
@Composable
private fun MlCashflowTrajectoryChart(
    points: List<MonthlyMlForecastPoint>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    positiveColor: Color,
    warningColor: Color,
    dangerColor: Color,
    lineColor: Color,
    gridColor: Color
) {
    var revealed by remember(points.size) { mutableStateOf(false) }
    LaunchedEffect(points.size) { revealed = true }
    val revealProgress by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessVeryLow),
        label = "MlChartReveal"
    )

    val maxVal = max(
        points.maxOfOrNull { max(it.upperConfidenceNetToman, it.projectedNetBalanceToman) } ?: 1_000_000L,
        1_000_000L
    ).toFloat()
    val minVal = kotlin.math.min(
        points.minOfOrNull { kotlin.math.min(it.lowerConfidenceNetToman, it.projectedNetBalanceToman) } ?: -500_000L,
        -500_000L
    ).toFloat()
    val span = (maxVal - minVal).coerceAtLeast(1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(152.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
            .border(0.75.dp, gridColor.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .pointerInput(points) {
                detectTapGestures { offset ->
                    if (points.isNotEmpty() && size.width > 0) {
                        val rtlX = (size.width.toFloat() - offset.x).coerceIn(0f, size.width.toFloat())
                        val idx = ((rtlX / size.width.toFloat()) * points.size)
                            .toInt()
                            .coerceIn(0, points.lastIndex)
                        onSelectIndex(idx)
                    }
                }
            }
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(128.dp)) {
            val w = size.width
            val h = size.height
            val n = points.size
            if (n == 0) return@Canvas

            fun mapY(value: Long): Float {
                val norm = ((value.toFloat() - minVal) / span).coerceIn(0f, 1f)
                return h * (1f - norm)
            }

            // Align with Persian RTL month axis below: index 0 sits at the right edge, flowing leftward
            fun mapX(index: Int): Float {
                return if (n == 1) w / 2f else w - (w / n) * (index + 0.5f)
            }

            val zeroY = mapY(0L)

            // 1. Subtle crimson deficit zone fill below the zero break-even line
            if (zeroY < h) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            dangerColor.copy(alpha = 0.14f),
                            dangerColor.copy(alpha = 0.04f)
                        ),
                        startY = zeroY,
                        endY = h
                    ),
                    topLeft = Offset(0f, zeroY),
                    size = androidx.compose.ui.geometry.Size(w, h - zeroY)
                )
            }

            // 2. Dashed zero break-even threshold line
            drawLine(
                color = dangerColor.copy(alpha = 0.55f),
                start = Offset(0f, zeroY),
                end = Offset(w, zeroY),
                strokeWidth = 1.1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f), 0f)
            )

            // 3. ML 80% Confidence Interval Band Polygon
            if (n >= 2) {
                val bandPath = Path().apply {
                    points.forEachIndexed { idx, pt ->
                        val x = mapX(idx)
                        val y = zeroY + (mapY(pt.upperConfidenceNetToman) - zeroY) * revealProgress
                        if (idx == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    for (idx in n - 1 downTo 0) {
                        val pt = points[idx]
                        val x = mapX(idx)
                        val y = zeroY + (mapY(pt.lowerConfidenceNetToman) - zeroY) * revealProgress
                        lineTo(x, y)
                    }
                    close()
                }
                drawPath(
                    path = bandPath,
                    color = lineColor.copy(alpha = 0.14f)
                )

                // 4. Main Forecast Curve
                val curvePath = Path().apply {
                    points.forEachIndexed { idx, pt ->
                        val x = mapX(idx)
                        val y = zeroY + (mapY(pt.projectedNetBalanceToman) - zeroY) * revealProgress
                        if (idx == 0) {
                            moveTo(x, y)
                        } else {
                            val prevX = mapX(idx - 1)
                            val prevY = zeroY + (mapY(points[idx - 1].projectedNetBalanceToman) - zeroY) * revealProgress
                            val midX = (prevX + x) / 2f
                            cubicTo(midX, prevY, midX, y, x, y)
                        }
                    }
                }
                drawPath(
                    path = curvePath,
                    color = lineColor,
                    style = Stroke(
                        width = 2.4.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // 5. Risk-Colored Monthly Nodes + Selected Highlight Halo
            points.forEachIndexed { idx, pt ->
                val x = mapX(idx)
                val y = zeroY + (mapY(pt.projectedNetBalanceToman) - zeroY) * revealProgress
                val nodeColor = when (pt.riskTier) {
                    ForecastRiskTier.DEFICIT -> dangerColor
                    ForecastRiskTier.TIGHT -> warningColor
                    ForecastRiskTier.HEALTHY -> positiveColor
                }
                val isSelected = idx == selectedIndex
                if (isSelected) {
                    drawLine(
                        color = nodeColor.copy(alpha = 0.35f),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                    drawCircle(
                        color = nodeColor.copy(alpha = 0.24f),
                        radius = 10.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
                drawCircle(
                    color = Color.White,
                    radius = if (isSelected) 5.5.dp.toPx() else 4.dp.toPx(),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = nodeColor,
                    radius = if (isSelected) 4.dp.toPx() else 2.8.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
    }
}
