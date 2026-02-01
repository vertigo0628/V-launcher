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

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    
    // Original full list
    private var allApps: List<AppModel> = emptyList()
    
    private val _apps = MutableStateFlow<List<AppModel>>(emptyList())
    val apps: StateFlow<List<AppModel>> = _apps.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow<AppCategory?>(null)
    val selectedCategory: StateFlow<AppCategory?> = _selectedCategory.asStateFlow()

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            allApps = repository.getInstalledApps()
            // Default to showing all apps or a specific category if needed. 
            // For now, let's show all apps initially, or we can filter by selected category.
            updateAppList()
        }
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
}
