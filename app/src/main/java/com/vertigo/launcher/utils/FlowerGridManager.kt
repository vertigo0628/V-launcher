package com.vertigo.launcher.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * FlowerGridManager - Manages apps pinned to the main flower grid
 */
class FlowerGridManager(context: Context) {
    
    private val prefs: SharedPreferences = 
        StorageHelper.getSafeSharedPreferences(context, "flower_grid_prefs")
    
    companion object {
        const val PREF_GRID_APPS = "grid_apps"
        const val PREF_MAX_GRID_APPS = "max_grid_apps"
        const val PREF_EXCLUDED_APPS = "excluded_grid_apps"
        // Default: center(1) + ring1(6) + ring2 partial(3) = 10 apps
        // A compact starting layout; users can increase via settings
        const val DEFAULT_MAX_GRID_APPS = 10
    }
    
    fun getGridApps(): List<String> {
        val appsString = prefs.getString(PREF_GRID_APPS, "") ?: ""
        if (appsString.isEmpty()) return emptyList()
        return appsString.split(",").filter { it.isNotEmpty() }
    }
    
    fun setGridApps(apps: List<String>) {
        prefs.edit().putString(PREF_GRID_APPS, apps.joinToString(",")).apply()
    }
    
    fun getMaxGridApps(): Int {
        return prefs.getInt(PREF_MAX_GRID_APPS, DEFAULT_MAX_GRID_APPS)
    }
    
    fun setMaxGridApps(max: Int) {
        prefs.edit().putInt(PREF_MAX_GRID_APPS, max.coerceIn(7, 61)).apply()
    }
    
    /**
     * Apps the user explicitly removed from the grid.
     * These are excluded from smart auto-fill so they don't reappear.
     */
    fun getExcludedApps(): Set<String> {
        return prefs.getStringSet(PREF_EXCLUDED_APPS, emptySet()) ?: emptySet()
    }
    
    private fun addToExcluded(packageName: String) {
        val excluded = getExcludedApps().toMutableSet()
        excluded.add(packageName)
        prefs.edit().putStringSet(PREF_EXCLUDED_APPS, excluded).apply()
    }
    
    private fun removeFromExcluded(packageName: String) {
        val excluded = getExcludedApps().toMutableSet()
        excluded.remove(packageName)
        prefs.edit().putStringSet(PREF_EXCLUDED_APPS, excluded).apply()
    }
    
    fun addToGrid(packageName: String): Boolean {
        val current = getGridApps().toMutableList()
        if (current.contains(packageName)) return false
        
        // Enforce the configurable max capacity
        if (current.size >= getMaxGridApps()) return false
        
        // Clear from exclusion list if user is explicitly adding it back
        removeFromExcluded(packageName)
        
        current.add(packageName)
        setGridApps(current)
        return true
    }
    
    /**
     * Remove from grid AND add to exclusion list.
     * This prevents the smart-fill from re-adding it automatically.
     */
    fun removeFromGrid(packageName: String) {
        val current = getGridApps().toMutableList()
        current.remove(packageName)
        setGridApps(current)
        // Always exclude — whether it was pinned or auto-filled
        addToExcluded(packageName)
    }
    
    fun isInGrid(packageName: String): Boolean {
        return getGridApps().contains(packageName)
    }
    
    fun getPinnedCount(): Int = getGridApps().size
}
