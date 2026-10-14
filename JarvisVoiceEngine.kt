package com.jarvis.android.core.voice

import android.content.Context
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Kullanıcı tetiklemeli konuşma oturumu. Sürekli arka plan uyandırma kelimesi
 * ayrı bir on-device keyword modeli ve foreground service gerektirir; mahremiyet
 * ve pil bütçesi açısından bu temel sürümde varsayılan olarak kapalıdır.
 */
class JarvisVoiceEngine(context: Context) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private val textToSpeech = TextToSpeech(appContext, this)
    private var ready = false
    private var recognizer: SpeechRecognizer? = null

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            textToSpeech.language = Locale("tr", "TR")
            textToSpeech.setSpeechRate(0.96f)
        }
    }

    fun speak(value: String) {
        if (ready) {
            textToSpeech.speak(value, TextToSpeech.QUEUE_FLUSH, null, "jarvis-response")
        }
    }

    fun startListening(
        onReady: () -> Unit,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            onError("Bu cihazda Android konuşma tanıma servisi kullanılamıyor.")
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).also { speechRecognizer ->
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = onReady()
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onError(error: Int) {
                    onError(errorText(error))
                }
                override fun onResults(results: Bundle?) {
                    val spoken = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    if (spoken.isNullOrBlank()) onError("Konuşma metne çevrilemedi.") else onResult(spoken)
                }
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            speechRecognizer.startListening(
                IntentRecognizer.trIntent()
            )
        }
    }

    fun close() {
        recognizer?.destroy()
        recognizer = null
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    private fun errorText(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "Mikrofon sesi okunamadı."
        SpeechRecognizer.ERROR_CLIENT -> "Sesli dinleme iptal edildi."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mikrofon izni verilmedi."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Konuşma servisine ağ üzerinden ulaşılamadı."
        SpeechRecognizer.ERROR_NO_MATCH -> "Söyleneni anlayamadım. Tekrar dener misiniz?"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Konuşma motoru şu an meşgul."
        SpeechRecognizer.ERROR_SERVER -> "Konuşma tanıma servisinde geçici hata oluştu."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Konuşma algılanmadı."
        else -> "Sesli komut işlenemedi."
    }

    private object IntentRecognizer {
        fun trIntent() = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "tr-TR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "JARVIS dinliyor")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }
}
