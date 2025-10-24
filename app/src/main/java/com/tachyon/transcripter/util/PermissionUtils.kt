package com.tachyon.transcripter.util

// PermissionUtils.kt

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Utility object for permission checking and handling.
 */
object PermissionUtils {

    /**
     * Check if audio recording permission is granted.
     */
    fun hasRecordAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if notification permission is granted (Android 13+).
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required on older versions
        }
    }

    /**
     * Check if phone state permission is granted.
     */
    fun hasPhoneStatePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if all required permissions are granted.
     */
    fun hasAllRequiredPermissions(context: Context): Boolean {
        return hasRecordAudioPermission(context) && hasNotificationPermission(context)
    }

    /**
     * Get list of required permissions.
     */
    fun getRequiredPermissions(): Array<String> {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return permissions.toTypedArray()
    }

    /**
     * Get list of optional permissions.
     */
    fun getOptionalPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.READ_PHONE_STATE
        )
    }

    /**
     * Check if permission is permanently denied.
     * (User selected "Don't ask again")
     */
    fun isPermissionPermanentlyDenied(
        context: Context,
        permission: String
    ): Boolean {
        return if (context is android.app.Activity) {
            !context.shouldShowRequestPermissionRationale(permission) &&
                    ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
    }

    /**
     * Get user-friendly permission name.
     */
    fun getPermissionName(permission: String): String {
        return when (permission) {
            Manifest.permission.RECORD_AUDIO -> "Microphone"
            Manifest.permission.POST_NOTIFICATIONS -> "Notifications"
            Manifest.permission.READ_PHONE_STATE -> "Phone State"
            else -> permission.substringAfterLast('.')
        }
    }

    /**
     * Get permission rationale text.
     */
    fun getPermissionRationale(permission: String): String {
        return when (permission) {
            Manifest.permission.RECORD_AUDIO ->
                "Microphone access is required to record audio."
            Manifest.permission.POST_NOTIFICATIONS ->
                "Notification permission is needed to show recording status and controls."
            Manifest.permission.READ_PHONE_STATE ->
                "Phone state access allows the app to pause recording during calls."
            else -> "This permission is required for the app to function properly."
        }
    }
}