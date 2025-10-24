package com.tachyon.transcripter.service

// StorageMonitor.kt

import android.content.Context
import android.os.Environment
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors available storage space during recording.
 */
@Singleton
class StorageMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var listener: ((Long) -> Unit)? = null
    private var monitoringJob: Job? = null
    private val monitoringScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val MIN_STORAGE_BYTES = 100L * 1024 * 1024  // 100 MB
        const val CHECK_INTERVAL_MS = 30_000L  // 30 seconds
    }

    /**
     * Set listener for storage changes.
     * Called when storage drops below threshold.
     */
    fun setListener(listener: (Long) -> Unit) {
        this.listener = listener
    }

    /**
     * Start monitoring storage.
     */
    fun startMonitoring() {
        if (monitoringJob?.isActive == true) return

        monitoringJob = monitoringScope.launch {
            while (isActive) {
                val availableBytes = getAvailableStorageBytes()
                listener?.invoke(availableBytes)
                delay(CHECK_INTERVAL_MS)
            }
        }
    }

    /**
     * Stop monitoring storage.
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    /**
     * Get available storage in bytes.
     */
    fun getAvailableStorageBytes(): Long {
        return try {
            val path = context.filesDir
            val stat = StatFs(path.absolutePath)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Get total storage in bytes.
     */
    fun getTotalStorageBytes(): Long {
        return try {
            val path = context.filesDir
            val stat = StatFs(path.absolutePath)
            stat.blockCountLong * stat.blockSizeLong
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Check if there's enough storage available.
     */
    fun hasEnoughStorage(requiredBytes: Long = MIN_STORAGE_BYTES): Boolean {
        return getAvailableStorageBytes() >= requiredBytes
    }

    /**
     * Get storage usage percentage.
     */
    fun getStorageUsagePercentage(): Int {
        val total = getTotalStorageBytes()
        if (total == 0L) return 0

        val used = total - getAvailableStorageBytes()
        return ((used.toDouble() / total) * 100).toInt()
    }

    /**
     * Calculate estimated recording time with available storage.
     * @param bytesPerSecond Estimated bytes per second for recording
     * @return Estimated seconds of recording time
     */
    fun getEstimatedRecordingTime(bytesPerSecond: Long = 8000): Long {
        val availableBytes = getAvailableStorageBytes() - MIN_STORAGE_BYTES
        return if (availableBytes > 0) {
            availableBytes / bytesPerSecond
        } else {
            0L
        }
    }

    /**
     * Format bytes to human-readable string.
     */
    fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.2f KB".format(bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
            else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    /**
     * Clean up when done.
     */
    fun cleanup() {
        stopMonitoring()
        monitoringScope.cancel()
    }
}