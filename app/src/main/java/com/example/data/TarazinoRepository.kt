package com.example.data

import android.content.Context
import com.example.core.CurrencyUnit
import com.example.core.JalaliCalendarEngine
import com.example.widget.TarazinoQuickLogWidgetProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class TarazinoThemeMode(val labelFa: String) {
    OBSIDIAN_EMERALD_DARK("زمرد شب"),
    HIGH_CONTRAST_NIGHT_DARK("تاریک پُرکنتراست شبانه"),
    ALABASTER_LIGHT("مروارید صبح (فیروزه تیره)"),
    PERSIAN_TURQUOISE_LIGHT("فیروزه بهار"),
    DEEP_SAPPHIRE("اقیانوس کبود"),
    MIDNIGHT_GOLD("شبِ طلایی"),
    VELVET_AMETHYST("ارغوان مخملی")
}

enum class FontScalePreset(val labelFa: String, val scaleFactor: Float) {
    SMALL("کوچک", 0.90f),
    STANDARD("استاندارد", 1.0f),
    LARGE("بزرگ و خوانا", 1.12f)
}

data class UserPreferencesState(
    val themeMode: TarazinoThemeMode = TarazinoThemeMode.OBSIDIAN_EMERALD_DARK,
    val currencyUnit: CurrencyUnit = CurrencyUnit.TOMAN,
    val fontScale: FontScalePreset = FontScalePreset.STANDARD,
    val monthlyIncomeToman: Long = 58_000_000L,
    val livingExpensePercent: Int = 50,
    val alertAdvanceDays: Int = 7,
    val monthlySavingsGoalTitle: String = "پس‌انداز و ذخیره طلایی ماه",
    val monthlySavingsTargetToman: Long = 8_000_000L,
    val monthlySavingsSavedToman: Long = 5_200_000L,
    val privacyMaskEnabled: Boolean = false,
    val pinHash: String = "",
    val pinLength: Int = 4,
    val biometricEnabled: Boolean = false,
    val autoLockMinutes: Int = 0, // 0 = immediate on lock button, 1 = 1 min, 5 = 5 min
    val recoveryQuestion: String = "نام اولین مدرسه یا شهر محل تولد شما چیست؟",
    val recoveryAnswerHash: String = "",
    val hasCompletedGuidedTour: Boolean = false,
    val isInitialSeedDone: Boolean = false,
    val autoPaymentScheduleEnabled: Boolean = false,
    val preferredAutoPayDayOfMonth: Int = 0,
    val preferredSecondaryPayDayOfMonth: Int = 0,
    val dashboardLayoutConfig: com.example.domain.DashboardLayoutConfig =
        com.example.domain.DashboardLayoutGridEngine.defaultConfig()
)

class TarazinoRepository(
    context: Context,
    private val dao: TarazinoDao
) {
    private val appContext = context.applicationContext
    private val prefs = context.getSharedPreferences("tarazino_prefs_v1", Context.MODE_PRIVATE)

    private val _preferencesState = MutableStateFlow(loadPreferences())
    val preferencesState: StateFlow<UserPreferencesState> = _preferencesState.asStateFlow()

    val counterpartiesFlow: Flow<List<CounterpartyEntity>> = dao.observeAllCounterparties()
    val obligationsFlow: Flow<List<ObligationEntity>> = dao.observeAllObligations()
    val installmentsFlow: Flow<List<InstallmentEntity>> = dao.observeAllInstallments()
    val subRecordsFlow: Flow<List<LedgerSubRecordEntity>> = dao.observeAllSubRecords()

    private fun loadPreferences(): UserPreferencesState {
        val themeName = prefs.getString("theme_mode", TarazinoThemeMode.OBSIDIAN_EMERALD_DARK.name)
            ?: TarazinoThemeMode.OBSIDIAN_EMERALD_DARK.name
        val unitName = prefs.getString("currency_unit", CurrencyUnit.TOMAN.name)
            ?: CurrencyUnit.TOMAN.name
        val fontName = prefs.getString("font_scale", FontScalePreset.STANDARD.name)
            ?: FontScalePreset.STANDARD.name

        return UserPreferencesState(
            themeMode = runCatching { TarazinoThemeMode.valueOf(themeName) }
                .getOrDefault(TarazinoThemeMode.OBSIDIAN_EMERALD_DARK),
            currencyUnit = runCatching { CurrencyUnit.valueOf(unitName) }
                .getOrDefault(CurrencyUnit.TOMAN),
            fontScale = runCatching { FontScalePreset.valueOf(fontName) }
                .getOrDefault(FontScalePreset.STANDARD),
            monthlyIncomeToman = prefs.getLong("monthly_income_toman", 58_000_000L),
            livingExpensePercent = prefs.getInt("living_expense_percent", 50),
            alertAdvanceDays = prefs.getInt("alert_advance_days", 7),
            monthlySavingsGoalTitle = prefs.getString(
                "monthly_savings_goal_title",
                "پس‌انداز و ذخیره طلایی ماه"
            ) ?: "پس‌انداز و ذخیره طلایی ماه",
            monthlySavingsTargetToman = prefs.getLong("monthly_savings_target_toman", 8_000_000L),
            monthlySavingsSavedToman = prefs.getLong("monthly_savings_saved_toman", 5_200_000L),
            privacyMaskEnabled = prefs.getBoolean("privacy_mask_enabled", false),
            pinHash = prefs.getString("pin_hash", "") ?: "",
            pinLength = prefs.getInt("pin_length", 4),
            biometricEnabled = prefs.getBoolean("biometric_enabled", false),
            autoLockMinutes = prefs.getInt("auto_lock_minutes", 1),
            recoveryQuestion = prefs.getString(
                "recovery_question",
                "نام اولین مدرسه یا شهر محل تولد شما چیست؟"
            ) ?: "نام اولین مدرسه یا شهر محل تولد شما چیست؟",
            recoveryAnswerHash = prefs.getString("recovery_answer_hash", "") ?: "",
            hasCompletedGuidedTour = prefs.getBoolean("has_completed_guided_tour", false),
            isInitialSeedDone = prefs.getBoolean("is_initial_seed_done", false),
            autoPaymentScheduleEnabled = prefs.getBoolean("auto_payment_schedule_enabled", false),
            preferredAutoPayDayOfMonth = prefs.getInt("preferred_auto_pay_day_of_month", 0),
            preferredSecondaryPayDayOfMonth = prefs.getInt("preferred_secondary_pay_day_of_month", 0),
            dashboardLayoutConfig = com.example.domain.DashboardLayoutGridEngine.deserializeConfig(
                prefs.getString("dashboard_layout_grid_v1", null)
            )
        )
    }

    fun updatePreferences(transform: (UserPreferencesState) -> UserPreferencesState) {
        val updated = transform(_preferencesState.value)
        prefs.edit()
            .putString("theme_mode", updated.themeMode.name)
            .putString("currency_unit", updated.currencyUnit.name)
            .putString("font_scale", updated.fontScale.name)
            .putLong("monthly_income_toman", updated.monthlyIncomeToman)
            .putInt("living_expense_percent", updated.livingExpensePercent)
            .putInt("alert_advance_days", updated.alertAdvanceDays)
            .putString("monthly_savings_goal_title", updated.monthlySavingsGoalTitle)
            .putLong("monthly_savings_target_toman", updated.monthlySavingsTargetToman)
            .putLong("monthly_savings_saved_toman", updated.monthlySavingsSavedToman)
            .putBoolean("privacy_mask_enabled", updated.privacyMaskEnabled)
            .putString("pin_hash", updated.pinHash)
            .putInt("pin_length", updated.pinLength)
            .putBoolean("biometric_enabled", updated.biometricEnabled)
            .putInt("auto_lock_minutes", updated.autoLockMinutes)
            .putString("recovery_question", updated.recoveryQuestion)
            .putString("recovery_answer_hash", updated.recoveryAnswerHash)
            .putBoolean("has_completed_guided_tour", updated.hasCompletedGuidedTour)
            .putBoolean("is_initial_seed_done", updated.isInitialSeedDone)
            .putBoolean("auto_payment_schedule_enabled", updated.autoPaymentScheduleEnabled)
            .putInt("preferred_auto_pay_day_of_month", updated.preferredAutoPayDayOfMonth)
            .putInt("preferred_secondary_pay_day_of_month", updated.preferredSecondaryPayDayOfMonth)
            .putString(
                "dashboard_layout_grid_v1",
                com.example.domain.DashboardLayoutGridEngine.serializeConfig(updated.dashboardLayoutConfig)
            )
            .apply()
        _preferencesState.value = updated
    }

    fun loadRecurringCashflowItems(): List<com.example.domain.RecurringCashflowItem> {
        val raw = prefs.getString("recurring_non_loan_cashflow_items_v1", null)
        return com.example.domain.RecurringCashflowHealthEngine.deserializeItems(raw)
    }

    fun loadCachedDailyAiTip(): com.example.domain.DailyFinancialTipResult? {
        val raw = prefs.getString("daily_ai_financial_tip_cache_v1", null)
        return com.example.domain.DailyAiFinancialTipEngine.deserializeCachedTip(raw)
    }

    fun saveCachedDailyAiTip(tip: com.example.domain.DailyFinancialTipResult) {
        val serialized = com.example.domain.DailyAiFinancialTipEngine.serializeCachedTip(tip)
        prefs.edit().putString("daily_ai_financial_tip_cache_v1", serialized).apply()
    }

    suspend fun ensureSeedDataLoadedIfFirstLaunch() {
        val seedV2Done = prefs.getBoolean("is_initial_seed_v2_done", false)
        if (!seedV2Done) {
            SeedDataEngine.populateRealisticIranianSeedData(dao)
            prefs.edit()
                .putBoolean("is_initial_seed_v2_done", true)
                .putBoolean("is_palette_v4_teal_gold_migrated", true)
                .apply()
            updatePreferences { it.copy(isInitialSeedDone = true) }
        } else if (!prefs.getBoolean("is_palette_v4_teal_gold_migrated", false)) {
            val existing = dao.getAllCounterpartiesOnce()
            existing.forEach { cp ->
                val upgradedHex = when (cp.avatarColorHex) {
                    0xFF3B82F6L, 0xFF38BDF8L, 0xFF0EA5E9L -> 0xFF4DA3B8L
                    0xFF2563EBL, 0xFF60A5FAL -> 0xFF5E96AEL
                    0xFF10B981L, 0xFF10D9A0L -> 0xFF35B8ABL
                    0xFF06B6D4L, 0xFF2DD4BFL -> 0xFF47B0C4L
                    0xFF8B5CF6L, 0xFF818CF8L -> 0xFF689EB8L
                    0xFFF59E0BL, 0xFFFBBF24L -> 0xFFDAB978L
                    0xFFEC4899L, 0xFFF472B6L -> 0xFFC99B68L
                    else -> cp.categoryEnum.defaultColorHex
                }
                if (upgradedHex != cp.avatarColorHex) {
                    dao.updateCounterparty(cp.copy(avatarColorHex = upgradedHex))
                }
            }
            prefs.edit().putBoolean("is_palette_v4_teal_gold_migrated", true).apply()
        }
    }

    suspend fun reloadDemoSeedData() {
        SeedDataEngine.populateRealisticIranianSeedData(dao)
        TarazinoQuickLogWidgetProvider.requestRefreshAll(appContext)
    }

    suspend fun clearAllDataToZero() {
        dao.clearAllInstallments()
        dao.clearAllSubRecords()
        dao.clearAllObligations()
        dao.clearAllCounterparties()
        TarazinoQuickLogWidgetProvider.requestRefreshAll(appContext)
    }

    // Counterparty CRUD
    suspend fun saveCounterparty(entity: CounterpartyEntity): Long {
        return if (entity.id == 0L) {
            dao.insertCounterparty(entity)
        } else {
            dao.updateCounterparty(entity)
            entity.id
        }
    }

    suspend fun deleteCounterpartyAndLinkedData(counterpartyId: Long) {
        val linkedObligations = dao.getAllObligationsOnce().filter { it.counterpartyId == counterpartyId }
        for (ob in linkedObligations) {
            dao.deleteInstallmentsByObligation(ob.id)
            dao.deleteSubRecordsByObligation(ob.id)
        }
        dao.deleteObligationsByCounterparty(counterpartyId)
        dao.deleteCounterpartyById(counterpartyId)
        TarazinoQuickLogWidgetProvider.requestRefreshAll(appContext)
    }

    // Obligation & Installments CRUD
    suspend fun createObligationWithSchedule(
        obligation: ObligationEntity,
        installments: List<InstallmentEntity>,
        initialSubRecords: List<LedgerSubRecordEntity> = emptyList()
    ): Long {
        val obligationId = dao.insertObligation(obligation)
        val linkedInstallments = installments.mapIndexed { idx, item ->
            item.copy(
                obligationId = obligationId,
                installmentNumber = idx + 1
            )
        }
        dao.insertInstallments(linkedInstallments)
        if (initialSubRecords.isNotEmpty()) {
            dao.insertSubRecords(initialSubRecords.map { it.copy(obligationId = obligationId) })
        }
        TarazinoQuickLogWidgetProvider.requestRefreshAll(appContext)
        return obligationId
    }

    suspend fun updateObligation(obligation: ObligationEntity) {
        dao.updateObligation(obligation)
        TarazinoQuickLogWidgetProvider.requestRefreshAll(appContext)
    }

    suspend fun deleteObligation(obligationId: Long) {
        dao.deleteInstallmentsByObligation(obligationId)
        dao.deleteSubRecordsByObligation(obligationId)
        dao.deleteObligationById(obligationId)
        TarazinoQuickLogWidgetProvider.requestRefreshAll(appContext)
    }

    suspend fun updateInstallment(installment: InstallmentEntity) {
        dao.updateInstallment(installment)
        TarazinoQuickLogWidgetProvider.requestRefreshAll(appContext)
    }

    suspend fun addManualInstallment(installment: InstallmentEntity) {
        dao.insertInstallment(installment)
        TarazinoQuickLogWidgetProvider.requestRefreshAll(appContext)
    }

    suspend fun deleteInstallmentById(installmentId: Long) {
        dao.deleteInstallmentById(installmentId)
        TarazinoQuickLogWidgetProvider.requestRefreshAll(appContext)
    }

    suspend fun addSubRecordAndSyncLedger(
        obligation: ObligationEntity,
        subRecord: LedgerSubRecordEntity
    ) {
        dao.insertSubRecord(subRecord)
        // Recalculate obligation principal or paid amounts if in RUNNING_LEDGER mode
        if (obligation.scheduleModeEnum == ScheduleMode.RUNNING_LEDGER) {
            val allSubs = dao.getAllSubRecordsOnce().filter { it.obligationId == obligation.id }
            val totalCharges = allSubs
                .filter { it.recordTypeEnum == SubRecordType.CHARGE }
                .sumOf { it.amountToman }
            val totalPayments = allSubs
                .filter { it.recordTypeEnum == SubRecordType.PAYMENT }
                .sumOf { it.amountToman }

            if (totalCharges > 0L) {
                dao.updateObligation(obligation.copy(principalAmountToman = totalCharges))
            }
            // Add or update ledger installment record to reflect net balance
            val existingInst = dao.getInstallmentsForObligation(obligation.id)
            if (subRecord.recordTypeEnum == SubRecordType.CHARGE) {
                val nextNum = (existingInst.maxOfOrNull { it.installmentNumber } ?: 0) + 1
                dao.insertInstallment(
                    InstallmentEntity(
                        obligationId = obligation.id,
                        installmentNumber = nextNum,
                        title = subRecord.title,
                        dueDateJalali = subRecord.dateJalali,
                        amountToman = subRecord.amountToman,
                        paidAmountToman = 0L,
                        status = InstallmentStatus.PENDING.name,
                        trackingCode = subRecord.receiptCode,
                        note = subRecord.note
                    )
                )
            } else {
                // Apply payment across unpaid installments oldest-first
                var paymentLeft = subRecord.amountToman
                for (inst in existingInst.sortedBy { it.dueDateJalali }) {
                    if (paymentLeft <= 0L) break
                    val rem = inst.remainingToman
                    if (rem > 0L) {
                        val applied = minOf(rem, paymentLeft)
                        val newPaid = inst.paidAmountToman + applied
                        val newStatus = if (newPaid >= inst.effectiveTotalToman) {
                            InstallmentStatus.PAID.name
                        } else {
                            InstallmentStatus.PARTIAL.name
                        }
                        dao.updateInstallment(
                            inst.copy(
                                paidAmountToman = newPaid,
                                status = newStatus,
                                paidDateJalali = subRecord.dateJalali,
                                trackingCode = subRecord.receiptCode.ifBlank { inst.trackingCode }
                            )
                        )
                        paymentLeft -= applied
                    }
                }
                // Keep totalPayments referenced for audit
                if (totalPayments >= totalCharges && totalCharges > 0L) {
                    dao.getInstallmentsForObligation(obligation.id).forEach { inst ->
                        if (inst.statusEnum != InstallmentStatus.PAID) {
                            dao.updateInstallment(
                                inst.copy(
                                    paidAmountToman = inst.effectiveTotalToman,
                                    status = InstallmentStatus.PAID.name,
                                    paidDateJalali = subRecord.dateJalali
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Serializes the entire offline database + preferences into a structured JSON backup string.
     */
    suspend fun exportFullBackupJson(): String {
        val counterparties = dao.getAllCounterpartiesOnce()
        val obligations = dao.getAllObligationsOnce()
        val installments = dao.getAllInstallmentsOnce()
        val subRecords = dao.getAllSubRecordsOnce()
        val root = JSONObject()
        root.put("app", "Tarazino")
        root.put("version", "1.0.0")
        root.put("exportedAtJalali", JalaliCalendarEngine.todayJalali())
        root.put("monthlyIncomeToman", _preferencesState.value.monthlyIncomeToman)

        val cpArr = JSONArray()
        counterparties.forEach { cp ->
            cpArr.put(
                JSONObject().apply {
                    put("id", cp.id)
                    put("name", cp.name)
                    put("category", cp.category)
                    put("avatarColorHex", cp.avatarColorHex)
                    put("cardNumber", cp.cardNumber)
                    put("shebaNumber", cp.shebaNumber)
                    put("phoneNumber", cp.phoneNumber)
                    put("trustPriority", cp.trustPriority)
                    put("description", cp.description)
                    put("createdAtJalali", cp.createdAtJalali)
                }
            )
        }
        root.put("counterparties", cpArr)

        val obArr = JSONArray()
        obligations.forEach { ob ->
            obArr.put(
                JSONObject().apply {
                    put("id", ob.id)
                    put("counterpartyId", ob.counterpartyId)
                    put("title", ob.title)
                    put("category", ob.category)
                    put("scheduleMode", ob.scheduleMode)
                    put("principalAmountToman", ob.principalAmountToman)
                    put("profitOrFeeToman", ob.profitOrFeeToman)
                    put("downPaymentToman", ob.downPaymentToman)
                    put("installmentCount", ob.installmentCount)
                    put("paymentInterval", ob.paymentInterval)
                    put("firstDueDateJalali", ob.firstDueDateJalali)
                    put("trustPriority", ob.trustPriority)
                    put("trackingOrCheckCode", ob.trackingOrCheckCode)
                    put("notes", ob.notes)
                    put("createdAtJalali", ob.createdAtJalali)
                }
            )
        }
        root.put("obligations", obArr)

        val instArr = JSONArray()
        installments.forEach { inst ->
            instArr.put(
                JSONObject().apply {
                    put("id", inst.id)
                    put("obligationId", inst.obligationId)
                    put("installmentNumber", inst.installmentNumber)
                    put("title", inst.title)
                    put("dueDateJalali", inst.dueDateJalali)
                    put("amountToman", inst.amountToman)
                    put("paidAmountToman", inst.paidAmountToman)
                    put("lateFeeToman", inst.lateFeeToman)
                    put("discountToman", inst.discountToman)
                    put("status", inst.status)
                    put("paidDateJalali", inst.paidDateJalali)
                    put("trackingCode", inst.trackingCode)
                    put("snoozedUntilJalali", inst.snoozedUntilJalali)
                    put("note", inst.note)
                }
            )
        }
        root.put("installments", instArr)

        val subArr = JSONArray()
        subRecords.forEach { sr ->
            subArr.put(
                JSONObject().apply {
                    put("id", sr.id)
                    put("obligationId", sr.obligationId)
                    put("title", sr.title)
                    put("dateJalali", sr.dateJalali)
                    put("amountToman", sr.amountToman)
                    put("recordType", sr.recordType)
                    put("receiptCode", sr.receiptCode)
                    put("note", sr.note)
                }
            )
        }
        root.put("subRecords", subArr)
        return root.toString(2)
    }

    /**
     * Restores the entire offline database from a JSON backup string.
     */
    suspend fun importFullBackupJson(jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            val cpArr = root.optJSONArray("counterparties") ?: return false
            val obArr = root.optJSONArray("obligations") ?: JSONArray()
            val instArr = root.optJSONArray("installments") ?: JSONArray()
            val subArr = root.optJSONArray("subRecords") ?: JSONArray()

            clearAllDataToZero()

            for (i in 0 until cpArr.length()) {
                val obj = cpArr.getJSONObject(i)
                dao.insertCounterparty(
                    CounterpartyEntity(
                        id = obj.optLong("id", 0L),
                        name = obj.optString("name", "طرف‌حساب"),
                        category = obj.optString("category", EntityCategory.BANK.name),
                        avatarColorHex = obj.optLong("avatarColorHex", 0xFF10B981),
                        cardNumber = obj.optString("cardNumber", ""),
                        shebaNumber = obj.optString("shebaNumber", ""),
                        phoneNumber = obj.optString("phoneNumber", ""),
                        trustPriority = obj.optString("trustPriority", TrustPriority.NORMAL.name),
                        description = obj.optString("description", ""),
                        createdAtJalali = obj.optString("createdAtJalali", "1405/08/01")
                    )
                )
            }

            for (i in 0 until obArr.length()) {
                val obj = obArr.getJSONObject(i)
                dao.insertObligation(
                    ObligationEntity(
                        id = obj.optLong("id", 0L),
                        counterpartyId = obj.optLong("counterpartyId", 1L),
                        title = obj.optString("title", "تعهد مالی"),
                        category = obj.optString("category", ObligationCategory.INSTALLMENT_LOAN.name),
                        scheduleMode = obj.optString("scheduleMode", ScheduleMode.AUTO_SCHEDULE.name),
                        principalAmountToman = obj.optLong("principalAmountToman", 0L),
                        profitOrFeeToman = obj.optLong("profitOrFeeToman", 0L),
                        downPaymentToman = obj.optLong("downPaymentToman", 0L),
                        installmentCount = obj.optInt("installmentCount", 1),
                        paymentInterval = obj.optString("paymentInterval", PaymentInterval.MONTHLY.name),
                        firstDueDateJalali = obj.optString("firstDueDateJalali", "1405/08/01"),
                        trustPriority = obj.optString("trustPriority", TrustPriority.NORMAL.name),
                        trackingOrCheckCode = obj.optString("trackingOrCheckCode", ""),
                        notes = obj.optString("notes", ""),
                        createdAtJalali = obj.optString("createdAtJalali", "1405/08/01")
                    )
                )
            }

            for (i in 0 until instArr.length()) {
                val obj = instArr.getJSONObject(i)
                dao.insertInstallment(
                    InstallmentEntity(
                        id = obj.optLong("id", 0L),
                        obligationId = obj.optLong("obligationId", 1L),
                        installmentNumber = obj.optInt("installmentNumber", i + 1),
                        title = obj.optString("title", "قسط"),
                        dueDateJalali = obj.optString("dueDateJalali", "1405/08/01"),
                        amountToman = obj.optLong("amountToman", 0L),
                        paidAmountToman = obj.optLong("paidAmountToman", 0L),
                        lateFeeToman = obj.optLong("lateFeeToman", 0L),
                        discountToman = obj.optLong("discountToman", 0L),
                        status = obj.optString("status", InstallmentStatus.PENDING.name),
                        paidDateJalali = obj.optString("paidDateJalali", ""),
                        trackingCode = obj.optString("trackingCode", ""),
                        snoozedUntilJalali = obj.optString("snoozedUntilJalali", ""),
                        note = obj.optString("note", "")
                    )
                )
            }

            for (i in 0 until subArr.length()) {
                val obj = subArr.getJSONObject(i)
                dao.insertSubRecord(
                    LedgerSubRecordEntity(
                        id = obj.optLong("id", 0L),
                        obligationId = obj.optLong("obligationId", 1L),
                        title = obj.optString("title", "تراکنش"),
                        dateJalali = obj.optString("dateJalali", "1405/08/01"),
                        amountToman = obj.optLong("amountToman", 0L),
                        recordType = obj.optString("recordType", SubRecordType.CHARGE.name),
                        receiptCode = obj.optString("receiptCode", ""),
                        note = obj.optString("note", "")
                    )
                )
            }

            val income = root.optLong("monthlyIncomeToman", -1L)
            if (income > 0L) {
                updatePreferences { it.copy(monthlyIncomeToman = income) }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
