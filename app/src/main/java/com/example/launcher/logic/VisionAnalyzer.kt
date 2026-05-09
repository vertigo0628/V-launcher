package com.example.launcher.logic

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Offline Vision Analyzer using Google ML Kit.
 * Runs Image Labeling + Text Recognition in parallel for maximum detail.
 * Both models are bundled on-device — no internet required (~70MB RAM total).
 */
class VisionAnalyzer(private val context: Context) {

    companion object {
        private const val TAG = "VisionAnalyzer"
        // Lower confidence threshold to catch more objects
        private const val LABEL_CONFIDENCE_THRESHOLD = 0.4f
        private const val MAX_LABELS = 15
    }

    data class VisionResult(
        val labels: List<LabelResult>,
        val extractedText: String?,
        val textBlocks: List<String>
    )

    data class LabelResult(
        val label: String,
        val confidence: Float
    )

    /**
     * Analyze a bitmap using ML Kit Image Labeling + Text Recognition.
     * Runs both analyses in parallel for speed.
     * Supports printed AND handwritten text recognition.
     */
    suspend fun analyze(bitmap: Bitmap): VisionResult = coroutineScope {
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        // Run both ML Kit engines in parallel
        val labelsDeferred = async { runImageLabeling(inputImage) }
        val textDeferred = async { runTextRecognition(inputImage) }

        val labels = labelsDeferred.await()
        val textResult = textDeferred.await()

        VisionResult(
            labels = labels,
            extractedText = textResult.first,
            textBlocks = textResult.second
        )
    }

    /**
     * Analyze from a content URI (e.g., from camera capture saved to file).
     */
    suspend fun analyze(uri: Uri): VisionResult = withContext(Dispatchers.IO) {
        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        analyze(bitmap)
    }

    private suspend fun runImageLabeling(image: InputImage): List<LabelResult> {
        return try {
            val options = ImageLabelerOptions.Builder()
                .setConfidenceThreshold(LABEL_CONFIDENCE_THRESHOLD)
                .build()
            val labeler = ImageLabeling.getClient(options)
            
            val labels = labeler.process(image).await()
            labeler.close()

            labels.take(MAX_LABELS).map { label ->
                LabelResult(
                    label = label.text,
                    confidence = label.confidence
                )
            }.also { resultLabels ->
                Log.d(TAG, "Image Labeling found ${resultLabels.size} labels: ${resultLabels.joinToString { l -> "${l.label} (${(l.confidence * 100).toInt()}%)" }}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Image Labeling failed", e)
            emptyList()
        }
    }

    private suspend fun runTextRecognition(image: InputImage): Pair<String?, List<String>> {
        return try {
            // Default TextRecognizerOptions handles both printed AND handwritten Latin text
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            
            val result = recognizer.process(image).await()
            recognizer.close()

            val fullText = result.text.takeIf { it.isNotBlank() }
            val blocks = result.textBlocks.map { block ->
                block.text
            }

            Log.d(TAG, "Text Recognition found: ${fullText?.take(100) ?: "no text"}")
            
            Pair(fullText, blocks)
        } catch (e: Exception) {
            Log.e(TAG, "Text Recognition failed", e)
            Pair(null, emptyList())
        }
    }

    /**
     * Build a system prompt + user prompt pair for the AI model from the vision analysis results.
     * Returns Pair(systemPrompt, userPrompt) for proper Ollama API usage.
     */
    fun buildPromptFromResult(result: VisionResult): Pair<String, String> {
        val systemPrompt = "You are an image analysis assistant. The user has taken a photo with their phone camera. A local computer vision engine has already scanned the photo and extracted all detected objects and text from it. You are receiving this pre-extracted data below. You MUST analyze this data and provide your response. NEVER ask the user to send, share, or upload the photo — you already have all the information from it."
        
        val sb = StringBuilder()
        
        // Object Labels
        if (result.labels.isNotEmpty()) {
            sb.appendLine("Objects detected in the photo:")
            result.labels.forEach { label ->
                val pct = (label.confidence * 100).toInt()
                if (pct >= 50) {
                    sb.appendLine("- ${label.label} (${pct}% confidence)")
                }
            }
        } else {
            sb.appendLine("No specific objects were detected in the photo.")
        }
        sb.appendLine()
        
        // Text Content
        if (!result.extractedText.isNullOrBlank()) {
            sb.appendLine("Text found in the photo:")
            sb.appendLine(result.extractedText.trim())
            sb.appendLine()
        }
        
        sb.appendLine("CRITICAL INSTRUCTIONS:")
        sb.appendLine("1. Describe the photo STRICTLY using ONLY the detected objects and text listed above.")
        sb.appendLine("2. If no objects or text are listed, simply say: 'I couldn't detect anything clearly in that photo.'")
        sb.appendLine("3. DO NOT invent, guess, or hallucinate details that are not in the data above.")
        sb.appendLine("4. If the text contains a question, answer it concisely.")
        
        return Pair(systemPrompt, sb.toString())
    }
}
