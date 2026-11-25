package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.CurrencyUnit
import com.example.core.PersianCurrencyEngine
import com.example.ui.theme.LocalTarazinoColors
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

enum class DebtRingFocusSegment {
    NONE,
    PAID,
    OUTSTANDING
}

data class DebtProgressRingMetrics(
    val totalDebtToman: Long,
    val paidDebtToman: Long,
    val outstandingDebtToman: Long,
    val paidRatio: Float,
    val outstandingRatio: Float,
    val paidPercent: Int,
    val outstandingPercent: Int,
    val paidSweepDegrees: Float,
    val outstandingSweepDegrees: Float
)

/**
 * Pure domain calculation helper for the Paid vs. Outstanding Debt Progress Ring Chart.
 * Computes exact normalized ratios, percentages (summing to 100% when totalDebtToman > 0),
 * and arc sweep angles in degrees (D3 arc generator semantics with padAngle).
 */
fun computeDebtProgressRingMetrics(
    paidDebtToman: Long,
    outstandingDebtToman: Long,
    padAngleDegrees: Float = 8f
): DebtProgressRingMetrics {
    val safePaid = paidDebtToman.coerceAtLeast(0L)
    val safeOutstanding = outstandingDebtToman.coerceAtLeast(0L)
    val total = safePaid + safeOutstanding

    if (total <= 0L) {
        return DebtProgressRingMetrics(
            totalDebtToman = 0L,
            paidDebtToman = 0L,
            outstandingDebtToman = 0L,
            paidRatio = 1f,
            outstandingRatio = 0f,
            paidPercent = 100,
            outstandingPercent = 0,
            paidSweepDegrees = 360f,
            outstandingSweepDegrees = 0f
        )
    }

    val paidRatio = (safePaid.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
    val outstandingRatio = (1f - paidRatio).coerceIn(0f, 1f)
    val paidPercent = (paidRatio * 100f).roundToInt().coerceIn(0, 100)
    val outstandingPercent = (100 - paidPercent).coerceIn(0, 100)

    val hasBothSegments = safePaid > 0L && safeOutstanding > 0L
    val totalPad = if (hasBothSegments) padAngleDegrees * 2f else 0f
    val availableDegrees = (360f - totalPad).coerceAtLeast(10f)

    val paidSweep = if (safePaid == 0L) {
        0f
    } else if (safeOutstanding == 0L) {
        360f
    } else {
        (paidRatio * availableDegrees).coerceIn(8f, availableDegrees - 8f)
    }

    val outstandingSweep = if (safeOutstanding == 0L) {
        0f
    } else if (safePaid == 0L) {
        360f
    } else {
        (availableDegrees - paidSweep).coerceAtLeast(8f)
    }

    return DebtProgressRingMetrics(
        totalDebtToman = total,
        paidDebtToman = safePaid,
        outstandingDebtToman = safeOutstanding,
        paidRatio = paidRatio,
        outstandingRatio = outstandingRatio,
        paidPercent = paidPercent,
        outstandingPercent = outstandingPercent,
        paidSweepDegrees = paidSweep,
        outstandingSweepDegrees = outstandingSweep
    )
}

/**
 * Interactive D3-style radial progress ring & dual-arc gauge chart built natively with Jetpack Compose Canvas.
 * Visualizes the exact ratio of Paid (تسویه‌شده) vs. Outstanding (مانده بدهی) debt amounts with:
 * - Calibrated radial scale tick marks (0%..100%)
 * - Concentric Paid (Soothing Deep Persian Teal) and Outstanding (Soft Champagne Gold) arcs
 * - Touch/tap polar-coordinate hit-testing on the ring or legend rows to inspect either segment
 * - Dynamic auto-sizing typography for all Persian numbers and labels
 */
@Composable
fun DebtPaidVsOutstandingRingCard(
    paidDebtToman: Long,
    outstandingDebtToman: Long,
    paidInstallmentsCount: Int,
    outstandingInstallmentsCount: Int,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    modifier: Modifier = Modifier
) {
    val ext = LocalTarazinoColors.current
    val paidColor = ext.positive // Soothing Deep Persian Teal (#35B8AB / #0F6B66)
    val outstandingColor = MaterialTheme.colorScheme.secondary // Soft Champagne Gold (#DAB978 / #B58538)

    val metrics = remember(paidDebtToman, outstandingDebtToman) {
        computeDebtProgressRingMetrics(
            paidDebtToman = paidDebtToman,
            outstandingDebtToman = outstandingDebtToman,
            padAngleDegrees = 10f
        )
    }

    var focusedSegment by remember { mutableStateOf(DebtRingFocusSegment.NONE) }
    var entranceTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        entranceTriggered = true
    }

    val animProgressState = rememberSpringFloatAnimatable(
        targetValue = if (entranceTriggered) 1f else 0f,
        initialValue = 0f,
        springSpec = TarazinoMotionSpecs.chartSweepSpring()
    )
    val animProgress = animProgressState.value

    val paidStrokeDp by animateDpAsState(
        targetValue = if (focusedSegment == DebtRingFocusSegment.PAID) 17.dp else 13.5.dp,
        animationSpec = TarazinoMotionSpecs.dpSpring(),
        label = "PaidStrokeDp"
    )
    val outstandingStrokeDp by animateDpAsState(
        targetValue = if (focusedSegment == DebtRingFocusSegment.OUTSTANDING) 17.dp else 13.5.dp,
        animationSpec = TarazinoMotionSpecs.dpSpring(),
        label = "OutstandingStrokeDp"
    )

    TarazinoGlassCard(
        cornerRadius = 24.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_debt_progress_ring_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TarazinoSectionHeader(
                title = "نمودار حلقه‌ای نسبت تسویه به مانده بدهی",
                subtitle = "لمس کمان‌ها یا کارت‌ها برای تمرکز روی سهم پرداخت‌شده و مانده",
                icon = Icons.Default.DonutLarge,
                accentColor = paidColor,
                trailing = {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = paidColor.copy(alpha = if (ext.isDark) 0.16f else 0.12f),
                        border = BorderStroke(0.8.dp, paidColor.copy(alpha = 0.42f))
                    ) {
                        Text(
                            text = "${PersianCurrencyEngine.toPersianDigits(metrics.paidPercent.toString())}٪ پیشرفت کل",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = paidColor,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // D3-Inspired Radial Progress Ring Canvas with Polar Hit-Testing & Tick Marks
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(134.dp)
                        .testTag("debt_progress_ring_canvas")
                        .pointerInput(metrics) {
                            detectTapGestures { tapOffset ->
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val dx = tapOffset.x - cx
                                val dy = tapOffset.y - cy
                                val dist = hypot(dx, dy)
                                val outerR = size.width / 2f
                                val innerR = outerR * 0.46f
                                if (dist < innerR) {
                                    focusedSegment = DebtRingFocusSegment.NONE
                                } else {
                                    // Convert polar angle so -90 deg (12 o'clock) is 0..360 clockwise
                                    var angleDeg = (atan2(dy, dx) * 180f / PI.toFloat()) + 90f
                                    if (angleDeg < 0f) angleDeg += 360f
                                    focusedSegment = if (angleDeg <= metrics.paidSweepDegrees + 5f) {
                                        if (focusedSegment == DebtRingFocusSegment.PAID) {
                                            DebtRingFocusSegment.NONE
                                        } else {
                                            DebtRingFocusSegment.PAID
                                        }
                                    } else {
                                        if (focusedSegment == DebtRingFocusSegment.OUTSTANDING) {
                                            DebtRingFocusSegment.NONE
                                        } else {
                                            DebtRingFocusSegment.OUTSTANDING
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (ext.isDark) 0.10f else 0.08f)
                    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
                    val innerGuideColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val maxStrokePx = 18.dp.toPx()
                        val paidStrokePx = paidStrokeDp.toPx()
                        val outStrokePx = outstandingStrokeDp.toPx()
                        val ringInset = maxStrokePx / 2f + 6.dp.toPx()
                        val ringDiameter = (size.minDimension - ringInset * 2f).coerceAtLeast(20f)
                        val arcTopLeft = Offset(
                            (size.width - ringDiameter) / 2f,
                            (size.height - ringDiameter) / 2f
                        )
                        val arcSize = Size(ringDiameter, ringDiameter)
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val outerTickRadius = size.minDimension / 2f - 1.5.dp.toPx()

                        // 1. D3 Polar Scale Tick Marks (24 radial ticks around perimeter; major ticks at 0/25/50/75%)
                        val tickCount = 24
                        for (i in 0 until tickCount) {
                            val angleRad = ((i * (360f / tickCount)) - 90f) * (PI / 180.0)
                            val isMajor = i % 6 == 0
                            val tickLen = if (isMajor) 4.2.dp.toPx() else 2.4.dp.toPx()
                            val rStart = outerTickRadius - tickLen
                            val rEnd = outerTickRadius
                            val startPt = Offset(
                                center.x + (cos(angleRad) * rStart).toFloat(),
                                center.y + (sin(angleRad) * rStart).toFloat()
                            )
                            val endPt = Offset(
                                center.x + (cos(angleRad) * rEnd).toFloat(),
                                center.y + (sin(angleRad) * rEnd).toFloat()
                            )
                            drawLine(
                                color = if (isMajor) paidColor.copy(alpha = 0.55f) else tickColor,
                                start = startPt,
                                end = endPt,
                                strokeWidth = if (isMajor) 1.5.dp.toPx() else 0.9.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }

                        // 2. Background Track Ring
                        drawArc(
                            color = trackColor,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(width = 13.dp.toPx())
                        )

                        // 3. Inner Reference Guide Circle
                        val innerRadius = (ringDiameter / 2f) - (maxStrokePx / 2f) - 4.dp.toPx()
                        if (innerRadius > 10f) {
                            drawCircle(
                                color = innerGuideColor,
                                radius = innerRadius,
                                center = center,
                                style = Stroke(width = 0.8.dp.toPx())
                            )
                        }

                        // 4. Outstanding Debt Arc (Soft Champagne Gold)
                        val hasBoth = metrics.paidSweepDegrees > 0f && metrics.outstandingSweepDegrees > 0f
                        val padAngle = if (hasBoth) 10f else 0f
                        val outStartAngle = -90f + metrics.paidSweepDegrees + (padAngle / 2f)
                        val outSweep = metrics.outstandingSweepDegrees * animProgress
                        val outAlpha = if (focusedSegment == DebtRingFocusSegment.PAID) 0.32f else 1f

                        if (outSweep > 0.5f) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        outstandingColor.copy(alpha = outAlpha * 0.85f),
                                        outstandingColor.copy(alpha = outAlpha),
                                        outstandingColor.copy(alpha = outAlpha * 0.90f)
                                    ),
                                    center = center
                                ),
                                startAngle = outStartAngle,
                                sweepAngle = outSweep,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = arcSize,
                                style = Stroke(width = outStrokePx, cap = StrokeCap.Round)
                            )
                        }

                        // 5. Paid Debt Progress Arc (Soothing Deep Persian Teal)
                        val paidStartAngle = -90f + (padAngle / 2f)
                        val paidSweep = metrics.paidSweepDegrees * animProgress
                        val paidAlpha = if (focusedSegment == DebtRingFocusSegment.OUTSTANDING) 0.32f else 1f

                        if (paidSweep > 0.5f) {
                            // Subtle outer luminous halo on active paid progress arc
                            drawArc(
                                color = paidColor.copy(alpha = 0.18f * paidAlpha),
                                startAngle = paidStartAngle,
                                sweepAngle = paidSweep,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = arcSize,
                                style = Stroke(width = paidStrokePx + 4.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = paidColor.copy(alpha = paidAlpha),
                                startAngle = paidStartAngle,
                                sweepAngle = paidSweep,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = arcSize,
                                style = Stroke(width = paidStrokePx, cap = StrokeCap.Round)
                            )

                            // Specular end-cap jewel dot at the tip of the Paid progress arc
                            val tipAngleRad = (paidStartAngle + paidSweep) * (PI / 180.0)
                            val arcRadius = ringDiameter / 2f
                            val tipCenter = Offset(
                                center.x + (cos(tipAngleRad) * arcRadius).toFloat(),
                                center.y + (sin(tipAngleRad) * arcRadius).toFloat()
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.88f * paidAlpha),
                                radius = 2.6.dp.toPx(),
                                center = tipCenter
                            )
                        }
                    }

                    // Center Readout inside the Ring
                    AnimatedContent(
                        targetState = focusedSegment,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "DebtRingCenterReadout"
                    ) { segment ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                        ) {
                            when (segment) {
                                DebtRingFocusSegment.PAID -> {
                                    TarazinoAutoSizeText(
                                        text = "${PersianCurrencyEngine.toPersianDigits(metrics.paidPercent.toString())}٪",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = paidColor,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        minFontSize = 13.sp
                                    )
                                    TarazinoAutoSizeText(
                                        text = "تسویه‌شده",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        minFontSize = 8.5.sp
                                    )
                                }
                                DebtRingFocusSegment.OUTSTANDING -> {
                                    TarazinoAutoSizeText(
                                        text = "${PersianCurrencyEngine.toPersianDigits(metrics.outstandingPercent.toString())}٪",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = outstandingColor,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        minFontSize = 13.sp
                                    )
                                    TarazinoAutoSizeText(
                                        text = "مانده بدهی",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        minFontSize = 8.5.sp
                                    )
                                }
                                DebtRingFocusSegment.NONE -> {
                                    TarazinoAutoSizeText(
                                        text = "${PersianCurrencyEngine.toPersianDigits(metrics.paidPercent.toString())}٪",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = paidColor,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        minFontSize = 13.sp
                                    )
                                    TarazinoAutoSizeText(
                                        text = "نسبت تسویه",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        minFontSize = 8.5.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Right/Left Column: Interactive Paid vs. Outstanding Breakdown Legend Cards
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    DebtSegmentInteractiveRow(
                        titleFa = "مبلغ تسویه‌شده",
                        subtitleFa = "${PersianCurrencyEngine.toPersianDigits(paidInstallmentsCount.toString())} قسط پرداخت‌شده",
                        amountToman = metrics.paidDebtToman,
                        percent = metrics.paidPercent,
                        accentColor = paidColor,
                        icon = Icons.Default.CheckCircle,
                        isFocused = focusedSegment == DebtRingFocusSegment.PAID,
                        currencyUnit = currencyUnit,
                        privacyMasked = privacyMasked,
                        onClick = {
                            focusedSegment = if (focusedSegment == DebtRingFocusSegment.PAID) {
                                DebtRingFocusSegment.NONE
                            } else {
                                DebtRingFocusSegment.PAID
                            }
                        },
                        modifier = Modifier.testTag("debt_ring_paid_segment_card")
                    )

                    DebtSegmentInteractiveRow(
                        titleFa = "مانده بدهی فعال",
                        subtitleFa = "${PersianCurrencyEngine.toPersianDigits(outstandingInstallmentsCount.toString())} قسط باقی‌مانده",
                        amountToman = metrics.outstandingDebtToman,
                        percent = metrics.outstandingPercent,
                        accentColor = outstandingColor,
                        icon = Icons.Default.Schedule,
                        isFocused = focusedSegment == DebtRingFocusSegment.OUTSTANDING,
                        currencyUnit = currencyUnit,
                        privacyMasked = privacyMasked,
                        onClick = {
                            focusedSegment = if (focusedSegment == DebtRingFocusSegment.OUTSTANDING) {
                                DebtRingFocusSegment.NONE
                            } else {
                                DebtRingFocusSegment.OUTSTANDING
                            }
                        },
                        modifier = Modifier.testTag("debt_ring_outstanding_segment_card")
                    )
                }
            }

            // Proportional Dual-Segment Horizontal Ratio Bar (Paid Teal vs. Outstanding Gold)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val paidWeight = (metrics.paidRatio * animProgress).coerceAtLeast(0.04f)
                    val outWeight = (1f - paidWeight).coerceAtLeast(0.04f)
                    Box(
                        modifier = Modifier
                            .weight(paidWeight)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(4.dp))
                            .background(paidColor)
                    )
                    if (metrics.outstandingDebtToman > 0L) {
                        Box(
                            modifier = Modifier
                                .weight(outWeight)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(4.dp))
                                .background(outstandingColor)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TarazinoAutoSizeText(
                        text = "کل تعهدات: ${
                            PersianCurrencyEngine.formatAmount(
                                metrics.totalDebtToman,
                                currencyUnit,
                                privacyMasked = privacyMasked
                            )
                        }",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        minFontSize = 8.5.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${PersianCurrencyEngine.toPersianDigits(metrics.paidPercent.toString())}٪ تسویه / ${
                            PersianCurrencyEngine.toPersianDigits(metrics.outstandingPercent.toString())
                        }٪ مانده",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun DebtSegmentInteractiveRow(
    titleFa: String,
    subtitleFa: String,
    amountToman: Long,
    percent: Int,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isFocused: Boolean,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTarazinoColors.current
    val surfaceBg by animateColorAsState(
        targetValue = if (isFocused) {
            accentColor.copy(alpha = if (ext.isDark) 0.16f else 0.11f)
        } else {
            ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.72f else 0.88f)
        },
        label = "SegmentSurfaceBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) accentColor.copy(alpha = 0.80f) else ext.cardBorderTop,
        label = "SegmentBorderColor"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = surfaceBg,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(if (isFocused) 1.4.dp else 0.85.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    TarazinoAutoSizeText(
                        text = titleFa,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        minFontSize = 9.sp,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = accentColor.copy(alpha = if (ext.isDark) 0.18f else 0.14f)
                ) {
                    Text(
                        text = "${PersianCurrencyEngine.toPersianDigits(percent.toString())}٪",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = accentColor,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            FormattedMoneyText(
                amountToman = amountToman,
                unit = currencyUnit,
                privacyMasked = privacyMasked,
                style = MaterialTheme.typography.titleSmall,
                numberColor = MaterialTheme.colorScheme.onSurface,
                unitColor = accentColor,
                minFontSize = 9.5.sp,
                modifier = Modifier.fillMaxWidth()
            )

            TarazinoAutoSizeText(
                text = subtitleFa,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                minFontSize = 8.sp
            )
        }
    }
}
