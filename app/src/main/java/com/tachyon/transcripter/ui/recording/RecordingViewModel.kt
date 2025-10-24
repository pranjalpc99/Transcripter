package com.tachyon.transcripter.ui.recording

// RecordingViewModel.kt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tachyon.transcripter.domain.model.RecordingState
import com.tachyon.transcripter.domain.model.getSessionId
import com.tachyon.transcripter.domain.usecases.GetRecordingStateUseCase
import com.tachyon.transcripter.domain.usecases.PauseRecordingUseCase
import com.tachyon.transcripter.domain.usecases.ResumeRecordingUseCase
import com.tachyon.transcripter.domain.usecases.StartRecordingUseCase
import com.tachyon.transcripter.domain.usecases.StopRecordingUseCase
import com.tachyon.transcripter.service.ServiceBridge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Recording screen.
 * Manages recording state and controls.
 */
@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val startRecordingUseCase: StartRecordingUseCase,
    private val stopRecordingUseCase: StopRecordingUseCase,
    private val pauseRecordingUseCase: PauseRecordingUseCase,
    private val resumeRecordingUseCase: ResumeRecordingUseCase,
    private val getRecordingStateUseCase: GetRecordingStateUseCase,
    private val serviceBridge: ServiceBridge
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordingUiState())
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        observeRecordingState()
    }

    /**
     * Observe recording state from service.
     */
    private fun observeRecordingState() {
        viewModelScope.launch {
            getRecordingStateUseCase().collect { recordingState ->
                _uiState.update { currentState ->
                    currentState.copy(
                        recordingState = recordingState,
                        sessionId = recordingState.getSessionId(),
                        duration = when (recordingState) {
                            is RecordingState.Recording -> recordingState.duration
                            is RecordingState.Paused -> recordingState.duration
                            is RecordingState.Stopped -> recordingState.totalDuration
                            else -> 0L
                        },
                        chunkCount = when (recordingState) {
                            is RecordingState.Recording -> recordingState.chunkCount
                            else -> 0
                        },
                        pauseReason = when (recordingState) {
                            is RecordingState.Paused -> recordingState.reason
                            else -> null
                        }
                    )
                }

                // Start/stop timer based on state
                when (recordingState) {
                    is RecordingState.Recording -> startTimer()
                    else -> stopTimer()
                }
            }
        }
    }

    /**
     * Start recording.
     */
    fun startRecording() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            startRecordingUseCase().fold(
                onSuccess = { sessionId ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            sessionId = sessionId
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to start recording"
                        )
                    }
                }
            )
        }
    }

    /**
     * Pause recording.
     */
    fun pauseRecording() {
        viewModelScope.launch {
            pauseRecordingUseCase(
                sessionId = _uiState.value.sessionId,
                reason = "user_action"
            ).fold(
                onSuccess = { /* State will update via flow */ },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "Failed to pause recording")
                    }
                }
            )
        }
    }

    /**
     * Resume recording.
     */
    fun resumeRecording() {
        viewModelScope.launch {
            resumeRecordingUseCase(_uiState.value.sessionId).fold(
                onSuccess = { /* State will update via flow */ },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "Failed to resume recording")
                    }
                }
            )
        }
    }

    /**
     * Stop recording.
     */
    fun stopRecording() {
        viewModelScope.launch {
            stopRecordingUseCase(_uiState.value.sessionId).fold(
                onSuccess = { /* State will update via flow */ },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "Failed to stop recording")
                    }
                }
            )
        }
    }

    /**
     * Start duration timer.
     */
    private fun startTimer() {
        if (timerJob?.isActive == true) return

        timerJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis() - _uiState.value.duration

            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTime
                _uiState.update { it.copy(duration = elapsed) }
                delay(1000) // Update every second
            }
        }
    }

    /**
     * Stop duration timer.
     */
    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    /**
     * Show stop confirmation dialog.
     */
    fun showStopConfirmation() {
        _uiState.update { it.copy(showStopConfirmation = true) }
    }

    /**
     * Hide stop confirmation dialog.
     */
    fun hideStopConfirmation() {
        _uiState.update { it.copy(showStopConfirmation = false) }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}