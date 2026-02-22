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
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.viewinterop.AndroidView
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
    searchResults: List<com.example.launcher.utils.SearchManager.SearchResult>,
    onResultClick: (com.example.launcher.utils.SearchManager.SearchResult) -> Unit,
    onClose: () -> Unit,
    isSearching: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    val context = LocalContext.current
    
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
                    
                    // Input Field (Upgraded to TextField for better stability and cursor visibility)
                    TextField(
                        value = query,
                        onValueChange = onQueryChange,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 18.sp
                        ),
                        placeholder = {
                            Text(
                                "Search apps, web & more...",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 18.sp
                            )
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color(0xFF00F0FF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (query.isNotEmpty()) {
                                    keyboardController?.hide()
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                    )
                    
                    
                    // Close/Clear button or Loading indicator
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFF00F0FF),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    if (query.isNotEmpty()) {
                                        onQueryChange("")
                                    } else {
                                        keyboardController?.hide()
                                        onClose()
                                    }
                                }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Search results using SearchResultItem which handles types correctly
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
                        SearchResultItem(
                            result = result,
                            onClick = {
                                keyboardController?.hide()
                                onResultClick(result)
                                onClose()
                            }
                        )
                    }
                }
            } else if (query.isEmpty()) {
                // Show hint when no query
                Text(
                    "Type to search apps, contacts or the web",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun SearchResultItem(
    result: com.example.launcher.utils.SearchManager.SearchResult,
    onClick: (com.example.launcher.utils.SearchManager.SearchResult) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(result) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (result.type == com.example.launcher.utils.SearchManager.ResultType.APP) {
            // Load App Icon
            AndroidView<android.widget.ImageView>(
                factory = { context ->
                    android.widget.ImageView(context).apply {
                        try {
                            val icon = context.packageManager.getApplicationIcon(result.id)
                            setImageDrawable(icon)
                        } catch (e: Exception) {
                            setImageResource(android.R.drawable.sym_def_app_icon)
                        }
                    }
                },
                modifier = Modifier.size(32.dp)
            )
        } else {
            // Generic icons for other types
            val iconRes = when (result.type) {
                com.example.launcher.utils.SearchManager.ResultType.CONTACT -> android.R.drawable.sym_action_call
                com.example.launcher.utils.SearchManager.ResultType.WEB_SEARCH -> android.R.drawable.ic_menu_search
                com.example.launcher.utils.SearchManager.ResultType.SETTING -> android.R.drawable.ic_menu_preferences
                else -> android.R.drawable.ic_menu_help
            }
            
            val tint = when(result.type) {
                com.example.launcher.utils.SearchManager.ResultType.CONTACT -> Color.Green
                com.example.launcher.utils.SearchManager.ResultType.WEB_SEARCH -> Color(0xFF00F0FF)
                else -> Color.White
            }
            
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = result.title,
                color = Color.White,
                fontSize = 16.sp
            )
            if (!result.subtitle.isNullOrBlank()) {
                Text(
                    text = result.subtitle,
                    color = Color.Gray,
                    fontSize = 12.sp
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
