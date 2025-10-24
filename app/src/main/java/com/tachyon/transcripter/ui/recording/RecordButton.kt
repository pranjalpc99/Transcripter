package com.tachyon.transcripter.ui.recording

// RecordButton.kt

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tachyon.transcripter.domain.model.RecordingState

/**
 * Main record button with state-based appearance.
 */
@Composable
fun RecordButton(
    recordingState: RecordingState,
    onStartRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isRecording = recordingState is RecordingState.Recording
    val isPaused = recordingState is RecordingState.Paused

    // Pulsing animation for recording state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer ring (pulsing when recording)
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .border(
                        width = 4.dp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
        }

        // Main button
        FloatingActionButton(
            onClick = {
                when {
                    isRecording -> onPauseRecording()
                    isPaused -> onResumeRecording()
                    else -> onStartRecording()
                }
            },
            modifier = Modifier.size(100.dp),
            containerColor = when {
                isRecording -> MaterialTheme.colorScheme.error
                isPaused -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.primary
            },
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp
            )
        ) {
            Icon(
                imageVector = when {
                    isRecording -> Icons.Default.Pause
                    isPaused -> Icons.Default.PlayArrow
                    else -> Icons.Default.Mic
                },
                contentDescription = when {
                    isRecording -> "Pause"
                    isPaused -> "Resume"
                    else -> "Start Recording"
                },
                modifier = Modifier.size(48.dp),
                tint = Color.White
            )
        }
    }
}