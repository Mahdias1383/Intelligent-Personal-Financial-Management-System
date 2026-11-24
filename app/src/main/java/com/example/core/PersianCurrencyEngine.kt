package com.example.core

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class CurrencyUnit(val labelFa: String, val multiplierFromToman: Long) {
    TOMAN("تومان", 1L),
    RIAL("ریال", 10L)
}

object PersianCurrencyEngine {

    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    private val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    private val ones = arrayOf(
        "", "یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه",
        "ده", "یازده", "دوازده", "سیزده", "چهارده", "پانزده", "شانزده", "هفده", "هجده", "نوزده"
    )

    private val tens = arrayOf(
        "", "", "بیست", "سی", "چهل", "پنجاه", "شصت", "هفتاد", "هشتاد", "نود"
    )

    private val hundreds = arrayOf(
        "", "صد", "دویست", "سیصد", "چهارصد", "پانصد", "ششصد", "هفتصد", "هشتصد", "نهصد"
    )

    private val scaleNames = arrayOf(
        "", "هزار", "میلیون", "میلیارد", "تریلیون", "کوادریلیون"
    )

    fun toPersianDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '0'..'9' -> sb.append(persianDigits[ch - '0'])
                in '٠'..'٩' -> sb.append(persianDigits[ch - '٠'])
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toEnglishDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val pIdx = persianDigits.indexOf(ch)
            val aIdx = arabicDigits.indexOf(ch)
            when {
                pIdx >= 0 -> sb.append(('0'.code + pIdx).toChar())
                aIdx >= 0 -> sb.append(('0'.code + aIdx).toChar())
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatNumberWithCommas(value: Long, usePersianDigits: Boolean = true): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
        }
        val formatted = DecimalFormat("#,###", symbols).format(value)
        return if (usePersianDigits) toPersianDigits(formatted) else formatted
    }

    const val LRI: String = "\u2066"
    const val RLI: String = "\u2067"
    const val PDI: String = "\u2069"
    const val RLM: String = "\u200F"

    private val persianLetterRegex = Regex("[\u0600-\u06FF]")
    private val embeddedLatinRunRegex = Regex(
        """(?:\.?[A-Za-z][A-Za-z0-9\u06F0-\u06F9._+-]*(?:\s+[A-Za-z0-9][A-Za-z0-9\u06F0-\u06F9._+-]*)*)"""
    )
    private val currencySymbolAmountRegex = Regex(
        """(?:[$€£¥₮₺]\s*[0-9\u06F0-\u06F9][0-9\u06F0-\u06F9,.\u066B\u066C]*)"""
    )
    private val signedNumberRegex = Regex(
        """(?:[+\-\u2212][0-9\u06F0-\u06F9][0-9\u06F0-\u06F9,.\u066B\u066C]*(?:[%٪])?)"""
    )

    /**
     * Wraps a purely LTR run (digits, signs, Latin acronyms, currency codes/symbols, card numbers,
     * Sheba/tracking codes, or filenames) in Unicode Bidirectional LRI...PDI isolate characters so
     * it never leaks LTR ordering into surrounding Persian RTL words, colons, parentheses, or digits.
     */
    fun isolateLtr(raw: String): String {
        if (raw.isEmpty()) return raw
        if (raw.startsWith(LRI) && raw.endsWith(PDI)) return raw
        return "$LRI$raw$PDI"
    }

    /**
     * Protects Persian RTL sentences containing embedded English words, currency codes/symbols,
     * alphanumeric tracking codes (including Persian digits like "SNP-۷۷۴۱۲۰" or "IR۸۲۰۵۶۰..."),
     * file extensions (".xls", "PDF", "A4", "SHA-256"), or signed numbers ("+۵٪", "−۲,۵۰۰,۰۰۰")
     * from UAX #9 Bidirectional reordering bugs. Purely unsigned Persian strings or purely Latin
     * strings are returned unchanged, and the function is strictly idempotent.
     */
    fun formatRtlBidiSafe(text: String): String {
        if (text.isEmpty() || !persianLetterRegex.containsMatchIn(text)) return text
        var needsIsolation = false
        for (ch in text) {
            if (
                (ch in 'A'..'Z') || (ch in 'a'..'z') ||
                ch == '$' || ch == '€' || ch == '£' || ch == '¥' || ch == '₮' || ch == '₺' ||
                ch == '+' || ch == '-' || ch == '\u2212'
            ) {
                needsIsolation = true
                break
            }
        }
        if (!needsIsolation) return text

        val result = StringBuilder(text.length + 16)
        var i = 0
        var insideIsolateDepth = 0
        while (i < text.length) {
            val ch = text[i]
            if (ch == '\u2066' || ch == '\u2067' || ch == '\u2068') {
                insideIsolateDepth++
                result.append(ch)
                i++
                continue
            }
            if (ch == '\u2069') {
                if (insideIsolateDepth > 0) insideIsolateDepth--
                result.append(ch)
                i++
                continue
            }
            if (insideIsolateDepth == 0) {
                val latinMatch = embeddedLatinRunRegex.matchAt(text, i)
                if (latinMatch != null) {
                    val token = latinMatch.value.trimEnd('.', '-', '+')
                    if (token.isNotEmpty() && token.any { (it in 'A'..'Z') || (it in 'a'..'z') }) {
                        result.append(LRI).append(token).append(PDI)
                        i += token.length
                        continue
                    }
                }
                val currencyMatch = currencySymbolAmountRegex.matchAt(text, i)
                if (currencyMatch != null) {
                    val token = currencyMatch.value.trimEnd('.', ',')
                    if (token.isNotEmpty()) {
                        result.append(LRI).append(token).append(PDI)
                        i += token.length
                        continue
                    }
                }
                val prevIsDigit = i > 0 && (text[i - 1] in '0'..'9' || text[i - 1] in '\u06F0'..'\u06F9')
                if (!prevIsDigit) {
                    val signedMatch = signedNumberRegex.matchAt(text, i)
                    if (signedMatch != null) {
                        val token = signedMatch.value.trimEnd('.', ',')
                        if (token.isNotEmpty()) {
                            result.append(LRI).append(token).append(PDI)
                            i += token.length
                            continue
                        }
                    }
                }
            }
            result.append(ch)
            i++
        }
        return result.toString()
    }

    /**
     * Formats an internal Toman amount in the requested display unit (Toman or Rial),
     * isolating ONLY the numeric run in LTR BiDi context so the Persian currency unit ("تومان"/"ریال")
     * always stays in strict RTL order to the left of the digits.
     */
    fun formatAmount(
        amountToman: Long,
        unit: CurrencyUnit = CurrencyUnit.TOMAN,
        includeUnit: Boolean = true,
        privacyMasked: Boolean = false
    ): String {
        if (privacyMasked) {
            return if (includeUnit) "•••,••• ${unit.labelFa}" else "•••,•••"
        }
        val displayVal = amountToman * unit.multiplierFromToman
        val signPrefix = if (displayVal < 0L) "−" else ""
        val formattedDigits = formatNumberWithCommas(kotlin.math.abs(displayVal), usePersianDigits = true)
        val isolatedDigits = isolateLtr("$signPrefix$formattedDigits")
        return if (includeUnit) "$isolatedDigits ${unit.labelFa}" else isolatedDigits
    }

    /**
     * Formats a signed Toman amount (e.g. "+۴,۰۰۰,۰۰۰ تومان" or "−۲,۵۰۰,۰۰۰ تومان") with the sign
     * and digits isolated together in an LTR BiDi run while the Persian currency unit stays RTL.
     */
    fun formatSignedAmount(
        amountToman: Long,
        unit: CurrencyUnit = CurrencyUnit.TOMAN,
        forcePositiveSign: Boolean = false,
        includeUnit: Boolean = true,
        privacyMasked: Boolean = false
    ): String {
        if (privacyMasked) {
            return if (includeUnit) "•••,••• ${unit.labelFa}" else "•••,•••"
        }
        val displayVal = amountToman * unit.multiplierFromToman
        val signPrefix = when {
            displayVal < 0L -> "−"
            forcePositiveSign && displayVal > 0L -> "+"
            else -> ""
        }
        val formattedDigits = formatNumberWithCommas(kotlin.math.abs(displayVal), usePersianDigits = true)
        val isolatedDigits = isolateLtr("$signPrefix$formattedDigits")
        return if (includeUnit) "$isolatedDigits ${unit.labelFa}" else isolatedDigits
    }

    /**
     * Formats an internal Toman amount into a crystal-clear, unambiguous Persian compact figure
     * using full scale names ("هزار", "میلیون", "میلیارد", "تریلیون") instead of ambiguous
     * single-letter abbreviations like "م" or "ه".
     *
     * ONLY the numeric digits + optional sign are wrapped in LTR isolate (\u2066...\u2069),
     * keeping the Persian scale word and currency unit in strict RTL flow so values always render
     * naturally right-to-left (e.g. "۱۳۷.۲ میلیون تومان", "۱.۲۵ میلیارد تومان", "۸۵۰ هزار تومان").
     */
    fun formatCompactAmount(
        amountToman: Long,
        unit: CurrencyUnit = CurrencyUnit.TOMAN,
        includeUnit: Boolean = false,
        privacyMasked: Boolean = false,
        showPlusSign: Boolean = false
    ): String {
        if (privacyMasked) {
            return if (includeUnit) "••• ${unit.labelFa}" else "•••"
        }
        val displayVal = amountToman * unit.multiplierFromToman
        val signPrefix = when {
            displayVal < 0L -> "−"
            showPlusSign && displayVal > 0L -> "+"
            else -> ""
        }
        val absVal = kotlin.math.abs(displayVal)

        val (numStr, scaleWord) = when {
            absVal >= 1_000_000_000_000L -> {
                formatTrimmedDecimal(absVal / 1_000_000_000_000.0, maxDecimals = 2) to "تریلیون"
            }
            absVal >= 1_000_000_000L -> {
                val b = absVal / 1_000_000_000.0
                val rounded = kotlin.math.round(b * 100.0) / 100.0
                if (rounded >= 1000.0) {
                    formatTrimmedDecimal(rounded / 1000.0, maxDecimals = 2) to "تریلیون"
                } else {
                    formatTrimmedDecimal(rounded, maxDecimals = 2) to "میلیارد"
                }
            }
            absVal >= 1_000_000L -> {
                val m = absVal / 1_000_000.0
                val rounded = kotlin.math.round(m * 10.0) / 10.0
                if (rounded >= 1000.0) {
                    formatTrimmedDecimal(rounded / 1000.0, maxDecimals = 2) to "میلیارد"
                } else {
                    formatTrimmedDecimal(rounded, maxDecimals = 1) to "میلیون"
                }
            }
            absVal >= 1_000L -> {
                val k = absVal / 1_000.0
                val rounded = kotlin.math.round(k * 10.0) / 10.0
                if (rounded >= 1000.0) {
                    formatTrimmedDecimal(rounded / 1000.0, maxDecimals = 1) to "میلیون"
                } else {
                    formatTrimmedDecimal(rounded, maxDecimals = 1) to "هزار"
                }
            }
            else -> absVal.toString() to ""
        }

        val isolatedNumeric = isolateLtr(toPersianDigits("$signPrefix$numStr"))
        val withScale = if (scaleWord.isNotEmpty()) "$isolatedNumeric $scaleWord" else isolatedNumeric
        return if (includeUnit) "$withScale ${unit.labelFa}" else withScale
    }

    private fun formatTrimmedDecimal(value: Double, maxDecimals: Int): String {
        val pattern = if (maxDecimals <= 1) "%.1f" else "%.2f"
        var formatted = String.format(Locale.US, pattern, value)
        if (formatted.contains('.')) {
            formatted = formatted.trimEnd('0').trimEnd('.')
        }
        return formatted
    }

    /**
     * Converts any non-negative integer into full fluent Persian words.
     * E.g., 15400000 -> "پانزده میلیون و چهارصد هزار"
     */
    fun numberToPersianWords(number: Long): String {
        if (number == 0L) return "صفر"
        if (number < 0L) return "منفی " + numberToPersianWords(-number)

        var remaining = number
        var scaleIndex = 0
        val parts = mutableListOf<String>()

        while (remaining > 0 && scaleIndex < scaleNames.size) {
            val chunk = (remaining % 1000L).toInt()
            if (chunk > 0) {
                val chunkWords = threeDigitToPersianWords(chunk)
                val scale = scaleNames[scaleIndex]
                val combined = if (scale.isNotEmpty()) "$chunkWords $scale" else chunkWords
                parts.add(0, combined)
            }
            remaining /= 1000L
            scaleIndex++
        }

        return parts.joinToString(" و ")
    }

    private fun threeDigitToPersianWords(n: Int): String {
        val h = n / 100
        val rem = n % 100
        val subParts = mutableListOf<String>()
        if (h > 0) {
            subParts.add(hundreds[h])
        }
        if (rem > 0) {
            if (rem < 20) {
                subParts.add(ones[rem])
            } else {
                val t = rem / 10
                val o = rem % 10
                subParts.add(tens[t])
                if (o > 0) {
                    subParts.add(ones[o])
                }
            }
        }
        return subParts.joinToString(" و ")
    }

    /**
     * Converts an internal Toman amount into a complete Persian Currency Wordifier phrase.
     * E.g., 15_400_000L (TOMAN) -> "پانزده میلیون و چهارصد هزار تومان"
     */
    fun amountToPersianWords(amountToman: Long, unit: CurrencyUnit = CurrencyUnit.TOMAN): String {
        val displayVal = (amountToman * unit.multiplierFromToman).coerceAtLeast(0L)
        return "${numberToPersianWords(displayVal)} ${unit.labelFa}"
    }

    /**
     * Evaluates simple inline arithmetic expressions (+, -, *) entered in money inputs.
     * E.g. "15000000+400000" -> 15400000L
     */
    fun evaluateInlineMath(rawInput: String): Long? {
        val clean = toEnglishDigits(rawInput)
            .replace(",", "")
            .replace(" ", "")
            .trim()
        if (clean.isEmpty()) return 0L

        clean.toLongOrNull()?.let { return it.coerceAtLeast(0L) }

        return try {
            if (clean.contains("+")) {
                clean.split("+")
                    .filter { it.isNotBlank() }
                    .sumOf { it.toLong() }
                    .coerceAtLeast(0L)
            } else if (clean.contains("-") && !clean.startsWith("-")) {
                val tokens = clean.split("-").filter { it.isNotBlank() }
                if (tokens.isEmpty()) 0L
                else tokens.drop(1).fold(tokens.first().toLong()) { acc, s -> acc - s.toLong() }
                    .coerceAtLeast(0L)
            } else if (clean.contains("*")) {
                clean.split("*")
                    .filter { it.isNotBlank() }
                    .fold(1L) { acc, s -> acc * s.toLong() }
                    .coerceAtLeast(0L)
            } else {
                val digitsOnly = clean.filter { it.isDigit() }
                digitsOnly.toLongOrNull() ?: 0L
            }
        } catch (_: Exception) {
            val digitsOnly = clean.filter { it.isDigit() }
            digitsOnly.toLongOrNull()
        }
    }

    /**
     * Formats a 16-digit Iranian bank card number into "XXXX - XXXX - XXXX - XXXX".
     */
    fun formatCardNumber(raw: String, usePersianDigits: Boolean = true): String {
        val digits = toEnglishDigits(raw).filter { it.isDigit() }.take(16)
        val grouped = digits.chunked(4).joinToString(" - ")
        return if (usePersianDigits) toPersianDigits(grouped) else grouped
    }

    /**
     * Normalizes an Iranian IBAN / Sheba number to start with "IR" + up to 24 digits.
     */
    fun formatSheba(raw: String): String {
        val digits = toEnglishDigits(raw).filter { it.isDigit() }.take(24)
        return if (digits.isEmpty()) "" else "IR$digits"
    }
}
