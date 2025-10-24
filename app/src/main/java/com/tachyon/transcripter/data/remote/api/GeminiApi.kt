package com.tachyon.transcripter.data.remote.api

// GeminiApi.kt
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API interface for Gemini API endpoints.
 * Handles audio transcription and summary generation.
 */
interface GeminiApi {

    /**
     * Generate content with Gemini 2.5 Flash (supports text, images, and audio)
     * This is the working endpoint for transcription
     */
    @POST("models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: RequestBody
    ): Response<GeminiResponse>

    /**
     * Stream generate content with Gemini 2.5 Flash
     */
    @Streaming
    @POST("models/gemini-2.5-flash:streamGenerateContent")
    suspend fun streamGenerateContent(
        @Query("key") apiKey: String,
        @Body request: RequestBody
    ): Response<ResponseBody>
}

// Response models matching the actual API response
data class GeminiResponse(
    val candidates: List<Candidate>?,
    val usageMetadata: UsageMetadata?,
    val modelVersion: String?,
    val responseId: String?
)

data class Candidate(
    val content: Content?,
    val finishReason: String?,
    val index: Int?
)

data class Content(
    val parts: List<Part>?,
    val role: String?
)

data class Part(
    val text: String?
)

data class UsageMetadata(
    val promptTokenCount: Int?,
    val candidatesTokenCount: Int?,
    val totalTokenCount: Int?,
    val promptTokensDetails: List<TokenDetail>?,
    val thoughtsTokenCount: Int?
)

data class TokenDetail(
    val modality: String?,
    val tokenCount: Int?
)