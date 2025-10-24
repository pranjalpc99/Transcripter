package com.tachyon.transcripter.domain.usecases

// GetRecordingStateUseCase.kt

import com.tachyon.transcripter.domain.model.RecordingState
import com.tachyon.transcripter.domain.model.getSessionId
import com.tachyon.transcripter.service.ServiceBridge
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for observing the current recording state.
 * Provides a reactive stream of recording state updates.
 */
class GetRecordingStateUseCase @Inject constructor(
    private val serviceBridge: ServiceBridge
) {
    /**
     * Execute the use case to get recording state.
     *
     * @return Flow of RecordingState
     */
    operator fun invoke(): Flow<RecordingState> {
        return serviceBridge.recordingState
    }

    /**
     * Get current recording state synchronously.
     *
     * @return Current RecordingState
     */
    fun getCurrentState(): RecordingState {
        return serviceBridge.recordingState.value
    }

    /**
     * Check if currently recording.
     */
    fun isRecording(): Boolean {
        return serviceBridge.recordingState.value.isRecording
    }

    /**
     * Check if currently paused.
     */
    fun isPaused(): Boolean {
        return serviceBridge.recordingState.value.isPaused
    }

    /**
     * Get current session ID if available.
     */
    fun getCurrentSessionId(): String? {
        return serviceBridge.recordingState.value.getSessionId()
    }
}