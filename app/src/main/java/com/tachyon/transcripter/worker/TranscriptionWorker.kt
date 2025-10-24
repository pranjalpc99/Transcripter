package com.tachyon.transcripter.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.tachyon.transcripter.data.local.dao.AudioChunkDao
import com.tachyon.transcripter.data.local.dao.RecordingSessionDao
import com.tachyon.transcripter.data.local.entity.SessionStatus
import com.tachyon.transcripter.data.repository.TranscriptionRepository
import com.tachyon.transcripter.domain.model.TranscriptionStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

// worker/TranscriptionWorker.kt
@HiltWorker
class TranscriptionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val transcriptionRepository: TranscriptionRepository,
    private val audioChunkDao: AudioChunkDao,
    private val recordingSessionDao: RecordingSessionDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("TranscriptionWorker", "TranscriptionWorker is running!")
        val sessionId = inputData.getString(KEY_SESSION_ID)
        Log.d(TAG, "TranscriptionWorker started for session: $sessionId")
        if (sessionId == null) {
            Log.e(TAG, "Session ID is null, cannot proceed")
            return Result.failure()
        }

        return try {
            // Update session status
            recordingSessionDao.updateStatus(sessionId, SessionStatus.TRANSCRIBING)

            // Get all pending chunks
            val chunks = audioChunkDao.getChunksBySession(sessionId)
                .filter { it.transcriptionStatus == TranscriptionStatus.PENDING }

            // Transcribe each chunk sequentially
            chunks.forEach { chunk ->
                val result = transcriptionRepository.transcribeChunk(chunk.id)
                if (result.isFailure) {
                    // Log error but continue with other chunks
                    setProgressAsync(
                        workDataOf(
                            KEY_PROGRESS to chunks.indexOf(chunk),
                            KEY_TOTAL to chunks.size,
                            KEY_ERROR to result.exceptionOrNull()?.message
                        )
                    )
                } else {
                    setProgressAsync(
                        workDataOf(
                            KEY_PROGRESS to chunks.indexOf(chunk) + 1,
                            KEY_TOTAL to chunks.size
                        )
                    )
                }
            }

            // Check if all chunks are transcribed
            val completedCount = audioChunkDao.getCompletedTranscriptionCount(sessionId)
            if (completedCount == chunks.size) {
                // Enqueue summary generation
                recordingSessionDao.updateStatus(sessionId, SessionStatus.GENERATING_SUMMARY)
                enqueueSummaryWork(sessionId)
                Result.success()
            } else {
                recordingSessionDao.updateStatus(sessionId, SessionStatus.TRANSCRIPTION_FAILED)
                Result.failure()
            }

        } catch (e: Exception) {
            recordingSessionDao.updateStatus(sessionId, SessionStatus.TRANSCRIPTION_FAILED)
            Result.retry()
        }
    }

    private fun enqueueSummaryWork(sessionId: String) {
        val summaryWork = OneTimeWorkRequestBuilder<SummaryWorker>()
            .setInputData(workDataOf(KEY_SESSION_ID to sessionId))
            .build()

        WorkManager.getInstance(applicationContext).enqueue(summaryWork)
    }

    companion object {
        private const val TAG = "TranscriptionWorker"
        const val KEY_SESSION_ID = "session_id"
        const val KEY_PROGRESS = "progress"
        const val KEY_TOTAL = "total"
        const val KEY_ERROR = "error"
    }
}