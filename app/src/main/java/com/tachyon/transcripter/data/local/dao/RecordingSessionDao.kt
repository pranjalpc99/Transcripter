package com.tachyon.transcripter.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tachyon.transcripter.data.local.entity.RecordingSession
import com.tachyon.transcripter.data.local.entity.SessionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: RecordingSession): Long

    @Update
    suspend fun update(session: RecordingSession)

    @Query("SELECT * FROM recording_sessions WHERE id = :sessionId")
    suspend fun getById(sessionId: String): RecordingSession?

    @Query("SELECT * FROM recording_sessions WHERE id = :sessionId")
    fun observeById(sessionId: String): Flow<RecordingSession?>

    @Query("SELECT * FROM recording_sessions ORDER BY created_at DESC")
    fun observeAll(): Flow<List<RecordingSession>>

    @Query("SELECT * FROM recording_sessions WHERE status IN (:statuses)")
    suspend fun getByStatuses(statuses: List<SessionStatus>): List<RecordingSession>

    @Query("UPDATE recording_sessions SET status = :status, updated_at = :timestamp WHERE id = :sessionId")
    suspend fun updateStatus(sessionId: String, status: SessionStatus, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE recording_sessions SET chunk_count = chunk_count + 1, updated_at = :timestamp WHERE id = :sessionId")
    suspend fun incrementChunkCount(sessionId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE recording_sessions SET transcribed_chunk_count = transcribed_chunk_count + 1, updated_at = :timestamp WHERE id = :sessionId")
    suspend fun incrementTranscribedCount(sessionId: String, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(session: RecordingSession)

    @Query("DELETE FROM recording_sessions WHERE id = :sessionId")
    suspend fun deleteById(sessionId: String)
}