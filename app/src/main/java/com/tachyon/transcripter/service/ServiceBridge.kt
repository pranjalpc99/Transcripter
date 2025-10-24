package com.tachyon.transcripter.service

import com.tachyon.transcripter.domain.model.RecordingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// service/ServiceBridge.kt
@Singleton
class ServiceBridge @Inject constructor() {

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private var boundService: RecordingService? = null

    fun bindService(service: RecordingService) {
        boundService = service
        // Observe service state and map to RecordingState
        service.serviceScope.launch {
            service.serviceState.collect { state ->
                _recordingState.value = when (state) {
                    is ServiceState.Idle -> RecordingState.Idle
                    is ServiceState.Recording -> RecordingState.Recording(state.sessionId, state.duration)
                    is ServiceState.Paused -> RecordingState.Paused(state.sessionId, state.reason)
                    is ServiceState.Stopped -> RecordingState.Stopped(state.sessionId)
                    is ServiceState.Error -> RecordingState.Error(state.message)
                }
            }
        }
    }

    fun unbindService() {
        boundService = null
    }

    fun isServiceBound(): Boolean = boundService != null
}