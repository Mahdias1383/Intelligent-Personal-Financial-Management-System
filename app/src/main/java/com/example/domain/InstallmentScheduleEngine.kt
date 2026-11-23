package com.example.domain

import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.PaymentInterval

object InstallmentScheduleEngine {

    /**
     * Generates an exact installment schedule where the sum of all installment amounts
     * equals [netPayableToman] with ZERO rounding discrepancy.
     * Any integer division remainder is placed on the final installment.
     */
    fun generateAutoInstallments(
        obligationId: Long,
        titlePrefix: String,
        netPayableToman: Long,
        installmentCount: Int,
        firstDueDateJalali: String,
        interval: PaymentInterval,
        todayJalali: String = JalaliCalendarEngine.todayJalali()
    ): List<InstallmentEntity> {
        val safeCount = installmentCount.coerceAtLeast(1)
        val safeTotal = netPayableToman.coerceAtLeast(0L)
        val baseAmount = safeTotal / safeCount
        val remainder = safeTotal % safeCount

        val dueDates = JalaliCalendarEngine.generateInstallmentDueDates(
            firstDueDateJalali = firstDueDateJalali,
            installmentCount = safeCount,
            interval = interval
        )

        return (0 until safeCount).map { index ->
            val instNumber = index + 1
            val amount = if (index == safeCount - 1) {
                baseAmount + remainder
            } else {
                baseAmount
            }

            val dueDate = dueDates[index]

            val daysFromToday = JalaliCalendarEngine.diffJalaliDays(todayJalali, dueDate)
            val initialStatus = if (daysFromToday < 0 && amount > 0L) {
                InstallmentStatus.OVERDUE
            } else {
                InstallmentStatus.PENDING
            }

            val numFa = PersianCurrencyEngine.toPersianDigits(instNumber.toString())
            InstallmentEntity(
                obligationId = obligationId,
                installmentNumber = instNumber,
                title = "$titlePrefix — قسط $numFa",
                dueDateJalali = dueDate,
                amountToman = amount,
                paidAmountToman = 0L,
                lateFeeToman = 0L,
                discountToman = 0L,
                status = initialStatus.name
            )
        }
    }

    /**
     * Applies a full or partial payment, late fee, discount, and tracking code to an installment,
     * automatically computing its updated status.
     */
    fun applyPaymentUpdate(
        installment: InstallmentEntity,
        paidAmountToman: Long,
        lateFeeToman: Long = installment.lateFeeToman,
        discountToman: Long = installment.discountToman,
        paidDateJalali: String = JalaliCalendarEngine.todayJalali(),
        trackingCode: String = installment.trackingCode,
        note: String = installment.note,
        todayJalali: String = JalaliCalendarEngine.todayJalali()
    ): InstallmentEntity {
        val effectiveTotal = (installment.amountToman + lateFeeToman - discountToman).coerceAtLeast(0L)
        val clampedPaid = paidAmountToman.coerceIn(0L, effectiveTotal)
        val newStatus = when {
            clampedPaid >= effectiveTotal && effectiveTotal > 0L -> InstallmentStatus.PAID
            clampedPaid > 0L -> InstallmentStatus.PARTIAL
            JalaliCalendarEngine.diffJalaliDays(todayJalali, installment.dueDateJalali) < 0 -> InstallmentStatus.OVERDUE
            else -> InstallmentStatus.PENDING
        }
        return installment.copy(
            paidAmountToman = clampedPaid,
            lateFeeToman = lateFeeToman.coerceAtLeast(0L),
            discountToman = discountToman.coerceAtLeast(0L),
            status = newStatus.name,
            paidDateJalali = if (clampedPaid > 0L) paidDateJalali else "",
            trackingCode = trackingCode,
            note = note
        )
    }
}
