package com.jarvis.android.data

import android.content.Context
import com.jarvis.android.core.model.AiMode
import com.jarvis.android.core.model.ConversationEntry
import com.jarvis.android.core.model.ConversationRole
import com.jarvis.android.core.model.JarvisPreferences
import com.jarvis.android.core.model.MemoryNote
import org.json.JSONArray
import org.json.JSONObject

/**
 * İlk sürümde küçük, kullanıcı kaynaklı verileri cihazdaki uygulama alanında saklar.
 * Büyük arama endeksi ve şifreli uzun süreli bellek için sonraki modülde Room + Keystore
 * deposuna geçirilecek bir sınırdır.
 */
class JarvisMemoryStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadPreferences(): JarvisPreferences = JarvisPreferences(
        operatorName = preferences.getString(KEY_OPERATOR_NAME, "Komutan").orEmpty().ifBlank { "Komutan" },
        speechEnabled = preferences.getBoolean(KEY_SPEECH_ENABLED, true),
        animationsEnabled = preferences.getBoolean(KEY_ANIMATIONS_ENABLED, true),
        selectedAiMode = preferences.getString(KEY_AI_MODE, AiMode.HYBRID.name)
            ?.let { runCatching { AiMode.valueOf(it) }.getOrNull() }
            ?: AiMode.HYBRID
    )

    fun savePreferences(value: JarvisPreferences) {
        preferences.edit()
            .putString(KEY_OPERATOR_NAME, value.operatorName)
            .putBoolean(KEY_SPEECH_ENABLED, value.speechEnabled)
            .putBoolean(KEY_ANIMATIONS_ENABLED, value.animationsEnabled)
            .putString(KEY_AI_MODE, value.selectedAiMode.name)
            .apply()
    }

    fun loadConversation(): List<ConversationEntry> = parseArray(KEY_CONVERSATION).mapNotNull { item ->
        runCatching {
            ConversationEntry(
                id = item.getLong("id"),
                role = ConversationRole.valueOf(item.getString("role")),
                text = item.getString("text"),
                createdAtMillis = item.getLong("createdAt")
            )
        }.getOrNull()
    }

    fun appendConversation(entry: ConversationEntry) {
        val current = loadConversation().plus(entry).takeLast(MAX_CONVERSATION_ITEMS)
        val encoded = JSONArray()
        current.forEach {
            encoded.put(
                JSONObject()
                    .put("id", it.id)
                    .put("role", it.role.name)
                    .put("text", it.text)
                    .put("createdAt", it.createdAtMillis)
            )
        }
        preferences.edit().putString(KEY_CONVERSATION, encoded.toString()).apply()
    }

    fun loadNotes(): List<MemoryNote> = parseArray(KEY_NOTES).mapNotNull { item ->
        runCatching {
            MemoryNote(
                id = item.getLong("id"),
                value = item.getString("value"),
                createdAtMillis = item.getLong("createdAt")
            )
        }.getOrNull()
    }

    fun saveNote(value: String): MemoryNote {
        val note = MemoryNote(id = System.currentTimeMillis(), value = value)
        val notes = loadNotes().plus(note).takeLast(MAX_NOTES)
        saveNotes(notes)
        return note
    }

    fun forget(query: String): Int {
        val original = loadNotes()
        val kept = if (query.isBlank()) original.dropLast(1) else original.filterNot {
            it.value.contains(query, ignoreCase = true)
        }
        saveNotes(kept)
        return original.size - kept.size
    }

    private fun saveNotes(notes: List<MemoryNote>) {
        val encoded = JSONArray()
        notes.forEach {
            encoded.put(
                JSONObject()
                    .put("id", it.id)
                    .put("value", it.value)
                    .put("createdAt", it.createdAtMillis)
            )
        }
        preferences.edit().putString(KEY_NOTES, encoded.toString()).apply()
    }

    private fun parseArray(key: String): List<JSONObject> = runCatching {
        val array = JSONArray(preferences.getString(key, "[]"))
        List(array.length()) { index -> array.getJSONObject(index) }
    }.getOrDefault(emptyList())

    companion object {
        private const val PREFS_NAME = "jarvis_local_memory"
        private const val KEY_OPERATOR_NAME = "operator_name"
        private const val KEY_SPEECH_ENABLED = "speech_enabled"
        private const val KEY_ANIMATIONS_ENABLED = "animations_enabled"
        private const val KEY_AI_MODE = "ai_mode"
        private const val KEY_CONVERSATION = "conversation"
        private const val KEY_NOTES = "notes"
        private const val MAX_CONVERSATION_ITEMS = 60
        private const val MAX_NOTES = 120
    }
}
