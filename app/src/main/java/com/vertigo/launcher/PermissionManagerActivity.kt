package com.vertigo.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vertigo.launcher.model.AppModel
import com.vertigo.launcher.data.AppRepository
import com.vertigo.launcher.logic.AppCommander
import kotlinx.coroutines.launch
import androidx.compose.ui.viewinterop.AndroidView

import com.vertigo.launcher.compose.AsyncAppIcon

class PermissionManagerActivity : ComponentActivity() {
    
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val repository = AppRepository(this)
        
        setContent {
            val scope = rememberCoroutineScope()
            var allApps by remember { mutableStateOf<List<AppModel>>(emptyList()) }
            var filteredApps by remember { mutableStateOf<List<AppModel>>(emptyList()) }
            var selectedApp by remember { mutableStateOf<AppModel?>(null) }
            var permissions by remember { mutableStateOf<List<AppCommander.AppPermission>>(emptyList()) }
            var searchQuery by remember { mutableStateOf("") }
            var isLoading by remember { mutableStateOf(true) }
            
            // Handle system back button
            androidx.activity.compose.BackHandler(enabled = selectedApp != null) {
                selectedApp = null
            }
            
            LaunchedEffect(Unit) {
                isLoading = true
                allApps = repository.getInstalledApps()
                filteredApps = allApps
                isLoading = false
            }
            
            LaunchedEffect(searchQuery) {
                filteredApps = if (searchQuery.isEmpty()) allApps else {
                    allApps.filter { it.label.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true) }
                }
            }
            
            LaunchedEffect(selectedApp) {
                selectedApp?.let { app ->
                    permissions = AppCommander.getAppPermissions(app.packageName)
                }
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
                            title = { 
                                Text(
                                    selectedApp?.label?.uppercase() ?: "PERMISSION MANAGER", 
                                    fontWeight = FontWeight.Bold, 
                                    letterSpacing = 2.sp,
                                    fontSize = 16.sp
                                ) 
                            },
                            navigationIcon = {
                                IconButton(onClick = { 
                                    if (selectedApp != null) selectedApp = null else finish() 
                                }) {
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
                    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                        if (selectedApp == null) {
                            // App Search Bar
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                placeholder = { Text("Search apps...", color = Color.Gray) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Cyan) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF1E293B),
                                    unfocusedContainerColor = Color(0xFF1E293B),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true
                            )
                            
                            if (isLoading) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = Color(0xFF00F0FF))
                                }
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(filteredApps, key = { it.packageName }) { app ->
                                        AppItem(
                                            app = app, 
                                            isSystem = (app.category == com.vertigo.launcher.model.AppCategory.SYSTEM),
                                            onClick = { selectedApp = app }
                                        )
                                    }
                                }
                            }
                        } else {
                            // Permissions List
                            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                                if (permissions.isEmpty()) {
                                    item {
                                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("No dangerous permissions found", color = Color.Gray)
                                        }
                                    }
                                } else {
                                    items(permissions, key = { it.fullName }) { perm ->
                                        PermissionItem(
                                            permission = perm,
                                            onToggle = { isGranted ->
                                                // Optimistic Update
                                                val oldPermissions = permissions
                                                permissions = permissions.map {
                                                    if (it.fullName == perm.fullName) it.copy(isGranted = isGranted) else it
                                                }
                                                
                                                scope.launch {
                                                    val result = if (isGranted) {
                                                        AppCommander.grantPermission(selectedApp!!.packageName, perm.fullName)
                                                    } else {
                                                        AppCommander.revokePermission(selectedApp!!.packageName, perm.fullName)
                                                    }
                                                    
                                                    if (!result.isSuccess) {
                                                        // Rollback on failure
                                                        permissions = oldPermissions
                                                        android.widget.Toast.makeText(
                                                            this@PermissionManagerActivity,
                                                            "Failed: ${result.stderr.takeIf { it.isNotBlank() } ?: "Operation not allowed"}",
                                                            android.widget.Toast.LENGTH_LONG
                                                        ).show()
                                                    } else {
                                                        // Refresh to ensure sync
                                                        permissions = AppCommander.getAppPermissions(selectedApp!!.packageName)
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
}

@Composable
fun AppItem(app: AppModel, isSystem: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncAppIcon(
            app = app,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = app.label, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(text = app.packageName, color = Color.Gray, fontSize = 11.sp)
        }
        
        if (isSystem) {
            Text(
                "SYSTEM",
                color = Color.Red,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun PermissionItem(permission: AppCommander.AppPermission, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = permission.displayName.replace("_", " "),
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Text(
                text = permission.fullName,
                color = Color.Gray,
                fontSize = 10.sp
            )
        }
        
        Switch(
            checked = permission.isGranted,
            onCheckedChange = { onToggle(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00F0FF),
                checkedTrackColor = Color(0xFF00F0FF).copy(alpha = 0.5f)
            )
        )
    }
}
