package com.tachyon.transcripter.util

// Extensions.kt

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.File

/**
 * Extension functions for common operations.
 */

// ========== Context Extensions ==========

/**
 * Show a short toast message.
 */
fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

/**
 * Show a long toast message.
 */
fun Context.showLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

// ========== String Extensions ==========

/**
 * Capitalize first letter of string.
 */
fun String.capitalizeFirst(): String {
    return replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

/**
 * Truncate string to max length with ellipsis.
 */
fun String.truncate(maxLength: Int, ellipsis: String = "..."): String {
    return if (length <= maxLength) {
        this
    } else {
        take(maxLength - ellipsis.length) + ellipsis
    }
}

/**
 * Check if string is a valid email.
 */
fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

// ========== Long Extensions ==========

/**
 * Convert bytes to megabytes.
 */
fun Long.toMB(): Double {
    return this / (1024.0 * 1024.0)
}

/**
 * Convert bytes to gigabytes.
 */
fun Long.toGB(): Double {
    return this / (1024.0 * 1024.0 * 1024.0)
}

/**
 * Format milliseconds to duration string.
 */
fun Long.toDurationString(): String {
    return TimeUtils.formatDuration(this)
}

/**
 * Format timestamp to date string.
 */
fun Long.toDateString(): String {
    return TimeUtils.formatDate(this)
}

/**
 * Format timestamp to relative time string.
 */
fun Long.toRelativeTime(): String {
    return TimeUtils.getRelativeTime(this)
}

// ========== File Extensions ==========

/**
 * Get file size in human-readable format.
 */
fun File.formatSize(): String {
    return FileUtils.formatFileSize(length())
}

/**
 * Get file extension.
 */
fun File.getExtension(): String {
    return FileUtils.getFileExtension(name)
}

/**
 * Check if file is an audio file.
 */
fun File.isAudioFile(): Boolean {
    val audioExtensions = listOf("mp3", "m4a", "wav", "aac", "ogg", "flac")
    return audioExtensions.contains(getExtension().lowercase())
}

/**
 * Create parent directories if they don't exist.
 */
fun File.ensureParentExists(): Boolean {
    return parentFile?.let { FileUtils.ensureDirectoryExists(it) } ?: false
}

// ========== Flow Extensions ==========

/**
 * Map Flow to Result.
 */
fun <T> Flow<T>.asResult(): Flow<Result<T>> {
    return this
        .map<T, Result<T>> { Result.success(it) }
        .catch { emit(Result.failure(it)) }
}

/**
 * Catch and log errors in Flow.
 */
fun <T> Flow<T>.catchAndLog(tag: String = "Flow"): Flow<T> {
    return catch { throwable ->
        android.util.Log.e(tag, "Flow error", throwable)
        throw throwable
    }
}

// ========== Collection Extensions ==========

/**
 * Safe get with default value.
 */
fun <T> List<T>.getOrDefault(index: Int, default: T): T {
    return getOrNull(index) ?: default
}

/**
 * Check if list is not null or empty.
 */
fun <T> List<T>?.isNotNullOrEmpty(): Boolean {
    return this != null && isNotEmpty()
}

// ========== Boolean Extensions ==========

/**
 * Execute action if true.
 */
inline fun Boolean.ifTrue(action: () -> Unit): Boolean {
    if (this) action()
    return this
}

/**
 * Execute action if false.
 */
inline fun Boolean.ifFalse(action: () -> Unit): Boolean {
    if (!this) action()
    return this
}

// ========== Any Extensions ==========

/**
 * Execute action with receiver.
 */
inline fun <T> T.also(action: T.() -> Unit): T {
    action()
    return this
}

/**
 * Safe cast with default value.
 */
inline fun <reified T> Any?.safeCast(default: T): T {
    return this as? T ?: default
}