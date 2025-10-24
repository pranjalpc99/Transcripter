package com.tachyon.transcripter.ui.recording

// RecordingScreen.kt

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tachyon.transcripter.domain.model.RecordingState

/**
 * Recording screen for active recording session.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    sessionId: String?,
    viewModel: RecordingViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToSummary: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Start recording if new session
    LaunchedEffect(sessionId) {
        if (sessionId == "new") {
            viewModel.startRecording()
        }
    }

    // Navigate to summary when recording completes
    LaunchedEffect(uiState.recordingState) {
        if (uiState.recordingState is RecordingState.Stopped && uiState.sessionId != null) {
            onNavigateToSummary(uiState.sessionId!!)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recording") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.recordingState is RecordingState.Recording ||
                            uiState.recordingState is RecordingState.Paused) {
                            viewModel.showStopConfirmation()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingContent()
                }
                uiState.error != null -> {
                    ErrorContent(
                        error = uiState.error!!,
                        onDismiss = { viewModel.clearError() },
                        onRetry = { viewModel.startRecording() }
                    )
                }
                else -> {
                    RecordingContent(
                        uiState = uiState,
                        onStartRecording = { viewModel.startRecording() },
                        onPauseRecording = { viewModel.pauseRecording() },
                        onResumeRecording = { viewModel.resumeRecording() },
                        onStopRecording = { viewModel.showStopConfirmation() }
                    )
                }
            }

            // Stop confirmation dialog
            if (uiState.showStopConfirmation) {
                StopConfirmationDialog(
                    onConfirm = {
                        viewModel.stopRecording()
                        viewModel.hideStopConfirmation()
                    },
                    onDismiss = { viewModel.hideStopConfirmation() }
                )
            }
        }
    }
}

/**
 * Loading state.
 */
@Composable
fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Preparing recording...")
        }
    }
}

/**
 * Error state.
 */
@Composable
fun ErrorContent(
    error: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recording Error") },
        text = { Text(error) },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text("Retry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Main recording content.
 */
@Composable
fun RecordingContent(
    uiState: RecordingUiState,
    onStartRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top section - Status and info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Status indicator
            StatusIndicator(
                recordingState = uiState.recordingState,
                pauseReason = uiState.pauseReason
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Timer
            RecordingTimer(
                duration = uiState.duration,
                isRecording = uiState.recordingState is RecordingState.Recording,
                isPaused = uiState.recordingState is RecordingState.Paused
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Chunk count
            if (uiState.chunkCount > 0) {
                Text(
                    text = "${uiState.chunkCount} chunk${if (uiState.chunkCount != 1) "s" else ""} recorded",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Middle section - Record button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RecordButton(
                recordingState = uiState.recordingState,
                onStartRecording = onStartRecording,
                onPauseRecording = onPauseRecording,
                onResumeRecording = onResumeRecording,
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(48.dp))
        }

        // Bottom section - Stop button
        if (uiState.recordingState is RecordingState.Recording ||
            uiState.recordingState is RecordingState.Paused) {
            Button(
                onClick = onStopRecording,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stop Recording")
            }
        }
    }
}

/**
 * Stop confirmation dialog.
 */
@Composable
fun StopConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stop Recording?") },
        text = { Text("Your recording will be saved and transcription will begin.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Stop")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Continue")
            }
        }
    )
}