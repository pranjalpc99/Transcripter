package com.tachyon.transcripter.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tachyon.transcripter.data.local.dao.AudioChunkDao
import com.tachyon.transcripter.data.local.dao.RecordingSessionDao
import com.tachyon.transcripter.data.local.dao.SummaryDao
import com.tachyon.transcripter.data.local.dao.TranscriptSegmentDao
import com.tachyon.transcripter.data.local.entity.AudioChunk
import com.tachyon.transcripter.data.local.entity.RecordingSession
import com.tachyon.transcripter.data.local.entity.Summary
import com.tachyon.transcripter.data.local.entity.TranscriptSegment

@Database(
    entities = [
        RecordingSession::class,
        AudioChunk::class,
        TranscriptSegment::class,
        Summary::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordingSessionDao(): RecordingSessionDao
    abstract fun audioChunkDao(): AudioChunkDao
    abstract fun transcriptSegmentDao(): TranscriptSegmentDao
    abstract fun summaryDao(): SummaryDao

    companion object {
        const val DATABASE_NAME = "voice_recorder_db"
    }
}