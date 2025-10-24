package com.tachyon.transcripter.domain.usecases

// GenerateSummaryUseCase.kt
import com.tachyon.transcripter.data.local.entity.SessionStatus
import com.tachyon.transcripter.data.repository.RecordingRepository
import com.tachyon.transcripter.data.repository.SummaryRepository
import com.tachyon.transcripter.data.repository.SummaryStreamState
import com.tachyon.transcripter.data.repository.TranscriptionRepository
import com.tachyon.transcripter.domain.model.SummaryData
import com.tachyon.transcripter.domain.model.SummaryStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case for generating AI summary from transcription.
 * Handles streaming summary generation with state updates.
 */
class GenerateSummaryUseCase @Inject constructor(
    private val summaryRepository: SummaryRepository,
    private val transcriptionRepository: TranscriptionRepository,
    private val recordingRepository: RecordingRepository
) {
    /**
     * Execute the use case to generate summary.
     * Emits streaming updates as summary is generated.
     *
     * @param sessionId Session ID to generate summary for
     * @return Flow of SummaryStreamState
     */
    operator fun invoke(sessionId: String): Flow<SummaryStreamState> {
        return summaryRepository.generateSummary(sessionId)
    }

    /**
     * Generate summary without streaming (for background processing).
     *
     * @param sessionId Session ID to generate summary for
     * @return Result with SummaryData
     */
    suspend fun generateNonStreaming(sessionId: String): Result<SummaryData> {
        return try {
            // Check if transcription is complete
            val isTranscriptionComplete = transcriptionRepository.isTranscriptionComplete(sessionId)
            if (!isTranscriptionComplete) {
                return Result.failure(
                    Exception("Transcription must be complete before generating summary")
                )
            }

            // Update session status
            recordingRepository.updateSessionStatus(sessionId, SessionStatus.GENERATING_SUMMARY)

            // Generate summary
            val result = summaryRepository.generateSummaryNonStreaming(sessionId)

            if (result.isSuccess) {
                val summary = result.getOrNull()

                // Update session status
                recordingRepository.updateSessionStatus(sessionId, SessionStatus.COMPLETED)

                // Convert to SummaryData
                summary?.let {
                    Result.success(
                        SummaryData(
                            sessionId = it.sessionId,
                            title = it.title,
                            summary = it.summary,
                            actionItems = it.actionItems?.split("\n") ?: emptyList(),
                            keyPoints = it.keyPoints?.split("\n") ?: emptyList(),
                            status = SummaryStatus.COMPLETED,
                            createdAt = it.createdAt,
                            updatedAt = it.updatedAt
                        )
                    )
                } ?: Result.failure(Exception("Summary generation returned null"))
            } else {
                recordingRepository.updateSessionStatus(sessionId, SessionStatus.FAILED)
                result.map {
                    SummaryData(
                        sessionId = sessionId,
                        title = null,
                        summary = null,
                        status = SummaryStatus.FAILED
                    )
                }
            }

        } catch (e: Exception) {
            recordingRepository.updateSessionStatus(sessionId, SessionStatus.FAILED)
            Result.failure(e)
        }
    }

    /**
     * Retry summary generation for a failed session.
     *
     * @param sessionId Session ID to retry
     * @return Flow of SummaryStreamState
     */
    suspend fun retrySummaryGeneration(sessionId: String): Flow<SummaryStreamState> {
        return summaryRepository.retrySummaryGeneration(sessionId)
    }

    /**
     * Get existing summary for a session.
     *
     * @param sessionId Session ID
     * @return SummaryData if exists, null otherwise
     */
    suspend fun getSummary(sessionId: String): SummaryData? {
        val summary = summaryRepository.getSummaryBySessionId(sessionId) ?: return null

        return SummaryData(
            sessionId = summary.sessionId,
            title = summary.title,
            summary = summary.summary,
            actionItems = summary.actionItems?.split("\n")?.filter { it.isNotBlank() } ?: emptyList(),
            keyPoints = summary.keyPoints?.split("\n")?.filter { it.isNotBlank() } ?: emptyList(),
            status = when (summary.generationStatus) {
                com.tachyon.transcripter.data.local.entity.GenerationStatus.PENDING -> SummaryStatus.PENDING
                com.tachyon.transcripter.data.local.entity.GenerationStatus.GENERATING -> SummaryStatus.GENERATING
                com.tachyon.transcripter.data.local.entity.GenerationStatus.COMPLETED -> SummaryStatus.COMPLETED
                com.tachyon.transcripter.data.local.entity.GenerationStatus.FAILED -> SummaryStatus.FAILED
            },
            partialContent = summary.partialContent,
            errorMessage = summary.errorMessage,
            createdAt = summary.createdAt,
            updatedAt = summary.updatedAt
        )
    }

    /**
     * Observe summary changes for a session.
     *
     * @param sessionId Session ID
     * @return Flow of SummaryData
     */
    fun observeSummary(sessionId: String): Flow<SummaryData?> {
        return summaryRepository.observeSummary(sessionId).map { summary ->
            summary?.let {
                SummaryData(
                    sessionId = it.sessionId,
                    title = it.title,
                    summary = it.summary,
                    actionItems = it.actionItems?.split("\n")?.filter { it.isNotBlank() } ?: emptyList(),
                    keyPoints = it.keyPoints?.split("\n")?.filter { it.isNotBlank() } ?: emptyList(),
                    status = when (it.generationStatus) {
                        com.tachyon.transcripter.data.local.entity.GenerationStatus.PENDING -> SummaryStatus.PENDING
                        com.tachyon.transcripter.data.local.entity.GenerationStatus.GENERATING -> SummaryStatus.GENERATING
                        com.tachyon.transcripter.data.local.entity.GenerationStatus.COMPLETED -> SummaryStatus.COMPLETED
                        com.tachyon.transcripter.data.local.entity.GenerationStatus.FAILED -> SummaryStatus.FAILED
                    },
                    partialContent = it.partialContent,
                    errorMessage = it.errorMessage,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            }
        }
    }
}