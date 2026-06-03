package com.vertigo.launcher.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import kotlinx.coroutines.*
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap

/**
 * PerformanceOptimizer - Optimizations for smooth launcher performance
 * - Icon caching with LRU cache
 * - Background loading with coroutines
 * - Memory management
 */
object PerformanceOptimizer {
    
    // LRU Cache for app icons (max 50MB or 100 icons)
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8 // Use 1/8 of available memory
    
    private val iconCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }
    
    // Soft reference cache for less critical items
    private val softIconCache = ConcurrentHashMap<String, SoftReference<Drawable>>()
    
    // Coroutine scope for background operations
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Get cached icon or load it in background
     */
    fun getCachedIcon(packageName: String, loader: () -> Drawable?): Drawable? {
        // Check soft cache first
        softIconCache[packageName]?.get()?.let { return it }
        
        // Load and cache
        val drawable = loader()
        if (drawable != null) {
            softIconCache[packageName] = SoftReference(drawable)
        }
        return drawable
    }
    
    /**
     * Get cached icon if already present in memory cache
     */
    fun getIconIfCached(packageName: String): Drawable? {
        return softIconCache[packageName]?.get()
    }

    
    /**
     * Preload icons in background
     */
    fun preloadIcons(packageNames: List<String>, loader: (String) -> Drawable?) {
        scope.launch {
            packageNames.forEach { packageName ->
                if (softIconCache[packageName]?.get() == null) {
                    val drawable = loader(packageName)
                    if (drawable != null) {
                        softIconCache[packageName] = SoftReference(drawable)
                    }
                }
            }
        }
    }
    
    /**
     * Cache a bitmap icon
     */
    fun cacheBitmapIcon(key: String, bitmap: Bitmap) {
        iconCache.put(key, bitmap)
    }
    
    /**
     * Get cached bitmap icon
     */
    fun getCachedBitmapIcon(key: String): Bitmap? {
        return iconCache.get(key)
    }
    
    /**
     * Clear all caches (call on low memory)
     */
    fun clearCaches() {
        iconCache.evictAll()
        softIconCache.clear()
    }
    
    /**
     * Trim caches based on memory pressure
     */
    fun trimMemory(level: Int) {
        when {
            level >= android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                clearCaches()
            }
            level >= android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE -> {
                iconCache.trimToSize(cacheSize / 2)
            }
            level >= android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> {
                iconCache.trimToSize(cacheSize * 3 / 4)
            }
        }
    }
    
    /**
     * Debounce function for search
     */
    class Debouncer(private val delayMs: Long = 300L) {
        private var job: Job? = null
        
        fun debounce(action: suspend () -> Unit) {
            job?.cancel()
            job = scope.launch {
                delay(delayMs)
                withContext(Dispatchers.Main) {
                    action()
                }
            }
        }
    }
    
    /**
     * App launch time tracker
     */
    object LaunchTracker {
        private val launchTimes = ConcurrentHashMap<String, MutableList<Long>>()
        
        fun recordLaunchTime(packageName: String, timeMs: Long) {
            val times = launchTimes.getOrPut(packageName) { mutableListOf() }
            times.add(timeMs)
            // Keep only last 10 launches
            if (times.size > 10) times.removeAt(0)
        }
        
        fun getAverageLaunchTime(packageName: String): Long {
            val times = launchTimes[packageName] ?: return 0
            return if (times.isNotEmpty()) times.average().toLong() else 0
        }
        
        fun getFrequentlyUsedApps(limit: Int = 10): List<String> {
            return launchTimes.entries
                .sortedByDescending { it.value.size }
                .take(limit)
                .map { it.key }
        }
    }
    
    /**
     * Smooth scrolling helper
     */
    fun calculateScrollVelocity(
        currentVelocity: Float,
        targetVelocity: Float,
        friction: Float = 0.8f
    ): Float {
        return currentVelocity * friction + targetVelocity * (1 - friction)
    }
}
