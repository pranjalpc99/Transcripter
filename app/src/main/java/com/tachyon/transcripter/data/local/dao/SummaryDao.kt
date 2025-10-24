package com.tachyon.transcripter.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tachyon.transcripter.data.local.entity.Summary
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: Summary): Long

    @Update
    suspend fun update(summary: Summary)

    @Query("SELECT * FROM summaries WHERE session_id = :sessionId")
    suspend fun getBySessionId(sessionId: String): Summary?

    @Query("SELECT * FROM summaries WHERE session_id = :sessionId")
    fun observeBySessionId(sessionId: String): Flow<Summary?>

    @Query("UPDATE summaries SET partial_content = :content, updated_at = :timestamp WHERE session_id = :sessionId")
    suspend fun updatePartialContent(sessionId: String, content: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM summaries WHERE session_id = :sessionId")
    suspend fun deleteBySession(sessionId: String)
}