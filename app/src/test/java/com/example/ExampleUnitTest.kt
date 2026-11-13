package com.example

import com.example.core.CurrencyUnit
import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.core.SecurityVaultEngine
import com.example.data.PaymentInterval
import com.example.domain.InstallmentScheduleEngine
import com.example.domain.QaDiagnosticsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun verifyAll10QaDiagnosticSuitesPass() {
        val results = QaDiagnosticsEngine.runAll10DiagnosticSuites()
        assertEquals(10, results.size)
        results.forEach { suite ->
            assertTrue("Suite ${suite.suiteId} (${suite.titleFa}) failed: ${suite.detailFa}", suite.passed)
        }
    }

    @Test
    fun verifyJalaliMonthClampingAndPersianWordifier() {
        assertEquals("1405/07/30", JalaliCalendarEngine.addJalaliMonths("1405/06/31", 1, anchorDay = 31))
        assertEquals("1405/12/29", JalaliCalendarEngine.addJalaliMonths("1405/06/31", 6, anchorDay = 31))
        assertEquals(
            "پانزده میلیون و چهارصد هزار تومان",
            PersianCurrencyEngine.amountToPersianWords(15_400_000L, CurrencyUnit.TOMAN)
        )
    }

    @Test
    fun verifyExactInstallmentIntegerSplitAndPinHash() {
        val schedule = InstallmentScheduleEngine.generateAutoInstallments(
            obligationId = 1L,
            titlePrefix = "وام تست",
            netPayableToman = 25_000_000L,
            installmentCount = 6,
            firstDueDateJalali = "1405/08/01",
            interval = PaymentInterval.MONTHLY,
            todayJalali = "1405/08/01"
        )
        assertEquals(25_000_000L, schedule.sumOf { it.amountToman })

        val pinHash = SecurityVaultEngine.hashWithSalt("۲۵۸۰")
        assertTrue(SecurityVaultEngine.verifyPin("2580", pinHash))
    }
}
