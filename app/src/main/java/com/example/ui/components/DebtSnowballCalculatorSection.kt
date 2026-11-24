package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.CurrencyUnit
import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.data.CounterpartyEntity
import com.example.data.InstallmentEntity
import com.example.data.ObligationEntity
import com.example.domain.DebtCategoryTag
import com.example.domain.DebtSnowballCalculatorEngine
import com.example.domain.DebtSnowballPlanReport
import com.example.domain.SnowballDebtAccount
import com.example.domain.SnowballPayoffMilestone
import com.example.ui.theme.LocalTarazinoColors
import kotlin.math.roundToLong

private enum class SnowballSourceMode(val labelFa: String) {
    LIVE_PORTFOLIO("بدهی‌های فعال من"),
    CUSTOM_SCENARIO("شبیه‌سازی دستی")
}

/**
 * Dedicated interactive **Debt Snowball Calculator Module**:
 * Prioritizes debts strictly by smallest remaining balance to build rapid psychological momentum,
 * rolls freed minimum payments into the next target automatically, and projects exact Shamsi payoff dates.
 */
@Composable
fun DebtSnowballCalculatorSection(
    obligations: List<ObligationEntity>,
    installments: List<InstallmentEntity>,
    counterparties: List<CounterpartyEntity>,
    initialExtraBudgetToman: Long,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    modifier: Modifier = Modifier
) {
    val ext = LocalTarazinoColors.current
    val tealAccent = ext.positive // Deep Persian Teal (#35B8AB / #0F6B66)
    val goldAccent = MaterialTheme.colorScheme.secondary // Champagne Gold (#DAB978 / #B58538)

    val portfolioAccounts = remember(obligations, installments, counterparties) {
        DebtSnowballCalculatorEngine.extractAccountsFromPortfolio(
            obligations = obligations,
            installments = installments,
            counterparties = counterparties
        )
    }

    var sourceMode by remember {
        mutableStateOf(
            if (portfolioAccounts.isNotEmpty()) SnowballSourceMode.LIVE_PORTFOLIO
            else SnowballSourceMode.CUSTOM_SCENARIO
        )
    }

    // Editable custom scenario list pre-seeded with realistic Iranian debts for instant exploration
    val customAccounts = remember {
        mutableStateListOf(
            SnowballDebtAccount(
                id = "custom_1",
                titleFa = "قرض دوستانه / کارت اعتباری",
                counterpartyFa = "اسنپ‌پی",
                balanceToman = 8_000_000L,
                minimumMonthlyPaymentToman = 2_000_000L,
                annualInterestRatePercent = 0.0,
                categoryTag = DebtCategoryTag.CREDIT_CARD,
                isFromPortfolio = false
            ),
            SnowballDebtAccount(
                id = "custom_2",
                titleFa = "خرید اقساطی لوازم منزل",
                counterpartyFa = "دیجی‌پی",
                balanceToman = 24_000_000L,
                minimumMonthlyPaymentToman = 3_000_000L,
                annualInterestRatePercent = 23.0,
                categoryTag = DebtCategoryTag.CREDIT_CARD,
                isFromPortfolio = false
            ),
            SnowballDebtAccount(
                id = "custom_3",
                titleFa = "وام بانکی قرض‌الحسنه",
                counterpartyFa = "بانک مهر ایران",
                balanceToman = 65_000_000L,
                minimumMonthlyPaymentToman = 4_500_000L,
                annualInterestRatePercent = 4.0,
                categoryTag = DebtCategoryTag.BANK_LOAN,
                isFromPortfolio = false
            )
        )
    }

    var extraSnowballBudgetToman by remember(initialExtraBudgetToman) {
        mutableLongStateOf(initialExtraBudgetToman.coerceAtLeast(2_000_000L))
    }
    var showAddDebtForm by remember { mutableStateOf(false) }
    var showMonthlyLedger by remember { mutableStateOf(false) }
    var selectedMilestoneRank by remember { mutableStateOf<Int?>(null) }

    // Custom Debt Form Fields
    var newDebtTitle by remember { mutableStateOf("") }
    var newDebtBalanceToman by remember { mutableLongStateOf(12_000_000L) }
    var newDebtMinPaymentToman by remember { mutableLongStateOf(2_000_000L) }
    var newDebtRateText by remember { mutableStateOf("18") }

    val activeAccounts: List<SnowballDebtAccount> = when (sourceMode) {
        SnowballSourceMode.LIVE_PORTFOLIO -> portfolioAccounts
        SnowballSourceMode.CUSTOM_SCENARIO -> customAccounts.toList()
    }

    val planReport: DebtSnowballPlanReport = remember(activeAccounts, extraSnowballBudgetToman) {
        DebtSnowballCalculatorEngine.calculateSnowballPlan(
            accounts = activeAccounts,
            extraMonthlyBudgetToman = extraSnowballBudgetToman
        )
    }

    var curveEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { curveEntered = true }
    val curveProgress by animateFloatAsState(
        targetValue = if (curveEntered) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow),
        label = "SnowballCurveEntrance"
    )

    val extraPresets = remember {
        listOf(
            "۰" to 0L,
            "${PersianCurrencyEngine.isolateLtr("+۲")} میلیون" to 2_000_000L,
            "${PersianCurrencyEngine.isolateLtr("+۵")} میلیون" to 5_000_000L,
            "${PersianCurrencyEngine.isolateLtr("+۱۰")} میلیون" to 10_000_000L,
            "${PersianCurrencyEngine.isolateLtr("+۲۰")} میلیون" to 20_000_000L
        )
    }

    TarazinoGlassCard(
        cornerRadius = 26.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("debt_snowball_calculator_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header with Psychological Momentum Score Pill
            TarazinoSectionHeader(
                title = "ماشین‌حساب استراتژی گلوله برفی",
                subtitle = "اولویت‌بندی از کوچک‌ترین مانده بدهی برای پیروزی سریع و شتاب روانی",
                icon = Icons.Default.AcUnit,
                accentColor = tealAccent,
                trailing = {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = tealAccent.copy(alpha = if (ext.isDark) 0.16f else 0.12f),
                        border = BorderStroke(0.9.dp, tealAccent.copy(alpha = 0.45f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = goldAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "انگیزه ${PersianCurrencyEngine.toPersianDigits(planReport.momentumScorePercent.toString())}٪",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = tealAccent,
                                maxLines = 1
                            )
                        }
                    }
                }
            )

            // 2. Source Mode Switcher (Live Portfolio vs Custom Scenario)
            Surface(
                shape = RoundedCornerShape(15.dp),
                color = ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.55f else 0.78f),
                border = BorderStroke(0.85.dp, ext.cardBorderTop),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SnowballSourceMode.entries.forEach { mode ->
                        val isSelected = sourceMode == mode
                        val bgColor by animateColorAsState(
                            targetValue = if (isSelected) tealAccent else Color.Transparent,
                            label = "SnowballModeBg_${mode.name}"
                        )
                        val countLabel = if (mode == SnowballSourceMode.LIVE_PORTFOLIO) {
                            " (${PersianCurrencyEngine.toPersianDigits(portfolioAccounts.size.toString())})"
                        } else {
                            " (${PersianCurrencyEngine.toPersianDigits(customAccounts.size.toString())})"
                        }
                        Surface(
                            shape = RoundedCornerShape(11.dp),
                            color = bgColor,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(11.dp))
                                .clickable {
                                    sourceMode = mode
                                    selectedMilestoneRank = null
                                }
                                .testTag("snowball_mode_${mode.name}")
                        ) {
                            Text(
                                text = "${mode.labelFa}$countLabel",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                            )
                        }
                    }
                }
            }

            // 3. Extra Monthly Snowball Budget Controller
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.58f else 0.85f),
                border = BorderStroke(0.85.dp, ext.cardBorderTop),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = goldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "بودجه مازاد ماهانه گلوله برفی:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        FormattedMoneyText(
                            amountToman = extraSnowballBudgetToman,
                            unit = currencyUnit,
                            privacyMasked = privacyMasked,
                            style = MaterialTheme.typography.titleSmall,
                            numberColor = tealAccent,
                            unitColor = tealAccent.copy(alpha = 0.85f),
                            minFontSize = 9.5.sp
                        )
                    }

                    Slider(
                        value = extraSnowballBudgetToman.toFloat().coerceIn(0f, 30_000_000f),
                        onValueChange = { raw ->
                            val snapped = ((raw / 500_000f).roundToLong() * 500_000L).coerceIn(0L, 30_000_000L)
                            extraSnowballBudgetToman = snapped
                        },
                        valueRange = 0f..30_000_000f,
                        steps = 59,
                        modifier = Modifier.testTag("snowball_extra_budget_slider")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        extraPresets.forEach { (label, presetVal) ->
                            val isSel = extraSnowballBudgetToman == presetVal
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = if (isSel) {
                                    tealAccent.copy(alpha = if (ext.isDark) 0.22f else 0.14f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                },
                                border = BorderStroke(
                                    width = if (isSel) 1.1.dp else 0.7.dp,
                                    color = if (isSel) tealAccent else ext.cardBorderTop
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable { extraSnowballBudgetToman = presetVal }
                                    .testTag("snowball_preset_$presetVal")
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSel) tealAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Custom Scenario Debt Adder (visible only in CUSTOM_SCENARIO mode)
            AnimatedVisibility(visible = sourceMode == SnowballSourceMode.CUSTOM_SCENARIO) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "بدهی‌های شبیه‌سازی دستی (${PersianCurrencyEngine.toPersianDigits(customAccounts.size.toString())} مورد)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedButton(
                            onClick = { showAddDebtForm = !showAddDebtForm },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, tealAccent.copy(alpha = 0.65f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("snowball_toggle_add_custom_debt_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "افزودن بدهی فرضی",
                                tint = tealAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showAddDebtForm) "بستن فرم" else "افزودن بدهی",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = tealAccent
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showAddDebtForm,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = tealAccent.copy(alpha = if (ext.isDark) 0.10f else 0.06f),
                            border = BorderStroke(0.9.dp, tealAccent.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                OutlinedTextField(
                                    value = newDebtTitle,
                                    onValueChange = { newDebtTitle = it },
                                    label = { Text("عنوان بدهی (مثلاً کارت اعتباری یا قرض)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("snowball_custom_title_input")
                                )

                                MoneyInputField(
                                    labelFa = "مانده فعلی بدهی",
                                    amountToman = newDebtBalanceToman,
                                    currencyUnit = currencyUnit,
                                    onAmountChange = { newDebtBalanceToman = it }
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1.4f)) {
                                        MoneyInputField(
                                            labelFa = "حداقل قسط ماهانه",
                                            amountToman = newDebtMinPaymentToman,
                                            currencyUnit = currencyUnit,
                                            onAmountChange = { newDebtMinPaymentToman = it }
                                        )
                                    }
                                    OutlinedTextField(
                                        value = newDebtRateText,
                                        onValueChange = { input ->
                                            if (input.length <= 5) newDebtRateText = input
                                        },
                                        label = { Text("سود سالانه ٪") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(0.8f)
                                    )
                                }

                                Button(
                                    onClick = {
                                        val cleanTitle = newDebtTitle.trim().ifBlank {
                                            "بدهی شبیه‌سازی ${PersianCurrencyEngine.toPersianDigits((customAccounts.size + 1).toString())}"
                                        }
                                        val parsedApr = PersianCurrencyEngine.toEnglishDigits(newDebtRateText)
                                            .toDoubleOrNull()
                                            ?.coerceIn(0.0, 60.0) ?: 0.0
                                        if (newDebtBalanceToman > 0L) {
                                            customAccounts.add(
                                                SnowballDebtAccount(
                                                    id = "custom_${System.currentTimeMillis()}",
                                                    titleFa = cleanTitle,
                                                    counterpartyFa = "سناریوی دستی",
                                                    balanceToman = newDebtBalanceToman,
                                                    minimumMonthlyPaymentToman = newDebtMinPaymentToman
                                                        .coerceAtLeast(200_000L)
                                                        .coerceAtMost(newDebtBalanceToman),
                                                    annualInterestRatePercent = parsedApr,
                                                    categoryTag = DebtCategoryTag.PERSONAL,
                                                    isFromPortfolio = false
                                                )
                                            )
                                            newDebtTitle = ""
                                            showAddDebtForm = false
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = tealAccent),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("snowball_confirm_add_debt_btn")
                                ) {
                                    Text(
                                        text = "افزودن به صف گلوله برفی",
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (planReport.prioritizedMilestones.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ext.elevatedSurface.copy(alpha = 0.7f),
                    border = BorderStroke(0.8.dp, ext.cardBorderTop),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = planReport.actionableStrategySummaryFa,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                // 5. Hero Psychological Momentum & First Quick Win Spotlight Card
                planReport.firstQuickWinMilestone?.let { firstWin ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.1.dp, tealAccent.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        tealAccent.copy(alpha = if (ext.isDark) 0.22f else 0.14f),
                                        goldAccent.copy(alpha = if (ext.isDark) 0.14f else 0.10f)
                                    )
                                )
                            )
                            .testTag("snowball_first_quick_win_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(13.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = goldAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "اولین پیروزی سریع (کوچک‌ترین بدهی)",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = goldAccent.copy(alpha = 0.22f)
                                ) {
                                    Text(
                                        text = "ماه ${PersianCurrencyEngine.toPersianDigits(firstWin.payoffMonthIndex.toString())} (${firstWin.payoffShamsiMonthLabelFa})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = goldAccent,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = firstWin.account.titleFa,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = tealAccent,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "حمله ماهانه پیشنهادی: ${
                                            PersianCurrencyEngine.formatAmount(
                                                firstWin.initialAttackPaymentToman,
                                                currencyUnit,
                                                privacyMasked = privacyMasked
                                            )
                                        }",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "آزادسازی برای مرحله بعد:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 9.5.sp
                                    )
                                    Text(
                                        text = "+${
                                            PersianCurrencyEngine.formatAmount(
                                                firstWin.rolloverFreedMonthlyToman,
                                                currencyUnit,
                                                privacyMasked = privacyMasked
                                            )
                                        }",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = tealAccent
                                    )
                                }
                            }

                            Text(
                                text = planReport.actionableStrategySummaryFa,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // 6. 3-Column Executive Snowball KPI Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SnowballMiniKpiCard(
                        labelFa = "آزادی کامل از بدهی",
                        valueFa = "${PersianCurrencyEngine.toPersianDigits(planReport.snowballMonthsToDebtFree.toString())} ماه",
                        subLabelFa = planReport.debtFreeShamsiMonthLabelFa,
                        accentColor = tealAccent,
                        modifier = Modifier.weight(1f)
                    )
                    SnowballMiniKpiCard(
                        labelFa = "شتاب نسبت به عادی",
                        valueFa = "${PersianCurrencyEngine.toPersianDigits(planReport.monthsSavedWithSnowball.toString())} ماه زودتر",
                        subLabelFa = "از ${PersianCurrencyEngine.toPersianDigits(planReport.minimumOnlyMonthsToDebtFree.toString())} ماه",
                        accentColor = goldAccent,
                        modifier = Modifier.weight(1f)
                    )
                    SnowballMiniKpiCard(
                        labelFa = "قدرت نهایی گلوله",
                        valueFa = formatCompactPersianToman(
                            planReport.peakSnowballPowerToman * currencyUnit.multiplierFromToman,
                            privacyMasked
                        ),
                        subLabelFa = if (privacyMasked) "ماهانه" else "${currencyUnit.labelFa}/ماه",
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 7. Visual Snowball Debt Decay & Knockout Milestones Area Chart
                if (planReport.monthlyLedger.isNotEmpty()) {
                    SnowballDecayCanvasCard(
                        report = planReport,
                        animProgress = curveProgress,
                        tealAccent = tealAccent,
                        goldAccent = goldAccent
                    )
                }

                // 8. Step-by-Step Prioritized Debt Payoff Order (Smallest Balance First)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ترتیب پیشنهادی تسویه (کوچک‌ترین مانده به بزرگ‌ترین):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = planReport.momentumTierLabelFa,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = tealAccent
                        )
                    }

                    planReport.prioritizedMilestones.forEach { milestone ->
                        SnowballMilestoneOrderCard(
                            milestone = milestone,
                            isSelected = selectedMilestoneRank == milestone.priorityRank,
                            canDelete = sourceMode == SnowballSourceMode.CUSTOM_SCENARIO && customAccounts.size > 1,
                            tealAccent = tealAccent,
                            goldAccent = goldAccent,
                            currencyUnit = currencyUnit,
                            privacyMasked = privacyMasked,
                            onClick = {
                                selectedMilestoneRank = if (selectedMilestoneRank == milestone.priorityRank) {
                                    null
                                } else {
                                    milestone.priorityRank
                                }
                            },
                            onDelete = {
                                customAccounts.removeAll { it.id == milestone.account.id }
                            }
                        )
                    }
                }

                // 9. Expandable Month-by-Month Shamsi Execution Ledger
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.55f else 0.78f),
                    border = BorderStroke(0.8.dp, ext.cardBorderTop),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showMonthlyLedger = !showMonthlyLedger }
                        .testTag("snowball_toggle_ledger_btn")
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
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAddCheck,
                                contentDescription = null,
                                tint = tealAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "جدول ماه‌به‌ماه اجرای گلوله برفی (${PersianCurrencyEngine.toPersianDigits(planReport.monthlyLedger.size.toString())} ماه شمسی)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            imageVector = if (showMonthlyLedger) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showMonthlyLedger,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.testTag("snowball_monthly_ledger_list")
                    ) {
                        planReport.monthlyLedger.take(24).forEach { step ->
                            val hasKnockout = step.clearedDebtTitlesThisMonth.isNotEmpty()
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (hasKnockout) {
                                    tealAccent.copy(alpha = if (ext.isDark) 0.14f else 0.09f)
                                } else {
                                    ext.elevatedSurface.copy(alpha = 0.55f)
                                },
                                border = BorderStroke(
                                    0.75.dp,
                                    if (hasKnockout) tealAccent.copy(alpha = 0.45f) else ext.cardBorderTop
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ماه ${PersianCurrencyEngine.toPersianDigits(step.monthIndex.toString())} • ${step.shamsiMonthLabelFa}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (hasKnockout) tealAccent else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "مانده کل: ${
                                                PersianCurrencyEngine.formatAmount(
                                                    step.remainingTotalDebtToman,
                                                    currencyUnit,
                                                    privacyMasked = privacyMasked
                                                )
                                            }",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "هدف تمرکز: ${step.focusDebtTitleFa}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "گلوله: ${
                                                PersianCurrencyEngine.formatAmount(
                                                    step.snowballRolloverAppliedToman,
                                                    currencyUnit,
                                                    privacyMasked = privacyMasked
                                                )
                                            }",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = goldAccent
                                        )
                                    }
                                    if (hasKnockout) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = tealAccent,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "حذف کامل: ${step.clearedDebtTitlesThisMonth.joinToString("، ")}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = tealAccent
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (planReport.monthlyLedger.size > 24) {
                            Text(
                                text = "نمایش ۲۴ ماه اول از مجموع ${PersianCurrencyEngine.toPersianDigits(planReport.monthlyLedger.size.toString())} ماه تا تسویه نهایی (${planReport.debtFreeShamsiMonthLabelFa}).",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SnowballMiniKpiCard(
    labelFa: String,
    valueFa: String,
    subLabelFa: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val ext = LocalTarazinoColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.62f else 0.88f),
        border = BorderStroke(0.85.dp, accentColor.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            TarazinoAutoSizeText(
                text = labelFa,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                minFontSize = 8.sp
            )
            TarazinoAutoSizeText(
                text = valueFa,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                maxLines = 1,
                minFontSize = 9.5.sp
            )
            TarazinoAutoSizeText(
                text = subLabelFa,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                minFontSize = 7.5.sp
            )
        }
    }
}

@Composable
private fun SnowballDecayCanvasCard(
    report: DebtSnowballPlanReport,
    animProgress: Float,
    tealAccent: Color,
    goldAccent: Color
) {
    val ext = LocalTarazinoColors.current
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.56f else 0.80f),
        border = BorderStroke(0.85.dp, ext.cardBorderTop),
        modifier = Modifier.fillMaxWidth()
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
                Text(
                    text = "منحنی کاهش مانده بدهی و نقاط حذف سریع",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(goldAccent)
                    )
                    Text(
                        text = "نقطه حذف بدهی",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.5.sp,
                        color = goldAccent
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .testTag("snowball_decay_curve_canvas")
            ) {
                val pointsCount = report.monthlyLedger.size + 1
                val startDebt = report.totalStartingDebtToman.coerceAtLeast(1L).toFloat()
                val padH = 10.dp.toPx()
                val padV = 12.dp.toPx()
                val plotW = (size.width - padH * 2f).coerceAtLeast(10f)
                val plotH = (size.height - padV * 2f).coerceAtLeast(10f)

                // Horizontal reference grid lines
                val dash = PathEffect.dashPathEffect(floatArrayOf(7f, 6f), 0f)
                for (i in 0..2) {
                    val y = padV + plotH * (i / 2f)
                    drawLine(
                        color = gridColor,
                        start = Offset(padH, y),
                        end = Offset(size.width - padH, y),
                        strokeWidth = 0.9.dp.toPx(),
                        pathEffect = dash
                    )
                }

                // Build points: index 0 = starting debt, index 1..N = remaining after each month
                val balances = buildList {
                    add(startDebt)
                    report.monthlyLedger.forEach { add(it.remainingTotalDebtToman.toFloat()) }
                }

                val coords = balances.mapIndexed { idx, bal ->
                    val xFrac = if (pointsCount > 1) idx.toFloat() / (pointsCount - 1).toFloat() else 0f
                    val yFrac = (bal / startDebt).coerceIn(0f, 1f)
                    val x = padH + (xFrac * plotW * animProgress)
                    val y = padV + plotH * (1f - yFrac)
                    Offset(x, y)
                }

                if (coords.size >= 2) {
                    val linePath = Path().apply {
                        moveTo(coords.first().x, coords.first().y)
                        coords.drop(1).forEach { pt -> lineTo(pt.x, pt.y) }
                    }
                    val fillPath = Path().apply {
                        moveTo(coords.first().x, padV + plotH)
                        coords.forEach { pt -> lineTo(pt.x, pt.y) }
                        lineTo(coords.last().x, padV + plotH)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                tealAccent.copy(alpha = 0.34f),
                                tealAccent.copy(alpha = 0.03f)
                            ),
                            startY = padV,
                            endY = padV + plotH
                        ),
                        style = Fill
                    )
                    drawPath(
                        path = linePath,
                        color = tealAccent,
                        style = Stroke(
                            width = 2.4.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Highlight milestone dots where a debt is completely knocked out
                    report.monthlyLedger.forEachIndexed { idx, step ->
                        if (step.clearedDebtTitlesThisMonth.isNotEmpty()) {
                            val pt = coords.getOrNull(idx + 1) ?: return@forEachIndexed
                            drawCircle(
                                color = goldAccent.copy(alpha = 0.28f),
                                radius = 7.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = goldAccent,
                                radius = 4.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 1.6.dp.toPx(),
                                center = pt
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "شروع (${PersianCurrencyEngine.toPersianDigits(report.startDateJalali)})",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "صفر شدن کامل (${report.debtFreeShamsiMonthLabelFa})",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = tealAccent
                )
            }
        }
    }
}

@Composable
private fun SnowballMilestoneOrderCard(
    milestone: SnowballPayoffMilestone,
    isSelected: Boolean,
    canDelete: Boolean,
    tealAccent: Color,
    goldAccent: Color,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val ext = LocalTarazinoColors.current
    val isFirstTarget = milestone.priorityRank == 1
    val badgeColor = if (isFirstTarget) goldAccent else tealAccent

    val cardBg by animateColorAsState(
        targetValue = if (isSelected || isFirstTarget) {
            badgeColor.copy(alpha = if (ext.isDark) 0.14f else 0.09f)
        } else {
            ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.65f else 0.86f)
        },
        label = "SnowballMilestoneBg_${milestone.priorityRank}"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(
            width = if (isSelected || isFirstTarget) 1.25.dp else 0.8.dp,
            color = if (isSelected || isFirstTarget) badgeColor.copy(alpha = 0.65f) else ext.cardBorderTop
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("snowball_milestone_card_${milestone.priorityRank}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(badgeColor)
                    ) {
                        Text(
                            text = PersianCurrencyEngine.toPersianDigits(milestone.priorityRank.toString()),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = milestone.account.titleFa,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${milestone.account.counterpartyFa} • ${milestone.momentumBadgeFa}",
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف بدهی شبیه‌سازی",
                            tint = ext.danger,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "مانده بدهی",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.5.sp
                    )
                    FormattedMoneyText(
                        amountToman = milestone.account.balanceToman,
                        unit = currencyUnit,
                        privacyMasked = privacyMasked,
                        style = MaterialTheme.typography.labelLarge,
                        numberColor = MaterialTheme.colorScheme.onSurface,
                        unitColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        minFontSize = 9.sp
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "پرداخت ماهانه تمرکزی",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.5.sp
                    )
                    Text(
                        text = PersianCurrencyEngine.formatAmount(
                            milestone.initialAttackPaymentToman,
                            currencyUnit,
                            privacyMasked = privacyMasked
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = tealAccent
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "تاریخ صفر شدن",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.5.sp
                    )
                    Text(
                        text = "ماه ${PersianCurrencyEngine.toPersianDigits(milestone.payoffMonthIndex.toString())} (${milestone.payoffShamsiMonthLabelFa})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = goldAccent
                    )
                }
            }

            AnimatedVisibility(visible = isSelected || isFirstTarget) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    Text(
                        text = milestone.psychologicalBoostNoteFa,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "اثر گلوله برفی پس از حذف: +${
                            PersianCurrencyEngine.formatAmount(
                                milestone.rolloverFreedMonthlyToman,
                                currencyUnit,
                                privacyMasked = privacyMasked
                            )
                        } آزاد می‌شود (قدرت تجمعی گلوله: ${
                            PersianCurrencyEngine.formatAmount(
                                milestone.cumulativeSnowballPowerToman,
                                currencyUnit,
                                privacyMasked = privacyMasked
                            )
                        })",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = tealAccent,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
