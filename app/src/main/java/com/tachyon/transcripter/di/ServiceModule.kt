package com.tachyon.transcripter.di

// ServiceModule.kt

import android.content.Context
import com.tachyon.transcripter.service.AudioDeviceMonitor
import com.tachyon.transcripter.service.AudioFocusManager
import com.tachyon.transcripter.service.AudioRecorder
import com.tachyon.transcripter.service.ChunkManager
import com.tachyon.transcripter.service.InterruptionHandler
import com.tachyon.transcripter.service.NotificationHelper
import com.tachyon.transcripter.service.PhoneStateHandler
import com.tachyon.transcripter.service.ServiceBridge
import com.tachyon.transcripter.service.SilenceDetector
import com.tachyon.transcripter.service.StorageMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing service-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideAudioRecorder(
        @ApplicationContext context: Context
    ): AudioRecorder {
        return AudioRecorder(context)
    }

    @Provides
    @Singleton
    fun provideChunkManager(
        audioRecorder: AudioRecorder,
        recordingRepository: com.tachyon.transcripter.data.repository.RecordingRepository,
        fileRepository: com.tachyon.transcripter.data.repository.FileRepository
    ): ChunkManager {
        return ChunkManager(audioRecorder, recordingRepository, fileRepository)
    }

    @Provides
    @Singleton
    fun provideAudioFocusManager(
        @ApplicationContext context: Context
    ): AudioFocusManager {
        return AudioFocusManager(context)
    }

    @Provides
    @Singleton
    fun providePhoneStateHandler(
        @ApplicationContext context: Context
    ): PhoneStateHandler {
        return PhoneStateHandler(context)
    }

    @Provides
    @Singleton
    fun provideAudioDeviceMonitor(
        @ApplicationContext context: Context
    ): AudioDeviceMonitor {
        return AudioDeviceMonitor(context)
    }

    @Provides
    @Singleton
    fun provideStorageMonitor(
        @ApplicationContext context: Context
    ): StorageMonitor {
        return StorageMonitor(context)
    }

    @Provides
    @Singleton
    fun provideSilenceDetector(): SilenceDetector {
        return SilenceDetector()
    }

    @Provides
    @Singleton
    fun provideNotificationHelper(
        @ApplicationContext context: Context
    ): NotificationHelper {
        return NotificationHelper(context)
    }

    @Provides
    @Singleton
    fun provideInterruptionHandler(
        audioFocusManager: AudioFocusManager,
        phoneStateHandler: PhoneStateHandler,
        audioDeviceMonitor: AudioDeviceMonitor,
        storageMonitor: StorageMonitor,
        silenceDetector: SilenceDetector
    ): InterruptionHandler {
        return InterruptionHandler(
            audioFocusManager,
            phoneStateHandler,
            audioDeviceMonitor,
            storageMonitor,
            silenceDetector
        )
    }

    @Provides
    @Singleton
    fun provideServiceBridge(): ServiceBridge {
        return ServiceBridge()
    }
}

/**
 * Note: RecordingService itself cannot be provided by Hilt
 * because it's a system service that must be started via Intent.
 * Instead, we provide all the dependencies that RecordingService needs,
 * and inject them directly into the service using @AndroidEntryPoint.
 *
 * Example in RecordingService:
 *
 * @AndroidEntryPoint
 * class RecordingService : Service() {
 *     @Inject lateinit var audioRecorder: AudioRecorder
 *     @Inject lateinit var chunkManager: ChunkManager
 *     @Inject lateinit var interruptionHandler: InterruptionHandler
 *     // ... etc
 * }
 */