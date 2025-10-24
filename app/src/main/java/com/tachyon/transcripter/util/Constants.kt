package com.tachyon.transcripter.util

import android.media.AudioManager

// util/Constants.kt
object Constants {

    // Recording
    const val CHUNK_DURATION_MS = 30_000L
    const val OVERLAP_DURATION_MS = 2_000L
    const val AUDIO_SAMPLE_RATE = 44100
    const val AUDIO_BIT_RATE = 64000
    const val AUDIO_CHANNELS = 1  // Mono

    // Storage
    const val MIN_STORAGE_MB = 100L
    const val ESTIMATED_MB_PER_MINUTE = 1.5
    const val RECORDINGS_DIR = "recordings"

    // Transcription
    const val MAX_TRANSCRIPTION_RETRIES = 3
    const val RETRY_INITIAL_DELAY_MS = 1000L
    const val RETRY_MAX_DELAY_MS = 32000L
    const val RETRY_BACKOFF_MULTIPLIER = 2.0

    // Summary
    const val MAX_SUMMARY_RETRIES = 3
    const val SUMMARY_MAX_TOKENS = 1000
    const val SUMMARY_TEMPERATURE = 0.7f

    // Notifications
    const val NOTIFICATION_CHANNEL_ID = "recording_channel"
    const val NOTIFICATION_ID = 1001
    const val NOTIFICATION_CHANNEL_NAME = "Voice Recording"

    // Silence Detection
    const val SILENCE_THRESHOLD_DB = -40f
    const val SILENCE_DURATION_THRESHOLD_MS = 10_000L
    const val SILENCE_CHECK_INTERVAL_MS = 500L

    // Audio Focus
    const val AUDIO_FOCUS_GAIN_TYPE = AudioManager.AUDIOFOCUS_GAIN

    // Permissions
    val REQUIRED_PERMISSIONS = arrayOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.FOREGROUND_SERVICE,
        android.Manifest.permission.POST_NOTIFICATIONS
    )

    // WorkManager
    const val WORK_TAG_TRANSCRIPTION = "transcription"
    const val WORK_TAG_SUMMARY = "summary"
    const val WORK_TAG_CLEANUP = "cleanup"
}