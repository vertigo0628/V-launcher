package com.vertigo.launcher.utils

import android.content.Context
import android.content.SharedPreferences
import com.vertigo.launcher.model.AppModel
import com.vertigo.launcher.model.AppCategory
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

    companion object {
        private const val THREE_DAYS_MS = 3L * 24 * 60 * 60 * 1000
        private const val SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000
    }

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
     * Time-decay scoring algorithm:
     * - Base score = launchCount
     * - Recency boost: +10 if used in last 3 days, +5 if used in last 7 days
     * - This ensures frequently AND recently used apps bubble to the top
     */
    private fun computeScore(stats: AppUsageStats): Double {
        val now = System.currentTimeMillis()
        val age = now - stats.lastLaunchTime
        val recencyBoost = when {
            age < THREE_DAYS_MS -> 10.0
            age < SEVEN_DAYS_MS -> 5.0
            else -> 0.0
        }
        return stats.launchCount.toDouble() + recencyBoost
    }

    /**
     * Get top suggested apps by combined frequency + recency score.
     */
    fun getSuggestedApps(limit: Int): List<String> {
        return usageMap.values
            .sortedByDescending { computeScore(it) }
            .take(limit)
            .map { it.packageName }
    }

    /**
     * Get recently accessed apps sorted by last launch time (most recent first).
     */
    fun getRecentApps(limit: Int): List<String> {
        return usageMap.values
            .filter { it.lastLaunchTime > 0 }
            .sortedByDescending { it.lastLaunchTime }
            .take(limit)
            .map { it.packageName }
    }

    /**
     * Category-diverse smart selection for the flower grid auto-fill.
     * Picks top apps while ensuring no single category dominates.
     * @param availableApps the pool of apps to select from
     * @param limit max number of apps to return
     * @param maxPerCategory max apps from any single category
     */
    fun getDiverseSuggestions(
        availableApps: List<AppModel>,
        limit: Int,
        maxPerCategory: Int = 4
    ): List<AppModel> {
        // Score all available apps
        val scored = availableApps.map { app ->
            val score = usageMap[app.packageName]?.let { computeScore(it) } ?: 0.0
            app to score
        }.sortedByDescending { it.second }

        val result = mutableListOf<AppModel>()
        val categoryCount = mutableMapOf<AppCategory, Int>()

        for ((app, _) in scored) {
            if (result.size >= limit) break
            val catCount = categoryCount.getOrDefault(app.category, 0)
            if (catCount < maxPerCategory) {
                result.add(app)
                categoryCount[app.category] = catCount + 1
            }
        }

        // If we still have room after category-capping, fill with remaining
        if (result.size < limit) {
            val remaining = scored.filter { (app, _) -> app !in result }
            for ((app, _) in remaining) {
                if (result.size >= limit) break
                result.add(app)
            }
        }

        return result
    }

    fun hasUsageData(): Boolean = usageMap.isNotEmpty()
}
