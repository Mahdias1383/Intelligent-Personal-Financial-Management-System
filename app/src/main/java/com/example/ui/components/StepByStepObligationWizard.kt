package com.example.ui.components

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditScore
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.CurrencyUnit
import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.data.CounterpartyEntity
import com.example.data.EntityCategory
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.ObligationCategory
import com.example.data.ObligationEntity
import com.example.data.PaymentInterval
import com.example.data.ScheduleMode
import com.example.data.TrustPriority
import com.example.domain.InstallmentScheduleEngine

data class EditableInstallmentDraft(
    val number: Int,
    val title: String,
    val dueDateJalali: String,
    val amountToman: Long,
    val isPaidAlready: Boolean = false,
    val note: String = ""
)

@Composable
fun StepByStepObligationWizardModal(
    counterparties: List<CounterpartyEntity>,
    preselectedCategory: EntityCategory?,
    preselectedCounterpartyId: Long?,
    currencyUnit: CurrencyUnit,
    onDismiss: () -> Unit,
    onCompleteWizard: (
        obligation: ObligationEntity,
        customInstallments: List<InstallmentEntity>,
        newCounterpartyToCreate: CounterpartyEntity?
    ) -> Unit
) {
    val initialCp = remember(counterparties, preselectedCounterpartyId) {
        counterparties.firstOrNull { it.id == preselectedCounterpartyId }
    }

    var currentStep by remember { mutableIntStateOf(1) } // 1, 2, or 3

    // Step 1 State: Parent Category & Sub-Provider (e.g., BNPL -> SnappPay / DigiPay)
    var selectedParentCategory by remember {
        mutableStateOf(
            initialCp?.categoryEnum ?: preselectedCategory ?: EntityCategory.BNPL_PLATFORM
        )
    }
    val matchingCounterparties = remember(counterparties, selectedParentCategory) {
        counterparties.filter { it.categoryEnum == selectedParentCategory }
    }
    var selectedCounterpartyId by remember(matchingCounterparties, preselectedCounterpartyId) {
        mutableLongStateOf(
            matchingCounterparties.firstOrNull { it.id == preselectedCounterpartyId }?.id
                ?: matchingCounterparties.firstOrNull()?.id
                ?: -1L
        )
    }
    var isCreatingNewSubProvider by remember { mutableStateOf(matchingCounterparties.isEmpty()) }
    var newSubProviderName by remember { mutableStateOf("") }
    var newSubProviderCard by remember { mutableStateOf("") }

    var obligationTitle by remember { mutableStateOf("") }
    var isReceivableMode by remember { mutableStateOf(false) }
    var isCheckOrRentMode by remember { mutableStateOf(false) }
    var priority by remember { mutableStateOf(TrustPriority.HIGH) }
    var trackingOrCheckCode by remember { mutableStateOf("") }

    // Step 2 State: Base Amount & Initial Split Parameters
    var principalAmountToman by remember { mutableLongStateOf(20_000_000L) }
    var showAdvancedFeeFields by remember { mutableStateOf(false) }
    var profitOrFeeToman by remember { mutableLongStateOf(0L) }
    var downPaymentToman by remember { mutableLongStateOf(0L) }
    var installmentCount by remember { mutableIntStateOf(4) }
    var paymentInterval by remember { mutableStateOf(PaymentInterval.MONTHLY) }
    var firstDueDateJalali by remember {
        mutableStateOf(JalaliCalendarEngine.firstOfNextJalaliMonth(JalaliCalendarEngine.todayJalali()))
    }

    // Step 3 State: Granular Editable Installment Rows
    val draftRows = remember { mutableStateListOf<EditableInstallmentDraft>() }
    var editingRowIndex by remember { mutableStateOf<Int?>(null) }
    var pickingDateForRowIndex by remember { mutableStateOf<Int?>(null) }

    val netPayableToman = (principalAmountToman + profitOrFeeToman - downPaymentToman).coerceAtLeast(0L)

    fun regenerateDraftRowsFromStep2() {
        val safeTitle = obligationTitle.trim().ifBlank {
            when (selectedParentCategory) {
                EntityCategory.BNPL_PLATFORM -> "خرید اقساطی"
                EntityCategory.BANK -> "وام بانکی"
                EntityCategory.INSTITUTION -> "وام صندوق"
                EntityCategory.PERSON -> if (isCheckOrRentMode) "چک صیادی" else "قرض شخصی"
            }
        }
        val generated = InstallmentScheduleEngine.generateAutoInstallments(
            obligationId = 0L,
            titlePrefix = safeTitle,
            netPayableToman = netPayableToman,
            installmentCount = installmentCount.coerceAtLeast(1),
            firstDueDateJalali = firstDueDateJalali,
            interval = paymentInterval
        )
        draftRows.clear()
        generated.forEach { inst ->
            draftRows.add(
                EditableInstallmentDraft(
                    number = inst.installmentNumber,
                    title = inst.title,
                    dueDateJalali = inst.dueDateJalali,
                    amountToman = inst.amountToman,
                    isPaidAlready = false,
                    note = ""
                )
            )
        }
    }

    fun redistributeAmountsEqually() {
        if (draftRows.isEmpty()) return
        val count = draftRows.size
        val base = netPayableToman / count
        val rem = netPayableToman % count
        for (i in draftRows.indices) {
            val row = draftRows[i]
            draftRows[i] = row.copy(
                number = i + 1,
                amountToman = if (i == count - 1) base + rem else base
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .testTag("step_by_step_obligation_wizard"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Wizard Progress Header
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "ثبت هوشمند قسط، خرید اقساطی یا وام",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = when (currentStep) {
                                        1 -> "گام ۱ از ۳: انتخاب دسته اصلی و زیرمجموعه پرداخت (مثلاً اسنپ‌پی)"
                                        2 -> "گام ۲ از ۳: تعیین مبلغ کل، تعداد اقساط و اولین سررسید"
                                        else -> "گام ۳ از ۳: بررسی و ویرایش تفکیکی تک‌تک قسط‌ها"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "بستن ویزارد")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3-Step Visual Stepper Indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                1 to "۱. دسته و طرف پرداخت",
                                2 to "۲. مبلغ و الگو",
                                3 to "۳. جدول تک‌تک اقساط"
                            ).forEach { (stepNum, stepTitle) ->
                                val isActive = currentStep == stepNum
                                val isCompleted = currentStep > stepNum
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            when {
                                                isActive -> MaterialTheme.colorScheme.primary
                                                isCompleted -> MaterialTheme.colorScheme.primaryContainer
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        )
                                        .padding(vertical = 8.dp, horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = stepTitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isActive || isCompleted) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isActive -> MaterialTheme.colorScheme.onPrimary
                                            isCompleted -> MaterialTheme.colorScheme.onPrimaryContainer
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Wizard Step Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (currentStep) {
                        1 -> WizardStep1CategoryAndProvider(
                            selectedCategory = selectedParentCategory,
                            onSelectCategory = { cat ->
                                selectedParentCategory = cat
                                val firstMatch = counterparties.firstOrNull { it.categoryEnum == cat }
                                if (firstMatch != null) {
                                    selectedCounterpartyId = firstMatch.id
                                    isCreatingNewSubProvider = false
                                } else {
                                    isCreatingNewSubProvider = true
                                }
                                if (cat == EntityCategory.BNPL_PLATFORM) {
                                    installmentCount = 4
                                }
                            },
                            matchingProviders = matchingCounterparties,
                            selectedProviderId = selectedCounterpartyId,
                            isCreatingNewProvider = isCreatingNewSubProvider,
                            onSelectExistingProvider = { cpId ->
                                selectedCounterpartyId = cpId
                                isCreatingNewSubProvider = false
                            },
                            onToggleCreateNewProvider = {
                                isCreatingNewSubProvider = true
                            },
                            newProviderName = newSubProviderName,
                            onNewProviderNameChange = { newSubProviderName = it },
                            newProviderCard = newSubProviderCard,
                            onNewProviderCardChange = { newSubProviderCard = it },
                            obligationTitle = obligationTitle,
                            onObligationTitleChange = { obligationTitle = it },
                            isReceivable = isReceivableMode,
                            isCheckOrRent = isCheckOrRentMode,
                            onTypeFlagChange = { rec, chk ->
                                isReceivableMode = rec
                                isCheckOrRentMode = chk
                            },
                            priority = priority,
                            onPriorityChange = { priority = it }
                        )

                        2 -> WizardStep2AmountAndTemplate(
                            principalAmountToman = principalAmountToman,
                            onPrincipalChange = { principalAmountToman = it },
                            showAdvancedFeeFields = showAdvancedFeeFields,
                            onToggleAdvancedFee = { showAdvancedFeeFields = !showAdvancedFeeFields },
                            profitOrFeeToman = profitOrFeeToman,
                            onProfitChange = { profitOrFeeToman = it },
                            downPaymentToman = downPaymentToman,
                            onDownPaymentChange = { downPaymentToman = it },
                            installmentCount = installmentCount,
                            onInstallmentCountChange = { installmentCount = it },
                            interval = paymentInterval,
                            onIntervalChange = { paymentInterval = it },
                            firstDueDateJalali = firstDueDateJalali,
                            onFirstDueDateChange = { firstDueDateJalali = it },
                            netPayableToman = netPayableToman,
                            currencyUnit = currencyUnit
                        )

                        3 -> WizardStep3GranularInstallmentTable(
                            draftRows = draftRows,
                            targetNetPayableToman = netPayableToman,
                            currencyUnit = currencyUnit,
                            onRedistributeEqually = { redistributeAmountsEqually() },
                            onAddCustomRow = {
                                val lastRow = draftRows.lastOrNull()
                                val nextNum = draftRows.size + 1
                                val nextDate = if (lastRow != null) {
                                    JalaliCalendarEngine.addJalaliMonths(lastRow.dueDateJalali, 1)
                                } else {
                                    firstDueDateJalali
                                }
                                val numFa = PersianCurrencyEngine.toPersianDigits(nextNum.toString())
                                draftRows.add(
                                    EditableInstallmentDraft(
                                        number = nextNum,
                                        title = "${obligationTitle.ifBlank { "قسط" }} — قسط $numFa",
                                        dueDateJalali = nextDate,
                                        amountToman = lastRow?.amountToman ?: 1_000_000L
                                    )
                                )
                            },
                            onOpenEditRowModal = { idx -> editingRowIndex = idx },
                            onOpenDatePickerForRow = { idx -> pickingDateForRowIndex = idx },
                            onToggleRowPaidStatus = { idx ->
                                val r = draftRows[idx]
                                draftRows[idx] = r.copy(isPaidAlready = !r.isPaidAlready)
                            },
                            onDeleteRow = { idx ->
                                if (draftRows.size > 1) {
                                    draftRows.removeAt(idx)
                                    for (i in draftRows.indices) {
                                        draftRows[i] = draftRows[i].copy(number = i + 1)
                                    }
                                }
                            }
                        )
                    }
                }

                // Bottom Wizard Navigation Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStep > 1) {
                            OutlinedButton(
                                onClick = { currentStep-- },
                                modifier = Modifier.testTag("wizard_prev_step_btn")
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مرحله قبل")
                            }
                        } else {
                            TextButton(onClick = onDismiss) {
                                Text("انصراف")
                            }
                        }

                        Button(
                            onClick = {
                                when (currentStep) {
                                    1 -> currentStep = 2
                                    2 -> {
                                        regenerateDraftRowsFromStep2()
                                        currentStep = 3
                                    }
                                    3 -> {
                                        val today = JalaliCalendarEngine.todayJalali()
                                        val finalTitle = obligationTitle.trim().ifBlank {
                                            when (selectedParentCategory) {
                                                EntityCategory.BNPL_PLATFORM -> "خرید اقساطی جدید"
                                                EntityCategory.BANK -> "وام بانکی جدید"
                                                EntityCategory.INSTITUTION -> "وام صندوق قرض‌الحسنه"
                                                EntityCategory.PERSON -> if (isCheckOrRentMode) "چک صیادی" else "قرض شخصی"
                                            }
                                        }
                                        val obCategory = when {
                                            isReceivableMode -> ObligationCategory.PERSONAL_RECEIVABLE
                                            isCheckOrRentMode -> ObligationCategory.CHECK_OR_BILL
                                            selectedParentCategory == EntityCategory.BNPL_PLATFORM -> ObligationCategory.BNPL_PURCHASE
                                            selectedParentCategory == EntityCategory.PERSON -> ObligationCategory.PERSONAL_DEBT
                                            else -> ObligationCategory.INSTALLMENT_LOAN
                                        }

                                        val newCp = if (isCreatingNewSubProvider || selectedCounterpartyId <= 0L) {
                                            CounterpartyEntity(
                                                name = newSubProviderName.trim().ifBlank { selectedParentCategory.badgeFa },
                                                category = selectedParentCategory.name,
                                                avatarColorHex = selectedParentCategory.defaultColorHex,
                                                cardNumber = newSubProviderCard,
                                                trustPriority = priority.name
                                            )
                                        } else null

                                        val finalInstallments = draftRows.mapIndexed { idx, row ->
                                            val isOverdue = !row.isPaidAlready &&
                                                JalaliCalendarEngine.diffJalaliDays(today, row.dueDateJalali) < 0
                                            val st = when {
                                                row.isPaidAlready -> InstallmentStatus.PAID
                                                isOverdue -> InstallmentStatus.OVERDUE
                                                else -> InstallmentStatus.PENDING
                                            }
                                            InstallmentEntity(
                                                obligationId = 0L,
                                                installmentNumber = idx + 1,
                                                title = row.title.ifBlank { "$finalTitle — قسط ${idx + 1}" },
                                                dueDateJalali = row.dueDateJalali,
                                                amountToman = row.amountToman,
                                                paidAmountToman = if (row.isPaidAlready) row.amountToman else 0L,
                                                status = st.name,
                                                paidDateJalali = if (row.isPaidAlready) row.dueDateJalali else "",
                                                note = row.note
                                            )
                                        }

                                        val ob = ObligationEntity(
                                            counterpartyId = selectedCounterpartyId.coerceAtLeast(1L),
                                            title = finalTitle,
                                            category = obCategory.name,
                                            scheduleMode = ScheduleMode.AUTO_SCHEDULE.name,
                                            principalAmountToman = principalAmountToman,
                                            profitOrFeeToman = profitOrFeeToman,
                                            downPaymentToman = downPaymentToman,
                                            installmentCount = finalInstallments.size,
                                            paymentInterval = paymentInterval.name,
                                            firstDueDateJalali = finalInstallments.firstOrNull()?.dueDateJalali ?: firstDueDateJalali,
                                            trustPriority = priority.name,
                                            trackingOrCheckCode = trackingOrCheckCode.trim()
                                        )

                                        onCompleteWizard(ob, finalInstallments, newCp)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentStep == 3) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("wizard_next_or_save_btn")
                        ) {
                            Text(
                                text = when (currentStep) {
                                    1 -> "ادامه به تنظیم مبلغ"
                                    2 -> "مشاهده و ویرایش تک‌تک اقساط"
                                    else -> "ثبت نهایی (${PersianCurrencyEngine.toPersianDigits(draftRows.size.toString())} قسط)"
                                },
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    // Modal for editing a single installment row in Step 3
    editingRowIndex?.let { rowIdx ->
        val row = draftRows.getOrNull(rowIdx)
        if (row != null) {
            var editedTitle by remember(row) { mutableStateOf(row.title) }
            var editedAmount by remember(row) { mutableLongStateOf(row.amountToman) }
            var editedDate by remember(row) { mutableStateOf(row.dueDateJalali) }
            var editedNote by remember(row) { mutableStateOf(row.note) }

            AlertDialog(
                onDismissRequest = { editingRowIndex = null },
                title = {
                    Text(
                        text = "ویرایش اختصاصی قسط شماره ${PersianCurrencyEngine.toPersianDigits(row.number.toString())}",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = editedTitle,
                            onValueChange = { editedTitle = it },
                            label = { Text("عنوان این قسط") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        MoneyInputField(
                            labelFa = "مبلغ دقیق این قسط",
                            amountToman = editedAmount,
                            onAmountChange = { editedAmount = it },
                            currencyUnit = currencyUnit
                        )

                        JalaliDateSelectorField(
                            labelFa = "تاریخ سررسید این قسط",
                            dateJalali = editedDate,
                            onDateSelected = { editedDate = it }
                        )

                        OutlinedTextField(
                            value = editedNote,
                            onValueChange = { editedNote = it },
                            label = { Text("توضیح یا شماره چک/رهگیری این قسط") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            draftRows[rowIdx] = row.copy(
                                title = editedTitle.trim(),
                                amountToman = editedAmount,
                                dueDateJalali = editedDate,
                                note = editedNote.trim()
                            )
                            editingRowIndex = null
                        }
                    ) {
                        Text("تایید تغییرات این قسط")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingRowIndex = null }) {
                        Text("انصراف")
                    }
                }
            )
        }
    }

    // Direct Jalali Date Picker for a specific installment row in Step 3
    pickingDateForRowIndex?.let { rowIdx ->
        val row = draftRows.getOrNull(rowIdx)
        if (row != null) {
            JalaliDatePickerModal(
                initialDateJalali = row.dueDateJalali,
                onDismiss = { pickingDateForRowIndex = null },
                onConfirmDate = { newDate ->
                    draftRows[rowIdx] = row.copy(dueDateJalali = newDate)
                    pickingDateForRowIndex = null
                }
            )
        }
    }
}

@Composable
private fun WizardStep1CategoryAndProvider(
    selectedCategory: EntityCategory,
    onSelectCategory: (EntityCategory) -> Unit,
    matchingProviders: List<CounterpartyEntity>,
    selectedProviderId: Long,
    isCreatingNewProvider: Boolean,
    onSelectExistingProvider: (Long) -> Unit,
    onToggleCreateNewProvider: () -> Unit,
    newProviderName: String,
    onNewProviderNameChange: (String) -> Unit,
    newProviderCard: String,
    onNewProviderCardChange: (String) -> Unit,
    obligationTitle: String,
    onObligationTitleChange: (String) -> Unit,
    isReceivable: Boolean,
    isCheckOrRent: Boolean,
    onTypeFlagChange: (Boolean, Boolean) -> Unit,
    priority: TrustPriority,
    onPriorityChange: (TrustPriority) -> Unit
) {
    val quickSubProviderPresets = when (selectedCategory) {
        EntityCategory.BNPL_PLATFORM -> listOf("اسنپ‌پی (SnappPay)", "دیجی‌پی (DigiPay)", "تارا (Tara)", "ازکی‌وام (AzkiVam)", "لندو (Lendo)")
        EntityCategory.BANK -> listOf("بانک مهر ایران", "بانک ملت", "بلوبانک", "بانک ملی", "بانک رسالت", "بانک مسکن")
        EntityCategory.INSTITUTION -> listOf("صندوق قرض‌الحسنه امید", "تعاونی اداره", "شرکت کارفرما")
        EntityCategory.PERSON -> listOf("دوست / همکار", "صاحب‌خانه (اجاره/چک)", "اعضای خانواده")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Select Parent Category
        item {
            Text(
                text = "۱. انتخاب دسته اصلی:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ParentCategoryChoiceCard(
                    title = "خرید اقساطی (BNPL)",
                    subtitle = "اسنپ‌پی، دیجی‌پی، تارا...",
                    icon = Icons.Default.CreditScore,
                    color = Color(0xFF3B82F6),
                    isSelected = selectedCategory == EntityCategory.BNPL_PLATFORM,
                    onClick = { onSelectCategory(EntityCategory.BNPL_PLATFORM) },
                    modifier = Modifier.weight(1f)
                )
                ParentCategoryChoiceCard(
                    title = "بانک‌ها و تسهیلات",
                    subtitle = "مهر ایران، ملت، بلوبانک...",
                    icon = Icons.Default.AccountBalance,
                    color = Color(0xFF10B981),
                    isSelected = selectedCategory == EntityCategory.BANK,
                    onClick = { onSelectCategory(EntityCategory.BANK) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ParentCategoryChoiceCard(
                    title = "صندوق و موسسات",
                    subtitle = "صندوق خانگی، شرکت...",
                    icon = Icons.Default.Groups,
                    color = Color(0xFF8B5CF6),
                    isSelected = selectedCategory == EntityCategory.INSTITUTION,
                    onClick = { onSelectCategory(EntityCategory.INSTITUTION) },
                    modifier = Modifier.weight(1f)
                )
                ParentCategoryChoiceCard(
                    title = "اشخاص، قرض و چک",
                    subtitle = "دوست، صاحب‌خانه، چک...",
                    icon = Icons.Default.Person,
                    color = Color(0xFFF59E0B),
                    isSelected = selectedCategory == EntityCategory.PERSON,
                    onClick = { onSelectCategory(EntityCategory.PERSON) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2. Select or Create Sub-Category Provider (e.g. SnappPay, DigiPay)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "۲. انتخاب زیرمجموعه / طرف پرداخت در دسته «${selectedCategory.badgeFa}»:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        matchingProviders.forEach { cp ->
                            FilterChip(
                                selected = !isCreatingNewProvider && selectedProviderId == cp.id,
                                onClick = { onSelectExistingProvider(cp.id) },
                                label = { Text(cp.name, fontWeight = FontWeight.SemiBold) }
                            )
                        }
                        FilterChip(
                            selected = isCreatingNewProvider,
                            onClick = onToggleCreateNewProvider,
                            leadingIcon = { Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text("زیرمجموعه جدید") }
                        )
                    }

                    AnimatedVisibility(visible = isCreatingNewProvider) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Text(
                                text = "پیشنهادهای سریع زیرمجموعه:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                quickSubProviderPresets.forEach { preset ->
                                    AssistChip(
                                        onClick = { onNewProviderNameChange(preset) },
                                        label = { Text(preset) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newProviderName,
                                onValueChange = onNewProviderNameChange,
                                label = { Text("نام زیرمجموعه (مثلاً اسنپ‌پی، دیجی‌پی، بانک ملت...)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newProviderCard,
                                onValueChange = {
                                    onNewProviderCardChange(
                                        PersianCurrencyEngine.toEnglishDigits(it).filter { c -> c.isDigit() }.take(16)
                                    )
                                },
                                label = { Text("شماره کارت ۱۶ رقمی مقصد (اختیاری)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // 3. Obligation Title & Priority
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "۳. عنوان خرید یا تعهد مالی:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = obligationTitle,
                        onValueChange = onObligationTitleChange,
                        label = { Text("عنوان (مثلاً خرید گوشی، لپ‌تاپ، وام ازدواج، چک اجاره...)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wizard_obligation_title_input")
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        FilterChip(
                            selected = !isReceivable && !isCheckOrRent,
                            onClick = { onTypeFlagChange(false, false) },
                            label = { Text("بدهی / اقساط پرداختنی") }
                        )
                        FilterChip(
                            selected = isCheckOrRent,
                            onClick = { onTypeFlagChange(false, true) },
                            label = { Text("چک صیادی / اجاره‌بها") }
                        )
                        FilterChip(
                            selected = isReceivable,
                            onClick = { onTypeFlagChange(true, false) },
                            label = { Text("طلب دریافتنی از دیگران") }
                        )
                    }

                    Text("درجه حساسیت خوش‌حسابی:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        TrustPriority.entries.forEach { prio ->
                            FilterChip(
                                selected = priority == prio,
                                onClick = { onPriorityChange(prio) },
                                label = { Text(prio.labelFa) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParentCategoryChoiceCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
        modifier = modifier
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.2f))
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WizardStep2AmountAndTemplate(
    principalAmountToman: Long,
    onPrincipalChange: (Long) -> Unit,
    showAdvancedFeeFields: Boolean,
    onToggleAdvancedFee: () -> Unit,
    profitOrFeeToman: Long,
    onProfitChange: (Long) -> Unit,
    downPaymentToman: Long,
    onDownPaymentChange: (Long) -> Unit,
    installmentCount: Int,
    onInstallmentCountChange: (Int) -> Unit,
    interval: PaymentInterval,
    onIntervalChange: (PaymentInterval) -> Unit,
    firstDueDateJalali: String,
    onFirstDueDateChange: (String) -> Unit,
    netPayableToman: Long,
    currencyUnit: CurrencyUnit
) {
    val perInstallmentPreview = if (installmentCount > 0) netPayableToman / installmentCount else netPayableToman

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    MoneyInputField(
                        labelFa = "مبلغ کل خرید، وام یا تعهد",
                        amountToman = principalAmountToman,
                        onAmountChange = onPrincipalChange,
                        currencyUnit = currencyUnit
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = onToggleAdvancedFee) {
                        Text(
                            text = if (showAdvancedFeeFields) {
                                "بستن بخش پیش‌پرداخت و سود/کارمزد"
                            } else {
                                "+ افزودن پیش‌پرداخت یا کارمزد/سود (اختیاری)"
                            }
                        )
                    }

                    AnimatedVisibility(visible = showAdvancedFeeFields) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            MoneyInputField(
                                labelFa = "مبلغ پیش‌پرداخت (کسر از کل)",
                                amountToman = downPaymentToman,
                                onAmountChange = onDownPaymentChange,
                                currencyUnit = currencyUnit,
                                showQuickChips = false
                            )
                            MoneyInputField(
                                labelFa = "مبلغ کارمزد یا سود کل (اضافه به کل)",
                                amountToman = profitOrFeeToman,
                                onAmountChange = onProfitChange,
                                currencyUnit = currencyUnit,
                                showQuickChips = false
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "تعداد اقساط (در مرحله بعد تک‌تک اقساط قابل ویرایش هستند):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        listOf(1, 2, 3, 4, 6, 8, 10, 12, 18, 24, 36).forEach { count ->
                            FilterChip(
                                selected = installmentCount == count,
                                onClick = { onInstallmentCountChange(count) },
                                label = {
                                    Text(
                                        text = "${PersianCurrencyEngine.toPersianDigits(count.toString())} قسط",
                                        fontWeight = if (installmentCount == count) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }

                    Text("فاصله زمانی پیش‌فرض بین اقساط:", style = MaterialTheme.typography.labelLarge)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        PaymentInterval.entries.forEach { iv ->
                            FilterChip(
                                selected = interval == iv,
                                onClick = { onIntervalChange(iv) },
                                label = { Text(iv.labelFa) }
                            )
                        }
                    }

                    JalaliDateSelectorField(
                        labelFa = "تاریخ اولین سررسید شمسی (قسط اول)",
                        dateJalali = firstDueDateJalali,
                        onDateSelected = onFirstDueDateChange
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "مبلغ پایه هر قسط: ${PersianCurrencyEngine.formatAmount(perInstallmentPreview, currencyUnit)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "در مرحله بعد می‌توانید مبلغ یا تاریخ هر قسط را به صورت تکی نیز تغییر دهید.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WizardStep3GranularInstallmentTable(
    draftRows: List<EditableInstallmentDraft>,
    targetNetPayableToman: Long,
    currencyUnit: CurrencyUnit,
    onRedistributeEqually: () -> Unit,
    onAddCustomRow: () -> Unit,
    onOpenEditRowModal: (Int) -> Unit,
    onOpenDatePickerForRow: (Int) -> Unit,
    onToggleRowPaidStatus: (Int) -> Unit,
    onDeleteRow: (Int) -> Unit
) {
    val currentSumToman = draftRows.sumOf { it.amountToman }
    val diffToman = currentSumToman - targetNetPayableToman

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("wizard_step3_installment_table"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Summary & Quick Table Controls Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "مجموع ${PersianCurrencyEngine.toPersianDigits(draftRows.size.toString())} قسط جدول:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = PersianCurrencyEngine.formatAmount(currentSumToman, currencyUnit),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (diffToman != 0L) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "اختلاف با مبلغ پایه: ${PersianCurrencyEngine.formatAmount(kotlin.math.abs(diffToman), currencyUnit)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFF59E0B),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "تراز دقیق (بدون اختلاف)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onAddCustomRow,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("افزودن ردیف قسط")
                        }
                        OutlinedButton(
                            onClick = onRedistributeEqually,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Balance, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("توزیع مساوی مبالغ")
                        }
                    }
                }
            }
        }

        // Individual Editable Installment Rows
        itemsIndexed(draftRows) { idx, row ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (row.isPaidAlready) Color(0xFF10B981).copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp)
                    )
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
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Text(
                                    text = PersianCurrencyEngine.toPersianDigits(row.number.toString()),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = row.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = PersianCurrencyEngine.amountToPersianWords(row.amountToman, currencyUnit),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (draftRows.size > 1) {
                            IconButton(onClick = { onDeleteRow(idx) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "حذف این قسط",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Interactive Chips for Editing Due Date, Amount, and Paid Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Date Selector Pill
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.clickable { onOpenDatePickerForRow(idx) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = PersianCurrencyEngine.toPersianDigits(row.dueDateJalali),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Amount Editor Pill
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                            modifier = Modifier.clickable { onOpenEditRowModal(idx) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = PersianCurrencyEngine.formatAmount(row.amountToman, currencyUnit),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "ویرایش مبلغ و جزئیات قسط",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Already Paid Toggle Pill
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (row.isPaidAlready) Color(0xFF10B981).copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.clickable { onToggleRowPaidStatus(idx) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (row.isPaidAlready) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (row.isPaidAlready) "پرداخت‌شده" else "در انتظار",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (row.isPaidAlready) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
