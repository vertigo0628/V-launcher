package com.example.launcher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.launcher.data.AppRepository
import com.example.launcher.model.AppModel
import com.example.launcher.model.AppCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import com.example.launcher.logic.VoiceManager
import com.example.launcher.data.WeatherRepository
import android.location.Location
import android.location.LocationManager
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Process
import android.graphics.drawable.Drawable
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.CalendarContract
import java.util.Calendar
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val flowerGridManager = com.example.launcher.utils.FlowerGridManager(application)
    private val preferencesManager = com.example.launcher.utils.PreferencesManager(application)
    private val launcherApps = application.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    
    // Shortcut Model
    data class AppShortcut(
        val id: String,
        val label: String,
        val icon: Drawable?,
        val packageName: String
    )
    
    private val _shortcuts = MutableStateFlow<List<AppShortcut>>(emptyList())
    val shortcuts: StateFlow<List<AppShortcut>> = _shortcuts.asStateFlow()
    
    // Original full list
    private var allApps: List<AppModel> = emptyList()
    
    private val _apps = MutableStateFlow<List<AppModel>>(emptyList())
    val apps: StateFlow<List<AppModel>> = _apps.asStateFlow()

    private val _hiddenAppsList = MutableStateFlow<List<AppModel>>(emptyList())
    val hiddenAppsList: StateFlow<List<AppModel>> = _hiddenAppsList.asStateFlow()
    
    private val _flowerApps = MutableStateFlow<List<AppModel>>(emptyList())
    val flowerApps: StateFlow<List<AppModel>> = _flowerApps.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow<AppCategory?>(null)
    val selectedCategory: StateFlow<AppCategory?> = _selectedCategory.asStateFlow()
    
    // UI State
    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen: StateFlow<Boolean> = _isDrawerOpen.asStateFlow()
    
    fun setDrawerOpen(isOpen: Boolean) {
        _isDrawerOpen.value = isOpen
    }

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            val fullList = repository.getInstalledApps()
            val hiddenSet = preferencesManager.getHiddenApps()
            
            // Filter out hidden apps from main list
            allApps = fullList.filter { !hiddenSet.contains(it.packageName) }
            
            // Expose hidden apps separately (for management)
            val hiddenList = fullList.filter { hiddenSet.contains(it.packageName) }
            _hiddenAppsList.value = hiddenList
            
            updateAppList()
            updateFlowerGrid()
        }
    }

    fun hideApp(app: AppModel) {
        preferencesManager.hideApp(app.packageName)
        loadApps() // Reload to refresh lists
    }
    
    fun unhideApp(app: AppModel) {
        preferencesManager.unhideApp(app.packageName)
        loadApps()
    }
    
    fun updateFlowerGrid() {
        val gridPackages = flowerGridManager.getGridApps()
        
        if (gridPackages.isEmpty()) {
            // Default behavior: show first 37 apps if no custom grid set
            // Wait, we should migrate or just show default?
            // Let's just show top apps but NOT persist them as "pinned" yet to keep it clean,
            // OR auto-populate logic. 
            // For now, let's auto-populate if empty to ensure it's not blank on first run.
            val defaults = allApps.take(37)
            _flowerApps.value = defaults
            
            // Optionally auto-populate persistence so removal works immediately
            if (allApps.isNotEmpty()) {
                flowerGridManager.setGridApps(defaults.map { it.packageName })
            }
        } else {
            // Map packages to actual AppModels
            val mappedApps = gridPackages.mapNotNull { pkg ->
                allApps.find { it.packageName == pkg }
            }
            _flowerApps.value = mappedApps
        }
    }
    
    fun addToGrid(app: AppModel) {
        if (flowerGridManager.addToGrid(app.packageName)) {
            updateFlowerGrid()
        }
    }
    
    fun removeFromGrid(app: AppModel) {
        flowerGridManager.removeFromGrid(app.packageName)
        updateFlowerGrid()
    }
    
    fun selectCategory(category: AppCategory?) {
        _selectedCategory.value = category
        updateAppList()
    }
    
    // Unified search logic in onSearchQueryChanged
    
    private fun updateAppList() {
        val category = _selectedCategory.value
        if (category != null) {
            _apps.value = allApps.filter { it.category == category }
        } else {
            _apps.value = allApps
        }
    }
    
    // Neural Hub State
    data class NeuralHubState(
        val batteryLevel: Int = 0,
        val isCharging: Boolean = false,
        val batteryTemp: Float = 0f,
        val batteryHealth: String = "Unknown",
        val batteryVoltage: Int = 0,
        val batteryTech: String = "Unknown",
        val ramPercent: Int = 0,
        val ramText: String = "",
        val ramUsed: Long = 0,
        val ramTotal: Long = 0,
        val storagePercent: Int = 0,
        val storageText: String = "",
        val storageUsed: Long = 0,
        val storageTotal: Long = 0,
        val cpuLoad: Int = 0
    )
    
    private val _neuralHubState = MutableStateFlow(NeuralHubState())
    val neuralHubState: StateFlow<NeuralHubState> = _neuralHubState.asStateFlow()
    
    private var hubUpdateJob: kotlinx.coroutines.Job? = null
    private val systemMonitor = com.example.launcher.utils.SystemMonitor(application)
    
    fun startHubUpdates() {
        if (hubUpdateJob?.isActive == true) return
        
        hubUpdateJob = viewModelScope.launch {
            while (coroutineContext.isActive) {
                updateHubStats()
                kotlinx.coroutines.delay(1000)
            }
        }
    }
    
    fun boostRam() {
        viewModelScope.launch {
            // 1. Suggest GC
            System.gc()
            // 2. Clear internal caches if any
            // 3. Update stats immediately
            updateHubStats()
        }
    }
    
    private fun updateHubStats() {
        val battery = systemMonitor.getBatteryInfo()
        val ram = systemMonitor.getRamInfo()
        val storage = systemMonitor.getStorageInfo()
        val cpu = systemMonitor.getCpuLoad()

        val newState = NeuralHubState(
            batteryLevel   = battery.level,
            isCharging     = battery.isCharging,
            batteryTemp    = battery.temperature,
            batteryHealth  = battery.health,
            batteryVoltage = battery.voltage,
            batteryTech    = battery.technology,
            ramPercent     = ram.percentUsed,
            ramText        = "${formatSize(ram.used)} / ${formatSize(ram.total)}",
            ramUsed        = ram.used,
            ramTotal       = ram.total,
            storagePercent = storage.percentUsed,
            storageText    = "${formatSize(storage.used)} / ${formatSize(storage.total)}",
            storageUsed    = storage.used,
            storageTotal   = storage.total,
            cpuLoad        = cpu
        )
        _neuralHubState.value = newState
        pushCpuSample(cpu)
        _neuralInsight.value = computeInsight(newState)
    }
    
    private fun formatSize(bytes: Long): String {
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return String.format("%.1f GB", gb)
    }

    // ─── Music Player ────────────────────────────────────────────────────────
    data class MusicState(
        val title: String = "",
        val artist: String = "",
        val isPlaying: Boolean = false,
        val albumArt: android.graphics.Bitmap? = null
    )

    private val _musicState = MutableStateFlow(MusicState())
    val musicState: StateFlow<MusicState> = _musicState.asStateFlow()

    private var mediaSessionManager: MediaSessionManager? = null
    private var musicPollJob: kotlinx.coroutines.Job? = null
    
    private fun getNotificationServiceComponent(): android.content.ComponentName {
        return android.content.ComponentName(getApplication<Application>(), com.example.launcher.service.LauncherNotificationService::class.java)
    }

    fun startMusicMonitor() {
        if (musicPollJob?.isActive == true) return
        mediaSessionManager = getApplication<Application>()
            .getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
        musicPollJob = viewModelScope.launch {
            while (coroutineContext.isActive) {
                refreshMusicState()
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    fun stopMusicMonitor() {
        musicPollJob?.cancel()
    }

    // ─── Dynamic Theme Engine ────────────────────────────────────────────────
    private val themeEngine = com.example.launcher.utils.ThemeEngine(application)

    private val _themeColors = MutableStateFlow(
        themeEngine.extractWallpaperColors()
            ?: themeEngine.getThemePreset(com.example.launcher.utils.ThemeEngine.ThemePreset.NEON_CYAN)
    )
    val themeColors: StateFlow<com.example.launcher.utils.ThemeEngine.DynamicThemeColors> = _themeColors.asStateFlow()

    fun refreshTheme() {
        _themeColors.value = themeEngine.extractWallpaperColors()
            ?: themeEngine.getThemePreset(com.example.launcher.utils.ThemeEngine.ThemePreset.NEON_CYAN)
    }

    fun applyPreset(preset: com.example.launcher.utils.ThemeEngine.ThemePreset) {
        _themeColors.value = themeEngine.getThemePreset(preset)
    }

    // ─── CPU History Ring Buffer (for Phase 11 graph) ──────────────────────
    private val _cpuHistory = MutableStateFlow<List<Int>>(emptyList())
    val cpuHistory: StateFlow<List<Int>> = _cpuHistory.asStateFlow()

    private val _neuralInsight = MutableStateFlow("System running optimally")
    val neuralInsight: StateFlow<String> = _neuralInsight.asStateFlow()

    private fun pushCpuSample(value: Int) {
        val current = _cpuHistory.value.toMutableList()
        current.add(value)
        if (current.size > 30) current.removeAt(0)  // Keep last 30 samples
        _cpuHistory.value = current
    }

    private fun computeInsight(state: NeuralHubState): String {
        return when {
            state.batteryLevel < 15 && !state.isCharging -> "🔋 Critical battery – plug in soon!"
            state.batteryLevel < 30 && !state.isCharging -> "⚡ Low battery – connect charger"
            state.ramPercent > 85 -> "💾 High RAM usage – try closing background apps"
            state.cpuLoad > 80 -> "⚠️ High CPU – some apps are working hard"
            state.storagePercent > 90 -> "📂 Storage almost full – consider cleaning up"
            state.isCharging && state.batteryLevel == 100 -> "✅ Fully charged! Safe to unplug"
            state.isCharging -> "⚡ Charging… ${state.batteryLevel}% - ${100 - state.batteryLevel}% to go"
            else -> "🛡️ System running optimally"
        }
    }

    // ─── Privacy Shield ───────────────────────────────────────────────────────
    private val _lockedApps = MutableStateFlow(preferencesManager.getLockedApps())
    val lockedApps: StateFlow<Set<String>> = _lockedApps.asStateFlow()

    fun lockApp(app: AppModel) {
        preferencesManager.lockApp(app.packageName)
        _lockedApps.value = preferencesManager.getLockedApps()
    }

    fun unlockApp(app: AppModel) {
        preferencesManager.unlockApp(app.packageName)
        _lockedApps.value = preferencesManager.getLockedApps()
    }

    fun isAppLocked(app: AppModel): Boolean = preferencesManager.isAppLocked(app.packageName)

    // ─── Notification Bridge ────────────────────────────────────────────────
    private val _notificationCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val notificationCounts: StateFlow<Map<String, Int>> = _notificationCounts.asStateFlow()

    private val notificationReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            updateNotificationCounts()
        }
    }

    private fun updateNotificationCounts() {
        val counts = mutableMapOf<String, Int>()
        // We can't directy access the service instance easily, but we can have the service 
        // provide the counts. The existing service uses a static map for now as a simple bridge.
        val packages = apps.value.map { it.packageName }
        for (pkg in packages) {
            val count = com.example.launcher.service.LauncherNotificationService.getNotificationCount(pkg)
            if (count > 0) counts[pkg] = count
        }
        _notificationCounts.value = counts
    }

    // ─── Folder Support ──────────────────────────────────────────────────────
    private val _folders = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val folders: StateFlow<Map<String, Set<String>>> = _folders.asStateFlow()

    fun createFolder(name: String, packageNames: Set<String> = emptySet()) {
        val current = _folders.value.toMutableMap()
        current[name] = packageNames
        preferencesManager.saveFolders(current)
        _folders.value = current
    }

    fun addAppToFolder(folderName: String, packageName: String) {
        preferencesManager.addAppToFolder(folderName, packageName)
        _folders.value = preferencesManager.getFolders()
    }

    fun removeAppFromFolder(folderName: String, packageName: String) {
        preferencesManager.removeAppFromFolder(folderName, packageName)
        _folders.value = preferencesManager.getFolders()
    }

    fun deleteFolder(folderName: String) {
        preferencesManager.deleteFolder(folderName)
        _folders.value = preferencesManager.getFolders()
    }

    // ─── App Shortcuts ──────────────────────────────────────────────────────
    fun loadShortcutsForApp(packageName: String) {
        viewModelScope.launch {
            try {
                val query = LauncherApps.ShortcutQuery().apply {
                    setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or 
                                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or 
                                LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
                    setPackage(packageName)
                }
                
                val shortcutList = launcherApps.getShortcuts(query, Process.myUserHandle()) ?: emptyList()
                _shortcuts.value = shortcutList.map { info ->
                    AppShortcut(
                        id = info.id,
                        label = (info.shortLabel ?: info.longLabel ?: "Shortcut").toString(),
                        icon = try { launcherApps.getShortcutIconDrawable(info, 0) } catch (e: Exception) { null },
                        packageName = packageName
                    )
                }
            } catch (e: Exception) {
                _shortcuts.value = emptyList()
            }
        }
    }

    fun launchShortcut(shortcut: AppShortcut) {
        try {
            launcherApps.startShortcut(shortcut.packageName, shortcut.id, null, null, Process.myUserHandle())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearShortcuts() {
        _shortcuts.value = emptyList()
    }

    private fun refreshMusicState() {
        try {
            val mgr = mediaSessionManager ?: return
            val sessions: List<MediaController> = mgr.getActiveSessions(getNotificationServiceComponent())
            val active = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: sessions.firstOrNull()
            if (active == null) {
                _musicState.value = MusicState()
                return
            }
            val meta = active.metadata
            _musicState.value = MusicState(
                title = meta?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "",
                artist = meta?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST)
                    ?: meta?.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: "",
                isPlaying = active.playbackState?.state == PlaybackState.STATE_PLAYING,
                albumArt = meta?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
            )
        } catch (e: Exception) {
            // SecurityException if MEDIA_CONTENT_CONTROL not granted – fail silently
            e.printStackTrace()
        }
    }

    fun musicPlayPause() {
        try {
            val sessions: List<MediaController> = mediaSessionManager?.getActiveSessions(getNotificationServiceComponent()) ?: return
            val active = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: sessions.firstOrNull() ?: return
            val controls = active.transportControls
            if (active.playbackState?.state == PlaybackState.STATE_PLAYING) controls.pause()
            else controls.play()
            viewModelScope.launch {
                kotlinx.coroutines.delay(300)
                refreshMusicState()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun musicSkipNext() {
        try {
            val sessions: List<MediaController> = mediaSessionManager?.getActiveSessions(getNotificationServiceComponent()) ?: return
            val active = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: sessions.firstOrNull()
            active?.transportControls?.skipToNext()
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun musicSkipPrev() {
        try {
            val sessions: List<MediaController> = mediaSessionManager?.getActiveSessions(getNotificationServiceComponent()) ?: return
            val active = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: sessions.firstOrNull()
            active?.transportControls?.skipToPrevious()
        } catch (e: Exception) { e.printStackTrace() }
    }
    
    // Flow Launcher Features State
    
    // Flow Launcher Features State
    
    // Universal Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val searchManager = com.example.launcher.utils.SearchManager(application)
    private val _searchResults = MutableStateFlow<List<com.example.launcher.utils.SearchManager.SearchResult>>(emptyList())
    val searchResults: StateFlow<List<com.example.launcher.utils.SearchManager.SearchResult>> = _searchResults.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    
    fun onSearchQueryChanged(query: String) {
        android.util.Log.d("HomeViewModel", "Search query changed: '$query'")
        _searchQuery.value = query
        viewModelScope.launch {
            if (query.isBlank()) {
                android.util.Log.d("HomeViewModel", "Query blank, clearing results")
                _searchResults.value = emptyList()
                _isSearching.value = false
            } else {
                android.util.Log.d("HomeViewModel", "Calling searchManager.search()")
                _isSearching.value = true
                val results = searchManager.search(query)
                android.util.Log.d("HomeViewModel", "Search returned ${results.size} results")
                _searchResults.value = results
                _isSearching.value = false
            }
        }
    }
    
    fun performSearchAction(action: com.example.launcher.utils.SearchManager.SearchAction) {
        viewModelScope.launch {
            try {
                when (action) {
                    is com.example.launcher.utils.SearchManager.SearchAction.LaunchApp -> {
                        val intent = getApplication<Application>().packageManager.getLaunchIntentForPackage(action.packageName)
                        intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent?.let { getApplication<Application>().startActivity(it) }
                    }
                    is com.example.launcher.utils.SearchManager.SearchAction.WebSearch -> {
                        val url = searchManager.getSearchUrl(action.query, action.provider)
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        getApplication<Application>().startActivity(intent)
                    }
                    is com.example.launcher.utils.SearchManager.SearchAction.CallContact -> {
                        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:${action.phoneNumber}"))
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        getApplication<Application>().startActivity(intent)
                    }
                    is com.example.launcher.utils.SearchManager.SearchAction.MessageContact -> {
                         val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("sms:${action.phoneNumber}"))
                         intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                         getApplication<Application>().startActivity(intent)
                    }
                    is com.example.launcher.utils.SearchManager.SearchAction.OpenSetting -> {
                        val intent = android.content.Intent(action.settingAction)
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        getApplication<Application>().startActivity(intent)
                    }
                    is com.example.launcher.utils.SearchManager.SearchAction.QuickDial -> {
                         val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:${action.number}"))
                         intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                         getApplication<Application>().startActivity(intent)
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // Weather
    data class WeatherState(
        val temp: String = "--°",
        val condition: String = "Loading...",
        val iconRes: Int = android.R.drawable.ic_menu_today // Default fallback icon
    )
    
    private val _weatherState = MutableStateFlow(WeatherState())
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()
    
    private val weatherRepository = WeatherRepository()
    private val locationHelper = com.example.launcher.logic.LocationHelper(application)
    
    private fun fetchWeather() {
        viewModelScope.launch {
            // Try to get real location first
            val location = locationHelper.getLastKnownLocation() 
                ?: locationHelper.getCurrentLocation()
            
            // Use real location or fallback to London
            val lat = location?.latitude ?: 51.5074
            val long = location?.longitude ?: -0.1278
            
            val response = weatherRepository.getCurrentWeather(lat, long)
            response?.current?.let { current ->
                val conditionText = interpretWeatherCode(current.weatherCode)
                val icon = getWeatherIcon(current.weatherCode)
                
                _weatherState.value = WeatherState(
                    temp = "${current.temperature}°",
                    condition = conditionText,
                    iconRes = icon
                )
            }
        }
    }
    
    // Public method to refresh weather after permission grant
    fun refreshWeather() {
        fetchWeather()
    }
    
    // Simple WMO Code interpretation
    private fun interpretWeatherCode(code: Int): String {
        return when (code) {
            0 -> "Clear Sky"
            1, 2, 3 -> "Partly Cloudy"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            71, 73, 75 -> "Snow"
            95, 96, 99 -> "Thunderstorm"
            else -> "Unknown"
        }
    }
    
    private fun getWeatherIcon(code: Int): Int {
        return when (code) {
            0 -> android.R.drawable.ic_menu_day
            1, 2, 3 -> android.R.drawable.ic_menu_sort_by_size // Cloud-like
            else -> android.R.drawable.ic_menu_report_image // Rain-like
        }
    }
    
    // Voice Assistant
    private val voiceManager = VoiceManager(application)
    val isVoiceListening: StateFlow<Boolean> = voiceManager.isListening
    
    private val _isVoiceEnabled = MutableStateFlow(false)
    val isVoiceEnabled: StateFlow<Boolean> = _isVoiceEnabled.asStateFlow()
    
    fun setVoiceListening(listening: Boolean) {
        // Update persistent state & UI toggle state
        prefs.edit().putBoolean("voice_assistant_enabled", listening).apply()
        _isVoiceEnabled.value = listening
        
        if (listening) {
            voiceManager.startListening()
        } else {
            voiceManager.stopListening()
        }
    }
    
    fun toggleVoiceEnabled() {
        val current = _isVoiceEnabled.value
        setVoiceListening(!current)
    }
    
    // SharedPreferences for settings - must use default prefs to match PreferenceFragmentCompat
    private val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(application)
    
    init {
        // Voice Callback
        voiceManager.onSpeechResult = { text ->
             processVoiceCommand(text)
        }
        
        // Fetch real weather
        fetchWeather()
        
        // Fetch Calendar Events
        fetchTodayEvents()
        
        // Start System Monitor if enabled
        if (prefs.getBoolean("neural_hub_enabled", true)) {
            startHubUpdates()
        }
        
        // Start Voice Assistant if enabled
        if (prefs.getBoolean("voice_assistant_enabled", true)) {
            setVoiceListening(true)
        }

        // Notification Bridge
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(application)
            .registerReceiver(notificationReceiver, android.content.IntentFilter(com.example.launcher.service.LauncherNotificationService.ACTION_NOTIFICATION_CHANGED))
        updateNotificationCounts()
    }
    
    // Public method to reload settings (call from Activity onResume)
    fun reloadSettings() {
        // Voice
        val voiceEnabled = prefs.getBoolean("voice_assistant_enabled", true)
        // Only start if enabled. If disabled, stop.
        setVoiceListening(voiceEnabled)
        
        // Refresh App List (in case Hidden Apps changed)
        loadApps()
        
        // Neural Hub
        val hubEnabled = prefs.getBoolean("neural_hub_enabled", true)
        if (hubEnabled && hubUpdateJob?.isActive != true) {
            startHubUpdates()
        } else if (!hubEnabled) {
            hubUpdateJob?.cancel()
        }

        // Refresh Notifications
        updateNotificationCounts()

        // Refresh Folders
        _folders.value = preferencesManager.getFolders()
    }
    
    // Call from Activity onPause to save battery
    fun onActivityPause() {
        // Stop listening (system will kill it anyway if we don't)
        voiceManager.stopListening()
        
        // Stop updating hub stats since they aren't visible
        hubUpdateJob?.cancel()
    }
    
    private fun processVoiceCommand(text: String) {
        val command = text.trim().lowercase()
        
        // Wake word detection: "SUNDAY"
        val wakeWord = "sunday"
        
        if (command.startsWith(wakeWord)) {
            // Extract command after wake word
            val actualCommand = command.removePrefix(wakeWord).trim()
            
            if (actualCommand.isEmpty()) {
                // User just said "SUNDAY" - acknowledge but wait for more
                return
            }
            
            // Process the actual command
            executeVoiceCommand(actualCommand)
        }
        // If no wake word, ignore the speech (background noise detection)
    }
    
    private fun executeVoiceCommand(command: String) {
        // Command: Play Music
        if (command == "play music" || command == "play some music" || command.contains("play music")) {
            try {
                val intent = android.content.Intent.makeMainSelectorActivity(
                    android.content.Intent.ACTION_MAIN,
                    android.content.Intent.CATEGORY_APP_MUSIC
                )
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                getApplication<Application>().startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } 
        // Command: Search Google
        else if (command.startsWith("search") || command.startsWith("google")) {
            val query = command.removePrefix("search").removePrefix("google").trim()
            if (query.isNotEmpty()) {
                performSearchAction(
                    com.example.launcher.utils.SearchManager.SearchResult(
                        type = com.example.launcher.utils.SearchManager.ResultType.WEB_SEARCH,
                        id = "voice_web",
                        title = query,
                        action = com.example.launcher.utils.SearchManager.SearchAction.WebSearch(query, "google")
                    ).action
                )
            }
        }
        // Command: Open App
        else if (command.startsWith("open")) {
            val appName = command.removePrefix("open").trim()
            onSearchQueryChanged(appName)
            
            viewModelScope.launch {
                // Find and launch the first matching app
                val results = searchManager.search(appName)
                val appResult = results.firstOrNull { it.type == com.example.launcher.utils.SearchManager.ResultType.APP }
                
                appResult?.let { 
                    performSearchAction(it.action)
                }
            }
        }
        // Fallback: Update search with the command text
        else {
            onSearchQueryChanged(command)
        }
    }

    // ─── Calendar Events ───────────────────────────────────────────────────
    data class CalendarEvent(
        val title: String,
        val startTimeMs: Long,
        val endTimeMs: Long,
        val location: String?,
        val color: Int?
    )
    
    private val _todayEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val todayEvents: StateFlow<List<CalendarEvent>> = _todayEvents.asStateFlow()
    
    fun fetchTodayEvents() {
        if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        viewModelScope.launch {
            try {
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startOfDay = cal.timeInMillis
                
                cal.add(Calendar.DAY_OF_MONTH, 1)
                val endOfDay = cal.timeInMillis
                
                val projection = arrayOf(
                    CalendarContract.Events._ID,
                    CalendarContract.Events.TITLE,
                    CalendarContract.Events.DTSTART,
                    CalendarContract.Events.DTEND,
                    CalendarContract.Events.EVENT_LOCATION,
                    CalendarContract.Events.DISPLAY_COLOR
                )
                
                // Query events that start OR end within today, or span across today
                val selection = "(${CalendarContract.Events.DTSTART} < ?) AND (${CalendarContract.Events.DTEND} >= ?)"
                val selectionArgs = arrayOf(endOfDay.toString(), startOfDay.toString())
                
                val uri = CalendarContract.Events.CONTENT_URI
                val cursor = getApplication<Application>().contentResolver.query(
                    uri,
                    projection,
                    selection,
                    selectionArgs,
                    "${CalendarContract.Events.DTSTART} ASC"
                )
                
                val events = mutableListOf<CalendarEvent>()
                cursor?.use {
                    val titleIdx = it.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                    val startIdx = it.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                    val endIdx = it.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
                    val locIdx = it.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)
                    val colorIdx = it.getColumnIndex(CalendarContract.Events.DISPLAY_COLOR)
                    
                    while (it.moveToNext()) {
                        events.add(
                            CalendarEvent(
                                title = it.getString(titleIdx) ?: "No Title",
                                startTimeMs = it.getLong(startIdx),
                                endTimeMs = it.getLong(endIdx),
                                location = if (locIdx >= 0) it.getString(locIdx) else null,
                                color = if (colorIdx >= 0 && !it.isNull(colorIdx)) it.getInt(colorIdx) else null
                            )
                        )
                    }
                }
                _todayEvents.value = events
            } catch (e: Exception) {
                e.printStackTrace()
                _todayEvents.value = emptyList()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(getApplication())
            .unregisterReceiver(notificationReceiver)
        hubUpdateJob?.cancel()
        musicPollJob?.cancel()
    }
}
