package com.example.data.ai

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiModelsListResponse(
    val models: List<GeminiModelInfo>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiModelInfo(
    val name: String = "",
    val displayName: String? = null,
    val supportedGenerationMethods: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val temperature: Double = 0.7,
    val maxOutputTokens: Int = 180
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateResponse(
    val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null
)
