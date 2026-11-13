package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.core.PersianCurrencyEngine
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
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showRecoveryModal by remember { mutableStateOf(false) }
    var recoveryInput by remember { mutableStateOf("") }
    var showBiometricTouchSheet by remember { mutableStateOf(false) }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF061014), Color(0xFF0F1E26), Color(0xFF091318))
                )
            )
            .padding(24.dp)
            .testTag("security_lock_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Emblem Shield
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981).copy(alpha = 0.16f))
                    .border(1.5.dp, Color(0xFF10B981), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "قفل خزانه امنیتی ترازینو",
                    tint = Color(0xFF34D399),
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "خزانه امنیتی ترازینو قفل است",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "لطفاً رمز پین ${PersianCurrencyEngine.toPersianDigits(pinLength.toString())} رقمی خود را وارد نمایید",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Animated PIN Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
            ) {
                for (i in 0 until pinLength) {
                    val filled = i < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (filled) Color(0xFF10B981) else Color.White.copy(alpha = 0.18f)
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (filled) Color(0xFF34D399) else Color(0xFF64748B),
                                shape = CircleShape
                            )
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFEF4444)
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
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                if (biometricEnabled) Color(0xFF10B981).copy(alpha = 0.2f)
                                else Color.White.copy(alpha = 0.05f)
                            )
                            .clickable(enabled = biometricEnabled) {
                                showBiometricTouchSheet = true
                            }
                            .testTag("biometric_keypad_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "ورود بیومتریک اثر انگشت",
                            tint = if (biometricEnabled) Color(0xFF34D399) else Color(0xFF475569),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    KeypadCircleButton(
                        label = "۰",
                        onClick = { handleDigitPress("0") }
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable {
                                if (enteredPin.isNotEmpty()) {
                                    enteredPin = enteredPin.dropLast(1)
                                }
                            }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "پاک کردن رقم آخر",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (biometricEnabled) {
                OutlinedButton(
                    onClick = { showBiometricTouchSheet = true }
                ) {
                    Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = Color(0xFF34D399))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("لمس سنسور اثر انگشت / تشخیص چهره", color = Color(0xFF34D399))
                }
            }

            if (hasRecoveryConfigured) {
                TextButton(onClick = { showRecoveryModal = true }) {
                    Text("رمز پین را فراموش کرده‌اید؟ بازیابی با سوال امنیتی", color = Color(0xFFFBBF24))
                }
            }
        }
    }

    if (showBiometricTouchSheet) {
        AlertDialog(
            onDismissRequest = { showBiometricTouchSheet = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "اسکنر اثر انگشت",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
            },
            title = {
                Text("احراز هویت بیومتریک ترازینو", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("برای باز کردن قفل خزانه مالی، حسگر اثر انگشت زیر را لمس نمایید:")
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .size(88.dp)
                            .clickable {
                                showBiometricTouchSheet = false
                                onBiometricUnlock()
                            }
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBiometricTouchSheet = false
                        onBiometricUnlock()
                    }
                ) {
                    Text("تایید اثر انگشت")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBiometricTouchSheet = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    if (showRecoveryModal) {
        AlertDialog(
            onDismissRequest = { showRecoveryModal = false },
            title = { Text("بازیابی امنیتی رمز عبور", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = recoveryQuestion,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = recoveryInput,
                        onValueChange = { recoveryInput = it },
                        label = { Text("پاسخ سوال امنیتی") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (onVerifyRecoveryAnswer(recoveryInput)) {
                            showRecoveryModal = false
                        } else {
                            errorMessage = "پاسخ سوال امنیتی صحیح نیست."
                            showRecoveryModal = false
                        }
                    }
                ) {
                    Text("بازگشایی قفل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecoveryModal = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
private fun KeypadCircleButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.09f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
