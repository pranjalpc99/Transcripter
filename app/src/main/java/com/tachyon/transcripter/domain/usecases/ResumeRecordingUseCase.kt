package com.tachyon.transcripter.domain.usecases

// ResumeRecordingUseCase.kt

import android.content.Context
import android.content.Intent
import com.tachyon.transcripter.data.repository.RecordingRepository
import com.tachyon.transcripter.service.RecordingService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Use case for resuming a paused recording session.
 * Resumes the service and updates session status.
 */
class ResumeRecordingUseCase @Inject constructor(
    private val recordingRepository: RecordingRepository,
    @ApplicationContext private val context: Context
) {
    /**
     * Execute the use case to resume recording.
     *
     * @param sessionId Optional session ID to resume
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(sessionId: String? = null): Result<Unit> {
        return try {
            // Send resume action to service
            val serviceIntent = Intent(context, RecordingService::class.java).apply {
                action = RecordingService.ACTION_RESUME
                sessionId?.let { putExtra(RecordingService.EXTRA_SESSION_ID, it) }
            }
            context.startService(serviceIntent)

            // Update session in database if session ID provided
            sessionId?.let {
                recordingRepository.recordResume(it)
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}