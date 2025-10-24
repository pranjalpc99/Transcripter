package com.tachyon.transcripter.domain.usecases

// StartRecordingUseCase.kt

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.tachyon.transcripter.data.local.entity.RecordingSession
import com.tachyon.transcripter.data.local.entity.SessionStatus
import com.tachyon.transcripter.data.repository.FileRepository
import com.tachyon.transcripter.data.repository.RecordingRepository
import com.tachyon.transcripter.service.RecordingService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject

/**
 * Use case for starting a new recording session.
 * Validates permissions, checks storage, creates session, and starts service.
 */
class StartRecordingUseCase @Inject constructor(
    private val recordingRepository: RecordingRepository,
    private val fileRepository: FileRepository,
    @ApplicationContext private val context: Context
) {
    /**
     * Execute the use case to start recording.
     *
     * @return Result with session ID if successful
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend operator fun invoke(): Result<String> {
        return try {
            // Check storage availability
            val storageCheck = recordingRepository.checkStorageForRecording()
            if (storageCheck.isFailure) {
                return storageCheck.map { "" }
            }

            val storageResult = storageCheck.getOrNull()
            if (storageResult?.hasEnoughSpace == false) {
                return Result.failure(
                    Exception("Insufficient storage space. Need at least 100 MB free.")
                )
            }

            // Generate session ID
            val sessionId = UUID.randomUUID().toString()

            // Create session directory
            val sessionDir = fileRepository.getSessionDirectory(sessionId)
            if (!sessionDir.exists()) {
                sessionDir.mkdirs()
            }

            // Create session entity
            val session = RecordingSession(
                id = sessionId,
                startTime = System.currentTimeMillis(),
                status = SessionStatus.RECORDING,
                storagePath = sessionDir.absolutePath,
                chunkCount = 0,
                transcribedChunkCount = 0,
                hasSummary = false
            )

            // Save session to database
            val createResult = recordingRepository.createSession(session)
            if (createResult.isFailure) {
                return createResult.map { it.id }
            }

            // Start recording service
            val serviceIntent = Intent(context, RecordingService::class.java).apply {
                action = RecordingService.ACTION_START
                putExtra(RecordingService.EXTRA_SESSION_ID, sessionId)
            }
            context.startForegroundService(serviceIntent)

            Result.success(sessionId)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}