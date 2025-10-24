package com.tachyon.transcripter.ui.recording

import com.tachyon.transcripter.domain.model.RecordingState

// RecordingUiState.kt

/**
 * UI state for Recording screen.
 */
data class RecordingUiState(
    val sessionId: String? = null,
    val recordingState: RecordingState = RecordingState.Idle,
    val duration: Long = 0L,
    val chunkCount: Int = 0,
    val pauseReason: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showStopConfirmation: Boolean = false
) {
    /**
     * Check if currently recording.
     */
    val isRecording: Boolean
        get() = recordingState is RecordingState.Recording

    /**
     * Check if currently paused.
     */
    val isPaused: Boolean
        get() = recordingState is RecordingState.Paused

    /**
     * Check if recording is active (recording or paused).
     */
    val isActive: Boolean
        get() = isRecording || isPaused

    /**
     * Get formatted duration string.
     */
    val formattedDuration: String
        get() = formatDuration(duration)

    /**
     * Get status text for display.
     */
    val statusText: String
        get() = when (recordingState) {
            is RecordingState.Recording -> "Recording"
            is RecordingState.Paused -> pauseReason?.let {
                "Paused: ${formatPauseReason(it)}"
            } ?: "Paused"
            is RecordingState.Stopped -> "Processing"
            is RecordingState.Error -> "Error"
            RecordingState.Idle -> "Ready"
        }

    /**
     * Format duration to HH:MM:SS.
     */
    private fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Format pause reason for display.
     */
    private fun formatPauseReason(reason: String): String {
        return when (reason) {
            "phone_call" -> "Phone call"
            "audio_focus_loss" -> "Another app using audio"
            "low_storage" -> "Low storage"
            "user_action" -> "Manual pause"
            "bluetooth_disconnected" -> "Bluetooth disconnected"
            "headset_disconnected" -> "Headset disconnected"
            "silence_detected" -> "Silence detected"
            else -> reason.replace("_", " ").capitalize()
        }
    }
}
