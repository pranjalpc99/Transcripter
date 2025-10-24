package com.tachyon.transcripter.service

// AudioDeviceMonitor.kt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors audio device changes (Bluetooth, wired headset).
 */
@Singleton
class AudioDeviceMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var listener: ((AudioDeviceEvent) -> Unit)? = null
    private var isMonitoring = false

    private val headsetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_HEADSET_PLUG -> {
                    val state = intent.getIntExtra("state", -1)
                    when (state) {
                        0 -> listener?.invoke(AudioDeviceEvent.Disconnected("wired_headset"))
                        1 -> listener?.invoke(AudioDeviceEvent.Connected("wired_headset"))
                    }
                }
                AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED -> {
                    val state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
                    when (state) {
                        AudioManager.SCO_AUDIO_STATE_CONNECTED -> {
                            listener?.invoke(AudioDeviceEvent.Connected("bluetooth_sco"))
                        }
                        AudioManager.SCO_AUDIO_STATE_DISCONNECTED -> {
                            listener?.invoke(AudioDeviceEvent.Disconnected("bluetooth_sco"))
                        }
                    }
                }
                AudioManager.ACTION_HEADSET_PLUG -> {
                    val state = intent.getIntExtra("state", -1)
                    when (state) {
                        0 -> listener?.invoke(AudioDeviceEvent.Disconnected("headset"))
                        1 -> listener?.invoke(AudioDeviceEvent.Connected("headset"))
                    }
                }
            }
        }
    }

    /**
     * Set listener for audio device changes.
     */
    fun setListener(listener: (AudioDeviceEvent) -> Unit) {
        this.listener = listener
    }

    /**
     * Start monitoring audio device changes.
     */
    fun startMonitoring() {
        if (isMonitoring) return

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
            addAction(AudioManager.ACTION_HEADSET_PLUG)
        }

        context.registerReceiver(headsetReceiver, filter)
        isMonitoring = true
    }

    /**
     * Stop monitoring audio device changes.
     */
    fun stopMonitoring() {
        if (!isMonitoring) return

        try {
            context.unregisterReceiver(headsetReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
        isMonitoring = false
    }

    /**
     * Get currently connected audio devices.
     */
    fun getConnectedDevices(): List<String> {
        val devices = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (device in audioDevices) {
                when (device.type) {
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> devices.add("wired_headset")
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> devices.add("bluetooth")
                    AudioDeviceInfo.TYPE_USB_HEADSET -> devices.add("usb_headset")
                }
            }
        } else {
            @Suppress("DEPRECATION")
            if (audioManager.isWiredHeadsetOn) {
                devices.add("wired_headset")
            }
            @Suppress("DEPRECATION")
            if (audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn) {
                devices.add("bluetooth")
            }
        }

        return devices
    }

    /**
     * Check if any external audio device is connected.
     */
    fun hasExternalDevice(): Boolean {
        return getConnectedDevices().isNotEmpty()
    }
}

/**
 * Sealed class for audio device events.
 */
sealed class AudioDeviceEvent {
    data class Connected(val deviceType: String) : AudioDeviceEvent()
    data class Disconnected(val deviceType: String) : AudioDeviceEvent()
}