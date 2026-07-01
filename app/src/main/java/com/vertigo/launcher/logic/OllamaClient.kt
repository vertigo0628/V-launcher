package com.vertigo.launcher.logic

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

    // No default model - always use what Ollama has installed

    data class OllamaRequest(
        val model: String,
        val prompt: String,
        val stream: Boolean = true,
        val system: String? = null
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
        model: String,
        baseUrl: String = "http://127.0.0.1:11434",
        systemPrompt: String? = null
    ): Flow<String> = flow {
        // Try /api/generate first, then /api/chat as fallback
        val generateEndpoint = "${baseUrl.trimEnd('/')}/api/generate"
        val chatEndpoint = "${baseUrl.trimEnd('/')}/api/chat"
        
        val generateBody = gson.toJson(OllamaRequest(model = model, prompt = prompt, stream = true, system = systemPrompt))
        
        val chatMessages = mutableListOf<Map<String, String>>()
        if (systemPrompt != null) {
            chatMessages.add(mapOf("role" to "system", "content" to systemPrompt))
        }
        chatMessages.add(mapOf("role" to "user", "content" to prompt))
        val chatBody = gson.toJson(mapOf("model" to model, "messages" to chatMessages, "stream" to true))
        
        var shouldFallback = false
        
        // Attempt 1: /api/generate
        try {
            val result = executeStreamRequest(generateEndpoint, generateBody)
            if (result == null) {
                shouldFallback = true
            } else {
                result.forEach { emit(it) }
            }
        } catch (e: Exception) {
            shouldFallback = true
        }
        
        // Attempt 2: /api/chat (fallback)
        if (shouldFallback) {
            android.util.Log.w("OllamaClient", "/api/generate failed, falling back to /api/chat")
            try {
                val result = executeStreamRequest(chatEndpoint, chatBody)
                result?.forEach { emit(it) }
            } catch (e: Exception) {
                android.util.Log.e("OllamaClient", "Connection error reaching $chatEndpoint", e)
                throw e
            }
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Executes a streaming HTTP request to the given endpoint.
     * Returns null if the server returns 404 (signal to try fallback).
     * Returns a list of text chunks on success.
     */
    private suspend fun executeStreamRequest(endpoint: String, jsonBody: String): List<String>? {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            val requestBody = jsonBody.toRequestBody(mediaType)
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            val call = client.newCall(request)
            android.util.Log.d("OllamaClient", "Sending streaming request to $endpoint")

            call.execute().use { response ->
                if (response.code == 404) {
                    android.util.Log.w("OllamaClient", "404 at $endpoint, will try fallback")
                    return@withContext null // signal to try fallback
                }
                
                if (!response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    android.util.Log.e("OllamaClient", "HTTP ${response.code} from $endpoint: $body")
                    throw IOException("Ollama Error: ${response.code} ${response.message} - $body")
                }

                val source = response.body?.source() ?: throw IOException("Empty response body")
                val chunks = mutableListOf<String>()
                
                while (!source.exhausted()) {
                    val line = source.readUtf8Line()
                    android.util.Log.d("OllamaClient", "RAW LINE: $line")
                    if (line != null && line.isNotBlank()) {
                        try {
                            val jsonObj = com.google.gson.JsonParser.parseString(line).asJsonObject
                            // Check for Ollama error inside 200 response body
                            if (jsonObj.has("error")) {
                                val errMsg = jsonObj.get("error").asString
                                android.util.Log.e("OllamaClient", "Ollama returned error in body: $errMsg")
                                throw IOException("Ollama model error: $errMsg")
                            }
                            val text = when {
                                jsonObj.has("response") -> jsonObj.get("response").asString
                                jsonObj.has("message") && jsonObj.getAsJsonObject("message").has("content") ->
                                    jsonObj.getAsJsonObject("message").get("content").asString
                                else -> ""
                            }
                            if (text.isNotEmpty()) chunks.add(text)
                            val done = jsonObj.has("done") && jsonObj.get("done").asBoolean
                            if (done) break
                        } catch (e: IOException) {
                            throw e // re-throw real errors
                        } catch (e: Exception) {
                            android.util.Log.e("OllamaClient", "Error parsing chunk: $line", e)
                        }
                    }
                }
                android.util.Log.d("OllamaClient", "Stream finished. Total chunks: ${chunks.size}")
                chunks
            }
        }
    }

    /**
     * Wrapper for non-streaming usage or legacy calls.
     */
    suspend fun generateResponse(
        prompt: String, 
        model: String,
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
