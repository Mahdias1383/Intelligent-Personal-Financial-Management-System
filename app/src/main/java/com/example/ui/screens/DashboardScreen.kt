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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.data.InstallmentEntity
import com.example.data.TrustPriority
import com.example.domain.AlertUrgencyBucket
import com.example.domain.EnrichedInstallmentAlert
import com.example.ui.TarazinoUiState

@Composable
fun DashboardScreen(
    uiState: TarazinoUiState,
    onQuickPay: (InstallmentEntity) -> Unit,
    onSnoozeTomorrow: (InstallmentEntity) -> Unit,
    onOpenObligationsTab: () -> Unit,
    onOpenIntelligenceTab: () -> Unit,
    onOpenFullA4Report: () -> Unit
) {
    val unit = uiState.preferences.currencyUnit
    val masked = uiState.preferences.privacyMaskEnabled
    val todayVerbose = remember { JalaliCalendarEngine.formatJalaliVerbose(JalaliCalendarEngine.todayJalali()) }

    var selectedBucketFilter by remember { mutableStateOf<AlertUrgencyBucket?>(null) }

    val totalPayable = uiState.totalDebtPayableToman
    val totalPaid = uiState.totalDebtPaidToman
    val totalRem = uiState.totalDebtRemainingToman
    val totalReceivable = uiState.totalReceivableRemainingToman
    val progressRatio = if (totalPayable > 0L) {
        (totalPaid.toFloat() / totalPayable.toFloat()).coerceIn(0f, 1f)
    } else 1f
    val progressPercent = (progressRatio * 100).toInt()

    val filteredAlerts = remember(uiState.smartAlertQueue, selectedBucketFilter) {
        if (selectedBucketFilter == null) uiState.smartAlertQueue
        else uiState.smartAlertQueue.filter { it.bucket == selectedBucketFilter }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Hero Financial Command Card with Graphic Backdrop & Animated Progress Ring
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_tarazino_1794581824442),
                        contentDescription = "بنر گرافیکی فرماندهی مالی ترازینو",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xCC061318),
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
                            Column {
                                Text(
                                    text = "امروز: $todayVerbose",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF6EE7B7)
                                )
                                Text(
                                    text = "ترازنامه تعهدات و اقساط فعال",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            // Circular Progress Ring
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(68.dp)
                            ) {
                                Canvas(modifier = Modifier.size(64.dp)) {
                                    drawArc(
                                        color = Color.White.copy(alpha = 0.16f),
                                        startAngle = 0f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                    drawArc(
                                        color = Color(0xFF10B981),
                                        startAngle = -90f,
                                        sweepAngle = 360f * progressRatio,
                                        useCenter = false,
                                        style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                                Text(
                                    text = "${PersianCurrencyEngine.toPersianDigits(progressPercent.toString())}٪",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3-Column KPI Strip inside Hero
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HeroKpiPill(
                                label = "مانده کل بدهی‌ها",
                                value = PersianCurrencyEngine.formatAmount(totalRem, unit, privacyMasked = masked),
                                accentColor = Color(0xFFF59E0B),
                                modifier = Modifier.weight(1f)
                            )
                            HeroKpiPill(
                                label = "پرداخت‌شده تا امروز",
                                value = PersianCurrencyEngine.formatAmount(totalPaid, unit, privacyMasked = masked),
                                accentColor = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HeroKpiPill(
                                label = "مطالبات و طلب از دیگران",
                                value = PersianCurrencyEngine.formatAmount(totalReceivable, unit, privacyMasked = masked),
                                accentColor = Color(0xFF38BDF8),
                                modifier = Modifier.weight(1f)
                            )
                            HeroKpiPill(
                                label = "کل تعهدات ثبت‌شده",
                                value = PersianCurrencyEngine.formatAmount(totalPayable, unit, privacyMasked = masked),
                                accentColor = Color(0xFFE2E8F0),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Hero Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onOpenObligationsTab,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier
                                    .weight(1f)
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
                                    text = "ثبت وام / قسط جدید",
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

        // 2. Current Month Cashflow & DTI Radar Strip
        uiState.dtiHealthReport?.let { dti ->
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenIntelligenceTab() }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "رادار نقدینگی ${dti.currentMonthYearLabelFa}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = dti.statusBadgeFa,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(dti.statusColorHex)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(dti.statusColorHex).copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "امتیاز سلامت: ${PersianCurrencyEngine.toPersianDigits(dti.obligationCoverageScore.toString())}/۱۰۰",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(dti.statusColorHex),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { (dti.dtiPercent / 100f).coerceIn(0f, 1f) },
                            color = Color(dti.statusColorHex),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "اقساط این ماه: ${
                                    PersianCurrencyEngine.formatAmount(
                                        dti.currentMonthDebtDueToman,
                                        unit,
                                        privacyMasked = masked
                                    )
                                } (${PersianCurrencyEngine.toPersianDigits(dti.dtiPercent.toString())}٪ درآمد)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "نقدینگی آزاد: ${
                                    PersianCurrencyEngine.formatAmount(
                                        dti.freeCashflowToman,
                                        unit,
                                        privacyMasked = masked
                                    )
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // 3. Smart Countdown Alert Radar Header + Bucket Filter Chips
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
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
                        Text(
                            text = "رادار هوشمند سررسیدها و هشدارها",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${PersianCurrencyEngine.toPersianDigits(filteredAlerts.size.toString())} مورد فعال",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    FilterChip(
                        selected = selectedBucketFilter == null,
                        onClick = { selectedBucketFilter = null },
                        label = {
                            Text("همه (${PersianCurrencyEngine.toPersianDigits(uiState.smartAlertQueue.size.toString())})")
                        }
                    )
                    AlertUrgencyBucket.entries.forEach { bucket ->
                        val count = uiState.smartAlertQueue.count { it.bucket == bucket }
                        FilterChip(
                            selected = selectedBucketFilter == bucket,
                            onClick = {
                                selectedBucketFilter = if (selectedBucketFilter == bucket) null else bucket
                            },
                            label = {
                                Text("${bucket.badgeFa} (${PersianCurrencyEngine.toPersianDigits(count.toString())})")
                            }
                        )
                    }
                }
            }
        }

        // 4. Alert Cards List
        if (filteredAlerts.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
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
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "هیچ قسط یا تعهد پرداخت‌نشده‌ای در این دسته وجود ندارد!",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else {
            items(filteredAlerts, key = { it.installment.id }) { alert ->
                SmartAlertRadarCard(
                    alert = alert,
                    currencyUnit = unit,
                    privacyMasked = masked,
                    onQuickPay = { onQuickPay(alert.installment) },
                    onSnoozeTomorrow = { onSnoozeTomorrow(alert.installment) }
                )
            }
        }
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
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFCBD5E1)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
    }
}

@Composable
private fun SmartAlertRadarCard(
    alert: EnrichedInstallmentAlert,
    currencyUnit: com.example.core.CurrencyUnit,
    privacyMasked: Boolean,
    onQuickPay: () -> Unit,
    onSnoozeTomorrow: () -> Unit
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
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isCritical || alert.bucket == AlertUrgencyBucket.OVERDUE) 1.5.dp else 0.8.dp,
                color = bucketColor.copy(alpha = 0.65f),
                shape = RoundedCornerShape(18.dp)
            )
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
                            color = Color(0xFFEF4444).copy(alpha = 0.18f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PriorityHigh,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "حیاتی / قول شرف",
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${alert.counterparty?.name ?: "طرف‌حساب"} • ${alert.obligation.categoryEnum.shortLabelFa}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = PersianCurrencyEngine.formatAmount(
                            amountToman = alert.installment.remainingToman,
                            unit = currencyUnit,
                            privacyMasked = privacyMasked
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (alert.installment.paidAmountToman > 0L) {
                        Text(
                            text = "پرداخت جزئی: ${
                                PersianCurrencyEngine.formatAmount(
                                    alert.installment.paidAmountToman,
                                    currencyUnit,
                                    privacyMasked = privacyMasked
                                )
                            }",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF3B82F6)
                        )
                    }
                }
            }

            if (alert.installment.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alert.installment.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
