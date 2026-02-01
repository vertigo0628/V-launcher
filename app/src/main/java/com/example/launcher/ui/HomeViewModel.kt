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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    
    fun stopHubUpdates() {
        hubUpdateJob?.cancel()
        hubUpdateJob = null
    }
    
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
}
