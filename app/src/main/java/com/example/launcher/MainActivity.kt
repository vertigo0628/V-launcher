package com.example.launcher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.launcher.compose.HomeScreen
import com.example.launcher.model.AppModel
import com.example.launcher.ui.HomeViewModel
import com.example.launcher.utils.ThemeEngine

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var themeEngine: ThemeEngine
    
    // Permission launcher for Location and Mic
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // After permissions granted/denied, refresh weather (it will use location if available)
        viewModel.refreshWeather()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge: make status bar and navigation bar transparent
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        )
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        // Initialize managers
        themeEngine = ThemeEngine(this)
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        
        // Request necessary permissions
        requestPermissionsIfNeeded()
        
        setContent {
            val flowerApps by viewModel.flowerApps.collectAsState(initial = emptyList())
            val allApps by viewModel.apps.collectAsState(initial = emptyList())
            val showDrawer by viewModel.isDrawerOpen.collectAsState(initial = false)
            val neuralHubState by viewModel.neuralHubState.collectAsState()
            
            // Neural Hub visibility state
            var showNeuralHub by remember { androidx.compose.runtime.mutableStateOf(false) }
            
            // Updates are now always-on in ViewModel

            
            val weatherState by viewModel.weatherState.collectAsState()
            val isVoiceListening by viewModel.isVoiceListening.collectAsState()
            val isVoiceEnabled by viewModel.isVoiceEnabled.collectAsState()
            val searchResults by viewModel.searchResults.collectAsState()
            val searchQuery by viewModel.searchQuery.collectAsState()
            
            
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
                    isVoiceEnabled = isVoiceEnabled,
                    onVoiceClick = { 
                        // Toggle Listening Persistence
                        viewModel.toggleVoiceEnabled()
                    },
                    searchQuery = searchQuery, // Pass query state
                    searchResults = searchResults,
                    onSearchQuery = { viewModel.onSearchQueryChanged(it) },
                    onSearchResultClick = { result -> viewModel.performSearchAction(result.action) },
                    onAddToGrid = { app -> viewModel.addToGrid(app) },
                    onRemoveFromGrid = { app -> viewModel.removeFromGrid(app) },
                    onHideApp = { app -> viewModel.hideApp(app) },
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
        // Reload settings in case user changed Voice/Hub toggles
        viewModel.reloadSettings()
    }
    
    override fun onPause() {
        super.onPause()
        // Stop background resources to save battery
        viewModel.onActivityPause()
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
    
    private fun requestPermissionsIfNeeded() {
        val permissionsToRequest = mutableListOf<String>()
        
        // Check Location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        
        // Check Microphone permission (for Voice Assistant)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        
        // Check Contacts permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_CONTACTS)
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}
