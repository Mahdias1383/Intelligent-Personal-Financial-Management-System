package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.CurrencyUnit
import com.example.export.ReportExportEngine
import com.example.ui.TarazinoNavTab
import com.example.ui.TarazinoUiState
import com.example.ui.TarazinoViewModel
import com.example.ui.components.A4ReportPreviewModal
import com.example.ui.components.InteractiveOnboardingTourOverlay
import com.example.ui.components.SecurityLockOverlay
import com.example.ui.components.StepByStepObligationWizardModal
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.IntelligenceScreen
import com.example.ui.screens.ReportsAndSettingsScreen
import com.example.ui.screens.TreePortfolioScreen
import com.example.ui.theme.TarazinoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: TarazinoViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            TarazinoTheme(
                themeMode = uiState.preferences.themeMode,
                fontScale = uiState.preferences.fontScale
            ) {
                TarazinoAppRoot(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TarazinoAppRoot(
    uiState: TarazinoUiState,
    viewModel: TarazinoViewModel
) {
    val context = LocalContext.current

    // Handle hierarchical tree step-back and non-dashboard tab back navigation
    if (uiState.activeTab != TarazinoNavTab.DASHBOARD) {
        BackHandler {
            if (uiState.activeTab == TarazinoNavTab.PORTFOLIO_TREE) {
                val steppedBack = viewModel.stepBackInTree()
                if (!steppedBack) {
                    viewModel.selectTab(TarazinoNavTab.DASHBOARD)
                }
            } else {
                viewModel.selectTab(TarazinoNavTab.DASHBOARD)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(
                                    text = "ترازینو",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "مدیریت هوشمند اقساط، بدهی و وام",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        // Currency Unit Quick Toggle Chip (تومان / ریال)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clickable {
                                    val nextUnit = if (uiState.preferences.currencyUnit == CurrencyUnit.TOMAN) {
                                        CurrencyUnit.RIAL
                                    } else {
                                        CurrencyUnit.TOMAN
                                    }
                                    viewModel.setCurrencyUnit(nextUnit)
                                }
                                .testTag("currency_unit_toggle_chip")
                        ) {
                            Text(
                                text = uiState.preferences.currencyUnit.labelFa,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        // Privacy Mask Toggle Button (مخفی‌سازی ارقام در محیط عمومی)
                        IconButton(
                            onClick = { viewModel.togglePrivacyMask() },
                            modifier = Modifier.testTag("privacy_mask_toggle_btn")
                        ) {
                            Icon(
                                imageVector = if (uiState.preferences.privacyMaskEnabled) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = "مخفی‌سازی ارقام مالی",
                                tint = if (uiState.preferences.privacyMaskEnabled) {
                                    Color(0xFFF59E0B)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }

                        // Interactive Guided Tour Launch Button
                        IconButton(
                            onClick = { viewModel.startGuidedTour() },
                            modifier = Modifier.testTag("top_bar_tour_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "اجرای تور آموزشی تعاملی",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }

                        // Immediate Lock Button
                        IconButton(
                            onClick = { viewModel.lockAppImmediately() },
                            modifier = Modifier.testTag("top_bar_lock_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "قفل آنی برنامه"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    val navItems = listOf(
                        Triple(TarazinoNavTab.DASHBOARD, "داشبورد", Icons.Default.Dashboard),
                        Triple(TarazinoNavTab.PORTFOLIO_TREE, "پوشه اقساط", Icons.Default.AccountTree),
                        Triple(TarazinoNavTab.INTELLIGENCE, "هوش مالی", Icons.Default.Insights),
                        Triple(TarazinoNavTab.REPORTS_SETTINGS, "گزارش و تنظیمات", Icons.Default.PictureAsPdf)
                    )
                    navItems.forEach { (tab, label, icon) ->
                        NavigationBarItem(
                            selected = uiState.activeTab == tab,
                            onClick = { viewModel.selectTab(tab) },
                            icon = { Icon(imageVector = icon, contentDescription = label) },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            },
                            modifier = Modifier.testTag("bottom_nav_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Celebratory Banner on Installment Settlement
                AnimatedVisibility(visible = uiState.celebrationMessageFa != null) {
                    uiState.celebrationMessageFa?.let { msg ->
                        Surface(
                            color = Color(0xFF065F46),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.clearCelebration() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF34D399),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "بستن",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA7F3D0)
                                )
                            }
                        }
                    }
                }

                // Toast Feedback Banner
                AnimatedVisibility(visible = uiState.toastMessageFa != null) {
                    uiState.toastMessageFa?.let { toast ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .clickable { viewModel.clearToast() }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = toast,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Active Tab Screen Content
                Box(modifier = Modifier.weight(1f)) {
                    when (uiState.activeTab) {
                        TarazinoNavTab.DASHBOARD -> DashboardScreen(
                            uiState = uiState,
                            onQuickPay = { viewModel.quickPayInstallment(it) },
                            onSnoozeTomorrow = { viewModel.snoozeInstallmentTomorrow(it) },
                            onOpenNewObligationWizard = { viewModel.openStepByStepWizard() },
                            onOpenCategoryInTree = { cat -> viewModel.openCategoryFolderInTree(cat) },
                            onOpenCounterpartyInTree = { cp -> viewModel.selectTreeCounterparty(cp) },
                            onOpenObligationInTree = { ob, cat -> viewModel.selectTreeObligation(ob, cat) },
                            onOpenIntelligenceTab = { viewModel.selectTab(TarazinoNavTab.INTELLIGENCE) },
                            onOpenFullA4Report = { viewModel.openA4ReportPreview() }
                        )

                        TarazinoNavTab.PORTFOLIO_TREE -> TreePortfolioScreen(
                            uiState = uiState,
                            onSelectCategory = { cat -> viewModel.openCategoryFolderInTree(cat) },
                            onSelectCounterparty = { cp -> viewModel.selectTreeCounterparty(cp) },
                            onSelectObligation = { ob, cat -> viewModel.selectTreeObligation(ob, cat) },
                            onStepBackInTree = { viewModel.stepBackInTree() },
                            onResetTreeToRoot = { viewModel.resetTreeToRoot() },
                            onOpenWizard = { cat, cpId -> viewModel.openStepByStepWizard(cat, cpId) },
                            onSaveCounterparty = { viewModel.saveCounterparty(it) },
                            onDeleteCounterparty = { id, name -> viewModel.deleteCounterparty(id, name) },
                            onDeleteObligation = { id, title -> viewModel.deleteObligation(id, title) },
                            onQuickPayInstallment = { viewModel.quickPayInstallment(it) },
                            onDetailedInstallmentUpdate = { inst, title, baseAmt, dueDate, paid, fee, disc, paidDate, code, note ->
                                viewModel.applyDetailedInstallmentPayment(
                                    installment = inst,
                                    customTitle = title,
                                    customBaseAmountToman = baseAmt,
                                    customDueDateJalali = dueDate,
                                    paidAmountToman = paid,
                                    lateFeeToman = fee,
                                    discountToman = disc,
                                    paidDateJalali = paidDate,
                                    trackingCode = code,
                                    note = note
                                )
                            },
                            onAddSingleCustomInstallment = { obId, nextNum, title, dueDate, amt, isPaid, note ->
                                viewModel.addSingleCustomInstallmentToObligation(
                                    obligationId = obId,
                                    nextNumber = nextNum,
                                    title = title,
                                    dueDateJalali = dueDate,
                                    amountToman = amt,
                                    isAlreadyPaid = isPaid,
                                    note = note
                                )
                            },
                            onDeleteSingleInstallment = { id, title ->
                                viewModel.deleteSingleInstallment(id, title)
                            },
                            onAddSubRecord = { ob, sub -> viewModel.addSubRecordToObligation(ob, sub) },
                            onOpenCounterpartyA4Report = { cp ->
                                viewModel.openA4ReportPreview(
                                    reportTitleFa = "صورتحساب اختصاصی زیرمجموعه: ${cp.name}",
                                    subtitleFa = "${cp.categoryEnum.labelFa} • ${cp.priorityEnum.labelFa}",
                                    filterCounterpartyId = cp.id
                                )
                            },
                            onOpenObligationA4Report = { ob ->
                                viewModel.openA4ReportPreview(
                                    reportTitleFa = "جدول رسمی اقساط: ${ob.title}",
                                    subtitleFa = "${ob.categoryEnum.labelFa} • ${ob.scheduleModeEnum.labelFa}",
                                    filterObligationId = ob.id
                                )
                            },
                            onShowToast = { viewModel.showToast(it) }
                        )

                        TarazinoNavTab.INTELLIGENCE -> IntelligenceScreen(
                            uiState = uiState,
                            onUpdateExtraPayoffBudget = { viewModel.updateExtraPayoffBudget(it) },
                            onChangeHeatmapMonth = { viewModel.changeHeatmapMonth(it) },
                            onUpdateIncomeAndAlertDays = { inc, days ->
                                viewModel.updateMonthlyIncomeAndAlertDays(inc, days)
                            }
                        )

                        TarazinoNavTab.REPORTS_SETTINGS -> ReportsAndSettingsScreen(
                            uiState = uiState,
                            onOpenFullA4Report = { viewModel.openA4ReportPreview() },
                            onOpenCounterpartyA4Report = { cp ->
                                viewModel.openA4ReportPreview(
                                    reportTitleFa = "صورتحساب اختصاصی طرف‌حساب: ${cp.name}",
                                    subtitleFa = cp.categoryEnum.labelFa,
                                    filterCounterpartyId = cp.id
                                )
                            },
                            onDirectExportPdf = {
                                val payload = ReportExportEngine.buildReportPayload(
                                    reportTitleFa = "گزارش جامع وضعیت اقساط و تعهدات مالی",
                                    subtitleFa = "خروجی رسمی استاندارد A4 ترازینو",
                                    obligations = uiState.obligations,
                                    installments = uiState.installments,
                                    counterparties = uiState.counterparties,
                                    currencyUnit = uiState.preferences.currencyUnit
                                )
                                viewModel.exportA4Pdf(context, payload)
                            },
                            onDirectExportExcel = {
                                val payload = ReportExportEngine.buildReportPayload(
                                    reportTitleFa = "گزارش جامع وضعیت اقساط و تعهدات مالی",
                                    subtitleFa = "خروجی اکسل راست‌به‌چپ ترازینو",
                                    obligations = uiState.obligations,
                                    installments = uiState.installments,
                                    counterparties = uiState.counterparties,
                                    currencyUnit = uiState.preferences.currencyUnit
                                )
                                viewModel.exportRtlExcel(context, payload)
                            },
                            onDirectExportCsv = {
                                val payload = ReportExportEngine.buildReportPayload(
                                    reportTitleFa = "گزارش جامع وضعیت اقساط و تعهدات مالی",
                                    subtitleFa = "خروجی CSV جهانی ترازینو",
                                    obligations = uiState.obligations,
                                    installments = uiState.installments,
                                    counterparties = uiState.counterparties,
                                    currencyUnit = uiState.preferences.currencyUnit
                                )
                                viewModel.exportUtf8Csv(context, payload)
                            },
                            onSetThemeMode = { viewModel.setThemeMode(it) },
                            onSetCurrencyUnit = { viewModel.setCurrencyUnit(it) },
                            onSetFontScale = { viewModel.setFontScale(it) },
                            onConfigureSecurityPin = { pin, len, bio, q, ans ->
                                viewModel.configureSecurityPin(pin, len, bio, q, ans)
                            },
                            onRemoveSecurityPin = { viewModel.removeSecurityPin() },
                            onLockAppNow = { viewModel.lockAppImmediately() },
                            onStartGuidedTour = { viewModel.startGuidedTour() },
                            onClearAllDataToZero = { viewModel.clearAllDataToZero() },
                            onRestoreDemoSeedData = { viewModel.restoreDemoSeedData() },
                            onExportBackupJson = { cb -> viewModel.exportFullBackupJson(cb) },
                            onImportBackupJson = { viewModel.importFullBackupJson(it) },
                            onReRunQaSuites = { viewModel.reRunQaDiagnostics() },
                            onShowToast = { viewModel.showToast(it) }
                        )
                    }
                }
            }
        }

        // 3-Step Full-Screen Obligation & Editable Per-Installment Wizard Modal
        if (uiState.wizardPreset.isOpen) {
            StepByStepObligationWizardModal(
                counterparties = uiState.counterparties,
                preselectedCategory = uiState.wizardPreset.preselectedCategory,
                preselectedCounterpartyId = uiState.wizardPreset.preselectedCounterpartyId,
                currencyUnit = uiState.preferences.currencyUnit,
                onDismiss = { viewModel.closeStepByStepWizard() },
                onCompleteWizard = { ob, customInst, newCp ->
                    viewModel.saveWizardObligationAndCustomInstallments(ob, customInst, newCp)
                }
            )
        }

        // Standard A4 Colorful Report Preview Modal
        uiState.activeA4ReportPayload?.let { payload ->
            A4ReportPreviewModal(
                payload = payload,
                onDismiss = { viewModel.closeA4ReportPreview() },
                onExportPdf = { viewModel.exportA4Pdf(context, payload) },
                onExportRtlExcel = { viewModel.exportRtlExcel(context, payload) },
                onExportCsv = { viewModel.exportUtf8Csv(context, payload) }
            )
        }

        // Interactive Graphical Spotlight Onboarding Tour Overlay (First Launch & On-Demand)
        if (uiState.isTourActive && !uiState.isVaultLocked) {
            InteractiveOnboardingTourOverlay(
                steps = viewModel.guidedTourSteps,
                currentStepIndex = uiState.currentTourStepIndex,
                onNextStep = { viewModel.nextTourStep() },
                onPreviousStep = { viewModel.previousTourStep() },
                onSkipOrFinish = { viewModel.finishGuidedTour() }
            )
        }

        // Security Vault Lock Gate Overlay
        if (uiState.isVaultLocked) {
            SecurityLockOverlay(
                pinLength = uiState.preferences.pinLength,
                biometricEnabled = uiState.preferences.biometricEnabled,
                recoveryQuestion = uiState.preferences.recoveryQuestion,
                hasRecoveryConfigured = uiState.preferences.recoveryAnswerHash.isNotBlank(),
                onVerifyPin = { viewModel.attemptUnlockWithPin(it) },
                onBiometricUnlock = { viewModel.unlockViaBiometricSensor() },
                onVerifyRecoveryAnswer = { viewModel.attemptRecoveryUnlock(it) }
            )
        }
    }
}
