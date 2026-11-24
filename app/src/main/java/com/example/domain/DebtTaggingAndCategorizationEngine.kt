package com.example.domain

import com.example.core.JalaliCalendarEngine
import com.example.data.CounterpartyEntity
import com.example.data.EntityCategory
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.ObligationCategory
import com.example.data.ObligationEntity
import kotlin.math.roundToInt

/**
 * High-level debt categorization and tagging taxonomy (e.g., Bank Loan, Personal,
 * Credit Card & BNPL, Housing / Check & Rent, Receivable) used for filtered views
 * in the Dashboard and Detailed Spending Reports.
 */
enum class DebtCategoryTag(
    val id: String,
    val labelFa: String,
    val shortBadgeFa: String,
    val labelEn: String,
    val descriptionFa: String,
    val colorHex: Long
) {
    ALL(
        id = "ALL",
        labelFa = "همه دسته‌ها",
        shortBadgeFa = "همه",
        labelEn = "All Debts",
        descriptionFa = "نمایش یکپارچه تمامی وام‌ها، کارت‌های اعتباری و قرض‌ها",
        colorHex = 0xFF35B8AB
    ),
    BANK_LOAN(
        id = "BANK_LOAN",
        labelFa = "وام بانکی و صندوق",
        shortBadgeFa = "وام بانکی",
        labelEn = "Bank Loan",
        descriptionFa = "تسهیلات بانکی، قرض‌الحسنه و وام‌های صندوقی",
        colorHex = 0xFF35B8AB
    ),
    CREDIT_CARD(
        id = "CREDIT_CARD",
        labelFa = "کارت اعتباری و خرید اقساطی",
        shortBadgeFa = "کارت اعتباری و اقساطی",
        labelEn = "Credit Card",
        descriptionFa = "کارت‌های اعتباری، اسنپ‌پی، دیجی‌پی و خریدهای اقساطی",
        colorHex = 0xFF4DA3B8
    ),
    PERSONAL(
        id = "PERSONAL",
        labelFa = "شخصی و قرض دوستانه",
        shortBadgeFa = "شخصی",
        labelEn = "Personal",
        descriptionFa = "بدهی‌های شخصی، قرض از دوستان، خانواده و حساب دفتری",
        colorHex = 0xFFDAB978
    ),
    CHECK_AND_HOUSING(
        id = "CHECK_AND_HOUSING",
        labelFa = "چک صیادی و اجاره مسکن",
        shortBadgeFa = "چک و اجاره",
        labelEn = "Check & Housing",
        descriptionFa = "چک‌های صیادی، ودیعه و اجاره‌بهای دوره‌ای",
        colorHex = 0xFFC99B68
    ),
    RECEIVABLE(
        id = "RECEIVABLE",
        labelFa = "مطالبات و قرض داده‌شده",
        shortBadgeFa = "طلب از دیگران",
        labelEn = "Receivable",
        descriptionFa = "مبالغ طلب و قرض داده‌شده به اشخاص یا کارفرما",
        colorHex = 0xFF53C9BD
    );

    companion object {
        val DEBT_ONLY_TAGS = listOf(
            BANK_LOAN,
            CREDIT_CARD,
            PERSONAL,
            CHECK_AND_HOUSING
        )

        val FILTERABLE_TAGS = listOf(
            ALL,
            BANK_LOAN,
            CREDIT_CARD,
            PERSONAL,
            CHECK_AND_HOUSING,
            RECEIVABLE
        )

        fun fromId(id: String?): DebtCategoryTag {
            if (id.isNullOrBlank()) return ALL
            return entries.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) } ?: ALL
        }
    }
}

data class TaggedObligationSummary(
    val obligation: ObligationEntity,
    val counterparty: CounterpartyEntity?,
    val primaryTag: DebtCategoryTag,
    val customLabels: List<String>,
    val totalPayableToman: Long,
    val paidToman: Long,
    val remainingToman: Long,
    val currentMonthDueToman: Long,
    val currentMonthRemainingToman: Long,
    val overdueToman: Long,
    val totalInstallmentsCount: Int,
    val paidInstallmentsCount: Int,
    val remainingInstallmentsCount: Int,
    val nextDueDateJalali: String?,
    val progressPercent: Int
)

data class CategoryTagSpendingBreakdown(
    val tag: DebtCategoryTag,
    val obligationsCount: Int,
    val activeObligationsCount: Int,
    val totalPayableToman: Long,
    val paidToman: Long,
    val remainingToman: Long,
    val currentMonthDueToman: Long,
    val currentMonthRemainingToman: Long,
    val overdueToman: Long,
    val totalInstallmentsCount: Int,
    val paidInstallmentsCount: Int,
    val remainingInstallmentsCount: Int,
    val shareOfTotalRemainingPercent: Int,
    val shareOfMonthlySpendingPercent: Int,
    val settlementProgressPercent: Int,
    val sampleCounterpartyNamesFa: String,
    val customLabels: List<String>
)

data class DebtTaggingPortfolioReport(
    val selectedFilterTag: DebtCategoryTag,
    val selectedCustomTagFilter: String?,
    val todayJalali: String,
    val currentShamsiMonthNameFa: String,
    val allAvailableCustomTags: List<String>,
    val filteredObligations: List<TaggedObligationSummary>,
    val filteredInstallments: List<InstallmentEntity>,
    val filteredAlerts: List<EnrichedInstallmentAlert>,
    val filteredTotalPayableToman: Long,
    val filteredTotalPaidToman: Long,
    val filteredTotalRemainingToman: Long,
    val filteredCurrentMonthDueToman: Long,
    val filteredCurrentMonthRemainingToman: Long,
    val filteredOverdueToman: Long,
    val filteredSettlementProgressPercent: Int,
    val categoryBreakdowns: List<CategoryTagSpendingBreakdown>,
    val dominantCategoryTag: DebtCategoryTag?,
    val insightSummaryFa: String
)

/**
 * Pure domain engine for classifying, tagging, filtering, and generating detailed
 * category spending reports across obligations and installments.
 */
object DebtTaggingAndCategorizationEngine {

    private const val TAG_PREFIX = "#tag:"

    val PRESET_CUSTOM_TAGS_FA = listOf(
        "وام بانکی",
        "کارت اعتباری",
        "شخصی",
        "مسکن و اجاره",
        "چک صیادی",
        "خودرو",
        "کالای دیجیتال",
        "درمان و سلامت",
        "تحصیل",
        "اضطراری"
    )

    /**
     * Resolves the canonical [DebtCategoryTag] for an obligation based on explicit tag metadata
     * in `notes`, its [ObligationCategory], and its linked [CounterpartyEntity] category.
     */
    fun resolvePrimaryCategoryTag(
        obligation: ObligationEntity,
        counterparty: CounterpartyEntity? = null,
        explicitOverrideTagId: String? = null
    ): DebtCategoryTag {
        if (!explicitOverrideTagId.isNullOrBlank()) {
            val resolved = DebtCategoryTag.fromId(explicitOverrideTagId)
            if (resolved != DebtCategoryTag.ALL) return resolved
        }

        // Check if notes contain an embedded primary tag marker like [#cat:CREDIT_CARD]
        val embeddedCatRegex = Regex("""\[#cat:([A-Z_]+)]""")
        val embeddedMatch = embeddedCatRegex.find(obligation.notes)
        if (embeddedMatch != null) {
            val tag = DebtCategoryTag.fromId(embeddedMatch.groupValues[1])
            if (tag != DebtCategoryTag.ALL) return tag
        }

        val obCat = obligation.categoryEnum
        if (obCat.isReceivable || obCat == ObligationCategory.PERSONAL_RECEIVABLE) {
            return DebtCategoryTag.RECEIVABLE
        }
        if (obCat == ObligationCategory.CHECK_OR_BILL) {
            return DebtCategoryTag.CHECK_AND_HOUSING
        }
        if (obCat == ObligationCategory.BNPL_PURCHASE) {
            return DebtCategoryTag.CREDIT_CARD
        }
        if (obCat == ObligationCategory.PERSONAL_DEBT) {
            return DebtCategoryTag.PERSONAL
        }

        // Fallback to counterparty category or title heuristics
        val titleLower = obligation.title.lowercase()
        if (titleLower.contains("کارت اعتباری") ||
            titleLower.contains("credit") ||
            titleLower.contains("اسنپ") ||
            titleLower.contains("دیجی")
        ) {
            return DebtCategoryTag.CREDIT_CARD
        }
        if (titleLower.contains("چک") || titleLower.contains("اجاره") || titleLower.contains("رهن")) {
            return DebtCategoryTag.CHECK_AND_HOUSING
        }
        if (titleLower.contains("شخصی") || titleLower.contains("دوست") || titleLower.contains("قرض")) {
            if (counterparty?.categoryEnum == EntityCategory.PERSON) {
                return DebtCategoryTag.PERSONAL
            }
        }

        return when (counterparty?.categoryEnum) {
            EntityCategory.BNPL_PLATFORM -> DebtCategoryTag.CREDIT_CARD
            EntityCategory.PERSON -> DebtCategoryTag.PERSONAL
            EntityCategory.BANK, EntityCategory.INSTITUTION -> DebtCategoryTag.BANK_LOAN
            null -> DebtCategoryTag.BANK_LOAN
        }
    }

    /**
     * Extracts custom user-defined or preset tags stored in `obligation.notes` or external map.
     */
    fun extractCustomTags(
        obligation: ObligationEntity,
        counterparty: CounterpartyEntity? = null,
        externalCustomTagsMap: Map<Long, List<String>> = emptyMap()
    ): List<String> {
        val tags = linkedSetOf<String>()

        externalCustomTagsMap[obligation.id]?.forEach { raw ->
            val cleaned = raw.trim().removePrefix("#")
            if (cleaned.isNotBlank()) tags.add(cleaned)
        }

        // Parse `#tag:label` or `#برچسب` tokens from notes
        val hashRegex = Regex("""#([\p{L}\p{N}_\-‌]+)""")
        hashRegex.findAll(obligation.notes).forEach { match ->
            val token = match.groupValues[1].replace('_', ' ').trim()
            if (token.isNotBlank() && !token.startsWith("cat:", ignoreCase = true)) {
                tags.add(token)
            }
        }

        // Always include a default semantic tag if no explicit tag was added
        val primary = resolvePrimaryCategoryTag(obligation, counterparty)
        if (tags.isEmpty()) {
            tags.add(primary.shortBadgeFa)
        }
        return tags.toList()
    }

    /**
     * Serializes a map of `obligationId -> List<String>` and `obligationId -> DebtCategoryTag`
     * without using `org.json` so it runs identically on Android and pure JVM unit tests.
     */
    fun serializeTagOverrides(
        categoryOverrides: Map<Long, DebtCategoryTag>,
        customTagsMap: Map<Long, List<String>>
    ): String {
        val allIds = (categoryOverrides.keys + customTagsMap.keys).sorted()
        if (allIds.isEmpty()) return ""
        return allIds.joinToString(separator = "\n") { id ->
            val catId = categoryOverrides[id]?.id ?: ""
            val customJoined = customTagsMap[id]
                ?.map { it.replace("|", " ").replace(",", " ").trim() }
                ?.filter { it.isNotEmpty() }
                ?.distinct()
                ?.joinToString(",")
                ?: ""
            "$id|$catId|$customJoined"
        }
    }

    fun deserializeTagOverrides(
        serialized: String?
    ): Pair<Map<Long, DebtCategoryTag>, Map<Long, List<String>>> {
        if (serialized.isNullOrBlank()) return emptyMap<Long, DebtCategoryTag>() to emptyMap()
        val catMap = mutableMapOf<Long, DebtCategoryTag>()
        val customMap = mutableMapOf<Long, List<String>>()

        serialized.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEach
            val parts = trimmed.split("|")
            val obId = parts.getOrNull(0)?.toLongOrNull() ?: return@forEach
            val catId = parts.getOrNull(1).orEmpty()
            val customRaw = parts.getOrNull(2).orEmpty()

            if (catId.isNotBlank()) {
                val parsedTag = DebtCategoryTag.fromId(catId)
                if (parsedTag != DebtCategoryTag.ALL) {
                    catMap[obId] = parsedTag
                }
            }
            if (customRaw.isNotBlank()) {
                val tags = customRaw.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                if (tags.isNotEmpty()) {
                    customMap[obId] = tags
                }
            }
        }
        return catMap to customMap
    }

    /**
     * Evaluates the entire debt portfolio, enriches obligations with category tags, applies
     * the active category or custom tag filter, and builds a comprehensive spending breakdown.
     */
    fun evaluateCategorizedPortfolio(
        obligations: List<ObligationEntity>,
        installments: List<InstallmentEntity>,
        counterparties: List<CounterpartyEntity>,
        alerts: List<EnrichedInstallmentAlert> = emptyList(),
        selectedFilterTag: DebtCategoryTag = DebtCategoryTag.ALL,
        selectedCustomTagFilter: String? = null,
        categoryOverrides: Map<Long, DebtCategoryTag> = emptyMap(),
        customTagsMap: Map<Long, List<String>> = emptyMap(),
        todayJalali: String = JalaliCalendarEngine.todayJalali()
    ): DebtTaggingPortfolioReport {
        val parsedToday = JalaliCalendarEngine.parseJalali(todayJalali)
        val cpMap = counterparties.associateBy { it.id }
        val instByOb = installments.groupBy { it.obligationId }

        val enrichedObligations = obligations.map { ob ->
            val cp = cpMap[ob.counterpartyId]
            val overrideTag = categoryOverrides[ob.id]
            val primaryTag = resolvePrimaryCategoryTag(
                obligation = ob,
                counterparty = cp,
                explicitOverrideTagId = overrideTag?.id
            )
            val customLabels = extractCustomTags(
                obligation = ob,
                counterparty = cp,
                externalCustomTagsMap = customTagsMap
            )

            val obInsts = instByOb[ob.id].orEmpty()
            val totalPayable = obInsts.sumOf { it.effectiveTotalToman }
            val paid = obInsts.sumOf { it.paidAmountToman }
            val remaining = obInsts.sumOf { it.remainingToman }

            val monthInsts = obInsts.filter {
                JalaliCalendarEngine.isInJalaliMonth(it.dueDateJalali, parsedToday.year, parsedToday.month)
            }
            val currentMonthDue = monthInsts.sumOf { it.effectiveTotalToman }
            val currentMonthRem = monthInsts
                .filter { it.statusEnum != InstallmentStatus.PAID }
                .sumOf { it.remainingToman }

            val overdue = obInsts
                .filter {
                    it.statusEnum != InstallmentStatus.PAID &&
                        it.remainingToman > 0L &&
                        (it.statusEnum == InstallmentStatus.OVERDUE ||
                            JalaliCalendarEngine.diffJalaliDays(todayJalali, it.dueDateJalali) < 0)
                }
                .sumOf { it.remainingToman }

            val totalCount = obInsts.size
            val paidCount = obInsts.count { it.statusEnum == InstallmentStatus.PAID }
            val remCount = (totalCount - paidCount).coerceAtLeast(0)
            val nextDue = obInsts
                .filter { it.statusEnum != InstallmentStatus.PAID && it.remainingToman > 0L }
                .minByOrNull { it.dueDateJalali }
                ?.dueDateJalali

            val progressPct = if (totalPayable > 0L) {
                ((paid.toDouble() / totalPayable.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
            } else {
                100
            }

            TaggedObligationSummary(
                obligation = ob,
                counterparty = cp,
                primaryTag = primaryTag,
                customLabels = customLabels,
                totalPayableToman = totalPayable,
                paidToman = paid,
                remainingToman = remaining,
                currentMonthDueToman = currentMonthDue,
                currentMonthRemainingToman = currentMonthRem,
                overdueToman = overdue,
                totalInstallmentsCount = totalCount,
                paidInstallmentsCount = paidCount,
                remainingInstallmentsCount = remCount,
                nextDueDateJalali = nextDue,
                progressPercent = progressPct
            )
        }

        val allCustomTags = enrichedObligations
            .flatMap { it.customLabels }
            .distinct()

        // Apply active category tag filter + optional custom tag filter
        val matchingObligations = enrichedObligations.filter { item ->
            val matchesCategory = when (selectedFilterTag) {
                DebtCategoryTag.ALL -> !item.obligation.categoryEnum.isReceivable
                DebtCategoryTag.RECEIVABLE -> item.primaryTag == DebtCategoryTag.RECEIVABLE
                else -> item.primaryTag == selectedFilterTag
            }
            val matchesCustomTag = selectedCustomTagFilter.isNullOrBlank() ||
                item.customLabels.any { it.equals(selectedCustomTagFilter.trim(), ignoreCase = true) }
            matchesCategory && matchesCustomTag
        }

        val matchingObIds = matchingObligations.map { it.obligation.id }.toSet()
        val matchingInstallments = installments
            .filter { it.obligationId in matchingObIds }
            .sortedBy { it.dueDateJalali }

        val matchingAlerts = alerts.filter { it.obligation.id in matchingObIds }

        val filteredPayable = matchingObligations.sumOf { it.totalPayableToman }
        val filteredPaid = matchingObligations.sumOf { it.paidToman }
        val filteredRem = matchingObligations.sumOf { it.remainingToman }
        val filteredMonthDue = matchingObligations.sumOf { it.currentMonthDueToman }
        val filteredMonthRem = matchingObligations.sumOf { it.currentMonthRemainingToman }
        val filteredOverdue = matchingObligations.sumOf { it.overdueToman }
        val filteredProgressPct = if (filteredPayable > 0L) {
            ((filteredPaid.toDouble() / filteredPayable.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
        } else {
            100
        }

        // Calculate breakdown across all non-ALL tags
        val debtObligationsOnly = enrichedObligations.filter { it.primaryTag != DebtCategoryTag.RECEIVABLE }
        val grandTotalDebtRem = debtObligationsOnly.sumOf { it.remainingToman }.coerceAtLeast(1L)
        val grandTotalMonthlyDue = debtObligationsOnly.sumOf { it.currentMonthDueToman }.coerceAtLeast(1L)

        val breakdownTags = listOf(
            DebtCategoryTag.BANK_LOAN,
            DebtCategoryTag.CREDIT_CARD,
            DebtCategoryTag.PERSONAL,
            DebtCategoryTag.CHECK_AND_HOUSING,
            DebtCategoryTag.RECEIVABLE
        )

        val breakdowns = breakdownTags.map { tag ->
            val group = enrichedObligations.filter { it.primaryTag == tag }
            val payable = group.sumOf { it.totalPayableToman }
            val paid = group.sumOf { it.paidToman }
            val rem = group.sumOf { it.remainingToman }
            val monthDue = group.sumOf { it.currentMonthDueToman }
            val monthRem = group.sumOf { it.currentMonthRemainingToman }
            val overdue = group.sumOf { it.overdueToman }
            val totalInst = group.sumOf { it.totalInstallmentsCount }
            val paidInst = group.sumOf { it.paidInstallmentsCount }
            val remInst = group.sumOf { it.remainingInstallmentsCount }
            val activeObsCount = group.count { it.remainingToman > 0L }

            val shareRemPct = if (tag == DebtCategoryTag.RECEIVABLE) {
                0
            } else {
                ((rem.toDouble() / grandTotalDebtRem.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
            }
            val shareMonthPct = if (tag == DebtCategoryTag.RECEIVABLE) {
                0
            } else {
                ((monthDue.toDouble() / grandTotalMonthlyDue.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
            }
            val settlementPct = if (payable > 0L) {
                ((paid.toDouble() / payable.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
            } else {
                100
            }

            val cpNames = group
                .mapNotNull { it.counterparty?.name?.substringBefore(" (") }
                .distinct()
                .take(3)
                .joinToString("، ")
                .ifBlank { "بدون پرونده فعال" }

            val groupTags = group.flatMap { it.customLabels }.distinct()

            CategoryTagSpendingBreakdown(
                tag = tag,
                obligationsCount = group.size,
                activeObligationsCount = activeObsCount,
                totalPayableToman = payable,
                paidToman = paid,
                remainingToman = rem,
                currentMonthDueToman = monthDue,
                currentMonthRemainingToman = monthRem,
                overdueToman = overdue,
                totalInstallmentsCount = totalInst,
                paidInstallmentsCount = paidInst,
                remainingInstallmentsCount = remInst,
                shareOfTotalRemainingPercent = shareRemPct,
                shareOfMonthlySpendingPercent = shareMonthPct,
                settlementProgressPercent = settlementPct,
                sampleCounterpartyNamesFa = cpNames,
                customLabels = groupTags
            )
        }

        val dominantCategory = breakdowns
            .filter { it.tag != DebtCategoryTag.RECEIVABLE && it.remainingToman > 0L }
            .maxByOrNull { it.remainingToman }
            ?.tag

        val insightFa = buildInsightSummaryFa(
            selectedFilterTag = selectedFilterTag,
            dominantTag = dominantCategory,
            breakdowns = breakdowns,
            monthNameFa = parsedToday.monthNameFa
        )

        return DebtTaggingPortfolioReport(
            selectedFilterTag = selectedFilterTag,
            selectedCustomTagFilter = selectedCustomTagFilter,
            todayJalali = todayJalali,
            currentShamsiMonthNameFa = parsedToday.monthNameFa,
            allAvailableCustomTags = allCustomTags,
            filteredObligations = matchingObligations,
            filteredInstallments = matchingInstallments,
            filteredAlerts = matchingAlerts,
            filteredTotalPayableToman = filteredPayable,
            filteredTotalPaidToman = filteredPaid,
            filteredTotalRemainingToman = filteredRem,
            filteredCurrentMonthDueToman = filteredMonthDue,
            filteredCurrentMonthRemainingToman = filteredMonthRem,
            filteredOverdueToman = filteredOverdue,
            filteredSettlementProgressPercent = filteredProgressPct,
            categoryBreakdowns = breakdowns,
            dominantCategoryTag = dominantCategory,
            insightSummaryFa = insightFa
        )
    }

    private fun buildInsightSummaryFa(
        selectedFilterTag: DebtCategoryTag,
        dominantTag: DebtCategoryTag?,
        breakdowns: List<CategoryTagSpendingBreakdown>,
        monthNameFa: String
    ): String {
        if (selectedFilterTag != DebtCategoryTag.ALL) {
            val active = breakdowns.firstOrNull { it.tag == selectedFilterTag }
            if (active == null || active.obligationsCount == 0) {
                return "در دسته «${selectedFilterTag.labelFa}» هیچ پرونده فعالی ثبت نشده است."
            }
            return "دسته «${selectedFilterTag.labelFa}» شامل ${active.obligationsCount} پرونده (${active.sampleCounterpartyNamesFa}) با ${active.shareOfTotalRemainingPercent}٪ سهم از کل بدهی مانده و ${active.settlementProgressPercent}٪ پیشرفت تسویه است."
        }

        val dom = breakdowns.firstOrNull { it.tag == dominantTag }
        return if (dom != null && dom.remainingToman > 0L) {
            "بیشترین سهم بدهی فعال شما مربوط به دسته «${dom.tag.labelFa}» (${dom.shareOfTotalRemainingPercent}٪ از کل مانده و ${dom.shareOfMonthlySpendingPercent}٪ از بار اقساط $monthNameFa) است."
        } else {
            "تمامی دسته‌های بدهی (وام بانکی، کارت اعتباری و قرض شخصی) در وضعیت متعادل قرار دارند."
        }
    }
}
