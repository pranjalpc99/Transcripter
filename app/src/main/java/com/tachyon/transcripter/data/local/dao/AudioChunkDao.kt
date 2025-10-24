package com.tachyon.transcripter.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tachyon.transcripter.data.local.entity.AudioChunk
import com.tachyon.transcripter.domain.model.TranscriptionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioChunkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chunk: AudioChunk): Long

    @Update
    suspend fun update(chunk: AudioChunk)

    @Query("SELECT * FROM audio_chunks WHERE session_id = :sessionId ORDER BY chunk_number ASC")
    suspend fun getChunksBySession(sessionId: String): List<AudioChunk>

    @Query("SELECT * FROM audio_chunks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AudioChunk?

    @Query("SELECT * FROM audio_chunks WHERE session_id = :sessionId ORDER BY chunk_number ASC")
    fun observeChunksBySession(sessionId: String): Flow<List<AudioChunk>>

    @Query("SELECT * FROM audio_chunks WHERE transcription_status = :status ORDER BY created_at ASC LIMIT :limit")
    suspend fun getChunksByStatus(status: TranscriptionStatus, limit: Int = 10): List<AudioChunk>

    @Query("UPDATE audio_chunks SET transcription_status = :status WHERE id = :chunkId")
    suspend fun updateTranscriptionStatus(chunkId: String, status: TranscriptionStatus)

    @Query("UPDATE audio_chunks SET upload_attempt_count = upload_attempt_count + 1, last_upload_attempt = :timestamp WHERE id = :chunkId")
    suspend fun incrementUploadAttempt(chunkId: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM audio_chunks WHERE session_id = :sessionId AND transcription_status = 'COMPLETED'")
    suspend fun getCompletedTranscriptionCount(sessionId: String): Int

    @Query("DELETE FROM audio_chunks WHERE session_id = :sessionId")
    suspend fun deleteBySession(sessionId: String)
}