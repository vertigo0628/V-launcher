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
    
    // Original full list
    private var allApps: List<AppModel> = emptyList()
    
    private val _apps = MutableStateFlow<List<AppModel>>(emptyList())
    val apps: StateFlow<List<AppModel>> = _apps.asStateFlow()
    
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
            allApps = repository.getInstalledApps()
            updateAppList()
            updateFlowerGrid()
        }
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
    
    private val _filteredApps = MutableStateFlow<List<AppModel>>(emptyList())
    val filteredApps: StateFlow<List<AppModel>> = _filteredApps.asStateFlow()
    
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _filteredApps.value = emptyList()
        } else {
            val all = _apps.value
            _filteredApps.value = all.filter { app ->
                app.label.contains(query, ignoreCase = true)
            }
        }
    }
    
    // Weather
    data class WeatherState(
        val temp: String = "--°",
        val condition: String = "Loading...",
        val iconRes: Int = android.R.drawable.ic_menu_today // Placeholder
    )
    
    private val _weatherState = MutableStateFlow(WeatherState())
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()
    
    private val weatherRepository = WeatherRepository()
    
    private fun fetchWeather() {
        viewModelScope.launch {
            // Check permissions first? For prototype assume granted or use default location (London)
            // In real app, we would request location updates
            val lat = 51.5074
            val long = -0.1278
            
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
    
    fun setVoiceListening(listening: Boolean) {
        if (listening) {
            voiceManager.startListening()
        } else {
            voiceManager.stopListening()
        }
    }
    
    init {
        // Voice Callback
        voiceManager.onSpeechResult = { text ->
             // On voice result, we can either set search query or parse command
             // For now, let's pipe it into search
             onSearchQueryChanged(text)
        }
        
        // Fetch real weather
        fetchWeather()
        
        // Start System Monitor (Always On)
        startHubUpdates()
        
        // Start Voice Assistant (Always On - Hands Free)
        // Note: In real app, check permission!
        setVoiceListening(true)
    }
    
    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}
