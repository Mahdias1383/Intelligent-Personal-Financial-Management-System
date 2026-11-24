package com.example.domain

/**
 * Stable identifiers for every customizable Dashboard Overview module.
 * Users can prioritize, drag-and-drop, pin to top, or toggle visibility of each module.
 */
enum class DashboardModuleId(
    val titleFa: String,
    val shortTitleFa: String,
    val subtitleFa: String,
    val categoryBadgeFa: String,
    val accentColorHex: Long
) {
    PLATINUM_VAULT_HERO(
        titleFa = "کارت خزانه پلاتینیوم",
        shortTitleFa = "کارت خزانه",
        subtitleFa = "مانده این ماه، کل بدهی و مطالبات با نمایش سه‌بعدی",
        categoryBadgeFa = "نمای کلان",
        accentColorHex = 0xFF35B8AB
    ),
    AI_DAILY_FINANCIAL_TIP(
        titleFa = "نکته مالی روزانه هوشمند",
        shortTitleFa = "نکته روزانه",
        subtitleFa = "تحلیل الگوی مخارج اخیر و توصیه شخصی‌سازی‌شده روزانه",
        categoryBadgeFa = "هوش مصنوعی",
        accentColorHex = 0xFFDAB978
    ),
    FINANCIAL_HEALTH_RADAR(
        titleFa = "رادار سلامت مالی",
        shortTitleFa = "رادار سلامت مالی",
        subtitleFa = "نمودار راداری تعادل دسته‌های بدهی و ظرفیت بودجه",
        categoryBadgeFa = "تحلیل سلامت",
        accentColorHex = 0xFF47B0C4
    ),
    DEBT_SNOWBALL_TRACKER(
        titleFa = "گلوله برفی بدهی",
        shortTitleFa = "گلوله برفی بدهی",
        subtitleFa = "صف تسویه سریع‌ترین خرده‌بدهی‌ها و شبیه‌ساز بودجه مازاد",
        categoryBadgeFa = "شتاب تسویه",
        accentColorHex = 0xFF2CC896
    ),
    SHAMSI_DEBT_AND_SAVINGS(
        titleFa = "خلاصه ماه شمسی و هدف پس‌انداز",
        shortTitleFa = "ماه و پس‌انداز",
        subtitleFa = "نوار پیشرفت هدف پس‌انداز ماهانه و نزدیک‌ترین سررسیدها",
        categoryBadgeFa = "نمای کلان",
        accentColorHex = 0xFF53C9BD
    ),
    URGENT_ALERTS_RADAR(
        titleFa = "۳ سررسید فوری پیش‌رو",
        shortTitleFa = "سررسیدهای فوری",
        subtitleFa = "تسویه تک‌کلیکی و اسلایدر پرداخت نزدیک‌ترین اقساط",
        categoryBadgeFa = "فوریت پرداخت",
        accentColorHex = 0xFFD96B6B
    ),
    PAID_VS_OUTSTANDING_RING(
        titleFa = "حلقه پیشرفت پرداخت کل",
        shortTitleFa = "پیشرفت تسویه",
        subtitleFa = "مقایسه کل بدهی تسویه‌شده در برابر مانده فعلی",
        categoryBadgeFa = "نمودارها",
        accentColorHex = 0xFF35B8AB
    ),
    QUICK_ACTIONS_AND_SCANNER(
        titleFa = "اقدامات سریع و اسکنر قرارداد",
        shortTitleFa = "ابزارهای سریع",
        subtitleFa = "ثبت قسط، تسویه فوری، گزارش چاپی و اسکن دوربین",
        categoryBadgeFa = "ابزارها",
        accentColorHex = 0xFF689EB8
    ),
    COUNTERPARTY_WALLET_DECK(
        titleFa = "کیف کارت طرف‌های پرداخت",
        shortTitleFa = "کیف کارت‌ها",
        subtitleFa = "دسترسی سریع به کارت بانک‌ها، اسنپ‌پی، دیجی‌پی و اشخاص",
        categoryBadgeFa = "پوشه‌ها",
        accentColorHex = 0xFFC99B68
    ),
    WEEKLY_CASHFLOW_STRIP(
        titleFa = "نبض جریان نقدینگی ۷ روزه",
        shortTitleFa = "جریان ۷ روزه",
        subtitleFa = "نمای روزبه‌روز اقساط پیش‌رو در هفته جاری شمسی",
        categoryBadgeFa = "فوریت پرداخت",
        accentColorHex = 0xFF4DA3B8
    ),
    CATEGORY_DONUT_CHART(
        titleFa = "نمودار حلقه‌ای ترکیب دسته‌ها",
        shortTitleFa = "ترکیب دسته‌ها",
        subtitleFa = "سهم درصد هر دسته (خرید اقساطی، بانک، صندوق، اشخاص)",
        categoryBadgeFa = "نمودارها",
        accentColorHex = 0xFFDAB978
    ),
    DEBT_TAG_FILTER_VIEW(
        titleFa = "فیلتر برچسب و نوع تعهدات",
        shortTitleFa = "فیلتر برچسب‌ها",
        subtitleFa = "دسته‌بندی فوری بر اساس وام بانکی، چک، شخصی و تگ‌ها",
        categoryBadgeFa = "ابزارها",
        accentColorHex = 0xFF5E96AE
    ),
    SAFE_CAPACITY_ML_FORECAST(
        titleFa = "سقف امن قسط و پیش‌بینی هوشمند",
        shortTitleFa = "سقف امن و پیش‌بینی",
        subtitleFa = "ظرفیت امن افزودن قسط جدید و هشدار کسری ۱۲ ماهه",
        categoryBadgeFa = "تحلیل سلامت",
        accentColorHex = 0xFF47B0C4
    );

    companion object {
        fun fromNameOrNull(raw: String): DashboardModuleId? {
            return entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
        }
    }
}

/**
 * One-tap focus presets that immediately re-prioritize the Dashboard modules
 * according to the user's current financial goal.
 */
enum class DashboardFocusPreset(
    val labelFa: String,
    val shortLabelFa: String,
    val descriptionFa: String
) {
    BALANCED_DEFAULT(
        labelFa = "چیدمان استاندارد ترازینو",
        shortLabelFa = "استاندارد",
        descriptionFa = "تعادل میان کارت خزانه، نکته هوشمند روزانه و خلاصه ماه"
    ),
    FINANCIAL_HEALTH_RADAR_FOCUS(
        labelFa = "تمرکز رادار سلامت مالی",
        shortLabelFa = "رادار سلامت",
        descriptionFa = "اولویت با رادار سلامت مالی، سقف امن قسط و تحلیل هوشمند"
    ),
    DEBT_SNOWBALL_FOCUS(
        labelFa = "تمرکز گلوله برفی بدهی",
        shortLabelFa = "گلوله برفی",
        descriptionFa = "اولویت با صف گلوله برفی بدهی، حلقه تسویه و سررسیدهای فوری"
    ),
    URGENT_DEADLINES_FOCUS(
        labelFa = "تمرکز سررسیدهای فوری",
        shortLabelFa = "سررسید فوری",
        descriptionFa = "اولویت با اقساط معوق/فوری، جریان ۷ روزه و اقدامات سریع"
    ),
    CUSTOM(
        labelFa = "چیدمان شخصی‌سازی‌شده",
        shortLabelFa = "شخصی",
        descriptionFa = "ترتیب دلخواه شما با کشیدن و رها کردن ماژول‌ها"
    );

    companion object {
        fun fromNameOrDefault(raw: String?): DashboardFocusPreset {
            if (raw.isNullOrBlank()) return BALANCED_DEFAULT
            return entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
                ?: BALANCED_DEFAULT
        }
    }
}

data class DashboardLayoutConfig(
    val orderedModules: List<DashboardModuleId> = DashboardLayoutGridEngine.DEFAULT_ORDER,
    val hiddenModules: Set<DashboardModuleId> = emptySet(),
    val activePreset: DashboardFocusPreset = DashboardFocusPreset.BALANCED_DEFAULT
) {
    val visibleOrderedModules: List<DashboardModuleId>
        get() = orderedModules.filterNot { it in hiddenModules }
            .ifEmpty { listOf(DashboardModuleId.PLATINUM_VAULT_HERO) }
}

/**
 * Pure Kotlin engine managing dashboard module prioritization, drag-and-drop grid reordering,
 * one-tap focus presets, visibility toggles, and safe serialization for SharedPreferences.
 */
object DashboardLayoutGridEngine {

    val DEFAULT_ORDER: List<DashboardModuleId> = listOf(
        DashboardModuleId.PLATINUM_VAULT_HERO,
        DashboardModuleId.AI_DAILY_FINANCIAL_TIP,
        DashboardModuleId.SHAMSI_DEBT_AND_SAVINGS,
        DashboardModuleId.DEBT_SNOWBALL_TRACKER,
        DashboardModuleId.FINANCIAL_HEALTH_RADAR,
        DashboardModuleId.PAID_VS_OUTSTANDING_RING,
        DashboardModuleId.QUICK_ACTIONS_AND_SCANNER,
        DashboardModuleId.COUNTERPARTY_WALLET_DECK,
        DashboardModuleId.WEEKLY_CASHFLOW_STRIP,
        DashboardModuleId.CATEGORY_DONUT_CHART,
        DashboardModuleId.DEBT_TAG_FILTER_VIEW,
        DashboardModuleId.URGENT_ALERTS_RADAR,
        DashboardModuleId.SAFE_CAPACITY_ML_FORECAST
    )

    fun defaultConfig(): DashboardLayoutConfig = DashboardLayoutConfig(
        orderedModules = DEFAULT_ORDER,
        hiddenModules = emptySet(),
        activePreset = DashboardFocusPreset.BALANCED_DEFAULT
    )

    fun presetOrderFor(preset: DashboardFocusPreset): List<DashboardModuleId> {
        val prioritizedTop = when (preset) {
            DashboardFocusPreset.BALANCED_DEFAULT,
            DashboardFocusPreset.CUSTOM -> DEFAULT_ORDER

            DashboardFocusPreset.FINANCIAL_HEALTH_RADAR_FOCUS -> listOf(
                DashboardModuleId.FINANCIAL_HEALTH_RADAR,
                DashboardModuleId.SAFE_CAPACITY_ML_FORECAST,
                DashboardModuleId.AI_DAILY_FINANCIAL_TIP,
                DashboardModuleId.PLATINUM_VAULT_HERO,
                DashboardModuleId.SHAMSI_DEBT_AND_SAVINGS,
                DashboardModuleId.DEBT_SNOWBALL_TRACKER
            )

            DashboardFocusPreset.DEBT_SNOWBALL_FOCUS -> listOf(
                DashboardModuleId.DEBT_SNOWBALL_TRACKER,
                DashboardModuleId.PAID_VS_OUTSTANDING_RING,
                DashboardModuleId.URGENT_ALERTS_RADAR,
                DashboardModuleId.AI_DAILY_FINANCIAL_TIP,
                DashboardModuleId.FINANCIAL_HEALTH_RADAR,
                DashboardModuleId.PLATINUM_VAULT_HERO
            )

            DashboardFocusPreset.URGENT_DEADLINES_FOCUS -> listOf(
                DashboardModuleId.URGENT_ALERTS_RADAR,
                DashboardModuleId.WEEKLY_CASHFLOW_STRIP,
                DashboardModuleId.QUICK_ACTIONS_AND_SCANNER,
                DashboardModuleId.PLATINUM_VAULT_HERO,
                DashboardModuleId.SHAMSI_DEBT_AND_SAVINGS,
                DashboardModuleId.AI_DAILY_FINANCIAL_TIP
            )
        }
        return normalizeOrder(prioritizedTop)
    }

    fun applyFocusPreset(
        preset: DashboardFocusPreset,
        preserveHidden: Set<DashboardModuleId> = emptySet()
    ): DashboardLayoutConfig {
        val targetOrder = presetOrderFor(preset)
        // Unhide top 3 modules of the chosen preset so the user immediately sees their chosen focus
        val revealedTop = targetOrder.take(3).toSet()
        val updatedHidden = preserveHidden - revealedTop
        return normalizeConfig(
            rawOrder = targetOrder,
            rawHidden = updatedHidden,
            preset = preset
        )
    }

    fun moveModule(
        config: DashboardLayoutConfig,
        fromIndex: Int,
        toIndex: Int
    ): DashboardLayoutConfig {
        val normalized = normalizeOrder(config.orderedModules)
        if (normalized.isEmpty()) return defaultConfig()
        val safeFrom = fromIndex.coerceIn(0, normalized.lastIndex)
        val safeTo = toIndex.coerceIn(0, normalized.lastIndex)
        if (safeFrom == safeTo) return config

        val mutable = normalized.toMutableList()
        val moved = mutable.removeAt(safeFrom)
        mutable.add(safeTo, moved)

        return config.copy(
            orderedModules = mutable,
            activePreset = DashboardFocusPreset.CUSTOM
        )
    }

    fun moveModuleByDelta(
        config: DashboardLayoutConfig,
        moduleId: DashboardModuleId,
        delta: Int
    ): DashboardLayoutConfig {
        val normalized = normalizeOrder(config.orderedModules)
        val currentIndex = normalized.indexOf(moduleId)
        if (currentIndex < 0) return config
        val targetIndex = (currentIndex + delta).coerceIn(0, normalized.lastIndex)
        return moveModule(config, currentIndex, targetIndex)
    }

    fun pinModuleToTop(
        config: DashboardLayoutConfig,
        moduleId: DashboardModuleId
    ): DashboardLayoutConfig {
        val normalized = normalizeOrder(config.orderedModules)
        val currentIndex = normalized.indexOf(moduleId)
        if (currentIndex <= 0 && moduleId !in config.hiddenModules) {
            return config
        }
        val moved = if (currentIndex > 0) {
            moveModule(config, currentIndex, 0)
        } else {
            config
        }
        return moved.copy(
            hiddenModules = moved.hiddenModules - moduleId,
            activePreset = DashboardFocusPreset.CUSTOM
        )
    }

    fun toggleModuleVisibility(
        config: DashboardLayoutConfig,
        moduleId: DashboardModuleId
    ): DashboardLayoutConfig {
        val normalized = normalizeConfig(config.orderedModules, config.hiddenModules, config.activePreset)
        val currentlyHidden = moduleId in normalized.hiddenModules
        return if (currentlyHidden) {
            normalized.copy(
                hiddenModules = normalized.hiddenModules - moduleId,
                activePreset = DashboardFocusPreset.CUSTOM
            )
        } else {
            // Never allow hiding the final remaining visible module
            val visibleCount = normalized.orderedModules.count { it !in normalized.hiddenModules }
            if (visibleCount <= 1) {
                normalized
            } else {
                normalized.copy(
                    hiddenModules = normalized.hiddenModules + moduleId,
                    activePreset = DashboardFocusPreset.CUSTOM
                )
            }
        }
    }

    fun normalizeOrder(rawOrder: List<DashboardModuleId>): List<DashboardModuleId> {
        val uniqueValid = LinkedHashSet<DashboardModuleId>(DashboardModuleId.entries.size)
        rawOrder.forEach { uniqueValid.add(it) }
        DEFAULT_ORDER.forEach { uniqueValid.add(it) }
        DashboardModuleId.entries.forEach { uniqueValid.add(it) }
        return uniqueValid.toList()
    }

    fun normalizeConfig(
        rawOrder: List<DashboardModuleId>,
        rawHidden: Set<DashboardModuleId>,
        preset: DashboardFocusPreset
    ): DashboardLayoutConfig {
        val order = normalizeOrder(rawOrder)
        val safeHidden = rawHidden.filterTo(LinkedHashSet()) { it in order }
        if (safeHidden.size >= order.size && order.isNotEmpty()) {
            safeHidden.remove(order.first())
        }
        return DashboardLayoutConfig(
            orderedModules = order,
            hiddenModules = safeHidden,
            activePreset = preset
        )
    }

    fun serializeConfig(config: DashboardLayoutConfig): String {
        val normalized = normalizeConfig(config.orderedModules, config.hiddenModules, config.activePreset)
        val orderStr = normalized.orderedModules.joinToString(",") { it.name }
        val hiddenStr = normalized.hiddenModules.joinToString(",") { it.name }
        return "${normalized.activePreset.name}|$orderStr|$hiddenStr"
    }

    fun deserializeConfig(raw: String?): DashboardLayoutConfig {
        if (raw.isNullOrBlank()) return defaultConfig()
        val parts = raw.split("|")
        if (parts.size < 2) return defaultConfig()
        val preset = DashboardFocusPreset.fromNameOrDefault(parts[0])
        val order = parts[1]
            .split(",")
            .mapNotNull { DashboardModuleId.fromNameOrNull(it) }
        val hidden = parts.getOrNull(2)
            ?.split(",")
            ?.mapNotNull { DashboardModuleId.fromNameOrNull(it) }
            ?.toSet()
            ?: emptySet()
        return normalizeConfig(order, hidden, preset)
    }
}
