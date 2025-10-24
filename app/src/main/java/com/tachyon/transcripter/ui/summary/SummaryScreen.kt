package com.tachyon.transcripter.ui.summary

// SummaryScreen.kt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tachyon.transcripter.domain.model.SummaryStatus

/**
 * Summary screen displaying the generated summary.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    sessionId: String,
    viewModel: SummaryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Load summary for session
    LaunchedEffect(sessionId) {
        viewModel.loadSummary(sessionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Summary") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Share button
                    if (uiState.summaryData?.status == SummaryStatus.COMPLETED) {
                        IconButton(onClick = { viewModel.exportSummary() }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
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
                        onRetry = { viewModel.retrySummary() },
                        onDismiss = { viewModel.clearError() }
                    )
                }
                uiState.summaryData == null -> {
                    EmptyContent(
                        onGenerate = { viewModel.generateSummary(sessionId) }
                    )
                }
                else -> {
                    SummaryContent(
                        uiState = uiState,
                        onRetry = { viewModel.retrySummary() }
                    )
                }
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
            Text("Loading summary...")
        }
    }
}

/**
 * Error state.
 */
@Composable
fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = error,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        RetryButton(onClick = onRetry)

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onDismiss) {
            Text("Dismiss")
        }
    }
}

/**
 * Empty state (no summary generated yet).
 */
@Composable
fun EmptyContent(
    onGenerate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No Summary Yet",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Generate an AI-powered summary of your recording",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onGenerate,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generate Summary")
        }
    }
}

/**
 * Main summary content.
 */
@Composable
fun SummaryContent(
    uiState: SummaryUiState,
    onRetry: () -> Unit
) {
    val scrollState = rememberScrollState()
    val summaryData = uiState.summaryData!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Title section
        if (summaryData.title != null) {
            Text(
                text = summaryData.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Status indicator for generating
        if (summaryData.status == SummaryStatus.GENERATING) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Generating summary...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Summary section
        if (summaryData.summary != null || uiState.streamingContent != null) {
            SummarySection(
                title = "Summary",
                icon = Icons.Default.Description
            ) {
                if (summaryData.status == SummaryStatus.GENERATING && uiState.streamingContent != null) {
                    StreamingText(text = uiState.streamingContent)
                } else {
                    Text(
                        text = summaryData.summary ?: "",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Action Items section
        if (summaryData.actionItems.isNotEmpty()) {
            SummarySection(
                title = "Action Items",
                icon = Icons.Default.CheckCircle
            ) {
                summaryData.actionItems.forEach { item ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Circle,
                            contentDescription = null,
                            modifier = Modifier
                                .size(8.dp)
                                .padding(top = 8.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Key Points section
        if (summaryData.keyPoints.isNotEmpty()) {
            SummarySection(
                title = "Key Points",
                icon = Icons.Default.Star
            ) {
                summaryData.keyPoints.forEach { point ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Circle,
                            contentDescription = null,
                            modifier = Modifier
                                .size(8.dp)
                                .padding(top = 8.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = point,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Failed state - show retry button
        if (summaryData.status == SummaryStatus.FAILED) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = summaryData.errorMessage ?: "Failed to generate summary",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    RetryButton(onClick = onRetry)
                }
            }
        }

        // Bottom padding
        Spacer(modifier = Modifier.height(32.dp))
    }
}