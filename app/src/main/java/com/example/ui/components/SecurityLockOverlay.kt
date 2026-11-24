package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.BiometricAuthResult
import com.example.core.BiometricPromptAuthenticator
import com.example.core.PersianCurrencyEngine
import com.example.ui.theme.LocalTarazinoColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SecurityLockOverlay(
    pinLength: Int,
    biometricEnabled: Boolean,
    recoveryQuestion: String,
    hasRecoveryConfigured: Boolean,
    onVerifyPin: (String) -> Boolean,
    onBiometricUnlock: () -> Unit,
    onVerifyRecoveryAnswer: (String) -> Boolean
) {
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showRecoveryModal by remember { mutableStateOf(false) }
    var recoveryInput by remember { mutableStateOf("") }
    var showBiometricTouchSheet by remember { mutableStateOf(false) }
    var biometricFallbackReason by remember { mutableStateOf<String?>(null) }

    val biometricHardwareStatus = remember(context) {
        BiometricPromptAuthenticator.queryHardwareStatus(context)
    }

    fun triggerNativeOrFallbackBiometricPrompt() {
        errorMessage = null
        BiometricPromptAuthenticator.authenticateWithBiometricPrompt(
            context = context,
            onResult = { result ->
                when (result) {
                    is BiometricAuthResult.Success -> {
                        showBiometricTouchSheet = false
                        onBiometricUnlock()
                    }
                    is BiometricAuthResult.FallbackToTouchSheet -> {
                        biometricFallbackReason = result.reasonFa
                        showBiometricTouchSheet = true
                    }
                    is BiometricAuthResult.CancelledOrFailed -> {
                        errorMessage = result.errorMessageFa
                    }
                }
            }
        )
    }

    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled && biometricHardwareStatus.isReadyToAuthenticate) {
            triggerNativeOrFallbackBiometricPrompt()
        }
    }

    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun handleDigitPress(digit: String) {
        if (enteredPin.length < pinLength) {
            val next = enteredPin + digit
            enteredPin = next
            errorMessage = null
            if (next.length == pinLength) {
                val ok = onVerifyPin(next)
                if (!ok) {
                    errorMessage = "رمز پین واردشده نادرست است. دوباره تلاش کنید."
                    enteredPin = ""
                    scope.launch {
                        for (delta in listOf(-18f, 18f, -12f, 12f, 0f)) {
                            shakeOffset.animateTo(delta, tween(45))
                        }
                    }
                }
            }
        }
    }

    val ext = LocalTarazinoColors.current
    val primaryAccent = MaterialTheme.colorScheme.primary
    val lockOnSurface = MaterialTheme.colorScheme.onSurface
    val lockOnMuted = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxSize()
            .tarazinoAmbientBackdrop(
                backgroundColor = ext.lockBackdropStart,
                auroraPrimary = ext.auroraPrimary.copy(alpha = 0.28f),
                auroraSecondary = ext.auroraSecondary.copy(alpha = 0.22f)
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 18.dp)
            .testTag("security_lock_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Concentric Halo Vault Emblem Shield
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                primaryAccent.copy(alpha = 0.26f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    primaryAccent.copy(alpha = 0.22f),
                                    primaryAccent.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .border(1.5.dp, primaryAccent.copy(alpha = 0.65f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "قفل خزانه امنیتی ترازینو",
                        tint = primaryAccent,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            TarazinoAutoSizeText(
                text = "خزانه امنیتی ترازینو قفل است",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = lockOnSurface,
                maxLines = 1,
                minFontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            TarazinoAutoSizeText(
                text = "لطفاً رمز پین ${PersianCurrencyEngine.toPersianDigits(pinLength.toString())} رقمی خود را وارد نمایید",
                style = MaterialTheme.typography.bodyMedium,
                color = lockOnMuted,
                maxLines = 2,
                minFontSize = 10.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Spring-Animated PIN Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(24.dp)
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
            ) {
                for (i in 0 until pinLength) {
                    val filled = i < enteredPin.length
                    val dotSize by animateDpAsState(
                        targetValue = if (filled) 19.dp else 15.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "PinDotSize_$i"
                    )
                    val dotColor by animateColorAsState(
                        targetValue = if (filled) primaryAccent else MaterialTheme.colorScheme.surfaceVariant,
                        label = "PinDotColor_$i"
                    )
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(CircleShape)
                            .background(dotColor)
                            .border(
                                width = 1.5.dp,
                                color = if (filled) primaryAccent else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                TarazinoStatusPill(
                    text = errorMessage!!,
                    accentColor = ext.danger
                )
            } else {
                Spacer(modifier = Modifier.height(28.dp))
            }

            // Glassmorphic Persian Keypad (1..9, Biometric/0/Backspace)
            val rows = listOf(
                listOf("۱" to "1", "۲" to "2", "۳" to "3"),
                listOf("۴" to "4", "۵" to "5", "۶" to "6"),
                listOf("۷" to "7", "۸" to "8", "۹" to "9")
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                rows.forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        rowItems.forEach { (faDigit, enDigit) ->
                            KeypadCircleButton(
                                label = faDigit,
                                onClick = { handleDigitPress(enDigit) }
                            )
                        }
                    }
                }

                // Bottom Row: Biometric | 0 | Backspace
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(66.dp)
                            .clip(CircleShape)
                            .background(
                                if (biometricEnabled) primaryAccent.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            )
                            .border(
                                1.dp,
                                if (biometricEnabled) primaryAccent.copy(alpha = 0.5f)
                                else ext.cardBorderTop,
                                CircleShape
                            )
                            .clickable(enabled = biometricEnabled) {
                                triggerNativeOrFallbackBiometricPrompt()
                            }
                            .testTag("biometric_keypad_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "ورود بیومتریک اثر انگشت",
                            tint = if (biometricEnabled) primaryAccent else lockOnMuted.copy(alpha = 0.55f),
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    KeypadCircleButton(
                        label = "۰",
                        onClick = { handleDigitPress("0") }
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(66.dp)
                            .clip(CircleShape)
                            .background(
                                if (ext.isDark) Color.White.copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(1.dp, ext.cardBorderTop, CircleShape)
                            .clickable {
                                if (enteredPin.isNotEmpty()) {
                                    enteredPin = enteredPin.dropLast(1)
                                }
                            }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "پاک کردن رقم آخر",
                            tint = lockOnSurface,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            if (biometricEnabled) {
                OutlinedButton(
                    onClick = { triggerNativeOrFallbackBiometricPrompt() },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.45f)),
                    modifier = Modifier.testTag("biometric_prompt_unlock_btn")
                ) {
                    Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = primaryAccent)
                    Spacer(modifier = Modifier.size(8.dp))
                    TarazinoAutoSizeText(
                        text = "بازگشایی با اثر انگشت یا تشخیص چهره",
                        style = MaterialTheme.typography.labelLarge,
                        color = primaryAccent,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        minFontSize = 9.5.sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                TarazinoStatusPill(
                    text = biometricHardwareStatus.statusBadgeFa,
                    accentColor = if (biometricHardwareStatus.isReadyToAuthenticate) ext.positive else MaterialTheme.colorScheme.secondary
                )
            }

            if (hasRecoveryConfigured) {
                TextButton(onClick = { showRecoveryModal = true }) {
                    TarazinoAutoSizeText(
                        text = "رمز پین را فراموش کرده‌اید؟ بازیابی با سوال امنیتی",
                        style = MaterialTheme.typography.labelLarge,
                        color = ext.warning,
                        maxLines = 1,
                        minFontSize = 9.5.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showBiometricTouchSheet) {
        TarazinoRtlDialog(
            onDismissRequest = { showBiometricTouchSheet = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            TarazinoGlassCard(
                cornerRadius = 28.dp,
                accentBorderColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .widthIn(max = 420.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    TarazinoIconBadge(
                        icon = Icons.Default.Fingerprint,
                        accentColor = MaterialTheme.colorScheme.primary,
                        size = 56.dp,
                        iconSize = 30.dp,
                        cornerRadius = 18.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TarazinoAutoSizeText(
                        text = "احراز هویت بیومتریک ترازینو",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        minFontSize = 11.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "برای باز کردن قفل خزانه مالی، حسگر زیر را لمس نمایید:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        border = BorderStroke(1.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .size(92.dp)
                            .tarazinoPressScale(
                                scaleDown = 0.90f,
                                onClick = {
                                    showBiometricTouchSheet = false
                                    onBiometricUnlock()
                                }
                            )
                            .testTag("confirm_biometric_sensor_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "تایید حسگر بیومتریک",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showBiometricTouchSheet = false },
                            shape = RoundedCornerShape(13.dp),
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Text("انصراف", maxLines = 1)
                        }
                        Button(
                            onClick = {
                                showBiometricTouchSheet = false
                                onBiometricUnlock()
                            },
                            shape = RoundedCornerShape(13.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            TarazinoAutoSizeText(
                                text = "تایید اثر انگشت",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                minFontSize = 9.5.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRecoveryModal) {
        TarazinoRtlDialog(
            onDismissRequest = { showRecoveryModal = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            TarazinoGlassCard(
                cornerRadius = 26.dp,
                accentBorderColor = ext.warning,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 420.dp)
                    .imePadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp)
                ) {
                    TarazinoAutoSizeText(
                        text = "بازیابی امنیتی رمز عبور",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        minFontSize = 11.5.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = recoveryQuestion,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = recoveryInput,
                        onValueChange = { recoveryInput = it },
                        label = { Text("پاسخ سوال امنیتی", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showRecoveryModal = false },
                            shape = RoundedCornerShape(13.dp),
                            modifier = Modifier.weight(0.8f)
                        ) {
                            TarazinoAutoSizeText(
                                text = "انصراف",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                minFontSize = 9.5.sp
                            )
                        }
                        Button(
                            onClick = {
                                if (onVerifyRecoveryAnswer(recoveryInput)) {
                                    showRecoveryModal = false
                                } else {
                                    errorMessage = "پاسخ سوال امنیتی صحیح نیست."
                                    showRecoveryModal = false
                                }
                            },
                            shape = RoundedCornerShape(13.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            TarazinoAutoSizeText(
                                text = "بازگشایی قفل",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                minFontSize = 9.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadCircleButton(
    label: String,
    onClick: () -> Unit
) {
    val ext = LocalTarazinoColors.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(66.dp)
            .tarazinoPressScale(scaleDown = 0.88f, onClick = onClick)
            .clip(CircleShape)
            .background(
                if (ext.isDark) {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.14f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(ext.cardBorderTop, ext.cardBorderBottom)
                ),
                shape = CircleShape
            )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
