package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
}

val LocalSpotlightRegistry = compositionLocalOf { SpotlightTargetRegistry() }

fun Modifier.tourSpotlightTarget(targetId: String): Modifier = composed {
    val registry = LocalSpotlightRegistry.current
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
    val targetRect = registry.targetBounds[step.targetElementId]
    val density = LocalDensity.current

    val infiniteTransition = rememberInfiniteTransition(label = "SpotlightPulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 2f,
        targetValue = 13f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseRadius"
    )
    val pointerBouncePx by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 780, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PointerBounce"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("interactive_onboarding_tour_overlay")
    ) {
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val padPx = with(density) { 8.dp.toPx() }

        // Smoothly gliding spotlight cutout coordinates (Apple Keynote / Copilot fluid glide)
        val desiredLeft = targetRect?.let { (it.left - padPx).coerceAtLeast(6f) } ?: (screenWidthPx * 0.15f)
        val desiredTop = targetRect?.let { (it.top - padPx).coerceAtLeast(6f) } ?: (screenHeightPx * 0.18f)
        val desiredRight = targetRect?.let { (it.right + padPx).coerceAtMost(screenWidthPx - 6f) } ?: (screenWidthPx * 0.85f)
        val desiredBottom = targetRect?.let { (it.bottom + padPx).coerceAtMost(screenHeightPx - 6f) } ?: (screenHeightPx * 0.34f)

        val glideSpec = spring<Float>(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        )
        val animLeft by animateFloatAsState(targetValue = desiredLeft, animationSpec = glideSpec, label = "SpotLeft")
        val animTop by animateFloatAsState(targetValue = desiredTop, animationSpec = glideSpec, label = "SpotTop")
        val animRight by animateFloatAsState(targetValue = desiredRight, animationSpec = glideSpec, label = "SpotRight")
        val animBottom by animateFloatAsState(targetValue = desiredBottom, animationSpec = glideSpec, label = "SpotBottom")

        val isTargetInUpperHalf = targetRect?.let { it.center.y < screenHeightPx * 0.54f } ?: true

        // 1. Full-Screen Gliding Spotlight Cutout Canvas + Dual-Tone Aurora Halo
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            drawRect(color = Color(0xDC04090D))

            if (targetRect != null) {
                val spotWidth = (animRight - animLeft).coerceAtLeast(42f)
                val spotHeight = (animBottom - animTop).coerceAtLeast(36f)
                val corner = CornerRadius(19.dp.toPx(), 19.dp.toPx())

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
                    color = Color(0xFF10B981).copy(alpha = 0.22f),
                    topLeft = Offset(animLeft - pulseRadius * 1.15f, animTop - pulseRadius * 1.15f),
                    size = Size(spotWidth + pulseRadius * 2.3f, spotHeight + pulseRadius * 2.3f),
                    cornerRadius = CornerRadius(25.dp.toPx(), 25.dp.toPx()),
                    style = Stroke(width = 4.dp.toPx())
                )

                // Pulsing golden dashed radar halo
                drawRoundRect(
                    color = Color(0xFFF59E0B).copy(alpha = 0.65f),
                    topLeft = Offset(animLeft - pulseRadius * 0.6f, animTop - pulseRadius * 0.6f),
                    size = Size(spotWidth + pulseRadius * 1.2f, spotHeight + pulseRadius * 1.2f),
                    cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f), 0f)
                    )
                )

                // Crisp neon emerald specular rim
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF34D399), Color(0xFF38BDF8), Color(0xFFF59E0B))
                    ),
                    topLeft = Offset(animLeft, animTop),
                    size = Size(spotWidth, spotHeight),
                    cornerRadius = corner,
                    style = Stroke(width = 2.8.dp.toPx())
                )
            }
        }

        // 2. Smoothly Gliding & Bouncing Graphical Pointer Pill
        if (targetRect != null) {
            val pointerWidthPx = with(density) { 176.dp.toPx() }
            val animCenterX = (animLeft + animRight) / 2f
            val pointerX = (animCenterX - pointerWidthPx / 2f)
                .coerceIn(16f, (screenWidthPx - pointerWidthPx - 16f).coerceAtLeast(16f))
            val pointerY = if (isTargetInUpperHalf) {
                (animBottom + with(density) { 12.dp.toPx() } + pointerBouncePx)
                    .coerceAtMost(screenHeightPx * 0.55f)
            } else {
                (animTop - with(density) { 46.dp.toPx() } + pointerBouncePx)
                    .coerceAtLeast(with(density) { 112.dp.toPx() })
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFFF59E0B),
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                modifier = Modifier
                    .offset { IntOffset(pointerX.roundToInt(), pointerY.roundToInt()) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isTargetInUpperHalf) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = null,
                        tint = Color(0xFF042F2E),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = Color(0xFF042F2E),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "محل این بخش در صفحه",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF042F2E),
                        maxLines = 1
                    )
                }
            }
        }

        // 3. Compact Top Tour Map Bar (unless step 0 is highlighting top_bar_controls)
        if (step.targetElementId != "top_bar_controls") {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF0A141A).copy(alpha = 0.94f),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 28.dp)
                    .align(Alignment.TopCenter)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color(0xFF10B981).copy(alpha = 0.65f),
                                Color(0xFF38BDF8).copy(alpha = 0.35f)
                            )
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تور تعاملی المان‌به‌المان ترازینو",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF6EE7B7),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(
                            onClick = onSkipOrFinish,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("skip_tour_btn")
                        ) {
                            Text(
                                text = "بستن تور",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFCBD5E1)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "بستن تور آموزشی",
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TarazinoNavTab.entries.forEach { tab ->
                            val isTarget = tab == step.targetTab
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isTarget) Color(0xFF10B981)
                                        else Color(0xFF182630)
                                    )
                                    .padding(vertical = 5.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    text = tab.titleFa,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isTarget) FontWeight.ExtraBold else FontWeight.Normal,
                                    color = if (isTarget) Color(0xFF042F2E) else Color(0xFF94A3B8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Titanium Glass Coach-Mark Explanation Card (Never covers the highlighted element)
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (fadeIn(tween(240)) + slideInVertically(tween(260)) { it / 10 })
                    .togetherWith(fadeOut(tween(180)) + slideOutVertically(tween(180)) { -it / 12 })
            },
            label = "TourStepCardTransition",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .then(
                    if (isTargetInUpperHalf) {
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 18.dp)
                    } else {
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 116.dp)
                    }
                )
        ) { currentStep ->
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0C1820).copy(alpha = 0.96f),
                shadowElevation = 20.dp,
                border = BorderStroke(
                    width = 1.4.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFF10B981),
                            Color(0xFF38BDF8).copy(alpha = 0.7f),
                            Color(0xFFF59E0B)
                        )
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF10B981).copy(alpha = 0.10f),
                                    Color.Transparent
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF064E3B).copy(alpha = 0.85f),
                                border = BorderStroke(0.8.dp, Color(0xFF34D399).copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = currentStep.badgeTitleFa,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF6EE7B7),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }

                            if (currentStep.targetElementId == "top_bar_controls") {
                                TextButton(
                                    onClick = onSkipOrFinish,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("skip_tour_btn")
                                ) {
                                    Text(
                                        text = "رد کردن تور",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = currentStep.headlineFa,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentStep.descriptionFa,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        currentStep.highlightFeaturesFa.forEach { itemFa ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 2.5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = itemFa,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFF1F5F9)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Animated Progress Pills + Tactile Navigation Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                steps.forEachIndexed { idx, _ ->
                                    val active = idx == currentStepIndex
                                    val dotWidth by animateDpAsState(
                                        targetValue = if (active) 20.dp else 7.dp,
                                        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMedium),
                                        label = "TourDotWidth_$idx"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .height(7.dp)
                                            .width(dotWidth)
                                            .clip(CircleShape)
                                            .background(
                                                if (active) Color(0xFF10B981)
                                                else if (idx < currentStepIndex) Color(0xFF10B981).copy(alpha = 0.45f)
                                                else Color(0xFF334155)
                                            )
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (currentStepIndex > 0) {
                                    OutlinedButton(
                                        onClick = onPreviousStep,
                                        shape = RoundedCornerShape(13.dp),
                                        border = BorderStroke(1.dp, Color(0xFF475569)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier.testTag("tour_prev_btn")
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
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isLastStep) Color(0xFFF59E0B) else Color(0xFF10B981)
                                    ),
                                    modifier = Modifier.testTag("tour_next_or_finish_btn")
                                ) {
                                    Text(
                                        text = if (isLastStep) {
                                            "شروع کار با ترازینو"
                                        } else {
                                            "المان بعدی (${PersianCurrencyEngine.toPersianDigits((currentStepIndex + 2).toString())}/${PersianCurrencyEngine.toPersianDigits(steps.size.toString())})"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF042F2E),
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1
                                    )
                                    if (!isLastStep) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "بعدی",
                                            tint = Color(0xFF042F2E),
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
    }
}
