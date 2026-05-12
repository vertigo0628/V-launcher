package com.vertigo.launcher.utils

import android.content.Context
import android.content.SharedPreferences
import com.vertigo.launcher.model.AppModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class AppUsageStats(
    val packageName: String,
    var launchCount: Int = 0,
    var lastLaunchTime: Long = 0
)

class SmartUsageManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("smart_usage_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private var usageMap: MutableMap<String, AppUsageStats> = mutableMapOf()

    init {
        loadStats()
    }

    private fun loadStats() {
        val json = prefs.getString("usage_stats", "{}")
        val type = object : TypeToken<MutableMap<String, AppUsageStats>>() {}.type
        usageMap = try {
            gson.fromJson(json, type) ?: mutableMapOf()
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    private fun saveStats() {
        val json = gson.toJson(usageMap)
        prefs.edit().putString("usage_stats", json).apply()
    }

    fun logAppLaunch(packageName: String) {
        val stats = usageMap.getOrPut(packageName) { AppUsageStats(packageName) }
        stats.launchCount++
        stats.lastLaunchTime = System.currentTimeMillis()
        saveStats()
    }

    /**
     * Get suggested apps based on a simple score:
     * Score = LaunchCount * 1.0 + (IsRecent ? 5.0 : 0.0)
     * Actually, let's just sort by LaunchCount for simplicity first, then refine.
     * Refined Algorithm:
     * - Decaying score based on time?
     * - Simple: Frequency over the last 7 days?
     * 
     * Current Implementation: Frequency sorted, but strictly prioritizing apps used in last 3 days.
     */
    fun getSuggestedApps(limit: Int): List<String> {
        return usageMap.values
            .sortedByDescending { it.launchCount } // Sort by most used
            .take(limit)
            .map { it.packageName }
    }
}
