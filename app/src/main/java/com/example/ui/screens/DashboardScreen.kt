package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.data.CounterpartyEntity
import com.example.data.EntityCategory
import com.example.data.InstallmentEntity
import com.example.data.ObligationEntity
import com.example.data.TrustPriority
import com.example.domain.AlertUrgencyBucket
import com.example.domain.EnrichedInstallmentAlert
import com.example.ui.TarazinoUiState

private data class CategoryShareSlice(
    val category: EntityCategory,
    val labelFa: String,
    val subProvidersPreviewFa: String,
    val remainingToman: Long,
    val shareRatio: Float,
    val color: Color
)

@Composable
fun DashboardScreen(
    uiState: TarazinoUiState,
    onQuickPay: (InstallmentEntity) -> Unit,
    onSnoozeTomorrow: (InstallmentEntity) -> Unit,
    onOpenNewObligationWizard: () -> Unit,
    onOpenCategoryInTree: (EntityCategory) -> Unit,
    onOpenCounterpartyInTree: (CounterpartyEntity) -> Unit,
    onOpenObligationInTree: (ObligationEntity, EntityCategory?) -> Unit,
    onOpenIntelligenceTab: () -> Unit,
    onOpenFullA4Report: () -> Unit
) {
    val unit = uiState.preferences.currencyUnit
    val masked = uiState.preferences.privacyMaskEnabled
    val todayVerbose = remember { JalaliCalendarEngine.formatJalaliVerbose(JalaliCalendarEngine.todayJalali()) }

    var showAllAlertsModal by remember { mutableStateOf(false) }

    val totalPayable = uiState.totalDebtPayableToman
    val totalPaid = uiState.totalDebtPaidToman
    val totalRem = uiState.totalDebtRemainingToman
    val progressRatio = if (totalPayable > 0L) {
        (totalPaid.toFloat() / totalPayable.toFloat()).coerceIn(0f, 1f)
    } else 1f
    val progressPercent = (progressRatio * 100).toInt()

    // Calculate Category Share Slices for Donut Chart (ordered with BNPL first)
    val categorySlices = remember(uiState.counterparties, uiState.obligations, uiState.installments) {
        val ordered = listOf(
            EntityCategory.BNPL_PLATFORM,
            EntityCategory.BANK,
            EntityCategory.INSTITUTION,
            EntityCategory.PERSON
        )
        val rawTotals = ordered.map { cat ->
            val catProviders = uiState.counterparties.filter { it.categoryEnum == cat }
            val providerIds = catProviders.map { it.id }.toSet()
            val debtObIds = uiState.obligations
                .filter { it.counterpartyId in providerIds && !it.categoryEnum.isReceivable }
                .map { it.id }
                .toSet()
            val rem = uiState.installments
                .filter { it.obligationId in debtObIds }
                .sumOf { it.remainingToman }
            val namesPreview = catProviders.take(3).joinToString("، ") {
                it.name.substringBefore(" (")
            }.ifBlank { "بدون زیرمجموعه" }
            Triple(cat, rem, namesPreview)
        }
        val grandTotal = rawTotals.sumOf { it.second }.coerceAtLeast(1L)
        rawTotals.map { (cat, rem, preview) ->
            CategoryShareSlice(
                category = cat,
                labelFa = cat.labelFa,
                subProvidersPreviewFa = preview,
                remainingToman = rem,
                shareRatio = (rem.toFloat() / grandTotal.toFloat()).coerceIn(0f, 1f),
                color = Color(cat.defaultColorHex)
            )
        }
    }

    val top3UrgentAlerts = remember(uiState.smartAlertQueue) {
        uiState.smartAlertQueue.take(3)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SECTION 1: کارت خلاصه وضعیت ماه و تراز کل (Clean Monthly & Executive Summary Card)
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_tarazino_1794581824442),
                        contentDescription = "خلاصه وضعیت ماهانه ترازینو",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(235.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(235.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xD9061318),
                                        Color(0xF20B171D),
                                        Color(0xFF0B1215)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "امروز: $todayVerbose",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF6EE7B7)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = uiState.dtiHealthReport?.let { "خلاصه وضعیت ${it.currentMonthYearLabelFa}" }
                                        ?: "خلاصه وضعیت تعهدات ماه جاری",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            // Clean Progress Ring
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Canvas(modifier = Modifier.size(60.dp)) {
                                    drawArc(
                                        color = Color.White.copy(alpha = 0.15f),
                                        startAngle = 0f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                    drawArc(
                                        color = Color(0xFF10B981),
                                        startAngle = -90f,
                                        sweepAngle = 360f * progressRatio,
                                        useCenter = false,
                                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${PersianCurrencyEngine.toPersianDigits(progressPercent.toString())}٪",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "تسویه",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFA7F3D0)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Focused 3-Metric Strip (Current Month Due, Total Remaining Debt, Paid So Far)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val currentMonthDue = uiState.dtiHealthReport?.currentMonthDebtDueToman ?: 0L
                            HeroKpiPill(
                                label = "اقساط مانده این ماه",
                                value = PersianCurrencyEngine.formatAmount(currentMonthDue, unit, privacyMasked = masked),
                                accentColor = Color(0xFFF59E0B),
                                modifier = Modifier.weight(1f)
                            )
                            HeroKpiPill(
                                label = "کل بدهی باقی‌مانده",
                                value = PersianCurrencyEngine.formatAmount(totalRem, unit, privacyMasked = masked),
                                accentColor = Color(0xFF38BDF8),
                                modifier = Modifier.weight(1f)
                            )
                            HeroKpiPill(
                                label = "تسویه‌شده تا امروز",
                                value = PersianCurrencyEngine.formatAmount(totalPaid, unit, privacyMasked = masked),
                                accentColor = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Primary Actions Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onOpenNewObligationWizard,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier
                                    .weight(1.25f)
                                    .testTag("dashboard_add_obligation_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF042F2E),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ثبت قسط / خرید جدید",
                                    color = Color(0xFF042F2E),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = onOpenFullA4Report,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dashboard_open_a4_report_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = Color(0xFF6EE7B7),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "گزارش A4 و اکسل",
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // SECTION 2: نمودار حلقه‌ای سهم هر دسته و زیرمجموعه‌ها (Interactive Category Share Donut Chart)
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_category_donut_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DonutLarge,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "سهم هر دسته از تعهدات فعال",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "روی هر دسته (مثلاً خرید اقساطی اسنپ‌پی/دیجی‌پی) بزنید تا پوشه آن باز شود",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Interactive Multi-Segment Donut Canvas
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(132.dp)
                        ) {
                            Canvas(modifier = Modifier.size(120.dp)) {
                                val strokeWidth = 18.dp.toPx()
                                if (totalRem <= 0L) {
                                    drawArc(
                                        color = Color(0xFF10B981).copy(alpha = 0.25f),
                                        startAngle = 0f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth)
                                    )
                                } else {
                                    var currentStartAngle = -90f
                                    categorySlices.forEach { slice ->
                                        val sweep = (slice.shareRatio * 360f).coerceAtLeast(if (slice.remainingToman > 0L) 6f else 0f)
                                        if (sweep > 0f) {
                                            drawArc(
                                                color = slice.color,
                                                startAngle = currentStartAngle,
                                                sweepAngle = (sweep - 2.5f).coerceAtLeast(2f),
                                                useCenter = false,
                                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                            )
                                            currentStartAngle += sweep
                                        }
                                    }
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${PersianCurrencyEngine.toPersianDigits(uiState.obligations.size.toString())} پرونده",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${PersianCurrencyEngine.toPersianDigits(uiState.counterparties.size.toString())} زیرمجموعه",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Category Breakdown Legend with 1-Tap Drill into Tree
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categorySlices.forEach { slice ->
                                val pct = (slice.shareRatio * 100).toInt()
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = slice.color.copy(alpha = 0.1f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenCategoryInTree(slice.category) }
                                        .testTag("donut_legend_${slice.category.name}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(slice.color)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = "${slice.category.badgeFa} (${PersianCurrencyEngine.toPersianDigits(pct.toString())}٪)",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = slice.subProvidersPreviewFa,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = null,
                                            tint = slice.color,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Quick Sub-Provider Shortcut Pills (اسنپ‌پی، دیجی‌پی، بانک مهر و...)
                    if (uiState.counterparties.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "دسترسی سریع به طرف پرداخت:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            uiState.counterparties.forEach { cp ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(cp.avatarColorHex).copy(alpha = 0.14f),
                                    modifier = Modifier
                                        .clickable { onOpenCounterpartyInTree(cp) }
                                        .testTag("dashboard_quick_cp_${cp.id}")
                                ) {
                                    Text(
                                        text = cp.name.substringBefore(" ("),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(cp.avatarColorHex),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // SECTION 3: لیست ۳ سررسید فوری با طراحی خلوت (Focused Top 3 Urgent Due Dates)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "۳ سررسید و قسط فوری پیش‌رو",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "مهم‌ترین اقساط نزدیک برای پرداخت سریع بدون شلوغی صفحه",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (uiState.smartAlertQueue.size > 3) {
                    TextButton(
                        onClick = { showAllAlertsModal = true },
                        modifier = Modifier.testTag("dashboard_view_all_alerts_btn")
                    ) {
                        Text(
                            text = "همه (${PersianCurrencyEngine.toPersianDigits(uiState.smartAlertQueue.size.toString())}) ‹",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (top3UrgentAlerts.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "عالی! هیچ قسط فوری یا معوقه‌ای وجود ندارد.",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            items(top3UrgentAlerts, key = { "top_alert_${it.installment.id}" }) { alert ->
                SmartAlertRadarCard(
                    alert = alert,
                    currencyUnit = unit,
                    privacyMasked = masked,
                    onQuickPay = { onQuickPay(alert.installment) },
                    onSnoozeTomorrow = { onSnoozeTomorrow(alert.installment) },
                    onOpenInTree = {
                        onOpenObligationInTree(alert.obligation, alert.counterparty?.categoryEnum)
                    }
                )
            }
        }

        // Compact Cashflow & Intelligence Link Footer Card
        uiState.dtiHealthReport?.let { dti ->
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenIntelligenceTab() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = Color(dti.statusColorHex)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "شاخص سلامت نقدینگی: ${dti.statusBadgeFa}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "نقدینگی آزاد ماه: ${
                                        PersianCurrencyEngine.formatAmount(
                                            dti.freeCashflowToman,
                                            unit,
                                            privacyMasked = masked
                                        )
                                    } • مشاهده تقویم حرارتی ‹",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(dti.statusColorHex).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${PersianCurrencyEngine.toPersianDigits(dti.obligationCoverageScore.toString())}/۱۰۰",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(dti.statusColorHex),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet for Viewing All Alerts & Filtering by Bucket
    if (showAllAlertsModal) {
        AllAlertsModalDialog(
            alerts = uiState.smartAlertQueue,
            currencyUnit = unit,
            privacyMasked = masked,
            onDismiss = { showAllAlertsModal = false },
            onQuickPay = onQuickPay,
            onSnoozeTomorrow = onSnoozeTomorrow,
            onOpenAlertInTree = { alert ->
                showAllAlertsModal = false
                onOpenObligationInTree(alert.obligation, alert.counterparty?.categoryEnum)
            }
        )
    }
}

@Composable
private fun HeroKpiPill(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFCBD5E1),
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            maxLines = 1
        )
    }
}

@Composable
private fun SmartAlertRadarCard(
    alert: EnrichedInstallmentAlert,
    currencyUnit: com.example.core.CurrencyUnit,
    privacyMasked: Boolean,
    onQuickPay: () -> Unit,
    onSnoozeTomorrow: () -> Unit,
    onOpenInTree: () -> Unit
) {
    val bucketColor = Color(alert.bucket.colorHex)
    val isCritical = alert.obligation.priorityEnum == TrustPriority.CRITICAL ||
        alert.counterparty?.priorityEnum == TrustPriority.CRITICAL

    val countdownTextFa = when {
        alert.daysUntilDue < 0 -> "${PersianCurrencyEngine.toPersianDigits((-alert.daysUntilDue).toString())} روز گذشته از سررسید!"
        alert.daysUntilDue == 0 -> "سررسید امروز!"
        else -> "${PersianCurrencyEngine.toPersianDigits(alert.daysUntilDue.toString())} روز مانده تا سررسید"
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isCritical || alert.bucket == AlertUrgencyBucket.OVERDUE) 1.4.dp else 0.8.dp,
                color = bucketColor.copy(alpha = 0.55f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onOpenInTree)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = bucketColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = countdownTextFa,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = bucketColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (isCritical) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.16f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PriorityHigh,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "حیاتی",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = PersianCurrencyEngine.toPersianDigits(alert.installment.dueDateJalali),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alert.installment.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${alert.counterparty?.name ?: "طرف‌حساب"} • ${alert.obligation.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = PersianCurrencyEngine.formatAmount(
                        amountToman = alert.installment.remainingToman,
                        unit = currencyUnit,
                        privacyMasked = privacyMasked
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onQuickPay,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("quick_pay_btn_${alert.installment.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (alert.obligation.categoryEnum.isReceivable) "ثبت وصول سریع" else "ثبت پرداخت سریع",
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onSnoozeTomorrow,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("snooze_btn_${alert.installment.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("یادآوری فردا")
                }
            }
        }
    }
}

@Composable
private fun AllAlertsModalDialog(
    alerts: List<EnrichedInstallmentAlert>,
    currencyUnit: com.example.core.CurrencyUnit,
    privacyMasked: Boolean,
    onDismiss: () -> Unit,
    onQuickPay: (InstallmentEntity) -> Unit,
    onSnoozeTomorrow: (InstallmentEntity) -> Unit,
    onOpenAlertInTree: (EnrichedInstallmentAlert) -> Unit
) {
    var selectedBucketFilter by remember { mutableStateOf<AlertUrgencyBucket?>(null) }
    val filtered = remember(alerts, selectedBucketFilter) {
        if (selectedBucketFilter == null) alerts
        else alerts.filter { it.bucket == selectedBucketFilter }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "همه سررسیدها و هشدارهای اقساط (${PersianCurrencyEngine.toPersianDigits(filtered.size.toString())} مورد)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    FilterChip(
                        selected = selectedBucketFilter == null,
                        onClick = { selectedBucketFilter = null },
                        label = { Text("همه (${PersianCurrencyEngine.toPersianDigits(alerts.size.toString())})") }
                    )
                    AlertUrgencyBucket.entries.forEach { bucket ->
                        val c = alerts.count { it.bucket == bucket }
                        FilterChip(
                            selected = selectedBucketFilter == bucket,
                            onClick = {
                                selectedBucketFilter = if (selectedBucketFilter == bucket) null else bucket
                            },
                            label = { Text("${bucket.badgeFa} (${PersianCurrencyEngine.toPersianDigits(c.toString())})") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { "modal_alert_${it.installment.id}" }) { alert ->
                        SmartAlertRadarCard(
                            alert = alert,
                            currencyUnit = currencyUnit,
                            privacyMasked = privacyMasked,
                            onQuickPay = { onQuickPay(alert.installment) },
                            onSnoozeTomorrow = { onSnoozeTomorrow(alert.installment) },
                            onOpenInTree = { onOpenAlertInTree(alert) }
                        )
                    }
                }
            }
        }
    }
}
