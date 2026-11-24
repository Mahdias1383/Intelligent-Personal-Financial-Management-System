package com.example.ui.components

import android.content.Context
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.CurrencyUnit
import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.data.CounterpartyEntity
import com.example.data.EntityCategory
import com.example.data.InstallmentEntity
import com.example.data.ObligationEntity
import com.example.domain.CategoryTagSpendingBreakdown
import com.example.domain.DebtCategoryTag
import com.example.domain.DebtTaggingAndCategorizationEngine
import com.example.domain.EnrichedInstallmentAlert
import com.example.domain.TaggedObligationSummary
import com.example.ui.theme.LocalTarazinoColors

private const val PREFS_NAME = "tarazino_prefs_v1"
private const val KEY_DEBT_TAG_OVERRIDES_V1 = "debt_category_tag_overrides_v1"

fun debtTagIcon(tag: DebtCategoryTag): ImageVector {
    return when (tag) {
        DebtCategoryTag.ALL -> Icons.Default.FilterList
        DebtCategoryTag.BANK_LOAN -> Icons.Default.AccountBalance
        DebtCategoryTag.CREDIT_CARD -> Icons.Default.CreditCard
        DebtCategoryTag.PERSONAL -> Icons.Default.Person
        DebtCategoryTag.CHECK_AND_HOUSING -> Icons.Default.HomeWork
        DebtCategoryTag.RECEIVABLE -> Icons.Default.CheckCircle
    }
}

/**
 * Dashboard component that provides interactive debt tagging & category filtering
 * ('All', 'Bank Loan', 'Credit Card / BNPL', 'Personal', 'Check & Housing', 'Receivable')
 * plus custom user tags, filtered KPI summaries, filtered obligation rows, and an inline
 * Tag Assignment modal so users can tag or re-categorize any debt directly from the dashboard.
 */
@Composable
fun DashboardDebtCategoryTagFilterSection(
    obligations: List<ObligationEntity>,
    installments: List<InstallmentEntity>,
    counterparties: List<CounterpartyEntity>,
    alerts: List<EnrichedInstallmentAlert>,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    onQuickPay: (InstallmentEntity) -> Unit,
    onOpenObligationInTree: (ObligationEntity, EntityCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val ext = LocalTarazinoColors.current
    val prefs = remember(context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var persistedState by remember {
        mutableStateOf(
            DebtTaggingAndCategorizationEngine.deserializeTagOverrides(
                prefs.getString(KEY_DEBT_TAG_OVERRIDES_V1, null)
            )
        )
    }
    val categoryOverrides = persistedState.first
    val customTagsMap = persistedState.second

    fun saveTagOverrides(
        newCatOverrides: Map<Long, DebtCategoryTag>,
        newCustomTags: Map<Long, List<String>>
    ) {
        persistedState = newCatOverrides to newCustomTags
        prefs.edit()
            .putString(
                KEY_DEBT_TAG_OVERRIDES_V1,
                DebtTaggingAndCategorizationEngine.serializeTagOverrides(
                    categoryOverrides = newCatOverrides,
                    customTagsMap = newCustomTags
                )
            )
            .apply()
    }

    var selectedTag by remember { mutableStateOf(DebtCategoryTag.ALL) }
    var selectedCustomLabel by remember { mutableStateOf<String?>(null) }
    var taggingDialogTarget by remember { mutableStateOf<TaggedObligationSummary?>(null) }

    val todayJalali = remember { JalaliCalendarEngine.todayJalali() }
    val report = remember(
        obligations,
        installments,
        counterparties,
        alerts,
        selectedTag,
        selectedCustomLabel,
        categoryOverrides,
        customTagsMap,
        todayJalali
    ) {
        DebtTaggingAndCategorizationEngine.evaluateCategorizedPortfolio(
            obligations = obligations,
            installments = installments,
            counterparties = counterparties,
            alerts = alerts,
            selectedFilterTag = selectedTag,
            selectedCustomTagFilter = selectedCustomLabel,
            categoryOverrides = categoryOverrides,
            customTagsMap = customTagsMap,
            todayJalali = todayJalali
        )
    }

    val activeAccentColor = Color(selectedTag.colorHex)

    TarazinoGlassCard(
        cornerRadius = 24.dp,
        accentBorderColor = activeAccentColor,
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_debt_category_filter_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TarazinoSectionHeader(
                title = "فیلتر هوشمند دسته‌بندی و برچسب بدهی‌ها",
                subtitle = "تفکیک وام بانکی، کارت اعتباری، قرض شخصی و برچسب‌های سفارشی",
                icon = Icons.AutoMirrored.Filled.Label,
                accentColor = activeAccentColor,
                trailing = {
                    TarazinoStatusPill(
                        text = "${PersianCurrencyEngine.toPersianDigits(report.filteredObligations.size.toString())} پرونده",
                        accentColor = activeAccentColor
                    )
                }
            )

            // 1. Primary Debt Category Filter Chips ('All', 'Bank Loan', 'Credit Card', 'Personal', 'Check & Housing', 'Receivable')
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DebtCategoryTag.FILTERABLE_TAGS.forEach { tag ->
                    val isSelected = selectedTag == tag
                    val tagColor = Color(tag.colorHex)
                    val tagCount = if (tag == DebtCategoryTag.ALL) {
                        obligations.count { !it.categoryEnum.isReceivable }
                    } else {
                        report.categoryBreakdowns.firstOrNull { it.tag == tag }?.obligationsCount ?: 0
                    }

                    val bg by animateColorAsState(
                        targetValue = if (isSelected) {
                            tagColor.copy(alpha = if (ext.isDark) 0.24f else 0.16f)
                        } else {
                            ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.65f else 0.85f)
                        },
                        label = "DebtCatChipBg_${tag.id}"
                    )
                    val borderCol by animateColorAsState(
                        targetValue = if (isSelected) tagColor else ext.cardBorderTop,
                        label = "DebtCatChipBorder_${tag.id}"
                    )

                    Surface(
                        shape = RoundedCornerShape(13.dp),
                        color = bg,
                        border = BorderStroke(if (isSelected) 1.3.dp else 0.8.dp, borderCol),
                        modifier = Modifier
                            .clip(RoundedCornerShape(13.dp))
                            .clickable {
                                selectedTag = tag
                                selectedCustomLabel = null
                            }
                            .testTag("dashboard_debt_tag_chip_${tag.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = debtTagIcon(tag),
                                contentDescription = tag.labelFa,
                                tint = if (isSelected) tagColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = tag.shortBadgeFa,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isSelected) tagColor else MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = tagColor.copy(alpha = if (isSelected) 0.25f else 0.12f)
                            ) {
                                Text(
                                    text = PersianCurrencyEngine.toPersianDigits(tagCount.toString()),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = tagColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Custom Label Sub-Filter Bar
            if (report.allAvailableCustomTags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "برچسب‌ها:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    report.allAvailableCustomTags.forEach { label ->
                        val isLabelSel = selectedCustomLabel.equals(label, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (isLabelSel) {
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                            },
                            border = BorderStroke(
                                0.8.dp,
                                if (isLabelSel) MaterialTheme.colorScheme.secondary else ext.cardBorderTop
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable {
                                    selectedCustomLabel = if (isLabelSel) null else label
                                }
                                .testTag("dashboard_custom_label_chip_$label")
                        ) {
                            Text(
                                text = "#$label",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isLabelSel) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isLabelSel) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // 3. Filtered View Summary Metrics Strip (Remaining, Monthly Due, Settlement Progress)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.72f else 0.88f),
                    border = BorderStroke(0.8.dp, ext.cardBorderTop),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("filtered_category_remaining_kpi")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        TarazinoAutoSizeText(
                            text = "مانده (${selectedTag.shortBadgeFa})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            minFontSize = 8.5.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        FormattedMoneyText(
                            amountToman = report.filteredTotalRemainingToman,
                            unit = currencyUnit,
                            privacyMasked = privacyMasked,
                            style = MaterialTheme.typography.labelLarge,
                            numberColor = activeAccentColor,
                            unitColor = activeAccentColor.copy(alpha = 0.8f),
                            minFontSize = 9.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.72f else 0.88f),
                    border = BorderStroke(0.8.dp, ext.cardBorderTop),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("filtered_category_month_due_kpi")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        TarazinoAutoSizeText(
                            text = "بار اقساط ${report.currentShamsiMonthNameFa}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            minFontSize = 8.5.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        FormattedMoneyText(
                            amountToman = report.filteredCurrentMonthRemainingToman,
                            unit = currencyUnit,
                            privacyMasked = privacyMasked,
                            style = MaterialTheme.typography.labelLarge,
                            numberColor = MaterialTheme.colorScheme.onSurface,
                            unitColor = MaterialTheme.colorScheme.secondary,
                            minFontSize = 9.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.72f else 0.88f),
                    border = BorderStroke(0.8.dp, ext.cardBorderTop),
                    modifier = Modifier
                        .weight(0.7f)
                        .testTag("filtered_category_progress_kpi")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        TarazinoAutoSizeText(
                            text = "پیشرفت تسویه",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            minFontSize = 8.5.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${PersianCurrencyEngine.toPersianDigits(report.filteredSettlementProgressPercent.toString())}٪",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = ext.positive
                        )
                    }
                }
            }

            // 4. Filtered Obligations List with One-Tap Tag Editing & Quick Pay
            if (report.filteredObligations.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(0.8.dp, ext.cardBorderTop),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "هیچ پرونده‌ای با فیلتر انتخاب‌شده یافت نشد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    report.filteredObligations.take(5).forEach { item ->
                        val itemColor = Color(item.primaryTag.colorHex)
                        val nextUnpaidInst = remember(installments, item.obligation.id) {
                            installments
                                .filter { it.obligationId == item.obligation.id && it.remainingToman > 0L }
                                .minByOrNull { it.dueDateJalali }
                        }

                        Surface(
                            shape = RoundedCornerShape(15.dp),
                            color = ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.66f else 0.86f),
                            border = BorderStroke(0.85.dp, itemColor.copy(alpha = 0.38f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(15.dp))
                                .clickable {
                                    onOpenObligationInTree(item.obligation, item.counterparty?.categoryEnum)
                                }
                                .testTag("dashboard_filtered_obligation_row_${item.obligation.id}")
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(itemColor.copy(alpha = 0.16f))
                                        ) {
                                            Icon(
                                                imageVector = debtTagIcon(item.primaryTag),
                                                contentDescription = null,
                                                tint = itemColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            TarazinoAutoSizeText(
                                                text = item.obligation.title,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                minFontSize = 9.5.sp
                                            )
                                            TarazinoAutoSizeText(
                                                text = "${item.counterparty?.name ?: "طرف‌حساب"} • ${
                                                    PersianCurrencyEngine.toPersianDigits(item.remainingInstallmentsCount.toString())
                                                } قسط مانده",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                minFontSize = 8.5.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    FormattedMoneyText(
                                        amountToman = item.remainingToman,
                                        unit = currencyUnit,
                                        privacyMasked = privacyMasked,
                                        style = MaterialTheme.typography.labelLarge,
                                        numberColor = itemColor,
                                        minFontSize = 9.sp,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.widthIn(max = 125.dp)
                                    )
                                }

                                // Tag badges + Edit Tag button + Quick Settle Next Installment
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
                                            color = itemColor.copy(alpha = 0.16f),
                                            border = BorderStroke(0.6.dp, itemColor.copy(alpha = 0.45f))
                                        ) {
                                            Text(
                                                text = item.primaryTag.shortBadgeFa,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = itemColor,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                        item.customLabels
                                            .filter { !it.equals(item.primaryTag.shortBadgeFa, ignoreCase = true) }
                                            .take(3)
                                            .forEach { customTag ->
                                                Surface(
                                                    shape = RoundedCornerShape(7.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                                ) {
                                                    Text(
                                                        text = "#$customTag",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                                            border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { taggingDialogTarget = item }
                                                .testTag("edit_obligation_tags_btn_${item.obligation.id}")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Label,
                                                    contentDescription = "ویرایش برچسب",
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "برچسب",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }

                                        if (nextUnpaidInst != null) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = ext.positive.copy(alpha = 0.16f),
                                                border = BorderStroke(0.7.dp, ext.positive.copy(alpha = 0.45f)),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { onQuickPay(nextUnpaidInst) }
                                            ) {
                                                Text(
                                                    text = "تسویه قسط",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = ext.positive,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Smart Category Distribution Insight Bar
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = activeAccentColor.copy(alpha = if (ext.isDark) 0.14f else 0.09f),
                border = BorderStroke(0.8.dp, activeAccentColor.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = report.insightSummaryFa,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)
                )
            }
        }
    }

    // Modal for assigning category tag ('Bank Loan', 'Credit Card', 'Personal', etc.) & custom tags
    taggingDialogTarget?.let { target ->
        ObligationTagAssignmentModal(
            target = target,
            onDismiss = { taggingDialogTarget = null },
            onSaveTags = { chosenCategoryTag, chosenCustomLabels ->
                val updatedCats = categoryOverrides.toMutableMap().apply {
                    put(target.obligation.id, chosenCategoryTag)
                }
                val updatedCustom = customTagsMap.toMutableMap().apply {
                    put(target.obligation.id, chosenCustomLabels)
                }
                saveTagOverrides(updatedCats, updatedCustom)
                taggingDialogTarget = null
            }
        )
    }
}

@Composable
private fun ObligationTagAssignmentModal(
    target: TaggedObligationSummary,
    onDismiss: () -> Unit,
    onSaveTags: (DebtCategoryTag, List<String>) -> Unit
) {
    val ext = LocalTarazinoColors.current
    var selectedCategory by remember(target) { mutableStateOf(target.primaryTag) }
    var customTags by remember(target) { mutableStateOf(target.customLabels) }
    var newTagInput by remember { mutableStateOf("") }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                val dialogView = androidx.compose.ui.platform.LocalView.current
                if (!dialogView.isInEditMode) {
                    androidx.compose.runtime.SideEffect {
                        val dialogWindow = (dialogView.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
                        if (dialogWindow != null && dialogWindow.decorView.layoutDirection != android.view.View.LAYOUT_DIRECTION_RTL) {
                            dialogWindow.decorView.layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
                        }
                    }
                }
                Text(
                    text = "دسته‌بندی و برچسب‌گذاری «${target.obligation.title}»",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )
            },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "انتخاب دسته اصلی بدهی:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val editableTags = listOf(
                    DebtCategoryTag.BANK_LOAN,
                    DebtCategoryTag.CREDIT_CARD,
                    DebtCategoryTag.PERSONAL,
                    DebtCategoryTag.CHECK_AND_HOUSING,
                    DebtCategoryTag.RECEIVABLE
                )

                editableTags.forEach { catTag ->
                    val isSel = selectedCategory == catTag
                    val col = Color(catTag.colorHex)
                    Surface(
                        shape = RoundedCornerShape(11.dp),
                        color = if (isSel) col.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, if (isSel) col else ext.cardBorderTop),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(11.dp))
                            .clickable { selectedCategory = catTag }
                            .testTag("modal_select_cat_${catTag.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = debtTagIcon(catTag),
                                    contentDescription = null,
                                    tint = col,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = catTag.labelFa,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSel) col else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (isSel) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = col,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text(
                    text = "برچسب‌های سفارشی (لمس برای افزودن/حذف):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DebtTaggingAndCategorizationEngine.PRESET_CUSTOM_TAGS_FA.forEach { preset ->
                        val active = customTags.any { it.equals(preset, ignoreCase = true) }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(0.8.dp, if (active) MaterialTheme.colorScheme.primary else ext.cardBorderTop),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable {
                                    customTags = if (active) {
                                        customTags.filterNot { it.equals(preset, ignoreCase = true) }
                                    } else {
                                        (customTags + preset).distinct()
                                    }
                                }
                        ) {
                            Text(
                                text = "#$preset",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newTagInput,
                        onValueChange = { newTagInput = it },
                        label = { Text("برچسب جدید...") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_tag_text_input")
                    )
                    Button(
                        onClick = {
                            val clean = newTagInput.trim().removePrefix("#")
                            if (clean.isNotBlank()) {
                                customTags = (customTags + clean).distinct()
                                newTagInput = ""
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "افزودن برچسب")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveTags(selectedCategory, customTags) },
                modifier = Modifier.testTag("save_obligation_tags_btn")
            ) {
                Text("ذخیره دسته‌بندی و برچسب")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
    }
}

/**
 * Detailed Category & Tag Spending Report Studio for the Reports & Settings screen.
 * Allows users to inspect detailed spending & debt metrics per category ('Bank Loan',
 * 'Credit Card', 'Personal', 'Check & Housing', 'Receivable'), filter obligations by tag,
 * and preview/export an A4 PDF or RTL Excel report scoped specifically to the chosen category tag.
 */
@Composable
fun DetailedCategorySpendingReportSection(
    obligations: List<ObligationEntity>,
    installments: List<InstallmentEntity>,
    counterparties: List<CounterpartyEntity>,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    onOpenCategoryFilteredA4Report: (DebtCategoryTag, List<Long>) -> Unit,
    onExportCategoryFilteredPdf: (DebtCategoryTag, List<Long>) -> Unit,
    onExportCategoryFilteredExcel: (DebtCategoryTag, List<Long>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val ext = LocalTarazinoColors.current
    val prefs = remember(context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    val persistedOverrides = remember(obligations) {
        DebtTaggingAndCategorizationEngine.deserializeTagOverrides(
            prefs.getString(KEY_DEBT_TAG_OVERRIDES_V1, null)
        )
    }

    var selectedReportTag by remember { mutableStateOf(DebtCategoryTag.ALL) }

    val report = remember(
        obligations,
        installments,
        counterparties,
        selectedReportTag,
        persistedOverrides
    ) {
        DebtTaggingAndCategorizationEngine.evaluateCategorizedPortfolio(
            obligations = obligations,
            installments = installments,
            counterparties = counterparties,
            selectedFilterTag = selectedReportTag,
            categoryOverrides = persistedOverrides.first,
            customTagsMap = persistedOverrides.second
        )
    }

    val activeAccent = Color(selectedReportTag.colorHex)
    val filteredObligationIds = remember(report.filteredObligations) {
        report.filteredObligations.map { it.obligation.id }
    }

    TarazinoGlassCard(
        cornerRadius = 24.dp,
        accentBorderColor = activeAccent,
        modifier = modifier
            .fillMaxWidth()
            .testTag("detailed_category_spending_report_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TarazinoSectionHeader(
                title = "گزارش تفصیلی هزینه‌ها و بدهی به تفکیک دسته و برچسب",
                subtitle = "تحلیل سهم وام بانکی، کارت اعتباری، قرض شخصی و خروجی A4 فیلترشده",
                icon = Icons.Default.PieChart,
                accentColor = activeAccent
            )

            // 1. Category Filter Selector Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DebtCategoryTag.FILTERABLE_TAGS.forEach { tag ->
                    val isSelected = selectedReportTag == tag
                    val tagColor = Color(tag.colorHex)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            tagColor.copy(alpha = 0.20f)
                        } else {
                            ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.65f else 0.85f)
                        },
                        border = BorderStroke(
                            if (isSelected) 1.3.dp else 0.8.dp,
                            if (isSelected) tagColor else ext.cardBorderTop
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedReportTag = tag }
                            .testTag("report_category_filter_chip_${tag.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = debtTagIcon(tag),
                                contentDescription = tag.labelFa,
                                tint = if (isSelected) tagColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = tag.labelFa,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) tagColor else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 2. Category Breakdown Comparison Table (Bank Loan vs. Credit Card vs. Personal vs. Check & Housing)
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                report.categoryBreakdowns
                    .filter { it.obligationsCount > 0 || it.tag == selectedReportTag }
                    .forEach { breakdown ->
                        CategorySpendingBreakdownRow(
                            breakdown = breakdown,
                            isFocused = selectedReportTag == breakdown.tag,
                            currencyUnit = currencyUnit,
                            privacyMasked = privacyMasked,
                            onClick = {
                                selectedReportTag = if (selectedReportTag == breakdown.tag) {
                                    DebtCategoryTag.ALL
                                } else {
                                    breakdown.tag
                                }
                            }
                        )
                    }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))

            // 3. Selected Category Detailed Summary Box & Filtered A4/Excel Export Actions
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = activeAccent.copy(alpha = if (ext.isDark) 0.14f else 0.08f),
                border = BorderStroke(1.dp, activeAccent.copy(alpha = 0.42f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(13.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "خلاصه گزارش دسته: ${selectedReportTag.labelFa}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = activeAccent
                            )
                            Text(
                                text = "${PersianCurrencyEngine.toPersianDigits(report.filteredObligations.size.toString())} پرونده • ${
                                    PersianCurrencyEngine.toPersianDigits(report.filteredInstallments.size.toString())
                                } ردیف قسط",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TarazinoStatusPill(
                            text = "${PersianCurrencyEngine.toPersianDigits(report.filteredSettlementProgressPercent.toString())}٪ تسویه",
                            accentColor = ext.positive
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "کل تعهدات دسته",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FormattedMoneyText(
                                amountToman = report.filteredTotalPayableToman,
                                unit = currencyUnit,
                                privacyMasked = privacyMasked,
                                style = MaterialTheme.typography.labelLarge,
                                numberColor = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "پرداخت‌شده",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FormattedMoneyText(
                                amountToman = report.filteredTotalPaidToman,
                                unit = currencyUnit,
                                privacyMasked = privacyMasked,
                                style = MaterialTheme.typography.labelLarge,
                                numberColor = ext.positive
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "مانده بدهی دسته",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FormattedMoneyText(
                                amountToman = report.filteredTotalRemainingToman,
                                unit = currencyUnit,
                                privacyMasked = privacyMasked,
                                style = MaterialTheme.typography.labelLarge,
                                numberColor = activeAccent,
                                textAlign = TextAlign.End
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onOpenCategoryFilteredA4Report(selectedReportTag, filteredObligationIds)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = activeAccent),
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("open_category_filtered_a4_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            TarazinoAutoSizeText(
                                text = "پیش‌نمایش A4 این دسته",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                minFontSize = 8.5.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                onExportCategoryFilteredExcel(selectedReportTag, filteredObligationIds)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_category_filtered_excel_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            TarazinoAutoSizeText(
                                text = "اکسل دسته",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                minFontSize = 8.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySpendingBreakdownRow(
    breakdown: CategoryTagSpendingBreakdown,
    isFocused: Boolean,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    onClick: () -> Unit
) {
    val ext = LocalTarazinoColors.current
    val tagColor = Color(breakdown.tag.colorHex)
    val shareRatio = (breakdown.shareOfTotalRemainingPercent / 100f).coerceIn(0f, 1f)

    Surface(
        shape = RoundedCornerShape(15.dp),
        color = if (isFocused) {
            tagColor.copy(alpha = if (ext.isDark) 0.18f else 0.11f)
        } else {
            ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.68f else 0.88f)
        },
        border = BorderStroke(
            if (isFocused) 1.3.dp else 0.85.dp,
            if (isFocused) tagColor else ext.cardBorderTop
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .clickable(onClick = onClick)
            .testTag("category_spending_breakdown_row_${breakdown.tag.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(tagColor.copy(alpha = 0.18f))
                    ) {
                        Icon(
                            imageVector = debtTagIcon(breakdown.tag),
                            contentDescription = null,
                            tint = tagColor,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(9.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TarazinoAutoSizeText(
                                text = breakdown.tag.labelFa,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                minFontSize = 9.5.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = tagColor.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "${PersianCurrencyEngine.toPersianDigits(breakdown.obligationsCount.toString())} پرونده",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = tagColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                        TarazinoAutoSizeText(
                            text = breakdown.sampleCounterpartyNamesFa,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            minFontSize = 8.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    FormattedMoneyText(
                        amountToman = breakdown.remainingToman,
                        unit = currencyUnit,
                        privacyMasked = privacyMasked,
                        style = MaterialTheme.typography.labelLarge,
                        numberColor = tagColor,
                        minFontSize = 9.sp,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = if (breakdown.tag == DebtCategoryTag.RECEIVABLE) {
                            "پیشرفت دریافت: ${PersianCurrencyEngine.toPersianDigits(breakdown.settlementProgressPercent.toString())}٪"
                        } else {
                            "سهم از بدهی: ${PersianCurrencyEngine.toPersianDigits(breakdown.shareOfTotalRemainingPercent.toString())}٪ • قسط ماه: ${
                                PersianCurrencyEngine.toPersianDigits(breakdown.shareOfMonthlySpendingPercent.toString())
                            }٪"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            TarazinoLuminousProgressBar(
                progress = if (breakdown.tag == DebtCategoryTag.RECEIVABLE) {
                    (breakdown.settlementProgressPercent / 100f).coerceIn(0f, 1f)
                } else {
                    shareRatio.coerceAtLeast(0.05f)
                },
                color = tagColor,
                height = 6.dp
            )
        }
    }
}
