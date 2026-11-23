package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.theme.LocalTarazinoColors

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
        val ext = LocalTarazinoColors.current
        val animStepProgress by animateFloatAsState(
            targetValue = currentStep / 3f,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
            label = "WizardStepProgress"
        )

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(10.dp)
                .testTag("step_by_step_obligation_wizard"),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(
                1.dp,
                Brush.verticalGradient(listOf(ext.cardBorderTop, ext.cardBorderBottom))
            ),
            shadowElevation = 18.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .tarazinoAmbientBackdrop(
                        backgroundColor = MaterialTheme.colorScheme.background,
                        auroraPrimary = ext.auroraPrimary,
                        auroraSecondary = ext.auroraSecondary
                    )
            ) {
                // Top Titanium Wizard Progress Header
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = if (ext.isDark) 0.90f else 0.96f),
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
                                TarazinoIconBadge(
                                    icon = Icons.Default.CreditScore,
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    size = 42.dp,
                                    iconSize = 21.dp,
                                    cornerRadius = 14.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    TarazinoAutoSizeText(
                                        text = "ثبت هوشمند قسط، خرید یا وام",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        minFontSize = 11.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    TarazinoAutoSizeText(
                                        text = when (currentStep) {
                                            1 -> "گام ۱ از ۳: انتخاب دسته اصلی و طرف پرداخت"
                                            2 -> "گام ۲ از ۳: تعیین مبلغ کل، تعداد و سررسید"
                                            else -> "گام ۳ از ۳: ویرایش تفکیکی تک‌تک اقساط"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        minFontSize = 8.5.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                IconButton(onClick = onDismiss) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "بستن ویزارد",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3-Step Visual Stepper Indicator
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(0.9.dp, ext.cardBorderTop),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    1 to "۱. دسته و طرف",
                                    2 to "۲. مبلغ و الگو",
                                    3 to "۳. جدول اقساط"
                                ).forEach { (stepNum, stepTitle) ->
                                    val isActive = currentStep == stepNum
                                    val isCompleted = currentStep > stepNum
                                    val pillBg by animateColorAsState(
                                        targetValue = when {
                                            isActive -> MaterialTheme.colorScheme.primary
                                            isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                            else -> Color.Transparent
                                        },
                                        label = "StepPillBg_$stepNum"
                                    )
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(pillBg)
                                            .padding(vertical = 8.dp, horizontal = 4.dp)
                                    ) {
                                        TarazinoAutoSizeText(
                                            text = stepTitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isActive || isCompleted) FontWeight.Bold else FontWeight.Medium,
                                            color = when {
                                                isActive -> MaterialTheme.colorScheme.onPrimary
                                                isCompleted -> MaterialTheme.colorScheme.primary
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            maxLines = 1,
                                            minFontSize = 8.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { animStepProgress },
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }
                }

                // Animated Wizard Step Body (Smooth Directional Slide + Fade)
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        val forward = targetState > initialState
                        if (forward) {
                            (slideInHorizontally(tween(260)) { -it / 4 } + fadeIn(tween(240)))
                                .togetherWith(slideOutHorizontally(tween(200)) { it / 5 } + fadeOut(tween(180)))
                        } else {
                            (slideInHorizontally(tween(260)) { it / 4 } + fadeIn(tween(240)))
                                .togetherWith(slideOutHorizontally(tween(200)) { -it / 5 } + fadeOut(tween(180)))
                        }
                    },
                    label = "WizardStepContent",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { step ->
                    when (step) {
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

                        else -> WizardStep3GranularInstallmentTable(
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

                // Bottom Sticky Glass Action Footer
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = if (ext.isDark) 0.94f else 0.98f),
                    border = BorderStroke(0.8.dp, ext.cardBorderTop),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStep > 1) {
                            OutlinedButton(
                                onClick = { currentStep-- },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier.testTag("wizard_prev_step_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                TarazinoAutoSizeText(
                                    text = "مرحله قبل",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    minFontSize = 9.5.sp
                                )
                            }
                        } else {
                            TextButton(onClick = onDismiss) {
                                Text("انصراف", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

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
                            shape = RoundedCornerShape(15.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 11.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentStep == 3) ext.positive else MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("wizard_next_or_save_btn")
                        ) {
                            TarazinoAutoSizeText(
                                text = when (currentStep) {
                                    1 -> "ادامه به تنظیم مبلغ"
                                    2 -> "مشاهده جدول اقساط"
                                    else -> "ثبت نهایی (${PersianCurrencyEngine.toPersianDigits(draftRows.size.toString())} قسط)"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (currentStep == 3) ext.onPositive else MaterialTheme.colorScheme.onPrimary,
                                maxLines = 1,
                                minFontSize = 9.5.sp,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (currentStep == 3) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = if (currentStep == 3) ext.onPositive else MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Specular Glass Modal for editing a single installment row in Step 3
    editingRowIndex?.let { rowIdx ->
        val row = draftRows.getOrNull(rowIdx)
        if (row != null) {
            var editedTitle by remember(row) { mutableStateOf(row.title) }
            var editedAmount by remember(row) { mutableLongStateOf(row.amountToman) }
            var editedDate by remember(row) { mutableStateOf(row.dueDateJalali) }
            var editedNote by remember(row) { mutableStateOf(row.note) }

            Dialog(
                onDismissRequest = { editingRowIndex = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                TarazinoGlassCard(
                    cornerRadius = 26.dp,
                    accentBorderColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth(0.93f)
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TarazinoSectionHeader(
                            title = "ویرایش قسط شماره ${PersianCurrencyEngine.toPersianDigits(row.number.toString())}",
                            subtitle = "تنظیم دقیق مبلغ، تاریخ سررسید و توضیحات این ردیف",
                            icon = Icons.Default.Edit,
                            accentColor = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = editedTitle,
                            onValueChange = { editedTitle = it },
                            label = { Text("عنوان این قسط") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
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
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { editingRowIndex = null },
                                shape = RoundedCornerShape(13.dp),
                                modifier = Modifier.weight(0.8f)
                            ) {
                                Text("انصراف")
                            }
                            Button(
                                onClick = {
                                    draftRows[rowIdx] = row.copy(
                                        title = editedTitle.trim(),
                                        amountToman = editedAmount,
                                        dueDateJalali = editedDate,
                                        note = editedNote.trim()
                                    )
                                    editingRowIndex = null
                                },
                                shape = RoundedCornerShape(13.dp),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Text("تایید تغییرات", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
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
    val ext = LocalTarazinoColors.current
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
        // 1. Select Parent Category (2x2 Tactile Bento Cards)
        item {
            Text(
                text = "۱. انتخاب دسته اصلی",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ParentCategoryChoiceCard(
                    title = "خرید اقساطی (BNPL)",
                    subtitle = "اسنپ‌پی، دیجی‌پی، تارا...",
                    icon = Icons.Default.CreditScore,
                    color = ext.info,
                    isSelected = selectedCategory == EntityCategory.BNPL_PLATFORM,
                    onClick = { onSelectCategory(EntityCategory.BNPL_PLATFORM) },
                    modifier = Modifier.weight(1f)
                )
                ParentCategoryChoiceCard(
                    title = "بانک‌ها و تسهیلات",
                    subtitle = "مهر ایران، ملت، بلوبانک...",
                    icon = Icons.Default.AccountBalance,
                    color = ext.positive,
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
                    color = Color(EntityCategory.INSTITUTION.defaultColorHex),
                    isSelected = selectedCategory == EntityCategory.INSTITUTION,
                    onClick = { onSelectCategory(EntityCategory.INSTITUTION) },
                    modifier = Modifier.weight(1f)
                )
                ParentCategoryChoiceCard(
                    title = "اشخاص، قرض و چک",
                    subtitle = "دوست، صاحب‌خانه، چک...",
                    icon = Icons.Default.Person,
                    color = ext.warning,
                    isSelected = selectedCategory == EntityCategory.PERSON,
                    onClick = { onSelectCategory(EntityCategory.PERSON) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2. Select or Create Sub-Category Provider with Branded Monogram Pills
        item {
            TarazinoGlassCard(cornerRadius = 22.dp) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "۲. زیرمجموعه / طرف پرداخت در «${selectedCategory.badgeFa}»",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        matchingProviders.forEach { cp ->
                            val isSelected = !isCreatingNewProvider && selectedProviderId == cp.id
                            val cpAccent = Color(cp.avatarColorHex)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) cpAccent.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 0.8.dp,
                                    color = if (isSelected) cpAccent else ext.cardBorderTop
                                ),
                                modifier = Modifier.tarazinoPressScale(
                                    scaleDown = 0.94f,
                                    onClick = { onSelectExistingProvider(cp.id) }
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    CounterpartyMonogramAvatar(
                                        name = cp.name,
                                        color = cpAccent,
                                        size = 24.dp
                                    )
                                    Spacer(modifier = Modifier.width(7.dp))
                                    Text(
                                        text = cp.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (isSelected) cpAccent else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isCreatingNewProvider) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(
                                width = if (isCreatingNewProvider) 1.5.dp else 0.8.dp,
                                color = if (isCreatingNewProvider) MaterialTheme.colorScheme.primary else ext.cardBorderTop
                            ),
                            modifier = Modifier.tarazinoPressScale(
                                scaleDown = 0.94f,
                                onClick = onToggleCreateNewProvider
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "زیرمجموعه جدید",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = isCreatingNewProvider) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Text(
                                text = "پیشنهادهای سریع:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                quickSubProviderPresets.forEach { preset ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                                        modifier = Modifier.tarazinoPressScale(
                                            scaleDown = 0.95f,
                                            onClick = { onNewProviderNameChange(preset) }
                                        )
                                    ) {
                                        Text(
                                            text = preset,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = newProviderName,
                                onValueChange = onNewProviderNameChange,
                                label = { Text("نام زیرمجموعه", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                placeholder = { Text("مثلاً اسنپ‌پی، دیجی‌پی، بانک ملت...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
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
                                label = { Text("شماره کارت ۱۶ رقمی (اختیاری)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                placeholder = { Text("مثلاً ۶۰۳۷۹۹۷۵۰۰۰۰۰۰۰۰", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // 3. Obligation Title, Type & Priority (Sliding Segmented Controls)
        item {
            TarazinoGlassCard(cornerRadius = 22.dp) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "۳. عنوان خرید یا تعهد مالی",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    OutlinedTextField(
                        value = obligationTitle,
                        onValueChange = onObligationTitleChange,
                        label = { Text("عنوان خرید یا تعهد", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("مثلاً خرید گوشی، لپ‌تاپ، وام ازدواج، چک اجاره...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wizard_obligation_title_input")
                    )

                    val selectedTypeIdx = when {
                        isReceivable -> 2
                        isCheckOrRent -> 1
                        else -> 0
                    }
                    TarazinoSegmentedControl(
                        options = listOf("بدهی / قسط", "چک / اجاره", "طلب از دیگران"),
                        selectedIndex = selectedTypeIdx,
                        onSelectIndex = { idx ->
                            when (idx) {
                                0 -> onTypeFlagChange(false, false)
                                1 -> onTypeFlagChange(false, true)
                                else -> onTypeFlagChange(true, false)
                            }
                        },
                        compact = true
                    )

                    Text(
                        text = "درجه حساسیت خوش‌حسابی:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TarazinoSegmentedControl(
                        options = TrustPriority.entries.map {
                            when (it) {
                                TrustPriority.CRITICAL -> "حیاتی و چک"
                                TrustPriority.HIGH -> "اولویت بالا"
                                TrustPriority.NORMAL -> "عادی"
                            }
                        },
                        selectedIndex = TrustPriority.entries.indexOf(priority).coerceAtLeast(0),
                        onSelectIndex = { idx -> onPriorityChange(TrustPriority.entries[idx]) },
                        compact = true
                    )
                }
            }
        }
    }
}

@Composable
private fun ParentCategoryChoiceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTarazinoColors.current
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) color else ext.cardBorderTop,
        label = "WizardCatBorder"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) color.copy(alpha = if (ext.isDark) 0.20f else 0.12f)
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (isSelected) 1.8.dp else 0.9.dp, borderColor),
        modifier = modifier.tarazinoPressScale(scaleDown = 0.95f, onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TarazinoIconBadge(
                    icon = icon,
                    accentColor = color,
                    size = 34.dp,
                    iconSize = 18.dp,
                    cornerRadius = 11.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                TarazinoAutoSizeText(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    minFontSize = 9.sp,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(7.dp))
            TarazinoAutoSizeText(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                minFontSize = 8.sp,
                modifier = Modifier.fillMaxWidth()
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
    val ext = LocalTarazinoColors.current
    val perInstallmentPreview = if (installmentCount > 0) netPayableToman / installmentCount else netPayableToman

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TarazinoGlassCard(cornerRadius = 22.dp) {
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
                            },
                            fontWeight = FontWeight.SemiBold
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
            TarazinoGlassCard(cornerRadius = 22.dp) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "تعداد اقساط (در مرحله بعد تک‌تک قابل ویرایش هستند)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        listOf(1, 2, 3, 4, 6, 8, 10, 12, 18, 24, 36).forEach { count ->
                            val selected = installmentCount == count
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                border = BorderStroke(
                                    0.9.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else ext.cardBorderTop
                                ),
                                modifier = Modifier.tarazinoPressScale(
                                    scaleDown = 0.93f,
                                    onClick = { onInstallmentCountChange(count) }
                                )
                            ) {
                                Text(
                                    text = "${PersianCurrencyEngine.toPersianDigits(count.toString())} قسط",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "فاصله زمانی پیش‌فرض بین اقساط:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        PaymentInterval.entries.forEach { itemInterval ->
                            val selected = interval == itemInterval
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                border = BorderStroke(
                                    0.9.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else ext.cardBorderTop
                                ),
                                modifier = Modifier.tarazinoPressScale(
                                    scaleDown = 0.93f,
                                    onClick = { onIntervalChange(itemInterval) }
                                )
                            ) {
                                Text(
                                    text = itemInterval.labelFa,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    JalaliDateSelectorField(
                        labelFa = "تاریخ اولین سررسید شمسی (قسط اول)",
                        dateJalali = firstDueDateJalali,
                        onDateSelected = onFirstDueDateChange
                    )

                    // Live Specular Per-Installment Preview Banner
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ext.positive.copy(alpha = if (ext.isDark) 0.14f else 0.09f),
                        border = BorderStroke(1.dp, ext.positive.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(13.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "مبلغ پایه هر قسط",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                FormattedMoneyText(
                                    amountToman = perInstallmentPreview,
                                    unit = currencyUnit,
                                    privacyMasked = false,
                                    style = MaterialTheme.typography.titleMedium,
                                    numberColor = ext.positive
                                )
                                Text(
                                    text = "در گام بعد می‌توانید مبلغ یا تاریخ هر قسط را تکی ویرایش کنید.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
    val ext = LocalTarazinoColors.current
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
            TarazinoGlassCard(
                cornerRadius = 22.dp,
                accentBorderColor = if (diffToman == 0L) ext.positive else ext.warning
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            TarazinoAutoSizeText(
                                text = "مجموع ${PersianCurrencyEngine.toPersianDigits(draftRows.size.toString())} قسط جدول:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                minFontSize = 9.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                            FormattedMoneyText(
                                amountToman = currentSumToman,
                                unit = currencyUnit,
                                privacyMasked = false,
                                style = MaterialTheme.typography.titleMedium,
                                numberColor = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (diffToman != 0L) {
                            TarazinoStatusPill(
                                text = "اختلاف: ${ltrIsolateDigits(PersianCurrencyEngine.formatAmount(kotlin.math.abs(diffToman), currencyUnit))}",
                                accentColor = ext.warning
                            )
                        } else {
                            TarazinoStatusPill(
                                text = "تراز دقیق ✓",
                                accentColor = ext.positive
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onAddCustomRow,
                            shape = RoundedCornerShape(13.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            TarazinoAutoSizeText(
                                text = "افزودن ردیف",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                minFontSize = 9.sp,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                        OutlinedButton(
                            onClick = onRedistributeEqually,
                            shape = RoundedCornerShape(13.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Balance, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            TarazinoAutoSizeText(
                                text = "توزیع مساوی",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                minFontSize = 9.sp,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }
                }
            }
        }

        // Individual Editable Installment Rows
        itemsIndexed(draftRows) { idx, row ->
            TarazinoGlassCard(
                cornerRadius = 18.dp,
                accentBorderColor = if (row.isPaidAlready) ext.positive.copy(alpha = 0.6f) else null
            ) {
                Column(modifier = Modifier.padding(13.dp)) {
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
                                    .background(
                                        if (row.isPaidAlready) ext.positive.copy(alpha = 0.18f)
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                    )
                            ) {
                                Text(
                                    text = PersianCurrencyEngine.toPersianDigits(row.number.toString()),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Black,
                                    color = if (row.isPaidAlready) ext.positive else MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                TarazinoAutoSizeText(
                                    text = row.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    minFontSize = 10.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                TarazinoAutoSizeText(
                                    text = PersianCurrencyEngine.amountToPersianWords(row.amountToman, currencyUnit),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    minFontSize = 8.5.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        if (draftRows.size > 1) {
                            IconButton(onClick = { onDeleteRow(idx) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "حذف این قسط",
                                    tint = ext.danger,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = ext.cardBorderTop,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Interactive Haptic Pills for Editing Due Date, Amount, and Paid Status (Scrollable so huge Rial amounts never collide!)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(11.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            border = BorderStroke(0.8.dp, ext.cardBorderTop),
                            modifier = Modifier.tarazinoPressScale(
                                scaleDown = 0.94f,
                                onClick = { onOpenDatePickerForRow(idx) }
                            )
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

                        Surface(
                            shape = RoundedCornerShape(11.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            modifier = Modifier.tarazinoPressScale(
                                scaleDown = 0.94f,
                                onClick = { onOpenEditRowModal(idx) }
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                TarazinoAutoSizeText(
                                    text = ltrIsolateDigits(PersianCurrencyEngine.formatAmount(row.amountToman, currencyUnit)),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    minFontSize = 9.5.sp
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "ویرایش مبلغ و جزئیات قسط",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(11.dp),
                            color = if (row.isPaidAlready) ext.positive.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(
                                0.8.dp,
                                if (row.isPaidAlready) ext.positive.copy(alpha = 0.45f) else ext.cardBorderTop
                            ),
                            modifier = Modifier.tarazinoPressScale(
                                scaleDown = 0.94f,
                                onClick = { onToggleRowPaidStatus(idx) }
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (row.isPaidAlready) ext.positive else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (row.isPaidAlready) "پرداخت‌شده" else "در انتظار",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (row.isPaidAlready) ext.positive else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
