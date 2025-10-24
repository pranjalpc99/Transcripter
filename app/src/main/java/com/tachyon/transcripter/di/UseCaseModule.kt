package com.tachyon.transcripter.di

// UseCaseModule.kt
import android.content.Context
import com.tachyon.transcripter.data.repository.FileRepository
import com.tachyon.transcripter.data.repository.RecordingRepository
import com.tachyon.transcripter.data.repository.SummaryRepository
import com.tachyon.transcripter.data.repository.TranscriptionRepository
import com.tachyon.transcripter.domain.usecases.GenerateSummaryUseCase
import com.tachyon.transcripter.domain.usecases.GetRecordingStateUseCase
import com.tachyon.transcripter.domain.usecases.PauseRecordingUseCase
import com.tachyon.transcripter.domain.usecases.ProcessTranscriptionUseCase
import com.tachyon.transcripter.domain.usecases.ResumeRecordingUseCase
import com.tachyon.transcripter.domain.usecases.StartRecordingUseCase
import com.tachyon.transcripter.domain.usecases.StopRecordingUseCase
import com.tachyon.transcripter.service.ServiceBridge
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for providing use case dependencies.
 * Use cases are scoped to ViewModel lifecycle.
 */
@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {

    @Provides
    @ViewModelScoped
    fun provideStartRecordingUseCase(
        recordingRepository: RecordingRepository,
        fileRepository: FileRepository,
        @ApplicationContext context: Context
    ): StartRecordingUseCase {
        return StartRecordingUseCase(
            recordingRepository,
            fileRepository,
            context
        )
    }

    @Provides
    @ViewModelScoped
    fun provideStopRecordingUseCase(
        recordingRepository: RecordingRepository,
        @ApplicationContext context: Context
    ): StopRecordingUseCase {
        return StopRecordingUseCase(
            recordingRepository,
            context
        )
    }

    @Provides
    @ViewModelScoped
    fun providePauseRecordingUseCase(
        recordingRepository: RecordingRepository,
        @ApplicationContext context: Context
    ): PauseRecordingUseCase {
        return PauseRecordingUseCase(
            recordingRepository,
            context
        )
    }

    @Provides
    @ViewModelScoped
    fun provideResumeRecordingUseCase(
        recordingRepository: RecordingRepository,
        @ApplicationContext context: Context
    ): ResumeRecordingUseCase {
        return ResumeRecordingUseCase(
            recordingRepository,
            context
        )
    }

    @Provides
    @ViewModelScoped
    fun provideGetRecordingStateUseCase(
        serviceBridge: ServiceBridge
    ): GetRecordingStateUseCase {
        return GetRecordingStateUseCase(serviceBridge)
    }

    @Provides
    @ViewModelScoped
    fun provideProcessTranscriptionUseCase(
        transcriptionRepository: TranscriptionRepository,
        recordingRepository: RecordingRepository
    ): ProcessTranscriptionUseCase {
        return ProcessTranscriptionUseCase(
            transcriptionRepository,
            recordingRepository
        )
    }

    @Provides
    @ViewModelScoped
    fun provideGenerateSummaryUseCase(
        summaryRepository: SummaryRepository,
        transcriptionRepository: TranscriptionRepository,
        recordingRepository: RecordingRepository
    ): GenerateSummaryUseCase {
        return GenerateSummaryUseCase(
            summaryRepository,
            transcriptionRepository,
            recordingRepository
        )
    }
}

/**
 * Alternative approach using @Inject constructor in use cases:
 *
 * If your use cases have @Inject constructors, you don't need this module.
 * Hilt will automatically provide them.
 *
 * Example:
 * class StartRecordingUseCase @Inject constructor(
 *     private val repository: RecordingRepository
 * ) { ... }
 *
 * Then this entire module can be removed.
 */