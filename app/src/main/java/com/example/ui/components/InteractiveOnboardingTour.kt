package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.PersianCurrencyEngine
import com.example.ui.InteractiveTourStep
import com.example.ui.TarazinoNavTab

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC060D10))
            .padding(16.dp)
            .testTag("interactive_onboarding_tour_overlay")
    ) {
        // Top Live Tab Spotlight Indicator showing movement across app sections
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF132229),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 36.dp)
                .border(1.5.dp, Color(0xFF10B981), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
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
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تور آموزشی گرافیکی و هوشمند ترازینو",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF34D399),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(
                        onClick = onSkipOrFinish,
                        modifier = Modifier.testTag("skip_tour_btn")
                    ) {
                        Text("رد کردن تور", color = Color(0xFFCBD5E1))
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "رد کردن تور آموزشی",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Visual Navigation Bar Map showing which tab the tour has moved to
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
                                    if (isTarget) Color(0xFF10B981) else Color(0xFF1E293B)
                                )
                                .padding(vertical = 6.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                text = tab.titleFa,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isTarget) FontWeight.Bold else FontWeight.Normal,
                                color = if (isTarget) Color(0xFF042F2E) else Color(0xFF94A3B8),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Main Animated Spotlight Coach-Mark Card
        AnimatedContent(
            targetState = step,
            label = "TourStepTransition",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        ) { currentStep ->
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111D24)),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF10B981), Color(0xFFF59E0B))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Graphical Header Art
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(128.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_tarazino_1794581824442),
                            contentDescription = "تصویر گرافیکی تور آموزشی ترازینو",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color(0xEE111D24))
                                    )
                                )
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF064E3B).copy(alpha = 0.92f),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = currentStep.badgeTitleFa,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF6EE7B7),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = currentStep.headlineFa,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentStep.descriptionFa,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFCBD5E1)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Feature Bullet Highlights
                        currentStep.highlightFeaturesFa.forEach { itemFa ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = itemFa,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFF1F5F9)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Step Dots + Navigation Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Step Dots
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                steps.forEachIndexed { idx, _ ->
                                    val active = idx == currentStepIndex
                                    Box(
                                        modifier = Modifier
                                            .height(8.dp)
                                            .width(if (active) 24.dp else 8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (active) Color(0xFF10B981) else Color(0xFF475569)
                                            )
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (currentStepIndex > 0) {
                                    OutlinedButton(
                                        onClick = onPreviousStep,
                                        modifier = Modifier.testTag("tour_prev_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "قبلی",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("قبلی")
                                    }
                                }

                                Button(
                                    onClick = if (isLastStep) onSkipOrFinish else onNextStep,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isLastStep) Color(0xFFF59E0B) else Color(0xFF10B981)
                                    ),
                                    modifier = Modifier.testTag("tour_next_or_finish_btn")
                                ) {
                                    Text(
                                        text = if (isLastStep) {
                                            "شروع کار با ترازینو"
                                        } else {
                                            "بخش بعدی (${PersianCurrencyEngine.toPersianDigits((currentStepIndex + 2).toString())}/${PersianCurrencyEngine.toPersianDigits(steps.size.toString())})"
                                        },
                                        color = Color(0xFF042F2E),
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (!isLastStep) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "بعدی",
                                            tint = Color(0xFF042F2E),
                                            modifier = Modifier.size(16.dp)
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
