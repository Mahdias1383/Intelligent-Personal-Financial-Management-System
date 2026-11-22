package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.PersianCurrencyEngine
import com.example.ui.InteractiveTourStep
import com.example.ui.TarazinoNavTab
import kotlin.math.roundToInt

class SpotlightTargetRegistry {
    val targetBounds = mutableStateMapOf<String, Rect>()

    fun updateBounds(targetId: String, rect: Rect) {
        if (rect.width > 0f && rect.height > 0f) {
            targetBounds[targetId] = rect
        }
    }

    fun clearBounds(targetId: String) {
        targetBounds.remove(targetId)
    }
}

val LocalSpotlightRegistry = compositionLocalOf { SpotlightTargetRegistry() }

fun Modifier.tourSpotlightTarget(targetId: String): Modifier = composed {
    val registry = LocalSpotlightRegistry.current
    DisposableEffect(targetId) {
        onDispose {
            registry.clearBounds(targetId)
        }
    }
    this.onGloballyPositioned { coordinates ->
        if (coordinates.isAttached) {
            registry.updateBounds(targetId, coordinates.boundsInRoot())
        }
    }
}

@Composable
fun InteractiveOnboardingTourOverlay(
    steps: List<InteractiveTourStep>,
    currentStepIndex: Int,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onSkipOrFinish: () -> Unit
) {
    val step = steps.getOrElse(currentStepIndex) { steps.first() }
    val isLastStep = currentStepIndex == steps.lastIndex
    val registry = LocalSpotlightRegistry.current
    val rawTargetRect = registry.targetBounds[step.targetElementId]
    val density = LocalDensity.current

    val statusBarTopPx = WindowInsets.statusBars.getTop(density).toFloat()
    val navBarBottomPx = WindowInsets.navigationBars.getBottom(density).toFloat()

    val infiniteTransition = rememberInfiniteTransition(label = "SpotlightPulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 2f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseRadius"
    )
    val pointerBouncePx by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 780, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PointerBounce"
    )

    val stepProgress by animateFloatAsState(
        targetValue = (currentStepIndex + 1).toFloat() / steps.size.coerceAtLeast(1).toFloat(),
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "TourStepProgress"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            // Absorb stray background touches so user cannot accidentally click underlying UI during tour
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .testTag("interactive_onboarding_tour_overlay")
    ) {
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val padPx = with(density) { 6.dp.toPx() }
        val edgeMarginPx = with(density) { 8.dp.toPx() }

        // Only treat targetRect as visible if it genuinely intersects the usable viewport
        val targetRect = rawTargetRect?.takeIf { rect ->
            rect.width > 4f &&
                rect.height > 4f &&
                rect.bottom > statusBarTopPx + 4f &&
                rect.top < screenHeightPx - navBarBottomPx - 4f
        }

        // Clamped target spotlight cutout coordinates inside safe viewport bounds
        val desiredLeft = targetRect?.let { (it.left - padPx).coerceIn(edgeMarginPx, screenWidthPx - 56f) }
            ?: (screenWidthPx * 0.15f)
        val desiredTop = targetRect?.let { (it.top - padPx).coerceIn(statusBarTopPx + 4f, screenHeightPx - navBarBottomPx - 56f) }
            ?: (statusBarTopPx + screenHeightPx * 0.14f)
        val desiredRight = targetRect?.let { (it.right + padPx).coerceIn(desiredLeft + 44f, screenWidthPx - edgeMarginPx) }
            ?: (screenWidthPx * 0.85f)
        val desiredBottom = targetRect?.let { (it.bottom + padPx).coerceIn(desiredTop + 36f, screenHeightPx - navBarBottomPx - 6f) }
            ?: (statusBarTopPx + screenHeightPx * 0.30f)

        val glideSpec = spring<Float>(
            dampingRatio = 0.84f,
            stiffness = Spring.StiffnessMediumLow
        )
        val animLeft by animateFloatAsState(targetValue = desiredLeft, animationSpec = glideSpec, label = "SpotLeft")
        val animTop by animateFloatAsState(targetValue = desiredTop, animationSpec = glideSpec, label = "SpotTop")
        val animRight by animateFloatAsState(targetValue = desiredRight, animationSpec = glideSpec, label = "SpotRight")
        val animBottom by animateFloatAsState(targetValue = desiredBottom, animationSpec = glideSpec, label = "SpotBottom")

        // Compare genuine unobstructed vertical space ABOVE vs BELOW the target spotlight
        val spaceAbovePx = (desiredTop - statusBarTopPx).coerceAtLeast(0f)
        val spaceBelowPx = ((screenHeightPx - navBarBottomPx) - desiredBottom).coerceAtLeast(0f)
        val dockCardAtBottom = if (targetRect == null) true else (spaceBelowPx >= spaceAbovePx)

        // Estimated coach-mark height (~270dp) to decide whether the pointer pill has unobstructed room
        val estimatedCardHeightPx = with(density) { 274.dp.toPx() }
        val pointerPillHeightPx = with(density) { 32.dp.toPx() }
        val minPointerGapPx = with(density) { 52.dp.toPx() }

        val freeGapBetweenTargetAndCardPx = if (dockCardAtBottom) {
            (screenHeightPx - navBarBottomPx - estimatedCardHeightPx) - animBottom
        } else {
            animTop - (statusBarTopPx + estimatedCardHeightPx)
        }
        val showPointerPill = targetRect != null && freeGapBetweenTargetAndCardPx >= minPointerGapPx

        // 1. Full-Screen Gliding Spotlight Cutout Canvas + Luminous Halo
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            drawRect(color = Color(0xDC050A12))

            if (targetRect != null) {
                val spotWidth = (animRight - animLeft).coerceAtLeast(42f)
                val spotHeight = (animBottom - animTop).coerceAtLeast(36f)
                val corner = CornerRadius(18.dp.toPx(), 18.dp.toPx())

                // Clear the gliding spotlight window at 100% clarity
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(animLeft, animTop),
                    size = Size(spotWidth, spotHeight),
                    cornerRadius = corner,
                    blendMode = BlendMode.Clear
                )

                // Soft outer ambient aura ring
                drawRoundRect(
                    color = Color(0xFF10E5A5).copy(alpha = 0.22f),
                    topLeft = Offset(animLeft - pulseRadius, animTop - pulseRadius),
                    size = Size(spotWidth + pulseRadius * 2f, spotHeight + pulseRadius * 2f),
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                    style = Stroke(width = 3.5.dp.toPx())
                )

                // Pulsing golden dashed radar halo
                drawRoundRect(
                    color = Color(0xFFF5B742).copy(alpha = 0.65f),
                    topLeft = Offset(animLeft - pulseRadius * 0.55f, animTop - pulseRadius * 0.55f),
                    size = Size(spotWidth + pulseRadius * 1.1f, spotHeight + pulseRadius * 1.1f),
                    cornerRadius = CornerRadius(21.dp.toPx(), 21.dp.toPx()),
                    style = Stroke(
                        width = 1.8.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), 0f)
                    )
                )

                // Crisp emerald-cyan specular rim
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF2FF0B5), Color(0xFF38BDF8), Color(0xFFF5B742))
                    ),
                    topLeft = Offset(animLeft, animTop),
                    size = Size(spotWidth, spotHeight),
                    cornerRadius = corner,
                    style = Stroke(width = 2.4.dp.toPx())
                )
            }
        }

        // 2. Physically-Positioned Bouncing Pointer Pill (uses absoluteOffset to prevent RTL X-mirroring!)
        if (showPointerPill) {
            val pointerWidthPx = with(density) { 156.dp.toPx() }
            val animCenterX = (animLeft + animRight) / 2f
            val pointerX = (animCenterX - pointerWidthPx / 2f)
                .coerceIn(edgeMarginPx, (screenWidthPx - pointerWidthPx - edgeMarginPx).coerceAtLeast(edgeMarginPx))

            val pointerY = if (dockCardAtBottom) {
                (animBottom + with(density) { 10.dp.toPx() } + pointerBouncePx)
                    .coerceIn(
                        statusBarTopPx + edgeMarginPx,
                        (screenHeightPx - navBarBottomPx - estimatedCardHeightPx - pointerPillHeightPx - 8f)
                            .coerceAtLeast(statusBarTopPx + edgeMarginPx)
                    )
            } else {
                (animTop - pointerPillHeightPx - with(density) { 10.dp.toPx() } + pointerBouncePx)
                    .coerceIn(
                        statusBarTopPx + estimatedCardHeightPx + 8f,
                        (screenHeightPx - navBarBottomPx - pointerPillHeightPx - edgeMarginPx)
                            .coerceAtLeast(statusBarTopPx + edgeMarginPx)
                    )
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFFF5B742),
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .absoluteOffset { IntOffset(pointerX.roundToInt(), pointerY.roundToInt()) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = if (dockCardAtBottom) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = null,
                        tint = Color(0xFF031915),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = Color(0xFF031915),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "محل این بخش",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF031915),
                        maxLines = 1
                    )
                }
            }
        }

        // 3. Single Unified Glass Coach-Mark Card (Docked cleanly at Top or Bottom with full Safe Insets)
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (fadeIn(tween(220)) + slideInVertically(tween(240)) { if (dockCardAtBottom) it / 12 else -it / 12 })
                    .togetherWith(fadeOut(tween(160)) + slideOutVertically(tween(160)) { if (dockCardAtBottom) -it / 14 else it / 14 })
            },
            label = "TourStepCardTransition",
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (dockCardAtBottom) {
                        Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    } else {
                        Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    }
                )
        ) { currentStep ->
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF101824).copy(alpha = 0.97f),
                shadowElevation = 18.dp,
                border = BorderStroke(
                    width = 1.15.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFF10E5A5).copy(alpha = 0.85f),
                            Color(0xFF38BDF8).copy(alpha = 0.55f),
                            Color(0xFFF5B742).copy(alpha = 0.75f)
                        )
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF10E5A5).copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    // Integrated Compact Header: Step Badge + Close Button
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
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFF5B742),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = Color(0xFF064E3B).copy(alpha = 0.85f),
                                border = BorderStroke(0.75.dp, Color(0xFF34D399).copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = currentStep.badgeTitleFa,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF6EE7B7),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.14f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onSkipOrFinish)
                                .testTag("skip_tour_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "بستن",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFCBD5E1),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "بستن تور آموزشی",
                                    tint = Color(0xFFCBD5E1),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Integrated Compact 4-Tab Location Breadcrumb Strip (Zero Extra Floating Bar!)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        TarazinoNavTab.entries.forEach { tab ->
                            val isTarget = tab == currentStep.targetTab
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isTarget) Color(0xFF10E5A5)
                                        else Color.White.copy(alpha = 0.06f)
                                    )
                                    .padding(vertical = 4.dp, horizontal = 3.dp)
                            ) {
                                Text(
                                    text = tab.titleFa,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = if (isTarget) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isTarget) Color(0xFF031915) else Color(0xFF94A3B8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Slim Animated Step Progress Track
                    LinearProgressIndicator(
                        progress = { stepProgress },
                        color = Color(0xFF10E5A5),
                        trackColor = Color.White.copy(alpha = 0.10f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.5.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = currentStep.headlineFa,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentStep.descriptionFa,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )

                    if (currentStep.highlightFeaturesFa.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        currentStep.highlightFeaturesFa.forEach { itemFa ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2FF0B5),
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = itemFa,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFF1F5F9),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Spacious Uncrowded Bottom Navigation Row (Zero Overflow on 360dp Screens)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStepIndex > 0) {
                            OutlinedButton(
                                onClick = onPreviousStep,
                                shape = RoundedCornerShape(13.dp),
                                border = BorderStroke(1.dp, Color(0xFF475569)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(0.85f)
                                    .testTag("tour_prev_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "قبلی",
                                    tint = Color(0xFFE2E8F0),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "قبلی",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFE2E8F0),
                                    maxLines = 1
                                )
                            }
                        }

                        Button(
                            onClick = if (isLastStep) onSkipOrFinish else onNextStep,
                            shape = RoundedCornerShape(13.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLastStep) Color(0xFFF5B742) else Color(0xFF10E5A5)
                            ),
                            modifier = Modifier
                                .weight(1.35f)
                                .testTag("tour_next_or_finish_btn")
                        ) {
                            Text(
                                text = if (isLastStep) {
                                    "شروع کار با ترازینو"
                                } else {
                                    "مرحله بعد (${PersianCurrencyEngine.toPersianDigits((currentStepIndex + 2).toString())}/${PersianCurrencyEngine.toPersianDigits(steps.size.toString())})"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF031915),
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!isLastStep) {
                                Spacer(modifier = Modifier.width(5.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "بعدی",
                                    tint = Color(0xFF031915),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
