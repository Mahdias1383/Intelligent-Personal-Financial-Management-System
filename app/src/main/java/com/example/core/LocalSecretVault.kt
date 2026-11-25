package com.example.core

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed Android Keystore (AES-256-GCM) vault for encrypting optional user-entered
 * online credentials (custom AI API keys and Cloud Sync Bearer tokens) at rest on the device.
 * Secrets stored here are isolated from standard preferences and are never included in JSON backups.
 */
class LocalSecretVault(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun saveSecret(key: String, plainValue: String) {
        val trimmed = plainValue.trim()
        if (trimmed.isEmpty()) {
            prefs.edit().remove(key).apply()
            return
        }
        val encoded = encryptToBase64(trimmed)
        prefs.edit().putString(key, encoded).apply()
    }

    fun readSecret(key: String): String {
        val stored = prefs.getString(key, null) ?: return ""
        return decryptFromBase64(stored)
    }

    fun clearAllSecrets() {
        prefs.edit().clear().apply()
    }

    private fun getOrCreateAesKey(): SecretKey? {
        return runCatching {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            if (existing != null) return existing

            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGen.init(spec)
            keyGen.generateKey()
        }.getOrNull()
    }

    private fun encryptToBase64(plainText: String): String {
        val secretKey = getOrCreateAesKey() ?: return ""
        return runCatching {
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(1 + iv.size + ciphertext.size)
            combined[0] = iv.size.toByte()
            System.arraycopy(iv, 0, combined, 1, iv.size)
            System.arraycopy(ciphertext, 0, combined, 1 + iv.size, ciphertext.size)
            ENC_PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
        }.getOrDefault("")
    }

    private fun decryptFromBase64(stored: String): String {
        if (!stored.startsWith(ENC_PREFIX)) return ""
        val secretKey = getOrCreateAesKey() ?: return ""
        return runCatching {
            val payload = Base64.decode(stored.removePrefix(ENC_PREFIX), Base64.NO_WRAP)
            if (payload.size < 14) return ""
            val ivLength = payload[0].toInt() and 0xFF
            if (ivLength !in 12..16 || payload.size <= 1 + ivLength) return ""
            val iv = payload.copyOfRange(1, 1 + ivLength)
            val ciphertext = payload.copyOfRange(1 + ivLength, payload.size)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        }.getOrDefault("")
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "tarazino_online_secret_vault_aes_v1"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val PREFS_NAME = "tarazino_online_secrets_v1"
        private const val ENC_PREFIX = "enc_v1:"

        const val KEY_CUSTOM_AI_API_KEY = "custom_ai_api_key"
        const val KEY_CLOUD_SYNC_AUTH_TOKEN = "cloud_sync_auth_token"
    }
}
