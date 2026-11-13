package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseRadius"
    )
    val pointerBouncePx by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
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

        // Determine whether the highlighted element is in the top half or bottom half of the screen
        val isTargetInUpperHalf = targetRect?.let { it.center.y < screenHeightPx * 0.54f } ?: true

        // 1. Full-Screen Spotlight Cutout Canvas + Glowing Ring
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            // Dim background
            drawRect(color = Color(0xD8050B0E))

            targetRect?.let { rect ->
                val pad = 8.dp.toPx()
                val left = (rect.left - pad).coerceAtLeast(6f)
                val top = (rect.top - pad).coerceAtLeast(6f)
                val right = (rect.right + pad).coerceAtMost(size.width - 6f)
                val bottom = (rect.bottom + pad).coerceAtMost(size.height - 6f)
                val spotWidth = (right - left).coerceAtLeast(40f)
                val spotHeight = (bottom - top).coerceAtLeast(36f)
                val corner = CornerRadius(18.dp.toPx(), 18.dp.toPx())

                // Clear the spotlight hole so the actual UI button/card shines through at 100% clarity
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(left, top),
                    size = Size(spotWidth, spotHeight),
                    cornerRadius = corner,
                    blendMode = BlendMode.Clear
                )

                // Outer pulsing golden-emerald halo
                drawRoundRect(
                    color = Color(0xFFF59E0B).copy(alpha = 0.55f),
                    topLeft = Offset(left - pulseRadius, top - pulseRadius),
                    size = Size(spotWidth + pulseRadius * 2, spotHeight + pulseRadius * 2),
                    cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f), 0f)
                    )
                )

                // Inner crisp neon emerald border
                drawRoundRect(
                    color = Color(0xFF10B981),
                    topLeft = Offset(left, top),
                    size = Size(spotWidth, spotHeight),
                    cornerRadius = corner,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        // 2. Bouncing Graphical Pointer Badge (نشانگر گرافیکی متحرک کنار المان)
        targetRect?.let { rect ->
            val pointerWidthPx = with(density) { 176.dp.toPx() }
            val pointerX = (rect.center.x - pointerWidthPx / 2f)
                .coerceIn(16f, (screenWidthPx - pointerWidthPx - 16f).coerceAtLeast(16f))
            val pointerY = if (isTargetInUpperHalf) {
                (rect.bottom + with(density) { 12.dp.toPx() } + pointerBouncePx)
                    .coerceAtMost(screenHeightPx * 0.55f)
            } else {
                (rect.top - with(density) { 46.dp.toPx() } + pointerBouncePx)
                    .coerceAtLeast(with(density) { 112.dp.toPx() })
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFFF59E0B),
                shadowElevation = 10.dp,
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
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF042F2E),
                        maxLines = 1
                    )
                }
            }
        }

        // 3. Compact Top Tour Map Bar (unless step 0 is highlighting top_bar_controls!)
        if (step.targetElementId != "top_bar_controls") {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0F1D24).copy(alpha = 0.96f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 28.dp)
                    .align(Alignment.TopCenter)
                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.65f), RoundedCornerShape(16.dp))
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
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isTarget) Color(0xFF10B981) else Color(0xFF1E293B))
                                    .padding(vertical = 5.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    text = tab.titleFa,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isTarget) FontWeight.Bold else FontWeight.Normal,
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

        // 4. Smartly Positioned Coach-Mark Explanation Card (Never covers the highlighted element)
        AnimatedContent(
            targetState = step,
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
                            .padding(top = 112.dp)
                    }
                )
        ) { currentStep ->
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101D24)),
                elevation = CardDefaults.cardElevation(defaultElevation = 18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF10B981), Color(0xFFF59E0B))
                        ),
                        shape = RoundedCornerShape(22.dp)
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
                            color = Color(0xFF064E3B)
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
                                Text("رد کردن تور", style = MaterialTheme.typography.labelSmall, color = Color(0xFFCBD5E1))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = currentStep.headlineFa,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentStep.descriptionFa,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    currentStep.highlightFeaturesFa.forEach { itemFa ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress Dots + Previous/Next Rectangular Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            steps.forEachIndexed { idx, _ ->
                                val active = idx == currentStepIndex
                                Box(
                                    modifier = Modifier
                                        .height(7.dp)
                                        .width(if (active) 18.dp else 7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (active) Color(0xFF10B981) else Color(0xFF334155)
                                        )
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (currentStepIndex > 0) {
                                OutlinedButton(
                                    onClick = onPreviousStep,
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag("tour_prev_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "قبلی",
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("قبلی", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                }
                            }

                            Button(
                                onClick = if (isLastStep) onSkipOrFinish else onNextStep,
                                shape = RoundedCornerShape(12.dp),
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
                                    fontWeight = FontWeight.Bold,
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
