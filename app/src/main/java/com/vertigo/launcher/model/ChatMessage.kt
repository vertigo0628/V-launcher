package com.vertigo.launcher.model

data class ChatMessage(
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String? = null // For photo analysis messages
)
