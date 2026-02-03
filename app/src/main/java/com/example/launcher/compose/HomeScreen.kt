package com.example.launcher.compose

import android.content.res.Configuration
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.launcher.R
import com.example.launcher.model.AppModel
import com.example.launcher.ui.CyberClockView
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.setValue
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
    showSearch: Boolean,
    onSearchToggle: (Boolean) -> Unit,
    searchQuery: String,
    searchResults: List<com.example.launcher.utils.SearchManager.SearchResult>,
    onSearchQuery: (String) -> Unit,
    onSearchResultClick: (com.example.launcher.utils.SearchManager.SearchResult) -> Unit,
    onAddToGrid: (AppModel) -> Unit,
    onRemoveFromGrid: (AppModel) -> Unit,
    onHideApp: (AppModel) -> Unit,
    onSettings: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    // Gesture Logic Removed: Swipe Up/Down disabled as per request
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent) // Show wallpaper
    ) {
        // Mutable Grid State
        var appPendingAction by remember { androidx.compose.runtime.mutableStateOf<AppModel?>(null) }
        var actionType by remember { androidx.compose.runtime.mutableStateOf<String?>(null) } // "ADD" or "REMOVE"

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
                    },
                    title = { Text(appPendingAction?.label ?: "Options", color = Color.White) },
                    text = { Text("Choose an action:", color = Color.LightGray) },
                    confirmButton = {
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
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)) // Red
                            ) { Text("Hide App", color = Color.White) }
                        }
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
            }
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
                    Spacer(modifier = Modifier.height(32.dp))
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
                        }
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
                        }
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Bottom: Search & Dock
                SearchBar(
                    isListening = isVoiceListening,
                    onSearchClick = { onSearchToggle(true) }
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
                onAppClick = onAppClick,
                onClose = { onDrawerToggle(false) },
                onAppLongClick = { app ->
                    appPendingAction = app
                    actionType = "DRAWER_OPTIONS"
                }
            )
        }
        
        // Neural Hub Overlay
        if (showNeuralHub) {
            NeuralHub(
                state = neuralHubState,
                onClose = { onNeuralHubToggle(false) }
            )
        }
        
        // Universal Search Overlay
        if (showSearch) {
            UniversalSearch(
                query = searchQuery,
                onClose = { onSearchToggle(false) },
                searchResults = searchResults,
                onQueryChange = onSearchQuery,
                onResultClick = onSearchResultClick
            )
        }
    }
}

@Composable
fun ClockWidget(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            CyberClockView(context).apply {
                setTime(System.currentTimeMillis())
            }
        },
        update = { view ->
            view.setTime(System.currentTimeMillis())
        },
        modifier = modifier
    )
}

@Composable
fun FlowerGrid(
    apps: List<AppModel>,
    onAppClick: (AppModel) -> Unit,
    onAppLongClick: (AppModel) -> Unit
) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val minDim = minOf(width, height)
        val density = LocalDensity.current.density
        
        // Adaptive Logic
        // Determine rings based on available space
        // Standard Phone: 3 Rings (1+6+12+18 = 37 apps)
        // Tablet/PC (Small dim > 600dp): 4 Rings (37+24 = 61 apps)
        val isLargeScreen = (minDim / density) > 600
        val targetRings = if (isLargeScreen) 4 else 3
        val maxApps = if (targetRings == 4) 61 else 37
        
        // Calculate max icon size that fits
        // Radius = rings * spacing + iconSize/2
        // spacing = iconSize * 1.25 (tighter packing for adaptive)
        // minDim/2 >= iconSize * (1.25 * rings + 0.5)
        val spacingFactor = 1.25f
        val maxIconSizePx = (minDim / 2f) / (spacingFactor * targetRings + 0.5f)
        
        // Clamp icon size: Min 48dp, Max 80dp (or user preference base)
        val calculatedSizeDp = (maxIconSizePx / density).dp
        val iconSizeDp = maxOf(40.dp, minOf(80.dp, calculatedSizeDp))
        
        // Use custom Layout for precise positioning
        val iconSizePx = with(LocalDensity.current) { iconSizeDp.toPx() }
        val spacing = iconSizePx * spacingFactor
        
        Layout(
            content = {
                apps.take(maxApps).forEach { app ->
                    AppIcon(app, onAppClick, onAppLongClick, iconSizeDp)
                }
            }
        ) { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            val cx = constraints.maxWidth / 2f
            val cy = constraints.maxHeight / 2f
            
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
                        
                        val radius = ring * spacing
                        
                        val x1 = (radius * cos(angle1)).toFloat()
                        val y1 = (radius * sin(angle1)).toFloat()
                        val x2 = (radius * cos(angle2)).toFloat()
                        val y2 = (radius * sin(angle2)).toFloat()
                        
                        val fraction = step.toFloat() / ring.toFloat()
                        val px = x1 + (x2 - x1) * fraction
                        val py = y1 + (y2 - y1) * fraction
                        
                        val placeable = placeables[vectorIdx]
                        val lx = (cx + px - placeable.width / 2).toInt()
                        val ly = (cy + py - placeable.height / 2).toInt()
                        
                        layoutList.add(vectorIdx to (lx to ly))
                        vectorIdx++
                    }
                }
                ring++
            }
            
            layout(constraints.maxWidth, constraints.maxHeight) {
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
    size: androidx.compose.ui.unit.Dp = 64.dp
) {
    Box(
        modifier = Modifier
            .size(size)
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
            modifier = Modifier.size(size * 0.75f)
        )
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .height(48.dp)
            .border(borderWidth, borderColor, CircleShape)
            .background(Color(0x33FFFFFF), CircleShape)
            .clickable(onClick = onSearchClick),
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
    onAppLongClick: (AppModel) -> Unit
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
                items(apps) { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(8.dp)
                            .combinedClickable(
                                onClick = { onAppClick(app) },
                                onLongClick = { onAppLongClick(app) }
                            )
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                android.widget.ImageView(ctx).apply {
                                    setImageDrawable(app.icon)
                                    scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                                }
                            },
                            modifier = Modifier.size(56.dp)
                        )
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
