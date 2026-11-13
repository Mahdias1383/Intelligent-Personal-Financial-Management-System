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

    /**
     * Formats an internal Toman amount in the requested display unit (Toman or Rial),
     * optionally applying the Privacy Mask ("•••••••").
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
        val formatted = formatNumberWithCommas(displayVal, usePersianDigits = true)
        return if (includeUnit) "$formatted ${unit.labelFa}" else formatted
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
