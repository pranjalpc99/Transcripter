package com.tachyon.transcripter.service

import android.Manifest
import android.app.Service
import android.app.Service.START_STICKY
import android.app.Service.STOP_FOREGROUND_REMOVE
import android.content.Intent
import android.media.AudioManager
import android.os.Binder
import android.os.IBinder
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import androidx.core.app.ServiceCompat
import androidx.core.app.ServiceCompat.startForeground
import androidx.core.app.ServiceCompat.stopForeground
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.tachyon.transcripter.data.local.entity.RecordingSession
import com.tachyon.transcripter.data.local.entity.SessionStatus
import com.tachyon.transcripter.data.repository.FileRepository
import com.tachyon.transcripter.data.repository.RecordingRepository
import com.tachyon.transcripter.worker.SummaryWorker
import com.tachyon.transcripter.worker.TestWorker
import com.tachyon.transcripter.worker.TranscriptionWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// service/RecordingService.kt
// RecordingService.kt

/**
 * Foreground service for managing audio recording.
 */
@AndroidEntryPoint
class RecordingService : Service() {

    @Inject
    lateinit var recordingRepository: RecordingRepository

    @Inject
    lateinit var fileRepository: FileRepository

    @Inject
    lateinit var audioRecorder: AudioRecorder

    @Inject
    lateinit var chunkManager: ChunkManager

    @Inject
    lateinit var interruptionHandler: InterruptionHandler

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var storageMonitor: StorageMonitor

    @Inject
    lateinit var serviceBridge: ServiceBridge

    val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _serviceState = MutableStateFlow<ServiceState>(ServiceState.Idle)
    val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()

    private var currentSessionId: String? = null
    private var recordingStartTime: Long = 0
    private var pauseStartTime: Long = 0
    private var totalPauseDuration: Long = 0

    private val binder = RecordingBinder()

    inner class RecordingBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun onCreate() {
        super.onCreate()
        setupInterruptionHandlers()
        serviceBridge.bindService(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sessionId = intent?.getStringExtra(EXTRA_SESSION_ID)

        when (intent?.action) {
            ACTION_START -> startRecording(sessionId)
            ACTION_PAUSE -> pauseRecording(sessionId, intent.getStringExtra(EXTRA_PAUSE_REASON))
            ACTION_RESUME -> resumeRecording(sessionId)
            ACTION_STOP -> stopRecording(sessionId)
            ACTION_RECOVER -> recoverFromProcessDeath()
        }
        return START_STICKY
    }

    /**
     * Set up interruption handlers.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun setupInterruptionHandlers() {
        // Set up audio focus listener
        interruptionHandler.audioFocusManager.setListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    currentSessionId?.let { pauseRecording(it, "audio_focus_loss") }
                }
            }
        }

        // Set up phone state listener
        interruptionHandler.phoneStateHandler.setListener { callState ->
            when (callState) {
                TelephonyManager.CALL_STATE_RINGING,
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    currentSessionId?.let { pauseRecording(it, "phone_call") }
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    // Optionally auto-resume after call ends
                }
            }
        }

        // Set up audio device monitor
        interruptionHandler.audioDeviceMonitor.setListener { event ->
            when (event) {
                is AudioDeviceEvent.Disconnected -> {
                    if (event.deviceType == "bluetooth" || event.deviceType == "wired_headset") {
                        currentSessionId?.let {
                            pauseRecording(it, "${event.deviceType}_disconnected")
                        }
                    }
                }
                is AudioDeviceEvent.Connected -> {
                    // Device connected, could auto-resume if desired
                }
            }
        }

        // Set up storage monitor
        storageMonitor.setListener { availableBytes ->
            if (availableBytes < StorageMonitor.MIN_STORAGE_BYTES) {
                currentSessionId?.let { pauseRecording(it, "low_storage") }
            }
        }

        // Start monitoring
        interruptionHandler.startMonitoring()
    }

    private fun startRecording(sessionId: String?) {
        serviceScope.launch {
            try {
                // Check storage
                if (!storageMonitor.hasEnoughStorage()) {
                    _serviceState.value = ServiceState.Error("Insufficient storage")
                    return@launch
                }

                // Use provided session ID or get from intent
                val id = sessionId ?: currentSessionId ?: run {
                    _serviceState.value = ServiceState.Error("No session ID provided")
                    return@launch
                }

                currentSessionId = id
                recordingStartTime = System.currentTimeMillis()
                totalPauseDuration = 0

                // Start foreground
                val notification = notificationHelper.buildRecordingNotification()
                startForeground(NOTIFICATION_ID, notification)

                // Start recording
                chunkManager.startChunking(id)
                _serviceState.value = ServiceState.Recording(id, 0)

            } catch (e: Exception) {
                _serviceState.value = ServiceState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun pauseRecording(sessionId: String?, reason: String? = null) {
        serviceScope.launch {
            val id = sessionId ?: currentSessionId ?: return@launch

            chunkManager.pauseChunking()
            pauseStartTime = System.currentTimeMillis()

            recordingRepository.updateSessionStatus(id, SessionStatus.PAUSED)
            _serviceState.value = ServiceState.Paused(id, reason)

            // Update notification with resume action
            notificationHelper.updateNotification(isPaused = true, pauseReason = reason)
        }
    }

    private fun resumeRecording(sessionId: String?) {
        serviceScope.launch {
            val id = sessionId ?: currentSessionId ?: return@launch

            totalPauseDuration += System.currentTimeMillis() - pauseStartTime

            chunkManager.resumeChunking()
            recordingRepository.updateSessionStatus(id, SessionStatus.RECORDING)

            val duration = System.currentTimeMillis() - recordingStartTime - totalPauseDuration
            _serviceState.value = ServiceState.Recording(id, duration)

            notificationHelper.updateNotification(isPaused = false)
        }
    }

    private fun stopRecording(sessionId: String?) {
        serviceScope.launch {
            val id = sessionId ?: currentSessionId ?: return@launch

            chunkManager.stopChunking()

            val endTime = System.currentTimeMillis()
            val totalDuration = endTime - recordingStartTime - totalPauseDuration

            recordingRepository.finalizeSession(
                sessionId = id,
                endTime = endTime,
                totalDuration = totalDuration,
                pauseDuration = totalPauseDuration
            )

            // Enqueue transcription work
            enqueueTranscriptionWork(id)

            _serviceState.value = ServiceState.Stopped(id)
            ServiceCompat.stopForeground(this@RecordingService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun recoverFromProcessDeath() {
        serviceScope.launch {
            // Find incomplete sessions
            val incompleteSessions = recordingRepository.getIncompleteSessions()
            incompleteSessions.forEach { session ->
                when (session.status) {
                    SessionStatus.RECORDING, SessionStatus.PAUSED -> {
                        // Finalize the session
                        recordingRepository.updateSessionStatus(session.id, SessionStatus.STOPPED)
                        enqueueTranscriptionWork(session.id)
                    }
                    else -> { /* Already handled */ }
                }
            }
        }
    }

    /**
     * Enqueue transcription work using WorkManager.
     */
    private fun enqueueTranscriptionWork(sessionId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val transcriptionWork = OneTimeWorkRequestBuilder<TranscriptionWorker>()
            .setInputData(workDataOf(TranscriptionWorker.KEY_SESSION_ID to sessionId))
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                "transcription_$sessionId",
                ExistingWorkPolicy.KEEP,
                transcriptionWork
            )

        android.util.Log.d("RecordingService", "Enqueued transcription work for session: $sessionId")
    }

    override fun onDestroy() {
        super.onDestroy()
        interruptionHandler.stopMonitoring()
        serviceScope.cancel()
        serviceBridge.unbindService()
    }

    companion object {
        // Actions
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_RECOVER = "ACTION_RECOVER"

        // Extras
        const val EXTRA_SESSION_ID = "EXTRA_SESSION_ID"
        const val EXTRA_PAUSE_REASON = "EXTRA_PAUSE_REASON"

        // Notification
        const val NOTIFICATION_ID = 1001
    }
}

/**
 * Service state sealed class.
 */
sealed class ServiceState {
    object Idle : ServiceState()
    data class Recording(val sessionId: String, val duration: Long) : ServiceState()
    data class Paused(val sessionId: String, val reason: String?) : ServiceState()
    data class Stopped(val sessionId: String) : ServiceState()
    data class Error(val message: String) : ServiceState()
}