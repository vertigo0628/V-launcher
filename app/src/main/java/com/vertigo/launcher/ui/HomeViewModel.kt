package com.vertigo.launcher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vertigo.launcher.data.AppRepository
import com.vertigo.launcher.model.AppModel
import com.vertigo.launcher.model.AppCategory
import com.vertigo.launcher.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.collect
import com.vertigo.launcher.logic.VoiceManager
import com.vertigo.launcher.data.WeatherRepository
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
import java.util.Locale
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import android.content.ClipboardManager
import android.content.ClipData
import android.hardware.camera2.CameraManager
import android.media.MediaPlayer
import android.net.Uri
import android.provider.MediaStore

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    // --- Models ---
    data class AppShortcut(
        val id: String,
        val label: String,
        val icon: Drawable?,
        val packageName: String
    )

    data class CalendarEvent(
        val title: String,
        val startTimeMs: Long,
        val endTimeMs: Long,
        val location: String?,
        val color: Int?
    )

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

    data class MusicState(
        val title: String = "",
        val artist: String = "",
        val isPlaying: Boolean = false,
        val albumArt: android.graphics.Bitmap? = null,
        val currentPosition: Long = 0L,
        val duration: Long = 0L
    )

    data class WeatherState(
        val temp: String = "--°",
        val condition: String = "Loading...",
        val iconRes: Int = android.R.drawable.ic_menu_today
    )

    // --- Managers & Repositories ---
    private val repository = AppRepository(application)
    private val flowerGridManager = com.vertigo.launcher.utils.FlowerGridManager(application)
    private val preferencesManager = com.vertigo.launcher.utils.PreferencesManager(application)
    private val launcherApps = application.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val voiceManager = VoiceManager(application)
    private val searchManager = com.vertigo.launcher.utils.SearchManager(application)
    private val ollamaClient = com.vertigo.launcher.logic.OllamaClient()
    private val visionAnalyzer = com.vertigo.launcher.logic.VisionAnalyzer(application)
    private val systemMonitor = com.vertigo.launcher.utils.SystemMonitor(application)
    private val themeEngine = com.vertigo.launcher.utils.ThemeEngine(application)
    private val weatherRepository = WeatherRepository(application)
    private val locationHelper = com.vertigo.launcher.logic.LocationHelper(application)
    private val neuralInsightRepository = com.vertigo.launcher.data.NeuralInsightRepository(application)
    private val smartUsageManager = com.vertigo.launcher.utils.SmartUsageManager(application)
    private val prefs = application.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)

    // Shizuku state
    val shizukuState = com.vertigo.launcher.utils.ShizukuSetup.state
    
    private val _frozenApps = MutableStateFlow<Set<String>>(emptySet())
    val frozenApps: StateFlow<Set<String>> = _frozenApps.asStateFlow()
    
    private val _frozenAppsList = MutableStateFlow<List<AppModel>>(emptyList())
    val frozenAppsList: StateFlow<List<AppModel>> = _frozenAppsList.asStateFlow()
    
    private val _shizukuActionResult = MutableStateFlow<String?>(null)
    val shizukuActionResult: StateFlow<String?> = _shizukuActionResult.asStateFlow()

    // --- State Flows ---
    private val _shortcuts = MutableStateFlow<List<AppShortcut>>(emptyList())
    val shortcuts: StateFlow<List<AppShortcut>> = _shortcuts.asStateFlow()
    
    private var cachedFullApps: List<AppModel>? = null
    private var allApps: List<AppModel> = emptyList()
    
    private val _apps = MutableStateFlow<List<AppModel>>(emptyList())
    val apps: StateFlow<List<AppModel>> = _apps.asStateFlow()

    private val _hiddenAppsList = MutableStateFlow<List<AppModel>>(emptyList())
    val hiddenAppsList: StateFlow<List<AppModel>> = _hiddenAppsList.asStateFlow()
    
    private val _flowerApps = MutableStateFlow<List<AppModel>>(emptyList())
    val flowerApps: StateFlow<List<AppModel>> = _flowerApps.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow<AppCategory?>(null)
    val selectedCategory: StateFlow<AppCategory?> = _selectedCategory.asStateFlow()
    
    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen: StateFlow<Boolean> = _isDrawerOpen.asStateFlow()
    
    fun setDrawerOpen(isOpen: Boolean) {
        _isDrawerOpen.value = isOpen
    }

    private val _isNeuralNavEnabled = MutableStateFlow(prefs.getBoolean("neural_nav_enabled", false))
    val isNeuralNavEnabled: StateFlow<Boolean> = _isNeuralNavEnabled.asStateFlow()

    fun setNeuralNavEnabled(enabled: Boolean) {
        _isNeuralNavEnabled.value = enabled
        prefs.edit().putBoolean("neural_nav_enabled", enabled).apply()
        viewModelScope.launch {
            if (enabled) {
                com.vertigo.launcher.logic.AppCommander.hideNavigationBar()
            } else {
                com.vertigo.launcher.logic.AppCommander.showNavigationBar()
            }
        }
    }

    private val _todayEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val todayEvents: StateFlow<List<CalendarEvent>> = _todayEvents.asStateFlow()

    private val _neuralHubState = MutableStateFlow(NeuralHubState())
    val neuralHubState: StateFlow<NeuralHubState> = _neuralHubState.asStateFlow()

    private val _dailyInsight = MutableStateFlow<List<String>>(emptyList())
    val dailyInsight: StateFlow<List<String>> = _dailyInsight.asStateFlow()

    private val _currentHoliday = MutableStateFlow<String?>(null)
    val currentHoliday: StateFlow<String?> = _currentHoliday.asStateFlow()

    private val _cosmicInsight = MutableStateFlow<String?>("Scanning Cosmos...")
    val cosmicInsight: StateFlow<String?> = _cosmicInsight.asStateFlow()

    // Insight Preferences
    private val _insightPrefs = MutableStateFlow(mapOf(
        "Tech" to true,
        "Space" to true,
        "History" to false,
        "Science" to true,
        "Innovators" to true
    ))
    val insightPrefs: StateFlow<Map<String, Boolean>> = _insightPrefs.asStateFlow()

    fun toggleInsightPref(category: String) {
        val current = _insightPrefs.value.toMutableMap()
        current[category] = !(current[category] ?: false)
        _insightPrefs.value = current
        // Save to prefs
        prefs.edit().putBoolean("pref_insight_$category", current[category] ?: false).apply()
        // Refresh insights with new filter
        fetchNeuralInsights()
    }

    private val _musicState = MutableStateFlow(MusicState())
    val musicState: StateFlow<MusicState> = _musicState.asStateFlow()

    private val _themeColors = MutableStateFlow(
        themeEngine.extractWallpaperColors()
            ?: themeEngine.getThemePreset(com.vertigo.launcher.utils.ThemeEngine.ThemePreset.NEON_CYAN)
    )
    val themeColors: StateFlow<com.vertigo.launcher.utils.ThemeEngine.DynamicThemeColors> = _themeColors.asStateFlow()

    private val _cpuHistory = MutableStateFlow<List<Int>>(emptyList())
    val cpuHistory: StateFlow<List<Int>> = _cpuHistory.asStateFlow()

    private val _neuralInsight = MutableStateFlow("System running optimally")
    val neuralInsight: StateFlow<String> = _neuralInsight.asStateFlow()

    private val _lockedApps = MutableStateFlow(preferencesManager.getLockedApps())
    val lockedApps: StateFlow<Set<String>> = _lockedApps.asStateFlow()

    private val _notificationCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val notificationCounts: StateFlow<Map<String, Int>> = _notificationCounts.asStateFlow()
    
    // Packages to suppress notification updates for a short time after launch
    private val suppressedPackages = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val SUPPRESSION_MS = 5000L // 5 seconds grace period

    private val _folders = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val folders: StateFlow<Map<String, Set<String>>> = _folders.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()
    
    // Photo thumbnails for chat messages (keyed by message timestamp)
    private val _photoBitmaps = mutableMapOf<Long, android.graphics.Bitmap>()
    fun getPhotoBitmap(timestamp: Long): android.graphics.Bitmap? = _photoBitmaps[timestamp]
    
    private val _currentStreamingResponse = MutableStateFlow<String?>(null)
    val currentStreamingResponse: StateFlow<String?> = _currentStreamingResponse.asStateFlow()
    
    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private var aiQueryJob: kotlinx.coroutines.Job? = null

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<com.vertigo.launcher.utils.SearchManager.SearchResult>>(emptyList())
    val searchResults: StateFlow<List<com.vertigo.launcher.utils.SearchManager.SearchResult>> = _searchResults.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _weatherState = MutableStateFlow(WeatherState())
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()

    private val _isVoiceEnabled = MutableStateFlow(false)
    val isVoiceEnabled: StateFlow<Boolean> = _isVoiceEnabled.asStateFlow()

    val isVoiceListening: StateFlow<Boolean> = voiceManager.isListening
    val spokenText: StateFlow<String> = voiceManager.spokenText

    private val _isHotwordActive = MutableStateFlow(false)
    val isHotwordActive: StateFlow<Boolean> = _isHotwordActive.asStateFlow()

    private val _showLabels = MutableStateFlow(true)
    val showLabels: StateFlow<Boolean> = _showLabels.asStateFlow()

    private val _showBadges = MutableStateFlow(true)
    val showBadges: StateFlow<Boolean> = _showBadges.asStateFlow()

    // Other Properties
    private var _flashlightState = false
    private var hubUpdateJob: kotlinx.coroutines.Job? = null
    private var musicPollJob: kotlinx.coroutines.Job? = null
    private var mediaSessionManager: MediaSessionManager? = null

    // Local Music State
    private var localMediaPlayer: MediaPlayer? = null
    private var localMusicQueue: List<android.net.Uri> = emptyList()
    private var localMusicTitles: List<String> = emptyList()
    private var localMusicArtists: List<String> = emptyList()
    private var currentMusicIndex = 0
    
    // Audio Focus — ensures we pause when another app plays or phone rings
    private var audioFocusRequest: android.media.AudioFocusRequest? = null
    private val audioFocusChangeListener = android.media.AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            android.media.AudioManager.AUDIOFOCUS_LOSS,
            android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Another app took focus or phone is ringing — pause
                if (localMediaPlayer?.isPlaying == true) {
                    localMediaPlayer?.pause()
                    _musicState.value = _musicState.value.copy(isPlaying = false)
                }
            }
            android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Lower volume temporarily (e.g. notification sound)
                localMediaPlayer?.setVolume(0.2f, 0.2f)
            }
            android.media.AudioManager.AUDIOFOCUS_GAIN -> {
                // Regained focus — restore volume
                localMediaPlayer?.setVolume(1f, 1f)
            }
        }
    }
    
    // Voice State
    private var hotwordResetJob: kotlinx.coroutines.Job? = null

    private val launcherCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: android.os.UserHandle) { loadApps(true) }
        override fun onPackageAdded(packageName: String?, user: android.os.UserHandle) { loadApps(true) }
        override fun onPackageChanged(packageName: String?, user: android.os.UserHandle) { loadApps(true) }
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: android.os.UserHandle, replacing: Boolean) { loadApps(true) }
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: android.os.UserHandle, replacing: Boolean) { loadApps(true) }
    }

    private val notificationReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            updateNotificationCounts()
        }
    }

    init {
        // Core data load
        loadApps(true)
        
        // Register for system app changes
        launcherApps.registerCallback(launcherCallback)

        // Voice Assistant Setup
        voiceManager.onSpeechResult = { text ->
             processVoiceCommand(text)
        }
        
        // Initial Fetchers
        fetchWeather()
        fetchTodayEvents()
        
        // Monitors & Services are started lazily via onForeground()
        // to avoid running background loops before lifecycle connects

        // Notification Bridge
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(getApplication())
            .registerReceiver(notificationReceiver, android.content.IntentFilter(com.vertigo.launcher.service.LauncherNotificationService.ACTION_NOTIFICATION_CHANGED))
        updateNotificationCounts()
        
        // Start Local LLM Backend silently
        startOllamaBackend()
        
        // Folders initialization
        _folders.value = preferencesManager.getFolders()
        
        reloadSettings()
    }

    // --- Lifecycle Management ---
    
    fun onForeground() {
        reloadSettings()
        if (prefs.getBoolean("neural_hub_enabled", true)) {
            startHubUpdates()
        }
        startMusicMonitor()
        if (prefs.getBoolean("voice_assistant_enabled", true)) {
            setVoiceListening(true)
        }
        
        // Refresh time-sensitive data
        fetchTodayEvents()
        fetchWeather()
        fetchNeuralInsights()
    }
    
    fun onBackground() {
        // Halt heavy CPU/polling tasks when launcher is hidden
        hubUpdateJob?.cancel()
        hubUpdateJob = null
        stopMusicMonitor()
        setVoiceListening(false)
    }

    private fun fetchNeuralInsights() {
        val countryCode = Locale.getDefault().country.ifBlank { "KE" }
        
        viewModelScope.launch {
            _currentHoliday.value = neuralInsightRepository.getHolidayForToday(countryCode)
        }
        
        viewModelScope.launch {
            _dailyInsight.value = neuralInsightRepository.getDailyInsights(_insightPrefs.value)
        }
        
        viewModelScope.launch {
            _cosmicInsight.value = neuralInsightRepository.getCosmicInsight()
        }
    }

    fun loadApps(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val fullList = if (forceRefresh || cachedFullApps == null) {
                val list = repository.getInstalledApps()
                cachedFullApps = list
                list
            } else {
                cachedFullApps!!
            }
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
        val maxApps = flowerGridManager.getMaxGridApps()
        val excludedApps = flowerGridManager.getExcludedApps()
        
        // Map user's explicitly pinned apps into the core slots
        val mappedApps = gridPackages.mapNotNull { pkg ->
            allApps.find { it.packageName == pkg }
        }.toMutableList()
        
        // Smart Auto-Fill: Only fill if user has fewer pinned than max,
        // and use category-diverse, usage-ranked suggestions instead of dumping everything.
        // CRITICAL: Skip apps the user explicitly removed (exclusion list)
        val needed = maxApps - mappedApps.size
        if (needed > 0) {
            val unpinnedApps = allApps.filter { app ->
                !gridPackages.contains(app.packageName) &&
                !excludedApps.contains(app.packageName)
            }
            val smartFill = smartUsageManager.getDiverseSuggestions(unpinnedApps, needed)
            mappedApps.addAll(smartFill)
        }
        
        // First run behavior: auto-seed with top smart suggestions if grid is empty
        if (gridPackages.isEmpty() && mappedApps.isNotEmpty()) {
            flowerGridManager.addToGrid(mappedApps[0].packageName)
        }
        
        _flowerApps.value = mappedApps
    }
    
    fun addToGrid(app: AppModel) {
        val maxApps = flowerGridManager.getMaxGridApps()
        val currentCount = flowerGridManager.getPinnedCount()
        if (currentCount >= maxApps) {
            // Grid is full — notify user
            android.widget.Toast.makeText(
                getApplication(),
                "Grid full ($maxApps max). Remove an app first.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (flowerGridManager.addToGrid(app.packageName)) {
            updateFlowerGrid()
        }
    }
    
    fun removeFromGrid(app: AppModel) {
        flowerGridManager.removeFromGrid(app.packageName)
        updateFlowerGrid()
    }
    
    /**
     * Record an app launch for smart usage tracking.
     * Called from MainActivity when any app is launched.
     */
    fun recordAppLaunch(packageName: String) {
        smartUsageManager.logAppLaunch(packageName)
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
    

    

    
    fun startHubUpdates() {
        if (hubUpdateJob?.isActive == true) return
        
        hubUpdateJob = viewModelScope.launch {
            while (coroutineContext.isActive) {
                updateHubStats()
                kotlinx.coroutines.delay(5000) // 5s — battery-optimized; system stats don't change fast
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



    
    private fun getNotificationServiceComponent(): android.content.ComponentName {
        return android.content.ComponentName(getApplication<Application>(), com.vertigo.launcher.service.LauncherNotificationService::class.java)
    }

    fun startMusicMonitor() {
        if (musicPollJob?.isActive == true) return
        mediaSessionManager = getApplication<Application>()
            .getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
        musicPollJob = viewModelScope.launch {
            while (coroutineContext.isActive) {
                refreshMusicState()
                kotlinx.coroutines.delay(3000) // 3s — battery-optimized media session poll
            }
        }
    }

    fun stopMusicMonitor() {
        musicPollJob?.cancel()
        musicPollJob = null
    }

    // ─── Dynamic Theme Engine ────────────────────────────────────────────────


    fun refreshTheme() {
        _themeColors.value = themeEngine.extractWallpaperColors()
            ?: themeEngine.getThemePreset(com.vertigo.launcher.utils.ThemeEngine.ThemePreset.NEON_CYAN)
    }

    fun applyPreset(preset: com.vertigo.launcher.utils.ThemeEngine.ThemePreset) {
        _themeColors.value = themeEngine.getThemePreset(preset)
    }

    // ─── CPU History Ring Buffer (for Phase 11 graph) ──────────────────────


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


    fun lockApp(app: AppModel) {
        preferencesManager.lockApp(app.packageName)
        _lockedApps.value = preferencesManager.getLockedApps()
    }

    fun unlockApp(app: AppModel) {
        preferencesManager.unlockApp(app.packageName)
        _lockedApps.value = preferencesManager.getLockedApps()
    }

    fun isAppLocked(app: AppModel): Boolean = preferencesManager.isAppLocked(app.packageName)



    // --- Multi-Select State ---
    private val _selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    val selectedPackages: StateFlow<Set<String>> = _selectedPackages.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    fun toggleSelection(packageName: String) {
        val current = _selectedPackages.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _selectedPackages.value = current
        if (current.isEmpty()) _isSelectionMode.value = false
    }

    fun enterSelectionMode(firstPackage: String) {
        _selectedPackages.value = setOf(firstPackage)
        _isSelectionMode.value = true
    }

    fun clearSelection() {
        _selectedPackages.value = emptySet()
        _isSelectionMode.value = false
    }

    fun batchHideApps() {
        val packages = _selectedPackages.value
        packages.forEach { preferencesManager.hideApp(it) }
        clearSelection()
        loadApps()
    }

    fun batchFreezeApps() {
        val packages = _selectedPackages.value
        viewModelScope.launch {
            packages.forEach { pkg ->
                com.vertigo.launcher.logic.AppCommander.freezeApp(pkg)
            }
            clearSelection()
            loadApps()
        }
    }

    fun batchUnfreezeApps() {
        val packages = _selectedPackages.value
        viewModelScope.launch {
            packages.forEach { pkg ->
                com.vertigo.launcher.logic.AppCommander.unfreezeApp(pkg)
            }
            clearSelection()
            loadApps()
        }
    }

    fun batchUnhideApps() {
        val packages = _selectedPackages.value
        packages.forEach { preferencesManager.unhideApp(it) }
        clearSelection()
        loadApps()
    }
    
    fun batchAddToGrid() {
        val packages = _selectedPackages.value
        var addedCount = 0
        packages.forEach { pkg ->
            if (flowerGridManager.addToGrid(pkg)) {
                addedCount++
            }
        }
        if (addedCount > 0) {
            updateFlowerGrid()
            _shizukuActionResult.value = "📌 Added $addedCount apps to grid"
        }
        clearSelection()
    }

    fun batchUninstallApps() {
        val packages = _selectedPackages.value
        val isShizukuAuthorized = shizukuState.value == com.vertigo.launcher.utils.ShizukuSetup.ShizukuState.AUTHORIZED
        
        if (isShizukuAuthorized) {
            // Silent uninstall via Shizuku (no prompts)
            viewModelScope.launch {
                var successCount = 0
                var failCount = 0
                packages.forEach { pkg ->
                    val result = com.vertigo.launcher.logic.AppCommander.silentUninstall(pkg)
                    if (result.isSuccess) successCount++ else failCount++
                }
                _shizukuActionResult.value = "🗑️ Uninstalled $successCount apps" + if (failCount > 0) " ($failCount failed)" else ""
                clearSelection()
                loadApps(true)
            }
        } else {
            // Standard uninstall via system intent (prompts user for each)
            val context = getApplication<Application>()
            packages.forEach { pkg ->
                val intent = android.content.Intent(android.content.Intent.ACTION_UNINSTALL_PACKAGE).apply {
                    data = android.net.Uri.parse("package:$pkg")
                    putExtra(android.content.Intent.EXTRA_RETURN_RESULT, true)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
            clearSelection()
            // Reload after a delay to let system process
            viewModelScope.launch {
                kotlinx.coroutines.delay(2000)
                loadApps(true)
            }
        }
    }

    private fun updateNotificationCounts() {
        val now = System.currentTimeMillis()
        
        // Clean up old suppressions
        val it = suppressedPackages.entries.iterator()
        while (it.hasNext()) {
            if (now - it.next().value > SUPPRESSION_MS) it.remove()
        }
        
        val counts = mutableMapOf<String, Int>()
        val packages = apps.value.map { it.packageName }
        for (pkg in packages) {
            // Skip if suppressed
            if (suppressedPackages.containsKey(pkg)) continue
            
            val count = com.vertigo.launcher.service.LauncherNotificationService.getNotificationCount(pkg)
            if (count > 0) counts[pkg] = count
        }
        _notificationCounts.value = counts
    }
    
    /**
     * Clear badge and suppress updates for a specific app (called when launching that app)
     */
    fun clearNotificationBadge(packageName: String) {
        suppressedPackages[packageName] = System.currentTimeMillis()
        val current = _notificationCounts.value.toMutableMap()
        current.remove(packageName)
        _notificationCounts.value = current
    }

    // ─── Folder Support ──────────────────────────────────────────────────────


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
            val component = getNotificationServiceComponent()
            val sessions: List<MediaController> = mgr.getActiveSessions(component)
            
            if (sessions.isEmpty()) {
                // Fallback to internal player state
                if (localMediaPlayer != null) {
                    val pos = try { localMediaPlayer?.currentPosition?.toLong() ?: 0L } catch (e: Exception) { 0L }
                    val dur = try { localMediaPlayer?.duration?.toLong() ?: 0L } catch (e: Exception) { 0L }
                    _musicState.value = MusicState(
                        title = localMusicTitles.getOrNull(currentMusicIndex) ?: "Local Music",
                        artist = localMusicArtists.getOrNull(currentMusicIndex) ?: "V-launcher",
                        isPlaying = localMediaPlayer?.isPlaying == true,
                        albumArt = _musicState.value.albumArt,
                        currentPosition = pos,
                        duration = dur
                    )
                } else {
                    _musicState.value = _musicState.value.copy(isPlaying = false)
                }
                return
            }
            
            val active = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: sessions.firstOrNull()
            
            if (active == null) {
                // Fallback to internal player state
                if (localMediaPlayer != null) {
                    val pos = try { localMediaPlayer?.currentPosition?.toLong() ?: 0L } catch (e: Exception) { 0L }
                    val dur = try { localMediaPlayer?.duration?.toLong() ?: 0L } catch (e: Exception) { 0L }
                    _musicState.value = MusicState(
                        title = localMusicTitles.getOrNull(currentMusicIndex) ?: "Local Music",
                        artist = localMusicArtists.getOrNull(currentMusicIndex) ?: "V-launcher",
                        isPlaying = localMediaPlayer?.isPlaying == true,
                        albumArt = _musicState.value.albumArt,
                        currentPosition = pos,
                        duration = dur
                    )
                } else {
                    _musicState.value = _musicState.value.copy(isPlaying = false)
                }
                return
            }
            
            try {
                val meta = active.metadata
                val playbackState = active.playbackState
                
                // Extract position and duration from external media session
                val position = playbackState?.position ?: 0L
                val duration = meta?.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION) ?: 0L
                
                _musicState.value = MusicState(
                    title = meta?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: _musicState.value.title,
                    artist = meta?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST)
                        ?: meta?.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM_ARTIST) 
                        ?: _musicState.value.artist,
                    isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING,
                    albumArt = try { meta?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART) } catch (e: Exception) { null } ?: _musicState.value.albumArt,
                    currentPosition = position,
                    duration = duration
                )
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error extracting metadata from session", e)
            }
        } catch (e: SecurityException) {
            // No permission – fail silently
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "Error refreshing music state", e)
        }
    }

    private fun promptNotificationAccess() {
        val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
        android.widget.Toast.makeText(getApplication<Application>(), "Please grant V-Launcher Notification Access to control music.", android.widget.Toast.LENGTH_LONG).show()
    }

    private fun fetchLocalMusic() {
        val context = getApplication<Application>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) return
        } else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) return
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST
        )
        // Select only music
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val uris = mutableListOf<Uri>()
        val titles = mutableListOf<String>()
        val artists = mutableListOf<String>()

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown Title"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val contentUri = android.content.ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                uris.add(contentUri)
                titles.add(title)
                artists.add(artist)
            }
        }
        
        localMusicQueue = uris
        localMusicTitles = titles
        localMusicArtists = artists
        currentMusicIndex = 0
    }

    private fun playLocalMusic() {
        if (localMusicQueue.isEmpty()) return
        val uri = localMusicQueue[currentMusicIndex]
        
        try {
            // Request Audio Focus before playing
            val audioManager = getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val focusRequest = android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = focusRequest
            
            val focusResult = audioManager.requestAudioFocus(focusRequest)
            if (focusResult != android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // Could not get focus — don't play
                return
            }
            
            if (localMediaPlayer == null) {
                localMediaPlayer = MediaPlayer()
                localMediaPlayer?.setOnCompletionListener {
                    musicSkipNext() // Auto-advance track
                }
            }
            localMediaPlayer?.reset()
            localMediaPlayer?.setDataSource(getApplication(), uri)
            localMediaPlayer?.prepare()
            localMediaPlayer?.start()
            
            // Re-trigger visual updates
            refreshMusicState()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun musicPlayPause() {
        try {
            val component = getNotificationServiceComponent()
            val sessions = try { mediaSessionManager?.getActiveSessions(component) } catch (e: Exception) { null }
            
            if (sessions.isNullOrEmpty()) {
                // Local Fallback
                if (localMediaPlayer?.isPlaying == true) {
                    localMediaPlayer?.pause()
                    _musicState.value = _musicState.value.copy(isPlaying = false)
                    // Abandon audio focus when pausing
                    audioFocusRequest?.let { req ->
                        val am = getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                        am.abandonAudioFocusRequest(req)
                    }
                } else {
                    if (localMusicQueue.isEmpty()) fetchLocalMusic()
                    if (localMediaPlayer != null) {
                        localMediaPlayer?.start()
                        _musicState.value = _musicState.value.copy(isPlaying = true)
                    } else {
                        playLocalMusic()
                    }
                }
                return
            }

            val audioManager = getApplication<android.app.Application>().getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
            audioManager.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
            viewModelScope.launch {
                kotlinx.coroutines.delay(500)
                refreshMusicState()
            }
        } catch (e: Exception) { 
            e.printStackTrace() 
        }
    }

    fun musicSkipNext() {
        try {
            val component = getNotificationServiceComponent()
            val sessions = try { mediaSessionManager?.getActiveSessions(component) } catch (e: Exception) { null }
            
            if (sessions.isNullOrEmpty()) {
                if (localMusicQueue.isNotEmpty()) {
                    currentMusicIndex = (currentMusicIndex + 1) % localMusicQueue.size
                    playLocalMusic()
                }
                return
            }

            val audioManager = getApplication<android.app.Application>().getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_NEXT))
            audioManager.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_NEXT))
            viewModelScope.launch {
                kotlinx.coroutines.delay(500)
                refreshMusicState()
            }
        } catch (e: Exception) { 
            e.printStackTrace() 
        }
    }

    fun musicSkipPrev() {
        try {
            val component = getNotificationServiceComponent()
            val sessions = try { mediaSessionManager?.getActiveSessions(component) } catch (e: Exception) { null }
            
            if (sessions.isNullOrEmpty()) {
                if (localMusicQueue.isNotEmpty()) {
                    currentMusicIndex = if (currentMusicIndex - 1 < 0) localMusicQueue.size - 1 else currentMusicIndex - 1
                    playLocalMusic()
                }
                return
            }

            val audioManager = getApplication<android.app.Application>().getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS))
            audioManager.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS))
            viewModelScope.launch {
                kotlinx.coroutines.delay(500)
                refreshMusicState()
            }
        } catch (e: Exception) { 
            e.printStackTrace() 
        }
    }
    
    fun musicSeekTo(positionMs: Long) {
        try {
            val component = getNotificationServiceComponent()
            val sessions = try { mediaSessionManager?.getActiveSessions(component) } catch (e: Exception) { null }
            
            if (sessions.isNullOrEmpty()) {
                // Seek local player
                localMediaPlayer?.seekTo(positionMs.toInt())
                refreshMusicState()
                return
            }
            
            // Seek external media session
            val active = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: sessions.firstOrNull()
            active?.transportControls?.seekTo(positionMs)
            viewModelScope.launch {
                kotlinx.coroutines.delay(300)
                refreshMusicState()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Flow Launcher Features State
    
    // Flow Launcher Features State
    
    // Universal Search

    
    private var searchJob: kotlinx.coroutines.Job? = null
    
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        
        // Cancel any in-flight search immediately
        searchJob?.cancel()
        
        // Secret UI Bypass for Hidden Apps
        if (query == "!hidden") {
            _isSearching.value = true
            val hiddenList = _hiddenAppsList.value.map { app ->
                com.vertigo.launcher.utils.SearchManager.SearchResult(
                    type = com.vertigo.launcher.utils.SearchManager.ResultType.APP,
                    id = app.packageName,
                    title = app.label,
                    subtitle = "Hidden Application",
                    action = com.vertigo.launcher.utils.SearchManager.SearchAction.LaunchApp(app.packageName)
                )
            }
            _searchResults.value = hiddenList
            _isSearching.value = false
            return
        }

        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        
        // Debounce: wait 150ms after user stops typing before searching
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(150)
            _isSearching.value = true
            val results = searchManager.search(query)
            _searchResults.value = results
            _isSearching.value = false
        }
    }
    
    fun performSearchAction(action: com.vertigo.launcher.utils.SearchManager.SearchAction) {
        viewModelScope.launch {
            try {
                when (action) {
                    is com.vertigo.launcher.utils.SearchManager.SearchAction.LaunchApp -> {
                        val intent = getApplication<Application>().packageManager.getLaunchIntentForPackage(action.packageName)
                        intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent?.let { getApplication<Application>().startActivity(it) }
                    }
                    is com.vertigo.launcher.utils.SearchManager.SearchAction.WebSearch -> {
                        val url = searchManager.getSearchUrl(action.query, action.provider)
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        getApplication<Application>().startActivity(intent)
                    }
                    is com.vertigo.launcher.utils.SearchManager.SearchAction.WebUrl -> {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(action.url))
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        getApplication<Application>().startActivity(intent)
                    }
                    is com.vertigo.launcher.utils.SearchManager.SearchAction.CallContact -> {
                        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:${action.phoneNumber}"))
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        getApplication<Application>().startActivity(intent)
                    }
                    is com.vertigo.launcher.utils.SearchManager.SearchAction.MessageContact -> {
                         val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("sms:${action.phoneNumber}"))
                         intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                         getApplication<Application>().startActivity(intent)
                    }
                    is com.vertigo.launcher.utils.SearchManager.SearchAction.OpenSetting -> {
                        val intent = android.content.Intent(action.settingAction)
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        getApplication<Application>().startActivity(intent)
                    }
                    is com.vertigo.launcher.utils.SearchManager.SearchAction.QuickDial -> {
                         val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:${action.number}"))
                         intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                         getApplication<Application>().startActivity(intent)
                    }
                    is com.vertigo.launcher.utils.SearchManager.SearchAction.OpenFile -> {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                        intent.setDataAndType(android.net.Uri.parse(action.uri), action.mimeType)
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        getApplication<Application>().startActivity(intent)
                    }
                    is com.vertigo.launcher.utils.SearchManager.SearchAction.CopyToClipboard -> {
                        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText(action.label, action.text)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(getApplication<Application>(), "${action.label} copied", Toast.LENGTH_SHORT).show()
                    }
                    is com.vertigo.launcher.utils.SearchManager.SearchAction.ToggleFlashlight -> {
                        val cameraManager = getApplication<Application>().getSystemService(Context.CAMERA_SERVICE) as CameraManager
                        try {
                            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                                cameraManager.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                            }
                            if (cameraId != null) {
                                // Toggle logic: We need state, but for a quick action, let's just turn it on or toggle if we could track state.
                                // Simplest robust way without tracking complex state is just to try turning it on, if it throws, try off. 
                                // Actually, API 23+ has torchCallback, but for a simple toggle, we can just flip a boolean we store locally.
                                val currentState = _flashlightState
                                cameraManager.setTorchMode(cameraId, !currentState)
                                _flashlightState = !currentState
                                val status = if (_flashlightState) "On" else "Off"
                                Toast.makeText(getApplication<Application>(), "Flashlight $status", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(getApplication<Application>(), "No flashlight found", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(getApplication<Application>(), "Failed to toggle flashlight", Toast.LENGTH_SHORT).show()
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    

    

    
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

    

    
    // Public method to reload settings (call from Activity onResume)
    fun reloadSettings() {
        _showLabels.value = prefs.getBoolean("show_labels", true)
        _showBadges.value = prefs.getBoolean("show_badges", true)
        
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

        // Refresh Notifications (delay to let system process dismissed notifications)
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            updateNotificationCounts()
        }

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
        val wakeWords = listOf("sunday", "sandy", "sundae", "sandra", "santa", "santo", "ollama", "llama", "brain", "hola", "mama")
        
        android.util.Log.d("VoiceAssistant", "Processing: '$command' (Persistent: ${_isHotwordActive.value})")
        
        val foundWakeWord = wakeWords.find { command.contains(it) }
        
        if (foundWakeWord != null) {
            val index = command.indexOf(foundWakeWord)
            val actualCommand = command.substring(index + foundWakeWord.length).trim()
            
            if (actualCommand.isNotEmpty()) {
                android.util.Log.d("VoiceAssistant", "Command after wake word ($foundWakeWord): '$actualCommand'")
                voiceManager.clearSpokenText()
                _isHotwordActive.value = false
                hotwordResetJob?.cancel()
                executeVoiceCommand(actualCommand)
            } else {
                android.util.Log.d("VoiceAssistant", "Wake word ($foundWakeWord) only. Window OPEN.")
                _isHotwordActive.value = true
                hotwordResetJob?.cancel()
                hotwordResetJob = viewModelScope.launch {
                    kotlinx.coroutines.delay(15000) // 15 second window
                    _isHotwordActive.value = false
                    android.util.Log.d("VoiceAssistant", "Window CLOSED.")
                }
            }
        } else if (_isHotwordActive.value && command.isNotEmpty()) {
            android.util.Log.d("VoiceAssistant", "Processing via persistence: '$command'")
            _isHotwordActive.value = false
            hotwordResetJob?.cancel()
            voiceManager.clearSpokenText()
            executeVoiceCommand(command)
        }
    }
    
    // ─── AI Brain (Local Ollama via Termux) ────────────────────────────────

    
    fun clearChatHistory() {
        _chatHistory.value = emptyList()
        _currentStreamingResponse.value = null
    }
    
    fun clearAiResponse() {
        _currentStreamingResponse.value = null
    }
    
    fun sendTextToAiBrain(text: String) {
        if (text.isBlank()) return
        processAiQuery(text)
    }

    /**
     * Process an image through ML Kit Vision (labels + OCR) and send to Ollama for description.
     * Supports printed and handwritten text recognition.
     */
    fun processImageQuery(bitmap: android.graphics.Bitmap, imageUriString: String? = null) {
        // Add user photo message to chat history
        val userMsg = ChatMessage(
            role = "user",
            content = "📷 Sent a photo for analysis",
            imageUri = "photo"
        )
        // Store a scaled-down thumbnail for display
        val thumbSize = 120
        val aspect = bitmap.width.toFloat() / bitmap.height.toFloat()
        val thumbW = if (aspect >= 1f) thumbSize else (thumbSize * aspect).toInt()
        val thumbH = if (aspect >= 1f) (thumbSize / aspect).toInt() else thumbSize
        val thumbnail = android.graphics.Bitmap.createScaledBitmap(bitmap, thumbW, thumbH, true)
        _photoBitmaps[userMsg.timestamp] = thumbnail
        
        _chatHistory.value = _chatHistory.value + userMsg

        // Cancel any pending query
        aiQueryJob?.cancel()

        aiQueryJob = viewModelScope.launch {
            _isAiThinking.value = true
            _currentStreamingResponse.value = null

            try {
                // Step 1: Run ML Kit analysis (Image Labeling + Text Recognition)
                android.util.Log.d("HomeViewModel", "Starting ML Kit vision analysis...")
                val visionResult = visionAnalyzer.analyze(bitmap)

                // Step 2: Build prompt from vision results
                val (systemPrompt, userPrompt) = visionAnalyzer.buildPromptFromResult(visionResult)
                android.util.Log.d("HomeViewModel", "Vision prompt built: ${userPrompt.take(200)}...")

                // Step 3: Stream to Ollama (reuse existing pipeline)
                val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(getApplication())
                val selectedModel = prefs.getString("ollama_model_select", "llama3.2:1b") ?: "llama3.2:1b"
                val baseUrl = prefs.getString("ollama_base_url", "http://127.0.0.1:11434") ?: "http://127.0.0.1:11434"

                var fullResponse = ""
                var hasReceivedFirstChunk = false

                ollamaClient.generateResponseStream(userPrompt, model = selectedModel, baseUrl = baseUrl, systemPrompt = systemPrompt).collect { chunk ->
                    if (!hasReceivedFirstChunk) {
                        hasReceivedFirstChunk = true
                        _isAiThinking.value = false
                    }
                    fullResponse += chunk
                    _currentStreamingResponse.value = fullResponse
                }

                _currentStreamingResponse.value = fullResponse

                if (fullResponse.isNotBlank()) {
                    val assistantMsg = ChatMessage(role = "assistant", content = fullResponse)
                    _chatHistory.value = _chatHistory.value + assistantMsg
                } else if (!hasReceivedFirstChunk) {
                    throw Exception("No response received from model.")
                }

            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    val errorMsg = "Vision Error: ${e.message ?: "Unknown"}\n\nMake sure Ollama is running in Termux."
                    val assistantMsg = ChatMessage(role = "assistant", content = errorMsg)
                    _chatHistory.value = _chatHistory.value + assistantMsg
                    android.util.Log.e("HomeViewModel", "Vision analysis error", e)
                }
            } finally {
                _currentStreamingResponse.value = null
                _isAiThinking.value = false
                aiQueryJob = null
            }
        }
    }

    private fun processAiQuery(query: String) {
        // Add User message to history
        val userMsg = ChatMessage(role = "user", content = query)
        _chatHistory.value = _chatHistory.value + userMsg

        // Cancel any pending query before starting a new one
        aiQueryJob?.cancel()
        
        aiQueryJob = viewModelScope.launch {
            _isAiThinking.value = true
            _currentStreamingResponse.value = null // Keep null so UI shows "Thinking..."
            
            val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(getApplication())
            val selectedModel = prefs.getString("ollama_model_select", "llama3.2:1b") ?: "llama3.2:1b"
            val baseUrl = prefs.getString("ollama_base_url", "http://127.0.0.1:11434") ?: "http://127.0.0.1:11434"
            
            var fullResponse = ""
            var hasReceivedFirstChunk = false
            
            try {
                android.util.Log.d("HomeViewModel", "Querying AI: '$query' Model: $selectedModel at $baseUrl")
                
                ollamaClient.generateResponseStream(query, model = selectedModel, baseUrl = baseUrl).collect { chunk ->
                    if (!hasReceivedFirstChunk) {
                        hasReceivedFirstChunk = true
                        _isAiThinking.value = false
                    }
                    fullResponse += chunk
                    
                    // Update UI immediately for every chunk to match raw terminal output exactly
                _currentStreamingResponse.value = fullResponse
            }
            
            _currentStreamingResponse.value = fullResponse // Ensure final result is set
                
                // Once finished, move to permanent history and clear stream
                if (fullResponse.isNotBlank()) {
                    val assistantMsg = ChatMessage(role = "assistant", content = fullResponse)
                    _chatHistory.value = _chatHistory.value + assistantMsg
                } else if (hasReceivedFirstChunk) {
                     // Empty but finished? 
                } else {
                     throw Exception("No response received from model.")
                }
                
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    val errorMsg = "Error [${e.javaClass.simpleName}]: ${e.message ?: "Unknown error"}\n\nTarget: $baseUrl"
                    val assistantMsg = ChatMessage(role = "assistant", content = errorMsg)
                    _chatHistory.value = _chatHistory.value + assistantMsg
                    android.util.Log.e("HomeViewModel", "Ollama Error", e)
                }
            } finally {
                _currentStreamingResponse.value = null
                _isAiThinking.value = false
                aiQueryJob = null
            }
        }
    }

    fun stopAiResponse() {
        android.util.Log.d("HomeViewModel", "Stopping AI response manually. Job active: ${aiQueryJob?.isActive}")
        aiQueryJob?.cancel()
        // Give immediate UI feedback
        _isAiThinking.value = false
        _currentStreamingResponse.value = null
    }

    private fun executeVoiceCommand(command: String) {
        android.util.Log.d("VoiceAssistant", "Executing command: '$command'")
        // Clear previous AI response on new command
        clearAiResponse()
        
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
                    com.vertigo.launcher.utils.SearchManager.SearchResult(
                        type = com.vertigo.launcher.utils.SearchManager.ResultType.WEB_SEARCH,
                        id = "voice_web",
                        title = query,
                        action = com.vertigo.launcher.utils.SearchManager.SearchAction.WebSearch(query, "google")
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
                val appResult = results.firstOrNull { it.type == com.vertigo.launcher.utils.SearchManager.ResultType.APP }
                
                appResult?.let { 
                    performSearchAction(it.action)
                }
            }
        }
        // Unrecognized command - Query Local Termux Brain
        else {
            processAiQuery(command)
        }
    }

    // ─── Calendar Events ───────────────────────────────────────────────────
    
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
                
                val selection = "(${CalendarContract.Events.DTSTART} < ?) AND (${CalendarContract.Events.DTEND} > ?)"
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
                    
                    val currentHoliday = _currentHoliday.value
                    
                    while (it.moveToNext()) {
                        val title = it.getString(titleIdx) ?: "No Title"
                        val startTime = it.getLong(startIdx)
                        
                        // Skip if it's already identified as a holiday to avoid double display
                        if (currentHoliday != null && title.uppercase(Locale.getDefault()).contains(currentHoliday)) {
                            continue
                        }
                        
                        events.add(
                            CalendarEvent(
                                title = title,
                                startTimeMs = startTime,
                                endTimeMs = it.getLong(endIdx),
                                location = if (locIdx >= 0) it.getString(locIdx) else null,
                                color = if (colorIdx >= 0 && !it.isNull(colorIdx)) it.getInt(colorIdx) else null
                            )
                        )
                    }
                }
                
                // Final deduplication by title and start time
                val uniqueEvents = events.distinctBy { "${it.title}_${it.startTimeMs}" }
                _todayEvents.value = uniqueEvents
            } catch (e: Exception) {
                e.printStackTrace()
                _todayEvents.value = emptyList()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            voiceManager.destroy()
            launcherApps.unregisterCallback(launcherCallback)
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(getApplication())
                .unregisterReceiver(notificationReceiver)
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "Error during onCleared cleanup", e)
        }
        
        hubUpdateJob?.cancel()
        musicPollJob?.cancel()
    }


    
    private fun startOllamaBackend() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val intent = android.content.Intent()
                intent.setClassName("com.termux", "com.termux.app.RunCommandService")
                intent.action = "com.termux.RUN_COMMAND"
                
                // Start the Ollama server in Termux
                intent.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/ollama")
                intent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("serve"))
                intent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
                
                getApplication<Application>().startService(intent)
                android.util.Log.d("HomeViewModel", "Sent RUN_COMMAND intent to Termux for Ollama")
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Failed to start Termux, is it installed?", e)
            }
        }
    }

    // ─── Shizuku Power Actions ───────────────────────────────────────────

    fun clearShizukuResult() { _shizukuActionResult.value = null }

    fun refreshFrozenApps() {
        viewModelScope.launch {
            val frozenPkgs = com.vertigo.launcher.logic.AppCommander.getFrozenApps()
            _frozenApps.value = frozenPkgs.toSet()
            
            // Also fetch full AppModel objects from repository
            _frozenAppsList.value = repository.getFrozenApps()
        }
    }

    fun freezeApp(packageName: String) {
        viewModelScope.launch {
            val result = com.vertigo.launcher.logic.AppCommander.freezeApp(packageName)
            _shizukuActionResult.value = if (result.isSuccess) "❄️ Frozen: $packageName" else "❌ Failed: ${result.stderr}"
            refreshFrozenApps()
            loadApps(true) // Refresh app lists
        }
    }

    fun unfreezeApp(packageName: String) {
        viewModelScope.launch {
            val result = com.vertigo.launcher.logic.AppCommander.unfreezeApp(packageName)
            _shizukuActionResult.value = if (result.isSuccess) "🔥 Unfrozen: $packageName" else "❌ Failed: ${result.stderr}"
            refreshFrozenApps()
            loadApps(true)
        }
    }

    fun forceStopApp(packageName: String) {
        viewModelScope.launch {
            val result = com.vertigo.launcher.logic.AppCommander.forceStop(packageName)
            _shizukuActionResult.value = if (result.isSuccess) "💀 Force stopped" else "❌ Failed: ${result.stderr}"
        }
    }

    fun clearAppData(packageName: String) {
        viewModelScope.launch {
            val result = com.vertigo.launcher.logic.AppCommander.clearData(packageName)
            _shizukuActionResult.value = if (result.isSuccess) "🗑️ Data cleared" else "❌ Failed: ${result.stderr}"
        }
    }

    fun silentUninstallApp(packageName: String) {
        viewModelScope.launch {
            val result = com.vertigo.launcher.logic.AppCommander.silentUninstall(packageName)
            _shizukuActionResult.value = if (result.isSuccess) "🗑️ Uninstalled" else "❌ Failed: ${result.stderr}"
            loadApps(true)
        }
    }
}

