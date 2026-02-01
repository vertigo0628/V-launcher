package com.example.launcher.compose

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.launcher.R
import com.example.launcher.model.AppModel
import com.example.launcher.ui.CyberClockView
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
    onSettings: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent) // Show wallpaper
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
            // Portrait Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp), // Status bar padding
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top: Clock
                ClockWidget(modifier = Modifier.size(280.dp))
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Middle: Flower Grid
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp), // Fixed height area for grid
                    contentAlignment = Alignment.Center
                ) {
                    FlowerGrid(apps = flowerApps, onAppClick = onAppClick)
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Bottom: Search & Dock
                SearchBar(onSearchClick = { onDrawerToggle(true) })
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
fun SearchBar(onSearchClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .background(Color(0x33FFFFFF), CircleShape)
            .clickable { onSearchClick() },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "Search apps...",
            color = Color.White,
            modifier = Modifier.padding(start = 16.dp)
        )
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
        DockItem(R.drawable.launcher, onNeuralHub) // Neural Hub (using launcher icon as placeholder)
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
