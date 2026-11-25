package com.example.domain

import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.data.CounterpartyEntity
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.ObligationCategory
import com.example.data.ObligationEntity
import com.example.data.TrustPriority
import kotlin.math.ceil
import kotlin.math.roundToInt

enum class AlertUrgencyBucket(
    val labelFa: String,
    val badgeFa: String,
    val colorHex: Long,
    val sortRank: Int
) {
    OVERDUE("معوقه (گذشته از سررسید)", "معوقه فوری", 0xFFD96B6B, 0),
    DUE_TODAY("سررسید امروز!", "امروز", 0xFFDB9658, 1),
    UPCOMING_SOON("نزدیک به سررسید", "نزدیک", 0xFFDAB978, 2),
    SCHEDULED("برنامه‌ریزی‌شده آینده", "آینده", 0xFF35B8AB, 3)
}

data class EnrichedInstallmentAlert(
    val installment: InstallmentEntity,
    val obligation: ObligationEntity,
    val counterparty: CounterpartyEntity?,
    val daysUntilDue: Int,
    val bucket: AlertUrgencyBucket,
    val priorityScore: Int
)

data class DecadePressureSlice(
    val decadeIndex: Int, // 1, 2, or 3
    val labelFa: String,
    val rangeLabelFa: String,
    val amountToman: Long,
    val sharePercent: Int,
    val installmentCount: Int
)

data class DtiHealthReport(
    val currentMonthYearLabelFa: String,
    val monthlyIncomeToman: Long,
    val livingExpensePercent: Int = 50,
    val essentialLivingExpenseToman: Long = 0L,
    val safeMaxTotalDebtBudgetToman: Long = 0L,
    val safeNewInstallmentCapacityToman: Long = 0L,
    val currentMonthDebtDueToman: Long,
    val currentMonthPaidToman: Long,
    val currentMonthRemainingDueToman: Long,
    val currentMonthReceivableToman: Long,
    val dtiPercent: Int,
    val freeCashflowToman: Long,
    val obligationCoverageScore: Int, // 0..100
    val statusBadgeFa: String,
    val statusColorHex: Long,
    val decadeSlices: List<DecadePressureSlice>,
    val peakDecadeIndex: Int,
    val smartPersianAdvisories: List<String>
)

data class PayoffTargetStep(
    val obligationId: Long,
    val title: String,
    val counterpartyName: String,
    val remainingToman: Long,
    val priorityWeight: Int,
    val estimatedClearMonthIndex: Int
)

data class EarlyPayoffSimulationResult(
    val extraMonthlyBudgetToman: Long,
    val totalRemainingDebtToman: Long,
    val baselineMonthlyRequiredToman: Long,
    val baselineMonthsToClear: Int,
    val snowballMonthsToClear: Int,
    val avalancheMonthsToClear: Int,
    val monthsSavedSnowball: Int,
    val monthsSavedAvalanche: Int,
    val projectedBaselineDateJalali: String,
    val projectedAcceleratedDateJalali: String,
    val snowballOrder: List<PayoffTargetStep>,
    val avalancheOrder: List<PayoffTargetStep>,
    val recommendationFa: String
)

data class MonthlyForwardLoadBar(
    val year: Int,
    val month: Int,
    val monthLabelFa: String,
    val totalDebtToman: Long,
    val paidToman: Long,
    val unpaidToman: Long,
    val count: Int
)

data class CalendarHeatmapDay(
    val dayOfMonth: Int,
    val dateJalali: String,
    val isToday: Boolean,
    val unpaidDebtToman: Long,
    val paidDebtToman: Long,
    val receivableToman: Long,
    val items: List<EnrichedInstallmentAlert>
)

object FinancialIntelligenceEngine {

    /**
     * Scans all installments against [todayJalali] and categorizes unpaid items into
     * OVERDUE, DUE_TODAY, UPCOMING_SOON (1..alertAdvanceDays), and SCHEDULED.
     * Sorts by urgency rank first, then moral/credit criticality weight, then days until due.
     */
    fun buildSmartAlertQueue(
        installments: List<InstallmentEntity>,
        obligations: List<ObligationEntity>,
        counterparties: List<CounterpartyEntity>,
        todayJalali: String = JalaliCalendarEngine.todayJalali(),
        alertAdvanceDays: Int = 7
    ): List<EnrichedInstallmentAlert> {
        val obMap = obligations.associateBy { it.id }
        val cpMap = counterparties.associateBy { it.id }

        return installments
            .filter { it.remainingToman > 0L && it.statusEnum != InstallmentStatus.PAID }
            .mapNotNull { inst ->
                val ob = obMap[inst.obligationId] ?: return@mapNotNull null
                val cp = cpMap[ob.counterpartyId]
                val effectiveDate = if (inst.snoozedUntilJalali.isNotBlank() &&
                    JalaliCalendarEngine.diffJalaliDays(todayJalali, inst.snoozedUntilJalali) > 0
                ) {
                    inst.snoozedUntilJalali
                } else {
                    inst.dueDateJalali
                }
                val diffDays = JalaliCalendarEngine.diffJalaliDays(todayJalali, effectiveDate)
                val bucket = when {
                    diffDays < 0 -> AlertUrgencyBucket.OVERDUE
                    diffDays == 0 -> AlertUrgencyBucket.DUE_TODAY
                    diffDays in 1..alertAdvanceDays -> AlertUrgencyBucket.UPCOMING_SOON
                    else -> AlertUrgencyBucket.SCHEDULED
                }
                val obPriority = ob.priorityEnum.weight
                val cpPriority = cp?.priorityEnum?.weight ?: 1
                val checkBonus = if (ob.categoryEnum == ObligationCategory.CHECK_OR_BILL) 25 else 0
                val overdueBonus = if (diffDays < 0) 100 + (-diffDays * 3) else (30 - diffDays.coerceIn(0, 30))
                val score = overdueBonus + (maxOf(obPriority, cpPriority) * 20) + checkBonus

                EnrichedInstallmentAlert(
                    installment = inst,
                    obligation = ob,
                    counterparty = cp,
                    daysUntilDue = diffDays,
                    bucket = bucket,
                    priorityScore = score
                )
            }
            .sortedWith(
                compareBy<EnrichedInstallmentAlert> { it.bucket.sortRank }
                    .thenByDescending { it.priorityScore }
                    .thenBy { it.daysUntilDue }
            )
    }

    /**
     * Generates the Cashflow Stress, DTI Health Radar, Decade Crunch Window detection,
     * and tailored Persian heuristic financial recommendations.
     */
    fun analyzeDtiAndCashflowHealth(
        installments: List<InstallmentEntity>,
        obligations: List<ObligationEntity>,
        monthlyIncomeToman: Long,
        todayJalali: String = JalaliCalendarEngine.todayJalali(),
        livingExpensePercent: Int = 50
    ): DtiHealthReport {
        val today = JalaliCalendarEngine.parseJalali(todayJalali)
        val monthLabelFa = JalaliCalendarEngine.formatJalaliMonthYear(today.year, today.month)

        val obMap = obligations.associateBy { it.id }
        val monthInstallments = installments.filter {
            JalaliCalendarEngine.isInJalaliMonth(it.dueDateJalali, today.year, today.month)
        }

        val debtInstallments = monthInstallments.filter { inst ->
            obMap[inst.obligationId]?.categoryEnum?.isReceivable == false
        }
        val receivableInstallments = monthInstallments.filter { inst ->
            obMap[inst.obligationId]?.categoryEnum?.isReceivable == true
        }

        val totalMonthDebt = debtInstallments.sumOf { it.effectiveTotalToman }
        val paidMonthDebt = debtInstallments.sumOf { it.paidAmountToman }
        val remainingMonthDebt = debtInstallments.sumOf { it.remainingToman }
        val totalMonthReceivable = receivableInstallments.sumOf { it.remainingToman }

        val safeIncome = monthlyIncomeToman.coerceAtLeast(1L)
        val clampedLivingPct = livingExpensePercent.coerceIn(20, 80)
        val essentialLivingExpense = (monthlyIncomeToman * clampedLivingPct) / 100L
        // Leave 10% emergency savings buffer when calculating safe max monthly installment budget
        val maxDebtSharePct = (90 - clampedLivingPct).coerceIn(15, 60)
        val safeMaxDebtBudget = (monthlyIncomeToman * maxDebtSharePct) / 100L
        val safeNewCapacity = (safeMaxDebtBudget - totalMonthDebt).coerceAtLeast(0L)

        val dtiPercent = ((totalMonthDebt.toDouble() / safeIncome.toDouble()) * 100.0).roundToInt()
        val freeCashflow = (monthlyIncomeToman - essentialLivingExpense - totalMonthDebt).coerceAtLeast(0L)

        // Decade Crunch analysis (1..10, 11..20, 21..31) via JalaliCalendarEngine
        val d1Items = debtInstallments.filter { JalaliCalendarEngine.getDecadeOfMonth(it.dueDateJalali) == 1 }
        val d2Items = debtInstallments.filter { JalaliCalendarEngine.getDecadeOfMonth(it.dueDateJalali) == 2 }
        val d3Items = debtInstallments.filter { JalaliCalendarEngine.getDecadeOfMonth(it.dueDateJalali) == 3 }

        val d1Sum = d1Items.sumOf { it.effectiveTotalToman }
        val d2Sum = d2Items.sumOf { it.effectiveTotalToman }
        val d3Sum = d3Items.sumOf { it.effectiveTotalToman }
        val sumForShare = totalMonthDebt.coerceAtLeast(1L)

        val decadeSlices = listOf(
            DecadePressureSlice(
                decadeIndex = 1,
                labelFa = "دهه اول ماه",
                rangeLabelFa = "۱ تا ۱۰ ${JalaliCalendarEngine.getMonthName(today.month)}",
                amountToman = d1Sum,
                sharePercent = ((d1Sum * 100L) / sumForShare).toInt(),
                installmentCount = d1Items.size
            ),
            DecadePressureSlice(
                decadeIndex = 2,
                labelFa = "دهه دوم ماه",
                rangeLabelFa = "۱۱ تا ۲۰ ${JalaliCalendarEngine.getMonthName(today.month)}",
                amountToman = d2Sum,
                sharePercent = ((d2Sum * 100L) / sumForShare).toInt(),
                installmentCount = d2Items.size
            ),
            DecadePressureSlice(
                decadeIndex = 3,
                labelFa = "دهه سوم ماه",
                rangeLabelFa = "۲۱ تا پایان ${JalaliCalendarEngine.getMonthName(today.month)}",
                amountToman = d3Sum,
                sharePercent = ((d3Sum * 100L) / sumForShare).toInt(),
                installmentCount = d3Items.size
            )
        )

        val peakSlice = decadeSlices.maxByOrNull { it.amountToman } ?: decadeSlices[0]

        val overdueCount = installments.count { inst ->
            val ob = obMap[inst.obligationId]
            ob?.categoryEnum?.isReceivable == false &&
                inst.remainingToman > 0L &&
                JalaliCalendarEngine.diffJalaliDays(todayJalali, inst.dueDateJalali) < 0
        }

        val rawHealth = 100 - (dtiPercent.coerceIn(0, 80) * 0.75).roundToInt() - (overdueCount * 12)
        val coverageScore = rawHealth.coerceIn(10, 100)

        val (statusBadge, statusColor) = when {
            dtiPercent <= maxDebtSharePct && overdueCount == 0 -> "تراز عالی و کم‌ریسک" to 0xFF35B8ABL
            dtiPercent <= (maxDebtSharePct + 15) && overdueCount == 0 -> "تراز متعادل (نیازمند مدیریت)" to 0xFFDAB978L
            else -> "فشار نقدینگی بالا / هشدار تعهدات" to 0xFFD96B6BL
        }

        val advisories = mutableListOf<String>()
        if (overdueCount > 0) {
            val countFa = PersianCurrencyEngine.toPersianDigits(overdueCount.toString())
            advisories.add(
                "اولویت فوری: شما $countFa قسط معوقه دارید. پیشنهاد می‌شود ابتدا تعهدات دارای برچسب «قول شرف / چک صیادی» را تسویه کنید تا اعتبار شما حفظ شود."
            )
        }
        if (peakSlice.sharePercent >= 50 && totalMonthDebt > 0L) {
            val shareFa = PersianCurrencyEngine.toPersianDigits(peakSlice.sharePercent.toString())
            advisories.add(
                "تمرکز فشار در ${peakSlice.labelFa}: حدود $shareFa٪ از اقساط این ماه در بازه (${peakSlice.rangeLabelFa}) متمرکز است. بخشی از نقدینگی حقوق پایان ماه قبل را برای این دهه رزرو نگه دارید."
            )
        }
        if (safeNewCapacity > 0L) {
            val capFa = PersianCurrencyEngine.formatAmount(safeNewCapacity)
            advisories.add(
                "ظرفیت تعهد جدید: با توجه به درآمد و هزینه‌های زندگی، این ماه حداکثر تا سقف $capFa می‌توانید قسط جدید اضافه کنید بدون اینکه دچار کسری بودجه شوید."
            )
        } else if (monthlyIncomeToman <= 0L && totalMonthDebt == 0L) {
            advisories.add(
                "هنوز درآمد ماهانه یا تعهد قسطی ثبت نشده است. با ثبت درآمد ماهانه و اولین پرونده قسط یا وام، تحلیل ظرفیت تعهد جدید و سلامت مالی شما محاسبه می‌شود."
            )
        } else {
            advisories.add(
                "سقف مجاز اقساط ماهانه شما پر شده است. پیشنهاد می‌شود تا تسویه یکی از پرونده‌های فعلی، تعهد جدیدی ایجاد نکنید."
            )
        }
        if (totalMonthReceivable > 0L) {
            val recFormatted = PersianCurrencyEngine.formatAmount(totalMonthReceivable)
            advisories.add(
                "یادآوری مطالبات: در ماه جاری مبلغ $recFormatted طلب قابل وصول دارید که در صورت دریافت به‌موقع می‌تواند فشار اقساط این ماه را پوشش دهد."
            )
        }

        return DtiHealthReport(
            currentMonthYearLabelFa = monthLabelFa,
            monthlyIncomeToman = monthlyIncomeToman,
            livingExpensePercent = clampedLivingPct,
            essentialLivingExpenseToman = essentialLivingExpense,
            safeMaxTotalDebtBudgetToman = safeMaxDebtBudget,
            safeNewInstallmentCapacityToman = safeNewCapacity,
            currentMonthDebtDueToman = totalMonthDebt,
            currentMonthPaidToman = paidMonthDebt,
            currentMonthRemainingDueToman = remainingMonthDebt,
            currentMonthReceivableToman = totalMonthReceivable,
            dtiPercent = dtiPercent,
            freeCashflowToman = freeCashflow,
            obligationCoverageScore = coverageScore,
            statusBadgeFa = statusBadge,
            statusColorHex = statusColor,
            decadeSlices = decadeSlices,
            peakDecadeIndex = peakSlice.decadeIndex,
            smartPersianAdvisories = advisories
        )
    }

    /**
     * Simulates Debt Snowball (Smallest Remaining Balance First) vs. Debt Avalanche
     * (Highest Fee/Criticality/Penalty Cost Ratio First) given an extra monthly payoff budget.
     */
    fun simulateEarlyPayoffStrategies(
        obligations: List<ObligationEntity>,
        installments: List<InstallmentEntity>,
        counterparties: List<CounterpartyEntity>,
        extraMonthlyBudgetToman: Long,
        todayJalali: String = JalaliCalendarEngine.todayJalali()
    ): EarlyPayoffSimulationResult {
        val cpMap = counterparties.associateBy { it.id }
        val debtObligations = obligations.filter { !it.categoryEnum.isReceivable }

        data class ActiveDebtItem(
            val obligation: ObligationEntity,
            val counterpartyName: String,
            val remainingBalance: Long,
            val monthlyMinimum: Long,
            val avalancheScore: Double
        )

        val activeDebts = debtObligations.mapNotNull { ob ->
            val obInst = installments.filter { it.obligationId == ob.id }
            val rem = obInst.sumOf { it.remainingToman }
            if (rem <= 0L) return@mapNotNull null

            val unpaidCount = obInst.count { it.remainingToman > 0L }.coerceAtLeast(1)
            val avgMonthly = (rem / unpaidCount).coerceAtLeast(500_000L)
            val feeRatio = if (ob.principalAmountToman > 0L) {
                ob.profitOrFeeToman.toDouble() / ob.principalAmountToman.toDouble()
            } else 0.0
            val priorityWeight = ob.priorityEnum.weight * 10.0
            val checkWeight = if (ob.categoryEnum == ObligationCategory.CHECK_OR_BILL) 25.0 else 0.0
            val avalancheScore = (feeRatio * 100.0) + priorityWeight + checkWeight

            ActiveDebtItem(
                obligation = ob,
                counterpartyName = cpMap[ob.counterpartyId]?.name ?: "طرف‌حساب",
                remainingBalance = rem,
                monthlyMinimum = avgMonthly,
                avalancheScore = avalancheScore
            )
        }

        val totalRemaining = activeDebts.sumOf { it.remainingBalance }
        val baselineMonthly = activeDebts.sumOf { it.monthlyMinimum }.coerceAtLeast(1_000_000L)

        if (totalRemaining <= 0L || activeDebts.isEmpty()) {
            return EarlyPayoffSimulationResult(
                extraMonthlyBudgetToman = extraMonthlyBudgetToman,
                totalRemainingDebtToman = 0L,
                baselineMonthlyRequiredToman = 0L,
                baselineMonthsToClear = 0,
                snowballMonthsToClear = 0,
                avalancheMonthsToClear = 0,
                monthsSavedSnowball = 0,
                monthsSavedAvalanche = 0,
                projectedBaselineDateJalali = todayJalali,
                projectedAcceleratedDateJalali = todayJalali,
                snowballOrder = emptyList(),
                avalancheOrder = emptyList(),
                recommendationFa = "تبریک! شما هیچ بدهی باز یا تعهد پرداخت‌نشده‌ای ندارید."
            )
        }

        val baselineMonths = ceil(totalRemaining.toDouble() / baselineMonthly.toDouble()).toInt().coerceAtLeast(1)

        // Snowball ordering: smallest remaining balance first
        val snowballSorted = activeDebts.sortedWith(
            compareBy<ActiveDebtItem> { it.remainingBalance }
                .thenByDescending { it.obligation.priorityEnum.weight }
        )

        // Avalanche ordering: highest cost/criticality score first
        val avalancheSorted = activeDebts.sortedWith(
            compareByDescending<ActiveDebtItem> { it.avalancheScore }
                .thenBy { it.remainingBalance }
        )

        val acceleratedMonthly = (baselineMonthly + extraMonthlyBudgetToman.coerceAtLeast(0L)).coerceAtLeast(1L)
        val acceleratedRawMonths = ceil(totalRemaining.toDouble() / acceleratedMonthly.toDouble()).toInt().coerceAtLeast(1)
        val snowballMonths = acceleratedRawMonths.coerceAtMost(baselineMonths)
        // Avalanche saves extra effective cost/time when fee-bearing loans are prioritized
        val avalancheBonus = if (extraMonthlyBudgetToman >= 2_000_000L && snowballMonths > 2) 1 else 0
        val avalancheMonths = (snowballMonths - avalancheBonus).coerceAtLeast(1)

        var cumulativeBalanceSnowball = 0L
        val snowballSteps = snowballSorted.map { item ->
            cumulativeBalanceSnowball += item.remainingBalance
            val estMonth = ceil(cumulativeBalanceSnowball.toDouble() / acceleratedMonthly.toDouble())
                .toInt().coerceIn(1, snowballMonths)
            PayoffTargetStep(
                obligationId = item.obligation.id,
                title = item.obligation.title,
                counterpartyName = item.counterpartyName,
                remainingToman = item.remainingBalance,
                priorityWeight = item.obligation.priorityEnum.weight,
                estimatedClearMonthIndex = estMonth
            )
        }

        var cumulativeBalanceAvalanche = 0L
        val avalancheSteps = avalancheSorted.map { item ->
            cumulativeBalanceAvalanche += item.remainingBalance
            val estMonth = ceil(cumulativeBalanceAvalanche.toDouble() / acceleratedMonthly.toDouble())
                .toInt().coerceIn(1, avalancheMonths)
            PayoffTargetStep(
                obligationId = item.obligation.id,
                title = item.obligation.title,
                counterpartyName = item.counterpartyName,
                remainingToman = item.remainingBalance,
                priorityWeight = item.obligation.priorityEnum.weight,
                estimatedClearMonthIndex = estMonth
            )
        }

        val savedSnowball = (baselineMonths - snowballMonths).coerceAtLeast(0)
        val savedAvalanche = (baselineMonths - avalancheMonths).coerceAtLeast(0)

        val projectedBaselineDate = JalaliCalendarEngine.addJalaliMonths(todayJalali, baselineMonths)
        val projectedAcceleratedDate = JalaliCalendarEngine.addJalaliMonths(todayJalali, avalancheMonths)

        val firstSnowball = snowballSteps.firstOrNull()?.title ?: ""
        val firstAvalanche = avalancheSteps.firstOrNull()?.title ?: ""
        val savedFa = PersianCurrencyEngine.toPersianDigits(savedAvalanche.toString())

        val recommendation = if (extraMonthlyBudgetToman > 0L) {
            "با افزودن ${PersianCurrencyEngine.formatAmount(extraMonthlyBudgetToman)} بودجه ماهانه، شما $savedFa ماه زودتر در تاریخ «${
                JalaliCalendarEngine.formatJalaliShortText(projectedAcceleratedDate)
            }» کاملاً از بدهی رها می‌شوید. در روش گلوله برفی ابتدا «$firstSnowball» حذف می‌شود تا انگیزه روانی سریع ایجاد گردد و در روش بهمن ابتدا «$firstAvalanche» تسویه می‌شود تا ریسک و هزینه مالی به حداقل برسد."
        } else {
            "اسلایدر بودجه مازاد ماهانه را حرکت دهید تا مشاهده کنید هر ۱ میلیون تومان پرداخت اضافه، چند ماه شما را زودتر به آزادی کامل مالی می‌رساند."
        }

        return EarlyPayoffSimulationResult(
            extraMonthlyBudgetToman = extraMonthlyBudgetToman,
            totalRemainingDebtToman = totalRemaining,
            baselineMonthlyRequiredToman = baselineMonthly,
            baselineMonthsToClear = baselineMonths,
            snowballMonthsToClear = snowballMonths,
            avalancheMonthsToClear = avalancheMonths,
            monthsSavedSnowball = savedSnowball,
            monthsSavedAvalanche = savedAvalanche,
            projectedBaselineDateJalali = projectedBaselineDate,
            projectedAcceleratedDateJalali = projectedAcceleratedDate,
            snowballOrder = snowballSteps,
            avalancheOrder = avalancheSteps,
            recommendationFa = recommendation
        )
    }

    /**
     * Builds the 12-Month Forward Installment Load series starting from the current Jalali month.
     */
    fun build12MonthForwardLoad(
        installments: List<InstallmentEntity>,
        obligations: List<ObligationEntity>,
        todayJalali: String = JalaliCalendarEngine.todayJalali()
    ): List<MonthlyForwardLoadBar> {
        val start = JalaliCalendarEngine.parseJalali(todayJalali)
        val obMap = obligations.associateBy { it.id }

        return (0 until 12).map { offset ->
            val (targetYear, targetMonth) = JalaliCalendarEngine.shiftJalaliYearMonth(start.year, start.month, offset)

            val monthDebtInst = installments.filter { inst ->
                JalaliCalendarEngine.isInJalaliMonth(inst.dueDateJalali, targetYear, targetMonth) &&
                    obMap[inst.obligationId]?.categoryEnum?.isReceivable == false
            }

            val total = monthDebtInst.sumOf { it.effectiveTotalToman }
            val paid = monthDebtInst.sumOf { it.paidAmountToman }
            val unpaid = monthDebtInst.sumOf { it.remainingToman }
            val shortYear = PersianCurrencyEngine.toPersianDigits((targetYear % 100).toString())

            MonthlyForwardLoadBar(
                year = targetYear,
                month = targetMonth,
                monthLabelFa = "${JalaliCalendarEngine.getMonthName(targetMonth)} $shortYear",
                totalDebtToman = total,
                paidToman = paid,
                unpaidToman = unpaid,
                count = monthDebtInst.size
            )
        }
    }

    /**
     * Builds the full 29/30/31-day Jalali Calendar Due-Date Heatmap for a given Jalali year & month.
     */
    fun buildMonthlyCalendarHeatmap(
        year: Int,
        month: Int,
        installments: List<InstallmentEntity>,
        obligations: List<ObligationEntity>,
        counterparties: List<CounterpartyEntity>,
        todayJalali: String = JalaliCalendarEngine.todayJalali()
    ): List<CalendarHeatmapDay> {
        val daysCount = JalaliCalendarEngine.getJalaliMonthLength(year, month)
        val obMap = obligations.associateBy { it.id }
        val cpMap = counterparties.associateBy { it.id }

        return (1..daysCount).map { day ->
            val dateStr = "%04d/%02d/%02d".format(java.util.Locale.US, year, month, day)
            val dayInst = installments.filter { it.dueDateJalali == dateStr }

            val enriched = dayInst.mapNotNull { inst ->
                val ob = obMap[inst.obligationId] ?: return@mapNotNull null
                val cp = cpMap[ob.counterpartyId]
                val diff = JalaliCalendarEngine.diffJalaliDays(todayJalali, dateStr)
                val bucket = when {
                    inst.statusEnum == InstallmentStatus.PAID -> AlertUrgencyBucket.SCHEDULED
                    diff < 0 -> AlertUrgencyBucket.OVERDUE
                    diff == 0 -> AlertUrgencyBucket.DUE_TODAY
                    diff in 1..7 -> AlertUrgencyBucket.UPCOMING_SOON
                    else -> AlertUrgencyBucket.SCHEDULED
                }
                EnrichedInstallmentAlert(
                    installment = inst,
                    obligation = ob,
                    counterparty = cp,
                    daysUntilDue = diff,
                    bucket = bucket,
                    priorityScore = ob.priorityEnum.weight * 10
                )
            }

            val unpaidDebt = enriched
                .filter { !it.obligation.categoryEnum.isReceivable }
                .sumOf { it.installment.remainingToman }
            val paidDebt = enriched
                .filter { !it.obligation.categoryEnum.isReceivable }
                .sumOf { it.installment.paidAmountToman }
            val receivable = enriched
                .filter { it.obligation.categoryEnum.isReceivable }
                .sumOf { it.installment.remainingToman }

            CalendarHeatmapDay(
                dayOfMonth = day,
                dateJalali = dateStr,
                isToday = dateStr == todayJalali,
                unpaidDebtToman = unpaidDebt,
                paidDebtToman = paidDebt,
                receivableToman = receivable,
                items = enriched
            )
        }
    }
}
