// RepositoryModule.kt
package com.tachyon.transcripter.di

import android.content.Context
import com.tachyon.transcripter.data.local.dao.AudioChunkDao
import com.tachyon.transcripter.data.local.dao.RecordingSessionDao
import com.tachyon.transcripter.data.local.dao.SummaryDao
import com.tachyon.transcripter.data.local.dao.TranscriptSegmentDao
import com.tachyon.transcripter.data.remote.api.GeminiApi
import com.tachyon.transcripter.data.repository.FileRepository
import com.tachyon.transcripter.data.repository.RecordingRepository
import com.tachyon.transcripter.data.repository.SummaryRepository
import com.tachyon.transcripter.data.repository.TranscriptionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideFileRepository(
        @ApplicationContext context: Context,
        ioDispatcher: CoroutineDispatcher
    ): FileRepository {
        return FileRepository(context,ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideRecordingRepository(
        sessionDao: RecordingSessionDao,
        audioChunkDao: AudioChunkDao,
        fileRepository: FileRepository,
        ioDispatcher: CoroutineDispatcher
    ): RecordingRepository {
        return RecordingRepository(
            sessionDao,
            audioChunkDao,
            fileRepository,
            ioDispatcher
        )
    }

    @Provides
    @Singleton
    fun provideTranscriptionRepository(
        audioChunkDao: AudioChunkDao,
        transcriptSegmentDao: TranscriptSegmentDao,
        geminiApi: GeminiApi,
        fileRepository: FileRepository,
        recordingSessionDao: RecordingSessionDao,
        ioDispatcher: CoroutineDispatcher
    ): TranscriptionRepository {
        return TranscriptionRepository(
            geminiApi,
            audioChunkDao,
            transcriptSegmentDao,
            recordingSessionDao,
            fileRepository,
            ioDispatcher
        )
    }

    @Provides
    @Singleton
    fun provideSummaryRepository(
        summaryDao: SummaryDao,
        transcriptSegmentDao: TranscriptSegmentDao,
        transcriptionRepository: TranscriptionRepository,
        geminiApi: GeminiApi,
        ioDispatcher: CoroutineDispatcher
    ): SummaryRepository {
        return SummaryRepository(
            geminiApi,
            summaryDao,
            transcriptSegmentDao,
            transcriptionRepository,
            ioDispatcher
        )
    }
}