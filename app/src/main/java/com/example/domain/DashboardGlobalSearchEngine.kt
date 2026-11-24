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
 * Quick Shamsi due-date range presets for the Dashboard global search & filter bar.
 */
enum class DueDateRangePreset(val labelFa: String) {
    ALL("همه تاریخ‌ها"),
    OVERDUE("معوقه‌ها"),
    NEXT_7_DAYS("۷ روز آینده"),
    THIS_SHAMSI_MONTH("ماه جاری"),
    NEXT_30_DAYS("۳۰ روز آینده"),
    CUSTOM_RANGE("بازه دلخواه شمسی")
}

/**
 * Multi-criteria query state for the Dashboard global search and filter bar.
 */
data class DashboardSearchCriteria(
    val query: String = "",
    val selectedCategoryTag: DebtCategoryTag = DebtCategoryTag.ALL,
    val selectedCustomTag: String? = null,
    val dueDatePreset: DueDateRangePreset = DueDateRangePreset.ALL,
    val customStartDateJalali: String? = null,
    val customEndDateJalali: String? = null
) {
    val hasTextQuery: Boolean
        get() = query.trim().isNotEmpty()

    val hasTagFilter: Boolean
        get() = selectedCategoryTag != DebtCategoryTag.ALL || !selectedCustomTag.isNullOrBlank()

    val hasDateRangeFilter: Boolean
        get() = dueDatePreset != DueDateRangePreset.ALL

    val isActive: Boolean
        get() = hasTextQuery || hasTagFilter || hasDateRangeFilter

    val activeFilterCount: Int
        get() {
            var count = 0
            if (hasTextQuery) count += 1
            if (selectedCategoryTag != DebtCategoryTag.ALL) count += 1
            if (!selectedCustomTag.isNullOrBlank()) count += 1
            if (hasDateRangeFilter) count += 1
            return count
        }
}

/**
 * A single debt obligation matching the active global search/filter criteria.
 */
data class DashboardSearchDebtMatch(
    val obligation: ObligationEntity,
    val counterparty: CounterpartyEntity?,
    val primaryTag: DebtCategoryTag,
    val customTags: List<String>,
    val matchingInstallments: List<InstallmentEntity>,
    val nextPayableInstallment: InstallmentEntity?,
    val matchedPeriodRemainingToman: Long,
    val totalObligationRemainingToman: Long,
    val progressPercent: Int,
    val matchReasonsFa: List<String>
)

/**
 * Complete evaluated output of [DashboardGlobalSearchEngine].
 */
data class DashboardGlobalSearchResult(
    val criteria: DashboardSearchCriteria,
    val effectiveStartDateJalali: String?,
    val effectiveEndDateJalali: String?,
    val effectiveRangeLabelFa: String,
    val matches: List<DashboardSearchDebtMatch>,
    val totalMatchedDebtsCount: Int,
    val totalMatchedInstallmentsCount: Int,
    val totalMatchedRemainingToman: Long,
    val availableCustomTags: List<String>,
    val activeFilterCount: Int
)

/**
 * Pure domain search & multi-criteria filtering engine for the Dashboard global search bar.
 * Supports searching by debt/counterparty name, category or custom `#tags`, and Persian (Shamsi)
 * due-date ranges (presets or custom From/To Jalali bounds).
 */
object DashboardGlobalSearchEngine {

    /**
     * Normalizes Persian/Arabic characters (`ي`->`ی`, `ك`->`ک`), digits, and whitespace
     * for reliable case-insensitive Persian/English search matching.
     */
    fun normalizeForSearch(raw: String): String {
        return PersianCurrencyEngine.toEnglishDigits(raw)
            .replace('ي', 'ی')
            .replace('ى', 'ی')
            .replace('ك', 'ک')
            .replace('\u200C', ' ') // ZWNJ to space for tolerant matching
            .trim()
            .lowercase(Locale.ROOT)
    }

    /**
     * Resolves the effective canonical Jalali date bounds `[startDate, endDate]` for a preset or custom range.
     */
    fun resolveDateRangeBounds(
        criteria: DashboardSearchCriteria,
        todayJalali: String = JalaliCalendarEngine.todayJalali()
    ): Triple<String?, String?, String> {
        val normToday = JalaliCalendarEngine.normalizeJalaliDate(todayJalali)
        val parsedToday = JalaliCalendarEngine.parseJalali(normToday)

        return when (criteria.dueDatePreset) {
            DueDateRangePreset.ALL -> Triple(null, null, "همه تاریخ‌ها")
            DueDateRangePreset.OVERDUE -> {
                val yesterday = JalaliCalendarEngine.addJalaliDays(normToday, -1)
                Triple("1300/01/01", yesterday, "معوقه‌های پیش از امروز")
            }
            DueDateRangePreset.NEXT_7_DAYS -> {
                val end7 = JalaliCalendarEngine.addJalaliDays(normToday, 7)
                Triple(
                    normToday,
                    end7,
                    "۷ روز آینده (${PersianCurrencyEngine.toPersianDigits(normToday)} تا ${PersianCurrencyEngine.toPersianDigits(end7)})"
                )
            }
            DueDateRangePreset.THIS_SHAMSI_MONTH -> {
                val monthLen = JalaliCalendarEngine.getJalaliMonthLength(parsedToday.year, parsedToday.month)
                val startM = "%04d/%02d/01".format(Locale.US, parsedToday.year, parsedToday.month)
                val endM = "%04d/%02d/%02d".format(Locale.US, parsedToday.year, parsedToday.month, monthLen)
                Triple(
                    startM,
                    endM,
                    "ماه ${parsedToday.monthNameFa} ${PersianCurrencyEngine.toPersianDigits(parsedToday.year.toString())}"
                )
            }
            DueDateRangePreset.NEXT_30_DAYS -> {
                val end30 = JalaliCalendarEngine.addJalaliDays(normToday, 30)
                Triple(
                    normToday,
                    end30,
                    "۳۰ روز آینده (${PersianCurrencyEngine.toPersianDigits(normToday)} تا ${PersianCurrencyEngine.toPersianDigits(end30)})"
                )
            }
            DueDateRangePreset.CUSTOM_RANGE -> {
                val rawStart = criteria.customStartDateJalali?.takeIf { it.isNotBlank() }
                    ?.let { JalaliCalendarEngine.normalizeJalaliDate(it) }
                val rawEnd = criteria.customEndDateJalali?.takeIf { it.isNotBlank() }
                    ?.let { JalaliCalendarEngine.normalizeJalaliDate(it) }

                // Ensure start <= end even if user picked them in reverse order
                val (s, e) = if (rawStart != null && rawEnd != null && rawStart > rawEnd) {
                    rawEnd to rawStart
                } else {
                    rawStart to rawEnd
                }
                val label = when {
                    s != null && e != null ->
                        "از ${PersianCurrencyEngine.toPersianDigits(s)} تا ${PersianCurrencyEngine.toPersianDigits(e)}"
                    s != null ->
                        "از ${PersianCurrencyEngine.toPersianDigits(s)} به بعد"
                    e != null ->
                        "تا ${PersianCurrencyEngine.toPersianDigits(e)}"
                    else -> "بازه دلخواه شمسی"
                }
                Triple(s, e, label)
            }
        }
    }

    /**
     * Evaluates all obligations and installments against [criteria] and returns sorted matches.
     */
    fun searchAndFilter(
        obligations: List<ObligationEntity>,
        installments: List<InstallmentEntity>,
        counterparties: List<CounterpartyEntity>,
        criteria: DashboardSearchCriteria,
        categoryOverrides: Map<Long, DebtCategoryTag> = emptyMap(),
        customTagsMap: Map<Long, List<String>> = emptyMap(),
        todayJalali: String = JalaliCalendarEngine.todayJalali()
    ): DashboardGlobalSearchResult {
        val cpMap = counterparties.associateBy { it.id }
        val instByOb = installments.groupBy { it.obligationId }

        val (startDate, endDate, rangeLabelFa) = resolveDateRangeBounds(criteria, todayJalali)
        val normalizedQuery = normalizeForSearch(criteria.query)
        val queryTokens = normalizedQuery.split(Regex("\\s+")).filter { it.isNotBlank() }

        val allCustomTags = linkedSetOf<String>()
        val matches = mutableListOf<DashboardSearchDebtMatch>()

        obligations.forEach { ob ->
            val cp = cpMap[ob.counterpartyId]
            val primaryTag = DebtTaggingAndCategorizationEngine.resolvePrimaryCategoryTag(
                obligation = ob,
                counterparty = cp,
                explicitOverrideTagId = categoryOverrides[ob.id]?.id
            )
            val customTags = DebtTaggingAndCategorizationEngine.extractCustomTags(
                obligation = ob,
                counterparty = cp,
                externalCustomTagsMap = customTagsMap
            )
            allCustomTags.addAll(customTags)

            // 1. Category Tag Filter
            val passesCategory = when (criteria.selectedCategoryTag) {
                DebtCategoryTag.ALL -> true
                DebtCategoryTag.RECEIVABLE -> primaryTag == DebtCategoryTag.RECEIVABLE
                else -> primaryTag == criteria.selectedCategoryTag
            }
            if (!passesCategory) return@forEach

            // 2. Custom Tag Filter
            val targetCustomTag = criteria.selectedCustomTag?.trim()
            val passesCustomTag = targetCustomTag.isNullOrBlank() ||
                customTags.any { normalizeForSearch(it) == normalizeForSearch(targetCustomTag) }
            if (!passesCustomTag) return@forEach

            val obInsts = instByOb[ob.id].orEmpty().sortedBy {
                JalaliCalendarEngine.normalizeJalaliDate(it.dueDateJalali)
            }

            // 3. Text Query Matching (Name, Counterparty, Notes, Category Tag, Custom Tags, Installment titles)
            val matchReasons = mutableListOf<String>()
            if (queryTokens.isNotEmpty()) {
                val titleNorm = normalizeForSearch(ob.title)
                val cpNorm = normalizeForSearch(cp?.name.orEmpty())
                val notesNorm = normalizeForSearch(ob.notes)
                val tagNorm = normalizeForSearch(
                    listOf(primaryTag.labelFa, primaryTag.shortBadgeFa, primaryTag.labelEn)
                        .plus(customTags)
                        .joinToString(" ")
                )
                val instTitlesNorm = normalizeForSearch(obInsts.joinToString(" ") { it.title })
                val combinedSearchable = "$titleNorm $cpNorm $notesNorm $tagNorm $instTitlesNorm"

                val allTokensMatched = queryTokens.all { token ->
                    combinedSearchable.contains(token)
                }
                if (!allTokensMatched) return@forEach

                if (queryTokens.any { titleNorm.contains(it) || cpNorm.contains(it) }) {
                    matchReasons.add("تطابق نام")
                }
                if (queryTokens.any { tagNorm.contains(it) }) {
                    matchReasons.add("تطابق برچسب")
                }
            }

            // 4. Shamsi Due Date Range Filter
            val matchingInsts = if (criteria.hasDateRangeFilter) {
                obInsts.filter { inst ->
                    val normDue = JalaliCalendarEngine.normalizeJalaliDate(inst.dueDateJalali)
                    val afterStart = startDate == null || normDue >= startDate
                    val beforeEnd = endDate == null || normDue <= endDate
                    val passesOverdueUnpaid = if (criteria.dueDatePreset == DueDateRangePreset.OVERDUE) {
                        inst.statusEnum != InstallmentStatus.PAID && inst.remainingToman > 0L
                    } else {
                        true
                    }
                    afterStart && beforeEnd && passesOverdueUnpaid
                }
            } else {
                obInsts
            }

            if (criteria.hasDateRangeFilter && matchingInsts.isEmpty()) {
                return@forEach
            }
            if (criteria.hasDateRangeFilter) {
                matchReasons.add("در بازه تاریخ")
            }
            if (criteria.selectedCategoryTag != DebtCategoryTag.ALL) {
                matchReasons.add(primaryTag.shortBadgeFa)
            }
            if (!targetCustomTag.isNullOrBlank()) {
                matchReasons.add("#$targetCustomTag")
            }

            val totalPayable = obInsts.sumOf { it.effectiveTotalToman }
            val totalPaid = obInsts.sumOf { it.paidAmountToman }
            val totalRemaining = obInsts.sumOf { it.remainingToman }
            val matchedRemaining = matchingInsts
                .filter { it.statusEnum != InstallmentStatus.PAID }
                .sumOf { it.remainingToman }

            val nextPayable = matchingInsts
                .firstOrNull { it.statusEnum != InstallmentStatus.PAID && it.remainingToman > 0L }
                ?: obInsts.firstOrNull { it.statusEnum != InstallmentStatus.PAID && it.remainingToman > 0L }

            val progressPct = if (totalPayable > 0L) {
                ((totalPaid.toDouble() / totalPayable.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
            } else {
                100
            }

            matches.add(
                DashboardSearchDebtMatch(
                    obligation = ob,
                    counterparty = cp,
                    primaryTag = primaryTag,
                    customTags = customTags,
                    matchingInstallments = matchingInsts,
                    nextPayableInstallment = nextPayable,
                    matchedPeriodRemainingToman = if (criteria.hasDateRangeFilter) matchedRemaining else totalRemaining,
                    totalObligationRemainingToman = totalRemaining,
                    progressPercent = progressPct,
                    matchReasonsFa = matchReasons.distinct()
                )
            )
        }

        // Sort matches: active unpaid debts with soonest next due date first, then by remaining amount descending
        val sortedMatches = matches.sortedWith(
            compareBy<DashboardSearchDebtMatch> { it.nextPayableInstallment == null }
                .thenBy {
                    it.nextPayableInstallment?.dueDateJalali?.let { d ->
                        JalaliCalendarEngine.normalizeJalaliDate(d)
                    } ?: "9999/12/29"
                }
                .thenByDescending { it.matchedPeriodRemainingToman }
        )

        return DashboardGlobalSearchResult(
            criteria = criteria,
            effectiveStartDateJalali = startDate,
            effectiveEndDateJalali = endDate,
            effectiveRangeLabelFa = rangeLabelFa,
            matches = sortedMatches,
            totalMatchedDebtsCount = sortedMatches.size,
            totalMatchedInstallmentsCount = sortedMatches.sumOf { it.matchingInstallments.size },
            totalMatchedRemainingToman = sortedMatches.sumOf { it.matchedPeriodRemainingToman },
            availableCustomTags = allCustomTags.toList(),
            activeFilterCount = criteria.activeFilterCount
        )
    }
}
