package com.example.launcher.logic

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceManager(context: Context) {
    
    private val speechRecognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    
    // Track intent separately from actual state
    private var shouldBeListening = false
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()
    
    private val _spokenText = MutableStateFlow("")
    val spokenText: StateFlow<String> = _spokenText.asStateFlow()
    
    // Callback for ViewModel to handle end of speech
    var onSpeechResult: ((String) -> Unit)? = null
    
    // Handler for restarting
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    
    init {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            
            override fun onBeginningOfSpeech() {
                _isListening.value = true
            }
            
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            
            override fun onEndOfSpeech() {
                _isListening.value = false
                restartListeningIfNeeded()
            }
            
            override fun onError(error: Int) {
                _isListening.value = false
                Log.e("VoiceManager", "Speech error: $error")
                restartListeningIfNeeded()
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    _spokenText.value = text
                    onSpeechResult?.invoke(text)
                }
                // Restart happens in onEndOfSpeech usually, but just in case
                restartListeningIfNeeded()
            }
            
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }
    
    private fun restartListeningIfNeeded() {
        if (shouldBeListening) {
            // Tiny delay to prevent tight loop crash
            handler.postDelayed({
                startListeningInternal()
            }, 100)
        }
    }
    
    fun startListening() {
        shouldBeListening = true
        startListeningInternal()
    }
    
    private fun startListeningInternal() {
        if (_isListening.value) return
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        
        try {
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            Log.e("VoiceManager", "Start listening failed", e)
            _isListening.value = false
            restartListeningIfNeeded()
        }
    }
    
    fun stopListening() {
        shouldBeListening = false
        speechRecognizer.stopListening()
        _isListening.value = false
    }
    
    fun destroy() {
        shouldBeListening = false
        speechRecognizer.destroy()
    }
}
