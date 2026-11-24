package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.core.CurrencyUnit
import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.data.EntityCategory
import com.example.data.InstallmentEntity
import com.example.domain.LoanContractOcrParserEngine
import com.example.domain.ScannedContractExtraction
import com.example.domain.ScannedDocumentType
import com.example.scanner.CameraContractOcrHelper
import com.example.ui.theme.LocalTarazinoColors
import kotlinx.coroutines.launch
import java.util.Locale

private enum class ScannerModalStage {
    CAMERA_CAPTURE,
    REVIEW_EXTRACTION
}

@Composable
fun LoanContractScannerModal(
    currencyUnit: CurrencyUnit,
    unpaidInstallments: List<InstallmentEntity>,
    onDismiss: () -> Unit,
    onPopulateNewObligationWizard: (ScannedContractExtraction) -> Unit,
    onApplyAsInstallmentPayment: (InstallmentEntity, ScannedContractExtraction) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ext = LocalTarazinoColors.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var stage by remember { mutableStateOf(ScannerModalStage.CAMERA_CAPTURE) }
    var isProcessingOcr by remember { mutableStateOf(false) }
    var errorMessageFa by remember { mutableStateOf<String?>(null) }

    // Editable Review State populated by LoanContractOcrParserEngine
    var rawOcrText by remember { mutableStateOf("") }
    var lastExtraction by remember { mutableStateOf<ScannedContractExtraction?>(null) }
    var editedTitle by remember { mutableStateOf("") }
    var editedCounterparty by remember { mutableStateOf("") }
    var editedCategory by remember { mutableStateOf(EntityCategory.BANK) }
    var editedPrincipalToman by remember { mutableLongStateOf(0L) }
    var editedInstallmentToman by remember { mutableLongStateOf(0L) }
    var editedInstallmentCount by remember { mutableIntStateOf(12) }
    var editedInterestRateText by remember { mutableStateOf("") }
    var editedProfitOrFeeToman by remember { mutableLongStateOf(0L) }
    var editedFirstDueDateJalali by remember {
        mutableStateOf(JalaliCalendarEngine.firstOfNextJalaliMonth(JalaliCalendarEngine.todayJalali()))
    }
    var editedTrackingCode by remember { mutableStateOf("") }
    var showRawTextEditor by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    fun applyParsedExtraction(extraction: ScannedContractExtraction) {
        lastExtraction = extraction
        rawOcrText = extraction.rawRecognizedText
        editedTitle = extraction.suggestedTitle
        editedCounterparty = extraction.detectedCounterpartyName.orEmpty()
        editedCategory = extraction.suggestedCategory
        editedPrincipalToman = extraction.principalAmountToman ?: 0L
        editedInstallmentToman = extraction.installmentAmountToman ?: 0L
        editedInstallmentCount = (extraction.installmentCount ?: 12).coerceIn(1, 360)
        editedInterestRateText = extraction.annualInterestRatePercent?.let { rate ->
            if (rate % 1.0 == 0.0) rate.toInt().toString() else String.format(Locale.US, "%.1f", rate)
        }.orEmpty()
        editedProfitOrFeeToman = extraction.computedProfitOrFeeToman ?: 0L
        editedFirstDueDateJalali = extraction.firstDueDateJalali
            ?: JalaliCalendarEngine.firstOfNextJalaliMonth(JalaliCalendarEngine.todayJalali())
        editedTrackingCode = extraction.trackingCode.orEmpty()
        stage = ScannerModalStage.REVIEW_EXTRACTION
        errorMessageFa = null
    }

    fun buildVerifiedExtraction(): ScannedContractExtraction {
        val parsedRate = PersianCurrencyEngine.toEnglishDigits(editedInterestRateText)
            .replace(",", ".")
            .toDoubleOrNull()
        val effectivePrincipal = when {
            editedPrincipalToman > 0L -> editedPrincipalToman
            editedInstallmentToman > 0L -> editedInstallmentToman * editedInstallmentCount.coerceAtLeast(1)
            else -> 0L
        }
        val effectiveProfit = when {
            editedProfitOrFeeToman > 0L -> editedProfitOrFeeToman
            parsedRate != null && parsedRate > 0.0 && effectivePrincipal > 0L ->
                LoanContractOcrParserEngine.computeAmortizedTotalInterest(
                    principalToman = effectivePrincipal,
                    annualInterestPercent = parsedRate,
                    installmentCount = editedInstallmentCount.coerceAtLeast(1)
                )
            editedInstallmentToman > 0L && effectivePrincipal > 0L &&
                editedInstallmentToman * editedInstallmentCount > effectivePrincipal ->
                (editedInstallmentToman * editedInstallmentCount - effectivePrincipal)
            else -> 0L
        }
        val base = lastExtraction ?: LoanContractOcrParserEngine.parseRecognizedText(rawOcrText)
        return base.copy(
            rawRecognizedText = rawOcrText,
            suggestedTitle = editedTitle.trim().ifBlank { "وام اسکن‌شده" },
            suggestedCategory = editedCategory,
            detectedCounterpartyName = editedCounterparty.trim().ifBlank { null },
            principalAmountToman = effectivePrincipal.takeIf { it > 0L },
            installmentAmountToman = editedInstallmentToman.takeIf { it > 0L },
            installmentCount = editedInstallmentCount.coerceAtLeast(1),
            annualInterestRatePercent = parsedRate,
            computedProfitOrFeeToman = effectiveProfit,
            firstDueDateJalali = editedFirstDueDateJalali,
            trackingCode = editedTrackingCode.trim().ifBlank { null }
        )
    }

    // Zero-Permission Android Photo Picker for Scanning Existing Photos
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isProcessingOcr = true
            errorMessageFa = null
            scope.launch {
                try {
                    val text = CameraContractOcrHelper.recognizeTextFromUri(context, uri)
                    if (text.isBlank()) {
                        errorMessageFa = "متنی در تصویر شناسایی نشد. می‌توانید متن را به صورت دستی وارد یا ویرایش کنید."
                        showRawTextEditor = true
                        applyParsedExtraction(LoanContractOcrParserEngine.parseRecognizedText(""))
                    } else {
                        applyParsedExtraction(LoanContractOcrParserEngine.parseRecognizedText(text))
                    }
                } catch (e: Exception) {
                    errorMessageFa = "خطا در خواندن متن تصویر: ${e.localizedMessage ?: "نامشخص"}"
                } finally {
                    isProcessingOcr = false
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("loan_contract_scanner_modal"),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = stage,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "ScannerModalStageAnim"
                ) { activeStage ->
                    when (activeStage) {
                        ScannerModalStage.CAMERA_CAPTURE -> {
                            LoanContractCameraViewport(
                                hasCameraPermission = hasCameraPermission,
                                isProcessingOcr = isProcessingOcr,
                                onRequestCameraPermission = {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                },
                                onBitmapCaptured = { bitmap, rotationDegrees ->
                                    isProcessingOcr = true
                                    errorMessageFa = null
                                    scope.launch {
                                        try {
                                            val text = CameraContractOcrHelper.recognizeTextFromBitmap(
                                                bitmap = bitmap,
                                                rotationDegrees = rotationDegrees
                                            )
                                            bitmap.recycle()
                                            if (text.isBlank()) {
                                                errorMessageFa = "متن واضحی در کادر یافت نشد. نور را افزایش دهید یا متن را دستی ویرایش کنید."
                                                showRawTextEditor = true
                                                applyParsedExtraction(LoanContractOcrParserEngine.parseRecognizedText(""))
                                            } else {
                                                applyParsedExtraction(LoanContractOcrParserEngine.parseRecognizedText(text))
                                            }
                                        } catch (e: Exception) {
                                            runCatching { bitmap.recycle() }
                                            errorMessageFa = "خطا در تشخیص متن دوربین: ${e.localizedMessage ?: "نامشخص"}"
                                        } finally {
                                            isProcessingOcr = false
                                        }
                                    }
                                },
                                onCaptureError = { msg -> errorMessageFa = msg },
                                onPickFromGallery = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onOpenManualTextPaste = {
                                    showRawTextEditor = true
                                    applyParsedExtraction(LoanContractOcrParserEngine.parseRecognizedText(rawOcrText))
                                },
                                onClose = onDismiss
                            )
                        }

                        ScannerModalStage.REVIEW_EXTRACTION -> {
                            val currentVerified = remember(
                                editedTitle, editedCounterparty, editedCategory,
                                editedPrincipalToman, editedInstallmentToman, editedInstallmentCount,
                                editedInterestRateText, editedProfitOrFeeToman, editedFirstDueDateJalali,
                                editedTrackingCode, rawOcrText, lastExtraction
                            ) {
                                buildVerifiedExtraction()
                            }
                            val matchedInstallment = remember(currentVerified, unpaidInstallments) {
                                LoanContractOcrParserEngine.matchBestUnpaidInstallment(
                                    currentVerified,
                                    unpaidInstallments
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .tarazinoAmbientBackdrop(
                                        backgroundColor = MaterialTheme.colorScheme.background,
                                        auroraPrimary = ext.auroraPrimary,
                                        auroraSecondary = ext.auroraSecondary
                                    )
                            ) {
                                // Top Header
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = if (ext.isDark) 0.92f else 0.97f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            TarazinoIconBadge(
                                                icon = Icons.Default.DocumentScanner,
                                                accentColor = MaterialTheme.colorScheme.primary,
                                                size = 42.dp,
                                                iconSize = 21.dp,
                                                cornerRadius = 14.dp
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "بررسی و تأیید اطلاعات استخراج‌شده",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${lastExtraction?.detectedDocumentType?.labelFa ?: ScannedDocumentType.UNKNOWN.labelFa} • اطمینان ${PersianCurrencyEngine.toPersianDigits((lastExtraction?.confidenceScore ?: 0).toString())}٪",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        IconButton(onClick = onDismiss) {
                                            Icon(Icons.Default.Close, contentDescription = "بستن")
                                        }
                                    }
                                }

                                // Scrollable Verification Form
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .verticalScroll(rememberScrollState())
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (!errorMessageFa.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.errorContainer,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = errorMessageFa.orEmpty(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.padding(12.dp)
                                            )
                                        }
                                    }

                                    // Extraction Notes Summary Pill
                                    val notes = lastExtraction?.extractionNotesFa.orEmpty()
                                    if (notes.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.AutoAwesome,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = "خلاصه تحلیل خودکار متن (OCR)",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                notes.forEach { note ->
                                                    Text(
                                                        text = "• $note",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Raw OCR Text Collapsible Editor & Re-parse Card
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { showRawTextEditor = !showRawTextEditor },
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "متن خام شناسایی‌شده (قابل ویرایش و تحلیل مجدد)",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Icon(
                                                    imageVector = if (showRawTextEditor) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = null
                                                )
                                            }
                                            AnimatedVisibility(visible = showRawTextEditor) {
                                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                                    OutlinedTextField(
                                                        value = rawOcrText,
                                                        onValueChange = { rawOcrText = it },
                                                        label = { Text("متن قرارداد یا رسید") },
                                                        placeholder = {
                                                            Text("مثال: وام بانک ملت - مبلغ ۵۰,۰۰۰,۰۰۰ تومان - نرخ سود ۲۳٪ - ۲۴ ماهه - سررسید ۱۴۰۵/۰۹/۱۵")
                                                        },
                                                        minLines = 3,
                                                        maxLines = 6,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .testTag("scanner_raw_ocr_text_input")
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Button(
                                                            onClick = {
                                                                applyParsedExtraction(
                                                                    LoanContractOcrParserEngine.parseRecognizedText(rawOcrText)
                                                                )
                                                            },
                                                            shape = RoundedCornerShape(12.dp),
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .testTag("scanner_reparse_text_btn")
                                                        ) {
                                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text("استخراج خودکار مجدد", fontWeight = FontWeight.Bold)
                                                        }
                                                        OutlinedButton(
                                                            onClick = {
                                                                photoPickerLauncher.launch(
                                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                                )
                                                            },
                                                            shape = RoundedCornerShape(12.dp)
                                                        ) {
                                                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("گالری")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Core Extracted Debt Fields Card
                                    TarazinoGlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = editedTitle,
                                                onValueChange = { editedTitle = it },
                                                label = { Text("عنوان وام / تعهد") },
                                                singleLine = true,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("scanner_title_input")
                                            )

                                            OutlinedTextField(
                                                value = editedCounterparty,
                                                onValueChange = { editedCounterparty = it },
                                                label = { Text("نام بانک، صندوق یا طرف‌حساب") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            // Category Chips
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                EntityCategory.entries.forEach { cat ->
                                                    val selected = editedCategory == cat
                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                        border = BorderStroke(
                                                            1.dp,
                                                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                                        ),
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .clickable { editedCategory = cat }
                                                    ) {
                                                        Text(
                                                            text = cat.labelFa,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            MoneyInputField(
                                                labelFa = "مبلغ اصل وام / کل قرارداد",
                                                amountToman = editedPrincipalToman,
                                                onAmountChange = { editedPrincipalToman = it },
                                                currencyUnit = currencyUnit,
                                                showQuickChips = false,
                                                testTagName = "scanner_principal_input"
                                            )

                                            MoneyInputField(
                                                labelFa = "مبلغ هر قسط / مبلغ رسید",
                                                amountToman = editedInstallmentToman,
                                                onAmountChange = { editedInstallmentToman = it },
                                                currencyUnit = currencyUnit,
                                                showQuickChips = false,
                                                testTagName = "scanner_installment_amount_input"
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = editedInstallmentCount.toString(),
                                                    onValueChange = { raw ->
                                                        val digits = PersianCurrencyEngine.toEnglishDigits(raw).filter { it.isDigit() }
                                                        editedInstallmentCount = (digits.toIntOrNull() ?: 1).coerceIn(1, 360)
                                                    },
                                                    label = { Text("تعداد اقساط") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("scanner_installment_count_input")
                                                )

                                                OutlinedTextField(
                                                    value = editedInterestRateText,
                                                    onValueChange = { raw ->
                                                        editedInterestRateText = PersianCurrencyEngine.toEnglishDigits(raw)
                                                            .filter { it.isDigit() || it == '.' }
                                                        val rate = editedInterestRateText.toDoubleOrNull()
                                                        if (rate != null && rate >= 0.0 && editedPrincipalToman > 0L) {
                                                            editedProfitOrFeeToman = LoanContractOcrParserEngine.computeAmortizedTotalInterest(
                                                                principalToman = editedPrincipalToman,
                                                                annualInterestPercent = rate,
                                                                installmentCount = editedInstallmentCount
                                                            )
                                                        }
                                                    },
                                                    label = { Text("نرخ سود (٪)") },
                                                    trailingIcon = {
                                                        Icon(Icons.Default.Percent, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    singleLine = true,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("scanner_interest_rate_input")
                                                )
                                            }

                                            // Computed Profit/Interest Summary + Due Date Selector
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .clickable { showDatePicker = true }
                                                        .testTag("scanner_due_date_picker_btn")
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.CalendarMonth,
                                                            contentDescription = "انتخاب تاریخ اولین سررسید",
                                                            tint = MaterialTheme.colorScheme.secondary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Column {
                                                            Text(
                                                                text = "تاریخ اولین سررسید",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                            Text(
                                                                text = PersianCurrencyEngine.toPersianDigits(editedFirstDueDateJalali),
                                                                style = MaterialTheme.typography.labelLarge,
                                                                fontWeight = FontWeight.ExtraBold
                                                            )
                                                        }
                                                    }
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                                        Text(
                                                            text = "سود / کارمزد برآوردی",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = PersianCurrencyEngine.formatAmount(
                                                                currentVerified.computedProfitOrFeeToman ?: 0L,
                                                                currencyUnit
                                                            ),
                                                            style = MaterialTheme.typography.labelLarge,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }

                                            OutlinedTextField(
                                                value = editedTrackingCode,
                                                onValueChange = { editedTrackingCode = it },
                                                label = { Text("شماره قرارداد یا کد پیگیری (اختیاری)") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }

                                    // Smart Receipt Payment Action Card (if an unpaid installment matches!)
                                    if (matchedInstallment != null) {
                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.ReceiptLong,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.tertiary
                                                    )
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "رسید پرداخت قسط است؟",
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.ExtraBold
                                                        )
                                                        Text(
                                                            text = "نزدیک‌ترین قسط معوق/جاری: «${matchedInstallment.title}» — ${PersianCurrencyEngine.formatAmount(matchedInstallment.remainingToman, currencyUnit)} (سررسید ${PersianCurrencyEngine.toPersianDigits(matchedInstallment.dueDateJalali)})",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        onApplyAsInstallmentPayment(matchedInstallment, currentVerified)
                                                    },
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(48.dp)
                                                        .testTag("scanner_apply_receipt_btn")
                                                ) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "ثبت به عنوان پرداخت «${matchedInstallment.title}»",
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Sticky Bottom Actions
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 6.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { stage = ScannerModalStage.CAMERA_CAPTURE },
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .height(50.dp)
                                                .testTag("scanner_rescan_btn")
                                        ) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("اسکن مجدد", fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                onPopulateNewObligationWizard(currentVerified)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(50.dp)
                                                .testTag("scanner_populate_wizard_btn")
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "تکمیل خودکار فرم ثبت بدهی",
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Full-screen OCR Processing Overlay
                if (isProcessingOcr) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.72f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "در حال تشخیص متن و استخراج ارقام قرارداد...",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "پردازش کاملاً آفلاین روی دستگاه شما (ML Kit OCR)",
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

    if (showDatePicker) {
        JalaliDatePickerModal(
            initialDateJalali = editedFirstDueDateJalali,
            onDismiss = { showDatePicker = false },
            onConfirmDate = { picked ->
                editedFirstDueDateJalali = picked
                showDatePicker = false
            }
        )
    }
}
