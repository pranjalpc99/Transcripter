package com.tachyon.transcripter.data.remote.dto

// SummaryResponse.kt

import com.google.gson.annotations.SerializedName

/**
 * Response model from summary generation API (non-streaming).
 */
data class SummaryResponse(
    @SerializedName("text")
    val text: String,

    @SerializedName("finish_reason")
    val finishReason: String? = null,

    @SerializedName("model")
    val model: String? = null,

    @SerializedName("usage")
    val usage: UsageInfo? = null
)

/**
 * Streaming chunk for summary generation.
 * Used when stream=true in request.
 */
data class SummaryChunk(
    @SerializedName("id")
    val id: String,

    @SerializedName("text")
    val text: String,

    @SerializedName("finish_reason")
    val finishReason: String? = null,

    @SerializedName("index")
    val index: Int? = null
)

/**
 * Token usage information (if provided by API).
 */
data class UsageInfo(
    @SerializedName("prompt_tokens")
    val promptTokens: Int,

    @SerializedName("completion_tokens")
    val completionTokens: Int,

    @SerializedName("total_tokens")
    val totalTokens: Int
)

/**
 * Structured summary content (after parsing JSON response).
 */
data class StructuredSummary(
    val title: String,
    val summary: String,
    val actionItems: List<String>,
    val keyPoints: List<String>
)
