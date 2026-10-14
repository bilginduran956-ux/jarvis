package com.jarvis.android.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.android.core.JarvisCore
import com.jarvis.android.core.command.TurkishCommandParser
import com.jarvis.android.core.device.AndroidActionGateway
import com.jarvis.android.core.device.DeviceMonitor
import com.jarvis.android.core.model.ConversationEntry
import com.jarvis.android.core.model.ConversationRole
import com.jarvis.android.core.model.DeviceSnapshot
import com.jarvis.android.core.model.JarvisPreferences
import com.jarvis.android.core.model.JarvisStatus
import com.jarvis.android.core.model.MemoryNote
import com.jarvis.android.data.JarvisMemoryStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class JarvisUiState(
    val status: JarvisStatus = JarvisStatus.READY,
    val statusDetail: String = "Merhaba. JARVIS hazır.",
    val preferences: JarvisPreferences = JarvisPreferences(),
    val snapshot: DeviceSnapshot? = null,
    val conversation: List<ConversationEntry> = emptyList(),
    val notes: List<MemoryNote> = emptyList(),
    val isProcessing: Boolean = false,
    val spokenText: String? = null,
    val spokenToken: Long = 0L,
    val lastPlanSummary: String = "Komut bekleniyor",
    val lastError: String? = null
)

class JarvisViewModel(application: Application) : AndroidViewModel(application) {
    private val memory = JarvisMemoryStore(application)
    private val monitor = DeviceMonitor(application)
    private val core = JarvisCore(
        parser = TurkishCommandParser(),
        actionGateway = AndroidActionGateway(application),
        deviceMonitor = monitor,
        memoryStore = memory
    )

    private val _uiState = MutableStateFlow(
        JarvisUiState(
            preferences = memory.loadPreferences(),
            conversation = memory.loadConversation(),
            notes = memory.loadNotes()
        )
    )
    val uiState: StateFlow<JarvisUiState> = _uiState.asStateFlow()

    init {
        refreshSystem()
    }

    fun submitCommand(command: String) {
        val clean = command.trim()
        if (clean.isBlank() || _uiState.value.isProcessing) return
        val userEntry = ConversationEntry(
            id = System.nanoTime(),
            role = ConversationRole.USER,
            text = clean
        )
        _uiState.value = _uiState.value.copy(
            status = JarvisStatus.UNDERSTANDING,
            statusDetail = "Komut ayrıştırılıyor…",
            isProcessing = true,
            conversation = (_uiState.value.conversation + userEntry).takeLast(60),
            lastError = null
        )
        viewModelScope.launch {
            runCatching { core.process(clean) }
                .onSuccess { outcome ->
                    val assistant = ConversationEntry(
                        id = System.nanoTime(),
                        role = ConversationRole.JARVIS,
                        text = outcome.reply
                    )
                    val finalStatus = if (outcome.results.all { it.success }) {
                        JarvisStatus.COMPLETE
                    } else {
                        JarvisStatus.WARNING
                    }
                    _uiState.value = _uiState.value.copy(
                        status = finalStatus,
                        statusDetail = outcome.reply,
                        snapshot = outcome.deviceSnapshot ?: _uiState.value.snapshot,
                        isProcessing = false,
                        conversation = (_uiState.value.conversation + assistant).takeLast(60),
                        notes = memory.loadNotes(),
                        spokenText = outcome.reply,
                        spokenToken = System.nanoTime(),
                        lastPlanSummary = outcome.plan.shortSummary,
                        lastError = outcome.results.firstOrNull { !it.success }?.message
                    )
                }
                .onFailure { error ->
                    val explanation = "JARVIS işlemi tamamlayamadı: " + (error.message ?: "bilinmeyen hata")
                    _uiState.value = _uiState.value.copy(
                        status = JarvisStatus.ERROR,
                        statusDetail = explanation,
                        isProcessing = false,
                        spokenText = explanation,
                        spokenToken = System.nanoTime(),
                        lastError = explanation
                    )
                }
        }
    }

    fun refreshSystem() {
        viewModelScope.launch {
            val snapshot = withContext(Dispatchers.Default) { monitor.snapshot() }
            _uiState.value = _uiState.value.copy(snapshot = snapshot)
        }
    }

    fun beginListening() {
        _uiState.value = _uiState.value.copy(
            status = JarvisStatus.LISTENING,
            statusDetail = "Dinliyorum. Komutunuzu söyleyin.",
            lastError = null
        )
    }

    fun voiceError(message: String) {
        _uiState.value = _uiState.value.copy(
            status = JarvisStatus.WARNING,
            statusDetail = message,
            lastError = message
        )
    }

    fun markReady() {
        if (!_uiState.value.isProcessing) {
            _uiState.value = _uiState.value.copy(
                status = JarvisStatus.READY,
                statusDetail = "Yeni bir komut için hazırım."
            )
        }
    }

    fun savePreferences(value: JarvisPreferences) {
        memory.savePreferences(value)
        _uiState.value = _uiState.value.copy(preferences = value)
    }

    fun addMemory(value: String) {
        if (value.isBlank()) return
        memory.saveNote(value.trim())
        _uiState.value = _uiState.value.copy(notes = memory.loadNotes())
    }
}
