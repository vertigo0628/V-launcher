package com.example.launcher.compose

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.launcher.model.ChatMessage

@Composable
fun VoiceAssistantWidget(
    isEnabled: Boolean,
    isListening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    chatHistory: List<ChatMessage> = emptyList(),
    currentStreamingResponse: String? = null,
    isAiThinking: Boolean = false,
    spokenText: String = "",
    isHotwordActive: Boolean = false,
    onClearResponse: () -> Unit = {},
    onSendText: (String) -> Unit = {},
    onStopAi: () -> Unit = {},
    onCameraClick: () -> Unit = {},
    getPhotoBitmap: (Long) -> android.graphics.Bitmap? = { null }
) {
    // Pulse animation for Listening state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isListening) 1.1f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Colors
    val activeGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF00F0FF), Color(0xFF0099FF)) // Neon Cyan
    )
    val inactiveGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFEF4444), Color(0xFF991B1B)) // Red "Off"
    )
    
    // Main state driven by isEnabled (Intention), not just isListening (Hardware)
    val bgBrush = if (isEnabled) activeGradient else inactiveGradient
    
    val iconRes = if (isEnabled) android.R.drawable.ic_btn_speak_now else android.R.drawable.ic_menu_close_clear_cancel
    
    val statusText = if (isEnabled) "VOICE ON" else "VOICE OFF"
    
    var textInput by remember { mutableStateOf("") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .scale(if (isEnabled && isListening) pulseScale else 1.0f) // Only pulse if enabled AND listening
                .clip(RoundedCornerShape(32.dp))
                .background(bgBrush)
                .clickable { onClick() }
                .padding(horizontal = 20.dp, vertical = 12.dp), // Pill padding
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(10.dp))
                
                Text(
                    text = statusText,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
        
        AnimatedVisibility(visible = true) {
            Box(
                modifier = Modifier
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xBB1E293B))
                    .border(1.dp, Color(0x3300F0FF), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI BRAIN (TERMUX)",
                            color = Color(0xFF00F0FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        if (chatHistory.isNotEmpty() || currentStreamingResponse != null) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                                contentDescription = "Clear",
                                tint = Color.Gray,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onClearResponse() }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val listState = rememberLazyListState()
                    
                    // Auto-scroll to bottom when history or streaming changes
                    LaunchedEffect(chatHistory.size, currentStreamingResponse) {
                        if (chatHistory.isNotEmpty() || currentStreamingResponse != null) {
                            listState.animateScrollToItem((chatHistory.size + if (currentStreamingResponse != null) 1 else 0).coerceAtLeast(0))
                        }
                    }

                    SelectionContainer {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                        ) {
                            items(chatHistory) { message ->
                                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                                    if (message.role == "user") {
                                        Column(modifier = Modifier.padding(bottom = 4.dp)) {
                                            Row {
                                                Text(
                                                    text = "vertigo@v-launcher:~$ ",
                                                    color = Color(0xFF4ADE80), // Terminal Green
                                                    fontSize = 12.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = message.content,
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                            // Show photo thumbnail if available
                                            if (message.imageUri != null) {
                                                val photoBmp = getPhotoBitmap(message.timestamp)
                                                if (photoBmp != null) {
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    androidx.compose.foundation.Image(
                                                        bitmap = photoBmp.asImageBitmap(),
                                                        contentDescription = "Captured photo",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(72.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .border(1.dp, Color(0x5500F0FF), RoundedCornerShape(8.dp))
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = parseThinkingTags(message.content),
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                            
                            // Streaming Response (if active)
                            if (currentStreamingResponse != null) {
                                item {
                                    Text(
                                        text = parseThinkingTags(currentStreamingResponse),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                            
                            // Thinking State
                            if (isAiThinking && currentStreamingResponse == null) {
                                item {
                                    Text(
                                        text = "Thinking...",
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            
                            if (chatHistory.isEmpty() && currentStreamingResponse == null && !isAiThinking) {
                                item {
                                    Text(
                                        text = "Waiting for input...",
                                        color = Color.Gray,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                    
                    // Persistent Ready State
                    if (isHotwordActive && spokenText.isEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Sunday is listening...",
                            color = Color(0xFF00F0FF).copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                    
                    // Real-time Transcription
                    if (isListening && spokenText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Hearing: ",
                                color = Color(0xFF00F0FF),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = spokenText,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Terminal Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = ">",
                            color = Color(0xFF00F0FF),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 8.dp, bottom = 14.dp)
                        )
                        
                        TextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("Query Local AI...", color = Color.Gray, fontSize = 14.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.Transparent),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = Color(0xFF00F0FF)
                            ),
                            minLines = 1,
                            maxLines = 4,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                                imeAction = androidx.compose.ui.text.input.ImeAction.Send
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSend = {
                                    if (textInput.isNotBlank() && !isAiThinking && currentStreamingResponse == null) {
                                        onSendText(textInput)
                                        textInput = ""
                                    }
                                }
                            )
                        )
                        
                        if (textInput.isNotBlank() && !isAiThinking && currentStreamingResponse == null) {
                            IconButton(
                                onClick = { 
                                    onSendText(textInput)
                                    textInput = "" // Clear input after sending
                                },
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_menu_send),
                                    contentDescription = "Send",
                                    tint = Color(0xFF00F0FF)
                                )
                            }
                        }

                        // Camera Button: Capture photo for AI vision analysis
                        if (!isAiThinking && currentStreamingResponse == null) {
                            IconButton(
                                onClick = { onCameraClick() },
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_menu_camera),
                                    contentDescription = "Take Photo",
                                    tint = Color(0xFF00F0FF).copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Stop Button: Visible only when AI is active
                        if (isAiThinking || currentStreamingResponse != null) {
                            IconButton(
                                onClick = {
                                    android.util.Log.d("VoiceWidget", "Stop button clicked")
                                    onStopAi()
                                },
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                                    contentDescription = "Stop AI",
                                    tint = Color.Red.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Parses text for <think> and </think> tags and returns an AnnotatedString
 * with the "thinking" part dimmed and italicized.
 */
private fun parseThinkingTags(text: String): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        val thinkStartTag = "<think>"
        val thinkEndTag = "</think>"
        
        while (currentIndex < text.length) {
            val startIdx = text.indexOf(thinkStartTag, currentIndex)
            
            if (startIdx != -1) {
                // Add text before the tag
                append(text.substring(currentIndex, startIdx))
                
                // Add the start tag itself (dimmed)
                withStyle(style = SpanStyle(color = Color.Gray.copy(alpha = 0.5f))) {
                    append(thinkStartTag)
                }
                
                val contentStart = startIdx + thinkStartTag.length
                val endIdx = text.indexOf(thinkEndTag, contentStart)
                
                if (endIdx != -1) {
                    // Found both tags
                    withStyle(style = SpanStyle(
                        color = Color.Gray.copy(alpha = 0.7f),
                        fontStyle = FontStyle.Italic
                    )) {
                        append(text.substring(contentStart, endIdx))
                    }
                    
                    // Add the end tag (dimmed)
                    withStyle(style = SpanStyle(color = Color.Gray.copy(alpha = 0.5f))) {
                        append(thinkEndTag)
                    }
                    
                    currentIndex = endIdx + thinkEndTag.length
                } else {
                    // Streaming: Only start tag found so far
                    withStyle(style = SpanStyle(
                        color = Color.Gray.copy(alpha = 0.7f),
                        fontStyle = FontStyle.Italic
                    )) {
                        append(text.substring(contentStart))
                    }
                    currentIndex = text.length
                }
            } else {
                // No more think tags
                append(text.substring(currentIndex))
                currentIndex = text.length
            }
        }
    }
}
