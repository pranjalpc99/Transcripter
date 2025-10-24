package com.tachyon.transcripter.service

import android.Manifest
import android.media.AudioManager
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import com.tachyon.transcripter.data.repository.FileRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

// service/InterruptionHandler.kt
class InterruptionHandler @Inject constructor(
    val audioFocusManager: AudioFocusManager,
    val phoneStateHandler: PhoneStateHandler,
    val audioDeviceMonitor: AudioDeviceMonitor,
    private val storageMonitor: StorageMonitor,
    private val silenceDetector: SilenceDetector
) {
    private val _interruptions = MutableSharedFlow<Interruption>()
    val interruptions: SharedFlow<Interruption> = _interruptions.asSharedFlow()
    private var isMonitoring = false

    fun initialize(service: RecordingService) {
        // Audio focus
        audioFocusManager.setListener { focusChange ->
            handleAudioFocusChange(focusChange)
        }

        // Phone calls
        phoneStateHandler.setListener { state ->
            handlePhoneStateChange(state)
        }

        // Audio devices
        audioDeviceMonitor.setListener { event ->
            handleAudioDeviceChange(event)
        }

        // Storage
        storageMonitor.setListener { availableBytes ->
            if (availableBytes < FileRepository.MIN_STORAGE_MB * 1024 * 1024) {
                handleLowStorage()
            }
        }

        // Silence
        silenceDetector.setListener { silenceDuration ->
            if (silenceDuration >= 10_000) {  // 10 seconds
                handleSilence()
            }
        }
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                _interruptions.tryEmit(Interruption.AudioFocusLost)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                _interruptions.tryEmit(Interruption.AudioFocusGained)
            }
        }
    }

    private fun handlePhoneStateChange(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING,
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                _interruptions.tryEmit(Interruption.PhoneCallStarted)
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                _interruptions.tryEmit(Interruption.PhoneCallEnded)
            }
        }
    }

    private fun handleAudioDeviceChange(event: AudioDeviceEvent) {
        when (event) {
            is AudioDeviceEvent.Connected -> {
                _interruptions.tryEmit(Interruption.AudioDeviceChanged(event.deviceType))
            }
            is AudioDeviceEvent.Disconnected -> {
                _interruptions.tryEmit(Interruption.AudioDeviceChanged("default"))
            }
        }
    }

    private fun handleLowStorage() {
        _interruptions.tryEmit(Interruption.LowStorage)
    }

    private fun handleSilence() {
        _interruptions.tryEmit(Interruption.SilenceDetected)
    }

    /**
     * Start monitoring all interruption sources.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startMonitoring() {
        if (isMonitoring) return

        // Request audio focus
        audioFocusManager.requestAudioFocus()

        // Start phone state monitoring
        phoneStateHandler.startMonitoring()

        // Start audio device monitoring
        audioDeviceMonitor.startMonitoring()

        // Start storage monitoring
        storageMonitor.startMonitoring()

        // Start silence detection
        silenceDetector.startDetection()

        isMonitoring = true
    }

    /**
     * Stop monitoring all interruption sources.
     */
    fun stopMonitoring() {
        if (!isMonitoring) return

        // Abandon audio focus
        audioFocusManager.abandonAudioFocus()

        // Stop phone state monitoring
        phoneStateHandler.stopMonitoring()

        // Stop audio device monitoring
        audioDeviceMonitor.stopMonitoring()

        // Stop storage monitoring
        storageMonitor.stopMonitoring()

        // Stop silence detection
        silenceDetector.stopDetection()

        isMonitoring = false
    }

    /**
     * Check if currently monitoring.
     */
    fun isCurrentlyMonitoring(): Boolean = isMonitoring

    /**
     * Cleanup resources.
     */
    fun cleanup() {
        stopMonitoring()
        storageMonitor.cleanup()
        silenceDetector.cleanup()
    }
}

sealed class Interruption {
    object AudioFocusLost : Interruption()
    object AudioFocusGained : Interruption()
    object PhoneCallStarted : Interruption()
    object PhoneCallEnded : Interruption()
    data class AudioDeviceChanged(val deviceType: String) : Interruption()
    object LowStorage : Interruption()
    object SilenceDetected : Interruption()
}