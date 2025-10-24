package com.tachyon.transcripter

// MainActivity.kt

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.WorkManager
import com.tachyon.transcripter.ui.theme.VoiceRecorderTheme
import com.tachyon.transcripter.ui.navigation.VoiceRecorderNavGraph
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single activity host for the voice recording app.
 * Handles permissions and sets up navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var hasRecordPermission by mutableStateOf(false)
    private var hasNotificationPermission by mutableStateOf(true)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasRecordPermission = permissions[Manifest.permission.RECORD_AUDIO] == true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = permissions[Manifest.permission.POST_NOTIFICATIONS] == true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check initial permissions
        checkPermissions()

        WorkManager.getInstance(applicationContext)
            .getWorkInfosForUniqueWorkLiveData("transcription_YOUR_SESSION_ID")
            .observe(this) { workInfos ->
                workInfos?.forEach { workInfo ->
                    Log.d("MainActivity", "Work status: ${workInfo.state}")
                    Log.d("MainActivity", "Work progress: ${workInfo.progress}")
                }
            }

        setContent {
            VoiceRecorderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VoiceRecorderApp(
                        hasRecordPermission = hasRecordPermission,
                        hasNotificationPermission = hasNotificationPermission,
                        onRequestPermissions = { requestPermissions() }
                    )
                }
            }
        }
    }

    /**
     * Check if all required permissions are granted.
     */
    private fun checkPermissions() {
        hasRecordPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Request required permissions.
     */
    private fun requestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        permissionLauncher.launch(permissions.toTypedArray())
    }
}

/**
 * Main app composable.
 */
@Composable
fun VoiceRecorderApp(
    hasRecordPermission: Boolean,
    hasNotificationPermission: Boolean,
    onRequestPermissions: () -> Unit
) {
    if (!hasRecordPermission) {
        // Show permission request UI
        PermissionRequestScreen(
            onRequestPermissions = onRequestPermissions
        )
    } else {
        // Show main app navigation
        VoiceRecorderNavGraph()
    }
}

/**
 * Permission request screen.
 */
@Composable
fun PermissionRequestScreen(
    onRequestPermissions: () -> Unit
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        androidx.compose.material3.Text(
            text = "Microphone Permission Required",
            style = MaterialTheme.typography.headlineMedium
        )

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

        androidx.compose.material3.Text(
            text = "This app needs access to your microphone to record audio.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(32.dp))

        androidx.compose.material3.Button(
            onClick = onRequestPermissions
        ) {
            androidx.compose.material3.Text("Grant Permission")
        }
    }
}