package com.tachyon.transcripter.ui.recording

// StatusIndicator.kt

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tachyon.transcripter.domain.model.RecordingState

/**
 * Status indicator showing current recording state and messages.
 */
@Composable
fun StatusIndicator(
    recordingState: RecordingState,
    pauseReason: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = recordingState !is RecordingState.Idle,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Surface(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp)),
            color = when (recordingState) {
                is RecordingState.Recording -> MaterialTheme.colorScheme.errorContainer
                is RecordingState.Paused -> MaterialTheme.colorScheme.secondaryContainer
                is RecordingState.Stopped -> MaterialTheme.colorScheme.primaryContainer
                is RecordingState.Error -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Status icon
                Icon(
                    imageVector = when (recordingState) {
                        is RecordingState.Recording -> Icons.Default.FiberManualRecord
                        is RecordingState.Paused -> Icons.Default.Pause
                        is RecordingState.Stopped -> Icons.Default.CheckCircle
                        is RecordingState.Error -> Icons.Default.Error
                        else -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = when (recordingState) {
                        is RecordingState.Recording -> MaterialTheme.colorScheme.onErrorContainer
                        is RecordingState.Paused -> MaterialTheme.colorScheme.onSecondaryContainer
                        is RecordingState.Stopped -> MaterialTheme.colorScheme.onPrimaryContainer
                        is RecordingState.Error -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Status text
                Text(
                    text = getStatusText(recordingState, pauseReason),
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (recordingState) {
                        is RecordingState.Recording -> MaterialTheme.colorScheme.onErrorContainer
                        is RecordingState.Paused -> MaterialTheme.colorScheme.onSecondaryContainer
                        is RecordingState.Stopped -> MaterialTheme.colorScheme.onPrimaryContainer
                        is RecordingState.Error -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

/**
 * Get status text based on recording state.
 */
private fun getStatusText(recordingState: RecordingState, pauseReason: String?): String {
    return when (recordingState) {
        is RecordingState.Recording -> "Recording in progress"
        is RecordingState.Paused -> {
            pauseReason?.let { reason ->
                when (reason) {
                    "phone_call" -> "Paused: Phone call"
                    "audio_focus_loss" -> "Paused: Another app using audio"
                    "low_storage" -> "Paused: Low storage"
                    "user_action" -> "Paused"
                    "bluetooth_disconnected" -> "Paused: Bluetooth disconnected"
                    "headset_disconnected" -> "Paused: Headset disconnected"
                    "silence_detected" -> "Paused: Silence detected"
                    else -> "Paused: ${reason.replace("_", " ")}"
                }
            } ?: "Paused"
        }
        is RecordingState.Stopped -> "Processing recording..."
        is RecordingState.Error -> "Error: ${recordingState.message}"
        else -> ""
    }
}