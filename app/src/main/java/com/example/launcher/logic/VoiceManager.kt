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
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()
    
    private val _spokenText = MutableStateFlow("")
    val spokenText: StateFlow<String> = _spokenText.asStateFlow()
    
    // Callback for ViewModel to handle end of speech
    var onSpeechResult: ((String) -> Unit)? = null
    
    init {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("VoiceManager", "Ready for speech")
            }
            override fun onBeginningOfSpeech() {
                Log.d("VoiceManager", "Speech started")
                _isListening.value = true
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                Log.d("VoiceManager", "Speech ended")
                _isListening.value = false
            }
            override fun onError(error: Int) {
                Log.e("VoiceManager", "Speech error: $error")
                _isListening.value = false
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    _spokenText.value = text
                    onSpeechResult?.invoke(text)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }
    
    fun startListening() {
        if (_isListening.value) return
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        
        try {
            speechRecognizer.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            Log.e("VoiceManager", "Start listening failed", e)
            _isListening.value = false
        }
    }
    
    fun stopListening() {
        speechRecognizer.stopListening()
        _isListening.value = false
    }
    
    fun destroy() {
        speechRecognizer.destroy()
    }
}
