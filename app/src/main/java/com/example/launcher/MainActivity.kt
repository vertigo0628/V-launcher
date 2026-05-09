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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import com.example.launcher.compose.HomeScreen
import com.example.launcher.model.AppModel
import com.example.launcher.ui.HomeViewModel
import com.example.launcher.utils.ThemeEngine

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var themeEngine: ThemeEngine

    // Camera capture launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.processImageQuery(bitmap)
        }
    }
    
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
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        // Ensure lighting of status bars matches appearance
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = false // Dark mode theme default
        controller.isAppearanceLightNavigationBars = false
        
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
            val musicState by viewModel.musicState.collectAsState()
            val lockedApps by viewModel.lockedApps.collectAsState()
            val cpuHistory by viewModel.cpuHistory.collectAsState()
            val neuralInsight by viewModel.neuralInsight.collectAsState()
            val notificationCounts by viewModel.notificationCounts.collectAsState()
            val folders by viewModel.folders.collectAsState()
            val shortcuts by viewModel.shortcuts.collectAsState()
            val isSearching by viewModel.isSearching.collectAsState()
            val chatHistory by viewModel.chatHistory.collectAsState()
            val currentStreamingResponse by viewModel.currentStreamingResponse.collectAsState()
            val isAiThinking by viewModel.isAiThinking.collectAsState()
            val spokenText by viewModel.spokenText.collectAsState()
            val isHotwordActive by viewModel.isHotwordActive.collectAsState()
            
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
                    onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                    onSearchResultClick = { result -> viewModel.performSearchAction(result.action) },
                    isSearching = isSearching,
                    onAddToGrid = { app -> viewModel.addToGrid(app) },
                    onRemoveFromGrid = { app -> viewModel.removeFromGrid(app) },
                    onHideApp = { app -> viewModel.hideApp(app) },
                    viewModel = viewModel,
                    onSettings = {  
                        startActivity(Intent(this, LauncherSettingsActivity::class.java))
                    },
                    musicState = musicState,
                    onMusicPlayPause = { viewModel.musicPlayPause() },
                    onMusicNext = { viewModel.musicSkipNext() },
                    onMusicPrev = { viewModel.musicSkipPrev() },
                    lockedApps = lockedApps,
                    onLockApp = { app -> viewModel.lockApp(app) },
                    onUnlockApp = { app -> viewModel.unlockApp(app) },
                    cpuHistory = cpuHistory,
                    neuralInsight = neuralInsight,
                    notificationCounts = notificationCounts,
                    folders = folders,
                    onAddAppToFolder = { folder, pkg -> viewModel.addAppToFolder(folder, pkg) },
                    onDeleteFolder = { name -> viewModel.deleteFolder(name) },
                    shortcuts = shortcuts,
                    onLoadShortcuts = { pkg -> viewModel.loadShortcutsForApp(pkg) },
                    onShortcutClick = { shortcut -> 
                        viewModel.launchShortcut(shortcut)
                        viewModel.clearShortcuts()
                    },
                    onClearShortcuts = { viewModel.clearShortcuts() },
                    chatHistory = chatHistory,
                    currentStreamingResponse = currentStreamingResponse,
                    isAiThinking = isAiThinking,
                    spokenText = spokenText,
                    isHotwordActive = isHotwordActive,
                    onClearAiResponse = { viewModel.clearChatHistory() },
                    onSendAiText = { text -> viewModel.sendTextToAiBrain(text) },
                    onStopAiText = { viewModel.stopAiResponse() },
                    onCameraClick = { cameraLauncher.launch(null) }
                )
            }
        }
        
        // Apply theme colors to window background (Transparent/Black)
        applyTheme()
    }
    
    override fun onResume() {
        super.onResume()
        try {
            if (::viewModel.isInitialized) {
                viewModel.onForeground()
                viewModel.reloadSettings()
            }
            extractWallpaperColors()
            applyTheme()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in onResume", e)
        }
    }
    
    override fun onPause() {
        super.onPause()
        try {
            if (::viewModel.isInitialized) {
                viewModel.onActivityPause()
                viewModel.onBackground()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in onPause", e)
        }
    }
    
    private fun applyTheme() {
        val bgColor = themeEngine.let {
            val themeManager = com.example.launcher.utils.ThemeManager(this)
            themeManager.getBackgroundColor()
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
        // Clear notification badge immediately for instant feedback
        viewModel.clearNotificationBadge(app.packageName)
        
        val intent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (intent != null) {
            startActivity(intent)
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
        
        // Check Calendar permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_CALENDAR)
        }
        
        // Check Camera permission (for Vision Analysis)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }
        
        // Check Media permission (for Local Music Fallback)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}
