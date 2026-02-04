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

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val flowerGridManager = com.example.launcher.utils.FlowerGridManager(application)
    private val preferencesManager = com.example.launcher.utils.PreferencesManager(application)
    
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
    
    fun searchApps(query: String) {
        if (query.isBlank()) {
            updateAppList()
            return
        }
        
        _apps.value = allApps.filter { 
            it.label.contains(query, ignoreCase = true) 
        }
    }
    
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
        val ramPercent: Int = 0,
        val ramText: String = "",
        val storagePercent: Int = 0,
        val storageText: String = ""
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
    
    // Removed stopHubUpdates since we want it always on
    
    private fun updateHubStats() {
        val battery = systemMonitor.getBatteryInfo()
        val ram = systemMonitor.getRamInfo()
        val storage = systemMonitor.getStorageInfo()
        
        _neuralHubState.value = NeuralHubState(
            batteryLevel = battery.level,
            isCharging = battery.isCharging,
            ramPercent = ram.percentUsed,
            ramText = "${formatSize(ram.used)} / ${formatSize(ram.total)}",
            storagePercent = storage.percentUsed,
            storageText = "${formatSize(storage.total - storage.available)} / ${formatSize(storage.total)}"
        )
    }
    
    private fun formatSize(bytes: Long): String {
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return String.format("%.1f GB", gb)
    }
    
    // Flow Launcher Features State
    
    // Flow Launcher Features State
    
    // Universal Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val searchManager = com.example.launcher.utils.SearchManager(application)
    private val _searchResults = MutableStateFlow<List<com.example.launcher.utils.SearchManager.SearchResult>>(emptyList())
    val searchResults: StateFlow<List<com.example.launcher.utils.SearchManager.SearchResult>> = _searchResults.asStateFlow()
    
    fun onSearchQueryChanged(query: String) {
        android.util.Log.d("HomeViewModel", "Search query changed: '$query'")
        _searchQuery.value = query
        viewModelScope.launch {
            if (query.isBlank()) {
                android.util.Log.d("HomeViewModel", "Query blank, clearing results")
                _searchResults.value = emptyList()
            } else {
                android.util.Log.d("HomeViewModel", "Calling searchManager.search()")
                val results = searchManager.search(query)
                android.util.Log.d("HomeViewModel", "Search returned ${results.size} results")
                _searchResults.value = results
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
        
        // Start System Monitor if enabled
        if (prefs.getBoolean("neural_hub_enabled", true)) {
            startHubUpdates()
        }
        
        // Start Voice Assistant if enabled
        if (prefs.getBoolean("voice_assistant_enabled", true)) {
            setVoiceListening(true)
        }
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

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}
