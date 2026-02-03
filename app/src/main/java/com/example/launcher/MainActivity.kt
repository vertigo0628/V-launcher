package com.example.launcher

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import com.example.launcher.compose.HomeScreen
import com.example.launcher.model.AppModel
import com.example.launcher.ui.HomeViewModel
import com.example.launcher.utils.ThemeEngine

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var themeEngine: ThemeEngine
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make status bar transparent
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        
        // Initialize managers
        themeEngine = ThemeEngine(this)
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        
        setContent {
            val flowerApps by viewModel.flowerApps.collectAsState(initial = emptyList())
            val allApps by viewModel.apps.collectAsState(initial = emptyList())
            val showDrawer by viewModel.isDrawerOpen.collectAsState(initial = false)
            val neuralHubState by viewModel.neuralHubState.collectAsState()
            
            // Neural Hub visibility state
            var showNeuralHub by remember { androidx.compose.runtime.mutableStateOf(false) }
            
            // Start/stop hub updates based on visibility
            androidx.compose.runtime.LaunchedEffect(showNeuralHub) {
                if (showNeuralHub) {
                    viewModel.startHubUpdates()
                } else {
                    viewModel.stopHubUpdates()
                }
            }
            
            val weatherState by viewModel.weatherState.collectAsState()
            val isVoiceListening by viewModel.isVoiceListening.collectAsState()
            val filteredApps by viewModel.filteredApps.collectAsState()
            
            var showSearch by remember { androidx.compose.runtime.mutableStateOf(false) }
            
            MaterialTheme {
                HomeScreen(
                    flowerApps = flowerApps,
                    allApps = allApps,
                    onAppClick = { launchApp(it) },
                    showDrawer = showDrawer,
                    onDrawerToggle = { viewModel.setDrawerOpen(it) },
                    neuralHubState = neuralHubState,
                    showNeuralHub = showNeuralHub,
                    onNeuralHubToggle = { showNeuralHub = it },
                    weatherState = weatherState,
                    isVoiceListening = isVoiceListening,
                    onVoiceClick = { 
                        // Toggle Listening
                        viewModel.setVoiceListening(!isVoiceListening)
                    },
                    showSearch = showSearch,
                    onSearchToggle = { showSearch = it },
                    filteredApps = filteredApps,
                    onSearchQuery = { viewModel.onSearchQueryChanged(it) },
                    onSettings = { 
                        startActivity(Intent(this, LauncherSettingsActivity::class.java))
                    }
                )
            }
        }
        
        // Apply theme colors to window background (Transparent/Black)
        applyTheme()
    }
    
    override fun onResume() {
        super.onResume()
        extractWallpaperColors()
    }
    
    private fun applyTheme() {
        val prefs = getSharedPreferences("theme_prefs", android.content.Context.MODE_PRIVATE)
        val amoledMode = prefs.getBoolean("amoled_mode", false)
        
        val bgColor = if (amoledMode) {
            android.graphics.Color.BLACK
        } else {
            android.graphics.Color.TRANSPARENT
        }
        window.decorView.setBackgroundColor(bgColor)
    }
    
    private fun extractWallpaperColors() {
        try {
            val wallpaperManager = android.app.WallpaperManager.getInstance(this)
            val wallpaperDrawable = wallpaperManager.drawable
            themeEngine.extractColorsFromDrawable(wallpaperDrawable)
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    private fun launchApp(app: AppModel) {
        val intent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (intent != null) {
            startActivity(intent)
        }
    }
    
    override fun onBackPressed() {
        if (viewModel.isDrawerOpen.value) {
            viewModel.setDrawerOpen(false)
        } else {
            // Can't easily access local showSearch state here without moving it to ViewModel
            // For checking purposes, we rely on standard back stack or let users tap outside to close overlays
            super.onBackPressed()
        }
    }
}
