package com.tachyon.transcripter.ui.summary

// SummaryViewModel.kt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tachyon.transcripter.data.repository.SummaryStreamState
import com.tachyon.transcripter.domain.usecases.GenerateSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Summary screen.
 * Manages summary generation and display.
 */
@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val generateSummaryUseCase: GenerateSummaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SummaryUiState())
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()

    private var currentSessionId: String? = null

    /**
     * Load existing summary for a session.
     */
    fun loadSummary(sessionId: String) {
        currentSessionId = sessionId

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // First, try to get existing summary
            val existingSummary = generateSummaryUseCase.getSummary(sessionId)

            if (existingSummary != null) {
                _uiState.update {
                    it.copy(
                        summaryData = existingSummary,
                        isLoading = false
                    )
                }

                // If summary is pending or failed, might want to auto-generate
                if (existingSummary.isPending) {
                    // Optionally auto-generate
                    // generateSummary(sessionId)
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }

            // Observe for updates
            generateSummaryUseCase.observeSummary(sessionId).collect { summary ->
                if (summary != null) {
                    _uiState.update { it.copy(summaryData = summary) }
                }
            }
        }
    }

    /**
     * Generate summary with streaming.
     */
    fun generateSummary(sessionId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    error = null,
                    streamingContent = null
                )
            }

            generateSummaryUseCase(sessionId).collect { state ->
                when (state) {
                    is SummaryStreamState.Loading -> {
                        _uiState.update {
                            it.copy(
                                isGenerating = true,
                                streamingContent = null
                            )
                        }
                    }
                    is SummaryStreamState.Streaming -> {
                        _uiState.update {
                            it.copy(
                                isGenerating = true,
                                streamingContent = state.partialContent
                            )
                        }
                    }
                    is SummaryStreamState.Complete -> {
                        _uiState.update {
                            it.copy(
                                summaryData = com.tachyon.transcripter.domain.model.SummaryData(
                                    sessionId = state.summary.sessionId,
                                    title = state.summary.title,
                                    summary = state.summary.summary,
                                    actionItems = state.summary.actionItems?.split("\n")?.filter { it.isNotBlank() } ?: emptyList(),
                                    keyPoints = state.summary.keyPoints?.split("\n")?.filter { it.isNotBlank() } ?: emptyList(),
                                    status = com.tachyon.transcripter.domain.model.SummaryStatus.COMPLETED,
                                    createdAt = state.summary.createdAt,
                                    updatedAt = state.summary.updatedAt
                                ),
                                isGenerating = false,
                                streamingContent = null
                            )
                        }
                    }
                    is SummaryStreamState.Error -> {
                        _uiState.update {
                            it.copy(
                                isGenerating = false,
                                error = state.message,
                                streamingContent = null
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Retry summary generation.
     */
    fun retrySummary() {
        currentSessionId?.let { sessionId ->
            generateSummary(sessionId)
        }
    }

    /**
     * Export summary (share).
     */
    fun exportSummary() {
        viewModelScope.launch {
            // TODO: Implement export/share functionality
            // This would typically use Android's share intent
            currentSessionId?.let { sessionId ->
                // Generate markdown or text for sharing
                val summary = generateSummaryUseCase.getSummary(sessionId)
                summary?.let {
                    // Create share intent with formatted text
                    val formattedText = it.toFormattedText()
                    // Share using Android intent
                }
            }
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}