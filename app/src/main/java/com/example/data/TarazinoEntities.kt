package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EntityCategory(
    val labelFa: String,
    val badgeFa: String,
    val defaultColorHex: Long
) {
    BANK("بانک‌های عامل", "بانک", 0xFF10D9A0),
    BNPL_PLATFORM("پلتفرم خرید اقساطی (BNPL)", "اقساطی / BNPL", 0xFF38BDF8),
    INSTITUTION("صندوق قرض‌الحسنه / موسسه", "صندوق و موسسه", 0xFF818CF8),
    PERSON("اشخاص حقیقی (دوست، خانواده، صاحب‌خانه)", "شخص حقیقی", 0xFFFBBF24)
}

enum class TrustPriority(
    val labelFa: String,
    val weight: Int,
    val badgeColorHex: Long
) {
    CRITICAL("حیاتی / قول شرف و چک صیادی", 3, 0xFFF43F5E),
    HIGH("اولویت بالا / خوش‌حسابی بانکی", 2, 0xFFFBBF24),
    NORMAL("عادی / برنامه‌ریزی‌شده", 1, 0xFF10D9A0)
}

enum class ObligationCategory(
    val labelFa: String,
    val shortLabelFa: String,
    val isReceivable: Boolean,
    val colorHex: Long
) {
    INSTALLMENT_LOAN("وام و تسهیلات اقساطی بانکی/صندوقی", "وام اقساطی", false, 0xFF10D9A0),
    BNPL_PURCHASE("خرید اقساطی کالا و خدمات (BNPL)", "خرید اقساطی", false, 0xFF38BDF8),
    PERSONAL_DEBT("بدهی و قرض دریافتی از اشخاص/موسسات", "قرض و بدهی", false, 0xFFFBBF24),
    PERSONAL_RECEIVABLE("طلب و قرض داده‌شده به دیگران", "طلب از دیگران", true, 0xFF2DD4BF),
    CHECK_OR_BILL("چک صیادی، اجاره‌بها یا تعهدات دوره‌ای", "چک صیادی / اجاره", false, 0xFFF472B6)
}

enum class ScheduleMode(val labelFa: String) {
    AUTO_SCHEDULE("محاسبه خودکار جدول اقساط"),
    RUNNING_LEDGER("حساب دفتری پویا و خریدهای موردی")
}

enum class PaymentInterval(
    val labelFa: String,
    val monthsStep: Int,
    val daysStep: Int
) {
    MONTHLY("ماهانه", 1, 0),
    WEEKLY("هفتگی (۷ روز)", 0, 7),
    BIWEEKLY("دوهفته‌یک‌بار (۱۴ روز)", 0, 14),
    BIMONTHLY("دوماه‌یک‌بار", 2, 0),
    QUARTERLY("فصلی (۳ ماه یک‌بار)", 3, 0)
}

enum class InstallmentStatus(val labelFa: String, val colorHex: Long) {
    PAID("پرداخت‌شده", 0xFF10D9A0),
    PARTIAL("پرداخت جزئی", 0xFF38BDF8),
    PENDING("در انتظار پرداخت", 0xFFFBBF24),
    OVERDUE("معوقه (گذشته از سررسید)", 0xFFF43F5E)
}

enum class SubRecordType(val labelFa: String) {
    CHARGE("افزودن خرید / افزایش بدهی یا اصل"),
    PAYMENT("ثبت پرداخت / کسر از مانده")
}

@Entity(tableName = "counterparties")
data class CounterpartyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = EntityCategory.BANK.name,
    val avatarColorHex: Long = 0xFF10B981,
    val cardNumber: String = "",
    val shebaNumber: String = "",
    val phoneNumber: String = "",
    val trustPriority: String = TrustPriority.NORMAL.name,
    val description: String = "",
    val createdAtJalali: String = "1405/08/01"
) {
    val categoryEnum: EntityCategory
        get() = runCatching { EntityCategory.valueOf(category) }.getOrDefault(EntityCategory.BANK)

    val priorityEnum: TrustPriority
        get() = runCatching { TrustPriority.valueOf(trustPriority) }.getOrDefault(TrustPriority.NORMAL)
}

@Entity(tableName = "obligations")
data class ObligationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val counterpartyId: Long,
    val title: String,
    val category: String = ObligationCategory.INSTALLMENT_LOAN.name,
    val scheduleMode: String = ScheduleMode.AUTO_SCHEDULE.name,
    val principalAmountToman: Long,
    val profitOrFeeToman: Long = 0L,
    val downPaymentToman: Long = 0L,
    val installmentCount: Int = 12,
    val paymentInterval: String = PaymentInterval.MONTHLY.name,
    val firstDueDateJalali: String,
    val trustPriority: String = TrustPriority.NORMAL.name,
    val trackingOrCheckCode: String = "",
    val notes: String = "",
    val createdAtJalali: String = "1405/08/01"
) {
    val categoryEnum: ObligationCategory
        get() = runCatching { ObligationCategory.valueOf(category) }.getOrDefault(ObligationCategory.INSTALLMENT_LOAN)

    val scheduleModeEnum: ScheduleMode
        get() = runCatching { ScheduleMode.valueOf(scheduleMode) }.getOrDefault(ScheduleMode.AUTO_SCHEDULE)

    val intervalEnum: PaymentInterval
        get() = runCatching { PaymentInterval.valueOf(paymentInterval) }.getOrDefault(PaymentInterval.MONTHLY)

    val priorityEnum: TrustPriority
        get() = runCatching { TrustPriority.valueOf(trustPriority) }.getOrDefault(TrustPriority.NORMAL)

    val netPayableToman: Long
        get() = (principalAmountToman + profitOrFeeToman - downPaymentToman).coerceAtLeast(0L)
}

@Entity(tableName = "installments")
data class InstallmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val obligationId: Long,
    val installmentNumber: Int,
    val title: String,
    val dueDateJalali: String,
    val amountToman: Long,
    val paidAmountToman: Long = 0L,
    val lateFeeToman: Long = 0L,
    val discountToman: Long = 0L,
    val status: String = InstallmentStatus.PENDING.name,
    val paidDateJalali: String = "",
    val trackingCode: String = "",
    val snoozedUntilJalali: String = "",
    val note: String = ""
) {
    val statusEnum: InstallmentStatus
        get() = runCatching { InstallmentStatus.valueOf(status) }.getOrDefault(InstallmentStatus.PENDING)

    val effectiveTotalToman: Long
        get() = (amountToman + lateFeeToman - discountToman).coerceAtLeast(0L)

    val remainingToman: Long
        get() = (effectiveTotalToman - paidAmountToman).coerceAtLeast(0L)
}

@Entity(tableName = "ledger_sub_records")
data class LedgerSubRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val obligationId: Long,
    val title: String,
    val dateJalali: String,
    val amountToman: Long,
    val recordType: String = SubRecordType.CHARGE.name,
    val receiptCode: String = "",
    val note: String = ""
) {
    val recordTypeEnum: SubRecordType
        get() = runCatching { SubRecordType.valueOf(recordType) }.getOrDefault(SubRecordType.CHARGE)
}
