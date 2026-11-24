package com.example.domain

import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.data.CounterpartyEntity
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.ObligationCategory
import com.example.data.ObligationEntity
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Mathematical verdict determining whether consolidating the selected debts into a single
 * lower-interest loan is financially beneficial.
 */
enum class ConsolidationVerdict(
    val titleFa: String,
    val badgeFa: String
) {
    STRONGLY_ADVANTAGEOUS(
        titleFa = "تجمیع از نظر ریاضی کاملاً به‌صرفه است",
        badgeFa = "به‌صرفه و توصیه‌شده"
    ),
    MARGINALLY_ADVANTAGEOUS(
        titleFa = "تجمیع سود خالص جزئی و نظم بهتر ایجاد می‌کند",
        badgeFa = "به‌صرفه (سود ملایم)"
    ),
    CASHFLOW_RELIEF_TRADEOFF(
        titleFa = "فشار قسط ماهانه کم می‌شود اما هزینه کل افزایش می‌یابد",
        badgeFa = "کاهش قسط / هزینه کل بیشتر"
    ),
    NOT_ADVANTAGEOUS(
        titleFa = "تجمیع با این شرایط از نظر ریاضی به‌صرفه نیست",
        badgeFa = "غیراقتصادی / توصیه نمی‌شود"
    ),
    INSUFFICIENT_DEBTS(
        titleFa = "حداقل یک بدهی را برای شبیه‌سازی انتخاب کنید",
        badgeFa = "انتخاب بدهی"
    )
}

/**
 * Single active debt account eligible for consolidation analysis.
 */
data class ConsolidatableDebtItem(
    val id: Long,
    val titleFa: String,
    val counterpartyFa: String,
    val categoryTag: DebtCategoryTag,
    val remainingPrincipalToman: Long,
    val remainingScheduledPayableToman: Long,
    val remainingEmbeddedInterestToman: Long,
    val currentMonthlyPaymentToman: Long,
    val remainingMonths: Int,
    val effectiveAnnualRatePercent: Double
)

/**
 * Complete mathematical analysis comparing the user's existing selected debts against
 * a single consolidated loan with custom APR, repayment term, and origination fee.
 */
data class DebtConsolidationReport(
    val verdict: ConsolidationVerdict,
    val selectedDebts: List<ConsolidatableDebtItem>,
    val excludedLowRateCount: Int,
    val consolidationAnnualRatePercent: Double,
    val consolidationTermMonths: Int,
    val originationFeePercent: Double,
    val currentTotalPrincipalToman: Long,
    val currentTotalRemainingInterestToman: Long,
    val currentTotalRemainingCostToman: Long,
    val currentCombinedMonthlyPaymentToman: Long,
    val currentWeightedAnnualRatePercent: Double,
    val currentLongestRemainingMonths: Int,
    val consolidationLoanPrincipalToman: Long,
    val originationFeeToman: Long,
    val consolidatedMonthlyPaymentToman: Long,
    val consolidatedTotalInterestToman: Long,
    val consolidatedTotalCostToman: Long,
    val netTotalSavingsToman: Long,
    val netInterestSavingsToman: Long,
    val monthlyCashflowReliefToman: Long,
    val rateSpreadPercent: Double,
    val breakEvenMonths: Int?,
    val monthlyDtiBeforePercent: Int?,
    val monthlyDtiAfterPercent: Int?,
    val consolidatedDebtFreeDateJalali: String,
    val consolidatedDebtFreeMonthLabelFa: String,
    val verdictHeadlineFa: String,
    val verdictDetailFa: String,
    val actionableNotesFa: List<String>
)

/**
 * Pure domain mathematical engine for the **Debt Consolidation Simulator**.
 *
 * Evaluates current user obligations/installments (or custom sample scenarios), separates remaining
 * principal from embedded interest/fees, computes exact compound amortization (PMT) for a single
 * consolidation loan, accounts for bank origination fees and break-even horizon, and determines
 * whether consolidation is mathematically advantageous.
 */
object DebtConsolidationEngine {

    /**
     * Extracts active, non-receivable debt obligations with remaining balances and infers
     * each debt's remaining principal, embedded interest, effective annual interest rate (APR),
     * remaining term, and average monthly payment.
     */
    fun extractConsolidatableDebts(
        obligations: List<ObligationEntity>,
        installments: List<InstallmentEntity>,
        counterparties: List<CounterpartyEntity>
    ): List<ConsolidatableDebtItem> {
        val cpMap = counterparties.associateBy { it.id }
        val instByOb = installments.groupBy { it.obligationId }

        return obligations
            .filter { !it.categoryEnum.isReceivable }
            .mapNotNull { ob ->
                val allInsts = instByOb[ob.id].orEmpty()
                val unpaid = allInsts.filter {
                    it.statusEnum != InstallmentStatus.PAID && it.remainingToman > 0L
                }
                val remainingPayable = unpaid.sumOf { it.remainingToman }
                if (remainingPayable <= 0L) return@mapNotNull null

                val remainingMonths = unpaid.size.coerceAtLeast(1)
                val totalInstCount = allInsts.size.coerceAtLeast(ob.installmentCount.coerceAtLeast(1))
                val totalScheduled = allInsts.sumOf { it.effectiveTotalToman }
                    .coerceAtLeast(ob.netPayableToman)
                    .coerceAtLeast(remainingPayable)

                val financedPrincipal = (ob.principalAmountToman - ob.downPaymentToman)
                    .coerceAtLeast(1L)
                val totalProfit = maxOf(
                    ob.profitOrFeeToman,
                    (totalScheduled - financedPrincipal).coerceAtLeast(0L)
                )

                // Separate remaining payoff principal vs remaining embedded profit/interest proportionally
                val remainingShare = (remainingPayable.toDouble() / totalScheduled.toDouble())
                    .coerceIn(0.0, 1.0)
                val estimatedRemainingInterest = (totalProfit * remainingShare)
                    .roundToLong()
                    .coerceIn(0L, (remainingPayable - 1L).coerceAtLeast(0L))
                val estimatedRemainingPrincipal = (remainingPayable - estimatedRemainingInterest)
                    .coerceAtLeast(1L)

                // Annualized effective interest rate approximation (converting flat profit over term to APR)
                val termYears = (totalInstCount / 12.0).coerceAtLeast(0.25)
                val inferredApr = if (totalProfit > 0L && financedPrincipal > 0L) {
                    // For installment loans, flat profit ratio ~ (APR / 2) * years, so effective APR ~ 1.8 * flatRate / years
                    val flatProfitRatio = totalProfit.toDouble() / financedPrincipal.toDouble()
                    val annualApprox = (flatProfitRatio / termYears) * 100.0 * 1.65
                    ((annualApprox * 10.0).roundToInt() / 10.0).coerceIn(0.0, 60.0)
                } else {
                    0.0
                }

                val avgMonthly = (remainingPayable.toDouble() / remainingMonths.toDouble())
                    .roundToLong()
                    .coerceAtLeast(1L)

                val cpName = cpMap[ob.counterpartyId]?.name ?: "طرف حساب"
                val tag = when (ob.categoryEnum) {
                    ObligationCategory.INSTALLMENT_LOAN -> DebtCategoryTag.BANK_LOAN
                    ObligationCategory.BNPL_PURCHASE -> DebtCategoryTag.CREDIT_CARD
                    ObligationCategory.PERSONAL_DEBT -> DebtCategoryTag.PERSONAL
                    ObligationCategory.CHECK_OR_BILL -> DebtCategoryTag.CHECK_AND_HOUSING
                    ObligationCategory.PERSONAL_RECEIVABLE -> DebtCategoryTag.RECEIVABLE
                }

                ConsolidatableDebtItem(
                    id = ob.id,
                    titleFa = ob.title,
                    counterpartyFa = cpName,
                    categoryTag = tag,
                    remainingPrincipalToman = estimatedRemainingPrincipal,
                    remainingScheduledPayableToman = remainingPayable,
                    remainingEmbeddedInterestToman = estimatedRemainingInterest,
                    currentMonthlyPaymentToman = avgMonthly,
                    remainingMonths = remainingMonths,
                    effectiveAnnualRatePercent = inferredApr
                )
            }
            .sortedByDescending { it.effectiveAnnualRatePercent }
    }

    /**
     * Built-in realistic Iranian sample debts for "what-if" exploration when a user has no active
     * high-interest debts yet or wants to benchmark a consolidation offer.
     */
    fun sampleScenarioDebts(): List<ConsolidatableDebtItem> = listOf(
        ConsolidatableDebtItem(
            id = -101L,
            titleFa = "خرید اقساطی دیجی‌پی / اسنپ‌پی",
            counterpartyFa = "پلتفرم اعتباری",
            categoryTag = DebtCategoryTag.CREDIT_CARD,
            remainingPrincipalToman = 28_000_000L,
            remainingScheduledPayableToman = 35_800_000L,
            remainingEmbeddedInterestToman = 7_800_000L,
            currentMonthlyPaymentToman = 2_983_333L,
            remainingMonths = 12,
            effectiveAnnualRatePercent = 34.0
        ),
        ConsolidatableDebtItem(
            id = -102L,
            titleFa = "تسهیلات خرید کالا با نرخ آزاد",
            counterpartyFa = "موسسه اعتباری",
            categoryTag = DebtCategoryTag.CREDIT_CARD,
            remainingPrincipalToman = 42_000_000L,
            remainingScheduledPayableToman = 54_600_000L,
            remainingEmbeddedInterestToman = 12_600_000L,
            currentMonthlyPaymentToman = 3_033_333L,
            remainingMonths = 18,
            effectiveAnnualRatePercent = 28.5
        ),
        ConsolidatableDebtItem(
            id = -103L,
            titleFa = "وام خرد بانکی ۲۳ درصدی",
            counterpartyFa = "بانک عامل",
            categoryTag = DebtCategoryTag.BANK_LOAN,
            remainingPrincipalToman = 30_000_000L,
            remainingScheduledPayableToman = 37_200_000L,
            remainingEmbeddedInterestToman = 7_200_000L,
            currentMonthlyPaymentToman = 1_860_000L,
            remainingMonths = 20,
            effectiveAnnualRatePercent = 23.0
        ),
        ConsolidatableDebtItem(
            id = -104L,
            titleFa = "وام قرض‌الحسنه ۴ درصدی (کم‌بهره)",
            counterpartyFa = "بانک رسالت / مهر",
            categoryTag = DebtCategoryTag.BANK_LOAN,
            remainingPrincipalToman = 25_000_000L,
            remainingScheduledPayableToman = 26_000_000L,
            remainingEmbeddedInterestToman = 1_000_000L,
            currentMonthlyPaymentToman = 1_300_000L,
            remainingMonths = 20,
            effectiveAnnualRatePercent = 4.0
        )
    )

    /**
     * Recommends the mathematically optimal subset of debts to consolidate: only debts whose
     * effective APR strictly exceeds the new consolidation loan's APR.
     */
    fun recommendOptimalSubsetIds(
        allDebts: List<ConsolidatableDebtItem>,
        consolidationAnnualRatePercent: Double
    ): Set<Long> {
        val highRate = allDebts.filter { it.effectiveAnnualRatePercent > consolidationAnnualRatePercent + 0.5 }
        return if (highRate.isNotEmpty()) {
            highRate.map { it.id }.toSet()
        } else {
            allDebts.map { it.id }.toSet()
        }
    }

    /**
     * Computes standard compound monthly installment (PMT) for a fixed-rate amortized loan.
     */
    fun calculateMonthlyPaymentPmt(
        principalToman: Long,
        annualRatePercent: Double,
        termMonths: Int
    ): Long {
        val safePrincipal = principalToman.coerceAtLeast(0L)
        val safeMonths = termMonths.coerceAtLeast(1)
        val safeRate = annualRatePercent.coerceIn(0.0, 60.0)
        if (safePrincipal == 0L) return 0L
        if (safeRate == 0.0) {
            return (safePrincipal.toDouble() / safeMonths.toDouble()).roundToLong()
        }
        val monthlyRate = (safeRate / 100.0) / 12.0
        val factor = (1.0 + monthlyRate).pow(safeMonths.toDouble())
        val pmt = safePrincipal.toDouble() * (monthlyRate * factor) / (factor - 1.0)
        return pmt.roundToLong().coerceAtLeast(1L)
    }

    /**
     * Runs the complete mathematical Debt Consolidation simulation comparing status-quo payments
     * of [selectedDebtIds] against a single consolidation loan at [consolidationAnnualRatePercent]
     * over [consolidationTermMonths] with [originationFeePercent] bank fee.
     */
    fun simulateConsolidation(
        allDebts: List<ConsolidatableDebtItem>,
        selectedDebtIds: Set<Long>,
        consolidationAnnualRatePercent: Double,
        consolidationTermMonths: Int,
        originationFeePercent: Double = 0.0,
        monthlyIncomeToman: Long = 0L,
        startDateJalali: String = JalaliCalendarEngine.todayJalali()
    ): DebtConsolidationReport {
        val safeRate = consolidationAnnualRatePercent.coerceIn(0.0, 60.0)
        val safeTerm = consolidationTermMonths.coerceIn(3, 84)
        val safeFeePct = originationFeePercent.coerceIn(0.0, 10.0)

        val selected = allDebts.filter { it.id in selectedDebtIds }
        val excludedLowRateCount = allDebts.count {
            it.id !in selectedDebtIds && it.effectiveAnnualRatePercent <= safeRate
        }

        val endDateJalali = JalaliCalendarEngine.addJalaliMonths(startDateJalali, safeTerm)
        val parsedEnd = JalaliCalendarEngine.parseJalali(endDateJalali)
        val endMonthLabelFa = JalaliCalendarEngine.formatJalaliMonthYear(parsedEnd.year, parsedEnd.month)

        if (selected.isEmpty()) {
            return DebtConsolidationReport(
                verdict = ConsolidationVerdict.INSUFFICIENT_DEBTS,
                selectedDebts = emptyList(),
                excludedLowRateCount = excludedLowRateCount,
                consolidationAnnualRatePercent = safeRate,
                consolidationTermMonths = safeTerm,
                originationFeePercent = safeFeePct,
                currentTotalPrincipalToman = 0L,
                currentTotalRemainingInterestToman = 0L,
                currentTotalRemainingCostToman = 0L,
                currentCombinedMonthlyPaymentToman = 0L,
                currentWeightedAnnualRatePercent = 0.0,
                currentLongestRemainingMonths = 0,
                consolidationLoanPrincipalToman = 0L,
                originationFeeToman = 0L,
                consolidatedMonthlyPaymentToman = 0L,
                consolidatedTotalInterestToman = 0L,
                consolidatedTotalCostToman = 0L,
                netTotalSavingsToman = 0L,
                netInterestSavingsToman = 0L,
                monthlyCashflowReliefToman = 0L,
                rateSpreadPercent = 0.0,
                breakEvenMonths = null,
                monthlyDtiBeforePercent = null,
                monthlyDtiAfterPercent = null,
                consolidatedDebtFreeDateJalali = endDateJalali,
                consolidatedDebtFreeMonthLabelFa = endMonthLabelFa,
                verdictHeadlineFa = "بدهی‌هایی را که قصد تجمیع دارید انتخاب کنید",
                verdictDetailFa = "با انتخاب بدهی‌های پرهزینه در لیست بالا، بررسی می‌کنیم آیا گرفتن یک وام کم‌بهره از نظر ریاضی به سود شماست یا خیر.",
                actionableNotesFa = listOf(
                    "برای مقایسه دقیق‌تر، ابتدا بدهی‌هایی با نرخ سود بالاتر از نرخ وام جدید را انتخاب کنید."
                )
            )
        }

        val currentPrincipal = selected.sumOf { it.remainingPrincipalToman }
        val currentInterest = selected.sumOf { it.remainingEmbeddedInterestToman }
        val currentTotalCost = selected.sumOf { it.remainingScheduledPayableToman }
        val currentMonthly = selected.sumOf { it.currentMonthlyPaymentToman }
        val longestRemaining = selected.maxOf { it.remainingMonths }

        val weightedApr = if (currentPrincipal > 0L) {
            val num = selected.sumOf { it.effectiveAnnualRatePercent * it.remainingPrincipalToman.toDouble() }
            ((num / currentPrincipal.toDouble()) * 10.0).roundToInt() / 10.0
        } else {
            0.0
        }

        val originationFee = (currentPrincipal.toDouble() * (safeFeePct / 100.0)).roundToLong()
        val consolidatedMonthly = calculateMonthlyPaymentPmt(
            principalToman = currentPrincipal,
            annualRatePercent = safeRate,
            termMonths = safeTerm
        )
        val consolidatedTotalRepayment = consolidatedMonthly * safeTerm.toLong()
        val consolidatedInterest = (consolidatedTotalRepayment - currentPrincipal).coerceAtLeast(0L)
        val consolidatedTotalCost = currentPrincipal + consolidatedInterest + originationFee

        val netInterestSavings = currentInterest - consolidatedInterest
        val netTotalSavings = currentTotalCost - consolidatedTotalCost
        val monthlyRelief = currentMonthly - consolidatedMonthly
        val rateSpread = (((weightedApr - safeRate) * 10.0).roundToInt() / 10.0)

        val breakEvenMonths: Int? = if (originationFee > 0L && netInterestSavings > 0L) {
            val avgMonthlyInterestSavings = netInterestSavings.toDouble() / safeTerm.toDouble()
            if (avgMonthlyInterestSavings > 1.0) {
                ceil(originationFee.toDouble() / avgMonthlyInterestSavings).toInt().coerceAtLeast(1)
            } else {
                null
            }
        } else if (originationFee == 0L && netTotalSavings > 0L) {
            1
        } else {
            null
        }

        val dtiBefore = if (monthlyIncomeToman > 0L) {
            ((currentMonthly.toDouble() / monthlyIncomeToman.toDouble()) * 100.0)
                .roundToInt()
                .coerceIn(0, 300)
        } else {
            null
        }
        val dtiAfter = if (monthlyIncomeToman > 0L) {
            ((consolidatedMonthly.toDouble() / monthlyIncomeToman.toDouble()) * 100.0)
                .roundToInt()
                .coerceIn(0, 300)
        } else {
            null
        }

        val savingsShareOfCost = if (currentTotalCost > 0L) {
            netTotalSavings.toDouble() / currentTotalCost.toDouble()
        } else {
            0.0
        }

        val verdict = when {
            netTotalSavings > 0L && (savingsShareOfCost >= 0.03 || rateSpread >= 2.0) ->
                ConsolidationVerdict.STRONGLY_ADVANTAGEOUS
            netTotalSavings > 0L ->
                ConsolidationVerdict.MARGINALLY_ADVANTAGEOUS
            monthlyRelief > 0L ->
                ConsolidationVerdict.CASHFLOW_RELIEF_TRADEOFF
            else ->
                ConsolidationVerdict.NOT_ADVANTAGEOUS
        }

        val lowRateSelectedDebts = selected.filter { it.effectiveAnnualRatePercent < safeRate }

        val headlineFa = when (verdict) {
            ConsolidationVerdict.STRONGLY_ADVANTAGEOUS ->
                "به‌صرفه است! حدود ${PersianCurrencyEngine.formatAmount(netTotalSavings)} صرفه‌جویی خالص دارید"
            ConsolidationVerdict.MARGINALLY_ADVANTAGEOUS ->
                "به‌صرفه است با ${PersianCurrencyEngine.formatAmount(netTotalSavings)} صرفه‌جویی خالص و یکپارچه‌سازی ${PersianCurrencyEngine.toPersianDigits(selected.size.toString())} قسط"
            ConsolidationVerdict.CASHFLOW_RELIEF_TRADEOFF ->
                "قسط ماهانه ${PersianCurrencyEngine.formatAmount(monthlyRelief)} سبک‌تر می‌شود، اما ${PersianCurrencyEngine.formatAmount(-netTotalSavings)} هزینه کل بیشتری می‌پردازید"
            ConsolidationVerdict.NOT_ADVANTAGEOUS ->
                "به‌صرفه نیست! تجمیع باعث ${PersianCurrencyEngine.formatAmount(-netTotalSavings)} زیان کل می‌شود"
            ConsolidationVerdict.INSUFFICIENT_DEBTS ->
                "بدهی برای شبیه‌سازی انتخاب نشده است"
        }

        val detailFa = when (verdict) {
            ConsolidationVerdict.STRONGLY_ADVANTAGEOUS, ConsolidationVerdict.MARGINALLY_ADVANTAGEOUS ->
                "تبدیل ${PersianCurrencyEngine.toPersianDigits(selected.size.toString())} بدهی با نرخ میانگین ${PersianCurrencyEngine.toPersianDigits(weightedApr.toString())}٪ به یک وام ${PersianCurrencyEngine.toPersianDigits(safeRate.toString())}٪ (با احتساب کارمزد) هزینه کل را کاهش می‌دهد و تاریخ تسویه شما «$endMonthLabelFa» خواهد بود."
            ConsolidationVerdict.CASHFLOW_RELIEF_TRADEOFF ->
                "اگر هدف اصلی شما کاهش فشار نقدینگی ماهانه است، این وام قسط ماهانه را کاهش می‌دهد؛ اما به دلیل طولانی‌تر شدن دوره یا نرخ سود بالاتر، جمع کل پرداختی شما بیشتر از وضعیت فعلی خواهد شد."
            ConsolidationVerdict.NOT_ADVANTAGEOUS ->
                "نرخ وام جدید (${PersianCurrencyEngine.toPersianDigits(safeRate.toString())}٪) نسبت به نرخ میانگین بدهی‌های انتخاب‌شده (${PersianCurrencyEngine.toPersianDigits(weightedApr.toString())}٪) یا هزینه‌های کارمزد به‌صرفه نیست. ادامه پرداخت فعلی یا استفاده از استراتژی گلوله برفی بهتر است."
            ConsolidationVerdict.INSUFFICIENT_DEBTS -> ""
        }

        val notes = mutableListOf<String>()
        if (lowRateSelectedDebts.isNotEmpty()) {
            val names = lowRateSelectedDebts.joinToString("، ") { it.titleFa }
            notes.add("هشدار ریاضی: بدهی‌های کم‌بهره ($names) نرخ پایین‌تری از وام تجمیع دارند؛ حذف آن‌ها از لیست، سود شما را بیشتر می‌کند.")
        }
        if (excludedLowRateCount > 0) {
            notes.add("${PersianCurrencyEngine.toPersianDigits(excludedLowRateCount.toString())} بدهی کم‌بهره/قرض‌الحسنه هوشمندانه خارج از تجمیع نگه داشته شدند تا سود بی‌مورد نپردازید.")
        }
        if (originationFee > 0L && breakEvenMonths != null) {
            notes.add("کارمزد تشکیل پرونده (${PersianCurrencyEngine.formatAmount(originationFee)}) در ماه ${PersianCurrencyEngine.toPersianDigits(breakEvenMonths.toString())}ام از محل صرفه‌جویی سود جبران می‌شود.")
        }
        if (monthlyRelief > 0L) {
            notes.add("آزادسازی نقدینگی ماهانه به میزان ${PersianCurrencyEngine.formatAmount(monthlyRelief)} — می‌توانید همین مبلغ را پس‌انداز یا صرف تسویه سریع‌تر کنید.")
        } else if (monthlyRelief < 0L && netTotalSavings > 0L) {
            notes.add("قسط ماهانه ${PersianCurrencyEngine.formatAmount(-monthlyRelief)} بیشتر می‌شود، اما در کل ${PersianCurrencyEngine.formatAmount(netTotalSavings)} کمتر سود می‌پردازید.")
        }
        if (dtiBefore != null && dtiAfter != null) {
            notes.add("نسبت قسط به درآمد این بدهی‌ها از ${PersianCurrencyEngine.toPersianDigits(dtiBefore.toString())}٪ به ${PersianCurrencyEngine.toPersianDigits(dtiAfter.toString())}٪ تغییر می‌کند.")
        }

        return DebtConsolidationReport(
            verdict = verdict,
            selectedDebts = selected,
            excludedLowRateCount = excludedLowRateCount,
            consolidationAnnualRatePercent = safeRate,
            consolidationTermMonths = safeTerm,
            originationFeePercent = safeFeePct,
            currentTotalPrincipalToman = currentPrincipal,
            currentTotalRemainingInterestToman = currentInterest,
            currentTotalRemainingCostToman = currentTotalCost,
            currentCombinedMonthlyPaymentToman = currentMonthly,
            currentWeightedAnnualRatePercent = weightedApr,
            currentLongestRemainingMonths = longestRemaining,
            consolidationLoanPrincipalToman = currentPrincipal,
            originationFeeToman = originationFee,
            consolidatedMonthlyPaymentToman = consolidatedMonthly,
            consolidatedTotalInterestToman = consolidatedInterest,
            consolidatedTotalCostToman = consolidatedTotalCost,
            netTotalSavingsToman = netTotalSavings,
            netInterestSavingsToman = netInterestSavings,
            monthlyCashflowReliefToman = monthlyRelief,
            rateSpreadPercent = rateSpread,
            breakEvenMonths = breakEvenMonths,
            monthlyDtiBeforePercent = dtiBefore,
            monthlyDtiAfterPercent = dtiAfter,
            consolidatedDebtFreeDateJalali = endDateJalali,
            consolidatedDebtFreeMonthLabelFa = endMonthLabelFa,
            verdictHeadlineFa = headlineFa,
            verdictDetailFa = detailFa,
            actionableNotesFa = notes
        )
    }
}
