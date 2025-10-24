package com.tachyon.transcripter.di

import android.content.Context
import androidx.room.Room
import com.tachyon.transcripter.data.local.AppDatabase
import com.tachyon.transcripter.data.local.dao.AudioChunkDao
import com.tachyon.transcripter.data.local.dao.RecordingSessionDao
import com.tachyon.transcripter.data.local.dao.SummaryDao
import com.tachyon.transcripter.data.local.dao.TranscriptSegmentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideRecordingSessionDao(database: AppDatabase): RecordingSessionDao {
        return database.recordingSessionDao()
    }

    @Provides
    @Singleton
    fun provideAudioChunkDao(database: AppDatabase): AudioChunkDao {
        return database.audioChunkDao()
    }

    @Provides
    @Singleton
    fun provideTranscriptSegmentDao(database: AppDatabase): TranscriptSegmentDao {
        return database.transcriptSegmentDao()
    }

    @Provides
    @Singleton
    fun provideSummaryDao(database: AppDatabase): SummaryDao {
        return database.summaryDao()
    }
}