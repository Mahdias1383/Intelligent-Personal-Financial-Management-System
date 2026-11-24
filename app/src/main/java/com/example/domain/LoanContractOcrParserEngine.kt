package com.example.domain

import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.data.EntityCategory
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

enum class ScannedDocumentType(val labelFa: String) {
    LOAN_CONTRACT("قرارداد وام / تسهیلات"),
    PAYMENT_RECEIPT("رسید پرداخت قسط"),
    BNPL_INVOICE("فاکتور خرید اقساطی"),
    CHECK_OR_PROMISSORY("چک صیادی / سفته"),
    UNKNOWN("سند مالی اسکن‌شده")
}

data class ScannedContractExtraction(
    val rawRecognizedText: String,
    val detectedDocumentType: ScannedDocumentType,
    val suggestedTitle: String,
    val suggestedCategory: EntityCategory,
    val detectedCounterpartyName: String?,
    val principalAmountToman: Long?,
    val installmentAmountToman: Long?,
    val installmentCount: Int?,
    val annualInterestRatePercent: Double?,
    val computedProfitOrFeeToman: Long?,
    val firstDueDateJalali: String?,
    val allDetectedDatesJalali: List<String>,
    val trackingCode: String?,
    val detectedRialUnit: Boolean,
    val isLikelySinglePaymentReceipt: Boolean,
    val confidenceScore: Int,
    val extractionNotesFa: List<String>
) {
    val hasAnyExtractedField: Boolean
        get() = principalAmountToman != null ||
            installmentAmountToman != null ||
            firstDueDateJalali != null ||
            annualInterestRatePercent != null ||
            installmentCount != null
}

/**
 * Pure bilingual (Persian + English) OCR text extraction engine for paper loan contracts,
 * installment schedules, BNPL invoices, checks, and bank payment receipts.
 *
 * Converts Persian/Arabic digits, detects Rial vs Toman units, converts Gregorian dates to
 * canonical Jalali ("YYYY/MM/DD"), parses annual interest rates (APR / نرخ سود), and computes
 * amortized installment totals so Tarazino's debt management system can be populated automatically.
 */
object LoanContractOcrParserEngine {

    private data class KnownProviderPattern(
        val canonicalNameFa: String,
        val category: EntityCategory,
        val keywords: List<String>
    )

    private val KNOWN_PROVIDERS = listOf(
        KnownProviderPattern("بانک ملت", EntityCategory.BANK, listOf("بانک ملت", "mellat", "bank mellat")),
        KnownProviderPattern("بانک ملی ایران", EntityCategory.BANK, listOf("بانک ملی", "melli", "bmi")),
        KnownProviderPattern("بانک مهر ایران", EntityCategory.BANK, listOf("مهر ایران", "بانک مهر", "qmb", "mehr iran")),
        KnownProviderPattern("بانک رسالت", EntityCategory.BANK, listOf("بانک رسالت", "رسالت", "resalat")),
        KnownProviderPattern("بانک سامان", EntityCategory.BANK, listOf("بانک سامان", "بلوبانک", "بلو بانک", "saman", "blubank", "blu bank")),
        KnownProviderPattern("بانک پاسارگاد", EntityCategory.BANK, listOf("بانک پاسارگاد", "پاسارگاد", "ویپاد", "pasargad", "wepod")),
        KnownProviderPattern("بانک تجارت", EntityCategory.BANK, listOf("بانک تجارت", "تجارت", "tejarat")),
        KnownProviderPattern("بانک صادرات", EntityCategory.BANK, listOf("بانک صادرات", "صادرات", "saderat")),
        KnownProviderPattern("بانک سپه", EntityCategory.BANK, listOf("بانک سپه", "سپه", "sepah")),
        KnownProviderPattern("بانک رفاه", EntityCategory.BANK, listOf("بانک رفاه", "رفاه کارگران", "refah")),
        KnownProviderPattern("بانک کشاورزی", EntityCategory.BANK, listOf("بانک کشاورزی", "کشاورزی", "keshavarzi")),
        KnownProviderPattern("بانک مسکن", EntityCategory.BANK, listOf("بانک مسکن", "مسکن", "maskan")),
        KnownProviderPattern("بانک شهر", EntityCategory.BANK, listOf("بانک شهر", "shahr")),
        KnownProviderPattern("بانک آینده", EntityCategory.BANK, listOf("بانک آینده", "آینده", "ayandeh")),
        KnownProviderPattern("اسنپ‌پی", EntityCategory.BNPL_PLATFORM, listOf("اسنپ‌پی", "اسنپ پی", "snapppay", "snapp pay")),
        KnownProviderPattern("دیجی‌پی", EntityCategory.BNPL_PLATFORM, listOf("دیجی‌پی", "دیجی پی", "digipay", "digi pay")),
        KnownProviderPattern("ازکی‌وام", EntityCategory.BNPL_PLATFORM, listOf("ازکی‌وام", "ازکی وام", "azkivam", "azki vam")),
        KnownProviderPattern("تارا", EntityCategory.BNPL_PLATFORM, listOf("تارا", "tara")),
        KnownProviderPattern("لندو", EntityCategory.BNPL_PLATFORM, listOf("لندو", "lendo")),
        KnownProviderPattern("صندوق قرض‌الحسنه", EntityCategory.INSTITUTION, listOf("صندوق", "قرض‌الحسنه", "قرض الحسنه", "تعاونی", "credit union"))
    )

    private val ENGLISH_MONTHS = mapOf(
        "jan" to 1, "january" to 1,
        "feb" to 2, "february" to 2,
        "mar" to 3, "march" to 3,
        "apr" to 4, "april" to 4,
        "may" to 5,
        "jun" to 6, "june" to 6,
        "jul" to 7, "july" to 7,
        "aug" to 8, "august" to 8,
        "sep" to 9, "sept" to 9, "september" to 9,
        "oct" to 10, "october" to 10,
        "nov" to 11, "november" to 11,
        "dec" to 12, "december" to 12
    )

    fun parseRecognizedText(
        rawText: String,
        todayJalali: String = JalaliCalendarEngine.todayJalali()
    ): ScannedContractExtraction {
        val cleaned = normalizeTextForParsing(rawText)
        val lower = cleaned.lowercase(Locale.US)
        val lines = cleaned.lines().map { it.trim() }.filter { it.isNotBlank() }
        val notes = mutableListOf<String>()

        // 1. Detect Document Currency Unit (Rial vs Toman)
        val hasRialKeyword = Regex("(ریال|ریالی|\\brial\\b|\\birr\\b)").containsMatchIn(lower)
        val hasTomanKeyword = Regex("(تومان|تومانی|\\btoman\\b|\\birt\\b)").containsMatchIn(lower)
        val defaultDocIsRial = hasRialKeyword && !hasTomanKeyword

        // 2. Detect Counterparty / Lender & Category
        val matchedProvider = KNOWN_PROVIDERS.firstOrNull { provider ->
            provider.keywords.any { kw -> lower.contains(kw) }
        }
        val customEnglishLender = if (matchedProvider == null) {
            Regex("(?:lender|bank|creditor|institution|payee|وام‌دهنده|بانک|فروشگاه)\\s*[:：\\-]\\s*([^\\n\\r]{2,42})", RegexOption.IGNORE_CASE)
                .find(cleaned)
                ?.groupValues?.getOrNull(1)
                ?.trim()
                ?.takeIf { it.length >= 2 }
        } else null

        val detectedCounterparty = matchedProvider?.canonicalNameFa ?: customEnglishLender

        // 3. Detect Document Type
        val docType = classifyDocumentType(lower, matchedProvider?.category)
        val suggestedCategory = matchedProvider?.category ?: when (docType) {
            ScannedDocumentType.BNPL_INVOICE -> EntityCategory.BNPL_PLATFORM
            ScannedDocumentType.CHECK_OR_PROMISSORY -> EntityCategory.PERSON
            ScannedDocumentType.LOAN_CONTRACT, ScannedDocumentType.PAYMENT_RECEIPT -> EntityCategory.BANK
            ScannedDocumentType.UNKNOWN -> EntityCategory.BANK
        }

        // 4. Extract Interest Rate (% / APR / نرخ سود)
        val interestRate = extractAnnualInterestRate(cleaned, lines)
        if (interestRate != null) {
            notes.add("نرخ سود ${formatPercentFa(interestRate)}٪ از متن سند استخراج شد.")
        }

        // 5. Extract Installment Count (تعداد اقساط / ماهه / Term)
        var installmentCount = extractInstallmentCount(cleaned, lines)
        if (installmentCount != null) {
            notes.add("تعداد ${PersianCurrencyEngine.toPersianDigits(installmentCount.toString())} قسط شناسایی شد.")
        }

        // 6. Extract Principal & Installment Monetary Amounts
        val (rawPrincipal, rawInstallment, convertedFromRial) = extractAmountsToman(
            lines = lines,
            defaultDocIsRial = defaultDocIsRial,
            docType = docType
        )
        var principalToman = rawPrincipal
        var installmentToman = rawInstallment

        if (convertedFromRial) {
            notes.add("مبالغ ریالی سند به صورت خودکار به تومان (تقسیم بر ۱۰) تبدیل شدند.")
        }

        // 7. Reconcile Principal, Installment, Count & Interest Math
        if (installmentCount == null && principalToman != null && installmentToman != null &&
            principalToman > installmentToman && installmentToman > 0L
        ) {
            val estCount = estimateInstallmentCount(principalToman, installmentToman, interestRate)
            if (estCount in 2..120) {
                installmentCount = estCount
                notes.add("تعداد اقساط (${PersianCurrencyEngine.toPersianDigits(estCount.toString())} قسط) بر اساس نسبت مبلغ کل به هر قسط تخمین زده شد.")
            }
        }

        var computedProfitOrFee: Long? = null
        if (principalToman != null && installmentToman != null && installmentCount != null && installmentCount > 1) {
            val totalScheduled = installmentToman * installmentCount
            if (totalScheduled >= principalToman) {
                computedProfitOrFee = totalScheduled - principalToman
            }
        } else if (principalToman != null && interestRate != null && interestRate > 0.0 && installmentCount != null && installmentCount > 0) {
            val totalInterest = computeAmortizedTotalInterest(principalToman, interestRate, installmentCount)
            computedProfitOrFee = totalInterest
            if (installmentToman == null) {
                installmentToman = ((principalToman + totalInterest).toDouble() / installmentCount).roundToLong()
                notes.add("مبلغ هر قسط و سود کل بر اساس نرخ ${formatPercentFa(interestRate)}٪ محاسبه شد.")
            }
        } else if (principalToman == null && installmentToman != null && installmentCount != null && installmentCount > 1) {
            if (interestRate != null && interestRate > 0.0) {
                val inferredPrincipal = computePrincipalFromInstallment(installmentToman, interestRate, installmentCount)
                principalToman = inferredPrincipal
                computedProfitOrFee = (installmentToman * installmentCount - inferredPrincipal).coerceAtLeast(0L)
                notes.add("اصل وام و سود کل از روی مبلغ قسط و نرخ سود محاسبه شد.")
            } else {
                principalToman = installmentToman * installmentCount
            }
        } else if (principalToman == null && installmentToman != null) {
            principalToman = installmentToman
        } else if (principalToman != null && installmentToman == null && installmentCount != null && installmentCount > 0) {
            installmentToman = (principalToman / installmentCount).coerceAtLeast(1L)
        }

        // 8. Extract Dates (Jalali + Gregorian auto-converted to Jalali)
        val (firstDueDateJalali, allDatesJalali, convertedGregorian) = extractDatesJalali(lines, todayJalali)
        if (convertedGregorian) {
            notes.add("تاریخ میلادی سند با تقویم نجومی به تاریخ شمسی تبدیل شد.")
        }

        // 9. Extract Tracking / Contract Code
        val trackingCode = extractTrackingCode(cleaned)

        // 10. Build Clean Suggested Title
        val suggestedTitle = buildSuggestedTitle(
            docType = docType,
            counterparty = detectedCounterparty,
            installmentCount = installmentCount,
            lowerText = lower
        )

        val hasStandaloneReceiptWord = hasPersianStandaloneToken(lower, "رسید")
        val isLikelyReceipt = docType == ScannedDocumentType.PAYMENT_RECEIPT ||
            (installmentCount == null && installmentToman != null &&
                (hasStandaloneReceiptWord || lower.contains("receipt") || lower.contains("پرداخت موفق") || lower.contains("paid")))

        // 11. Confidence Score (0..100)
        var confidence = 0
        if (installmentToman != null || principalToman != null) confidence += 35
        if (principalToman != null && installmentToman != null && principalToman != installmentToman) confidence += 10
        if (firstDueDateJalali != null) confidence += 25
        if (interestRate != null) confidence += 15
        if (installmentCount != null) confidence += 10
        if (detectedCounterparty != null || trackingCode != null) confidence += 5
        confidence = confidence.coerceIn(0, 100)

        return ScannedContractExtraction(
            rawRecognizedText = rawText.trim(),
            detectedDocumentType = docType,
            suggestedTitle = suggestedTitle,
            suggestedCategory = suggestedCategory,
            detectedCounterpartyName = detectedCounterparty,
            principalAmountToman = principalToman,
            installmentAmountToman = installmentToman,
            installmentCount = installmentCount,
            annualInterestRatePercent = interestRate,
            computedProfitOrFeeToman = computedProfitOrFee,
            firstDueDateJalali = firstDueDateJalali,
            allDetectedDatesJalali = allDatesJalali,
            trackingCode = trackingCode,
            detectedRialUnit = convertedFromRial,
            isLikelySinglePaymentReceipt = isLikelyReceipt,
            confidenceScore = confidence,
            extractionNotesFa = notes
        )
    }

    /**
     * Standard monthly amortizing loan total interest:
     * M = P * r * (1+r)^n / ((1+r)^n - 1), Total Interest = round(M) * n - P
     */
    fun computeAmortizedTotalInterest(
        principalToman: Long,
        annualInterestPercent: Double,
        installmentCount: Int
    ): Long {
        if (principalToman <= 0L || annualInterestPercent <= 0.0 || installmentCount <= 0) return 0L
        val monthlyRate = (annualInterestPercent / 100.0) / 12.0
        val factor = (1.0 + monthlyRate).pow(installmentCount)
        if (factor <= 1.0) return 0L
        val monthlyPayment = principalToman.toDouble() * monthlyRate * factor / (factor - 1.0)
        val totalRepayment = (monthlyPayment * installmentCount).roundToLong()
        return (totalRepayment - principalToman).coerceAtLeast(0L)
    }

    /**
     * Inverse amortization formula to infer loan principal from fixed monthly installment, APR, and term.
     */
    fun computePrincipalFromInstallment(
        installmentToman: Long,
        annualInterestPercent: Double,
        installmentCount: Int
    ): Long {
        if (installmentToman <= 0L || installmentCount <= 0) return 0L
        if (annualInterestPercent <= 0.0) return installmentToman * installmentCount
        val monthlyRate = (annualInterestPercent / 100.0) / 12.0
        val factor = (1.0 + monthlyRate).pow(installmentCount)
        val pv = installmentToman.toDouble() * (factor - 1.0) / (monthlyRate * factor)
        return pv.roundToLong().coerceAtLeast(1L)
    }

    /**
     * Finds the best matching unpaid installment when a user scans a payment receipt.
     */
    fun matchBestUnpaidInstallment(
        extraction: ScannedContractExtraction,
        installments: List<InstallmentEntity>
    ): InstallmentEntity? {
        val unpaid = installments.filter { it.statusEnum != InstallmentStatus.PAID && it.remainingToman > 0L }
        if (unpaid.isEmpty()) return null

        val targetAmount = extraction.installmentAmountToman ?: extraction.principalAmountToman
        val targetDate = extraction.firstDueDateJalali
        val targetCounterparty = extraction.detectedCounterpartyName?.lowercase(Locale.US)

        var best: InstallmentEntity? = null
        var bestScore = 0

        for (inst in unpaid) {
            var score = 0
            if (targetAmount != null && targetAmount > 0L) {
                val diff = abs(inst.remainingToman - targetAmount)
                val ratio = diff.toDouble() / inst.remainingToman.coerceAtLeast(1L).toDouble()
                when {
                    diff == 0L -> score += 60
                    ratio <= 0.02 -> score += 45
                    ratio <= 0.10 -> score += 25
                }
            }
            if (targetDate != null) {
                val dayDiff = abs(JalaliCalendarEngine.diffJalaliDays(inst.dueDateJalali, targetDate))
                when {
                    dayDiff == 0 -> score += 30
                    dayDiff <= 7 -> score += 18
                    dayDiff <= 20 -> score += 8
                }
            }
            if (targetCounterparty != null && inst.title.lowercase(Locale.US).contains(targetCounterparty)) {
                score += 15
            }
            if (score > bestScore) {
                bestScore = score
                best = inst
            }
        }
        return if (bestScore >= 25) best else unpaid.minByOrNull { it.dueDateJalali }
    }

    private fun normalizeTextForParsing(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            val mapped = when (ch) {
                '۰', '٠' -> '0'
                '۱', '١' -> '1'
                '۲', '٢' -> '2'
                '۳', '٣' -> '3'
                '۴', '٤' -> '4'
                '۵', '٥' -> '5'
                '۶', '٦' -> '6'
                '۷', '٧' -> '7'
                '۸', '٨' -> '8'
                '۹', '٩' -> '9'
                '٬', '،' -> ','
                '٫' -> '.'
                '٪' -> '%'
                'ك' -> 'ک'
                'ي' -> 'ی'
                '\u200C' -> ' ' // ZWNJ to space for easier regex token matching
                else -> ch
            }
            sb.append(mapped)
        }
        return sb.toString()
    }

    private fun hasPersianStandaloneToken(text: String, token: String): Boolean {
        return Regex("(?<![\\u0600-\\u06FF])${Regex.escape(token)}(?![\\u0600-\\u06FF])")
            .containsMatchIn(text)
    }

    private fun classifyDocumentType(lower: String, providerCategory: EntityCategory?): ScannedDocumentType {
        val hasCheckWord = hasPersianStandaloneToken(lower, "چک") ||
            lower.contains("صیادی") || lower.contains("سفته") || Regex("\\bcheck\\b").containsMatchIn(lower)
        val hasReceiptWord = hasPersianStandaloneToken(lower, "رسید") ||
            lower.contains("تراکنش موفق") || lower.contains("پرداخت موفق") ||
            lower.contains("receipt") || lower.contains("payment confirmation") || lower.contains("paid successfully")
        val hasContractOrLoanWord = lower.contains("قرارداد") || lower.contains("تسهیلات") ||
            hasPersianStandaloneToken(lower, "وام") || lower.contains("loan") || lower.contains("mortgage")

        return when {
            hasCheckWord -> ScannedDocumentType.CHECK_OR_PROMISSORY
            hasReceiptWord && !hasContractOrLoanWord -> ScannedDocumentType.PAYMENT_RECEIPT
            providerCategory == EntityCategory.BNPL_PLATFORM || lower.contains("خرید اقساطی") || lower.contains("bnpl") ->
                ScannedDocumentType.BNPL_INVOICE
            hasContractOrLoanWord || lower.contains("apr") ||
                lower.contains("interest") || lower.contains("installment") || lower.contains("قسط") ->
                ScannedDocumentType.LOAN_CONTRACT
            hasReceiptWord -> ScannedDocumentType.PAYMENT_RECEIPT
            else -> ScannedDocumentType.UNKNOWN
        }
    }

    private fun extractAnnualInterestRate(cleaned: String, lines: List<String>): Double? {
        val labeledRegex = Regex(
            "(?:نرخ\\s*سود|نرخ\\s*کارمزد|سود\\s*سالانه|سود\\s*تسهیلات|کارمزد|سود|بهره|interest\\s*rate|annual\\s*rate|apr|rate)\\s*[:：\\-]?\\s*(\\d{1,2}(?:\\.\\d{1,2})?)\\s*(?:%|درصد|percent)?",
            RegexOption.IGNORE_CASE
        )
        for (line in lines) {
            val match = labeledRegex.find(line)
            if (match != null) {
                val value = match.groupValues[1].toDoubleOrNull()
                if (value != null && value in 0.1..65.0) {
                    return value
                }
            }
        }
        // Fallback: standalone percentage token (e.g. "23%", "18.5 درصد")
        val pctRegex = Regex("(\\d{1,2}(?:\\.\\d{1,2})?)\\s*(?:%|درصد|percent)", RegexOption.IGNORE_CASE)
        for (match in pctRegex.findAll(cleaned)) {
            val value = match.groupValues[1].toDoubleOrNull()
            if (value != null && value in 0.5..60.0) {
                return value
            }
        }
        return null
    }

    private fun extractInstallmentCount(cleaned: String, lines: List<String>): Int? {
        val labeledRegex = Regex(
            "(?:تعداد\\s*اقساط|تعداد\\s*قسط|مدت\\s*بازپرداخت|مدت\\s*وام|اقساط|installments?|number\\s*of\\s*payments|loan\\s*term|term|tenor)\\s*[:：\\-]?\\s*(\\d{1,3})\\b",
            RegexOption.IGNORE_CASE
        )
        for (line in lines) {
            val m = labeledRegex.find(line)
            val v = m?.groupValues?.getOrNull(1)?.toIntOrNull()
            if (v != null && v in 1..360) return v
        }
        val suffixRegex = Regex(
            "\\b(\\d{1,3})\\s*(?:ماهه|ماه|قسطه|قسط|months?|mos\\.?|installments?|payments?)\\b",
            RegexOption.IGNORE_CASE
        )
        for (match in suffixRegex.findAll(cleaned)) {
            val v = match.groupValues[1].toIntOrNull()
            if (v != null && v in 1..360) return v
        }
        return null
    }

    private data class ExtractedAmountsResult(
        val principalToman: Long?,
        val installmentToman: Long?,
        val convertedAnyFromRial: Boolean
    )

    private fun extractAmountsToman(
        lines: List<String>,
        defaultDocIsRial: Boolean,
        docType: ScannedDocumentType
    ): ExtractedAmountsResult {
        var principalToman: Long? = null
        var installmentToman: Long? = null
        var convertedFromRial = false
        val unlabeledAmounts = mutableListOf<Long>()

        val principalKeywords = listOf(
            "اصل وام", "مبلغ وام", "مبلغ کل", "مبلغ تسهیلات", "کل تسهیلات", "اصل تسهیلات",
            "مبلغ قرارداد", "سقف تسهیلات", "loan amount", "principal", "total loan", "total amount",
            "financed amount", "credit amount", "amount borrowed"
        )
        val installmentKeywords = listOf(
            "مبلغ قسط", "قسط ماهانه", "هر قسط", "مبلغ هر قسط", "مبلغ پرداختی", "مبلغ واریزی",
            "مبلغ تراکنش", "مبلغ رسید", "پرداخت ماهانه", "قسط", "installment", "monthly payment",
            "emi", "payment amount", "amount due", "paid amount", "receipt amount", "monthly due"
        )
        val ignoreAmountKeywords = listOf(
            "کد ملی", "شماره حساب", "شماره کارت", "شبا", "شماره قرارداد", "کد پیگیری", "شماره پیگیری",
            "تلفن", "موبایل", "account", "card", "iban", "contract #", "ref", "receipt #", "phone", "id:"
        )

        for (line in lines) {
            val lowerLine = line.lowercase(Locale.US)
            if (ignoreAmountKeywords.any { lowerLine.contains(it) } &&
                principalKeywords.none { lowerLine.contains(it) } &&
                installmentKeywords.none { lowerLine.contains(it) }
            ) {
                continue
            }

            val (amountToman, wasRial) = parseMonetaryAmountFromLine(line, defaultDocIsRial) ?: continue
            if (wasRial) convertedFromRial = true

            val isPrincipalLine = principalKeywords.any { lowerLine.contains(it) }
            val isInstallmentLine = installmentKeywords.any { lowerLine.contains(it) }

            when {
                isPrincipalLine && !isInstallmentLine -> {
                    if (principalToman == null || amountToman > principalToman) {
                        principalToman = amountToman
                    }
                }
                isInstallmentLine && !isPrincipalLine -> {
                    if (installmentToman == null) {
                        installmentToman = amountToman
                    }
                }
                isPrincipalLine && isInstallmentLine -> {
                    // Prefer principal for larger value, installment for smaller
                    if (principalToman == null) principalToman = amountToman else installmentToman = amountToman
                }
                else -> {
                    unlabeledAmounts.add(amountToman)
                }
            }
        }

        // Fall back to unlabeled monetary amounts if labeled ones were missing
        if (unlabeledAmounts.isNotEmpty()) {
            val sortedDesc = unlabeledAmounts.distinct().sortedDescending()
            if (principalToman == null && installmentToman == null) {
                if (sortedDesc.size >= 2 && docType != ScannedDocumentType.PAYMENT_RECEIPT) {
                    principalToman = sortedDesc.first()
                    installmentToman = sortedDesc[1]
                } else if (docType == ScannedDocumentType.PAYMENT_RECEIPT) {
                    installmentToman = sortedDesc.first()
                } else {
                    principalToman = sortedDesc.first()
                }
            } else if (principalToman == null && installmentToman != null) {
                val larger = sortedDesc.firstOrNull { it > installmentToman }
                if (larger != null) principalToman = larger
            } else if (installmentToman == null && principalToman != null) {
                val smaller = sortedDesc.firstOrNull { it < principalToman }
                if (smaller != null) installmentToman = smaller
            }
        }

        // Sanity swap if installment > principal (except single receipt)
        if (principalToman != null && installmentToman != null && installmentToman > principalToman) {
            val tmp = principalToman
            principalToman = installmentToman
            installmentToman = tmp
        }

        return ExtractedAmountsResult(
            principalToman = principalToman,
            installmentToman = installmentToman,
            convertedAnyFromRial = convertedFromRial
        )
    }

    private fun parseMonetaryAmountFromLine(line: String, defaultDocIsRial: Boolean): Pair<Long, Boolean>? {
        val lower = line.lowercase(Locale.US)
        val lineHasRial = Regex("(ریال|ریالی|\\brial\\b|\\birr\\b)").containsMatchIn(lower)
        val lineHasToman = Regex("(تومان|تومانی|\\btoman\\b|\\birt\\b)").containsMatchIn(lower)
        val isRial = when {
            lineHasToman -> false
            lineHasRial -> true
            else -> defaultDocIsRial
        }

        // Check shorthand Persian units first: e.g. "50 میلیون تومان", "12.5 میلیون ریال"
        val millionRegex = Regex("(\\d+(?:\\.\\d+)?)\\s*میلیون")
        millionRegex.find(lower)?.let { m ->
            val v = m.groupValues[1].toDoubleOrNull() ?: return@let
            val raw = (v * 1_000_000.0).roundToLong()
            val toman = if (isRial) raw / 10L else raw
            if (toman >= 1_000L) return toman to isRial
        }

        val thousandRegex = Regex("(\\d+(?:\\.\\d+)?)\\s*هزار")
        thousandRegex.find(lower)?.let { m ->
            val v = m.groupValues[1].toDoubleOrNull() ?: return@let
            val raw = (v * 1_000.0).roundToLong()
            val toman = if (isRial) raw / 10L else raw
            if (toman >= 1_000L) return toman to isRial
        }

        // Remove dates & percentages from line before scanning currency numbers so years/rates aren't confused
        val stripped = line
            .replace(Regex("\\b(?:13|14|19|20)\\d{2}[/\\-.]\\d{1,2}[/\\-.]\\d{1,2}\\b"), " ")
            .replace(Regex("\\b\\d{1,2}[/\\-.]\\d{1,2}[/\\-.](?:13|14|19|20)\\d{2}\\b"), " ")
            .replace(Regex("\\d{1,2}(?:\\.\\d{1,2})?\\s*(?:%|درصد)"), " ")

        // Match grouped currency numbers like 12,500,000 or $1,250.00 or plain 50000000
        val amountRegex = Regex("(?:\\$|USD\\s*)?(\\d{1,3}(?:,\\d{3})+(?:\\.\\d{1,2})?|\\d{4,12}(?:\\.\\d{1,2})?)")
        val candidates = amountRegex.findAll(stripped).mapNotNull { match ->
            val token = match.groupValues[1].replace(",", "")
            // Ignore 16-digit card numbers or 10-digit national IDs without commas if > 11 digits
            if (!match.groupValues[1].contains(",") && token.substringBefore(".").length >= 11) {
                return@mapNotNull null
            }
            val parsedDouble = token.toDoubleOrNull() ?: return@mapNotNull null
            val rawLong = parsedDouble.roundToLong()
            // Filter out lone calendar years like 1405 or 2026 unless preceded by currency sign or comma
            val hasCurrencyMarker = match.value.contains("$") || match.value.contains(",") || lineHasRial || lineHasToman
            if (!hasCurrencyMarker && rawLong in 1300L..2100L) return@mapNotNull null
            if (rawLong < 100L) return@mapNotNull null
            rawLong
        }.toList()

        val bestRaw = candidates.maxOrNull() ?: return null
        val toman = if (isRial && bestRaw >= 10L) bestRaw / 10L else bestRaw
        return if (toman > 0L) toman to isRial else null
    }

    private fun estimateInstallmentCount(
        principalToman: Long,
        installmentToman: Long,
        annualInterestPercent: Double?
    ): Int {
        if (installmentToman <= 0L || principalToman <= installmentToman) return 1
        if (annualInterestPercent == null || annualInterestPercent <= 0.0) {
            return (principalToman.toDouble() / installmentToman.toDouble()).roundToLong().toInt()
        }
        val r = (annualInterestPercent / 100.0) / 12.0
        val ratio = (principalToman.toDouble() * r) / installmentToman.toDouble()
        if (ratio >= 0.99) {
            return (principalToman.toDouble() / installmentToman.toDouble()).roundToLong().toInt()
        }
        val n = -kotlin.math.ln(1.0 - ratio) / kotlin.math.ln(1.0 + r)
        return n.roundToLong().toInt().coerceIn(2, 360)
    }

    private data class ExtractedDatesResult(
        val firstDueDateJalali: String?,
        val allDatesJalali: List<String>,
        val convertedAnyGregorian: Boolean
    )

    private fun extractDatesJalali(lines: List<String>, todayJalali: String): ExtractedDatesResult {
        val dueKeywords = listOf(
            "سررسید", "اولین قسط", "قسط اول", "تاریخ شروع", "تاریخ پرداخت", "تاریخ واریز",
            "due date", "first payment", "next payment", "payment date", "due on", "start date"
        )
        var prioritizedDate: String? = null
        val allDates = LinkedHashSet<String>()
        var convertedGregorian = false

        val jalaliFullRegex = Regex("\\b(1[34]\\d{2})[/\\-.](\\d{1,2})[/\\-.](\\d{1,2})\\b")
        val jalaliShortYearRegex = Regex("(?:تاریخ|سررسید|قسط)\\s*[:：\\-]?\\s*(0\\d)[/\\-.](\\d{1,2})[/\\-.](\\d{1,2})\\b")
        val gregYmdRegex = Regex("\\b(20\\d{2})[/\\-.](\\d{1,2})[/\\-.](\\d{1,2})\\b")
        val gregDmyOrMdyRegex = Regex("\\b(\\d{1,2})[/\\-.](\\d{1,2})[/\\-.](20\\d{2})\\b")
        val gregMonthFirstRegex = Regex("\\b([A-Za-z]{3,9})\\s+(\\d{1,2}),?\\s+(20\\d{2})\\b")
        val gregDayFirstRegex = Regex("\\b(\\d{1,2})\\s+([A-Za-z]{3,9}),?\\s+(20\\d{2})\\b")

        for (line in lines) {
            val lowerLine = line.lowercase(Locale.US)
            val isDueLine = dueKeywords.any { lowerLine.contains(it) }
            val lineDates = mutableListOf<String>()

            // 1. Jalali YYYY/MM/DD
            jalaliFullRegex.findAll(line).forEach { m ->
                val y = m.groupValues[1].toIntOrNull() ?: return@forEach
                val mo = m.groupValues[2].toIntOrNull() ?: return@forEach
                val d = m.groupValues[3].toIntOrNull() ?: return@forEach
                if (JalaliCalendarEngine.isValidJalaliDate(y, mo, d)) {
                    lineDates.add(JalaliCalendarEngine.normalizeJalaliDate("$y/$mo/$d"))
                }
            }

            // 2. Short Jalali YY/MM/DD (e.g., 05/09/15 -> 1405/09/15)
            jalaliShortYearRegex.findAll(line).forEach { m ->
                val y = 1400 + (m.groupValues[1].toIntOrNull() ?: return@forEach)
                val mo = m.groupValues[2].toIntOrNull() ?: return@forEach
                val d = m.groupValues[3].toIntOrNull() ?: return@forEach
                if (JalaliCalendarEngine.isValidJalaliDate(y, mo, d)) {
                    lineDates.add(JalaliCalendarEngine.normalizeJalaliDate("$y/$mo/$d"))
                }
            }

            // 3. Gregorian YYYY-MM-DD / YYYY/MM/DD
            gregYmdRegex.findAll(line).forEach { m ->
                val gy = m.groupValues[1].toIntOrNull() ?: return@forEach
                val gm = m.groupValues[2].toIntOrNull() ?: return@forEach
                val gd = m.groupValues[3].toIntOrNull() ?: return@forEach
                if (gm in 1..12 && gd in 1..31) {
                    lineDates.add(JalaliCalendarEngine.gregorianToJalali(gy, gm, gd).formatCanonical())
                    convertedGregorian = true
                }
            }

            // 4. Gregorian DD/MM/YYYY or MM/DD/YYYY
            gregDmyOrMdyRegex.findAll(line).forEach { m ->
                val p1 = m.groupValues[1].toIntOrNull() ?: return@forEach
                val p2 = m.groupValues[2].toIntOrNull() ?: return@forEach
                val gy = m.groupValues[3].toIntOrNull() ?: return@forEach
                val (gm, gd) = when {
                    p1 > 12 && p2 in 1..12 -> p2 to p1
                    p2 > 12 && p1 in 1..12 -> p1 to p2
                    p1 in 1..12 && p2 in 1..31 -> p1 to p2
                    else -> return@forEach
                }
                lineDates.add(JalaliCalendarEngine.gregorianToJalali(gy, gm, gd).formatCanonical())
                convertedGregorian = true
            }

            // 5. English Month Name Dates ("Dec 15, 2026" or "15 Dec 2026")
            gregMonthFirstRegex.findAll(line).forEach { m ->
                val gm = ENGLISH_MONTHS[m.groupValues[1].lowercase(Locale.US)] ?: return@forEach
                val gd = m.groupValues[2].toIntOrNull() ?: return@forEach
                val gy = m.groupValues[3].toIntOrNull() ?: return@forEach
                if (gd in 1..31) {
                    lineDates.add(JalaliCalendarEngine.gregorianToJalali(gy, gm, gd).formatCanonical())
                    convertedGregorian = true
                }
            }
            gregDayFirstRegex.findAll(line).forEach { m ->
                val gd = m.groupValues[1].toIntOrNull() ?: return@forEach
                val gm = ENGLISH_MONTHS[m.groupValues[2].lowercase(Locale.US)] ?: return@forEach
                val gy = m.groupValues[3].toIntOrNull() ?: return@forEach
                if (gd in 1..31) {
                    lineDates.add(JalaliCalendarEngine.gregorianToJalali(gy, gm, gd).formatCanonical())
                    convertedGregorian = true
                }
            }

            if (lineDates.isNotEmpty()) {
                allDates.addAll(lineDates)
                if (isDueLine && prioritizedDate == null) {
                    prioritizedDate = lineDates.first()
                }
            }
        }

        val sortedDates = allDates.sorted()
        val chosenFirst = prioritizedDate
            ?: sortedDates.firstOrNull { it >= todayJalali }
            ?: sortedDates.firstOrNull()

        return ExtractedDatesResult(
            firstDueDateJalali = chosenFirst,
            allDatesJalali = sortedDates,
            convertedAnyGregorian = convertedGregorian
        )
    }

    private fun extractTrackingCode(cleaned: String): String? {
        val codeRegex = Regex(
            "(?:شماره\\s*قرارداد|شماره\\s*پیگیری|کد\\s*رهگیری|شماره\\s*تسهیلات|شماره\\s*رسید|شماره\\s*مرجع|contract\\s*(?:no\\.?|#|id)|ref(?:erence)?\\s*(?:no\\.?|#)?|receipt\\s*(?:no\\.?|#)|loan\\s*id)\\s*[:：\\-]?\\s*([A-Za-z0-9\\-/]{4,24})",
            RegexOption.IGNORE_CASE
        )
        return codeRegex.find(cleaned)?.groupValues?.getOrNull(1)?.trim()
    }

    private fun buildSuggestedTitle(
        docType: ScannedDocumentType,
        counterparty: String?,
        installmentCount: Int?,
        lowerText: String
    ): String {
        val loanKindFa = when {
            lowerText.contains("مسکن") || lowerText.contains("mortgage") -> "وام مسکن"
            lowerText.contains("خودرو") || lowerText.contains("auto loan") || lowerText.contains("car loan") -> "وام خودرو"
            lowerText.contains("ازدواج") -> "وام ازدواج"
            lowerText.contains("قرض الحسنه") || lowerText.contains("قرض‌الحسنه") -> "وام قرض‌الحسنه"
            lowerText.contains("مرابحه") -> "تسهیلات مرابحه"
            docType == ScannedDocumentType.BNPL_INVOICE -> "خرید اقساطی"
            docType == ScannedDocumentType.CHECK_OR_PROMISSORY -> "چک صیادی"
            docType == ScannedDocumentType.PAYMENT_RECEIPT -> "رسید پرداخت قسط"
            else -> "وام"
        }
        val base = if (!counterparty.isNullOrBlank()) {
            "$loanKindFa $counterparty"
        } else {
            "$loanKindFa اسکن‌شده"
        }
        return if (installmentCount != null && installmentCount > 1 && docType != ScannedDocumentType.PAYMENT_RECEIPT) {
            "$base (${PersianCurrencyEngine.toPersianDigits(installmentCount.toString())} قسط)"
        } else {
            base
        }
    }

    private fun formatPercentFa(rate: Double): String {
        val formatted = if (rate % 1.0 == 0.0) {
            rate.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", rate)
        }
        return PersianCurrencyEngine.toPersianDigits(formatted)
    }
}
