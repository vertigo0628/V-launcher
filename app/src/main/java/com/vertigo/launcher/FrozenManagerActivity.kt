package com.vertigo.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vertigo.launcher.model.AppModel
import com.vertigo.launcher.data.AppRepository
import com.vertigo.launcher.logic.AppCommander
import kotlinx.coroutines.launch
import androidx.compose.ui.viewinterop.AndroidView

class FrozenManagerActivity : ComponentActivity() {
    
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val repository = AppRepository(this)
        
        setContent {
            val scope = rememberCoroutineScope()
            var frozenApps by remember { mutableStateOf<List<AppModel>>(emptyList()) }
            var isLoading by remember { mutableStateOf(true) }
            
            fun refresh() {
                scope.launch {
                    isLoading = true
                    frozenApps = repository.getFrozenApps()
                    isLoading = false
                }
            }
            
            LaunchedEffect(Unit) {
                refresh()
            }
            
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF00F0FF),
                    background = Color(0xFF0F172A),
                    surface = Color(0xFF1E293B)
                )
            ) {
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("FROZEN APP MANAGER", fontWeight = FontWeight.Bold, letterSpacing = 2.sp) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = Color(0xFF0F172A),
                                titleContentColor = Color(0xFF00F0FF),
                                navigationIconContentColor = Color.White
                            )
                        )
                    },
                    containerColor = Color(0xFF0F172A)
                ) { padding ->
                    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF00F0FF))
                        } else if (frozenApps.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("❄️", fontSize = 64.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No frozen apps found", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                                items(frozenApps) { app ->
                                    FrozenAppItem(
                                        app = app,
                                        onUnfreeze = {
                                            scope.launch {
                                                val result = AppCommander.unfreezeApp(app.packageName)
                                                if (result.isSuccess) {
                                                    refresh()
                                                }
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FrozenAppItem(app: AppModel, onUnfreeze: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E293B))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AndroidView(
            factory = { ctx ->
                android.widget.ImageView(ctx).apply {
                    setImageDrawable(app.icon)
                }
            },
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(text = app.label, color = Color.White, fontWeight = FontWeight.Bold)
            Text(text = app.packageName, color = Color.Gray, fontSize = 11.sp)
        }
        
        Button(
            onClick = onUnfreeze,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF).copy(alpha = 0.2f)),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F0FF))
        ) {
            Text("UNFREEZE", color = Color(0xFF00F0FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
