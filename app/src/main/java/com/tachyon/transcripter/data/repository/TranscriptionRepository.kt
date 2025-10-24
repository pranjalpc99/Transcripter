package com.tachyon.transcripter.data.repository

import android.util.Log
import com.tachyon.transcripter.BuildConfig
import com.tachyon.transcripter.data.local.dao.AudioChunkDao
import com.tachyon.transcripter.data.local.dao.RecordingSessionDao
import com.tachyon.transcripter.data.local.dao.TranscriptSegmentDao
import com.tachyon.transcripter.data.local.entity.AudioChunk
import com.tachyon.transcripter.data.local.entity.TranscriptSegment
import com.tachyon.transcripter.data.remote.api.GeminiApi
import com.tachyon.transcripter.data.remote.dto.TranscriptionResponse
import com.tachyon.transcripter.domain.model.TranscriptionProgress
import com.tachyon.transcripter.domain.model.TranscriptionStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow

/**
 * Repository for managing audio transcription operations.
 * Handles API calls, retry logic, and transcript storage.
 */
@Singleton
class TranscriptionRepository @Inject constructor(
    private val geminiApi: GeminiApi,
    private val audioChunkDao: AudioChunkDao,
    private val transcriptSegmentDao: TranscriptSegmentDao,
    private val recordingSessionDao: RecordingSessionDao,
    private val fileRepository: FileRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    // ========== Transcription Operations ==========

    private fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }

    /**
     * Transcribe a single audio chunk.
     * Uploads the audio file to the API and processes the response.
     *
     * @param chunkId The chunk ID to transcribe
     * @return Result with list of transcript segments
     */
    suspend fun transcribeChunk(chunkId: String): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                // Get chunk from database
                val chunk = audioChunkDao.getById(chunkId)
                    ?: return@withContext Result.failure(Exception("Chunk not found: $chunkId"))

                // Verify file exists
                val audioFile = File(chunk.filePath)
                if (!audioFile.exists() || !audioFile.canRead()) {
                    return@withContext Result.failure(
                        Exception("Audio file not found or not readable: ${chunk.filePath}")
                    )
                }

                // Update status to uploading
                audioChunkDao.updateTranscriptionStatus(chunkId, TranscriptionStatus.UPLOADING)

                // Read audio file and encode to base64
                val audioBytes = audioFile.readBytes()
                val base64Audio = android.util.Base64.encodeToString(audioBytes, android.util.Base64.NO_WRAP)

                Log.d(TAG, "Base64 encoded, length: ${base64Audio.length}")

                // Prepare multipart request
                // Create request body
                val requestJson = """
                {
                  "contents": [{
                    "parts": [
                      {
                        "text": "Transcribe this audio accurately. Return only the transcribed text with proper punctuation."
                      },
                      {
                        "inline_data": {
                          "mime_type": "audio/m4a",
                          "data": "$base64Audio"
                        }
                      }
                    ]
                  }]
                }
                """.trimIndent()

                val requestBody = requestJson.toRequestBody("application/json".toMediaType())

                // Update status to transcribing
                audioChunkDao.updateTranscriptionStatus(chunkId, TranscriptionStatus.TRANSCRIBING)
                Log.d("Gemini", "API key length: ${getApiKey().length}")


                // Call API
                val response = geminiApi.generateContent(getApiKey(), requestBody)

                if (response.isSuccessful) {
                    val geminiResponse = response.body()
                    val transcription = geminiResponse
                        ?.candidates?.firstOrNull()
                        ?.content?.parts?.firstOrNull()
                        ?.text

                    Log.d(TAG, "Transcription result: $transcription")
                    Log.d(TAG, "Token usage: ${geminiResponse?.usageMetadata?.totalTokenCount}")

                    if (transcription != null && transcription.isNotBlank()) {
                        // Save transcription
                        saveTranscription(chunkId, chunk.sessionId, transcription, chunk.startTimeMs, chunk.endTimeMs)
                        audioChunkDao.updateTranscriptionStatus(chunkId, TranscriptionStatus.COMPLETED)
                        Log.d(TAG, "Transcription saved successfully")
                        // Increment session transcribed count
                        recordingSessionDao.incrementTranscribedCount(chunk.sessionId)
                        Result.success(Unit)
                    } else {
                        Log.e(TAG, "No transcription text in response")
                        audioChunkDao.updateTranscriptionStatus(chunkId, TranscriptionStatus.FAILED)
                        Result.failure(Exception("No transcription in response"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val error = "API Error [${response.code()}]: $errorBody"
                    Log.e(TAG, error)
                    audioChunkDao.updateTranscriptionStatus(chunkId, TranscriptionStatus.UPLOAD_FAILED)
                    Result.failure(Exception(error))
                }

            } catch (e: ApiException) {
                handleTranscriptionError(chunkId, e)
                Result.failure(e)
            } catch (e: Exception) {
                handleTranscriptionError(chunkId, e)
                Result.failure(e)
            }
        }

    /**
     * Transcribe a chunk with retry logic.
     * Implements exponential backoff for failed attempts.
     *
     * @param chunkId The chunk ID to transcribe
     * @param maxRetries Maximum number of retry attempts
     * @return Result with list of transcript segments
     */
/*    suspend fun transcribeChunkWithRetry(
        chunkId: String,
        maxRetries: Int = MAX_RETRIES
    ): Result<List<TranscriptSegment>> = withContext(ioDispatcher) {
        var lastException: Exception? = null

        repeat(maxRetries + 1) { attempt ->
            val result = transcribeChunk(chunkId)

            if (result.isSuccess) {
                return@withContext result
            }

            lastException = result.exceptionOrNull() as? Exception

            // Don't retry on permanent errors
            if (lastException is ApiException) {
                val apiException = lastException as ApiException
                if (apiException.code in 400..499 && apiException.code != 429) {
                    // Client error (except rate limit) - don't retry
                    return@withContext result
                }
            }

            // Calculate backoff delay
            if (attempt < maxRetries) {
                val delayMs = calculateBackoffDelay(attempt)
                delay(delayMs)
            }
        }

        Result.failure(lastException ?: Exception("Transcription failed after $maxRetries retries"))
    }*/

    private suspend fun saveTranscription(
        chunkId: String,
        sessionId: String,
        transcriptionText: String,
        startTimeMs: Long,
        endTimeMs: Long
    ) {
        // Get the chunk number for sequence
        val chunk = audioChunkDao.getById(chunkId)
        val sequenceNumber = chunk?.chunkNumber ?: 0

        // Create transcript segment
        val segment = TranscriptSegment(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            chunkId = chunkId,
            text = transcriptionText,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            sequenceNumber = sequenceNumber
        )

        transcriptSegmentDao.insert(segment)
        Log.d(TAG, "Saved transcript segment: seq=$sequenceNumber, text=${transcriptionText.take(50)}...")
    }

    /**
     * Parse API response and save transcript segments to database.
     */
    private suspend fun parseAndSaveTranscriptSegments(
        chunk: AudioChunk,
        response: TranscriptionResponse
    ): List<TranscriptSegment> {
        // Get current max sequence number for this session
        val maxSequence = transcriptSegmentDao.getMaxSequenceNumber(chunk.sessionId) ?: -1
        var currentSequence = maxSequence + 1

        val segments = if (response.segments.isNullOrEmpty()) {
            // If no detailed segments, create one segment with full text
            listOf(
                TranscriptSegment(
                    sessionId = chunk.sessionId,
                    chunkId = chunk.id,
                    sequenceNumber = currentSequence,
                    text = response.text.trim(),
                    startTimeMs = chunk.startTimeMs,
                    endTimeMs = chunk.endTimeMs,
                    isInOverlap = false,
                    confidence = null
                )
            )
        } else {
            // Parse detailed segments with timestamps
            response.segments.map { segment ->
                val startMs = chunk.startTimeMs + (segment.start * 1000).toLong()
                val endMs = chunk.startTimeMs + (segment.end * 1000).toLong()

                // Check if this segment is in the overlap region
                val isInOverlap = chunk.hasOverlap &&
                        chunk.overlapStartMs != null &&
                        startMs < chunk.overlapStartMs + (chunk.overlapDurationMs ?: 0)

                TranscriptSegment(
                    sessionId = chunk.sessionId,
                    chunkId = chunk.id,
                    sequenceNumber = currentSequence++,
                    text = segment.text.trim(),
                    startTimeMs = startMs,
                    endTimeMs = endMs,
                    isInOverlap = isInOverlap,
                    confidence = segment.confidence
                ).also {
                    // Only include non-empty segments
                    if (it.text.isNotBlank()) {
                        currentSequence++
                    }
                }
            }.filter { it.text.isNotBlank() }
        }

        // Save all segments to database
        transcriptSegmentDao.insertAll(segments)

        return segments
    }

    /**
     * Handle transcription errors and update chunk status.
     */
    private suspend fun handleTranscriptionError(chunkId: String, error: Exception) {
        try {
            // Increment attempt count
            audioChunkDao.incrementUploadAttempt(chunkId)

            // Get current attempt count
            val chunk = audioChunkDao.getById(chunkId)
            val attemptCount = chunk?.uploadAttemptCount ?: 0

            // Update status based on error type and attempt count
            val newStatus = when {
                error is ApiException && error.code in 400..499 && error.code != 429 -> {
                    // Permanent client error
                    TranscriptionStatus.FAILED
                }
                attemptCount >= MAX_RETRIES -> {
                    // Max retries exceeded
                    TranscriptionStatus.FAILED
                }
                else -> {
                    // Temporary error, will retry
                    TranscriptionStatus.UPLOAD_FAILED
                }
            }

            audioChunkDao.updateTranscriptionStatus(chunkId, newStatus)

            // Update chunk with error message
            chunk?.let {
                audioChunkDao.update(
                    it.copy(uploadError = error.message?.take(500))
                )
            }
        } catch (e: Exception) {
            // Log error but don't throw
        }
    }

    /**
     * Calculate exponential backoff delay.
     */
    private fun calculateBackoffDelay(attempt: Int): Long {
        val baseDelay = RETRY_INITIAL_DELAY_MS
        val maxDelay = RETRY_MAX_DELAY_MS
        val multiplier = RETRY_BACKOFF_MULTIPLIER

        val delay = (baseDelay * multiplier.pow(attempt.toDouble())).toLong()
        return min(delay, maxDelay)
    }

    // ========== Batch Operations ==========

    /**
     * Transcribe all pending chunks for a session.
     * Processes chunks sequentially to avoid API rate limits.
     *
     * @param sessionId Session ID
     * @return Result with transcription summary
     */
/*    suspend fun transcribeSession(sessionId: String): Result<TranscriptionSummary> =
        withContext(ioDispatcher) {
            try {
                val chunks = audioChunkDao.getChunksBySession(sessionId)
                    .filter { it.transcriptionStatus == TranscriptionStatus.PENDING }

                if (chunks.isEmpty()) {
                    return@withContext Result.success(
                        TranscriptionSummary(
                            totalChunks = 0,
                            successCount = 0,
                            failedCount = 0,
                            failedChunkIds = emptyList()
                        )
                    )
                }

                var successCount = 0
                val failedChunkIds = mutableListOf<String>()

                for (chunk in chunks) {
                    val result = transcribeChunkWithRetry(chunk.id)
                    if (result.isSuccess) {
                        successCount++
                    } else {
                        failedChunkIds.add(chunk.id)
                    }
                }

                val summary = TranscriptionSummary(
                    totalChunks = chunks.size,
                    successCount = successCount,
                    failedCount = failedChunkIds.size,
                    failedChunkIds = failedChunkIds
                )

                Result.success(summary)

            } catch (e: Exception) {
                Result.failure(e)
            }
        }*/

    /**
     * Retry all failed transcriptions for a session.
     */
/*    suspend fun retryFailedTranscriptions(sessionId: String): Result<TranscriptionSummary> =
        withContext(ioDispatcher) {
            try {
                val chunks = audioChunkDao.getChunksBySession(sessionId)
                    .filter {
                        it.transcriptionStatus == TranscriptionStatus.UPLOAD_FAILED ||
                                it.transcriptionStatus == TranscriptionStatus.FAILED
                    }

                if (chunks.isEmpty()) {
                    return@withContext Result.success(
                        TranscriptionSummary(0, 0, 0, emptyList())
                    )
                }

                // Reset status to pending for retry
                chunks.forEach { chunk ->
                    audioChunkDao.updateTranscriptionStatus(chunk.id, TranscriptionStatus.PENDING)
                }

                // Transcribe again
                transcribeSession(sessionId)

            } catch (e: Exception) {
                Result.failure(e)
            }
        }*/

    // ========== Transcript Retrieval ==========

    /**
     * Get the full transcript for a session.
     * Merges all segments, handling overlaps.
     *
     * @param sessionId Session ID
     * @return Complete transcript text
     */
    suspend fun getFullTranscript(sessionId: String): String =
        withContext(ioDispatcher) {
            val segments = transcriptSegmentDao.getSegmentsBySession(sessionId)
            mergeTranscriptSegments(segments)
        }

    /**
     * Get transcript segments for a session (with Flow for reactive updates).
     */
    fun observeTranscriptSegments(sessionId: String) =
        transcriptSegmentDao.observeSegmentsBySession(sessionId)

    /**
     * Get formatted transcript with timestamps.
     *
     * @param sessionId Session ID
     * @param includeTimestamps Whether to include timestamps in output
     * @return Formatted transcript
     */
    suspend fun getFormattedTranscript(
        sessionId: String,
        includeTimestamps: Boolean = false
    ): String = withContext(ioDispatcher) {
        val segments = transcriptSegmentDao.getSegmentsBySession(sessionId)
            .filterNot { it.isInOverlap }
            .sortedBy { it.sequenceNumber }

        if (includeTimestamps) {
            segments.joinToString("\n") { segment ->
                val timestamp = formatTimestamp(segment.startTimeMs)
                "[$timestamp] ${segment.text}"
            }
        } else {
            segments.joinToString(" ") { it.text }
        }
    }

    /**
     * Merge transcript segments, handling overlaps.
     * Removes duplicate content from overlap regions.
     */
    private fun mergeTranscriptSegments(segments: List<TranscriptSegment>): String {
        if (segments.isEmpty()) return ""

        // Filter out segments in overlap regions (keep only unique content)
        val uniqueSegments = segments
            .filterNot { it.isInOverlap }
            .sortedBy { it.sequenceNumber }

        return uniqueSegments.joinToString(" ") { it.text.trim() }
    }

    /**
     * Format milliseconds to timestamp string (MM:SS).
     */
    private fun formatTimestamp(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    // ========== Progress Tracking ==========

    /**
     * Get transcription progress for a session.
     */
    suspend fun getTranscriptionProgress(sessionId: String): TranscriptionProgress =
        withContext(ioDispatcher) {
            val chunks = audioChunkDao.getChunksBySession(sessionId)

            val totalChunks = chunks.size
            val transcribedChunks = chunks.count {
                it.transcriptionStatus == TranscriptionStatus.COMPLETED
            }
            val failedChunks = chunks.count {
                it.transcriptionStatus == TranscriptionStatus.FAILED ||
                        it.transcriptionStatus == TranscriptionStatus.UPLOAD_FAILED
            }
            val pendingChunks = chunks.count {
                it.transcriptionStatus == TranscriptionStatus.PENDING
            }

            val progressPercentage = if (totalChunks > 0) {
                ((transcribedChunks.toFloat() / totalChunks) * 100).toInt()
            } else {
                0
            }

            val status = when {
                // All completed
                transcribedChunks == totalChunks && failedChunks == 0 -> TranscriptionStatus.COMPLETED
                // All failed
                failedChunks == totalChunks -> TranscriptionStatus.FAILED
                // Some are still transcribing
                chunks.any { it.transcriptionStatus == TranscriptionStatus.TRANSCRIBING } -> TranscriptionStatus.TRANSCRIBING
                // Some are uploading
                chunks.any { it.transcriptionStatus == TranscriptionStatus.UPLOADING } -> TranscriptionStatus.UPLOADING
                // Some failed but can retry
                failedChunks > 0 -> TranscriptionStatus.UPLOAD_FAILED
                // None started yet
                else -> TranscriptionStatus.PENDING
            }

            TranscriptionProgress(
                sessionId = sessionId,
                totalChunks = totalChunks,
                transcribedChunks = transcribedChunks,
                failedChunks = failedChunks,
                pendingChunks = pendingChunks,
                progressPercentage = progressPercentage,
                currentChunkNumber = null,
                estimatedTimeRemaining = null,
                status = status
            )
        }

    /**
     * Check if a session has completed transcription.
     */
    suspend fun isTranscriptionComplete(sessionId: String): Boolean =
        withContext(ioDispatcher) {
            val progress = getTranscriptionProgress(sessionId)
            progress.totalChunks > 0 &&
                    progress.transcribedChunks == progress.totalChunks &&
                    progress.failedChunks == 0
        }

    // ========== Cleanup ==========

    /**
     * Delete all transcript segments for a session.
     */
    suspend fun deleteTranscriptSegments(sessionId: String): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                transcriptSegmentDao.deleteBySession(sessionId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    companion object {
        private const val TAG = "TranscriptionRepo"
        private const val MAX_RETRIES = 3
        private const val RETRY_INITIAL_DELAY_MS = 1000L
        private const val RETRY_MAX_DELAY_MS = 32000L
        private const val RETRY_BACKOFF_MULTIPLIER = 2.0
    }
}

/**
 * Custom exception for API errors.
 */
class ApiException(
    val code: Int,
    message: String
) : Exception("API Error [$code]: $message")

/**
 * Summary of transcription results.
 */
data class TranscriptionSummary(
    val totalChunks: Int,
    val successCount: Int,
    val failedCount: Int,
    val failedChunkIds: List<String>
)