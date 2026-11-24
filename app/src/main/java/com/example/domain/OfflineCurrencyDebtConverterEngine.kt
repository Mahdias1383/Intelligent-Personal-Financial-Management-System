package com.example.domain

import com.example.core.CurrencyUnit
import com.example.core.PersianCurrencyEngine
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

enum class ForeignDebtCurrency(
    val code: String,
    val labelFa: String,
    val shortLabelFa: String,
    val symbol: String,
    val defaultManualRateToman: Long,
    val decimalPlaces: Int,
    val accentColorHex: Long
) {
    USD(
        code = "USD",
        labelFa = "دلار آمریکا (USD)",
        shortLabelFa = "دلار",
        symbol = "$",
        defaultManualRateToman = 72_000L,
        decimalPlaces = 2,
        accentColorHex = 0xFF35B8AB
    ),
    EUR(
        code = "EUR",
        labelFa = "یورو اروپا (EUR)",
        shortLabelFa = "یورو",
        symbol = "€",
        defaultManualRateToman = 76_500L,
        decimalPlaces = 2,
        accentColorHex = 0xFF4DA3B8
    ),
    AED(
        code = "AED",
        labelFa = "درهم امارات (AED)",
        shortLabelFa = "درهم",
        symbol = "د.إ",
        defaultManualRateToman = 19_700L,
        decimalPlaces = 2,
        accentColorHex = 0xFF689EB8
    ),
    GOLD_GRAM(
        code = "GOLD18",
        labelFa = "گرم طلای ۱۸ عیار",
        shortLabelFa = "گرم طلا",
        symbol = "گرم",
        defaultManualRateToman = 4_650_000L,
        decimalPlaces = 3,
        accentColorHex = 0xFFDAB978
    ),
    USDT(
        code = "USDT",
        labelFa = "تتر / دلار دیجیتال (USDT)",
        shortLabelFa = "تتر",
        symbol = "₮",
        defaultManualRateToman = 72_300L,
        decimalPlaces = 2,
        accentColorHex = 0xFF53C9BD
    ),
    TRY(
        code = "TRY",
        labelFa = "لیر ترکیه (TRY)",
        shortLabelFa = "لیر",
        symbol = "₺",
        defaultManualRateToman = 2_150L,
        decimalPlaces = 2,
        accentColorHex = 0xFFC99B68
    );

    companion object {
        fun fromCode(code: String): ForeignDebtCurrency {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) || it.name.equals(code, ignoreCase = true) }
                ?: USD
        }
    }
}

data class ForeignCurrencyDebtPosition(
    val id: Long,
    val titleFa: String,
    val counterpartyNameFa: String,
    val currency: ForeignDebtCurrency,
    val totalForeignAmount: Double,
    val paidForeignAmount: Double = 0.0,
    val entryExchangeRateToman: Long,
    val dueDateJalali: String = "1405/10/15"
) {
    val remainingForeignAmount: Double
        get() = (totalForeignAmount - paidForeignAmount).coerceAtLeast(0.0)

    val isSettled: Boolean
        get() = remainingForeignAmount <= 0.0001
}

data class EvaluatedForeignDebtPosition(
    val position: ForeignCurrencyDebtPosition,
    val currentManualRateToman: Long,
    val remainingTomanAtCurrentRate: Long,
    val remainingTomanAtEntryRate: Long,
    val fxDeltaToman: Long,
    val fxChangePercent: Int
)

data class CrossCurrencyConversionRow(
    val currency: ForeignDebtCurrency,
    val manualRateToman: Long,
    val convertedForeignAmount: Double,
    val formattedForeignAmountFa: String
)

data class MultiCurrencyConversionResult(
    val sourceCurrency: ForeignDebtCurrency,
    val sourceForeignAmount: Double,
    val manualRateToman: Long,
    val equivalentToman: Long,
    val equivalentRial: Long,
    val crossConversions: List<CrossCurrencyConversionRow>
)

data class MultiCurrencyDebtPortfolioSummary(
    val manualRates: Map<ForeignDebtCurrency, Long>,
    val evaluatedPositions: List<EvaluatedForeignDebtPosition>,
    val totalForeignDebtEquivalentToman: Long,
    val totalForeignDebtEntryToman: Long,
    val totalFxDeltaToman: Long,
    val domesticRemainingDebtToman: Long,
    val combinedGrandTotalDebtToman: Long,
    val activePositionsCount: Int
)

/**
 * Pure offline-first currency conversion and multi-currency debt valuation engine
 * powered entirely by user-definable manual exchange rates.
 */
object OfflineCurrencyDebtConverterEngine {

    fun defaultManualRatesMap(): Map<ForeignDebtCurrency, Long> {
        return ForeignDebtCurrency.entries.associateWith { it.defaultManualRateToman }
    }

    fun defaultSampleForeignDebts(): List<ForeignCurrencyDebtPosition> {
        return listOf(
            ForeignCurrencyDebtPosition(
                id = 1L,
                titleFa = "بدهی خرید لپ‌تاپ و تجهیزات (دلاری)",
                counterpartyNameFa = "فروشگاه دیجیتال پایتخت",
                currency = ForeignDebtCurrency.USD,
                totalForeignAmount = 850.0,
                paidForeignAmount = 250.0,
                entryExchangeRateToman = 68_500L,
                dueDateJalali = "1405/09/25"
            ),
            ForeignCurrencyDebtPosition(
                id = 2L,
                titleFa = "قرض خانوادگی بر پایه طلای ۱۸ عیار",
                counterpartyNameFa = "صندوق قرض‌الحسنه خانواده",
                currency = ForeignDebtCurrency.GOLD_GRAM,
                totalForeignAmount = 4.500,
                paidForeignAmount = 1.000,
                entryExchangeRateToman = 4_350_000L,
                dueDateJalali = "1405/10/15"
            )
        )
    }

    fun convertForeignToToman(
        foreignAmount: Double,
        manualRateToman: Long
    ): Long {
        if (foreignAmount <= 0.0 || manualRateToman <= 0L) return 0L
        return (foreignAmount * manualRateToman.toDouble()).roundToLong().coerceAtLeast(0L)
    }

    fun convertTomanToForeign(
        amountToman: Long,
        manualRateToman: Long
    ): Double {
        if (amountToman <= 0L || manualRateToman <= 0L) return 0.0
        return amountToman.toDouble() / manualRateToman.toDouble()
    }

    fun convertBetweenCurrencies(
        amount: Double,
        fromRateToman: Long,
        toRateToman: Long
    ): Double {
        if (amount <= 0.0 || fromRateToman <= 0L || toRateToman <= 0L) return 0.0
        val toman = amount * fromRateToman.toDouble()
        return toman / toRateToman.toDouble()
    }

    fun evaluateConversion(
        sourceCurrency: ForeignDebtCurrency,
        sourceForeignAmount: Double,
        manualRates: Map<ForeignDebtCurrency, Long>
    ): MultiCurrencyConversionResult {
        val safeAmount = sourceForeignAmount.coerceAtLeast(0.0)
        val sourceRate = (manualRates[sourceCurrency] ?: sourceCurrency.defaultManualRateToman).coerceAtLeast(1L)
        val equivalentToman = convertForeignToToman(safeAmount, sourceRate)
        val equivalentRial = equivalentToman * 10L

        val crossRows = ForeignDebtCurrency.entries.map { targetCurrency ->
            val targetRate = (manualRates[targetCurrency] ?: targetCurrency.defaultManualRateToman).coerceAtLeast(1L)
            val converted = if (targetCurrency == sourceCurrency) {
                safeAmount
            } else {
                convertBetweenCurrencies(safeAmount, sourceRate, targetRate)
            }
            CrossCurrencyConversionRow(
                currency = targetCurrency,
                manualRateToman = targetRate,
                convertedForeignAmount = converted,
                formattedForeignAmountFa = formatForeignAmount(converted, targetCurrency)
            )
        }

        return MultiCurrencyConversionResult(
            sourceCurrency = sourceCurrency,
            sourceForeignAmount = safeAmount,
            manualRateToman = sourceRate,
            equivalentToman = equivalentToman,
            equivalentRial = equivalentRial,
            crossConversions = crossRows
        )
    }

    fun evaluatePortfolioSummary(
        positions: List<ForeignCurrencyDebtPosition>,
        manualRates: Map<ForeignDebtCurrency, Long>,
        domesticRemainingDebtToman: Long = 0L
    ): MultiCurrencyDebtPortfolioSummary {
        val normalizedRates = ForeignDebtCurrency.entries.associateWith { curr ->
            (manualRates[curr] ?: curr.defaultManualRateToman).coerceAtLeast(1L)
        }

        val evaluated = positions.map { pos ->
            val currentRate = normalizedRates[pos.currency] ?: pos.currency.defaultManualRateToman
            val entryRate = pos.entryExchangeRateToman.coerceAtLeast(1L)
            val remForeign = pos.remainingForeignAmount
            val currentToman = convertForeignToToman(remForeign, currentRate)
            val entryToman = convertForeignToToman(remForeign, entryRate)
            val deltaToman = currentToman - entryToman
            val pctChange = if (entryRate > 0L) {
                (((currentRate - entryRate).toDouble() / entryRate.toDouble()) * 100.0).roundToInt()
            } else {
                0
            }

            EvaluatedForeignDebtPosition(
                position = pos,
                currentManualRateToman = currentRate,
                remainingTomanAtCurrentRate = currentToman,
                remainingTomanAtEntryRate = entryToman,
                fxDeltaToman = deltaToman,
                fxChangePercent = pctChange
            )
        }

        val activeEvaluated = evaluated.filter { !it.position.isSettled }
        val totalCurrentToman = activeEvaluated.sumOf { it.remainingTomanAtCurrentRate }
        val totalEntryToman = activeEvaluated.sumOf { it.remainingTomanAtEntryRate }
        val totalFxDelta = totalCurrentToman - totalEntryToman
        val safeDomestic = domesticRemainingDebtToman.coerceAtLeast(0L)

        return MultiCurrencyDebtPortfolioSummary(
            manualRates = normalizedRates,
            evaluatedPositions = evaluated,
            totalForeignDebtEquivalentToman = totalCurrentToman,
            totalForeignDebtEntryToman = totalEntryToman,
            totalFxDeltaToman = totalFxDelta,
            domesticRemainingDebtToman = safeDomestic,
            combinedGrandTotalDebtToman = safeDomestic + totalCurrentToman,
            activePositionsCount = activeEvaluated.size
        )
    }

    fun formatForeignAmount(
        amount: Double,
        currency: ForeignDebtCurrency,
        includeUnit: Boolean = true,
        usePersianDigits: Boolean = true
    ): String {
        val pattern = when (currency.decimalPlaces) {
            0 -> "#,###"
            3 -> "#,##0.###"
            else -> "#,##0.##"
        }
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
        val raw = DecimalFormat(pattern, symbols).format(amount.coerceAtLeast(0.0))
        val localized = if (usePersianDigits) {
            PersianCurrencyEngine.toPersianDigits(raw).replace('.', '٫')
        } else {
            raw
        }
        return if (includeUnit) "$localized ${currency.shortLabelFa}" else localized
    }

    fun formatExchangeRate(
        rateToman: Long,
        displayUnit: CurrencyUnit = CurrencyUnit.TOMAN,
        privacyMasked: Boolean = false
    ): String {
        return PersianCurrencyEngine.formatAmount(
            amountToman = rateToman.coerceAtLeast(0L),
            unit = displayUnit,
            includeUnit = true,
            privacyMasked = privacyMasked
        )
    }

    fun serializeRatesToJson(rates: Map<ForeignDebtCurrency, Long>): String {
        return ForeignDebtCurrency.entries.joinToString(separator = ";") { currency ->
            val rate = (rates[currency] ?: currency.defaultManualRateToman).coerceAtLeast(1L)
            "${currency.code}:$rate"
        }
    }

    fun deserializeRatesFromJson(serialized: String?): Map<ForeignDebtCurrency, Long> {
        if (serialized.isNullOrBlank()) return defaultManualRatesMap()
        return try {
            val parsedMap = mutableMapOf<String, Long>()
            val cleaned = serialized.trim().removePrefix("{").removeSuffix("}")
            val pairs = if (cleaned.contains(";")) cleaned.split(";") else cleaned.split(",")
            for (pair in pairs) {
                val parts = pair.split(":")
                if (parts.size == 2) {
                    val key = parts[0].trim().removeSurrounding("\"")
                    val value = parts[1].trim().removeSurrounding("\"").toLongOrNull()
                    if (value != null && value > 0L) {
                        parsedMap[key.uppercase(Locale.US)] = value
                    }
                }
            }
            ForeignDebtCurrency.entries.associateWith { currency ->
                parsedMap[currency.code.uppercase(Locale.US)] ?: currency.defaultManualRateToman
            }
        } catch (_: Exception) {
            defaultManualRatesMap()
        }
    }

    private fun escapeField(raw: String): String {
        return raw.replace("|", " ").replace("\n", " ").trim()
    }

    fun serializePositionsToJson(positions: List<ForeignCurrencyDebtPosition>): String {
        if (positions.isEmpty()) return "EMPTY"
        return positions.joinToString(separator = "\n") { pos ->
            listOf(
                pos.id.toString(),
                escapeField(pos.titleFa),
                escapeField(pos.counterpartyNameFa),
                pos.currency.code,
                pos.totalForeignAmount.toString(),
                pos.paidForeignAmount.toString(),
                pos.entryExchangeRateToman.toString(),
                escapeField(pos.dueDateJalali)
            ).joinToString(separator = "|")
        }
    }

    fun deserializePositionsFromJson(serialized: String?): List<ForeignCurrencyDebtPosition> {
        if (serialized == null) return defaultSampleForeignDebts()
        if (serialized.isBlank() || serialized == "EMPTY") return emptyList()
        return try {
            val lines = serialized.lines().filter { it.isNotBlank() }
            val result = mutableListOf<ForeignCurrencyDebtPosition>()
            for ((idx, line) in lines.withIndex()) {
                val parts = line.split("|")
                if (parts.size >= 8) {
                    val currency = ForeignDebtCurrency.fromCode(parts[3])
                    result += ForeignCurrencyDebtPosition(
                        id = parts[0].toLongOrNull() ?: (idx + 1L),
                        titleFa = parts[1].ifBlank { "بدهی ارزی" },
                        counterpartyNameFa = parts[2].ifBlank { "طرف‌حساب" },
                        currency = currency,
                        totalForeignAmount = (parts[4].toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                        paidForeignAmount = (parts[5].toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                        entryExchangeRateToman = (parts[6].toLongOrNull() ?: currency.defaultManualRateToman)
                            .coerceAtLeast(1L),
                        dueDateJalali = parts[7].ifBlank { "1405/10/15" }
                    )
                }
            }
            if (result.isEmpty() && serialized != "EMPTY") defaultSampleForeignDebts() else result
        } catch (_: Exception) {
            defaultSampleForeignDebts()
        }
    }
}
