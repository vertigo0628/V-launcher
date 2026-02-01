package com.example.launcher.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * GestureManager - Manages gesture actions and settings
 */
class GestureManager(context: Context) {
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("gesture_prefs", Context.MODE_PRIVATE)
    
    companion object {
        // Gesture types
        const val GESTURE_DOUBLE_TAP = "double_tap"
        const val GESTURE_SWIPE_UP = "swipe_up"
        const val GESTURE_SWIPE_DOWN = "swipe_down"
        const val GESTURE_SWIPE_LEFT = "swipe_left"
        const val GESTURE_SWIPE_RIGHT = "swipe_right"
        const val GESTURE_LONG_PRESS = "long_press"
        const val GESTURE_TWO_FINGER_SWIPE_UP = "two_finger_swipe_up"
        const val GESTURE_TWO_FINGER_SWIPE_DOWN = "two_finger_swipe_down"
        const val GESTURE_PINCH = "pinch"
        
        // Actions
        const val ACTION_NONE = "none"
        const val ACTION_OPEN_DRAWER = "open_drawer"
        const val ACTION_OPEN_SEARCH = "open_search"
        const val ACTION_LOCK_SCREEN = "lock_screen"
        const val ACTION_OPEN_NOTIFICATIONS = "open_notifications"
        const val ACTION_OPEN_QUICK_SETTINGS = "open_quick_settings"
        const val ACTION_OPEN_SETTINGS = "open_settings"
        const val ACTION_OPEN_HIDDEN_APPS = "open_hidden_apps"
        const val ACTION_TOGGLE_WIDGETS = "toggle_widgets"
        const val ACTION_OPEN_APP = "open_app:"  // Prefix for app package
    }
    
    // Default gesture mappings
    private val defaultMappings = mapOf(
        GESTURE_DOUBLE_TAP to ACTION_OPEN_SEARCH,
        GESTURE_SWIPE_UP to ACTION_OPEN_DRAWER,
        GESTURE_SWIPE_DOWN to ACTION_OPEN_NOTIFICATIONS,
        GESTURE_SWIPE_LEFT to ACTION_NONE,
        GESTURE_SWIPE_RIGHT to ACTION_NONE,
        GESTURE_LONG_PRESS to ACTION_OPEN_SETTINGS,
        GESTURE_TWO_FINGER_SWIPE_UP to ACTION_TOGGLE_WIDGETS,
        GESTURE_TWO_FINGER_SWIPE_DOWN to ACTION_OPEN_QUICK_SETTINGS,
        GESTURE_PINCH to ACTION_OPEN_HIDDEN_APPS
    )
    
    fun getGestureAction(gesture: String): String {
        return prefs.getString(gesture, defaultMappings[gesture] ?: ACTION_NONE) ?: ACTION_NONE
    }
    
    fun setGestureAction(gesture: String, action: String) {
        prefs.edit().putString(gesture, action).apply()
    }
    
    fun getAllGestures(): Map<String, String> {
        return defaultMappings.keys.associateWith { getGestureAction(it) }
    }
    
    fun getAvailableActions(): List<Pair<String, String>> {
        return listOf(
            ACTION_NONE to "Do Nothing",
            ACTION_OPEN_DRAWER to "Open App Drawer",
            ACTION_OPEN_SEARCH to "Open Search",
            ACTION_LOCK_SCREEN to "Lock Screen",
            ACTION_OPEN_NOTIFICATIONS to "Open Notifications",
            ACTION_OPEN_QUICK_SETTINGS to "Quick Settings",
            ACTION_OPEN_SETTINGS to "Open Launcher Settings",
            ACTION_OPEN_HIDDEN_APPS to "Show Hidden Apps",
            ACTION_TOGGLE_WIDGETS to "Toggle Widgets"
        )
    }
    
    fun getGestureDisplayName(gesture: String): String {
        return when (gesture) {
            GESTURE_DOUBLE_TAP -> "Double Tap"
            GESTURE_SWIPE_UP -> "Swipe Up"
            GESTURE_SWIPE_DOWN -> "Swipe Down"
            GESTURE_SWIPE_LEFT -> "Swipe Left"
            GESTURE_SWIPE_RIGHT -> "Swipe Right"
            GESTURE_LONG_PRESS -> "Long Press"
            GESTURE_TWO_FINGER_SWIPE_UP -> "Two-Finger Swipe Up"
            GESTURE_TWO_FINGER_SWIPE_DOWN -> "Two-Finger Swipe Down"
            GESTURE_PINCH -> "Pinch"
            else -> gesture
        }
    }
}
