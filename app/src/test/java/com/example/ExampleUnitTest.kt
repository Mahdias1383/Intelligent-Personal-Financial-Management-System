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

    @Test
    fun verifyTarazinoMotionSpecsExplicitSpringConfigurations() {
        val bouncy = com.example.ui.components.TarazinoMotionSpecs.fluidBouncySpring()
        assertEquals(androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, bouncy.dampingRatio, 0.0001f)
        assertEquals(androidx.compose.animation.core.Spring.StiffnessLow, bouncy.stiffness, 0.0001f)
        assertEquals(0.005f, bouncy.visibilityThreshold ?: 0f, 0.0001f)

        val bouncyOffset = com.example.ui.components.TarazinoMotionSpecs.fluidBouncyIntOffsetSpring()
        assertEquals(androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, bouncyOffset.dampingRatio, 0.0001f)
        assertEquals(androidx.compose.animation.core.Spring.StiffnessLow, bouncyOffset.stiffness, 0.0001f)

        val bouncySize = com.example.ui.components.TarazinoMotionSpecs.fluidBouncyIntSizeSpring()
        assertEquals(androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, bouncySize.dampingRatio, 0.0001f)
        assertEquals(androidx.compose.animation.core.Spring.StiffnessLow, bouncySize.stiffness, 0.0001f)

        val press = com.example.ui.components.TarazinoMotionSpecs.pressScaleSpring()
        assertEquals(androidx.compose.animation.core.Spring.DampingRatioNoBouncy, press.dampingRatio, 0.0001f)
        assertEquals(androidx.compose.animation.core.Spring.StiffnessHigh, press.stiffness, 0.0001f)
        assertEquals(0.004f, press.visibilityThreshold ?: 0f, 0.0001f)

        val control = com.example.ui.components.TarazinoMotionSpecs.snappyControlSpring()
        assertEquals(androidx.compose.animation.core.Spring.DampingRatioNoBouncy, control.dampingRatio, 0.0001f)
        assertEquals(androidx.compose.animation.core.Spring.StiffnessMedium, control.stiffness, 0.0001f)
        assertEquals(0.005f, control.visibilityThreshold ?: 0f, 0.0001f)

        val chart = com.example.ui.components.TarazinoMotionSpecs.chartSweepSpring()
        assertEquals(androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, chart.dampingRatio, 0.0001f)
        assertEquals(androidx.compose.animation.core.Spring.StiffnessLow, chart.stiffness, 0.0001f)
        assertEquals(0.005f, chart.visibilityThreshold ?: 0f, 0.0001f)

        val flip = com.example.ui.components.TarazinoMotionSpecs.cardFlipSpring()
        assertEquals(androidx.compose.animation.core.Spring.DampingRatioNoBouncy, flip.dampingRatio, 0.0001f)
        assertEquals(androidx.compose.animation.core.Spring.StiffnessMedium, flip.stiffness, 0.0001f)
        assertEquals(0.5f, flip.visibilityThreshold ?: 0f, 0.0001f)

        val spotlight = com.example.ui.components.TarazinoMotionSpecs.spotlightGlideSpring()
        assertEquals(androidx.compose.animation.core.Spring.DampingRatioNoBouncy, spotlight.dampingRatio, 0.0001f)
        assertEquals(androidx.compose.animation.core.Spring.StiffnessHigh, spotlight.stiffness, 0.0001f)
        assertEquals(0.5f, spotlight.visibilityThreshold ?: 0f, 0.0001f)
    }
}
