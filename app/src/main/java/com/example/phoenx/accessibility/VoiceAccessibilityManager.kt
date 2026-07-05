package com.example.phoenx.accessibility

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceAccessibilityManager @Inject constructor(
    @ApplicationContext private val context: Context
) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isListening = false
    private var onCommandRecognized: ((String) -> Unit)? = null

    init {
        // Initialiser le TextToSpeech (Lecture à voix haute)
        tts = TextToSpeech(context) { status ->
            if (status != TextToSpeech.ERROR) {
                tts?.language = Locale.FRENCH
            }
        }
        
        // Initialiser le SpeechRecognizer (Reconnaissance vocale)
        Handler(Looper.getMainLooper()).post {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer?.setRecognitionListener(this)
            }
        }
    }

    /**
     * Lit un texte à haute voix (pour l'aide à la navigation)
     */
    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    /**
     * Active l'écoute des commandes vocales
     */
    fun startListening(onCommand: (String) -> Unit) {
        this.onCommandRecognized = onCommand
        if (isListening) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            speak("La reconnaissance vocale n'est pas disponible sur cet appareil.")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        Handler(Looper.getMainLooper()).post {
            try {
                speechRecognizer?.startListening(intent)
                isListening = true
            } catch (e: Exception) {
                android.util.Log.e("VoiceManager", "Error starting listening", e)
                isListening = false
            }
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
    }

    // --- Implémentation RecognitionListener ---

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        matches?.firstOrNull()?.let { command ->
            onCommandRecognized?.invoke(command.lowercase())
        }
        isListening = false
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() { isListening = false }
    override fun onError(error: Int) { isListening = false }
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
}
