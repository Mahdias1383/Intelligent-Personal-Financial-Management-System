package com.example.domain

import com.example.core.JalaliCalendarEngine
import com.example.data.PaymentInterval
import kotlin.math.pow
import kotlin.math.roundToLong

enum class AmortizationFormulaMode(val labelFa: String, val subtitleFa: String) {
    COMPOUND_ANNUITY("فرمول بانکی (کاهش مانده)", "سود روی مانده بدهی هر ماه"),
    FLAT_SIMPLE("کارمزد ثابت (قرض‌الحسنه/بازاری)", "کارمزد سالانه روی کل اصل وام")
}

data class AmortizationInstallmentRow(
    val installmentNumber: Int,
    val dueDateJalali: String,
    val dueDateShortFa: String,
    val monthYearLabelFa: String,
    val paymentAmountToman: Long,
    val principalPortionToman: Long,
    val interestPortionToman: Long,
    val remainingPrincipalAfterToman: Long
)

data class DebtAmortizationResult(
    val principalToman: Long,
    val annualInterestRatePercent: Double,
    val durationMonths: Int,
    val startDateJalali: String,
    val endDateJalali: String,
    val formulaMode: AmortizationFormulaMode,
    val monthlyPaymentToman: Long,
    val totalPayableToman: Long,
    val totalInterestToman: Long,
    val interestToTotalRatio: Float,
    val principalToTotalRatio: Float,
    val schedule: List<AmortizationInstallmentRow>
)

/**
 * Pure domain calculator for Loan & Debt Amortization Schedules using the Shamsi (Jalali) calendar utility.
 *
 * Supports both:
 * 1. Standard Reducing-Balance Compound Annuity (`COMPOUND_ANNUITY`):
 *    M = P * [r * (1 + r)^n] / [(1 + r)^n - 1], where r = (annualRate / 100) / 12.
 * 2. Flat Simple Annual Fee / Qarz-al-Hasaneh (`FLAT_SIMPLE`):
 *    Total Interest = P * (annualRate / 100) * (n / 12.0).
 *
 * Every installment due date is computed strictly via [JalaliCalendarEngine.generateInstallmentDueDates]
 * so 31-day spring/summer months, 30-day autumn/winter months, and 29/30-day Esfand leap rules are respected.
 */
object DebtAmortizationCalculatorEngine {

    fun calculateAmortization(
        principalToman: Long,
        annualInterestRatePercent: Double,
        durationMonths: Int,
        startDateJalali: String = JalaliCalendarEngine.firstOfNextJalaliMonth(JalaliCalendarEngine.todayJalali()),
        formulaMode: AmortizationFormulaMode = AmortizationFormulaMode.COMPOUND_ANNUITY
    ): DebtAmortizationResult {
        val safePrincipal = principalToman.coerceAtLeast(0L)
        val safeRate = annualInterestRatePercent.coerceIn(0.0, 200.0)
        val safeMonths = durationMonths.coerceIn(1, 360)
        val normalizedStart = JalaliCalendarEngine.normalizeJalaliDate(startDateJalali)

        val dueDates = JalaliCalendarEngine.generateInstallmentDueDates(
            firstDueDateJalali = normalizedStart,
            installmentCount = safeMonths,
            interval = PaymentInterval.MONTHLY
        )

        if (safePrincipal == 0L) {
            val emptyRows = dueDates.mapIndexed { idx, dateStr ->
                val parsed = JalaliCalendarEngine.parseJalali(dateStr)
                AmortizationInstallmentRow(
                    installmentNumber = idx + 1,
                    dueDateJalali = dateStr,
                    dueDateShortFa = JalaliCalendarEngine.formatJalaliShortText(dateStr),
                    monthYearLabelFa = JalaliCalendarEngine.formatJalaliMonthYear(parsed.year, parsed.month),
                    paymentAmountToman = 0L,
                    principalPortionToman = 0L,
                    interestPortionToman = 0L,
                    remainingPrincipalAfterToman = 0L
                )
            }
            return DebtAmortizationResult(
                principalToman = 0L,
                annualInterestRatePercent = safeRate,
                durationMonths = safeMonths,
                startDateJalali = dueDates.first(),
                endDateJalali = dueDates.last(),
                formulaMode = formulaMode,
                monthlyPaymentToman = 0L,
                totalPayableToman = 0L,
                totalInterestToman = 0L,
                interestToTotalRatio = 0f,
                principalToTotalRatio = 1f,
                schedule = emptyRows
            )
        }

        val rows = mutableListOf<AmortizationInstallmentRow>()

        when {
            safeRate <= 1e-6 -> {
                // Zero-interest loan: exact division with remainder on the last installment
                val basePrincipal = safePrincipal / safeMonths
                val remainder = safePrincipal % safeMonths
                var remainingPrincipal = safePrincipal

                for (i in 0 until safeMonths) {
                    val dateStr = dueDates[i]
                    val parsed = JalaliCalendarEngine.parseJalali(dateStr)
                    val principalPart = if (i == safeMonths - 1) basePrincipal + remainder else basePrincipal
                    remainingPrincipal = (remainingPrincipal - principalPart).coerceAtLeast(0L)

                    rows.add(
                        AmortizationInstallmentRow(
                            installmentNumber = i + 1,
                            dueDateJalali = dateStr,
                            dueDateShortFa = JalaliCalendarEngine.formatJalaliShortText(dateStr),
                            monthYearLabelFa = JalaliCalendarEngine.formatJalaliMonthYear(parsed.year, parsed.month),
                            paymentAmountToman = principalPart,
                            principalPortionToman = principalPart,
                            interestPortionToman = 0L,
                            remainingPrincipalAfterToman = remainingPrincipal
                        )
                    )
                }
            }

            formulaMode == AmortizationFormulaMode.FLAT_SIMPLE -> {
                val totalInterest = (safePrincipal.toDouble() * (safeRate / 100.0) * (safeMonths.toDouble() / 12.0))
                    .roundToLong()
                    .coerceAtLeast(0L)
                val basePrincipal = safePrincipal / safeMonths
                val principalRemainder = safePrincipal % safeMonths
                val baseInterest = totalInterest / safeMonths
                val interestRemainder = totalInterest % safeMonths
                var remainingPrincipal = safePrincipal

                for (i in 0 until safeMonths) {
                    val dateStr = dueDates[i]
                    val parsed = JalaliCalendarEngine.parseJalali(dateStr)
                    val isLast = (i == safeMonths - 1)
                    val principalPart = if (isLast) basePrincipal + principalRemainder else basePrincipal
                    val interestPart = if (isLast) baseInterest + interestRemainder else baseInterest
                    val payment = principalPart + interestPart
                    remainingPrincipal = (remainingPrincipal - principalPart).coerceAtLeast(0L)

                    rows.add(
                        AmortizationInstallmentRow(
                            installmentNumber = i + 1,
                            dueDateJalali = dateStr,
                            dueDateShortFa = JalaliCalendarEngine.formatJalaliShortText(dateStr),
                            monthYearLabelFa = JalaliCalendarEngine.formatJalaliMonthYear(parsed.year, parsed.month),
                            paymentAmountToman = payment,
                            principalPortionToman = principalPart,
                            interestPortionToman = interestPart,
                            remainingPrincipalAfterToman = remainingPrincipal
                        )
                    )
                }
            }

            else -> {
                // Standard Reducing-Balance Compound Annuity formula
                val monthlyRate = (safeRate / 100.0) / 12.0
                val factor = (1.0 + monthlyRate).pow(safeMonths.toDouble())
                val standardMonthlyPayment = (safePrincipal.toDouble() * (monthlyRate * factor) / (factor - 1.0))
                    .roundToLong()
                    .coerceAtLeast(1L)

                var remainingPrincipal = safePrincipal

                for (i in 0 until safeMonths) {
                    val dateStr = dueDates[i]
                    val parsed = JalaliCalendarEngine.parseJalali(dateStr)
                    val isLast = (i == safeMonths - 1)
                    val interestPart = (remainingPrincipal.toDouble() * monthlyRate).roundToLong().coerceAtLeast(0L)

                    val principalPart: Long
                    val payment: Long
                    if (isLast) {
                        principalPart = remainingPrincipal
                        payment = principalPart + interestPart
                    } else {
                        principalPart = (standardMonthlyPayment - interestPart).coerceIn(0L, remainingPrincipal)
                        payment = principalPart + interestPart
                    }
                    remainingPrincipal = (remainingPrincipal - principalPart).coerceAtLeast(0L)

                    rows.add(
                        AmortizationInstallmentRow(
                            installmentNumber = i + 1,
                            dueDateJalali = dateStr,
                            dueDateShortFa = JalaliCalendarEngine.formatJalaliShortText(dateStr),
                            monthYearLabelFa = JalaliCalendarEngine.formatJalaliMonthYear(parsed.year, parsed.month),
                            paymentAmountToman = payment,
                            principalPortionToman = principalPart,
                            interestPortionToman = interestPart,
                            remainingPrincipalAfterToman = remainingPrincipal
                        )
                    )
                }
            }
        }

        val totalPayable = rows.sumOf { it.paymentAmountToman }
        val totalInterest = rows.sumOf { it.interestPortionToman }
        val monthlyFirstPayment = rows.firstOrNull()?.paymentAmountToman ?: 0L
        val safeTotal = totalPayable.coerceAtLeast(1L).toFloat()
        val interestRatio = (totalInterest.toFloat() / safeTotal).coerceIn(0f, 1f)
        val principalRatio = (1f - interestRatio).coerceIn(0f, 1f)

        return DebtAmortizationResult(
            principalToman = safePrincipal,
            annualInterestRatePercent = safeRate,
            durationMonths = safeMonths,
            startDateJalali = dueDates.first(),
            endDateJalali = dueDates.last(),
            formulaMode = formulaMode,
            monthlyPaymentToman = monthlyFirstPayment,
            totalPayableToman = totalPayable,
            totalInterestToman = totalInterest,
            interestToTotalRatio = interestRatio,
            principalToTotalRatio = principalRatio,
            schedule = rows
        )
    }
}
