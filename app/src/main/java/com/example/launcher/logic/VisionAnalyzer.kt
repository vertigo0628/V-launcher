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
     * Build a rich prompt for the AI model from the vision analysis results.
     * This constructs the bridge between ML Kit output and Ollama input.
     */
    fun buildPromptFromResult(result: VisionResult): String {
        val sb = StringBuilder()
        sb.appendLine("SYSTEM: You are an AI assistant. The user has just uploaded a photo. You CANNOT see the photo directly. Instead, a local vision engine has ALREADY extracted the data from the photo for you. DO NOT ask the user to send the photo — use the data provided below.")
        sb.appendLine()
        sb.appendLine("--- IMAGE EXTRACTION DATA ---")
        sb.appendLine()
        
        // Object Labels
        if (result.labels.isNotEmpty()) {
            sb.appendLine("Objects Detected in Image:")
            result.labels.forEach { label ->
                val pct = (label.confidence * 100).toInt()
                if (pct >= 60) {
                    sb.appendLine("- ${label.label} (${pct}% match)")
                }
            }
            sb.appendLine()
        } else {
            sb.appendLine("Objects Detected in Image: None specific.")
            sb.appendLine()
        }
        
        // Text Content
        if (!result.extractedText.isNullOrBlank()) {
            sb.appendLine("Text Written/Printed in Image:")
            sb.appendLine(result.extractedText.trim())
            sb.appendLine()
        }
        
        // Instructions for the LLM
        sb.appendLine("--- END OF IMAGE DATA ---")
        sb.appendLine()
        sb.appendLine("USER TASK:")
        sb.appendLine("1. Describe the image layout based on the objects and text above.")
        sb.appendLine("2. If there is text, transcribe it accurately.")
        sb.appendLine("3. If the text contains a question, math problem, or prompt, please answer it as an AI assistant.")
        
        return sb.toString()
    }
}
