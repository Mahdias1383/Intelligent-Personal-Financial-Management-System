package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.CurrencyUnit
import com.example.core.JalaliCalendarEngine
import com.example.core.SecurityVaultEngine
import com.example.data.CounterpartyEntity
import com.example.data.EntityCategory
import com.example.data.FontScalePreset
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.LedgerSubRecordEntity
import com.example.data.ObligationEntity
import com.example.data.ScheduleMode
import com.example.data.TarazinoDatabase
import com.example.data.TarazinoRepository
import com.example.data.TarazinoThemeMode
import com.example.data.UserPreferencesState
import com.example.domain.CalendarHeatmapDay
import com.example.domain.DtiHealthReport
import com.example.domain.EarlyPayoffSimulationResult
import com.example.domain.EnrichedInstallmentAlert
import com.example.domain.FinancialIntelligenceEngine
import com.example.domain.InstallmentScheduleEngine
import com.example.domain.MonthlyForwardLoadBar
import com.example.domain.QaDiagnosticsEngine
import com.example.domain.QaSuiteResult
import com.example.export.A4ReportPayload
import com.example.export.ReportExportEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TarazinoNavTab(val index: Int, val titleFa: String) {
    DASHBOARD(0, "داشبورد"),
    PORTFOLIO_TREE(1, "پوشه اقساط"),
    INTELLIGENCE(2, "هوش مالی"),
    REPORTS_SETTINGS(3, "گزارش و تنظیمات")
}

data class TreeDrillState(
    val selectedCategory: EntityCategory? = null,
    val selectedCounterpartyId: Long? = null,
    val selectedObligationId: Long? = null
)

data class WizardLaunchPreset(
    val isOpen: Boolean = false,
    val preselectedCategory: EntityCategory? = null,
    val preselectedCounterpartyId: Long? = null
)

data class InteractiveTourStep(
    val stepIndex: Int,
    val targetTab: TarazinoNavTab,
    val badgeTitleFa: String,
    val headlineFa: String,
    val descriptionFa: String,
    val highlightFeaturesFa: List<String>
)

data class TarazinoUiState(
    val isLoading: Boolean = false,
    val activeTab: TarazinoNavTab = TarazinoNavTab.DASHBOARD,
    val treeDrill: TreeDrillState = TreeDrillState(),
    val wizardPreset: WizardLaunchPreset = WizardLaunchPreset(),
    val preferences: UserPreferencesState = UserPreferencesState(),
    val counterparties: List<CounterpartyEntity> = emptyList(),
    val obligations: List<ObligationEntity> = emptyList(),
    val installments: List<InstallmentEntity> = emptyList(),
    val subRecords: List<LedgerSubRecordEntity> = emptyList(),
    val smartAlertQueue: List<EnrichedInstallmentAlert> = emptyList(),
    val dtiHealthReport: DtiHealthReport? = null,
    val payoffSimulation: EarlyPayoffSimulationResult? = null,
    val forward12MonthBars: List<MonthlyForwardLoadBar> = emptyList(),
    val heatmapYear: Int = 1405,
    val heatmapMonth: Int = 8,
    val heatmapDays: List<CalendarHeatmapDay> = emptyList(),
    val extraPayoffBudgetToman: Long = 3_000_000L,
    val isVaultLocked: Boolean = false,
    val isTourActive: Boolean = false,
    val currentTourStepIndex: Int = 0,
    val activeA4ReportPayload: A4ReportPayload? = null,
    val qaSuiteResults: List<QaSuiteResult> = emptyList(),
    val toastMessageFa: String? = null,
    val celebrationMessageFa: String? = null
) {
    val totalDebtPayableToman: Long
        get() {
            val debtObIds = obligations.filter { !it.categoryEnum.isReceivable }.map { it.id }.toSet()
            return installments.filter { it.obligationId in debtObIds }.sumOf { it.effectiveTotalToman }
        }

    val totalDebtPaidToman: Long
        get() {
            val debtObIds = obligations.filter { !it.categoryEnum.isReceivable }.map { it.id }.toSet()
            return installments.filter { it.obligationId in debtObIds }.sumOf { it.paidAmountToman }
        }

    val totalDebtRemainingToman: Long
        get() {
            val debtObIds = obligations.filter { !it.categoryEnum.isReceivable }.map { it.id }.toSet()
            return installments.filter { it.obligationId in debtObIds }.sumOf { it.remainingToman }
        }

    val totalReceivableRemainingToman: Long
        get() {
            val recObIds = obligations.filter { it.categoryEnum.isReceivable }.map { it.id }.toSet()
            return installments.filter { it.obligationId in recObIds }.sumOf { it.remainingToman }
        }
}

class TarazinoViewModel(application: Application) : AndroidViewModel(application) {

    private val database = TarazinoDatabase.getInstance(application)
    private val repository = TarazinoRepository(application, database.tarazinoDao())

    private val _activeTab = MutableStateFlow(TarazinoNavTab.DASHBOARD)
    private val _treeDrill = MutableStateFlow(TreeDrillState())
    private val _wizardPreset = MutableStateFlow(WizardLaunchPreset())
    private val _extraPayoffBudgetToman = MutableStateFlow(3_000_000L)
    private val todayInit = JalaliCalendarEngine.parseJalali(JalaliCalendarEngine.todayJalali())
    private val _heatmapYearMonth = MutableStateFlow(todayInit.year to todayInit.month)
    private val _isVaultLocked = MutableStateFlow(false)
    private val _isTourActive = MutableStateFlow(false)
    private val _tourStepIndex = MutableStateFlow(0)
    private val _activeA4Report = MutableStateFlow<A4ReportPayload?>(null)
    private val _qaResults = MutableStateFlow(QaDiagnosticsEngine.runAll10DiagnosticSuites())
    private val _toastMessage = MutableStateFlow<String?>(null)
    private val _celebrationMessage = MutableStateFlow<String?>(null)

    val guidedTourSteps: List<InteractiveTourStep> = listOf(
        InteractiveTourStep(
            stepIndex = 0,
            targetTab = TarazinoNavTab.DASHBOARD,
            badgeTitleFa = "گام ۱ از ۵ — داشبورد خلوت و نمودار حلقه‌ای دسته‌ها",
            headlineFa = "به ترازینو خوش آمدید! تجربه مالی ساده، خلوت و هوشمند",
            descriptionFa = "در داشبورد بازطراحی‌شده، وضعیت کلی ماه جاری، نمودار حلقه‌ای سهم هر دسته و ۳ سررسید فوری را در یک نگاه شفاف می‌بینید.",
            highlightFeaturesFa = listOf(
                "نمودار حلقه‌ای تعاملی؛ با لمس هر دسته مستقیم وارد پوشه آن می‌شوید",
                "ثبت پرداخت تک‌کلیکی ۳ سررسید فوری بدون شلوغی صفحه",
                "دکمه چشم برای مخفی‌سازی فوری مبالغ و سوئیچ آنی تومان/ریال در نوار بالا"
            )
        ),
        InteractiveTourStep(
            stepIndex = 1,
            targetTab = TarazinoNavTab.PORTFOLIO_TREE,
            badgeTitleFa = "گام ۲ از ۵ — ساختار درختی دسته‌ها و زیرمجموعه‌ها",
            headlineFa = "دسته‌بندی درختی بر اساس طرف پرداخت (اسنپ‌پی، دیجی‌پی، بانک‌ها...)",
            descriptionFa = "در تب «پوشه اقساط»، ابتدا دسته‌های مادر (مثل خرید اقساطی یا بانک‌ها) و سپس زیرمجموعه‌های آن (مثل اسنپ‌پی و دیجی‌پی) را مشاهده می‌کنید.",
            highlightFeaturesFa = listOf(
                "مسیر درختی شفاف: دسته اصلی ← زیرمجموعه (اسنپ‌پی / دیجی‌پی) ← لیست خریدها و اقساط",
                "کپی آنی شماره کارت ۱۶ رقمی و شبا + خروجی صورتحساب A4 برای هر زیرمجموعه",
                "امکان افزودن زیرمجموعه جدید در هر دسته (مانند تارا، ازکی‌وام، بانک ملت)"
            )
        ),
        InteractiveTourStep(
            stepIndex = 2,
            targetTab = TarazinoNavTab.PORTFOLIO_TREE,
            badgeTitleFa = "گام ۳ از ۵ — ویزارد ۳ مرحله‌ای و جدول تفکیکی اقساط",
            headlineFa = "ورود و ویرایش تک‌تک قسط‌ها به صورت کاملاً تفکیک‌شده",
            descriptionFa = "با زدن دکمه «+ ثبت قسط / خرید جدید»، ویزارد ۳ مرحله‌ای باز می‌شود که در گام سوم می‌توانید مبلغ و تاریخ شمسی تک‌تک قسط‌ها را به دلخواه تغییر دهید.",
            highlightFeaturesFa = listOf(
                "ویرایش مستقیم مبلغ، تاریخ سررسید شمسی و وضعیت هر قسط به صورت مجزا",
                "افزودن یا حذف ردیف قسط و توزیع مساوی خودکار بدون خطای گرد کردن",
                "مبدل زنده عدد به حروف فارسی و چیپ‌های افزایش سریع مبلغ"
            )
        ),
        InteractiveTourStep(
            stepIndex = 3,
            targetTab = TarazinoNavTab.INTELLIGENCE,
            badgeTitleFa = "گام ۴ از ۵ — هوش مالی، تقویم حرارتی و شبیه‌ساز تسویه",
            headlineFa = "نقشه حرارتی ماه شمسی و شبیه‌ساز گلوله برفی در برابر بهمن",
            descriptionFa = "در تب «هوش مالی» دهه‌های اوج فشار مالی ماه، تقویم حرارتی ۳۰/۳۱ روزه و مقایسه استراتژی‌های تسویه زودهنگام را بررسی کنید.",
            highlightFeaturesFa = listOf(
                "نقشه حرارتی تقویم شمسی با نمایش تعهدات هر روز با یک لمس",
                "شبیه‌ساز تعاملی تسویه زودهنگام با محاسبه تاریخ دقیق آزادی از بدهی",
                "نمودار میله‌ای بار اقساط ۱۲ ماه آینده"
            )
        ),
        InteractiveTourStep(
            stepIndex = 4,
            targetTab = TarazinoNavTab.REPORTS_SETTINGS,
            badgeTitleFa = "گام ۵ از ۵ — چاپ رسمی A4 PDF، اکسل و خزانه امنیتی",
            headlineFa = "گزارش رنگی استاندارد A4، قفل پین/بیومتریک و کنترل داده‌ها",
            descriptionFa = "در تب پایانی می‌توانید گزارش رسمی A4 PDF و اکسل بگیرید، تم روشن/تیره و قفل اثر انگشت را تنظیم کنید یا داده‌های نمونه را پاکسازی نمایید.",
            highlightFeaturesFa = listOf(
                "پیش‌نمایش و چاپ مستقیم برگه استاندارد A4 رنگی و اکسل راست‌به‌چپ (.xls / .csv)",
                "قفل پین ۴ یا ۶ رقمی با صفحه‌کلید شیشه‌ای فارسی و سنسور بیومتریک",
                "دکمه «پاکسازی داده‌های نمونه (شروع از صفر)» و پشتیبان‌گیری JSON"
            )
        )
    )

    private data class CoreDataBundle(
        val counterparties: List<CounterpartyEntity>,
        val obligations: List<ObligationEntity>,
        val installments: List<InstallmentEntity>,
        val subRecords: List<LedgerSubRecordEntity>,
        val preferences: UserPreferencesState
    )

    private data class UiOverlayBundle(
        val activeTab: TarazinoNavTab,
        val treeDrill: TreeDrillState,
        val wizardPreset: WizardLaunchPreset,
        val extraBudget: Long,
        val heatmapYm: Pair<Int, Int>,
        val isLocked: Boolean,
        val isTourActive: Boolean,
        val tourStep: Int,
        val activeReport: A4ReportPayload?,
        val qaResults: List<QaSuiteResult>,
        val toast: String?,
        val celebration: String?
    )

    private val coreDataFlow = combine(
        repository.counterpartiesFlow,
        repository.obligationsFlow,
        repository.installmentsFlow,
        repository.subRecordsFlow,
        repository.preferencesState
    ) { cp, ob, inst, sub, prefs ->
        CoreDataBundle(cp, ob, inst, sub, prefs)
    }

    private val uiOverlayFlow = combine(
        combine(_activeTab, _treeDrill, _wizardPreset) { t, d, w -> Triple(t, d, w) },
        combine(_extraPayoffBudgetToman, _heatmapYearMonth, _isVaultLocked) { b, ym, l -> Triple(b, ym, l) },
        combine(_isTourActive, _tourStepIndex, _activeA4Report) { tour, step, rep -> Triple(tour, step, rep) },
        combine(_qaResults, _toastMessage, _celebrationMessage) { qa, tst, cel -> Triple(qa, tst, cel) }
    ) { g1, g2, g3, g4 ->
        UiOverlayBundle(
            activeTab = g1.first,
            treeDrill = g1.second,
            wizardPreset = g1.third,
            extraBudget = g2.first,
            heatmapYm = g2.second,
            isLocked = g2.third,
            isTourActive = g3.first,
            tourStep = g3.second,
            activeReport = g3.third,
            qaResults = g4.first,
            toast = g4.second,
            celebration = g4.third
        )
    }

    val uiState: StateFlow<TarazinoUiState> = combine(coreDataFlow, uiOverlayFlow) { core, over ->
        val today = JalaliCalendarEngine.todayJalali()
        val alerts = FinancialIntelligenceEngine.buildSmartAlertQueue(
            installments = core.installments,
            obligations = core.obligations,
            counterparties = core.counterparties,
            todayJalali = today,
            alertAdvanceDays = core.preferences.alertAdvanceDays
        )
        val dtiReport = FinancialIntelligenceEngine.analyzeDtiAndCashflowHealth(
            installments = core.installments,
            obligations = core.obligations,
            monthlyIncomeToman = core.preferences.monthlyIncomeToman,
            todayJalali = today
        )
        val simResult = FinancialIntelligenceEngine.simulateEarlyPayoffStrategies(
            obligations = core.obligations,
            installments = core.installments,
            counterparties = core.counterparties,
            extraMonthlyBudgetToman = over.extraBudget,
            todayJalali = today
        )
        val forwardBars = FinancialIntelligenceEngine.build12MonthForwardLoad(
            installments = core.installments,
            obligations = core.obligations,
            todayJalali = today
        )
        val heatmapDays = FinancialIntelligenceEngine.buildMonthlyCalendarHeatmap(
            year = over.heatmapYm.first,
            month = over.heatmapYm.second,
            installments = core.installments,
            obligations = core.obligations,
            counterparties = core.counterparties,
            todayJalali = today
        )

        TarazinoUiState(
            isLoading = false,
            activeTab = over.activeTab,
            treeDrill = over.treeDrill,
            wizardPreset = over.wizardPreset,
            preferences = core.preferences,
            counterparties = core.counterparties,
            obligations = core.obligations,
            installments = core.installments,
            subRecords = core.subRecords,
            smartAlertQueue = alerts,
            dtiHealthReport = dtiReport,
            payoffSimulation = simResult,
            forward12MonthBars = forwardBars,
            heatmapYear = over.heatmapYm.first,
            heatmapMonth = over.heatmapYm.second,
            heatmapDays = heatmapDays,
            extraPayoffBudgetToman = over.extraBudget,
            isVaultLocked = over.isLocked,
            isTourActive = over.isTourActive,
            currentTourStepIndex = over.tourStep,
            activeA4ReportPayload = over.activeReport,
            qaSuiteResults = over.qaResults,
            toastMessageFa = over.toast,
            celebrationMessageFa = over.celebration
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TarazinoUiState(isLoading = true)
    )

    init {
        viewModelScope.launch {
            repository.ensureSeedDataLoadedIfFirstLaunch()
            val prefs = repository.preferencesState.value
            if (prefs.pinHash.isNotBlank()) {
                _isVaultLocked.value = true
            }
            if (!prefs.hasCompletedGuidedTour) {
                startGuidedTour()
            }
        }
    }

    // Navigation & Hierarchical Tree Drill Controls
    fun selectTab(tab: TarazinoNavTab) {
        _activeTab.value = tab
    }

    fun openCategoryFolderInTree(category: EntityCategory?) {
        _treeDrill.value = TreeDrillState(
            selectedCategory = category,
            selectedCounterpartyId = null,
            selectedObligationId = null
        )
        _activeTab.value = TarazinoNavTab.PORTFOLIO_TREE
    }

    fun selectTreeCounterparty(counterparty: CounterpartyEntity) {
        _treeDrill.value = TreeDrillState(
            selectedCategory = counterparty.categoryEnum,
            selectedCounterpartyId = counterparty.id,
            selectedObligationId = null
        )
        _activeTab.value = TarazinoNavTab.PORTFOLIO_TREE
    }

    fun selectTreeObligation(obligation: ObligationEntity, counterpartyCategory: EntityCategory?) {
        _treeDrill.value = TreeDrillState(
            selectedCategory = counterpartyCategory ?: _treeDrill.value.selectedCategory,
            selectedCounterpartyId = obligation.counterpartyId,
            selectedObligationId = obligation.id
        )
        _activeTab.value = TarazinoNavTab.PORTFOLIO_TREE
    }

    fun stepBackInTree(): Boolean {
        val cur = _treeDrill.value
        return when {
            cur.selectedObligationId != null -> {
                _treeDrill.value = cur.copy(selectedObligationId = null)
                true
            }
            cur.selectedCounterpartyId != null -> {
                _treeDrill.value = cur.copy(selectedCounterpartyId = null)
                true
            }
            cur.selectedCategory != null -> {
                _treeDrill.value = TreeDrillState()
                true
            }
            else -> false
        }
    }

    fun resetTreeToRoot() {
        _treeDrill.value = TreeDrillState()
    }

    // 3-Step Wizard Controls
    fun openStepByStepWizard(
        preselectedCategory: EntityCategory? = null,
        preselectedCounterpartyId: Long? = null
    ) {
        _wizardPreset.value = WizardLaunchPreset(
            isOpen = true,
            preselectedCategory = preselectedCategory ?: _treeDrill.value.selectedCategory,
            preselectedCounterpartyId = preselectedCounterpartyId ?: _treeDrill.value.selectedCounterpartyId
        )
    }

    fun closeStepByStepWizard() {
        _wizardPreset.value = WizardLaunchPreset(isOpen = false)
    }

    fun saveWizardObligationAndCustomInstallments(
        obligation: ObligationEntity,
        customInstallments: List<InstallmentEntity>,
        newCounterpartyToCreate: CounterpartyEntity? = null
    ) {
        viewModelScope.launch {
            val today = JalaliCalendarEngine.todayJalali()
            val finalCounterpartyId = if (newCounterpartyToCreate != null) {
                repository.saveCounterparty(newCounterpartyToCreate.copy(createdAtJalali = today))
            } else {
                obligation.counterpartyId
            }

            val totalFromRows = customInstallments.sumOf { it.amountToman }
            val finalPrincipal = if (totalFromRows > 0L) {
                (totalFromRows - obligation.profitOrFeeToman + obligation.downPaymentToman).coerceAtLeast(totalFromRows)
            } else {
                obligation.principalAmountToman
            }

            val newObId = repository.createObligationWithSchedule(
                obligation = obligation.copy(
                    counterpartyId = finalCounterpartyId,
                    principalAmountToman = finalPrincipal,
                    installmentCount = customInstallments.size.coerceAtLeast(1),
                    firstDueDateJalali = customInstallments.firstOrNull()?.dueDateJalali ?: obligation.firstDueDateJalali,
                    createdAtJalali = today
                ),
                installments = customInstallments
            )

            _wizardPreset.value = WizardLaunchPreset(isOpen = false)
            _activeTab.value = TarazinoNavTab.PORTFOLIO_TREE
            _treeDrill.value = TreeDrillState(
                selectedCategory = newCounterpartyToCreate?.categoryEnum ?: _treeDrill.value.selectedCategory,
                selectedCounterpartyId = finalCounterpartyId,
                selectedObligationId = newObId
            )
            showToast("«${obligation.title}» با ${customInstallments.size} قسط تفکیک‌شده ثبت شد.")
        }
    }

    // Interactive Tour Controls
    fun startGuidedTour() {
        _tourStepIndex.value = 0
        _activeTab.value = guidedTourSteps[0].targetTab
        _isTourActive.value = true
    }

    fun nextTourStep() {
        val next = _tourStepIndex.value + 1
        if (next < guidedTourSteps.size) {
            _tourStepIndex.value = next
            _activeTab.value = guidedTourSteps[next].targetTab
        } else {
            finishGuidedTour()
        }
    }

    fun previousTourStep() {
        val prev = (_tourStepIndex.value - 1).coerceAtLeast(0)
        _tourStepIndex.value = prev
        _activeTab.value = guidedTourSteps[prev].targetTab
    }

    fun finishGuidedTour() {
        _isTourActive.value = false
        _activeTab.value = TarazinoNavTab.DASHBOARD
        repository.updatePreferences { it.copy(hasCompletedGuidedTour = true) }
    }

    // Toast & Celebration Banner
    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun clearCelebration() {
        _celebrationMessage.value = null
    }

    // Quick Pay, Partial Pay, Granular Installment Edit & Snooze
    fun quickPayInstallment(installment: InstallmentEntity, trackingCode: String = "") {
        viewModelScope.launch {
            val today = JalaliCalendarEngine.todayJalali()
            val updated = InstallmentScheduleEngine.applyPaymentUpdate(
                installment = installment,
                paidAmountToman = installment.effectiveTotalToman,
                paidDateJalali = today,
                trackingCode = trackingCode.ifBlank { installment.trackingCode.ifBlank { "PAY-${System.currentTimeMillis() % 10000}" } },
                todayJalali = today
            )
            repository.updateInstallment(updated)
            _celebrationMessage.value = "تبریک! «${installment.title}» با موفقیت تسویه شد."
        }
    }

    fun applyDetailedInstallmentPayment(
        installment: InstallmentEntity,
        customTitle: String = installment.title,
        customBaseAmountToman: Long = installment.amountToman,
        customDueDateJalali: String = installment.dueDateJalali,
        paidAmountToman: Long,
        lateFeeToman: Long,
        discountToman: Long,
        paidDateJalali: String,
        trackingCode: String,
        note: String
    ) {
        viewModelScope.launch {
            val baseAdjusted = installment.copy(
                title = customTitle.ifBlank { installment.title },
                amountToman = customBaseAmountToman.coerceAtLeast(0L),
                dueDateJalali = customDueDateJalali.ifBlank { installment.dueDateJalali }
            )
            val updated = InstallmentScheduleEngine.applyPaymentUpdate(
                installment = baseAdjusted,
                paidAmountToman = paidAmountToman,
                lateFeeToman = lateFeeToman,
                discountToman = discountToman,
                paidDateJalali = paidDateJalali,
                trackingCode = trackingCode,
                note = note
            )
            repository.updateInstallment(updated)
            if (updated.statusEnum == InstallmentStatus.PAID) {
                _celebrationMessage.value = "«${updated.title}» به طور کامل تسویه شد!"
            } else {
                showToast("جزئیات «${updated.title}» به‌روزرسانی شد.")
            }
        }
    }

    fun addSingleCustomInstallmentToObligation(
        obligationId: Long,
        nextNumber: Int,
        title: String,
        dueDateJalali: String,
        amountToman: Long,
        isAlreadyPaid: Boolean,
        note: String
    ) {
        viewModelScope.launch {
            val today = JalaliCalendarEngine.todayJalali()
            val status = when {
                isAlreadyPaid -> InstallmentStatus.PAID
                JalaliCalendarEngine.diffJalaliDays(today, dueDateJalali) < 0 -> InstallmentStatus.OVERDUE
                else -> InstallmentStatus.PENDING
            }
            repository.addManualInstallment(
                InstallmentEntity(
                    obligationId = obligationId,
                    installmentNumber = nextNumber,
                    title = title,
                    dueDateJalali = dueDateJalali,
                    amountToman = amountToman,
                    paidAmountToman = if (isAlreadyPaid) amountToman else 0L,
                    status = status.name,
                    paidDateJalali = if (isAlreadyPaid) today else "",
                    note = note
                )
            )
            showToast("قسط جدید «$title» به جدول اضافه شد.")
        }
    }

    fun deleteSingleInstallment(installmentId: Long, title: String) {
        viewModelScope.launch {
            repository.deleteInstallmentById(installmentId)
            showToast("ردیف «$title» حذف شد.")
        }
    }

    fun snoozeInstallmentTomorrow(installment: InstallmentEntity) {
        viewModelScope.launch {
            val tomorrow = JalaliCalendarEngine.addJalaliDays(JalaliCalendarEngine.todayJalali(), 1)
            repository.updateInstallment(installment.copy(snoozedUntilJalali = tomorrow))
            showToast("یادآوری «${installment.title}» به فردا منتقل شد.")
        }
    }

    // Counterparty (Sub-Category Provider) Actions
    fun saveCounterparty(entity: CounterpartyEntity) {
        viewModelScope.launch {
            val createdDate = entity.createdAtJalali.ifBlank { JalaliCalendarEngine.todayJalali() }
            val savedId = repository.saveCounterparty(entity.copy(createdAtJalali = createdDate))
            _treeDrill.value = _treeDrill.value.copy(
                selectedCategory = entity.categoryEnum,
                selectedCounterpartyId = savedId
            )
            showToast("زیرمجموعه / طرف‌حساب «${entity.name}» ذخیره شد.")
        }
    }

    fun deleteCounterparty(counterpartyId: Long, name: String) {
        viewModelScope.launch {
            repository.deleteCounterpartyAndLinkedData(counterpartyId)
            if (_treeDrill.value.selectedCounterpartyId == counterpartyId) {
                _treeDrill.value = _treeDrill.value.copy(selectedCounterpartyId = null, selectedObligationId = null)
            }
            showToast("زیرمجموعه «$name» و تعهدات مرتبط حذف شد.")
        }
    }

    fun addSubRecordToObligation(obligation: ObligationEntity, subRecord: LedgerSubRecordEntity) {
        viewModelScope.launch {
            repository.addSubRecordAndSyncLedger(obligation, subRecord)
            showToast("ریزتراکنش جدید در پرونده «${obligation.title}» ثبت شد.")
        }
    }

    fun deleteObligation(obligationId: Long, title: String) {
        viewModelScope.launch {
            repository.deleteObligation(obligationId)
            if (_treeDrill.value.selectedObligationId == obligationId) {
                _treeDrill.value = _treeDrill.value.copy(selectedObligationId = null)
            }
            showToast("پرونده «$title» حذف شد.")
        }
    }

    // Intelligence Simulator & Heatmap Controls
    fun updateExtraPayoffBudget(budgetToman: Long) {
        _extraPayoffBudgetToman.value = budgetToman.coerceAtLeast(0L)
    }

    fun changeHeatmapMonth(deltaMonths: Int) {
        val (y, m) = _heatmapYearMonth.value
        val cur = "%04d/%02d/01".format(java.util.Locale.US, y, m)
        val next = JalaliCalendarEngine.parseJalali(JalaliCalendarEngine.addJalaliMonths(cur, deltaMonths))
        _heatmapYearMonth.value = next.year to next.month
    }

    // A4 Report & Excel Export Studio Controls
    fun openA4ReportPreview(
        reportTitleFa: String = "گزارش جامع وضعیت اقساط، وام‌ها و تعهدات مالی",
        subtitleFa: String = "صورتحساب رسمی ترازینو — تفکیک کامل اقساط، پرداخت‌ها و مانده تعهدات",
        filterCounterpartyId: Long? = null,
        filterObligationId: Long? = null
    ) {
        val state = uiState.value
        val payload = ReportExportEngine.buildReportPayload(
            reportTitleFa = reportTitleFa,
            subtitleFa = subtitleFa,
            obligations = state.obligations,
            installments = state.installments,
            counterparties = state.counterparties,
            currencyUnit = state.preferences.currencyUnit,
            filterCounterpartyId = filterCounterpartyId,
            filterObligationId = filterObligationId
        )
        _activeA4Report.value = payload
    }

    fun closeA4ReportPreview() {
        _activeA4Report.value = null
    }

    fun exportA4Pdf(context: Context, payload: A4ReportPayload) {
        viewModelScope.launch {
            val file = ReportExportEngine.saveAndSharePdfFile(context, payload)
            showToast("فایل PDF استاندارد A4 (${file.name}) آماده اشتراک‌گذاری و چاپ شد.")
        }
    }

    fun exportRtlExcel(context: Context, payload: A4ReportPayload) {
        viewModelScope.launch {
            val file = ReportExportEngine.saveAndShareRtlExcelFile(context, payload)
            showToast("فایل اکسل راست‌به‌چپ (${file.name}) تولید شد.")
        }
    }

    fun exportUtf8Csv(context: Context, payload: A4ReportPayload) {
        viewModelScope.launch {
            val file = ReportExportEngine.saveAndShareCsvFile(context, payload)
            showToast("فایل CSV مجهز به UTF-8 BOM (${file.name}) تولید شد.")
        }
    }

    // Security Vault Controls
    fun togglePrivacyMask() {
        repository.updatePreferences { it.copy(privacyMaskEnabled = !it.privacyMaskEnabled) }
    }

    fun lockAppImmediately() {
        if (repository.preferencesState.value.pinHash.isNotBlank()) {
            _isVaultLocked.value = true
        } else {
            showToast("ابتدا از بخش تنظیمات امنیت یک رمز پین ۴ یا ۶ رقمی تعریف کنید.")
        }
    }

    fun configureSecurityPin(
        newPin: String,
        pinLength: Int,
        biometricEnabled: Boolean,
        recoveryQuestion: String,
        recoveryAnswer: String
    ) {
        if (!SecurityVaultEngine.isValidPinFormat(newPin)) {
            showToast("رمز پین باید دقیقاً ۴ یا ۶ رقم عددی باشد.")
            return
        }
        val pinHash = SecurityVaultEngine.hashWithSalt(newPin)
        val answerHash = if (recoveryAnswer.isNotBlank()) {
            SecurityVaultEngine.hashWithSalt(recoveryAnswer)
        } else {
            repository.preferencesState.value.recoveryAnswerHash
        }
        repository.updatePreferences {
            it.copy(
                pinHash = pinHash,
                pinLength = pinLength,
                biometricEnabled = biometricEnabled,
                recoveryQuestion = recoveryQuestion.ifBlank { it.recoveryQuestion },
                recoveryAnswerHash = answerHash
            )
        }
        showToast("قفل امنیتی پین با هش SHA-256 فعال و ذخیره شد.")
    }

    fun removeSecurityPin() {
        repository.updatePreferences {
            it.copy(pinHash = "", biometricEnabled = false)
        }
        _isVaultLocked.value = false
        showToast("قفل رمز عبور برنامه غیرفعال شد.")
    }

    fun attemptUnlockWithPin(inputPin: String): Boolean {
        val storedHash = repository.preferencesState.value.pinHash
        val verified = SecurityVaultEngine.verifyPin(inputPin, storedHash)
        if (verified) {
            _isVaultLocked.value = false
        }
        return verified
    }

    fun unlockViaBiometricSensor() {
        _isVaultLocked.value = false
        showToast("احراز هویت بیومتریک با موفقیت انجام شد.")
    }

    fun attemptRecoveryUnlock(answer: String): Boolean {
        val storedAnswerHash = repository.preferencesState.value.recoveryAnswerHash
        val ok = SecurityVaultEngine.verifyRecoveryAnswer(answer, storedAnswerHash)
        if (ok) {
            _isVaultLocked.value = false
            showToast("بازیابی امنیتی تایید شد. قفل برنامه باز گردید.")
        }
        return ok
    }

    // Personalization & Settings
    fun setThemeMode(mode: TarazinoThemeMode) {
        repository.updatePreferences { it.copy(themeMode = mode) }
    }

    fun setCurrencyUnit(unit: CurrencyUnit) {
        repository.updatePreferences { it.copy(currencyUnit = unit) }
    }

    fun setFontScale(scale: FontScalePreset) {
        repository.updatePreferences { it.copy(fontScale = scale) }
    }

    fun updateMonthlyIncomeAndAlertDays(monthlyIncomeToman: Long, alertDays: Int) {
        repository.updatePreferences {
            it.copy(
                monthlyIncomeToman = monthlyIncomeToman.coerceAtLeast(1_000_000L),
                alertAdvanceDays = alertDays.coerceIn(1, 30)
            )
        }
        showToast("درآمد ماهانه و بازه هشدار هوشمند به‌روزرسانی شد.")
    }

    // Data Seed, Reset & JSON Backup
    fun clearAllDataToZero() {
        viewModelScope.launch {
            repository.clearAllDataToZero()
            _treeDrill.value = TreeDrillState()
            showToast("تمامی داده‌های نمونه پاکسازی شد. اکنون می‌توانید اطلاعات واقعی خود را ثبت کنید.")
        }
    }

    fun restoreDemoSeedData() {
        viewModelScope.launch {
            repository.reloadDemoSeedData()
            _treeDrill.value = TreeDrillState()
            showToast("داده‌های نمونه ساختار درختی (اسنپ‌پی، دیجی‌پی، بانک مهر و...) بازگردانی شد.")
        }
    }

    fun exportFullBackupJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportFullBackupJson()
            onResult(json)
            showToast("پشتیبان کامل JSON آماده کپی و ذخیره‌سازی شد.")
        }
    }

    fun importFullBackupJson(jsonStr: String) {
        viewModelScope.launch {
            val ok = repository.importFullBackupJson(jsonStr)
            if (ok) {
                showToast("فایل پشتیبان JSON با موفقیت بازیابی شد!")
            } else {
                showToast("خطا در ساختار فایل JSON واردشده.")
            }
        }
    }

    fun reRunQaDiagnostics() {
        _qaResults.value = QaDiagnosticsEngine.runAll10DiagnosticSuites()
        showToast("هر ۱۰ سوئیت تست خودکار QA با موفقیت اجرا و بررسی شدند.")
    }
}
