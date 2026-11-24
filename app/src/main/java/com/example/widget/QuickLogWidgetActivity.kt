package com.example.widget

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.data.CounterpartyEntity
import com.example.data.EntityCategory
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.ObligationCategory
import com.example.data.ObligationEntity
import com.example.data.TarazinoDatabase
import com.example.data.TarazinoRepository
import com.example.ui.theme.LocalTarazinoColors
import com.example.ui.theme.TarazinoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val QUICK_EXPENSE_TITLE_PRESETS = listOf(
    "خرید روزمره و خواروبار",
    "حمل‌ونقل و تاکسی",
    "قبض و اینترنت",
    "کافه و رستوران",
    "درمان و دارو"
)

private val QUICK_AMOUNT_PRESETS_TOMAN = listOf(
    100_000L to "+۱۰۰ هزار",
    500_000L to "+۵۰۰ هزار",
    1_000_000L to "+۱ میلیون",
    2_000_000L to "+۲ میلیون"
)

private data class UnpaidDebtQuickItem(
    val installment: InstallmentEntity,
    val obligationTitle: String,
    val relativeDueLabelFa: String
)

/**
 * Lightweight, translucent floating quick-log overlay launched directly from the Home Screen Widget.
 * Allows users to log a new expense or partial/full debt payment directly over their home screen
 * without opening the full Tarazino application.
 */
class QuickLogWidgetActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val requestedMode = intent?.getStringExtra(TarazinoQuickLogWidgetProvider.EXTRA_QUICK_LOG_MODE)
            ?.let { runCatching { WidgetQuickLogMode.valueOf(it) }.getOrNull() }
            ?: WidgetQuickLogMode.EXPENSE

        val db = TarazinoDatabase.getInstance(applicationContext)
        val repository = TarazinoRepository(applicationContext, db.tarazinoDao())
        val prefs = repository.preferencesState.value

        setContent {
            TarazinoTheme(
                themeMode = prefs.themeMode,
                fontScale = prefs.fontScale
            ) {
                QuickLogOverlaySheet(
                    initialMode = requestedMode,
                    repository = repository,
                    dao = db.tarazinoDao(),
                    onCompleted = { messageFa ->
                        TarazinoQuickLogWidgetProvider.requestRefreshAll(applicationContext)
                        Toast.makeText(applicationContext, messageFa, Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }
}

@Composable
private fun QuickLogOverlaySheet(
    initialMode: WidgetQuickLogMode,
    repository: TarazinoRepository,
    dao: com.example.data.TarazinoDao,
    onCompleted: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalTarazinoColors.current
    val tealAccent = ext.positive
    val goldAccent = MaterialTheme.colorScheme.secondary
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(initialMode) }
    var expenseTitle by remember { mutableStateOf("") }
    var amountInputText by remember { mutableStateOf("") }
    var markExpenseAsPaid by remember { mutableStateOf(true) }
    var noteText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    var unpaidItems by remember { mutableStateOf<List<UnpaidDebtQuickItem>>(emptyList()) }
    var selectedInstallmentId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) {
            val today = JalaliCalendarEngine.todayJalali()
            val obsMap = dao.getAllObligationsOnce()
                .filter { !it.categoryEnum.isReceivable }
                .associateBy { it.id }
            dao.getAllInstallmentsOnce()
                .filter { it.obligationId in obsMap && it.statusEnum != InstallmentStatus.PAID && it.remainingToman > 0L }
                .sortedWith(
                    compareByDescending<InstallmentEntity> {
                        it.statusEnum == InstallmentStatus.OVERDUE ||
                            JalaliCalendarEngine.diffJalaliDays(today, it.dueDateJalali) < 0
                    }.thenBy { it.dueDateJalali }
                )
                .map { inst ->
                    UnpaidDebtQuickItem(
                        installment = inst,
                        obligationTitle = obsMap[inst.obligationId]?.title ?: inst.title,
                        relativeDueLabelFa = JalaliCalendarEngine.formatRelativeDueStatusFa(today, inst.dueDateJalali)
                    )
                }
        }
        unpaidItems = loaded
        if (selectedInstallmentId == null && loaded.isNotEmpty()) {
            selectedInstallmentId = loaded.first().installment.id
            if (initialMode == WidgetQuickLogMode.DEBT_PAYMENT && amountInputText.isBlank()) {
                amountInputText = loaded.first().installment.remainingToman.toString()
            }
        }
    }

    val parsedAmountToman = remember(amountInputText) {
        val normalized = PersianCurrencyEngine.toEnglishDigits(amountInputText)
            .filter { it.isDigit() }
        normalized.toLongOrNull() ?: 0L
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onDismiss)
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.2.dp, tealAccent.copy(alpha = 0.45f)),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false) {}
                .testTag("widget_quick_log_overlay_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(tealAccent.copy(alpha = 0.16f))
                        ) {
                            Icon(
                                imageVector = if (mode == WidgetQuickLogMode.EXPENSE) {
                                    Icons.Default.ReceiptLong
                                } else {
                                    Icons.Default.Bolt
                                },
                                contentDescription = null,
                                tint = tealAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ثبت سریع ترازینو",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "ثبت فوری بدون باز کردن برنامه اصلی",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("widget_quick_log_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن پنجره ثبت سریع",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 2-Mode Switcher: Expense vs Debt Payment
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WidgetQuickLogMode.entries.forEach { tab ->
                            val isSel = mode == tab
                            val bg by animateColorAsState(
                                targetValue = if (isSel) {
                                    if (tab == WidgetQuickLogMode.EXPENSE) goldAccent else tealAccent
                                } else {
                                    Color.Transparent
                                },
                                label = "wgtModeBg"
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = bg,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        mode = tab
                                        errorMessage = null
                                        if (tab == WidgetQuickLogMode.DEBT_PAYMENT) {
                                            val cur = unpaidItems.firstOrNull { it.installment.id == selectedInstallmentId }
                                                ?: unpaidItems.firstOrNull()
                                            if (cur != null) {
                                                selectedInstallmentId = cur.installment.id
                                                amountInputText = cur.installment.remainingToman.toString()
                                            }
                                        }
                                    }
                                    .testTag("widget_mode_${tab.name.lowercase()}")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = tab.labelFa,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSel) Color(0xFF0A1619) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                if (mode == WidgetQuickLogMode.EXPENSE) {
                    // Expense Title + Quick Preset Chips
                    OutlinedTextField(
                        value = expenseTitle,
                        onValueChange = {
                            expenseTitle = it
                            errorMessage = null
                        },
                        label = { Text("عنوان هزینه یا خرید") },
                        placeholder = { Text("مثلاً خرید سوپرمارکت یا قبض برق") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("widget_expense_title_input")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QUICK_EXPENSE_TITLE_PRESETS.forEach { preset ->
                            val isSel = expenseTitle == preset
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSel) goldAccent.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    0.8.dp,
                                    if (isSel) goldAccent else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        expenseTitle = preset
                                        errorMessage = null
                                    }
                            ) {
                                Text(
                                    text = preset,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSel) goldAccent else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Toggle: Paid immediately vs New unpaid debt
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (markExpenseAsPaid) "پرداخت نقدی / کارتی (ثبت هزینه)" else "ثبت به‌عنوان بدهی پرداخت‌نشده",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (markExpenseAsPaid) "در گزارش هزینه‌ها ثبت می‌شود" else "به لیست اقساط در انتظار اضافه می‌شود",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = markExpenseAsPaid,
                            onCheckedChange = { markExpenseAsPaid = it }
                        )
                    }
                } else {
                    // Debt Payment Target Installment Selector
                    if (unpaidItems.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = tealAccent.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = tealAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "هیچ قسط پرداخت‌نشده‌ای ندارید! همه بدهی‌ها تسویه شده‌اند.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "انتخاب قسط بدهی برای ثبت پرداخت:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 170.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            unpaidItems.take(8).forEach { item ->
                                val isSelected = selectedInstallmentId == item.installment.id
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) tealAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(
                                        width = if (isSelected) 1.2.dp else 0.8.dp,
                                        color = if (isSelected) tealAccent else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedInstallmentId = item.installment.id
                                            amountInputText = item.installment.remainingToman.toString()
                                            errorMessage = null
                                        }
                                        .testTag("widget_debt_item_${item.installment.id}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.obligationTitle,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${item.installment.title} • ${item.relativeDueLabelFa}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = PersianCurrencyEngine.formatAmount(item.installment.remainingToman),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isSelected) tealAccent else goldAccent
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Amount Input (Toman) + Quick Preset Pills
                OutlinedTextField(
                    value = amountInputText,
                    onValueChange = { raw ->
                        amountInputText = PersianCurrencyEngine.toEnglishDigits(raw).filter { it.isDigit() }
                        errorMessage = null
                    },
                    label = {
                        Text(
                            if (mode == WidgetQuickLogMode.EXPENSE) "مبلغ هزینه (تومان)"
                            else "مبلغ پرداختی (تومان — کامل یا جزئی)"
                        )
                    },
                    supportingText = {
                        if (parsedAmountToman > 0L) {
                            Text(
                                text = PersianCurrencyEngine.formatAmount(parsedAmountToman),
                                color = tealAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("widget_amount_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QUICK_AMOUNT_PRESETS_TOMAN.forEach { (delta, labelFa) ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    val nextVal = parsedAmountToman + delta
                                    amountInputText = nextVal.toString()
                                    errorMessage = null
                                }
                        ) {
                            Text(
                                text = labelFa,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Optional Note Input
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("یادداشت کوتاه (اختیاری)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = ext.danger,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Primary Save Button (48dp+ accessible height)
                Button(
                    onClick = {
                        val validation = QuickLogWidgetEngine.validateInput(
                            mode = mode,
                            title = expenseTitle,
                            amountToman = parsedAmountToman,
                            selectedInstallmentId = selectedInstallmentId
                        )
                        if (validation is QuickLogValidationResult.Invalid) {
                            errorMessage = validation.errorMessageFa
                            return@Button
                        }

                        isSaving = true
                        scope.launch {
                            val today = JalaliCalendarEngine.todayJalali()
                            val successMsg = withContext(Dispatchers.IO) {
                                if (mode == WidgetQuickLogMode.EXPENSE) {
                                    val existingCps = dao.getAllCounterpartiesOnce()
                                    val quickCpId = existingCps.firstOrNull {
                                        it.name == "هزینه‌های روزمره (ویجت سریع)"
                                    }?.id ?: repository.saveCounterparty(
                                        CounterpartyEntity(
                                            name = "هزینه‌های روزمره (ویجت سریع)",
                                            category = EntityCategory.PERSON.name,
                                            avatarColorHex = 0xFFDAB978,
                                            createdAtJalali = today
                                        )
                                    )
                                    val bundle = QuickLogWidgetEngine.createQuickExpenseBundle(
                                        counterpartyId = quickCpId,
                                        title = expenseTitle,
                                        amountToman = parsedAmountToman,
                                        category = ObligationCategory.PERSONAL_DEBT,
                                        markAsPaidImmediately = markExpenseAsPaid,
                                        note = noteText,
                                        dateJalali = today
                                    )
                                    repository.createObligationWithSchedule(
                                        obligation = bundle.obligation,
                                        installments = listOf(bundle.installment)
                                    )
                                    "«${bundle.obligation.title}» (${PersianCurrencyEngine.formatAmount(parsedAmountToman)}) ثبت شد."
                                } else {
                                    val target = unpaidItems.firstOrNull {
                                        it.installment.id == selectedInstallmentId
                                    } ?: return@withContext "قسط انتخاب‌شده یافت نشد."
                                    val updated = QuickLogWidgetEngine.applyQuickDebtPayment(
                                        installment = target.installment,
                                        paymentAmountToman = parsedAmountToman,
                                        todayJalali = today
                                    )
                                    repository.updateInstallment(updated)
                                    if (updated.statusEnum == InstallmentStatus.PAID) {
                                        "قسط «${target.obligationTitle}» به‌طور کامل تسویه شد."
                                    } else {
                                        "پرداخت جزئی ${PersianCurrencyEngine.formatAmount(parsedAmountToman)} برای «${target.obligationTitle}» ثبت شد."
                                    }
                                }
                            }
                            isSaving = false
                            onCompleted(successMsg)
                        }
                    },
                    enabled = !isSaving && (mode == WidgetQuickLogMode.EXPENSE || unpaidItems.isNotEmpty()),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (mode == WidgetQuickLogMode.EXPENSE) goldAccent else tealAccent,
                        contentColor = Color(0xFF061619)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("widget_quick_log_submit_btn")
                ) {
                    Text(
                        text = if (isSaving) {
                            "در حال ثبت..."
                        } else if (mode == WidgetQuickLogMode.EXPENSE) {
                            "ثبت فوری هزینه"
                        } else {
                            "ثبت پرداخت بدهی"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
