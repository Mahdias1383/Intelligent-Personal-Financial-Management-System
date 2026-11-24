package com.example.domain

import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.ObligationEntity
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

enum class PaymentScheduleStrategyMode(
    val labelFa: String,
    val shortLabelFa: String
) {
    SINGLE_BEST_DAY("یک‌مرحله‌ای (بهترین روز ماه)", "تک‌روز بهینه"),
    SPLIT_BI_CYCLE("دومرحله‌ای (همگام با ۲ درآمد)", "دومرحله‌ای")
}

/**
 * Represents a real historical income deposit logged by the user (or synthesized from active recurring income).
 */
data class HistoricalIncomeDeposit(
    val id: Long,
    val titleFa: String,
    val amountToman: Long,
    val receivedDateJalali: String,
    val isUserLogged: Boolean = true
) {
    val dayOfShamsiMonth: Int
        get() = JalaliCalendarEngine.parseJalali(receivedDateJalali).day.coerceIn(1, 30)
}

/**
 * A clustered historical payday discovered across multiple past Shamsi months.
 */
data class DetectedPaydayCluster(
    val medianDayOfMonth: Int,
    val averageAmountToman: Long,
    val sampleCount: Int,
    val consistencyPercent: Int,
    val primarySourceTitleFa: String
)

/**
 * Scoring breakdown for a single day (1..30) of the Persian (Shamsi) month.
 */
data class DayScheduleEvaluation(
    val dayOfMonth: Int,
    val compositeScore: Int,            // 0..100 overall optimality
    val liquidityScore: Int,            // 0..45 available cash after payday clearance
    val deadlineSafetyScore: Int,       // 0..30 on-time coverage before due dates
    val historicalAlignmentScore: Int,  // 0..25 alignment with past income & paid history
    val projectedNetCashToman: Long,    // cumulative cash available on this day
    val incomeInflowToman: Long,        // income hitting on this day
    val expenseOutflowToman: Long,      // non-loan expenses hitting on this day
    val installmentsDueCount: Int,      // upcoming installments due on this day
    val installmentsDueToman: Long,
    val isPrimaryRecommended: Boolean = false,
    val isSecondaryRecommended: Boolean = false
)

/**
 * Concrete mapping of an upcoming unpaid installment onto the automated recommended payment day.
 */
data class ScheduledInstallmentAction(
    val installment: InstallmentEntity,
    val obligationTitleFa: String,
    val recommendedPayDayOfMonth: Int,
    val recommendedPayDateJalali: String,
    val originalDueDateJalali: String,
    val daysAheadOfDueDate: Int,
    val usesSecondaryCycle: Boolean
)

/**
 * Complete automated schedule recommendation report displayed inside the Dashboard Strategy tab.
 */
data class AutomatedPaymentScheduleReport(
    val primaryRecommendedDay: Int,
    val secondaryRecommendedDay: Int?,
    val effectiveMode: PaymentScheduleStrategyMode,
    val topCandidateDays: List<DayScheduleEvaluation>,
    val dailyEvaluations: List<DayScheduleEvaluation>,
    val detectedPaydays: List<DetectedPaydayCluster>,
    val totalMonthlyIncomeToman: Long,
    val totalMonthlyScheduledDebtToman: Long,
    val projectedBufferOnBestDayToman: Long,
    val onTimeCoveragePercent: Int,
    val confidencePercent: Int,
    val incomeCyclePatternFa: String,
    val headlineSummaryFa: String,
    val rationaleBulletsFa: List<String>,
    val scheduledActions: List<ScheduledInstallmentAction>
)

/**
 * Pure deterministic quantitative engine that analyzes the user's historical income cycles,
 * recurring non-loan living outflows, past installment payment behavior, and upcoming installment
 * due dates to recommend the mathematically optimal day(s) of the Shamsi month for automated payments.
 */
object IncomeCyclePaymentSchedulerEngine {

    private const val SHAMSI_STANDARD_DAYS = 30
    private const val PAYDAY_SETTLEMENT_BUFFER_DAYS = 2

    fun buildScheduleRecommendation(
        recurringItems: List<RecurringCashflowItem>,
        historicalIncomeDeposits: List<HistoricalIncomeDeposit>,
        obligations: List<ObligationEntity>,
        installments: List<InstallmentEntity>,
        fallbackMonthlyIncomeToman: Long,
        preferredMode: PaymentScheduleStrategyMode = PaymentScheduleStrategyMode.SINGLE_BEST_DAY,
        todayJalali: String = JalaliCalendarEngine.todayJalali()
    ): AutomatedPaymentScheduleReport {
        val activeIncomes = recurringItems.filter { it.isActive && it.type == RecurringCashflowType.INCOME && it.monthlyAmountToman > 0L }
        val activeExpenses = recurringItems.filter { it.isActive && it.type == RecurringCashflowType.EXPENSE && it.monthlyAmountToman > 0L }

        // 1. Assemble effective historical income deposits (real user logs + 3-month recurring baseline)
        val effectiveHistory = assembleEffectiveIncomeHistory(
            userLogged = historicalIncomeDeposits,
            activeIncomes = activeIncomes,
            fallbackMonthlyIncomeToman = fallbackMonthlyIncomeToman,
            todayJalali = todayJalali
        )

        // 2. Cluster historical income deposits into recurring payday cycles
        val detectedPaydays = detectPaydayClusters(effectiveHistory)
        val totalMonthlyIncome = detectedPaydays.sumOf { it.averageAmountToman }
            .coerceAtLeast(activeIncomes.sumOf { it.monthlyAmountToman })
            .coerceAtLeast(fallbackMonthlyIncomeToman.coerceAtLeast(1_000_000L))

        // 3. Extract active debt installments (non-receivable) and historical on-time payment days
        val debtObligationMap = obligations
            .filter { !it.categoryEnum.isReceivable }
            .associateBy { it.id }

        val unpaidDebtInstallments = installments
            .filter { it.obligationId in debtObligationMap.keys && it.statusEnum != InstallmentStatus.PAID && it.remainingToman > 0L }
            .sortedBy { it.dueDateJalali }

        val paidHistoryDays = installments
            .filter { it.statusEnum == InstallmentStatus.PAID && it.paidDateJalali.isNotBlank() }
            .map { JalaliCalendarEngine.parseJalali(it.paidDateJalali).day.coerceIn(1, SHAMSI_STANDARD_DAYS) }

        // Next upcoming installment per active obligation represents one monthly billing cycle
        val nextCycleInstallments = unpaidDebtInstallments
            .groupBy { it.obligationId }
            .mapNotNull { (_, list) -> list.minByOrNull { it.dueDateJalali } }
            .sortedBy { it.dueDateJalali }

        val totalMonthlyDebt = nextCycleInstallments.sumOf { it.remainingToman }

        // 4. Build per-day inflow, outflow, and installment due maps across Shamsi days 1..30
        val incomeByDay = LongArray(SHAMSI_STANDARD_DAYS + 1)
        detectedPaydays.forEach { cluster ->
            val d = cluster.medianDayOfMonth.coerceIn(1, SHAMSI_STANDARD_DAYS)
            incomeByDay[d] += cluster.averageAmountToman
        }
        val expenseByDay = LongArray(SHAMSI_STANDARD_DAYS + 1)
        activeExpenses.forEach { exp ->
            val d = exp.dayOfShamsiMonth.coerceIn(1, SHAMSI_STANDARD_DAYS)
            expenseByDay[d] += exp.monthlyAmountToman
        }
        val dueCountByDay = IntArray(SHAMSI_STANDARD_DAYS + 1)
        val dueAmountByDay = LongArray(SHAMSI_STANDARD_DAYS + 1)
        nextCycleInstallments.forEach { inst ->
            val d = JalaliCalendarEngine.parseJalali(inst.dueDateJalali).day.coerceIn(1, SHAMSI_STANDARD_DAYS)
            dueCountByDay[d] += 1
            dueAmountByDay[d] += inst.remainingToman
        }

        // Primary payday anchor (largest income stream)
        val primaryPayday = detectedPaydays.maxByOrNull { it.averageAmountToman }?.medianDayOfMonth ?: 1

        // 5. Simulate cyclic 30-day available cash starting from primary payday
        //    We apply a +1..+2 day settlement ramp so cash becomes fully "cleared" after payday!
        val projectedNetCash = LongArray(SHAMSI_STANDARD_DAYS + 1)
        var runningCash = 0L
        for (offset in 0 until SHAMSI_STANDARD_DAYS) {
            val day = wrapDay(primaryPayday + offset)
            // Income clears with full confidence 1-2 days after deposit; on payday itself 65% cleared
            val yesterday = wrapDay(day - 1)
            val twoDaysAgo = wrapDay(day - 2)
            val clearedInflowToday = (incomeByDay[day] * 0.65).roundToLong() +
                (incomeByDay[yesterday] * 0.25).roundToLong() +
                (incomeByDay[twoDaysAgo] * 0.10).roundToLong()
            runningCash += clearedInflowToday - expenseByDay[day]
            projectedNetCash[day] = runningCash
        }

        val minCash = (1..SHAMSI_STANDARD_DAYS).minOf { projectedNetCash[it] }
        val maxCash = (1..SHAMSI_STANDARD_DAYS).maxOf { projectedNetCash[it] }
        val cashSpan = (maxCash - minCash).coerceAtLeast(1L)

        // 6. Score every day 1..30 across Liquidity (45) + Deadline Safety (30) + Historical Alignment (25)
        val rawEvaluations = (1..SHAMSI_STANDARD_DAYS).map { day ->
            // A) Liquidity Score (0..45): peaks right after income clears and before major living outflows drain cash
            val normalizedLiquidity = ((projectedNetCash[day] - minCash).toDouble() / cashSpan.toDouble()).coerceIn(0.0, 1.0)
            val coversAllMonthlyDebtBonus = if (totalMonthlyDebt > 0L && projectedNetCash[day] >= totalMonthlyDebt) 1.0 else 0.72
            val liquidityScore = (normalizedLiquidity * 45.0 * coversAllMonthlyDebtBonus).roundToInt().coerceIn(0, 45)

            // B) Deadline Safety Score (0..30): how well paying on `day` covers upcoming due dates BEFORE they become late
            val deadlineSafetyScore = computeDeadlineSafetyScore(
                candidateDay = day,
                primaryPayday = primaryPayday,
                cycleInstallments = nextCycleInstallments
            )

            // C) Historical Income & Payment Behavior Alignment (0..25):
            //    Rewards paying 2..4 days AFTER an income payday (ideal bank clearance buffer without delay risk)
            val alignmentScore = computeHistoricalAlignmentScore(
                candidateDay = day,
                paydays = detectedPaydays,
                paidHistoryDays = paidHistoryDays
            )

            val composite = (liquidityScore + deadlineSafetyScore + alignmentScore).coerceIn(0, 100)

            DayScheduleEvaluation(
                dayOfMonth = day,
                compositeScore = composite,
                liquidityScore = liquidityScore,
                deadlineSafetyScore = deadlineSafetyScore,
                historicalAlignmentScore = alignmentScore,
                projectedNetCashToman = projectedNetCash[day].coerceAtLeast(0L),
                incomeInflowToman = incomeByDay[day],
                expenseOutflowToman = expenseByDay[day],
                installmentsDueCount = dueCountByDay[day],
                installmentsDueToman = dueAmountByDay[day]
            )
        }

        val primaryBest = rawEvaluations.maxWithOrNull(
            compareBy<DayScheduleEvaluation> { it.compositeScore }
                .thenBy { it.liquidityScore }
                .thenBy { -circularForwardDistance(primaryPayday, it.dayOfMonth) }
        ) ?: rawEvaluations.first()

        // 7. Determine Secondary Split-Pay Day when user has >= 2 distinct income clusters separated by >= 7 days
        val secondaryPaydayCluster = detectedPaydays
            .filter { circularAbsoluteDistance(it.medianDayOfMonth, primaryPayday) >= 7 }
            .maxByOrNull { it.averageAmountToman }

        val secondaryBestDay: Int? = if (secondaryPaydayCluster != null) {
            // Find highest scoring day within +1..+4 days after secondary payday
            val windowDays = (1..4).map { offset -> wrapDay(secondaryPaydayCluster.medianDayOfMonth + offset) }.toSet()
            rawEvaluations
                .filter { it.dayOfMonth in windowDays && it.dayOfMonth != primaryBest.dayOfMonth }
                .maxByOrNull { it.compositeScore }
                ?.dayOfMonth
        } else null

        val effectiveMode = if (preferredMode == PaymentScheduleStrategyMode.SPLIT_BI_CYCLE && secondaryBestDay != null) {
            PaymentScheduleStrategyMode.SPLIT_BI_CYCLE
        } else {
            PaymentScheduleStrategyMode.SINGLE_BEST_DAY
        }

        val markedEvaluations = rawEvaluations.map { eval ->
            eval.copy(
                isPrimaryRecommended = eval.dayOfMonth == primaryBest.dayOfMonth,
                isSecondaryRecommended = secondaryBestDay != null && eval.dayOfMonth == secondaryBestDay
            )
        }

        val topCandidates = markedEvaluations
            .sortedByDescending { it.compositeScore }
            .take(3)

        // 8. Map upcoming cycle installments to concrete scheduled actions
        val todayParsed = JalaliCalendarEngine.parseJalali(todayJalali)
        val scheduledActions = nextCycleInstallments.map { inst ->
            val obTitle = debtObligationMap[inst.obligationId]?.title ?: inst.title
            val dueParsed = JalaliCalendarEngine.parseJalali(inst.dueDateJalali)
            val chosenDay = if (effectiveMode == PaymentScheduleStrategyMode.SPLIT_BI_CYCLE && secondaryBestDay != null) {
                chooseBestCycleDayForDueDate(
                    dueDay = dueParsed.day.coerceIn(1, SHAMSI_STANDARD_DAYS),
                    primaryDay = primaryBest.dayOfMonth,
                    secondaryDay = secondaryBestDay
                )
            } else {
                primaryBest.dayOfMonth
            }
            val usesSecondary = secondaryBestDay != null && chosenDay == secondaryBestDay && chosenDay != primaryBest.dayOfMonth
            val scheduledDateJalali = buildTargetScheduledDate(
                dueYear = dueParsed.year,
                dueMonth = dueParsed.month,
                dueDay = dueParsed.day,
                targetPayDay = chosenDay,
                todayYear = todayParsed.year,
                todayMonth = todayParsed.month
            )
            val daysAhead = JalaliCalendarEngine.diffJalaliDays(scheduledDateJalali, inst.dueDateJalali)
            ScheduledInstallmentAction(
                installment = inst,
                obligationTitleFa = obTitle,
                recommendedPayDayOfMonth = chosenDay,
                recommendedPayDateJalali = scheduledDateJalali,
                originalDueDateJalali = inst.dueDateJalali,
                daysAheadOfDueDate = daysAhead,
                usesSecondaryCycle = usesSecondary
            )
        }

        val onTimeCount = scheduledActions.count { it.daysAheadOfDueDate >= 0 }
        val onTimeCoveragePercent = if (scheduledActions.isEmpty()) {
            100
        } else {
            ((onTimeCount.toDouble() / scheduledActions.size.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
        }

        val projectedBufferOnBestDay = (primaryBest.projectedNetCashToman - totalMonthlyDebt).coerceAtLeast(0L)

        // 9. Build Persian Cycle Pattern & Transparent Rationale
        val userLogCount = historicalIncomeDeposits.count { it.isUserLogged }
        val confidencePercent = (58 + (detectedPaydays.sumOf { it.sampleCount }.coerceAtMost(8) * 4) +
            (userLogCount.coerceAtMost(4) * 3) +
            if (paidHistoryDays.isNotEmpty()) 6 else 0).coerceIn(55, 98)

        val incomeCyclePatternFa = when {
            detectedPaydays.size >= 2 -> {
                val p1 = PersianCurrencyEngine.toPersianDigits(detectedPaydays[0].medianDayOfMonth.toString())
                val p2 = PersianCurrencyEngine.toPersianDigits(detectedPaydays[1].medianDayOfMonth.toString())
                "چرخه دودرآمدی ماهانه (روزهای $p1 و $p2 هر ماه)"
            }
            detectedPaydays.size == 1 -> {
                val p1 = PersianCurrencyEngine.toPersianDigits(detectedPaydays[0].medianDayOfMonth.toString())
                "چرخه درآمد متمرکز (روز $p1 هر ماه شمسی)"
            }
            else -> "چرخه استاندارد حقوق ابتدای ماه"
        }

        val primaryDayFa = PersianCurrencyEngine.toPersianDigits(primaryBest.dayOfMonth.toString())
        val primaryPaydayFa = PersianCurrencyEngine.toPersianDigits(primaryPayday.toString())
        val bufferDaysAfterPayday = circularForwardDistance(primaryPayday, primaryBest.dayOfMonth)
        val bufferDaysFa = PersianCurrencyEngine.toPersianDigits(bufferDaysAfterPayday.toString())

        val headlineSummaryFa = if (effectiveMode == PaymentScheduleStrategyMode.SPLIT_BI_CYCLE && secondaryBestDay != null) {
            val secDayFa = PersianCurrencyEngine.toPersianDigits(secondaryBestDay.toString())
            "پیشنهاد دومرحله‌ای: روز $primaryDayFa (بدهی‌های اصلی) و روز $secDayFa هر ماه (اقساط نیمه دوم) برای تعادل کامل نقدینگی"
        } else {
            "بهترین روز پرداخت ماهانه: $primaryDayFa ام هر ماه ($bufferDaysFa روز پس از واریز اصلی روز $primaryPaydayFa با پوشش ${PersianCurrencyEngine.toPersianDigits(onTimeCoveragePercent.toString())}٪ سررسیدها)"
        }

        val rationaleBullets = buildList {
            add("حاشیه اطمینان بانکی: انتخاب روز $primaryDayFa ($bufferDaysFa روز پس از واریز اصلی در روز $primaryPaydayFa) از تأخیر احتمالی واریز حقوق و برگشت قسط جلوگیری می‌کند.")
            if (totalMonthlyDebt > 0L) {
                add("نقدینگی بهینه: در روز $primaryDayFa مانده نقدی تخمینی پس از پرداخت اقساط حدود ${PersianCurrencyEngine.formatAmount(projectedBufferOnBestDay)} خواهد بود.")
            }
            if (secondaryBestDay != null) {
                val secFa = PersianCurrencyEngine.toPersianDigits(secondaryBestDay.toString())
                add("فرصت پرداخت دومرحله‌ای: به دلیل درآمد دوم در میانه ماه، می‌توانید اقساط نیمه دوم را به روز $secFa منتقل کنید تا فشار نقدینگی یکجا کاهش یابد.")
            }
            if (paidHistoryDays.isNotEmpty()) {
                val medianPaidDay = paidHistoryDays.sorted()[paidHistoryDays.size / 2]
                add("الگوی رفتاری شما: در سوابق گذشته معمولاً حوالی روز ${PersianCurrencyEngine.toPersianDigits(medianPaidDay.toString())} اقساط خود را با موفقیت تسویه کرده‌اید.")
            }
        }

        return AutomatedPaymentScheduleReport(
            primaryRecommendedDay = primaryBest.dayOfMonth,
            secondaryRecommendedDay = secondaryBestDay,
            effectiveMode = effectiveMode,
            topCandidateDays = topCandidates,
            dailyEvaluations = markedEvaluations,
            detectedPaydays = detectedPaydays,
            totalMonthlyIncomeToman = totalMonthlyIncome,
            totalMonthlyScheduledDebtToman = totalMonthlyDebt,
            projectedBufferOnBestDayToman = projectedBufferOnBestDay,
            onTimeCoveragePercent = onTimeCoveragePercent,
            confidencePercent = confidencePercent,
            incomeCyclePatternFa = incomeCyclePatternFa,
            headlineSummaryFa = headlineSummaryFa,
            rationaleBulletsFa = rationaleBullets,
            scheduledActions = scheduledActions
        )
    }

    /**
     * Persists user-logged historical income deposits as a lightweight pure-Kotlin
     * record string in SharedPreferences (zero Android org.json dependency).
     */
    fun serializeHistoricalDeposits(items: List<HistoricalIncomeDeposit>): String {
        return items.joinToString("\n") { item ->
            val safeTitle = item.titleFa
                .replace("\\", "\\\\")
                .replace("|", "\\p")
                .replace("\n", " ")
            "${item.id}|$safeTitle|${item.amountToman}|${item.receivedDateJalali}|${if (item.isUserLogged) 1 else 0}"
        }
    }

    fun deserializeHistoricalDeposits(raw: String?): List<HistoricalIncomeDeposit> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.lines().mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@mapNotNull null
            val parts = trimmed.split("|")
            if (parts.size < 4) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val title = parts[1].replace("\\p", "|").replace("\\\\", "\\")
            val amount = parts[2].toLongOrNull() ?: return@mapNotNull null
            val date = parts[3].trim()
            val isUserLogged = parts.getOrNull(4)?.let { it != "0" } ?: true
            if (date.isBlank() || amount <= 0L) return@mapNotNull null
            HistoricalIncomeDeposit(
                id = id,
                titleFa = title.ifBlank { "واریز درآمد" },
                amountToman = amount,
                receivedDateJalali = JalaliCalendarEngine.normalizeJalaliDate(date),
                isUserLogged = isUserLogged
            )
        }
    }

    private fun assembleEffectiveIncomeHistory(
        userLogged: List<HistoricalIncomeDeposit>,
        activeIncomes: List<RecurringCashflowItem>,
        fallbackMonthlyIncomeToman: Long,
        todayJalali: String
    ): List<HistoricalIncomeDeposit> {
        val combined = mutableListOf<HistoricalIncomeDeposit>()
        combined.addAll(userLogged.filter { it.amountToman > 0L })

        val today = JalaliCalendarEngine.parseJalali(todayJalali)
        if (userLogged.size < 3) {
            val sources = if (activeIncomes.isNotEmpty()) {
                activeIncomes
            } else {
                listOf(
                    RecurringCashflowItem(
                        id = 9001L,
                        titleFa = "حقوق و درآمد ماهانه",
                        category = RecurringCashflowCategory.SALARY,
                        monthlyAmountToman = fallbackMonthlyIncomeToman.coerceAtLeast(10_000_000L),
                        dayOfShamsiMonth = 1
                    )
                )
            }
            var syntheticId = -1L
            for (monthsBack in 1..3) {
                val (y, m) = JalaliCalendarEngine.shiftJalaliYearMonth(today.year, today.month, -monthsBack)
                val maxDay = JalaliCalendarEngine.getJalaliMonthLength(y, m)
                sources.forEach { src ->
                    val d = src.dayOfShamsiMonth.coerceIn(1, maxDay)
                    val dateStr = JalaliCalendarEngine.normalizeJalaliDate("$y/$m/$d")
                    combined.add(
                        HistoricalIncomeDeposit(
                            id = syntheticId--,
                            titleFa = src.titleFa,
                            amountToman = src.monthlyAmountToman,
                            receivedDateJalali = dateStr,
                            isUserLogged = false
                        )
                    )
                }
            }
        }
        return combined
    }

    private fun detectPaydayClusters(history: List<HistoricalIncomeDeposit>): List<DetectedPaydayCluster> {
        if (history.isEmpty()) return emptyList()

        // Group deposits within +/- 2 circular days of each other into payday clusters
        val sortedByAmountDesc = history.sortedWith(
            compareByDescending<HistoricalIncomeDeposit> { it.isUserLogged }
                .thenByDescending { it.amountToman }
        )
        val clusters = mutableListOf<MutableList<HistoricalIncomeDeposit>>()

        for (deposit in sortedByAmountDesc) {
            val d = deposit.dayOfShamsiMonth
            val matchingCluster = clusters.firstOrNull { cluster ->
                val anchorDay = cluster.first().dayOfShamsiMonth
                circularAbsoluteDistance(anchorDay, d) <= 2
            }
            if (matchingCluster != null) {
                matchingCluster.add(deposit)
            } else {
                clusters.add(mutableListOf(deposit))
            }
        }

        return clusters.map { group ->
            val days = group.map { it.dayOfShamsiMonth }.sorted()
            val medianDay = days[days.size / 2]
            val avgAmount = group.map { it.amountToman }.average().roundToLong()
            val maxDeviation = days.maxOf { circularAbsoluteDistance(medianDay, it) }
            val consistency = (100 - maxDeviation * 10).coerceIn(65, 100)
            val mainSource = group.maxByOrNull { it.amountToman }?.titleFa ?: "درآمد ماهانه"
            DetectedPaydayCluster(
                medianDayOfMonth = medianDay,
                averageAmountToman = avgAmount,
                sampleCount = group.size,
                consistencyPercent = consistency,
                primarySourceTitleFa = mainSource
            )
        }.sortedByDescending { it.averageAmountToman }
    }

    private fun computeDeadlineSafetyScore(
        candidateDay: Int,
        primaryPayday: Int,
        cycleInstallments: List<InstallmentEntity>
    ): Int {
        if (cycleInstallments.isEmpty()) {
            // Without active installments, slightly prefer 2..5 days after primary payday
            val dist = circularForwardDistance(primaryPayday, candidateDay)
            return when (dist) {
                in 2..5 -> 26
                in 1..7 -> 20
                else -> 14
            }
        }
        val totalAmount = cycleInstallments.sumOf { it.remainingToman }.coerceAtLeast(1L)
        var weightedSafety = 0.0
        for (inst in cycleInstallments) {
            val dueDay = JalaliCalendarEngine.parseJalali(inst.dueDateJalali).day.coerceIn(1, SHAMSI_STANDARD_DAYS)
            val weight = inst.remainingToman.toDouble() / totalAmount.toDouble()
            // Measure how early candidateDay is relative to dueDay within the billing cycle starting at primaryPayday
            val payOffset = circularForwardDistance(primaryPayday, candidateDay)
            val dueOffset = circularForwardDistance(primaryPayday, dueDay)
            val safetyForThisInst = when {
                payOffset <= dueOffset -> {
                    // Paid on or before due date! Bonus for 1..7 days buffer
                    val leadDays = dueOffset - payOffset
                    when (leadDays) {
                        in 1..8 -> 1.0
                        0 -> 0.90
                        else -> 0.85
                    }
                }
                else -> {
                    // Paid after due date in this billing cycle -> low score
                    0.22
                }
            }
            weightedSafety += safetyForThisInst * weight
        }
        return (weightedSafety * 30.0).roundToInt().coerceIn(0, 30)
    }

    private fun computeHistoricalAlignmentScore(
        candidateDay: Int,
        paydays: List<DetectedPaydayCluster>,
        paidHistoryDays: List<Int>
    ): Int {
        if (paydays.isEmpty()) return 12
        val totalIncome = paydays.sumOf { it.averageAmountToman }.coerceAtLeast(1L)
        var paydayScore = 0.0
        for (cluster in paydays) {
            val weight = cluster.averageAmountToman.toDouble() / totalIncome.toDouble()
            val forwardDist = circularForwardDistance(cluster.medianDayOfMonth, candidateDay)
            val proximityPoints = when (forwardDist) {
                PAYDAY_SETTLEMENT_BUFFER_DAYS, PAYDAY_SETTLEMENT_BUFFER_DAYS + 1 -> 20.0 // +2 or +3 days after payday (sweet spot!)
                1, 4 -> 16.0
                5 -> 12.0
                0 -> 9.0 // exact payday carries minor deposit-delay risk
                in 6..9 -> 7.0
                else -> 2.0
            }
            paydayScore += proximityPoints * weight
        }

        val habitBonus = if (paidHistoryDays.isNotEmpty()) {
            val avgHabitDist = paidHistoryDays.map { circularAbsoluteDistance(it, candidateDay) }.average()
            when {
                avgHabitDist <= 2.0 -> 5.0
                avgHabitDist <= 5.0 -> 3.0
                else -> 1.0
            }
        } else 2.5

        return (paydayScore + habitBonus).roundToInt().coerceIn(0, 25)
    }

    private fun chooseBestCycleDayForDueDate(
        dueDay: Int,
        primaryDay: Int,
        secondaryDay: Int
    ): Int {
        // Choose whichever cycle day precedes dueDay with the shortest safe forward distance
        val distFromPrimary = circularForwardDistance(primaryDay, dueDay)
        val distFromSecondary = circularForwardDistance(secondaryDay, dueDay)
        return if (distFromSecondary <= distFromPrimary) secondaryDay else primaryDay
    }

    private fun buildTargetScheduledDate(
        dueYear: Int,
        dueMonth: Int,
        dueDay: Int,
        targetPayDay: Int,
        todayYear: Int,
        todayMonth: Int
    ): String {
        // Place payment on `targetPayDay` in the same month if targetPayDay <= dueDay,
        // otherwise in the preceding Shamsi month (clamped to current month if already past)
        val (targetY, targetM) = if (targetPayDay <= dueDay) {
            dueYear to dueMonth
        } else {
            val (prevY, prevM) = JalaliCalendarEngine.shiftJalaliYearMonth(dueYear, dueMonth, -1)
            if (prevY > todayYear || (prevY == todayYear && prevM >= todayMonth)) {
                prevY to prevM
            } else {
                dueYear to dueMonth
            }
        }
        val clampedDay = targetPayDay.coerceIn(1, JalaliCalendarEngine.getJalaliMonthLength(targetY, targetM))
        return JalaliCalendarEngine.normalizeJalaliDate("$targetY/$targetM/$clampedDay")
    }

    internal fun wrapDay(day: Int): Int {
        return ((day - 1).mod(SHAMSI_STANDARD_DAYS)) + 1
    }

    internal fun circularForwardDistance(fromDay: Int, toDay: Int): Int {
        return (toDay - fromDay).mod(SHAMSI_STANDARD_DAYS)
    }

    internal fun circularAbsoluteDistance(dayA: Int, dayB: Int): Int {
        val fwd = circularForwardDistance(dayA, dayB)
        return minOf(fwd, SHAMSI_STANDARD_DAYS - fwd)
    }
}
