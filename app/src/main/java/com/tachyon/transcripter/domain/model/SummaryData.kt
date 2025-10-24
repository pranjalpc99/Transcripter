package com.tachyon.transcripter.domain.model

// SummaryData.kt

/**
 * Domain model for summary data.
 * Represents the structured summary of a recording session.
 */
data class SummaryData(
    val sessionId: String,
    val title: String?,
    val summary: String?,
    val actionItems: List<String> = emptyList(),
    val keyPoints: List<String> = emptyList(),
    val status: SummaryStatus = SummaryStatus.PENDING,
    val partialContent: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Returns true if summary generation is complete.
     */
    val isComplete: Boolean
        get() = status == SummaryStatus.COMPLETED && title != null && summary != null

    /**
     * Returns true if summary generation is in progress.
     */
    val isGenerating: Boolean
        get() = status == SummaryStatus.GENERATING

    /**
     * Returns true if summary generation has failed.
     */
    val hasFailed: Boolean
        get() = status == SummaryStatus.FAILED

    /**
     * Returns true if summary is pending generation.
     */
    val isPending: Boolean
        get() = status == SummaryStatus.PENDING

    /**
     * Returns true if there's content to display (partial or complete).
     */
    val hasContent: Boolean
        get() = !title.isNullOrBlank() || !summary.isNullOrBlank() ||
                actionItems.isNotEmpty() || keyPoints.isNotEmpty()

    /**
     * Returns a formatted summary as plain text.
     */
    fun toFormattedText(): String = buildString {
        title?.let {
            appendLine("# $it")
            appendLine()
        }

        summary?.let {
            appendLine("## Summary")
            appendLine(it)
            appendLine()
        }

        if (actionItems.isNotEmpty()) {
            appendLine("## Action Items")
            actionItems.forEach { item ->
                appendLine("- $item")
            }
            appendLine()
        }

        if (keyPoints.isNotEmpty()) {
            appendLine("## Key Points")
            keyPoints.forEach { point ->
                appendLine("- $point")
            }
        }
    }

    /**
     * Returns the display status text.
     */
    fun getStatusText(): String = when (status) {
        SummaryStatus.PENDING -> "Pending"
        SummaryStatus.GENERATING -> "Generating..."
        SummaryStatus.COMPLETED -> "Complete"
        SummaryStatus.FAILED -> "Failed"
    }
}

/**
 * Status of summary generation.
 */
enum class SummaryStatus {
    PENDING,        // Not started
    GENERATING,     // Currently generating
    COMPLETED,      // Generation complete
    FAILED          // Generation failed
}
