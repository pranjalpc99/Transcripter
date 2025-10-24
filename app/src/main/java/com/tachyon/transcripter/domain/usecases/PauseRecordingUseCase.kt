package com.tachyon.transcripter.domain.usecases

// PauseRecordingUseCase.kt

import android.content.Context
import android.content.Intent
import com.tachyon.transcripter.data.repository.RecordingRepository
import com.tachyon.transcripter.service.RecordingService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Use case for pausing a recording session.
 * Pauses the service and updates session status.
 */
class PauseRecordingUseCase @Inject constructor(
    private val recordingRepository: RecordingRepository,
    @ApplicationContext private val context: Context
) {
    /**
     * Execute the use case to pause recording.
     *
     * @param sessionId Optional session ID to pause
     * @param reason Optional reason for pausing (e.g., "user_action", "phone_call")
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(
        sessionId: String? = null,
        reason: String? = null
    ): Result<Unit> {
        return try {
            // Send pause action to service
            val serviceIntent = Intent(context, RecordingService::class.java).apply {
                action = RecordingService.ACTION_PAUSE
                sessionId?.let { putExtra(RecordingService.EXTRA_SESSION_ID, it) }
                reason?.let { putExtra(RecordingService.EXTRA_PAUSE_REASON, it) }
            }
            context.startService(serviceIntent)

            // Update session in database if session ID provided
            sessionId?.let {
                recordingRepository.recordPause(it, reason)
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}