// ProcessTranscriptionUseCase.kt
package com.tachyon.transcripter.domain.usecases

import com.tachyon.transcripter.data.local.entity.SessionStatus
import com.tachyon.transcripter.data.repository.RecordingRepository
import com.tachyon.transcripter.data.repository.TranscriptionRepository
import com.tachyon.transcripter.domain.model.TranscriptionProgress
import com.tachyon.transcripter.domain.model.TranscriptionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Use case for processing transcription of a recording session.
 * Handles batch transcription with progress updates.
 */
class ProcessTranscriptionUseCase @Inject constructor(
    private val transcriptionRepository: TranscriptionRepository,
    private val recordingRepository: RecordingRepository
) {
    /**
     * Execute the use case to process transcription.
     * Emits progress updates as a Flow.
     *
     * @param sessionId Session ID to transcribe
     * @return Flow of TranscriptionProgress
     */
    operator fun invoke(sessionId: String): Flow<TranscriptionProgress> = flow {
        try {
            recordingRepository.updateSessionStatus(sessionId, SessionStatus.TRANSCRIBING)

            var progress = transcriptionRepository.getTranscriptionProgress(sessionId)
            emit(progress)

            val chunks = recordingRepository.getChunksBySession(sessionId)

            if (chunks.isEmpty()) {
                emit(progress.copy(status = TranscriptionStatus.FAILED))
                return@flow
            }

            for (chunk in chunks) {
                // Skip already transcribed chunks
                if (chunk.transcriptionStatus == TranscriptionStatus.COMPLETED) {
                    continue
                }

                // Transcribe chunk with retry
                val result = transcriptionRepository.transcribeChunk(chunk.id)
                progress = transcriptionRepository.getTranscriptionProgress(sessionId)

                // Emit progress update
                emit(
                    progress.copy(
                        currentChunkNumber = chunk.chunkNumber,
                        status = if (result.isSuccess) {
                            TranscriptionStatus.TRANSCRIBING
                        } else {
                            TranscriptionStatus.UPLOAD_FAILED
                        }
                    )
                )
            }

            // Get final progress after all chunks processed
            progress = transcriptionRepository.getTranscriptionProgress(sessionId)

            // Determine final status and update session
            when {
                // All chunks completed successfully
                progress.isComplete -> {
                    recordingRepository.updateSessionStatus(sessionId, SessionStatus.STOPPED)
                    emit(progress.copy(status = TranscriptionStatus.COMPLETED))
                }
                // All chunks failed
                progress.hasFailed && progress.transcribedChunks == 0 -> {
                    recordingRepository.updateSessionStatus(sessionId, SessionStatus.TRANSCRIPTION_FAILED)
                    emit(progress.copy(status = TranscriptionStatus.FAILED))
                }
                // Some chunks failed but some succeeded - keep as UPLOAD_FAILED for retry
                progress.hasFailed -> {
                    emit(progress.copy(status = TranscriptionStatus.UPLOAD_FAILED))
                }
                // Still in progress (shouldn't happen here, but handle it)
                else -> {
                    emit(progress.copy(status = TranscriptionStatus.TRANSCRIBING))
                }
            }

        } catch (e: Exception) {
            recordingRepository.updateSessionStatus(sessionId, SessionStatus.TRANSCRIPTION_FAILED)
            emit(
                TranscriptionProgress(
                    sessionId = sessionId,
                    totalChunks = 0,
                    transcribedChunks = 0,
                    failedChunks = 0,
                    pendingChunks = 0,
                    progressPercentage = 0,
                    currentChunkNumber = null,
                    estimatedTimeRemaining = null,
                    status = TranscriptionStatus.FAILED
                )
            )
        }
    }

    /**
     * Retry failed transcriptions for a session.
     *
     * @param sessionId Session ID to retry
     * @return Flow of TranscriptionProgress
     */
    /*fun retryFailedTranscriptions(sessionId: String): Flow<TranscriptionProgress> = flow {
        try {
            // Reset failed chunks to pending
            val result = transcriptionRepository.retryFailedTranscriptions(sessionId)

            if (result.isSuccess) {
                // Process transcription again
                invoke(sessionId).collect { progress ->
                    emit(progress)
                }
            } else {
                emit(
                    TranscriptionProgress(
                        sessionId = sessionId,
                        totalChunks = 0,
                        transcribedChunks = 0,
                        failedChunks = 0,
                        pendingChunks = 0,
                        progressPercentage = 0,
                        currentChunkNumber = null,
                        estimatedTimeRemaining = null,
                        status = TranscriptionStatus.FAILED
                    )
                )
            }
        } catch (e: Exception) {
            emit(
                TranscriptionProgress(
                    sessionId = sessionId,
                    totalChunks = 0,
                    transcribedChunks = 0,
                    failedChunks = 0,
                    pendingChunks = 0,
                    progressPercentage = 0,
                    currentChunkNumber = null,
                    estimatedTimeRemaining = null,
                    status = TranscriptionStatus.FAILED
                )
            )
        }
    }*/

    /**
     * Check if transcription is complete for a session.
     */
    suspend fun isTranscriptionComplete(sessionId: String): Boolean {
        return transcriptionRepository.isTranscriptionComplete(sessionId)
    }

    /**
     * Get transcription progress for a session.
     */
    suspend fun getProgress(sessionId: String): TranscriptionProgress {
        return transcriptionRepository.getTranscriptionProgress(sessionId)
    }
}