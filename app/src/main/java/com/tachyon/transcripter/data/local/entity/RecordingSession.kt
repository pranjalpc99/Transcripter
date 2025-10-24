package com.tachyon.transcripter.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "recording_sessions")
data class RecordingSession(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "start_time")
    val startTime: Long,

    @ColumnInfo(name = "end_time")
    val endTime: Long? = null,

    @ColumnInfo(name = "total_duration_ms")
    val totalDurationMs: Long = 0,

    @ColumnInfo(name = "pause_duration_ms")
    val pauseDurationMs: Long = 0,

    @ColumnInfo(name = "status")
    val status: SessionStatus,

    @ColumnInfo(name = "storage_path")
    val storagePath: String,

    @ColumnInfo(name = "total_size_bytes")
    val totalSizeBytes: Long = 0,

    @ColumnInfo(name = "chunk_count")
    val chunkCount: Int = 0,

    @ColumnInfo(name = "transcribed_chunk_count")
    val transcribedChunkCount: Int = 0,

    @ColumnInfo(name = "has_summary")
    val hasSummary: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "pause_reason")
    val pauseReason: String? = null,  // ADD THIS LINE
)

enum class SessionStatus {
    RECORDING,          // Currently recording
    PAUSED,             // Paused by user or interruption
    STOPPED,            // Stopped, waiting for transcription
    TRANSCRIBING,       // Transcription in progress
    TRANSCRIPTION_FAILED, // Transcription failed
    GENERATING_SUMMARY, // Summary generation in progress
    COMPLETED,          // Fully processed
    FAILED              // Unrecoverable error
}

fun SessionStatus.toDisplayText(): String = when (this) {
    SessionStatus.RECORDING            -> "Recording"
    SessionStatus.PAUSED               -> "Paused"
    SessionStatus.STOPPED              -> "Stopped"
    SessionStatus.TRANSCRIBING         -> "Transcribing…"
    SessionStatus.TRANSCRIPTION_FAILED -> "Transcription failed"
    SessionStatus.GENERATING_SUMMARY   -> "Generating summary…"
    SessionStatus.COMPLETED            -> "Completed"
    SessionStatus.FAILED               -> "Failed"
}
