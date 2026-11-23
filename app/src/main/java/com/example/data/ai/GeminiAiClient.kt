package com.example.data.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.domain.RevisionEngine
import com.example.i18n.AppLang
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.Calendar
import java.util.concurrent.TimeUnit

interface GeminiRestApi {
    @GET("v1beta/models")
    suspend fun listModels(
        @Header("x-goog-api-key") apiKey: String,
        @Query("pageSize") pageSize: Int = 100
    ): Response<GeminiModelsListResponse>

    @POST("v1beta/models/{modelId}:generateContent")
    suspend fun generateContent(
        @Header("x-goog-api-key") apiKey: String,
        @Path("modelId") modelId: String,
        @Body request: GeminiGenerateRequest
    ): Response<GeminiGenerateResponse>
}

sealed interface AiConnectResult {
    data class Success(val modelId: String, val testedCount: Int) : AiConnectResult
    data class Failure(val message: String) : AiConnectResult
}

class GeminiAiClient(private val context: Context) {

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/"
        const val MODEL_CACHE_TTL_MS = 24L * 60L * 60L * 1000L
    }

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // Intentionally omit header/body logging so user API keys are never written to Logcat.
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()

    private val api: GeminiRestApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(GeminiRestApi::class.java)

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun discoverAndSelectWorkingModel(apiKey: String): AiConnectResult {
        if (apiKey.isBlank()) return AiConnectResult.Failure("API key is empty")
        if (!isOnline()) return AiConnectResult.Failure("Offline — check internet connection")

        return try {
            val listResponse = api.listModels(apiKey = apiKey)
            if (!listResponse.isSuccessful) {
                return AiConnectResult.Failure("Key check failed (HTTP ${listResponse.code()})")
            }
            val rawModels = listResponse.body()?.models.orEmpty().map {
                it.name to it.supportedGenerationMethods.orEmpty()
            }
            val ranked = RevisionEngine.rankGeminiModels(rawModels)
            if (ranked.isEmpty()) {
                return AiConnectResult.Failure("No text generation models available for this key")
            }

            var tested = 0
            var lastHttpCode: Int? = null
            var lastExceptionMsg: String? = null
            for (candidate in ranked.take(15)) {
                tested++
                try {
                    val pingResp = api.generateContent(
                        apiKey = apiKey,
                        modelId = candidate.id,
                        request = GeminiGenerateRequest(
                            contents = listOf(GeminiContent(parts = listOf(GeminiPart("Reply with OK")))),
                            generationConfig = GeminiGenerationConfig(temperature = 0.0, maxOutputTokens = 64)
                        )
                    )
                    if (pingResp.isSuccessful) {
                        val hasCandidate = pingResp.body()?.candidates?.isNotEmpty() == true
                        if (hasCandidate) {
                            return AiConnectResult.Success(modelId = candidate.id, testedCount = tested)
                        }
                    } else {
                        val code = pingResp.code()
                        lastHttpCode = code
                        if (code == 401) {
                            return AiConnectResult.Failure("Invalid API key (HTTP 401)")
                        }
                        // Fall back to next model on 400/403/404/408/429/500/502/503/504
                        if (!RevisionEngine.shouldFallbackOnHttpStatus(code)) {
                            return AiConnectResult.Failure("API error (HTTP $code)")
                        }
                    }
                } catch (e: Exception) {
                    // Per-candidate network/timeout error: continue falling back to next model
                    lastExceptionMsg = e.localizedMessage
                }
            }
            val reason = when {
                lastHttpCode != null -> "No available model responded (last HTTP $lastHttpCode)"
                !lastExceptionMsg.isNullOrBlank() -> "Connection timed out ($lastExceptionMsg)"
                else -> "All discovered models reached quota or lacked access"
            }
            AiConnectResult.Failure(reason)
        } catch (e: Exception) {
            AiConnectResult.Failure(e.localizedMessage ?: "Connection failed")
        }
    }

    suspend fun generateNotificationCopy(
        apiKey: String,
        cachedModelId: String,
        cachedModelAtMillis: Long,
        onModelUpdated: suspend (String, Long) -> Unit,
        lang: AppLang,
        styleRequirement: String,
        whyText: String,
        whoForText: String,
        stageNames: List<String>,
        noteTitles: List<String>,
        optionalEntryExcerpt: String?,
        streakDays: Int,
        hasMissed: Boolean,
        missedCount: Int,
        dueCount: Int,
        nowMillis: Long
    ): Pair<String, String> {
        val fallback = RevisionEngine.buildFallbackNotificationText(
            lang = lang,
            dueTitles = noteTitles,
            hasMissed = hasMissed,
            missedCount = missedCount,
            dueCount = dueCount,
            whyText = whyText,
            whoForText = whoForText,
            streakDays = streakDays
        )
        if (apiKey.isBlank() || !isOnline()) return fallback

        val isCacheStale = cachedModelId.isBlank() ||
            (System.currentTimeMillis() - cachedModelAtMillis) > MODEL_CACHE_TTL_MS

        var activeModel = cachedModelId
        if (isCacheStale) {
            when (val res = discoverAndSelectWorkingModel(apiKey)) {
                is AiConnectResult.Success -> {
                    activeModel = res.modelId
                    onModelUpdated(activeModel, System.currentTimeMillis())
                }
                is AiConnectResult.Failure -> return fallback
            }
        }

        val prompt = buildNotificationPrompt(
            lang = lang,
            styleRequirement = styleRequirement,
            whyText = whyText,
            whoForText = whoForText,
            stageNames = stageNames,
            noteTitles = noteTitles,
            optionalEntryExcerpt = optionalEntryExcerpt,
            streakDays = streakDays,
            hasMissed = hasMissed,
            nowMillis = nowMillis
        )

        suspend fun attemptWith(modelId: String): String? {
            return try {
                val resp = api.generateContent(
                    apiKey = apiKey,
                    modelId = modelId,
                    request = GeminiGenerateRequest(
                        contents = listOf(GeminiContent(parts = listOf(GeminiPart(prompt)))),
                        generationConfig = GeminiGenerationConfig(temperature = 0.7, maxOutputTokens = 140)
                    )
                )
                if (resp.isSuccessful) {
                    resp.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                } else if (resp.code() in listOf(403, 404, 429)) {
                    null
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        }

        var rawText = attemptWith(activeModel)
        if (rawText.isNullOrBlank()) {
            // Re-discover on failure and try again once
            val rediscovered = discoverAndSelectWorkingModel(apiKey)
            if (rediscovered is AiConnectResult.Success) {
                onModelUpdated(rediscovered.modelId, System.currentTimeMillis())
                rawText = attemptWith(rediscovered.modelId)
            }
        }

        if (rawText.isNullOrBlank()) return fallback

        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val aiTitle = lines.firstOrNull()?.removePrefix("Title:")?.trim()?.take(65)
            ?: fallback.first
        val aiBody = lines.drop(1).joinToString(" ").removePrefix("Body:").trim().take(180)
            .ifBlank { lines.firstOrNull()?.take(180) ?: fallback.second }

        return if (hasMissed) {
            val requiredPrefix = if (lang == AppLang.BN) {
                "আজকের এই সময়ের রিভিশন দেওয়া হয়নি"
            } else {
                "Today's revision for this time slot is not done yet."
            }
            requiredPrefix to aiBody
        } else {
            aiTitle to aiBody
        }
    }

    private fun buildNotificationPrompt(
        lang: AppLang,
        styleRequirement: String,
        whyText: String,
        whoForText: String,
        stageNames: List<String>,
        noteTitles: List<String>,
        optionalEntryExcerpt: String?,
        streakDays: Int,
        hasMissed: Boolean,
        nowMillis: Long
    ): String {
        val hour = Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.HOUR_OF_DAY)
        val timeOfDay = when (hour) {
            in 5..11 -> "morning"
            in 12..16 -> "afternoon"
            in 17..20 -> "evening"
            else -> "night"
        }
        val targetLang = if (lang == AppLang.BN) "Bengali" else "English"
        return buildString {
            appendLine("Write one compassionate, short study revision push notification in $targetLang.")
            appendLine("Output EXACTLY 2 lines: Line 1 is a short title (max 45 chars), Line 2 is the body (max 120 chars). No markdown, no quotes.")
            if (styleRequirement.isNotBlank()) {
                appendLine("USER'S STRICT STYLE REQUIREMENT (follow this above everything): $styleRequirement")
            }
            appendLine("Student's Why: ${whyText.ifBlank { "Building a better future" }}")
            if (whoForText.isNotBlank()) appendLine("Who they study for: $whoForText")
            appendLine("Due stages: ${stageNames.distinct().take(3).joinToString(", ")}")
            appendLine("Note titles: ${noteTitles.distinct().take(3).joinToString(", ")}")
            if (!optionalEntryExcerpt.isNullOrBlank()) {
                appendLine("Lesson snippet: ${optionalEntryExcerpt.take(180)}")
            }
            appendLine("Time of day: $timeOfDay. Streak: $streakDays days. Missed slot waiting: $hasMissed.")
            appendLine("Tone: warm, encouraging, never guilt-tripping or urgent.")
        }
    }
}
