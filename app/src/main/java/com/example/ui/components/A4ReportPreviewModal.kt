package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.PersianCurrencyEngine
import com.example.data.InstallmentStatus
import com.example.export.A4ReportPayload

@Composable
fun A4ReportPreviewModal(
    payload: A4ReportPayload,
    onDismiss: () -> Unit,
    onExportPdf: () -> Unit,
    onExportRtlExcel: () -> Unit,
    onExportCsv: () -> Unit
) {
    TarazinoRtlDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val ext = com.example.ui.theme.LocalTarazinoColors.current
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(10.dp)
                .testTag("a4_report_preview_modal"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Brush.verticalGradient(listOf(ext.cardBorderTop, ext.cardBorderBottom))
            ),
            shadowElevation = 14.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Top Action Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        TarazinoIconBadge(
                            icon = Icons.Default.PictureAsPdf,
                            accentColor = MaterialTheme.colorScheme.primary,
                            size = 40.dp,
                            iconSize = 20.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            TarazinoAutoSizeText(
                                text = "پیش‌نمایش رسمی برگه A4",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                minFontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                            TarazinoAutoSizeText(
                                text = "استاندارد ۲۱۰×۲۹۷ میلی‌متر • چاپ و PDF فارسی",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                minFontSize = 8.5.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "بستن پیش‌نمایش")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Symmetrical Single-Line Export Buttons Bar (Zero Text Wrapping)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onExportPdf,
                        shape = RoundedCornerShape(13.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        modifier = Modifier
                            .weight(1.35f)
                            .testTag("export_pdf_a4_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        TarazinoAutoSizeText(
                            text = "چاپ PDF A4",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            minFontSize = 9.sp,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    OutlinedButton(
                        onClick = onExportRtlExcel,
                        shape = RoundedCornerShape(13.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_xls_rtl_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        TarazinoAutoSizeText(
                            text = "اکسل",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            minFontSize = 9.sp,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    OutlinedButton(
                        onClick = onExportCsv,
                        shape = RoundedCornerShape(13.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(0.85f)
                            .testTag("export_csv_bom_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        TarazinoAutoSizeText(
                            text = "CSV",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            minFontSize = 9.sp,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Authentic White A4 Paper Canvas Preview
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        // 1. A4 Gradient Header Banner
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF064E3B), Color(0xFF0F172A))
                                        )
                                    )
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TarazinoAutoSizeText(
                                            text = "ترازینو — سامانه مدیریت اقساط",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFF34D399),
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            minFontSize = 8.5.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        TarazinoAutoSizeText(
                                            text = "سند: ${payload.trackingId}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFFDE68A),
                                            maxLines = 1,
                                            minFontSize = 8.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    TarazinoAutoSizeText(
                                        text = payload.reportTitleFa,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 2,
                                        minFontSize = 11.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TarazinoAutoSizeText(
                                            text = payload.subtitleFa,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFCBD5E1),
                                            maxLines = 1,
                                            minFontSize = 8.5.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        TarazinoAutoSizeText(
                                            text = "صدور: ${payload.issueDateJalali}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            maxLines = 1,
                                            minFontSize = 8.sp
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // 2. 4-Column Colorful KPI Summary Cards
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                A4KpiMiniBox(
                                    title = "کل تعهدات",
                                    value = PersianCurrencyEngine.formatAmount(payload.totalObligationsToman, payload.currencyUnit),
                                    borderColor = Color(0xFF0F172A),
                                    modifier = Modifier.weight(1f)
                                )
                                A4KpiMiniBox(
                                    title = "پرداخت‌شده",
                                    value = PersianCurrencyEngine.formatAmount(payload.totalPaidToman, payload.currencyUnit),
                                    borderColor = Color(0xFF059669),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                A4KpiMiniBox(
                                    title = "مانده بدهی/تعهد",
                                    value = PersianCurrencyEngine.formatAmount(payload.totalRemainingToman, payload.currencyUnit),
                                    borderColor = Color(0xFFD97706),
                                    modifier = Modifier.weight(1f)
                                )
                                A4KpiMiniBox(
                                    title = "پیشرفت تسویه",
                                    value = "${PersianCurrencyEngine.toPersianDigits(payload.progressPercent.toString())}٪",
                                    borderColor = Color(0xFF2563EB),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            LinearProgressIndicator(
                                progress = { payload.progressPercent / 100f },
                                color = Color(0xFF047857),
                                trackColor = Color(0xFFE2E8F0),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // 3. Table Header
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(Color(0xFF1E293B))
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    modifier = Modifier.width(22.dp),
                                    textAlign = TextAlign.Center
                                )
                                TarazinoAutoSizeText(
                                    text = "عنوان و طرف‌حساب",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    minFontSize = 8.sp,
                                    modifier = Modifier.weight(1.45f)
                                )
                                TarazinoAutoSizeText(
                                    text = "سررسید",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    minFontSize = 8.sp,
                                    modifier = Modifier.weight(0.85f),
                                    textAlign = TextAlign.Center
                                )
                                TarazinoAutoSizeText(
                                    text = "مبلغ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    minFontSize = 8.sp,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                                TarazinoAutoSizeText(
                                    text = "وضعیت",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    minFontSize = 8.sp,
                                    modifier = Modifier.weight(0.72f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // 4. Zebra-Striped Rows
                        itemsIndexed(payload.rows) { idx, row ->
                            val bg = if (idx % 2 == 0) Color(0xFFF8FAFC) else Color.White
                            val statusColor = when (row.statusEnum) {
                                InstallmentStatus.PAID -> Color(0xFF059669)
                                InstallmentStatus.PARTIAL -> Color(0xFF2563EB)
                                InstallmentStatus.OVERDUE -> Color(0xFFDC2626)
                                InstallmentStatus.PENDING -> Color(0xFFD97706)
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(bg)
                                    .padding(vertical = 7.dp, horizontal = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = PersianCurrencyEngine.toPersianDigits(row.rowNumber.toString()),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF334155),
                                        maxLines = 1,
                                        modifier = Modifier.width(22.dp),
                                        textAlign = TextAlign.Center
                                    )
                                    Column(modifier = Modifier.weight(1.45f)) {
                                        TarazinoAutoSizeText(
                                            text = row.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0F172A),
                                            maxLines = 2,
                                            minFontSize = 8.5.sp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        TarazinoAutoSizeText(
                                            text = "${row.counterpartyName} • ${row.trackingOrNote}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64748B),
                                            maxLines = 1,
                                            minFontSize = 7.5.sp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    TarazinoAutoSizeText(
                                        text = ltrIsolateDigits(row.dueDateJalali),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF1E293B),
                                        maxLines = 1,
                                        minFontSize = 7.5.sp,
                                        modifier = Modifier.weight(0.85f),
                                        textAlign = TextAlign.Center
                                    )
                                    TarazinoAutoSizeText(
                                        text = ltrIsolateDigits(
                                            PersianCurrencyEngine.formatAmount(
                                                row.totalAmountToman,
                                                payload.currencyUnit,
                                                includeUnit = false
                                            )
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        minFontSize = 7.5.sp,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )
                                    TarazinoAutoSizeText(
                                        text = row.statusLabelFa,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor,
                                        maxLines = 1,
                                        minFontSize = 7.5.sp,
                                        modifier = Modifier.weight(0.72f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(top = 6.dp))
                            }
                        }

                        // 5. Official Signature & Stamp Boxes
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(74.dp)
                                        .border(1.dp, Color(0xFF94A3B8), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "محل مهر و امضای پرداخت‌کننده / متعهد:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF475569),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(74.dp)
                                        .border(1.dp, Color(0xFF94A3B8), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "محل مهر و امضای دریافت‌کننده / طرف‌حساب:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF475569),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = payload.footerNoteFa,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B),
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
private fun A4KpiMiniBox(
    title: String,
    value: String,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp)
    ) {
        TarazinoAutoSizeText(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF475569),
            maxLines = 1,
            minFontSize = 8.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(2.dp))
        TarazinoAutoSizeText(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = borderColor,
            maxLines = 1,
            minFontSize = 8.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
