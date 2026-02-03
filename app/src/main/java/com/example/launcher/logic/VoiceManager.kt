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
    
    companion object {
        private const val TAG = "VoiceManager"
        private const val BASE_DELAY_MS = 1000L       // Start with 1 second
        private const val MAX_DELAY_MS = 30000L       // Cap at 30 seconds
        private const val MAX_CONSECUTIVE_ERRORS = 5  // Stop after 5 consecutive errors
    }
    
    private val speechRecognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    
    // Track intent separately from actual state
    private var shouldBeListening = false
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()
    
    private val _spokenText = MutableStateFlow("")
    val spokenText: StateFlow<String> = _spokenText.asStateFlow()
    
    // Callback for ViewModel to handle end of speech
    var onSpeechResult: ((String) -> Unit)? = null
    
    // Error tracking for backoff
    private var consecutiveErrors = 0
    private var currentDelayMs = BASE_DELAY_MS
    
    // Handler for restarting
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var restartRunnable: Runnable? = null
    
    init {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                // Reset error count on successful ready
                consecutiveErrors = 0
                currentDelayMs = BASE_DELAY_MS
            }
            
            override fun onBeginningOfSpeech() {
                _isListening.value = true
            }
            
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            
            override fun onEndOfSpeech() {
                _isListening.value = false
                // Normal end, restart with short delay
                scheduleRestart(500)
            }
            
            override fun onError(error: Int) {
                _isListening.value = false
                val errorName = getErrorName(error)
                Log.w(TAG, "Speech error: $error ($errorName)")
                
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        // Normal "no speech detected" - restart with short delay
                        scheduleRestart(500)
                    }
                    
                    SpeechRecognizer.ERROR_CLIENT,
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                        // Client error or busy - use exponential backoff
                        handleRetryWithBackoff()
                    }
                    
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                        // Network issue - longer delay
                        scheduleRestart(5000)
                    }
                    
                    SpeechRecognizer.ERROR_AUDIO,
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        // Critical error - stop trying
                        Log.e(TAG, "Critical speech error, stopping: $errorName")
                        shouldBeListening = false
                    }
                    
                    else -> {
                        // Unknown error - use backoff
                        handleRetryWithBackoff()
                    }
                }
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    _spokenText.value = text
                    onSpeechResult?.invoke(text)
                }
                // onEndOfSpeech handles restart
            }
            
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }
    
    private fun handleRetryWithBackoff() {
        consecutiveErrors++
        
        if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
            Log.e(TAG, "Too many consecutive errors ($consecutiveErrors), pausing for $MAX_DELAY_MS ms")
            currentDelayMs = MAX_DELAY_MS
            consecutiveErrors = 0 // Reset for next cycle
        } else {
            // Exponential backoff: 1s, 2s, 4s, 8s...
            currentDelayMs = (currentDelayMs * 2).coerceAtMost(MAX_DELAY_MS)
        }
        
        scheduleRestart(currentDelayMs)
    }
    
    private fun scheduleRestart(delayMs: Long) {
        if (!shouldBeListening) return
        
        // Cancel any pending restart
        restartRunnable?.let { handler.removeCallbacks(it) }
        
        restartRunnable = Runnable {
            startListeningInternal()
        }
        handler.postDelayed(restartRunnable!!, delayMs)
    }
    
    private fun getErrorName(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "ERROR_AUDIO"
        SpeechRecognizer.ERROR_CLIENT -> "ERROR_CLIENT"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "ERROR_INSUFFICIENT_PERMISSIONS"
        SpeechRecognizer.ERROR_NETWORK -> "ERROR_NETWORK"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ERROR_NETWORK_TIMEOUT"
        SpeechRecognizer.ERROR_NO_MATCH -> "ERROR_NO_MATCH"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ERROR_RECOGNIZER_BUSY"
        SpeechRecognizer.ERROR_SERVER -> "ERROR_SERVER"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "ERROR_SPEECH_TIMEOUT"
        else -> "UNKNOWN($error)"
    }
    
    fun startListening() {
        shouldBeListening = true
        consecutiveErrors = 0
        currentDelayMs = BASE_DELAY_MS
        _isListening.value = true // Optimistic UI update
        startListeningInternal()
    }
    
    private fun startListeningInternal() {
        if (!shouldBeListening) return
        // if (_isListening.value) return // Removed to allow optimistic update flow
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        
        try {
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Start listening failed", e)
            _isListening.value = false
            handleRetryWithBackoff()
        }
    }
    
    fun stopListening() {
        shouldBeListening = false
        restartRunnable?.let { handler.removeCallbacks(it) }
        restartRunnable = null
        speechRecognizer.stopListening()
        _isListening.value = false
    }
    
    fun destroy() {
        stopListening()
        speechRecognizer.destroy()
    }
}
