package com.example.domain

import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.ObligationEntity
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Baseline portfolio parameters derived from the user's live obligations & installments.
 */
data class PortfolioDebtBaseline(
    val totalRemainingDebtToman: Long,
    val baseMonthlyPaymentToman: Long,
    val weightedAnnualRatePercent: Double,
    val activeDebtsCount: Int
)

/**
 * Single monthly point on the dual Baseline vs. Accelerated payoff trajectory chart.
 */
data class PayoffTrajectoryPoint(
    val monthIndex: Int,
    val shamsiDateJalali: String,
    val shamsiMonthLabelFa: String,
    val baselineRemainingToman: Long,
    val acceleratedRemainingToman: Long
)

/**
 * Precomputed What-If comparison row for quick slider presets (e.g., +1M, +3M, +5M, +10M/month).
 */
data class WhatIfExtraStepComparison(
    val extraMonthlyToman: Long,
    val monthsToDebtFree: Int,
    val monthsSaved: Int,
    val interestSavedToman: Long,
    val debtFreeShamsiMonthLabelFa: String
)

/**
 * Complete analytical result from the interactive slider-based Debt-Free Accelerator calculator.
 */
data class DebtFreeAccelerationReport(
    val startDateJalali: String,
    val totalRemainingDebtToman: Long,
    val baseMonthlyPaymentToman: Long,
    val extraMonthlyPaymentToman: Long,
    val oneTimeLumpSumBoostToman: Long,
    val effectiveAnnualRatePercent: Double,
    val newTotalMonthlyPaymentToman: Long,
    val baselineMonthsToDebtFree: Int,
    val acceleratedMonthsToDebtFree: Int,
    val monthsSaved: Int,
    val approxDaysSaved: Int,
    val accelerationPercent: Int,
    val baselineDebtFreeDateJalali: String,
    val baselineDebtFreeMonthLabelFa: String,
    val acceleratedDebtFreeDateJalali: String,
    val acceleratedDebtFreeMonthLabelFa: String,
    val baselineTotalInterestToman: Long,
    val acceleratedTotalInterestToman: Long,
    val totalInterestSavedToman: Long,
    val trajectoryPoints: List<PayoffTrajectoryPoint>,
    val quickStepComparisons: List<WhatIfExtraStepComparison>,
    val coachingHeadlineFa: String,
    val coachingDetailFa: String
)

/**
 * Pure domain simulation engine for the interactive slider-based **Debt-Free Accelerator Calculator**.
 *
 * Models how paying recurring extra amounts each month (and an optional one-time lump-sum boost)
 * compresses the debt-free timeline, reduces total interest cost, and shifts the user's exact
 * Persian (Shamsi / Jalali) debt-free date forward.
 */
object DebtFreeAcceleratorEngine {

    private const val MAX_SIMULATION_MONTHS = 360

    /**
     * Derives realistic baseline debt balance, monthly payment commitment, and weighted APR
     * from the user's live active debt obligations and unpaid installments (excluding receivables).
     */
    fun derivePortfolioBaseline(
        obligations: List<ObligationEntity>,
        installments: List<InstallmentEntity>
    ): PortfolioDebtBaseline {
        val debtObs = obligations.filter { !it.categoryEnum.isReceivable }
        val instByOb = installments.groupBy { it.obligationId }

        var totalRemaining = 0L
        var totalMonthlyBase = 0L
        var weightedRateNumerator = 0.0
        var activeCount = 0

        debtObs.forEach { ob ->
            val obInsts = instByOb[ob.id].orEmpty()
            val unpaid = obInsts.filter {
                it.statusEnum != InstallmentStatus.PAID && it.remainingToman > 0L
            }
            val rem = unpaid.sumOf { it.remainingToman }
            if (rem <= 0L) return@forEach

            activeCount += 1
            totalRemaining += rem

            val avgMonthly = (rem / unpaid.size.coerceAtLeast(1))
                .coerceAtLeast(250_000L)
                .coerceAtMost(rem)
            totalMonthlyBase += avgMonthly

            val totalPayable = obInsts.sumOf { it.effectiveTotalToman }
            val inferredApr = if (ob.principalAmountToman > 0L && totalPayable > ob.principalAmountToman) {
                val profitRatio = (totalPayable - ob.principalAmountToman).toDouble() /
                    ob.principalAmountToman.toDouble()
                val years = (obInsts.size.coerceAtLeast(1) / 12.0).coerceAtLeast(0.5)
                ((profitRatio / years) * 100.0).coerceIn(0.0, 48.0)
            } else {
                0.0
            }
            weightedRateNumerator += inferredApr * rem.toDouble()
        }

        val weightedApr = if (totalRemaining > 0L) {
            ((weightedRateNumerator / totalRemaining.toDouble()) * 10.0).roundToInt() / 10.0
        } else {
            0.0
        }

        return PortfolioDebtBaseline(
            totalRemainingDebtToman = totalRemaining,
            baseMonthlyPaymentToman = totalMonthlyBase,
            weightedAnnualRatePercent = weightedApr,
            activeDebtsCount = activeCount
        )
    }

    /**
     * Simulates Baseline vs. Accelerated monthly amortization curves for a given debt balance,
     * recurring base payment, annual interest rate, recurring extra monthly payment,
     * and optional one-time lump-sum boost.
     */
    fun simulateAcceleration(
        totalRemainingDebtToman: Long,
        baseMonthlyPaymentToman: Long,
        extraMonthlyPaymentToman: Long,
        annualInterestRatePercent: Double = 0.0,
        oneTimeLumpSumBoostToman: Long = 0L,
        startDateJalali: String = JalaliCalendarEngine.todayJalali(),
        includeComparisons: Boolean = true
    ): DebtFreeAccelerationReport {
        val safeDebt = totalRemainingDebtToman.coerceAtLeast(0L)
        val safeRate = annualInterestRatePercent.coerceIn(0.0, 60.0)
        val safeExtra = extraMonthlyPaymentToman.coerceAtLeast(0L)
        val safeLump = oneTimeLumpSumBoostToman.coerceIn(0L, safeDebt)

        val parsedStart = JalaliCalendarEngine.parseJalali(startDateJalali)
        val startMonthLabel = JalaliCalendarEngine.formatJalaliMonthYear(parsedStart.year, parsedStart.month)

        if (safeDebt <= 0L) {
            return DebtFreeAccelerationReport(
                startDateJalali = startDateJalali,
                totalRemainingDebtToman = 0L,
                baseMonthlyPaymentToman = 0L,
                extraMonthlyPaymentToman = safeExtra,
                oneTimeLumpSumBoostToman = 0L,
                effectiveAnnualRatePercent = safeRate,
                newTotalMonthlyPaymentToman = safeExtra,
                baselineMonthsToDebtFree = 0,
                acceleratedMonthsToDebtFree = 0,
                monthsSaved = 0,
                approxDaysSaved = 0,
                accelerationPercent = 0,
                baselineDebtFreeDateJalali = startDateJalali,
                baselineDebtFreeMonthLabelFa = startMonthLabel,
                acceleratedDebtFreeDateJalali = startDateJalali,
                acceleratedDebtFreeMonthLabelFa = startMonthLabel,
                baselineTotalInterestToman = 0L,
                acceleratedTotalInterestToman = 0L,
                totalInterestSavedToman = 0L,
                trajectoryPoints = emptyList(),
                quickStepComparisons = emptyList(),
                coachingHeadlineFa = "شما در حال حاضر بدون بدهی فعال هستید!",
                coachingDetailFa = "برای تست سناریوهای فرضی، روی «تنظیم دستی با اسلایدر» بزنید و مبلغ بدهی دلخواه را شبیه‌سازی کنید."
            )
        }

        // Guarantee minimum payment covers first month's interest + meaningful principal reduction
        val firstMonthInterest = ((safeDebt * ((safeRate / 100.0) / 12.0)).roundToLong()).coerceAtLeast(0L)
        val safeBaseMonthly = baseMonthlyPaymentToman
            .coerceAtLeast(firstMonthInterest + 250_000L)
            .coerceAtMost(safeDebt + firstMonthInterest)

        val baselineSim = runSinglePathSimulation(
            startingBalance = safeDebt,
            monthlyPayment = safeBaseMonthly,
            annualRatePercent = safeRate,
            initialLumpSum = 0L
        )

        val acceleratedSim = runSinglePathSimulation(
            startingBalance = safeDebt,
            monthlyPayment = safeBaseMonthly + safeExtra,
            annualRatePercent = safeRate,
            initialLumpSum = safeLump
        )

        val baselineMonths = baselineSim.monthsToClear
        val acceleratedMonths = acceleratedSim.monthsToClear
        val monthsSaved = (baselineMonths - acceleratedMonths).coerceAtLeast(0)
        val approxDaysSaved = monthsSaved * 30
        val accelerationPct = if (baselineMonths > 0) {
            ((monthsSaved.toDouble() / baselineMonths.toDouble()) * 100.0)
                .roundToInt()
                .coerceIn(0, 100)
        } else {
            0
        }

        val baselineEndDate = JalaliCalendarEngine.addJalaliMonths(startDateJalali, baselineMonths)
        val parsedBaselineEnd = JalaliCalendarEngine.parseJalali(baselineEndDate)
        val baselineEndLabel = JalaliCalendarEngine.formatJalaliMonthYear(parsedBaselineEnd.year, parsedBaselineEnd.month)

        val acceleratedEndDate = JalaliCalendarEngine.addJalaliMonths(startDateJalali, acceleratedMonths)
        val parsedAccelEnd = JalaliCalendarEngine.parseJalali(acceleratedEndDate)
        val acceleratedEndLabel = JalaliCalendarEngine.formatJalaliMonthYear(parsedAccelEnd.year, parsedAccelEnd.month)

        val interestSaved = (baselineSim.totalInterestToman - acceleratedSim.totalInterestToman).coerceAtLeast(0L)

        // Build synchronized dual trajectory points from Month 0..baselineMonths
        val totalPlotMonths = maxOf(baselineMonths, acceleratedMonths, 1)
        val trajectory = (0..totalPlotMonths).map { m ->
            val dateAtM = if (m == 0) startDateJalali else JalaliCalendarEngine.addJalaliMonths(startDateJalali, m)
            val parsedM = JalaliCalendarEngine.parseJalali(dateAtM)
            val labelM = JalaliCalendarEngine.formatJalaliMonthYear(parsedM.year, parsedM.month)
            val baseBal = baselineSim.balancesByMonth.getOrElse(m) { 0L }
            val accelBal = acceleratedSim.balancesByMonth.getOrElse(m) { 0L }
            PayoffTrajectoryPoint(
                monthIndex = m,
                shamsiDateJalali = dateAtM,
                shamsiMonthLabelFa = labelM,
                baselineRemainingToman = baseBal,
                acceleratedRemainingToman = accelBal
            )
        }

        val comparisons = if (includeComparisons) {
            listOf(1_000_000L, 3_000_000L, 5_000_000L, 10_000_000L).map { presetExtra ->
                val simPreset = runSinglePathSimulation(
                    startingBalance = safeDebt,
                    monthlyPayment = safeBaseMonthly + presetExtra,
                    annualRatePercent = safeRate,
                    initialLumpSum = safeLump
                )
                val endDate = JalaliCalendarEngine.addJalaliMonths(startDateJalali, simPreset.monthsToClear)
                val parsed = JalaliCalendarEngine.parseJalali(endDate)
                WhatIfExtraStepComparison(
                    extraMonthlyToman = presetExtra,
                    monthsToDebtFree = simPreset.monthsToClear,
                    monthsSaved = (baselineMonths - simPreset.monthsToClear).coerceAtLeast(0),
                    interestSavedToman = (baselineSim.totalInterestToman - simPreset.totalInterestToman).coerceAtLeast(0L),
                    debtFreeShamsiMonthLabelFa = JalaliCalendarEngine.formatJalaliMonthYear(parsed.year, parsed.month)
                )
            }
        } else {
            emptyList()
        }

        val headlineFa = when {
            safeExtra == 0L && safeLump == 0L ->
                "اسلایدر پرداخت اضافه را حرکت دهید تا تاریخ آزادی شما جلو بیفتد"
            monthsSaved > 0 ->
                "${PersianCurrencyEngine.toPersianDigits(monthsSaved.toString())} ماه زودتر به آزادی کامل مالی می‌رسید (${PersianCurrencyEngine.toPersianDigits(accelerationPct.toString())}٪ سریع‌تر)!"
            else ->
                "پرداخت اضافه باعث کاهش مانده و سبک‌تر شدن آخرین قسط شما می‌شود"
        }

        val detailFa = when {
            safeExtra == 0L && safeLump == 0L ->
                "با روند فعلی (${PersianCurrencyEngine.formatAmount(safeBaseMonthly)} در ماه)، تاریخ آزادی شما «$baselineEndLabel» خواهد بود."
            interestSaved > 0L ->
                "تاریخ تسویه کامل از «$baselineEndLabel» به «$acceleratedEndLabel» جلو می‌افتد و حدود ${PersianCurrencyEngine.formatAmount(interestSaved)} در هزینه‌های سود صرفه‌جویی می‌کنید."
            else ->
                "با پرداخت ماهانه ${PersianCurrencyEngine.formatAmount(safeBaseMonthly + safeExtra)}، تمامی بدهی‌ها به‌جای «$baselineEndLabel» در «$acceleratedEndLabel» به صفر می‌رسند."
        }

        return DebtFreeAccelerationReport(
            startDateJalali = startDateJalali,
            totalRemainingDebtToman = safeDebt,
            baseMonthlyPaymentToman = safeBaseMonthly,
            extraMonthlyPaymentToman = safeExtra,
            oneTimeLumpSumBoostToman = safeLump,
            effectiveAnnualRatePercent = safeRate,
            newTotalMonthlyPaymentToman = safeBaseMonthly + safeExtra,
            baselineMonthsToDebtFree = baselineMonths,
            acceleratedMonthsToDebtFree = acceleratedMonths,
            monthsSaved = monthsSaved,
            approxDaysSaved = approxDaysSaved,
            accelerationPercent = accelerationPct,
            baselineDebtFreeDateJalali = baselineEndDate,
            baselineDebtFreeMonthLabelFa = baselineEndLabel,
            acceleratedDebtFreeDateJalali = acceleratedEndDate,
            acceleratedDebtFreeMonthLabelFa = acceleratedEndLabel,
            baselineTotalInterestToman = baselineSim.totalInterestToman,
            acceleratedTotalInterestToman = acceleratedSim.totalInterestToman,
            totalInterestSavedToman = interestSaved,
            trajectoryPoints = trajectory,
            quickStepComparisons = comparisons,
            coachingHeadlineFa = headlineFa,
            coachingDetailFa = detailFa
        )
    }

    private data class SinglePathSimResult(
        val monthsToClear: Int,
        val totalInterestToman: Long,
        val balancesByMonth: List<Long>
    )

    private fun runSinglePathSimulation(
        startingBalance: Long,
        monthlyPayment: Long,
        annualRatePercent: Double,
        initialLumpSum: Long
    ): SinglePathSimResult {
        var balance = (startingBalance - initialLumpSum).coerceAtLeast(0L)
        val balances = mutableListOf<Long>()
        balances.add(balance)

        if (balance == 0L) {
            return SinglePathSimResult(
                monthsToClear = 0,
                totalInterestToman = 0L,
                balancesByMonth = balances
            )
        }

        val monthlyRate = (annualRatePercent / 100.0) / 12.0
        var months = 0
        var totalInterest = 0L

        while (balance > 0L && months < MAX_SIMULATION_MONTHS) {
            months += 1
            val interest = if (monthlyRate > 0.0) {
                (balance * monthlyRate).roundToLong().coerceAtLeast(0L)
            } else {
                0L
            }
            totalInterest += interest
            val withInterest = balance + interest
            val effectivePay = monthlyPayment.coerceAtLeast(interest + 100_000L)
            val paid = minOf(withInterest, effectivePay)
            balance = (withInterest - paid).coerceAtLeast(0L)
            balances.add(balance)
        }

        return SinglePathSimResult(
            monthsToClear = months,
            totalInterestToman = totalInterest,
            balancesByMonth = balances
        )
    }
}
