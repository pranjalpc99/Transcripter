package com.tachyon.transcripter.service

// PhoneStateHandler.kt

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.os.Build
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors phone call state to pause/resume recording.
 */
@Singleton
class PhoneStateHandler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private var listener: ((Int) -> Unit)? = null
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyCallback: TelephonyCallback? = null

    /**
     * Set listener for phone state changes.
     * @param listener Callback with TelephonyManager.CALL_STATE_* constants
     */
    fun setListener(listener: (Int) -> Unit) {
        this.listener = listener
    }

    /**
     * Start monitoring phone state.
     */
    fun startMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startMonitoringModern()
        } else {
            startMonitoringLegacy()
        }
    }

    /**
     * Stop monitoring phone state.
     */
    fun stopMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            stopMonitoringModern()
        } else {
            stopMonitoringLegacy()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun startMonitoringModern() {
        telephonyCallback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                listener?.invoke(state)
            }
        }

        try {
            telephonyManager.registerTelephonyCallback(
                context.mainExecutor,
                telephonyCallback!!
            )
        } catch (e: SecurityException) {
            // Handle missing READ_PHONE_STATE permission
        }
    }

    @Suppress("DEPRECATION")
    private fun startMonitoringLegacy() {
        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                listener?.invoke(state)
            }
        }

        try {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        } catch (e: SecurityException) {
            // Handle missing READ_PHONE_STATE permission
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun stopMonitoringModern() {
        telephonyCallback?.let {
            telephonyManager.unregisterTelephonyCallback(it)
        }
        telephonyCallback = null
    }

    @Suppress("DEPRECATION")
    private fun stopMonitoringLegacy() {
        phoneStateListener?.let {
            telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
        }
        phoneStateListener = null
    }

    /**
     * Get current call state.
     */
    fun getCurrentCallState(): Int {
        return try {
            telephonyManager.callState
        } catch (e: SecurityException) {
            TelephonyManager.CALL_STATE_IDLE
        }
    }

    /**
     * Check if currently in a call.
     */
    fun isInCall(): Boolean {
        val state = getCurrentCallState()
        return state == TelephonyManager.CALL_STATE_RINGING ||
                state == TelephonyManager.CALL_STATE_OFFHOOK
    }
}