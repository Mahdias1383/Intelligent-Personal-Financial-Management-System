package com.example.domain

import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.data.CounterpartyEntity
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.ObligationCategory
import com.example.data.ObligationEntity
import com.example.data.TrustPriority
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Ranking objective for ordering existing debts in the Dashboard **Strategy** tab.
 */
enum class PayoffRankingMode(
    val labelFa: String,
    val shortLabelFa: String,
    val descriptionFa: String
) {
    INTEREST_AND_URGENCY(
        labelFa = "سود + فوریت (بهینه هوشمند)",
        shortLabelFa = "سود + فوریت",
        descriptionFa = "رتبه‌بندی ترکیبی بر اساس نرخ سود مؤثر و فوریت سررسید برای بیشترین صرفه‌جویی سود بدون جریمه دیرکرد"
    ),
    MAX_INTEREST_SAVINGS(
        labelFa = "بیشترین صرفه‌جویی سود (بهمن)",
        shortLabelFa = "بیشترین سود",
        descriptionFa = "اولویت مطلق با بالاترین نرخ سود سالانه (APR) برای حداقل کردن کل هزینه سود"
    ),
    URGENCY_FIRST(
        labelFa = "اولویت فوریت و سررسید",
        shortLabelFa = "فوریت سررسید",
        descriptionFa = "اولویت با اقساط معوقه، نزدیک‌ترین سررسید و تعهدات حیاتی (چک صیادی)"
    )
}

/**
 * Urgency bucket computed from overdue count, days until next due date, late fees, and trust priority.
 */
enum class DebtUrgencyLevel(
    val labelFa: String,
    val badgeFa: String
) {
    CRITICAL("بحرانی / معوقه یا چک فوری", "بحرانی"),
    HIGH("فوری (سررسید نزدیک)", "فوری"),
    MODERATE("نیاز به برنامه‌ریزی", "متوسط"),
    LOW("عادی / زمان کافی", "عادی")
}

/**
 * Single ranked debt entry in the recommended Pay-Off Order queue.
 */
data class RankedPayoffDebtItem(
    val rank: Int,
    val obligation: ObligationEntity,
    val counterparty: CounterpartyEntity?,
    val nextUnpaidInstallment: InstallmentEntity?,
    val categoryTag: DebtCategoryTag,
    val remainingBalanceToman: Long,
    val remainingPrincipalToman: Long,
    val remainingEmbeddedInterestToman: Long,
    val effectiveAnnualRatePercent: Double,
    val monthlyMinimumPaymentToman: Long,
    val remainingInstallmentCount: Int,
    val overdueInstallmentCount: Int,
    val activeLateFeesToman: Long,
    val nextDueDateJalali: String,
    val daysUntilNextDue: Int,
    val relativeDueLabelFa: String,
    val urgencyLevel: DebtUrgencyLevel,
    val interestRateScore: Int, // 0..55
    val urgencyScore: Int, // 0..45
    val compositePriorityScore: Int, // 0..100
    val projectedPayoffMonthIndex: Int,
    val projectedPayoffShamsiLabelFa: String,
    val estimatedInterestSavedToman: Long,
    val rationaleBadgesFa: List<String>,
    val actionGuidanceFa: String
)

/**
 * Complete automated ranking and multi-debt interest-savings report for the Dashboard **Strategy** tab.
 */
data class DebtPayoffStrategyReport(
    val rankingMode: PayoffRankingMode,
    val extraMonthlyBudgetToman: Long,
    val rankedDebts: List<RankedPayoffDebtItem>,
    val totalRemainingDebtToman: Long,
    val totalMinimumMonthlyToman: Long,
    val highestAprPercent: Double,
    val weightedAverageAprPercent: Double,
    val urgentDebtsCount: Int,
    val projectedDebtFreeMonths: Int,
    val projectedDebtFreeShamsiLabelFa: String,
    val baselineMinimumOnlyMonths: Int,
    val monthsSavedVsMinimumOnly: Int,
    val totalEstimatedInterestSavedVsBaselineToman: Long,
    val interestSavedByOrderVsReverseToman: Long,
    val topPriorityHeadlineFa: String,
    val strategyInsightFa: String
)

/**
 * Pure domain ranking and multi-debt amortization engine that automatically ranks existing debts
 * by **Effective Interest Rate (APR)** and **Urgency** (overdue status, imminent due date, late fees,
 * and trust priority), producing an actionable **Pay-Off Order** to maximize interest savings.
 */
object DebtPayoffStrategyRankingEngine {

    private const val MAX_SIM_MONTHS = 360

    fun buildStrategyReport(
        obligations: List<ObligationEntity>,
        installments: List<InstallmentEntity>,
        counterparties: List<CounterpartyEntity>,
        rankingMode: PayoffRankingMode = PayoffRankingMode.INTEREST_AND_URGENCY,
        extraMonthlyBudgetToman: Long = 0L,
        todayJalali: String = JalaliCalendarEngine.todayJalali()
    ): DebtPayoffStrategyReport {
        val safeExtra = extraMonthlyBudgetToman.coerceAtLeast(0L)
        val cpMap = counterparties.associateBy { it.id }
        val instByOb = installments.groupBy { it.obligationId }

        val parsedToday = JalaliCalendarEngine.parseJalali(todayJalali)
        val todayMonthLabel = JalaliCalendarEngine.formatJalaliMonthYear(parsedToday.year, parsedToday.month)

        // 1. Extract and score every active non-receivable debt
        val rawScored = obligations
            .filter { !it.categoryEnum.isReceivable }
            .mapNotNull { ob ->
                val allInsts = instByOb[ob.id].orEmpty()
                val unpaid = allInsts
                    .filter { it.statusEnum != InstallmentStatus.PAID && it.remainingToman > 0L }
                    .sortedBy { it.dueDateJalali }

                val remainingBalance = unpaid.sumOf { it.remainingToman }
                if (remainingBalance <= 0L) return@mapNotNull null

                val totalInstCount = allInsts.size.coerceAtLeast(ob.installmentCount.coerceAtLeast(1))
                val totalScheduled = allInsts.sumOf { it.effectiveTotalToman }
                    .coerceAtLeast(ob.netPayableToman)
                    .coerceAtLeast(remainingBalance)

                val financedPrincipal = (ob.principalAmountToman - ob.downPaymentToman).coerceAtLeast(1L)
                val totalProfit = maxOf(
                    ob.profitOrFeeToman,
                    (totalScheduled - financedPrincipal).coerceAtLeast(0L)
                )

                val remShare = (remainingBalance.toDouble() / totalScheduled.toDouble()).coerceIn(0.0, 1.0)
                val remInterest = (totalProfit * remShare)
                    .roundToLong()
                    .coerceIn(0L, (remainingBalance - 1L).coerceAtLeast(0L))
                val remPrincipal = (remainingBalance - remInterest).coerceAtLeast(1L)

                val termYears = (totalInstCount / 12.0).coerceAtLeast(0.25)
                val effectiveApr = if (totalProfit > 0L && financedPrincipal > 0L) {
                    val flatRatio = totalProfit.toDouble() / financedPrincipal.toDouble()
                    val apr = (flatRatio / termYears) * 100.0 * 1.65
                    ((apr * 10.0).roundToInt() / 10.0).coerceIn(0.0, 60.0)
                } else {
                    0.0
                }

                val monthlyMin = (remainingBalance.toDouble() / unpaid.size.coerceAtLeast(1).toDouble())
                    .roundToLong()
                    .coerceAtLeast(100_000L)
                    .coerceAtMost(remainingBalance)

                val nextInst = unpaid.firstOrNull()
                val nextDue = nextInst?.dueDateJalali ?: ob.firstDueDateJalali
                val daysUntilDue = JalaliCalendarEngine.diffJalaliDays(todayJalali, nextDue)
                val overdueCount = unpaid.count {
                    it.statusEnum == InstallmentStatus.OVERDUE ||
                        JalaliCalendarEngine.diffJalaliDays(todayJalali, it.dueDateJalali) < 0
                }
                val activeLateFees = unpaid.sumOf { it.lateFeeToman }

                // Interest Rate Score (0..55)
                val aprPoints = ((effectiveApr / 36.0) * 45.0).roundToInt().coerceIn(0, 45)
                val interestBurdenPoints = if (remainingBalance > 0L) {
                    ((remInterest.toDouble() / remainingBalance.toDouble()) * 30.0)
                        .roundToInt()
                        .coerceIn(0, 10)
                } else {
                    0
                }
                val interestScore = (aprPoints + interestBurdenPoints).coerceIn(0, 55)

                // Urgency Score (0..45)
                val dueProximityPoints = when {
                    overdueCount > 0 || daysUntilDue < 0 -> (24 + minOf(abs(daysUntilDue), 10)).coerceAtMost(32)
                    daysUntilDue == 0 -> 26
                    daysUntilDue <= 3 -> 22
                    daysUntilDue <= 7 -> 18
                    daysUntilDue <= 15 -> 13
                    daysUntilDue <= 30 -> 8
                    else -> 3
                }
                val trustPriorityPoints = when (ob.priorityEnum) {
                    TrustPriority.CRITICAL -> 9
                    TrustPriority.HIGH -> 5
                    TrustPriority.NORMAL -> 1
                }
                val lateFeePoints = if (activeLateFees > 0L) 4 else 0
                val urgencyScore = (dueProximityPoints + trustPriorityPoints + lateFeePoints).coerceIn(0, 45)

                val urgencyLevel = when {
                    overdueCount > 0 || daysUntilDue < 0 || activeLateFees > 0L ||
                        (ob.priorityEnum == TrustPriority.CRITICAL && daysUntilDue <= 5) -> DebtUrgencyLevel.CRITICAL
                    daysUntilDue <= 7 || (ob.priorityEnum == TrustPriority.HIGH && daysUntilDue <= 14) -> DebtUrgencyLevel.HIGH
                    daysUntilDue <= 30 -> DebtUrgencyLevel.MODERATE
                    else -> DebtUrgencyLevel.LOW
                }

                val compositeScore = (interestScore + urgencyScore).coerceIn(1, 100)

                val tag = when (ob.categoryEnum) {
                    ObligationCategory.INSTALLMENT_LOAN -> DebtCategoryTag.BANK_LOAN
                    ObligationCategory.BNPL_PURCHASE -> DebtCategoryTag.CREDIT_CARD
                    ObligationCategory.PERSONAL_DEBT -> DebtCategoryTag.PERSONAL
                    ObligationCategory.CHECK_OR_BILL -> DebtCategoryTag.CHECK_AND_HOUSING
                    ObligationCategory.PERSONAL_RECEIVABLE -> DebtCategoryTag.RECEIVABLE
                }

                RawScoredDebt(
                    obligation = ob,
                    counterparty = cpMap[ob.counterpartyId],
                    nextUnpaidInstallment = nextInst,
                    categoryTag = tag,
                    remainingBalanceToman = remainingBalance,
                    remainingPrincipalToman = remPrincipal,
                    remainingEmbeddedInterestToman = remInterest,
                    effectiveAnnualRatePercent = effectiveApr,
                    monthlyMinimumPaymentToman = monthlyMin,
                    remainingInstallmentCount = unpaid.size,
                    overdueInstallmentCount = overdueCount,
                    activeLateFeesToman = activeLateFees,
                    nextDueDateJalali = nextDue,
                    daysUntilNextDue = daysUntilDue,
                    relativeDueLabelFa = JalaliCalendarEngine.formatRelativeDueStatusFa(todayJalali, nextDue),
                    urgencyLevel = urgencyLevel,
                    interestRateScore = interestScore,
                    urgencyScore = urgencyScore,
                    compositePriorityScore = compositeScore
                )
            }

        if (rawScored.isEmpty()) {
            return DebtPayoffStrategyReport(
                rankingMode = rankingMode,
                extraMonthlyBudgetToman = safeExtra,
                rankedDebts = emptyList(),
                totalRemainingDebtToman = 0L,
                totalMinimumMonthlyToman = 0L,
                highestAprPercent = 0.0,
                weightedAverageAprPercent = 0.0,
                urgentDebtsCount = 0,
                projectedDebtFreeMonths = 0,
                projectedDebtFreeShamsiLabelFa = todayMonthLabel,
                baselineMinimumOnlyMonths = 0,
                monthsSavedVsMinimumOnly = 0,
                totalEstimatedInterestSavedVsBaselineToman = 0L,
                interestSavedByOrderVsReverseToman = 0L,
                topPriorityHeadlineFa = "تبریک! هیچ بدهی فعالی برای اولویت‌بندی وجود ندارد",
                strategyInsightFa = "تمامی بدهی‌های شما تسویه شده‌اند. می‌توانید تمرکز خود را روی افزایش اهداف پس‌انداز ماهانه بگذارید."
            )
        }

        // 2. Sort according to the chosen PayoffRankingMode
        val orderedRaw = when (rankingMode) {
            PayoffRankingMode.INTEREST_AND_URGENCY -> rawScored.sortedWith(
                compareByDescending<RawScoredDebt> { it.compositePriorityScore }
                    .thenByDescending { it.effectiveAnnualRatePercent }
                    .thenBy { it.daysUntilNextDue }
                    .thenBy { it.remainingBalanceToman }
            )
            PayoffRankingMode.MAX_INTEREST_SAVINGS -> rawScored.sortedWith(
                compareByDescending<RawScoredDebt> { it.effectiveAnnualRatePercent }
                    .thenByDescending { it.urgencyScore }
                    .thenBy { it.remainingBalanceToman }
            )
            PayoffRankingMode.URGENCY_FIRST -> rawScored.sortedWith(
                compareByDescending<RawScoredDebt> { it.overdueInstallmentCount > 0 }
                    .thenBy { it.daysUntilNextDue }
                    .thenByDescending { it.obligation.priorityEnum.weight }
                    .thenByDescending { it.effectiveAnnualRatePercent }
            )
        }

        // 3. Simulate multi-debt compound amortization with automatic payment rollover
        val recommendedSim = simulateMultiDebtOrder(orderedRaw, extraMonthlyBudgetToman = safeExtra)
        val minOnlySim = simulateMultiDebtOrder(orderedRaw, extraMonthlyBudgetToman = 0L)

        // Compare recommended order vs worst-case reverse-APR order (using at least 1M attack rollover benchmark if extra == 0)
        val benchmarkExtra = if (safeExtra > 0L) safeExtra else 0L
        val reverseAprOrder = rawScored.sortedWith(
            compareBy<RawScoredDebt> { it.effectiveAnnualRatePercent }
                .thenByDescending { it.remainingBalanceToman }
        )
        val reverseSim = simulateMultiDebtOrder(reverseAprOrder, extraMonthlyBudgetToman = benchmarkExtra)

        val totalInterestSavedVsBaseline = (minOnlySim.totalInterestToman - recommendedSim.totalInterestToman)
            .coerceAtLeast(0L)
        val interestSavedByOrder = (reverseSim.totalInterestToman - recommendedSim.totalInterestToman)
            .coerceAtLeast(0L)

        // 4. Construct final ranked items with per-debt interest savings & guidance
        val rankedItems = orderedRaw.mapIndexed { index, raw ->
            val rank = index + 1
            val clearMonth = recommendedSim.clearMonthByObligationId[raw.obligation.id]
                ?: raw.remainingInstallmentCount
            val clearDate = JalaliCalendarEngine.addJalaliMonths(todayJalali, clearMonth)
            val parsedClear = JalaliCalendarEngine.parseJalali(clearDate)
            val clearLabelFa = JalaliCalendarEngine.formatJalaliMonthYear(parsedClear.year, parsedClear.month)

            val baseInterestForDebt = minOnlySim.interestByObligationId[raw.obligation.id] ?: 0L
            val recInterestForDebt = recommendedSim.interestByObligationId[raw.obligation.id] ?: 0L
            val perDebtSaved = (baseInterestForDebt - recInterestForDebt).coerceAtLeast(0L)

            val badges = buildList {
                if (raw.effectiveAnnualRatePercent >= 20.0) {
                    add("نرخ سود بالا (${PersianCurrencyEngine.toPersianDigits(raw.effectiveAnnualRatePercent.toString())}٪)")
                } else if (raw.effectiveAnnualRatePercent > 0.0) {
                    add("نرخ ${PersianCurrencyEngine.toPersianDigits(raw.effectiveAnnualRatePercent.toString())}٪")
                } else {
                    add("بدون سود / ۰٪")
                }
                if (raw.overdueInstallmentCount > 0) {
                    add("${PersianCurrencyEngine.toPersianDigits(raw.overdueInstallmentCount.toString())} قسط معوقه")
                } else if (raw.daysUntilNextDue <= 7) {
                    add("سررسید نزدیک")
                }
                if (raw.activeLateFeesToman > 0L) {
                    add("دارای جریمه دیرکرد")
                }
                if (raw.obligation.priorityEnum == TrustPriority.CRITICAL) {
                    add("تعهد حیاتی")
                }
            }

            val guidance = when {
                rank == 1 && raw.overdueInstallmentCount > 0 ->
                    "اولویت شماره ۱: ابتدا اقساط معوقه را تسویه کنید تا جریمه دیرکرد و سود مضاعف متوقف شود."
                rank == 1 && raw.effectiveAnnualRatePercent >= 15.0 ->
                    "اولویت شماره ۱: کل بودجه مازاد ماهانه را روی این بدهی متمرکز کنید تا بیشترین سود ممکن حذف شود."
                rank == 1 ->
                    "اولویت شماره ۱: با تسویه سریع این بدهی، قسط ماهانه آن آزاد شده و به بدهی بعدی منتقل می‌شود."
                raw.effectiveAnnualRatePercent == 0.0 ->
                    "حداقل قسط ماهانه را منظم بپردازید و بودجه مازاد را ابتدا صرف بدهی‌های پُرسودتر بالاتر کنید."
                else ->
                    "تا زمان تسویه رتبه‌های بالاتر، قسط عادی را بپردازید؛ سپس کل توان مالی آزادشده را روی این پرونده متمرکز کنید."
            }

            RankedPayoffDebtItem(
                rank = rank,
                obligation = raw.obligation,
                counterparty = raw.counterparty,
                nextUnpaidInstallment = raw.nextUnpaidInstallment,
                categoryTag = raw.categoryTag,
                remainingBalanceToman = raw.remainingBalanceToman,
                remainingPrincipalToman = raw.remainingPrincipalToman,
                remainingEmbeddedInterestToman = raw.remainingEmbeddedInterestToman,
                effectiveAnnualRatePercent = raw.effectiveAnnualRatePercent,
                monthlyMinimumPaymentToman = raw.monthlyMinimumPaymentToman,
                remainingInstallmentCount = raw.remainingInstallmentCount,
                overdueInstallmentCount = raw.overdueInstallmentCount,
                activeLateFeesToman = raw.activeLateFeesToman,
                nextDueDateJalali = raw.nextDueDateJalali,
                daysUntilNextDue = raw.daysUntilNextDue,
                relativeDueLabelFa = raw.relativeDueLabelFa,
                urgencyLevel = raw.urgencyLevel,
                interestRateScore = raw.interestRateScore,
                urgencyScore = raw.urgencyScore,
                compositePriorityScore = raw.compositePriorityScore,
                projectedPayoffMonthIndex = clearMonth,
                projectedPayoffShamsiLabelFa = clearLabelFa,
                estimatedInterestSavedToman = perDebtSaved,
                rationaleBadgesFa = badges,
                actionGuidanceFa = guidance
            )
        }

        val totalRemaining = rankedItems.sumOf { it.remainingBalanceToman }
        val totalMinMonthly = rankedItems.sumOf { it.monthlyMinimumPaymentToman }
        val highestApr = rankedItems.maxOf { it.effectiveAnnualRatePercent }
        val weightedApr = if (totalRemaining > 0L) {
            val num = rankedItems.sumOf { it.effectiveAnnualRatePercent * it.remainingBalanceToman.toDouble() }
            ((num / totalRemaining.toDouble()) * 10.0).roundToInt() / 10.0
        } else {
            0.0
        }
        val urgentCount = rankedItems.count {
            it.urgencyLevel == DebtUrgencyLevel.CRITICAL || it.urgencyLevel == DebtUrgencyLevel.HIGH
        }

        val endMonths = recommendedSim.totalMonths
        val endDate = JalaliCalendarEngine.addJalaliMonths(todayJalali, endMonths)
        val parsedEnd = JalaliCalendarEngine.parseJalali(endDate)
        val endLabelFa = JalaliCalendarEngine.formatJalaliMonthYear(parsedEnd.year, parsedEnd.month)
        val monthsSaved = (minOnlySim.totalMonths - endMonths).coerceAtLeast(0)

        val topTarget = rankedItems.first()
        val topHeadline = "هدف شماره ۱: «${topTarget.obligation.title}» با نرخ ${PersianCurrencyEngine.toPersianDigits(topTarget.effectiveAnnualRatePercent.toString())}٪ و امتیاز اولویت ${PersianCurrencyEngine.toPersianDigits(topTarget.compositePriorityScore.toString())}/۱۰۰"

        val insight = when {
            totalInterestSavedVsBaseline > 0L ->
                "با رعایت این ترتیب تسویه و تخصیص پرداخت مازاد، حدود ${PersianCurrencyEngine.formatAmount(totalInterestSavedVsBaseline)} در هزینه‌های سود صرفه‌جویی می‌کنید و ${PersianCurrencyEngine.toPersianDigits(monthsSaved.toString())} ماه زودتر به آزادی کامل مالی می‌رسید."
            interestSavedByOrder > 0L ->
                "این ترتیب تسویه نسبت به تسویه بدهی‌های کم‌بهره در اولویت، حدود ${PersianCurrencyEngine.formatAmount(interestSavedByOrder)} سود کمتری به شما تحمیل می‌کند."
            urgentCount > 0 ->
                "${PersianCurrencyEngine.toPersianDigits(urgentCount.toString())} بدهی با فوریت بالا شناسایی شد؛ پرداخت به‌موقع رتبه‌های اول از جریمه دیرکرد جلوگیری می‌کند و با افزودن بودجه مازاد، صرفه‌جویی سود فعال می‌شود."
            else ->
                "بدهی‌های شما بر اساس نرخ سود مؤثر و زمان سررسید مرتب شده‌اند. با حرکت دادن اسلایدر بودجه مازاد، اثر آن بر کاهش سود و زمان تسویه را ببینید."
        }

        return DebtPayoffStrategyReport(
            rankingMode = rankingMode,
            extraMonthlyBudgetToman = safeExtra,
            rankedDebts = rankedItems,
            totalRemainingDebtToman = totalRemaining,
            totalMinimumMonthlyToman = totalMinMonthly,
            highestAprPercent = highestApr,
            weightedAverageAprPercent = weightedApr,
            urgentDebtsCount = urgentCount,
            projectedDebtFreeMonths = endMonths,
            projectedDebtFreeShamsiLabelFa = endLabelFa,
            baselineMinimumOnlyMonths = minOnlySim.totalMonths,
            monthsSavedVsMinimumOnly = monthsSaved,
            totalEstimatedInterestSavedVsBaselineToman = totalInterestSavedVsBaseline,
            interestSavedByOrderVsReverseToman = interestSavedByOrder,
            topPriorityHeadlineFa = topHeadline,
            strategyInsightFa = insight
        )
    }

    private data class RawScoredDebt(
        val obligation: ObligationEntity,
        val counterparty: CounterpartyEntity?,
        val nextUnpaidInstallment: InstallmentEntity?,
        val categoryTag: DebtCategoryTag,
        val remainingBalanceToman: Long,
        val remainingPrincipalToman: Long,
        val remainingEmbeddedInterestToman: Long,
        val effectiveAnnualRatePercent: Double,
        val monthlyMinimumPaymentToman: Long,
        val remainingInstallmentCount: Int,
        val overdueInstallmentCount: Int,
        val activeLateFeesToman: Long,
        val nextDueDateJalali: String,
        val daysUntilNextDue: Int,
        val relativeDueLabelFa: String,
        val urgencyLevel: DebtUrgencyLevel,
        val interestRateScore: Int,
        val urgencyScore: Int,
        val compositePriorityScore: Int
    )

    private data class MultiDebtSimState(
        val id: Long,
        var balanceToman: Long,
        val monthlyRate: Double,
        val minimumPaymentToman: Long,
        var accumulatedInterestToman: Long = 0L,
        var clearMonthIndex: Int = 0
    )

    private data class MultiDebtSimResult(
        val totalMonths: Int,
        val totalInterestToman: Long,
        val clearMonthByObligationId: Map<Long, Int>,
        val interestByObligationId: Map<Long, Long>
    )

    /**
     * Simulates exact month-by-month compound amortization across all debts in [orderedDebts],
     * paying every active debt's minimum payment and rolling over [extraMonthlyBudgetToman] plus
     * freed minimum payments of paid-off debts into the highest-ranked remaining debt.
     */
    private fun simulateMultiDebtOrder(
        orderedDebts: List<RawScoredDebt>,
        extraMonthlyBudgetToman: Long
    ): MultiDebtSimResult {
        if (orderedDebts.isEmpty()) {
            return MultiDebtSimResult(0, 0L, emptyMap(), emptyMap())
        }

        val states = orderedDebts.map {
            MultiDebtSimState(
                id = it.obligation.id,
                balanceToman = it.remainingPrincipalToman.coerceAtLeast(1L),
                monthlyRate = (it.effectiveAnnualRatePercent / 100.0) / 12.0,
                minimumPaymentToman = it.monthlyMinimumPaymentToman.coerceAtLeast(100_000L)
            )
        }

        val totalMonthlyCommit = states.sumOf { it.minimumPaymentToman } + extraMonthlyBudgetToman
        var month = 0

        while (states.any { it.balanceToman > 0L } && month < MAX_SIM_MONTHS) {
            month += 1

            // 1. Accrue monthly interest on each active debt
            states.forEach { st ->
                if (st.balanceToman > 0L && st.monthlyRate > 0.0) {
                    val interest = (st.balanceToman * st.monthlyRate).roundToLong().coerceAtLeast(0L)
                    st.balanceToman += interest
                    st.accumulatedInterestToman += interest
                }
            }

            // 2. Pay minimum on every active debt
            var remainingPool = totalMonthlyCommit
            states.forEach { st ->
                if (st.balanceToman > 0L) {
                    val minPay = minOf(st.balanceToman, st.minimumPaymentToman, remainingPool)
                    st.balanceToman -= minPay
                    remainingPool -= minPay
                    if (st.balanceToman == 0L && st.clearMonthIndex == 0) {
                        st.clearMonthIndex = month
                    }
                }
            }

            // 3. Direct all extra budget + freed rollover pool strictly by ranked pay-off order
            if (remainingPool > 0L) {
                for (st in states) {
                    if (st.balanceToman > 0L && remainingPool > 0L) {
                        val attack = minOf(st.balanceToman, remainingPool)
                        st.balanceToman -= attack
                        remainingPool -= attack
                        if (st.balanceToman == 0L && st.clearMonthIndex == 0) {
                            st.clearMonthIndex = month
                        }
                    }
                }
            }
        }

        states.forEach { st ->
            if (st.clearMonthIndex == 0) st.clearMonthIndex = month
        }

        return MultiDebtSimResult(
            totalMonths = month,
            totalInterestToman = states.sumOf { it.accumulatedInterestToman },
            clearMonthByObligationId = states.associate { it.id to it.clearMonthIndex },
            interestByObligationId = states.associate { it.id to it.accumulatedInterestToman }
        )
    }
}
