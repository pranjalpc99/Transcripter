package com.tachyon.transcripter.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tachyon.transcripter.data.local.entity.TranscriptSegment
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptSegmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(segment: TranscriptSegment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(segments: List<TranscriptSegment>)

    @Query("SELECT * FROM transcript_segments WHERE session_id = :sessionId ORDER BY sequence_number ASC")
    suspend fun getSegmentsBySession(sessionId: String): List<TranscriptSegment>

    @Query("SELECT * FROM transcript_segments WHERE session_id = :sessionId ORDER BY sequence_number ASC")
    fun observeSegmentsBySession(sessionId: String): Flow<List<TranscriptSegment>>

    @Query("SELECT MAX(sequence_number) FROM transcript_segments WHERE session_id = :sessionId")
    suspend fun getMaxSequenceNumber(sessionId: String): Int?

    @Query("DELETE FROM transcript_segments WHERE session_id = :sessionId")
    suspend fun deleteBySession(sessionId: String)
}