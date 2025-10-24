package com.tachyon.transcripter.data.remote.dto

// SummaryRequest.kt

import com.google.gson.annotations.SerializedName

/**
 * Request model for summary generation.
 */
data class SummaryRequest(
    @SerializedName("prompt")
    val prompt: String,

    @SerializedName("max_tokens")
    val maxTokens: Int = 1000,

    @SerializedName("temperature")
    val temperature: Float = 0.7f,

    @SerializedName("stream")
    val stream: Boolean = false,

    @SerializedName("model")
    val model: String = "gemini-pro",

    @SerializedName("top_p")
    val topP: Float? = null,

    @SerializedName("top_k")
    val topK: Int? = null,

    @SerializedName("stop_sequences")
    val stopSequences: List<String>? = null
)
