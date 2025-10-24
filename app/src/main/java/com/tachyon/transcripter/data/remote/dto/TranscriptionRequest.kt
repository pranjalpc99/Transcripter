package com.tachyon.transcripter.data.remote.dto

// TranscriptionRequest.kt

import okhttp3.MultipartBody
import okhttp3.RequestBody

/**
 * Request model for audio transcription.
 * Used with multipart/form-data for file upload.
 */
data class TranscriptionRequest(
    val file: MultipartBody.Part,
    val language: RequestBody? = null,
    val responseFormat: RequestBody? = null,
    val prompt: String? = null,
    val temperature: Float? = null
)