package com.vertigo.launcher

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
import com.vertigo.launcher.compose.HomeScreen
import com.vertigo.launcher.model.AppModel
import com.vertigo.launcher.ui.HomeViewModel
import com.vertigo.launcher.utils.ThemeEngine
import android.content.Context
import android.content.res.Configuration

class MainActivity : AppCompatActivity() {

    // Force fontScale to 1.0 so the launcher layout is immune to system font size changes.
    // This is a launcher — it must look consistent on every device regardless of accessibility settings.
    override fun attachBaseContext(newBase: Context) {
        val override = Configuration(newBase.resources.configuration)
        override.fontScale = 1.0f
        val ctx = newBase.createConfigurationContext(override)
        super.attachBaseContext(ctx)
    }

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
        
        // AutoStart Check for MIUI
        checkMiuiAutoStart()
        
        // Initialize Shizuku connection manager
        com.vertigo.launcher.utils.ShizukuSetup.init()
        
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
            val hiddenApps by viewModel.hiddenAppsList.collectAsState()
            val frozenApps by viewModel.frozenApps.collectAsState()
            val shizukuState by viewModel.shizukuState.collectAsState()
            val shizukuActionResult by viewModel.shizukuActionResult.collectAsState()
            val showLabels by viewModel.showLabels.collectAsState()
            val showBadges by viewModel.showBadges.collectAsState()
            val gridSize by viewModel.gridSize.collectAsState()
            val themeAccentColor by viewModel.themeAccentColor.collectAsState()
            val clockColors by viewModel.clockColors.collectAsState()
            
            val hiddenLayers by viewModel.hiddenLayers.collectAsState()
            
            // Multi-Select Flows
            val isSelectionMode by viewModel.isSelectionMode.collectAsState()
            val selectedPackages by viewModel.selectedPackages.collectAsState()
            
            MaterialTheme {
                HomeScreen(
                    flowerApps = flowerApps,
                    allApps = allApps,
                    hiddenApps = hiddenApps,
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
                    onUnhideApp = { app -> viewModel.unhideApp(app) },
                    onLaunchPopup = { app -> launchAppInFreeform(app) },
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
                    onCameraClick = { cameraLauncher.launch(null) },
                    shizukuState = shizukuState,
                    shizukuActionResult = shizukuActionResult,
                    frozenApps = frozenApps,
                    onFreezeApp = { pkg -> viewModel.freezeApp(pkg) },
                    onUnfreezeApp = { pkg -> viewModel.unfreezeApp(pkg) },
                    onForceStopApp = { pkg -> viewModel.forceStopApp(pkg) },
                    onClearAppData = { pkg -> viewModel.clearAppData(pkg) },
                    onSilentUninstallApp = { pkg -> viewModel.silentUninstallApp(pkg) },
                    onClearShizukuResult = { viewModel.clearShizukuResult() },
                    showLabels = showLabels,
                    showBadges = showBadges,
                    gridSize = gridSize,
                    themeAccentColor = themeAccentColor,
                    clockColors = clockColors,
                    hiddenLayers = hiddenLayers,
                    onCreateLayer = { name, isProtected -> viewModel.createHiddenLayer(name, isProtected) },
                    onDeleteLayer = { viewModel.deleteHiddenLayer(it) },
                    
                    // Batch Callbacks
                    isSelectionMode = isSelectionMode,
                    selectedPackages = selectedPackages,
                    onToggleSelection = { viewModel.toggleSelection(it) },
                    onEnterSelectionMode = { viewModel.enterSelectionMode(it) },
                    onBatchHide = { viewModel.batchHideApps() },
                    onBatchFreeze = { viewModel.batchFreezeApps() },
                    onBatchUnhide = { viewModel.batchUnhideApps() },
                    onBatchUnfreeze = { viewModel.batchUnfreezeApps() },
                    onClearSelection = { viewModel.clearSelection() }
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
        window.decorView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
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
    
    private var freeformCascadeOffset = 0
    
    private fun launchAppInFreeform(app: AppModel) {
        viewModel.clearNotificationBadge(app.packageName)
        viewModel.recordAppLaunch(app.packageName)
        val intent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (intent != null) {
            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or 
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT
            )
            val metrics = resources.displayMetrics
            val screenWidth = metrics.widthPixels
            val screenHeight = metrics.heightPixels
            
            val windowWidth = (screenWidth * 0.7f).toInt()
            val windowHeight = (screenHeight * 0.6f).toInt()
            
            val baseStartX = (screenWidth * 0.1f).toInt()
            val baseStartY = (screenHeight * 0.15f).toInt()
            val step = 80 // Stagger step
            
            var currentX = baseStartX + (freeformCascadeOffset * step)
            var currentY = baseStartY + (freeformCascadeOffset * step)
            
            if (currentX + windowWidth > screenWidth || currentY + windowHeight > screenHeight) {
                freeformCascadeOffset = 0
                currentX = baseStartX
                currentY = baseStartY
            }
            
            freeformCascadeOffset++ // Advance cascade
            
            val bounds = android.graphics.Rect(currentX, currentY, currentX + windowWidth, currentY + windowHeight)
            val options = android.app.ActivityOptions.makeBasic()
            options.setLaunchBounds(bounds)
            
            // Magic: Force WINDOWING_MODE_FREEFORM (constant 5) using deep bundle injection
            val bundle = options.toBundle()
            bundle?.putInt("android.activity.windowingMode", 5)
            
            try {
                startActivity(intent, bundle)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to launch freeform", e)
                startActivity(intent) // Fallback
            }
        }
    }

    private fun launchApp(app: AppModel) {
        // Clear notification badge immediately for instant feedback
        viewModel.clearNotificationBadge(app.packageName)
        // Track launch for smart usage-based grid suggestions
        viewModel.recordAppLaunch(app.packageName)
        
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
    
    private fun checkMiuiAutoStart() {
        val prefs = com.vertigo.launcher.utils.StorageHelper.getSafeDefaultSharedPreferences(this)
        if (!prefs.getBoolean("miui_autostart_prompted", false)) {
            val intent = Intent()
            intent.component = android.content.ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
            try {
                if (packageManager.resolveActivity(intent, 0) != null) {
                    android.app.AlertDialog.Builder(this)
                        .setTitle("MIUI AutoStart Required")
                        .setMessage("To allow notification badges to work properly on this device, please manually enable 'AutoStart' for V-Launcher.")
                        .setPositiveButton("Open Settings") { _, _ -> startActivity(intent) }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            } catch (e: Exception) {}
            prefs.edit().putBoolean("miui_autostart_prompted", true).apply()
        }
     }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        try {
            com.vertigo.launcher.utils.PerformanceOptimizer.trimMemory(level)
            if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
                // Launcher is hidden, prompt GC to clean up unused memory immediately
                System.gc()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in onTrimMemory", e)
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            // Suggest GC when launcher transitions to stopped state
            System.gc()
        } catch (e: Exception) {
            // Ignore
        }
    }
}

