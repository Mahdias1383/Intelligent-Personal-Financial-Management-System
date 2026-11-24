package com.example.widget

import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.ObligationCategory
import com.example.data.ObligationEntity
import com.example.data.PaymentInterval
import com.example.data.ScheduleMode
import com.example.data.TrustPriority
import com.example.domain.InstallmentScheduleEngine

/**
 * Supported quick-log workflows available directly from the Home Screen Widget.
 */
enum class WidgetQuickLogMode(val labelFa: String) {
    EXPENSE("ثبت هزینه سریع"),
    DEBT_PAYMENT("پرداخت بدهی / قسط")
}

/**
 * Snapshot rendered inside the Home Screen Widget (`RemoteViews`).
 */
data class QuickLogWidgetSnapshot(
    val todayJalali: String,
    val totalRemainingDebtToman: Long,
    val activeUnpaidDebtCount: Int,
    val urgentCount: Int,
    val nextDueInstallmentId: Long?,
    val nextDueObligationTitleFa: String,
    val nextDueInstallmentTitleFa: String,
    val nextDueRemainingToman: Long,
    val nextDueDateJalali: String,
    val nextDueRelativeLabelFa: String,
    val summaryHeaderFa: String
)

/**
 * Validation result for quick-log input submitted from the floating widget sheet.
 */
sealed class QuickLogValidationResult {
    data object Valid : QuickLogValidationResult()
    data class Invalid(val errorMessageFa: String) : QuickLogValidationResult()
}

/**
 * Pair of newly created obligation + single installment records for a quick expense or new debt.
 */
data class QuickExpenseBundle(
    val obligation: ObligationEntity,
    val installment: InstallmentEntity
)

/**
 * Pure domain engine powering the **Tarazino Home Screen Quick-Log Widget** and its lightweight
 * floating quick-entry overlay. Handles snapshot derivation, 1-tap installment settlement math,
 * partial debt payment updates, and instant expense/debt creation.
 */
object QuickLogWidgetEngine {

    /**
     * Computes the live home-screen widget summary and selects the single highest-priority
     * upcoming/overdue debt installment for 1-tap instant settlement directly on the home screen.
     */
    fun buildSnapshot(
        obligations: List<ObligationEntity>,
        installments: List<InstallmentEntity>,
        todayJalali: String = JalaliCalendarEngine.todayJalali()
    ): QuickLogWidgetSnapshot {
        val debtObsById = obligations
            .filter { !it.categoryEnum.isReceivable }
            .associateBy { it.id }

        val unpaidDebtInstallments = installments
            .filter { inst ->
                inst.obligationId in debtObsById &&
                    inst.statusEnum != InstallmentStatus.PAID &&
                    inst.remainingToman > 0L
            }
            .sortedWith(
                compareByDescending<InstallmentEntity> {
                    it.statusEnum == InstallmentStatus.OVERDUE ||
                        JalaliCalendarEngine.diffJalaliDays(todayJalali, it.dueDateJalali) < 0
                }
                    .thenBy { it.dueDateJalali }
                    .thenBy { it.installmentNumber }
            )

        val totalRemaining = unpaidDebtInstallments.sumOf { it.remainingToman }
        val activeDebtIds = unpaidDebtInstallments.map { it.obligationId }.toSet()
        val urgentCount = unpaidDebtInstallments.count { inst ->
            val diff = JalaliCalendarEngine.diffJalaliDays(todayJalali, inst.dueDateJalali)
            inst.statusEnum == InstallmentStatus.OVERDUE || diff <= 7
        }

        val nextInst = unpaidDebtInstallments.firstOrNull()
        val nextOb = nextInst?.let { debtObsById[it.obligationId] }

        val summaryText = if (totalRemaining > 0L) {
            "مانده بدهی: ${PersianCurrencyEngine.formatAmount(totalRemaining)}"
        } else {
            "بدون بدهی فعال — آماده ثبت هزینه"
        }

        return QuickLogWidgetSnapshot(
            todayJalali = todayJalali,
            totalRemainingDebtToman = totalRemaining,
            activeUnpaidDebtCount = activeDebtIds.size,
            urgentCount = urgentCount,
            nextDueInstallmentId = nextInst?.id,
            nextDueObligationTitleFa = nextOb?.title ?: "همه اقساط تسویه شده‌اند!",
            nextDueInstallmentTitleFa = nextInst?.title ?: "",
            nextDueRemainingToman = nextInst?.remainingToman ?: 0L,
            nextDueDateJalali = nextInst?.dueDateJalali ?: todayJalali,
            nextDueRelativeLabelFa = if (nextInst != null) {
                JalaliCalendarEngine.formatRelativeDueStatusFa(todayJalali, nextInst.dueDateJalali)
            } else {
                "آزادی مالی"
            },
            summaryHeaderFa = summaryText
        )
    }

    /**
     * Validates user inputs before committing a quick expense or debt payment from the widget.
     */
    fun validateInput(
        mode: WidgetQuickLogMode,
        title: String,
        amountToman: Long,
        selectedInstallmentId: Long?
    ): QuickLogValidationResult {
        if (amountToman <= 0L) {
            return QuickLogValidationResult.Invalid("لطفاً مبلغ معتبر و بزرگ‌تر از صفر وارد کنید.")
        }
        return when (mode) {
            WidgetQuickLogMode.EXPENSE -> {
                if (title.trim().isEmpty()) {
                    QuickLogValidationResult.Invalid("لطفاً عنوان هزینه را وارد یا انتخاب کنید.")
                } else {
                    QuickLogValidationResult.Valid
                }
            }
            WidgetQuickLogMode.DEBT_PAYMENT -> {
                if (selectedInstallmentId == null || selectedInstallmentId <= 0L) {
                    QuickLogValidationResult.Invalid("لطفاً یک قسط بدهی را برای ثبت پرداخت انتخاب کنید.")
                } else {
                    QuickLogValidationResult.Valid
                }
            }
        }
    }

    /**
     * Creates an [ObligationEntity] + [InstallmentEntity] pair for a quick expense logged from
     * the home screen widget.
     *
     * - When [markAsPaidImmediately] is `true` (default for cash/card expenses), the installment
     *   is stored as `PAID` immediately so it reflects in spending analytics without inflating unpaid debt.
     * - When `false`, it is stored as a `PENDING` unpaid debt/expense due on [dateJalali].
     */
    fun createQuickExpenseBundle(
        counterpartyId: Long,
        title: String,
        amountToman: Long,
        category: ObligationCategory = ObligationCategory.PERSONAL_DEBT,
        markAsPaidImmediately: Boolean = true,
        note: String = "",
        dateJalali: String = JalaliCalendarEngine.todayJalali(),
        trackingSuffix: String = "${System.currentTimeMillis() % 10000}"
    ): QuickExpenseBundle {
        val cleanTitle = title.trim().ifBlank { "هزینه سریع" }
        val safeAmount = amountToman.coerceAtLeast(1L)

        val obligation = ObligationEntity(
            id = 0L,
            counterpartyId = counterpartyId,
            title = cleanTitle,
            category = category.name,
            scheduleMode = ScheduleMode.AUTO_SCHEDULE.name,
            principalAmountToman = safeAmount,
            profitOrFeeToman = 0L,
            downPaymentToman = 0L,
            installmentCount = 1,
            paymentInterval = PaymentInterval.MONTHLY.name,
            firstDueDateJalali = dateJalali,
            trustPriority = TrustPriority.NORMAL.name,
            trackingOrCheckCode = "WGT-$trackingSuffix",
            notes = note.trim().ifBlank { "ثبت سریع از ویجت صفحه اصلی" },
            createdAtJalali = dateJalali
        )

        val installment = InstallmentEntity(
            id = 0L,
            obligationId = 0L,
            installmentNumber = 1,
            title = cleanTitle,
            dueDateJalali = dateJalali,
            amountToman = safeAmount,
            paidAmountToman = if (markAsPaidImmediately) safeAmount else 0L,
            status = if (markAsPaidImmediately) InstallmentStatus.PAID.name else InstallmentStatus.PENDING.name,
            paidDateJalali = if (markAsPaidImmediately) dateJalali else "",
            trackingCode = if (markAsPaidImmediately) "WGT-EXP-$trackingSuffix" else "",
            note = note.trim().ifBlank { "ثبت‌شده با ویجت سریع ترازینو" }
        )

        return QuickExpenseBundle(obligation = obligation, installment = installment)
    }

    /**
     * Applies either a full or partial payment to an existing unpaid debt [installment].
     */
    fun applyQuickDebtPayment(
        installment: InstallmentEntity,
        paymentAmountToman: Long,
        todayJalali: String = JalaliCalendarEngine.todayJalali(),
        trackingCode: String = "WGT-PAY-${System.currentTimeMillis() % 10000}"
    ): InstallmentEntity {
        val safePayment = paymentAmountToman.coerceAtLeast(0L)
        val newTotalPaid = (installment.paidAmountToman + safePayment)
            .coerceAtMost(installment.effectiveTotalToman)

        return InstallmentScheduleEngine.applyPaymentUpdate(
            installment = installment,
            paidAmountToman = newTotalPaid,
            paidDateJalali = todayJalali,
            trackingCode = trackingCode.ifBlank { installment.trackingCode },
            todayJalali = todayJalali
        )
    }
}
