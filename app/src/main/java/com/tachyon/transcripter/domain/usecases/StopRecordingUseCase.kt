package com.tachyon.transcripter.domain.usecases

// StopRecordingUseCase.kt

import android.content.Context
import android.content.Intent
import com.tachyon.transcripter.data.repository.RecordingRepository
import com.tachyon.transcripter.service.RecordingService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Use case for stopping a recording session.
 * Stops the service and finalizes the session.
 */
class StopRecordingUseCase @Inject constructor(
    private val recordingRepository: RecordingRepository,
    @ApplicationContext private val context: Context
) {
    /**
     * Execute the use case to stop recording.
     *
     * @param sessionId Optional session ID to stop (if not provided, stops current session)
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(sessionId: String? = null): Result<Unit> {
        return try {
            // Send stop action to service
            val serviceIntent = Intent(context, RecordingService::class.java).apply {
                action = RecordingService.ACTION_STOP
                sessionId?.let { putExtra(RecordingService.EXTRA_SESSION_ID, it) }
            }
            context.startService(serviceIntent)

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}