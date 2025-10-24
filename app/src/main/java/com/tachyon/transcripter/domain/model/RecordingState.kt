package com.tachyon.transcripter.domain.model

// RecordingState.kt

/**
 * Sealed class representing the state of a recording session.
 * Used for UI state management and service communication.
 */
sealed class RecordingState {

    /**
     * No recording in progress.
     */
    object Idle : RecordingState()

    /**
     * Recording is in progress.
     */
    data class Recording(
        val sessionId: String,
        val duration: Long,  // Duration in milliseconds
        val chunkCount: Int = 0
    ) : RecordingState()

    /**
     * Recording is paused.
     */
    data class Paused(
        val sessionId: String,
        val reason: String? = null,  // Reason for pause (e.g., "phone_call", "user_action")
        val duration: Long = 0
    ) : RecordingState()

    /**
     * Recording has been stopped and is being processed.
     */
    data class Stopped(
        val sessionId: String,
        val totalDuration: Long = 0
    ) : RecordingState()

    /**
     * An error occurred during recording.
     */
    data class Error(
        val message: String,
        val sessionId: String? = null
    ) : RecordingState()

    /**
     * Returns true if currently recording.
     */
    val isRecording: Boolean
        get() = this is Recording

    /**
     * Returns true if paused.
     */
    val isPaused: Boolean
        get() = this is Paused

    /**
     * Returns true if stopped.
     */
    val isStopped: Boolean
        get() = this is Stopped

    /**
     * Returns true if there's an error.
     */
    val isError: Boolean
        get() = this is Error

    /**
     * Returns true if idle.
     */
    val isIdle: Boolean
        get() = this is Idle
}
/**
 * Extension function to get session ID from any state.
 */
fun RecordingState.getSessionId(): String? {
    return when (this) {
        is RecordingState.Recording -> this.sessionId
        is RecordingState.Paused -> this.sessionId
        is RecordingState.Stopped -> this.sessionId
        is RecordingState.Error -> this.sessionId
        is RecordingState.Idle -> null
    }
}