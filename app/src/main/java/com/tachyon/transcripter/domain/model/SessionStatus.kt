package com.tachyon.transcripter.domain.model

// SessionStatus.kt

/**
 * Enum representing the status of a recording session.
 * Used throughout the app to track session lifecycle.
 */
enum class SessionStatus {
    /**
     * Recording is currently in progress.
     */
    RECORDING,

    /**
     * Recording is paused (by user or interruption).
     */
    PAUSED,

    /**
     * Recording has been stopped, waiting for transcription.
     */
    STOPPED,

    /**
     * Transcription is in progress.
     */
    TRANSCRIBING,

    /**
     * Transcription has failed.
     */
    TRANSCRIPTION_FAILED,

    /**
     * Summary is being generated.
     */
    GENERATING_SUMMARY,

    /**
     * Session is fully processed and complete.
     */
    COMPLETED,

    /**
     * Session has encountered an unrecoverable error.
     */
    FAILED;

    /**
     * Returns true if session is active (recording or paused).
     */
    val isActive: Boolean
        get() = this == RECORDING || this == PAUSED

    /**
     * Returns true if session is being processed.
     */
    val isProcessing: Boolean
        get() = this == TRANSCRIBING || this == GENERATING_SUMMARY

    /**
     * Returns true if session is in a final state.
     */
    val isFinal: Boolean
        get() = this == COMPLETED || this == FAILED

    /**
     * Returns true if session can be resumed.
     */
    val canResume: Boolean
        get() = this == PAUSED

    /**
     * Returns true if session can be stopped.
     */
    val canStop: Boolean
        get() = this == RECORDING || this == PAUSED

    /**
     * Returns a user-friendly display text.
     */
    fun toDisplayText(): String = when (this) {
        RECORDING -> "Recording"
        PAUSED -> "Paused"
        STOPPED -> "Stopped"
        TRANSCRIBING -> "Transcribing"
        TRANSCRIPTION_FAILED -> "Transcription Failed"
        GENERATING_SUMMARY -> "Generating Summary"
        COMPLETED -> "Completed"
        FAILED -> "Failed"
    }

    /**
     * Returns a color indicator for UI.
     * Using Android color resource names.
     */
    fun getColorIndicator(): String = when (this) {
        RECORDING -> "red"          // Active recording
        PAUSED -> "orange"          // Paused state
        STOPPED -> "gray"           // Stopped
        TRANSCRIBING -> "blue"      // Processing
        TRANSCRIPTION_FAILED -> "red"  // Error
        GENERATING_SUMMARY -> "blue"   // Processing
        COMPLETED -> "green"        // Success
        FAILED -> "red"             // Error
    }
}