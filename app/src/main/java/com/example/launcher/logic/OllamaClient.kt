package com.example.launcher.logic

import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class OllamaClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES) // Extreme timeout for slow model loads
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    // Default configuration for the local Ollama instance
    // The default model to use
    private val defaultModel = "llama3.2:1b"

    data class OllamaRequest(
        val model: String,
        val prompt: String,
        val stream: Boolean = true
    )

    data class OllamaResponse(
        val model: String,
        val created_at: String,
        val response: String,
        val done: Boolean,
        val total_duration: Long?
    )

    /**
     * Streams a prompt response from the specified Ollama server chunk-by-chunk.
     * [baseUrl] should be the root address, e.g., "http://127.0.0.1:11434"
     */
    fun generateResponseStream(
        prompt: String, 
        model: String = defaultModel, 
        baseUrl: String = "http://127.0.0.1:11434"
    ): Flow<String> = flow {
        val endpoint = "${baseUrl.trimEnd('/')}/api/generate"
        val requestData = OllamaRequest(model = model, prompt = prompt, stream = true)
        val jsonBody = gson.toJson(requestData)
        val requestBody = jsonBody.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .build()

        val call = client.newCall(request)
        
        // Ensure network call is cancelled if coroutine is cancelled
        val job = kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]
        job?.invokeOnCompletion { 
            if (it is kotlinx.coroutines.CancellationException) {
                android.util.Log.d("OllamaClient", "Cancelling network call due to coroutine cancellation")
                call.cancel() 
            }
        }

        android.util.Log.d("OllamaClient", "Sending streaming request to $endpoint")

        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    val errorMsg = "Ollama Error: ${response.code} ${response.message}"
                    android.util.Log.e("OllamaClient", errorMsg)
                    throw IOException(errorMsg)
                }

                val source = response.body?.source() ?: throw IOException("Empty response body")
                
                while (currentCoroutineContext().isActive && !source.exhausted()) {
                    val line = source.readUtf8Line()
                    if (line != null && line.isNotBlank()) {
                        try {
                            val chunk = gson.fromJson(line, OllamaResponse::class.java)
                            emit(chunk.response)
                            if (chunk.done) break
                        } catch (e: Exception) {
                            android.util.Log.e("OllamaClient", "Error parsing chunk: $line", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (e is java.io.IOException && call.isCanceled()) {
                 android.util.Log.d("OllamaClient", "Stream cancelled successfully")
            } else {
                android.util.Log.e("OllamaClient", "Connection error reaching $endpoint", e)
                throw e
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Wrapper for non-streaming usage or legacy calls.
     */
    suspend fun generateResponse(
        prompt: String, 
        model: String = defaultModel, 
        baseUrl: String = "http://127.0.0.1:11434"
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val sb = StringBuilder()
                generateResponseStream(prompt, model, baseUrl).collect { chunk ->
                    sb.append(chunk)
                }
                Result.success(sb.toString())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    data class OllamaModel(
        val name: String,
        val modified_at: String,
        val size: Long
    )

    data class OllamaTagsResponse(
        val models: List<OllamaModel>
    )

    /**
     * Fetches installed models from the specified Ollama server.
     */
    suspend fun getAvailableModels(baseUrl: String = "http://127.0.0.1:11434"): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            val endpoint = "${baseUrl.trimEnd('/')}/api/tags"
            try {
                val request = Request.Builder()
                    .url(endpoint)
                    .get()
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Failed to fetch models from $endpoint: $response"))
                }

                val responseBodyStr = response.body?.string()
                if (responseBodyStr != null) {
                    val tagsResponse = gson.fromJson(responseBodyStr, OllamaTagsResponse::class.java)
                    val modelNames = tagsResponse.models.map { it.name }
                    Result.success(modelNames)
                } else {
                    Result.failure(IOException("Empty response from $endpoint"))
                }
            } catch (e: Exception) {
                android.util.Log.e("OllamaClient", "Failed to reach $endpoint", e)
                Result.failure(e)
            }
        }
    }
}
