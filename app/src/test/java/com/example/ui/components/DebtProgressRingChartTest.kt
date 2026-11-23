package com.example.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebtProgressRingChartTest {

    @Test
    fun `computeDebtProgressRingMetrics calculates exact paid vs outstanding ratios and arc sweeps`() {
        val metrics = computeDebtProgressRingMetrics(
            paidDebtToman = 60_000_000L,
            outstandingDebtToman = 140_000_000L,
            padAngleDegrees = 10f
        )

        assertEquals(200_000_000L, metrics.totalDebtToman)
        assertEquals(60_000_000L, metrics.paidDebtToman)
        assertEquals(140_000_000L, metrics.outstandingDebtToman)
        assertEquals(30, metrics.paidPercent)
        assertEquals(70, metrics.outstandingPercent)
        assertEquals(100, metrics.paidPercent + metrics.outstandingPercent)

        // Available sweep with 2 * 10 deg padAngle = 340 deg
        assertEquals(340f, metrics.paidSweepDegrees + metrics.outstandingSweepDegrees, 0.01f)
        assertEquals(340f * 0.30f, metrics.paidSweepDegrees, 0.01f)
        assertEquals(340f * 0.70f, metrics.outstandingSweepDegrees, 0.01f)
    }

    @Test
    fun `computeDebtProgressRingMetrics handles zero total debt gracefully`() {
        val metrics = computeDebtProgressRingMetrics(
            paidDebtToman = 0L,
            outstandingDebtToman = 0L
        )
        assertEquals(0L, metrics.totalDebtToman)
        assertEquals(100, metrics.paidPercent)
        assertEquals(0, metrics.outstandingPercent)
        assertEquals(360f, metrics.paidSweepDegrees, 0.01f)
        assertEquals(0f, metrics.outstandingSweepDegrees, 0.01f)
    }

    @Test
    fun `computeDebtProgressRingMetrics handles 100 percent outstanding debt without padAngle overflow`() {
        val metrics = computeDebtProgressRingMetrics(
            paidDebtToman = 0L,
            outstandingDebtToman = 85_000_000L,
            padAngleDegrees = 10f
        )
        assertEquals(0, metrics.paidPercent)
        assertEquals(100, metrics.outstandingPercent)
        assertEquals(0f, metrics.paidSweepDegrees, 0.01f)
        assertEquals(360f, metrics.outstandingSweepDegrees, 0.01f)
        assertTrue(metrics.outstandingRatio == 1f)
    }
}
