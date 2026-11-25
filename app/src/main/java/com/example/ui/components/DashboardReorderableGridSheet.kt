package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.example.core.PersianCurrencyEngine
import com.example.domain.DashboardFocusPreset
import com.example.domain.DashboardLayoutConfig
import com.example.domain.DashboardModuleId
import com.example.ui.theme.LocalTarazinoColors

fun dashboardModuleIcon(moduleId: DashboardModuleId): ImageVector = when (moduleId) {
    DashboardModuleId.PLATINUM_VAULT_HERO -> Icons.Default.CreditCard
    DashboardModuleId.AI_DAILY_FINANCIAL_TIP -> Icons.Default.AutoAwesome
    DashboardModuleId.FINANCIAL_HEALTH_RADAR -> Icons.Default.Radar
    DashboardModuleId.DEBT_SNOWBALL_TRACKER -> Icons.Default.AcUnit
    DashboardModuleId.SHAMSI_DEBT_AND_SAVINGS -> Icons.Default.Savings
    DashboardModuleId.URGENT_ALERTS_RADAR -> Icons.Default.NotificationsActive
    DashboardModuleId.PAID_VS_OUTSTANDING_RING -> Icons.Default.PieChart
    DashboardModuleId.QUICK_ACTIONS_AND_SCANNER -> Icons.Default.Widgets
    DashboardModuleId.COUNTERPARTY_WALLET_DECK -> Icons.Default.CreditCard
    DashboardModuleId.WEEKLY_CASHFLOW_STRIP -> Icons.Default.CalendarMonth
    DashboardModuleId.CATEGORY_DONUT_CHART -> Icons.Default.DonutLarge
    DashboardModuleId.DEBT_TAG_FILTER_VIEW -> Icons.Default.FilterAlt
    DashboardModuleId.SAFE_CAPACITY_ML_FORECAST -> Icons.Default.Insights
}

/**
 * Compact Dashboard toolbar allowing 1-tap access to the Drag-and-Drop Reorderable Grid Studio,
 * inline card-by-card reordering, and instant Focus Presets (Financial Health Radar / Debt Snowball).
 */
@Composable
fun DashboardLayoutCustomizeToolbar(
    layoutConfig: DashboardLayoutConfig,
    isInlineReorderMode: Boolean,
    onToggleInlineReorderMode: () -> Unit,
    onOpenReorderGridModal: () -> Unit,
    onApplyPreset: (DashboardFocusPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTarazinoColors.current
    val visibleCount = layoutConfig.visibleOrderedModules.size
    val totalCount = layoutConfig.orderedModules.size

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (ext.isDark) 0.90f else 0.96f),
        border = BorderStroke(
            1.dp,
            if (isInlineReorderMode) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_customize_layout_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.DashboardCustomize,
                            contentDescription = "شخصی‌سازی چیدمان داشبورد",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isInlineReorderMode) {
                                "حالت اولویت‌بندی فعال است"
                            } else {
                                layoutConfig.activePreset.labelFa
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${PersianCurrencyEngine.toPersianDigits(visibleCount.toString())} از ${
                                PersianCurrencyEngine.toPersianDigits(totalCount.toString())
                            } ماژول نمایش داده می‌شود",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Inline Quick Reorder Mode Toggle
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isInlineReorderMode) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        },
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onToggleInlineReorderMode)
                            .testTag("dashboard_inline_reorder_toggle_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = if (isInlineReorderMode) Icons.Default.Check else Icons.Default.Tune,
                                contentDescription = if (isInlineReorderMode) "اتمام جابه‌جایی" else "جابه‌جایی سریع",
                                tint = if (isInlineReorderMode) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isInlineReorderMode) "ذخیره" else "ترتیب",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isInlineReorderMode) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }

                    // Open Full 2-Column Drag-and-Drop Grid Studio
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenReorderGridModal)
                            .testTag("dashboard_open_reorder_grid_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "شبکه کشیدن و رها کردن ماژول‌ها",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "شبکه چیدمان",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            // 1-Tap Focus Preset Strip
            val presets = listOf(
                DashboardFocusPreset.FINANCIAL_HEALTH_RADAR_FOCUS,
                DashboardFocusPreset.DEBT_SNOWBALL_FOCUS,
                DashboardFocusPreset.URGENT_DEADLINES_FOCUS,
                DashboardFocusPreset.BALANCED_DEFAULT
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                presets.forEach { preset ->
                    val selected = layoutConfig.activePreset == preset
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        },
                        border = BorderStroke(
                            0.85.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { onApplyPreset(preset) }
                            .testTag("dashboard_focus_preset_${preset.name}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 11.dp)
                        ) {
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                            Text(
                                text = preset.shortLabelFa,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Inline reorder chrome bar wrapped around each module when the user activates inline reorder mode
 * directly inside the Dashboard Overview feed.
 */
@Composable
fun InlineReorderableModuleWrapper(
    moduleId: DashboardModuleId,
    rankIndex: Int,
    totalVisible: Int,
    isInlineReorderMode: Boolean,
    onPinToTop: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onHideModule: () -> Unit,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    if (!isInlineReorderMode) {
        content()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
            .padding(6.dp)
            .testTag("inline_reorder_module_${moduleId.name}"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "#${PersianCurrencyEngine.toPersianDigits((rankIndex + 1).toString())}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = moduleId.shortTitleFa,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPinToTop()
                        },
                        enabled = rankIndex > 0,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("inline_pin_top_${moduleId.name}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerticalAlignTop,
                            contentDescription = "انتقال به بالاترین اولویت",
                            tint = if (rankIndex > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onMoveUp()
                        },
                        enabled = rankIndex > 0,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("inline_move_up_${moduleId.name}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "انتقال به بالا",
                            tint = if (rankIndex > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onMoveDown()
                        },
                        enabled = rankIndex < totalVisible - 1,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("inline_move_down_${moduleId.name}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "انتقال به پایین",
                            tint = if (rankIndex < totalVisible - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onHideModule()
                        },
                        enabled = totalVisible > 1,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("inline_hide_${moduleId.name}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "مخفی کردن ماژول",
                            tint = if (totalVisible > 1) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        content()
    }
}

/**
 * Full-screen interactive 2-Column Drag-and-Drop Reorderable Grid Studio.
 * Supports true pointer drag-and-drop across 2-column grid tiles (via long-press on any tile OR
 * immediate drag on the drag handle), live slot swapping with haptic feedback, 1-tap focus presets,
 * pin-to-top, and per-module show/hide controls.
 */
@Composable
fun DashboardReorderableGridModal(
    layoutConfig: DashboardLayoutConfig,
    onMoveModule: (fromIndex: Int, toIndex: Int) -> Unit,
    onPinModuleToTop: (DashboardModuleId) -> Unit,
    onToggleVisibility: (DashboardModuleId) -> Unit,
    onApplyPreset: (DashboardFocusPreset) -> Unit,
    onResetDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalTarazinoColors.current
    val haptic = LocalHapticFeedback.current
    val currentConfig by rememberUpdatedState(layoutConfig)

    var draggingModule by remember { mutableStateOf<DashboardModuleId?>(null) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var dragOriginBounds by remember { mutableStateOf<Rect?>(null) }
    val tileWindowBounds = remember { mutableStateMapOf<DashboardModuleId, Rect>() }

    fun handleLiveSwapIfCrossed(draggedId: DashboardModuleId) {
        val origin = dragOriginBounds ?: return
        val currentCenter = Offset(
            x = origin.center.x + dragOffsetX,
            y = origin.center.y + dragOffsetY
        )
        val ordered = currentConfig.orderedModules
        val fromIdx = ordered.indexOf(draggedId)
        if (fromIdx < 0) return

        val targetEntry = tileWindowBounds.entries.firstOrNull { (candidateId, rect) ->
            candidateId != draggedId && rect.contains(currentCenter)
        } ?: return

        val toIdx = ordered.indexOf(targetEntry.key)
        if (toIdx >= 0 && toIdx != fromIdx) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            // Shift origin smoothly so tile stays directly under the user's finger after swapping slots
            val newRect = targetEntry.value
            dragOffsetX += (origin.center.x - newRect.center.x)
            dragOffsetY += (origin.center.y - newRect.center.y)
            dragOriginBounds = newRect
            onMoveModule(fromIdx, toIdx)
        }
    }

    TarazinoRtlDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler(onBack = onDismiss)

        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("dashboard_reorderable_grid_modal")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        TarazinoIconBadge(
                            icon = Icons.Default.GridView,
                            accentColor = MaterialTheme.colorScheme.primary,
                            size = 42.dp,
                            iconSize = 21.dp,
                            cornerRadius = 14.dp
                        )
                        Column {
                            Text(
                                text = "شبکه چیدمان و اولویت‌بندی داشبورد",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "دستگیره هر کارت را بکشید و در جایگاه دلخواه رها کنید",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("dashboard_grid_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن شبکه چیدمان",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Focus Presets Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "تمرکز سریع داشبورد (تنظیم اولویت با یک لمس):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            DashboardFocusPreset.FINANCIAL_HEALTH_RADAR_FOCUS,
                            DashboardFocusPreset.DEBT_SNOWBALL_FOCUS,
                            DashboardFocusPreset.URGENT_DEADLINES_FOCUS,
                            DashboardFocusPreset.BALANCED_DEFAULT
                        ).forEach { preset ->
                            val isSelected = layoutConfig.activePreset == preset
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onApplyPreset(preset)
                                    }
                                    .testTag("grid_modal_preset_${preset.name}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 13.dp)
                                ) {
                                    Text(
                                        text = preset.shortLabelFa,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive 2-Column Drag-and-Drop Grid Body
                val rows = remember(layoutConfig.orderedModules) {
                    layoutConfig.orderedModules.chunked(2)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rows.forEachIndexed { rowIdx, pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (pair.any { it == draggingModule }) 10f else 0f),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEachIndexed { colIdx, moduleId ->
                                val globalIdx = rowIdx * 2 + colIdx
                                val isHidden = moduleId in layoutConfig.hiddenModules
                                val isDragging = draggingModule == moduleId
                                val tileScale = rememberSpringFloatAnimatable(
                                    targetValue = if (isDragging) 1.05f else 1f,
                                    springSpec = TarazinoMotionSpecs.pressScaleSpring()
                                ).value
                                val tileElevation = rememberSpringFloatAnimatable(
                                    targetValue = if (isDragging) 16f else if (ext.isDark) 0f else 3f,
                                    springSpec = TarazinoMotionSpecs.fluidBouncySpring()
                                ).value.dp
                                val moduleAccent = Color(moduleId.accentColorHex)
                                val borderColor = rememberSpringColorAnimatable(
                                    targetColor = when {
                                        isDragging -> MaterialTheme.colorScheme.primary
                                        isHidden -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        globalIdx < 3 -> moduleAccent.copy(alpha = 0.65f)
                                        else -> MaterialTheme.colorScheme.outlineVariant
                                    }
                                ).value

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isHidden) {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    border = BorderStroke(
                                        width = if (isDragging) 2.dp else 1.dp,
                                        color = borderColor
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .zIndex(if (isDragging) 20f else 0f)
                                        .onGloballyPositioned { coords ->
                                            if (!isDragging) {
                                                tileWindowBounds[moduleId] = coords.boundsInWindow()
                                            }
                                        }
                                        .graphicsLayer {
                                            scaleX = tileScale
                                            scaleY = tileScale
                                            translationX = if (isDragging) dragOffsetX else 0f
                                            translationY = if (isDragging) dragOffsetY else 0f
                                        }
                                        .shadow(
                                            elevation = tileElevation,
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .alpha(if (isHidden && !isDragging) 0.58f else 1f)
                                        .pointerInput(moduleId) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    draggingModule = moduleId
                                                    dragOffsetX = 0f
                                                    dragOffsetY = 0f
                                                    dragOriginBounds = tileWindowBounds[moduleId]
                                                },
                                                onDragEnd = {
                                                    draggingModule = null
                                                    dragOffsetX = 0f
                                                    dragOffsetY = 0f
                                                    dragOriginBounds = null
                                                },
                                                onDragCancel = {
                                                    draggingModule = null
                                                    dragOffsetX = 0f
                                                    dragOffsetY = 0f
                                                    dragOriginBounds = null
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffsetX += dragAmount.x
                                                    dragOffsetY += dragAmount.y
                                                    handleLiveSwapIfCrossed(moduleId)
                                                }
                                            )
                                        }
                                        .testTag("grid_tile_${moduleId.name}")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        moduleAccent.copy(
                                                            alpha = if (isHidden) 0.03f else if (ext.isDark) 0.14f else 0.08f
                                                        ),
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                            .padding(11.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Top Row: Priority Rank Badge + Immediate Drag Handle
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (globalIdx < 3 && !isHidden) {
                                                    moduleAccent.copy(alpha = 0.20f)
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant
                                                }
                                            ) {
                                                Text(
                                                    text = "#${PersianCurrencyEngine.toPersianDigits((globalIdx + 1).toString())}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (globalIdx < 3 && !isHidden) {
                                                        moduleAccent
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }

                                            // Direct Drag Handle (No long-press delay needed on the handle!)
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                    .pointerInput(moduleId) {
                                                        detectDragGestures(
                                                            onDragStart = {
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                draggingModule = moduleId
                                                                dragOffsetX = 0f
                                                                dragOffsetY = 0f
                                                                dragOriginBounds = tileWindowBounds[moduleId]
                                                            },
                                                            onDragEnd = {
                                                                draggingModule = null
                                                                dragOffsetX = 0f
                                                                dragOffsetY = 0f
                                                                dragOriginBounds = null
                                                            },
                                                            onDragCancel = {
                                                                draggingModule = null
                                                                dragOffsetX = 0f
                                                                dragOffsetY = 0f
                                                                dragOriginBounds = null
                                                            },
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()
                                                                dragOffsetX += dragAmount.x
                                                                dragOffsetY += dragAmount.y
                                                                handleLiveSwapIfCrossed(moduleId)
                                                            }
                                                        )
                                                    }
                                                    .testTag("grid_drag_handle_${moduleId.name}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DragIndicator,
                                                    contentDescription = "کشیدن برای تغییر ترتیب ${moduleId.shortTitleFa}",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        // Module Icon + Title
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(moduleAccent.copy(alpha = 0.16f))
                                            ) {
                                                Icon(
                                                    imageVector = dashboardModuleIcon(moduleId),
                                                    contentDescription = null,
                                                    tint = moduleAccent,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = moduleId.shortTitleFa,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = moduleId.categoryBadgeFa,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.sp,
                                                    color = moduleAccent,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        // Tile Quick Actions: Pin to #1 + Visibility Toggle
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(9.dp),
                                                color = if (globalIdx == 0 && !isHidden) {
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                                },
                                                modifier = Modifier
                                                    .height(32.dp)
                                                    .clip(RoundedCornerShape(9.dp))
                                                    .clickable(enabled = globalIdx > 0 || isHidden) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        onPinModuleToTop(moduleId)
                                                    }
                                                    .testTag("grid_pin_top_${moduleId.name}")
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                    modifier = Modifier.padding(horizontal = 8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.VerticalAlignTop,
                                                        contentDescription = "سنجاق به بالای داشبورد",
                                                        tint = if (globalIdx == 0 && !isHidden) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        },
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = if (globalIdx == 0 && !isHidden) "نفر اول" else "بالا",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (globalIdx == 0 && !isHidden) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        }
                                                    )
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(9.dp),
                                                color = if (isHidden) {
                                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                                } else {
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                },
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(9.dp))
                                                    .clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        onToggleVisibility(moduleId)
                                                    }
                                                    .testTag("grid_toggle_vis_${moduleId.name}")
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = if (isHidden) {
                                                            Icons.Default.VisibilityOff
                                                        } else {
                                                            Icons.Default.Visibility
                                                        },
                                                        contentDescription = if (isHidden) "نمایش ماژول" else "مخفی کردن ماژول",
                                                        tint = if (isHidden) {
                                                            MaterialTheme.colorScheme.error
                                                        } else {
                                                            MaterialTheme.colorScheme.primary
                                                        },
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Sticky Bottom Action Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onResetDefault()
                            },
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("dashboard_grid_reset_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "پیش‌فرض",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("dashboard_grid_done_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "اعمال و نمایش داشبورد",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}
