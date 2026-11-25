package com.example.ai

import com.example.core.CurrencyUnit
import com.example.domain.AiProviderMode
import com.example.domain.ConnectivityPrivacyEngine
import com.example.domain.DailyAiFinancialTipEngine
import com.example.domain.DailyFinancialTipResult
import com.example.domain.OnlineServiceInputsState
import com.example.domain.SpendingAndDebtSnapshot
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Generates personalized daily Persian financial tips from the user's spending and debt profile.
 * Supports:
 * 1) Built-in Firebase AI Logic (keyless Gemini via App Check)
 * 2) User-configured Custom Gemini API Key & Model (HTTPS REST, header-based key auth)
 * 3) User-configured OpenAI-compatible HTTPS Endpoint, Model & Bearer API Key
 */
class FirebaseAiFinancialTipService {

    private val tipResponseSchema = Schema.obj(
        properties = mapOf(
            "headlineFa" to Schema.string(
                description = "Short, high-impact Persian headline (5 to 10 words) summarizing today's financial insight"
            ),
            "spendingPatternInsightFa" to Schema.string(
                description = "1 to 2 concise Persian sentences analyzing the user's recent spending patterns, essential vs discretionary balance, and cashflow"
            ),
            "dailyTipFa" to Schema.string(
                description = "2 to 3 actionable Persian sentences giving a personalized daily financial tip tailored to the user's specific debt profile"
            ),
            "concreteActionStepFa" to Schema.string(
                description = "One concrete, immediately executable Persian action step for today"
            ),
            "focusBadgeFa" to Schema.string(
                description = "Short 2 to 3 word Persian category pill such as کاهش سود بدهی or کنترل مخارج جاری"
            ),
            "estimatedImpactFa" to Schema.string(
                description = "Concise Persian estimate of the practical savings or risk reduction benefit"
            ),
            "urgencyTone" to Schema.enumeration(
                values = listOf("PROTECTIVE", "OPTIMIZATION", "MOMENTUM"),
                description = "PROTECTIVE if overdue debt or high DTI, OPTIMIZATION for interest/expense savings, MOMENTUM if healthy trajectory"
            )
        )
    )

    // Lazy initialization ensures Firebase.ai is only evaluated upon first inference after App Check is wired.
    private val generativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = tipResponseSchema
                temperature = 0.6f
            }
        )
    }

    suspend fun generateDailyFinancialTip(
        snapshot: SpendingAndDebtSnapshot,
        currencyUnit: CurrencyUnit,
        inputs: OnlineServiceInputsState = ConnectivityPrivacyEngine.defaultOnlineInputs()
    ): DailyFinancialTipResult {
        val basePrompt = DailyAiFinancialTipEngine.buildGeminiPrompt(
            snapshot = snapshot,
            currencyUnit = currencyUnit
        )

        val rawJson = when (inputs.aiProviderMode) {
            AiProviderMode.FIREBASE_AI_BUILTIN -> {
                val response = withTimeout(60_000L) {
                    generativeModel.generateContent(basePrompt)
                }
                response.text?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("پاسخ خالی از مدل هوش مصنوعی دریافت شد.")
            }

            AiProviderMode.CUSTOM_GEMINI_API -> {
                callCustomGeminiRestEndpoint(
                    prompt = buildExplicitJsonSchemaPrompt(basePrompt),
                    inputs = inputs
                )
            }

            AiProviderMode.OPENAI_COMPATIBLE_ENDPOINT -> {
                callOpenAiCompatibleEndpoint(
                    prompt = buildExplicitJsonSchemaPrompt(basePrompt),
                    inputs = inputs
                )
            }
        }

        return parseTipJson(rawJson = rawJson, snapshot = snapshot)
    }

    private fun buildExplicitJsonSchemaPrompt(basePrompt: String): String {
        return """
            $basePrompt

            پاسخ را فقط و فقط به صورت یک شیء JSON معتبر (بدون Markdown یا توضیح اضافه) با دقیقاً این کلیدها برگردان:
            {
              "headlineFa": "تیتر کوتاه ۵ تا ۱۰ کلمه‌ای فارسی",
              "spendingPatternInsightFa": "تحلیل کوتاه الگوی مخارج ۱ تا ۲ جمله فارسی",
              "dailyTipFa": "نکته مالی روزانه کاربردی ۲ تا ۳ جمله فارسی",
              "concreteActionStepFa": "یک اقدام عملی فوری برای امروز به فارسی",
              "focusBadgeFa": "برچسب کوتاه ۲ تا ۳ کلمه‌ای فارسی",
              "estimatedImpactFa": "تخمین اثر عملی یا صرفه‌جویی به فارسی",
              "urgencyTone": "PROTECTIVE | OPTIMIZATION | MOMENTUM"
            }
        """.trimIndent()
    }

    private suspend fun callCustomGeminiRestEndpoint(
        prompt: String,
        inputs: OnlineServiceInputsState
    ): String = withContext(Dispatchers.IO) {
        val validation = ConnectivityPrivacyEngine.validateAiInputs(inputs)
        if (!validation.isValid) {
            throw IllegalStateException(validation.errorFa ?: "ورودی‌های کلید Gemini معتبر نیست.")
        }
        val baseUrl = inputs.customAiEndpointUrl.trim()
            .ifBlank { ConnectivityPrivacyEngine.DEFAULT_GEMINI_BASE_URL }
            .trimEnd('/')
        val model = inputs.customAiModelName.trim()
            .ifBlank { AiProviderMode.CUSTOM_GEMINI_API.defaultModelName }
        val encodedModel = URLEncoder.encode(model, "UTF-8")
        val endpoint = "$baseUrl/v1beta/models/$encodedModel:generateContent"

        val bodyJson = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", prompt))
                    )
                )
            )
            put(
                "generationConfig",
                JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.6)
                }
            )
        }.toString()

        withTimeout(60_000L) {
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 45_000
                doOutput = true
                instanceFollowRedirects = false
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("x-goog-api-key", inputs.customAiApiKey.trim())
            }
            try {
                connection.outputStream.use { it.write(bodyJson.toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val responseStream = if (code in 200..299) connection.inputStream else connection.errorStream
                val payload = responseStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (code !in 200..299) {
                    throw IllegalStateException(
                        "سرویس اختصاصی Gemini با کد خطای $code پاسخ داد. کلید API و نام مدل را بررسی کنید."
                    )
                }
                val root = JSONObject(payload)
                val candidates = root.optJSONArray("candidates")
                val text = candidates
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text", "")
                    ?.trim()
                    .orEmpty()
                if (text.isBlank()) {
                    throw IllegalStateException("پاسخ متنی معتبری از سرویس Gemini دریافت نشد.")
                }
                text
            } finally {
                connection.disconnect()
            }
        }
    }

    private suspend fun callOpenAiCompatibleEndpoint(
        prompt: String,
        inputs: OnlineServiceInputsState
    ): String = withContext(Dispatchers.IO) {
        val validation = ConnectivityPrivacyEngine.validateAiInputs(inputs)
        if (!validation.isValid) {
            throw IllegalStateException(validation.errorFa ?: "ورودی‌های سرور هوش مصنوعی معتبر نیست.")
        }
        val endpoint = inputs.customAiEndpointUrl.trim()
        val model = inputs.customAiModelName.trim()
        val bodyJson = JSONObject().apply {
            put("model", model)
            put("temperature", 0.6)
            put("response_format", JSONObject().put("type", "json_object"))
            put(
                "messages",
                JSONArray()
                    .put(
                        JSONObject().apply {
                            put("role", "system")
                            put("content", "You are a Persian personal financial advisor. Respond strictly with valid JSON matching the requested schema.")
                        }
                    )
                    .put(
                        JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        }
                    )
            )
        }.toString()

        withTimeout(60_000L) {
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 45_000
                doOutput = true
                instanceFollowRedirects = false
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Authorization", "Bearer ${inputs.customAiApiKey.trim()}")
            }
            try {
                connection.outputStream.use { it.write(bodyJson.toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val responseStream = if (code in 200..299) connection.inputStream else connection.errorStream
                val payload = responseStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (code !in 200..299) {
                    throw IllegalStateException(
                        "سرور هوش مصنوعی با کد خطای $code پاسخ داد. آدرس سرور، کلید دسترسی و نام مدل را بررسی کنید."
                    )
                }
                val root = JSONObject(payload)
                val text = root.optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content", "")
                    ?.trim()
                    .orEmpty()
                if (text.isBlank()) {
                    throw IllegalStateException("پاسخ معتبری از سرور اختصاصی هوش مصنوعی دریافت نشد.")
                }
                text
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun parseTipJson(
        rawJson: String,
        snapshot: SpendingAndDebtSnapshot
    ): DailyFinancialTipResult {
        val cleaned = rawJson
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val jsonStart = cleaned.indexOf('{')
        val jsonEnd = cleaned.lastIndexOf('}')
        val slice = if (jsonStart >= 0 && jsonEnd > jsonStart) {
            cleaned.substring(jsonStart, jsonEnd + 1)
        } else {
            cleaned
        }

        val json = JSONObject(slice)
        val headline = json.optString("headlineFa", "").trim()
        val spendingInsight = json.optString("spendingPatternInsightFa", "").trim()
        val dailyTip = json.optString("dailyTipFa", "").trim()
        val actionStep = json.optString("concreteActionStepFa", "").trim()
        val focusBadge = json.optString("focusBadgeFa", "").trim().ifBlank { "نکته مالی امروز" }
        val impact = json.optString("estimatedImpactFa", "").trim()
        val tone = DailyAiFinancialTipEngine.parseUrgencyTone(json.optString("urgencyTone", ""))

        if (headline.isBlank() || dailyTip.isBlank()) {
            throw IllegalStateException("ساختار پاسخ هوش مصنوعی کامل نبود. لطفاً دوباره تلاش کنید.")
        }

        return DailyFinancialTipResult(
            headlineFa = headline,
            spendingPatternInsightFa = spendingInsight,
            dailyTipFa = dailyTip,
            concreteActionStepFa = actionStep,
            focusBadgeFa = focusBadge,
            estimatedImpactFa = impact,
            urgencyTone = tone,
            generatedDateJalali = snapshot.todayJalali,
            profileFingerprint = snapshot.profileFingerprint
        )
    }
}
