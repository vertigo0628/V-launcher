package com.vertigo.launcher.service

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.vertigo.launcher.R
import com.vertigo.launcher.logic.OllamaClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import java.util.Locale
import kotlin.math.abs

/**
 * A floating "Dynamic Island" style mini-terminal overlay.
 * - Collapsed state: Small draggable pill with mic icon
 * - Expanded state: Full mini terminal with chat, voice, and text input
 */
class FloatingTerminalOverlay(
    private val context: Context,
    private val windowManager: WindowManager
) {
    companion object {
        private const val TAG = "FloatingTerminal"

        // Colors (cyberpunk theme matching launcher)
        private const val COLOR_BG = 0xF00F172A.toInt()           // Dark blue background
        private const val COLOR_ACCENT = 0xFF00F0FF.toInt()       // Neon Cyan
        private const val COLOR_ACCENT_DIM = 0x6600F0FF.toInt()   // Dim Cyan border
        private const val COLOR_GREEN = 0xFF4ADE80.toInt()        // Terminal Green
        private const val COLOR_RED = 0xFFEF4444.toInt()          // Red
        private const val COLOR_TEXT = 0xFFFFFFFF.toInt()          // White
        private const val COLOR_TEXT_DIM = 0x99FFFFFF.toInt()      // Dim White
        private const val COLOR_PILL_START = 0xFF00D4FF.toInt()    // Pill gradient start
        private const val COLOR_PILL_END = 0xFF0066FF.toInt()      // Pill gradient end
    }

    // State
    private var isExpanded = false
    private var isListening = false
    private var isAiThinking = false

    // Views
    private var pillView: View? = null
    private var expandedView: View? = null
    private var pillParams: WindowManager.LayoutParams? = null
    private var expandedParams: WindowManager.LayoutParams? = null

    // Chat
    private val chatMessages = mutableListOf<Pair<String, String>>() // role, content
    private var currentStreamingText = ""
    private var chatScrollView: ScrollView? = null
    private var chatContainer: LinearLayout? = null
    private var statusText: TextView? = null
    private var inputField: EditText? = null
    private var sendButton: ImageView? = null
    private var micButton: ImageView? = null
    private var stopButton: ImageView? = null

    // AI Client
    private val ollamaClient = OllamaClient()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentStreamJob: Job? = null

    // Voice
    private var speechRecognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())

    // Drag state for pill
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    // ===================== Public API =====================

    fun show() {
        createPillView()
    }

    fun hide() {
        removePill()
        removeExpanded()
        destroyVoice()
        scope.cancel()
    }

    fun isVisible(): Boolean = pillView != null || expandedView != null

    // ===================== Pill (Collapsed State) =====================

    private fun createPillView() {
        if (pillView != null) return

        val sizePx = dpToPx(52)
        val iconSizePx = dpToPx(22)

        val container = FrameLayout(context)

        // Gradient circle background
        val bg = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(COLOR_PILL_START, COLOR_PILL_END)
        )
        bg.shape = GradientDrawable.OVAL
        container.background = bg

        // Mic icon
        val micIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_mic)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setColorFilter(Color.WHITE)
        }
        val iconParams = FrameLayout.LayoutParams(iconSizePx, iconSizePx).apply {
            gravity = Gravity.CENTER
        }
        container.addView(micIcon, iconParams)

        // Window params
        val prefs = com.vertigo.launcher.utils.StorageHelper
            .getSafeDefaultSharedPreferences(context)
        val displayMetrics = context.resources.displayMetrics

        val params = WindowManager.LayoutParams(
            sizePx, sizePx,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.getInt("fab_pos_x", displayMetrics.widthPixels - sizePx - dpToPx(12))
            y = prefs.getInt("fab_pos_y", displayMetrics.heightPixels / 3)
        }

        setupPillTouch(container, params)

        try {
            windowManager.addView(container, params)
            pillView = container
            pillParams = params

            // Entrance animation
            container.alpha = 0f
            container.scaleX = 0.5f
            container.scaleY = 0.5f
            container.animate()
                .alpha(0.85f).scaleX(1f).scaleY(1f)
                .setDuration(350)
                .setInterpolator(OvershootInterpolator(1.5f))
                .start()

            // Fade to idle after 4s
            scheduleIdleFade(container)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add pill view", e)
        }
    }

    private fun setupPillTouch(view: View, params: WindowManager.LayoutParams) {
        var isDragging = false
        val tapThreshold = dpToPx(8)

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    cancelIdleFade()
                    v.animate().alpha(0.95f).setDuration(100).start()
                    v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).start()

                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > tapThreshold || abs(dy) > tapThreshold) isDragging = true
                    if (isDragging) {
                        params.x = initialX + dx
                        params.y = initialY + dy
                        try { windowManager.updateViewLayout(v, params) } catch (_: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                    if (!isDragging) {
                        // TAP → Expand
                        expandTerminal()
                    } else {
                        saveButtonPosition(params.x, params.y)
                        snapToEdge(v, params)
                    }
                    scheduleIdleFade(v)
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    scheduleIdleFade(v)
                    true
                }
                else -> false
            }
        }
    }

    // ===================== Expanded State (Mini Terminal) =====================

    private fun expandTerminal() {
        if (isExpanded) return
        isExpanded = true

        // Hide pill
        pillView?.animate()?.alpha(0f)?.scaleX(0f)?.scaleY(0f)?.setDuration(150)
            ?.withEndAction { pillView?.visibility = View.GONE }?.start()

        createExpandedView()
    }

    private fun collapseTerminal() {
        if (!isExpanded) return
        isExpanded = false

        // Stop voice if active
        stopListening()

        // Cancel any streaming
        currentStreamJob?.cancel()
        currentStreamJob = null
        isAiThinking = false

        // Animate out expanded
        expandedView?.animate()?.alpha(0f)?.scaleY(0.5f)?.setDuration(200)
            ?.withEndAction { removeExpanded() }?.start()

        // Show pill
        pillView?.visibility = View.VISIBLE
        pillView?.animate()?.alpha(0.85f)?.scaleX(1f)?.scaleY(1f)?.setDuration(250)
            ?.setInterpolator(OvershootInterpolator(1.2f))?.start()

        scheduleIdleFade(pillView ?: return)
    }

    private fun createExpandedView() {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val width = (screenWidth * 0.92f).toInt()
        val maxHeight = (displayMetrics.heightPixels * 0.55f).toInt()

        // Main container
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))

            val bg = GradientDrawable()
            bg.setColor(COLOR_BG)
            bg.cornerRadius = dpToPx(24).toFloat()
            bg.setStroke(dpToPx(1), COLOR_ACCENT_DIM)
            background = bg

            // Elevation/shadow
            elevation = dpToPx(8).toFloat()
        }

        // ─── Header Row ───
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Title
        val titleText = TextView(context).apply {
            text = "⚡ V-TERMINAL"
            setTextColor(COLOR_ACCENT)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerRow.addView(titleText)

        // Voice status
        statusText = TextView(context).apply {
            text = "Ready"
            setTextColor(COLOR_TEXT_DIM)
            textSize = 10f
            typeface = Typeface.MONOSPACE
            setPadding(dpToPx(8), 0, dpToPx(8), 0)
        }
        headerRow.addView(statusText)

        // Close button
        val closeBtn = TextView(context).apply {
            text = "✕"
            setTextColor(COLOR_TEXT_DIM)
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(dpToPx(8), dpToPx(4), dpToPx(4), dpToPx(4))
            setOnClickListener { collapseTerminal() }
        }
        headerRow.addView(closeBtn)

        root.addView(headerRow, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        // ─── Divider ───
        val divider = View(context).apply {
            val bg = GradientDrawable()
            bg.setColor(COLOR_ACCENT_DIM)
            background = bg
        }
        val dividerParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1)
        ).apply { setMargins(0, dpToPx(8), 0, dpToPx(8)) }
        root.addView(divider, dividerParams)

        // ─── Chat Scroll Area ───
        chatScrollView = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        chatContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        chatScrollView!!.addView(chatContainer, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        // Rebuild existing chat messages
        rebuildChatUI()

        // If no messages, show hint
        if (chatMessages.isEmpty() && currentStreamingText.isEmpty()) {
            addSystemMessage("Tap 🎤 or type to talk to Sunday")
        }

        val chatParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ).apply { setMargins(0, 0, 0, dpToPx(8)) }
        root.addView(chatScrollView, chatParams)

        // ─── Input Row ───
        val inputRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            val bg = GradientDrawable()
            bg.setColor(0x1AFFFFFF)
            bg.cornerRadius = dpToPx(20).toFloat()
            background = bg
            setPadding(dpToPx(12), dpToPx(4), dpToPx(4), dpToPx(4))
        }

        // Text input
        inputField = EditText(context).apply {
            hint = "Ask Sunday..."
            setHintTextColor(0x55FFFFFF)
            setTextColor(COLOR_TEXT)
            textSize = 13f
            typeface = Typeface.MONOSPACE
            setBackgroundColor(Color.TRANSPARENT)
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_SEND
            setPadding(0, dpToPx(8), 0, dpToPx(8))

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendCurrentInput()
                    true
                } else false
            }
        }
        inputRow.addView(inputField, LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ))

        // Send button
        sendButton = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_send)
            setColorFilter(COLOR_ACCENT)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            setOnClickListener { sendCurrentInput() }
            visibility = View.GONE
        }
        inputRow.addView(sendButton, LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)))

        // Stop button (visible during streaming)
        stopButton = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(COLOR_RED)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            setOnClickListener { stopAi() }
            visibility = View.GONE
        }
        inputRow.addView(stopButton, LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)))

        // Mic button
        micButton = ImageView(context).apply {
            setImageResource(R.drawable.ic_mic)
            setColorFilter(COLOR_ACCENT)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            setOnClickListener { toggleVoice() }
        }
        inputRow.addView(micButton, LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)))

        root.addView(inputRow, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        // Show/hide send button based on input
        inputField?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val hasText = !s.isNullOrBlank()
                sendButton?.visibility = if (hasText && !isAiThinking) View.VISIBLE else View.GONE
            }
        })

        // Window params — focusable so keyboard works
        val params = WindowManager.LayoutParams(
            width, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dpToPx(48)
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        // Constrain max height
        root.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST)
        )

        try {
            windowManager.addView(root, params)
            expandedView = root
            expandedParams = params

            // Entrance animation (Dynamic Island expand)
            root.alpha = 0f
            root.scaleX = 0.3f
            root.scaleY = 0.1f
            root.pivotY = 0f
            root.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(300)
                .setInterpolator(OvershootInterpolator(0.8f))
                .start()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add expanded view", e)
        }
    }

    // ===================== Chat Logic =====================

    private fun sendCurrentInput() {
        val text = inputField?.text?.toString()?.trim() ?: return
        if (text.isBlank() || isAiThinking) return

        inputField?.setText("")
        sendQuery(text)
    }

    private fun sendQuery(query: String) {
        if (executeLocalCommand(query)) {
            return
        }

        // Add user message
        chatMessages.add("user" to query)
        addUserMessageUI(query)

        isAiThinking = true
        currentStreamingText = ""
        updateButtonStates()
        statusText?.text = "Thinking..."
        statusText?.setTextColor(COLOR_ACCENT)

        // Stop voice while AI responds
        stopListening()

        // Get settings
        val prefs = com.vertigo.launcher.utils.StorageHelper
            .getSafeDefaultSharedPreferences(context)
        val baseUrl = prefs.getString("ollama_base_url", "http://127.0.0.1:11434") ?: "http://127.0.0.1:11434"
        val model = prefs.getString("ollama_model_select", "llama3.2:1b") ?: "llama3.2:1b"

        val systemPrompt = "You are Sunday, an advanced AI assistant integrated into the V-Launcher for Android. " +
            "Keep responses concise and helpful. You are speaking through a floating overlay terminal. " +
            "Be direct and efficient."

        // Create streaming message view
        val streamView = createAssistantMessageView("")
        chatContainer?.addView(streamView)
        scrollToBottom()

        currentStreamJob = scope.launch {
            try {
                ollamaClient.generateResponseStream(query, model, baseUrl, systemPrompt)
                    .catch { e ->
                        withContext(Dispatchers.Main) {
                            val errMsg = when {
                                e.message?.contains("Connection refused") == true -> "⚠ Cannot reach AI server at $baseUrl"
                                e.message?.contains("timeout") == true -> "⚠ Connection timed out"
                                else -> "⚠ Error: ${e.message?.take(80)}"
                            }
                            updateStreamView(streamView, errMsg)
                            chatMessages.add("assistant" to errMsg)
                        }
                    }
                    .collect { chunk ->
                        currentStreamingText += chunk
                        withContext(Dispatchers.Main) {
                            updateStreamView(streamView, currentStreamingText)
                            scrollToBottom()
                        }
                    }

                // Stream complete
                withContext(Dispatchers.Main) {
                    if (currentStreamingText.isNotBlank()) {
                        chatMessages.add("assistant" to currentStreamingText)
                    }
                    finishStreaming()
                }
            } catch (e: CancellationException) {
                withContext(Dispatchers.Main) {
                    if (currentStreamingText.isNotBlank()) {
                        chatMessages.add("assistant" to currentStreamingText + " [stopped]")
                    }
                    finishStreaming()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateStreamView(streamView, "⚠ ${e.message?.take(100)}")
                    chatMessages.add("assistant" to "⚠ ${e.message?.take(100)}")
                    finishStreaming()
                }
            }
        }
    }

    private fun stopAi() {
        currentStreamJob?.cancel()
        currentStreamJob = null
        finishStreaming()
    }

    private fun finishStreaming() {
        isAiThinking = false
        currentStreamingText = ""
        updateButtonStates()
        statusText?.text = "Ready"
        statusText?.setTextColor(COLOR_TEXT_DIM)
    }

    private fun updateButtonStates() {
        val hasText = !inputField?.text.isNullOrBlank()
        sendButton?.visibility = if (hasText && !isAiThinking) View.VISIBLE else View.GONE
        stopButton?.visibility = if (isAiThinking) View.VISIBLE else View.GONE
        micButton?.visibility = if (!isAiThinking) View.VISIBLE else View.GONE
    }

    // ===================== Chat UI Builders =====================

    private fun addUserMessageUI(text: String) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dpToPx(4), 0, dpToPx(4))
        }

        val prompt = TextView(context).apply {
            this.text = "v@terminal:~$ "
            setTextColor(COLOR_GREEN)
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setTypeface(typeface, Typeface.BOLD)
        }
        row.addView(prompt)

        val content = TextView(context).apply {
            this.text = text
            setTextColor(COLOR_TEXT)
            textSize = 12f
            typeface = Typeface.MONOSPACE
        }
        row.addView(content)

        chatContainer?.addView(row)
        scrollToBottom()
    }

    private fun createAssistantMessageView(text: String): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(COLOR_TEXT)
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setLineSpacing(dpToPx(2).toFloat(), 1f)
            setPadding(0, dpToPx(2), 0, dpToPx(6))
        }
    }

    private fun updateStreamView(view: TextView, text: String) {
        // Strip <think> tags for display, dim them
        view.text = text.replace(Regex("</?think>"), "")
    }

    private fun addSystemMessage(text: String) {
        val tv = TextView(context).apply {
            this.text = text
            setTextColor(COLOR_ACCENT_DIM)
            textSize = 11f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(16), 0, dpToPx(16))
        }
        chatContainer?.addView(tv)
    }

    private fun rebuildChatUI() {
        chatContainer?.removeAllViews()
        for ((role, content) in chatMessages) {
            if (role == "user") {
                addUserMessageUI(content)
            } else {
                val tv = createAssistantMessageView(content)
                chatContainer?.addView(tv)
            }
        }
    }

    private fun scrollToBottom() {
        chatScrollView?.post {
            chatScrollView?.fullScroll(View.FOCUS_DOWN)
        }
    }

    // ===================== Voice =====================

    private fun toggleVoice() {
        if (isListening) {
            stopListening()
        } else {
            startListening()
        }
    }

    private fun startListening() {
        if (isAiThinking) return

        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer?.setRecognitionListener(createRecognitionListener())
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            speechRecognizer?.startListening(intent)
            isListening = true
            micButton?.setColorFilter(COLOR_GREEN)
            statusText?.text = "Listening..."
            statusText?.setTextColor(COLOR_GREEN)

            // Pulse animation on mic
            micButton?.animate()?.scaleX(1.2f)?.scaleY(1.2f)?.setDuration(300)
                ?.setInterpolator(OvershootInterpolator())?.start()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start voice", e)
            statusText?.text = "Mic error"
            statusText?.setTextColor(COLOR_RED)
        }
    }

    private fun stopListening() {
        isListening = false
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        micButton?.setColorFilter(COLOR_ACCENT)
        micButton?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(150)?.start()
        if (!isAiThinking) {
            statusText?.text = "Ready"
            statusText?.setTextColor(COLOR_TEXT_DIM)
        }
    }

    private fun destroyVoice() {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        private var partialText = ""

        override fun onReadyForSpeech(params: Bundle?) {
            statusText?.text = "Listening..."
            statusText?.setTextColor(COLOR_GREEN)
        }

        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            isListening = false
            micButton?.setColorFilter(COLOR_ACCENT)
            micButton?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(150)?.start()
        }

        override fun onError(error: Int) {
            isListening = false
            micButton?.setColorFilter(COLOR_ACCENT)
            micButton?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(150)?.start()

            // If we have partial text and it was a NO_MATCH, use the partial
            if (error == SpeechRecognizer.ERROR_NO_MATCH && partialText.isNotBlank()) {
                sendQuery(partialText)
                partialText = ""
                return
            }

            if (!isAiThinking) {
                statusText?.text = "Ready"
                statusText?.setTextColor(COLOR_TEXT_DIM)
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val text = matches[0]
                if (text.isNotBlank()) {
                    sendQuery(text)
                }
            }
            partialText = ""
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                partialText = matches[0]
                statusText?.text = "Hearing: ${partialText.take(30)}..."
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    // ===================== Idle Fade =====================

    private var idleFadeRunnable: Runnable? = null

    private fun scheduleIdleFade(view: View) {
        cancelIdleFade()
        idleFadeRunnable = Runnable {
            view.animate().alpha(0.35f).setDuration(800).start()
        }
        handler.postDelayed(idleFadeRunnable!!, 5000)
    }

    private fun cancelIdleFade() {
        idleFadeRunnable?.let { handler.removeCallbacks(it) }
    }

    // ===================== Edge Snapping =====================

    private fun snapToEdge(view: View, params: WindowManager.LayoutParams) {
        val screenWidth = context.resources.displayMetrics.widthPixels
        val buttonCenter = params.x + view.width / 2
        val margin = dpToPx(6)

        val targetX = if (buttonCenter < screenWidth / 2) margin
        else screenWidth - view.width - margin

        val animator = ValueAnimator.ofInt(params.x, targetX)
        animator.duration = 250
        animator.interpolator = OvershootInterpolator(0.8f)
        animator.addUpdateListener { anim ->
            params.x = anim.animatedValue as Int
            try { windowManager.updateViewLayout(view, params) } catch (_: Exception) {}
        }
        animator.start()

        handler.postDelayed({ saveButtonPosition(targetX, params.y) }, 300)
    }

    private fun saveButtonPosition(x: Int, y: Int) {
        try {
            val prefs = com.vertigo.launcher.utils.StorageHelper
                .getSafeDefaultSharedPreferences(context)
            prefs.edit().putInt("fab_pos_x", x).putInt("fab_pos_y", y).apply()
        } catch (_: Exception) {}
    }

    // ===================== Cleanup =====================

    private fun removePill() {
        try { pillView?.let { windowManager.removeView(it) } } catch (_: Exception) {}
        pillView = null
        pillParams = null
        cancelIdleFade()
    }

    private fun removeExpanded() {
        try { expandedView?.let { windowManager.removeView(it) } } catch (_: Exception) {}
        expandedView = null
        expandedParams = null
    }

    // ===================== Local Command Execution =====================

    private fun executeLocalCommand(query: String): Boolean {
        val cleaned = query.trim().lowercase()
        if (cleaned.isBlank()) return false

        // 1. App Drawer Navigation / Home / Back
        val isHomeCommand = cleaned == "go home" || cleaned == "close drawer" || cleaned == "hide apps" || cleaned == "hide drawer"
        val isBackCommand = cleaned == "go back" || cleaned == "back"
        val isRecentsCommand = cleaned == "recents" || cleaned == "recent apps" || cleaned == "show recents"
        
        if (isHomeCommand || isBackCommand || isRecentsCommand) {
            chatMessages.add("user" to query)
            addUserMessageUI(query)
            if (isHomeCommand) {
                VLauncherAccessibilityService.performHome()
                addAssistantMessageDirect("Returning to Home screen.")
            } else if (isBackCommand) {
                VLauncherAccessibilityService.performBack()
                addAssistantMessageDirect("Going back.")
            } else {
                VLauncherAccessibilityService.performRecents()
                addAssistantMessageDirect("Opening recent apps.")
            }
            return true
        }

        // 2. Play Music
        val isMusicCommand = cleaned.contains("music") || cleaned.contains("song") || 
                             cleaned.contains("songs") || cleaned.contains("tunes") || 
                             cleaned.contains("audio")
        val hasMusicActionWord = cleaned.contains("play") || cleaned.contains("start") || 
                                 cleaned.contains("resume") || cleaned.contains("listen")
        if (isMusicCommand && hasMusicActionWord) {
            chatMessages.add("user" to query)
            addUserMessageUI(query)
            try {
                val intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                addAssistantMessageDirect("Opening default music player...")
            } catch (e: Exception) {
                addAssistantMessageDirect("Failed to open music player: ${e.message}")
            }
            return true
        }

        // 3. Volume Control
        val isVolumeUp = cleaned.contains("volume up") || cleaned.contains("increase volume") || 
                         cleaned.contains("louder") || cleaned.contains("make it louder")
        val isVolumeDown = cleaned.contains("volume down") || cleaned.contains("decrease volume") || 
                           cleaned.contains("softer") || cleaned.contains("quieter") || 
                           cleaned.contains("lower volume")
        val isMute = cleaned.contains("mute") || cleaned.contains("silence") || 
                     cleaned.contains("shutup") || cleaned.contains("quiet")
        val isUnmute = cleaned.contains("unmute") || cleaned.contains("turn volume on")

        if (isVolumeUp || isVolumeDown || isMute || isUnmute) {
            chatMessages.add("user" to query)
            addUserMessageUI(query)
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                when {
                    isVolumeUp -> {
                        audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_RAISE, android.media.AudioManager.FLAG_SHOW_UI)
                        addAssistantMessageDirect("Raising system volume.")
                    }
                    isVolumeDown -> {
                        audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_LOWER, android.media.AudioManager.FLAG_SHOW_UI)
                        addAssistantMessageDirect("Lowering system volume.")
                    }
                    isMute -> {
                        audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_MUTE, android.media.AudioManager.FLAG_SHOW_UI)
                        addAssistantMessageDirect("Muting audio.")
                    }
                    isUnmute -> {
                        audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_UNMUTE, android.media.AudioManager.FLAG_SHOW_UI)
                        addAssistantMessageDirect("Unmuting audio.")
                    }
                }
            } catch (e: Exception) {
                addAssistantMessageDirect("Error adjusting volume: ${e.message}")
            }
            return true
        }

        // 4. Settings Shortcuts
        val isSettingsCommand = cleaned.contains("settings") || cleaned.contains("configure") || 
                                cleaned.contains("preferences")
        if (isSettingsCommand) {
            chatMessages.add("user" to query)
            addUserMessageUI(query)
            try {
                if (cleaned.contains("launcher")) {
                    val intent = Intent(context, com.vertigo.launcher.LauncherSettingsActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    addAssistantMessageDirect("Opening Launcher settings...")
                } else if (cleaned.contains("wifi") || cleaned.contains("wi-fi")) {
                    context.startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                    addAssistantMessageDirect("Opening Wi-Fi settings...")
                } else if (cleaned.contains("bluetooth")) {
                    context.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                    addAssistantMessageDirect("Opening Bluetooth settings...")
                } else {
                    context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                    addAssistantMessageDirect("Opening system settings...")
                }
            } catch (e: Exception) {
                addAssistantMessageDirect("Failed to open settings: ${e.message}")
            }
            return true
        }

        // 5. Open/Launch App
        val openAppVerbs = listOf("open up", "open", "launch", "start", "run", "go to")
        var appToOpen: String? = null
        for (verb in openAppVerbs) {
            if (cleaned.startsWith(verb + " ")) {
                appToOpen = cleaned.removePrefix(verb).trim()
                break
            }
        }

        if (appToOpen != null && appToOpen.isNotEmpty()) {
            chatMessages.add("user" to query)
            addUserMessageUI(query)
            val pkg = findInstalledAppPackage(appToOpen)
            if (pkg != null) {
                val appName = getAppName(pkg)
                addAssistantMessageDirect("Launching $appName...")
                launchAppPackage(pkg)
            } else {
                addAssistantMessageDirect("Could not find an app named '$appToOpen'.")
            }
            return true
        }

        // 6. Search Web
        val searchVerbs = listOf("search for", "search", "google", "look up", "find", "web search")
        var searchQuery: String? = null
        for (verb in searchVerbs) {
            if (cleaned.startsWith(verb + " ")) {
                searchQuery = cleaned.removePrefix(verb).trim()
                break
            }
        }

        if (searchQuery != null && searchQuery.isNotEmpty()) {
            chatMessages.add("user" to query)
            addUserMessageUI(query)
            try {
                val url = "https://www.google.com/search?q=" + java.net.URLEncoder.encode(searchQuery, "UTF-8")
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                addAssistantMessageDirect("Searching the web for '$searchQuery'...")
            } catch (e: Exception) {
                addAssistantMessageDirect("Failed to start web search: ${e.message}")
            }
            return true
        }

        // 7. Direct App Match
        val pkg = findInstalledAppPackage(cleaned)
        if (pkg != null) {
            chatMessages.add("user" to query)
            addUserMessageUI(query)
            val appName = getAppName(pkg)
            addAssistantMessageDirect("Launching $appName...")
            launchAppPackage(pkg)
            return true
        }

        return false
    }

    private fun addAssistantMessageDirect(text: String) {
        chatMessages.add("assistant" to text)
        val tv = createAssistantMessageView(text)
        chatContainer?.addView(tv)
        scrollToBottom()
    }

    private fun findInstalledAppPackage(name: String): String? {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val list = pm.queryIntentActivities(intent, 0)
        
        // Try exact match
        for (info in list) {
            val label = info.loadLabel(pm).toString().lowercase().trim()
            if (label == name) {
                return info.activityInfo.packageName
            }
        }
        
        // Try loose match
        for (info in list) {
            val label = info.loadLabel(pm).toString().lowercase().replace(" ", "").trim()
            val cleanName = name.replace(" ", "")
            if (label == cleanName || label.contains(cleanName) || cleanName.contains(label)) {
                return info.activityInfo.packageName
            }
        }
        return null
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun launchAppPackage(packageName: String) {
        try {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package $packageName", e)
        }
    }

    // ===================== Utilities =====================

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
