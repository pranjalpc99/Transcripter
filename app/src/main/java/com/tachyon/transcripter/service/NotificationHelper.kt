package com.tachyon.transcripter.service

// NotificationHelper.kt

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.tachyon.transcripter.MainActivity
import com.tachyon.transcripter.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages notifications for the recording service.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "recording_channel"
        const val CHANNEL_NAME = "Voice Recording"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"
    }

    init {
        createNotificationChannel()
    }

    /**
     * Create notification channel (Android 8.0+).
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows recording status and controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Build notification for recording state.
     */
    fun buildRecordingNotification(
        duration: String = "00:00:00",
        isPaused: Boolean = false,
        pauseReason: String? = null
    ): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mic)  // You'll need to add this icon
            .setContentTitle(if (isPaused) "Recording Paused" else "Recording")
            .setContentText(buildContentText(duration, isPaused, pauseReason))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Add action buttons
        if (isPaused) {
            addResumeAction(builder)
        } else {
            addPauseAction(builder)
        }
        addStopAction(builder)

        return builder.build()
    }

    /**
     * Build notification for processing state.
     */
    fun buildProcessingNotification(status: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_processing)
            .setContentTitle("Processing Recording")
            .setContentText(status)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Build notification for completion.
     */
    fun buildCompletionNotification(sessionId: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                putExtra("session_id", sessionId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check)
            .setContentTitle("Recording Complete")
            .setContentText("Tap to view summary")
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    /**
     * Update notification with new content.
     */
    fun updateNotification(
        duration: String = "00:00:00",
        isPaused: Boolean = false,
        pauseReason: String? = null
    ) {
        val notification = buildRecordingNotification(duration, isPaused, pauseReason)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Show notification.
     */
    fun showNotification(notification: Notification) {
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Cancel notification.
     */
    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    /**
     * Build content text based on state.
     */
    private fun buildContentText(duration: String, isPaused: Boolean, pauseReason: String?): String {
        return if (isPaused && pauseReason != null) {
            "$duration • Paused: ${formatPauseReason(pauseReason)}"
        } else if (isPaused) {
            "$duration • Paused"
        } else {
            duration
        }
    }

    /**
     * Format pause reason for display.
     */
    private fun formatPauseReason(reason: String): String {
        return when (reason) {
            "phone_call" -> "Phone call"
            "audio_focus_loss" -> "Another app using audio"
            "low_storage" -> "Low storage"
            "user_action" -> "Manual pause"
            else -> reason.replace("_", " ").capitalize()
        }
    }

    /**
     * Add pause action to notification.
     */
    private fun addPauseAction(builder: NotificationCompat.Builder) {
        val pauseIntent = PendingIntent.getService(
            context,
            0,
            Intent(context, RecordingService::class.java).apply {
                action = ACTION_PAUSE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        builder.addAction(
            R.drawable.ic_pause,
            "Pause",
            pauseIntent
        )
    }

    /**
     * Add resume action to notification.
     */
    private fun addResumeAction(builder: NotificationCompat.Builder) {
        val resumeIntent = PendingIntent.getService(
            context,
            0,
            Intent(context, RecordingService::class.java).apply {
                action = ACTION_RESUME
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        builder.addAction(
            R.drawable.ic_play,
            "Resume",
            resumeIntent
        )
    }

    /**
     * Add stop action to notification.
     */
    private fun addStopAction(builder: NotificationCompat.Builder) {
        val stopIntent = PendingIntent.getService(
            context,
            0,
            Intent(context, RecordingService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        builder.addAction(
            R.drawable.ic_stop,
            "Stop",
            stopIntent
        )
    }
}