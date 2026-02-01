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
    
    // PIN Protection
    private val PREF_PIN = "hidden_apps_pin"
    
    fun isPinSet(): Boolean {
        return prefs.getString(PREF_PIN, null) != null
    }
    
    fun setPin(pin: String) {
        // Simple hash for storage (not cryptographically secure, but adequate for app hiding)
        val hashedPin = pin.hashCode().toString()
        prefs.edit().putString(PREF_PIN, hashedPin).apply()
    }
    
    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(PREF_PIN, null) ?: return false
        return pin.hashCode().toString() == storedHash
    }
    
    fun clearPin() {
        prefs.edit().remove(PREF_PIN).apply()
    }
}
