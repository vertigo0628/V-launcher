package com.example.launcher.compose

import android.content.res.Configuration
import android.content.Intent
import android.content.Context
import android.net.Uri
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.launcher.R
import com.example.launcher.model.AppModel
import com.example.launcher.ui.CyberClockView
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeScreen(
    flowerApps: List<AppModel>,
    allApps: List<AppModel>,
    onAppClick: (AppModel) -> Unit,
    showDrawer: Boolean,
    onDrawerToggle: (Boolean) -> Unit,
    neuralHubState: com.example.launcher.ui.HomeViewModel.NeuralHubState,
    showNeuralHub: Boolean,
    onNeuralHubToggle: (Boolean) -> Unit,
    weatherState: com.example.launcher.ui.HomeViewModel.WeatherState,
    isVoiceListening: Boolean,
    isVoiceEnabled: Boolean,
    onVoiceClick: () -> Unit,
    searchQuery: String,
    searchResults: List<com.example.launcher.utils.SearchManager.SearchResult>,
                    onSearchQueryChange: (String) -> Unit = {},
                    onSearchResultClick: (com.example.launcher.utils.SearchManager.SearchResult) -> Unit = {},
                    isSearching: Boolean = false,
                    onAddToGrid: (AppModel) -> Unit,
                    onRemoveFromGrid: (AppModel) -> Unit,
    onHideApp: (AppModel) -> Unit,
    onSettings: () -> Unit,
    musicState: com.example.launcher.ui.HomeViewModel.MusicState = com.example.launcher.ui.HomeViewModel.MusicState(),
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
    shortcuts: List<com.example.launcher.ui.HomeViewModel.AppShortcut> = emptyList(),
    onLoadShortcuts: (String) -> Unit = {},
    onShortcutClick: (com.example.launcher.ui.HomeViewModel.AppShortcut) -> Unit = {},
    onClearShortcuts: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    // Internal state for the inline search overlay
    var showInlineSearch by remember { androidx.compose.runtime.mutableStateOf(false) }
    
    // Gesture Logic Removed: Swipe Up/Down disabled as per request
    
    // Internal state for folder view
    var activeFolder by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    
    // Parallax State (Phase 15)
    var roll by remember { androidx.compose.runtime.mutableStateOf(0f) }
    var pitch by remember { androidx.compose.runtime.mutableStateOf(0f) }
    val sensorManager = androidx.compose.ui.platform.LocalContext.current.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    
    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                    roll = event.values[0] * 2f // Sensitivity
                    pitch = event.values[1] * 2f
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI)
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = -roll
                translationY = pitch
            }
            .background(Color.Transparent) // Show wallpaper
    ) {
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF))
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
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF))
                                ) { Text("Add to Grid", color = Color.Black) }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Button(
                                    onClick = {
                                        appPendingAction?.let { onHideApp(it) }
                                        appPendingAction = null
                                        actionType = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                                ) { Text("Hide App", color = Color.White) }
                            }
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
                    }) { Text("ADD", color = Color(0xFF00F0FF)) }
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
                onRemoveFromFolder = { app ->
                    // Logic already handled via onDeleteFolder or add/remove methods in future
                }
            )
        }
        if (isLandscape) {
            Row(
                modifier = Modifier
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
                    ClockWidget(modifier = Modifier.size(200.dp))
                    WeatherWidget(state = weatherState)
                    Spacer(modifier = Modifier.height(32.dp))
                    VoiceAssistantWidget(isEnabled = isVoiceEnabled, isListening = isVoiceListening, onClick = onVoiceClick)
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
                        notificationCounts = notificationCounts
                    )
                }
            }
        } else {
            // Portrait Layout - Use WindowInsets for proper system bar handling
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding() // Proper status bar padding
                    .navigationBarsPadding(), // Proper navigation bar padding
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp)) // Extra top breathing room
                
                // Top: Clock & Widgets
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ClockWidget(modifier = Modifier.size(260.dp))
                    WeatherWidget(state = weatherState)
                }

                Box(modifier = Modifier.padding(vertical = 12.dp)) {
                    VoiceAssistantWidget(isEnabled = isVoiceEnabled, isListening = isVoiceListening, onClick = onVoiceClick)
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Middle: Flower Grid
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp), // Fixed height area for grid
                    contentAlignment = Alignment.Center
                ) {
                    FlowerGrid(
                        apps = flowerApps,
                        onAppClick = onAppClick,
                        onAppLongClick = { app ->
                            appPendingAction = app
                            actionType = "REMOVE"
                        },
                        notificationCounts = notificationCounts
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Bottom: Inline Search & Dock
                InlineSearchBar(
                    isVoiceListening = isVoiceListening,
                    onSearchClick = { showInlineSearch = true }
                )
                Dock(
                    onSettings = onSettings,
                    onDrawer = { onDrawerToggle(true) },
                    onNeuralHub = { onNeuralHubToggle(true) }
                )
            }
        }
        
        // App Drawer Overlay
        if (showDrawer) {
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
                onDeleteFolder = onDeleteFolder
            )
        }
        
        // Neural Hub Overlay
        if (showNeuralHub) {
            NeuralHub(
                state = neuralHubState,
                musicState = musicState,
                onClose = { onNeuralHubToggle(false) },
                onMusicPlayPause = onMusicPlayPause,
                onMusicNext = onMusicNext,
                onMusicPrev = onMusicPrev,
                cpuHistory = cpuHistory,
                neuralInsight = neuralInsight
            )
        }
        
        // NEW: Inline Search Overlay (Rendered at Root)
        if (showInlineSearch) {
            SearchOverlay(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                searchResults = searchResults,
                onResultClick = onSearchResultClick,
                onClose = { showInlineSearch = false },
                isSearching = isSearching
            )
        }
        
    }
}

@Composable
fun ClockWidget(modifier: Modifier = Modifier) {
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
            }
        },
        update = { view ->
            view.setTime(currentTime)
        },
        modifier = modifier
    )
}

@Composable
fun FlowerGrid(
    apps: List<AppModel>,
    onAppClick: (AppModel) -> Unit,
    onAppLongClick: (AppModel) -> Unit,
    notificationCounts: Map<String, Int> = emptyMap()
) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
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
        
        val spacingFactor = 1.25f
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
                    val count = notificationCounts[app.packageName] ?: 0
                    AppIcon(app, onAppClick, onAppLongClick, iconSizeDp, count)
                }
            }
        ) { measurables, lConstraints ->
            val placeables = measurables.map { it.measure(lConstraints) }
            val cx = lConstraints.maxWidth / 2f
            val cy = lConstraints.maxHeight / 2f
            
            val layoutList = mutableListOf<Pair<Int, Pair<Int, Int>>>()
            
            if (placeables.isNotEmpty()) {
                // Center Item
                val center = placeables[0]
                layoutList.add(0 to ((cx - center.width / 2).toInt() to (cy - center.height / 2).toInt()))
            }
            
            var vectorIdx = 1
            var ring = 1
            
            while (vectorIdx < placeables.size) {
                // Number of items in this ring = 6 * ring
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
                        
                        layoutList.add(vectorIdx to (lx to ly))
                        vectorIdx++
                    }
                }
                ring++
            }
            
            layout(lConstraints.maxWidth, lConstraints.maxHeight) {
                layoutList.forEach { (idx, pos) ->
                    placeables[idx].placeRelative(pos.first, pos.second)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIcon(
    app: AppModel,
    onClick: (AppModel) -> Unit,
    onLongClick: (AppModel) -> Unit,
    size: androidx.compose.ui.unit.Dp = 64.dp,
    notificationCount: Int = 0
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
            AndroidView(
                factory = { ctx ->
                    android.widget.ImageView(ctx).apply {
                        setImageDrawable(app.icon)
                        scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                    }
                },
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
}

@Composable
fun SearchBar(
    isListening: Boolean,
    onSearchClick: () -> Unit
) {
    val borderColor = if (isListening) Color(0xFF00F0FF) else Color.Transparent
    val borderWidth = if (isListening) 2.dp else 0.dp
    val textColor = if (isListening) Color(0xFF00F0FF) else Color.White.copy(alpha = 0.7f)
    val text = if (isListening) "Listening..." else "Search apps, web, & more..."
    val icon = if (isListening) android.R.drawable.ic_btn_speak_now else android.R.drawable.ic_menu_search
    val iconTint = if (isListening) Color(0xFF00F0FF) else Color.White.copy(alpha = 0.7f)
    
    // Debug: Log every time this composable is rendered
    android.util.Log.d("SearchBar", "SearchBar composable rendered - isListening: $isListening")


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
    onDeleteFolder: (String) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3000000)) // Dark scrim
            .clickable { onClose() }
            .padding(top = 40.dp)
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
            
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 80.dp),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Phase 13: Render Folders
                folders.entries.forEach { (name, packages) ->
                    item {
                        val folderApps = apps.filter { packages.contains(it.packageName) }
                        val folderNotifCount = folderApps.sumOf { notificationCounts[it.packageName] ?: 0 }
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
                val appsInFolders = folders.values.flatten().toSet()
                items(apps.filter { !appsInFolders.contains(it.packageName) }) { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(8.dp)
                            .combinedClickable(
                                onClick = { onAppClick(app) },
                                onLongClick = { onAppLongClick(app) }
                            )
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            AndroidView(
                                factory = { ctx ->
                                    android.widget.ImageView(ctx).apply {
                                        setImageDrawable(app.icon)
                                        scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                                    }
                                },
                                modifier = Modifier.size(56.dp)
                            )
                            // Notification Dot
                            val count = notificationCounts[app.packageName] ?: 0
                            if (count > 0) {
                                NotificationDot(count = count)
                            }
                        }
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

@Composable
fun NotificationDot(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(18.dp)
            .background(Color(0xFF00F0FF), CircleShape)
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
    apps: List<AppModel>, // First few apps in folder
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
            // 2x2 Grid of mini icons
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.padding(6.dp).size(44.dp),
                userScrollEnabled = false
            ) {
                items(apps.take(4)) { app ->
                    AndroidView(
                        factory = { ctx ->
                            android.widget.ImageView(ctx).apply {
                                setImageDrawable(app.icon)
                            }
                        },
                        modifier = Modifier.padding(2.dp).size(18.dp)
                    )
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
    onRemoveFromFolder: (AppModel) -> Unit
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
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0x1AFFFFFF))
                .border(1.dp, Color(0xFF00F0FF).copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .padding(24.dp)
                .clickable(enabled = false) {},
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name.uppercase(),
                color = Color(0xFF00F0FF),
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
                        AndroidView(
                            factory = { ctx ->
                                android.widget.ImageView(ctx).apply {
                                    setImageDrawable(app.icon)
                                }
                            },
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
