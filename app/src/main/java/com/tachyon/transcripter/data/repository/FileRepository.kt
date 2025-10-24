package com.tachyon.transcripter.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing file operations and storage.
 * Handles all file I/O for audio recordings.
 */
@Singleton
class FileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Get the root recordings directory.
     * Creates it if it doesn't exist.
     */
    private val recordingsDir: File
        get() = File(context.filesDir, RECORDINGS_DIR).apply {
            if (!exists()) mkdirs()
        }

    // ========== Session Directory Management ==========

    /**
     * Get the directory for a specific session.
     * Creates it if it doesn't exist.
     *
     * @param sessionId The session ID
     * @return The session directory
     */
    fun getSessionDirectory(sessionId: String): File {
        return File(recordingsDir, "session_$sessionId").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Check if a session directory exists.
     */
    fun sessionDirectoryExists(sessionId: String): Boolean {
        return getSessionDirectory(sessionId).exists()
    }

    // ========== Chunk File Management ==========

    /**
     * Get the file for a specific audio chunk.
     * Does not create the file, just returns the File object.
     *
     * @param sessionId The session ID
     * @param chunkNumber The chunk number (0-based)
     * @return The chunk file
     */
    fun getChunkFile(sessionId: String, chunkNumber: Int): File {
        val sessionDir = getSessionDirectory(sessionId)
        val paddedNumber = chunkNumber.toString().padStart(3, '0')
        return File(sessionDir, "chunk_$paddedNumber.m4a")
    }

    /**
     * Check if a chunk file exists.
     */
    fun chunkFileExists(sessionId: String, chunkNumber: Int): Boolean {
        return getChunkFile(sessionId, chunkNumber).exists()
    }

    /**
     * Get the size of a chunk file in bytes.
     */
    suspend fun getChunkFileSize(sessionId: String, chunkNumber: Int): Long =
        withContext(ioDispatcher) {
            val file = getChunkFile(sessionId, chunkNumber)
            if (file.exists()) file.length() else 0L
        }

    /**
     * Get all chunk files for a session, ordered by chunk number.
     */
    suspend fun getChunkFiles(sessionId: String): List<File> =
        withContext(ioDispatcher) {
            getSessionDirectory(sessionId)
                .listFiles { file -> file.extension == "m4a" && file.name.startsWith("chunk_") }
                ?.sortedBy { it.name }
                ?: emptyList()
        }

    /**
     * Delete a specific chunk file.
     */
    suspend fun deleteChunkFile(sessionId: String, chunkNumber: Int): Boolean =
        withContext(ioDispatcher) {
            try {
                getChunkFile(sessionId, chunkNumber).delete()
            } catch (e: Exception) {
                false
            }
        }

    // ========== Storage Calculations ==========

    /**
     * Calculate the total size of all files in a session.
     *
     * @param sessionId The session ID
     * @return Total size in bytes
     */
    suspend fun calculateSessionSize(sessionId: String): Long =
        withContext(ioDispatcher) {
            try {
                getSessionDirectory(sessionId)
                    .walkTopDown()
                    .filter { it.isFile }
                    .sumOf { it.length() }
            } catch (e: Exception) {
                0L
            }
        }

    /**
     * Get the total storage used by all recording sessions.
     *
     * @return Total size in bytes
     */
    suspend fun getTotalStorageUsed(): Long = withContext(ioDispatcher) {
        try {
            recordingsDir
                .walkTopDown()
                .filter { it.isFile }
                .sumOf { it.length() }
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Check available storage space.
     *
     * @return Available bytes
     */
    suspend fun checkAvailableStorage(): Long = withContext(ioDispatcher) {
        try {
            recordingsDir.usableSpace
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Check if there's enough storage to start recording.
     *
     * @param requiredBytes Minimum required bytes (default: MIN_STORAGE_MB)
     * @return True if enough storage is available
     */
    suspend fun hasEnoughStorage(
        requiredBytes: Long = MIN_STORAGE_MB * 1024 * 1024
    ): Boolean = withContext(ioDispatcher) {
        checkAvailableStorage() >= requiredBytes
    }

    /**
     * Calculate estimated storage needed for a recording duration.
     *
     * @param durationMinutes Recording duration in minutes
     * @return Estimated bytes needed
     */
    fun calculateEstimatedStorage(durationMinutes: Int): Long {
        return (durationMinutes * ESTIMATED_MB_PER_MINUTE * 1024 * 1024).toLong()
    }

    /**
     * Calculate maximum recording duration possible with available storage.
     *
     * @return Maximum duration in minutes
     */
    suspend fun calculateMaxRecordingDuration(): Int = withContext(ioDispatcher) {
        val availableBytes = checkAvailableStorage()
        val usableBytes = availableBytes - (MIN_STORAGE_MB * 1024 * 1024)

        if (usableBytes <= 0) return@withContext 0

        val bytesPerMinute = ESTIMATED_MB_PER_MINUTE * 1024 * 1024
        (usableBytes / bytesPerMinute).toInt()
    }

    // ========== Session Deletion ==========

    /**
     * Delete all files for a session.
     *
     * @param sessionId The session ID
     * @return True if deletion was successful
     */
    suspend fun deleteSession(sessionId: String): Boolean =
        withContext(ioDispatcher) {
            try {
                getSessionDirectory(sessionId).deleteRecursively()
            } catch (e: Exception) {
                false
            }
        }

    /**
     * Delete multiple sessions.
     *
     * @param sessionIds List of session IDs to delete
     * @return Number of successfully deleted sessions
     */
    suspend fun deleteSessions(sessionIds: List<String>): Int =
        withContext(ioDispatcher) {
            sessionIds.count { deleteSession(it) }
        }

    // ========== Cleanup Operations ==========

    /**
     * Clean up orphaned files (files not associated with any session in database).
     * This should be called with a list of valid session IDs from the database.
     *
     * @param validSessionIds List of session IDs that should exist
     * @return Number of orphaned directories deleted
     */
    suspend fun cleanupOrphanedFiles(
        validSessionIds: List<String>? = null
    ): Int = withContext(ioDispatcher) {
        try {
            val sessionDirs = recordingsDir.listFiles { file ->
                file.isDirectory && file.name.startsWith("session_")
            } ?: return@withContext 0

            var deletedCount = 0

            for (dir in sessionDirs) {
                val sessionId = dir.name.removePrefix("session_")

                // If we have a list of valid IDs and this isn't one of them, delete it
                if (validSessionIds != null && sessionId !in validSessionIds) {
                    if (dir.deleteRecursively()) {
                        deletedCount++
                    }
                }
            }

            deletedCount
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Clean up old sessions (older than a certain number of days).
     *
     * @param daysOld Delete sessions older than this many days
     * @return Number of sessions deleted
     */
    suspend fun cleanupOldSessions(daysOld: Int = 30): Int =
        withContext(ioDispatcher) {
            try {
                val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)

                val sessionDirs = recordingsDir.listFiles { file ->
                    file.isDirectory && file.name.startsWith("session_")
                } ?: return@withContext 0

                var deletedCount = 0

                for (dir in sessionDirs) {
                    // Check directory modification time
                    if (dir.lastModified() < cutoffTime) {
                        if (dir.deleteRecursively()) {
                            deletedCount++
                        }
                    }
                }

                deletedCount
            } catch (e: Exception) {
                0
            }
        }

    /**
     * Delete temporary/incomplete files.
     * Removes files that may have been left from incomplete recordings.
     */
    suspend fun cleanupTempFiles(): Int = withContext(ioDispatcher) {
        try {
            var deletedCount = 0

            // Look for any .tmp files or files without proper naming
            recordingsDir.walkTopDown()
                .filter { it.isFile && (it.extension == "tmp" || !it.name.startsWith("chunk_")) }
                .forEach {
                    if (it.delete()) deletedCount++
                }

            deletedCount
        } catch (e: Exception) {
            0
        }
    }

    // ========== File Validation ==========

    /**
     * Validate that a chunk file exists and is readable.
     *
     * @param sessionId Session ID
     * @param chunkNumber Chunk number
     * @return True if file is valid
     */
    suspend fun validateChunkFile(sessionId: String, chunkNumber: Int): Boolean =
        withContext(ioDispatcher) {
            try {
                val file = getChunkFile(sessionId, chunkNumber)
                file.exists() && file.canRead() && file.length() > 0
            } catch (e: Exception) {
                false
            }
        }

    /**
     * Validate all chunk files for a session.
     *
     * @param sessionId Session ID
     * @param expectedChunkCount Expected number of chunks
     * @return List of missing or invalid chunk numbers
     */
    suspend fun validateSessionFiles(
        sessionId: String,
        expectedChunkCount: Int
    ): List<Int> = withContext(ioDispatcher) {
        val invalidChunks = mutableListOf<Int>()

        for (i in 0 until expectedChunkCount) {
            if (!validateChunkFile(sessionId, i)) {
                invalidChunks.add(i)
            }
        }

        invalidChunks
    }

    // ========== Metadata Operations ==========

    /**
     * Create a metadata file for a session (optional, for backup purposes).
     * Stores JSON metadata about the session.
     */
    suspend fun saveSessionMetadata(
        sessionId: String,
        metadata: String
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            val metadataFile = File(getSessionDirectory(sessionId), "metadata.json")
            metadataFile.writeText(metadata)
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(e)
        }
    }

    /**
     * Read session metadata file.
     */
    suspend fun readSessionMetadata(sessionId: String): Result<String> =
        withContext(ioDispatcher) {
            try {
                val metadataFile = File(getSessionDirectory(sessionId), "metadata.json")
                if (metadataFile.exists()) {
                    Result.success(metadataFile.readText())
                } else {
                    Result.failure(Exception("Metadata file not found"))
                }
            } catch (e: IOException) {
                Result.failure(e)
            }
        }

    // ========== Backup & Export ==========

    /**
     * Get the external storage directory for exports (if available).
     * Falls back to internal storage if external is unavailable.
     */
    fun getExportDirectory(): File {
        return context.getExternalFilesDir(null) ?: context.filesDir
    }

    /**
     * Copy a session to export directory.
     *
     * @param sessionId Session ID to export
     * @param exportName Optional custom name for export directory
     * @return Path to exported directory
     */
    suspend fun exportSession(
        sessionId: String,
        exportName: String = "export_$sessionId"
    ): Result<String> = withContext(ioDispatcher) {
        try {
            val sourceDir = getSessionDirectory(sessionId)
            if (!sourceDir.exists()) {
                return@withContext Result.failure(Exception("Session directory not found"))
            }

            val exportDir = File(getExportDirectory(), exportName)
            if (exportDir.exists()) {
                exportDir.deleteRecursively()
            }
            exportDir.mkdirs()

            // Copy all files
            sourceDir.copyRecursively(exportDir, overwrite = true)

            Result.success(exportDir.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== Utility Functions ==========

    /**
     * Format bytes to human-readable string.
     */
    fun formatStorageSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.2f KB".format(bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
            else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    /**
     * Get storage statistics.
     */
    suspend fun getStorageStats(): StorageStats = withContext(ioDispatcher) {
        StorageStats(
            totalUsed = getTotalStorageUsed(),
            availableSpace = checkAvailableStorage(),
            sessionCount = recordingsDir.listFiles { it.isDirectory }?.size ?: 0,
            maxRecordingMinutes = calculateMaxRecordingDuration()
        )
    }

    companion object {
        const val RECORDINGS_DIR = "recordings"
        const val MIN_STORAGE_MB = 100L
        const val ESTIMATED_MB_PER_MINUTE = 1.5  // M4A at 64kbps
    }
}

/**
 * Storage statistics data class.
 */
data class StorageStats(
    val totalUsed: Long,
    val availableSpace: Long,
    val sessionCount: Int,
    val maxRecordingMinutes: Int
)