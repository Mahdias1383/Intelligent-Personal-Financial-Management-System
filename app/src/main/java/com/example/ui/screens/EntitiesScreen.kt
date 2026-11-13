package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.unit.dp
import com.example.core.PersianCurrencyEngine
import com.example.data.CounterpartyEntity
import com.example.data.EntityCategory
import com.example.data.InstallmentEntity
import com.example.data.ObligationEntity
import com.example.data.TrustPriority
import com.example.ui.TarazinoUiState

@Composable
fun EntitiesScreen(
    uiState: TarazinoUiState,
    onSaveCounterparty: (CounterpartyEntity) -> Unit,
    onDeleteCounterparty: (Long, String) -> Unit,
    onOpenCounterpartyA4Report: (CounterpartyEntity) -> Unit,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    val unit = uiState.preferences.currencyUnit
    val masked = uiState.preferences.privacyMaskEnabled

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<EntityCategory?>(null) }
    var editingEntity by remember { mutableStateOf<CounterpartyEntity?>(null) }
    var isCreateModalOpen by remember { mutableStateOf(false) }
    var selectedLedgerEntity by remember { mutableStateOf<CounterpartyEntity?>(null) }

    val filteredEntities = remember(uiState.counterparties, searchQuery, selectedCategory) {
        uiState.counterparties.filter { cp ->
            val matchesCat = selectedCategory == null || cp.categoryEnum == selectedCategory
            val matchesQuery = searchQuery.isBlank() ||
                cp.name.contains(searchQuery, ignoreCase = true) ||
                cp.description.contains(searchQuery, ignoreCase = true) ||
                cp.cardNumber.contains(PersianCurrencyEngine.toEnglishDigits(searchQuery))
            matchesCat && matchesQuery
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingEntity = null
                    isCreateModalOpen = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("add_counterparty_fab")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "افزودن طرف‌حساب")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("طرف‌حساب جدید", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("entities_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search & Category Filters
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("جستجوی نام بانک، شخص، پلتفرم یا شماره کارت...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_counterparty_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = {
                            Text("همه (${PersianCurrencyEngine.toPersianDigits(uiState.counterparties.size.toString())})")
                        }
                    )
                    EntityCategory.entries.forEach { cat ->
                        val count = uiState.counterparties.count { it.categoryEnum == cat }
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                selectedCategory = if (selectedCategory == cat) null else cat
                            },
                            label = {
                                Text("${cat.badgeFa} (${PersianCurrencyEngine.toPersianDigits(count.toString())})")
                            }
                        )
                    }
                }
            }

            items(filteredEntities, key = { it.id }) { entity ->
                val linkedObligations = uiState.obligations.filter { it.counterpartyId == entity.id }
                val linkedObIds = linkedObligations.map { it.id }.toSet()
                val linkedInstallments = uiState.installments.filter { it.obligationId in linkedObIds }

                val totalAmount = linkedInstallments.sumOf { it.effectiveTotalToman }
                val paidAmount = linkedInstallments.sumOf { it.paidAmountToman }
                val remainingAmount = linkedInstallments.sumOf { it.remainingToman }

                CounterpartyProfileCard(
                    entity = entity,
                    linkedObligationsCount = linkedObligations.size,
                    totalToman = totalAmount,
                    paidToman = paidAmount,
                    remainingToman = remainingAmount,
                    currencyUnit = unit,
                    privacyMasked = masked,
                    onCopyText = { label, text ->
                        copyToClipboard(context, label, text)
                        onShowToast("$label در حافظه کپی شد.")
                    },
                    onOpenLedger = { selectedLedgerEntity = entity },
                    onExportA4Statement = { onOpenCounterpartyA4Report(entity) },
                    onEdit = {
                        editingEntity = entity
                        isCreateModalOpen = true
                    },
                    onDelete = { onDeleteCounterparty(entity.id, entity.name) }
                )
            }
        }
    }

    if (isCreateModalOpen) {
        CounterpartyEditorModal(
            initialEntity = editingEntity,
            onDismiss = { isCreateModalOpen = false },
            onSave = { saved ->
                onSaveCounterparty(saved)
                isCreateModalOpen = false
            }
        )
    }

    selectedLedgerEntity?.let { targetEntity ->
        val linkedObligations = uiState.obligations.filter { it.counterpartyId == targetEntity.id }
        val linkedObIds = linkedObligations.map { it.id }.toSet()
        val linkedInstallments = uiState.installments.filter { it.obligationId in linkedObIds }

        EntityLedgerModal(
            entity = targetEntity,
            obligations = linkedObligations,
            installments = linkedInstallments,
            currencyUnit = unit,
            privacyMasked = masked,
            onDismiss = { selectedLedgerEntity = null },
            onExportA4AndExcel = {
                selectedLedgerEntity = null
                onOpenCounterpartyA4Report(targetEntity)
            }
        )
    }
}

@Composable
private fun CounterpartyProfileCard(
    entity: CounterpartyEntity,
    linkedObligationsCount: Int,
    totalToman: Long,
    paidToman: Long,
    remainingToman: Long,
    currencyUnit: com.example.core.CurrencyUnit,
    privacyMasked: Boolean,
    onCopyText: (String, String) -> Unit,
    onOpenLedger: () -> Unit,
    onExportA4Statement: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val avatarColor = Color(entity.avatarColorHex)
    val priority = entity.priorityEnum

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
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
                            .clip(CircleShape)
                            .background(avatarColor.copy(alpha = 0.2f))
                            .border(1.5.dp, avatarColor, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (entity.categoryEnum == EntityCategory.PERSON) {
                                Icons.Default.Person
                            } else {
                                Icons.Default.AccountBalance
                            },
                            contentDescription = null,
                            tint = avatarColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = entity.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = entity.categoryEnum.badgeFa,
                                style = MaterialTheme.typography.labelSmall,
                                color = avatarColor
                            )
                            Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = priority.labelFa,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(priority.badgeColorHex),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "ویرایش طرف‌حساب",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف طرف‌حساب",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (entity.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = entity.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Card Number & Sheba Copy Bar
            if (entity.cardNumber.isNotBlank() || entity.shebaNumber.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        if (entity.cardNumber.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "کارت: ${PersianCurrencyEngine.formatCardNumber(entity.cardNumber)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                TextButton(
                                    onClick = { onCopyText("شماره کارت ${entity.name}", entity.cardNumber) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "کپی شماره کارت",
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("کپی کارت", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        if (entity.shebaNumber.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "شبا: ${entity.shebaNumber}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = { onCopyText("شماره شبا ${entity.name}", entity.shebaNumber) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "کپی شماره شبا",
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("کپی شبا", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Financial Balance Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "پرونده‌های فعال: ${PersianCurrencyEngine.toPersianDigits(linkedObligationsCount.toString())}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "کل: ${PersianCurrencyEngine.formatAmount(totalToman, currencyUnit, privacyMasked = privacyMasked)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "مانده: ${PersianCurrencyEngine.formatAmount(remainingToman, currencyUnit, privacyMasked = privacyMasked)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (remainingToman > 0L) Color(0xFFF59E0B) else Color(0xFF10B981)
                    )
                    Text(
                        text = "تسویه‌شده: ${PersianCurrencyEngine.formatAmount(paidToman, currencyUnit, privacyMasked = privacyMasked)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF10B981)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Comprehensive Ledger + A4 Statement Export
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenLedger,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_entity_ledger_btn_${entity.id}")
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("پرونده جامع")
                }

                OutlinedButton(
                    onClick = onExportA4Statement,
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("export_entity_a4_btn_${entity.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("صورتحساب A4 و اکسل")
                }
            }
        }
    }
}

@Composable
private fun CounterpartyEditorModal(
    initialEntity: CounterpartyEntity?,
    onDismiss: () -> Unit,
    onSave: (CounterpartyEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialEntity?.name ?: "") }
    var category by remember { mutableStateOf(initialEntity?.categoryEnum ?: EntityCategory.BANK) }
    var priority by remember { mutableStateOf(initialEntity?.priorityEnum ?: TrustPriority.HIGH) }
    var avatarColorHex by remember { mutableLongStateOf(initialEntity?.avatarColorHex ?: category.defaultColorHex) }
    var cardNumber by remember { mutableStateOf(initialEntity?.cardNumber ?: "") }
    var shebaNumber by remember { mutableStateOf(initialEntity?.shebaNumber ?: "") }
    var phoneNumber by remember { mutableStateOf(initialEntity?.phoneNumber ?: "") }
    var description by remember { mutableStateOf(initialEntity?.description ?: "") }

    val colorChoices = listOf(0xFF10B981L, 0xFF3B82F6L, 0xFFF59E0BL, 0xFF8B5CF6L, 0xFFEC4899L, 0xFF06B6D4L)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialEntity == null) "تعریف طرف‌حساب جدید" else "ویرایش پروفایل طرف‌حساب",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام بانک، پلتفرم اقساطی، صندوق یا شخص") },
                    placeholder = { Text("مثلاً بانک ملت، دیجی‌پی، علی رضایی") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("نوع طرف‌حساب:", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    EntityCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = {
                                category = cat
                                avatarColorHex = cat.defaultColorHex
                            },
                            label = { Text(cat.badgeFa) }
                        )
                    }
                }

                Text("درجه حساسیت خوش‌حسابی و اولویت:", style = MaterialTheme.typography.labelMedium)
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

                Text("رنگ نشان طرف‌حساب:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    colorChoices.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(
                                    width = if (avatarColorHex == hex) 2.5.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable { avatarColorHex = hex }
                        )
                    }
                }

                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = {
                        cardNumber = PersianCurrencyEngine.toEnglishDigits(it).filter { c -> c.isDigit() }.take(16)
                    },
                    label = { Text("شماره کارت ۱۶ رقمی (اختیاری)") },
                    placeholder = { Text("60379911...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = shebaNumber,
                    onValueChange = { shebaNumber = PersianCurrencyEngine.formatSheba(it) },
                    label = { Text("شماره شبا (IR...)") },
                    placeholder = { Text("IR450600...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("شماره تماس / شعبه") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات تکمیلی") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            CounterpartyEntity(
                                id = initialEntity?.id ?: 0L,
                                name = name.trim(),
                                category = category.name,
                                avatarColorHex = avatarColorHex,
                                cardNumber = cardNumber,
                                shebaNumber = shebaNumber,
                                phoneNumber = phoneNumber.trim(),
                                trustPriority = priority.name,
                                description = description.trim(),
                                createdAtJalali = initialEntity?.createdAtJalali ?: ""
                            )
                        )
                    }
                }
            ) {
                Text("ذخیره طرف‌حساب")
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
private fun EntityLedgerModal(
    entity: CounterpartyEntity,
    obligations: List<ObligationEntity>,
    installments: List<InstallmentEntity>,
    currencyUnit: com.example.core.CurrencyUnit,
    privacyMasked: Boolean,
    onDismiss: () -> Unit,
    onExportA4AndExcel: () -> Unit
) {
    val totalBorrowed = installments.sumOf { it.effectiveTotalToman }
    val totalRepaid = installments.sumOf { it.paidAmountToman }
    val remaining = installments.sumOf { it.remainingToman }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("پرونده جامع مالی: ${entity.name}", fontWeight = FontWeight.Bold)
                Text(
                    text = "${entity.categoryEnum.labelFa} • ${entity.priorityEnum.labelFa}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "مجموع تعهدات: ${PersianCurrencyEngine.formatAmount(totalBorrowed, currencyUnit, privacyMasked = privacyMasked)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "مجموع پرداخت‌شده: ${PersianCurrencyEngine.formatAmount(totalRepaid, currencyUnit, privacyMasked = privacyMasked)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "مانده نهایی حساب: ${PersianCurrencyEngine.formatAmount(remaining, currencyUnit, privacyMasked = privacyMasked)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = onExportA4AndExcel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("خروجی PDF استاندارد A4 و Excel صورتحساب این طرف‌حساب")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "لیست وام‌ها، خریدها و اقساط مرتبط (${PersianCurrencyEngine.toPersianDigits(installments.size.toString())} ردیف):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                if (obligations.isEmpty()) {
                    Text(
                        text = "هنوز هیچ وام یا بدهی برای این طرف‌حساب ثبت نشده است.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    installments.sortedBy { it.dueDateJalali }.forEach { inst ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = inst.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "سررسید: ${PersianCurrencyEngine.toPersianDigits(inst.dueDateJalali)} • ${inst.statusEnum.labelFa}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(inst.statusEnum.colorHex)
                                    )
                                }
                                Text(
                                    text = PersianCurrencyEngine.formatAmount(inst.effectiveTotalToman, currencyUnit, privacyMasked = privacyMasked),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن")
            }
        }
    )
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
}
