package com.vertigo.launcher.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * DockManager - Manages dock apps and settings
 */
class DockManager(context: Context) {
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("dock_prefs", Context.MODE_PRIVATE)
    
    companion object {
        const val PREF_DOCK_APPS = "dock_apps"
        const val PREF_DOCK_ENABLED = "dock_enabled"
        const val PREF_DOCK_LABELS = "dock_labels"
        const val PREF_DOCK_SIZE = "dock_size"
        const val DEFAULT_DOCK_SIZE = 5
    }
    
    fun isDockEnabled(): Boolean {
        return prefs.getBoolean(PREF_DOCK_ENABLED, true)
    }
    
    fun setDockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_DOCK_ENABLED, enabled).apply()
    }
    
    fun showDockLabels(): Boolean {
        return prefs.getBoolean(PREF_DOCK_LABELS, false)
    }
    
    fun setShowDockLabels(show: Boolean) {
        prefs.edit().putBoolean(PREF_DOCK_LABELS, show).apply()
    }
    
    fun getDockSize(): Int {
        return prefs.getInt(PREF_DOCK_SIZE, DEFAULT_DOCK_SIZE)
    }
    
    fun setDockSize(size: Int) {
        prefs.edit().putInt(PREF_DOCK_SIZE, size.coerceIn(3, 7)).apply()
    }
    
    fun getDockApps(): List<String> {
        val appsString = prefs.getString(PREF_DOCK_APPS, "") ?: ""
        if (appsString.isEmpty()) return emptyList()
        return appsString.split(",").filter { it.isNotEmpty() }
    }
    
    fun setDockApps(apps: List<String>) {
        prefs.edit().putString(PREF_DOCK_APPS, apps.joinToString(",")).apply()
    }
    
    fun addToDock(packageName: String): Boolean {
        val current = getDockApps().toMutableList()
        if (current.size >= getDockSize()) return false
        if (current.contains(packageName)) return false
        current.add(packageName)
        setDockApps(current)
        return true
    }
    
    fun removeFromDock(packageName: String) {
        val current = getDockApps().toMutableList()
        current.remove(packageName)
        setDockApps(current)
    }
    
    fun isInDock(packageName: String): Boolean {
        return getDockApps().contains(packageName)
    }
    
    fun reorderDock(fromIndex: Int, toIndex: Int) {
        val apps = getDockApps().toMutableList()
        if (fromIndex in apps.indices && toIndex in apps.indices) {
            val app = apps.removeAt(fromIndex)
            apps.add(toIndex, app)
            setDockApps(apps)
        }
    }
}
