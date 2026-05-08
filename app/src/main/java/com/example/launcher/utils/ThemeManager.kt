package com.example.launcher.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class ThemeManager(context: Context) {
    
    private val prefs: SharedPreferences = 
        PreferenceManager.getDefaultSharedPreferences(context)
    
    companion object {
        const val PREF_THEME = "theme"
        const val THEME_MIDNIGHT = "midnight"
        const val THEME_DARK = "dark"
        const val THEME_AMOLED = "amoled"
    }
    
    fun getCurrentTheme(): String {
        val theme = prefs.getString(PREF_THEME, THEME_DARK) ?: THEME_DARK
        // Handle legacy "light" value - fall back to dark
        return if (theme == "light") THEME_DARK else theme
    }
    
    fun setTheme(theme: String) {
        prefs.edit().putString(PREF_THEME, theme).apply()
    }
    
    fun getBackgroundColor(): Int {
        return when (getCurrentTheme()) {
            THEME_MIDNIGHT -> 0xFF0A1628.toInt() // Deep navy blue
            THEME_DARK -> 0xFF1E1E1E.toInt()
            THEME_AMOLED -> 0xFF000000.toInt()
            else -> 0xFF1E1E1E.toInt()
        }
    }
    
    fun getTextColor(): Int {
        return when (getCurrentTheme()) {
            THEME_MIDNIGHT -> 0xFFE0E8FF.toInt() // Soft blue-white
            THEME_DARK -> 0xFFEEEEEE.toInt()
            THEME_AMOLED -> 0xFFFFFFFF.toInt()
            else -> 0xFFEEEEEE.toInt()
        }
    }
    
    fun getSecondaryTextColor(): Int {
        return when (getCurrentTheme()) {
            THEME_MIDNIGHT -> 0xFF7B8DAA.toInt() // Muted blue-gray
            THEME_DARK -> 0xFF999999.toInt()
            THEME_AMOLED -> 0xFFAAAAAA.toInt()
            else -> 0xFF999999.toInt()
        }
    }
}
