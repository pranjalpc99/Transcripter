package com.tachyon.transcripter.ui.summary

import com.tachyon.transcripter.domain.model.SummaryData
import com.tachyon.transcripter.domain.model.SummaryStatus

// SummaryUiState.kt

/**
 * UI state for Summary screen.
 */
data class SummaryUiState(
    val summaryData: SummaryData? = null,
    val streamingContent: String? = null,
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val error: String? = null
) {
    /**
     * Check if summary is complete.
     */
    val isComplete: Boolean
        get() = summaryData?.status == SummaryStatus.COMPLETED

    /**
     * Check if summary is generating.
     */
    val isCurrentlyGenerating: Boolean
        get() = summaryData?.status == SummaryStatus.GENERATING || isGenerating

    /**
     * Check if summary has failed.
     */
    val hasFailed: Boolean
        get() = summaryData?.status == SummaryStatus.FAILED

    /**
     * Check if summary is pending.
     */
    val isPending: Boolean
        get() = summaryData?.status == SummaryStatus.PENDING

    /**
     * Check if there's content to display.
     */
    val hasContent: Boolean
        get() = summaryData?.hasContent == true || streamingContent != null

    /**
     * Get display status text.
     */
    val statusText: String
        get() = when {
            isCurrentlyGenerating -> "Generating summary..."
            hasFailed -> "Failed to generate summary"
            isComplete -> "Summary complete"
            isPending -> "Summary pending"
            else -> ""
        }
}
