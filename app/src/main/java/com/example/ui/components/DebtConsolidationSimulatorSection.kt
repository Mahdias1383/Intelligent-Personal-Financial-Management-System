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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.InstallmentEntity
import com.example.data.ObligationEntity
import com.example.domain.ConsolidatableDebtItem
import com.example.domain.ConsolidationVerdict
import com.example.domain.DebtConsolidationEngine
import com.example.domain.DebtConsolidationReport
import com.example.ui.theme.LocalTarazinoColors
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class ConsolidationSourceMode(val labelFa: String) {
    LIVE_PORTFOLIO("بدهی‌های فعلی من"),
    SAMPLE_SCENARIO("سناریوی نمونه مقایسه")
}

private val LOAN_RATE_PRESETS = listOf(
    4.0 to "قرض‌الحسنه ۴٪",
    14.0 to "وام ۱۴٪",
    18.0 to "وام بانکی ۱۸٪",
    23.0 to "تسهیلات ۲۳٪"
)

/**
 * Interactive **Debt Consolidation Simulator** section.
 *
 * Analyzes the user's current active debts (or a realistic sample scenario), lets the user toggle
 * individual debts to roll into a single consolidation loan, tune the new loan's APR, repayment
 * duration, and bank origination fee, and immediately computes whether consolidating is
 * mathematically advantageous.
 */
@Composable
fun DebtConsolidationSimulatorSection(
    obligations: List<ObligationEntity>,
    installments: List<InstallmentEntity>,
    counterparties: List<CounterpartyEntity>,
    monthlyIncomeToman: Long,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    modifier: Modifier = Modifier
) {
    val ext = LocalTarazinoColors.current
    val tealAccent = ext.positive
    val goldAccent = MaterialTheme.colorScheme.secondary
    val dangerAccent = ext.danger

    val liveDebts = remember(obligations, installments, counterparties) {
        DebtConsolidationEngine.extractConsolidatableDebts(
            obligations = obligations,
            installments = installments,
            counterparties = counterparties
        )
    }
    val sampleDebts = remember { DebtConsolidationEngine.sampleScenarioDebts() }

    var sourceMode by remember {
        mutableStateOf(ConsolidationSourceMode.LIVE_PORTFOLIO)
    }

    val activeDebts = if (sourceMode == ConsolidationSourceMode.LIVE_PORTFOLIO) liveDebts else sampleDebts

    var newLoanRatePct by remember { mutableDoubleStateOf(14.0) }
    var newLoanTermMonths by remember { mutableIntStateOf(24) }
    var originationFeePct by remember { mutableDoubleStateOf(1.5) }

    var selectedDebtIds by remember(sourceMode, activeDebts) {
        mutableStateOf(
            DebtConsolidationEngine.recommendOptimalSubsetIds(
                allDebts = activeDebts,
                consolidationAnnualRatePercent = 14.0
            )
        )
    }

    // Keep selected IDs valid whenever activeDebts changes
    LaunchedEffect(activeDebts) {
        val validIds = activeDebts.map { it.id }.toSet()
        val intersection = selectedDebtIds.intersect(validIds)
        if (intersection.isEmpty() && activeDebts.isNotEmpty()) {
            selectedDebtIds = DebtConsolidationEngine.recommendOptimalSubsetIds(
                allDebts = activeDebts,
                consolidationAnnualRatePercent = newLoanRatePct
            )
        } else if (intersection.size != selectedDebtIds.size) {
            selectedDebtIds = intersection
        }
    }

    val report: DebtConsolidationReport = remember(
        activeDebts,
        selectedDebtIds,
        newLoanRatePct,
        newLoanTermMonths,
        originationFeePct,
        monthlyIncomeToman
    ) {
        DebtConsolidationEngine.simulateConsolidation(
            allDebts = activeDebts,
            selectedDebtIds = selectedDebtIds,
            consolidationAnnualRatePercent = newLoanRatePct,
            consolidationTermMonths = newLoanTermMonths,
            originationFeePercent = originationFeePct,
            monthlyIncomeToman = monthlyIncomeToman
        )
    }

    val verdictAccentColor = when (report.verdict) {
        ConsolidationVerdict.STRONGLY_ADVANTAGEOUS,
        ConsolidationVerdict.MARGINALLY_ADVANTAGEOUS -> tealAccent
        ConsolidationVerdict.CASHFLOW_RELIEF_TRADEOFF -> goldAccent
        ConsolidationVerdict.NOT_ADVANTAGEOUS -> dangerAccent
        ConsolidationVerdict.INSUFFICIENT_DEBTS -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    TarazinoGlassCard(
        modifier = modifier.testTag("debt_consolidation_simulator_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            TarazinoSectionHeader(
                title = "شبیه‌ساز تجمیع بدهی‌ها (وام یکپارچه)",
                subtitle = "تحلیل ریاضی به‌صرفه بودن دریافت یک وام کم‌بهره برای تسویه بدهی‌های کوچک و پُرسود",
                icon = Icons.Default.CompareArrows,
                accentColor = goldAccent
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Source Mode Switcher (Live Portfolio vs Sample Benchmark)
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
                    ConsolidationSourceMode.entries.forEach { mode ->
                        val isSel = sourceMode == mode
                        val bg by animateColorAsState(
                            targetValue = if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent,
                            label = "consolModeBg"
                        )
                        val fg by animateColorAsState(
                            targetValue = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "consolModeFg"
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = bg,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { sourceMode = mode }
                                .testTag("consolidation_mode_${mode.name.lowercase()}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = mode.labelFa,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = fg,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Mathematical Verdict Hero Banner
            ConsolidationVerdictBanner(
                report = report,
                accentColor = verdictAccentColor,
                privacyMasked = privacyMasked
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. New Single Consolidation Loan Controls
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
                border = BorderStroke(0.9.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = tealAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "شرایط وام یکپارچه جدید",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bank Loan Rate Presets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LOAN_RATE_PRESETS.forEach { (rate, labelFa) ->
                            val isSel = abs(newLoanRatePct - rate) < 0.25
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSel) tealAccent else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSel) tealAccent else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { newLoanRatePct = rate }
                                    .testTag("consolidation_rate_preset_${rate.toInt()}")
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // APR Slider
                    ConsolidationSliderRow(
                        label = "نرخ سود سالانه وام تجمیع",
                        valueText = "${PersianCurrencyEngine.toPersianDigits(newLoanRatePct.toString())}٪",
                        value = newLoanRatePct.toFloat(),
                        valueRange = 0f..36f,
                        accentColor = tealAccent,
                        onValueChange = { raw ->
                            newLoanRatePct = ((raw * 2f).roundToInt() / 2.0).coerceIn(0.0, 36.0)
                        },
                        testTag = "consolidation_apr_slider"
                    )

                    // Term Slider
                    ConsolidationSliderRow(
                        label = "مدت بازپرداخت وام جدید",
                        valueText = "${PersianCurrencyEngine.toPersianDigits(newLoanTermMonths.toString())} ماه",
                        value = newLoanTermMonths.toFloat(),
                        valueRange = 6f..60f,
                        accentColor = goldAccent,
                        onValueChange = { raw ->
                            newLoanTermMonths = raw.roundToInt().coerceIn(6, 60)
                        },
                        testTag = "consolidation_term_slider"
                    )

                    // Origination Fee Slider
                    ConsolidationSliderRow(
                        label = "کارمزد تشکیل پرونده و بیمه",
                        valueText = "${PersianCurrencyEngine.toPersianDigits(originationFeePct.toString())}٪",
                        value = originationFeePct.toFloat(),
                        valueRange = 0f..5f,
                        accentColor = MaterialTheme.colorScheme.primary,
                        onValueChange = { raw ->
                            originationFeePct = ((raw * 2f).roundToInt() / 2.0).coerceIn(0.0, 5.0)
                        },
                        testTag = "consolidation_fee_slider"
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Interactive Debt Selection Checklist
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "بدهی‌های هدف تجمیع (${PersianCurrencyEngine.toPersianDigits(selectedDebtIds.size.toString())} از ${PersianCurrencyEngine.toPersianDigits(activeDebts.size.toString())})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = tealAccent.copy(alpha = 0.16f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedDebtIds = DebtConsolidationEngine.recommendOptimalSubsetIds(
                                    allDebts = activeDebts,
                                    consolidationAnnualRatePercent = newLoanRatePct
                                )
                            }
                            .testTag("consolidation_smart_select_btn")
                    ) {
                        Text(
                            text = "انتخاب هوشمند",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = tealAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedDebtIds = if (selectedDebtIds.size == activeDebts.size) {
                                    emptySet()
                                } else {
                                    activeDebts.map { it.id }.toSet()
                                }
                            }
                            .testTag("consolidation_select_all_btn")
                    ) {
                        Text(
                            text = if (selectedDebtIds.size == activeDebts.size) "لغو همه" else "همه",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (activeDebts.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "بدهی فعالی در پرتفوی ثبت نشده است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = tealAccent.copy(alpha = 0.16f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { sourceMode = ConsolidationSourceMode.SAMPLE_SCENARIO }
                        ) {
                            Text(
                                text = "مشاهده با سناریوی نمونه",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = tealAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activeDebts.forEach { item ->
                        val isChecked = item.id in selectedDebtIds
                        val isHigherThanNewLoan = item.effectiveAnnualRatePercent > newLoanRatePct + 0.5
                        ConsolidatableDebtSelectionRow(
                            item = item,
                            isChecked = isChecked,
                            isHigherThanNewLoan = isHigherThanNewLoan,
                            currencyUnit = currencyUnit,
                            privacyMasked = privacyMasked,
                            tealAccent = tealAccent,
                            goldAccent = goldAccent,
                            dangerAccent = dangerAccent,
                            onToggle = {
                                selectedDebtIds = if (isChecked) {
                                    selectedDebtIds - item.id
                                } else {
                                    selectedDebtIds + item.id
                                }
                            }
                        )
                    }
                }
            }

            // 5. Visual Before-vs-After Stacked Cost Chart + 4 KPI Grid
            if (report.selectedDebts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))

                ConsolidationBeforeAfterChart(
                    report = report,
                    privacyMasked = privacyMasked,
                    tealAccent = tealAccent,
                    goldAccent = goldAccent,
                    dangerAccent = dangerAccent
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2x2 KPI Comparison Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val savingsPositive = report.netTotalSavingsToman >= 0L
                    ConsolidationKpiTile(
                        label = if (savingsPositive) "صرفه‌جویی خالص کل" else "افزایش هزینه کل",
                        valueText = PersianCurrencyEngine.formatCompactAmount(
                            amountToman = report.netTotalSavingsToman,
                            includeUnit = false,
                            privacyMasked = privacyMasked,
                            showPlusSign = true
                        ),
                        subText = "با احتساب کارمزد وام",
                        accentColor = if (savingsPositive) tealAccent else dangerAccent,
                        modifier = Modifier.weight(1f)
                    )

                    val reliefPositive = report.monthlyCashflowReliefToman >= 0L
                    ConsolidationKpiTile(
                        label = if (reliefPositive) "کاهش قسط ماهانه" else "افزایش قسط ماهانه",
                        valueText = PersianCurrencyEngine.formatCompactAmount(
                            amountToman = -report.monthlyCashflowReliefToman,
                            includeUnit = false,
                            privacyMasked = privacyMasked,
                            showPlusSign = true
                        ),
                        subText = "قسط جدید: ${formatCompactPersianToman(report.consolidatedMonthlyPaymentToman, privacyMasked)}",
                        accentColor = if (reliefPositive) tealAccent else goldAccent,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val spreadSign = if (report.rateSpreadPercent >= 0.0) "کاهش " else "افزایش "
                    ConsolidationKpiTile(
                        label = "اختلاف نرخ سود",
                        valueText = "$spreadSign${PersianCurrencyEngine.toPersianDigits(abs(report.rateSpreadPercent).toString())}٪",
                        subText = "${PersianCurrencyEngine.toPersianDigits(report.currentWeightedAnnualRatePercent.toString())}٪ ← ${PersianCurrencyEngine.toPersianDigits(report.consolidationAnnualRatePercent.toString())}٪",
                        accentColor = if (report.rateSpreadPercent > 0.0) tealAccent else dangerAccent,
                        modifier = Modifier.weight(1f)
                    )

                    ConsolidationKpiTile(
                        label = if (report.breakEvenMonths != null) "نقطه سر‌به‌سر کارمزد" else "تاریخ تسویه جدید",
                        valueText = if (report.breakEvenMonths != null && report.originationFeeToman > 0L) {
                            "ماه ${PersianCurrencyEngine.toPersianDigits(report.breakEvenMonths.toString())}ام"
                        } else {
                            report.consolidatedDebtFreeMonthLabelFa
                        },
                        subText = if (report.originationFeeToman > 0L) {
                            "کارمزد: ${formatCompactPersianToman(report.originationFeeToman, privacyMasked)}"
                        } else {
                            "بدون کارمزد اضافی"
                        },
                        accentColor = goldAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 6. Actionable Analytical Notes
            if (report.actionableNotesFa.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = verdictAccentColor.copy(alpha = 0.09f),
                    border = BorderStroke(0.8.dp, verdictAccentColor.copy(alpha = 0.28f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = verdictAccentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "نکات تحلیلی و توصیه مهندسی",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        report.actionableNotesFa.forEach { note ->
                            Text(
                                text = "• $note",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsolidationVerdictBanner(
    report: DebtConsolidationReport,
    accentColor: Color,
    privacyMasked: Boolean
) {
    val icon = when (report.verdict) {
        ConsolidationVerdict.STRONGLY_ADVANTAGEOUS,
        ConsolidationVerdict.MARGINALLY_ADVANTAGEOUS -> Icons.Default.CheckCircle
        ConsolidationVerdict.CASHFLOW_RELIEF_TRADEOFF -> Icons.Default.WarningAmber
        ConsolidationVerdict.NOT_ADVANTAGEOUS -> Icons.Default.ErrorOutline
        ConsolidationVerdict.INSUFFICIENT_DEBTS -> Icons.Default.RadioButtonUnchecked
    }

    val bannerBg = Brush.horizontalGradient(
        colors = listOf(
            accentColor.copy(alpha = 0.18f),
            accentColor.copy(alpha = 0.06f)
        )
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(1.1.dp, accentColor.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(bannerBg)
            .testTag("consolidation_verdict_banner")
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
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = report.verdict.titleFa,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = accentColor
                ) {
                    Text(
                        text = report.verdict.badgeFa,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (privacyMasked) {
                    "نتیجه تحلیل با حفظ محرمانگی مبالغ آماده است."
                } else {
                    report.verdictHeadlineFa
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            if (report.verdictDetailFa.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = report.verdictDetailFa,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConsolidatableDebtSelectionRow(
    item: ConsolidatableDebtItem,
    isChecked: Boolean,
    isHigherThanNewLoan: Boolean,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    tealAccent: Color,
    goldAccent: Color,
    dangerAccent: Color,
    onToggle: () -> Unit
) {
    val aprBadgeColor = when {
        isHigherThanNewLoan -> dangerAccent
        item.effectiveAnnualRatePercent == 0.0 -> tealAccent
        else -> goldAccent
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isChecked) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
        },
        border = BorderStroke(
            width = if (isChecked) 1.1.dp else 0.8.dp,
            color = if (isChecked) tealAccent.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .testTag("consolidation_debt_toggle_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = tealAccent,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.titleFa,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = aprBadgeColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "نرخ ${PersianCurrencyEngine.toPersianDigits(item.effectiveAnnualRatePercent.toString())}٪",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = aprBadgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.counterpartyFa} • ${PersianCurrencyEngine.toPersianDigits(item.remainingMonths.toString())} قسط باقی‌مانده" +
                        if (!isHigherThanNewLoan) " • نرخ پایین‌تر از وام جدید" else " • گزینه عالی تجمیع",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isHigherThanNewLoan) MaterialTheme.colorScheme.onSurfaceVariant else tealAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(horizontalAlignment = Alignment.End) {
                FormattedMoneyText(
                    amountToman = item.remainingScheduledPayableToman,
                    unit = currencyUnit,
                    privacyMasked = privacyMasked,
                    style = MaterialTheme.typography.labelMedium,
                    numberColor = MaterialTheme.colorScheme.onSurface,
                    minFontSize = 9.sp
                )
                Text(
                    text = "${formatCompactPersianToman(item.currentMonthlyPaymentToman, privacyMasked)} در ماه",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConsolidationBeforeAfterChart(
    report: DebtConsolidationReport,
    privacyMasked: Boolean,
    tealAccent: Color,
    goldAccent: Color,
    dangerAccent: Color
) {
    // Capture theme colors outside Canvas DrawScope
    val principalColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    val cardBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    val costExtraColor = if (report.netTotalSavingsToman >= 0L) goldAccent else dangerAccent

    val maxTotalCost = maxOf(
        report.currentTotalRemainingCostToman,
        report.consolidatedTotalCostToman,
        1L
    ).toFloat()

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBgColor,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("consolidation_comparison_chart")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مقایسه کل پرداختی (اصل + سود و کارمزد)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallChartLegend(color = principalColor, label = "اصل بدهی")
                    SmallChartLegend(color = costExtraColor, label = "سود و کارمزد")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 1: Current Multiple Debts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "وضعیت فعلی (${PersianCurrencyEngine.toPersianDigits(report.selectedDebts.size.toString())} بدهی جداگانه)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatCompactPersianToman(report.currentTotalRemainingCostToman, privacyMasked),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            StackedHorizontalBar(
                principalRatio = (report.currentTotalPrincipalToman / maxTotalCost).coerceIn(0f, 1f),
                interestRatio = (report.currentTotalRemainingInterestToman / maxTotalCost).coerceIn(0f, 1f),
                principalColor = principalColor,
                interestColor = goldAccent,
                trackColor = trackColor
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Single Consolidation Loan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "وام تجمیع یکپارچه (${PersianCurrencyEngine.toPersianDigits(report.consolidationAnnualRatePercent.toString())}٪ • ${PersianCurrencyEngine.toPersianDigits(report.consolidationTermMonths.toString())} ماه)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatCompactPersianToman(report.consolidatedTotalCostToman, privacyMasked),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (report.netTotalSavingsToman >= 0L) tealAccent else dangerAccent
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            StackedHorizontalBar(
                principalRatio = (report.consolidationLoanPrincipalToman / maxTotalCost).coerceIn(0f, 1f),
                interestRatio = ((report.consolidatedTotalInterestToman + report.originationFeeToman) / maxTotalCost).coerceIn(0f, 1f),
                principalColor = tealAccent,
                interestColor = costExtraColor,
                trackColor = trackColor
            )
        }
    }
}

@Composable
private fun StackedHorizontalBar(
    principalRatio: Float,
    interestRatio: Float,
    principalColor: Color,
    interestColor: Color,
    trackColor: Color
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(8.dp))
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

        val principalW = (w * principalRatio).coerceIn(0f, w)
        val interestW = (w * interestRatio).coerceIn(0f, (w - principalW).coerceAtLeast(0f))

        if (principalW > 0f) {
            drawRoundRect(
                color = principalColor,
                topLeft = Offset.Zero,
                size = Size(principalW, h),
                cornerRadius = r
            )
        }
        if (interestW > 0f) {
            drawRoundRect(
                color = interestColor,
                topLeft = Offset(principalW, 0f),
                size = Size(interestW, h),
                cornerRadius = r
            )
        }
    }
}

@Composable
private fun SmallChartLegend(color: Color, label: String) {
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
private fun ConsolidationSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    onValueChange: (Float) -> Unit,
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
                fontWeight = FontWeight.Medium,
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

@Composable
private fun ConsolidationKpiTile(
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
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
