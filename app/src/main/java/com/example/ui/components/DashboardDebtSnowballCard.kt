package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.CurrencyUnit
import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.domain.EarlyPayoffSimulationResult
import com.example.ui.theme.LocalTarazinoColors

/**
 * Prioritizable Dashboard module highlighting the Debt Snowball queue (smallest balances first)
 * with 1-tap extra monthly payoff budget presets and projected debt-free acceleration.
 */
@Composable
fun DashboardDebtSnowballCard(
    simulation: EarlyPayoffSimulationResult?,
    extraBudgetToman: Long,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    onUpdateExtraBudget: (Long) -> Unit,
    onOpenStrategyTab: () -> Unit,
    onOpenIntelligenceTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTarazinoColors.current
    val accent = ext.positive
    val goldAccent = MaterialTheme.colorScheme.secondary

    TarazinoGlassCard(
        cornerRadius = 24.dp,
        accentBorderColor = accent,
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_debt_snowball_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        accent.copy(alpha = 0.24f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    )
                                )
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.AcUnit,
                            contentDescription = "ماژول گلوله برفی بدهی",
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "گلوله برفی بدهی",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "حذف سریع خرده‌بدهی‌ها برای آزادسازی نقدینگی ماهانه",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = accent.copy(alpha = 0.14f),
                    border = BorderStroke(0.75.dp, accent.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .clickable(onClick = onOpenStrategyTab)
                        .testTag("dashboard_snowball_open_strategy_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "استراتژی",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = accent
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            if (simulation == null || simulation.snowballOrder.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = accent.copy(alpha = 0.10f),
                    border = BorderStroke(0.8.dp, accent.copy(alpha = 0.28f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "تبریک! هیچ بدهی فعالی در صف گلوله برفی باقی نمانده است.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                // Acceleration KPI Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = accent.copy(alpha = if (ext.isDark) 0.16f else 0.10f),
                        border = BorderStroke(0.8.dp, accent.copy(alpha = 0.35f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "شتاب آزادی مالی",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            TarazinoAutoSizeText(
                                text = if (simulation.monthsSavedSnowball > 0) {
                                    "${PersianCurrencyEngine.toPersianDigits(simulation.monthsSavedSnowball.toString())} ماه زودتر"
                                } else {
                                    "${PersianCurrencyEngine.toPersianDigits(simulation.snowballMonthsToClear.toString())} ماه تا تسویه"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = accent,
                                maxLines = 1,
                                minFontSize = 10.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = goldAccent.copy(alpha = if (ext.isDark) 0.15f else 0.10f),
                        border = BorderStroke(0.8.dp, goldAccent.copy(alpha = 0.32f)),
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "تاریخ آزادی پیش‌بینی",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            TarazinoAutoSizeText(
                                text = JalaliCalendarEngine.formatJalaliShortText(
                                    simulation.projectedAcceleratedDateJalali
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = goldAccent,
                                maxLines = 1,
                                minFontSize = 10.sp
                            )
                        }
                    }
                }

                // Quick Extra Budget Boost Chips (Instant Snowball Simulation)
                val quickBudgets = listOf(1_000_000L, 3_000_000L, 5_000_000L, 10_000_000L)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "بودجه مازاد ماهانه گلوله برفی:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = PersianCurrencyEngine.formatAmount(
                                amountToman = extraBudgetToman,
                                unit = currencyUnit,
                                privacyMasked = privacyMasked
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = accent
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickBudgets.forEach { presetAmount ->
                            val isSelected = extraBudgetToman == presetAmount
                            val labelMil = PersianCurrencyEngine.toPersianDigits((presetAmount / 1_000_000L).toString())
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) {
                                    accent
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                },
                                border = BorderStroke(
                                    0.75.dp,
                                    if (isSelected) accent else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onUpdateExtraBudget(presetAmount) }
                                    .testTag("dashboard_snowball_budget_${presetAmount / 1_000_000L}m")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    TarazinoAutoSizeText(
                                        text = "${PersianCurrencyEngine.isolateLtr("+$labelMil")} میلیون",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        minFontSize = 8.5.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Top 3 Smallest-Balance Snowball Queue Steps
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    simulation.snowballOrder.take(3).forEachIndexed { idx, step ->
                        val stepAccent = if (idx == 0) accent else MaterialTheme.colorScheme.primary
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(
                                0.75.dp,
                                if (idx == 0) accent.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 11.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(stepAccent.copy(alpha = 0.18f))
                                    ) {
                                        Text(
                                            text = PersianCurrencyEngine.toPersianDigits((idx + 1).toString()),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = stepAccent
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = step.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${step.counterpartyName} • تسویه در ماه ${
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
                                    text = PersianCurrencyEngine.formatAmount(
                                        amountToman = step.remainingToman,
                                        unit = currencyUnit,
                                        privacyMasked = privacyMasked
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = stepAccent,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Footer Simulator Shortcut
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenIntelligenceTab)
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = goldAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "مقایسه کامل گلوله برفی و بهمن در شبیه‌ساز",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
