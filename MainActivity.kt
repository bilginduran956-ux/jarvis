package com.jarvis.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import com.jarvis.android.core.voice.JarvisVoiceEngine
import com.jarvis.android.ui.JarvisApp
import com.jarvis.android.ui.JarvisViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: JarvisViewModel by viewModels()
    private lateinit var voiceEngine: JarvisVoiceEngine

    private val microphonePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceSession()
        } else {
            viewModel.voiceError("Sesli komut için Mikrofon izni gerekli. İzni Android ayarlarından verebilirsiniz.")
        }
    }

    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.markReady()
        } else {
            viewModel.voiceError("Fener ve kamera işlemleri için Kamera izni verilmedi.")
        }
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.markReady()
        } else {
            viewModel.voiceError("Bildirim izni verilmedi. JARVIS bunu değiştirmedi.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        voiceEngine = JarvisVoiceEngine(this)
        window.statusBarColor = android.graphics.Color.rgb(3, 9, 14)
        window.navigationBarColor = android.graphics.Color.rgb(3, 9, 14)

        setContent {
            val state = viewModel.uiState.collectAsStateWithLifecycle().value
            LaunchedEffect(state.spokenToken) {
                if (state.spokenToken > 0L && state.preferences.speechEnabled) {
                    state.spokenText?.let(voiceEngine::speak)
                }
            }
            JarvisApp(
                state = state,
                onSubmitCommand = viewModel::submitCommand,
                onVoiceClick = ::requestVoiceSession,
                onRefreshSystem = viewModel::refreshSystem,
                onSavePreferences = viewModel::savePreferences,
                onSaveMemory = viewModel::addMemory,
                onRequestCameraPermission = ::requestCameraPermission,
                onRequestNotificationPermission = ::requestNotificationPermission
            )
        }
    }

    private fun requestVoiceSession() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startVoiceSession()
        } else {
            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVoiceSession() {
        viewModel.beginListening()
        voiceEngine.speak("Dinliyorum.")
        voiceEngine.startListening(
            onReady = viewModel::beginListening,
            onResult = viewModel::submitCommand,
            onError = viewModel::voiceError
        )
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            viewModel.markReady()
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT < 33) {
            viewModel.markReady()
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            viewModel.markReady()
        } else {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onDestroy() {
        voiceEngine.close()
        super.onDestroy()
    }
}
