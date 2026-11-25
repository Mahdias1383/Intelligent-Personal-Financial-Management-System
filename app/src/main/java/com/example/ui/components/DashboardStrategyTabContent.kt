package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.CurrencyUnit
import com.example.core.PersianCurrencyEngine
import com.example.data.CounterpartyEntity
import com.example.data.EntityCategory
import com.example.data.InstallmentEntity
import com.example.data.ObligationEntity
import com.example.domain.DebtPayoffStrategyRankingEngine
import com.example.domain.DebtPayoffStrategyReport
import com.example.domain.DebtUrgencyLevel
import com.example.domain.PayoffRankingMode
import com.example.domain.RankedPayoffDebtItem
import com.example.ui.theme.LocalTarazinoColors
import kotlin.math.roundToLong

private val STRATEGY_EXTRA_PRESETS = listOf(
    0L to "۰",
    1_000_000L to "${PersianCurrencyEngine.isolateLtr("+۱")} میلیون",
    3_000_000L to "${PersianCurrencyEngine.isolateLtr("+۳")} میلیون",
    5_000_000L to "${PersianCurrencyEngine.isolateLtr("+۵")} میلیون",
    10_000_000L to "${PersianCurrencyEngine.isolateLtr("+۱۰")} میلیون"
)

/**
 * Dedicated **Strategy** tab view displayed inside the Dashboard.
 *
 * Automatically ranks all existing active debts by effective interest rate (APR) and urgency,
 * suggests an optimized **Pay-Off Order** to maximize interest savings, visualizes the Interest
 * vs. Urgency weight breakdown, and provides direct Quick-Pay & Inspect actions on every target.
 */
@Composable
fun DashboardStrategyTabContent(
    obligations: List<ObligationEntity>,
    installments: List<InstallmentEntity>,
    counterparties: List<CounterpartyEntity>,
    extraPayoffBudgetToman: Long,
    onUpdateExtraPayoffBudget: (Long) -> Unit,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    onQuickPay: (InstallmentEntity) -> Unit,
    onOpenObligationInTree: (ObligationEntity, EntityCategory?) -> Unit,
    onOpenNewObligationWizard: () -> Unit,
    onOpenIntelligenceTab: () -> Unit,
    monthlyIncomeToman: Long = 0L,
    autoPaymentScheduleEnabled: Boolean = false,
    preferredAutoPayDayOfMonth: Int = 0,
    preferredSecondaryPayDayOfMonth: Int = 0,
    onSaveAutomatedSchedule: (enabled: Boolean, primaryDay: Int, secondaryDay: Int?) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val ext = LocalTarazinoColors.current
    val tealAccent = ext.positive
    val goldAccent = MaterialTheme.colorScheme.secondary
    val dangerAccent = ext.danger

    var rankingMode by remember { mutableStateOf(PayoffRankingMode.INTEREST_AND_URGENCY) }

    val report: DebtPayoffStrategyReport = remember(
        obligations,
        installments,
        counterparties,
        rankingMode,
        extraPayoffBudgetToman
    ) {
        DebtPayoffStrategyRankingEngine.buildStrategyReport(
            obligations = obligations,
            installments = installments,
            counterparties = counterparties,
            rankingMode = rankingMode,
            extraMonthlyBudgetToman = extraPayoffBudgetToman
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_strategy_tab_content"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Strategy Command Card
        StrategyCommandHeroCard(
            report = report,
            tealAccent = tealAccent,
            goldAccent = goldAccent,
            dangerAccent = dangerAccent,
            privacyMasked = privacyMasked
        )

        // 1.5. Automated Optimal Payment-Day Scheduler Based on Historical Income Cycles
        StrategyAutomatedPaymentSchedulerCard(
            obligations = obligations,
            installments = installments,
            fallbackMonthlyIncomeToman = monthlyIncomeToman,
            currencyUnit = currencyUnit,
            privacyMasked = privacyMasked,
            autoScheduleEnabled = autoPaymentScheduleEnabled,
            savedAutoPayPrimaryDay = preferredAutoPayDayOfMonth,
            savedAutoPaySecondaryDay = preferredSecondaryPayDayOfMonth,
            onSaveAutomatedSchedule = onSaveAutomatedSchedule
        )

        // 2. Ranking Mode Selector + Extra Monthly Attack Budget Control
        TarazinoGlassCard {
            Column(modifier = Modifier.padding(16.dp)) {
                TarazinoSectionHeader(
                    title = "معیار رتبه‌بندی و توان تسویه ماهانه",
                    subtitle = rankingMode.descriptionFa,
                    icon = Icons.Default.AutoAwesome,
                    accentColor = tealAccent
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3-Mode Ranking Switcher
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PayoffRankingMode.entries.forEach { mode ->
                            val selected = rankingMode == mode
                            val bg by animateColorAsState(
                                targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                label = "rankModeBg"
                            )
                            val fg by animateColorAsState(
                                targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "rankModeFg"
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = bg,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { rankingMode = mode }
                                    .testTag("strategy_ranking_mode_${mode.name.lowercase()}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    TarazinoAutoSizeText(
                                        text = mode.shortLabelFa,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = fg,
                                        maxLines = 1,
                                        minFontSize = 9.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Extra Monthly Budget Slider for Amplifying Interest Savings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "بودجه مازاد ماهانه روی هدف شماره ۱:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    FormattedMoneyText(
                        amountToman = extraPayoffBudgetToman,
                        unit = currencyUnit,
                        privacyMasked = privacyMasked,
                        style = MaterialTheme.typography.labelLarge,
                        numberColor = tealAccent
                    )
                }

                Slider(
                    value = extraPayoffBudgetToman.coerceIn(0L, 20_000_000L).toFloat(),
                    onValueChange = { raw ->
                        val stepped = ((raw / 500_000f).roundToLong() * 500_000L)
                            .coerceIn(0L, 20_000_000L)
                        if (stepped != extraPayoffBudgetToman) {
                            onUpdateExtraPayoffBudget(stepped)
                        }
                    },
                    valueRange = 0f..20_000_000f,
                    steps = 39,
                    colors = SliderDefaults.colors(
                        thumbColor = tealAccent,
                        activeTrackColor = tealAccent,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("strategy_extra_budget_slider")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    STRATEGY_EXTRA_PRESETS.forEach { (amount, labelFa) ->
                        val isSel = extraPayoffBudgetToman == amount
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isSel) tealAccent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                1.dp,
                                if (isSel) tealAccent else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onUpdateExtraPayoffBudget(amount) }
                                .testTag("strategy_extra_preset_$amount")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = labelFa,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Empty State OR Interest-vs-Urgency Matrix + Ranked Pay-Off Order Queue
        if (report.rankedDebts.isEmpty()) {
            StrategyDebtFreeEmptyCard(
                tealAccent = tealAccent,
                onOpenNewObligationWizard = onOpenNewObligationWizard,
                onOpenIntelligenceTab = onOpenIntelligenceTab
            )
        } else {
            // Priority Breakdown Matrix Bar Chart
            InterestUrgencyMatrixCard(
                rankedDebts = report.rankedDebts,
                tealAccent = tealAccent,
                goldAccent = goldAccent,
                dangerAccent = dangerAccent
            )

            // Section Title for Ranked Pay-Off Queue
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = goldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ترتیب پیشنهادی تسویه",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${PersianCurrencyEngine.toPersianDigits(report.rankedDebts.size.toString())} پرونده فعال",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Ranked Pay-Off Order Cards (#1, #2, #3...)
            report.rankedDebts.forEach { item ->
                RankedPayoffDebtCard(
                    item = item,
                    currencyUnit = currencyUnit,
                    privacyMasked = privacyMasked,
                    tealAccent = tealAccent,
                    goldAccent = goldAccent,
                    dangerAccent = dangerAccent,
                    onQuickPay = onQuickPay,
                    onOpenDetail = {
                        onOpenObligationInTree(item.obligation, item.counterparty?.categoryEnum)
                    }
                )
            }

            // Deep Tools CTA Footer
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(0.9.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenIntelligenceTab)
                    .testTag("strategy_open_full_intelligence_btn")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Insights,
                            contentDescription = null,
                            tint = tealAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "شبیه‌سازهای پیشرفته (تجمیع وام، گلوله برفی و شتاب‌دهنده)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "مشاهده ابزارهای کامل تحلیل مالی در تب هوشمند",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "ورود به تب تحلیل هوشمند",
                        tint = tealAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun StrategyCommandHeroCard(
    report: DebtPayoffStrategyReport,
    tealAccent: Color,
    goldAccent: Color,
    dangerAccent: Color,
    privacyMasked: Boolean
) {
    val heroGradient = Brush.linearGradient(
        colors = listOf(
            tealAccent.copy(alpha = 0.22f),
            goldAccent.copy(alpha = 0.14f)
        )
    )

    TarazinoGlassCard(
        accentBorderColor = tealAccent.copy(alpha = 0.45f),
        modifier = Modifier.testTag("strategy_hero_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(heroGradient)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = null,
                        tint = tealAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "استراتژی هوشمند تسویه بدهی",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "بیشترین صرفه‌جویی سود بر اساس نرخ سود و فوریت",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (report.urgentDebtsCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = dangerAccent.copy(alpha = 0.18f),
                        border = BorderStroke(0.8.dp, dangerAccent.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "${PersianCurrencyEngine.toPersianDigits(report.urgentDebtsCount.toString())} فوری",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = dangerAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Hero KPI Tiles
            val displaySavings = maxOf(
                report.totalEstimatedInterestSavedVsBaselineToman,
                report.interestSavedByOrderVsReverseToman
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StrategyHeroMiniTile(
                    label = "صرفه‌جویی سود",
                    value = formatCompactPersianToman(displaySavings, privacyMasked),
                    sub = if (report.monthsSavedVsMinimumOnly > 0) {
                        "${PersianCurrencyEngine.toPersianDigits(report.monthsSavedVsMinimumOnly.toString())} ماه زودتر"
                    } else {
                        "بهینه ترتیب سود"
                    },
                    accent = tealAccent,
                    modifier = Modifier.weight(1f)
                )
                StrategyHeroMiniTile(
                    label = "بالاترین نرخ سود",
                    value = "${PersianCurrencyEngine.toPersianDigits(report.highestAprPercent.toString())}٪",
                    sub = "میانگین ${PersianCurrencyEngine.toPersianDigits(report.weightedAverageAprPercent.toString())}٪",
                    accent = if (report.highestAprPercent >= 20.0) dangerAccent else goldAccent,
                    modifier = Modifier.weight(1f)
                )
                StrategyHeroMiniTile(
                    label = "آزادی از بدهی",
                    value = report.projectedDebtFreeShamsiLabelFa,
                    sub = "${PersianCurrencyEngine.toPersianDigits(report.projectedDebtFreeMonths.toString())} ماه",
                    accent = goldAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                border = BorderStroke(0.8.dp, tealAccent.copy(alpha = 0.28f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(11.dp)) {
                    Text(
                        text = report.topPriorityHeadlineFa,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = report.strategyInsightFa,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StrategyHeroMiniTile(
    label: String,
    value: String,
    sub: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(13.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        border = BorderStroke(0.8.dp, accent.copy(alpha = 0.30f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            TarazinoAutoSizeText(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                maxLines = 1,
                minFontSize = 9.5.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun InterestUrgencyMatrixCard(
    rankedDebts: List<RankedPayoffDebtItem>,
    tealAccent: Color,
    goldAccent: Color,
    dangerAccent: Color
) {
    // Capture colors outside Canvas DrawScope
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    val interestBarColor = tealAccent
    val urgencyBarColor = goldAccent
    val topItems = rankedDebts.take(5)

    TarazinoGlassCard(modifier = Modifier.testTag("strategy_priority_matrix_card")) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ماتریس امتیاز اولویت (نرخ سود + فوریت)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PriorityLegendChip(color = interestBarColor, label = "نرخ سود (۵۵)")
                    PriorityLegendChip(color = urgencyBarColor, label = "فوریت (۴۵)")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                topItems.forEach { item ->
                    val urgencyColor = if (item.urgencyLevel == DebtUrgencyLevel.CRITICAL) dangerAccent else urgencyBarColor
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${PersianCurrencyEngine.toPersianDigits(item.rank.toString())} ${item.obligation.title}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${PersianCurrencyEngine.toPersianDigits(item.compositePriorityScore.toString())} / ۱۰۰",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (item.rank == 1) tealAccent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(6.dp))
                        ) {
                            val w = size.width
                            val h = size.height
                            val r = CornerRadius(h / 2f, h / 2f)

                            drawRoundRect(
                                color = trackColor,
                                topLeft = Offset.Zero,
                                size = Size(w, h),
                                cornerRadius = r
                            )
                            val interestW = w * (item.interestRateScore / 100f).coerceIn(0f, 1f)
                            val urgencyW = w * (item.urgencyScore / 100f).coerceIn(0f, (1f - interestW / w).coerceAtLeast(0f))
                            if (interestW > 0f) {
                                drawRoundRect(
                                    color = interestBarColor,
                                    topLeft = Offset.Zero,
                                    size = Size(interestW, h),
                                    cornerRadius = r
                                )
                            }
                            if (urgencyW > 0f) {
                                drawRoundRect(
                                    color = urgencyColor,
                                    topLeft = Offset(interestW, 0f),
                                    size = Size(urgencyW, h),
                                    cornerRadius = r
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PriorityLegendChip(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RankedPayoffDebtCard(
    item: RankedPayoffDebtItem,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    tealAccent: Color,
    goldAccent: Color,
    dangerAccent: Color,
    onQuickPay: (InstallmentEntity) -> Unit,
    onOpenDetail: () -> Unit
) {
    val isTopTarget = item.rank == 1
    val urgencyColor = when (item.urgencyLevel) {
        DebtUrgencyLevel.CRITICAL -> dangerAccent
        DebtUrgencyLevel.HIGH -> goldAccent
        DebtUrgencyLevel.MODERATE -> MaterialTheme.colorScheme.primary
        DebtUrgencyLevel.LOW -> tealAccent
    }

    TarazinoGlassCard(
        accentBorderColor = if (isTopTarget) tealAccent.copy(alpha = 0.55f) else null,
        modifier = Modifier.testTag("strategy_ranked_debt_${item.rank}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Rank Badge + Debt Title + APR & Urgency Chips
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isTopTarget) {
                                    Brush.linearGradient(listOf(tealAccent, goldAccent))
                                } else {
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )
                                }
                            )
                    ) {
                        Text(
                            text = "#${PersianCurrencyEngine.toPersianDigits(item.rank.toString())}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = if (isTopTarget) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.obligation.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.counterparty?.name ?: "طرف حساب"} • ${item.categoryTag.shortBadgeFa}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = urgencyColor.copy(alpha = 0.16f),
                        border = BorderStroke(0.7.dp, urgencyColor.copy(alpha = 0.45f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            if (item.urgencyLevel == DebtUrgencyLevel.CRITICAL) {
                                Icon(
                                    imageVector = Icons.Default.PriorityHigh,
                                    contentDescription = null,
                                    tint = urgencyColor,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(
                                text = item.urgencyLevel.badgeFa,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = urgencyColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "امتیاز ${PersianCurrencyEngine.toPersianDigits(item.compositePriorityScore.toString())}/۱۰۰",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isTopTarget) tealAccent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Rationale Badges Row (APR, Overdue, Late Fee, Critical)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item.rationaleBadgesFa.forEach { badge ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Financial Metrics Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f))
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "مانده کل بدهی",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FormattedMoneyText(
                        amountToman = item.remainingBalanceToman,
                        unit = currencyUnit,
                        privacyMasked = privacyMasked,
                        style = MaterialTheme.typography.labelLarge,
                        numberColor = MaterialTheme.colorScheme.onSurface,
                        minFontSize = 9.5.sp
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = urgencyColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "قسط بعدی",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = item.relativeDueLabelFa,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = urgencyColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "پایان پیش‌بینی‌شده",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.projectedPayoffShamsiLabelFa,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = tealAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Actionable Guidance
            Text(
                text = item.actionGuidanceFa,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Direct Action Buttons (48dp accessible touch height)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item.nextUnpaidInstallment?.let { nextInst ->
                    Button(
                        onClick = { onQuickPay(nextInst) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTopTarget) tealAccent else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("strategy_quick_pay_${item.obligation.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        TarazinoAutoSizeText(
                            text = "پرداخت قسط (${formatCompactPersianToman(nextInst.remainingToman, privacyMasked)})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 1,
                            minFontSize = 9.5.sp
                        )
                    }
                }

                OutlinedButton(
                    onClick = onOpenDetail,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .weight(0.8f)
                        .height(48.dp)
                        .testTag("strategy_open_detail_${item.obligation.id}")
                ) {
                    Text(
                        text = "جزئیات",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StrategyDebtFreeEmptyCard(
    tealAccent: Color,
    onOpenNewObligationWizard: () -> Unit,
    onOpenIntelligenceTab: () -> Unit
) {
    TarazinoGlassCard(modifier = Modifier.testTag("strategy_empty_state_card")) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = tealAccent,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "بدون بدهی فعال — آزادی کامل مالی!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "با ثبت هر وام یا خرید اقساطی جدید، این تب به‌صورت خودکار ترتیب بهینه تسویه را بر اساس نرخ سود و فوریت پیشنهاد می‌دهد.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onOpenNewObligationWizard,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ثبت تعهد جدید")
                }
                OutlinedButton(
                    onClick = onOpenIntelligenceTab,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("ابزارهای هوشمند")
                }
            }
        }
    }
}
