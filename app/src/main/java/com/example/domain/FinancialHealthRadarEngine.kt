package com.example.domain

import com.example.core.JalaliCalendarEngine
import com.example.data.CounterpartyEntity
import com.example.data.InstallmentEntity
import com.example.data.ObligationEntity
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Display mode for the D3 Financial Health Radar chart layers.
 */
enum class RadarLayerMode(val labelFa: String) {
    DUAL_OVERLAY("هم‌پوشانی کامل (بدهی + بودجه)"),
    DEBT_DISTRIBUTION("توزیع دسته‌های بدهی"),
    BUDGET_IMPACT("اثر بر ظرفیت بودجه")
}

/**
 * Represents a single polar axis on the D3 Financial Health Radar chart.
 * Combines debt category distribution (e.g., 'Bank Loan' vs 'Personal') and its
 * relative impact on the user's overall monthly budget capacity.
 */
data class RadarCategoryAxisMetric(
    val axisIndex: Int,
    val axisId: String,
    val categoryTag: DebtCategoryTag?,
    val labelFa: String,
    val shortLabelFa: String,
    val angleDegrees: Float,
    val angleRadians: Double,
    val obligationsCount: Int,
    val remainingDebtToman: Long,
    val monthlyInstallmentDueToman: Long,
    val debtDistributionPercent: Int,
    val budgetCapacityImpactPercent: Int,
    val shareOfTotalIncomePercent: Int,
    val debtDistributionNormalizedRadius: Float,
    val budgetImpactNormalizedRadius: Float,
    val settlementProgressPercent: Int,
    val pressureStatusFa: String,
    val sampleCounterpartiesFa: String,
    val colorHex: Long
)

/**
 * Complete domain model for the D3 Financial Health Radar visualization.
 */
data class FinancialHealthRadarReport(
    val shamsiMonthNameFa: String,
    val monthlyIncomeToman: Long,
    val essentialLivingExpenseToman: Long,
    val totalMonthlyDebtBudgetCapacityToman: Long,
    val totalCurrentMonthInstallmentsToman: Long,
    val remainingSafeBudgetCapacityToman: Long,
    val overallBudgetCapacityUtilizationPercent: Int,
    val totalRemainingDebtToman: Long,
    val radarHealthScore: Int,
    val radarStatusBadgeFa: String,
    val dominantDebtCategory: DebtCategoryTag?,
    val highestBudgetImpactCategory: DebtCategoryTag?,
    val axes: List<RadarCategoryAxisMetric>,
    val executiveInsightFa: String
)

/**
 * Pure domain and D3 radial geometry engine for the 'Financial Health Radar'.
 *
 * Computes:
 * 1. Distribution of debt categories ('Bank Loan', 'Credit Card / BNPL', 'Personal', 'Check & Housing')
 *    as normalized polar ratios [0..1] and percentages [0..100%].
 * 2. Relative impact of each debt category's monthly installment burden on the user's
 *    overall monthly budget capacity (`monthlyIncome - essentialLivingExpense`).
 * 3. Remaining free monthly budget capacity axis (`ظرفیت آزاد بودجه`) to form a balanced 5-axis
 *    pentagonal D3 radar coordinate system.
 */
object FinancialHealthRadarEngine {

    /**
     * Converts polar radar coordinates `(angleRadians, normalizedRadius [0..1])`
     * into Cartesian `(x, y)` coordinates around `(centerX, centerY)` using D3 radial scale semantics.
     */
    fun computePolarVertex(
        angleRadians: Double,
        normalizedRadius: Float,
        maxRadiusPx: Float,
        centerX: Float,
        centerY: Float
    ): Pair<Float, Float> {
        val clampedRatio = normalizedRadius.coerceIn(0f, 1f)
        val r = clampedRatio * maxRadiusPx
        val x = centerX + (cos(angleRadians) * r).toFloat()
        val y = centerY + (sin(angleRadians) * r).toFloat()
        return x to y
    }

    /**
     * Finds the nearest radar axis index [0..axes.lastIndex] to a user tap `(tapX, tapY)`
     * within `maxHitDistancePx` of either the outer axis label/vertex or along the axis spoke.
     */
    fun findNearestTappedAxisIndex(
        axes: List<RadarCategoryAxisMetric>,
        tapX: Float,
        tapY: Float,
        centerX: Float,
        centerY: Float,
        maxRadiusPx: Float
    ): Int? {
        if (axes.isEmpty() || maxRadiusPx <= 0f) return null
        val distFromCenter = hypot(tapX - centerX, tapY - centerY)
        // Ignore taps right on the dead-center origin if user wants to clear selection
        if (distFromCenter < maxRadiusPx * 0.16f) return null

        var bestIndex: Int? = null
        var bestDistance = Float.MAX_VALUE

        axes.forEachIndexed { index, axis ->
            // Check distance to the outer vertex and the midpoint of the axis spoke
            val (outerX, outerY) = computePolarVertex(
                angleRadians = axis.angleRadians,
                normalizedRadius = 0.85f,
                maxRadiusPx = maxRadiusPx,
                centerX = centerX,
                centerY = centerY
            )
            val dOuter = hypot(tapX - outerX, tapY - outerY)

            val (debtX, debtY) = computePolarVertex(
                angleRadians = axis.angleRadians,
                normalizedRadius = axis.debtDistributionNormalizedRadius,
                maxRadiusPx = maxRadiusPx,
                centerX = centerX,
                centerY = centerY
            )
            val dDebt = hypot(tapX - debtX, tapY - debtY)

            val (budgetX, budgetY) = computePolarVertex(
                angleRadians = axis.angleRadians,
                normalizedRadius = axis.budgetImpactNormalizedRadius,
                maxRadiusPx = maxRadiusPx,
                centerX = centerX,
                centerY = centerY
            )
            val dBudget = hypot(tapX - budgetX, tapY - budgetY)

            val candidateDist = minOf(dOuter, dDebt, dBudget)
            if (candidateDist < bestDistance) {
                bestDistance = candidateDist
                bestIndex = index
            }
        }

        return if (bestDistance <= maxRadiusPx * 0.55f) bestIndex else null
    }

    /**
     * Evaluates the user's obligations, installments, counterparties, and monthly budget parameters
     * to produce the 5-axis D3 Financial Health Radar model.
     */
    fun evaluateFinancialHealthRadar(
        obligations: List<ObligationEntity>,
        installments: List<InstallmentEntity>,
        counterparties: List<CounterpartyEntity>,
        monthlyIncomeToman: Long,
        livingExpensePercent: Int,
        recurringNonLoanExpenseToman: Long = 0L,
        categoryOverrides: Map<Long, DebtCategoryTag> = emptyMap(),
        todayJalali: String = JalaliCalendarEngine.todayJalali()
    ): FinancialHealthRadarReport {
        val portfolio = DebtTaggingAndCategorizationEngine.evaluateCategorizedPortfolio(
            obligations = obligations,
            installments = installments,
            counterparties = counterparties,
            selectedFilterTag = DebtCategoryTag.ALL,
            categoryOverrides = categoryOverrides,
            todayJalali = todayJalali
        )

        val safeIncome = monthlyIncomeToman.coerceAtLeast(1_000_000L)
        val safeLivingPercent = livingExpensePercent.coerceIn(10, 85)
        val percentageBasedLivingExpense = (safeIncome * safeLivingPercent) / 100L
        val effectiveLivingExpense = maxOf(percentageBasedLivingExpense, recurringNonLoanExpenseToman)
            .coerceAtMost((safeIncome * 90L) / 100L)

        // Monthly budget capacity available for debt service + savings after essential living expenses
        val monthlyDebtBudgetCapacity = (safeIncome - effectiveLivingExpense).coerceAtLeast(1_000_000L)

        val debtCategories = listOf(
            DebtCategoryTag.BANK_LOAN,
            DebtCategoryTag.CREDIT_CARD,
            DebtCategoryTag.PERSONAL,
            DebtCategoryTag.CHECK_AND_HOUSING
        )

        val breakdownByTag = portfolio.categoryBreakdowns.associateBy { it.tag }

        val totalRemainingDebt = debtCategories.sumOf { tag ->
            breakdownByTag[tag]?.remainingToman ?: 0L
        }
        val safeTotalRemainingDebt = totalRemainingDebt.coerceAtLeast(1L)

        val totalCurrentMonthInstallments = debtCategories.sumOf { tag ->
            breakdownByTag[tag]?.currentMonthDueToman ?: 0L
        }

        val remainingSafeBudgetCapacity = (monthlyDebtBudgetCapacity - totalCurrentMonthInstallments)
            .coerceAtLeast(0L)

        val overallBudgetUtilizationPct = ((totalCurrentMonthInstallments.toDouble() / monthlyDebtBudgetCapacity.toDouble()) * 100.0)
            .roundToInt()
            .coerceIn(0, 200)

        val freeBudgetBufferPct = (100 - overallBudgetUtilizationPct).coerceIn(0, 100)

        // Total 5 radar axes: 4 core debt categories + 1 Monthly Budget Capacity Buffer axis
        val totalAxesCount = 5
        val angleStepDeg = 360f / totalAxesCount.toFloat()

        val categoryAxes = debtCategories.mapIndexed { index, tag ->
            val breakdown = breakdownByTag[tag]
            val remDebt = breakdown?.remainingToman ?: 0L
            val monthDue = breakdown?.currentMonthDueToman ?: 0L
            val obsCount = breakdown?.obligationsCount ?: 0
            val settlementPct = breakdown?.settlementProgressPercent ?: 100

            val debtDistPct = if (totalRemainingDebt > 0L) {
                ((remDebt.toDouble() / safeTotalRemainingDebt.toDouble()) * 100.0)
                    .roundToInt()
                    .coerceIn(0, 100)
            } else {
                0
            }

            val budgetImpactPct = ((monthDue.toDouble() / monthlyDebtBudgetCapacity.toDouble()) * 100.0)
                .roundToInt()
                .coerceIn(0, 100)

            val incomeSharePct = ((monthDue.toDouble() / safeIncome.toDouble()) * 100.0)
                .roundToInt()
                .coerceIn(0, 100)

            // D3 radial scale with minimum inner baseline (0.10f) so zero-value vertices still form a visible inner pentagon core
            val debtNormRadius = if (remDebt <= 0L) {
                0.10f
            } else {
                (0.14f + (debtDistPct / 100f) * 0.86f).coerceIn(0.14f, 1.0f)
            }

            val budgetNormRadius = if (monthDue <= 0L) {
                0.10f
            } else {
                (0.14f + (budgetImpactPct / 100f) * 0.86f).coerceIn(0.14f, 1.0f)
            }

            val pressureStatus = when {
                remDebt == 0L && monthDue == 0L -> "بدون بدهی فعال"
                budgetImpactPct >= 45 -> "فشار بالا بر ظرفیت بودجه"
                budgetImpactPct >= 25 -> "فشار متوسط بر بودجه"
                else -> "ایمن و متعادل"
            }

            val angleDeg = -90f + (index * angleStepDeg)
            val angleRad = angleDeg * (PI / 180.0)

            RadarCategoryAxisMetric(
                axisIndex = index,
                axisId = tag.id,
                categoryTag = tag,
                labelFa = tag.labelFa,
                shortLabelFa = tag.shortBadgeFa,
                angleDegrees = angleDeg,
                angleRadians = angleRad,
                obligationsCount = obsCount,
                remainingDebtToman = remDebt,
                monthlyInstallmentDueToman = monthDue,
                debtDistributionPercent = debtDistPct,
                budgetCapacityImpactPercent = budgetImpactPct,
                shareOfTotalIncomePercent = incomeSharePct,
                debtDistributionNormalizedRadius = debtNormRadius,
                budgetImpactNormalizedRadius = budgetNormRadius,
                settlementProgressPercent = settlementPct,
                pressureStatusFa = pressureStatus,
                sampleCounterpartiesFa = breakdown?.sampleCounterpartyNamesFa ?: "بدون پرونده فعال",
                colorHex = tag.colorHex
            )
        }

        // 5th Axis: Monthly Budget Capacity Reserve / Safe Buffer
        val overallSettlementPct = portfolio.filteredSettlementProgressPercent.coerceIn(0, 100)
        val bufferAngleDeg = -90f + (4 * angleStepDeg)
        val bufferAngleRad = bufferAngleDeg * (PI / 180.0)

        val bufferAxis = RadarCategoryAxisMetric(
            axisIndex = 4,
            axisId = "BUDGET_CAPACITY_BUFFER",
            categoryTag = null,
            labelFa = "ظرفیت آزاد بودجه ماهانه",
            shortLabelFa = "ظرفیت آزاد بودجه",
            angleDegrees = bufferAngleDeg,
            angleRadians = bufferAngleRad,
            obligationsCount = obligations.count { !it.categoryEnum.isReceivable },
            remainingDebtToman = remainingSafeBudgetCapacity,
            monthlyInstallmentDueToman = remainingSafeBudgetCapacity,
            debtDistributionPercent = overallSettlementPct,
            budgetCapacityImpactPercent = freeBudgetBufferPct,
            shareOfTotalIncomePercent = ((remainingSafeBudgetCapacity.toDouble() / safeIncome.toDouble()) * 100.0)
                .roundToInt()
                .coerceIn(0, 100),
            debtDistributionNormalizedRadius = (0.14f + (overallSettlementPct / 100f) * 0.86f).coerceIn(0.14f, 1.0f),
            budgetImpactNormalizedRadius = (0.14f + (freeBudgetBufferPct / 100f) * 0.86f).coerceIn(0.14f, 1.0f),
            settlementProgressPercent = overallSettlementPct,
            pressureStatusFa = when {
                freeBudgetBufferPct >= 45 -> "حاشیه امن عالی"
                freeBudgetBufferPct >= 20 -> "ظرفیت قابل‌قبول"
                else -> "ظرفیت بودجه اشباع‌شده"
            },
            sampleCounterpartiesFa = "سقف آزاد تعهد جدید پس از کسر مخارج",
            colorHex = 0xFF35B8AB
        )

        val allAxes = categoryAxes + bufferAxis

        val dominantDebtCat = categoryAxes
            .filter { it.remainingDebtToman > 0L }
            .maxByOrNull { it.remainingDebtToman }
            ?.categoryTag

        val highestBudgetImpactCat = categoryAxes
            .filter { it.monthlyInstallmentDueToman > 0L }
            .maxByOrNull { it.monthlyInstallmentDueToman }
            ?.categoryTag

        // Calculate composite Radar Financial Health Score (0..100)
        // Rewards balanced category distribution, low budget capacity utilization, and steady settlement progress
        val maxSingleCategoryShare = categoryAxes.maxOfOrNull { it.debtDistributionPercent } ?: 0
        val concentrationPenalty = if (totalRemainingDebt > 0L && maxSingleCategoryShare > 65) {
            ((maxSingleCategoryShare - 65) * 0.4).roundToInt()
        } else {
            0
        }
        val utilizationPenalty = (overallBudgetUtilizationPct * 0.48).roundToInt()
        val settlementBonus = (overallSettlementPct * 0.15).roundToInt()
        val radarScore = (100 - utilizationPenalty - concentrationPenalty + settlementBonus).coerceIn(12, 100)

        val statusBadgeFa = when {
            radarScore >= 75 -> "توازن عالی رادار"
            radarScore >= 52 -> "توازن قابل‌قبول"
            else -> "فشار متمرکز بر بودجه"
        }

        val insightFa = buildRadarExecutiveInsightFa(
            categoryAxes = categoryAxes,
            dominantDebtCategory = dominantDebtCat,
            highestBudgetImpactCategory = highestBudgetImpactCat,
            overallBudgetUtilizationPct = overallBudgetUtilizationPct,
            freeBudgetBufferPct = freeBudgetBufferPct,
            shamsiMonthNameFa = portfolio.currentShamsiMonthNameFa
        )

        return FinancialHealthRadarReport(
            shamsiMonthNameFa = portfolio.currentShamsiMonthNameFa,
            monthlyIncomeToman = safeIncome,
            essentialLivingExpenseToman = effectiveLivingExpense,
            totalMonthlyDebtBudgetCapacityToman = monthlyDebtBudgetCapacity,
            totalCurrentMonthInstallmentsToman = totalCurrentMonthInstallments,
            remainingSafeBudgetCapacityToman = remainingSafeBudgetCapacity,
            overallBudgetCapacityUtilizationPercent = overallBudgetUtilizationPct,
            totalRemainingDebtToman = totalRemainingDebt,
            radarHealthScore = radarScore,
            radarStatusBadgeFa = statusBadgeFa,
            dominantDebtCategory = dominantDebtCat,
            highestBudgetImpactCategory = highestBudgetImpactCat,
            axes = allAxes,
            executiveInsightFa = insightFa
        )
    }

    private fun buildRadarExecutiveInsightFa(
        categoryAxes: List<RadarCategoryAxisMetric>,
        dominantDebtCategory: DebtCategoryTag?,
        highestBudgetImpactCategory: DebtCategoryTag?,
        overallBudgetUtilizationPct: Int,
        freeBudgetBufferPct: Int,
        shamsiMonthNameFa: String
    ): String {
        val domAxis = categoryAxes.firstOrNull { it.categoryTag == dominantDebtCategory }
        val budgetAxis = categoryAxes.firstOrNull { it.categoryTag == highestBudgetImpactCategory }

        if (domAxis == null || domAxis.remainingDebtToman <= 0L) {
            return "تمامی دسته‌های بدهی (وام بانکی، شخصی و کارت اعتباری) صفر هستند و ۱۰۰٪ ظرفیت بودجه ماهانه آزاد است."
        }

        return if (budgetAxis != null && budgetAxis.monthlyInstallmentDueToman > 0L) {
            "دسته «${domAxis.shortLabelFa}» با ${domAxis.debtDistributionPercent}٪ بیشترین سهم از کل بدهی را دارد و دسته «${budgetAxis.shortLabelFa}» به تنهایی ${budgetAxis.budgetCapacityImpactPercent}٪ از ظرفیت بودجه $shamsiMonthNameFa را به خود اختصاص داده است (${freeBudgetBufferPct}٪ ظرفیت آزاد)."
        } else {
            "دسته «${domAxis.shortLabelFa}» شامل ${domAxis.debtDistributionPercent}٪ از کل بدهی مانده است و مجموع اقساط این ماه ${overallBudgetUtilizationPct}٪ از ظرفیت بودجه را مصرف می‌کند."
        }
    }
}
