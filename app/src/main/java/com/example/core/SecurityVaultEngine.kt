package com.example.core

import java.security.MessageDigest

object SecurityVaultEngine {

    const val SECURITY_SALT = "TARAZINO_VAULT_SALT_2026"

    /**
     * Computes salted SHA-256 hex digest for PIN or security recovery answers.
     */
    fun hashWithSalt(rawSecret: String): String {
        val normalized = PersianCurrencyEngine.toEnglishDigits(rawSecret.trim().lowercase())
        val payload = "$SECURITY_SALT:$normalized"
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(payload.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(inputPin: String, storedHash: String): Boolean {
        if (storedHash.isBlank()) return true
        return hashWithSalt(inputPin) == storedHash
    }

    fun verifyRecoveryAnswer(inputAnswer: String, storedRecoveryHash: String): Boolean {
        if (storedRecoveryHash.isBlank()) return false
        return hashWithSalt(inputAnswer) == storedRecoveryHash
    }

    fun isValidPinFormat(pin: String): Boolean {
        val clean = PersianCurrencyEngine.toEnglishDigits(pin.trim())
        return (clean.length == 4 || clean.length == 6) && clean.all { it.isDigit() }
    }
}
