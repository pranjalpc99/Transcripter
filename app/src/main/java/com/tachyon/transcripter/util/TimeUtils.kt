package com.tachyon.transcripter.util

// TimeUtils.kt

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Utility object for time and date formatting.
 */
object TimeUtils {

    /**
     * Format duration in milliseconds to HH:MM:SS.
     *
     * @param millis Duration in milliseconds
     * @return Formatted string (e.g., "01:23:45")
     */
    fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Format duration in milliseconds to compact format (e.g., "1h 23m").
     *
     * @param millis Duration in milliseconds
     * @return Formatted string
     */
    fun formatDurationCompact(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return buildString {
            if (hours > 0) {
                append("${hours}h ")
            }
            if (minutes > 0 || hours > 0) {
                append("${minutes}m ")
            }
            append("${seconds}s")
        }.trim()
    }

    /**
     * Format timestamp to date string.
     *
     * @param timestamp Timestamp in milliseconds
     * @param pattern Date pattern (default: "MMM dd, yyyy")
     * @return Formatted date string
     */
    fun formatDate(timestamp: Long, pattern: String = "MMM dd, yyyy"): String {
        val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    /**
     * Format timestamp to time string.
     *
     * @param timestamp Timestamp in milliseconds
     * @param pattern Time pattern (default: "hh:mm a")
     * @return Formatted time string
     */
    fun formatTime(timestamp: Long, pattern: String = "hh:mm a"): String {
        val timeFormat = SimpleDateFormat(pattern, Locale.getDefault())
        return timeFormat.format(Date(timestamp))
    }

    /**
     * Format timestamp to date and time string.
     *
     * @param timestamp Timestamp in milliseconds
     * @return Formatted string (e.g., "Jan 15, 2025 at 10:30 AM")
     */
    fun formatDateTime(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    /**
     * Get relative time string (e.g., "5 minutes ago", "Yesterday").
     *
     * @param timestamp Timestamp in milliseconds
     * @return Relative time string
     */
    fun getRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                "$minutes minute${if (minutes != 1L) "s" else ""} ago"
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                "$hours hour${if (hours != 1L) "s" else ""} ago"
            }
            diff < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                when (days) {
                    1L -> "Yesterday"
                    else -> "$days days ago"
                }
            }
            else -> formatDate(timestamp)
        }
    }

    /**
     * Check if timestamp is today.
     *
     * @param timestamp Timestamp in milliseconds
     * @return True if today
     */
    fun isToday(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)

        calendar.timeInMillis = timestamp
        val thatDay = calendar.get(Calendar.DAY_OF_YEAR)

        return today == thatDay
    }

    /**
     * Check if timestamp is yesterday.
     *
     * @param timestamp Timestamp in milliseconds
     * @return True if yesterday
     */
    fun isYesterday(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = calendar.get(Calendar.DAY_OF_YEAR)

        calendar.timeInMillis = timestamp
        val thatDay = calendar.get(Calendar.DAY_OF_YEAR)

        return yesterday == thatDay
    }

    /**
     * Get start of day timestamp.
     *
     * @param timestamp Reference timestamp
     * @return Start of day timestamp
     */
    fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Get end of day timestamp.
     *
     * @param timestamp Reference timestamp
     * @return End of day timestamp
     */
    fun getEndOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
}