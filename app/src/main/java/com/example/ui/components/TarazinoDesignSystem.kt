package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.core.CurrencyUnit
import com.example.core.PersianCurrencyEngine
import com.example.ui.theme.LocalTarazinoColors
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Lightweight choreographed spring cascade entrance for modern airy screens.
 * Glides content gently upward with subtle scale & alpha fade in <280ms.
 */
@Composable
fun TarazinoStaggeredReveal(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay((index.coerceIn(0, 8) * 38L))
        visible = true
    }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.84f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "StaggerReveal_$index"
    )
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress.coerceIn(0f, 1f)
            translationY = (1f - progress) * 26f
            val s = 0.975f + (progress * 0.025f)
            scaleX = s
            scaleY = s
        }
    ) {
        content()
    }
}

/**
 * Delicate breathing live pulse dot for radar alerts & active financial indicators.
 */
@Composable
fun TarazinoLivePulseDot(
    color: Color,
    size: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "LivePulse")
    val pulse by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LivePulseVal"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size * 2.2f)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val baseR = (size.toPx() / 2f)
            drawCircle(
                color = color.copy(alpha = (1f - pulse) * 0.45f),
                radius = baseR * (1f + pulse * 1.15f)
            )
            drawCircle(
                color = color,
                radius = baseR
            )
        }
    }
}

/**
 * Tactile spring-scale press feedback modifier with subtle native Android haptic pulse.
 */
fun Modifier.tarazinoPressScale(
    scaleDown: Float = 0.965f,
    enableHaptics: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "PressScale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.material3.ripple(),
            onClick = {
                if (enableHaptics) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onClick()
            }
        )
}

/**
 * Living breathing fintech ambient backdrop with 3 ultra-soft drifting radial aurora clouds.
 * Reads animation phases strictly inside drawBehind for silky 60fps performance with zero layout work.
 */
fun Modifier.tarazinoAmbientBackdrop(
    backgroundColor: Color,
    auroraPrimary: Color,
    auroraSecondary: Color,
    auroraTertiary: Color = Color.Transparent
): Modifier = composed {
    val infinite = rememberInfiniteTransition(label = "TarazinoLivingAurora")
    val driftPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AuroraDriftPhase"
    )
    val breathPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AuroraBreathPhase"
    )

    this.drawBehind {
        drawRect(color = backgroundColor)

        val dx1 = (cos(driftPhase.toDouble()) * size.width * 0.055f).toFloat()
        val dy1 = (sin(driftPhase.toDouble()) * size.height * 0.025f).toFloat()
        val breath1 = 1f + (sin(breathPhase.toDouble()) * 0.06f).toFloat()

        val c1 = Offset(size.width * 0.86f + dx1, size.height * 0.05f + dy1)
        val r1 = size.width * 1.12f * breath1
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    auroraPrimary,
                    auroraPrimary.copy(alpha = auroraPrimary.alpha * 0.45f),
                    auroraPrimary.copy(alpha = auroraPrimary.alpha * 0.12f),
                    Color.Transparent
                ),
                center = c1,
                radius = r1
            ),
            radius = r1,
            center = c1
        )

        val dx2 = (sin(driftPhase.toDouble()) * size.width * 0.05f).toFloat()
        val dy2 = (cos(driftPhase.toDouble()) * size.height * 0.025f).toFloat()
        val breath2 = 1f + (cos(breathPhase.toDouble()) * 0.05f).toFloat()

        val c2 = Offset(size.width * 0.10f + dx2, size.height * 0.34f + dy2)
        val r2 = size.width * 0.96f * breath2
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    auroraSecondary,
                    auroraSecondary.copy(alpha = auroraSecondary.alpha * 0.38f),
                    Color.Transparent
                ),
                center = c2,
                radius = r2
            ),
            radius = r2,
            center = c2
        )

        val tertiaryColor = if (auroraTertiary.alpha > 0.001f) {
            auroraTertiary
        } else {
            auroraPrimary.copy(alpha = auroraPrimary.alpha * 0.40f)
        }
        val c3 = Offset(size.width * 0.52f - dx1 * 0.7f, size.height * 0.92f - dy2 * 0.6f)
        val r3 = size.width * 0.88f * breath1
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    tertiaryColor,
                    tertiaryColor.copy(alpha = tertiaryColor.alpha * 0.35f),
                    Color.Transparent
                ),
                center = c3,
                radius = r3
            ),
            radius = r3,
            center = c3
        )
    }
}

/**
 * CRED × Revolut Ultra Interactive 3D-Tiltable & 3D-Flippable Platinum Metal Vault Card:
 * - Touch/drag across the surface to tilt the physical card in 3D space with a real-time holographic
 *   specular light reflection following your finger!
 * - Flip 180° in 3D (`isFlipped`) to reveal the engraved Back of the Vault Card.
 * - Features dynamic color-morphing radial aura, brushed titanium micro-grain, banknote guilloché rings,
 *   and a gentle continuous specular light sweep.
 */
@Composable
fun TarazinoMetalVaultCard(
    accentPrimary: Color,
    accentSecondary: Color,
    baseStart: Color,
    baseEnd: Color,
    modifier: Modifier = Modifier,
    baseMid: Color? = null,
    cornerRadius: Dp = 30.dp,
    isFlipped: Boolean = false,
    backContent: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val density = LocalDensity.current.density
    val animAccent1 by animateColorAsState(
        targetValue = accentPrimary,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "MetalAccent1"
    )
    val animAccent2 by animateColorAsState(
        targetValue = accentSecondary,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "MetalAccent2"
    )
    val animBaseStart by animateColorAsState(
        targetValue = baseStart,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "MetalBaseStart"
    )
    val animBaseMid by animateColorAsState(
        targetValue = baseMid ?: Color(
            red = (baseStart.red + baseEnd.red) * 0.5f,
            green = (baseStart.green + baseEnd.green) * 0.5f,
            blue = (baseStart.blue + baseEnd.blue) * 0.5f,
            alpha = 1f
        ),
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "MetalBaseMid"
    )
    val animBaseEnd by animateColorAsState(
        targetValue = baseEnd,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "MetalBaseEnd"
    )

    // Interactive 3D touch-driven tilt state
    var rawTiltX by remember { mutableFloatStateOf(0f) }
    var rawTiltY by remember { mutableFloatStateOf(0f) }
    var lightNormX by remember { mutableFloatStateOf(0.75f) }
    var lightNormY by remember { mutableFloatStateOf(0.20f) }
    var isTouching by remember { mutableStateOf(false) }

    val animTiltX by animateFloatAsState(
        targetValue = rawTiltX,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "CardTiltX"
    )
    val animTiltY by animateFloatAsState(
        targetValue = rawTiltY,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "CardTiltY"
    )
    val animLightX by animateFloatAsState(
        targetValue = lightNormX,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "CardLightX"
    )
    val animLightY by animateFloatAsState(
        targetValue = lightNormY,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "CardLightY"
    )
    val flipDegrees by animateFloatAsState(
        targetValue = if (isFlipped && backContent != null) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow),
        label = "CardFlipY"
    )
    val showingBack = flipDegrees > 90f && backContent != null

    val infiniteTransition = rememberInfiniteTransition(label = "MetalSheen")
    val sheenPhase by infiniteTransition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SheenPhase"
    )

    val shape = RoundedCornerShape(cornerRadius)
    Surface(
        shape = shape,
        color = Color.Transparent,
        contentColor = LocalTarazinoColors.current.heroOnSurface,
        shadowElevation = 10.dp,
        border = BorderStroke(
            width = 0.95.dp,
            brush = Brush.linearGradient(
                listOf(
                    animAccent1.copy(alpha = 0.58f),
                    Color.White.copy(alpha = 0.22f),
                    animAccent2.copy(alpha = 0.28f),
                    Color.White.copy(alpha = 0.08f)
                )
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationX = animTiltX
                rotationY = animTiltY + flipDegrees
                cameraDistance = 15f * density
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isTouching = true
                        val nx = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        val ny = (offset.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                        lightNormX = nx
                        lightNormY = ny
                        rawTiltY = (nx - 0.5f) * 15f
                        rawTiltX = (0.5f - ny) * 11f
                    },
                    onDragEnd = {
                        isTouching = false
                        rawTiltX = 0f
                        rawTiltY = 0f
                        lightNormX = 0.75f
                        lightNormY = 0.20f
                    },
                    onDragCancel = {
                        isTouching = false
                        rawTiltX = 0f
                        rawTiltY = 0f
                        lightNormX = 0.75f
                        lightNormY = 0.20f
                    },
                    onDrag = { change, _ ->
                        val pos = change.position
                        val nx = (pos.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        val ny = (pos.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                        lightNormX = nx
                        lightNormY = ny
                        rawTiltY = (nx - 0.5f) * 16f
                        rawTiltX = (0.5f - ny) * 12f
                    }
                )
            }
            .clip(shape)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(modifier = Modifier.matchParentSize()) {
                // 1. Deep 3-stop velvet brushed metallic base
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(animBaseStart, animBaseMid, animBaseEnd),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    )
                )

                // 2. Soft airy corner radial aurora orbs
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(animAccent1.copy(alpha = 0.32f), animAccent1.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(size.width * 0.88f, size.height * 0.12f),
                        radius = size.width * 0.66f
                    ),
                    radius = size.width * 0.66f,
                    center = Offset(size.width * 0.88f, size.height * 0.12f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(animAccent2.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width * 0.12f, size.height * 0.90f),
                        radius = size.width * 0.56f
                    ),
                    radius = size.width * 0.56f,
                    center = Offset(size.width * 0.12f, size.height * 0.90f)
                )

                // 3. Refined twin concentric guilloché filigree arcs
                drawCircle(
                    color = Color.White.copy(alpha = 0.055f),
                    radius = size.width * 0.48f,
                    center = Offset(size.width * 0.10f, size.height * 0.12f),
                    style = Stroke(width = 0.85.dp.toPx())
                )
                drawCircle(
                    color = animAccent1.copy(alpha = 0.06f),
                    radius = size.width * 0.36f,
                    center = Offset(size.width * 0.10f, size.height * 0.12f),
                    style = Stroke(width = 0.7.dp.toPx())
                )

                // 4. Real-time holographic spotlight tracking finger tilt
                val glareCenter = Offset(size.width * animLightX, size.height * animLightY)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isTouching) 0.19f else 0.06f),
                            animAccent1.copy(alpha = if (isTouching) 0.12f else 0.03f),
                            Color.Transparent
                        ),
                        center = glareCenter,
                        radius = size.width * 0.48f
                    ),
                    radius = size.width * 0.48f,
                    center = glareCenter
                )

                // 5. Whisper-soft diagonal specular sheen
                val sweepCenterX = size.width * sheenPhase
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.055f),
                            animAccent1.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        start = Offset(sweepCenterX - size.width * 0.25f, 0f),
                        end = Offset(sweepCenterX + size.width * 0.25f, size.height)
                    )
                )
            }

            if (showingBack && backContent != null) {
                Column(
                    modifier = Modifier.graphicsLayer { rotationY = 180f },
                    content = backContent
                )
            } else {
                Column(content = content)
            }
        }
    }
}

/**
 * 60fps Animated Dual-Wave Liquid Crystal Orb (CRED / Apple Watch style fluid gauge).
 * Displays animated overlapping sine-wave liquid rising to `progress` (0f..1f) inside a specular glass sphere.
 */
@Composable
fun LiquidWaveHealthOrb(
    progress: Float,
    accentColor: Color,
    centerPrimaryText: String,
    centerSubText: String? = null,
    size: Dp = 62.dp,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0f, 1f)
    val animProgress by animateFloatAsState(
        targetValue = clamped,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessVeryLow),
        label = "LiquidLevel"
    )
    val infinite = rememberInfiniteTransition(label = "LiquidWavePhase")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val circlePath = Path().apply {
                addOval(Rect(0f, 0f, w, h))
            }

            // Frosted deep core backdrop
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.14f),
                        Color(0xFF050A0E).copy(alpha = 0.82f)
                    ),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = w * 0.52f
                )
            )

            // Clip inside crystal sphere and draw dual undulating liquid waves
            clipPath(circlePath) {
                val baseWaterY = h * (1f - (0.14f + animProgress * 0.76f))
                val amp = h * 0.065f

                // Back secondary translucent wave
                val backPath = Path().apply {
                    moveTo(0f, h)
                    var x = 0f
                    while (x <= w) {
                        val y = baseWaterY + sin((x / w) * 2f * PI.toFloat() - phase + 1.4f) * (amp * 0.85f)
                        lineTo(x, y)
                        x += 3f
                    }
                    lineTo(w, h)
                    close()
                }
                drawPath(backPath, color = accentColor.copy(alpha = 0.32f))

                // Front primary luminous wave
                val frontPath = Path().apply {
                    moveTo(0f, h)
                    var x = 0f
                    while (x <= w) {
                        val y = baseWaterY + sin((x / w) * 2.2f * PI.toFloat() + phase) * amp
                        lineTo(x, y)
                        x += 3f
                    }
                    lineTo(w, h)
                    close()
                }
                drawPath(
                    path = frontPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.78f),
                            accentColor.copy(alpha = 0.45f)
                        ),
                        startY = baseWaterY - amp,
                        endY = h
                    )
                )

                // Top crystal glass crescent highlight
                drawOval(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.34f), Color.Transparent)
                    ),
                    topLeft = Offset(w * 0.18f, h * 0.06f),
                    size = Size(w * 0.64f, h * 0.24f)
                )
            }

            // Outer glowing ring border
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(
                        accentColor.copy(alpha = 0.85f),
                        Color.White.copy(alpha = 0.45f),
                        accentColor.copy(alpha = 0.28f),
                        accentColor.copy(alpha = 0.85f)
                    )
                ),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
        ) {
            TarazinoAutoSizeText(
                text = centerPrimaryText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1,
                minFontSize = 8.5.sp,
                textAlign = TextAlign.Center
            )
            if (!centerSubText.isNullOrBlank()) {
                TarazinoAutoSizeText(
                    text = centerSubText,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    minFontSize = 7.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * CRED / Revolut Kinetic RTL Slide-to-Settle Track:
 * - Drag the glowing emerald checkmark thumb from right to left to trigger payment with live fill + haptics!
 * - Also supports instant single-tap on the thumb/track for zero-friction accessibility and testing.
 */
@Composable
fun TarazinoSlideToSettleBar(
    labelFa: String,
    accentColor: Color = LocalTarazinoColors.current.positive,
    onConfirmSettle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTarazinoColors.current
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var triggered by remember { mutableStateOf(false) }

    val animFraction by animateFloatAsState(
        targetValue = dragFraction,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMedium),
        label = "SlideSettleFrac"
    )
    val shimmer = rememberInfiniteTransition(label = "SlideChevronPulse")
    val chevronAlpha by shimmer.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ChevronPulse"
    )

    BoxWithConstraints(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(
                if (ext.isDark) accentColor.copy(alpha = 0.11f) else accentColor.copy(alpha = 0.10f)
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        accentColor.copy(alpha = 0.55f),
                        accentColor.copy(alpha = 0.25f)
                    )
                ),
                shape = RoundedCornerShape(15.dp)
            )
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onConfirmSettle()
            }
    ) {
        val thumbSizeDp = 36.dp
        val padDp = 4.dp
        val maxTravelPx = with(density) { (maxWidth - thumbSizeDp - padDp * 2).toPx().coerceAtLeast(1f) }
        val baseFillFrac = ((thumbSizeDp + padDp * 2) / maxWidth).coerceIn(0.12f, 0.36f)

        // Dynamic luminous fill growing behind the dragged thumb (RTL start -> end)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth((baseFillFrac + animFraction * (1f - baseFillFrac)).coerceIn(0f, 1f))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            accentColor.copy(alpha = 0.10f),
                            accentColor.copy(alpha = 0.34f + animFraction * 0.35f)
                        )
                    )
                )
        )

        // Centered kinetic Persian cue text + pulsing chevron arrows
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 44.dp, end = 10.dp)
        ) {
            TarazinoAutoSizeText(
                text = if (animFraction > 0.65f) "رها کنید برای تسویه ✓" else labelFa,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                maxLines = 1,
                minFontSize = 9.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (animFraction <= 0.65f) {
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = accentColor.copy(alpha = chevronAlpha),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Draggable glowing checkmark thumb (starts at RTL start = visual right)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = padDp)
                .offset { IntOffset((maxTravelPx * animFraction).roundToInt(), 0) }
                .size(thumbSizeDp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            accentColor,
                            accentColor.copy(alpha = 0.82f)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                .pointerInput(maxTravelPx) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragFraction >= 0.58f && !triggered) {
                                triggered = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onConfirmSettle()
                            }
                            dragFraction = 0f
                            triggered = false
                        },
                        onDragCancel = {
                            dragFraction = 0f
                            triggered = false
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            // In RTL layout, dragging left has negative screen X delta -> increases progress
                            val deltaFrac = (-dragAmount) / maxTravelPx
                            val next = (dragFraction + deltaFrac).coerceIn(0f, 1f)
                            if (dragFraction < 0.58f && next >= 0.58f) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            dragFraction = next
                        }
                    )
                }
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "تسویه قسط",
                tint = ext.onPositive,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Apple Wallet × BluBank Miniature Physical Card for Branded Counterparties (اسنپ‌پی، دیجی‌پی، بانک مهر...).
 */
@Composable
fun TarazinoMiniBrandCard(
    name: String,
    categoryBadgeFa: String,
    remainingToman: Long,
    progressRatio: Float,
    pendingCount: Int,
    lastFourDigits: String?,
    accentColor: Color,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTarazinoColors.current
    val displayAmount = remainingToman * currencyUnit.multiplierFromToman
    val shape = RoundedCornerShape(22.dp)

    Surface(
        shape = shape,
        color = Color.Transparent,
        contentColor = ext.miniCardOnSurface,
        shadowElevation = if (ext.isDark) 3.dp else 5.dp,
        border = BorderStroke(
            width = if (ext.isDark) 0.85.dp else 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    accentColor.copy(alpha = if (ext.isDark) 0.50f else 0.42f),
                    if (ext.isDark) Color.White.copy(alpha = 0.14f) else ext.cardBorderTop,
                    accentColor.copy(alpha = if (ext.isDark) 0.14f else 0.20f)
                )
            )
        ),
        modifier = modifier
            .width(170.dp)
            .height(108.dp)
            .tarazinoPressScale(scaleDown = 0.95f, onClick = onClick)
            .clip(shape)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(ext.miniCardStart, ext.miniCardEnd),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (ext.isDark) 0.26f else 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.14f),
                        radius = size.width * 0.68f
                    ),
                    radius = size.width * 0.68f,
                    center = Offset(size.width * 0.88f, size.height * 0.14f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(13.dp),
                verticalArrangement = Arrangement.SpaceBetween
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
                        CounterpartyMonogramAvatar(
                            name = name,
                            color = accentColor,
                            size = 28.dp
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        TarazinoAutoSizeText(
                            text = name.substringBefore(" ("),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = ext.miniCardOnSurface,
                            maxLines = 1,
                            minFontSize = 9.5.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Contactless,
                        contentDescription = null,
                        tint = ext.miniCardOnSurfaceMuted.copy(alpha = 0.65f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            TarazinoAutoSizeText(
                                text = if (!lastFourDigits.isNullOrBlank()) {
                                    "•••• ${PersianCurrencyEngine.toPersianDigits(lastFourDigits)}"
                                } else {
                                    categoryBadgeFa
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = ext.miniCardOnSurfaceMuted,
                                maxLines = 1,
                                minFontSize = 8.sp
                            )
                            val compactCombined = if (privacyMasked) {
                                formatCompactPersianToman(displayAmount, true)
                            } else {
                                "${ltrIsolateDigits(formatCompactPersianToman(displayAmount, false))} ${currencyUnit.labelFa}"
                            }
                            TarazinoAutoSizeText(
                                text = compactCombined,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = ext.miniCardOnSurface,
                                maxLines = 1,
                                minFontSize = 10.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accentColor.copy(alpha = if (ext.isDark) 0.18f else 0.12f),
                            border = BorderStroke(0.7.dp, accentColor.copy(alpha = 0.42f))
                        ) {
                            TarazinoAutoSizeText(
                                text = if (pendingCount > 0) {
                                    "${PersianCurrencyEngine.toPersianDigits(pendingCount.toString())} قسط"
                                } else {
                                    "تسویه"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                maxLines = 1,
                                minFontSize = 8.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(7.dp))

                    // Luminous metallic progress filament
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.5.dp)
                            .clip(CircleShape)
                            .background(
                                if (ext.isDark) Color.White.copy(alpha = 0.12f)
                                else accentColor.copy(alpha = 0.14f)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressRatio.coerceIn(0.06f, 1f))
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            accentColor,
                                            if (ext.isDark) Color.White.copy(alpha = 0.85f) else accentColor.copy(alpha = 0.72f)
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Minimal brushed-gold EMV smart chip + contactless wave vector emblem for vault cards.
 */
@Composable
fun MetallicSmartChipEmblem(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(width = 34.dp, height = 24.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(
                    accentColor.copy(alpha = 0.72f),
                    Color.White.copy(alpha = 0.55f),
                    accentColor.copy(alpha = 0.38f)
                )
            ),
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.6f),
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
            style = Stroke(width = 0.8.dp.toPx())
        )
        val lineColor = Color(0xFF041C19).copy(alpha = 0.58f)
        val sw = 0.9.dp.toPx()
        drawLine(lineColor, Offset(0f, h * 0.35f), Offset(w * 0.32f, h * 0.35f), sw)
        drawLine(lineColor, Offset(0f, h * 0.65f), Offset(w * 0.32f, h * 0.65f), sw)
        drawLine(lineColor, Offset(w * 0.68f, h * 0.35f), Offset(w, h * 0.35f), sw)
        drawLine(lineColor, Offset(w * 0.68f, h * 0.65f), Offset(w, h * 0.65f), sw)
        drawRoundRect(
            color = lineColor,
            topLeft = Offset(w * 0.32f, h * 0.16f),
            size = Size(w * 0.36f, h * 0.68f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = Stroke(width = sw)
        )
    }
}

/**
 * Lightweight 60fps procedural celebratory confetti & sparkle burst fired when settling an installment.
 */
@Composable
fun TarazinoCelebrationConfettiBurst(
    triggerKey: String,
    modifier: Modifier = Modifier
) {
    var progressTarget by remember(triggerKey) { mutableStateOf(0f) }
    LaunchedEffect(triggerKey) {
        progressTarget = 1f
    }
    val progress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 1550, easing = FastOutSlowInEasing),
        label = "ConfettiProgress"
    )
    if (progress >= 0.99f) return

    val particles = remember(triggerKey) {
        val palette = listOf(
            Color(0xFF35B8AB),
            Color(0xFF53C9BD),
            Color(0xFFDAB978),
            Color(0xFFE8CB90),
            Color(0xFF4DA3B8),
            Color(0xFFC99B68)
        )
        List(32) { idx ->
            val angleDeg = -165f + (idx * (150f / 31f))
            val speed = 0.55f + ((idx * 37) % 100) / 135f
            val sizeDp = 5f + ((idx * 19) % 6)
            val spin = if (idx % 2 == 0) 280f else -260f
            Triple(angleDeg, speed, Triple(sizeDp, spin, palette[idx % palette.size]))
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val originX = size.width * 0.5f
        val originY = 96.dp.toPx()
        val maxDist = size.width * 0.55f
        val alpha = (1f - ((progress - 0.55f) / 0.45f).coerceIn(0f, 1f))

        particles.forEach { (angleDeg, speed, meta) ->
            val (sizeDp, spin, color) = meta
            val rad = angleDeg * PI / 180.0
            val dist = maxDist * speed * progress
            val gravityY = (progress * progress) * 200.dp.toPx()
            val px = originX + (cos(rad) * dist).toFloat()
            val py = originY + (sin(rad) * dist * 0.65f).toFloat() + gravityY
            val sz = sizeDp.dp.toPx()

            rotate(degrees = spin * progress, pivot = Offset(px, py)) {
                drawRoundRect(
                    color = color.copy(alpha = alpha),
                    topLeft = Offset(px - sz / 2f, py - sz * 0.35f),
                    size = Size(sz, sz * 0.68f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }
    }
}

/**
 * Sculpted Fintech Glass Card with overhead tonal sheen, specular top-to-bottom border, and tactile press motion.
 */
@Composable
fun TarazinoGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    accentBorderColor: Color? = null,
    containerColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val ext = LocalTarazinoColors.current
    val shape = RoundedCornerShape(cornerRadius)
    val useSculptedGlass = containerColor == Color.Unspecified
    val borderBrush = if (accentBorderColor != null) {
        Brush.verticalGradient(
            listOf(
                accentBorderColor.copy(alpha = if (ext.isDark) 0.52f else 0.44f),
                accentBorderColor.copy(alpha = if (ext.isDark) 0.12f else 0.16f)
            )
        )
    } else {
        Brush.verticalGradient(listOf(ext.cardBorderTop, ext.cardBorderBottom))
    }

    Surface(
        shape = shape,
        color = if (useSculptedGlass) ext.cardGlassBottom else containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = if (ext.isDark) 0.dp else 2.5.dp,
        border = BorderStroke(0.85.dp, borderBrush),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(shape)
                        .tarazinoPressScale(scaleDown = 0.985f, onClick = onClick)
                } else Modifier
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (useSculptedGlass) {
                        Modifier.background(
                            Brush.verticalGradient(
                                listOf(ext.cardGlassTop, ext.cardGlassBottom)
                            )
                        )
                    } else {
                        Modifier
                    }
                ),
            content = content
        )
    }
}

/**
 * Pure deterministic binary-search solver that computes the largest font size in [minFontSizeSp, maxFontSizeSp]
 * satisfying [fits]. Executes in at most 10 evaluations with 0.25sp precision and zero UI flicker.
 */
fun findFittingFontSizeSp(
    maxFontSizeSp: Float,
    minFontSizeSp: Float,
    precisionSp: Float = 0.25f,
    fits: (Float) -> Boolean
): Float {
    val safeMax = maxFontSizeSp.coerceAtLeast(1f)
    val safeMin = minFontSizeSp.coerceIn(1f, safeMax)
    if (safeMax - safeMin <= precisionSp) {
        return if (fits(safeMax)) safeMax else safeMin
    }
    // Fast-path: common case where text already fits at maximum size (single measurement!)
    if (fits(safeMax)) return safeMax
    // Floor guard: never drop below minimum readable size
    if (!fits(safeMin)) return safeMin

    var low = safeMin
    var high = safeMax
    var best = safeMin
    var iterations = 0
    while (high - low > precisionSp && iterations < 10) {
        val mid = (low + high) * 0.5f
        if (fits(mid)) {
            best = mid
            low = mid
        } else {
            high = mid
        }
        iterations++
    }
    return best
}

/**
 * Isolates a numeric run (digits, commas, decimal points, minus/plus sign) in LTR bidi context
 * so thousands separators and negative signs never invert inside RTL Persian layouts.
 */
fun ltrIsolateDigits(raw: String): String = "\u2066$raw\u2069"

/**
 * Zero-flicker, container-aware auto-sizing Persian Text renderer.
 * Uses [BoxWithConstraints] + [rememberTextMeasurer] binary search with [EstedadFontFamily]
 * before the first draw so long sentences, headers, badges, and financial figures adapt cleanly
 * on every screen width (320dp mini -> 600dp+ foldable) and every system/app font scale.
 */
@Composable
fun TarazinoAutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    minFontSize: TextUnit = 9.sp,
    maxFontSize: TextUnit = style.fontSize,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    softWrap: Boolean = maxLines > 1
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val resolvedFontFamily = style.fontFamily ?: com.example.ui.theme.EstedadFontFamily
    val resolvedWeight = fontWeight ?: style.fontWeight
    val baseMaxSp = if (maxFontSize.isSpecified) maxFontSize.value else 14f
    val safeMinSp = if (minFontSize.isSpecified) minFontSize.value.coerceAtMost(baseMaxSp) else 9f
    val boxAlignment = when (textAlign) {
        TextAlign.Center -> Alignment.Center
        TextAlign.End -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = boxAlignment
    ) {
        val maxWidthPx = if (constraints.hasBoundedWidth && constraints.maxWidth > 0 && constraints.maxWidth < Constraints.Infinity) {
            constraints.maxWidth
        } else {
            -1
        }

        val fittedSp = remember(
            text,
            maxWidthPx,
            density.density,
            density.fontScale,
            baseMaxSp,
            safeMinSp,
            maxLines,
            resolvedWeight,
            resolvedFontFamily,
            softWrap
        ) {
            if (maxWidthPx <= 0 || text.isEmpty()) {
                baseMaxSp
            } else {
                findFittingFontSizeSp(
                    maxFontSizeSp = baseMaxSp,
                    minFontSizeSp = safeMinSp
                ) { candidateSp ->
                    val scaledLineHeight = if (style.lineHeight.isSpecified && baseMaxSp > 0f) {
                        (style.lineHeight.value * (candidateSp / baseMaxSp)).coerceAtLeast(candidateSp * 1.28f).sp
                    } else {
                        (candidateSp * 1.32f).sp
                    }
                    val result = textMeasurer.measure(
                        text = text,
                        style = style.copy(
                            fontFamily = resolvedFontFamily,
                            fontWeight = resolvedWeight,
                            fontSize = candidateSp.sp,
                            lineHeight = scaledLineHeight
                        ),
                        maxLines = maxLines,
                        softWrap = softWrap,
                        overflow = TextOverflow.Clip,
                        constraints = Constraints(maxWidth = maxWidthPx)
                    )
                    !result.hasVisualOverflow && result.lineCount <= maxLines
                }
            }
        }

        val finalLineHeight = if (style.lineHeight.isSpecified && baseMaxSp > 0f) {
            (style.lineHeight.value * (fittedSp / baseMaxSp)).coerceAtLeast(fittedSp * 1.28f).sp
        } else {
            (fittedSp * 1.32f).sp
        }

        val resolvedColor = color.takeOrElse {
            style.color.takeOrElse {
                LocalContentColor.current.takeOrElse { MaterialTheme.colorScheme.onSurface }
            }
        }

        Text(
            text = text,
            color = resolvedColor,
            style = style.copy(
                fontFamily = resolvedFontFamily,
                fontWeight = resolvedWeight,
                fontSize = fittedSp.sp,
                lineHeight = finalLineHeight
            ),
            textAlign = textAlign,
            maxLines = maxLines,
            softWrap = softWrap,
            overflow = overflow
        )
    }
}

/**
 * Container-aware auto-sizing renderer for styled [AnnotatedString] (e.g. mixed-size numbers & labels).
 */
@Composable
fun TarazinoAutoSizeText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    minFontSize: TextUnit = 9.sp,
    maxFontSize: TextUnit = style.fontSize,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    softWrap: Boolean = maxLines > 1
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val resolvedFontFamily = style.fontFamily ?: com.example.ui.theme.EstedadFontFamily
    val baseMaxSp = if (maxFontSize.isSpecified) maxFontSize.value else 14f
    val safeMinSp = if (minFontSize.isSpecified) minFontSize.value.coerceAtMost(baseMaxSp) else 9f
    val boxAlignment = when (textAlign) {
        TextAlign.Center -> Alignment.Center
        TextAlign.End -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }

    fun scaleAnnotated(source: AnnotatedString, ratio: Float): AnnotatedString {
        if (ratio >= 0.999f) return source
        return AnnotatedString(
            text = source.text,
            spanStyles = source.spanStyles.map { range ->
                val item = range.item
                val scaledItem = if (item.fontSize.isSpecified) {
                    item.copy(fontSize = (item.fontSize.value * ratio).coerceAtLeast(7.5f).sp)
                } else {
                    item
                }
                AnnotatedString.Range(scaledItem, range.start, range.end, range.tag)
            },
            paragraphStyles = source.paragraphStyles
        )
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = boxAlignment
    ) {
        val maxWidthPx = if (constraints.hasBoundedWidth && constraints.maxWidth > 0 && constraints.maxWidth < Constraints.Infinity) {
            constraints.maxWidth
        } else {
            -1
        }

        val fittedSp = remember(
            text,
            maxWidthPx,
            density.density,
            density.fontScale,
            baseMaxSp,
            safeMinSp,
            maxLines,
            resolvedFontFamily,
            softWrap
        ) {
            if (maxWidthPx <= 0 || text.isEmpty()) {
                baseMaxSp
            } else {
                findFittingFontSizeSp(
                    maxFontSizeSp = baseMaxSp,
                    minFontSizeSp = safeMinSp
                ) { candidateSp ->
                    val ratio = if (baseMaxSp > 0f) candidateSp / baseMaxSp else 1f
                    val scaledLineHeight = if (style.lineHeight.isSpecified && baseMaxSp > 0f) {
                        (style.lineHeight.value * ratio).coerceAtLeast(candidateSp * 1.28f).sp
                    } else {
                        (candidateSp * 1.32f).sp
                    }
                    val result = textMeasurer.measure(
                        text = scaleAnnotated(text, ratio),
                        style = style.copy(
                            fontFamily = resolvedFontFamily,
                            fontSize = candidateSp.sp,
                            lineHeight = scaledLineHeight
                        ),
                        maxLines = maxLines,
                        softWrap = softWrap,
                        overflow = TextOverflow.Clip,
                        constraints = Constraints(maxWidth = maxWidthPx)
                    )
                    !result.hasVisualOverflow && result.lineCount <= maxLines
                }
            }
        }

        val finalRatio = if (baseMaxSp > 0f) fittedSp / baseMaxSp else 1f
        val finalLineHeight = if (style.lineHeight.isSpecified && baseMaxSp > 0f) {
            (style.lineHeight.value * finalRatio).coerceAtLeast(fittedSp * 1.28f).sp
        } else {
            (fittedSp * 1.32f).sp
        }

        val resolvedColor = style.color.takeOrElse {
            LocalContentColor.current.takeOrElse { MaterialTheme.colorScheme.onSurface }
        }

        Text(
            text = remember(text, finalRatio) { scaleAnnotated(text, finalRatio) },
            color = resolvedColor,
            style = style.copy(
                fontFamily = resolvedFontFamily,
                fontSize = fittedSp.sp,
                lineHeight = finalLineHeight
            ),
            textAlign = textAlign,
            maxLines = maxLines,
            softWrap = softWrap,
            overflow = overflow
        )
    }
}

/**
 * Apple Stocks / Revolut Style Rolling-Digit Odometer Money Display:
 * Animates each Persian digit slot vertically with spring deceleration whenever the balance,
 * currency unit (تومان / ریال), or privacy mask changes, while measuring exact container width
 * so 12–15 digit Rial values NEVER overflow or clip on compact 320dp phones!
 */
@Composable
fun AnimatedOdometerMoneyText(
    amountToman: Long,
    unit: CurrencyUnit,
    privacyMasked: Boolean,
    style: TextStyle = MaterialTheme.typography.displayLarge,
    numberColor: Color = MaterialTheme.colorScheme.onSurface,
    unitColor: Color = numberColor.copy(alpha = 0.80f),
    minFontSize: TextUnit = 15.sp,
    modifier: Modifier = Modifier
) {
    val formattedDigits = remember(amountToman, unit, privacyMasked) {
        if (privacyMasked) {
            "•••,•••"
        } else {
            val displayVal = amountToman * unit.multiplierFromToman
            PersianCurrencyEngine.formatNumberWithCommas(displayVal, usePersianDigits = true)
        }
    }
    var previousAmount by remember { mutableStateOf(amountToman) }
    val rollingUp = amountToman >= previousAmount
    LaunchedEffect(amountToman) {
        previousAmount = amountToman
    }

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val resolvedFontFamily = style.fontFamily ?: com.example.ui.theme.EstedadFontFamily
    val baseFontSizeSp = if (style.fontSize.isSpecified) style.fontSize.value else 32f
    val minFontSizeSp = minFontSize.value.coerceAtMost(baseFontSizeSp)

    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = if (constraints.hasBoundedWidth && constraints.maxWidth > 0 && constraints.maxWidth < Constraints.Infinity) {
            // Reserve 10.dp safety buffer for digit-slot kerning & spacer between digits and unit
            (constraints.maxWidth - with(density) { 10.dp.roundToPx() }).coerceAtLeast(48)
        } else {
            -1
        }

        val fittedSp = remember(
            formattedDigits,
            unit,
            maxWidthPx,
            density.density,
            density.fontScale,
            baseFontSizeSp,
            minFontSizeSp
        ) {
            if (maxWidthPx <= 0) {
                baseFontSizeSp
            } else {
                findFittingFontSizeSp(
                    maxFontSizeSp = baseFontSizeSp,
                    minFontSizeSp = minFontSizeSp
                ) { candidateSp ->
                    val candidateUnitSp = (candidateSp * 0.48f).coerceAtLeast(10f)
                    val probe = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                fontFamily = resolvedFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = candidateSp.sp
                            )
                        ) {
                            append(formattedDigits)
                        }
                        append(" ")
                        withStyle(
                            SpanStyle(
                                fontFamily = resolvedFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = candidateUnitSp.sp
                            )
                        ) {
                            append(unit.labelFa)
                        }
                    }
                    val res = textMeasurer.measure(
                        text = probe,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        constraints = Constraints(maxWidth = maxWidthPx)
                    )
                    !res.hasVisualOverflow
                }
            }
        }

        val ratio = if (baseFontSizeSp > 0f) fittedSp / baseFontSizeSp else 1f
        val adaptiveStyle = style.copy(
            fontFamily = resolvedFontFamily,
            fontSize = fittedSp.sp,
            lineHeight = if (style.lineHeight.isSpecified) {
                (style.lineHeight.value * ratio).coerceAtLeast(fittedSp * 1.24f).sp
            } else {
                (fittedSp * 1.28f).sp
            }
        )
        val unitFontSize = (fittedSp * 0.48f).coerceAtLeast(10f).sp

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Keep numeric character order strictly LTR so thousands separators never flip in RTL
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    formattedDigits.forEachIndexed { idx, ch ->
                        val slotKey = formattedDigits.length - idx
                        androidx.compose.runtime.key(slotKey) {
                            AnimatedContent(
                                targetState = ch,
                                transitionSpec = {
                                    val dir = if (rollingUp) 1 else -1
                                    (slideInVertically(
                                        animationSpec = spring(
                                            dampingRatio = 0.78f,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    ) { h -> dir * (h * 0.65f).roundToInt() } + fadeIn(tween(180))) togetherWith
                                        (slideOutVertically(
                                            animationSpec = tween(150, easing = FastOutSlowInEasing)
                                        ) { h -> -dir * (h * 0.55f).roundToInt() } + fadeOut(tween(130)))
                                },
                                label = "OdometerDigit_$slotKey"
                            ) { charState ->
                                Text(
                                    text = charState.toString(),
                                    style = adaptiveStyle,
                                    fontWeight = FontWeight.Black,
                                    color = numberColor,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
            Text(
                text = unit.labelFa,
                style = adaptiveStyle.copy(
                    fontSize = unitFontSize,
                    fontWeight = FontWeight.Medium
                ),
                color = unitColor,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
    }
}

/**
 * High-scannability Persian financial figure renderer with zero-flicker container-aware dynamic auto-sizing.
 * Automatically shrinks both digits and unit proportionally down to [minFontSize] so figures NEVER chop off
 * digits with `...` even in 14-digit Rial mode or on compact 320dp screens!
 */
@Composable
fun FormattedMoneyText(
    amountToman: Long,
    unit: CurrencyUnit,
    privacyMasked: Boolean,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    numberColor: Color = MaterialTheme.colorScheme.onSurface,
    unitColor: Color = numberColor.copy(alpha = 0.72f),
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    minFontSize: TextUnit = 9.sp,
    textAlign: TextAlign? = null
) {
    val digitsPart = if (privacyMasked) {
        "•••,•••"
    } else {
        val displayVal = amountToman * unit.multiplierFromToman
        ltrIsolateDigits(PersianCurrencyEngine.formatNumberWithCommas(displayVal, usePersianDigits = true))
    }

    val baseFontSizeSp = if (style.fontSize.isSpecified) style.fontSize.value else 15f
    val unitFontSize = (baseFontSizeSp * 0.62f).coerceAtLeast(9.5f).sp
    val resolvedFontFamily = style.fontFamily ?: com.example.ui.theme.EstedadFontFamily

    val annotated = remember(digitsPart, unit, numberColor, unitColor, baseFontSizeSp, resolvedFontFamily) {
        buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = numberColor,
                    fontFamily = resolvedFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = baseFontSizeSp.sp
                )
            ) {
                append(digitsPart)
            }
            append(" ")
            withStyle(
                SpanStyle(
                    color = unitColor,
                    fontFamily = resolvedFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = unitFontSize
                )
            ) {
                append(unit.labelFa)
            }
        }
    }

    TarazinoAutoSizeText(
        text = annotated,
        style = style.copy(fontFamily = resolvedFontFamily, fontSize = baseFontSizeSp.sp),
        textAlign = textAlign,
        maxLines = maxLines,
        minFontSize = minFontSize,
        maxFontSize = baseFontSizeSp.sp,
        modifier = modifier
    )
}

/**
 * Compact Persian short amount for charts & tight badges (e.g., "۱۵.۴ م" or "۸۵۰ ه").
 */
fun formatCompactPersianToman(amountToman: Long, privacyMasked: Boolean): String {
    if (privacyMasked) return "•••"
    val isNeg = amountToman < 0L
    val absVal = kotlin.math.abs(amountToman)
    val raw = when {
        absVal >= 1_000_000_000_000L -> {
            val t = absVal / 1_000_000_000_000.0
            String.format(java.util.Locale.US, "%.1f", t).removeSuffix(".0") + " همت"
        }
        absVal >= 1_000_000_000L -> {
            val b = absVal / 1_000_000_000.0
            String.format(java.util.Locale.US, "%.1f", b).removeSuffix(".0") + " م‌د"
        }
        absVal >= 1_000_000L -> {
            val m = absVal / 1_000_000.0
            String.format(java.util.Locale.US, "%.1f", m).removeSuffix(".0") + " م"
        }
        absVal >= 1_000L -> {
            "${absVal / 1_000L} ه"
        }
        else -> absVal.toString()
    }
    val signed = if (isNeg && absVal > 0L) "−$raw" else raw
    return PersianCurrencyEngine.toPersianDigits(signed)
}

/**
 * Editorial fintech section header with tinted rounded icon badge, auto-fitting title/subtitle, and optional trailing action.
 */
@Composable
fun TarazinoSectionHeader(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            TarazinoIconBadge(
                icon = icon,
                accentColor = accentColor,
                size = 38.dp,
                iconSize = 19.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                TarazinoAutoSizeText(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    minFontSize = 11.5.sp
                )
                if (!subtitle.isNullOrBlank()) {
                    TarazinoAutoSizeText(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        minFontSize = 9.5.sp
                    )
                }
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

/**
 * Tinted squircle icon container with subtle specular inner border.
 */
@Composable
fun TarazinoIconBadge(
    icon: ImageVector,
    accentColor: Color,
    size: Dp = 42.dp,
    iconSize: Dp = 22.dp,
    cornerRadius: Dp = 13.dp,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    listOf(
                        accentColor.copy(alpha = 0.25f),
                        accentColor.copy(alpha = 0.08f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.36f),
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * BluBank / Revolut style Quick Action Squircle Tile with dynamic auto-fitting Persian label.
 */
@Composable
fun TarazinoQuickActionTile(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    badgeCount: Int = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTarazinoColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .tarazinoPressScale(scaleDown = 0.91f, onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accentColor.copy(alpha = if (ext.isDark) 0.26f else 0.16f),
                                accentColor.copy(alpha = if (ext.isDark) 0.08f else 0.05f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                accentColor.copy(alpha = 0.62f),
                                accentColor.copy(alpha = 0.15f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            if (badgeCount > 0) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(ext.danger)
                        .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape)
                ) {
                    Text(
                        text = PersianCurrencyEngine.toPersianDigits(badgeCount.coerceAtMost(9).toString()),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        TarazinoAutoSizeText(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            minFontSize = 8.5.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Compact status pill badge with subtle tinted background, crisp hairline ring, and auto-sizing label.
 */
@Composable
fun TarazinoStatusPill(
    text: String,
    accentColor: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = accentColor.copy(alpha = 0.14f),
        border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.36f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )
            }
            TarazinoAutoSizeText(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
                minFontSize = 8.5.sp
            )
        }
    }
}

/**
 * Unified sliding-thumb segmented pill control with spring physics, haptic tick, and auto-sizing labels.
 */
@Composable
fun TarazinoSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    testTagPrefix: String? = null
) {
    if (options.isEmpty()) return
    val ext = LocalTarazinoColors.current
    val haptics = LocalHapticFeedback.current
    val safeIndex = selectedIndex.coerceIn(0, options.lastIndex)
    val animatedIndex by animateFloatAsState(
        targetValue = safeIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.80f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "SegThumbSlide"
    )
    val outerRadius = if (compact) 13.dp else 16.dp
    val thumbRadius = if (compact) 10.dp else 12.dp
    val barHeight = if (compact) 36.dp else 42.dp

    Surface(
        shape = RoundedCornerShape(outerRadius),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (ext.isDark) 0.58f else 0.78f),
        border = BorderStroke(0.9.dp, ext.cardBorderTop),
        modifier = modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.5.dp)
                .height(barHeight)
        ) {
            val segWidth = maxWidth / options.size

            Box(
                modifier = Modifier
                    .offset(x = segWidth * animatedIndex)
                    .width(segWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(thumbRadius))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                ext.fabStart.copy(alpha = 0.96f),
                                ext.fabMid
                            )
                        )
                    )
                    .border(
                        width = 0.75.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.42f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(thumbRadius)
                    )
            )

            Row(modifier = Modifier.fillMaxSize()) {
                options.forEachIndexed { idx, label ->
                    val selected = idx == safeIndex
                    val fg by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "SegFg_$idx"
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(thumbRadius))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (idx != safeIndex) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectIndex(idx)
                                }
                            }
                            .padding(horizontal = 4.dp)
                            .then(if (testTagPrefix != null) Modifier.testTag("${testTagPrefix}_$idx") else Modifier)
                    ) {
                        TarazinoAutoSizeText(
                            text = label,
                            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = fg,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            minFontSize = 8.5.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Branded circular monogram avatar for Iranian counterparties.
 */
@Composable
fun CounterpartyMonogramAvatar(
    name: String,
    color: Color,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val clean = name.trim().substringBefore(" (").trim()
    val initial = clean.take(1).ifBlank { "ت" }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        color.copy(alpha = 0.30f),
                        color.copy(alpha = 0.12f)
                    )
                )
            )
            .border(1.2.dp, color.copy(alpha = 0.62f), CircleShape)
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
    }
}

/**
 * Spring-animated, RTL-native luminous progress bar with soft horizontal gradient sheen.
 */
@Composable
fun TarazinoLuminousProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
    height: Dp = 7.dp
) {
    val target = progress.coerceIn(0f, 1f)
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val animatedProgress by animateFloatAsState(
        targetValue = if (started) target else 0f,
        animationSpec = spring(
            dampingRatio = 0.84f,
            stiffness = Spring.StiffnessLow
        ),
        label = "LuminousProgress"
    )
    val animColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "LuminousProgressColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(trackColor)
    ) {
        if (animatedProgress > 0.005f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress.coerceIn(0.015f, 1f))
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                animColor,
                                animColor.copy(alpha = 0.82f)
                            )
                        )
                    )
            )
        }
    }
}

