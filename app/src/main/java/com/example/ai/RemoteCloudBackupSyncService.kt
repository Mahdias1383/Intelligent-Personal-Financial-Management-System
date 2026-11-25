package com.example.ai

import com.example.domain.ConnectivityPrivacyEngine
import com.example.domain.OnlineServiceInputsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class CloudSyncUploadOutcome(
    val success: Boolean,
    val httpStatusCode: Int,
    val messageFa: String
)

/**
 * Executes an opt-in HTTPS JSON backup upload to the user-configured remote server/webhook.
 * Only invoked when the user explicitly enables Cloud Sync and configures a validated HTTPS URL
 * and Bearer token in the Connectivity & Privacy settings screen.
 */
class RemoteCloudBackupSyncService {

    suspend fun uploadEncryptedBackupPayload(
        inputs: OnlineServiceInputsState,
        backupJson: String,
        timestampJalali: String
    ): CloudSyncUploadOutcome = withContext(Dispatchers.IO) {
        val validation = ConnectivityPrivacyEngine.validateCloudSyncInputs(inputs)
        if (!validation.isValid) {
            return@withContext CloudSyncUploadOutcome(
                success = false,
                httpStatusCode = 0,
                messageFa = validation.errorFa ?: "ورودی‌های سرور همگام‌سازی معتبر نیست."
            )
        }

        val endpoint = inputs.cloudSyncEndpointUrl.trim()
        val token = inputs.cloudSyncAuthToken.trim()
        val workspace = inputs.cloudSyncWorkspaceLabel.trim().ifBlank { "tarazino-android-device" }

        val envelope = JSONObject().apply {
            put("app", "Tarazino")
            put("schemaVersion", 1)
            put("workspaceLabel", workspace)
            put("syncedDateJalali", timestampJalali)
            put("backupPayload", JSONObject(backupJson))
        }.toString()

        withTimeout(30_000L) {
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
                doOutput = true
                instanceFollowRedirects = false
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("X-Tarazino-Workspace", workspace)
            }
            try {
                connection.outputStream.use { os ->
                    os.write(envelope.toByteArray(Charsets.UTF_8))
                }
                val status = connection.responseCode
                if (status in 200..299) {
                    CloudSyncUploadOutcome(
                        success = true,
                        httpStatusCode = status,
                        messageFa = "پشتیبان کامل با موفقیت به سرور اختصاصی شما ارسال و همگام‌سازی شد."
                    )
                } else {
                    CloudSyncUploadOutcome(
                        success = false,
                        httpStatusCode = status,
                        messageFa = "سرور همگام‌سازی با کد خطای $status پاسخ داد. آدرس سرور و توکن احراز هویت را بررسی کنید."
                    )
                }
            } finally {
                connection.disconnect()
            }
        }
    }
}
