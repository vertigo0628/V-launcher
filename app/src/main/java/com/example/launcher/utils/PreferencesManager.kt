package com.example.launcher.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
    
    companion object {
        const val PREF_GRID_SIZE = "grid_size"
        const val PREF_HIDDEN_APPS = "hidden_apps"
        const val DEFAULT_GRID_SIZE = 4
    }
    
    fun getGridSize(): Int {
        return prefs.getInt(PREF_GRID_SIZE, DEFAULT_GRID_SIZE)
    }
    
    fun setGridSize(size: Int) {
        prefs.edit().putInt(PREF_GRID_SIZE, size).apply()
    }
    
    fun getHiddenApps(): Set<String> {
        return prefs.getStringSet(PREF_HIDDEN_APPS, emptySet()) ?: emptySet()
    }
    
    fun hideApp(packageName: String) {
        val hiddenApps = getHiddenApps().toMutableSet()
        hiddenApps.add(packageName)
        prefs.edit().putStringSet(PREF_HIDDEN_APPS, hiddenApps).apply()
    }
    
    fun unhideApp(packageName: String) {
        val hiddenApps = getHiddenApps().toMutableSet()
        hiddenApps.remove(packageName)
        prefs.edit().putStringSet(PREF_HIDDEN_APPS, hiddenApps).apply()
    }
    
    fun isAppHidden(packageName: String): Boolean {
        return getHiddenApps().contains(packageName)
    }
}
