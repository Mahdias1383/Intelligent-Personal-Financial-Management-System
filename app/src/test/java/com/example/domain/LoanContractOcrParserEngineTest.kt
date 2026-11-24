package com.example.domain

import com.example.data.EntityCategory
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoanContractOcrParserEngineTest {

    @Test
    fun parsePersianBankLoanContract_convertsRialExtractsInterestDateAndCount() {
        val ocrText = """
            قرارداد اعطای تسهیلات مرابحه - بانک ملت
            شماره قرارداد: MLT-1405-8821
            مبلغ کل تسهیلات: 1,200,000,000 ریال
            نرخ سود سالانه: ۲۳٪
            تعداد اقساط: ۲۴ ماهه
            مبلغ هر قسط: 62,850,000 ریال
            تاریخ اولین سررسید: ۱۴۰۵/۰۹/۱۵
        """.trimIndent()

        val result = LoanContractOcrParserEngine.parseRecognizedText(ocrText, todayJalali = "1405/08/25")

        assertEquals(ScannedDocumentType.LOAN_CONTRACT, result.detectedDocumentType)
        assertEquals(EntityCategory.BANK, result.suggestedCategory)
        assertEquals("بانک ملت", result.detectedCounterpartyName)
        assertTrue(result.detectedRialUnit)
        assertEquals(120_000_000L, result.principalAmountToman)
        assertEquals(6_285_000L, result.installmentAmountToman)
        assertEquals(24, result.installmentCount)
        assertEquals(23.0, result.annualInterestRatePercent ?: 0.0, 0.01)
        assertEquals("1405/09/15", result.firstDueDateJalali)
        assertEquals("MLT-1405-8821", result.trackingCode)
        // 6,285,000 * 24 = 150,840,000 - 120,000,000 = 30,840,000 Toman total profit
        assertEquals(30_840_000L, result.computedProfitOrFeeToman)
        assertTrue(result.confidenceScore >= 85)
        assertFalse(result.isLikelySinglePaymentReceipt)
    }

    @Test
    fun parseEnglishLoanContract_convertsGregorianDateAndCalculatesAmortizedInterest() {
        val ocrText = """
            AUTO LOAN AGREEMENT
            Lender: Meridian Credit Bank
            Contract #: LN-90452
            Loan Amount: 50,000,000
            APR: 18.5%
            Loan Term: 12 months
            First Payment Due Date: 2026-12-15
        """.trimIndent()

        val result = LoanContractOcrParserEngine.parseRecognizedText(ocrText, todayJalali = "1405/08/25")

        assertEquals(ScannedDocumentType.LOAN_CONTRACT, result.detectedDocumentType)
        assertEquals("Meridian Credit Bank", result.detectedCounterpartyName)
        assertEquals(50_000_000L, result.principalAmountToman)
        assertEquals(18.5, result.annualInterestRatePercent ?: 0.0, 0.01)
        assertEquals(12, result.installmentCount)
        assertEquals("LN-90452", result.trackingCode)
        // 2026-12-15 Gregorian -> 1405/09/24 Jalali
        assertEquals("1405/09/24", result.firstDueDateJalali)
        assertNotNull(result.installmentAmountToman)
        assertNotNull(result.computedProfitOrFeeToman)
        assertTrue((result.computedProfitOrFeeToman ?: 0L) > 4_000_000L)
        assertTrue(result.extractionNotesFa.any { it.contains("میلادی") })
    }

    @Test
    fun parsePaymentReceipt_detectsReceiptAndMatchesUnpaidInstallment() {
        val ocrText = """
            رسید پرداخت موفق قسط بانک مهر ایران
            مبلغ واریزی: 4,500,000 تومان
            تاریخ پرداخت: 1405/09/01
            شماره پیگیری: 78451209
        """.trimIndent()

        val extraction = LoanContractOcrParserEngine.parseRecognizedText(ocrText, todayJalali = "1405/09/01")

        assertEquals(ScannedDocumentType.PAYMENT_RECEIPT, extraction.detectedDocumentType)
        assertTrue(extraction.isLikelySinglePaymentReceipt)
        assertEquals(4_500_000L, extraction.installmentAmountToman)
        assertEquals("1405/09/01", extraction.firstDueDateJalali)
        assertEquals("78451209", extraction.trackingCode)

        val installments = listOf(
            InstallmentEntity(
                id = 1L,
                obligationId = 10L,
                installmentNumber = 1,
                title = "قسط اسنپ‌پی",
                amountToman = 1_800_000L,
                dueDateJalali = "1405/09/10",
                status = InstallmentStatus.PENDING.name
            ),
            InstallmentEntity(
                id = 2L,
                obligationId = 11L,
                installmentNumber = 4,
                title = "قسط وام بانک مهر ایران",
                amountToman = 4_500_000L,
                dueDateJalali = "1405/09/01",
                status = InstallmentStatus.PENDING.name
            )
        )

        val matched = LoanContractOcrParserEngine.matchBestUnpaidInstallment(extraction, installments)
        assertNotNull(matched)
        assertEquals(2L, matched?.id)
    }

    @Test
    fun parseBnplInvoice_extractsPersianShorthandMillionsAndCategory() {
        val ocrText = """
            فاکتور خرید اقساطی اسنپ‌پی
            مبلغ کل: 12 میلیون تومان
            تعداد قسط: 4 قسط
            مبلغ هر قسط: 3,000,000 تومان
            سررسید اول: 1405/09/05
        """.trimIndent()

        val result = LoanContractOcrParserEngine.parseRecognizedText(ocrText, todayJalali = "1405/08/25")

        assertEquals(ScannedDocumentType.BNPL_INVOICE, result.detectedDocumentType)
        assertEquals(EntityCategory.BNPL_PLATFORM, result.suggestedCategory)
        assertEquals("اسنپ‌پی", result.detectedCounterpartyName)
        assertEquals(12_000_000L, result.principalAmountToman)
        assertEquals(3_000_000L, result.installmentAmountToman)
        assertEquals(4, result.installmentCount)
        assertEquals(0L, result.computedProfitOrFeeToman)
        assertEquals("1405/09/05", result.firstDueDateJalali)
    }

    @Test
    fun computeAmortizedTotalInterest_matchesExpectedFormula() {
        // 100,000,000 Toman at 24% APR for 12 months => monthly payment ~9,455,960 => total ~113,471,516 => interest ~13,471,516
        val interest = LoanContractOcrParserEngine.computeAmortizedTotalInterest(
            principalToman = 100_000_000L,
            annualInterestPercent = 24.0,
            installmentCount = 12
        )
        assertTrue("Expected ~13.47M interest, got $interest", interest in 13_400_000L..13_550_000L)
    }
}
