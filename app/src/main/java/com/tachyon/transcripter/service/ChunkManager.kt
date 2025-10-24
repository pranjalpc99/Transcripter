package com.tachyon.transcripter.service

import com.tachyon.transcripter.data.local.entity.AudioChunk
import com.tachyon.transcripter.data.repository.FileRepository
import com.tachyon.transcripter.data.repository.RecordingRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

// service/ChunkManager.kt
class ChunkManager @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val recordingRepository: RecordingRepository,
    private val fileRepository: FileRepository
) {
    private var currentSessionId: String? = null
    private var chunkNumber = 0
    private var chunkJob: Job? = null

    private var sessionStartTime: Long = 0
    private var chunkStartTime: Long = 0

    suspend fun startChunking(sessionId: String) {
        currentSessionId = sessionId
        chunkNumber = 0
        sessionStartTime = System.currentTimeMillis()
        chunkStartTime = 0

        recordNextChunk()
    }

    private suspend fun recordNextChunk() {
        val sessionId = currentSessionId ?: return

        val chunkFile = fileRepository.getChunkFile(sessionId, chunkNumber)

        // Start recording with overlap
        val overlapDuration = if (chunkNumber > 0) 2000L else 0L

        audioRecorder.startRecording(
            outputFile = chunkFile,
            maxDuration = CHUNK_DURATION_MS + overlapDuration
        )

        // Schedule chunk finalization
        chunkJob = GlobalScope.launch {
            delay(CHUNK_DURATION_MS)
            finalizeCurrentChunk()
            recordNextChunk()
        }
    }

    private suspend fun finalizeCurrentChunk() {
        val sessionId = currentSessionId ?: return

        audioRecorder.stopRecording()

        val chunkFile = fileRepository.getChunkFile(sessionId, chunkNumber)
        val actualDuration = audioRecorder.getActualDuration()

        val chunk = AudioChunk(
            sessionId = sessionId,
            chunkNumber = chunkNumber,
            startTimeMs = chunkStartTime,
            endTimeMs = chunkStartTime + actualDuration,
            durationMs = actualDuration,
            filePath = chunkFile.absolutePath,
            fileSizeBytes = chunkFile.length(),
            hasOverlap = chunkNumber > 0,
            overlapStartMs = if (chunkNumber > 0) chunkStartTime else null,
            overlapDurationMs = if (chunkNumber > 0) 2000L else 0L
        )

        recordingRepository.insertChunk(chunk)

        chunkStartTime += CHUNK_DURATION_MS
        chunkNumber++
    }

    suspend fun pauseChunking() {
        chunkJob?.cancel()
        audioRecorder.pause()
    }

    suspend fun resumeChunking() {
        recordNextChunk()
    }

    suspend fun stopChunking() {
        chunkJob?.cancel()
        audioRecorder.stopRecording()
        finalizeCurrentChunk()
        currentSessionId = null
    }

    companion object {
        const val CHUNK_DURATION_MS = 30_000L  // 30 seconds
    }
}