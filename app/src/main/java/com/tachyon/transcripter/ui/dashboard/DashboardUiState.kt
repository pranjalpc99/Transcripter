package com.tachyon.transcripter.ui.dashboard

import com.tachyon.transcripter.data.local.entity.RecordingSession

// DashboardUiState.kt

/**
 * UI state for Dashboard screen.
 */
data class DashboardUiState(
    val sessions: List<RecordingSession> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    /**
     * Check if there are any sessions.
     */
    val hasSessions: Boolean
        get() = sessions.isNotEmpty()

    /**
     * Check if showing empty state.
     */
    val showEmptyState: Boolean
        get() = !isLoading && sessions.isEmpty() && error == null

    /**
     * Check if showing error state.
     */
    val showError: Boolean
        get() = error != null && !isLoading

    /**
     * Get count of active recordings (recording or paused).
     */
    val activeRecordingsCount: Int
        get() = sessions.count {
            it.status == com.tachyon.transcripter.data.local.entity.SessionStatus.RECORDING ||
                    it.status == com.tachyon.transcripter.data.local.entity.SessionStatus.PAUSED
        }

    /**
     * Get count of completed recordings.
     */
    val completedRecordingsCount: Int
        get() = sessions.count {
            it.status == com.tachyon.transcripter.data.local.entity.SessionStatus.COMPLETED
        }

    /**
     * Get total storage used by all sessions.
     */
    val totalStorageUsed: Long
        get() = sessions.sumOf { it.totalSizeBytes }
}
