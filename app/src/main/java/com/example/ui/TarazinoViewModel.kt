package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.CurrencyUnit
import com.example.core.JalaliCalendarEngine
import com.example.core.SecurityVaultEngine
import com.example.data.CounterpartyEntity
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
    ENTITIES(1, "طرف‌حساب‌ها"),
    OBLIGATIONS(2, "اقساط و بدهی"),
    INTELLIGENCE(3, "هوش مالی"),
    REPORTS_SETTINGS(4, "گزارش و امنیت")
}

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
            badgeTitleFa = "گام ۱ از ۶ — فرماندهی مالی و رادار سررسیدها",
            headlineFa = "به ترازینو خوش آمدید! مدیریت ۱۰۰٪ آفلاین و هوشمند اقساط",
            descriptionFa = "در داشبورد اصلی، خلاصه کل بدهی‌ها، طلب‌ها، نوار سلامت نقدینگی و هشدارهای تفکیک‌شده (معوقه، سررسید امروز و نزدیک به سررسید) را مشاهده می‌کنید.",
            highlightFeaturesFa = listOf(
                "ثبت پرداخت تک‌کلیکی و یادآوری فردا (Snooze) مستقیم از روی کارت هشدار",
                "دکمه چشم در نوار بالا برای مخفی‌سازی فوری مبالغ در محیط‌های عمومی",
                "تغییر آنی واحد پول بین تومان و ریال و سوئیچ بین تم تیره و روشن"
            )
        ),
        InteractiveTourStep(
            stepIndex = 1,
            targetTab = TarazinoNavTab.ENTITIES,
            badgeTitleFa = "گام ۲ از ۶ — پرونده جامع طرف‌حساب‌ها",
            headlineFa = "پروفایل اختصاصی برای هر بانک، پلتفرم اقساطی و شخص",
            descriptionFa = "در تب «طرف‌حساب‌ها» می‌توانید بانک‌ها (مهر، ملت، ملی...)، پلتفرم‌های خرید اقساطی (اسنپ‌پی، دیجی‌پی...)، صندوق‌های خانگی و دوستان را ثبت کنید.",
            highlightFeaturesFa = listOf(
                "ذخیره شماره کارت ۱۶ رقمی با کپی آنی و شماره شبا (IR) برای واریز سریع",
                "تعیین درجه حساسیت «قول شرف / حیاتی» برای اولویت‌بندی هوشمند در هشدارها",
                "مشاهده پرونده جامع هر طرف‌حساب و دریافت خروجی صورتحساب A4 PDF و اکسل"
            )
        ),
        InteractiveTourStep(
            stepIndex = 2,
            targetTab = TarazinoNavTab.OBLIGATIONS,
            badgeTitleFa = "گام ۳ از ۶ — جدول اقساط خودکار و حساب دفتری پویا",
            headlineFa = "محاسبه دقیق اقساط بدون حتی ۱ تومان خطای گرد کردن",
            descriptionFa = "در تب «اقساط و بدهی» می‌توانید وام‌ها، خریدهای اقساطی، چک‌های صیادی و حساب‌های دفتری موردی را در دو حالت خودکار یا دفتری مدیریت کنید.",
            highlightFeaturesFa = listOf(
                "مبدل زنده عدد به حروف فارسی و چیپ‌های افزایش سریع در زیر تمام فیلدهای مبلغ",
                "انتخابگر تقویم شمسی هوشمند با تنظیم خودکار ماه‌های ۳۰ و ۲۹ روزه",
                "امکان ثبت پرداخت جزئی (Partial)، جریمه دیرکرد، تخفیف خوش‌حسابی و کد پیگیری"
            )
        ),
        InteractiveTourStep(
            stepIndex = 3,
            targetTab = TarazinoNavTab.INTELLIGENCE,
            badgeTitleFa = "گام ۴ از ۶ — هوش مالی، تقویم حرارتی و شبیه‌ساز تسویه",
            headlineFa = "چند ماه زودتر به آزادی کامل مالی می‌رسید؟",
            descriptionFa = "موتور تصمیم‌یار آفلاین ترازینو نسبت اقساط به درآمد (DTI)، دهه‌های اوج فشار مالی ماه و مقایسه استراتژی گلوله برفی در برابر بهمن را محاسبه می‌کند.",
            highlightFeaturesFa = listOf(
                "نقشه حرارتی ۳۰/۳۱ روزه تقویم شمسی ماه جاری با نمایش تعهدات هر روز",
                "شبیه‌ساز تعاملی تسویه زودهنگام با اسلایدر بودجه مازاد ماهانه",
                "نمودار بار اقساط ۱۲ ماه آینده و تشخیص دهه بحرانی ماه (اول، دوم یا سوم)"
            )
        ),
        InteractiveTourStep(
            stepIndex = 4,
            targetTab = TarazinoNavTab.REPORTS_SETTINGS,
            badgeTitleFa = "گام ۵ از ۶ — استودیو چاپ PDF استاندارد A4 و اکسل",
            headlineFa = "گزارش‌های رسمی رنگی سایز A4 و اکسل راست‌به‌چپ",
            descriptionFa = "هر زمان اراده کنید می‌توانید صورتحساب کل تعهدات یا یک طرف‌حساب خاص را در قالب برگه استاندارد A4 رنگی (با کادر مهر و امضا) یا فایل اکسل دریافت کنید.",
            highlightFeaturesFa = listOf(
                "تولید مستقیم فایل PDF استاندارد A4 رنگی با فونت وزیرمتن و جدول زبرا",
                "خروجی اکسل راست‌به‌چپ (.xls) و فایل جهانی (.csv مجهز به UTF-8 BOM)",
                "پیش‌نمایش زنده برگه A4 قبل از اشتراک‌گذاری یا چاپ"
            )
        ),
        InteractiveTourStep(
            stepIndex = 5,
            targetTab = TarazinoNavTab.REPORTS_SETTINGS,
            badgeTitleFa = "گام ۶ از ۶ — خزانه امنیتی پین/بیومتریک و شروع به کار",
            headlineFa = "امنیت کامل آفلاین، پشتیبان‌گیری JSON و کنترل داده‌های نمونه",
            descriptionFa = "در همین تب می‌توانید قفل پین ۴ یا ۶ رقمی و اثر انگشت را فعال کنید، فایل پشتیبان JSON بگیرید یا داده‌های نمونه آزمایشی را با یک کلیک پاکسازی کنید.",
            highlightFeaturesFa = listOf(
                "صفحه‌کلید شیشه‌ای فارسی با رمزنگاری SHA-256 و سنسور بیومتریک",
                "دکمه «پاکسازی داده‌های نمونه (شروع از صفر)» برای ثبت اطلاعات واقعی خودتان",
                "مرکز حافظه ایجنت و اجرای خودکار ۱۰ سوئیت تست صحت‌سنجی (QA)"
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
        combine(_activeTab, _extraPayoffBudgetToman, _heatmapYearMonth) { t, b, ym -> Triple(t, b, ym) },
        combine(_isVaultLocked, _isTourActive, _tourStepIndex) { l, tour, step -> Triple(l, tour, step) },
        combine(_activeA4Report, _qaResults, _toastMessage, _celebrationMessage) { rep, qa, tst, cel ->
            listOf(rep, qa, tst, cel)
        }
    ) { g1, g2, g3 ->
        @Suppress("UNCHECKED_CAST")
        UiOverlayBundle(
            activeTab = g1.first,
            extraBudget = g1.second,
            heatmapYm = g1.third,
            isLocked = g2.first,
            isTourActive = g2.second,
            tourStep = g2.third,
            activeReport = g3[0] as A4ReportPayload?,
            qaResults = g3[1] as List<QaSuiteResult>,
            toast = g3[2] as String?,
            celebration = g3[3] as String?
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

    // Navigation & Interactive Tour Controls
    fun selectTab(tab: TarazinoNavTab) {
        _activeTab.value = tab
    }

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

    // Quick Pay, Partial Pay & Snooze
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
            _celebrationMessage.value = "تبریک! «${installment.title}» با موفقیت تسویه و در پرونده ثبت شد."
        }
    }

    fun applyDetailedInstallmentPayment(
        installment: InstallmentEntity,
        paidAmountToman: Long,
        lateFeeToman: Long,
        discountToman: Long,
        paidDateJalali: String,
        trackingCode: String,
        note: String
    ) {
        viewModelScope.launch {
            val updated = InstallmentScheduleEngine.applyPaymentUpdate(
                installment = installment,
                paidAmountToman = paidAmountToman,
                lateFeeToman = lateFeeToman,
                discountToman = discountToman,
                paidDateJalali = paidDateJalali,
                trackingCode = trackingCode,
                note = note
            )
            repository.updateInstallment(updated)
            if (updated.statusEnum == InstallmentStatus.PAID) {
                _celebrationMessage.value = "«${installment.title}» به طور کامل تسویه شد!"
            } else {
                showToast("وضعیت قسط «${installment.title}» به‌روزرسانی شد.")
            }
        }
    }

    fun snoozeInstallmentTomorrow(installment: InstallmentEntity) {
        viewModelScope.launch {
            val tomorrow = JalaliCalendarEngine.addJalaliDays(JalaliCalendarEngine.todayJalali(), 1)
            repository.updateInstallment(installment.copy(snoozedUntilJalali = tomorrow))
            showToast("یادآوری «${installment.title}» به فردا ($tomorrow) منتقل شد.")
        }
    }

    // Counterparty Actions
    fun saveCounterparty(entity: CounterpartyEntity) {
        viewModelScope.launch {
            val createdDate = entity.createdAtJalali.ifBlank { JalaliCalendarEngine.todayJalali() }
            repository.saveCounterparty(entity.copy(createdAtJalali = createdDate))
            showToast("پروفایل طرف‌حساب «${entity.name}» ذخیره شد.")
        }
    }

    fun deleteCounterparty(counterpartyId: Long, name: String) {
        viewModelScope.launch {
            repository.deleteCounterpartyAndLinkedData(counterpartyId)
            showToast("طرف‌حساب «$name» و تعهدات مرتبط حذف شد.")
        }
    }

    // Obligation & Running Ledger Actions
    fun createObligationAutoSchedule(obligation: ObligationEntity) {
        viewModelScope.launch {
            val today = JalaliCalendarEngine.todayJalali()
            val generated = InstallmentScheduleEngine.generateAutoInstallments(
                obligationId = 0L,
                titlePrefix = obligation.title,
                netPayableToman = obligation.netPayableToman,
                installmentCount = obligation.installmentCount,
                firstDueDateJalali = obligation.firstDueDateJalali,
                interval = obligation.intervalEnum,
                todayJalali = today
            )
            repository.createObligationWithSchedule(
                obligation = obligation.copy(
                    scheduleMode = ScheduleMode.AUTO_SCHEDULE.name,
                    createdAtJalali = today
                ),
                installments = generated
            )
            showToast("تعهد «${obligation.title}» با ${generated.size} ردیف قسط دقیق ثبت شد.")
        }
    }

    fun createObligationRunningLedger(
        obligation: ObligationEntity,
        firstItemTitle: String,
        firstItemAmountToman: Long,
        firstItemDateJalali: String,
        receiptCode: String
    ) {
        viewModelScope.launch {
            val today = JalaliCalendarEngine.todayJalali()
            val initialInstallment = InstallmentEntity(
                obligationId = 0L,
                installmentNumber = 1,
                title = firstItemTitle.ifBlank { obligation.title },
                dueDateJalali = obligation.firstDueDateJalali,
                amountToman = firstItemAmountToman.coerceAtLeast(obligation.principalAmountToman),
                status = InstallmentStatus.PENDING.name,
                trackingCode = receiptCode
            )
            val initialSub = LedgerSubRecordEntity(
                obligationId = 0L,
                title = firstItemTitle.ifBlank { "ثبت اولیه حساب دفتری" },
                dateJalali = firstItemDateJalali.ifBlank { today },
                amountToman = firstItemAmountToman.coerceAtLeast(obligation.principalAmountToman),
                receiptCode = receiptCode
            )
            repository.createObligationWithSchedule(
                obligation = obligation.copy(
                    scheduleMode = ScheduleMode.RUNNING_LEDGER.name,
                    createdAtJalali = today
                ),
                installments = listOf(initialInstallment),
                initialSubRecords = listOf(initialSub)
            )
            showToast("حساب دفتری «${obligation.title}» ایجاد شد.")
        }
    }

    fun addSubRecordToObligation(obligation: ObligationEntity, subRecord: LedgerSubRecordEntity) {
        viewModelScope.launch {
            repository.addSubRecordAndSyncLedger(obligation, subRecord)
            showToast("ریزتراکنش جدید در پرونده «${obligation.title}» ثبت و تراز بازمحاسبه شد.")
        }
    }

    fun deleteObligation(obligationId: Long, title: String) {
        viewModelScope.launch {
            repository.deleteObligation(obligationId)
            showToast("تعهد «$title» حذف شد.")
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
            showToast("فایل PDF استاندارد A4 (${file.name}) تولید و آماده اشتراک‌گذاری/چاپ شد.")
        }
    }

    fun exportRtlExcel(context: Context, payload: A4ReportPayload) {
        viewModelScope.launch {
            val file = ReportExportEngine.saveAndShareRtlExcelFile(context, payload)
            showToast("فایل اکسل راست‌به‌چپ (${file.name}) با موفقیت تولید شد.")
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
            showToast("تمامی داده‌های نمونه پاکسازی شد. اکنون می‌توانید اطلاعات واقعی خود را ثبت کنید.")
        }
    }

    fun restoreDemoSeedData() {
        viewModelScope.launch {
            repository.reloadDemoSeedData()
            showToast("داده‌های نمونه واقعی اکوسیستم مالی ایران بازگردانی شد.")
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
                showToast("خطا در ساختار فایل JSON واردشده. لطفاً صحت فایل پشتیبان را بررسی کنید.")
            }
        }
    }

    fun reRunQaDiagnostics() {
        _qaResults.value = QaDiagnosticsEngine.runAll10DiagnosticSuites()
        showToast("هر ۱۰ سوئیت تست خودکار QA با موفقیت اجرا و بررسی شدند.")
    }
}
