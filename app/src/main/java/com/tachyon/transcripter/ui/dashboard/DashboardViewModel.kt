package com.tachyon.transcripter.ui.dashboard

// DashboardViewModel.kt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tachyon.transcripter.data.repository.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Dashboard screen.
 * Manages list of recording sessions.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadSessions()
    }

    /**
     * Load all recording sessions.
     */
    private fun loadSessions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                recordingRepository.observeAllSessions()
                    .catch { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Failed to load sessions"
                            )
                        }
                    }
                    .collect { sessions ->
                        _uiState.update {
                            it.copy(
                                sessions = sessions.sortedByDescending { session -> session.startTime },
                                isLoading = false,
                                error = null
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load sessions"
                    )
                }
            }
        }
    }

    /**
     * Refresh sessions list.
     */
    fun refreshSessions() {
        loadSessions()
    }

    /**
     * Delete a recording session.
     */
    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            recordingRepository.deleteSession(sessionId).fold(
                onSuccess = {
                    // Session deleted, list will auto-update via Flow
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "Failed to delete session")
                    }
                }
            )
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}