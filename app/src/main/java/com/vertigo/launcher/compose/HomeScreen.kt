package com.vertigo.launcher.compose

import android.content.res.Configuration
import android.content.Intent
import android.content.Context
import android.net.Uri
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.filled.Lock
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.* // Use wildcard used to simplify or list relevant ones
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.scale
import com.vertigo.launcher.utils.rememberBouncyOverscrollModifier
import com.vertigo.launcher.R
import com.vertigo.launcher.model.AppModel
import com.vertigo.launcher.ui.CyberClockView
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.animation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.core.*
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    flowerApps: List<AppModel>,
    allApps: List<AppModel>,
    hiddenApps: List<AppModel> = emptyList(),
    onAppClick: (AppModel) -> Unit,
    showDrawer: Boolean,
    onDrawerToggle: (Boolean) -> Unit,
    neuralHubState: com.vertigo.launcher.ui.HomeViewModel.NeuralHubState,
    showNeuralHub: Boolean,
    onNeuralHubToggle: (Boolean) -> Unit,
    weatherState: com.vertigo.launcher.ui.HomeViewModel.WeatherState,
    isVoiceListening: Boolean,
    isVoiceEnabled: Boolean,
    onVoiceClick: () -> Unit,
    searchQuery: String,
    searchResults: List<com.vertigo.launcher.utils.SearchManager.SearchResult>,
    onSearchQueryChange: (String) -> Unit = {},
    onSearchResultClick: (com.vertigo.launcher.utils.SearchManager.SearchResult) -> Unit = {},
    isSearching: Boolean = false,
    onAddToGrid: (AppModel) -> Unit,
    onRemoveFromGrid: (AppModel) -> Unit,
    onHideApp: (AppModel) -> Unit,
    onUnhideApp: (AppModel) -> Unit = {},
    onLaunchPopup: (AppModel) -> Unit = {},
    viewModel: com.vertigo.launcher.ui.HomeViewModel? = null,
    onSettings: () -> Unit,
    musicState: com.vertigo.launcher.ui.HomeViewModel.MusicState = com.vertigo.launcher.ui.HomeViewModel.MusicState(),
    onMusicPlayPause: () -> Unit = {},
    onMusicNext: () -> Unit = {},
    onMusicPrev: () -> Unit = {},
    lockedApps: Set<String> = emptySet(),
    onLockApp: (AppModel) -> Unit = {},
    onUnlockApp: (AppModel) -> Unit = {},
    cpuHistory: List<Int> = emptyList(),
    neuralInsight: String = "System running optimally",
    notificationCounts: Map<String, Int> = emptyMap(),
    folders: Map<String, Set<String>> = emptyMap(),
    onAddAppToFolder: (String, String) -> Unit = { _, _ -> },
    onDeleteFolder: (String) -> Unit = {},
    shortcuts: List<com.vertigo.launcher.ui.HomeViewModel.AppShortcut> = emptyList(),
    onLoadShortcuts: (String) -> Unit = {},
    onShortcutClick: (com.vertigo.launcher.ui.HomeViewModel.AppShortcut) -> Unit = {},
    onClearShortcuts: () -> Unit = {},
    chatHistory: List<com.vertigo.launcher.model.ChatMessage> = emptyList(),
    currentStreamingResponse: String? = null,
    isAiThinking: Boolean = false,
    onClearAiResponse: () -> Unit = {},
    onSendAiText: (String) -> Unit = {},
    onStopAiText: () -> Unit = {},
    spokenText: String = "",
    isHotwordActive: Boolean = false,
    onCameraClick: () -> Unit = {},
    shizukuState: com.vertigo.launcher.utils.ShizukuSetup.ShizukuState = com.vertigo.launcher.utils.ShizukuSetup.ShizukuState.UNAVAILABLE,
    shizukuActionResult: String? = null,
    frozenApps: Set<String> = emptySet(),
    onFreezeApp: (String) -> Unit = {},
    onUnfreezeApp: (String) -> Unit = {},
    onForceStopApp: (String) -> Unit = {},
    onClearAppData: (String) -> Unit = {},
    onSilentUninstallApp: (String) -> Unit = {},
    onClearShizukuResult: () -> Unit = {},
    showLabels: Boolean = true,
    showBadges: Boolean = true,
    gridSize: Int = 4,
    themeAccentColor: Color = Color(0xFF00F0FF),
    clockColors: Triple<Int, Int, Int> = Triple(0xFF00F0FF.toInt(), 0xFFFF006E.toInt(), 0xFFBD00FF.toInt()),
    isSelectionMode: Boolean = false,
    selectedPackages: Set<String> = emptySet(),
    onToggleSelection: (String) -> Unit = {},
    onEnterSelectionMode: (String) -> Unit = {},
    onBatchHide: () -> Unit = {},
    onBatchFreeze: () -> Unit = {},
    onBatchUnhide: () -> Unit = {},
    onBatchUnfreeze: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    hiddenLayers: Map<String, Set<String>> = emptyMap(),
    // Callback to create/delete layers passed from ViewModel
    onCreateLayer: (String, Boolean) -> Unit = { _, _ -> },
    onDeleteLayer: (String) -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    // Shizuku Result Toast
    val context = LocalContext.current
    LaunchedEffect(shizukuActionResult) {
        shizukuActionResult?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            onClearShizukuResult()
        }
    }
    
    val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel?.refreshWeather()
        }
    }
    
    LaunchedEffect(Unit) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) 
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }
    
    var showInlineSearch by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showScribbleSearch by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showHiddenDrawer by remember { androidx.compose.runtime.mutableStateOf(false) }
    var activeFolder by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var showEdgePanel by remember { androidx.compose.runtime.mutableStateOf(false) }
    
    val prefs = remember { com.vertigo.launcher.utils.StorageHelper.getSafeSharedPreferences(context, "launcher_prefs") }
    
    var scribbleSearchEnabled by remember { androidx.compose.runtime.mutableStateOf(prefs.getBoolean("scribble_search_enabled", true)) }
    val glassmorphismEnabled = true
    var weatherPrefs by remember { androidx.compose.runtime.mutableStateOf(prefs.getBoolean("weather_particles_enabled", true)) }
    var edgePanelEnabled by remember { androidx.compose.runtime.mutableStateOf(prefs.getBoolean("edge_panel_enabled", false)) }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, prefs) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                scribbleSearchEnabled = prefs.getBoolean("scribble_search_enabled", true)
                weatherPrefs = prefs.getBoolean("weather_particles_enabled", true)
                edgePanelEnabled = prefs.getBoolean("edge_panel_enabled", false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // --- Onion Drawer Depth Navigation ---
    // Depth 0 = base hidden apps, Depth 1+ = custom layers (peel inward)
    var currentDepth by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var showBiometricAuth by remember { androidx.compose.runtime.mutableStateOf(false) }
    var pinUnlocked by remember { androidx.compose.runtime.mutableStateOf(false) }

    // BackHandler: peel outward through onion layers
    BackHandler(enabled = true) {
        if (showScribbleSearch) {
            showScribbleSearch = false
        } else if (showInlineSearch) {
            showInlineSearch = false
        } else if (showHiddenDrawer) {
            if (currentDepth > 0) {
                // Peel back one layer
                currentDepth--
            } else {
                // Exit hidden drawer entirely
                showHiddenDrawer = false
                currentDepth = 0
                pinUnlocked = false
            }
        } else if (activeFolder != null) {
            activeFolder = null
        } else if (showEdgePanel) {
            showEdgePanel = false
        } else if (showNeuralHub) {
            onNeuralHubToggle(false)
        } else if (showDrawer) {
            onDrawerToggle(false)
        } else {
            // Home screen is clean, do nothing (prevents activity finishing)
            android.util.Log.d("HomeScreen", "Back pressed on clean home screen - ignoring to prevent reset")
        }
    }
    
    // Removed duplicate BackHandler — handled by the first BackHandler above

    var accumulatedDragY by remember { mutableFloatStateOf(0f) }
    var accumulatedDragX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent) // Show wallpaper
            .pointerInput(showDrawer, showInlineSearch, showNeuralHub, showEdgePanel) {
                if (!showDrawer && !showInlineSearch && !showNeuralHub && !showEdgePanel) {
                    detectDragGestures(
                        onDragEnd = { accumulatedDragY = 0f; accumulatedDragX = 0f },
                        onDragCancel = { accumulatedDragY = 0f; accumulatedDragX = 0f },
                        onDrag = { _, dragAmount -> 
                            accumulatedDragY += dragAmount.y
                            accumulatedDragX += dragAmount.x
                            
                            // Check horizontal drag for edge panel (swipe left from right edge)
                            if (accumulatedDragX < -150f) {
                                if (edgePanelEnabled) {
                                    showEdgePanel = true
                                    accumulatedDragX = 0f
                                    accumulatedDragY = 0f
                                }
                            }
                            
                            if (accumulatedDragY > 150f) {
                                showInlineSearch = true
                                accumulatedDragY = 0f
                                accumulatedDragX = 0f
                            } else if (accumulatedDragY < -150f) {
                                onDrawerToggle(true)
                                accumulatedDragY = 0f
                                accumulatedDragX = 0f
                            }
                        }
                    )
                }
            }
    ) {
        // Weather Particles Background (drawn over the system wallpaper but behind launcher UI)
        if (weatherPrefs) {
            WeatherAtmosphereOverlay(weatherCode = weatherState.weatherCode)
        }
        
        // Blurred Background when Overlay is Open
        
        // Mutable Grid State
        var appPendingAction by remember { androidx.compose.runtime.mutableStateOf<AppModel?>(null) }
        var actionType by remember { androidx.compose.runtime.mutableStateOf<String?>(null) } // "ADD" or "REMOVE"
        // Privacy Shield: track app pending biometric unlock
        var lockedAppPending by remember { androidx.compose.runtime.mutableStateOf<AppModel?>(null) }

        if (appPendingAction != null && actionType != null) {
            if (actionType == "REMOVE") {
                AlertDialog(
                    onDismissRequest = { 
                        appPendingAction = null
                        actionType = null
                    },
                    title = { Text("Remove from Grid?", color = Color.White) },
                    text = { Text("Remove ${appPendingAction?.label} from the honeycomb grid?", color = Color.LightGray) },
                    confirmButton = {
                        Button(
                            onClick = {
                                appPendingAction?.let { onRemoveFromGrid(it) }
                                appPendingAction = null
                                actionType = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeAccentColor)
                        ) { Text("Remove", color = Color.Black) }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            appPendingAction = null
                            actionType = null
                        }) { Text("Cancel", color = Color.Gray) }
                    },
                    containerColor = Color(0xFF1E293B),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                )
            } else if (actionType == "DRAWER_OPTIONS") {
                AlertDialog(
                    onDismissRequest = { 
                        appPendingAction = null
                        actionType = null
                        onClearShortcuts()
                    },
                    title = { Text(appPendingAction?.label ?: "Options", color = Color.White) },
                    text = { Text("Choose an action:", color = Color.LightGray) },
                    confirmButton = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row {
                                Button(
                                    onClick = {
                                        appPendingAction?.let { onAddToGrid(it) }
                                        appPendingAction = null
                                        actionType = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeAccentColor)
                                ) { Text("Add to Grid", color = Color.Black) }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                val isCurrentlyHidden = hiddenApps.any { it.packageName == appPendingAction?.packageName }
                                
                                if (isCurrentlyHidden) {
                                    Button(
                                        onClick = {
                                            appPendingAction?.let { onUnhideApp(it) }
                                            appPendingAction = null
                                            actionType = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) { Text("Unhide App", color = Color.White) }
                                } else {
                                    Button(
                                        onClick = {
                                            appPendingAction?.let { onHideApp(it) }
                                            appPendingAction = null
                                            actionType = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                                    ) { Text("Hide App", color = Color.White) }
                                }
                            }
                            // Open in Pop-Up
                            Button(
                                onClick = {
                                    appPendingAction?.let { onLaunchPopup(it) }
                                    appPendingAction = null
                                    actionType = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Floating Window", color = Color.White) }
                            
                            // Lock / Unlock toggle
                            val isCurrentlyLocked = appPendingAction?.packageName?.let { lockedApps.contains(it) } ?: false
                            Button(
                                onClick = {
                                    appPendingAction?.let {
                                        if (isCurrentlyLocked) onUnlockApp(it) else onLockApp(it)
                                    }
                                    appPendingAction = null
                                    actionType = null
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCurrentlyLocked) Color(0xFF6B21A8) else Color(0xFF7E22CE)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (isCurrentlyLocked) "🔓 Unlock App" else "🔒 Lock App",
                                    color = Color.White
                                )
                            }

                             // Standard Uninstall (no Shizuku needed — uses system prompt)
                            val uninstallContext = LocalContext.current
                            Button(
                                onClick = {
                                    appPendingAction?.let { app ->
                                        val intent = android.content.Intent(android.content.Intent.ACTION_DELETE).apply {
                                            data = android.net.Uri.parse("package:${app.packageName}")
                                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        uninstallContext.startActivity(intent)
                                    }
                                    appPendingAction = null
                                    actionType = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🗑️ Uninstall App", color = Color.White)
                            }

                            // App Info + Share row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // App Info — opens system App Settings detail page
                                Button(
                                    onClick = {
                                        appPendingAction?.let { app ->
                                            val intent = android.content.Intent(
                                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                            ).apply {
                                                data = android.net.Uri.parse("package:${app.packageName}")
                                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            uninstallContext.startActivity(intent)
                                        }
                                        appPendingAction = null
                                        actionType = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                ) {
                                    Text("ℹ️ App Info", fontSize = 12.sp, color = Color.White)
                                }

                                // Share App — shares the actual installed APK file
                                Button(
                                    onClick = {
                                        appPendingAction?.let { app ->
                                            try {
                                                // Get the installed APK source path
                                                val appInfo = uninstallContext.packageManager
                                                    .getApplicationInfo(app.packageName, 0)
                                                val apkSource = java.io.File(appInfo.sourceDir)

                                                // Copy APK to external cache so FileProvider can serve it
                                                val destDir = uninstallContext.externalCacheDir
                                                    ?: uninstallContext.cacheDir
                                                val destFile = java.io.File(destDir, "${app.label}.apk")
                                                apkSource.copyTo(destFile, overwrite = true)

                                                // Build a content:// URI via FileProvider (required Android 7+)
                                                val apkUri = androidx.core.content.FileProvider.getUriForFile(
                                                    uninstallContext,
                                                    "${uninstallContext.packageName}.fileprovider",
                                                    destFile
                                                )

                                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                    type = "application/vnd.android.package-archive"
                                                    putExtra(android.content.Intent.EXTRA_STREAM, apkUri)
                                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "${app.label}.apk")
                                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                uninstallContext.startActivity(
                                                    android.content.Intent.createChooser(shareIntent, "Share ${app.label} APK via")
                                                        .apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                                                )
                                            } catch (e: Exception) {
                                                android.widget.Toast.makeText(
                                                    uninstallContext,
                                                    "Could not share APK: ${e.message}",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                        appPendingAction = null
                                        actionType = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0369A1)),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                ) {
                                    Text("↗️ Share APK", fontSize = 12.sp, color = Color.White)
                                }
                            }

                            // Shizuku Power Actions
                            if (shizukuState == com.vertigo.launcher.utils.ShizukuSetup.ShizukuState.AUTHORIZED) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "POWER ACTIONS (SHIZUKU)",
                                    color = Color.Red,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isFrozen = frozenApps.contains(appPendingAction?.packageName)
                                    Button(
                                        onClick = {
                                            appPendingAction?.let { 
                                                if (isFrozen) onUnfreezeApp(it.packageName) else onFreezeApp(it.packageName)
                                            }
                                            appPendingAction = null
                                            actionType = null
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isFrozen) Color(0xFF10B981) else Color(0xFF475569)
                                        ),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                    ) {
                                        Text(if (isFrozen) "🔥 Unfreeze" else "❄️ Freeze", fontSize = 11.sp, color = Color.White)
                                    }
                                    
                                    Button(
                                        onClick = {
                                            appPendingAction?.let { onForceStopApp(it.packageName) }
                                            appPendingAction = null
                                            actionType = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B)),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                    ) {
                                        Text("💀 Kill", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            appPendingAction?.let { onClearAppData(it.packageName) }
                                            appPendingAction = null
                                            actionType = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                    ) {
                                        Text("🗑️ Clear Data", fontSize = 11.sp, color = Color.White)
                                    }
                                    
                                    Button(
                                        onClick = {
                                            appPendingAction?.let { onSilentUninstallApp(it.packageName) }
                                            appPendingAction = null
                                            actionType = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                    ) {
                                        Text("❌ Uninstall", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                            
                            // Phase 14: App Shortcuts
                            if (shortcuts.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "QUICK ACTIONS",
                                    color = Color.Cyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 200.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    shortcuts.forEach { shortcut ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { onShortcutClick(shortcut); appPendingAction = null; actionType = null }
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (shortcut.icon != null) {
                                                AndroidView(
                                                    factory = { ctx ->
                                                        android.widget.ImageView(ctx).apply {
                                                            setImageDrawable(shortcut.icon)
                                                        }
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                            }
                                            Text(
                                                text = shortcut.label,
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Phase 13: Add to Folder
                            Button(
                                onClick = {
                                    actionType = "ADD_TO_FOLDER"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("📁 Add to Folder", color = Color.White)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Onion Layers Management
                            Button(
                                onClick = {
                                    actionType = "MANAGE_ONION_LAYERS"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, themeAccentColor.copy(alpha = 0.5f))
                            ) {
                                Text("⚙️ Sector Allocation", color = themeAccentColor)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            appPendingAction = null
                            actionType = null
                            onClearShortcuts()
                        }) { Text("Cancel", color = Color.Gray) }
                    },
                    containerColor = Color(0xFF1E293B),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                )
            }
        }
        // Onion Layers Management Dialog
        if (actionType == "MANAGE_ONION_LAYERS") {
            val app = appPendingAction
            if (app != null) {
                AlertDialog(
                    onDismissRequest = { 
                        appPendingAction = null
                        actionType = null
                    },
                    title = { Text("SECTOR ALLOCATION", color = themeAccentColor, fontWeight = FontWeight.Black) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "Assign console sectors for ${app.label.uppercase()}:",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            if (hiddenLayers.isEmpty()) {
                                Text(
                                    "No custom sectors found. Open system console and tap ＋ to create sectors first.",
                                    color = Color.LightGray,
                                    fontSize = 14.sp
                                )
                            } else {
                                hiddenLayers.forEach { (layerName, pkgs) ->
                                    val isAppInLayer = pkgs.contains(app.packageName)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isAppInLayer) Color(0x1F00F0FF) else Color(0x0AFFFFFF))
                                            .clickable {
                                                if (isAppInLayer) {
                                                    viewModel?.removeAppFromHiddenLayer(layerName, app.packageName)
                                                } else {
                                                    viewModel?.addAppToHiddenLayer(layerName, app.packageName)
                                                }
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        androidx.compose.material3.Checkbox(
                                            checked = isAppInLayer,
                                            onCheckedChange = null,
                                            colors = androidx.compose.material3.CheckboxDefaults.colors(
                                                checkedColor = themeAccentColor,
                                                uncheckedColor = Color.Gray
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = layerName,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                appPendingAction = null
                                actionType = null
                            }
                        ) { Text("DONE", color = themeAccentColor, fontWeight = FontWeight.Bold) }
                    },
                    containerColor = Color(0xFF0F172A),
                    shape = RoundedCornerShape(24.dp)
                )
            }
        }
        
        // Add to Folder Dialog (Phase 13)
        if (actionType == "ADD_TO_FOLDER") {
            var folderName by remember { androidx.compose.runtime.mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { actionType = null; appPendingAction = null },
                title = { Text("Add to Folder", color = Color.White) },
                text = {
                    Column {
                        Text("Enter folder name:", color = Color.LightGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.TextField(
                            value = folderName,
                            onValueChange = { folderName = it },
                            placeholder = { Text("Cyber-Folder-01", color = Color.Gray) },
                            singleLine = true,
                            colors = androidx.compose.material3.TextFieldDefaults.colors(
                                focusedContainerColor = Color(0x33FFFFFF),
                                unfocusedContainerColor = Color(0x1AFFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (folderName.isNotBlank() && appPendingAction != null) {
                            onAddAppToFolder(folderName, appPendingAction!!.packageName)
                        }
                        actionType = null
                        appPendingAction = null
                    }) { Text("ADD", color = themeAccentColor) }
                },
                dismissButton = {
                    TextButton(onClick = { actionType = null; appPendingAction = null }) { 
                        Text("CANCEL", color = Color.Gray) 
                    }
                },
                containerColor = Color(0xFF1E293B)
            )
        }

        // Privacy Shield overlay
        lockedAppPending?.let { appToUnlock ->
            PrivacyShieldPrompt(
                app = appToUnlock,
                onUnlocked = {
                    onAppClick(appToUnlock)
                    lockedAppPending = null
                },
                onDismiss = { lockedAppPending = null }
            )
        }
        
        // Folder Overlay (Phase 13)
        activeFolder?.let { folderName ->
            val folderPackageNames = folders[folderName] ?: emptySet()
            val folderApps = allApps.filter { folderPackageNames.contains(it.packageName) }
            FolderOverlay(
                name = folderName,
                apps = folderApps,
                onAppClick = { app ->
                    onAppClick(app)
                    activeFolder = null
                },
                onClose = { activeFolder = null },
                themeAccentColor = themeAccentColor
            )
        }
        // Blur Animation for Home Screen Desktop
        val isOverlayOpen = showDrawer || showNeuralHub || showInlineSearch || showHiddenDrawer || (lockedAppPending != null) || (actionType != null) || showScribbleSearch || showEdgePanel
        val blurRadius by animateDpAsState(
            targetValue = if (isOverlayOpen) 16.dp else 0.dp,
            animationSpec = tween(durationMillis = 300)
        )
        val desktopModifier = Modifier
            .blur(blurRadius)
            .pointerInput(pinUnlocked, scribbleSearchEnabled) {
                detectTapGestures(
                    onDoubleTap = {
                        val isBaseProtected = viewModel?.isOnionLayerProtected(0) ?: true
                        if (pinUnlocked || !isBaseProtected) {
                            pinUnlocked = true
                            currentDepth = 0
                            showHiddenDrawer = true
                        } else {
                            showBiometricAuth = true
                        }
                    },
                    onLongPress = {
                        if (scribbleSearchEnabled) {
                            showScribbleSearch = true
                        }
                    }
                )
            }

        if (isLandscape) {
            Row(
                modifier = desktopModifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing) // Fix System UI interference
            ) {
                // Left Panel: Clock and Date
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val holiday by viewModel?.currentHoliday?.collectAsState() ?: mutableStateOf(null)
                    ClockWidget(modifier = Modifier.size(200.dp), holiday = holiday, clockColors = clockColors)
                    WeatherWidget(state = weatherState)
                    Spacer(modifier = Modifier.height(32.dp))
                    VoiceAssistantWidget(
                        isEnabled = isVoiceEnabled, 
                        isListening = isVoiceListening, 
                        onClick = onVoiceClick,
                        chatHistory = chatHistory,
                        currentStreamingResponse = currentStreamingResponse,
                        isAiThinking = isAiThinking,
                        spokenText = spokenText,
                        isHotwordActive = isHotwordActive,
                        onClearResponse = onClearAiResponse,
                        onSendText = onSendAiText,
                        onStopAi = onStopAiText,
                        onCameraClick = onCameraClick,
                        getPhotoBitmap = { ts -> viewModel?.getPhotoBitmap(ts) }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Search Bar in Landscape
                    InlineSearchBar(
                        isVoiceListening = isVoiceListening,
                        onSearchClick = { showInlineSearch = true }
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    Dock(
                        onSettings = onSettings,
                        onDrawer = { onDrawerToggle(true) },
                        onNeuralHub = { onNeuralHubToggle(true) }
                    )
                }
                
                // Right Panel: Flower Grid
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    FlowerGrid(
                        apps = flowerApps,
                        onAppClick = onAppClick,
                        onAppLongClick = { app ->
                            appPendingAction = app
                            actionType = "REMOVE"
                        },
                        notificationCounts = notificationCounts,
                        showLabels = showLabels,
                        showBadges = showBadges
                    )
                }
            }
        } else {
                // Portrait Layout — Flow-based with zIndex for terminal foreground
                BoxWithConstraints(
                    modifier = desktopModifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    // Dynamic max height: terminal can grow from its natural position
                    // down to 80% of screen, leaving 20% for search bar + dock
                    val totalHeight = maxHeight
                    // Large/tall screen detection for responsive scaling
                    val isLargeScreen = totalHeight > 700.dp
                    // On large screens, give less to Section 1, more to the grid
                    val section1Weight = if (isLargeScreen) 0.35f else 0.42f
                    val section2Weight = if (isLargeScreen) 0.45f else 0.38f
                    // Subtract ~150dp for widget chrome (header, input, padding, voice btn)
                    // so the TOTAL widget height stays within bounds, not just the LazyColumn
                    val calculatedMaxHeight = (totalHeight * section2Weight) - 150.dp

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Section 1: Clock + Weather + Terminal
                        // zIndex(1f) makes this ENTIRE section draw ON TOP of Section 2
                        // so the terminal overlay is never hidden behind the grid
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(section1Weight)
                                .zIndex(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            // MUST be Top so terminal grows downward from fixed position
                            // Center would push expanded terminal further past the search bar
                            verticalArrangement = Arrangement.Top
                        ) {
                            val screenWidthDp = configuration.screenWidthDp.dp
                            // On large screens, make the clock HUGE to fill the top space
                            val clockSize = if (isLargeScreen)
                                (screenWidthDp * 0.50f).coerceIn(160.dp, 220.dp)
                            else
                                (screenWidthDp * 0.30f).coerceIn(80.dp, 140.dp)
                            // Scale factor for weather widget
                            val weatherScale = if (isLargeScreen) 1.5f else 1f
                            val holiday by viewModel?.currentHoliday?.collectAsState() ?: mutableStateOf(null)
                            ClockWidget(modifier = Modifier.size(clockSize), holiday = holiday, clockColors = clockColors)
                            WeatherWidget(state = weatherState, scaleFactor = weatherScale)

                            Spacer(modifier = Modifier.height(4.dp))

                            // Terminal: always fully visible (unbounded=true)
                            // When idle it's compact (just input bar), slight grid overlap is fine
                            // When active, maxHeightOverride prevents crossing the search bar
                            Box(
                                modifier = Modifier
                                    .wrapContentHeight(
                                        align = Alignment.Top,
                                        unbounded = true
                                    )
                                    .graphicsLayer { shadowElevation = 10f }
                            ) {
                                VoiceAssistantWidget(
                                    isEnabled = isVoiceEnabled,
                                    isListening = isVoiceListening,
                                    onClick = onVoiceClick,
                                    chatHistory = chatHistory,
                                    currentStreamingResponse = currentStreamingResponse,
                                    isAiThinking = isAiThinking,
                                    spokenText = spokenText,
                                    isHotwordActive = isHotwordActive,
                                    onClearResponse = onClearAiResponse,
                                    onSendText = onSendAiText,
                                    onStopAi = onStopAiText,
                                    onCameraClick = onCameraClick,
                                    getPhotoBitmap = { ts -> viewModel?.getPhotoBitmap(ts) },
                                    maxHeightOverride = calculatedMaxHeight.coerceAtLeast(0.dp)
                                )
                            }
                        }

                        // Section 2: Flower Grid (dynamic weight)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(section2Weight)
                                .clipToBounds(),
                            contentAlignment = Alignment.Center
                        ) {
                            FlowerGrid(
                                apps = flowerApps,
                                onAppClick = onAppClick,
                                onAppLongClick = { app ->
                                    appPendingAction = app
                                    actionType = "REMOVE"
                                },
                                notificationCounts = notificationCounts,
                                showLabels = showLabels,
                                showBadges = showBadges
                            )
                        }

                        // Section 3: Search + Dock (20%)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.20f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            InlineSearchBar(
                                isVoiceListening = isVoiceListening,
                                onSearchClick = { showInlineSearch = true }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Dock(
                                onSettings = onSettings,
                                onDrawer = { onDrawerToggle(true) },
                                onNeuralHub = { onNeuralHubToggle(true) }
                            )
                        }
                    }
                }
        }
        
        // --- Hidden Drawer Onion Layers UI ---
        var showCreateLayerDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
        var showStealthLayerOptions by remember { androidx.compose.runtime.mutableStateOf(false) }
        var newLayerNameInput by remember { androidx.compose.runtime.mutableStateOf("") }
        var newLayerLocked by remember { androidx.compose.runtime.mutableStateOf(true) }
        // PIN dialog for diving into a protected deeper layer
        var pendingProtectedDepth by remember { androidx.compose.runtime.mutableIntStateOf(-1) }
        // Observe protected layers so recomposition happens on toggle
        @Suppress("UNUSED_VARIABLE")
        val protectedLayersState by viewModel?.protectedLayers?.collectAsState() ?: remember { mutableStateOf(emptySet<String>()) }

        // PIN Dialog side-effect integration
        // Biometric dialog for opening hidden drawer
        if (showBiometricAuth) {
            val ctx = LocalContext.current
            androidx.compose.runtime.LaunchedEffect(showBiometricAuth) {
                launchBiometricPrompt(
                    context = ctx,
                    title = "System Authentication",
                    subtitle = "Confirm device ownership to proceed",
                    onSuccess = {
                        pinUnlocked = true
                        showHiddenDrawer = true
                        currentDepth = 0
                        showBiometricAuth = false
                    },
                    onFail = {
                        showBiometricAuth = false
                    }
                )
            }
        }

        // Biometric dialog for peeling into a protected deeper layer
        if (pendingProtectedDepth >= 0) {
            val ctx = LocalContext.current
            androidx.compose.runtime.LaunchedEffect(pendingProtectedDepth) {
                launchBiometricPrompt(
                    context = ctx,
                    title = "System Verification",
                    subtitle = "Confirm device ownership to proceed",
                    onSuccess = {
                        currentDepth = pendingProtectedDepth
                        pendingProtectedDepth = -1
                    },
                    onFail = {
                        pendingProtectedDepth = -1
                    }
                )
            }
        }

        // Dialog to create a new Onion Layer
        if (showCreateLayerDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showCreateLayerDialog = false; newLayerNameInput = ""; newLayerLocked = true },
                title = { androidx.compose.material3.Text("NEW SYSTEM INSTANCE", color = themeAccentColor, fontWeight = FontWeight.Black) },
                text = {
                    Column {
                        androidx.compose.material3.OutlinedTextField(
                            value = newLayerNameInput,
                            onValueChange = { newLayerNameInput = it },
                            label = { androidx.compose.material3.Text("Instance key", color = androidx.compose.ui.graphics.Color.Gray) },
                            singleLine = true,
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeAccentColor,
                                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Gray,
                                focusedTextColor = androidx.compose.ui.graphics.Color.White,
                                unfocusedTextColor = androidx.compose.ui.graphics.Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { newLayerLocked = !newLayerLocked }
                                .background(
                                    if (newLayerLocked) androidx.compose.ui.graphics.Color(0x1A00F0FF)
                                    else androidx.compose.ui.graphics.Color(0x1AFFFFFF),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = if (newLayerLocked) Icons.Default.Lock else Icons.Default.Lock,
                                contentDescription = "Lock",
                                tint = if (newLayerLocked) themeAccentColor else androidx.compose.ui.graphics.Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (newLayerLocked) "Secure Instance — Auth Required" else "Standard Instance — Public",
                                color = if (newLayerLocked) themeAccentColor else androidx.compose.ui.graphics.Color.Gray,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            androidx.compose.material3.Switch(
                                checked = newLayerLocked,
                                onCheckedChange = { newLayerLocked = it },
                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                    checkedThumbColor = themeAccentColor,
                                    checkedTrackColor = themeAccentColor.copy(alpha = 0.3f),
                                    uncheckedThumbColor = androidx.compose.ui.graphics.Color.Gray,
                                    uncheckedTrackColor = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                },
                confirmButton = {
                    androidx.compose.material3.Button(
                        onClick = {
                            val name = newLayerNameInput.trim()
                            if (name.isNotEmpty()) {
                                onCreateLayer(name, newLayerLocked)
                                newLayerNameInput = ""
                                newLayerLocked = true
                                showCreateLayerDialog = false
                                // Immediately peel into this newly created layer!
                                currentDepth++
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeAccentColor)
                    ) { androidx.compose.material3.Text("CREATE", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showCreateLayerDialog = false; newLayerNameInput = ""; newLayerLocked = true }) {
                        androidx.compose.material3.Text("CANCEL", color = androidx.compose.ui.graphics.Color.Gray)
                    }
                },
                containerColor = androidx.compose.ui.graphics.Color(0xFF0F172A),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
            )
        }

        // Stealth Options configuration dialog
        if (showStealthLayerOptions) {
            val isCurrentLayerProtected = viewModel?.isOnionLayerProtected(currentDepth) ?: false
            val layerKey = viewModel?.getOnionLayerKey(currentDepth)
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showStealthLayerOptions = false },
                title = { androidx.compose.material3.Text("SYSTEM CONFIGURATION", color = themeAccentColor, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.Text("Configure core settings for Sector $currentDepth:", color = androidx.compose.ui.graphics.Color.Gray)
                        
                        // Toggle security shield
                        androidx.compose.material3.Button(
                            onClick = {
                                viewModel?.setOnionLayerProtected(currentDepth, !isCurrentLayerProtected)
                                showStealthLayerOptions = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCurrentLayerProtected) androidx.compose.ui.graphics.Color(0xFF6B21A8) else themeAccentColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            androidx.compose.material3.Text(
                                text = if (isCurrentLayerProtected) "Disable Security Shield" else "Enable Security Shield",
                                color = if (isCurrentLayerProtected) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Black
                            )
                        }
                        
                        // Terminate Sector (Delete Compartment, only if currentDepth > 0)
                        if (currentDepth > 0 && layerKey != null) {
                            androidx.compose.material3.Button(
                                onClick = {
                                    onDeleteLayer(layerKey)
                                    showStealthLayerOptions = false
                                    currentDepth--
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFFFF006E)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                androidx.compose.material3.Text("Terminate Sector", color = androidx.compose.ui.graphics.Color.White)
                            }
                        }
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = { showStealthLayerOptions = false }) {
                        androidx.compose.material3.Text("CLOSE", color = themeAccentColor)
                    }
                },
                containerColor = androidx.compose.ui.graphics.Color(0xFF0F172A),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
            )
        }

        // Hidden Drawer animated wrapper
        AnimatedVisibility(
            visible = showHiddenDrawer,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
            ) + fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color(0xFF0B0F19))
            ) {
                val layerApps = viewModel?.getOnionLayerApps(currentDepth) ?: emptyList()
                val layerName = viewModel?.getOnionLayerName(currentDepth) ?: "System Storage"
                val maxDepth = viewModel?.getMaxOnionDepth() ?: 0

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .pointerInput(currentDepth, maxDepth) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (currentDepth < maxDepth) {
                                        val nextDepth = currentDepth + 1
                                        val isNextProtected = viewModel?.isOnionLayerProtected(nextDepth) ?: false
                                        if (isNextProtected) {
                                            pendingProtectedDepth = nextDepth
                                        } else {
                                            currentDepth = nextDepth
                                        }
                                    } else {
                                        showCreateLayerDialog = true
                                    }
                                }
                            )
                        }
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(androidx.compose.ui.graphics.Color(0x3300F0FF))
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back Button (Peel Outward)
                        androidx.compose.material3.IconButton(
                            onClick = {
                                if (currentDepth > 0) {
                                    currentDepth--
                                } else {
                                    showHiddenDrawer = false
                                    pinUnlocked = false
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(androidx.compose.ui.graphics.Color(0x1DFFFFFF), CircleShape)
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = themeAccentColor
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .combinedClickable(
                                    onLongClick = { showStealthLayerOptions = true },
                                    onClick = {}
                                )
                        ) {
                            Text(
                                text = layerName.uppercase(),
                                color = themeAccentColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "CORE SERVICE: ACTIVE | SECTOR: $currentDepth",
                                color = androidx.compose.ui.graphics.Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                        // Lock/Unlock toggle
                        val isCurrentLayerProtected = viewModel?.isOnionLayerProtected(currentDepth) ?: false
                        androidx.compose.material3.IconButton(
                            onClick = {
                                viewModel?.setOnionLayerProtected(currentDepth, !isCurrentLayerProtected)
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (isCurrentLayerProtected) androidx.compose.ui.graphics.Color(0x1A00F0FF)
                                    else androidx.compose.ui.graphics.Color(0x1AFFFFFF),
                                    CircleShape
                                )
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = if (isCurrentLayerProtected) "Locked" else "Unlocked",
                                tint = if (isCurrentLayerProtected) themeAccentColor
                                    else androidx.compose.ui.graphics.Color.Gray
                            )
                        }
                    }
                    
                    androidx.compose.material3.HorizontalDivider(
                        color = themeAccentColor.copy(alpha = 0.3f),
                        thickness = 1.dp
                    )

                    if (layerApps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⚙️", fontSize = 48.sp)
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "No system logs recorded",
                                    color = androidx.compose.ui.graphics.Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "System is running in safe mode. No diagnostic entries have been logged to this console sector.",
                                    color = androidx.compose.ui.graphics.Color.Gray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 72.dp),
                            contentPadding = PaddingValues(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(layerApps, key = { it.packageName }) { app ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .combinedClickable(
                                            onClick = {
                                                if (lockedApps.contains(app.packageName)) {
                                                    lockedAppPending = app
                                                } else {
                                                    onAppClick(app)
                                                }
                                            },
                                            onLongClick = {
                                                appPendingAction = app
                                                actionType = "DRAWER_OPTIONS"
                                            }
                                        )
                                ) {
                                    Box(contentAlignment = Alignment.TopEnd) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(androidx.compose.ui.graphics.Color(0x3300F0FF))
                                                .border(1.dp, themeAccentColor.copy(alpha = 0.3f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncAppIcon(
                                                app = app,
                                                modifier = Modifier.size(40.dp)
                                            )
                                        }
                                        val count = if (showBadges) (notificationCounts[app.packageName] ?: 0) else 0
                                        if (count > 0) {
                                            NotificationDot(count = count)
                                        }

                                    }
                                    if (showLabels) {
                                        Text(
                                            text = app.label,
                                            color = androidx.compose.ui.graphics.Color.White,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // App Drawer Overlay
        AnimatedVisibility(
            visible = showDrawer,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
            ) + fadeOut(animationSpec = tween(300))
        ) {
            AppDrawer(
                apps = allApps,
                onAppClick = { app ->
                    if (lockedApps.contains(app.packageName)) {
                        lockedAppPending = app
                    } else {
                        onAppClick(app)
                    }
                },
                notificationCounts = notificationCounts,
                onClose = { onDrawerToggle(false) },
                onAppLongClick = { app ->
                    appPendingAction = app
                    actionType = "DRAWER_OPTIONS"
                    onLoadShortcuts(app.packageName)
                },
                folders = folders,
                onFolderClick = { activeFolder = it },
                onDeleteFolder = onDeleteFolder,
                viewModel = viewModel,
                showLabels = showLabels,
                showBadges = showBadges,
                gridSize = gridSize,
                themeAccentColor = themeAccentColor,
                isSelectionMode = isSelectionMode,
                selectedPackages = selectedPackages,
                onToggleSelection = onToggleSelection,
                onEnterSelectionMode = onEnterSelectionMode,
                onBatchHide = onBatchHide,
                onBatchFreeze = onBatchFreeze,
                onBatchUnhide = onBatchUnhide,
                onBatchUnfreeze = onBatchUnfreeze,
                onBatchUninstall = { viewModel?.batchUninstallApps() },
                onBatchAddToGrid = { viewModel?.batchAddToGrid() },
                onClearSelection = onClearSelection,
                shizukuState = shizukuState,
                frozenApps = frozenApps
            )
        }
        
        // Neural Hub Overlay
        AnimatedVisibility(
            visible = showNeuralHub,
            enter = scaleIn(
                initialScale = 0.9f, 
                animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(animationSpec = tween(300)),
            exit = scaleOut(
                targetScale = 0.9f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeOut(animationSpec = tween(300))
        ) {
            NeuralHub(
                state = neuralHubState,
                musicState = musicState,
                onClose = { onNeuralHubToggle(false) },
                onMusicPlayPause = onMusicPlayPause,
                onMusicNext = onMusicNext,
                onMusicPrev = onMusicPrev,
                cpuHistory = cpuHistory,
                neuralInsight = neuralInsight,
                viewModel = viewModel,
                shizukuState = shizukuState
            )
        }
        
        // NEW: Inline Search Overlay (Rendered at Root)
        AnimatedVisibility(
            visible = showInlineSearch,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
            ) + fadeOut(animationSpec = tween(300))
        ) {
            SearchOverlay(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                searchResults = searchResults,
                onResultClick = onSearchResultClick,
                onResultLongClick = { result ->
                    if (result.type == com.vertigo.launcher.utils.SearchManager.ResultType.APP) {
                        val matchingApp = allApps.find { it.packageName == result.id } ?: hiddenApps.find { it.packageName == result.id }
                        if (matchingApp != null) {
                            appPendingAction = matchingApp
                            actionType = "DRAWER_OPTIONS"
                        }
                    }
                },
                onClose = { showInlineSearch = false },
                isSearching = isSearching
            )
        }
        
        // Edge Panel Overlay
        EdgePanel(
            isVisible = showEdgePanel,
            onClose = { showEdgePanel = false }
        )

        // NEW: Scribble Search Overlay
        AnimatedVisibility(
            visible = showScribbleSearch,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            ScribbleSearchOverlay(
                apps = allApps,
                onAppClick = onAppClick,
                onClose = { showScribbleSearch = false },
                themeAccentColor = themeAccentColor,
                glassmorphismEnabled = glassmorphismEnabled
            )
        }
        
    }
}

@Composable
fun ClockWidget(
    modifier: Modifier = Modifier,
    holiday: String? = null,
    clockColors: Triple<Int, Int, Int> = Triple(0xFF00F0FF.toInt(), 0xFFFF006E.toInt(), 0xFFBD00FF.toInt())
) {
    var currentTime by remember { androidx.compose.runtime.mutableStateOf(System.currentTimeMillis()) }
    
    // Ticker to update time every second
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }

    AndroidView(
        factory = { context ->
            CyberClockView(context).apply {
                setTime(currentTime)
                setHoliday(holiday)
                setColors(clockColors.first, clockColors.second, clockColors.third)
            }
        },
        update = { view ->
            view.setTime(currentTime)
            view.setHoliday(holiday)
            view.setColors(clockColors.first, clockColors.second, clockColors.third)
        },
        modifier = modifier
    )
}

@Composable
fun FlowerGrid(
    apps: List<AppModel>,
    onAppClick: (AppModel) -> Unit,
    onAppLongClick: (AppModel) -> Unit,
    notificationCounts: Map<String, Int> = emptyMap(),
    showLabels: Boolean = true,
    showBadges: Boolean = true
) {
    BoxWithConstraints(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier.fillMaxSize()
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val minDimPx = if (widthPx < heightPx) widthPx else heightPx
        val densityValue = density.density
        
        // Adaptive Logic
        val isLargeScreen = (minDimPx / densityValue) > 600
        val targetRings = if (isLargeScreen) 4 else 3
        val maxApps = if (targetRings == 4) 61 else 37
        
        val spacingFactor = 1.4f
        val maxIconSizePx = (minDimPx / 2f) / (spacingFactor * targetRings.toFloat() + 0.5f)
        
        val iconSizeDp = with(density) {
            val calcSize = (maxIconSizePx / densityValue).dp
            if (calcSize < 40.dp) 40.dp else if (calcSize > 80.dp) 80.dp else calcSize
        }
        
        val iconSizePx = with(density) { iconSizeDp.toPx() }
        val spacing = iconSizePx * spacingFactor
        Layout(
            content = {
                apps.take(maxApps).forEach { app ->
                     key(app.packageName) {
                        val count = if (showBadges) (notificationCounts[app.packageName] ?: 0) else 0
                        AppIcon(app, onAppClick, onAppLongClick, iconSizeDp, count, showLabels)
                    }
                }
            },
            measurePolicy = { measurables, lConstraints ->
            val placeables = measurables.map { it.measure(lConstraints) }
            val cx = lConstraints.maxWidth / 2f
            // Anchor the pattern toward the bottom of the available space.
            // The offset pushes the center point down so apps cluster from below.
            val cy = lConstraints.maxHeight - (lConstraints.maxHeight * 0.35f)
            
            layout(lConstraints.maxWidth, lConstraints.maxHeight) {
                if (placeables.isNotEmpty()) {
                    // Center Item
                    val center = placeables[0]
                    center.placeRelative((cx - center.width / 2).toInt(), (cy - center.height / 2).toInt())
                }
                
                var vectorIdx = 1
                var ring = 1
                
                while (vectorIdx < placeables.size) {
                    for (side in 0 until 6) {
                        for (step in 0 until ring) {
                            if (vectorIdx >= placeables.size) break
                            
                            val angle1 = Math.toRadians(-90.0 + 60 * side)
                            val angle2 = Math.toRadians(-90.0 + 60 * (side + 1))
                            
                            val radius = ring.toFloat() * spacing
                            
                            val x1 = (radius * cos(angle1)).toFloat()
                            val y1 = (radius * sin(angle1)).toFloat()
                            val x2 = (radius * cos(angle2)).toFloat()
                            val y2 = (radius * sin(angle2)).toFloat()
                            
                            val fraction = step.toFloat() / ring.toFloat()
                            val px = x1 + (x2 - x1) * fraction
                            val py = y1 + (y2 - y1) * fraction
                            
                            val placeable = placeables[vectorIdx]
                            val lx = (cx + px - (placeable.width / 2f)).toInt()
                            val ly = (cy + py - (placeable.height / 2f)).toInt()
                            
                            placeable.placeRelative(lx, ly)
                            vectorIdx++
                        }
                    }
                    ring++
                }
            }
        })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AsyncAppIcon(
    app: AppModel,
    modifier: Modifier = Modifier
) {
    var drawable by remember(app.packageName) {
        mutableStateOf<android.graphics.drawable.Drawable?>(
            com.vertigo.launcher.utils.PerformanceOptimizer.getIconIfCached(app.packageName)
        )
    }

    LaunchedEffect(app.packageName) {
        if (drawable == null) {
            withContext(Dispatchers.IO) {
                try {
                    drawable = app.icon
                } catch (e: Exception) {
                    android.util.Log.e("AsyncAppIcon", "Failed to load icon for ${app.packageName}", e)
                }
            }
        }
    }

    if (drawable != null) {
        AndroidView(
            factory = { ctx ->
                android.widget.ImageView(ctx).apply {
                    scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                }
            },
            update = { imageView ->
                imageView.setImageDrawable(drawable)
            },
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIcon(
    app: AppModel,
    onClick: (AppModel) -> Unit,
    onLongClick: (AppModel) -> Unit,
    size: androidx.compose.ui.unit.Dp = 64.dp,
    notificationCount: Int = 0,
    showLabel: Boolean = true
) {
    Column(
        modifier = Modifier.width(size),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(size * 0.9f)
                    .clip(CircleShape)
                    .background(Color(0x3300F0FF))
                    .combinedClickable(
                        onClick = { onClick(app) },
                        onLongClick = { onLongClick(app) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                AsyncAppIcon(
                    app = app,
                    modifier = Modifier.size(size * 0.65f)
                )
            }
            
            if (notificationCount > 0) {
                NotificationDot(
                    count = notificationCount,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 2.dp, top = 2.dp)
                )
            }
        }
        
        if (showLabel) {
            Text(
                text = app.label,
                color = Color.White,
                fontSize = (size.value * 0.18f).coerceAtMost(14f).sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun SearchBar(
    isListening: Boolean,
    onSearchClick: () -> Unit,
    themeAccentColor: Color = Color(0xFF00F0FF)
) {
    val borderColor = if (isListening) themeAccentColor else Color.Transparent
    val borderWidth = if (isListening) 2.dp else 0.dp
    val textColor = if (isListening) themeAccentColor else Color.White.copy(alpha = 0.7f)
    val text = if (isListening) "Listening..." else "Search apps, web, & more..."
    val icon = if (isListening) android.R.drawable.ic_btn_speak_now else android.R.drawable.ic_menu_search
    val iconTint = if (isListening) themeAccentColor else Color.White.copy(alpha = 0.7f)
    
    // Debug logging removed to optimize performance


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .height(48.dp)
            .border(borderWidth, borderColor, CircleShape)
            .background(Color(0x33FFFFFF), CircleShape)
            .clickable(onClick = { 
                onSearchClick()
            }),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = "Search",
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = textColor
            )
        }
    }
}

@Composable
fun Dock(onSettings: () -> Unit, onDrawer: () -> Unit, onNeuralHub: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        DockItem(R.drawable.ic_category_settings, onSettings)
        DockItem(R.drawable.launcher, onNeuralHub) // Neural Hub
        DockItem(R.drawable.ic_category_home, onDrawer)
    }
}

@Composable
fun DockItem(iconRes: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Color(0x4D000000))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun rememberParallaxOffset(): State<Offset> {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    val offset = remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    // X and Y are inverted on screen, applying subtle clamping (x * 4, y * 4) makes it "a bit static"
                    val pitch = event.values[1] // Tilt up/down
                    val roll = event.values[0]  // Tilt left/right
                    offset.value = Offset(roll * 8f, pitch * 8f)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }
    return offset
}

@Composable
fun BatchSelectionBar(
    selectedCount: Int,
    onHide: () -> Unit,
    onFreeze: () -> Unit,
    onUnhide: () -> Unit,
    onUnfreeze: () -> Unit,
    onUninstall: () -> Unit,
    onAddToGrid: () -> Unit,
    onClear: () -> Unit,
    shizukuEnabled: Boolean,
    themeAccentColor: Color = Color(0xFF00F0FF)
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .statusBarsPadding(),
        color = Color(0xCC0F172A),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, themeAccentColor.copy(alpha = 0.4f)),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "$selectedCount SELECTED",
                    color = themeAccentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "NEURAL BATCH MODE",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onHide() }) {
                    Text("👻", fontSize = 20.sp)
                    Text("Hide", color = Color.White, fontSize = 10.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onUnhide() }) {
                    Text("👁️", fontSize = 20.sp)
                    Text("Unhide", color = Color.White, fontSize = 10.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onAddToGrid() }) {
                    Text("📌", fontSize = 20.sp)
                    Text("Pin Grid", color = Color.White, fontSize = 10.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onUninstall() }) {
                    Text("🗑️", fontSize = 20.sp)
                    Text("Uninstall", color = Color.White, fontSize = 10.sp)
                }
                if (shizukuEnabled) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onFreeze() }) {
                        Text("❄️", fontSize = 20.sp)
                        Text("Freeze", color = Color.White, fontSize = 10.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onUnfreeze() }) {
                        Text("🔥", fontSize = 20.sp)
                        Text("Unfreeze", color = Color.White, fontSize = 10.sp)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClear() }) {
                    Text("❌", fontSize = 20.sp)
                    Text("Clear", color = Color.White, fontSize = 10.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawer(
    apps: List<AppModel>,
    onAppClick: (AppModel) -> Unit,
    onClose: () -> Unit,
    onAppLongClick: (AppModel) -> Unit,
    notificationCounts: Map<String, Int> = emptyMap(),
    folders: Map<String, Set<String>> = emptyMap(),
    onFolderClick: (String) -> Unit = {},
    onDeleteFolder: (String) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") viewModel: com.vertigo.launcher.ui.HomeViewModel? = null,
    showLabels: Boolean = true,
    showBadges: Boolean = true,
    gridSize: Int = 4,
    themeAccentColor: Color = Color(0xFF00F0FF),
    isSelectionMode: Boolean = false,
    selectedPackages: Set<String> = emptySet(),
    onToggleSelection: (String) -> Unit = {},
    onEnterSelectionMode: (String) -> Unit = {},
    onBatchHide: () -> Unit = {},
    onBatchFreeze: () -> Unit = {},
    onBatchUnhide: () -> Unit = {},
    onBatchUnfreeze: () -> Unit = {},
    onBatchUninstall: () -> Unit = {},
    onBatchAddToGrid: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    shizukuState: com.vertigo.launcher.utils.ShizukuSetup.ShizukuState = com.vertigo.launcher.utils.ShizukuSetup.ShizukuState.UNAVAILABLE,
    frozenApps: Set<String> = emptySet()
) {
    val appsInFolders = remember(folders) { folders.values.flatten().toSet() }
    val filteredApps = remember(apps, appsInFolders) { apps.filter { !appsInFolders.contains(it.packageName) } }
    val folderAppsMap = remember(folders, apps) {
        folders.mapValues { (_, packages) ->
            apps.filter { packages.contains(it.packageName) }
        }
    }

    Box(
    modifier = Modifier
        .fillMaxSize()
        .background(Color(0x66000000)) // Translucent glass scrim
        .clickable { onClose() }
        .systemBarsPadding()
) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = false) {}
                .background(Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                contentAlignment = Alignment.Center
            ) {
                 Box(
                     modifier = Modifier
                         .width(40.dp)
                         .height(4.dp)
                         .background(Color.Cyan, CircleShape)
                 )
            }

            if (isSelectionMode) {
                BatchSelectionBar(
                    selectedCount = selectedPackages.size,
                    onHide = onBatchHide,
                    onFreeze = onBatchFreeze,
                    onUnhide = onBatchUnhide,
                    onUnfreeze = onBatchUnfreeze,
                    onUninstall = onBatchUninstall,
                    onAddToGrid = onBatchAddToGrid,
                    onClear = onClearSelection,
                    shizukuEnabled = shizukuState == com.vertigo.launcher.utils.ShizukuSetup.ShizukuState.AUTHORIZED,
                    themeAccentColor = themeAccentColor
                )
            }
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridSize),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Phase 13: Render Folders
                folders.entries.forEach { (name, _) ->
                    item(key = "folder_$name") {
                        val folderApps = folderAppsMap[name] ?: emptyList()
                        val folderNotifCount = if (showBadges) (folderApps.sumOf { notificationCounts[it.packageName] ?: 0 }) else 0
                        FolderIcon(
                            name = name,
                            apps = folderApps,
                            onClick = { onFolderClick(name) },
                            onLongClick = { onDeleteFolder(name) },
                            notificationCount = folderNotifCount
                        )
                    }
                }

                // Render Apps (filter out those already in folders for cleaner look)
                items(filteredApps.size, key = { filteredApps[it].packageName }) { index ->
                    val app = filteredApps[index]
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(8.dp)
                            .combinedClickable(
                                onClick = { 
                                    if (isSelectionMode) onToggleSelection(app.packageName) else onAppClick(app) 
                                },
                                onLongClick = { 
                                    if (!isSelectionMode) onEnterSelectionMode(app.packageName) else onAppLongClick(app)
                                }
                            )
                            .then(
                                if (selectedPackages.contains(app.packageName)) {
                                    Modifier.border(2.dp, themeAccentColor, RoundedCornerShape(12.dp)).padding(4.dp)
                                } else Modifier
                            )
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            AsyncAppIcon(
                                app = app,
                                modifier = Modifier.size(56.dp)
                            )
                            // Notification Dot
                            val count = if (showBadges) (notificationCounts[app.packageName] ?: 0) else 0
                            if (count > 0) {
                                NotificationDot(count = count)
                            }
                            
                            // Frozen Indicator (Phase 15)
                            val isFrozen = frozenApps.contains(app.packageName)
                            if (isFrozen) {
                                Text(
                                    "❄️",
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .background(Color(0x80000000), CircleShape)
                                        .padding(2.dp)
                                )
                            }

                            // Selection Indicator (Fire emoji)
                            if (selectedPackages.contains(app.packageName)) {
                                Text(
                                    "🔥",
                                    fontSize = 16.sp,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 8.dp, y = (-8).dp)
                                )
                            }
                        }
                        if (showLabels) {
                            Text(
                                text = app.label,
                                color = Color.White,
                                fontSize = 12.sp,
                                maxLines = 1,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationDot(count: Int, modifier: Modifier = Modifier, themeAccentColor: Color = Color(0xFF00F0FF)) {
    Box(
        modifier = modifier
            .size(18.dp)
            .background(themeAccentColor, CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 9) "9+" else count.toString(),
            color = Color.Black,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderIcon(
    name: String,
    apps: List<AppModel>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    notificationCount: Int = 0
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(8.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x33FFFFFF))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            // 2x2 Grid of mini icons using lightweight Column and Rows (no LazyVerticalGrid overhead!)
            Column(
                modifier = Modifier.padding(6.dp).size(44.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                val takenApps = apps.take(4)
                val row1 = takenApps.take(2)
                val row2 = takenApps.drop(2).take(2)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row1.forEach { app ->
                        AsyncAppIcon(
                            app = app,
                            modifier = Modifier.padding(2.dp).size(18.dp)
                        )
                    }
                    repeat(2 - row1.size) {
                        Spacer(modifier = Modifier.size(18.dp))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row2.forEach { app ->
                        AsyncAppIcon(
                            app = app,
                            modifier = Modifier.padding(2.dp).size(18.dp)
                        )
                    }
                    repeat(2 - row2.size) {
                        Spacer(modifier = Modifier.size(18.dp))
                    }
                }
            }
            
            if (notificationCount > 0) {
                NotificationDot(
                    count = notificationCount,
                    modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)
                )
            }
        }
        Text(
            text = name,
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun FolderOverlay(
    name: String,
    apps: List<AppModel>,
    onAppClick: (AppModel) -> Unit,
    onClose: () -> Unit,
    themeAccentColor: Color = Color(0xFF00F0FF)
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .widthIn(max = 400.dp) // Max out at reasonable modal width on tablets
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0x1AFFFFFF))
                .border(1.dp, themeAccentColor.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .padding(24.dp)
                .clickable(enabled = false) {},
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name.uppercase(),
                color = themeAccentColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 70.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(apps) { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable { onAppClick(app) }
                    ) {
                        AsyncAppIcon(
                            app = app,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = app.label,
                            color = Color.White,
                            fontSize = 11.sp,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(onClick = onClose) {
                Text("CLOSE", color = Color.White.copy(alpha = 0.6f))
            }
        }
    }
}
