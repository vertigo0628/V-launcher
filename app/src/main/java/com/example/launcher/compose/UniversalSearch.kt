package com.example.launcher.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

@Composable
fun UniversalSearch(
    query: String, // Clean hoisting
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    onAppClick: (AppModel) -> Unit,
    filteredApps: List<AppModel>
) {
    // Focus requester to show keyboard automatically
    val focusRequester = remember { FocusRequester() }
    
    // Request focus when composable enters composition
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3000000))
            .clickable { onClose() }
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
                            .focusRequester(focusRequester), // Attach requester
                        decorationBox = { innerTextField ->
                            if (query.isEmpty()) {
                                Text("Search apps, web, & more...", color = Color.Gray)
                            }
                            innerTextField()
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Search Results
            if (query.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    // ASST. AGENT ACTIONS
                    if (query.isNotEmpty()) {
                        item {
                            Text(
                                text = "WEB AGENTS",
                                color = Color(0xFF00F0FF),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                        // Search Google
                        item {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                                    .clickable { 
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://www.google.com/search?q=$query")
                                        )
                                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_menu_search),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "Search Google",
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Browser Search: \"$query\"",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Divider(color = Color(0xFF334155))
                        }

                        // Search YouTube
                        item {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                                    .clickable { 
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://www.youtube.com/results?search_query=$query")
                                        )
                                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_media_play),
                                    contentDescription = null,
                                    tint = Color(0xFFFF0000), // YouTube Red
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "Watch on YouTube",
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Video Search: \"$query\"",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Divider(color = Color(0xFF334155))
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                    
                    // APP RESULTS
                     if (filteredApps.isNotEmpty()) {
                        item {
                            Text(
                                text = "APPS",
                                color = Color(0xFF00F0FF),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                         items(filteredApps.size) { i ->
                            val app = filteredApps[i]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                                    .clickable { onAppClick(app) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // App Icon (Using AndroidView for Drawable)
                                AndroidView(
                                    factory = { context ->
                                        android.widget.ImageView(context).apply {
                                            setImageDrawable(app.icon)
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = app.label,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
