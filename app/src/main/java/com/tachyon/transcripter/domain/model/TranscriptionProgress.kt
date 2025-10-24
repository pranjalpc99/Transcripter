package com.tachyon.transcripter.domain.model

/**
 * Model representing the progress of transcription for a recording session.
 */
data class TranscriptionProgress(
    val sessionId: String,
    val totalChunks: Int,
    val transcribedChunks: Int,
    val failedChunks: Int,
    val pendingChunks: Int,
    val progressPercentage: Int,
    val currentChunkNumber: Int? = null,
    val estimatedTimeRemaining: Long? = null,  // In milliseconds
    val status: TranscriptionStatus = TranscriptionStatus.PENDING
) {
    /**
     * Returns true if transcription is complete.
     */
    val isComplete: Boolean
        get() = transcribedChunks == totalChunks && failedChunks == 0

    /**
     * Returns true if transcription has failed.
     */
    val hasFailed: Boolean
        get() = failedChunks > 0

    /**
     * Returns true if transcription is in progress.
     */
    val isInProgress: Boolean
        get() = transcribedChunks > 0 && transcribedChunks < totalChunks

    /**
     * Returns the success rate as a percentage.
     */
    val successRate: Int
        get() = if (totalChunks > 0) {
            ((transcribedChunks.toFloat() / totalChunks) * 100).toInt()
        } else {
            0
        }

    /**
     * Returns the failure rate as a percentage.
     */
    val failureRate: Int
        get() = if (totalChunks > 0) {
            ((failedChunks.toFloat() / totalChunks) * 100).toInt()
        } else {
            0
        }
}

/**
 * Status of transcription process.
 */
enum class TranscriptionStatus {
    PENDING,            // Not yet uploaded
    UPLOADING,          // Upload in progress
    UPLOAD_FAILED,      // Upload failed, will retry
    TRANSCRIBING,       // Sent to API, waiting for response
    COMPLETED,          // Transcription received
    FAILED              // Permanent failure
}