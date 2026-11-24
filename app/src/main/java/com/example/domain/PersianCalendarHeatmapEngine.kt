package com.example.domain

import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.data.CounterpartyEntity
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.ObligationEntity
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Five-level thermal heat intensity scale for upcoming installments across a Persian (Shamsi) month.
 */
enum class HeatmapIntensityLevel(
    val rank: Int,
    val labelFa: String,
    val shortLabelFa: String,
    val accentColorHex: Long
) {
    NONE(
        rank = 0,
        labelFa = "آرام (بدون قسط)",
        shortLabelFa = "آرام",
        accentColorHex = 0xFF64748B
    ),
    LOW(
        rank = 1,
        labelFa = "فشار سبک",
        shortLabelFa = "سبک",
        accentColorHex = 0xFF35B8AB
    ),
    MODERATE(
        rank = 2,
        labelFa = "فشار متوسط",
        shortLabelFa = "متوسط",
        accentColorHex = 0xFFDAB978
    ),
    HIGH(
        rank = 3,
        labelFa = "فشار بالا",
        shortLabelFa = "پرفشار",
        accentColorHex = 0xFFE08B4F
    ),
    CRITICAL(
        rank = 4,
        labelFa = "اوج فشار بحرانی",
        shortLabelFa = "بحرانی",
        accentColorHex = 0xFFD96B6B
    )
}

/**
 * Enriched daily cell for the Persian calendar heatmap grid.
 */
data class EnrichedHeatmapDayCell(
    val dayOfMonth: Int,
    val dateJalali: String,
    val weekdayIndex: Int, // 0 = Shanbeh .. 6 = Jomeh
    val decadeOfMonth: Int, // 1 (1..10), 2 (11..20), 3 (21..end)
    val isToday: Boolean,
    val isPastDay: Boolean,
    val isUpcomingOrToday: Boolean,
    val upcomingUnpaidDebtToman: Long,
    val paidDebtToman: Long,
    val receivableToman: Long,
    val totalDebtDueToman: Long,
    val upcomingInstallmentsCount: Int,
    val overdueInstallmentsCount: Int,
    val totalInstallmentsCount: Int,
    val normalizedIntensity: Float, // 0f..1f
    val intensityLevel: HeatmapIntensityLevel,
    val shareOfMonthlyUpcomingPercent: Int,
    val isInHighPressurePeriod: Boolean,
    val items: List<EnrichedInstallmentAlert>
)

/**
 * Detected multi-day or single-peak high-pressure cluster window within the Persian month.
 */
data class HighPressurePeriodWindow(
    val id: String,
    val startDay: Int,
    val endDay: Int,
    val rangeLabelFa: String,
    val totalUpcomingDebtToman: Long,
    val installmentsCount: Int,
    val shareOfMonthlyUpcomingPercent: Int,
    val peakIntensityLevel: HeatmapIntensityLevel,
    val severityBadgeFa: String,
    val advisoryFa: String
)

/**
 * Pressure summary for one of the three Persian month decades (دهه اول، دهه دوم، دهه سوم).
 */
data class PersianDecadeHeatSummary(
    val decadeIndex: Int, // 1, 2, 3
    val titleFa: String,
    val dayRangeFa: String,
    val startDay: Int,
    val endDay: Int,
    val upcomingDebtToman: Long,
    val paidDebtToman: Long,
    val installmentsCount: Int,
    val shareOfMonthlyUpcomingPercent: Int,
    val intensityLevel: HeatmapIntensityLevel,
    val isPeakDecade: Boolean
)

/**
 * Complete analytical model for the Persian Month Installment Intensity Calendar Heatmap.
 */
data class PersianMonthHeatmapReport(
    val year: Int,
    val month: Int,
    val monthNameFa: String,
    val monthYearLabelFa: String,
    val daysInMonth: Int,
    val firstDayWeekdayOffset: Int,
    val todayJalali: String,
    val totalUpcomingUnpaidToman: Long,
    val totalPaidDebtToman: Long,
    val totalReceivableToman: Long,
    val totalMonthlyDebtLoadToman: Long,
    val upcomingInstallmentsCount: Int,
    val highPressureDaysCount: Int,
    val peakDayCell: EnrichedHeatmapDayCell?,
    val cells: List<EnrichedHeatmapDayCell>,
    val highPressurePeriods: List<HighPressurePeriodWindow>,
    val decadeSummaries: List<PersianDecadeHeatSummary>,
    val peakDecadeIndex: Int?,
    val executiveSummaryFa: String
)

/**
 * Pure domain engine for computing the Persian (Shamsi) Calendar Installment Intensity Heatmap
 * and detecting High-Pressure Payment Periods (دوره‌های اوج فشار اقساط).
 */
object PersianCalendarHeatmapEngine {

    /**
     * Evaluates all installments falling in the specified Persian (Shamsi) [year] and [month],
     * assigns dynamic 5-tier thermal intensity levels, detects high-pressure cluster windows,
     * and computes Persian decade summaries.
     */
    fun buildPersianMonthHeatmap(
        year: Int,
        month: Int,
        installments: List<InstallmentEntity>,
        obligations: List<ObligationEntity>,
        counterparties: List<CounterpartyEntity>,
        monthlyIncomeToman: Long = 0L,
        todayJalali: String = JalaliCalendarEngine.todayJalali()
    ): PersianMonthHeatmapReport {
        val safeMonth = month.coerceIn(1, 12)
        val daysInMonth = JalaliCalendarEngine.getJalaliMonthLength(year, safeMonth)
        val monthNameFa = JalaliCalendarEngine.getMonthName(safeMonth)
        val monthYearLabelFa = JalaliCalendarEngine.formatJalaliMonthYear(year, safeMonth)
        val firstDateStr = "%04d/%02d/01".format(Locale.US, year, safeMonth)
        val firstDayOffset = JalaliCalendarEngine.getPersianWeekdayIndex(firstDateStr)

        val obMap = obligations.associateBy { it.id }
        val cpMap = counterparties.associateBy { it.id }

        // First pass: aggregate daily raw financials
        data class RawDayMetrics(
            val day: Int,
            val dateStr: String,
            val weekdayIdx: Int,
            val decade: Int,
            val isToday: Boolean,
            val isPast: Boolean,
            val unpaidDebt: Long,
            val paidDebt: Long,
            val receivable: Long,
            val totalDebtDue: Long,
            val upcomingCount: Int,
            val overdueCount: Int,
            val items: List<EnrichedInstallmentAlert>
        )

        val rawDays = (1..daysInMonth).map { day ->
            val dateStr = "%04d/%02d/%02d".format(Locale.US, year, safeMonth, day)
            val diffFromToday = JalaliCalendarEngine.diffJalaliDays(todayJalali, dateStr)
            val isToday = diffFromToday == 0
            val isPast = diffFromToday < 0
            val weekdayIdx = (firstDayOffset + (day - 1)) % 7
            val decade = when {
                day <= 10 -> 1
                day <= 20 -> 2
                else -> 3
            }

            val dayInsts = installments.filter {
                JalaliCalendarEngine.normalizeJalaliDate(it.dueDateJalali) == dateStr
            }

            val enrichedItems = dayInsts.mapNotNull { inst ->
                val ob = obMap[inst.obligationId] ?: return@mapNotNull null
                val cp = cpMap[ob.counterpartyId]
                val bucket = when {
                    inst.statusEnum == InstallmentStatus.PAID -> AlertUrgencyBucket.SCHEDULED
                    diffFromToday < 0 -> AlertUrgencyBucket.OVERDUE
                    diffFromToday == 0 -> AlertUrgencyBucket.DUE_TODAY
                    diffFromToday in 1..7 -> AlertUrgencyBucket.UPCOMING_SOON
                    else -> AlertUrgencyBucket.SCHEDULED
                }
                EnrichedInstallmentAlert(
                    installment = inst,
                    obligation = ob,
                    counterparty = cp,
                    daysUntilDue = diffFromToday,
                    bucket = bucket,
                    priorityScore = ob.priorityEnum.weight * 10
                )
            }

            val debtItems = enrichedItems.filter { !it.obligation.categoryEnum.isReceivable }
            val recItems = enrichedItems.filter { it.obligation.categoryEnum.isReceivable }

            val unpaidDebt = debtItems
                .filter { it.installment.statusEnum != InstallmentStatus.PAID }
                .sumOf { it.installment.remainingToman }
            val paidDebt = debtItems.sumOf { it.installment.paidAmountToman }
            val totalDebtDue = debtItems.sumOf { it.installment.effectiveTotalToman }
            val receivable = recItems
                .filter { it.installment.statusEnum != InstallmentStatus.PAID }
                .sumOf { it.installment.remainingToman }

            val upcomingCnt = debtItems.count {
                it.installment.statusEnum != InstallmentStatus.PAID && it.installment.remainingToman > 0L
            }
            val overdueCnt = debtItems.count {
                it.installment.statusEnum != InstallmentStatus.PAID &&
                    it.installment.remainingToman > 0L &&
                    diffFromToday < 0
            }

            RawDayMetrics(
                day = day,
                dateStr = dateStr,
                weekdayIdx = weekdayIdx,
                decade = decade,
                isToday = isToday,
                isPast = isPast,
                unpaidDebt = unpaidDebt,
                paidDebt = paidDebt,
                receivable = receivable,
                totalDebtDue = totalDebtDue,
                upcomingCount = upcomingCnt,
                overdueCount = overdueCnt,
                items = enrichedItems
            )
        }

        val totalUpcomingUnpaid = rawDays.sumOf { it.unpaidDebt }
        val totalPaidDebt = rawDays.sumOf { it.paidDebt }
        val totalReceivable = rawDays.sumOf { it.receivable }
        val totalMonthlyLoad = rawDays.sumOf { it.totalDebtDue }
        val totalUpcomingCount = rawDays.sumOf { it.upcomingCount }

        // Dynamic normalization scale:
        // Uses the maximum single-day unpaid debt in the month, anchored with a soft floor from monthly income
        // so tiny 500k debts don't falsely look "CRITICAL" if income is 100M, while still showing relative peaks.
        val peakDailyUnpaid = rawDays.maxOfOrNull { it.unpaidDebt } ?: 0L
        val incomeDailyReference = if (monthlyIncomeToman > 0L) {
            (monthlyIncomeToman * 12L) / 100L // 12% of monthly income in one day is high pressure
        } else {
            0L
        }
        val normalizationCeiling = maxOf(peakDailyUnpaid, incomeDailyReference, 1L)

        // Second pass: score each day's thermal intensity (0f..1f and HeatmapIntensityLevel)
        val prelimCells = rawDays.map { raw ->
            val ratio = if (raw.unpaidDebt > 0L) {
                (raw.unpaidDebt.toDouble() / normalizationCeiling.toDouble()).toFloat().coerceIn(0.08f, 1f)
            } else {
                0f
            }

            val shareOfMonthPct = if (totalUpcomingUnpaid > 0L && raw.unpaidDebt > 0L) {
                ((raw.unpaidDebt.toDouble() / totalUpcomingUnpaid.toDouble()) * 100.0)
                    .roundToInt()
                    .coerceIn(1, 100)
            } else {
                0
            }

            // Boost intensity if a single day has multiple unpaid installments or overdue debt
            val countBoost = if (raw.upcomingCount >= 3) 0.14f else if (raw.upcomingCount == 2) 0.06f else 0f
            val overdueBoost = if (raw.overdueCount > 0) 0.15f else 0f
            val effectiveScore = if (raw.unpaidDebt <= 0L) {
                0f
            } else {
                (ratio + countBoost + overdueBoost).coerceIn(0.10f, 1f)
            }

            val level = when {
                raw.unpaidDebt <= 0L -> HeatmapIntensityLevel.NONE
                effectiveScore >= 0.78f || shareOfMonthPct >= 35 -> HeatmapIntensityLevel.CRITICAL
                effectiveScore >= 0.52f || shareOfMonthPct >= 22 -> HeatmapIntensityLevel.HIGH
                effectiveScore >= 0.26f -> HeatmapIntensityLevel.MODERATE
                else -> HeatmapIntensityLevel.LOW
            }

            EnrichedHeatmapDayCell(
                dayOfMonth = raw.day,
                dateJalali = raw.dateStr,
                weekdayIndex = raw.weekdayIdx,
                decadeOfMonth = raw.decade,
                isToday = raw.isToday,
                isPastDay = raw.isPast,
                isUpcomingOrToday = !raw.isPast,
                upcomingUnpaidDebtToman = raw.unpaidDebt,
                paidDebtToman = raw.paidDebt,
                receivableToman = raw.receivable,
                totalDebtDueToman = raw.totalDebtDue,
                upcomingInstallmentsCount = raw.upcomingCount,
                overdueInstallmentsCount = raw.overdueCount,
                totalInstallmentsCount = raw.items.size,
                normalizedIntensity = effectiveScore,
                intensityLevel = level,
                shareOfMonthlyUpcomingPercent = shareOfMonthPct,
                isInHighPressurePeriod = false,
                items = raw.items
            )
        }

        // Third pass: detect High-Pressure Period windows (up to top 2 non-overlapping 4-to-5 day clusters)
        val highPressurePeriods = detectHighPressurePeriods(
            cells = prelimCells,
            totalUpcomingUnpaid = totalUpcomingUnpaid,
            monthNameFa = monthNameFa
        )

        val highPressureDaySet = buildSet {
            highPressurePeriods.forEach { window ->
                for (d in window.startDay..window.endDay) {
                    add(d)
                }
            }
        }

        val finalCells = prelimCells.map { cell ->
            cell.copy(
                isInHighPressurePeriod = cell.dayOfMonth in highPressureDaySet && cell.upcomingUnpaidDebtToman > 0L
            )
        }

        // Fourth pass: build 3 Persian Decade Heat Summaries (دهه اول، دوم، سوم)
        val decadeBounds = listOf(
            Triple(1, 1, 10),
            Triple(2, 11, 20),
            Triple(3, 21, daysInMonth)
        )
        val rawDecades = decadeBounds.map { (decIdx, startD, endD) ->
            val decCells = finalCells.filter { it.dayOfMonth in startD..endD }
            val decUpcoming = decCells.sumOf { it.upcomingUnpaidDebtToman }
            val decPaid = decCells.sumOf { it.paidDebtToman }
            val decCount = decCells.sumOf { it.upcomingInstallmentsCount }
            val sharePct = if (totalUpcomingUnpaid > 0L && decUpcoming > 0L) {
                ((decUpcoming.toDouble() / totalUpcomingUnpaid.toDouble()) * 100.0)
                    .roundToInt()
                    .coerceIn(1, 100)
            } else {
                0
            }
            val decLevel = when {
                decUpcoming <= 0L -> HeatmapIntensityLevel.NONE
                sharePct >= 50 -> HeatmapIntensityLevel.CRITICAL
                sharePct >= 35 -> HeatmapIntensityLevel.HIGH
                sharePct >= 18 -> HeatmapIntensityLevel.MODERATE
                else -> HeatmapIntensityLevel.LOW
            }
            val titleFa = when (decIdx) {
                1 -> "دهه اول"
                2 -> "دهه دوم"
                else -> "دهه سوم"
            }
            val rangeFa = "${PersianCurrencyEngine.toPersianDigits(startD.toString())} تا ${
                PersianCurrencyEngine.toPersianDigits(endD.toString())
            } $monthNameFa"

            PersianDecadeHeatSummary(
                decadeIndex = decIdx,
                titleFa = titleFa,
                dayRangeFa = rangeFa,
                startDay = startD,
                endDay = endD,
                upcomingDebtToman = decUpcoming,
                paidDebtToman = decPaid,
                installmentsCount = decCount,
                shareOfMonthlyUpcomingPercent = sharePct,
                intensityLevel = decLevel,
                isPeakDecade = false
            )
        }

        val peakDecadeIdx = rawDecades
            .filter { it.upcomingDebtToman > 0L }
            .maxByOrNull { it.upcomingDebtToman }
            ?.decadeIndex

        val decadeSummaries = rawDecades.map {
            it.copy(isPeakDecade = it.decadeIndex == peakDecadeIdx)
        }

        val peakDay = finalCells
            .filter { it.upcomingUnpaidDebtToman > 0L }
            .maxByOrNull { it.upcomingUnpaidDebtToman }

        val highPressureDaysCount = finalCells.count {
            it.intensityLevel == HeatmapIntensityLevel.HIGH ||
                it.intensityLevel == HeatmapIntensityLevel.CRITICAL
        }

        val executiveSummary = buildExecutiveSummaryFa(
            monthNameFa = monthNameFa,
            totalUpcomingUnpaid = totalUpcomingUnpaid,
            highPressurePeriods = highPressurePeriods,
            peakDay = peakDay,
            decadeSummaries = decadeSummaries
        )

        return PersianMonthHeatmapReport(
            year = year,
            month = safeMonth,
            monthNameFa = monthNameFa,
            monthYearLabelFa = monthYearLabelFa,
            daysInMonth = daysInMonth,
            firstDayWeekdayOffset = firstDayOffset,
            todayJalali = todayJalali,
            totalUpcomingUnpaidToman = totalUpcomingUnpaid,
            totalPaidDebtToman = totalPaidDebt,
            totalReceivableToman = totalReceivable,
            totalMonthlyDebtLoadToman = totalMonthlyLoad,
            upcomingInstallmentsCount = totalUpcomingCount,
            highPressureDaysCount = highPressureDaysCount,
            peakDayCell = peakDay,
            cells = finalCells,
            highPressurePeriods = highPressurePeriods,
            decadeSummaries = decadeSummaries,
            peakDecadeIndex = peakDecadeIdx,
            executiveSummaryFa = executiveSummary
        )
    }

    /**
     * Uses a 5-day sliding window over the Persian month to identify the hottest non-overlapping
     * high-pressure periods (e.g., days 12..16 concentrating 60% of the month's installments),
     * then trims leading/trailing empty days so the highlighted span tightly bounds actual due dates.
     */
    private fun detectHighPressurePeriods(
        cells: List<EnrichedHeatmapDayCell>,
        totalUpcomingUnpaid: Long,
        monthNameFa: String
    ): List<HighPressurePeriodWindow> {
        if (totalUpcomingUnpaid <= 0L || cells.isEmpty()) return emptyList()

        val windowSize = 5
        val results = mutableListOf<HighPressurePeriodWindow>()
        val usedDays = mutableSetOf<Int>()

        // Find up to 2 highest-pressure non-overlapping windows
        repeat(2) { passIndex ->
            var bestSum = 0L
            var bestSlice: List<EnrichedHeatmapDayCell> = emptyList()

            for (startIdx in cells.indices) {
                val endIdx = (startIdx + windowSize - 1).coerceAtMost(cells.lastIndex)
                val slice = cells.subList(startIdx, endIdx + 1)
                if (slice.any { it.dayOfMonth in usedDays }) continue
                val sum = slice.sumOf { it.upcomingUnpaidDebtToman }
                if (sum > bestSum) {
                    bestSum = sum
                    bestSlice = slice
                }
            }

            if (bestSum <= 0L || bestSlice.isEmpty()) return@repeat

            val sharePct = ((bestSum.toDouble() / totalUpcomingUnpaid.toDouble()) * 100.0)
                .roundToInt()
                .coerceIn(1, 100)

            // Trim empty leading/trailing days so the range tightly wraps active payment days
            val activeInSlice = bestSlice.filter { it.upcomingUnpaidDebtToman > 0L }
            if (activeInSlice.isEmpty()) return@repeat

            val hasHighOrCritical = activeInSlice.any {
                it.intensityLevel == HeatmapIntensityLevel.HIGH ||
                    it.intensityLevel == HeatmapIntensityLevel.CRITICAL
            }
            // Only emit second cluster if it is genuinely substantial (>= 25% of monthly load)
            if (passIndex == 1 && sharePct < 25 && !hasHighOrCritical) return@repeat

            val trimmedStart = activeInSlice.first().dayOfMonth
            val trimmedEnd = activeInSlice.last().dayOfMonth
            val instCount = activeInSlice.sumOf { it.upcomingInstallmentsCount }
            val peakLevel = activeInSlice.maxByOrNull { it.intensityLevel.rank }?.intensityLevel
                ?: HeatmapIntensityLevel.HIGH

            val rangeFa = if (trimmedStart == trimmedEnd) {
                "${PersianCurrencyEngine.toPersianDigits(trimmedStart.toString())} $monthNameFa"
            } else {
                "${PersianCurrencyEngine.toPersianDigits(trimmedStart.toString())} تا ${
                    PersianCurrencyEngine.toPersianDigits(trimmedEnd.toString())
                } $monthNameFa"
            }

            val severityBadge = when {
                sharePct >= 50 || peakLevel == HeatmapIntensityLevel.CRITICAL -> "بازه بحرانی اوج فشار 🔥"
                sharePct >= 30 || peakLevel == HeatmapIntensityLevel.HIGH -> "دوره پرفشار ماه ⚡"
                else -> "تمرکز اقساط"
            }

            val advisory = "در بازه $rangeFa تعداد ${PersianCurrencyEngine.toPersianDigits(instCount.toString())} قسط (${
                PersianCurrencyEngine.toPersianDigits(sharePct.toString())
            }٪ کل بار اقساط پیش‌روی $monthNameFa) متمرکز شده است؛ موجودی حساب را پیش از روز ${
                PersianCurrencyEngine.toPersianDigits(trimmedStart.toString())
            } آماده کنید."

            results.add(
                HighPressurePeriodWindow(
                    id = "hp_${trimmedStart}_${trimmedEnd}",
                    startDay = trimmedStart,
                    endDay = trimmedEnd,
                    rangeLabelFa = rangeFa,
                    totalUpcomingDebtToman = bestSum,
                    installmentsCount = instCount,
                    shareOfMonthlyUpcomingPercent = sharePct,
                    peakIntensityLevel = peakLevel,
                    severityBadgeFa = severityBadge,
                    advisoryFa = advisory
                )
            )
            for (d in trimmedStart..trimmedEnd) {
                usedDays.add(d)
            }
        }

        return results.sortedBy { it.startDay }
    }

    private fun buildExecutiveSummaryFa(
        monthNameFa: String,
        totalUpcomingUnpaid: Long,
        highPressurePeriods: List<HighPressurePeriodWindow>,
        peakDay: EnrichedHeatmapDayCell?,
        decadeSummaries: List<PersianDecadeHeatSummary>
    ): String {
        if (totalUpcomingUnpaid <= 0L) {
            return "در ماه $monthNameFa هیچ قسط پرداخت‌نشده‌ای ثبت نشده است؛ تقویم حرارتی کاملاً سبز و آرام است."
        }
        val topPeriod = highPressurePeriods.maxByOrNull { it.totalUpcomingDebtToman }
        val peakDecade = decadeSummaries.firstOrNull { it.isPeakDecade }
        return if (topPeriod != null && peakDay != null) {
            "سنگین‌ترین دوره پرداخت در $monthNameFa بازه «${topPeriod.rangeLabelFa}» با ${
                PersianCurrencyEngine.toPersianDigits(topPeriod.shareOfMonthlyUpcomingPercent.toString())
            }٪ از کل اقساط پیش‌رو است و بیشترین فشار روزانه در روز ${
                PersianCurrencyEngine.toPersianDigits(peakDay.dayOfMonth.toString())
            } (${peakDecade?.titleFa ?: ""}) رخ می‌دهد."
        } else {
            "بار اقساط ماه $monthNameFa به‌صورت متعادل در طول روزهای ماه توزیع شده است."
        }
    }
}
