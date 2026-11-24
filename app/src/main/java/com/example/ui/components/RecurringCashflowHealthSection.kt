package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.CurrencyUnit
import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.domain.RecurringCashflowCategory
import com.example.domain.RecurringCashflowHealthEngine
import com.example.domain.RecurringCashflowItem
import com.example.domain.RecurringCashflowType
import com.example.ui.theme.LocalTarazinoColors
import kotlin.math.abs

private const val PREFS_NAME = "tarazino_prefs_v1"
private const val KEY_RECURRING_CASHFLOW_ITEMS_V1 = "recurring_non_loan_cashflow_items_v1"

@Composable
fun RecurringCashflowHealthSection(
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    currentMonthLoanInstallmentsToman: Long,
    monthlySavingsTargetToman: Long,
    onSyncWithBudgetPlanner: (totalIncomeToman: Long, livingExpensePercent: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val ext = LocalTarazinoColors.current
    val prefs = remember(context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var items by remember {
        mutableStateOf(
            RecurringCashflowHealthEngine.deserializeItems(
                prefs.getString(KEY_RECURRING_CASHFLOW_ITEMS_V1, null)
            )
        )
    }

    fun persistItems(updated: List<RecurringCashflowItem>) {
        items = updated
        prefs.edit()
            .putString(
                KEY_RECURRING_CASHFLOW_ITEMS_V1,
                RecurringCashflowHealthEngine.serializeItems(updated)
            )
            .apply()
    }

    val todayJalali = remember { JalaliCalendarEngine.todayJalali() }
    val summary = remember(
        items,
        currentMonthLoanInstallmentsToman,
        monthlySavingsTargetToman,
        todayJalali
    ) {
        RecurringCashflowHealthEngine.evaluateHolisticHealth(
            items = items,
            currentMonthLoanInstallmentsToman = currentMonthLoanInstallmentsToman,
            monthlySavingsTargetToman = monthlySavingsTargetToman,
            todayJalali = todayJalali
        )
    }

    // 0 = All, 1 = Income Only, 2 = Non-Loan Expenses Only
    var filterTabIndex by remember { mutableIntStateOf(0) }
    var showAddForm by remember { mutableStateOf(false) }

    // New Item Form State
    var newType by remember { mutableStateOf(RecurringCashflowType.EXPENSE) }
    var newCategory by remember { mutableStateOf(RecurringCashflowCategory.GROCERIES_LIVING) }
    var newTitle by remember { mutableStateOf("") }
    var newAmountText by remember { mutableStateOf("4500000") }
    var newDayText by remember { mutableStateOf("5") }
    var newIsEssential by remember { mutableStateOf(true) }

    val filteredEnrichedItems = remember(summary.enrichedItems, filterTabIndex) {
        when (filterTabIndex) {
            1 -> summary.enrichedItems.filter { it.item.type == RecurringCashflowType.INCOME }
            2 -> summary.enrichedItems.filter { it.item.type == RecurringCashflowType.EXPENSE }
            else -> summary.enrichedItems
        }
    }

    val statusColor = Color(summary.healthStatusColorHex)
    val totalIncomeSafe = summary.totalRecurringIncomeToman.coerceAtLeast(1L).toFloat()

    val nonLoanFrac by animateFloatAsState(
        targetValue = (summary.totalRecurringNonLoanExpenseToman / totalIncomeSafe).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "NonLoanBarFrac"
    )
    val loanFrac by animateFloatAsState(
        targetValue = (summary.currentMonthLoanInstallmentsToman / totalIncomeSafe)
            .coerceIn(0f, (1f - nonLoanFrac).coerceAtLeast(0f)),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "LoanBarFrac"
    )
    val freeFrac = (1f - nonLoanFrac - loanFrac).coerceIn(0f, 1f)

    TarazinoGlassCard(
        cornerRadius = 24.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("recurring_cashflow_health_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            // 1. Header with Holistic Health Score Badge
            TarazinoSectionHeader(
                title = "درآمدها و هزینه‌های دوره‌ای غیرقسطی (سلامت جامع)",
                subtitle = "تراز کامل جریان نقدی ${summary.shamsiMonthNameFa} ${
                    PersianCurrencyEngine.toPersianDigits(summary.shamsiYear.toString())
                } در کنار اقساط",
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = MaterialTheme.colorScheme.primary,
                trailing = {
                    TarazinoStatusPill(
                        text = "امتیاز ${PersianCurrencyEngine.toPersianDigits(summary.holisticHealthScore.toString())}/۱۰۰",
                        accentColor = statusColor
                    )
                }
            )

            // 2. Holistic Cashflow Health Banner & 3-Pillar Allocation Bar
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.70f else 0.92f),
                border = BorderStroke(0.95.dp, statusColor.copy(alpha = 0.42f)),
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
                                text = "جریان نقدی آزاد پس از هزینه‌های جاری و اقساط",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (summary.netFreeCashflowAfterLoansToman < 0L) {
                                    Text(
                                        text = "کسری:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ext.danger
                                    )
                                }
                                FormattedMoneyText(
                                    amountToman = abs(summary.netFreeCashflowAfterLoansToman),
                                    unit = currencyUnit,
                                    privacyMasked = privacyMasked,
                                    style = MaterialTheme.typography.titleMedium,
                                    numberColor = if (summary.netFreeCashflowAfterLoansToman >= 0L) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        ext.danger
                                    },
                                    minFontSize = 11.sp
                                )
                            }
                        }

                        TarazinoStatusPill(
                            text = summary.healthStatusBadgeFa,
                            accentColor = statusColor
                        )
                    }

                    // 3-Pillar Stacked Allocation Bar (Non-Loan Expenses | Loan Installments | Free Cashflow)
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(11.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (nonLoanFrac > 0.005f) {
                                Box(
                                    modifier = Modifier
                                        .weight(nonLoanFrac.coerceAtLeast(0.01f))
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.secondary)
                                )
                            }
                            if (loanFrac > 0.005f) {
                                Box(
                                    modifier = Modifier
                                        .weight(loanFrac.coerceAtLeast(0.01f))
                                        .fillMaxHeight()
                                        .background(ext.info)
                                )
                            }
                            if (freeFrac > 0.005f) {
                                Box(
                                    modifier = Modifier
                                        .weight(freeFrac.coerceAtLeast(0.01f))
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "هزینه غیرقسطی: ${PersianCurrencyEngine.toPersianDigits(summary.nonLoanExpenseToIncomePercent.toString())}٪",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "اقساط وام: ${PersianCurrencyEngine.toPersianDigits(summary.loanDebtToIncomePercent.toString())}٪",
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.info,
                                fontWeight = FontWeight.Bold
                            )
                            val freePct = (100 - summary.totalCommitmentsToIncomePercent).coerceAtLeast(0)
                            Text(
                                text = "آزاد/پس‌انداز: ${PersianCurrencyEngine.toPersianDigits(freePct.toString())}٪",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Twin KPI Row: Total Recurring Income vs. Total Non-Loan Expenses
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(13.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = if (ext.isDark) 0.65f else 0.90f),
                            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("holistic_total_income_tile")
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                Text(
                                    text = "جمع درآمد دوره‌ای",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                FormattedMoneyText(
                                    amountToman = summary.totalRecurringIncomeToman,
                                    unit = currencyUnit,
                                    privacyMasked = privacyMasked,
                                    style = MaterialTheme.typography.labelLarge,
                                    numberColor = MaterialTheme.colorScheme.primary,
                                    minFontSize = 9.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(13.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = if (ext.isDark) 0.65f else 0.90f),
                            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.38f)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("holistic_total_non_loan_expense_tile")
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                Text(
                                    text = "جمع هزینه‌های غیرقسطی",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                FormattedMoneyText(
                                    amountToman = summary.totalRecurringNonLoanExpenseToman,
                                    unit = currencyUnit,
                                    privacyMasked = privacyMasked,
                                    style = MaterialTheme.typography.labelLarge,
                                    numberColor = MaterialTheme.colorScheme.secondary,
                                    minFontSize = 9.sp
                                )
                            }
                        }
                    }

                    // Essential vs. Discretionary Expense Breakdown + Sync with Global Budget Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            TarazinoAutoSizeText(
                                text = "ضروری: ${
                                    PersianCurrencyEngine.formatAmount(
                                        summary.essentialNonLoanExpenseToman,
                                        currencyUnit,
                                        includeUnit = true,
                                        privacyMasked = privacyMasked
                                    )
                                } • اختیاری: ${
                                    PersianCurrencyEngine.formatAmount(
                                        summary.discretionaryNonLoanExpenseToman,
                                        currencyUnit,
                                        includeUnit = true,
                                        privacyMasked = privacyMasked
                                    )
                                }",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                minFontSize = 8.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    if (summary.totalRecurringIncomeToman > 0L) {
                                        onSyncWithBudgetPlanner(
                                            summary.totalRecurringIncomeToman,
                                            summary.nonLoanExpenseToIncomePercent.coerceIn(20, 80)
                                        )
                                    }
                                }
                                .testTag("sync_recurring_with_budget_planner_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "اعمال در بودجه کل",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // 3. Filter Segment Row + Add New Recurring Item Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val tabs = listOf(
                        0 to "همه (${PersianCurrencyEngine.toPersianDigits(summary.enrichedItems.size.toString())})",
                        1 to "درآمدها",
                        2 to "هزینه‌های جاری"
                    )
                    tabs.forEach { (idx, titleFa) ->
                        val selected = filterTabIndex == idx
                        Surface(
                            shape = RoundedCornerShape(11.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(11.dp))
                                .clickable { filterTabIndex = idx }
                                .testTag("recurring_filter_tab_$idx")
                        ) {
                            Text(
                                text = titleFa,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .clickable { showAddForm = !showAddForm }
                        .testTag("toggle_add_recurring_item_form_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (showAddForm) "بستن فرم" else "ثبت ردیف جدید",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // 4. Expandable Add New Recurring Income / Non-Loan Expense Form
            AnimatedVisibility(
                visible = showAddForm,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = if (ext.isDark) 0.85f else 0.96f),
                    border = BorderStroke(0.9.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_recurring_cashflow_form_panel")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "افزودن درآمد یا هزینه دوره‌ای غیرقسطی ماهانه",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Type Toggle (Income vs Non-Loan Expense)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RecurringCashflowType.entries.forEach { t ->
                                val isSel = newType == t
                                val tColor = Color(t.accentColorHex)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSel) tColor.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = BorderStroke(
                                        width = if (isSel) 1.3.dp else 0.75.dp,
                                        color = if (isSel) tColor else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            newType = t
                                            val firstCat = RecurringCashflowCategory.entries.first { it.type == t }
                                            newCategory = firstCat
                                            newIsEssential = firstCat.defaultIsEssential
                                        }
                                        .testTag("new_recurring_type_${t.name}")
                                ) {
                                    Text(
                                        text = t.labelFa,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) tColor else MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                                    )
                                }
                            }
                        }

                        // Category Chips for Selected Type
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            RecurringCashflowCategory.entries
                                .filter { it.type == newType }
                                .forEach { cat ->
                                    val isSel = newCategory == cat
                                    val cColor = Color(cat.colorHex)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSel) cColor.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                                        border = BorderStroke(
                                            width = if (isSel) 1.2.dp else 0.7.dp,
                                            color = if (isSel) cColor else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                newCategory = cat
                                                newIsEssential = cat.defaultIsEssential
                                            }
                                            .testTag("new_recurring_cat_${cat.name}")
                                    ) {
                                        Text(
                                            text = cat.labelFa,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) cColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                        }

                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = {
                                Text(
                                    text = if (newType == RecurringCashflowType.INCOME) {
                                        "عنوان درآمد ماهانه (مثلاً حقوق شرکت، اجاره واحد)"
                                    } else {
                                        "عنوان هزینه غیرقسطی (مثلاً قبوض، خوراک، بیمه)"
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new_recurring_title_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = PersianCurrencyEngine.toPersianDigits(newAmountText),
                                onValueChange = { raw ->
                                    newAmountText = PersianCurrencyEngine.toEnglishDigits(raw)
                                        .filter { it.isDigit() }
                                        .take(12)
                                },
                                label = {
                                    Text("مبلغ ماهانه (تومان)", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.4f)
                                    .testTag("new_recurring_amount_input")
                            )

                            OutlinedTextField(
                                value = PersianCurrencyEngine.toPersianDigits(newDayText),
                                onValueChange = { raw ->
                                    newDayText = PersianCurrencyEngine.toEnglishDigits(raw)
                                        .filter { it.isDigit() }
                                        .take(2)
                                },
                                label = {
                                    Text("روز ماه شمسی", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .testTag("new_recurring_day_input")
                            )
                        }

                        if (newType == RecurringCashflowType.EXPENSE) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "هزینه ضروری و حیاتی زندگی (غیرقابل حذف)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Switch(
                                    checked = newIsEssential,
                                    onCheckedChange = { newIsEssential = it }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val amt = (newAmountText.toLongOrNull() ?: 0L).coerceAtLeast(100_000L)
                                    val day = (newDayText.toIntOrNull() ?: 1).coerceIn(1, 31)
                                    val nextId = (items.maxOfOrNull { it.id } ?: 0L) + 1L
                                    val created = RecurringCashflowItem(
                                        id = nextId,
                                        titleFa = newTitle.trim().ifBlank { newCategory.labelFa },
                                        category = newCategory,
                                        monthlyAmountToman = amt,
                                        dayOfShamsiMonth = day,
                                        isEssential = if (newType == RecurringCashflowType.INCOME) true else newIsEssential,
                                        isActive = true
                                    )
                                    persistItems(listOf(created) + items)
                                    newTitle = ""
                                    showAddForm = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                                modifier = Modifier
                                    .weight(1.4f)
                                    .testTag("confirm_save_recurring_item_btn")
                            ) {
                                Text(
                                    text = "ذخیره در جریان نقدی ماهانه",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }

                            OutlinedButton(
                                onClick = { showAddForm = false },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp),
                                modifier = Modifier.weight(0.8f)
                            ) {
                                Text("انصراف", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // 5. List of Recurring Monthly Incomes & Non-Loan Expenses
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredEnrichedItems.forEach { enriched ->
                    val item = enriched.item
                    val itemColor = Color(item.category.colorHex)
                    val isIncome = item.type == RecurringCashflowType.INCOME

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ext.elevatedSurface.copy(
                            alpha = if (!item.isActive) 0.35f else if (ext.isDark) 0.58f else 0.84f
                        ),
                        border = BorderStroke(
                            width = 0.8.dp,
                            color = if (item.isActive) itemColor.copy(alpha = 0.38f) else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recurring_cashflow_row_${item.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 11.dp, vertical = 9.dp),
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
                                        .clip(CircleShape)
                                        .background(itemColor.copy(alpha = 0.18f))
                                ) {
                                    Text(
                                        text = if (isIncome) "+" else "−",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = itemColor
                                    )
                                }
                                Spacer(modifier = Modifier.width(9.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    TarazinoAutoSizeText(
                                        text = item.titleFa,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        minFontSize = 9.5.sp
                                    )
                                    val dayBadge = "روز ${PersianCurrencyEngine.toPersianDigits(item.dayOfShamsiMonth.toString())} هر ماه"
                                    val nextInfo = if (enriched.daysUntilNextOccurrence == 0) {
                                        "سررسید امروز"
                                    } else {
                                        "${PersianCurrencyEngine.toPersianDigits(enriched.daysUntilNextOccurrence.toString())} روز دیگر"
                                    }
                                    val kindTag = if (isIncome) {
                                        "درآمد ثابت"
                                    } else if (item.isEssential) {
                                        "هزینه ضروری"
                                    } else {
                                        "هزینه اختیاری"
                                    }
                                    TarazinoAutoSizeText(
                                        text = "$kindTag • $dayBadge ($nextInfo)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        minFontSize = 8.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    FormattedMoneyText(
                                        amountToman = item.monthlyAmountToman,
                                        unit = currencyUnit,
                                        privacyMasked = privacyMasked,
                                        style = MaterialTheme.typography.labelLarge,
                                        numberColor = itemColor,
                                        minFontSize = 9.sp
                                    )
                                    Text(
                                        text = "${PersianCurrencyEngine.toPersianDigits(enriched.shareOfIncomePercent.toString())}٪ از درآمد",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 9.sp
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        persistItems(
                                            items.map { existing ->
                                                if (existing.id == item.id) {
                                                    existing.copy(isActive = !existing.isActive)
                                                } else {
                                                    existing
                                                }
                                            }
                                        )
                                    },
                                    modifier = Modifier
                                        .size(26.dp)
                                        .testTag("toggle_recurring_active_btn_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "فعال/غیرفعال کردن ردیف",
                                        tint = if (item.isActive) ext.positive else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        persistItems(items.filterNot { it.id == item.id })
                                    },
                                    modifier = Modifier
                                        .size(26.dp)
                                        .testTag("delete_recurring_item_btn_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "حذف ردیف",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (items.isEmpty()) {
                OutlinedButton(
                    onClick = {
                        persistItems(RecurringCashflowHealthEngine.defaultSampleRecurringItems())
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("بارگذاری نمونه درآمدها و هزینه‌های دوره‌ای ماهانه")
                }
            }
        }
    }
}
