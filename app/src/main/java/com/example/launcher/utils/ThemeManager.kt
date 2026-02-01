package com.example.launcher.utils

import android.content.Context
import android.content.SharedPreferences

class ThemeManager(context: Context) {
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
    
    companion object {
        const val PREF_THEME = "theme"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_AMOLED = "amoled"
    }
    
    fun getCurrentTheme(): String {
        return prefs.getString(PREF_THEME, THEME_LIGHT) ?: THEME_LIGHT
    }
    
    fun setTheme(theme: String) {
        prefs.edit().putString(PREF_THEME, theme).apply()
    }
    
    fun getBackgroundColor(): Int {
        return when (getCurrentTheme()) {
            THEME_LIGHT -> 0xFFFFFFFF.toInt()
            THEME_DARK -> 0xFF1E1E1E.toInt()
            THEME_AMOLED -> 0xFF000000.toInt()
            else -> 0xFFFFFFFF.toInt()
        }
    }
    
    fun getTextColor(): Int {
        return when (getCurrentTheme()) {
            THEME_LIGHT -> 0xFF000000.toInt()
            THEME_DARK -> 0xFFEEEEEE.toInt()
            THEME_AMOLED -> 0xFFFFFFFF.toInt()
            else -> 0xFF000000.toInt()
        }
    }
    
    fun getSecondaryTextColor(): Int {
        return when (getCurrentTheme()) {
            THEME_LIGHT -> 0xFF666666.toInt()
            THEME_DARK -> 0xFF999999.toInt()
            THEME_AMOLED -> 0xFFAAAAAA.toInt()
            else -> 0xFF666666.toInt()
        }
    }
}
