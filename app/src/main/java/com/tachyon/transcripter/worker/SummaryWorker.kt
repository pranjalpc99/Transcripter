package com.tachyon.transcripter.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tachyon.transcripter.data.local.dao.RecordingSessionDao
import com.tachyon.transcripter.data.local.entity.SessionStatus
import com.tachyon.transcripter.data.repository.SummaryRepository
import com.tachyon.transcripter.data.repository.SummaryStreamState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

// worker/SummaryWorker.kt
@HiltWorker
class SummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val summaryRepository: SummaryRepository,
    private val recordingSessionDao: RecordingSessionDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val sessionId = inputData.getString(KEY_SESSION_ID) ?: return Result.failure()

        return try {
            // Generate summary (non-streaming for worker)
            summaryRepository.generateSummary(sessionId).collect { state ->
                when (state) {
                    is SummaryStreamState.Complete -> {
                        recordingSessionDao.updateStatus(sessionId, SessionStatus.COMPLETED)
                        // Show notification
                        showCompletionNotification(sessionId)
                    }
                    is SummaryStreamState.Error -> {
                        recordingSessionDao.updateStatus(sessionId, SessionStatus.FAILED)
                        return@collect
                    }
                    else -> { /* Continue */ }
                }
            }

            Result.success()

        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showCompletionNotification(sessionId: String) {
        // Build notification with "View Summary" action
    }

    companion object {
        const val KEY_SESSION_ID = "session_id"
    }
}