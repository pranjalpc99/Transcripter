package com.tachyon.transcripter.util

// AudioUtils.kt

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Utility object for audio-related operations.
 */
object AudioUtils {

    // Audio configuration constants
    const val SAMPLE_RATE = 44100
    const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    const val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC

    // Chunk configuration
    const val CHUNK_DURATION_MS = 30_000L  // 30 seconds
    const val OVERLAP_DURATION_MS = 2_000L  // 2 seconds
    const val EFFECTIVE_CHUNK_DURATION_MS = CHUNK_DURATION_MS - OVERLAP_DURATION_MS

    // Silence detection
    const val SILENCE_THRESHOLD_DB = -40f
    const val SILENCE_DURATION_THRESHOLD_MS = 10_000L

    // File format
    const val AUDIO_MIME_TYPE = "audio/mp4"
    const val AUDIO_FILE_EXTENSION = ".m4a"

    /**
     * Get minimum buffer size for AudioRecord.
     */
    fun getMinBufferSize(): Int {
        return AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )
    }

    /**
     * Calculate buffer size for given duration.
     *
     * @param durationMs Duration in milliseconds
     * @return Buffer size in bytes
     */
    fun calculateBufferSize(durationMs: Long): Int {
        val bytesPerSecond = SAMPLE_RATE * 2 // 16-bit = 2 bytes per sample
        return ((durationMs / 1000.0) * bytesPerSecond).toInt()
    }

    /**
     * Calculate RMS (Root Mean Square) amplitude from audio buffer.
     *
     * @param buffer Audio buffer
     * @param size Number of samples to process
     * @return RMS amplitude
     */
    fun calculateRmsAmplitude(buffer: ShortArray, size: Int): Double {
        var sum = 0.0
        for (i in 0 until size) {
            val sample = buffer[i].toDouble()
            sum += sample * sample
        }
        return sqrt(sum / size)
    }

    /**
     * Convert amplitude to decibels.
     *
     * @param amplitude RMS amplitude
     * @return Decibels (dB)
     */
    fun amplitudeToDecibels(amplitude: Double): Float {
        return if (amplitude > 0) {
            (20 * log10(amplitude / 32768.0)).toFloat()
        } else {
            -100f  // Very quiet
        }
    }

    /**
     * Check if audio is silent based on amplitude.
     *
     * @param amplitude RMS amplitude
     * @return True if silent
     */
    fun isSilent(amplitude: Double): Boolean {
        val db = amplitudeToDecibels(amplitude)
        return db < SILENCE_THRESHOLD_DB
    }

    /**
     * Estimate file size for recording duration.
     *
     * @param durationMs Duration in milliseconds
     * @return Estimated file size in bytes
     */
    fun estimateFileSize(durationMs: Long): Long {
        // M4A at 64kbps = 8KB per second
        val bytesPerSecond = 8 * 1024L
        return (durationMs / 1000) * bytesPerSecond
    }

    /**
     * Calculate bitrate from file size and duration.
     *
     * @param fileSizeBytes File size in bytes
     * @param durationMs Duration in milliseconds
     * @return Bitrate in kbps
     */
    fun calculateBitrate(fileSizeBytes: Long, durationMs: Long): Int {
        if (durationMs == 0L) return 0
        val durationSeconds = durationMs / 1000.0
        val bitsPerSecond = (fileSizeBytes * 8) / durationSeconds
        return (bitsPerSecond / 1000).toInt()
    }

    /**
     * Validate audio configuration.
     *
     * @return True if configuration is valid
     */
    fun validateAudioConfiguration(): Boolean {
        val bufferSize = getMinBufferSize()
        return bufferSize != AudioRecord.ERROR_BAD_VALUE &&
                bufferSize != AudioRecord.ERROR
    }

    /**
     * Get audio quality string.
     *
     * @param sampleRate Sample rate in Hz
     * @param bitrate Bitrate in kbps
     * @return Quality string (e.g., "High Quality")
     */
    fun getAudioQualityString(sampleRate: Int, bitrate: Int): String {
        return when {
            sampleRate >= 44100 && bitrate >= 128 -> "High Quality"
            sampleRate >= 22050 && bitrate >= 64 -> "Medium Quality"
            else -> "Low Quality"
        }
    }
}