package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.CurrencyUnit
import com.example.core.PersianCurrencyEngine
import com.example.data.CounterpartyEntity
import com.example.data.InstallmentEntity
import com.example.data.ObligationEntity
import com.example.domain.DebtCategoryTag
import com.example.domain.FinancialHealthRadarEngine
import com.example.domain.FinancialHealthRadarReport
import com.example.domain.RadarCategoryAxisMetric
import com.example.domain.RadarLayerMode
import com.example.ui.theme.LocalTarazinoColors

/**
 * Interactive D3-based 'Financial Health Radar' chart card.
 *
 * Visualizes:
 * 1. Distribution of debt categories ('Bank Loan', 'Credit Card / BNPL', 'Personal', 'Check & Housing')
 *    as a normalized D3 polar radar polygon (Deep Persian Teal `#35B8AB`).
 * 2. Relative impact of each category's monthly installment burden on overall monthly budget capacity
 *    as a second overlaid D3 polar radar polygon (Soft Champagne Gold `#DAB978`).
 * 3. Remaining safe monthly budget capacity buffer (`ظرفیت آزاد بودجه`) on the 5th radar vertex.
 * 4. Polar tap hit-testing on the radar vertices/axes + interactive dual-bar category breakdown rows.
 */
@Composable
fun FinancialHealthRadarChartCard(
    obligations: List<ObligationEntity>,
    installments: List<InstallmentEntity>,
    counterparties: List<CounterpartyEntity>,
    monthlyIncomeToman: Long,
    livingExpensePercent: Int,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    recurringNonLoanExpenseToman: Long = 0L,
    categoryOverrides: Map<Long, DebtCategoryTag> = emptyMap(),
    onSelectCategoryTag: ((DebtCategoryTag) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val ext = LocalTarazinoColors.current
    val debtDistributionColor = ext.positive // Soothing Deep Persian Teal (#35B8AB / #0F6B66)
    val budgetImpactColor = MaterialTheme.colorScheme.secondary // Soft Champagne Gold (#DAB978 / #B58538)

    val radarReport: FinancialHealthRadarReport = remember(
        obligations,
        installments,
        counterparties,
        monthlyIncomeToman,
        livingExpensePercent,
        recurringNonLoanExpenseToman,
        categoryOverrides
    ) {
        FinancialHealthRadarEngine.evaluateFinancialHealthRadar(
            obligations = obligations,
            installments = installments,
            counterparties = counterparties,
            monthlyIncomeToman = monthlyIncomeToman,
            livingExpensePercent = livingExpensePercent,
            recurringNonLoanExpenseToman = recurringNonLoanExpenseToman,
            categoryOverrides = categoryOverrides
        )
    }

    var selectedLayerMode by remember { mutableStateOf(RadarLayerMode.DUAL_OVERLAY) }
    var focusedAxisIndex by remember { mutableStateOf<Int?>(null) }
    var entranceTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        entranceTriggered = true
    }

    val radarAnimProgress by animateFloatAsState(
        targetValue = if (entranceTriggered) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.80f,
            stiffness = Spring.StiffnessLow
        ),
        label = "FinancialHealthRadarSweep"
    )

    val focusedAxis: RadarCategoryAxisMetric? = remember(focusedAxisIndex, radarReport) {
        focusedAxisIndex?.let { idx -> radarReport.axes.getOrNull(idx) }
    }

    TarazinoGlassCard(
        cornerRadius = 26.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("financial_health_radar_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header with Composite Radar Health Score Badge
            TarazinoSectionHeader(
                title = "رادار سلامت مالی و توازن دسته‌های بدهی",
                subtitle = "توزیع دسته‌ها (وام بانکی، شخصی، اعتباری) و اثر بر ظرفیت بودجه ${radarReport.shamsiMonthNameFa}",
                icon = Icons.Default.Radar,
                accentColor = debtDistributionColor,
                trailing = {
                    val scoreColor = when {
                        radarReport.radarHealthScore >= 75 -> debtDistributionColor
                        radarReport.radarHealthScore >= 52 -> budgetImpactColor
                        else -> ext.danger
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = scoreColor.copy(alpha = if (ext.isDark) 0.16f else 0.12f),
                        border = BorderStroke(0.9.dp, scoreColor.copy(alpha = 0.45f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrackChanges,
                                contentDescription = null,
                                tint = scoreColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "امتیاز ${PersianCurrencyEngine.toPersianDigits(radarReport.radarHealthScore.toString())}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = scoreColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            )

            // 2. Interactive D3 Layer Filter Chips (Dual Overlay / Debt Distribution / Budget Capacity Impact)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RadarLayerMode.entries.forEach { mode ->
                    val isSelected = selectedLayerMode == mode
                    val chipAccent = when (mode) {
                        RadarLayerMode.DUAL_OVERLAY -> MaterialTheme.colorScheme.primary
                        RadarLayerMode.DEBT_DISTRIBUTION -> debtDistributionColor
                        RadarLayerMode.BUDGET_IMPACT -> budgetImpactColor
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            chipAccent.copy(alpha = if (ext.isDark) 0.22f else 0.14f)
                        } else {
                            ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.55f else 0.75f)
                        },
                        border = BorderStroke(
                            width = if (isSelected) 1.3.dp else 0.8.dp,
                            color = if (isSelected) chipAccent else ext.cardBorderTop
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedLayerMode = mode }
                            .testTag("radar_layer_chip_${mode.name}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(chipAccent)
                            )
                            Text(
                                text = mode.labelFa,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) chipAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 3. D3 Polar Radar Canvas with Concentric Web Rings, Axis Spokes & Dual Polygons
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(238.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                debtDistributionColor.copy(alpha = if (ext.isDark) 0.11f else 0.07f),
                                ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.55f else 0.72f)
                            )
                        )
                    )
                    .border(
                        width = 0.85.dp,
                        color = ext.cardBorderTop,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .testTag("financial_health_radar_canvas")
                    .pointerInput(radarReport) {
                        detectTapGestures { tapOffset ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val maxRadius = (minOf(size.width, size.height) / 2f) * 0.68f
                            val tappedIdx = FinancialHealthRadarEngine.findNearestTappedAxisIndex(
                                axes = radarReport.axes,
                                tapX = tapOffset.x,
                                tapY = tapOffset.y,
                                centerX = cx,
                                centerY = cy,
                                maxRadiusPx = maxRadius
                            )
                            focusedAxisIndex = if (tappedIdx != null && focusedAxisIndex == tappedIdx) {
                                null
                            } else {
                                tappedIdx
                            }
                            if (tappedIdx != null) {
                                radarReport.axes.getOrNull(tappedIdx)?.categoryTag?.let { tag ->
                                    onSelectCategoryTag?.invoke(tag)
                                }
                            }
                        }
                    }
            ) {
                val gridLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (ext.isDark) 0.15f else 0.12f)
                val spokeColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (ext.isDark) 0.24f else 0.20f)
                val safeZoneThresholdColor = debtDistributionColor.copy(alpha = 0.42f)
                val centerOriginColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f + 4.dp.toPx()
                    val maxRadius = (size.minDimension / 2f) * 0.64f
                    val axes = radarReport.axes
                    if (axes.isEmpty()) return@Canvas

                    // 3.1 Draw 4 Concentric D3 Pentagonal Radar Levels (25%, 50%, 75%, 100%)
                    val gridLevels = listOf(0.25f, 0.50f, 0.75f, 1.00f)
                    gridLevels.forEach { levelRatio ->
                        val gridPath = Path()
                        axes.forEachIndexed { idx, axis ->
                            val (vx, vy) = FinancialHealthRadarEngine.computePolarVertex(
                                angleRadians = axis.angleRadians,
                                normalizedRadius = levelRatio,
                                maxRadiusPx = maxRadius,
                                centerX = cx,
                                centerY = cy
                            )
                            if (idx == 0) {
                                gridPath.moveTo(vx, vy)
                            } else {
                                gridPath.lineTo(vx, vy)
                            }
                        }
                        gridPath.close()

                        val isSafeThresholdRing = levelRatio == 0.50f
                        drawPath(
                            path = gridPath,
                            color = if (isSafeThresholdRing) safeZoneThresholdColor else gridLineColor,
                            style = Stroke(
                                width = if (isSafeThresholdRing) 1.3.dp.toPx() else 0.9.dp.toPx(),
                                pathEffect = if (isSafeThresholdRing) {
                                    PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                                } else {
                                    null
                                }
                            )
                        )
                    }

                    // 3.2 Draw 5 Radial Axis Spokes from Center to Outer Vertices
                    axes.forEachIndexed { idx, axis ->
                        val isFocused = focusedAxisIndex == idx
                        val (outerX, outerY) = FinancialHealthRadarEngine.computePolarVertex(
                            angleRadians = axis.angleRadians,
                            normalizedRadius = 1.0f,
                            maxRadiusPx = maxRadius,
                            centerX = cx,
                            centerY = cy
                        )
                        drawLine(
                            color = if (isFocused) Color(axis.colorHex) else spokeColor,
                            start = Offset(cx, cy),
                            end = Offset(outerX, outerY),
                            strokeWidth = if (isFocused) 1.9.dp.toPx() else 1.0.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Outer axis anchor ring on the 100% perimeter
                        drawCircle(
                            color = if (isFocused) Color(axis.colorHex) else spokeColor,
                            radius = if (isFocused) 4.2.dp.toPx() else 2.4.dp.toPx(),
                            center = Offset(outerX, outerY)
                        )
                    }

                    // 3.3 Draw Series A: Debt Category Distribution Polygon (Deep Persian Teal)
                    val showDebtLayer = selectedLayerMode == RadarLayerMode.DUAL_OVERLAY ||
                        selectedLayerMode == RadarLayerMode.DEBT_DISTRIBUTION
                    if (showDebtLayer) {
                        val debtPath = Path()
                        val debtPoints = axes.map { axis ->
                            val r = (axis.debtDistributionNormalizedRadius * radarAnimProgress).coerceIn(0.06f, 1f)
                            FinancialHealthRadarEngine.computePolarVertex(
                                angleRadians = axis.angleRadians,
                                normalizedRadius = r,
                                maxRadiusPx = maxRadius,
                                centerX = cx,
                                centerY = cy
                            )
                        }
                        debtPoints.forEachIndexed { idx, (px, py) ->
                            if (idx == 0) debtPath.moveTo(px, py) else debtPath.lineTo(px, py)
                        }
                        debtPath.close()

                        drawPath(
                            path = debtPath,
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    debtDistributionColor.copy(alpha = 0.36f),
                                    debtDistributionColor.copy(alpha = 0.16f)
                                ),
                                center = Offset(cx, cy),
                                radius = maxRadius
                            ),
                            style = Fill
                        )
                        drawPath(
                            path = debtPath,
                            color = debtDistributionColor,
                            style = Stroke(
                                width = 2.4.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // Draw vertices for Debt Distribution polygon
                        debtPoints.forEachIndexed { idx, (px, py) ->
                            val isFocused = focusedAxisIndex == idx
                            if (isFocused) {
                                drawCircle(
                                    color = debtDistributionColor.copy(alpha = 0.28f),
                                    radius = 8.dp.toPx(),
                                    center = Offset(px, py)
                                )
                            }
                            drawCircle(
                                color = debtDistributionColor,
                                radius = if (isFocused) 4.8.dp.toPx() else 3.4.dp.toPx(),
                                center = Offset(px, py)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 1.5.dp.toPx(),
                                center = Offset(px, py)
                            )
                        }
                    }

                    // 3.4 Draw Series B: Monthly Budget Capacity Impact Polygon (Soft Champagne Gold)
                    val showBudgetLayer = selectedLayerMode == RadarLayerMode.DUAL_OVERLAY ||
                        selectedLayerMode == RadarLayerMode.BUDGET_IMPACT
                    if (showBudgetLayer) {
                        val budgetPath = Path()
                        val budgetPoints = axes.map { axis ->
                            val r = (axis.budgetImpactNormalizedRadius * radarAnimProgress).coerceIn(0.06f, 1f)
                            FinancialHealthRadarEngine.computePolarVertex(
                                angleRadians = axis.angleRadians,
                                normalizedRadius = r,
                                maxRadiusPx = maxRadius,
                                centerX = cx,
                                centerY = cy
                            )
                        }
                        budgetPoints.forEachIndexed { idx, (px, py) ->
                            if (idx == 0) budgetPath.moveTo(px, py) else budgetPath.lineTo(px, py)
                        }
                        budgetPath.close()

                        drawPath(
                            path = budgetPath,
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    budgetImpactColor.copy(alpha = 0.34f),
                                    budgetImpactColor.copy(alpha = 0.14f)
                                ),
                                center = Offset(cx, cy),
                                radius = maxRadius
                            ),
                            style = Fill
                        )
                        drawPath(
                            path = budgetPath,
                            color = budgetImpactColor,
                            style = Stroke(
                                width = 2.2.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // Draw vertices for Monthly Budget Capacity Impact polygon
                        budgetPoints.forEachIndexed { idx, (px, py) ->
                            val isFocused = focusedAxisIndex == idx
                            if (isFocused) {
                                drawCircle(
                                    color = budgetImpactColor.copy(alpha = 0.28f),
                                    radius = 8.dp.toPx(),
                                    center = Offset(px, py)
                                )
                            }
                            drawCircle(
                                color = budgetImpactColor,
                                radius = if (isFocused) 4.6.dp.toPx() else 3.3.dp.toPx(),
                                center = Offset(px, py)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 1.4.dp.toPx(),
                                center = Offset(px, py)
                            )
                        }
                    }

                    // Center origin pivot jewel
                    drawCircle(
                        color = centerOriginColor,
                        radius = 2.2.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }

                // 3.5 Perimeter Axis Interactive Pills Positioned at Pentagonal Cardinals
                RadarPerimeterLabelsOverlay(
                    axes = radarReport.axes,
                    focusedAxisIndex = focusedAxisIndex,
                    debtColor = debtDistributionColor,
                    budgetColor = budgetImpactColor,
                    onClickAxis = { idx ->
                        focusedAxisIndex = if (focusedAxisIndex == idx) null else idx
                        radarReport.axes.getOrNull(idx)?.categoryTag?.let { tag ->
                            onSelectCategoryTag?.invoke(tag)
                        }
                    }
                )
            }

            // 4. Dual-Polygon Color Legend & Monthly Budget Capacity Utilization Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.65f else 0.85f))
                    .border(0.8.dp, ext.cardBorderTop, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(debtDistributionColor)
                    )
                    Text(
                        text = "سهم از کل بدهی",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(budgetImpactColor)
                    )
                    Text(
                        text = "فشار بر ظرفیت بودجه ماهانه",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${PersianCurrencyEngine.toPersianDigits(radarReport.overallBudgetCapacityUtilizationPercent.toString())}٪ مصرف ظرفیت",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (radarReport.overallBudgetCapacityUtilizationPercent > 80) ext.danger else debtDistributionColor
                )
            }

            // 5. Focused Axis Spotlight Card OR Executive Radar Summary Banner
            AnimatedContent(
                targetState = focusedAxis,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "FocusedRadarAxisSpotlight"
            ) { selectedAxis ->
                if (selectedAxis != null) {
                    FocusedRadarAxisDetailBanner(
                        axis = selectedAxis,
                        totalBudgetCapacityToman = radarReport.totalMonthlyDebtBudgetCapacityToman,
                        debtColor = debtDistributionColor,
                        budgetColor = budgetImpactColor,
                        currencyUnit = currencyUnit,
                        privacyMasked = privacyMasked,
                        onClearFocus = { focusedAxisIndex = null }
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = debtDistributionColor.copy(alpha = if (ext.isDark) 0.12f else 0.08f),
                        border = BorderStroke(0.85.dp, debtDistributionColor.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "تحلیل هوشمند رادار سلامت مالی",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = debtDistributionColor
                                )
                                Text(
                                    text = "ظرفیت بودجه اقساط: ${
                                        PersianCurrencyEngine.formatAmount(
                                            radarReport.totalMonthlyDebtBudgetCapacityToman,
                                            currencyUnit,
                                            privacyMasked = privacyMasked
                                        )
                                    }",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = radarReport.executiveInsightFa,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // 6. Interactive Dual-Metric Category Comparison Matrix (Debt Distribution vs. Budget Capacity Impact)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "مقایسه توزیع دسته‌ها و اثر نسبی بر ظرفیت بودجه ماهانه (لمس برای تمرکز روی رادار)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                radarReport.axes.forEachIndexed { idx, axis ->
                    RadarAxisDualComparisonRow(
                        axis = axis,
                        isFocused = focusedAxisIndex == idx,
                        debtColor = debtDistributionColor,
                        budgetColor = budgetImpactColor,
                        animProgress = radarAnimProgress,
                        currencyUnit = currencyUnit,
                        privacyMasked = privacyMasked,
                        onClick = {
                            focusedAxisIndex = if (focusedAxisIndex == idx) null else idx
                            axis.categoryTag?.let { tag ->
                                onSelectCategoryTag?.invoke(tag)
                            }
                        },
                        modifier = Modifier.testTag("radar_axis_row_${axis.axisId}")
                    )
                }
            }
        }
    }
}

@Composable
private fun RadarPerimeterLabelsOverlay(
    axes: List<RadarCategoryAxisMetric>,
    focusedAxisIndex: Int?,
    debtColor: Color,
    budgetColor: Color,
    onClickAxis: (Int) -> Unit
) {
    if (axes.size < 5) return
    // 5 pentagonal cardinal slots:
    // Index 0 (-90 deg): Top Center (Bank Loan)
    // Index 1 (-18 deg): Top-End / Mid-Right (Credit Card / BNPL)
    // Index 2 (+54 deg): Bottom-End (Personal)
    // Index 3 (+126 deg): Bottom-Start (Check & Housing)
    // Index 4 (-162 deg): Top-Start / Mid-Left (Free Budget Capacity Buffer)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Index 0: Top Center
        RadarVertexLabelPill(
            axis = axes[0],
            isFocused = focusedAxisIndex == 0,
            debtColor = debtColor,
            budgetColor = budgetColor,
            onClick = { onClickAxis(0) },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Index 1: CenterEnd (shifted slightly up)
        RadarVertexLabelPill(
            axis = axes[1],
            isFocused = focusedAxisIndex == 1,
            debtColor = debtColor,
            budgetColor = budgetColor,
            onClick = { onClickAxis(1) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(bottom = 36.dp)
        )

        // Index 4: CenterStart (shifted slightly up)
        RadarVertexLabelPill(
            axis = axes[4],
            isFocused = focusedAxisIndex == 4,
            debtColor = debtColor,
            budgetColor = budgetColor,
            onClick = { onClickAxis(4) },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(bottom = 36.dp)
        )

        // Index 2: BottomEnd
        RadarVertexLabelPill(
            axis = axes[2],
            isFocused = focusedAxisIndex == 2,
            debtColor = debtColor,
            budgetColor = budgetColor,
            onClick = { onClickAxis(2) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(horizontal = 10.dp)
        )

        // Index 3: BottomStart
        RadarVertexLabelPill(
            axis = axes[3],
            isFocused = focusedAxisIndex == 3,
            debtColor = debtColor,
            budgetColor = budgetColor,
            onClick = { onClickAxis(3) },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 10.dp)
        )
    }
}

@Composable
private fun RadarVertexLabelPill(
    axis: RadarCategoryAxisMetric,
    isFocused: Boolean,
    debtColor: Color,
    budgetColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTarazinoColors.current
    val accent = Color(axis.colorHex)
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isFocused) {
            accent.copy(alpha = if (ext.isDark) 0.25f else 0.16f)
        } else {
            ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.82f else 0.92f)
        },
        border = BorderStroke(
            width = if (isFocused) 1.3.dp else 0.75.dp,
            color = if (isFocused) accent else ext.cardBorderTop
        ),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("radar_vertex_pill_${axis.axisId}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp)
        ) {
            Text(
                text = axis.shortLabelFa,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isFocused) FontWeight.ExtraBold else FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 10.sp,
                maxLines = 1
            )
            Text(
                text = "${PersianCurrencyEngine.toPersianDigits(axis.debtDistributionPercent.toString())}٪",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = debtColor,
                fontSize = 9.5.sp,
                maxLines = 1
            )
            Text(
                text = "|",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                fontSize = 9.sp
            )
            Text(
                text = "${PersianCurrencyEngine.toPersianDigits(axis.budgetCapacityImpactPercent.toString())}٪",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = budgetColor,
                fontSize = 9.5.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FocusedRadarAxisDetailBanner(
    axis: RadarCategoryAxisMetric,
    totalBudgetCapacityToman: Long,
    debtColor: Color,
    budgetColor: Color,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    onClearFocus: () -> Unit
) {
    val ext = LocalTarazinoColors.current
    val accent = Color(axis.colorHex)
    val isBufferAxis = axis.categoryTag == null

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = if (ext.isDark) 0.16f else 0.11f),
        border = BorderStroke(1.2.dp, accent.copy(alpha = 0.65f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClearFocus)
            .testTag("radar_focused_axis_banner")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = iconForRadarAxis(axis.categoryTag),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = axis.labelFa,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accent.copy(alpha = 0.20f)
                ) {
                    Text(
                        text = axis.pressureStatusFa,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBufferAxis) "میزان پیشرفت کل تسویه" else "مانده بدهی این دسته (${PersianCurrencyEngine.toPersianDigits(axis.debtDistributionPercent.toString())}٪ کل)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FormattedMoneyText(
                        amountToman = axis.remainingDebtToman,
                        unit = currencyUnit,
                        privacyMasked = privacyMasked,
                        style = MaterialTheme.typography.titleSmall,
                        numberColor = MaterialTheme.colorScheme.onSurface,
                        unitColor = debtColor,
                        minFontSize = 9.5.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBufferAxis) {
                            "ظرفیت آزاد (${PersianCurrencyEngine.toPersianDigits(axis.budgetCapacityImpactPercent.toString())}٪ از سقف بودجه)"
                        } else {
                            "قسط این ماه (${PersianCurrencyEngine.toPersianDigits(axis.budgetCapacityImpactPercent.toString())}٪ ظرفیت بودجه)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FormattedMoneyText(
                        amountToman = axis.monthlyInstallmentDueToman,
                        unit = currencyUnit,
                        privacyMasked = privacyMasked,
                        style = MaterialTheme.typography.titleSmall,
                        numberColor = MaterialTheme.colorScheme.onSurface,
                        unitColor = budgetColor,
                        minFontSize = 9.5.sp
                    )
                }
            }

            Text(
                text = "طرف‌های حساب: ${axis.sampleCounterpartiesFa} • از کل ظرفیت بودجه ماهانه (${
                    PersianCurrencyEngine.formatAmount(
                        totalBudgetCapacityToman,
                        currencyUnit,
                        privacyMasked = privacyMasked
                    )
                })",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RadarAxisDualComparisonRow(
    axis: RadarCategoryAxisMetric,
    isFocused: Boolean,
    debtColor: Color,
    budgetColor: Color,
    animProgress: Float,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTarazinoColors.current
    val accent = Color(axis.colorHex)
    val isBufferAxis = axis.categoryTag == null

    val rowBg by animateColorAsState(
        targetValue = if (isFocused) {
            accent.copy(alpha = if (ext.isDark) 0.16f else 0.10f)
        } else {
            ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.68f else 0.86f)
        },
        label = "RadarRowBg_${axis.axisId}"
    )

    val rowBorder by animateColorAsState(
        targetValue = if (isFocused) accent.copy(alpha = 0.82f) else ext.cardBorderTop,
        label = "RadarRowBorder_${axis.axisId}"
    )

    Surface(
        shape = RoundedCornerShape(15.dp),
        color = rowBg,
        border = BorderStroke(if (isFocused) 1.35.dp else 0.8.dp, rowBorder),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accent.copy(alpha = 0.16f))
                    ) {
                        Icon(
                            imageVector = iconForRadarAxis(axis.categoryTag),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = axis.labelFa,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isBufferAxis) {
                                axis.pressureStatusFa
                            } else {
                                "${PersianCurrencyEngine.toPersianDigits(axis.obligationsCount.toString())} پرونده • ${axis.pressureStatusFa}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }

                // Dual percentage pills (Debt Share % vs. Monthly Budget Impact %)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = debtColor.copy(alpha = if (ext.isDark) 0.18f else 0.12f)
                    ) {
                        Text(
                            text = if (isBufferAxis) {
                                "${PersianCurrencyEngine.toPersianDigits(axis.debtDistributionPercent.toString())}٪ تسویه"
                            } else {
                                "${PersianCurrencyEngine.toPersianDigits(axis.debtDistributionPercent.toString())}٪ بدهی"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = debtColor,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = budgetColor.copy(alpha = if (ext.isDark) 0.20f else 0.14f)
                    ) {
                        Text(
                            text = if (isBufferAxis) {
                                "${PersianCurrencyEngine.toPersianDigits(axis.budgetCapacityImpactPercent.toString())}٪ آزاد"
                            } else {
                                "${PersianCurrencyEngine.toPersianDigits(axis.budgetCapacityImpactPercent.toString())}٪ بودجه"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = budgetColor,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Dual D3 Comparative Horizontal Scale Bars
            Column(verticalArrangement = Arrangement.spacedBy(3.5.dp)) {
                // Bar 1: Share of Total Remaining Debt (Deep Persian Teal)
                val debtRatio = ((axis.debtDistributionPercent / 100f) * animProgress).coerceIn(0.03f, 1f)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(debtRatio)
                                .clip(RoundedCornerShape(3.dp))
                                .background(debtColor)
                        )
                    }
                    Text(
                        text = PersianCurrencyEngine.formatAmount(
                            axis.remainingDebtToman,
                            currencyUnit,
                            privacyMasked = privacyMasked
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.5.sp,
                        maxLines = 1
                    )
                }

                // Bar 2: Relative Impact on Monthly Budget Capacity (Soft Champagne Gold)
                val budgetRatio = ((axis.budgetCapacityImpactPercent / 100f) * animProgress).coerceIn(0.03f, 1f)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(budgetRatio)
                                .clip(RoundedCornerShape(3.dp))
                                .background(budgetColor)
                        )
                    }
                    Text(
                        text = if (isBufferAxis) {
                            "سقف آزاد ماهانه"
                        } else {
                            "قسط ماه: ${
                                PersianCurrencyEngine.formatAmount(
                                    axis.monthlyInstallmentDueToman,
                                    currencyUnit,
                                    privacyMasked = privacyMasked
                                )
                            }"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = budgetColor,
                        fontSize = 9.5.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun iconForRadarAxis(tag: DebtCategoryTag?): ImageVector {
    return when (tag) {
        DebtCategoryTag.BANK_LOAN -> Icons.Default.AccountBalance
        DebtCategoryTag.CREDIT_CARD -> Icons.Default.CreditCard
        DebtCategoryTag.PERSONAL -> Icons.Default.Groups
        DebtCategoryTag.CHECK_AND_HOUSING -> Icons.Default.HomeWork
        else -> Icons.Default.Savings
    }
}
