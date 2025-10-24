package com.tachyon.transcripter.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tachyon.transcripter.domain.model.TranscriptionStatus
import java.util.UUID

@Entity(
    tableName = "audio_chunks",
    foreignKeys = [
        ForeignKey(
            entity = RecordingSession::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["session_id"]),
        Index(value = ["session_id", "chunk_number"])
    ]
)
data class AudioChunk(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "session_id")
    val sessionId: String,

    @ColumnInfo(name = "chunk_number")
    val chunkNumber: Int,                       // 0-based sequential number

    @ColumnInfo(name = "start_time_ms")
    val startTimeMs: Long,                      // Relative to session start

    @ColumnInfo(name = "end_time_ms")
    val endTimeMs: Long,                        // Relative to session start

    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,                       // Actual duration (may be < 30s)

    @ColumnInfo(name = "file_path")
    val filePath: String,                       // Absolute path to .m4a file

    @ColumnInfo(name = "file_size_bytes")
    val fileSizeBytes: Long,

    @ColumnInfo(name = "has_overlap")
    val hasOverlap: Boolean = true,             // False for first chunk

    @ColumnInfo(name = "overlap_start_ms")
    val overlapStartMs: Long? = null,           // Where overlap begins

    @ColumnInfo(name = "overlap_duration_ms")
    val overlapDurationMs: Long = 2000,         // 2 seconds

    @ColumnInfo(name = "transcription_status")
    val transcriptionStatus: TranscriptionStatus = TranscriptionStatus.PENDING,

    @ColumnInfo(name = "upload_attempt_count")
    val uploadAttemptCount: Int = 0,

    @ColumnInfo(name = "last_upload_attempt")
    val lastUploadAttempt: Long? = null,

    @ColumnInfo(name = "upload_error")
    val uploadError: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

