package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.CurrencyUnit
import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.domain.AmortizationFormulaMode
import com.example.domain.AmortizationInstallmentRow
import com.example.domain.DebtAmortizationCalculatorEngine
import com.example.ui.theme.LocalTarazinoColors
import kotlin.math.roundToInt

@Composable
fun DebtAmortizationCalculatorSection(
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    modifier: Modifier = Modifier
) {
    val ext = LocalTarazinoColors.current
    val principalColor = MaterialTheme.colorScheme.primary // Soothing Deep Persian Teal
    val interestColor = MaterialTheme.colorScheme.secondary // Soft Champagne Gold

    var principalToman by remember { mutableLongStateOf(100_000_000L) }
    var annualRateText by remember { mutableStateOf("23") }
    var durationMonthsText by remember { mutableStateOf("24") }
    var startDateJalali by remember {
        mutableStateOf(JalaliCalendarEngine.firstOfNextJalaliMonth(JalaliCalendarEngine.todayJalali()))
    }
    var formulaMode by remember { mutableStateOf(AmortizationFormulaMode.COMPOUND_ANNUITY) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showFullSchedule by remember { mutableStateOf(false) }

    val parsedRate = remember(annualRateText) {
        PersianCurrencyEngine.toEnglishDigits(annualRateText).toDoubleOrNull()?.coerceIn(0.0, 120.0) ?: 23.0
    }
    val parsedMonths = remember(durationMonthsText) {
        PersianCurrencyEngine.toEnglishDigits(durationMonthsText).toIntOrNull()?.coerceIn(1, 240) ?: 12
    }

    val result = remember(principalToman, parsedRate, parsedMonths, startDateJalali, formulaMode) {
        DebtAmortizationCalculatorEngine.calculateAmortization(
            principalToman = principalToman,
            annualInterestRatePercent = parsedRate,
            durationMonths = parsedMonths,
            startDateJalali = startDateJalali,
            formulaMode = formulaMode
        )
    }

    val ratePresets = remember {
        listOf(
            "۴٪ قرض‌الحسنه" to 4,
            "۱۸٪ تسهیلات" to 18,
            "۲۳٪ بانکی" to 23,
            "۳۰٪ مرابحه" to 30
        )
    }
    val durationPresets = remember {
        listOf(6, 12, 18, 24, 36, 48, 60)
    }

    TarazinoGlassCard(
        cornerRadius = 26.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("debt_amortization_calculator_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TarazinoSectionHeader(
                title = "ماشین‌حساب استهلاک وام و اقساط شمسی",
                subtitle = "محاسبه دقیق قسط ماهانه، کل سود و جدول سررسیدهای تقویم جلالی",
                icon = Icons.Default.Calculate,
                accentColor = principalColor
            )

            // 1. Formula Mode Selector (Compound Banking vs. Flat Qarz-al-Hasaneh)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AmortizationFormulaMode.entries.forEach { mode ->
                    val selected = formulaMode == mode
                    val bg = rememberSpringColorAnimatable(
                        targetColor = if (selected) {
                            principalColor.copy(alpha = if (ext.isDark) 0.20f else 0.12f)
                        } else {
                            ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.55f else 0.78f)
                        }
                    ).value
                    val border = rememberSpringColorAnimatable(
                        targetColor = if (selected) principalColor else ext.cardBorderTop
                    ).value

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = bg,
                        border = BorderStroke(if (selected) 1.4.dp else 0.8.dp, border),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { formulaMode = mode }
                            .testTag("amortization_mode_${mode.name}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            TarazinoAutoSizeText(
                                text = mode.labelFa,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (selected) principalColor else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                minFontSize = 8.5.sp
                            )
                            TarazinoAutoSizeText(
                                text = mode.subtitleFa,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                minFontSize = 7.5.sp
                            )
                        }
                    }
                }
            }

            // 2. Principal Amount Input
            MoneyInputField(
                labelFa = "مبلغ اصل وام یا تسهیلات",
                amountToman = principalToman,
                currencyUnit = currencyUnit,
                onAmountChange = { principalToman = it },
                modifier = Modifier.testTag("amortization_principal_input")
            )

            // 3. Annual Interest Rate Input + Presets + Slider
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = PersianCurrencyEngine.toPersianDigits(annualRateText),
                        onValueChange = { raw ->
                            val clean = PersianCurrencyEngine.toEnglishDigits(raw)
                                .filter { it.isDigit() || it == '.' }
                                .take(5)
                            annualRateText = clean
                        },
                        label = { Text("نرخ سود سالانه (٪)", maxLines = 1) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("amortization_interest_rate_input")
                    )

                    OutlinedTextField(
                        value = PersianCurrencyEngine.toPersianDigits(durationMonthsText),
                        onValueChange = { raw ->
                            val clean = PersianCurrencyEngine.toEnglishDigits(raw)
                                .filter { it.isDigit() }
                                .take(3)
                            durationMonthsText = clean
                        },
                        label = { Text("مدت بازپرداخت (ماه)", maxLines = 1) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("amortization_duration_months_input")
                    )
                }

                // Quick Rate & Duration Preset Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    ratePresets.forEach { (labelFa, rateVal) ->
                        val isSel = parsedRate.roundToInt() == rateVal
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) interestColor.copy(alpha = 0.22f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(
                                0.8.dp,
                                if (isSel) interestColor else ext.cardBorderTop
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { annualRateText = rateVal.toString() }
                        ) {
                            Text(
                                text = labelFa,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) interestColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }

                    durationPresets.forEach { monthsVal ->
                        val isSel = parsedMonths == monthsVal
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) principalColor.copy(alpha = 0.20f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(
                                0.8.dp,
                                if (isSel) principalColor else ext.cardBorderTop
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { durationMonthsText = monthsVal.toString() }
                        ) {
                            Text(
                                text = "${PersianCurrencyEngine.toPersianDigits(monthsVal.toString())} ماهه",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) principalColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // 4. First Installment Shamsi Date Selector (Powered by JalaliCalendarEngine)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.68f else 0.86f),
                border = BorderStroke(0.85.dp, ext.cardBorderTop),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showDatePicker = true }
                    .testTag("amortization_start_date_picker_btn")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 13.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = principalColor,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تاریخ اولین قسط (تقویم شمسی)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TarazinoAutoSizeText(
                                text = "${JalaliCalendarEngine.formatJalaliVerbose(result.startDateJalali)} تا ${
                                    JalaliCalendarEngine.formatJalaliShortText(result.endDateJalali)
                                }",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                minFontSize = 8.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = principalColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "تغییر تاریخ",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = principalColor,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 5. Primary Calculation Results Hero Box (Monthly Payment, Total Interest, Total Payable)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = principalColor.copy(alpha = if (ext.isDark) 0.13f else 0.08f),
                border = BorderStroke(1.1.dp, principalColor.copy(alpha = 0.42f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("amortization_results_summary_box")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "مبلغ هر قسط ماهانه",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FormattedMoneyText(
                                amountToman = result.monthlyPaymentToman,
                                unit = currencyUnit,
                                privacyMasked = privacyMasked,
                                style = MaterialTheme.typography.titleLarge,
                                numberColor = MaterialTheme.colorScheme.onSurface,
                                unitColor = principalColor,
                                minFontSize = 12.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("amortization_monthly_payment_value")
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(11.dp),
                            color = interestColor.copy(alpha = if (ext.isDark) 0.18f else 0.14f),
                            border = BorderStroke(0.8.dp, interestColor.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = "${PersianCurrencyEngine.toPersianDigits(result.durationMonths.toString())} قسط شمسی",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = interestColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        thickness = 0.75.dp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "کل سود و کارمزد دوره",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FormattedMoneyText(
                                amountToman = result.totalInterestToman,
                                unit = currencyUnit,
                                privacyMasked = privacyMasked,
                                style = MaterialTheme.typography.labelLarge,
                                numberColor = interestColor,
                                unitColor = interestColor.copy(alpha = 0.85f),
                                minFontSize = 9.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "کل بازپرداخت نهایی",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FormattedMoneyText(
                                amountToman = result.totalPayableToman,
                                unit = currencyUnit,
                                privacyMasked = privacyMasked,
                                style = MaterialTheme.typography.labelLarge,
                                numberColor = MaterialTheme.colorScheme.onSurface,
                                unitColor = principalColor,
                                minFontSize = 9.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Proportional Principal vs. Interest Split Bar
                    val pPct = (result.principalToTotalRatio * 100f).roundToInt().coerceIn(0, 100)
                    val iPct = (100 - pPct).coerceIn(0, 100)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(result.principalToTotalRatio.coerceAtLeast(0.05f))
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(principalColor)
                            )
                            if (result.totalInterestToman > 0L) {
                                Box(
                                    modifier = Modifier
                                        .weight(result.interestToTotalRatio.coerceAtLeast(0.05f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(interestColor)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "سهم اصل وام: ${PersianCurrencyEngine.toPersianDigits(pPct.toString())}٪",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = principalColor
                            )
                            Text(
                                text = "سهم سود: ${PersianCurrencyEngine.toPersianDigits(iPct.toString())}٪",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = interestColor
                            )
                        }
                    }
                }
            }

            // 6. Shamsi Amortization Schedule Table Preview + Expand Toggle
            val displayedRows = remember(result.schedule, showFullSchedule) {
                if (showFullSchedule || result.schedule.size <= 4) {
                    result.schedule
                } else {
                    result.schedule.take(3) + listOf(result.schedule.last())
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "جدول استهلاک اقساط به تفکیک ماه شمسی:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (result.schedule.size > 4) {
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = principalColor.copy(alpha = 0.12f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .clickable { showFullSchedule = !showFullSchedule }
                                .testTag("amortization_toggle_full_schedule_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (showFullSchedule) "نمایش خلاصه"
                                    else "همه ${PersianCurrencyEngine.toPersianDigits(result.schedule.size.toString())} قسط",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = principalColor
                                )
                                Icon(
                                    imageVector = if (showFullSchedule) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = principalColor,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                displayedRows.forEachIndexed { idx, row ->
                    val isLastSummaryJump = !showFullSchedule &&
                        result.schedule.size > 4 &&
                        idx == displayedRows.lastIndex &&
                        row.installmentNumber > 4

                    if (isLastSummaryJump) {
                        Text(
                            text = "••• اقساط میانی (${
                                PersianCurrencyEngine.toPersianDigits("4")
                            } تا ${
                                PersianCurrencyEngine.toPersianDigits((row.installmentNumber - 1).toString())
                            }) •••",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    AmortizationScheduleRowCard(
                        row = row,
                        currencyUnit = currencyUnit,
                        privacyMasked = privacyMasked,
                        principalColor = principalColor,
                        interestColor = interestColor
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        JalaliDatePickerModal(
            initialDateJalali = startDateJalali,
            onDismiss = { showDatePicker = false },
            onConfirmDate = { picked: String ->
                startDateJalali = picked
                showDatePicker = false
            }
        )
    }
}

@Composable
private fun AmortizationScheduleRowCard(
    row: AmortizationInstallmentRow,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    principalColor: androidx.compose.ui.graphics.Color,
    interestColor: androidx.compose.ui.graphics.Color
) {
    val ext = LocalTarazinoColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.58f else 0.80f),
        border = BorderStroke(0.75.dp, ext.cardBorderTop),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("amortization_row_${row.installmentNumber}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = principalColor.copy(alpha = 0.16f),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = PersianCurrencyEngine.toPersianDigits(row.installmentNumber.toString()),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = principalColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        TarazinoAutoSizeText(
                            text = "قسط ${PersianCurrencyEngine.toPersianDigits(row.installmentNumber.toString())} • ${row.dueDateShortFa}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            minFontSize = 9.sp
                        )
                        TarazinoAutoSizeText(
                            text = "مانده اصل: ${
                                PersianCurrencyEngine.formatAmount(
                                    row.remainingPrincipalAfterToman,
                                    currencyUnit,
                                    privacyMasked = privacyMasked
                                )
                            }",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            minFontSize = 8.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                FormattedMoneyText(
                    amountToman = row.paymentAmountToman,
                    unit = currencyUnit,
                    privacyMasked = privacyMasked,
                    style = MaterialTheme.typography.labelLarge,
                    numberColor = MaterialTheme.colorScheme.onSurface,
                    unitColor = principalColor,
                    minFontSize = 9.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TarazinoAutoSizeText(
                    text = "سهم اصل: ${
                        PersianCurrencyEngine.formatAmount(
                            row.principalPortionToman,
                            currencyUnit,
                            privacyMasked = privacyMasked
                        )
                    }",
                    style = MaterialTheme.typography.labelSmall,
                    color = principalColor,
                    maxLines = 1,
                    minFontSize = 8.sp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                TarazinoAutoSizeText(
                    text = "سهم سود: ${
                        PersianCurrencyEngine.formatAmount(
                            row.interestPortionToman,
                            currencyUnit,
                            privacyMasked = privacyMasked
                        )
                    }",
                    style = MaterialTheme.typography.labelSmall,
                    color = interestColor,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    minFontSize = 8.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
