package com.example.data

import com.example.core.JalaliCalendarEngine
import com.example.domain.InstallmentScheduleEngine

object SeedDataEngine {

    suspend fun populateRealisticIranianSeedData(dao: TarazinoDao) {
        dao.clearAllInstallments()
        dao.clearAllSubRecords()
        dao.clearAllObligations()
        dao.clearAllCounterparties()

        val todayStr = JalaliCalendarEngine.todayJalali()
        val today = JalaliCalendarEngine.parseJalali(todayStr)
        val currentMonthPrefix = "%04d/%02d".format(java.util.Locale.US, today.year, today.month)

        // 1. Hierarchical Counterparties / Sub-Categories
        // Category: BNPL_PLATFORM (خرید اقساطی -> اسنپ‌پی، دیجی‌پی)
        val snappPayId = dao.insertCounterparty(
            CounterpartyEntity(
                name = "اسنپ‌پی",
                category = EntityCategory.BNPL_PLATFORM.name,
                avatarColorHex = 0xFF4DA3B8,
                cardNumber = "6219861045127890",
                shebaNumber = "IR820560081180005544332211",
                phoneNumber = "021-96612000",
                trustPriority = TrustPriority.HIGH.name,
                description = "سرویس اعتباری خرید اقساطی ۴ قسطه بدون کارمزد",
                createdAtJalali = JalaliCalendarEngine.addJalaliMonths(todayStr, -2)
            )
        )

        val digiPayId = dao.insertCounterparty(
            CounterpartyEntity(
                name = "دیجی‌پی",
                category = EntityCategory.BNPL_PLATFORM.name,
                avatarColorHex = 0xFF5E96AE,
                cardNumber = "6221061987654321",
                shebaNumber = "IR540540102580009876543210",
                phoneNumber = "021-61930400",
                trustPriority = TrustPriority.HIGH.name,
                description = "اعتبار خرید اقساطی دیجی‌کالا و فروشگاه‌های زنجیره‌ای",
                createdAtJalali = JalaliCalendarEngine.addJalaliMonths(todayStr, -1)
            )
        )

        // Category: BANK (بانک‌ها -> بانک مهر ایران، بلوبانک)
        val bankMehrId = dao.insertCounterparty(
            CounterpartyEntity(
                name = "بانک قرض‌الحسنه مهر ایران",
                category = EntityCategory.BANK.name,
                avatarColorHex = 0xFF35B8AB,
                cardNumber = "6063731099884412",
                shebaNumber = "IR450600000001009988776655",
                phoneNumber = "021-85201000",
                trustPriority = TrustPriority.HIGH.name,
                description = "شعبه مرکزی ولیعصر — تسهیلات قرض‌الحسنه ۴ درصد",
                createdAtJalali = JalaliCalendarEngine.addJalaliMonths(todayStr, -4)
            )
        )

        val bluBankId = dao.insertCounterparty(
            CounterpartyEntity(
                name = "بلوبانک (بانک سامان)",
                category = EntityCategory.BANK.name,
                avatarColorHex = 0xFF47B0C4,
                cardNumber = "6219861911223344",
                shebaNumber = "IR120560611828001122334455",
                phoneNumber = "021-87641",
                trustPriority = TrustPriority.HIGH.name,
                description = "وام فوری بلوبانک با بازپرداخت ماهانه خودکار",
                createdAtJalali = JalaliCalendarEngine.addJalaliMonths(todayStr, -2)
            )
        )

        // Category: INSTITUTION (صندوق و موسسه -> صندوق امید، شرکت پارس)
        val sandoghOmidId = dao.insertCounterparty(
            CounterpartyEntity(
                name = "صندوق قرض‌الحسنه امید خانواده",
                category = EntityCategory.INSTITUTION.name,
                avatarColorHex = 0xFF689EB8,
                cardNumber = "5892101144556677",
                shebaNumber = "IR630150000000112233445566",
                phoneNumber = "09121112233",
                trustPriority = TrustPriority.HIGH.name,
                description = "صندوق وام خانوادگی با قرعه‌کشی ماهانه",
                createdAtJalali = JalaliCalendarEngine.addJalaliMonths(todayStr, -3)
            )
        )

        val parsTechId = dao.insertCounterparty(
            CounterpartyEntity(
                name = "شرکت فناوری پارس (کارفرما)",
                category = EntityCategory.INSTITUTION.name,
                avatarColorHex = 0xFF53C9BD,
                cardNumber = "6274121190807060",
                shebaNumber = "IR340550011280004455667788",
                phoneNumber = "021-44228800",
                trustPriority = TrustPriority.NORMAL.name,
                description = "طلب قرارداد طراحی و بهینه‌سازی سامانه مالی",
                createdAtJalali = JalaliCalendarEngine.addJalaliMonths(todayStr, -1)
            )
        )

        // Category: PERSON (اشخاص حقیقی -> رضا محمدی، حاج‌آقا کریمی)
        val rezaId = dao.insertCounterparty(
            CounterpartyEntity(
                name = "رضا محمدی (دوست و همکار)",
                category = EntityCategory.PERSON.name,
                avatarColorHex = 0xFFDAB978,
                cardNumber = "6221061233448899",
                shebaNumber = "IR190540102580001234567890",
                phoneNumber = "09123456789",
                trustPriority = TrustPriority.CRITICAL.name,
                description = "قرض دوستانه قول شرف و حساب دفتری مشترک خرید قطعات",
                createdAtJalali = JalaliCalendarEngine.addJalaliMonths(todayStr, -1)
            )
        )

        val karimiLandlordId = dao.insertCounterparty(
            CounterpartyEntity(
                name = "حاج‌آقا کریمی (صاحب‌خانه)",
                category = EntityCategory.PERSON.name,
                avatarColorHex = 0xFFC99B68,
                cardNumber = "6104337811229900",
                shebaNumber = "IR910120000000009988771122",
                phoneNumber = "09129988776",
                trustPriority = TrustPriority.CRITICAL.name,
                description = "چک‌های صیادی اجاره‌بها و ودیعه مسکن",
                createdAtJalali = JalaliCalendarEngine.addJalaliMonths(todayStr, -2)
            )
        )

        // 2. BNPL Obligations under SnappPay & DigiPay
        val snappStart = JalaliCalendarEngine.addJalaliMonths(todayStr, -2)
        val snappLaptopId = dao.insertObligation(
            ObligationEntity(
                counterpartyId = snappPayId,
                title = "خرید اقساطی لپ‌تاپ ایسوس \u2066Vivobook\u2069",
                category = ObligationCategory.BNPL_PURCHASE.name,
                scheduleMode = ScheduleMode.AUTO_SCHEDULE.name,
                principalAmountToman = 38_000_000L,
                profitOrFeeToman = 0L,
                downPaymentToman = 8_000_000L,
                installmentCount = 4,
                paymentInterval = PaymentInterval.MONTHLY.name,
                firstDueDateJalali = snappStart,
                trustPriority = TrustPriority.HIGH.name,
                trackingOrCheckCode = "SNP-774120",
                notes = "خرید ۴ قسطه بدون سود از تکنولایف با اسنپ‌پی",
                createdAtJalali = snappStart
            )
        )
        dao.insertInstallments(
            InstallmentScheduleEngine.generateAutoInstallments(
                obligationId = snappLaptopId,
                titlePrefix = "قسط لپ‌تاپ (اسنپ‌پی)",
                netPayableToman = 30_000_000L,
                installmentCount = 4,
                firstDueDateJalali = snappStart,
                interval = PaymentInterval.MONTHLY,
                todayJalali = todayStr
            ).mapIndexed { idx, inst ->
                when (idx) {
                    0 -> inst.copy(
                        paidAmountToman = inst.amountToman,
                        status = InstallmentStatus.PAID.name,
                        paidDateJalali = inst.dueDateJalali,
                        trackingCode = "SNP-PAY-01"
                    )
                    1 -> inst.copy(
                        paidAmountToman = 4_000_000L,
                        status = InstallmentStatus.PARTIAL.name,
                        paidDateJalali = JalaliCalendarEngine.addJalaliDays(todayStr, -15),
                        trackingCode = "SNP-PART-02",
                        note = "۴ میلیون پرداخت شد، ۳.۵ میلیون مانده"
                    )
                    2 -> inst.copy(
                        dueDateJalali = todayStr,
                        status = InstallmentStatus.PENDING.name,
                        note = "سررسید امروز در کیف پول اسنپ‌پی"
                    )
                    else -> inst
                }
            }
        )

        val digiStart = JalaliCalendarEngine.addJalaliMonths(todayStr, -1)
        val digiPhoneId = dao.insertObligation(
            ObligationEntity(
                counterpartyId = digiPayId,
                title = "خرید گوشی سامسونگ \u2066Galaxy A55\u2069",
                category = ObligationCategory.BNPL_PURCHASE.name,
                scheduleMode = ScheduleMode.AUTO_SCHEDULE.name,
                principalAmountToman = 24_000_000L,
                profitOrFeeToman = 1_200_000L,
                downPaymentToman = 4_200_000L,
                installmentCount = 6,
                paymentInterval = PaymentInterval.MONTHLY.name,
                firstDueDateJalali = digiStart,
                trustPriority = TrustPriority.HIGH.name,
                trackingOrCheckCode = "DGP-90812",
                notes = "خرید ۶ قسطه با اعتبار دیجی‌پی از دیجی‌کالا",
                createdAtJalali = digiStart
            )
        )
        dao.insertInstallments(
            InstallmentScheduleEngine.generateAutoInstallments(
                obligationId = digiPhoneId,
                titlePrefix = "قسط گوشی سامسونگ (دیجی‌پی)",
                netPayableToman = 21_000_000L,
                installmentCount = 6,
                firstDueDateJalali = digiStart,
                interval = PaymentInterval.MONTHLY,
                todayJalali = todayStr
            ).mapIndexed { idx, inst ->
                if (idx == 0) {
                    inst.copy(
                        paidAmountToman = inst.amountToman,
                        status = InstallmentStatus.PAID.name,
                        paidDateJalali = inst.dueDateJalali,
                        trackingCode = "DGP-PAY-01"
                    )
                } else inst
            }
        )

        // 3. Bank Loans under Bank Mehr & BluBank
        val mehrStart = JalaliCalendarEngine.addJalaliMonths("$currentMonthPrefix/05", -3)
        val mehrLoanId = dao.insertObligation(
            ObligationEntity(
                counterpartyId = bankMehrId,
                title = "وام قرض‌الحسنه بانک مهر ایران",
                category = ObligationCategory.INSTALLMENT_LOAN.name,
                scheduleMode = ScheduleMode.AUTO_SCHEDULE.name,
                principalAmountToman = 120_000_000L,
                profitOrFeeToman = 4_800_000L,
                downPaymentToman = 0L,
                installmentCount = 12,
                paymentInterval = PaymentInterval.MONTHLY.name,
                firstDueDateJalali = mehrStart,
                trustPriority = TrustPriority.HIGH.name,
                trackingOrCheckCode = "MEHR-405-8821",
                notes = "کارمزد ۴ درصد سالانه — پرداخت از طریق همراه بانک مهر",
                createdAtJalali = mehrStart
            )
        )
        dao.insertInstallments(
            InstallmentScheduleEngine.generateAutoInstallments(
                obligationId = mehrLoanId,
                titlePrefix = "قسط وام بانک مهر",
                netPayableToman = 124_800_000L,
                installmentCount = 12,
                firstDueDateJalali = mehrStart,
                interval = PaymentInterval.MONTHLY,
                todayJalali = todayStr
            ).mapIndexed { index, item ->
                if (index < 3) {
                    item.copy(
                        paidAmountToman = item.amountToman,
                        status = InstallmentStatus.PAID.name,
                        paidDateJalali = item.dueDateJalali,
                        trackingCode = "TRK-990${index + 1}"
                    )
                } else item
            }
        )

        val bluStart = JalaliCalendarEngine.addJalaliDays(todayStr, 8)
        val bluLoanId = dao.insertObligation(
            ObligationEntity(
                counterpartyId = bluBankId,
                title = "وام فوری بلوبانک (طرح اعتبار پلاس)",
                category = ObligationCategory.INSTALLMENT_LOAN.name,
                scheduleMode = ScheduleMode.AUTO_SCHEDULE.name,
                principalAmountToman = 40_000_000L,
                profitOrFeeToman = 3_600_000L,
                downPaymentToman = 0L,
                installmentCount = 6,
                paymentInterval = PaymentInterval.MONTHLY.name,
                firstDueDateJalali = bluStart,
                trustPriority = TrustPriority.HIGH.name,
                trackingOrCheckCode = "BLU-44510",
                notes = "برداشت خودکار از حساب بلوبانک در سررسید",
                createdAtJalali = JalaliCalendarEngine.addJalaliMonths(todayStr, -1)
            )
        )
        dao.insertInstallments(
            InstallmentScheduleEngine.generateAutoInstallments(
                obligationId = bluLoanId,
                titlePrefix = "قسط وام بلوبانک",
                netPayableToman = 43_600_000L,
                installmentCount = 6,
                firstDueDateJalali = bluStart,
                interval = PaymentInterval.MONTHLY,
                todayJalali = todayStr
            )
        )

        // 4. Personal Debt & Running Ledger under Reza Mohammadi
        val rezaDue = JalaliCalendarEngine.addJalaliDays(todayStr, -3)
        val rezaObligationId = dao.insertObligation(
            ObligationEntity(
                counterpartyId = rezaId,
                title = "حساب دفتری و قرض دستی رضا محمدی",
                category = ObligationCategory.PERSONAL_DEBT.name,
                scheduleMode = ScheduleMode.RUNNING_LEDGER.name,
                principalAmountToman = 18_500_000L,
                profitOrFeeToman = 0L,
                downPaymentToman = 0L,
                installmentCount = 2,
                paymentInterval = PaymentInterval.MONTHLY.name,
                firstDueDateJalali = rezaDue,
                trustPriority = TrustPriority.CRITICAL.name,
                trackingOrCheckCode = "PROMISE-REZA",
                notes = "قول شرف برای تسویه تا پایان هفته + خرید مشترک قطعات",
                createdAtJalali = JalaliCalendarEngine.addJalaliDays(todayStr, -25)
            )
        )
        dao.insertSubRecords(
            listOf(
                LedgerSubRecordEntity(
                    obligationId = rezaObligationId,
                    title = "قرض دستی اولیه (کارت‌به‌کارت)",
                    dateJalali = JalaliCalendarEngine.addJalaliDays(todayStr, -25),
                    amountToman = 12_000_000L,
                    recordType = SubRecordType.CHARGE.name,
                    receiptCode = "CRD-1102"
                ),
                LedgerSubRecordEntity(
                    obligationId = rezaObligationId,
                    title = "خرید مشترک مانیتور ال‌جی",
                    dateJalali = JalaliCalendarEngine.addJalaliDays(todayStr, -14),
                    amountToman = 6_500_000L,
                    recordType = SubRecordType.CHARGE.name,
                    receiptCode = "INV-448"
                ),
                LedgerSubRecordEntity(
                    obligationId = rezaObligationId,
                    title = "پرداخت علی‌الحساب مرحله اول",
                    dateJalali = JalaliCalendarEngine.addJalaliDays(todayStr, -7),
                    amountToman = 5_000_000L,
                    recordType = SubRecordType.PAYMENT.name,
                    receiptCode = "PAY-8812"
                )
            )
        )
        dao.insertInstallments(
            listOf(
                InstallmentEntity(
                    obligationId = rezaObligationId,
                    installmentNumber = 1,
                    title = "پرداخت مرحله اول حساب دفتری رضا",
                    dueDateJalali = JalaliCalendarEngine.addJalaliDays(todayStr, -7),
                    amountToman = 5_000_000L,
                    paidAmountToman = 5_000_000L,
                    status = InstallmentStatus.PAID.name,
                    paidDateJalali = JalaliCalendarEngine.addJalaliDays(todayStr, -7),
                    trackingCode = "PAY-8812"
                ),
                InstallmentEntity(
                    obligationId = rezaObligationId,
                    installmentNumber = 2,
                    title = "تسویه مانده قول شرف رضا محمدی",
                    dueDateJalali = rezaDue,
                    amountToman = 13_500_000L,
                    paidAmountToman = 0L,
                    status = InstallmentStatus.OVERDUE.name,
                    note = "اولویت حیاتی — ۳ روز از تاریخ قول گذشته است"
                )
            )
        )

        // 5. Sayyadi Check for Rent under Karimi Landlord
        val checkDue = JalaliCalendarEngine.addJalaliDays(todayStr, 3)
        val checkObligationId = dao.insertObligation(
            ObligationEntity(
                counterpartyId = karimiLandlordId,
                title = "چک صیادی اجاره‌بهای فصل پاییز",
                category = ObligationCategory.CHECK_OR_BILL.name,
                scheduleMode = ScheduleMode.AUTO_SCHEDULE.name,
                principalAmountToman = 24_000_000L,
                profitOrFeeToman = 0L,
                downPaymentToman = 0L,
                installmentCount = 2,
                paymentInterval = PaymentInterval.MONTHLY.name,
                firstDueDateJalali = checkDue,
                trustPriority = TrustPriority.CRITICAL.name,
                trackingOrCheckCode = "SAYYAD-1098443211009876",
                notes = "چک بنفش صیادی ثبت‌شده در سامانه صیاد",
                createdAtJalali = JalaliCalendarEngine.addJalaliMonths(todayStr, -1)
            )
        )
        dao.insertInstallments(
            listOf(
                InstallmentEntity(
                    obligationId = checkObligationId,
                    installmentNumber = 1,
                    title = "چک صیادی اول اجاره‌بها (۱۰۹۸۴۴۳۲)",
                    dueDateJalali = checkDue,
                    amountToman = 12_000_000L,
                    paidAmountToman = 0L,
                    status = InstallmentStatus.PENDING.name
                ),
                InstallmentEntity(
                    obligationId = checkObligationId,
                    installmentNumber = 2,
                    title = "چک صیادی دوم اجاره‌بها (۱۰۹۸۴۴۳۳)",
                    dueDateJalali = JalaliCalendarEngine.addJalaliMonths(checkDue, 1),
                    amountToman = 12_000_000L,
                    paidAmountToman = 0L,
                    status = InstallmentStatus.PENDING.name
                )
            )
        )

        // 6. Sandogh Omid Loan
        val sandoghStart = JalaliCalendarEngine.addJalaliDays(todayStr, 6)
        val sandoghId = dao.insertObligation(
            ObligationEntity(
                counterpartyId = sandoghOmidId,
                title = "وام صندوق قرض‌الحسنه امید خانواده",
                category = ObligationCategory.INSTALLMENT_LOAN.name,
                scheduleMode = ScheduleMode.AUTO_SCHEDULE.name,
                principalAmountToman = 30_000_000L,
                profitOrFeeToman = 600_000L,
                downPaymentToman = 0L,
                installmentCount = 6,
                paymentInterval = PaymentInterval.MONTHLY.name,
                firstDueDateJalali = sandoghStart,
                trustPriority = TrustPriority.HIGH.name,
                trackingOrCheckCode = "OMID-1405-09",
                notes = "کارمزد ۲٪ صندوق خانوادگی",
                createdAtJalali = JalaliCalendarEngine.addJalaliMonths(todayStr, -1)
            )
        )
        dao.insertInstallments(
            InstallmentScheduleEngine.generateAutoInstallments(
                obligationId = sandoghId,
                titlePrefix = "قسط صندوق امید خانواده",
                netPayableToman = 30_600_000L,
                installmentCount = 6,
                firstDueDateJalali = sandoghStart,
                interval = PaymentInterval.MONTHLY,
                todayJalali = todayStr
            )
        )

        // 7. Receivable from Pars Tech
        val receivableDue = JalaliCalendarEngine.addJalaliDays(todayStr, 5)
        val receivableId = dao.insertObligation(
            ObligationEntity(
                counterpartyId = parsTechId,
                title = "طلب قرارداد طراحی سامانه مالی پارس",
                category = ObligationCategory.PERSONAL_RECEIVABLE.name,
                scheduleMode = ScheduleMode.AUTO_SCHEDULE.name,
                principalAmountToman = 45_000_000L,
                profitOrFeeToman = 0L,
                downPaymentToman = 15_000_000L,
                installmentCount = 2,
                paymentInterval = PaymentInterval.MONTHLY.name,
                firstDueDateJalali = receivableDue,
                trustPriority = TrustPriority.HIGH.name,
                trackingOrCheckCode = "CNT-PARS-405",
                notes = "پیش‌پرداخت ۱۵ میلیون دریافت شد؛ دو قسط ۱۵ میلیونی باقی‌مانده",
                createdAtJalali = JalaliCalendarEngine.addJalaliDays(todayStr, -20)
            )
        )
        dao.insertInstallments(
            listOf(
                InstallmentEntity(
                    obligationId = receivableId,
                    installmentNumber = 1,
                    title = "دریافت قسط اول قرارداد پارس",
                    dueDateJalali = receivableDue,
                    amountToman = 15_000_000L,
                    paidAmountToman = 0L,
                    status = InstallmentStatus.PENDING.name
                ),
                InstallmentEntity(
                    obligationId = receivableId,
                    installmentNumber = 2,
                    title = "دریافت تسویه نهایی قرارداد پارس",
                    dueDateJalali = JalaliCalendarEngine.addJalaliMonths(receivableDue, 1),
                    amountToman = 15_000_000L,
                    paidAmountToman = 0L,
                    status = InstallmentStatus.PENDING.name
                )
            )
        )
    }
}
