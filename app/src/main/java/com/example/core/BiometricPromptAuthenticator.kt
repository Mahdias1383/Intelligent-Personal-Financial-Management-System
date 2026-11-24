package com.example.core

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat

/**
 * Hardware capability and enrollment status for biometric authentication
 * (Fingerprint, Face Unlock, or Iris).
 */
enum class BiometricHardwareStatus(
    val isSupported: Boolean,
    val isReadyToAuthenticate: Boolean,
    val statusBadgeFa: String,
    val detailDescriptionFa: String
) {
    READY(
        isSupported = true,
        isReadyToAuthenticate = true,
        statusBadgeFa = "حسگر بیومتریک آماده",
        detailDescriptionFa = "سنسور اثر انگشت / تشخیص چهره دستگاه فعال و آماده احراز هویت است."
    ),
    NOT_ENROLLED(
        isSupported = true,
        isReadyToAuthenticate = false,
        statusBadgeFa = "اثر انگشت ثبت نشده",
        detailDescriptionFa = "سخت‌افزار بیومتریک موجود است؛ می‌توانید از تنظیمات اندروید اثر انگشت یا چهره ثبت کنید."
    ),
    HARDWARE_UNAVAILABLE(
        isSupported = false,
        isReadyToAuthenticate = false,
        statusBadgeFa = "سنسور موقتاً در دسترس نیست",
        detailDescriptionFa = "حسگر بیومتریک دستگاه در حال حاضر مشغول یا غیرفعال است."
    ),
    NO_HARDWARE(
        isSupported = false,
        isReadyToAuthenticate = false,
        statusBadgeFa = "فاقد حسگر سخت‌افزاری",
        detailDescriptionFa = "این دستگاه/شبیه‌ساز فاقد حسگر سخت‌افزاری اثر انگشت است (پشتیبانی از حالت شبیه‌ساز امن)."
    )
}

/**
 * Result of a biometric authentication attempt via Android's [BiometricPrompt].
 */
sealed class BiometricAuthResult {
    data class Success(val methodDescriptionFa: String) : BiometricAuthResult()
    data class FallbackToTouchSheet(val reasonFa: String) : BiometricAuthResult()
    data class CancelledOrFailed(val errorMessageFa: String) : BiometricAuthResult()
}

/**
 * Coordinates real Android [BiometricPrompt] authentication (Fingerprint & Face Unlock)
 * to protect sensitive financial data in Tarazino, with graceful fallback when running
 * on an emulator or device without enrolled biometrics.
 */
object BiometricPromptAuthenticator {

    /**
     * Pure, unit-testable mapper from Android [BiometricManager] result codes and
     * package manager feature flags to [BiometricHardwareStatus].
     */
    fun mapBiometricStatus(
        sdkInt: Int,
        biometricManagerResult: Int?,
        hasFingerprintFeature: Boolean,
        hasFaceFeature: Boolean
    ): BiometricHardwareStatus {
        if (sdkInt >= Build.VERSION_CODES.Q && biometricManagerResult != null) {
            return when (biometricManagerResult) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricHardwareStatus.READY
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricHardwareStatus.NOT_ENROLLED
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricHardwareStatus.HARDWARE_UNAVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricHardwareStatus.NO_HARDWARE
                else -> if (hasFingerprintFeature || hasFaceFeature) {
                    BiometricHardwareStatus.NOT_ENROLLED
                } else {
                    BiometricHardwareStatus.NO_HARDWARE
                }
            }
        }
        return if (hasFingerprintFeature || hasFaceFeature) {
            BiometricHardwareStatus.READY
        } else {
            BiometricHardwareStatus.NO_HARDWARE
        }
    }

    /**
     * Queries the current device's biometric hardware and enrollment state.
     */
    fun queryHardwareStatus(context: Context): BiometricHardwareStatus {
        val pm = context.packageManager
        val hasFingerprint = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        val hasFace = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pm.hasSystemFeature(PackageManager.FEATURE_FACE)
        } else {
            false
        }

        val managerResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bm = context.getSystemService(BiometricManager::class.java)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bm?.canAuthenticate(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.BIOMETRIC_WEAK
                    )
                } else {
                    @Suppress("DEPRECATION")
                    bm?.canAuthenticate()
                }
            }.getOrNull()
        } else {
            null
        }

        return mapBiometricStatus(
            sdkInt = Build.VERSION.SDK_INT,
            biometricManagerResult = managerResult,
            hasFingerprintFeature = hasFingerprint,
            hasFaceFeature = hasFace
        )
    }

    /**
     * Launches the native Android [BiometricPrompt] dialog for fingerprint or face unlock
     * on API 28+ devices when hardware is ready, or invokes [onResult] with a structured
     * [BiometricAuthResult] so the UI can unlock or display the interactive biometric sheet.
     */
    fun authenticateWithBiometricPrompt(
        context: Context,
        titleFa: String = "احراز هویت بیومتریک ترازینو",
        subtitleFa: String = "بازگشایی قفل خزانه اطلاعات مالی",
        descriptionFa: String = "برای دسترسی به اطلاعات وام‌ها، اقساط و حساب‌ها، حسگر اثر انگشت یا تشخیص چهره را تایید کنید.",
        negativeButtonTextFa: String = "ورود با رمز پین",
        onResult: (BiometricAuthResult) -> Unit
    ) {
        val hwStatus = queryHardwareStatus(context)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || !hwStatus.isReadyToAuthenticate) {
            onResult(
                BiometricAuthResult.FallbackToTouchSheet(
                    reasonFa = hwStatus.detailDescriptionFa
                )
            )
            return
        }

        try {
            val executor = ContextCompat.getMainExecutor(context)
            val builder = BiometricPrompt.Builder(context)
                .setTitle(titleFa)
                .setSubtitle(subtitleFa)
                .setDescription(descriptionFa)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
            }

            builder.setNegativeButton(
                negativeButtonTextFa,
                executor
            ) { _, _ ->
                onResult(
                    BiometricAuthResult.CancelledOrFailed(
                        "احراز هویت بیومتریک لغو شد؛ می‌توانید رمز پین خود را وارد کنید."
                    )
                )
            }

            val prompt = builder.build()
            val cancellationSignal = CancellationSignal()

            prompt.authenticate(
                cancellationSignal,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                        onResult(
                            BiometricAuthResult.Success(
                                methodDescriptionFa = "احراز هویت بیومتریک (اثر انگشت / تشخیص چهره) با موفقیت تایید شد."
                            )
                        )
                    }

                    override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence?) {
                        super.onAuthenticationHelp(helpCode, helpString)
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onResult(
                            BiometricAuthResult.CancelledOrFailed(
                                "اثر انگشت یا چهره شناسایی نشد؛ دوباره تلاش کنید."
                            )
                        )
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        super.onAuthenticationError(errorCode, errString)
                        if (errorCode == BiometricPrompt.BIOMETRIC_ERROR_NO_BIOMETRICS ||
                            errorCode == BiometricPrompt.BIOMETRIC_ERROR_HW_NOT_PRESENT ||
                            errorCode == BiometricPrompt.BIOMETRIC_ERROR_HW_UNAVAILABLE
                        ) {
                            onResult(
                                BiometricAuthResult.FallbackToTouchSheet(
                                    reasonFa = errString?.toString() ?: hwStatus.detailDescriptionFa
                                )
                            )
                        } else {
                            onResult(
                                BiometricAuthResult.CancelledOrFailed(
                                    errString?.toString() ?: "خطا در احراز هویت بیومتریک"
                                )
                            )
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            onResult(
                BiometricAuthResult.FallbackToTouchSheet(
                    reasonFa = hwStatus.detailDescriptionFa
                )
            )
        }
    }

    fun findActivity(context: Context): Activity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }
}
