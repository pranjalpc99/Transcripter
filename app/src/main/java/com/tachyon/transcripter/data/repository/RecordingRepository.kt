package com.tachyon.transcripter.data.repository

import com.tachyon.transcripter.data.local.dao.AudioChunkDao
import com.tachyon.transcripter.data.local.dao.RecordingSessionDao
import com.tachyon.transcripter.data.local.entity.AudioChunk
import com.tachyon.transcripter.data.local.entity.RecordingSession
import com.tachyon.transcripter.data.local.entity.SessionStatus
import com.tachyon.transcripter.domain.model.TranscriptionStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing recording sessions and audio chunks.
 * Provides a clean API for recording operations and state management.
 */
@Singleton
class RecordingRepository @Inject constructor(
    private val sessionDao: RecordingSessionDao,
    private val chunkDao: AudioChunkDao,
    private val fileRepository: FileRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    // ========== Session Management ==========

    /**
     * Create a new recording session in the database.
     * @return The created session
     */
    suspend fun createSession(session: RecordingSession): Result<RecordingSession> =
        withContext(ioDispatcher) {
            try {
                // Ensure session directory exists
                val sessionDir = fileRepository.getSessionDirectory(session.id)
                if (!sessionDir.exists()) {
                    sessionDir.mkdirs()
                }

                sessionDao.insert(session)
                Result.success(session)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Insert a session (wrapper for direct insert).
     */
    suspend fun insertSession(session: RecordingSession) = withContext(ioDispatcher) {
        sessionDao.insert(session)
    }

    /**
     * Update an existing session.
     */
    suspend fun updateSession(session: RecordingSession) = withContext(ioDispatcher) {
        sessionDao.update(session)
    }

    /**
     * Get a session by ID.
     */
    suspend fun getSessionById(sessionId: String): RecordingSession? =
        withContext(ioDispatcher) {
            sessionDao.getById(sessionId)
        }

    /**
     * Observe a session by ID (reactive).
     */
    fun observeSession(sessionId: String): Flow<RecordingSession?> {
        return sessionDao.observeById(sessionId)
    }

    /**
     * Get all recording sessions.
     */
    fun observeAllSessions(): Flow<List<RecordingSession>> {
        return sessionDao.observeAll()
    }

    /**
     * Update session status.
     */
    suspend fun updateSessionStatus(
        sessionId: String,
        status: SessionStatus
    ) = withContext(ioDispatcher) {
        sessionDao.updateStatus(sessionId, status, System.currentTimeMillis())
    }

    /**
     * Finalize a recording session when stopped.
     * Updates end time, total duration, and status.
     */
    suspend fun finalizeSession(
        sessionId: String,
        endTime: Long,
        totalDuration: Long,
        pauseDuration: Long
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            val session = sessionDao.getById(sessionId)
                ?: return@withContext Result.failure(Exception("Session not found"))

            val updatedSession = session.copy(
                endTime = endTime,
                totalDurationMs = totalDuration,
                pauseDurationMs = pauseDuration,
                status = SessionStatus.STOPPED,
                updatedAt = System.currentTimeMillis()
            )

            sessionDao.update(updatedSession)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get incomplete sessions (for process death recovery).
     * Returns sessions that were recording or paused when app died.
     */
    suspend fun getIncompleteSessions(): List<RecordingSession> =
        withContext(ioDispatcher) {
            sessionDao.getByStatuses(
                listOf(
                    SessionStatus.RECORDING,
                    SessionStatus.PAUSED
                )
            )
        }

    /**
     * Delete a recording session and all associated data.
     */
    suspend fun deleteSession(sessionId: String): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                // Delete from database (cascade deletes chunks and segments)
                sessionDao.deleteById(sessionId)

                // Delete files
                fileRepository.deleteSession(sessionId)

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ========== Chunk Management ==========

    /**
     * Insert a new audio chunk.
     */
    suspend fun insertChunk(chunk: AudioChunk): Result<AudioChunk> =
        withContext(ioDispatcher) {
            try {
                chunkDao.insert(chunk)

                // Increment session chunk count
                sessionDao.incrementChunkCount(chunk.sessionId)

                Result.success(chunk)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Update an existing chunk.
     */
    suspend fun updateChunk(chunk: AudioChunk) = withContext(ioDispatcher) {
        chunkDao.update(chunk)
    }

    /**
     * Get all chunks for a session.
     */
    suspend fun getChunksBySession(sessionId: String): List<AudioChunk> =
        withContext(ioDispatcher) {
            chunkDao.getChunksBySession(sessionId)
        }

    /**
     * Observe chunks for a session (reactive).
     */
    fun observeChunksBySession(sessionId: String): Flow<List<AudioChunk>> {
        return chunkDao.observeChunksBySession(sessionId)
    }

    /**
     * Get chunks by transcription status.
     */
    suspend fun getChunksByStatus(
        status: TranscriptionStatus,
        limit: Int = 10
    ): List<AudioChunk> = withContext(ioDispatcher) {
        chunkDao.getChunksByStatus(status, limit)
    }

    /**
     * Update chunk transcription status.
     */
    suspend fun updateChunkTranscriptionStatus(
        chunkId: String,
        status: TranscriptionStatus
    ) = withContext(ioDispatcher) {
        chunkDao.updateTranscriptionStatus(chunkId, status)
    }

    /**
     * Increment upload attempt count for a chunk.
     */
    suspend fun incrementChunkUploadAttempt(chunkId: String) =
        withContext(ioDispatcher) {
            chunkDao.incrementUploadAttempt(chunkId)
        }

    /**
     * Get the count of completed transcriptions for a session.
     */
    suspend fun getCompletedTranscriptionCount(sessionId: String): Int =
        withContext(ioDispatcher) {
            chunkDao.getCompletedTranscriptionCount(sessionId)
        }

    // ========== Statistics & Monitoring ==========

    /**
     * Get recording statistics for a session.
     */
    suspend fun getSessionStatistics(sessionId: String): Result<SessionStatistics> =
        withContext(ioDispatcher) {
            try {
                val session = sessionDao.getById(sessionId)
                    ?: return@withContext Result.failure(Exception("Session not found"))

                val chunks = chunkDao.getChunksBySession(sessionId)
                val completedCount = chunkDao.getCompletedTranscriptionCount(sessionId)

                val stats = SessionStatistics(
                    sessionId = sessionId,
                    totalChunks = chunks.size,
                    transcribedChunks = completedCount,
                    totalDurationMs = session.totalDurationMs,
                    pauseDurationMs = session.pauseDurationMs,
                    status = session.status,
                    storageSizeBytes = fileRepository.calculateSessionSize(sessionId)
                )

                Result.success(stats)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Check if a session is ready for summary generation.
     * A session is ready when all chunks are transcribed.
     */
    suspend fun isSessionReadyForSummary(sessionId: String): Boolean =
        withContext(ioDispatcher) {
            try {
                val session = sessionDao.getById(sessionId) ?: return@withContext false
                val chunks = chunkDao.getChunksBySession(sessionId)
                val completedCount = chunkDao.getCompletedTranscriptionCount(sessionId)

                chunks.isNotEmpty() && completedCount == chunks.size
            } catch (e: Exception) {
                false
            }
        }

    /**
     * Get total storage used by all sessions.
     */
    suspend fun getTotalStorageUsed(): Long = withContext(ioDispatcher) {
        fileRepository.getTotalStorageUsed()
    }

    /**
     * Get available storage space.
     */
    suspend fun getAvailableStorage(): Long = withContext(ioDispatcher) {
        fileRepository.checkAvailableStorage()
    }

    /**
     * Check if there's enough storage to start recording.
     * @param estimatedDurationMinutes Estimated recording duration in minutes
     * @return Result with available minutes, or error if insufficient
     */
    suspend fun checkStorageForRecording(
        estimatedDurationMinutes: Int = 60
    ): Result<StorageCheckResult> = withContext(ioDispatcher) {
        try {
            val availableBytes = fileRepository.checkAvailableStorage()
            val minRequired = FileRepository.MIN_STORAGE_MB * 1024 * 1024

            if (availableBytes < minRequired) {
                return@withContext Result.failure(
                    Exception("Insufficient storage. Need at least ${FileRepository.MIN_STORAGE_MB} MB free.")
                )
            }

            // Calculate max recording duration with available space
            val estimatedBytesPerMinute = FileRepository.ESTIMATED_MB_PER_MINUTE * 1024 * 1024
            val maxMinutes = ((availableBytes - minRequired) / estimatedBytesPerMinute).toInt()

            val result = StorageCheckResult(
                availableBytes = availableBytes,
                maxRecordingMinutes = maxMinutes,
                hasEnoughSpace = maxMinutes >= estimatedDurationMinutes
            )

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clean up orphaned files (files not in database).
     */
    suspend fun cleanupOrphanedFiles(): Result<Int> = withContext(ioDispatcher) {
        try {
            val deletedCount = fileRepository.cleanupOrphanedFiles()
            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== Pause/Resume Management ==========

    /**
     * Record a pause event.
     * Updates session status and records pause reason.
     */
    suspend fun recordPause(
        sessionId: String,
        reason: String?
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            val session = sessionDao.getById(sessionId)
                ?: return@withContext Result.failure(Exception("Session not found"))

            val updatedSession = session.copy(
                status = SessionStatus.PAUSED,
                pauseReason = reason,
                updatedAt = System.currentTimeMillis()
            )

            sessionDao.update(updatedSession)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Record a resume event.
     * Updates session status back to recording.
     */
    suspend fun recordResume(sessionId: String): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                val session = sessionDao.getById(sessionId)
                    ?: return@withContext Result.failure(Exception("Session not found"))

                val updatedSession = session.copy(
                    status = SessionStatus.RECORDING,
                    pauseReason = null,
                    updatedAt = System.currentTimeMillis()
                )

                sessionDao.update(updatedSession)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

/**
 * Statistics for a recording session.
 */
data class SessionStatistics(
    val sessionId: String,
    val totalChunks: Int,
    val transcribedChunks: Int,
    val totalDurationMs: Long,
    val pauseDurationMs: Long,
    val status: SessionStatus,
    val storageSizeBytes: Long
)

/**
 * Result of storage check.
 */
data class StorageCheckResult(
    val availableBytes: Long,
    val maxRecordingMinutes: Int,
    val hasEnoughSpace: Boolean
)