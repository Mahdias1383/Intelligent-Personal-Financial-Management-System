package com.example.core

import android.hardware.biometrics.BiometricManager
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricPromptAuthenticatorTest {

    @Test
    fun `mapBiometricStatus returns READY when BiometricManager reports BIOMETRIC_SUCCESS`() {
        val status = BiometricPromptAuthenticator.mapBiometricStatus(
            sdkInt = Build.VERSION_CODES.R,
            biometricManagerResult = BiometricManager.BIOMETRIC_SUCCESS,
            hasFingerprintFeature = true,
            hasFaceFeature = false
        )

        assertEquals(BiometricHardwareStatus.READY, status)
        assertTrue(status.isSupported)
        assertTrue(status.isReadyToAuthenticate)
    }

    @Test
    fun `mapBiometricStatus returns NOT_ENROLLED when hardware is present but no biometrics enrolled`() {
        val status = BiometricPromptAuthenticator.mapBiometricStatus(
            sdkInt = Build.VERSION_CODES.R,
            biometricManagerResult = BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
            hasFingerprintFeature = true,
            hasFaceFeature = true
        )

        assertEquals(BiometricHardwareStatus.NOT_ENROLLED, status)
        assertTrue(status.isSupported)
        assertFalse(status.isReadyToAuthenticate)
    }

    @Test
    fun `mapBiometricStatus returns NO_HARDWARE when neither fingerprint nor face hardware exists`() {
        val status = BiometricPromptAuthenticator.mapBiometricStatus(
            sdkInt = Build.VERSION_CODES.R,
            biometricManagerResult = BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            hasFingerprintFeature = false,
            hasFaceFeature = false
        )

        assertEquals(BiometricHardwareStatus.NO_HARDWARE, status)
        assertFalse(status.isSupported)
        assertFalse(status.isReadyToAuthenticate)
    }

    @Test
    fun `mapBiometricStatus falls back to feature flags on pre-Q API levels`() {
        val readyStatus = BiometricPromptAuthenticator.mapBiometricStatus(
            sdkInt = Build.VERSION_CODES.P,
            biometricManagerResult = null,
            hasFingerprintFeature = true,
            hasFaceFeature = false
        )
        assertEquals(BiometricHardwareStatus.READY, readyStatus)

        val noHwStatus = BiometricPromptAuthenticator.mapBiometricStatus(
            sdkInt = Build.VERSION_CODES.P,
            biometricManagerResult = null,
            hasFingerprintFeature = false,
            hasFaceFeature = false
        )
        assertEquals(BiometricHardwareStatus.NO_HARDWARE, noHwStatus)
    }
}
