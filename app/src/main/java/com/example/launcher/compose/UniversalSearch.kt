package com.example.launcher.compose
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.launcher.model.AppModel
import com.example.launcher.R

@Composable
fun UniversalSearch(
    query: String, // Clean hoisting
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    searchResults: List<com.example.launcher.utils.SearchManager.SearchResult>,
    onResultClick: (com.example.launcher.utils.SearchManager.SearchResult) -> Unit
) {
    // Focus requester to show keyboard automatically
    val focusRequester = remember { FocusRequester() }
    val view = androidx.compose.ui.platform.LocalView.current
    
    // Request focus when composable enters composition
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300) // Wait for animation/layout to settle
        focusRequester.requestFocus()
        
        // Use WindowInsetsControllerCompat for robust keyboard handling
        val window = (view.context as? android.app.Activity)?.window
        if (window != null) {
             androidx.core.view.WindowCompat.getInsetsController(window, view).show(
                androidx.core.view.WindowInsetsCompat.Type.ime()
            )
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3000000))
            .clickable { onClose() }
            .windowInsetsPadding(WindowInsets.ime) // push up for keyboard
            .padding(top = 48.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable(enabled = false) {} // Catch clicks
                .background(Color.Transparent)
        ) {
            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color(0xFF1E293B), RoundedCornerShape(28.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_search),
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange, // Direct callback
                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                        cursorBrush = SolidColor(Color(0xFF00F0FF)),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester) // Attach requester
                            .onGloballyPositioned { 
                                if (!it.isAttached) return@onGloballyPositioned
                                // Request focus (we rely on WindowInsetsController for the soft keyboard)
                                focusRequester.requestFocus() 
                            },
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) {
                                    Text("Search apps, web, & more...", color = Color.Gray)
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Search Results - Always show container when typing
            if (query.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    // Debug: Show total results count
                    item {
                        Text(
                            text = "Results: ${searchResults.size}",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    // Group results by type
                    val apps = searchResults.filter { it.type == com.example.launcher.utils.SearchManager.ResultType.APP }
                    val contacts = searchResults.filter { it.type == com.example.launcher.utils.SearchManager.ResultType.CONTACT }
                    val web = searchResults.filter { it.type == com.example.launcher.utils.SearchManager.ResultType.WEB_SEARCH }
                    val settings = searchResults.filter { it.type == com.example.launcher.utils.SearchManager.ResultType.SETTING }
                    
                    // Web First
                    if (web.isNotEmpty()) {
                        item {
                            Text(
                                text = "WEB AGENTS",
                                color = Color(0xFF00F0FF),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                         items(web.size) { i ->
                             SearchResultItem(web[i], onResultClick)
                         }
                         
                         // Add explicit YouTube button if not present in generic results
                         // (Just to be sure we have the cool buttons even if SearchManager logic varies)
                         // But if we already have web results, duplications might occur if SearchManager provides them.
                         // SearchManager.search() adds "Search 'query'" generic item.
                         // It doesn't add YouTube by default unless we type "youtube"? No, line 76 in SearchManager just adds generic.
                         
                         // Let's add the YouTube shortcut manually here for better UX
                         item {
                             val ytResult = com.example.launcher.utils.SearchManager.SearchResult(
                                 type = com.example.launcher.utils.SearchManager.ResultType.WEB_SEARCH,
                                 id = "yt_$query",
                                 title = "Watch on YouTube",
                                 subtitle = "Video Search: \"$query\"",
                                 action = com.example.launcher.utils.SearchManager.SearchAction.WebSearch(query, "youtube")
                             )
                             SearchResultItem(ytResult, onResultClick, iconOverride = android.R.drawable.ic_media_play, tintOverride = Color.Red)
                         }
                    }
                    
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                    
                    // Contacts
                    if (contacts.isNotEmpty()) {
                        item {
                            Text(
                                text = "CONTACTS",
                                color = Color(0xFF00F0FF),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(contacts.size) { i ->
                            SearchResultItem(contacts[i], onResultClick)
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }

                    // Apps
                    if (apps.isNotEmpty()) {
                        item {
                            Text(
                                text = "APPS",
                                color = Color(0xFF00F0FF),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(apps.size) { i ->
                            SearchResultItem(apps[i], onResultClick)
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                    
                    // Settings
                    if (settings.isNotEmpty()) {
                        item {
                            Text(
                                text = "SETTINGS",
                                color = Color(0xFF00F0FF),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(settings.size) { i ->
                            SearchResultItem(settings[i], onResultClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(
    result: com.example.launcher.utils.SearchManager.SearchResult,
    onClick: (com.example.launcher.utils.SearchManager.SearchResult) -> Unit,
    iconOverride: Int? = null,
    tintOverride: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable { onClick(result) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (result.type == com.example.launcher.utils.SearchManager.ResultType.APP) {
            // Load App Icon from Package Manager
             AndroidView(
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
            // Use generic icon
            val iconRes = iconOverride ?: when (result.type) {
                com.example.launcher.utils.SearchManager.ResultType.CONTACT -> android.R.drawable.sym_action_call
                com.example.launcher.utils.SearchManager.ResultType.WEB_SEARCH -> android.R.drawable.ic_menu_search
                com.example.launcher.utils.SearchManager.ResultType.SETTING -> R.drawable.ic_category_settings
                else -> android.R.drawable.ic_menu_help
            }
            
            val tint = tintOverride ?: when(result.type) {
                com.example.launcher.utils.SearchManager.ResultType.CONTACT -> Color.Green
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
    androidx.compose.material3.Divider(color = Color(0xFF334155))
}
