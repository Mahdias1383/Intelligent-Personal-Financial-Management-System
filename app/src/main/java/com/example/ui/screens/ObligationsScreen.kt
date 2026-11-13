package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.data.CounterpartyEntity
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.LedgerSubRecordEntity
import com.example.data.ObligationCategory
import com.example.data.ObligationEntity
import com.example.data.PaymentInterval
import com.example.data.ScheduleMode
import com.example.data.SubRecordType
import com.example.data.TrustPriority
import com.example.ui.TarazinoUiState
import com.example.ui.components.JalaliDateSelectorField
import com.example.ui.components.MoneyInputField

@Composable
fun ObligationsScreen(
    uiState: TarazinoUiState,
    onCreateAutoScheduleObligation: (ObligationEntity) -> Unit,
    onCreateRunningLedgerObligation: (ObligationEntity, String, Long, String, String) -> Unit,
    onAddSubRecord: (ObligationEntity, LedgerSubRecordEntity) -> Unit,
    onDeleteObligation: (Long, String) -> Unit,
    onQuickPayInstallment: (InstallmentEntity) -> Unit,
    onDetailedInstallmentUpdate: (InstallmentEntity, Long, Long, Long, String, String, String) -> Unit,
    onExportObligationA4Report: (ObligationEntity) -> Unit
) {
    val unit = uiState.preferences.currencyUnit
    val masked = uiState.preferences.privacyMaskEnabled

    var selectedCategoryFilter by remember { mutableStateOf<ObligationCategory?>(null) }
    var expandedObligationId by remember { mutableStateOf<Long?>(uiState.obligations.firstOrNull()?.id) }
    var showCreateModal by remember { mutableStateOf(false) }
    var editingInstallment by remember { mutableStateOf<InstallmentEntity?>(null) }
    var addingSubRecordForObligation by remember { mutableStateOf<ObligationEntity?>(null) }

    val cpMap = remember(uiState.counterparties) { uiState.counterparties.associateBy { it.id } }

    val filteredObligations = remember(uiState.obligations, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) uiState.obligations
        else uiState.obligations.filter { it.categoryEnum == selectedCategoryFilter }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateModal = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("create_obligation_fab")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "ثبت تعهد یا وام جدید")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ثبت وام / قسط / بدهی جدید", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("obligations_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category Filter Chips (5 Categories)
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    FilterChip(
                        selected = selectedCategoryFilter == null,
                        onClick = { selectedCategoryFilter = null },
                        label = {
                            Text("همه (${PersianCurrencyEngine.toPersianDigits(uiState.obligations.size.toString())})")
                        }
                    )
                    ObligationCategory.entries.forEach { cat ->
                        val count = uiState.obligations.count { it.categoryEnum == cat }
                        FilterChip(
                            selected = selectedCategoryFilter == cat,
                            onClick = {
                                selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat
                            },
                            label = {
                                Text("${cat.shortLabelFa} (${PersianCurrencyEngine.toPersianDigits(count.toString())})")
                            }
                        )
                    }
                }
            }

            items(filteredObligations, key = { it.id }) { obligation ->
                val obInstallments = uiState.installments.filter { it.obligationId == obligation.id }
                val obSubRecords = uiState.subRecords.filter { it.obligationId == obligation.id }
                val counterparty = cpMap[obligation.counterpartyId]
                val isExpanded = expandedObligationId == obligation.id

                ObligationExpandableCard(
                    obligation = obligation,
                    counterparty = counterparty,
                    installments = obInstallments,
                    subRecords = obSubRecords,
                    isExpanded = isExpanded,
                    currencyUnit = unit,
                    privacyMasked = masked,
                    onToggleExpand = {
                        expandedObligationId = if (isExpanded) null else obligation.id
                    },
                    onQuickPayInstallment = onQuickPayInstallment,
                    onOpenDetailedPayment = { editingInstallment = it },
                    onOpenAddSubRecord = { addingSubRecordForObligation = obligation },
                    onExportA4Schedule = { onExportObligationA4Report(obligation) },
                    onDeleteObligation = { onDeleteObligation(obligation.id, obligation.title) }
                )
            }
        }
    }

    if (showCreateModal) {
        CreateObligationModal(
            counterparties = uiState.counterparties,
            currencyUnit = unit,
            onDismiss = { showCreateModal = false },
            onSaveAutoSchedule = { ob ->
                onCreateAutoScheduleObligation(ob)
                showCreateModal = false
            },
            onSaveRunningLedger = { ob, itemTitle, itemAmt, itemDate, receipt ->
                onCreateRunningLedgerObligation(ob, itemTitle, itemAmt, itemDate, receipt)
                showCreateModal = false
            }
        )
    }

    editingInstallment?.let { targetInst ->
        DetailedInstallmentPaymentModal(
            installment = targetInst,
            currencyUnit = unit,
            onDismiss = { editingInstallment = null },
            onSavePayment = { paid, fee, discount, paidDate, code, note ->
                onDetailedInstallmentUpdate(targetInst, paid, fee, discount, paidDate, code, note)
                editingInstallment = null
            }
        )
    }

    addingSubRecordForObligation?.let { targetOb ->
        AddLedgerSubRecordModal(
            obligation = targetOb,
            currencyUnit = unit,
            onDismiss = { addingSubRecordForObligation = null },
            onSaveSubRecord = { sub ->
                onAddSubRecord(targetOb, sub)
                addingSubRecordForObligation = null
            }
        )
    }
}

@Composable
private fun ObligationExpandableCard(
    obligation: ObligationEntity,
    counterparty: CounterpartyEntity?,
    installments: List<InstallmentEntity>,
    subRecords: List<LedgerSubRecordEntity>,
    isExpanded: Boolean,
    currencyUnit: com.example.core.CurrencyUnit,
    privacyMasked: Boolean,
    onToggleExpand: () -> Unit,
    onQuickPayInstallment: (InstallmentEntity) -> Unit,
    onOpenDetailedPayment: (InstallmentEntity) -> Unit,
    onOpenAddSubRecord: () -> Unit,
    onExportA4Schedule: () -> Unit,
    onDeleteObligation: () -> Unit
) {
    val catColor = Color(obligation.categoryEnum.colorHex)
    val totalEffective = installments.sumOf { it.effectiveTotalToman }
    val totalPaid = installments.sumOf { it.paidAmountToman }
    val totalRemaining = installments.sumOf { it.remainingToman }
    val paidCount = installments.count { it.statusEnum == InstallmentStatus.PAID }
    val progress = if (totalEffective > 0L) (totalPaid.toFloat() / totalEffective.toFloat()).coerceIn(0f, 1f) else 1f

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = catColor.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = obligation.categoryEnum.shortLabelFa,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = catColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = obligation.scheduleModeEnum.labelFa,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = obligation.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "طرف‌حساب: ${counterparty?.name ?: "نامشخص"} • ${
                            PersianCurrencyEngine.toPersianDigits(paidCount.toString())
                        } از ${PersianCurrencyEngine.toPersianDigits(installments.size.toString())} قسط تسویه‌شده",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "مشاهده ریزاقساط"
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress },
                color = catColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "مانده: ${PersianCurrencyEngine.formatAmount(totalRemaining, currencyUnit, privacyMasked = privacyMasked)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (totalRemaining > 0L) Color(0xFFF59E0B) else Color(0xFF10B981)
                )
                Text(
                    text = "کل: ${PersianCurrencyEngine.formatAmount(totalEffective, currencyUnit, privacyMasked = privacyMasked)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expanded Schedule & Sub-Records Section
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    // Action Bar inside Expanded Obligation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenAddSubRecord,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ثبت ریزخرید / پرداخت دفتری", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = onExportA4Schedule,
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("چاپ A4 و اکسل", style = MaterialTheme.typography.labelMedium)
                        }

                        IconButton(onClick = onDeleteObligation) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف تعهد",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // Running Ledger Sub-Records History if any
                    if (subRecords.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "ریزتراکنش‌های حساب دفتری (${PersianCurrencyEngine.toPersianDigits(subRecords.size.toString())} مورد):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        subRecords.forEach { sr ->
                            val isCharge = sr.recordTypeEnum == SubRecordType.CHARGE
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sr.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${PersianCurrencyEngine.toPersianDigits(sr.dateJalali)} • ${sr.recordTypeEnum.labelFa}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = (if (isCharge) "+" else "-") + PersianCurrencyEngine.formatAmount(
                                        sr.amountToman,
                                        currencyUnit,
                                        privacyMasked = privacyMasked
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCharge) Color(0xFFF59E0B) else Color(0xFF10B981)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "جدول اقساط و سررسیدها:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    installments.sortedBy { it.installmentNumber }.forEach { inst ->
                        val statusColor = Color(inst.statusEnum.colorHex)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(0.8.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = inst.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "سررسید: ${PersianCurrencyEngine.toPersianDigits(inst.dueDateJalali)} • ${inst.statusEnum.labelFa}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = statusColor,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = PersianCurrencyEngine.formatAmount(
                                                inst.effectiveTotalToman,
                                                currencyUnit,
                                                privacyMasked = privacyMasked
                                            ),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (inst.remainingToman > 0L && inst.paidAmountToman > 0L) {
                                            Text(
                                                text = "مانده: ${
                                                    PersianCurrencyEngine.formatAmount(
                                                        inst.remainingToman,
                                                        currencyUnit,
                                                        privacyMasked = privacyMasked
                                                    )
                                                }",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFF59E0B)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = { onOpenDetailedPayment(inst) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EditNote,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("پرداخت جزئی / جریمه / تخفیف")
                                    }

                                    if (inst.statusEnum != InstallmentStatus.PAID) {
                                        Button(
                                            onClick = { onQuickPayInstallment(inst) },
                                            modifier = Modifier.height(34.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("تسویه کامل", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateObligationModal(
    counterparties: List<CounterpartyEntity>,
    currencyUnit: com.example.core.CurrencyUnit,
    onDismiss: () -> Unit,
    onSaveAutoSchedule: (ObligationEntity) -> Unit,
    onSaveRunningLedger: (ObligationEntity, String, Long, String, String) -> Unit
) {
    var selectedCounterpartyId by remember {
        mutableLongStateOf(counterparties.firstOrNull()?.id ?: 1L)
    }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ObligationCategory.INSTALLMENT_LOAN) }
    var scheduleMode by remember { mutableStateOf(ScheduleMode.AUTO_SCHEDULE) }
    var principalToman by remember { mutableLongStateOf(30_000_000L) }
    var profitOrFeeToman by remember { mutableLongStateOf(0L) }
    var downPaymentToman by remember { mutableLongStateOf(0L) }
    var installmentCount by remember { mutableIntStateOf(6) }
    var interval by remember { mutableStateOf(PaymentInterval.MONTHLY) }
    var firstDueDateJalali by remember {
        mutableStateOf(JalaliCalendarEngine.firstOfNextJalaliMonth(JalaliCalendarEngine.todayJalali()))
    }
    var priority by remember { mutableStateOf(TrustPriority.HIGH) }
    var trackingCode by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val netPayable = (principalToman + profitOrFeeToman - downPaymentToman).coerceAtLeast(0L)
    val perInstallmentPreview = if (installmentCount > 0) netPayable / installmentCount else netPayable

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("ثبت تعهد، وام، خرید اقساطی یا حساب دفتری جدید", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان وام، خرید اقساطی، چک یا قرض") },
                    placeholder = { Text("مثلاً وام بانک ملت، خرید گوشی از دیجی‌پی...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("انتخاب طرف‌حساب:", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    counterparties.forEach { cp ->
                        FilterChip(
                            selected = selectedCounterpartyId == cp.id,
                            onClick = { selectedCounterpartyId = cp.id },
                            label = { Text(cp.name) }
                        )
                    }
                }

                Text("دسته‌بندی مالی:", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    ObligationCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.shortLabelFa) }
                        )
                    }
                }

                Text("حالت محاسبه و ثبت:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScheduleMode.entries.forEach { mode ->
                        FilterChip(
                            selected = scheduleMode == mode,
                            onClick = { scheduleMode = mode },
                            label = { Text(mode.labelFa) }
                        )
                    }
                }

                MoneyInputField(
                    labelFa = "مبلغ اصل وام / خرید / قرض",
                    amountToman = principalToman,
                    onAmountChange = { principalToman = it },
                    currencyUnit = currencyUnit
                )

                if (scheduleMode == ScheduleMode.AUTO_SCHEDULE) {
                    MoneyInputField(
                        labelFa = "کارمزد یا سود کل (اختیاری)",
                        amountToman = profitOrFeeToman,
                        onAmountChange = { profitOrFeeToman = it },
                        currencyUnit = currencyUnit,
                        showQuickChips = false
                    )

                    MoneyInputField(
                        labelFa = "پیش‌پرداخت پرداخت‌شده (اختیاری)",
                        amountToman = downPaymentToman,
                        onAmountChange = { downPaymentToman = it },
                        currencyUnit = currencyUnit,
                        showQuickChips = false
                    )

                    Text("تعداد اقساط:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        listOf(1, 2, 3, 4, 6, 10, 12, 18, 24, 36).forEach { count ->
                            FilterChip(
                                selected = installmentCount == count,
                                onClick = { installmentCount = count },
                                label = { Text("${PersianCurrencyEngine.toPersianDigits(count.toString())} قسط") }
                            )
                        }
                    }

                    Text("فاصله زمانی اقساط:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        PaymentInterval.entries.forEach { iv ->
                            FilterChip(
                                selected = interval == iv,
                                onClick = { interval = iv },
                                label = { Text(iv.labelFa) }
                            )
                        }
                    }

                    // Live Schedule Preview Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "خالص قابل پرداخت: ${PersianCurrencyEngine.formatAmount(netPayable, currencyUnit)}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "مبلغ هر قسط: ${PersianCurrencyEngine.formatAmount(perInstallmentPreview, currencyUnit)} (${
                                    PersianCurrencyEngine.amountToPersianWords(perInstallmentPreview, currencyUnit)
                                })",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                JalaliDateSelectorField(
                    labelFa = "تاریخ اولین سررسید شمسی",
                    dateJalali = firstDueDateJalali,
                    onDateSelected = { firstDueDateJalali = it }
                )

                Text("درجه حساسیت تعهد:", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    TrustPriority.entries.forEach { prio ->
                        FilterChip(
                            selected = priority == prio,
                            onClick = { priority = prio },
                            label = { Text(prio.labelFa) }
                        )
                    }
                }

                OutlinedTextField(
                    value = trackingCode,
                    onValueChange = { trackingCode = it },
                    label = { Text("شماره قرارداد / شناسه صیادی چک / کد پیگیری") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val safeTitle = title.trim().ifBlank { "تعهد مالی جدید" }
                    val ob = ObligationEntity(
                        counterpartyId = selectedCounterpartyId,
                        title = safeTitle,
                        category = category.name,
                        scheduleMode = scheduleMode.name,
                        principalAmountToman = principalToman,
                        profitOrFeeToman = profitOrFeeToman,
                        downPaymentToman = downPaymentToman,
                        installmentCount = installmentCount.coerceAtLeast(1),
                        paymentInterval = interval.name,
                        firstDueDateJalali = firstDueDateJalali,
                        trustPriority = priority.name,
                        trackingOrCheckCode = trackingCode.trim(),
                        notes = notes.trim()
                    )
                    if (scheduleMode == ScheduleMode.AUTO_SCHEDULE) {
                        onSaveAutoSchedule(ob)
                    } else {
                        onSaveRunningLedger(
                            ob,
                            safeTitle,
                            principalToman,
                            firstDueDateJalali,
                            trackingCode.trim()
                        )
                    }
                }
            ) {
                Text("ثبت و تولید جدول اقساط")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

@Composable
private fun DetailedInstallmentPaymentModal(
    installment: InstallmentEntity,
    currencyUnit: com.example.core.CurrencyUnit,
    onDismiss: () -> Unit,
    onSavePayment: (Long, Long, Long, String, String, String) -> Unit
) {
    var paidAmount by remember {
        mutableLongStateOf(
            if (installment.paidAmountToman > 0L) installment.paidAmountToman else installment.effectiveTotalToman
        )
    }
    var lateFee by remember { mutableLongStateOf(installment.lateFeeToman) }
    var discount by remember { mutableLongStateOf(installment.discountToman) }
    var paidDate by remember {
        mutableStateOf(installment.paidDateJalali.ifBlank { JalaliCalendarEngine.todayJalali() })
    }
    var trackingCode by remember { mutableStateOf(installment.trackingCode) }
    var note by remember { mutableStateOf(installment.note) }

    val effectiveTotal = (installment.amountToman + lateFee - discount).coerceAtLeast(0L)
    val remainingPreview = (effectiveTotal - paidAmount).coerceAtLeast(0L)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("ثبت پرداخت (کامل یا جزئی)، جریمه دیرکرد و تخفیف", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${installment.title} • مبلغ پایه: ${PersianCurrencyEngine.formatAmount(installment.amountToman, currencyUnit)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                MoneyInputField(
                    labelFa = "مبلغ پرداخت‌شده تا این لحظه",
                    amountToman = paidAmount,
                    onAmountChange = { paidAmount = it },
                    currencyUnit = currencyUnit
                )

                MoneyInputField(
                    labelFa = "جریمه دیرکرد (در صورت وجود)",
                    amountToman = lateFee,
                    onAmountChange = { lateFee = it },
                    currencyUnit = currencyUnit,
                    showQuickChips = false
                )

                MoneyInputField(
                    labelFa = "تخفیف خوش‌حسابی / تسویه زودهنگام",
                    amountToman = discount,
                    onAmountChange = { discount = it },
                    currencyUnit = currencyUnit,
                    showQuickChips = false
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "جمع نهایی قسط پس از جریمه/تخفیف: ${PersianCurrencyEngine.formatAmount(effectiveTotal, currencyUnit)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "مانده باقیمانده این قسط: ${PersianCurrencyEngine.formatAmount(remainingPreview, currencyUnit)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (remainingPreview == 0L) Color(0xFF10B981) else Color(0xFFF59E0B)
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
                    label = { Text("کد پیگیری تراکنش / شماره فیش") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("یادداشت تراکنش") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSavePayment(paidAmount, lateFee, discount, paidDate, trackingCode.trim(), note.trim())
                }
            ) {
                Text("ثبت و بازمحاسبه قسط")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

@Composable
private fun AddLedgerSubRecordModal(
    obligation: ObligationEntity,
    currencyUnit: com.example.core.CurrencyUnit,
    onDismiss: () -> Unit,
    onSaveSubRecord: (LedgerSubRecordEntity) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var recordType by remember { mutableStateOf(SubRecordType.CHARGE) }
    var amountToman by remember { mutableLongStateOf(2_500_000L) }
    var dateJalali by remember { mutableStateOf(JalaliCalendarEngine.todayJalali()) }
    var receiptCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("افزودن ریزخرید یا پرداخت به «${obligation.title}»", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SubRecordType.entries.forEach { t ->
                        FilterChip(
                            selected = recordType == t,
                            onClick = { recordType = t },
                            label = { Text(t.labelFa) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("شرح آیتم خرید یا پرداخت مرحله‌ای") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                MoneyInputField(
                    labelFa = "مبلغ تراکنش",
                    amountToman = amountToman,
                    onAmountChange = { amountToman = it },
                    currencyUnit = currencyUnit
                )

                JalaliDateSelectorField(
                    labelFa = "تاریخ شمسی تراکنش",
                    dateJalali = dateJalali,
                    onDateSelected = { dateJalali = it }
                )

                OutlinedTextField(
                    value = receiptCode,
                    onValueChange = { receiptCode = it },
                    label = { Text("شماره فاکتور یا کد رهگیری") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveSubRecord(
                        LedgerSubRecordEntity(
                            obligationId = obligation.id,
                            title = title.trim().ifBlank { recordType.labelFa },
                            dateJalali = dateJalali,
                            amountToman = amountToman,
                            recordType = recordType.name,
                            receiptCode = receiptCode.trim()
                        )
                    )
                }
            ) {
                Text("ثبت در حساب دفتری")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
