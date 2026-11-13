package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.data.CounterpartyEntity
import com.example.data.EntityCategory
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.LedgerSubRecordEntity
import com.example.data.ObligationCategory
import com.example.data.ObligationEntity
import com.example.data.SubRecordType
import com.example.data.TrustPriority
import com.example.ui.TarazinoUiState
import com.example.ui.components.JalaliDateSelectorField
import com.example.ui.components.MoneyInputField
import com.example.ui.components.tourSpotlightTarget

enum class InstallmentSortOption(val labelFa: String) {
    BY_DUE_DATE("نزدیک‌ترین سررسید"),
    HIGHEST_AMOUNT("بیشترین مبلغ"),
    LOWEST_REMAINING("کمترین مانده")
}

enum class InstallmentFilterMode(val labelFa: String) {
    ALL("همه اقساط"),
    UNPAID_ONLY("پرداخت‌نشده"),
    OVERDUE_ONLY("فقط معوقه"),
    PAID_ONLY("تسویه‌شده")
}

@Composable
fun TreePortfolioScreen(
    uiState: TarazinoUiState,
    onSelectCategory: (EntityCategory?) -> Unit,
    onSelectCounterparty: (CounterpartyEntity) -> Unit,
    onSelectObligation: (ObligationEntity, EntityCategory?) -> Unit,
    onStepBackInTree: () -> Unit,
    onResetTreeToRoot: () -> Unit,
    onOpenWizard: (EntityCategory?, Long?) -> Unit,
    onSaveCounterparty: (CounterpartyEntity) -> Unit,
    onDeleteCounterparty: (Long, String) -> Unit,
    onUpdateObligationDetails: (ObligationEntity) -> Unit,
    onDeleteObligation: (Long, String) -> Unit,
    onQuickPayInstallment: (InstallmentEntity) -> Unit,
    onBatchPayInstallments: (Set<Long>) -> Unit,
    onDetailedInstallmentUpdate: (
        installment: InstallmentEntity,
        customTitle: String,
        customBaseAmountToman: Long,
        customDueDateJalali: String,
        paidAmountToman: Long,
        lateFeeToman: Long,
        discountToman: Long,
        paidDateJalali: String,
        trackingCode: String,
        note: String
    ) -> Unit,
    onAddSingleCustomInstallment: (
        obligationId: Long,
        nextNumber: Int,
        title: String,
        dueDateJalali: String,
        amountToman: Long,
        isAlreadyPaid: Boolean,
        note: String
    ) -> Unit,
    onDeleteSingleInstallment: (Long, String) -> Unit,
    onAddSubRecord: (ObligationEntity, LedgerSubRecordEntity) -> Unit,
    onOpenCounterpartyA4Report: (CounterpartyEntity) -> Unit,
    onOpenObligationA4Report: (ObligationEntity) -> Unit,
    onShowToast: (String) -> Unit,
    tourScrollIndex: Int? = null
) {
    val context = LocalContext.current
    val unit = uiState.preferences.currencyUnit
    val masked = uiState.preferences.privacyMaskEnabled
    val drill = uiState.treeDrill
    val listState = rememberLazyListState()

    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(InstallmentSortOption.BY_DUE_DATE) }
    var filterMode by remember { mutableStateOf(InstallmentFilterMode.ALL) }
    var batchSelectionIds by remember(drill.selectedObligationId) { mutableStateOf(setOf<Long>()) }

    var editingCounterparty by remember { mutableStateOf<CounterpartyEntity?>(null) }
    var showAddCounterpartyModalForCategory by remember { mutableStateOf<EntityCategory?>(null) }
    var editingObligation by remember { mutableStateOf<ObligationEntity?>(null) }
    var editingInstallment by remember { mutableStateOf<InstallmentEntity?>(null) }
    var addingCustomInstallmentToObligation by remember { mutableStateOf<ObligationEntity?>(null) }
    var addingSubRecordToObligation by remember { mutableStateOf<ObligationEntity?>(null) }

    LaunchedEffect(tourScrollIndex) {
        if (tourScrollIndex != null && tourScrollIndex >= 0) {
            listState.animateScrollToItem(tourScrollIndex)
        }
    }

    val selectedCounterparty = remember(drill.selectedCounterpartyId, uiState.counterparties) {
        uiState.counterparties.find { it.id == drill.selectedCounterpartyId }
    }
    val selectedObligation = remember(drill.selectedObligationId, uiState.obligations) {
        uiState.obligations.find { it.id == drill.selectedObligationId }
    }

    val orderedCategories = remember {
        listOf(
            EntityCategory.BNPL_PLATFORM,
            EntityCategory.BANK,
            EntityCategory.INSTITUTION,
            EntityCategory.PERSON
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("tree_portfolio_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Top Command, Search & Smart Sort/Filter Bar
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .tourSpotlightTarget("tree_command_bar")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "پوشه درختی اقساط و طرف‌حساب‌ها",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "دسته اصلی ← زیرمجموعه (اسنپ‌پی، دیجی‌پی، بانک) ← جدول قسط‌ها",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onOpenWizard(
                                    drill.selectedCategory ?: selectedCounterparty?.categoryEnum,
                                    drill.selectedCounterpartyId
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("tree_open_wizard_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ثبت قسط جدید",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "جستجو (اسنپ‌پی، دیجی‌پی، لپ‌تاپ، وام ازدواج...)",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "پاک کردن جستجو")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tree_search_input")
                    )

                    // Interactive Breadcrumb Bar
                    if (drill.selectedCategory != null || drill.selectedCounterpartyId != null || drill.selectedObligationId != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            IconButton(
                                onClick = onStepBackInTree,
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("tree_step_back_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "بازگشت به سطح قبل",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            BreadcrumbPill(
                                text = "همه دسته‌ها",
                                isActive = false,
                                onClick = onResetTreeToRoot
                            )

                            drill.selectedCategory?.let { cat ->
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                BreadcrumbPill(
                                    text = cat.badgeFa,
                                    isActive = drill.selectedCounterpartyId == null && drill.selectedObligationId == null,
                                    onClick = { onSelectCategory(cat) }
                                )
                            }

                            selectedCounterparty?.let { cp ->
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                BreadcrumbPill(
                                    text = cp.name,
                                    isActive = drill.selectedObligationId == null,
                                    onClick = { onSelectCounterparty(cp) }
                                )
                            }

                            selectedObligation?.let { ob ->
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                BreadcrumbPill(
                                    text = ob.title,
                                    isActive = true,
                                    onClick = {}
                                )
                            }
                        }
                    }
                }
            }
        }

        // If user is searching globally, show clean categorized search results
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim()
            val matchedCounterparties = uiState.counterparties.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.description.contains(q, ignoreCase = true) ||
                    it.categoryEnum.labelFa.contains(q, ignoreCase = true)
            }
            val matchedObligations = uiState.obligations.filter { ob ->
                val cpName = uiState.counterparties.find { it.id == ob.counterpartyId }?.name.orEmpty()
                ob.title.contains(q, ignoreCase = true) ||
                    ob.notes.contains(q, ignoreCase = true) ||
                    ob.trackingOrCheckCode.contains(q, ignoreCase = true) ||
                    cpName.contains(q, ignoreCase = true)
            }

            item {
                Text(
                    text = "نتایج جستجو برای «$q» (${PersianCurrencyEngine.toPersianDigits((matchedCounterparties.size + matchedObligations.size).toString())} مورد)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(matchedCounterparties, key = { "search_cp_${it.id}" }) { cp ->
                SubProviderFolderCard(
                    counterparty = cp,
                    obligations = uiState.obligations.filter { it.counterpartyId == cp.id },
                    installments = uiState.installments,
                    currencyUnit = unit,
                    privacyMasked = masked,
                    onClickProvider = {
                        searchQuery = ""
                        onSelectCounterparty(cp)
                    },
                    onAddPurchaseHere = {
                        searchQuery = ""
                        onOpenWizard(cp.categoryEnum, cp.id)
                    },
                    onOpenA4Report = { onOpenCounterpartyA4Report(cp) }
                )
            }

            items(matchedObligations, key = { "search_ob_${it.id}" }) { ob ->
                val cp = uiState.counterparties.find { it.id == ob.counterpartyId }
                val obInst = uiState.installments.filter { it.obligationId == ob.id }
                ObligationSummaryRowCard(
                    obligation = ob,
                    counterparty = cp,
                    installments = obInst,
                    currencyUnit = unit,
                    privacyMasked = masked,
                    onOpenDetail = {
                        searchQuery = ""
                        onSelectObligation(ob, cp?.categoryEnum)
                    },
                    onEditObligation = { editingObligation = ob },
                    onOpenA4 = { onOpenObligationA4Report(ob) }
                )
            }
        } else {
            // Normal Hierarchical Tree Explorer
            when {
                // LEVEL 4: Specific Obligation Detail, Batch Pay & Granular Per-Installment Editable Table
                selectedObligation != null -> {
                    val ob = selectedObligation
                    val cp = selectedCounterparty ?: uiState.counterparties.find { it.id == ob.counterpartyId }
                    val rawInstallments = uiState.installments.filter { it.obligationId == ob.id }
                    val unpaidIds = rawInstallments.filter { it.statusEnum != InstallmentStatus.PAID }.map { it.id }.toSet()

                    val filteredAndSortedInstallments = rawInstallments
                        .filter { inst ->
                            when (filterMode) {
                                InstallmentFilterMode.ALL -> true
                                InstallmentFilterMode.UNPAID_ONLY -> inst.statusEnum != InstallmentStatus.PAID
                                InstallmentFilterMode.OVERDUE_ONLY -> inst.statusEnum == InstallmentStatus.OVERDUE
                                InstallmentFilterMode.PAID_ONLY -> inst.statusEnum == InstallmentStatus.PAID
                            }
                        }
                        .let { list ->
                            when (sortOption) {
                                InstallmentSortOption.BY_DUE_DATE -> list.sortedBy { it.dueDateJalali }
                                InstallmentSortOption.HIGHEST_AMOUNT -> list.sortedByDescending { it.effectiveTotalToman }
                                InstallmentSortOption.LOWEST_REMAINING -> list.sortedBy { it.remainingToman }
                            }
                        }

                    val obSubRecords = uiState.subRecords.filter { it.obligationId == ob.id }

                    item {
                        ObligationGranularHeaderCard(
                            obligation = ob,
                            counterparty = cp,
                            installments = rawInstallments,
                            currencyUnit = unit,
                            privacyMasked = masked,
                            onEditObligation = { editingObligation = ob },
                            onAddCustomInstallment = { addingCustomInstallmentToObligation = ob },
                            onAddSubRecord = { addingSubRecordToObligation = ob },
                            onExportA4 = { onOpenObligationA4Report(ob) },
                            onDeleteObligation = { onDeleteObligation(ob.id, ob.title) }
                        )
                    }

                    // Batch Multi-Installment Settlement Banner (تسویه گروهی چند قسط با هم)
                    if (unpaidIds.isNotEmpty()) {
                        item {
                            val selectedTotalToman = rawInstallments
                                .filter { it.id in batchSelectionIds && it.statusEnum != InstallmentStatus.PAID }
                                .sumOf { it.remainingToman }

                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (batchSelectionIds.isNotEmpty()) {
                                        Color(0xFF064E3B).copy(alpha = 0.25f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateContentSize()
                                    .border(
                                        1.dp,
                                        if (batchSelectionIds.isNotEmpty()) Color(0xFF10B981) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                        RoundedCornerShape(18.dp)
                                    )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.DoneAll,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = "تسویه گروهی چند قسط با هم (Batch Pay)",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "تیک کنار اقساط را بزنید یا همه را انتخاب و یکجا تسویه کنید",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        TextButton(
                                            onClick = {
                                                batchSelectionIds = if (batchSelectionIds.size == unpaidIds.size) {
                                                    emptySet()
                                                } else {
                                                    unpaidIds
                                                }
                                            },
                                            modifier = Modifier.testTag("batch_select_all_unpaid_btn")
                                        ) {
                                            Text(
                                                text = if (batchSelectionIds.size == unpaidIds.size) "لغو انتخاب" else "انتخاب همه (${PersianCurrencyEngine.toPersianDigits(unpaidIds.size.toString())})",
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    AnimatedVisibility(visible = batchSelectionIds.isNotEmpty()) {
                                        Column(modifier = Modifier.padding(top = 8.dp)) {
                                            HorizontalDivider(color = Color(0xFF10B981).copy(alpha = 0.3f))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "${PersianCurrencyEngine.toPersianDigits(batchSelectionIds.size.toString())} قسط انتخاب‌شده • مجموع: ${
                                                            PersianCurrencyEngine.formatAmount(selectedTotalToman, unit, privacyMasked = masked)
                                                        }",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF10B981)
                                                    )
                                                    if (!masked && selectedTotalToman > 0L) {
                                                        Text(
                                                            text = PersianCurrencyEngine.amountToPersianWords(selectedTotalToman, unit),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                Button(
                                                    onClick = {
                                                        onBatchPayInstallments(batchSelectionIds)
                                                        batchSelectionIds = emptySet()
                                                    },
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                    modifier = Modifier.testTag("confirm_batch_pay_btn")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = Color(0xFF042F2E),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "تسویه یکجا",
                                                        color = Color(0xFF042F2E),
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Running Ledger Sub-Records Section (if any exist)
                    if (obSubRecords.isNotEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "ریزتراکنش‌های دفتری این پرونده (${PersianCurrencyEngine.toPersianDigits(obSubRecords.size.toString())} مورد)",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    obSubRecords.forEach { sr ->
                                        val isCharge = sr.recordTypeEnum == SubRecordType.CHARGE
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = sr.title,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = PersianCurrencyEngine.toPersianDigits(sr.dateJalali),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "${if (isCharge) "+" else "-"} ${
                                                    PersianCurrencyEngine.formatAmount(
                                                        sr.amountToman,
                                                        unit,
                                                        privacyMasked = masked
                                                    )
                                                }",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCharge) Color(0xFFF59E0B) else Color(0xFF10B981)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Smart Sort & Filter Bar for Installments
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "جدول تفکیکی تک‌تک اقساط (${PersianCurrencyEngine.toPersianDigits(filteredAndSortedInstallments.size.toString())} قسط)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = { addingCustomInstallmentToObligation = ob }) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("افزودن قسط جدید", maxLines = 1)
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                InstallmentFilterMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = filterMode == mode,
                                        onClick = { filterMode = mode },
                                        label = { Text(mode.labelFa, maxLines = 1) }
                                    )
                                }
                                InstallmentSortOption.entries.forEach { sOpt ->
                                    FilterChip(
                                        selected = sortOption == sOpt,
                                        onClick = { sortOption = sOpt },
                                        label = { Text(sOpt.labelFa, maxLines = 1) }
                                    )
                                }
                            }
                        }
                    }

                    items(filteredAndSortedInstallments, key = { "inst_${it.id}" }) { inst ->
                        val isSelectedForBatch = inst.id in batchSelectionIds
                        GranularInstallmentItemCard(
                            installment = inst,
                            currencyUnit = unit,
                            privacyMasked = masked,
                            isBatchChecked = isSelectedForBatch,
                            onToggleBatchCheck = {
                                batchSelectionIds = if (isSelectedForBatch) {
                                    batchSelectionIds - inst.id
                                } else {
                                    batchSelectionIds + inst.id
                                }
                            },
                            onQuickPay = { onQuickPayInstallment(inst) },
                            onEditInstallment = { editingInstallment = inst }
                        )
                    }
                }

                // LEVEL 3: Selected Sub-Provider Hub (e.g., اسنپ‌پی or دیجی‌پی or بانک مهر ایران)
                selectedCounterparty != null -> {
                    val cp = selectedCounterparty
                    val cpObligations = uiState.obligations.filter { it.counterpartyId == cp.id }
                    val cpObIds = cpObligations.map { it.id }.toSet()
                    val cpInstallments = uiState.installments.filter { it.obligationId in cpObIds }

                    val sortedCpObligations = cpObligations.let { list ->
                        when (sortOption) {
                            InstallmentSortOption.BY_DUE_DATE -> list.sortedBy { it.firstDueDateJalali }
                            InstallmentSortOption.HIGHEST_AMOUNT -> list.sortedByDescending { it.netPayableToman }
                            InstallmentSortOption.LOWEST_REMAINING -> list.sortedBy { ob ->
                                cpInstallments.filter { it.obligationId == ob.id }.sumOf { it.remainingToman }
                            }
                        }
                    }

                    item {
                        SubProviderDetailHubHeaderCard(
                            counterparty = cp,
                            obligationsCount = cpObligations.size,
                            installments = cpInstallments,
                            currencyUnit = unit,
                            privacyMasked = masked,
                            onCopyText = { label, text ->
                                copyToClipboard(context, label, text)
                                onShowToast("$label در حافظه کپی شد.")
                            },
                            onAddPurchaseForProvider = { onOpenWizard(cp.categoryEnum, cp.id) },
                            onEditProvider = { editingCounterparty = cp },
                            onDeleteProvider = { onDeleteCounterparty(cp.id, cp.name) },
                            onExportA4 = { onOpenCounterpartyA4Report(cp) }
                        )
                    }

                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "لیست خریدها، وام‌ها و پرونده‌های «${cp.name}»",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${PersianCurrencyEngine.toPersianDigits(cpObligations.size.toString())} پرونده",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (cpObligations.size > 1) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = "مرتب‌سازی:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    InstallmentSortOption.entries.forEach { opt ->
                                        FilterChip(
                                            selected = sortOption == opt,
                                            onClick = { sortOption = opt },
                                            label = { Text(opt.labelFa, maxLines = 1) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (sortedCpObligations.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp)
                                ) {
                                    Text(
                                        text = "هنوز هیچ خرید یا وامی برای «${cp.name}» ثبت نشده است.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { onOpenWizard(cp.categoryEnum, cp.id) },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.AddCircle, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("ثبت اولین خرید / قسط برای ${cp.name}", maxLines = 1)
                                    }
                                }
                            }
                        }
                    } else {
                        items(sortedCpObligations, key = { "cp_ob_${it.id}" }) { ob ->
                            val obInst = cpInstallments.filter { it.obligationId == ob.id }
                            ObligationSummaryRowCard(
                                obligation = ob,
                                counterparty = cp,
                                installments = obInst,
                                currencyUnit = unit,
                                privacyMasked = masked,
                                onOpenDetail = { onSelectObligation(ob, cp.categoryEnum) },
                                onEditObligation = { editingObligation = ob },
                                onOpenA4 = { onOpenObligationA4Report(ob) }
                            )
                        }
                    }
                }

                // LEVEL 2: Selected Parent Category (e.g., خرید اقساطی BNPL -> shows اسنپ‌پی, دیجی‌پی, ...)
                drill.selectedCategory != null -> {
                    val cat = drill.selectedCategory
                    val subProviders = uiState.counterparties.filter { it.categoryEnum == cat }

                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(cat.defaultColorHex).copy(alpha = 0.12f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    Color(cat.defaultColorHex).copy(alpha = 0.4f),
                                    RoundedCornerShape(20.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cat.labelFa,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(cat.defaultColorHex)
                                    )
                                    Text(
                                        text = "${PersianCurrencyEngine.toPersianDigits(subProviders.size.toString())} زیرمجموعه / طرف پرداخت در این دسته",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedButton(
                                    onClick = { showAddCounterpartyModalForCategory = cat },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag("add_subprovider_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("زیرمجموعه جدید", maxLines = 1)
                                }
                            }
                        }
                    }

                    items(subProviders, key = { "sub_cp_${it.id}" }) { cp ->
                        SubProviderFolderCard(
                            counterparty = cp,
                            obligations = uiState.obligations.filter { it.counterpartyId == cp.id },
                            installments = uiState.installments,
                            currencyUnit = unit,
                            privacyMasked = masked,
                            onClickProvider = { onSelectCounterparty(cp) },
                            onAddPurchaseHere = { onOpenWizard(cat, cp.id) },
                            onOpenA4Report = { onOpenCounterpartyA4Report(cp) }
                        )
                    }
                }

                // LEVEL 1 (ROOT): 4 Clean Parent Category Folders with Direct Sub-Provider Chips (اسنپ‌پی، دیجی‌پی، بانک‌ها...)
                else -> {
                    items(orderedCategories, key = { "root_cat_${it.name}" }) { cat ->
                        val catProviders = uiState.counterparties.filter { it.categoryEnum == cat }
                        val catProviderIds = catProviders.map { it.id }.toSet()
                        val catObligations = uiState.obligations.filter { it.counterpartyId in catProviderIds }
                        val catObIds = catObligations.map { it.id }.toSet()
                        val catInstallments = uiState.installments.filter { it.obligationId in catObIds }

                        ParentCategoryRootFolderCard(
                            category = cat,
                            subProviders = catProviders,
                            obligationsCount = catObligations.size,
                            installments = catInstallments,
                            currencyUnit = unit,
                            privacyMasked = masked,
                            onOpenCategoryFolder = { onSelectCategory(cat) },
                            onDirectClickSubProvider = { cp -> onSelectCounterparty(cp) },
                            onAddSubProvider = { showAddCounterpartyModalForCategory = cat },
                            onAddObligationInCategory = { onOpenWizard(cat, catProviders.firstOrNull()?.id) },
                            modifier = if (cat == EntityCategory.BNPL_PLATFORM) {
                                Modifier.tourSpotlightTarget("tree_parent_category_bnpl")
                            } else Modifier
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Sub-Provider (Counterparty) Modal
    if (showAddCounterpartyModalForCategory != null || editingCounterparty != null) {
        SubProviderFormDialog(
            initialEntity = editingCounterparty,
            defaultCategory = showAddCounterpartyModalForCategory ?: EntityCategory.BNPL_PLATFORM,
            onDismiss = {
                showAddCounterpartyModalForCategory = null
                editingCounterparty = null
            },
            onSave = { entity ->
                onSaveCounterparty(entity)
                showAddCounterpartyModalForCategory = null
                editingCounterparty = null
            }
        )
    }

    // Quick Edit Obligation Details Modal (ویرایش سریع پرونده وام / خرید)
    editingObligation?.let { ob ->
        EditObligationDetailsModal(
            obligation = ob,
            counterparties = uiState.counterparties,
            onDismiss = { editingObligation = null },
            onSave = { updatedOb ->
                onUpdateObligationDetails(updatedOb)
                editingObligation = null
            }
        )
    }

    // Granular Per-Installment Edit & Settlement Modal
    editingInstallment?.let { inst ->
        GranularInstallmentEditModal(
            installment = inst,
            currencyUnit = unit,
            onDismiss = { editingInstallment = null },
            onSave = { title, baseAmt, dueDate, paidAmt, fee, disc, paidDate, code, note ->
                onDetailedInstallmentUpdate(
                    inst,
                    title,
                    baseAmt,
                    dueDate,
                    paidAmt,
                    fee,
                    disc,
                    paidDate,
                    code,
                    note
                )
                editingInstallment = null
            },
            onDelete = {
                onDeleteSingleInstallment(inst.id, inst.title)
                editingInstallment = null
            }
        )
    }

    // Add Single Custom Installment Row Modal
    addingCustomInstallmentToObligation?.let { ob ->
        val existingCount = uiState.installments.count { it.obligationId == ob.id }
        AddSingleCustomInstallmentModal(
            obligation = ob,
            nextInstallmentNumber = existingCount + 1,
            currencyUnit = unit,
            onDismiss = { addingCustomInstallmentToObligation = null },
            onAdd = { title, dueDate, amount, isPaid, note ->
                onAddSingleCustomInstallment(
                    ob.id,
                    existingCount + 1,
                    title,
                    dueDate,
                    amount,
                    isPaid,
                    note
                )
                addingCustomInstallmentToObligation = null
            }
        )
    }

    // Add Running Ledger Sub-Record Modal
    addingSubRecordToObligation?.let { ob ->
        AddSubRecordDialog(
            obligation = ob,
            currencyUnit = unit,
            onDismiss = { addingSubRecordToObligation = null },
            onSave = { sub ->
                onAddSubRecord(ob, sub)
                addingSubRecordToObligation = null
            }
        )
    }
}

@Composable
private fun BreadcrumbPill(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun categoryIcon(category: EntityCategory): ImageVector = when (category) {
    EntityCategory.BNPL_PLATFORM -> Icons.Default.ShoppingBag
    EntityCategory.BANK -> Icons.Default.AccountBalance
    EntityCategory.INSTITUTION -> Icons.Default.HomeWork
    EntityCategory.PERSON -> Icons.Default.Groups
}

private fun categorySubtitleExamplesFa(category: EntityCategory): String = when (category) {
    EntityCategory.BNPL_PLATFORM -> "زیرمجموعه‌ها: اسنپ‌پی (SnappPay)، دیجی‌پی (DigiPay)، تارا، ازکی‌وام..."
    EntityCategory.BANK -> "زیرمجموعه‌ها: بانک مهر ایران، بلوبانک، بانک ملی، رسالت، ملت..."
    EntityCategory.INSTITUTION -> "زیرمجموعه‌ها: صندوق‌های قرض‌الحسنه خانوادگی، وام شرکتی و موسسات..."
    EntityCategory.PERSON -> "زیرمجموعه‌ها: صاحب‌خانه (اجاره/چک)، دوستان، همکاران و مطالبات شخصی..."
}

@Composable
private fun ParentCategoryRootFolderCard(
    category: EntityCategory,
    subProviders: List<CounterpartyEntity>,
    obligationsCount: Int,
    installments: List<InstallmentEntity>,
    currencyUnit: com.example.core.CurrencyUnit,
    privacyMasked: Boolean,
    onOpenCategoryFolder: () -> Unit,
    onDirectClickSubProvider: (CounterpartyEntity) -> Unit,
    onAddSubProvider: () -> Unit,
    onAddObligationInCategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = Color(category.defaultColorHex)
    val totalPayable = installments.sumOf { it.effectiveTotalToman }
    val totalPaid = installments.sumOf { it.paidAmountToman }
    val totalRem = installments.sumOf { it.remainingToman }
    val progress = if (totalPayable > 0L) {
        (totalPaid.toFloat() / totalPayable.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
            .clickable(onClick = onOpenCategoryFolder)
            .testTag("parent_category_card_${category.name}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(accent.copy(alpha = 0.16f))
                    ) {
                        Icon(
                            imageVector = categoryIcon(category),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = category.labelFa,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = categorySubtitleExamplesFa(category),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = PersianCurrencyEngine.formatAmount(totalRem, currencyUnit, privacyMasked = privacyMasked),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        maxLines = 1
                    )
                    Text(
                        text = "${PersianCurrencyEngine.toPersianDigits(obligationsCount.toString())} پرونده فعال",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "زیرمجموعه‌های این دسته (لمس برای ورود مستقیم):",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                subProviders.forEach { cp ->
                    AssistChip(
                        onClick = { onDirectClickSubProvider(cp) },
                        label = {
                            Text(
                                text = cp.name,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(cp.avatarColorHex))
                            )
                        },
                        modifier = Modifier.testTag("subprovider_chip_${cp.id}")
                    )
                }

                AssistChip(
                    onClick = onAddSubProvider,
                    label = { Text("+ زیرمجموعه جدید", maxLines = 1) }
                )
            }

            if (totalPayable > 0L) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    color = accent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "تسویه‌شده: ${PersianCurrencyEngine.formatAmount(totalPaid, currencyUnit, privacyMasked = privacyMasked)} (${PersianCurrencyEngine.toPersianDigits((progress * 100).toInt().toString())}٪)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF10B981)
                    )
                    Text(
                        text = "باز کردن پوشه ‹",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onAddObligationInCategory) {
                        Text("+ ثبت اولین قسط در این دسته", maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun SubProviderFolderCard(
    counterparty: CounterpartyEntity,
    obligations: List<ObligationEntity>,
    installments: List<InstallmentEntity>,
    currencyUnit: com.example.core.CurrencyUnit,
    privacyMasked: Boolean,
    onClickProvider: () -> Unit,
    onAddPurchaseHere: () -> Unit,
    onOpenA4Report: () -> Unit
) {
    val accent = Color(counterparty.avatarColorHex)
    val obIds = obligations.map { it.id }.toSet()
    val cpInst = installments.filter { it.obligationId in obIds }
    val totalPayable = cpInst.sumOf { it.effectiveTotalToman }
    val totalPaid = cpInst.sumOf { it.paidAmountToman }
    val totalRem = cpInst.sumOf { it.remainingToman }
    val pendingInstCount = cpInst.count { it.statusEnum != InstallmentStatus.PAID }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClickProvider)
            .testTag("subprovider_folder_card_${counterparty.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.18f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = accent
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = counterparty.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${counterparty.categoryEnum.badgeFa} • ${PersianCurrencyEngine.toPersianDigits(obligations.size.toString())} پرونده (${PersianCurrencyEngine.toPersianDigits(pendingInstCount.toString())} قسط باز)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = PersianCurrencyEngine.formatAmount(totalRem, currencyUnit, privacyMasked = privacyMasked),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        maxLines = 1
                    )
                    Text(
                        text = "مانده این زیرمجموعه",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (obligations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    obligations.forEach { ob ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        ) {
                            Text(
                                text = ob.title,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (totalPayable > 0L) {
                Spacer(modifier = Modifier.height(10.dp))
                val ratio = (totalPaid.toFloat() / totalPayable.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { ratio },
                    color = accent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onAddPurchaseHere,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ثبت خرید/قسط جدید", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onOpenA4Report,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "گزارش A4 این زیرمجموعه",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("گزارش A4", maxLines = 1)
                    }
                }

                TextButton(onClick = onClickProvider) {
                    Text("مشاهده اقساط ‹", fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun SubProviderDetailHubHeaderCard(
    counterparty: CounterpartyEntity,
    obligationsCount: Int,
    installments: List<InstallmentEntity>,
    currencyUnit: com.example.core.CurrencyUnit,
    privacyMasked: Boolean,
    onCopyText: (String, String) -> Unit,
    onAddPurchaseForProvider: () -> Unit,
    onEditProvider: () -> Unit,
    onDeleteProvider: () -> Unit,
    onExportA4: () -> Unit
) {
    val accent = Color(counterparty.avatarColorHex)
    val totalRem = installments.sumOf { it.remainingToman }
    val totalPaid = installments.sumOf { it.paidAmountToman }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.2.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = counterparty.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${counterparty.categoryEnum.labelFa} • ${counterparty.priorityEnum.labelFa}",
                        style = MaterialTheme.typography.labelMedium,
                        color = accent
                    )
                }
                Row {
                    IconButton(onClick = onEditProvider) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "ویرایش زیرمجموعه")
                    }
                    IconButton(onClick = onDeleteProvider) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف زیرمجموعه",
                            tint = Color(0xFFEF4444)
                        )
                    }
                }
            }

            if (counterparty.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = counterparty.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accent.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "مانده بدهی در ${counterparty.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = PersianCurrencyEngine.formatAmount(totalRem, currencyUnit, privacyMasked = privacyMasked),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            maxLines = 1
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "پرداخت‌شده تا امروز",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            text = PersianCurrencyEngine.formatAmount(totalPaid, currencyUnit, privacyMasked = privacyMasked),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            maxLines = 1
                        )
                    }
                }
            }

            if (counterparty.cardNumber.isNotBlank() || counterparty.shebaNumber.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    if (counterparty.cardNumber.isNotBlank()) {
                        AssistChip(
                            onClick = { onCopyText("شماره کارت ${counterparty.name}", counterparty.cardNumber) },
                            label = {
                                Text(
                                    PersianCurrencyEngine.toPersianDigits(
                                        PersianCurrencyEngine.formatCardNumber(counterparty.cardNumber)
                                    ),
                                    maxLines = 1
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        )
                    }
                    if (counterparty.shebaNumber.isNotBlank()) {
                        AssistChip(
                            onClick = { onCopyText("شماره شبا ${counterparty.name}", counterparty.shebaNumber) },
                            label = {
                                Text(PersianCurrencyEngine.formatSheba(counterparty.shebaNumber), maxLines = 1)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddPurchaseForProvider,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("provider_add_obligation_btn")
                ) {
                    Icon(imageVector = Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ثبت خرید / قسط جدید", fontWeight = FontWeight.Bold, maxLines = 1)
                }
                OutlinedButton(
                    onClick = onExportA4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("صورتحساب A4", maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ObligationSummaryRowCard(
    obligation: ObligationEntity,
    counterparty: CounterpartyEntity?,
    installments: List<InstallmentEntity>,
    currencyUnit: com.example.core.CurrencyUnit,
    privacyMasked: Boolean,
    onOpenDetail: () -> Unit,
    onEditObligation: () -> Unit,
    onOpenA4: () -> Unit
) {
    val totalPayable = installments.sumOf { it.effectiveTotalToman }
    val totalPaid = installments.sumOf { it.paidAmountToman }
    val totalRem = installments.sumOf { it.remainingToman }
    val paidCount = installments.count { it.statusEnum == InstallmentStatus.PAID }
    val totalCount = installments.size.coerceAtLeast(1)
    val progress = if (totalPayable > 0L) {
        (totalPaid.toFloat() / totalPayable.toFloat()).coerceIn(0f, 1f)
    } else 1f
    val nextUnpaid = installments
        .filter { it.statusEnum != InstallmentStatus.PAID }
        .minByOrNull { it.dueDateJalali }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetail)
            .testTag("obligation_row_card_${obligation.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = obligation.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${counterparty?.name ?: "طرف‌حساب"} • ${PersianCurrencyEngine.toPersianDigits(paidCount.toString())} از ${PersianCurrencyEngine.toPersianDigits(totalCount.toString())} قسط تسویه‌شده",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = PersianCurrencyEngine.formatAmount(totalRem, currencyUnit, privacyMasked = privacyMasked),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                    Text(
                        text = "مانده تعهد",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (nextUnpaid != null) {
                    Text(
                        text = "قسط بعدی: ${PersianCurrencyEngine.toPersianDigits(nextUnpaid.dueDateJalali)} (${
                            PersianCurrencyEngine.formatAmount(
                                nextUnpaid.remainingToman,
                                currencyUnit,
                                privacyMasked = privacyMasked
                            )
                        })",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF59E0B),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Text(
                        text = "تمامی اقساط این پرونده تسویه شده است",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditObligation, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "ویرایش سریع پرونده",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(onClick = onOpenA4, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "جدول اقساط A4",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "جدول قسط‌ها ‹",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ObligationGranularHeaderCard(
    obligation: ObligationEntity,
    counterparty: CounterpartyEntity?,
    installments: List<InstallmentEntity>,
    currencyUnit: com.example.core.CurrencyUnit,
    privacyMasked: Boolean,
    onEditObligation: () -> Unit,
    onAddCustomInstallment: () -> Unit,
    onAddSubRecord: () -> Unit,
    onExportA4: () -> Unit,
    onDeleteObligation: () -> Unit
) {
    val totalPayable = installments.sumOf { it.effectiveTotalToman }
    val totalPaid = installments.sumOf { it.paidAmountToman }
    val totalRem = installments.sumOf { it.remainingToman }
    val paidCount = installments.count { it.statusEnum == InstallmentStatus.PAID }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = obligation.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${counterparty?.name ?: "طرف‌حساب"} • ${obligation.categoryEnum.shortLabelFa}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row {
                    IconButton(onClick = onEditObligation) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "ویرایش مشخصات پرونده"
                        )
                    }
                    IconButton(onClick = onExportA4) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "چاپ جدول اقساط A4",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDeleteObligation) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف پرونده",
                            tint = Color(0xFFEF4444)
                        )
                    }
                }
            }

            if (obligation.notes.isNotBlank() || obligation.trackingOrCheckCode.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = listOfNotNull(
                        obligation.trackingOrCheckCode.takeIf { it.isNotBlank() }?.let { "کد چک/پیگیری: $it" },
                        obligation.notes.takeIf { it.isNotBlank() }
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "مانده این پرونده",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = PersianCurrencyEngine.formatAmount(totalRem, currencyUnit, privacyMasked = privacyMasked),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.14f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "پرداخت‌شده (${PersianCurrencyEngine.toPersianDigits(paidCount.toString())} از ${PersianCurrencyEngine.toPersianDigits(installments.size.toString())})",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = PersianCurrencyEngine.formatAmount(totalPaid, currencyUnit, privacyMasked = privacyMasked),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            maxLines = 1
                        )
                    }
                }
            }

            if (totalPayable > 0L) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { (totalPaid.toFloat() / totalPayable.toFloat()).coerceIn(0f, 1f) },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAddCustomInstallment,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("افزودن ردیف قسط", maxLines = 1)
                }

                OutlinedButton(
                    onClick = onAddSubRecord,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ریزتراکنش دفتری", maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun GranularInstallmentItemCard(
    installment: InstallmentEntity,
    currencyUnit: com.example.core.CurrencyUnit,
    privacyMasked: Boolean,
    isBatchChecked: Boolean,
    onToggleBatchCheck: () -> Unit,
    onQuickPay: () -> Unit,
    onEditInstallment: () -> Unit
) {
    val statusColor = Color(installment.statusEnum.colorHex)
    val isPaid = installment.statusEnum == InstallmentStatus.PAID

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isBatchChecked) {
                Color(0xFF10B981).copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isBatchChecked) 1.5.dp else 1.dp,
                color = if (isBatchChecked) Color(0xFF10B981) else statusColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onEditInstallment)
            .testTag("granular_installment_card_${installment.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (!isPaid) {
                        Checkbox(
                            checked = isBatchChecked,
                            onCheckedChange = { onToggleBatchCheck() },
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("batch_check_inst_${installment.id}")
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "قسط ${PersianCurrencyEngine.toPersianDigits(installment.installmentNumber.toString())}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = installment.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "سررسید: ${PersianCurrencyEngine.toPersianDigits(installment.dueDateJalali)} (${JalaliCalendarEngine.formatJalaliVerbose(installment.dueDateJalali)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = installment.statusEnum.labelFa,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "مبلغ کل قسط: ${
                            PersianCurrencyEngine.formatAmount(
                                installment.effectiveTotalToman,
                                currencyUnit,
                                privacyMasked = privacyMasked
                            )
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (installment.paidAmountToman > 0L && !isPaid) {
                        Text(
                            text = "پرداخت جزئی: ${
                                PersianCurrencyEngine.formatAmount(
                                    installment.paidAmountToman,
                                    currencyUnit,
                                    privacyMasked = privacyMasked
                                )
                            } • مانده: ${
                                PersianCurrencyEngine.formatAmount(
                                    installment.remainingToman,
                                    currencyUnit,
                                    privacyMasked = privacyMasked
                                )
                            }",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF3B82F6)
                        )
                    }
                    if (installment.trackingCode.isNotBlank()) {
                        Text(
                            text = "کد رهگیری: ${PersianCurrencyEngine.toPersianDigits(installment.trackingCode)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onEditInstallment,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ویرایش", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }

                    if (!isPaid) {
                        Button(
                            onClick = onQuickPay,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("tree_quick_pay_${installment.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF042F2E),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "تسویه",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF042F2E),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditObligationDetailsModal(
    obligation: ObligationEntity,
    counterparties: List<CounterpartyEntity>,
    onDismiss: () -> Unit,
    onSave: (ObligationEntity) -> Unit
) {
    var title by remember { mutableStateOf(obligation.title) }
    var selectedCounterpartyId by remember { mutableLongStateOf(obligation.counterpartyId) }
    var selectedCategory by remember { mutableStateOf(obligation.categoryEnum) }
    var selectedPriority by remember { mutableStateOf(obligation.priorityEnum) }
    var trackingOrCheckCode by remember { mutableStateOf(obligation.trackingOrCheckCode) }
    var notes by remember { mutableStateOf(obligation.notes) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "ویرایش مشخصات پرونده وام / خرید",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان پرونده / خرید اقساطی") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("طرف پرداخت / زیرمجموعه:", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    counterparties.forEach { cp ->
                        FilterChip(
                            selected = selectedCounterpartyId == cp.id,
                            onClick = { selectedCounterpartyId = cp.id },
                            label = { Text(cp.name, maxLines = 1) }
                        )
                    }
                }

                Text("نوع تعهد:", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    ObligationCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.shortLabelFa, maxLines = 1) }
                        )
                    }
                }

                Text("سطح اولویت اخلاقی / اعتباری:", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    TrustPriority.entries.forEach { prio ->
                        FilterChip(
                            selected = selectedPriority == prio,
                            onClick = { selectedPriority = prio },
                            label = { Text(prio.labelFa, maxLines = 1) }
                        )
                    }
                }

                OutlinedTextField(
                    value = trackingOrCheckCode,
                    onValueChange = { trackingOrCheckCode = it },
                    label = { Text("شناسه چک صیادی / شماره قرارداد (اختیاری)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("یادداشت پرونده") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("انصراف", maxLines = 1)
                    }
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(
                                    obligation.copy(
                                        title = title.trim(),
                                        counterpartyId = selectedCounterpartyId,
                                        category = selectedCategory.name,
                                        trustPriority = selectedPriority.name,
                                        trackingOrCheckCode = trackingOrCheckCode.trim(),
                                        notes = notes.trim()
                                    )
                                )
                            }
                        },
                        enabled = title.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Text("ذخیره تغییرات پرونده", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun GranularInstallmentEditModal(
    installment: InstallmentEntity,
    currencyUnit: com.example.core.CurrencyUnit,
    onDismiss: () -> Unit,
    onSave: (
        customTitle: String,
        customBaseAmountToman: Long,
        customDueDateJalali: String,
        paidAmountToman: Long,
        lateFeeToman: Long,
        discountToman: Long,
        paidDateJalali: String,
        trackingCode: String,
        note: String
    ) -> Unit,
    onDelete: () -> Unit
) {
    var customTitle by remember { mutableStateOf(installment.title) }
    var customBaseAmount by remember { mutableLongStateOf(installment.amountToman) }
    var customDueDate by remember { mutableStateOf(installment.dueDateJalali) }
    var paidAmount by remember { mutableLongStateOf(installment.paidAmountToman) }
    var lateFee by remember { mutableLongStateOf(installment.lateFeeToman) }
    var discount by remember { mutableLongStateOf(installment.discountToman) }
    var paidDate by remember {
        mutableStateOf(installment.paidDateJalali.ifBlank { JalaliCalendarEngine.todayJalali() })
    }
    var trackingCode by remember { mutableStateOf(installment.trackingCode) }
    var note by remember { mutableStateOf(installment.note) }

    val effectivePayable = (customBaseAmount + lateFee - discount).coerceAtLeast(0L)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ویرایش تفکیکی قسط ${PersianCurrencyEngine.toPersianDigits(installment.installmentNumber.toString())}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف این ردیف قسط",
                            tint = Color(0xFFEF4444)
                        )
                    }
                }

                OutlinedTextField(
                    value = customTitle,
                    onValueChange = { customTitle = it },
                    label = { Text("عنوان این قسط") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                JalaliDateSelectorField(
                    labelFa = "تاریخ سررسید شمسی این قسط",
                    dateJalali = customDueDate,
                    onDateSelected = { customDueDate = it }
                )

                MoneyInputField(
                    labelFa = "مبلغ پایه این قسط",
                    amountToman = customBaseAmount,
                    currencyUnit = currencyUnit,
                    onAmountChange = { customBaseAmount = it }
                )

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "وضعیت پرداخت و تسویه این قسط",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = { paidAmount = effectivePayable }) {
                        Text("پر کردن کل مبلغ (${PersianCurrencyEngine.formatAmount(effectivePayable, currencyUnit)})", maxLines = 1)
                    }
                }

                MoneyInputField(
                    labelFa = "مبلغ پرداخت‌شده تا کنون (جزئی یا کامل)",
                    amountToman = paidAmount,
                    currencyUnit = currencyUnit,
                    onAmountChange = { paidAmount = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        MoneyInputField(
                            labelFa = "جریمه دیرکرد (اختیاری)",
                            amountToman = lateFee,
                            currencyUnit = currencyUnit,
                            onAmountChange = { lateFee = it }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MoneyInputField(
                            labelFa = "تخفیف خوش‌حسابی",
                            amountToman = discount,
                            currencyUnit = currencyUnit,
                            onAmountChange = { discount = it }
                        )
                    }
                }

                JalaliDateSelectorField(
                    labelFa = "تاریخ پرداخت شمسی",
                    dateJalali = paidDate,
                    onDateSelected = { paidDate = it }
                )

                OutlinedTextField(
                    value = trackingCode,
                    onValueChange = { trackingCode = it },
                    label = { Text("کد پیگیری بانکی / شماره رسید") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("یادداشت قسط") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("انصراف", maxLines = 1)
                    }
                    Button(
                        onClick = {
                            onSave(
                                customTitle,
                                customBaseAmount,
                                customDueDate,
                                paidAmount,
                                lateFee,
                                discount,
                                paidDate,
                                trackingCode,
                                note
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Text("ذخیره تغییرات قسط", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddSingleCustomInstallmentModal(
    obligation: ObligationEntity,
    nextInstallmentNumber: Int,
    currencyUnit: com.example.core.CurrencyUnit,
    onDismiss: () -> Unit,
    onAdd: (title: String, dueDate: String, amountToman: Long, isPaid: Boolean, note: String) -> Unit
) {
    var title by remember {
        mutableStateOf("قسط ${PersianCurrencyEngine.toPersianDigits(nextInstallmentNumber.toString())} - ${obligation.title}")
    }
    var dueDate by remember { mutableStateOf(JalaliCalendarEngine.todayJalali()) }
    var amountToman by remember {
        mutableLongStateOf((obligation.netPayableToman / obligation.installmentCount.coerceAtLeast(1)).coerceAtLeast(500_000L))
    }
    var isPaid by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "افزودن ردیف قسط مجزا به «${obligation.title}»",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان قسط") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                JalaliDateSelectorField(
                    labelFa = "تاریخ سررسید شمسی",
                    dateJalali = dueDate,
                    onDateSelected = { dueDate = it }
                )

                MoneyInputField(
                    labelFa = "مبلغ این قسط",
                    amountToman = amountToman,
                    currencyUnit = currencyUnit,
                    onAmountChange = { amountToman = it }
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPaid = !isPaid }
                ) {
                    Checkbox(checked = isPaid, onCheckedChange = { isPaid = it })
                    Text("این قسط قبلاً پرداخت و تسویه شده است")
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("توضیحات (اختیاری)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("انصراف", maxLines = 1)
                    }
                    Button(
                        onClick = { onAdd(title, dueDate, amountToman, isPaid, note) },
                        enabled = amountToman > 0L,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("افزودن به جدول اقساط", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun SubProviderFormDialog(
    initialEntity: CounterpartyEntity?,
    defaultCategory: EntityCategory,
    onDismiss: () -> Unit,
    onSave: (CounterpartyEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialEntity?.name ?: "") }
    var category by remember { mutableStateOf(initialEntity?.categoryEnum ?: defaultCategory) }
    var priority by remember { mutableStateOf(initialEntity?.priorityEnum ?: TrustPriority.HIGH) }
    var cardNumber by remember { mutableStateOf(initialEntity?.cardNumber ?: "") }
    var shebaNumber by remember { mutableStateOf(initialEntity?.shebaNumber ?: "") }
    var description by remember { mutableStateOf(initialEntity?.description ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (initialEntity == null) "افزودن زیرمجموعه / طرف پرداخت جدید" else "ویرایش زیرمجموعه",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "دسته مادر:",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    EntityCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.badgeFa, maxLines = 1) }
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام زیرمجموعه (مثلاً: اسنپ‌پی، دیجی‌پی، تارا، بانک ملت)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { cardNumber = it },
                    label = { Text("شماره کارت ۱۶ رقمی مقصد (اختیاری)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = shebaNumber,
                    onValueChange = { shebaNumber = it },
                    label = { Text("شماره شبا ۲۴ رقمی (اختیاری)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات کوتاه") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("انصراف", maxLines = 1)
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(
                                    CounterpartyEntity(
                                        id = initialEntity?.id ?: 0L,
                                        name = name.trim(),
                                        category = category.name,
                                        avatarColorHex = category.defaultColorHex,
                                        cardNumber = cardNumber.trim(),
                                        shebaNumber = shebaNumber.trim(),
                                        trustPriority = priority.name,
                                        description = description.trim(),
                                        createdAtJalali = initialEntity?.createdAtJalali ?: JalaliCalendarEngine.todayJalali()
                                    )
                                )
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("ذخیره زیرمجموعه", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddSubRecordDialog(
    obligation: ObligationEntity,
    currencyUnit: com.example.core.CurrencyUnit,
    onDismiss: () -> Unit,
    onSave: (LedgerSubRecordEntity) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountToman by remember { mutableLongStateOf(1_000_000L) }
    var dateJalali by remember { mutableStateOf(JalaliCalendarEngine.todayJalali()) }
    var recordType by remember { mutableStateOf(SubRecordType.CHARGE) }
    var receiptCode by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "ثبت ریزتراکنش در پرونده «${obligation.title}»",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SubRecordType.entries.forEach { t ->
                        FilterChip(
                            selected = recordType == t,
                            onClick = { recordType = t },
                            label = { Text(t.labelFa, maxLines = 1) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("شرح تراکنش (مثلاً: خرید جدید یا واریز علی‌الحساب)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                MoneyInputField(
                    labelFa = "مبلغ تراکنش",
                    amountToman = amountToman,
                    currencyUnit = currencyUnit,
                    onAmountChange = { amountToman = it }
                )

                JalaliDateSelectorField(
                    labelFa = "تاریخ شمسی تراکنش",
                    dateJalali = dateJalali,
                    onDateSelected = { dateJalali = it }
                )

                OutlinedTextField(
                    value = receiptCode,
                    onValueChange = { receiptCode = it },
                    label = { Text("کد پیگیری / شماره رسید (اختیاری)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("انصراف", maxLines = 1)
                    }
                    Button(
                        onClick = {
                            if (title.isNotBlank() && amountToman > 0L) {
                                onSave(
                                    LedgerSubRecordEntity(
                                        obligationId = obligation.id,
                                        title = title.trim(),
                                        dateJalali = dateJalali,
                                        amountToman = amountToman,
                                        recordType = recordType.name,
                                        receiptCode = receiptCode.trim()
                                    )
                                )
                            }
                        },
                        enabled = title.isNotBlank() && amountToman > 0L,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("ثبت در پرونده", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText(label, text))
}
