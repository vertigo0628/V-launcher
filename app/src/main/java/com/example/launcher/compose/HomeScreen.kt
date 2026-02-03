package com.example.launcher.compose

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
    onVoiceClick: () -> Unit,
    showSearch: Boolean,
    onSearchToggle: (Boolean) -> Unit,
    filteredApps: List<AppModel>,
    onSearchQuery: (String) -> Unit,
    onSettings: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    // Gesture Logic
    var offsetY by remember { mutableFloatStateOf(0f) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent) // Show wallpaper
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (offsetY < -50f) {
                            onDrawerToggle(true) // Swipe Up
                        } else if (offsetY > 50f) {
                            onSearchToggle(true) // Swipe Down
                        }
                        offsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetY += dragAmount.y
                    }
                )
            }
    ) {
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Panel: Clock and Date
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ClockWidget(modifier = Modifier.size(200.dp))
                    WeatherWidget(state = weatherState)
                    Spacer(modifier = Modifier.height(32.dp))
                    VoiceAssistantWidget(isListening = isVoiceListening, onClick = onVoiceClick)
                }
                
                // Right Panel: Flower Grid
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    FlowerGrid(apps = flowerApps, onAppClick = onAppClick)
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
                    VoiceAssistantWidget(isListening = isVoiceListening, onClick = onVoiceClick)
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Middle: Flower Grid
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp), // Fixed height area for grid
                    contentAlignment = Alignment.Center
                ) {
                    FlowerGrid(apps = flowerApps, onAppClick = onAppClick)
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
                onClose = { onDrawerToggle(false) }
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
                onClose = { onSearchToggle(false) },
                onAppClick = onAppClick,
                filteredApps = filteredApps,
                onQueryChange = onSearchQuery
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
    onAppClick: (AppModel) -> Unit
) {
    // Custom Layout for Hex Grid logic ported from FlowerGridView.kt
    Layout(
        content = {
            apps.take(37).forEach { app ->
                AppIcon(app, onAppClick)
            }
        }
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val centerX = width / 2f
        val centerY = height / 2f
        
        val iconSizePx = 64.dp.toPx()
        val baseSpacing = iconSizePx * 1.35f
        
        val layoutList = mutableListOf<Pair<Int, Pair<Int, Int>>>()
        
        if (placeables.isNotEmpty()) {
            // Center Item
            val centerPlaceable = placeables[0]
            val cx = (centerX - centerPlaceable.width / 2).toInt()
            val cy = (centerY - centerPlaceable.height / 2).toInt()
            layoutList.add(0 to (cx to cy))
        }
        
        var vectorIdx = 1
        var ring = 1
        val spacing = baseSpacing
        
        while (vectorIdx < placeables.size) {
            for (side in 0 until 6) {
                for (step in 0 until ring) {
                    if (vectorIdx >= placeables.size) break
                    
                    val angle1Deg = -90.0 + 60 * side
                    val angle2Deg = -90.0 + 60 * (side + 1)
                    val angle1 = Math.toRadians(angle1Deg)
                    val angle2 = Math.toRadians(angle2Deg)
                    
                    val cornerRadius = ring * spacing
                    
                    val x1 = (cornerRadius * cos(angle1)).toFloat()
                    val y1 = (cornerRadius * sin(angle1)).toFloat()
                    val x2 = (cornerRadius * cos(angle2)).toFloat()
                    val y2 = (cornerRadius * sin(angle2)).toFloat()
                    
                    val fraction = step.toFloat() / ring.toFloat()
                    val px = x1 + (x2 - x1) * fraction
                    val py = y1 + (y2 - y1) * fraction
                    
                    val placeable = placeables[vectorIdx]
                    val lx = (centerX + px - placeable.width / 2).toInt()
                    val ly = (centerY + py - placeable.height / 2).toInt()
                    
                    layoutList.add(vectorIdx to (lx to ly))
                    vectorIdx++
                }
            }
            ring++
        }

        layout(width, height) {
            layoutList.forEach { (index, pos) ->
                val (x, y) = pos
                placeables[index].placeRelative(x, y)
            }
        }
    }
}

@Composable
fun AppIcon(app: AppModel, onClick: (AppModel) -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Color(0x3300F0FF))
            .clickable { onClick(app) },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                android.widget.ImageView(ctx).apply {
                    setImageDrawable(app.icon)
                    scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                }
            },
            modifier = Modifier.size(48.dp)
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

@Composable
fun AppDrawer(
    apps: List<AppModel>,
    onAppClick: (AppModel) -> Unit,
    onClose: () -> Unit
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
                            .clickable { onAppClick(app) }
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
