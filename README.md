# Transcripter - AI-Powered Voice Recording & Transcription App

A robust Android application that records audio, transcribes it using AI, and generates intelligent summaries - all with production-grade reliability and edge case handling.

## 🎯 Features

### Core Functionality
- **🎙️ Background Audio Recording** - Continuous recording with foreground service
- **✂️ Smart Chunking** - 30-second chunks with 2-second overlap for seamless transcription
- **🤖 AI Transcription** - Powered by Google Gemini 2.5 Flash API
- **📝 Intelligent Summaries** - Structured summaries with title, key points, and action items
- **💾 Offline-First Architecture** - Works offline, syncs when connected

### Advanced Capabilities
- **🔄 Process Death Recovery** - Survives app kills and system restarts
- **📞 Interruption Handling** - Gracefully handles phone calls, audio focus loss, and device changes
- **🔋 Low Storage Detection** - Prevents recording when storage is insufficient
- **🔁 Automatic Retry Logic** - Robust error handling with exponential backoff
- **📊 Real-time Progress** - Live transcription and summary generation updates
- **🔐 Secure Storage** - Local database with Room persistence

## 🏗️ Architecture

### Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt
- **Database**: Room
- **Networking**: Retrofit + OkHttp
- **Background Work**: WorkManager + Foreground Services
- **Async**: Kotlin Coroutines + Flow

### Project Structure
```
app/
├── data/
│   ├── local/          # Room database, DAOs, entities
│   ├── remote/         # Retrofit API interfaces
│   └── repository/     # Data layer implementations
├── domain/
│   ├── model/          # Domain models
│   └── usecase/        # Business logic use cases
├── di/                 # Hilt dependency injection modules
├── service/            # Foreground recording service
├── worker/             # WorkManager background tasks
├── ui/                 # Compose UI screens and ViewModels
└── util/               # Utility classes
```

## 🎬 How It Works

1. **Recording**: User starts recording → Service captures audio in 30s chunks with 2s overlap
2. **Transcription**: When stopped → WorkManager uploads chunks to Gemini API sequentially
3. **Summary**: After transcription → Generates structured summary with AI
4. **Storage**: All data persisted locally with Room database

## 🛡️ Edge Cases Handled

- ✅ Phone call interruptions (auto-pause and resume)
- ✅ Audio focus loss (Bluetooth/headphone disconnects)
- ✅ Low storage warnings
- ✅ Process death during recording
- ✅ Network failures with retry
- ✅ API rate limiting
- ✅ Configuration changes
- ✅ Silent audio detection (optional)

## 🚀 Setup

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 24+ (Android 7.0)
- Google Gemini API key

### Installation

1. Clone the repository:
```bash
git clone https://github.com/yourusername/transcripter.git
cd transcripter
```

2. Add your Gemini API key to `local.properties`:
```properties
GEMINI_API_KEY=your_api_key_here
```

3. Build and run:
```bash
./gradlew clean build
./gradlew installDebug
```

## 📱 Permissions Required

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.INTERNET" />
```

## 📄 License

This project is licensed under  **CC BY-NC-SA 4.0** (Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International) - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Google Gemini API for AI transcription and summarization
- Android Jetpack libraries
- Kotlin Coroutines team

## 📧 Contact

For questions or feedback, please open an issue on GitHub.

---

**Built with ❤️ using Kotlin & Jetpack Compose**
