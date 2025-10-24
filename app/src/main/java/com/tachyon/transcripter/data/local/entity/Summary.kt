package com.tachyon.transcripter.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "summaries",
    foreignKeys = [
        ForeignKey(
            entity = RecordingSession::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["session_id"])]
)
data class Summary(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "session_id")
    val sessionId: String,

    val title: String?,
    val summary: String?,

    @ColumnInfo(name = "action_items")
    val actionItems: String?,                   // JSON array or newline-separated

    @ColumnInfo(name = "key_points")
    val keyPoints: String?,                     // JSON array or newline-separated

    @ColumnInfo(name = "generation_status")
    val generationStatus: GenerationStatus = GenerationStatus.PENDING,

    @ColumnInfo(name = "partial_content")
    val partialContent: String? = null,         // For streaming progress

    @ColumnInfo(name = "attempt_count")
    val attemptCount: Int = 0,

    @ColumnInfo(name = "last_attempt")
    val lastAttempt: Long? = null,

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

enum class GenerationStatus {
    PENDING,
    GENERATING,
    COMPLETED,
    FAILED
}
