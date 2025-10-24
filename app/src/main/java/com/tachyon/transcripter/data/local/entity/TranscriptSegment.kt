package com.tachyon.transcripter.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "transcript_segments",
    foreignKeys = [
        ForeignKey(
            entity = AudioChunk::class,
            parentColumns = ["id"],
            childColumns = ["chunk_id"],
            onDelete = ForeignKey.Companion.CASCADE
        )
    ],
    indices = [
        Index(value = ["chunk_id"]),
        Index(value = ["session_id", "sequence_number"])
    ]
)
data class TranscriptSegment(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "session_id")
    val sessionId: String,                      // Denormalized for fast queries

    @ColumnInfo(name = "chunk_id")
    val chunkId: String,

    @ColumnInfo(name = "sequence_number")
    val sequenceNumber: Int,                    // Global order across all chunks

    @ColumnInfo(name = "text")
    val text: String,                           // Transcribed text

    @ColumnInfo(name = "start_time_ms")
    val startTimeMs: Long,                      // Relative to session start

    @ColumnInfo(name = "end_time_ms")
    val endTimeMs: Long,                        // Relative to session start

    @ColumnInfo(name = "is_in_overlap")
    val isInOverlap: Boolean = false,           // Used for deduplication

    @ColumnInfo(name = "confidence")
    val confidence: Float? = null,              // API confidence score

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)