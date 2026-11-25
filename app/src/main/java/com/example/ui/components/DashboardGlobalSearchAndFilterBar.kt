package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.CurrencyUnit
import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.data.CounterpartyEntity
import com.example.data.EntityCategory
import com.example.data.InstallmentEntity
import com.example.data.ObligationEntity
import com.example.domain.DashboardGlobalSearchEngine
import com.example.domain.DashboardGlobalSearchResult
import com.example.domain.DashboardSearchCriteria
import com.example.domain.DashboardSearchDebtMatch
import com.example.domain.DebtCategoryTag
import com.example.domain.DueDateRangePreset
import com.example.ui.theme.LocalTarazinoColors

private enum class ActiveDatePickerSlot {
    NONE,
    START_DATE,
    END_DATE
}

/**
 * Global Dashboard Search & Multi-Criteria Filter Bar.
 * Enables instant discovery of debts by name/counterparty, category or custom `#tags`,
 * and Shamsi due-date range presets or custom Jalali From/To bounds.
 */
@Composable
fun DashboardGlobalSearchAndFilterBar(
    obligations: List<ObligationEntity>,
    installments: List<InstallmentEntity>,
    counterparties: List<CounterpartyEntity>,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    onQuickPay: (InstallmentEntity) -> Unit,
    onOpenObligationInTree: (ObligationEntity, EntityCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTarazinoColors.current
    val tealAccent = ext.positive // Deep Persian Teal (#35B8AB / #0F6B66)
    val goldAccent = MaterialTheme.colorScheme.secondary // Champagne Gold (#DAB978 / #B58538)

    var criteria by remember { mutableStateOf(DashboardSearchCriteria()) }
    var isFilterPanelExpanded by remember { mutableStateOf(false) }
    var activeDatePicker by remember { mutableStateOf(ActiveDatePickerSlot.NONE) }

    val todayJalali = remember { JalaliCalendarEngine.todayJalali() }

    val searchResult: DashboardGlobalSearchResult = remember(
        obligations,
        installments,
        counterparties,
        criteria,
        todayJalali
    ) {
        DashboardGlobalSearchEngine.searchAndFilter(
            obligations = obligations,
            installments = installments,
            counterparties = counterparties,
            criteria = criteria,
            todayJalali = todayJalali
        )
    }

    val barBorderColor = rememberSpringColorAnimatable(
        targetColor = if (criteria.isActive || isFilterPanelExpanded) {
            tealAccent.copy(alpha = 0.72f)
        } else {
            ext.cardBorderTop
        }
    ).value

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_global_search_container"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Primary Glassmorphic Search Input Bar + Filter Drawer Toggle
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.80f else 0.94f),
            border = BorderStroke(
                width = if (criteria.isActive || isFilterPanelExpanded) 1.3.dp else 0.9.dp,
                color = barBorderColor
            ),
            shadowElevation = if (ext.isDark) 2.dp else 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "جستجوی سراسری بدهی‌ها",
                    tint = if (criteria.hasTextQuery) tealAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(21.dp)
                )

                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier.weight(1f)
                ) {
                    if (criteria.query.isEmpty()) {
                        Text(
                            text = "جستجوی بدهی با نام، برچسب یا تاریخ...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = criteria.query,
                        onValueChange = { next -> criteria = criteria.copy(query = next) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        ),
                        cursorBrush = SolidColor(tealAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_global_search_input")
                    )
                }

                if (criteria.hasTextQuery) {
                    IconButton(
                        onClick = { criteria = criteria.copy(query = "") },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("dashboard_global_search_clear_query_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "پاک کردن متن جستجو",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                // Filter Toggle Button with Live Active Filter Badge
                val filterActive = isFilterPanelExpanded || criteria.hasTagFilter || criteria.hasDateRangeFilter
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = if (filterActive) {
                        tealAccent.copy(alpha = if (ext.isDark) 0.24f else 0.15f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    },
                    border = BorderStroke(
                        width = if (filterActive) 1.1.dp else 0.75.dp,
                        color = if (filterActive) tealAccent else ext.cardBorderTop
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(13.dp))
                        .clickable { isFilterPanelExpanded = !isFilterPanelExpanded }
                        .testTag("dashboard_global_filter_toggle_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "فیلتر برچسب و بازه تاریخ",
                            tint = if (filterActive) tealAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "فیلتر",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (filterActive) tealAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchResult.activeFilterCount > 0) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(17.dp)
                                    .clip(CircleShape)
                                    .background(goldAccent)
                            ) {
                                Text(
                                    text = PersianCurrencyEngine.toPersianDigits(searchResult.activeFilterCount.toString()),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Collapsible Multi-Criteria Filter Panel (Tags + Shamsi Due Date Range)
        AnimatedVisibility(
            visible = isFilterPanelExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            TarazinoGlassCard(
                cornerRadius = 20.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_global_filter_panel")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    // 2A. Debt Category Tags Row
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalOffer,
                                    contentDescription = null,
                                    tint = tealAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "فیلتر بر اساس دسته و برچسب:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (criteria.isActive) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { criteria = DashboardSearchCriteria() }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .testTag("dashboard_global_reset_filters_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterAltOff,
                                        contentDescription = "پاک کردن فیلترها",
                                        tint = ext.danger,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "پاک کردن",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ext.danger
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DebtCategoryTag.FILTERABLE_TAGS.forEach { tag ->
                                val isSel = criteria.selectedCategoryTag == tag
                                val tagColor = Color(tag.colorHex)
                                FilterPillChip(
                                    label = tag.shortBadgeFa,
                                    selected = isSel,
                                    accentColor = tagColor,
                                    onClick = {
                                        criteria = criteria.copy(selectedCategoryTag = tag)
                                    },
                                    modifier = Modifier.testTag("global_filter_cat_${tag.id}")
                                )
                            }
                        }

                        // Optional Custom #Tags Strip extracted from user's obligations
                        if (searchResult.availableCustomTags.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                searchResult.availableCustomTags.forEach { customTag ->
                                    val isSel = criteria.selectedCustomTag.equals(customTag, ignoreCase = true)
                                    FilterPillChip(
                                        label = "#$customTag",
                                        selected = isSel,
                                        accentColor = goldAccent,
                                        onClick = {
                                            criteria = criteria.copy(
                                                selectedCustomTag = if (isSel) null else customTag
                                            )
                                        },
                                        modifier = Modifier.testTag("global_filter_custom_tag_$customTag")
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                    // 2B. Shamsi Due Date Range Presets & Custom Jalali Range Picker
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = goldAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "بازه تاریخ سررسید (شمسی):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DueDateRangePreset.entries.forEach { preset ->
                                val isSel = criteria.dueDatePreset == preset
                                val presetAccent = when (preset) {
                                    DueDateRangePreset.OVERDUE -> ext.danger
                                    DueDateRangePreset.CUSTOM_RANGE -> goldAccent
                                    else -> tealAccent
                                }
                                FilterPillChip(
                                    label = preset.labelFa,
                                    selected = isSel,
                                    accentColor = presetAccent,
                                    onClick = {
                                        if (preset == DueDateRangePreset.CUSTOM_RANGE) {
                                            val defaultStart = criteria.customStartDateJalali ?: todayJalali
                                            val defaultEnd = criteria.customEndDateJalali
                                                ?: JalaliCalendarEngine.addJalaliDays(todayJalali, 30)
                                            criteria = criteria.copy(
                                                dueDatePreset = preset,
                                                customStartDateJalali = defaultStart,
                                                customEndDateJalali = defaultEnd
                                            )
                                        } else {
                                            criteria = criteria.copy(dueDatePreset = preset)
                                        }
                                    },
                                    modifier = Modifier.testTag("global_filter_date_${preset.name}")
                                )
                            }
                        }

                        // Custom Jalali Start / End Date Selector Buttons
                        AnimatedVisibility(visible = criteria.dueDatePreset == DueDateRangePreset.CUSTOM_RANGE) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                JalaliRangeDateButton(
                                    labelFa = "از تاریخ سررسید",
                                    dateJalali = criteria.customStartDateJalali ?: todayJalali,
                                    accentColor = tealAccent,
                                    onClick = { activeDatePicker = ActiveDatePickerSlot.START_DATE },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("global_filter_start_date_btn")
                                )
                                JalaliRangeDateButton(
                                    labelFa = "تا تاریخ سررسید",
                                    dateJalali = criteria.customEndDateJalali
                                        ?: JalaliCalendarEngine.addJalaliDays(todayJalali, 30),
                                    accentColor = goldAccent,
                                    onClick = { activeDatePicker = ActiveDatePickerSlot.END_DATE },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("global_filter_end_date_btn")
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Instant Live Search & Filter Results Drawer (shown whenever any search or filter is active)
        AnimatedVisibility(
            visible = criteria.isActive,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            TarazinoGlassCard(
                cornerRadius = 22.dp,
                accentBorderColor = tealAccent.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_global_search_results_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Results Summary Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${PersianCurrencyEngine.toPersianDigits(searchResult.totalMatchedDebtsCount.toString())} بدهی یافت شد",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = tealAccent
                            )
                            Text(
                                text = buildString {
                                    if (criteria.hasDateRangeFilter) {
                                        append(searchResult.effectiveRangeLabelFa)
                                        append(" • ")
                                    }
                                    append("جمع مانده: ")
                                    append(
                                        PersianCurrencyEngine.formatAmount(
                                            searchResult.totalMatchedRemainingToman,
                                            currencyUnit,
                                            privacyMasked = privacyMasked
                                        )
                                    )
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ext.danger.copy(alpha = if (ext.isDark) 0.16f else 0.10f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    criteria = DashboardSearchCriteria()
                                    isFilterPanelExpanded = false
                                }
                                .testTag("dashboard_global_clear_all_results_btn")
                        ) {
                            Text(
                                text = "بستن و پاک‌سازی",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ext.danger,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }

                    if (searchResult.matches.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(15.dp),
                            color = ext.elevatedSurface.copy(alpha = 0.65f),
                            border = BorderStroke(0.8.dp, ext.cardBorderTop),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 18.dp, horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "هیچ بدهی‌ای با این نام، برچسب یا بازه تاریخ پیدا نشد.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "واژه جستجو یا بازه تاریخ شمسی را تغییر دهید یا فیلترها را پاک کنید.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            searchResult.matches.take(8).forEach { match ->
                                GlobalSearchDebtMatchCard(
                                    match = match,
                                    hasDateRangeFilter = criteria.hasDateRangeFilter,
                                    tealAccent = tealAccent,
                                    goldAccent = goldAccent,
                                    currencyUnit = currencyUnit,
                                    privacyMasked = privacyMasked,
                                    onQuickPay = onQuickPay,
                                    onOpen = {
                                        onOpenObligationInTree(
                                            match.obligation,
                                            match.counterparty?.categoryEnum
                                        )
                                    }
                                )
                            }
                            if (searchResult.matches.size > 8) {
                                Text(
                                    text = "نمایش ۸ مورد اول از ${PersianCurrencyEngine.toPersianDigits(searchResult.matches.size.toString())} نتیجه؛ برای دقت بیشتر برچسب یا بازه تاریخ اضافه کنید.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 4. Native Shamsi Date Picker Modal for Custom Due Date Range Start/End Selection
    if (activeDatePicker != ActiveDatePickerSlot.NONE) {
        val initialDate = when (activeDatePicker) {
            ActiveDatePickerSlot.START_DATE -> criteria.customStartDateJalali ?: todayJalali
            ActiveDatePickerSlot.END_DATE -> criteria.customEndDateJalali
                ?: JalaliCalendarEngine.addJalaliDays(todayJalali, 30)
            ActiveDatePickerSlot.NONE -> todayJalali
        }
        JalaliDatePickerModal(
            initialDateJalali = initialDate,
            onDismiss = { activeDatePicker = ActiveDatePickerSlot.NONE },
            onConfirmDate = { picked ->
                criteria = when (activeDatePicker) {
                    ActiveDatePickerSlot.START_DATE -> criteria.copy(
                        dueDatePreset = DueDateRangePreset.CUSTOM_RANGE,
                        customStartDateJalali = picked
                    )
                    ActiveDatePickerSlot.END_DATE -> criteria.copy(
                        dueDatePreset = DueDateRangePreset.CUSTOM_RANGE,
                        customEndDateJalali = picked
                    )
                    ActiveDatePickerSlot.NONE -> criteria
                }
                activeDatePicker = ActiveDatePickerSlot.NONE
            }
        )
    }
}

@Composable
private fun FilterPillChip(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTarazinoColors.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) {
            accentColor.copy(alpha = if (ext.isDark) 0.24f else 0.15f)
        } else {
            ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.55f else 0.80f)
        },
        border = BorderStroke(
            width = if (selected) 1.2.dp else 0.75.dp,
            color = if (selected) accentColor else ext.cardBorderTop
        ),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun JalaliRangeDateButton(
    labelFa: String,
    dateJalali: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTarazinoColors.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = if (ext.isDark) 0.14f else 0.09f),
        border = BorderStroke(0.95.dp, accentColor.copy(alpha = 0.45f)),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = labelFa,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Column {
                Text(
                    text = labelFa,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = PersianCurrencyEngine.toPersianDigits(dateJalali),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                )
            }
        }
    }
}

@Composable
private fun GlobalSearchDebtMatchCard(
    match: DashboardSearchDebtMatch,
    hasDateRangeFilter: Boolean,
    tealAccent: Color,
    goldAccent: Color,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    onQuickPay: (InstallmentEntity) -> Unit,
    onOpen: () -> Unit
) {
    val ext = LocalTarazinoColors.current
    val tagColor = Color(match.primaryTag.colorHex)

    Surface(
        shape = RoundedCornerShape(15.dp),
        color = ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.72f else 0.92f),
        border = BorderStroke(0.85.dp, ext.cardBorderTop),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .clickable(onClick = onOpen)
            .testTag("global_search_match_${match.obligation.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = match.obligation.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${match.counterparty?.name ?: match.primaryTag.labelFa} • ${
                            match.nextPayableInstallment?.dueDateJalali?.let {
                                "سررسید: ${PersianCurrencyEngine.toPersianDigits(it)}"
                            } ?: "تسویه‌شده"
                        }",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    FormattedMoneyText(
                        amountToman = match.matchedPeriodRemainingToman,
                        unit = currencyUnit,
                        privacyMasked = privacyMasked,
                        style = MaterialTheme.typography.labelLarge,
                        numberColor = if (match.matchedPeriodRemainingToman > 0L) goldAccent else tealAccent,
                        minFontSize = 9.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.widthIn(max = 136.dp)
                    )
                    if (hasDateRangeFilter) {
                        Text(
                            text = "${PersianCurrencyEngine.toPersianDigits(match.matchingInstallments.size.toString())} قسط در بازه",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Category Tag + Custom Tags + Quick Pay Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState())
                ) {
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = tagColor.copy(alpha = if (ext.isDark) 0.18f else 0.12f)
                    ) {
                        Text(
                            text = match.primaryTag.shortBadgeFa,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = tagColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    match.customTags.take(2).forEach { cTag ->
                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        ) {
                            Text(
                                text = "#$cTag",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                match.nextPayableInstallment?.let { payableInst ->
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = tealAccent.copy(alpha = if (ext.isDark) 0.20f else 0.13f),
                        border = BorderStroke(0.8.dp, tealAccent.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onQuickPay(payableInst) }
                            .testTag("global_search_quick_pay_${payableInst.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "پرداخت قسط",
                                tint = tealAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "پرداخت قسط",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = tealAccent,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
