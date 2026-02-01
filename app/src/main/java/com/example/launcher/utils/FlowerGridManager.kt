package com.example.launcher.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * FlowerGridManager - Manages apps pinned to the main flower grid
 */
class FlowerGridManager(context: Context) {
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("flower_grid_prefs", Context.MODE_PRIVATE)
    
    companion object {
        const val PREF_GRID_APPS = "grid_apps"
    }
    
    fun getGridApps(): List<String> {
        val appsString = prefs.getString(PREF_GRID_APPS, "") ?: ""
        if (appsString.isEmpty()) return emptyList()
        return appsString.split(",").filter { it.isNotEmpty() }
    }
    
    fun setGridApps(apps: List<String>) {
        prefs.edit().putString(PREF_GRID_APPS, apps.joinToString(",")).apply()
    }
    
    fun addToGrid(packageName: String): Boolean {
        val current = getGridApps().toMutableList()
        if (current.contains(packageName)) return false
        
        // Max capacity check (optional, but grid has physical limits)
        if (current.size >= 61) return false // 1 + 6 + 12 + 18 + 24
        
        current.add(packageName)
        setGridApps(current)
        return true
    }
    
    fun removeFromGrid(packageName: String) {
        val current = getGridApps().toMutableList()
        current.remove(packageName)
        setGridApps(current)
    }
    
    fun isInGrid(packageName: String): Boolean {
        return getGridApps().contains(packageName)
    }
}
