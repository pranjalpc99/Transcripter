package com.tachyon.transcripter.data.remote.dto

// TranscriptionResponse.kt

import com.google.gson.annotations.SerializedName

/**
 * Response model from transcription API.
 */
data class TranscriptionResponse(
    @SerializedName("text")
    val text: String,

    @SerializedName("segments")
    val segments: List<TranscriptSegmentDto>? = null,

    @SerializedName("language")
    val language: String? = null,

    @SerializedName("duration")
    val duration: Double? = null
)

/**
 * Individual transcript segment with timing information.
 */
data class TranscriptSegmentDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("start")
    val start: Double,  // Start time in seconds

    @SerializedName("end")
    val end: Double,    // End time in seconds

    @SerializedName("text")
    val text: String,

    @SerializedName("confidence")
    val confidence: Float? = null,

    @SerializedName("words")
    val words: List<WordDto>? = null
)

/**
 * Individual word with timing (optional, if API provides word-level timestamps).
 */
data class WordDto(
    @SerializedName("word")
    val word: String,

    @SerializedName("start")
    val start: Double,

    @SerializedName("end")
    val end: Double,

    @SerializedName("confidence")
    val confidence: Float? = null
)
