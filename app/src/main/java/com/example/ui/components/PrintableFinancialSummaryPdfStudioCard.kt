package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.CurrencyUnit
import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.data.CounterpartyEntity
import com.example.data.InstallmentEntity
import com.example.data.ObligationEntity
import com.example.data.UserPreferencesState
import com.example.domain.DebtTaggingAndCategorizationEngine
import com.example.domain.RecurringCashflowHealthEngine
import com.example.export.A4ReportPayload
import com.example.export.ComprehensiveFinancialPdfReportEngine
import com.example.ui.theme.LocalTarazinoColors
import java.io.File

private const val PREFS_NAME = "tarazino_prefs_v1"
private const val KEY_RECURRING_CASHFLOW_ITEMS_V1 = "recurring_non_loan_cashflow_items_v1"
private const val KEY_DEBT_TAG_OVERRIDES_V1 = "debt_category_tag_overrides_v1"

/**
 * Interactive card in Reports & Settings that builds, previews, saves to persistent app storage,
 * shares, and prints a comprehensive printable PDF summary report of all financial data,
 * categorized debts, cashflow balance, and monthly savings progress.
 */
@Composable
fun PrintableFinancialSummaryPdfStudioCard(
    obligations: List<ObligationEntity>,
    installments: List<InstallmentEntity>,
    counterparties: List<CounterpartyEntity>,
    preferences: UserPreferencesState,
    onOpenA4PreviewPayload: (A4ReportPayload) -> Unit,
    onShowToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val ext = LocalTarazinoColors.current
    val prefs = remember(context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    val recurringItems = remember(obligations, preferences) {
        RecurringCashflowHealthEngine.deserializeItems(
            prefs.getString(KEY_RECURRING_CASHFLOW_ITEMS_V1, null)
        )
    }
    val tagOverrides = remember(obligations) {
        DebtTaggingAndCategorizationEngine.deserializeTagOverrides(
            prefs.getString(KEY_DEBT_TAG_OVERRIDES_V1, null)
        )
    }

    val summaryReport = remember(
        obligations,
        installments,
        counterparties,
        preferences,
        recurringItems,
        tagOverrides
    ) {
        ComprehensiveFinancialPdfReportEngine.buildComprehensiveSummaryReport(
            obligations = obligations,
            installments = installments,
            counterparties = counterparties,
            recurringCashflowItems = recurringItems,
            monthlyIncomeToman = preferences.monthlyIncomeToman,
            livingExpensePercent = preferences.livingExpensePercent,
            savingsGoalTitleFa = preferences.monthlySavingsGoalTitle,
            savingsTargetToman = preferences.monthlySavingsTargetToman,
            savingsSavedToman = preferences.monthlySavingsSavedToman,
            categoryOverrides = tagOverrides.first,
            customTagsMap = tagOverrides.second,
            currencyUnit = preferences.currencyUnit,
            todayJalali = JalaliCalendarEngine.todayJalali()
        )
    }

    var savedRecords by remember {
        mutableStateOf(ComprehensiveFinancialPdfReportEngine.listSavedPdfRecords(context))
    }
    var lastSavedFileName by remember {
        mutableStateOf(savedRecords.firstOrNull()?.fileName)
    }

    val unit = preferences.currencyUnit
    val masked = preferences.privacyMaskEnabled

    TarazinoGlassCard(
        cornerRadius = 24.dp,
        accentBorderColor = ext.heroAccent,
        modifier = modifier
            .fillMaxWidth()
            .testTag("printable_financial_summary_pdf_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TarazinoSectionHeader(
                title = "کارنامه جامع PDF قابل چاپ (دارایی، بدهی و پیشرفت پس‌انداز)",
                subtitle = "تولید، بایگانی دائمی و چاپ گزارش یکپارچه تمام اطلاعات مالی، وام‌ها و هدف پس‌انداز",
                icon = Icons.Default.Print,
                accentColor = ext.heroAccent,
                trailing = {
                    TarazinoStatusPill(
                        text = "آماده چاپ A4",
                        accentColor = ext.positive
                    )
                }
            )

            // 1. Executive Snapshot of the Printable PDF Content (Debts + Savings Progress + Cashflow Health)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.72f else 0.90f),
                border = BorderStroke(0.9.dp, ext.cardBorderTop),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "خلاصه محتوای گزارش جامع (${summaryReport.issueDateVerboseFa}):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "سند: ${summaryReport.documentId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Debt Summary Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(9.dp)) {
                                TarazinoAutoSizeText(
                                    text = "مانده بدهی (${PersianCurrencyEngine.toPersianDigits(summaryReport.totalActiveDebtsCount.toString())} پرونده)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    minFontSize = 8.sp
                                )
                                FormattedMoneyText(
                                    amountToman = summaryReport.totalDebtRemainingToman,
                                    unit = unit,
                                    privacyMasked = masked,
                                    style = MaterialTheme.typography.labelLarge,
                                    numberColor = ext.heroAccent,
                                    minFontSize = 8.5.sp
                                )
                                Text(
                                    text = "تسویه کل: ${PersianCurrencyEngine.toPersianDigits(summaryReport.overallDebtSettlementPercent.toString())}٪",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.positive
                                )
                            }
                        }

                        // Savings Progress Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(9.dp)) {
                                TarazinoAutoSizeText(
                                    text = "پس‌انداز ماه (${summaryReport.shamsiMonthNameFa})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    minFontSize = 8.sp
                                )
                                FormattedMoneyText(
                                    amountToman = summaryReport.savingsSummary.savedToman,
                                    unit = unit,
                                    privacyMasked = masked,
                                    style = MaterialTheme.typography.labelLarge,
                                    numberColor = ext.positive,
                                    minFontSize = 8.5.sp
                                )
                                Text(
                                    text = "تحقق هدف: ${PersianCurrencyEngine.toPersianDigits(summaryReport.savingsSummary.progressPercent.toString())}٪",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Executive Notes Preview
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        summaryReport.executiveNotesFa.forEach { note ->
                            Text(
                                text = "• $note",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // 2. Primary Action Buttons: Save & Share Printable PDF | Direct Print Spooler | Live A4 Preview
            Button(
                onClick = {
                    val savedFile = ComprehensiveFinancialPdfReportEngine.generateAndSavePrintablePdfFile(
                        context = context,
                        report = summaryReport
                    )
                    savedRecords = ComprehensiveFinancialPdfReportEngine.listSavedPdfRecords(context)
                    lastSavedFileName = savedFile.name
                    ComprehensiveFinancialPdfReportEngine.shareSavedPdfRecord(context, savedFile)
                    onShowToast("گزارش جامع PDF (${savedFile.name}) در بایگانی ذخیره و آماده چاپ شد.")
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("generate_and_save_comprehensive_pdf_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.SaveAlt,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                TarazinoAutoSizeText(
                    text = "تولید، ذخیره در بایگانی و اشتراک‌گذاری PDF جامع مالی و پس‌انداز",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    minFontSize = 9.5.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val savedFile = ComprehensiveFinancialPdfReportEngine.generateAndSavePrintablePdfFile(
                            context = context,
                            report = summaryReport
                        )
                        savedRecords = ComprehensiveFinancialPdfReportEngine.listSavedPdfRecords(context)
                        lastSavedFileName = savedFile.name
                        val printed = ComprehensiveFinancialPdfReportEngine.launchAndroidPrintJob(
                            context = context,
                            pdfFile = savedFile
                        )
                        if (printed) {
                            onShowToast("سرویس چاپگر استاندارد A4 اندروید باز شد.")
                        } else {
                            ComprehensiveFinancialPdfReportEngine.shareSavedPdfRecord(context, savedFile)
                            onShowToast("فایل PDF جامع (${savedFile.name}) ذخیره و آماده چاپ شد.")
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("print_comprehensive_pdf_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    TarazinoAutoSizeText(
                        text = "چاپ مستقیم A4",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        minFontSize = 8.5.sp
                    )
                }

                OutlinedButton(
                    onClick = {
                        val a4Payload = ComprehensiveFinancialPdfReportEngine.toA4PreviewPayload(summaryReport)
                        onOpenA4PreviewPayload(a4Payload)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("preview_comprehensive_pdf_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    TarazinoAutoSizeText(
                        text = "پیش‌نمایش برگه جامع",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        minFontSize = 8.5.sp
                    )
                }
            }

            // 3. Saved PDF Records Archive List
            if (savedRecords.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FolderSpecial,
                            contentDescription = null,
                            tint = ext.heroAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "بایگانی گزارش‌های PDF ذخیره‌شده در دستگاه:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${PersianCurrencyEngine.toPersianDigits(savedRecords.size.toString())} فایل",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    savedRecords.take(3).forEach { record ->
                        val isLatest = record.fileName == lastSavedFileName
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isLatest) {
                                ext.positive.copy(alpha = 0.12f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
                            },
                            border = BorderStroke(
                                0.8.dp,
                                if (isLatest) ext.positive.copy(alpha = 0.5f) else ext.cardBorderTop
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    ComprehensiveFinancialPdfReportEngine.shareSavedPdfRecord(
                                        context,
                                        File(record.absolutePath)
                                    )
                                }
                                .testTag("saved_pdf_record_row_${record.fileName}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isLatest) Icons.Default.CheckCircle else Icons.Default.Description,
                                        contentDescription = null,
                                        tint = if (isLatest) ext.positive else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        TarazinoAutoSizeText(
                                            text = record.fileName,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            minFontSize = 8.5.sp
                                        )
                                        val kb = (record.sizeBytes / 1024L).coerceAtLeast(1L)
                                        Text(
                                            text = "حجم: ${PersianCurrencyEngine.toPersianDigits(kb.toString())} کیلوبایت • ذخیره دائمی در پرونده کاربر",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "اشتراک‌گذاری یا چاپ مجدد",
                                    tint = MaterialTheme.colorScheme.primary,
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
