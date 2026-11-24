package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.CurrencyUnit
import com.example.core.PersianCurrencyEngine
import com.example.data.InstallmentEntity
import com.example.data.ObligationEntity
import com.example.domain.DebtFreeAccelerationReport
import com.example.domain.DebtFreeAcceleratorEngine
import com.example.domain.PayoffTrajectoryPoint
import com.example.domain.WhatIfExtraStepComparison
import com.example.ui.theme.LocalTarazinoColors
import kotlin.math.roundToLong

private enum class AcceleratorInputMode(val labelFa: String) {
    LIVE_PORTFOLIO("پرتفوی فعلی من"),
    CUSTOM_SLIDERS("تنظیم دستی با اسلایدر")
}

private val EXTRA_PRESET_STEPS_TOMAN = listOf(
    0L to "۰",
    1_000_000L to "${PersianCurrencyEngine.isolateLtr("+۱")} میلیون",
    2_000_000L to "${PersianCurrencyEngine.isolateLtr("+۲")} میلیون",
    5_000_000L to "${PersianCurrencyEngine.isolateLtr("+۵")} میلیون",
    10_000_000L to "${PersianCurrencyEngine.isolateLtr("+۱۰")} میلیون",
    20_000_000L to "${PersianCurrencyEngine.isolateLtr("+۲۰")} میلیون"
)

/**
 * Interactive Slider-Based **Debt-Free Accelerator Calculator** card.
 *
 * Lets users slide recurring extra monthly payments (and an optional one-time lump-sum boost)
 * to simulate in real time how much earlier they will reach their Persian (Shamsi) debt-free date,
 * how much interest they save, and visually compare Baseline vs. Accelerated payoff trajectories.
 */
@Composable
fun DebtFreeAcceleratorSliderCard(
    obligations: List<ObligationEntity>,
    installments: List<InstallmentEntity>,
    extraMonthlyBudgetToman: Long,
    onExtraMonthlyBudgetChange: (Long) -> Unit,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    modifier: Modifier = Modifier
) {
    val ext = LocalTarazinoColors.current
    val tealAccent = ext.positive
    val goldAccent = MaterialTheme.colorScheme.secondary

    val portfolioBaseline = remember(obligations, installments) {
        DebtFreeAcceleratorEngine.derivePortfolioBaseline(obligations, installments)
    }

    var inputMode by remember(portfolioBaseline.activeDebtsCount) {
        mutableStateOf(
            if (portfolioBaseline.totalRemainingDebtToman > 0L) {
                AcceleratorInputMode.LIVE_PORTFOLIO
            } else {
                AcceleratorInputMode.CUSTOM_SLIDERS
            }
        )
    }

    // Custom manual slider parameters
    var customDebtToman by remember { mutableLongStateOf(120_000_000L) }
    var customBaseMonthlyToman by remember { mutableLongStateOf(6_000_000L) }
    var customAnnualRatePct by remember { mutableDoubleStateOf(18.0) }
    var oneTimeLumpSumToman by remember { mutableLongStateOf(0L) }
    var showLumpSumControl by remember { mutableStateOf(false) }

    val activeTotalDebt = if (inputMode == AcceleratorInputMode.LIVE_PORTFOLIO) {
        portfolioBaseline.totalRemainingDebtToman
    } else {
        customDebtToman
    }

    val activeBaseMonthly = if (inputMode == AcceleratorInputMode.LIVE_PORTFOLIO) {
        portfolioBaseline.baseMonthlyPaymentToman
    } else {
        customBaseMonthlyToman
    }

    val activeAnnualRate = if (inputMode == AcceleratorInputMode.LIVE_PORTFOLIO) {
        portfolioBaseline.weightedAnnualRatePercent
    } else {
        customAnnualRatePct
    }

    val maxLumpSum = activeTotalDebt.coerceAtMost(100_000_000L).coerceAtLeast(0L)
    LaunchedEffect(maxLumpSum) {
        if (oneTimeLumpSumToman > maxLumpSum) {
            oneTimeLumpSumToman = maxLumpSum
        }
    }

    val report: DebtFreeAccelerationReport = remember(
        activeTotalDebt,
        activeBaseMonthly,
        extraMonthlyBudgetToman,
        activeAnnualRate,
        oneTimeLumpSumToman
    ) {
        DebtFreeAcceleratorEngine.simulateAcceleration(
            totalRemainingDebtToman = activeTotalDebt,
            baseMonthlyPaymentToman = activeBaseMonthly,
            extraMonthlyPaymentToman = extraMonthlyBudgetToman,
            annualInterestRatePercent = activeAnnualRate,
            oneTimeLumpSumBoostToman = oneTimeLumpSumToman
        )
    }

    TarazinoGlassCard(
        modifier = modifier.testTag("debt_free_accelerator_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            TarazinoSectionHeader(
                title = "ماشین‌حساب شتاب‌دهنده آزادی از بدهی",
                subtitle = "با اسلایدر پرداخت اضافه ببینید تاریخ شمسی آزادی مالی چقدر جلو می‌افتد",
                icon = Icons.Default.RocketLaunch,
                accentColor = tealAccent
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Source Mode Selector (Live Portfolio vs Custom Sliders)
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AcceleratorInputMode.entries.forEach { mode ->
                        val isSelected = inputMode == mode
                        val bgColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            },
                            label = "accelModeBg"
                        )
                        val textColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            label = "accelModeText"
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = bgColor,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { inputMode = mode }
                                .testTag("accelerator_mode_${mode.name.lowercase()}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = mode.labelFa,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Portfolio Summary Strip or Manual Sliders Drawer
            AnimatedVisibility(
                visible = inputMode == AcceleratorInputMode.LIVE_PORTFOLIO,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    if (portfolioBaseline.totalRemainingDebtToman > 0L) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "مانده ${PersianCurrencyEngine.toPersianDigits(portfolioBaseline.activeDebtsCount.toString())} بدهی فعال • نرخ میانگین ${PersianCurrencyEngine.toPersianDigits(portfolioBaseline.weightedAnnualRatePercent.toString())}٪",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                FormattedMoneyText(
                                    amountToman = portfolioBaseline.totalRemainingDebtToman,
                                    unit = currencyUnit,
                                    privacyMasked = privacyMasked,
                                    style = MaterialTheme.typography.titleSmall,
                                    numberColor = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "قسط پایه ماهانه",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FormattedMoneyText(
                                    amountToman = report.baseMonthlyPaymentToman,
                                    unit = currencyUnit,
                                    privacyMasked = privacyMasked,
                                    style = MaterialTheme.typography.labelLarge,
                                    numberColor = goldAccent
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "بدهی فعالی در پرتفوی ثبت نشده است.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = tealAccent.copy(alpha = 0.16f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { inputMode = AcceleratorInputMode.CUSTOM_SLIDERS }
                            ) {
                                Text(
                                    text = "تست با اسلایدر دستی",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = tealAccent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = inputMode == AcceleratorInputMode.CUSTOM_SLIDERS,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Custom Total Debt Slider
                        CustomParameterSliderRow(
                            label = "کل مانده بدهی فرضی",
                            valueText = PersianCurrencyEngine.formatAmount(customDebtToman, currencyUnit),
                            value = customDebtToman.toFloat(),
                            valueRange = 5_000_000f..500_000_000f,
                            onValueChange = { raw ->
                                customDebtToman = ((raw / 5_000_000f).roundToLong() * 5_000_000L)
                                    .coerceIn(5_000_000L, 500_000_000L)
                            },
                            accentColor = MaterialTheme.colorScheme.primary,
                            testTag = "accelerator_custom_debt_slider"
                        )
                        // Custom Base Monthly Payment Slider
                        CustomParameterSliderRow(
                            label = "قسط ماهانه فعلی",
                            valueText = PersianCurrencyEngine.formatAmount(customBaseMonthlyToman, currencyUnit),
                            value = customBaseMonthlyToman.toFloat(),
                            valueRange = 1_000_000f..40_000_000f,
                            onValueChange = { raw ->
                                customBaseMonthlyToman = ((raw / 500_000f).roundToLong() * 500_000L)
                                    .coerceIn(1_000_000L, 40_000_000L)
                            },
                            accentColor = goldAccent,
                            testTag = "accelerator_custom_base_slider"
                        )
                        // Custom APR Slider
                        CustomParameterSliderRow(
                            label = "نرخ سود سالانه",
                            valueText = "${PersianCurrencyEngine.toPersianDigits(customAnnualRatePct.toInt().toString())}٪",
                            value = customAnnualRatePct.toFloat(),
                            valueRange = 0f..36f,
                            onValueChange = { raw ->
                                customAnnualRatePct = raw.roundToLong().toDouble().coerceIn(0.0, 36.0)
                            },
                            accentColor = ext.warning,
                            testTag = "accelerator_custom_apr_slider"
                        )
                    }
                }
            }

            // 3. Hero Debt-Free Date Shift Banner
            DebtFreeDateShiftHeroBanner(
                report = report,
                tealAccent = tealAccent,
                goldAccent = goldAccent
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Primary Extra Monthly Payment Interactive Slider + Precision Steppers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "پرداخت اضافه ماهانه",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "گام ۵۰۰ هزار تومانی تا ۲۵ میلیون تومان",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            onExtraMonthlyBudgetChange((extraMonthlyBudgetToman - 500_000L).coerceAtLeast(0L))
                        },
                        enabled = extraMonthlyBudgetToman > 0L,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("accelerator_extra_minus_btn")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "کاهش ۵۰۰ هزار تومان پرداخت اضافه",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = tealAccent.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, tealAccent.copy(alpha = 0.35f))
                    ) {
                        FormattedMoneyText(
                            amountToman = extraMonthlyBudgetToman,
                            unit = currencyUnit,
                            privacyMasked = privacyMasked,
                            style = MaterialTheme.typography.titleSmall,
                            numberColor = tealAccent,
                            minFontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            onExtraMonthlyBudgetChange((extraMonthlyBudgetToman + 500_000L).coerceAtMost(25_000_000L))
                        },
                        enabled = extraMonthlyBudgetToman < 25_000_000L,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("accelerator_extra_plus_btn")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = tealAccent.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, tealAccent.copy(alpha = 0.45f)),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "افزایش ۵۰۰ هزار تومان پرداخت اضافه",
                                    tint = tealAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Slider(
                value = extraMonthlyBudgetToman.coerceIn(0L, 25_000_000L).toFloat(),
                onValueChange = { raw ->
                    val stepped = ((raw / 500_000f).roundToLong() * 500_000L)
                        .coerceIn(0L, 25_000_000L)
                    if (stepped != extraMonthlyBudgetToman) {
                        onExtraMonthlyBudgetChange(stepped)
                    }
                },
                valueRange = 0f..25_000_000f,
                steps = 49,
                colors = SliderDefaults.colors(
                    thumbColor = tealAccent,
                    activeTrackColor = tealAccent,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payoff_extra_budget_slider")
            )

            // Quick Preset Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EXTRA_PRESET_STEPS_TOMAN.forEach { (amount, labelFa) ->
                    val isSelected = extraMonthlyBudgetToman == amount
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) tealAccent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) tealAccent else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onExtraMonthlyBudgetChange(amount) }
                            .testTag("accelerator_preset_$amount")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = labelFa,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Optional One-Time Lump-Sum Boost Toggle & Slider
            if (maxLumpSum >= 1_000_000L) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLumpSumControl = !showLumpSumControl }
                                .padding(vertical = 4.dp)
                                .testTag("accelerator_lumpsum_toggle"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "تنظیم پرداخت یکجای کمکی",
                                    tint = goldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "پرداخت یکجای کمکی (پاداش / پس‌انداز)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = if (oneTimeLumpSumToman > 0L) {
                                    formatCompactPersianToman(oneTimeLumpSumToman, privacyMasked)
                                } else {
                                    if (showLumpSumControl) "بستن" else "افزودن +"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = goldAccent
                            )
                        }

                        AnimatedVisibility(visible = showLumpSumControl || oneTimeLumpSumToman > 0L) {
                            Column(modifier = Modifier.padding(top = 4.dp)) {
                                Slider(
                                    value = oneTimeLumpSumToman.coerceIn(0L, maxLumpSum).toFloat(),
                                    onValueChange = { raw ->
                                        val stepped = ((raw / 1_000_000f).roundToLong() * 1_000_000L)
                                            .coerceIn(0L, maxLumpSum)
                                        oneTimeLumpSumToman = stepped
                                    },
                                    valueRange = 0f..maxLumpSum.toFloat(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = goldAccent,
                                        activeTrackColor = goldAccent,
                                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("accelerator_lumpsum_slider")
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Dual-Curve Baseline vs. Accelerated Payoff Trajectory Chart
            if (report.trajectoryPoints.size >= 2) {
                PayoffTrajectoryComparisonChart(
                    points = report.trajectoryPoints,
                    baselineMonths = report.baselineMonthsToDebtFree,
                    acceleratedMonths = report.acceleratedMonthsToDebtFree,
                    tealAccent = tealAccent,
                    goldAccent = goldAccent
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 7. 3-Column Impact KPI Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AcceleratorKpiTile(
                    label = "صرفه‌جویی زمانی",
                    valueText = if (report.monthsSaved > 0) {
                        "${PersianCurrencyEngine.toPersianDigits(report.monthsSaved.toString())} ماه زودتر"
                    } else {
                        "۰ ماه"
                    },
                    subText = if (report.approxDaysSaved > 0) {
                        "≈ ${PersianCurrencyEngine.toPersianDigits(report.approxDaysSaved.toString())} روز"
                    } else {
                        "روند عادی"
                    },
                    accentColor = tealAccent,
                    modifier = Modifier.weight(1f)
                )
                AcceleratorKpiTile(
                    label = "صرفه‌جویی سود",
                    valueText = formatCompactPersianToman(report.totalInterestSavedToman, privacyMasked),
                    subText = if (report.effectiveAnnualRatePercent > 0.0) {
                        "نرخ ${PersianCurrencyEngine.toPersianDigits(report.effectiveAnnualRatePercent.toString())}٪"
                    } else {
                        "قرض‌الحسنه / بدون سود"
                    },
                    accentColor = goldAccent,
                    modifier = Modifier.weight(1f)
                )
                AcceleratorKpiTile(
                    label = "قسط ماهانه جدید",
                    valueText = formatCompactPersianToman(report.newTotalMonthlyPaymentToman, privacyMasked),
                    subText = "پایه + مازاد",
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            // 8. Tappable What-If Step Comparison Ladder
            if (report.quickStepComparisons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "مقایسه سریع سناریوهای پرداخت اضافه (برای اعمال ضربه بزنید)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    report.quickStepComparisons.forEach { step ->
                        WhatIfComparisonRow(
                            step = step,
                            isSelected = extraMonthlyBudgetToman == step.extraMonthlyToman,
                            privacyMasked = privacyMasked,
                            tealAccent = tealAccent,
                            goldAccent = goldAccent,
                            onSelect = { onExtraMonthlyBudgetChange(step.extraMonthlyToman) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 9. Coaching Insight Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = tealAccent.copy(alpha = 0.10f),
                border = BorderStroke(0.9.dp, tealAccent.copy(alpha = 0.30f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = tealAccent,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = report.coachingHeadlineFa,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = report.coachingDetailFa,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DebtFreeDateShiftHeroBanner(
    report: DebtFreeAccelerationReport,
    tealAccent: Color,
    goldAccent: Color
) {
    val gradientBg = Brush.horizontalGradient(
        colors = listOf(
            tealAccent.copy(alpha = 0.16f),
            goldAccent.copy(alpha = 0.12f)
        )
    )
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, tealAccent.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(gradientBg)
            .testTag("accelerator_hero_date_shift_banner")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = tealAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "جلو افتادن تاریخ آزادی از بدهی",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (report.monthsSaved > 0) tealAccent else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (report.monthsSaved > 0) {
                            "${PersianCurrencyEngine.toPersianDigits(report.accelerationPercent.toString())}٪ سریع‌تر"
                        } else {
                            "مسیر پایه"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (report.monthsSaved > 0) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Baseline Date
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "تاریخ عادی",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    TarazinoAutoSizeText(
                        text = report.baselineDebtFreeMonthLabelFa,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = goldAccent,
                        maxLines = 1,
                        minFontSize = 11.sp
                    )
                    Text(
                        text = "${PersianCurrencyEngine.toPersianDigits(report.baselineMonthsToDebtFree.toString())} ماه",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Shift Arrow Pill
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = tealAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    if (report.monthsSaved > 0) {
                        Text(
                            text = "-${PersianCurrencyEngine.toPersianDigits(report.monthsSaved.toString())} ماه",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = tealAccent
                        )
                    }
                }

                // Accelerated Date
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "تاریخ جدید آزادی",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    TarazinoAutoSizeText(
                        text = report.acceleratedDebtFreeMonthLabelFa,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = tealAccent,
                        maxLines = 1,
                        minFontSize = 12.sp,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "${PersianCurrencyEngine.toPersianDigits(report.acceleratedMonthsToDebtFree.toString())} ماه",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = tealAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun PayoffTrajectoryComparisonChart(
    points: List<PayoffTrajectoryPoint>,
    baselineMonths: Int,
    acceleratedMonths: Int,
    tealAccent: Color,
    goldAccent: Color
) {
    // Capture all theme colors outside Canvas DrawScope
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val cardBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBgColor,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("accelerator_trajectory_chart")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "نمودار مقایسه کاهش مانده بدهی",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDotLabel(color = goldAccent, label = "مسیر عادی", dashed = true)
                    LegendDotLabel(color = tealAccent, label = "شتاب‌یافته", dashed = false)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                val width = size.width
                val height = size.height
                val padX = 12.dp.toPx()
                val padTop = 10.dp.toPx()
                val padBottom = 10.dp.toPx()
                val plotW = (width - padX * 2f).coerceAtLeast(1f)
                val plotH = (height - padTop - padBottom).coerceAtLeast(1f)

                val maxBalance = points.maxOfOrNull {
                    maxOf(it.baselineRemainingToman, it.acceleratedRemainingToman)
                }?.coerceAtLeast(1L) ?: 1L
                val maxIndex = (points.size - 1).coerceAtLeast(1)

                // Horizontal subtle grid lines
                for (step in 0..3) {
                    val y = padTop + (plotH * step / 3f)
                    drawLine(
                        color = gridColor,
                        start = Offset(padX, y),
                        end = Offset(width - padX, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                fun xAt(index: Int): Float = padX + (index.toFloat() / maxIndex.toFloat()) * plotW
                fun yAt(balance: Long): Float =
                    padTop + plotH - ((balance.toFloat() / maxBalance.toFloat()).coerceIn(0f, 1f) * plotH)

                // Baseline Path (Dashed Champagne Gold)
                val baselinePath = Path()
                points.forEachIndexed { idx, pt ->
                    val x = xAt(idx)
                    val y = yAt(pt.baselineRemainingToman)
                    if (idx == 0) baselinePath.moveTo(x, y) else baselinePath.lineTo(x, y)
                }
                drawPath(
                    path = baselinePath,
                    color = goldAccent,
                    style = Stroke(
                        width = 2.2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                )

                // Accelerated Path + Gradient Area Fill (Solid Deep Persian Teal)
                val accelPath = Path()
                val accelFillPath = Path()
                val accelEndIdx = acceleratedMonths.coerceIn(0, maxIndex)
                for (idx in 0..accelEndIdx) {
                    val pt = points[idx]
                    val x = xAt(idx)
                    val y = yAt(pt.acceleratedRemainingToman)
                    if (idx == 0) {
                        accelPath.moveTo(x, y)
                        accelFillPath.moveTo(x, padTop + plotH)
                        accelFillPath.lineTo(x, y)
                    } else {
                        accelPath.lineTo(x, y)
                        accelFillPath.lineTo(x, y)
                    }
                }
                accelFillPath.lineTo(xAt(accelEndIdx), padTop + plotH)
                accelFillPath.close()

                drawPath(
                    path = accelFillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            tealAccent.copy(alpha = 0.30f),
                            tealAccent.copy(alpha = 0.02f)
                        ),
                        startY = padTop,
                        endY = padTop + plotH
                    ),
                    style = Fill
                )
                drawPath(
                    path = accelPath,
                    color = tealAccent,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Zero-crossing milestone markers
                val baseEndX = xAt(baselineMonths.coerceIn(0, maxIndex))
                val baseEndY = padTop + plotH
                drawCircle(color = goldAccent, radius = 4.5.dp.toPx(), center = Offset(baseEndX, baseEndY))
                drawCircle(color = surfaceColor, radius = 2.dp.toPx(), center = Offset(baseEndX, baseEndY))

                val accelEndX = xAt(accelEndIdx)
                val accelEndY = padTop + plotH
                drawCircle(color = tealAccent.copy(alpha = 0.25f), radius = 8.dp.toPx(), center = Offset(accelEndX, accelEndY))
                drawCircle(color = tealAccent, radius = 5.dp.toPx(), center = Offset(accelEndX, accelEndY))
                drawCircle(color = surfaceColor, radius = 2.dp.toPx(), center = Offset(accelEndX, accelEndY))
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = points.first().shamsiMonthLabelFa,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (acceleratedMonths in 1 until baselineMonths) {
                    Text(
                        text = "آزادی جدید: ${points[acceleratedMonths.coerceIn(0, points.lastIndex)].shamsiMonthLabelFa}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = tealAccent
                    )
                }
                Text(
                    text = points.last().shamsiMonthLabelFa,
                    style = MaterialTheme.typography.labelSmall,
                    color = goldAccent
                )
            }
        }
    }
}

@Composable
private fun LegendDotLabel(color: Color, label: String, dashed: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(if (dashed) 12.dp else 10.dp)
                .height(if (dashed) 2.5.dp else 10.dp)
                .clip(if (dashed) RoundedCornerShape(2.dp) else CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AcceleratorKpiTile(
    label: String,
    valueText: String,
    subText: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.28f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            TarazinoAutoSizeText(
                text = valueText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                maxLines = 1,
                minFontSize = 9.5.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subText,
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
private fun WhatIfComparisonRow(
    step: WhatIfExtraStepComparison,
    isSelected: Boolean,
    privacyMasked: Boolean,
    tealAccent: Color,
    goldAccent: Color,
    onSelect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            tealAccent.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)
        },
        border = BorderStroke(
            width = if (isSelected) 1.2.dp else 0.8.dp,
            color = if (isSelected) tealAccent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect)
            .testTag("accelerator_whatif_row_${step.extraMonthlyToman}")
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
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) tealAccent else goldAccent.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = "${PersianCurrencyEngine.formatCompactAmount(step.extraMonthlyToman, showPlusSign = true)} در ماه",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else goldAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = step.debtFreeShamsiMonthLabelFa,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (step.interestSavedToman > 0L) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = goldAccent,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "صرفه‌جویی سود: ${formatCompactPersianToman(step.interestSavedToman, privacyMasked)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = tealAccent.copy(alpha = 0.14f),
                modifier = Modifier.widthIn(min = 70.dp)
            ) {
                Text(
                    text = if (step.monthsSaved > 0) {
                        "${PersianCurrencyEngine.toPersianDigits(step.monthsSaved.toString())} ماه زودتر"
                    } else {
                        "${PersianCurrencyEngine.toPersianDigits(step.monthsToDebtFree.toString())} ماه"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = tealAccent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CustomParameterSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    accentColor: Color,
    testTag: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}
