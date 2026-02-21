package com.example.launcher.compose

import android.util.Log
import com.example.launcher.model.AppModel
import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.TextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.delay

private const val TAG = "InlineSearch"

/**
 * Clickable search bar that opens a full-screen search overlay.
 * This appears at the bottom of the home screen.
 */
@Composable
fun InlineSearchBar(
    modifier: Modifier = Modifier,
    isVoiceListening: Boolean = false,
    onSearchClick: () -> Unit
) {
    // Debug: Log when composable renders
    LaunchedEffect(Unit) {
        Log.d(TAG, "InlineSearchBar MOUNTED")
    }
    
    // The clickable search bar placeholder
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .height(48.dp)
            .clip(CircleShape)
            .background(Color(0x55FFFFFF))
            .clickable {
                Log.d(TAG, "Search bar CLICKED - triggering callback")
                onSearchClick()
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(
                    id = if (isVoiceListening) 
                        android.R.drawable.ic_btn_speak_now 
                    else 
                        android.R.drawable.ic_menu_search
                ),
                contentDescription = "Search",
                tint = if (isVoiceListening) Color(0xFF00F0FF) else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isVoiceListening) "Listening..." else "Search apps, web & more...",
                color = if (isVoiceListening) Color(0xFF00F0FF) else Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Full-screen search overlay that appears on top of everything
 */
@Composable
fun SearchOverlay(
    query: String,
    onQueryChange: (String) -> Unit,
    allApps: List<AppModel>,
    onAppClick: (AppModel) -> Unit,
    onWebSearch: (String) -> Unit,
    onClose: () -> Unit
) {
    // Local state for immediate responsiveness
    var localQuery by remember { mutableStateOf(query) }
    
    // Sync local state when external query changes (e.g. cleared by button)
    LaunchedEffect(query) {
        if (query != localQuery) {
            localQuery = query
        }
    }

    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val view = LocalView.current
    val context = LocalContext.current
    
    // Perform search whenever query changes
    LaunchedEffect(localQuery) {
        if (localQuery.isNotEmpty()) {
            searchResults = allApps
                .filter { it.label.contains(localQuery, ignoreCase = true) }
                .take(8)
                .map { SearchResult(it.label, ResultType.APP, it) } +
                listOf(SearchResult("Search \"$localQuery\" on Google", ResultType.WEB, null))
        } else {
            searchResults = emptyList()
        }
    }
    
    // Request focus immediately
    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
        try {
            val window = (context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).show(
                    WindowInsetsCompat.Type.ime()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing keyboard: ${e.message}")
        }
    }
    
    // Full-screen overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xDD000000)) // Dark semi-transparent background
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { 
                // Close when tapping background
                keyboardController?.hide()
                onClose()
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding() // Avoid status bar
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .clickable(enabled = false) {} // Block clicks from propagating
        ) {
            // Search input bar at the top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_search),
                        contentDescription = "Search",
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Input Field
                    BasicTextField(
                        value = localQuery,
                        onValueChange = { 
                            localQuery = it
                            onQueryChange(it)
                        },
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 20.sp
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(Color(0xFF00F0FF)), 
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (localQuery.isNotEmpty()) {
                                    try {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://www.google.com/search?q=${android.net.Uri.encode(localQuery)}")
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error: ${e.message}")
                                    }
                                    keyboardController?.hide()
                                    onClose()
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        decorationBox = { innerTextField ->
                             Box(contentAlignment = Alignment.CenterStart) {
                                if (localQuery.isEmpty()) {
                                    Text(
                                        "Search apps...",
                                        color = Color.Gray,
                                        fontSize = 20.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    
                    // Force Focus Effect
                    LaunchedEffect(Unit) {
                         try {
                            delay(200) 
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        } catch(e: Exception) {
                            Log.e(TAG, "Focus failed", e)
                        }
                    }
                    
                    // Close/Clear button
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                if (localQuery.isNotEmpty()) {
                                    localQuery = ""
                                    onQueryChange("")
                                } else {
                                    keyboardController?.hide()
                                    onClose()
                                }
                            }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Search results
            if (searchResults.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false) 
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F172A)),
                    contentPadding = PaddingValues(
                        bottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding() + 16.dp,
                        top = 8.dp,
                        start = 8.dp,
                        end = 8.dp
                    )
                ) {
                    items(searchResults.size) { index ->
                        val result = searchResults[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    Log.d(TAG, "Result clicked: ${result.title}")
                                    keyboardController?.hide()
                                    when (result.type) {
                                        ResultType.APP -> {
                                            result.appModel?.let { onAppClick(it) }
                                        }
                                        ResultType.WEB -> {
                                            try {
                                                val intent = android.content.Intent(
                                                    android.content.Intent.ACTION_VIEW,
                                                    android.net.Uri.parse("https://www.google.com/search?q=${android.net.Uri.encode(localQuery)}")
                                                )
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Error opening web search: ${e.message}")
                                            }
                                            onClose()
                                        }
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = when (result.type) {
                                        ResultType.APP -> android.R.drawable.sym_def_app_icon
                                        ResultType.WEB -> android.R.drawable.ic_menu_search
                                    }
                                ),
                                contentDescription = null,
                                tint = Color(0xFF00F0FF),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = result.title,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            } else if (localQuery.isEmpty()) {
                // Show hint when no query
                Text(
                    "Type to search apps or the web",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

// Simple data classes for search results
data class SearchResult(
    val title: String,
    val type: ResultType,
    val appModel: com.example.launcher.model.AppModel?
)

enum class ResultType {
    APP, WEB
}
