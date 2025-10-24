package com.tachyon.transcripter.service

// SilenceDetector.kt

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Detects prolonged silence during recording.
 */
@Singleton
class SilenceDetector @Inject constructor() {

    private var listener: ((Long) -> Unit)? = null
    private var detectionJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private val detectionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var silenceStartTime: Long = 0
    private var isSilent = false

    companion object {
        const val SAMPLE_RATE = 44100
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val SILENCE_THRESHOLD_DB = -40f
        const val SILENCE_DURATION_THRESHOLD_MS = 10_000L  // 10 seconds
        const val SAMPLE_INTERVAL_MS = 500L  // Check every 500ms
    }

    /**
     * Set listener for silence detection.
     * Called with duration of silence in milliseconds.
     */
    fun setListener(listener: (Long) -> Unit) {
        this.listener = listener
    }

    /**
     * Start detecting silence.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startDetection() {
        if (detectionJob?.isActive == true) return

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            audioRecord?.startRecording()

            detectionJob = detectionScope.launch {
                val buffer = ShortArray(bufferSize)

                while (isActive) {
                    val readSize = audioRecord?.read(buffer, 0, bufferSize) ?: 0

                    if (readSize > 0) {
                        val amplitude = calculateAmplitude(buffer, readSize)
                        val db = amplitudeToDb(amplitude)

                        checkSilence(db)
                    }

                    delay(SAMPLE_INTERVAL_MS)
                }
            }
        } catch (e: Exception) {
            // Handle audio record initialization failure
        }
    }

    /**
     * Stop detecting silence.
     */
    fun stopDetection() {
        detectionJob?.cancel()
        detectionJob = null

        audioRecord?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                // Ignore
            }
        }
        audioRecord = null

        resetSilenceState()
    }

    /**
     * Calculate RMS amplitude from audio buffer.
     */
    private fun calculateAmplitude(buffer: ShortArray, size: Int): Double {
        var sum = 0.0
        for (i in 0 until size) {
            val sample = buffer[i].toDouble()
            sum += sample * sample
        }
        return sqrt(sum / size)
    }

    /**
     * Convert amplitude to decibels.
     */
    private fun amplitudeToDb(amplitude: Double): Float {
        return if (amplitude > 0) {
            (20 * log10(amplitude / 32768.0)).toFloat()
        } else {
            -100f  // Very quiet
        }
    }

    /**
     * Check if audio is silent and track duration.
     */
    private fun checkSilence(db: Float) {
        if (db < SILENCE_THRESHOLD_DB) {
            // Audio is silent
            if (!isSilent) {
                // Silence just started
                silenceStartTime = System.currentTimeMillis()
                isSilent = true
            } else {
                // Silence continuing
                val silenceDuration = System.currentTimeMillis() - silenceStartTime

                if (silenceDuration >= SILENCE_DURATION_THRESHOLD_MS) {
                    listener?.invoke(silenceDuration)
                }
            }
        } else {
            // Audio detected, reset silence tracking
            if (isSilent) {
                resetSilenceState()
            }
        }
    }

    /**
     * Reset silence tracking state.
     */
    private fun resetSilenceState() {
        silenceStartTime = 0
        isSilent = false
    }

    /**
     * Get current silence duration.
     */
    fun getCurrentSilenceDuration(): Long {
        return if (isSilent) {
            System.currentTimeMillis() - silenceStartTime
        } else {
            0L
        }
    }

    /**
     * Check if currently detecting silence.
     */
    fun isCurrentlySilent(): Boolean = isSilent

    /**
     * Clean up resources.
     */
    fun cleanup() {
        stopDetection()
        detectionScope.cancel()
    }
}