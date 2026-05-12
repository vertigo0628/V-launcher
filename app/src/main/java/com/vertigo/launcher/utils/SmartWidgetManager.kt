package com.vertigo.launcher.utils

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

/**
 * SmartWidgetManager - Manages time/day/location based widget changes
 * Widgets can automatically change based on conditions
 */
class SmartWidgetManager(context: Context) {
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("smart_widget_prefs", Context.MODE_PRIVATE)
    
    /**
     * Rule for when to show a specific widget configuration
     */
    data class WidgetRule(
        val id: String,
        val widgetId: Int,
        val condition: Condition,
        val value: String,
        val priority: Int = 0
    )
    
    sealed class Condition {
        data class TimeRange(val startHour: Int, val endHour: Int) : Condition()
        data class DayOfWeek(val days: Set<Int>) : Condition() // Calendar.MONDAY, etc.
        data class Location(val lat: Double, val lng: Double, val radiusMeters: Float) : Condition()
        object Always : Condition()
    }
    
    private val rules = mutableListOf<WidgetRule>()
    
    /**
     * Add a rule for a widget
     */
    fun addRule(rule: WidgetRule) {
        rules.add(rule)
        rules.sortByDescending { it.priority }
        saveRules()
    }
    
    /**
     * Remove rules for a widget
     */
    fun removeRulesForWidget(widgetId: Int) {
        rules.removeAll { it.widgetId == widgetId }
        saveRules()
    }
    
    /**
     * Check which widget should be shown based on current conditions
     */
    fun getActiveWidgetId(widgetIds: List<Int>): Int? {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
        
        for (rule in rules) {
            if (widgetIds.contains(rule.widgetId) && matchesCondition(rule.condition, currentHour, currentDay)) {
                return rule.widgetId
            }
        }
        
        // Return first available if no rule matches
        return widgetIds.firstOrNull()
    }
    
    private fun matchesCondition(condition: Condition, currentHour: Int, currentDay: Int): Boolean {
        return when (condition) {
            is Condition.Always -> true
            is Condition.TimeRange -> {
                if (condition.startHour <= condition.endHour) {
                    currentHour in condition.startHour..condition.endHour
                } else {
                    // Handles overnight ranges like 22:00 - 06:00
                    currentHour >= condition.startHour || currentHour <= condition.endHour
                }
            }
            is Condition.DayOfWeek -> currentDay in condition.days
            is Condition.Location -> {
                // TODO: Implement location-based switching
                // Would require location permissions and fused location provider
                false
            }
        }
    }
    
    /**
     * Check if it's "morning" (6am-12pm)
     */
    fun isMorning(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour in 6..11
    }
    
    /**
     * Check if it's "afternoon" (12pm-6pm)
     */
    fun isAfternoon(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour in 12..17
    }
    
    /**
     * Check if it's "evening" (6pm-10pm)
     */
    fun isEvening(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour in 18..21
    }
    
    /**
     * Check if it's "night" (10pm-6am)
     */
    fun isNight(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour >= 22 || hour < 6
    }
    
    /**
     * Check if it's a weekend
     */
    fun isWeekend(): Boolean {
        val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return day == Calendar.SATURDAY || day == Calendar.SUNDAY
    }
    
    /**
     * Get suggested theme mode based on time
     */
    fun getSuggestedThemeMode(): ThemeMode {
        return when {
            isNight() -> ThemeMode.DARK
            isMorning() -> ThemeMode.LIGHT
            else -> ThemeMode.SYSTEM
        }
    }
    
    enum class ThemeMode {
        LIGHT, DARK, SYSTEM
    }
    
    private fun saveRules() {
        // Simple JSON serialization
        val rulesJson = rules.joinToString(";") { rule ->
            "${rule.id},${rule.widgetId},${rule.priority}"
        }
        prefs.edit().putString("rules", rulesJson).apply()
    }
    
    private fun loadRules() {
        val rulesJson = prefs.getString("rules", "") ?: ""
        // Parse rules from JSON
        // Simplified for now - full implementation would use Gson/Moshi
    }
}
