package com.example.ui.components

import android.content.Context
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.CurrencyUnit
import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.data.InstallmentEntity
import com.example.data.ObligationEntity
import com.example.domain.AutomatedPaymentScheduleReport
import com.example.domain.DayScheduleEvaluation
import com.example.domain.HistoricalIncomeDeposit
import com.example.domain.IncomeCyclePaymentSchedulerEngine
import com.example.domain.PaymentScheduleStrategyMode
import com.example.domain.RecurringCashflowHealthEngine
import com.example.ui.theme.LocalTarazinoColors

private const val SCHEDULER_PREFS_NAME = "tarazino_prefs_v1"
private const val KEY_RECURRING_ITEMS = "recurring_non_loan_cashflow_items_v1"
private const val KEY_HISTORICAL_INCOME_LOGS = "historical_income_cycles_v1"

/**
 * Automated Optimal Payment-Day Scheduler card rendered inside the Dashboard **Strategy** tab.
 *
 * Analyzes historical income cycles, recurring living expenses, and upcoming installment deadlines
 * to recommend the mathematically best day(s) of the Shamsi month for installment payments.
 */
@Composable
fun StrategyAutomatedPaymentSchedulerCard(
    obligations: List<ObligationEntity>,
    installments: List<InstallmentEntity>,
    fallbackMonthlyIncomeToman: Long,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    autoScheduleEnabled: Boolean,
    savedAutoPayPrimaryDay: Int,
    savedAutoPaySecondaryDay: Int,
    onSaveAutomatedSchedule: (enabled: Boolean, primaryDay: Int, secondaryDay: Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val ext = LocalTarazinoColors.current
    val tealAccent = ext.positive
    val goldAccent = MaterialTheme.colorScheme.secondary
    val dangerAccent = ext.danger

    val prefs = remember(context) {
        context.getSharedPreferences(SCHEDULER_PREFS_NAME, Context.MODE_PRIVATE)
    }

    val recurringItems = remember(prefs) {
        RecurringCashflowHealthEngine.deserializeItems(
            prefs.getString(KEY_RECURRING_ITEMS, null)
        )
    }

    var historicalLogs by remember(prefs) {
        mutableStateOf(
            IncomeCyclePaymentSchedulerEngine.deserializeHistoricalDeposits(
                prefs.getString(KEY_HISTORICAL_INCOME_LOGS, null)
            )
        )
    }

    fun persistHistoricalLogs(updated: List<HistoricalIncomeDeposit>) {
        historicalLogs = updated
        prefs.edit()
            .putString(
                KEY_HISTORICAL_INCOME_LOGS,
                IncomeCyclePaymentSchedulerEngine.serializeHistoricalDeposits(updated)
            )
            .apply()
    }

    var preferredMode by remember(savedAutoPaySecondaryDay) {
        mutableStateOf(
            if (savedAutoPaySecondaryDay > 0) PaymentScheduleStrategyMode.SPLIT_BI_CYCLE
            else PaymentScheduleStrategyMode.SINGLE_BEST_DAY
        )
    }

    val report: AutomatedPaymentScheduleReport = remember(
        recurringItems,
        historicalLogs,
        obligations,
        installments,
        fallbackMonthlyIncomeToman,
        preferredMode
    ) {
        IncomeCyclePaymentSchedulerEngine.buildScheduleRecommendation(
            recurringItems = recurringItems,
            historicalIncomeDeposits = historicalLogs,
            obligations = obligations,
            installments = installments,
            fallbackMonthlyIncomeToman = fallbackMonthlyIncomeToman,
            preferredMode = preferredMode
        )
    }

    var inspectedDay by remember(report.primaryRecommendedDay) {
        mutableIntStateOf(report.primaryRecommendedDay)
    }
    var showAddIncomeLogForm by remember { mutableStateOf(false) }
    var showDatePickerForLog by remember { mutableStateOf(false) }
    var newLogTitle by remember { mutableStateOf("واریز حقوق / درآمد") }
    var newLogAmountToman by remember { mutableLongStateOf(fallbackMonthlyIncomeToman.coerceAtLeast(10_000_000L)) }
    var newLogDateJalali by remember { mutableStateOf(JalaliCalendarEngine.todayJalali()) }

    val inspectedEval = remember(report.dailyEvaluations, inspectedDay) {
        report.dailyEvaluations.firstOrNull { it.dayOfMonth == inspectedDay }
            ?: report.dailyEvaluations.first()
    }

    val activeSecondaryTarget = if (report.effectiveMode == PaymentScheduleStrategyMode.SPLIT_BI_CYCLE) {
        report.secondaryRecommendedDay
    } else null

    val isCurrentRecommendationActive = autoScheduleEnabled &&
        savedAutoPayPrimaryDay == report.primaryRecommendedDay &&
        (savedAutoPaySecondaryDay == (activeSecondaryTarget ?: 0))

    TarazinoGlassCard(
        accentBorderColor = tealAccent.copy(alpha = 0.55f),
        modifier = modifier
            .fillMaxWidth()
            .testTag("strategy_automated_payment_scheduler_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section Header
            TarazinoSectionHeader(
                title = "زمان‌بند خودکار بهترین روز پرداخت اقساط",
                subtitle = report.incomeCyclePatternFa,
                icon = Icons.Default.EventAvailable,
                accentColor = tealAccent
            )

            // 1. Optimal Payment Day Hero Recommendation Banner
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.Transparent,
                border = BorderStroke(
                    1.dp,
                    Brush.linearGradient(listOf(tealAccent.copy(alpha = 0.65f), goldAccent.copy(alpha = 0.45f)))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(ext.heroStart, ext.heroMid, ext.heroEnd)
                            )
                        )
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "بهترین روز پیشنهادی پرداخت در ماه شمسی",
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.heroOnSurfaceMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = buildString {
                                        append("روز ${PersianCurrencyEngine.toPersianDigits(report.primaryRecommendedDay.toString())} ام")
                                        if (activeSecondaryTarget != null) {
                                            append(" و ${PersianCurrencyEngine.toPersianDigits(activeSecondaryTarget.toString())} ام")
                                        }
                                        append(" هر ماه")
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = ext.heroOnSurface,
                                    modifier = Modifier.testTag("scheduler_recommended_day_label")
                                )
                            }
                        }

                        // Confidence Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ext.heroAccent.copy(alpha = 0.22f),
                            border = BorderStroke(0.8.dp, ext.heroAccent.copy(alpha = 0.55f))
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${PersianCurrencyEngine.toPersianDigits(report.confidencePercent.toString())}٪",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = ext.heroAccent
                                )
                                Text(
                                    text = "اطمینان تحلیل",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = ext.heroOnSurfaceMuted
                                )
                            }
                        }
                    }

                    Text(
                        text = report.headlineSummaryFa,
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.heroOnSurface.copy(alpha = 0.92f)
                    )

                    // 3 Mini KPI Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SchedulerHeroMetricPill(
                            label = "پوشش به‌موقع اقساط",
                            value = "${PersianCurrencyEngine.toPersianDigits(report.onTimeCoveragePercent.toString())}٪",
                            accent = Color(0xFF45E6D5),
                            modifier = Modifier.weight(1f)
                        )
                        SchedulerHeroMetricPill(
                            label = "حاشیه نقدی امن",
                            value = formatCompactPersianToman(report.projectedBufferOnBestDayToman, privacyMasked),
                            accent = Color(0xFFF5D28A),
                            modifier = Modifier.weight(1.25f)
                        )
                        SchedulerHeroMetricPill(
                            label = "تعداد چرخه درآمد",
                            value = "${PersianCurrencyEngine.toPersianDigits(report.detectedPaydays.size.toString())} واریز",
                            accent = ext.heroOnSurface,
                            modifier = Modifier.weight(0.95f)
                        )
                    }
                }
            }

            // 2. Single-Day vs. Split Bi-Cycle Switcher (shown when 2+ income cycles exist)
            if (report.secondaryRecommendedDay != null) {
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
                        PaymentScheduleStrategyMode.entries.forEach { mode ->
                            val selected = report.effectiveMode == mode
                            val bg by animateColorAsState(
                                targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                label = "schedModeBg"
                            )
                            val fg by animateColorAsState(
                                targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "schedModeFg"
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = bg,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { preferredMode = mode }
                                    .testTag("scheduler_mode_${mode.name.lowercase()}")
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                                    TarazinoAutoSizeText(
                                        text = mode.labelFa,
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
            }

            // 3. 30-Day Shamsi Liquidity & Optimal Payment Timing Graph
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "نمودار ۳۰ روزه نقدینگی و بهترین زمان پرداخت",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LegendDotLabel("بهینه", tealAccent)
                        LegendDotLabel("درآمد", goldAccent)
                        LegendDotLabel("سررسید", dangerAccent)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (ext.isDark) 0.40f else 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        ShamsiLiquidityTimelineChart(
                            evaluations = report.dailyEvaluations,
                            inspectedDay = inspectedDay,
                            tealAccent = tealAccent,
                            goldAccent = goldAccent,
                            dangerAccent = dangerAccent,
                            gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(112.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(1, 5, 10, 15, 20, 25, 30).forEach { dayMark ->
                                Text(
                                    text = PersianCurrencyEngine.toPersianDigits(dayMark.toString()),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = if (dayMark == inspectedDay) tealAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (dayMark == inspectedDay) FontWeight.ExtraBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Tappable Top Candidate Days & Selected Day Breakdown
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickInspectDays = buildList {
                        addAll(report.topCandidateDays.map { it.dayOfMonth })
                        report.secondaryRecommendedDay?.let { if (it !in this) add(it) }
                        report.detectedPaydays.forEach { if (it.medianDayOfMonth !in this) add(it.medianDayOfMonth) }
                    }.take(6)

                    quickInspectDays.forEach { day ->
                        val eval = report.dailyEvaluations.firstOrNull { it.dayOfMonth == day } ?: return@forEach
                        val selected = inspectedDay == day
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                selected -> tealAccent
                                eval.isPrimaryRecommended -> tealAccent.copy(alpha = 0.16f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (selected || eval.isPrimaryRecommended) tealAccent else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { inspectedDay = day }
                                .testTag("scheduler_candidate_day_$day")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                if (eval.isPrimaryRecommended) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = if (selected) MaterialTheme.colorScheme.onPrimary else tealAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Text(
                                    text = "روز ${PersianCurrencyEngine.toPersianDigits(day.toString())} (${PersianCurrencyEngine.toPersianDigits(eval.compositeScore.toString())})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Inspected Day Telemetry Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, tealAccent.copy(alpha = 0.30f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تحلیل روز ${PersianCurrencyEngine.toPersianDigits(inspectedEval.dayOfMonth.toString())} ام ماه — امتیاز ${PersianCurrencyEngine.toPersianDigits(inspectedEval.compositeScore.toString())} از ۱۰۰",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = tealAccent
                            )
                            Text(
                                text = "نقدینگی: ${PersianCurrencyEngine.toPersianDigits(inspectedEval.liquidityScore.toString())}/۴۵ • ایمنی سررسید: ${PersianCurrencyEngine.toPersianDigits(inspectedEval.deadlineSafetyScore.toString())}/۳۰ • همسویی درآمد: ${PersianCurrencyEngine.toPersianDigits(inspectedEval.historicalAlignmentScore.toString())}/۲۵",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "نقدینگی در دسترس",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatCompactPersianToman(inspectedEval.projectedNetCashToman, privacyMasked),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 4. Detected Historical Income Cycles & Quick Log Drawer
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "چرخه‌های درآمد شناسایی‌شده",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = goldAccent.copy(alpha = 0.16f),
                            border = BorderStroke(0.8.dp, goldAccent.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showAddIncomeLogForm = !showAddIncomeLogForm }
                                .testTag("scheduler_toggle_log_income_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "ثبت سابقه واریز درآمد",
                                    tint = goldAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (showAddIncomeLogForm) "بستن فرم" else "ثبت سابقه واریز",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = goldAccent
                                )
                            }
                        }
                    }

                    report.detectedPaydays.forEach { cluster ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(goldAccent.copy(alpha = 0.18f))
                                ) {
                                    Text(
                                        text = PersianCurrencyEngine.toPersianDigits(cluster.medianDayOfMonth.toString()),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = goldAccent
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cluster.primarySourceTitleFa,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "روز ${PersianCurrencyEngine.toPersianDigits(cluster.medianDayOfMonth.toString())} ماه • ثبات ${PersianCurrencyEngine.toPersianDigits(cluster.consistencyPercent.toString())}٪",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = formatCompactPersianToman(cluster.averageAmountToman, privacyMasked),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = tealAccent
                            )
                        }
                    }

                    // Expandable User Historical Income Deposit Logger
                    AnimatedVisibility(
                        visible = showAddIncomeLogForm,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newLogTitle,
                                onValueChange = { newLogTitle = it },
                                label = { Text("عنوان واریز (مثلاً حقوق آبان)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("scheduler_income_log_title_input")
                            )
                            MoneyInputField(
                                labelFa = "مبلغ واریزشده",
                                amountToman = newLogAmountToman,
                                onAmountChange = { newLogAmountToman = it },
                                currencyUnit = currencyUnit,
                                showQuickChips = false,
                                testTagName = "scheduler_income_log_amount_input"
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showDatePickerForLog = true }
                                    .testTag("scheduler_income_log_date_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CalendarMonth,
                                            contentDescription = "انتخاب تاریخ واریز درآمد",
                                            tint = goldAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text("تاریخ واریز شمسی:", style = MaterialTheme.typography.labelMedium)
                                    }
                                    Text(
                                        text = PersianCurrencyEngine.toPersianDigits(newLogDateJalali),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = tealAccent
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    if (newLogAmountToman > 0L) {
                                        val nextId = (historicalLogs.maxOfOrNull { it.id } ?: 0L) + 1L
                                        val entry = HistoricalIncomeDeposit(
                                            id = nextId,
                                            titleFa = newLogTitle.trim().ifBlank { "واریز درآمد" },
                                            amountToman = newLogAmountToman,
                                            receivedDateJalali = newLogDateJalali,
                                            isUserLogged = true
                                        )
                                        persistHistoricalLogs(listOf(entry) + historicalLogs)
                                        showAddIncomeLogForm = false
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("scheduler_save_income_log_btn")
                            ) {
                                Text("ثبت در تاریخچه و محاسبه مجدد بهترین روز", fontWeight = FontWeight.Bold)
                            }

                            if (historicalLogs.isNotEmpty()) {
                                Text(
                                    text = "سوابق ثبت‌شده توسط شما (${PersianCurrencyEngine.toPersianDigits(historicalLogs.size.toString())} مورد):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                historicalLogs.take(4).forEach { log ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${log.titleFa} • ${PersianCurrencyEngine.toPersianDigits(log.receivedDateJalali)} • ${formatCompactPersianToman(log.amountToman, privacyMasked)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { persistHistoricalLogs(historicalLogs.filterNot { it.id == log.id }) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "حذف سابقه درآمد",
                                                tint = dangerAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Rationale Bullets & Scheduled Upcoming Installment Mapping
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                report.rationaleBulletsFa.take(3).forEach { bullet ->
                    Text(
                        text = "• $bullet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (report.scheduledActions.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = tealAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "برنامه پیشنهادی پرداخت اقساط پیش‌رو",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        report.scheduledActions.take(4).forEach { action ->
                            val badgeColor = if (action.usesSecondaryCycle) goldAccent else tealAccent
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = action.obligationTitleFa,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val leadLabel = when {
                                        action.daysAheadOfDueDate > 0 -> "${PersianCurrencyEngine.toPersianDigits(action.daysAheadOfDueDate.toString())} روز پیش از سررسید (${PersianCurrencyEngine.toPersianDigits(action.originalDueDateJalali)})"
                                        action.daysAheadOfDueDate == 0 -> "دقیقاً در روز سررسید"
                                        else -> "سررسید ${PersianCurrencyEngine.toPersianDigits(action.originalDueDateJalali)}"
                                    }
                                    Text(
                                        text = leadLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = badgeColor.copy(alpha = 0.16f),
                                    border = BorderStroke(0.8.dp, badgeColor.copy(alpha = 0.45f))
                                ) {
                                    Text(
                                        text = "پرداخت: ${PersianCurrencyEngine.toPersianDigits(action.recommendedPayDateJalali)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = badgeColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. 1-Tap Automated Payment Schedule Activation Button (48dp Accessible Target)
            if (isCurrentRecommendationActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = tealAccent.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, tealAccent.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = tealAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            TarazinoAutoSizeText(
                                text = "زمان‌بند خودکار روز ${PersianCurrencyEngine.toPersianDigits(report.primaryRecommendedDay.toString())} ماه فعال است",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = tealAccent,
                                maxLines = 1,
                                minFontSize = 9.5.sp
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { onSaveAutomatedSchedule(false, 0, null) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("strategy_scheduler_disable_btn")
                    ) {
                        Text("غیرفعال", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(
                    onClick = {
                        onSaveAutomatedSchedule(
                            true,
                            report.primaryRecommendedDay,
                            activeSecondaryTarget
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tealAccent,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("strategy_scheduler_activate_btn")
                ) {
                    Icon(
                        Icons.Default.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TarazinoAutoSizeText(
                        text = buildString {
                            append("فعال‌سازی زمان‌بند خودکار برای روز ${PersianCurrencyEngine.toPersianDigits(report.primaryRecommendedDay.toString())}")
                            if (activeSecondaryTarget != null) {
                                append(" و ${PersianCurrencyEngine.toPersianDigits(activeSecondaryTarget.toString())}")
                            }
                            append(" هر ماه")
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        minFontSize = 10.sp
                    )
                }
            }
        }
    }

    if (showDatePickerForLog) {
        JalaliDatePickerModal(
            initialDateJalali = newLogDateJalali,
            onDismiss = { showDatePickerForLog = false },
            onConfirmDate = { picked ->
                newLogDateJalali = picked
                showDatePickerForLog = false
            }
        )
    }
}

@Composable
private fun SchedulerHeroMetricPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.14f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.5.sp,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            TarazinoAutoSizeText(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                maxLines = 1,
                minFontSize = 9.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LegendDotLabel(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ShamsiLiquidityTimelineChart(
    evaluations: List<DayScheduleEvaluation>,
    inspectedDay: Int,
    tealAccent: Color,
    goldAccent: Color,
    dangerAccent: Color,
    gridColor: Color,
    modifier: Modifier = Modifier
) {
    if (evaluations.isEmpty()) return

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val topPad = 14f
        val bottomPad = 12f
        val chartH = (h - topPad - bottomPad).coerceAtLeast(1f)
        val count = evaluations.size
        val stepX = if (count > 1) w / (count - 1).toFloat() else w

        val maxCash = evaluations.maxOf { it.projectedNetCashToman }.coerceAtLeast(1L).toFloat()
        val minCash = evaluations.minOf { it.projectedNetCashToman }.toFloat()
        val span = (maxCash - minCash).coerceAtLeast(1f)

        // Subtle horizontal guide lines
        listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
            val y = topPad + chartH * frac
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )
        }

        // Liquidity Area & Stroke Path
        val linePath = Path()
        val fillPath = Path()
        evaluations.forEachIndexed { idx, eval ->
            val x = idx * stepX
            val norm = ((eval.projectedNetCashToman.toFloat() - minCash) / span).coerceIn(0.06f, 1f)
            val y = topPad + chartH * (1f - norm)
            if (idx == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, h - bottomPad)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            if (idx == count - 1) {
                fillPath.lineTo(x, h - bottomPad)
                fillPath.close()
            }
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                listOf(tealAccent.copy(alpha = 0.34f), tealAccent.copy(alpha = 0.02f)),
                startY = topPad,
                endY = h - bottomPad
            )
        )
        drawPath(
            path = linePath,
            color = tealAccent,
            style = Stroke(width = 3.2f, cap = StrokeCap.Round)
        )

        // Draw Markers: Income Paydays (Gold), Installment Due Dates (Coral), Recommended & Inspected Day Pillars
        evaluations.forEachIndexed { idx, eval ->
            val x = idx * stepX
            val norm = ((eval.projectedNetCashToman.toFloat() - minCash) / span).coerceIn(0.06f, 1f)
            val y = topPad + chartH * (1f - norm)

            if (eval.incomeInflowToman > 0L) {
                drawLine(
                    color = goldAccent.copy(alpha = 0.75f),
                    start = Offset(x, topPad),
                    end = Offset(x, h - bottomPad),
                    strokeWidth = 2.2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f), 0f)
                )
                drawCircle(color = goldAccent, radius = 5.5f, center = Offset(x, topPad + 4f))
            }

            if (eval.installmentsDueCount > 0) {
                drawCircle(color = dangerAccent, radius = 4.5f, center = Offset(x, h - bottomPad - 4f))
            }

            if (eval.isPrimaryRecommended || eval.isSecondaryRecommended) {
                val pillarColor = if (eval.isPrimaryRecommended) tealAccent else goldAccent
                drawLine(
                    color = pillarColor,
                    start = Offset(x, y),
                    end = Offset(x, h - bottomPad),
                    strokeWidth = if (eval.isPrimaryRecommended) 4.5f else 3f,
                    cap = StrokeCap.Round
                )
                drawCircle(color = Color.White, radius = 7.5f, center = Offset(x, y))
                drawCircle(color = pillarColor, radius = 5f, center = Offset(x, y))
            } else if (eval.dayOfMonth == inspectedDay) {
                drawCircle(color = tealAccent, radius = 5.5f, center = Offset(x, y))
            }
        }
    }
}
