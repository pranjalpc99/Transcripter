package com.tachyon.transcripter.ui.dashboard

// DashboardScreen.kt

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.tachyon.transcripter.data.local.entity.RecordingSession
import com.tachyon.transcripter.data.local.entity.SessionStatus
import com.tachyon.transcripter.data.local.entity.toDisplayText
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dashboard screen showing list of all recording sessions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToRecording: (String) -> Unit,
    onNavigateToSummary: (String) -> Unit,
    onStartNewRecording: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Recordings") },
                actions = {
                    IconButton(onClick = { viewModel.refreshSessions() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .zIndex(2f)                 // ensure it's on top
                    .size(96.dp)                // a bit bigger than the FAB
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                // Eat all changes so nothing below receives them
                                event.changes.forEach { it.consume() }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                FloatingActionButton(
                    onClick = onStartNewRecording,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Start Recording")
                }
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                LoadingContent(modifier = Modifier.padding(paddingValues))
            }
            uiState.error != null -> {
                ErrorContent(
                    error = uiState.error!!,
                    onRetry = { viewModel.refreshSessions() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            uiState.sessions.isEmpty() -> {
                EmptyContent(
                    onStartRecording = onStartNewRecording,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            else -> {
                SessionList(
                    sessions = uiState.sessions,
                    onSessionClick = { session ->
                        when (session.status) {
                            SessionStatus.RECORDING,
                            SessionStatus.PAUSED -> onNavigateToRecording(session.id)
                            SessionStatus.COMPLETED -> onNavigateToSummary(session.id)
                            else -> onNavigateToSummary(session.id)
                        }
                    },
                    onDeleteSession = { session ->
                        viewModel.deleteSession(session.id)
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

/**
 * Loading indicator.
 */
@Composable
fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Error display with retry.
 */
@Composable
fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = error,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

/**
 * Empty state display.
 */
@Composable
fun EmptyContent(
    onStartRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MicNone,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No Recordings Yet",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tap the microphone button to start your first recording",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onStartRecording,
            modifier = Modifier.size(width = 200.dp, height = 56.dp)
        ) {
            Icon(Icons.Default.Mic, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start Recording")
        }
    }
}

/**
 * List of recording sessions.
 */
@Composable
fun SessionList(
    sessions: List<RecordingSession>,
    onSessionClick: (RecordingSession) -> Unit,
    onDeleteSession: (RecordingSession) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(sessions, key = { it.id }) { session ->
            SessionItem(
                session = session,
                onClick = { onSessionClick(session) },
                onDelete = { onDeleteSession(session) }
            )
        }
    }
}

/**
 * Individual session item card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionItem(
    session: RecordingSession,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            Icon(
                imageVector = when (session.status) {
                    SessionStatus.RECORDING -> Icons.Default.FiberManualRecord
                    SessionStatus.PAUSED -> Icons.Default.Pause
                    SessionStatus.COMPLETED -> Icons.Default.CheckCircle
                    SessionStatus.TRANSCRIBING -> Icons.Default.Sync
                    SessionStatus.GENERATING_SUMMARY -> Icons.Default.AutoAwesome
                    SessionStatus.FAILED -> Icons.Default.Error
                    else -> Icons.Default.AudioFile
                },
                contentDescription = null,
                tint = when (session.status) {
                    SessionStatus.RECORDING -> MaterialTheme.colorScheme.error
                    SessionStatus.PAUSED -> MaterialTheme.colorScheme.secondary
                    SessionStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                    SessionStatus.FAILED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Session info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatSessionTitle(session),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatSessionDate(session.startTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatDuration(session.totalDurationMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    StatusChip(status = session.status)
                }
            }

            // Delete button
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Recording?") },
            text = { Text("This action cannot be undone. All audio files and transcripts will be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Status chip showing session status.
 */
@Composable
fun StatusChip(status: SessionStatus) {
    Surface(
        color = when (status) {
            SessionStatus.RECORDING -> MaterialTheme.colorScheme.errorContainer
            SessionStatus.PAUSED -> MaterialTheme.colorScheme.secondaryContainer
            SessionStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
            SessionStatus.TRANSCRIBING,
            SessionStatus.GENERATING_SUMMARY -> MaterialTheme.colorScheme.tertiaryContainer
            SessionStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(0.dp)
    ) {
        Text(
            text = status.toDisplayText(),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = when (status) {
                SessionStatus.RECORDING -> MaterialTheme.colorScheme.onErrorContainer
                SessionStatus.PAUSED -> MaterialTheme.colorScheme.onSecondaryContainer
                SessionStatus.COMPLETED -> MaterialTheme.colorScheme.onPrimaryContainer
                SessionStatus.TRANSCRIBING,
                SessionStatus.GENERATING_SUMMARY -> MaterialTheme.colorScheme.onTertiaryContainer
                SessionStatus.FAILED -> MaterialTheme.colorScheme.onErrorContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

/**
 * Format session title (use date/time for now).
 */
fun formatSessionTitle(session: RecordingSession): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return "Recording - ${dateFormat.format(Date(session.startTime))}"
}

/**
 * Format session date/time.
 */
fun formatSessionDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

/**
 * Format duration in milliseconds to readable string.
 */
fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        minutes > 0 -> String.format("%d:%02d", minutes, seconds)
        else -> String.format("0:%02d", seconds)
    }
}