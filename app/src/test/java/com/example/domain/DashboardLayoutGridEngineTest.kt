package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardLayoutGridEngineTest {

    @Test
    fun defaultConfig_containsAllModulesUniquely() {
        val cfg = DashboardLayoutGridEngine.defaultConfig()
        assertEquals(DashboardModuleId.entries.size, cfg.orderedModules.size)
        assertEquals(DashboardModuleId.entries.toSet(), cfg.orderedModules.toSet())
        assertTrue(cfg.hiddenModules.isEmpty())
        assertEquals(DashboardFocusPreset.BALANCED_DEFAULT, cfg.activePreset)
    }

    @Test
    fun moveModule_reordersForwardAndBackwardAndMarksCustomPreset() {
        val initial = DashboardLayoutGridEngine.defaultConfig()
        val radarIndex = initial.orderedModules.indexOf(DashboardModuleId.FINANCIAL_HEALTH_RADAR)
        assertTrue(radarIndex > 0)

        val movedToTop = DashboardLayoutGridEngine.moveModule(initial, radarIndex, 0)
        assertEquals(DashboardModuleId.FINANCIAL_HEALTH_RADAR, movedToTop.orderedModules[0])
        assertEquals(DashboardFocusPreset.CUSTOM, movedToTop.activePreset)
        assertEquals(DashboardModuleId.entries.size, movedToTop.orderedModules.size)

        val movedDown = DashboardLayoutGridEngine.moveModule(movedToTop, 0, 3)
        assertEquals(DashboardModuleId.FINANCIAL_HEALTH_RADAR, movedDown.orderedModules[3])
    }

    @Test
    fun pinModuleToTop_movesToIndexZeroAndUnhidesIfHidden() {
        val hiddenSnowball = DashboardLayoutGridEngine.toggleModuleVisibility(
            DashboardLayoutGridEngine.defaultConfig(),
            DashboardModuleId.DEBT_SNOWBALL_TRACKER
        )
        assertTrue(DashboardModuleId.DEBT_SNOWBALL_TRACKER in hiddenSnowball.hiddenModules)

        val pinned = DashboardLayoutGridEngine.pinModuleToTop(
            hiddenSnowball,
            DashboardModuleId.DEBT_SNOWBALL_TRACKER
        )
        assertEquals(DashboardModuleId.DEBT_SNOWBALL_TRACKER, pinned.orderedModules.first())
        assertFalse(DashboardModuleId.DEBT_SNOWBALL_TRACKER in pinned.hiddenModules)
    }

    @Test
    fun applyFocusPresets_prioritizesRadarAndDebtSnowballRespectively() {
        val radarFocus = DashboardLayoutGridEngine.applyFocusPreset(
            DashboardFocusPreset.FINANCIAL_HEALTH_RADAR_FOCUS
        )
        assertEquals(DashboardModuleId.FINANCIAL_HEALTH_RADAR, radarFocus.orderedModules[0])
        assertEquals(DashboardFocusPreset.FINANCIAL_HEALTH_RADAR_FOCUS, radarFocus.activePreset)

        val snowballFocus = DashboardLayoutGridEngine.applyFocusPreset(
            DashboardFocusPreset.DEBT_SNOWBALL_FOCUS
        )
        assertEquals(DashboardModuleId.DEBT_SNOWBALL_TRACKER, snowballFocus.orderedModules[0])
        assertEquals(DashboardFocusPreset.DEBT_SNOWBALL_FOCUS, snowballFocus.activePreset)
    }

    @Test
    fun toggleVisibility_neverAllowsHidingFinalRemainingVisibleModule() {
        var cfg = DashboardLayoutGridEngine.defaultConfig()
        DashboardModuleId.entries.drop(1).forEach { id ->
            cfg = DashboardLayoutGridEngine.toggleModuleVisibility(cfg, id)
        }
        assertEquals(1, cfg.visibleOrderedModules.size)
        val lastVisible = cfg.visibleOrderedModules.first()

        val afterAttempt = DashboardLayoutGridEngine.toggleModuleVisibility(cfg, lastVisible)
        assertEquals(1, afterAttempt.visibleOrderedModules.size)
        assertEquals(lastVisible, afterAttempt.visibleOrderedModules.first())
    }

    @Test
    fun serializeAndDeserialize_roundTripsOrderHiddenAndPresetSafely() {
        var cfg = DashboardLayoutGridEngine.applyFocusPreset(DashboardFocusPreset.DEBT_SNOWBALL_FOCUS)
        cfg = DashboardLayoutGridEngine.pinModuleToTop(cfg, DashboardModuleId.FINANCIAL_HEALTH_RADAR)
        cfg = DashboardLayoutGridEngine.toggleModuleVisibility(cfg, DashboardModuleId.DEBT_TAG_FILTER_VIEW)

        val serialized = DashboardLayoutGridEngine.serializeConfig(cfg)
        val restored = DashboardLayoutGridEngine.deserializeConfig(serialized)

        assertEquals(cfg.orderedModules, restored.orderedModules)
        assertEquals(cfg.hiddenModules, restored.hiddenModules)
        assertEquals(cfg.activePreset, restored.activePreset)

        // Corrupted input safely falls back to complete default
        val fallback = DashboardLayoutGridEngine.deserializeConfig("BROKEN_DATA")
        assertEquals(DashboardLayoutGridEngine.DEFAULT_ORDER, fallback.orderedModules)
    }
}
