package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.CurrencyUnit
import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.core.SecurityVaultEngine
import com.example.data.CounterpartyEntity
import com.example.data.EntityCategory
import com.example.data.FontScalePreset
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.LedgerSubRecordEntity
import com.example.data.ObligationEntity
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
    val targetElementId: String,
    val scrollItemIndex: Int,
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
    private val _toastMessage = MutableStateFlow<String?>(null)
    private val _celebrationMessage = MutableStateFlow<String?>(null)

    val guidedTourSteps: List<InteractiveTourStep> = listOf(
        InteractiveTourStep(
            stepIndex = 0,
            targetTab = TarazinoNavTab.DASHBOARD,
            targetElementId = "top_bar_controls",
            scrollItemIndex = 0,
            badgeTitleFa = "گام ۱ از ۹ — نوار ابزار سریع بالا",
            headlineFa = "تغییر آنی تومان/ریال، مخفی‌سازی ارقام و قفل امنیتی",
            descriptionFa = "از دکمه‌های بالای صفحه می‌توانید در هر لحظه واحد پول را بین تومان و ریال تغییر دهید، مبالغ را در محیط عمومی مخفی کنید یا برنامه را قفل نمایید.",
            highlightFeaturesFa = listOf(
                "سوئیچ آنی تومان و ریال در کل صفحات و گزارش‌ها",
                "دکمه چشم برای مخفی‌سازی فوری مبالغ در حضور دیگران"
            )
        ),
        InteractiveTourStep(
            stepIndex = 1,
            targetTab = TarazinoNavTab.DASHBOARD,
            targetElementId = "dashboard_monthly_summary",
            scrollItemIndex = 0,
            badgeTitleFa = "گام ۲ از ۹ — کارت فرماندهی وضعیت ماه",
            headlineFa = "خلاصه وضعیت ماه جاری، ثبت قسط جدید و چاپ A4",
            descriptionFa = "در این کارت درصد پیشرفت تسویه کل، اقساط مانده ماه جاری و دو دکمه اصلی «ثبت قسط / خرید جدید» و «گزارش A4 و اکسل» همیشه در دسترس شماست.",
            highlightFeaturesFa = listOf(
                "حلقه گرافیکی درصد تسویه کل تعهدات",
                "باز کردن ویزارد ۳ مرحله‌ای ثبت قسط با یک کلیک"
            )
        ),
        InteractiveTourStep(
            stepIndex = 2,
            targetTab = TarazinoNavTab.DASHBOARD,
            targetElementId = "dashboard_category_donut",
            scrollItemIndex = 1,
            badgeTitleFa = "گام ۳ از ۹ — نمودار حلقه‌ای سهم هر دسته",
            headlineFa = "مشاهده سهم هر دسته و ورود مستقیم به پوشه اسنپ‌پی/دیجی‌پی",
            descriptionFa = "این نمودار سهم هر دسته (خرید اقساطی، بانک‌ها، صندوق‌ها و اشخاص) را نشان می‌دهد. با لمس هر دسته یا میانبرهای پایین آن، مستقیم وارد پوشه آن می‌شوید.",
            highlightFeaturesFa = listOf(
                "تفکیک رنگی سهم هر دسته از بدهی‌های فعال",
                "چیپ‌های میانبر برای ورود مستقیم به اسنپ‌پی، دیجی‌پی، بانک مهر و..."
            )
        ),
        InteractiveTourStep(
            stepIndex = 3,
            targetTab = TarazinoNavTab.DASHBOARD,
            targetElementId = "dashboard_urgent_alerts",
            scrollItemIndex = 3,
            badgeTitleFa = "گام ۴ از ۹ — ۳ سررسید فوری و پرداخت تک‌کلیکی",
            headlineFa = "تسویه سریع نزدیک‌ترین اقساط بدون شلوغی صفحه",
            descriptionFa = "مهم‌ترین اقساط نزدیک یا معوقه در این بخش نمایش داده می‌شوند تا با یک کلیک «ثبت پرداخت سریع» یا «یادآوری فردا» را انجام دهید.",
            highlightFeaturesFa = listOf(
                "اولویت‌بندی هوشمند بر اساس فوریت سررسید و چک صیادی",
                "دکمه مشاهده همه سررسیدها برای فیلتر کامل هشدارها"
            )
        ),
        InteractiveTourStep(
            stepIndex = 4,
            targetTab = TarazinoNavTab.PORTFOLIO_TREE,
            targetElementId = "tree_command_bar",
            scrollItemIndex = 0,
            badgeTitleFa = "گام ۵ از ۹ — مدیریت درختی، جستجو و فیلتر هوشمند",
            headlineFa = "ثبت قسط مرحله‌ای، جستجوی آنی و مرتب‌سازی اقساط",
            descriptionFa = "در تب «پوشه اقساط» می‌توانید با دکمه «ثبت قسط جدید» تک‌تک اقساط را به صورت تفکیک‌شده وارد کنید یا اقساط را بر اساس مبلغ و فوریت مرتب نمایید.",
            highlightFeaturesFa = listOf(
                "ویزارد ۳ مرحله‌ای با جدول قابل ویرایش برای مبلغ و تاریخ تک‌تک قسط‌ها",
                "فیلتر و مرتب‌سازی هوشمند بر اساس فوری‌ترین سررسید یا بیشترین مبلغ"
            )
        ),
        InteractiveTourStep(
            stepIndex = 5,
            targetTab = TarazinoNavTab.PORTFOLIO_TREE,
            targetElementId = "tree_parent_category_bnpl",
            scrollItemIndex = 1,
            badgeTitleFa = "گام ۶ از ۹ — پوشه‌های درختی طرف پرداخت",
            headlineFa = "ساختار درختی: دسته اصلی ← اسنپ‌پی / دیجی‌پی ← تسویه گروهی",
            descriptionFa = "با باز کردن هر پوشه یا لمس چیپ زیرمجموعه (مثل اسنپ‌پی یا دیجی‌پی)، لیست خریدها و جدول اقساط آن باز شده و می‌توانید چند قسط را یکجا تسویه کنید.",
            highlightFeaturesFa = listOf(
                "ویرایش سریع پرونده وام/خرید و افزودن زیرمجموعه جدید در هر دسته",
                "امکان انتخاب چند قسط با هم و تسویه گروهی یکجا (Batch Pay)"
            )
        ),
        InteractiveTourStep(
            stepIndex = 6,
            targetTab = TarazinoNavTab.INTELLIGENCE,
            targetElementId = "intel_budget_planner",
            scrollItemIndex = 0,
            badgeTitleFa = "گام ۷ از ۹ — برنامه‌ریزی بودجه ماهانه و سقف قسط جدید",
            headlineFa = "محاسبه هوشمند «سقف مجاز اقساط جدید» در ماه",
            descriptionFa = "در تب «هوش مالی»، با تنظیم درآمد ماهانه و درصد هزینه‌های ضروری زندگی، ترازینو دقیقاً محاسبه می‌کند چقدر ظرفیت امن برای اضافه کردن قسط جدید دارید.",
            highlightFeaturesFa = listOf(
                "نمایش سقف مجاز تعهد جدید بر اساس درآمد و هزینه‌های زندگی",
                "تحلیل دهه‌های اوج فشار مالی ماه (دهه اول، دوم و سوم)"
            )
        ),
        InteractiveTourStep(
            stepIndex = 7,
            targetTab = TarazinoNavTab.INTELLIGENCE,
            targetElementId = "intel_payoff_simulator",
            scrollItemIndex = 3,
            badgeTitleFa = "گام ۸ از ۹ — شبیه‌ساز تسویه زودهنگام بدهی‌ها",
            headlineFa = "مقایسه استراتژی بهمن و گلوله برفی برای آزادی مالی",
            descriptionFa = "با اسلایدر بودجه مازاد ماهانه ببینید چند ماه زودتر و در چه تاریخ شمسی دقیق از تمام بدهی‌ها رها می‌شوید.",
            highlightFeaturesFa = listOf(
                "سوییچر متقارن استراتژی بهمن (حداقل ریسک) و گلوله برفی (حذف سریع خرده‌بدهی)",
                "تقویم حرارتی ۳۰/۳۱ روزه ماه شمسی"
            )
        ),
        InteractiveTourStep(
            stepIndex = 8,
            targetTab = TarazinoNavTab.REPORTS_SETTINGS,
            targetElementId = "reports_a4_export_studio",
            scrollItemIndex = 1,
            badgeTitleFa = "گام ۹ از ۹ — استودیو چاپ A4، اکسل و امنیت",
            headlineFa = "گزارش رسمی A4 رنگی، اکسل راست‌به‌چپ و شخصی‌سازی",
            descriptionFa = "در تب پایانی می‌توانید پیش‌نمایش برگه A4 رنگی را ببینید و چاپ کنید، خروجی اکسل بگیرید، پوسته روشن/تیره و قفل پین/بیومتریک را تنظیم نمایید.",
            highlightFeaturesFa = listOf(
                "دکمه‌های مجزا برای پیش‌نمایش A4، فایل PDF، اکسل (.xls) و CSV",
                "پاکسازی داده‌های نمونه برای شروع از صفر یا پشتیبان‌گیری کامل JSON"
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
        combine(_toastMessage, _celebrationMessage) { tst, cel -> tst to cel }
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
            toast = g4.first,
            celebration = g4.second
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
            todayJalali = today,
            livingExpensePercent = core.preferences.livingExpensePercent
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
            showToast("«${obligation.title}» با ${PersianCurrencyEngine.toPersianDigits(customInstallments.size.toString())} قسط تفکیک‌شده ثبت شد.")
        }
    }

    // Interactive Element-by-Element Spotlight Tour Controls
    fun startGuidedTour() {
        _treeDrill.value = TreeDrillState()
        _tourStepIndex.value = 0
        _activeTab.value = guidedTourSteps[0].targetTab
        _isTourActive.value = true
    }

    fun nextTourStep() {
        val next = _tourStepIndex.value + 1
        if (next < guidedTourSteps.size) {
            _tourStepIndex.value = next
            val targetStep = guidedTourSteps[next]
            if (targetStep.targetTab == TarazinoNavTab.PORTFOLIO_TREE) {
                _treeDrill.value = TreeDrillState()
            }
            _activeTab.value = targetStep.targetTab
        } else {
            finishGuidedTour()
        }
    }

    fun previousTourStep() {
        val prev = (_tourStepIndex.value - 1).coerceAtLeast(0)
        _tourStepIndex.value = prev
        val targetStep = guidedTourSteps[prev]
        if (targetStep.targetTab == TarazinoNavTab.PORTFOLIO_TREE) {
            _treeDrill.value = TreeDrillState()
        }
        _activeTab.value = targetStep.targetTab
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

    // Quick Pay, Batch Pay, Partial Pay, Granular Installment Edit & Snooze
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

    fun batchPayInstallments(installmentIds: Set<Long>, trackingCode: String = "") {
        if (installmentIds.isEmpty()) return
        viewModelScope.launch {
            val today = JalaliCalendarEngine.todayJalali()
            val targets = uiState.value.installments.filter { it.id in installmentIds && it.statusEnum != InstallmentStatus.PAID }
            val batchCode = trackingCode.ifBlank { "BATCH-${System.currentTimeMillis() % 10000}" }
            var totalSettledToman = 0L
            targets.forEach { inst ->
                totalSettledToman += inst.remainingToman
                val updated = InstallmentScheduleEngine.applyPaymentUpdate(
                    installment = inst,
                    paidAmountToman = inst.effectiveTotalToman,
                    paidDateJalali = today,
                    trackingCode = batchCode,
                    todayJalali = today
                )
                repository.updateInstallment(updated)
            }
            val countFa = PersianCurrencyEngine.toPersianDigits(targets.size.toString())
            val amountFa = PersianCurrencyEngine.formatAmount(totalSettledToman, uiState.value.preferences.currencyUnit)
            _celebrationMessage.value = "تسویه گروهی موفق! $countFa قسط به مجموع $amountFa یکجا تسویه شد."
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

    // Counterparty (Sub-Category Provider) & Obligation Edit Actions
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

    fun updateObligationDetails(obligation: ObligationEntity) {
        viewModelScope.launch {
            repository.updateObligation(obligation)
            showToast("مشخصات پرونده «${obligation.title}» با موفقیت ویرایش شد.")
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

    // Intelligence Simulator, Budget Planner & Heatmap Controls
    fun updateExtraPayoffBudget(budgetToman: Long) {
        _extraPayoffBudgetToman.value = budgetToman.coerceAtLeast(0L)
    }

    fun changeHeatmapMonth(deltaMonths: Int) {
        val (y, m) = _heatmapYearMonth.value
        val cur = "%04d/%02d/01".format(java.util.Locale.US, y, m)
        val next = JalaliCalendarEngine.parseJalali(JalaliCalendarEngine.addJalaliMonths(cur, deltaMonths))
        _heatmapYearMonth.value = next.year to next.month
    }

    fun updateBudgetPlannerSettings(monthlyIncomeToman: Long, livingExpensePercent: Int, alertDays: Int) {
        repository.updatePreferences {
            it.copy(
                monthlyIncomeToman = monthlyIncomeToman.coerceAtLeast(1_000_000L),
                livingExpensePercent = livingExpensePercent.coerceIn(20, 80),
                alertAdvanceDays = alertDays.coerceIn(1, 30)
            )
        }
        showToast("برنامه بودجه ماهانه و سقف مجاز اقساط به‌روزرسانی شد.")
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
}
