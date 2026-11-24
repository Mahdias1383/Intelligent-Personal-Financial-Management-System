package com.example.ai

import com.example.core.CurrencyUnit
import com.example.domain.DailyAiFinancialTipEngine
import com.example.domain.DailyFinancialTipResult
import com.example.domain.SpendingAndDebtSnapshot
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

/**
 * Generates personalized daily Persian financial tips from the user's spending and debt profile
 * using Firebase AI Logic (Gemini gemini-3.8-flash) with strict structured JSON schema output.
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
        currencyUnit: CurrencyUnit
    ): DailyFinancialTipResult {
        val prompt = DailyAiFinancialTipEngine.buildGeminiPrompt(
            snapshot = snapshot,
            currencyUnit = currencyUnit
        )

        val response = withTimeout(60_000L) {
            generativeModel.generateContent(prompt)
        }

        val rawJson = response.text?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("پاسخ خالی از مدل هوش مصنوعی دریافت شد.")

        // Direct JSON parsing guaranteed by responseMimeType = "application/json" + responseSchema.
        val json = JSONObject(rawJson)
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
