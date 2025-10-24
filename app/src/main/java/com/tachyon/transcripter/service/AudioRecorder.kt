package com.tachyon.transcripter.service

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

// service/AudioRecorder.kt
class AudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaRecorder: MediaRecorder? = null
    private var isPaused = false
    private var recordingStartTime = 0L
    private var pauseTime = 0L
    private var totalPauseDuration = 0L

    @RequiresApi(Build.VERSION_CODES.N)
    fun startRecording(outputFile: File, maxDuration: Long) {
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(64000)  // 64 kbps
            setAudioSamplingRate(44100)     // 44.1 kHz
            setMaxDuration(maxDuration.toInt())
            setOutputFile(outputFile.absolutePath)

            prepare()
            start()

            recordingStartTime = System.currentTimeMillis()
            totalPauseDuration = 0L
            isPaused = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun pause() {
        if (!isPaused) {
            mediaRecorder?.pause()
            pauseTime = System.currentTimeMillis()
            isPaused = true
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun resume() {
        if (isPaused) {
            mediaRecorder?.resume()
            totalPauseDuration += System.currentTimeMillis() - pauseTime
            isPaused = false
        }
    }

    fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // Handle stop errors
        } finally {
            mediaRecorder = null
        }
    }

    fun getActualDuration(): Long {
        return System.currentTimeMillis() - recordingStartTime - totalPauseDuration
    }

    fun release() {
        mediaRecorder?.release()
        mediaRecorder = null
    }
}