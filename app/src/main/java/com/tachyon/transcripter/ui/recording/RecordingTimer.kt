package com.tachyon.transcripter.ui.recording

// RecordingTimer.kt

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Timer display for recording duration.
 */
@Composable
fun RecordingTimer(
    duration: Long,
    isRecording: Boolean,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor by animateColorAsState(
        targetValue = when {
            isRecording -> MaterialTheme.colorScheme.error
            isPaused -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "timer_color"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = formatDuration(duration),
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = when {
                isRecording -> "Recording..."
                isPaused -> "Paused"
                else -> "00:00:00"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = textColor.copy(alpha = 0.7f)
        )
    }
}

/**
 * Format duration in milliseconds to HH:MM:SS.
 */
private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}