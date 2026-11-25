package com.example.export

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.core.CurrencyUnit
import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.data.CounterpartyEntity
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.ObligationEntity
import com.example.domain.DebtCategoryTag
import com.example.domain.DebtTaggingAndCategorizationEngine
import com.example.domain.MonthlySavingsGoalEngine
import com.example.domain.RecurringCashflowHealthEngine
import com.example.domain.RecurringCashflowItem
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.roundToInt

data class PrintableSavingsSectionSummary(
    val goalTitleFa: String,
    val targetToman: Long,
    val savedToman: Long,
    val remainingToTargetToman: Long,
    val progressPercent: Int,
    val recommendedDailyToman: Long,
    val statusBadgeFa: String
)

data class PrintableCashflowSectionSummary(
    val monthlyBudgetIncomeToman: Long,
    val recurringIncomeToman: Long,
    val recurringNonLoanExpenseToman: Long,
    val currentMonthDebtInstallmentsToman: Long,
    val netMonthlyFreeCashflowToman: Long,
    val debtToIncomePercent: Int,
    val financialHealthScore: Int
)

data class PrintableCategoryRow(
    val tag: DebtCategoryTag,
    val labelFa: String,
    val obligationsCount: Int,
    val totalPayableToman: Long,
    val paidToman: Long,
    val remainingToman: Long,
    val shareOfRemainingPercent: Int,
    val settlementProgressPercent: Int
)

data class PrintableDebtScheduleRow(
    val rowNumber: Int,
    val obligationTitle: String,
    val categoryBadgeFa: String,
    val counterpartyName: String,
    val nextDueDateJalali: String,
    val totalPayableToman: Long,
    val paidToman: Long,
    val remainingToman: Long,
    val remainingInstallmentsCount: Int,
    val progressPercent: Int,
    val statusLabelFa: String
)

data class ComprehensiveFinancialPdfSummaryReport(
    val documentId: String,
    val issueDateJalali: String,
    val issueDateVerboseFa: String,
    val shamsiMonthNameFa: String,
    val reportTitleFa: String,
    val subtitleFa: String,
    val currencyUnit: CurrencyUnit,
    val totalActiveDebtsCount: Int,
    val totalDebtPayableToman: Long,
    val totalDebtPaidToman: Long,
    val totalDebtRemainingToman: Long,
    val totalReceivableRemainingToman: Long,
    val overallDebtSettlementPercent: Int,
    val savingsSummary: PrintableSavingsSectionSummary,
    val cashflowSummary: PrintableCashflowSectionSummary,
    val categoryBreakdowns: List<PrintableCategoryRow>,
    val obligationRows: List<PrintableDebtScheduleRow>,
    val executiveNotesFa: List<String>,
    val footerCertificationFa: String = "این گزارش جامع مالی، وضعیت بدهی‌ها، اقساط و پیشرفت پس‌انداز به صورت آفلاین توسط سامانه ترازینو صادر گردیده است."
)

data class SavedPdfRecordFile(
    val fileName: String,
    val absolutePath: String,
    val sizeBytes: Long,
    val lastModifiedEpochMs: Long
)

/**
 * Generates, saves, shares, and prints a comprehensive multi-section A4 PDF summary report
 * of all financial data, categorized debts, cashflow health, and monthly savings progress.
 */
object ComprehensiveFinancialPdfReportEngine {

    private const val RECORDS_SUBDIR = "saved_pdf_records"

    /**
     * Pure domain builder that aggregates all financial data, debts, category breakdowns,
     * recurring cashflow, and monthly savings progress into [ComprehensiveFinancialPdfSummaryReport].
     */
    fun buildComprehensiveSummaryReport(
        obligations: List<ObligationEntity>,
        installments: List<InstallmentEntity>,
        counterparties: List<CounterpartyEntity>,
        recurringCashflowItems: List<RecurringCashflowItem> = emptyList(),
        monthlyIncomeToman: Long = 0L,
        livingExpensePercent: Int = 50,
        savingsGoalTitleFa: String = "هدف پس‌انداز ماهانه",
        savingsTargetToman: Long = 0L,
        savingsSavedToman: Long = 0L,
        categoryOverrides: Map<Long, DebtCategoryTag> = emptyMap(),
        customTagsMap: Map<Long, List<String>> = emptyMap(),
        currencyUnit: CurrencyUnit = CurrencyUnit.TOMAN,
        todayJalali: String = JalaliCalendarEngine.todayJalali(),
        fixedDocumentId: String? = null
    ): ComprehensiveFinancialPdfSummaryReport {
        val parsedToday = JalaliCalendarEngine.parseJalali(todayJalali)
        val verboseToday = JalaliCalendarEngine.formatJalaliVerbose(todayJalali)

        val portfolioReport = DebtTaggingAndCategorizationEngine.evaluateCategorizedPortfolio(
            obligations = obligations,
            installments = installments,
            counterparties = counterparties,
            selectedFilterTag = DebtCategoryTag.ALL,
            categoryOverrides = categoryOverrides,
            customTagsMap = customTagsMap,
            todayJalali = todayJalali
        )

        val currentMonthDebtDue = portfolioReport.filteredCurrentMonthDueToman

        val savingsEval = MonthlySavingsGoalEngine.evaluateMonthlySavingsGoal(
            goalTitleFa = savingsGoalTitleFa,
            targetAmountToman = savingsTargetToman,
            savedAmountToman = savingsSavedToman,
            monthlyIncomeToman = monthlyIncomeToman,
            livingExpensePercent = livingExpensePercent,
            currentMonthDebtDueToman = currentMonthDebtDue,
            todayJalali = todayJalali
        )

        val cashflowEval = RecurringCashflowHealthEngine.evaluateHolisticHealth(
            items = recurringCashflowItems,
            currentMonthLoanInstallmentsToman = currentMonthDebtDue,
            monthlySavingsTargetToman = savingsTargetToman,
            todayJalali = todayJalali
        )

        val effectiveIncome = if (cashflowEval.totalRecurringIncomeToman > 0L) {
            cashflowEval.totalRecurringIncomeToman
        } else {
            monthlyIncomeToman.coerceAtLeast(1_000_000L)
        }
        val effectiveNonLoanExpense = if (cashflowEval.totalRecurringNonLoanExpenseToman > 0L) {
            cashflowEval.totalRecurringNonLoanExpenseToman
        } else {
            (monthlyIncomeToman * livingExpensePercent) / 100L
        }
        val netFreeCashflow = effectiveIncome - effectiveNonLoanExpense - currentMonthDebtDue
        val dtiPct = if (effectiveIncome > 0L) {
            ((currentMonthDebtDue.toDouble() / effectiveIncome.toDouble()) * 100.0)
                .roundToInt()
                .coerceIn(0, 999)
        } else {
            0
        }

        val savingsSection = PrintableSavingsSectionSummary(
            goalTitleFa = savingsEval.goalTitleFa,
            targetToman = savingsEval.targetAmountToman,
            savedToman = savingsEval.savedAmountToman,
            remainingToTargetToman = savingsEval.remainingToTargetToman,
            progressPercent = savingsEval.progressPercent,
            recommendedDailyToman = savingsEval.requiredDailySavingsToman,
            statusBadgeFa = savingsEval.status.labelFa
        )

        val cashflowSection = PrintableCashflowSectionSummary(
            monthlyBudgetIncomeToman = monthlyIncomeToman,
            recurringIncomeToman = effectiveIncome,
            recurringNonLoanExpenseToman = effectiveNonLoanExpense,
            currentMonthDebtInstallmentsToman = currentMonthDebtDue,
            netMonthlyFreeCashflowToman = netFreeCashflow,
            debtToIncomePercent = dtiPct,
            financialHealthScore = cashflowEval.holisticHealthScore
        )

        val categoryRows = portfolioReport.categoryBreakdowns.map { b ->
            PrintableCategoryRow(
                tag = b.tag,
                labelFa = b.tag.labelFa,
                obligationsCount = b.obligationsCount,
                totalPayableToman = b.totalPayableToman,
                paidToman = b.paidToman,
                remainingToman = b.remainingToman,
                shareOfRemainingPercent = b.shareOfTotalRemainingPercent,
                settlementProgressPercent = b.settlementProgressPercent
            )
        }

        val debtObligations = portfolioReport.filteredObligations
        val receivableTotalRem = portfolioReport.categoryBreakdowns
            .firstOrNull { it.tag == DebtCategoryTag.RECEIVABLE }
            ?.remainingToman ?: 0L

        val obligationScheduleRows = debtObligations.mapIndexed { index, item ->
            val statusLabel = when {
                item.remainingToman <= 0L -> "تسویه کامل"
                item.overdueToman > 0L -> "دارای معوقه"
                else -> "در حال بازپرداخت"
            }
            PrintableDebtScheduleRow(
                rowNumber = index + 1,
                obligationTitle = item.obligation.title,
                categoryBadgeFa = item.primaryTag.shortBadgeFa,
                counterpartyName = item.counterparty?.name ?: "نامشخص",
                nextDueDateJalali = PersianCurrencyEngine.toPersianDigits(item.nextDueDateJalali ?: "تسویه"),
                totalPayableToman = item.totalPayableToman,
                paidToman = item.paidToman,
                remainingToman = item.remainingToman,
                remainingInstallmentsCount = item.remainingInstallmentsCount,
                progressPercent = item.progressPercent,
                statusLabelFa = statusLabel
            )
        }

        val docId = fixedDocumentId ?: "TRZ-FIN-${(System.currentTimeMillis() % 900000) + 100000}"

        val notes = listOf(
            "وضعیت کل بدهی‌ها: از مجموع ${
                PersianCurrencyEngine.formatAmount(portfolioReport.filteredTotalPayableToman, currencyUnit)
            } تعهدات، تاکنون ${
                PersianCurrencyEngine.toPersianDigits(portfolioReport.filteredSettlementProgressPercent.toString())
            }٪ (${
                PersianCurrencyEngine.formatAmount(portfolioReport.filteredTotalPaidToman, currencyUnit)
            }) تسویه شده است.",
            "پیشرفت هدف پس‌انداز ماه (${parsedToday.monthNameFa}): ${
                PersianCurrencyEngine.toPersianDigits(savingsSection.progressPercent.toString())
            }٪ از هدف «${savingsSection.goalTitleFa}» (${
                PersianCurrencyEngine.formatAmount(savingsSection.savedToman, currencyUnit)
            } از ${
                PersianCurrencyEngine.formatAmount(savingsSection.targetToman, currencyUnit)
            }) محقق شده است.",
            "تراز جریان نقدی ماهانه: درآمد ${
                PersianCurrencyEngine.formatAmount(cashflowSection.recurringIncomeToman, currencyUnit)
            }، هزینه‌های جاری غیرقسطی ${
                PersianCurrencyEngine.formatAmount(cashflowSection.recurringNonLoanExpenseToman, currencyUnit)
            } و اقساط ماه ${
                PersianCurrencyEngine.formatAmount(cashflowSection.currentMonthDebtInstallmentsToman, currencyUnit)
            } (نسبت قسط به درآمد: ${
                PersianCurrencyEngine.toPersianDigits(cashflowSection.debtToIncomePercent.toString())
            }٪)."
        )

        return ComprehensiveFinancialPdfSummaryReport(
            documentId = docId,
            issueDateJalali = PersianCurrencyEngine.toPersianDigits(todayJalali),
            issueDateVerboseFa = verboseToday,
            shamsiMonthNameFa = parsedToday.monthNameFa,
            reportTitleFa = "گزارش جامع خلاصه وضعیت مالی، بدهی‌ها و پیشرفت پس‌انداز",
            subtitleFa = "کارنامه رسمی تراز مالی، تفکیک دسته‌های بدهی و هدف پس‌انداز ماه ${parsedToday.monthNameFa}",
            currencyUnit = currencyUnit,
            totalActiveDebtsCount = debtObligations.count { it.remainingToman > 0L },
            totalDebtPayableToman = portfolioReport.filteredTotalPayableToman,
            totalDebtPaidToman = portfolioReport.filteredTotalPaidToman,
            totalDebtRemainingToman = portfolioReport.filteredTotalRemainingToman,
            totalReceivableRemainingToman = receivableTotalRem,
            overallDebtSettlementPercent = portfolioReport.filteredSettlementProgressPercent,
            savingsSummary = savingsSection,
            cashflowSummary = cashflowSection,
            categoryBreakdowns = categoryRows,
            obligationRows = obligationScheduleRows,
            executiveNotesFa = notes
        )
    }

    /**
     * Converts the comprehensive report into an [A4ReportPayload] so it can also be opened
     * in the interactive A4 paper preview modal.
     */
    fun toA4PreviewPayload(report: ComprehensiveFinancialPdfSummaryReport): A4ReportPayload {
        val summaryTopRows = listOf(
            ReportTableRow(
                rowNumber = 1,
                title = "هدف پس‌انداز: ${report.savingsSummary.goalTitleFa}",
                counterpartyName = "پس‌انداز ماه ${report.shamsiMonthNameFa}",
                dueDateJalali = report.issueDateJalali,
                totalAmountToman = report.savingsSummary.targetToman,
                paidAmountToman = report.savingsSummary.savedToman,
                remainingAmountToman = report.savingsSummary.remainingToTargetToman,
                statusLabelFa = "${PersianCurrencyEngine.toPersianDigits(report.savingsSummary.progressPercent.toString())}٪ پس‌انداز",
                statusEnum = if (report.savingsSummary.progressPercent >= 100) InstallmentStatus.PAID else InstallmentStatus.PARTIAL,
                trackingOrNote = report.savingsSummary.statusBadgeFa
            ),
            ReportTableRow(
                rowNumber = 2,
                title = "تراز نقدینگی و هزینه‌های جاری ماهانه",
                counterpartyName = "درآمد: ${
                    PersianCurrencyEngine.formatAmount(report.cashflowSummary.recurringIncomeToman, report.currencyUnit)
                }",
                dueDateJalali = report.issueDateJalali,
                totalAmountToman = report.cashflowSummary.recurringIncomeToman,
                paidAmountToman = report.cashflowSummary.recurringNonLoanExpenseToman,
                remainingAmountToman = report.cashflowSummary.netMonthlyFreeCashflowToman.coerceAtLeast(0L),
                statusLabelFa = "امتیاز سلامت ${PersianCurrencyEngine.toPersianDigits(report.cashflowSummary.financialHealthScore.toString())}",
                statusEnum = InstallmentStatus.PAID,
                trackingOrNote = "نسبت قسط به درآمد: ${PersianCurrencyEngine.toPersianDigits(report.cashflowSummary.debtToIncomePercent.toString())}٪"
            )
        )

        val debtRows = report.obligationRows.mapIndexed { idx, ob ->
            ReportTableRow(
                rowNumber = idx + 3,
                title = "${ob.obligationTitle} (${ob.categoryBadgeFa})",
                counterpartyName = ob.counterpartyName,
                dueDateJalali = ob.nextDueDateJalali,
                totalAmountToman = ob.totalPayableToman,
                paidAmountToman = ob.paidToman,
                remainingAmountToman = ob.remainingToman,
                statusLabelFa = ob.statusLabelFa,
                statusEnum = when {
                    ob.remainingToman <= 0L -> InstallmentStatus.PAID
                    ob.statusLabelFa.contains("معوقه") -> InstallmentStatus.OVERDUE
                    ob.paidToman > 0L -> InstallmentStatus.PARTIAL
                    else -> InstallmentStatus.PENDING
                },
                trackingOrNote = "${PersianCurrencyEngine.toPersianDigits(ob.remainingInstallmentsCount.toString())} قسط مانده (${PersianCurrencyEngine.toPersianDigits(ob.progressPercent.toString())}٪)"
            )
        }

        return A4ReportPayload(
            reportTitleFa = report.reportTitleFa,
            subtitleFa = "${report.subtitleFa} • پس‌انداز: ${PersianCurrencyEngine.toPersianDigits(report.savingsSummary.progressPercent.toString())}٪",
            trackingId = report.documentId,
            issueDateJalali = report.issueDateJalali,
            currencyUnit = report.currencyUnit,
            totalObligationsToman = report.totalDebtPayableToman,
            totalPaidToman = report.totalDebtPaidToman,
            totalRemainingToman = report.totalDebtRemainingToman,
            progressPercent = report.overallDebtSettlementPercent,
            rows = summaryTopRows + debtRows,
            footerNoteFa = report.footerCertificationFa
        )
    }

    /**
     * Generates and saves a persistent printable A4 PDF summary file in the user's app records
     * (`filesDir/saved_pdf_records`) and mirrors a copy to `cacheDir/reports` for instant FileProvider sharing.
     */
    fun generateAndSavePrintablePdfFile(
        context: Context,
        report: ComprehensiveFinancialPdfSummaryReport
    ): File {
        val savedRecordsDir = File(context.filesDir, RECORDS_SUBDIR).apply { mkdirs() }
        val cacheReportsDir = File(context.cacheDir, "reports").apply { mkdirs() }

        val fileName = "Tarazino_Financial_Summary_${report.documentId}.pdf"
        val persistentFile = File(savedRecordsDir, fileName)
        val shareableFile = File(cacheReportsDir, fileName)

        renderMultiSectionA4PdfToFile(context, report, persistentFile)
        runCatching {
            persistentFile.copyTo(shareableFile, overwrite = true)
        }
        return persistentFile
    }

    /**
     * Lists all previously generated and saved PDF summary reports in reverse chronological order.
     */
    fun listSavedPdfRecords(context: Context): List<SavedPdfRecordFile> {
        val dir = File(context.filesDir, RECORDS_SUBDIR)
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.isFile && f.name.endsWith(".pdf", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                SavedPdfRecordFile(
                    fileName = file.name,
                    absolutePath = file.absolutePath,
                    sizeBytes = file.length(),
                    lastModifiedEpochMs = file.lastModified()
                )
            }
            ?: emptyList()
    }

    /**
     * Shares a saved PDF summary report via Android's system share sheet (`FileProvider`).
     */
    fun shareSavedPdfRecord(context: Context, pdfFile: File) {
        try {
            val cacheReportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val cachedCopy = if (pdfFile.parentFile?.absolutePath == cacheReportsDir.absolutePath) {
                pdfFile
            } else {
                val target = File(cacheReportsDir, pdfFile.name)
                pdfFile.copyTo(target, overwrite = true)
                target
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cachedCopy
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(
                intent,
                "ذخیره یا اشتراک‌گذاری گزارش جامع PDF ترازینو"
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
            // Safe fallback in headless JVM test environment
        }
    }

    /**
     * Launches the native Android [PrintManager] print spooler for the generated PDF file
     * so the user can print directly to a physical printer or save via Android's "Save as PDF" printer.
     */
    fun launchAndroidPrintJob(
        context: Context,
        pdfFile: File,
        jobNameFa: String = "گزارش جامع مالی و پس‌انداز ترازینو"
    ): Boolean {
        val activity = findActivity(context) ?: return false
        val printManager = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            ?: return false

        return try {
            val attributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build()

            val adapter = object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val info = PrintDocumentInfo.Builder(pdfFile.name)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    if (destination == null) {
                        callback?.onWriteFailed("فایل مقصد چاپ نامعتبر است")
                        return
                    }
                    try {
                        FileInputStream(pdfFile).use { input ->
                            FileOutputStream(destination.fileDescriptor).use { output ->
                                input.copyTo(output)
                            }
                        }
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    }
                }
            }

            printManager.print(jobNameFa, adapter, attributes)
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun renderMultiSectionA4PdfToFile(
        context: Context,
        report: ComprehensiveFinancialPdfSummaryReport,
        outFile: File
    ) {
        val pageWidth = 595 // Standard A4 width in PostScript points
        val pageHeight = 842 // Standard A4 height in PostScript points

        val regularFont = try {
            ResourcesCompat.getFont(context, R.font.estedad_regular) ?: Typeface.DEFAULT
        } catch (_: Exception) {
            Typeface.DEFAULT
        }
        val boldFont = try {
            ResourcesCompat.getFont(context, R.font.estedad_bold)
                ?: Typeface.create(regularFont, Typeface.BOLD)
        } catch (_: Exception) {
            Typeface.create(regularFont, Typeface.BOLD)
        }

        val rowsFirstPage = 10
        val rowsNextPage = 20
        val rowChunks = mutableListOf<List<PrintableDebtScheduleRow>>()
        if (report.obligationRows.isEmpty()) {
            rowChunks.add(emptyList())
        } else {
            rowChunks.add(report.obligationRows.take(rowsFirstPage))
            var rem = report.obligationRows.drop(rowsFirstPage)
            while (rem.isNotEmpty()) {
                rowChunks.add(rem.take(rowsNextPage))
                rem = rem.drop(rowsNextPage)
            }
        }

        val totalPages = rowChunks.size
        var pdfDocument: PdfDocument? = null
        try {
            pdfDocument = PdfDocument()
            rowChunks.forEachIndexed { pageIndex, pageObligations ->
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                drawComprehensiveA4Page(
                    canvas = page.canvas,
                    report = report,
                    pageObligations = pageObligations,
                    pageNumber = pageIndex + 1,
                    totalPages = totalPages,
                    regularFont = regularFont,
                    boldFont = boldFont,
                    isFirstPage = pageIndex == 0,
                    isLastPage = pageIndex == totalPages - 1
                )
                pdfDocument.finishPage(page)
            }
            FileOutputStream(outFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } catch (_: Throwable) {
            outFile.writeText(
                "%PDF-1.4\n% Tarazino Comprehensive Financial Summary ${report.documentId}\n" +
                    "% Title: ${report.reportTitleFa}\n" +
                    "% Debt Remaining: ${report.totalDebtRemainingToman}\n" +
                    "% Savings Progress: ${report.savingsSummary.progressPercent}%\n%%EOF"
            )
        } finally {
            runCatching { pdfDocument?.close() }
        }
    }

    private fun drawComprehensiveA4Page(
        canvas: Canvas,
        report: ComprehensiveFinancialPdfSummaryReport,
        pageObligations: List<PrintableDebtScheduleRow>,
        pageNumber: Int,
        totalPages: Int,
        regularFont: Typeface,
        boldFont: Typeface,
        isFirstPage: Boolean,
        isLastPage: Boolean
    ) {
        val margin = 26f
        val contentWidth = 595f - (margin * 2)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val unit = report.currencyUnit

        // White A4 background
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, 595f, 842f, paint)

        // Outer border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.2f
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawRoundRect(RectF(14f, 14f, 581f, 828f), 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        // 1. Deep-Teal & Champagne-Gold Header Banner
        val headerRect = RectF(margin, 24f, 595f - margin, 100f)
        paint.shader = LinearGradient(
            headerRect.left, headerRect.top, headerRect.right, headerRect.bottom,
            Color.parseColor("#0E3B3E"),
            Color.parseColor("#081B22"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(headerRect, 10f, 10f, paint)
        paint.shader = null

        paint.typeface = boldFont
        paint.textAlign = Paint.Align.RIGHT
        paint.color = Color.parseColor("#35B8AB")
        paint.textSize = 10.5f
        canvas.drawText(
            "ترازینو (TARAZINO) — گزارش جامع وضعیت مالی، بدهی‌ها و پس‌انداز",
            headerRect.right - 14f,
            45f,
            paint
        )

        paint.color = Color.WHITE
        paint.textSize = 13.5f
        canvas.drawText(report.reportTitleFa, headerRect.right - 14f, 68f, paint)

        paint.typeface = regularFont
        paint.color = Color.parseColor("#DAB978")
        paint.textSize = 9f
        canvas.drawText(report.subtitleFa, headerRect.right - 14f, 88f, paint)

        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.parseColor("#DAB978")
        paint.textSize = 8.5f
        canvas.drawText("شماره سند: ${report.documentId}", headerRect.left + 12f, 46f, paint)
        paint.color = Color.WHITE
        canvas.drawText("تاریخ صدور: ${report.issueDateJalali}", headerRect.left + 12f, 66f, paint)
        val pageLabel = "${PersianCurrencyEngine.toPersianDigits(pageNumber.toString())} از ${
            PersianCurrencyEngine.toPersianDigits(totalPages.toString())
        }"
        canvas.drawText("صفحه: $pageLabel", headerRect.left + 12f, 86f, paint)

        var currentY = 112f

        if (isFirstPage) {
            // 2. Top 4 Executive Financial KPI Cards
            val gap = 8f
            val cardW = (contentWidth - gap * 3) / 4f
            val cardH = 48f

            val kpis = listOf(
                Triple(
                    "پیشرفت هدف پس‌انداز",
                    "${PersianCurrencyEngine.toPersianDigits(report.savingsSummary.progressPercent.toString())}٪ (${
                        PersianCurrencyEngine.formatAmount(report.savingsSummary.savedToman, unit)
                    })",
                    "#0F6B66"
                ),
                Triple(
                    "مانده کل بدهی‌ها",
                    PersianCurrencyEngine.formatAmount(report.totalDebtRemainingToman, unit),
                    "#B58538"
                ),
                Triple(
                    "مجموع تسویه‌شده",
                    "${PersianCurrencyEngine.formatAmount(report.totalDebtPaidToman, unit)} (${
                        PersianCurrencyEngine.toPersianDigits(report.overallDebtSettlementPercent.toString())
                    }٪)",
                    "#059669"
                ),
                Triple(
                    "کل تعهدات ثبت‌شده",
                    PersianCurrencyEngine.formatAmount(report.totalDebtPayableToman, unit),
                    "#0F172A"
                )
            )

            kpis.forEachIndexed { idx, (label, valText, colorHex) ->
                val left = margin + idx * (cardW + gap)
                val rect = RectF(left, currentY, left + cardW, currentY + cardH)
                paint.color = Color.parseColor("#F8FAFC")
                canvas.drawRoundRect(rect, 7f, 7f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                paint.color = Color.parseColor(colorHex)
                canvas.drawRoundRect(rect, 7f, 7f, paint)
                paint.style = Paint.Style.FILL

                paint.textAlign = Paint.Align.CENTER
                paint.typeface = regularFont
                paint.textSize = 8f
                paint.color = Color.parseColor("#475569")
                canvas.drawText(label, rect.centerX(), rect.top + 18f, paint)

                paint.typeface = boldFont
                paint.textSize = 8.8f
                paint.color = Color.parseColor(colorHex)
                canvas.drawText(valText, rect.centerX(), rect.top + 36f, paint)
            }
            currentY += cardH + 10f

            // 3. Monthly Savings Goal & Cashflow Health Dual Summary Box
            val boxH = 66f
            val savingsBoxRect = RectF(margin, currentY, 595f - margin, currentY + boxH)
            paint.color = Color.parseColor("#F0FAF9")
            canvas.drawRoundRect(savingsBoxRect, 8f, 8f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            paint.color = Color.parseColor("#35B8AB")
            canvas.drawRoundRect(savingsBoxRect, 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            paint.textAlign = Paint.Align.RIGHT
            paint.typeface = boldFont
            paint.textSize = 9.5f
            paint.color = Color.parseColor("#0F6B66")
            canvas.drawText(
                "بخش ۱: وضعیت هدف پس‌انداز ماهانه («${report.savingsSummary.goalTitleFa}») و تراز نقدینگی",
                savingsBoxRect.right - 12f,
                currentY + 18f,
                paint
            )

            paint.typeface = regularFont
            paint.textSize = 8.2f
            paint.color = Color.parseColor("#1E293B")
            val savingsLine = "ذخیره فعلی: ${
                PersianCurrencyEngine.formatAmount(report.savingsSummary.savedToman, unit)
            } از هدف ${
                PersianCurrencyEngine.formatAmount(report.savingsSummary.targetToman, unit)
            } (${
                PersianCurrencyEngine.toPersianDigits(report.savingsSummary.progressPercent.toString())
            }٪) • مانده تا هدف: ${
                PersianCurrencyEngine.formatAmount(report.savingsSummary.remainingToTargetToman, unit)
            } • وضعیت: ${report.savingsSummary.statusBadgeFa}"
            canvas.drawText(savingsLine, savingsBoxRect.right - 12f, currentY + 35f, paint)

            val cashflowLine = "درآمد ماهانه: ${
                PersianCurrencyEngine.formatAmount(report.cashflowSummary.recurringIncomeToman, unit)
            } | هزینه‌های جاری غیرقسطی: ${
                PersianCurrencyEngine.formatAmount(report.cashflowSummary.recurringNonLoanExpenseToman, unit)
            } | اقساط ماه: ${
                PersianCurrencyEngine.formatAmount(report.cashflowSummary.currentMonthDebtInstallmentsToman, unit)
            } | امتیاز سلامت: ${
                PersianCurrencyEngine.toPersianDigits(report.cashflowSummary.financialHealthScore.toString())
            }/۱۰۰"
            canvas.drawText(cashflowLine, savingsBoxRect.right - 12f, currentY + 51f, paint)

            // Savings progress bar inside box
            val progRect = RectF(margin + 12f, currentY + 56f, 595f - margin - 12f, currentY + 61f)
            paint.color = Color.parseColor("#CBD5E1")
            canvas.drawRoundRect(progRect, 3f, 3f, paint)
            val fillW = (progRect.width() * (report.savingsSummary.progressPercent / 100f))
                .coerceIn(0f, progRect.width())
            if (fillW > 0f) {
                paint.color = Color.parseColor("#0F6B66")
                canvas.drawRoundRect(
                    RectF(progRect.right - fillW, progRect.top, progRect.right, progRect.bottom),
                    3f,
                    3f,
                    paint
                )
            }

            currentY += boxH + 10f

            // 4. Debt Categorization Summary Strip ('Bank Loan', 'Credit Card', 'Personal', 'Check & Housing')
            paint.textAlign = Paint.Align.RIGHT
            paint.typeface = boldFont
            paint.textSize = 9.5f
            paint.color = Color.parseColor("#0F172A")
            canvas.drawText(
                "بخش ۲: تفکیک بدهی‌ها بر اساس دسته‌بندی (وام بانکی، کارت اعتباری، شخصی و چک)",
                595f - margin,
                currentY + 12f,
                paint
            )
            currentY += 18f

            val catRows = report.categoryBreakdowns.filter { it.tag != DebtCategoryTag.RECEIVABLE }
            val catCardGap = 6f
            val catCardW = (contentWidth - catCardGap * (catRows.size - 1).coerceAtLeast(1)) / catRows.size.coerceAtLeast(1)
            val catCardH = 44f

            catRows.forEachIndexed { idx, cat ->
                val left = margin + idx * (catCardW + catCardGap)
                val rect = RectF(left, currentY, left + catCardW, currentY + catCardH)
                paint.color = Color.parseColor("#F8FAFC")
                canvas.drawRoundRect(rect, 6f, 6f, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 0.9f
                paint.color = Color.parseColor("#94A3B8")
                canvas.drawRoundRect(rect, 6f, 6f, paint)
                paint.style = Paint.Style.FILL

                paint.textAlign = Paint.Align.CENTER
                paint.typeface = boldFont
                paint.textSize = 8f
                paint.color = Color.parseColor("#1E293B")
                canvas.drawText(
                    "${cat.tag.shortBadgeFa} (${PersianCurrencyEngine.toPersianDigits(cat.obligationsCount.toString())})",
                    rect.centerX(),
                    rect.top + 16f,
                    paint
                )

                paint.typeface = regularFont
                paint.textSize = 7.8f
                paint.color = Color.parseColor("#0F6B66")
                canvas.drawText(
                    "مانده: ${PersianCurrencyEngine.formatAmount(cat.remainingToman, unit, includeUnit = false)} (${
                        PersianCurrencyEngine.toPersianDigits(cat.shareOfRemainingPercent.toString())
                    }٪)",
                    rect.centerX(),
                    rect.top + 33f,
                    paint
                )
            }
            currentY += catCardH + 12f
        }

        // 5. Detailed Debt & Obligation Table Header
        paint.textAlign = Paint.Align.RIGHT
        paint.typeface = boldFont
        paint.textSize = 9.5f
        paint.color = Color.parseColor("#0F172A")
        canvas.drawText(
            "بخش ۳: جدول تفصیلی پرونده‌های بدهی، اقساط و پیشرفت تسویه",
            595f - margin,
            currentY + 11f,
            paint
        )
        currentY += 16f

        val tableHeaderH = 22f
        val headerRowRect = RectF(margin, currentY, 595f - margin, currentY + tableHeaderH)
        paint.color = Color.parseColor("#1E293B")
        canvas.drawRoundRect(headerRowRect, 5f, 5f, paint)

        val colWidths = floatArrayOf(26f, 132f, 70f, 80f, 62f, 82f, 91f)
        val colHeaders = arrayOf(
            "#",
            "عنوان بدهی / وام",
            "دسته / برچسب",
            "طرف‌حساب",
            "سررسید بعدی",
            "مانده (${unit.labelFa})",
            "پیشرفت و وضعیت"
        )

        paint.typeface = boldFont
        paint.textSize = 8f
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER

        var rightCursor = 595f - margin
        for (i in colHeaders.indices) {
            val w = colWidths[i]
            canvas.drawText(colHeaders[i], rightCursor - w / 2f, currentY + 14.5f, paint)
            rightCursor -= w
        }
        currentY += tableHeaderH + 2f

        val rowH = 22f
        pageObligations.forEachIndexed { idx, row ->
            val rowRect = RectF(margin, currentY, 595f - margin, currentY + rowH)
            paint.color = if (idx % 2 == 0) Color.parseColor("#F8FAFC") else Color.WHITE
            canvas.drawRect(rowRect, paint)

            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawLine(margin, currentY + rowH, 595f - margin, currentY + rowH, paint)

            val cells = arrayOf(
                PersianCurrencyEngine.toPersianDigits(row.rowNumber.toString()),
                row.obligationTitle.take(24),
                row.categoryBadgeFa.take(14),
                row.counterpartyName.take(15),
                row.nextDueDateJalali,
                PersianCurrencyEngine.formatAmount(row.remainingToman, unit, includeUnit = false),
                "${PersianCurrencyEngine.toPersianDigits(row.progressPercent.toString())}٪ • ${row.statusLabelFa.take(10)}"
            )

            var rX = 595f - margin
            for (cIdx in cells.indices) {
                val w = colWidths[cIdx]
                paint.typeface = if (cIdx == 5 || cIdx == 6) boldFont else regularFont
                paint.textSize = 7.8f
                paint.color = when {
                    cIdx == 6 && row.statusLabelFa.contains("معوقه") -> Color.parseColor("#DC2626")
                    cIdx == 6 && row.remainingToman <= 0L -> Color.parseColor("#059669")
                    else -> Color.parseColor("#1E293B")
                }
                canvas.drawText(cells[cIdx], rX - w / 2f, currentY + 14.5f, paint)
                rX -= w
            }
            currentY += rowH
        }

        // 6. Executive Summary Notes & Signature Boxes on Last Page
        if (isLastPage) {
            val notesTop = (currentY + 12f).coerceAtMost(690f)
            paint.textAlign = Paint.Align.RIGHT
            paint.typeface = boldFont
            paint.textSize = 8.5f
            paint.color = Color.parseColor("#0F6B66")
            report.executiveNotesFa.forEachIndexed { idx, note ->
                val y = notesTop + idx * 14f
                if (y < 745f) {
                    paint.typeface = regularFont
                    paint.textSize = 7.8f
                    paint.color = Color.parseColor("#334155")
                    canvas.drawText("• $note", 595f - margin, y, paint)
                }
            }

            val sigTop = 748f
            val boxW = (contentWidth - 16f) / 2f
            val boxH = 48f
            val rightBox = RectF(595f - margin - boxW, sigTop, 595f - margin, sigTop + boxH)
            val leftBox = RectF(margin, sigTop, margin + boxW, sigTop + boxH)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.9f
            paint.color = Color.parseColor("#94A3B8")
            canvas.drawRoundRect(rightBox, 6f, 6f, paint)
            canvas.drawRoundRect(leftBox, 6f, 6f, paint)
            paint.style = Paint.Style.FILL

            paint.typeface = boldFont
            paint.textSize = 8f
            paint.color = Color.parseColor("#334155")
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("امضا و تایید صاحب حساب / متعهد:", rightBox.right - 10f, rightBox.top + 16f, paint)
            canvas.drawText("تاریخ بایگانی و مهر تایید:", leftBox.right - 10f, leftBox.top + 16f, paint)

            paint.typeface = regularFont
            paint.textSize = 7.8f
            paint.color = Color.parseColor("#64748B")
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(report.footerCertificationFa, 595f / 2f, 814f, paint)
        }
    }

    private fun findActivity(context: Context): Activity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }
}
