package com.example.domain

import com.example.core.JalaliCalendarEngine
import com.example.data.CounterpartyEntity
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.ObligationEntity
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Represents a single debt obligation entered into the Debt Snowball Calculator
 * (either imported automatically from the user's active portfolio or added manually for simulation).
 */
data class SnowballDebtAccount(
    val id: String,
    val titleFa: String,
    val counterpartyFa: String,
    val balanceToman: Long,
    val minimumMonthlyPaymentToman: Long,
    val annualInterestRatePercent: Double = 0.0,
    val categoryTag: DebtCategoryTag = DebtCategoryTag.PERSONAL,
    val isFromPortfolio: Boolean = true
)

/**
 * Prioritized payoff milestone for a single debt under the Debt Snowball strategy
 * (strictly ordered from smallest remaining balance to largest remaining balance).
 */
data class SnowballPayoffMilestone(
    val priorityRank: Int,
    val account: SnowballDebtAccount,
    val payoffMonthIndex: Int,
    val payoffShamsiDateJalali: String,
    val payoffShamsiMonthLabelFa: String,
    val totalPaidToman: Long,
    val totalInterestPaidToman: Long,
    val initialAttackPaymentToman: Long,
    val rolloverFreedMonthlyToman: Long,
    val cumulativeSnowballPowerToman: Long,
    val momentumBadgeFa: String,
    val psychologicalBoostNoteFa: String
)

/**
 * Month-by-month execution ledger step for the Debt Snowball payment schedule.
 */
data class SnowballMonthlyLedgerStep(
    val monthIndex: Int,
    val shamsiDateJalali: String,
    val shamsiMonthLabelFa: String,
    val focusDebtTitleFa: String,
    val totalPaymentThisMonthToman: Long,
    val snowballRolloverAppliedToman: Long,
    val interestAccruedThisMonthToman: Long,
    val remainingTotalDebtToman: Long,
    val clearedDebtTitlesThisMonth: List<String>,
    val cumulativeClearedCount: Int
)

/**
 * Complete output report of the Debt Snowball Calculator Engine.
 */
data class DebtSnowballPlanReport(
    val startDateJalali: String,
    val totalDebtsCount: Int,
    val totalStartingDebtToman: Long,
    val totalMinimumPaymentsToman: Long,
    val extraMonthlySnowballBudgetToman: Long,
    val totalMonthlyCommitmentToman: Long,
    val snowballMonthsToDebtFree: Int,
    val minimumOnlyMonthsToDebtFree: Int,
    val monthsSavedWithSnowball: Int,
    val totalInterestSnowballToman: Long,
    val totalInterestMinimumOnlyToman: Long,
    val interestSavedToman: Long,
    val debtFreeShamsiDateJalali: String,
    val debtFreeShamsiMonthLabelFa: String,
    val peakSnowballPowerToman: Long,
    val momentumScorePercent: Int,
    val momentumTierLabelFa: String,
    val firstQuickWinMilestone: SnowballPayoffMilestone?,
    val prioritizedMilestones: List<SnowballPayoffMilestone>,
    val monthlyLedger: List<SnowballMonthlyLedgerStep>,
    val actionableStrategySummaryFa: String
)

/**
 * Pure domain calculation engine for the **Debt Snowball Method**:
 *
 * 1. Prioritizes debts strictly from **smallest remaining balance to largest** so users achieve
 *    rapid early victories ("Quick Wins") and build strong psychological momentum.
 * 2. Pays the minimum monthly installment on all active debts while directing 100% of the user's
 *    extra monthly budget + all rolled-over minimum payments from already eliminated debts
 *    ("Snowball Rollover Power") toward the current smallest balance debt.
 * 3. Computes exact Persian (Shamsi / Jalali) payoff dates, months saved compared to paying
 *    minimums only, interest saved, and psychological momentum metrics.
 */
object DebtSnowballCalculatorEngine {

    private const val MAX_SIMULATION_MONTHS = 360

    /**
     * Converts the user's live database obligations & installments into active [SnowballDebtAccount]
     * entries, excluding receivables and fully paid obligations.
     */
    fun extractAccountsFromPortfolio(
        obligations: List<ObligationEntity>,
        installments: List<InstallmentEntity>,
        counterparties: List<CounterpartyEntity>,
        categoryOverrides: Map<Long, DebtCategoryTag> = emptyMap()
    ): List<SnowballDebtAccount> {
        val cpMap = counterparties.associateBy { it.id }
        val instByOb = installments.groupBy { it.obligationId }

        return obligations
            .filter { !it.categoryEnum.isReceivable }
            .mapNotNull { ob ->
                val obInsts = instByOb[ob.id].orEmpty()
                val unpaidInsts = obInsts.filter {
                    it.statusEnum != InstallmentStatus.PAID && it.remainingToman > 0L
                }
                val remainingBalance = unpaidInsts.sumOf { it.remainingToman }
                if (remainingBalance <= 0L) return@mapNotNull null

                val cp = cpMap[ob.counterpartyId]
                val tag = DebtTaggingAndCategorizationEngine.resolvePrimaryCategoryTag(
                    obligation = ob,
                    counterparty = cp,
                    explicitOverrideTagId = categoryOverrides[ob.id]?.id
                )

                // Derive realistic minimum monthly payment from the average remaining installment
                val avgInstallment = (remainingBalance / unpaidInsts.size.coerceAtLeast(1))
                    .coerceAtLeast(250_000L)
                    .coerceAtMost(remainingBalance)

                // Estimate effective annual rate if total payable exceeds principal
                val totalPayable = obInsts.sumOf { it.effectiveTotalToman }
                val inferredApr = if (ob.principalAmountToman > 0L && totalPayable > ob.principalAmountToman) {
                    val totalProfitRatio = (totalPayable - ob.principalAmountToman).toDouble() /
                        ob.principalAmountToman.toDouble()
                    val years = (obInsts.size.coerceAtLeast(1) / 12.0).coerceAtLeast(0.5)
                    ((totalProfitRatio / years) * 100.0).coerceIn(0.0, 48.0)
                } else {
                    0.0
                }

                SnowballDebtAccount(
                    id = "ob_${ob.id}",
                    titleFa = ob.title,
                    counterpartyFa = cp?.name?.substringBefore(" (") ?: tag.shortBadgeFa,
                    balanceToman = remainingBalance,
                    minimumMonthlyPaymentToman = avgInstallment,
                    annualInterestRatePercent = (inferredApr * 10.0).roundToInt() / 10.0,
                    categoryTag = tag,
                    isFromPortfolio = true
                )
            }
            .sortedWith(
                compareBy<SnowballDebtAccount> { it.balanceToman }
                    .thenBy { it.minimumMonthlyPaymentToman }
            )
    }

    /**
     * Generates a complete Debt Snowball payment plan strictly prioritizing smallest balances first,
     * rolling freed minimum payments forward each time a debt is eliminated.
     */
    fun calculateSnowballPlan(
        accounts: List<SnowballDebtAccount>,
        extraMonthlyBudgetToman: Long,
        startDateJalali: String = JalaliCalendarEngine.todayJalali()
    ): DebtSnowballPlanReport {
        val validAccounts = accounts
            .filter { it.balanceToman > 0L }
            .map { acc ->
                val safeMin = acc.minimumMonthlyPaymentToman
                    .coerceAtLeast(100_000L)
                    .coerceAtMost(acc.balanceToman)
                acc.copy(
                    minimumMonthlyPaymentToman = safeMin,
                    annualInterestRatePercent = acc.annualInterestRatePercent.coerceIn(0.0, 60.0)
                )
            }
            // Core Debt Snowball Rule: strictly smallest balance first!
            .sortedWith(
                compareBy<SnowballDebtAccount> { it.balanceToman }
                    .thenBy { it.minimumMonthlyPaymentToman }
                    .thenBy { it.titleFa }
            )

        val safeExtra = extraMonthlyBudgetToman.coerceAtLeast(0L)

        if (validAccounts.isEmpty()) {
            val parsedStart = JalaliCalendarEngine.parseJalali(startDateJalali)
            val startLabel = JalaliCalendarEngine.formatJalaliMonthYear(parsedStart.year, parsedStart.month)
            return DebtSnowballPlanReport(
                startDateJalali = startDateJalali,
                totalDebtsCount = 0,
                totalStartingDebtToman = 0L,
                totalMinimumPaymentsToman = 0L,
                extraMonthlySnowballBudgetToman = safeExtra,
                totalMonthlyCommitmentToman = safeExtra,
                snowballMonthsToDebtFree = 0,
                minimumOnlyMonthsToDebtFree = 0,
                monthsSavedWithSnowball = 0,
                totalInterestSnowballToman = 0L,
                totalInterestMinimumOnlyToman = 0L,
                interestSavedToman = 0L,
                debtFreeShamsiDateJalali = startDateJalali,
                debtFreeShamsiMonthLabelFa = startLabel,
                peakSnowballPowerToman = safeExtra,
                momentumScorePercent = 100,
                momentumTierLabelFa = "آزادی کامل مالی",
                firstQuickWinMilestone = null,
                prioritizedMilestones = emptyList(),
                monthlyLedger = emptyList(),
                actionableStrategySummaryFa = "هیچ بدهی فعالی برای ورود به برنامه گلوله برفی وجود ندارد. می‌توانید از بخش «شبیه‌سازی دستی» بدهی‌های فرضی خود را تست کنید."
            )
        }

        val totalStartingDebt = validAccounts.sumOf { it.balanceToman }
        val totalMinPayments = validAccounts.sumOf { it.minimumMonthlyPaymentToman }
        val totalMonthlyCommitment = totalMinPayments + safeExtra

        // 1. Run Baseline Minimum-Only simulation (no extra budget, no rollover snowball)
        val baselineOutcome = simulateMinimumOnlyBaseline(validAccounts)

        // 2. Run Full Debt Snowball simulation with rollover cascade
        val remainingBalances = validAccounts.map { it.balanceToman }.toMutableList()
        val totalPaidPerAccount = LongArray(validAccounts.size)
        val totalInterestPerAccount = LongArray(validAccounts.size)
        val payoffMonthPerAccount = IntArray(validAccounts.size) { -1 }
        val rolloverPowerAtClear = LongArray(validAccounts.size)

        val ledger = mutableListOf<SnowballMonthlyLedgerStep>()
        var currentMonth = 0
        var totalInterestSnowball = 0L
        var clearedCount = 0

        while (remainingBalances.any { it > 0L } && currentMonth < MAX_SIMULATION_MONTHS) {
            currentMonth += 1
            val monthDateJalali = JalaliCalendarEngine.addJalaliMonths(startDateJalali, currentMonth)
            val parsedDate = JalaliCalendarEngine.parseJalali(monthDateJalali)
            val monthLabelFa = JalaliCalendarEngine.formatJalaliMonthYear(parsedDate.year, parsedDate.month)

            // Identify the primary Snowball focus target (first active debt in smallest-balance order)
            val primaryFocusIndex = remainingBalances.indexOfFirst { it > 0L }
            val primaryFocusTitle = validAccounts[primaryFocusIndex].titleFa

            var monthInterestTotal = 0L
            var monthTotalPayment = 0L
            val clearedThisMonth = mutableListOf<String>()

            // Step A: Accrue monthly interest on every active debt
            for (i in validAccounts.indices) {
                val bal = remainingBalances[i]
                if (bal > 0L && validAccounts[i].annualInterestRatePercent > 0.0) {
                    val monthlyRate = (validAccounts[i].annualInterestRatePercent / 100.0) / 12.0
                    val interest = (bal * monthlyRate).roundToLong().coerceAtLeast(0L)
                    remainingBalances[i] = bal + interest
                    totalInterestPerAccount[i] += interest
                    monthInterestTotal += interest
                }
            }
            totalInterestSnowball += monthInterestTotal

            // Step B: Calculate total monthly budget available for this month
            // Under Debt Snowball, totalMonthlyCommitment stays constant (all minimums + extra budget),
            // so freed minimum payments from eliminated debts automatically roll into the Snowball Pool!
            var remainingPoolForSnowball = totalMonthlyCommitment

            // Pay minimum required on every active debt first
            for (i in validAccounts.indices) {
                val bal = remainingBalances[i]
                if (bal > 0L) {
                    val minPay = minOf(bal, validAccounts[i].minimumMonthlyPaymentToman)
                    val actualMinPay = minOf(minPay, remainingPoolForSnowball)
                    remainingBalances[i] = (bal - actualMinPay).coerceAtLeast(0L)
                    totalPaidPerAccount[i] += actualMinPay
                    monthTotalPayment += actualMinPay
                    remainingPoolForSnowball = (remainingPoolForSnowball - actualMinPay).coerceAtLeast(0L)
                }
            }

            // Step C: Apply 100% of Snowball Rollover Pool to the smallest remaining balance(s) in order
            val snowballAppliedThisMonth = remainingPoolForSnowball
            for (i in validAccounts.indices) {
                if (remainingPoolForSnowball <= 0L) break
                val bal = remainingBalances[i]
                if (bal > 0L) {
                    val extraAttack = minOf(bal, remainingPoolForSnowball)
                    remainingBalances[i] = (bal - extraAttack).coerceAtLeast(0L)
                    totalPaidPerAccount[i] += extraAttack
                    monthTotalPayment += extraAttack
                    remainingPoolForSnowball -= extraAttack
                }
            }

            // Step D: Record any debts completely eliminated in this month
            for (i in validAccounts.indices) {
                if (remainingBalances[i] == 0L && payoffMonthPerAccount[i] == -1) {
                    payoffMonthPerAccount[i] = currentMonth
                    clearedCount += 1
                    clearedThisMonth.add(validAccounts[i].titleFa)
                    val cumulativeFreedMins = validAccounts
                        .take(i + 1)
                        .sumOf { it.minimumMonthlyPaymentToman }
                    rolloverPowerAtClear[i] = safeExtra + cumulativeFreedMins
                }
            }

            val totalRemainingNow = remainingBalances.sum()
            ledger.add(
                SnowballMonthlyLedgerStep(
                    monthIndex = currentMonth,
                    shamsiDateJalali = monthDateJalali,
                    shamsiMonthLabelFa = monthLabelFa,
                    focusDebtTitleFa = primaryFocusTitle,
                    totalPaymentThisMonthToman = monthTotalPayment,
                    snowballRolloverAppliedToman = snowballAppliedThisMonth.coerceAtMost(monthTotalPayment),
                    interestAccruedThisMonthToman = monthInterestTotal,
                    remainingTotalDebtToman = totalRemainingNow,
                    clearedDebtTitlesThisMonth = clearedThisMonth,
                    cumulativeClearedCount = clearedCount
                )
            )
        }

        // Build prioritized milestones in strict smallest-balance order
        var runningRolloverBeforeDebt = safeExtra
        val milestones = validAccounts.mapIndexed { idx, acc ->
            val rank = idx + 1
            val payoffMonth = payoffMonthPerAccount[idx].let { if (it <= 0) currentMonth else it }
            val payoffDate = JalaliCalendarEngine.addJalaliMonths(startDateJalali, payoffMonth)
            val parsedPayoff = JalaliCalendarEngine.parseJalali(payoffDate)
            val payoffMonthLabel = JalaliCalendarEngine.formatJalaliMonthYear(parsedPayoff.year, parsedPayoff.month)

            val initialAttack = acc.minimumMonthlyPaymentToman + runningRolloverBeforeDebt
            val freedMin = acc.minimumMonthlyPaymentToman
            val cumulativePowerAfter = runningRolloverBeforeDebt + freedMin
            runningRolloverBeforeDebt = cumulativePowerAfter

            val badge = when {
                rank == 1 -> "پیروزی سریع اول (هدف شماره ۱) 🏆"
                rank == validAccounts.size -> "تسویه نهایی و آزادی کامل 🎯"
                rank == 2 -> "شتاب‌گیری گلوله برفی ⚡"
                else -> "مرحله $rank گلوله برفی ❄️"
            }

            val boostNote = when {
                rank == 1 -> "کوچک‌ترین مانده بدهی شماست؛ با حذف آن ظرف $payoffMonth ماه، انگیزه مضاعف می‌گیرید و حداقل قسط آن به بدهی بعدی منتقل می‌شود."
                rank == validAccounts.size -> "در این مرحله کل قدرت گلوله برفی متمرکز روی آخرین بدهی است تا کل پرونده‌ها به صفر برسد."
                else -> "با اضافه شدن اقساط آزادشده مراحل قبل، سرعت تسویه این بدهی به‌طور تصاعدی افزایش می‌یابد."
            }

            SnowballPayoffMilestone(
                priorityRank = rank,
                account = acc,
                payoffMonthIndex = payoffMonth,
                payoffShamsiDateJalali = payoffDate,
                payoffShamsiMonthLabelFa = payoffMonthLabel,
                totalPaidToman = totalPaidPerAccount[idx],
                totalInterestPaidToman = totalInterestPerAccount[idx],
                initialAttackPaymentToman = initialAttack,
                rolloverFreedMonthlyToman = freedMin,
                cumulativeSnowballPowerToman = cumulativePowerAfter,
                momentumBadgeFa = badge,
                psychologicalBoostNoteFa = boostNote
            )
        }

        val debtFreeDate = JalaliCalendarEngine.addJalaliMonths(startDateJalali, currentMonth)
        val parsedDebtFree = JalaliCalendarEngine.parseJalali(debtFreeDate)
        val debtFreeLabel = JalaliCalendarEngine.formatJalaliMonthYear(parsedDebtFree.year, parsedDebtFree.month)

        val monthsSaved = (baselineOutcome.monthsToClear - currentMonth).coerceAtLeast(0)
        val interestSaved = (baselineOutcome.totalInterestToman - totalInterestSnowball).coerceAtLeast(0L)
        val firstWin = milestones.firstOrNull()

        // Psychological Momentum Score (0..100):
        // Rewards fast time-to-first-win, meaningful snowball rollover ratio, and months saved
        val quickWinScore = when {
            firstWin == null -> 50
            firstWin.payoffMonthIndex <= 2 -> 50
            firstWin.payoffMonthIndex <= 4 -> 42
            firstWin.payoffMonthIndex <= 6 -> 34
            firstWin.payoffMonthIndex <= 10 -> 24
            else -> 16
        }
        val extraBoostScore = if (totalMinPayments > 0L) {
            ((safeExtra.toDouble() / totalMinPayments.toDouble()) * 35.0).roundToInt().coerceIn(0, 30)
        } else {
            15
        }
        val savingsBonus = (monthsSaved * 2).coerceIn(0, 20)
        val momentumScore = (quickWinScore + extraBoostScore + savingsBonus).coerceIn(25, 100)

        val momentumTier = when {
            momentumScore >= 80 -> "شتاب روانی فوق‌العاده 🔥"
            momentumScore >= 60 -> "انگیزه و شتاب عالی ⚡"
            else -> "شروع پایدار گلوله برفی ❄️"
        }

        val summaryFa = buildStrategySummaryFa(
            firstWin = firstWin,
            totalDebtsCount = validAccounts.size,
            snowballMonths = currentMonth,
            monthsSaved = monthsSaved,
            debtFreeLabel = debtFreeLabel
        )

        return DebtSnowballPlanReport(
            startDateJalali = startDateJalali,
            totalDebtsCount = validAccounts.size,
            totalStartingDebtToman = totalStartingDebt,
            totalMinimumPaymentsToman = totalMinPayments,
            extraMonthlySnowballBudgetToman = safeExtra,
            totalMonthlyCommitmentToman = totalMonthlyCommitment,
            snowballMonthsToDebtFree = currentMonth,
            minimumOnlyMonthsToDebtFree = baselineOutcome.monthsToClear,
            monthsSavedWithSnowball = monthsSaved,
            totalInterestSnowballToman = totalInterestSnowball,
            totalInterestMinimumOnlyToman = baselineOutcome.totalInterestToman,
            interestSavedToman = interestSaved,
            debtFreeShamsiDateJalali = debtFreeDate,
            debtFreeShamsiMonthLabelFa = debtFreeLabel,
            peakSnowballPowerToman = totalMonthlyCommitment,
            momentumScorePercent = momentumScore,
            momentumTierLabelFa = momentumTier,
            firstQuickWinMilestone = firstWin,
            prioritizedMilestones = milestones,
            monthlyLedger = ledger,
            actionableStrategySummaryFa = summaryFa
        )
    }

    private data class BaselineSimulationResult(
        val monthsToClear: Int,
        val totalInterestToman: Long
    )

    private fun simulateMinimumOnlyBaseline(accounts: List<SnowballDebtAccount>): BaselineSimulationResult {
        val balances = accounts.map { it.balanceToman }.toMutableList()
        var months = 0
        var totalInterest = 0L

        while (balances.any { it > 0L } && months < MAX_SIMULATION_MONTHS) {
            months += 1
            for (i in accounts.indices) {
                val bal = balances[i]
                if (bal <= 0L) continue
                val rate = accounts[i].annualInterestRatePercent
                val interest = if (rate > 0.0) {
                    (bal * ((rate / 100.0) / 12.0)).roundToLong().coerceAtLeast(0L)
                } else {
                    0L
                }
                totalInterest += interest
                val updated = bal + interest
                val minPayment = accounts[i].minimumMonthlyPaymentToman.coerceAtLeast(interest + 100_000L)
                balances[i] = (updated - minOf(updated, minPayment)).coerceAtLeast(0L)
            }
        }
        return BaselineSimulationResult(
            monthsToClear = months,
            totalInterestToman = totalInterest
        )
    }

    private fun buildStrategySummaryFa(
        firstWin: SnowballPayoffMilestone?,
        totalDebtsCount: Int,
        snowballMonths: Int,
        monthsSaved: Int,
        debtFreeLabel: String
    ): String {
        if (firstWin == null) return ""
        val savingsPart = if (monthsSaved > 0) {
            " و $monthsSaved ماه زودتر از پرداخت حداقلی"
        } else {
            ""
        }
        return "ابتدا تمام بودجه مازاد را روی کوچک‌ترین بدهی یعنی «${firstWin.account.titleFa}» متمرکز کنید تا در ماه ${firstWin.payoffMonthIndex} (${firstWin.payoffShamsiMonthLabelFa}) اولین پیروزی قطعی را ثبت کنید. با انتقال اقساط آزادشده به بدهی‌های بعدی، هر $totalDebtsCount بدهی ظرف $snowballMonths ماه (تا $debtFreeLabel$savingsPart) کاملاً صفر می‌شوند."
    }
}
