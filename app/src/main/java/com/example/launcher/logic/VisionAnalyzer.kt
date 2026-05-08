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
        sb.appendLine("The user took a photo and wants you to analyze it. Here is what was detected:")
        sb.appendLine()
        
        // Object Labels
        if (result.labels.isNotEmpty()) {
            sb.appendLine("## Objects & Scenes Detected:")
            result.labels.forEach { label ->
                val pct = (label.confidence * 100).toInt()
                sb.appendLine("- ${label.label} (${pct}% confidence)")
            }
            sb.appendLine()
        } else {
            sb.appendLine("## Objects: No clear objects were detected.")
            sb.appendLine()
        }
        
        // Text Content
        if (!result.extractedText.isNullOrBlank()) {
            sb.appendLine("## Text Found in Image (printed or handwritten):")
            sb.appendLine("```")
            sb.appendLine(result.extractedText)
            sb.appendLine("```")
            sb.appendLine()
        }
        
        // Instructions for the LLM
        sb.appendLine("## Your Task:")
        sb.appendLine("1. Provide a clear, natural language description of what appears to be in this image based on the detected objects and any text.")
        sb.appendLine("2. If there is a question, math problem, or any text that asks something — answer it fully.")
        sb.appendLine("3. If there is handwritten or printed text, transcribe it accurately and explain what it says.")
        sb.appendLine("4. Be descriptive and helpful. Describe the scene as if the user cannot see the image.")
        
        return sb.toString()
    }
}
