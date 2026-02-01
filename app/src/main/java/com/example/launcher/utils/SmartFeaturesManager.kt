package com.example.launcher.utils

import android.accessibilityservice.AccessibilityService
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * SmartFeaturesManager - Screen-off, immersive mode, and power features
 */
class SmartFeaturesManager(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("smart_features_prefs", Context.MODE_PRIVATE)
    
    companion object {
        const val PREF_DOUBLE_TAP_LOCK = "double_tap_lock"
        const val PREF_IMMERSIVE_MODE = "immersive_mode"
        const val PREF_AUTO_IMMERSIVE = "auto_immersive"
        const val PREF_KEEP_SCREEN_ON = "keep_screen_on"
        const val PREF_AMBIENT_DISPLAY = "ambient_display"
    }
    
    // ===== Screen Lock/Off =====
    
    /**
     * Lock the screen using device admin
     */
    fun lockScreen(): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        
        return try {
            dpm.lockNow()
            true
        } catch (e: SecurityException) {
            // Device admin not enabled
            false
        }
    }
    
    /**
     * Check if device admin is enabled for screen lock
     */
    fun isDeviceAdminEnabled(): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(context, LauncherDeviceAdmin::class.java)
        return dpm.isAdminActive(componentName)
    }
    
    /**
     * Request device admin permission
     */
    fun requestDeviceAdmin(): Intent {
        val componentName = ComponentName(context, LauncherDeviceAdmin::class.java)
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, 
                "Enable device admin to allow double-tap to lock screen")
        }
    }
    
    fun setDoubleTapLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_DOUBLE_TAP_LOCK, enabled).apply()
    }
    
    fun isDoubleTapLockEnabled(): Boolean {
        return prefs.getBoolean(PREF_DOUBLE_TAP_LOCK, false)
    }
    
    // ===== Immersive Mode =====
    
    /**
     * Enable immersive mode (hide status & nav bars)
     */
    fun enterImmersiveMode(window: android.view.Window, decorView: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = WindowCompat.getInsetsController(window, decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = 
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            @Suppress("DEPRECATION")
            decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
    }
    
    /**
     * Exit immersive mode
     */
    fun exitImmersiveMode(window: android.view.Window, decorView: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = WindowCompat.getInsetsController(window, decorView)
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }
    
    fun setImmersiveModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_IMMERSIVE_MODE, enabled).apply()
    }
    
    fun isImmersiveModeEnabled(): Boolean {
        return prefs.getBoolean(PREF_IMMERSIVE_MODE, false)
    }
    
    fun setAutoImmersiveEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_AUTO_IMMERSIVE, enabled).apply()
    }
    
    fun isAutoImmersiveEnabled(): Boolean {
        return prefs.getBoolean(PREF_AUTO_IMMERSIVE, false)
    }
    
    // ===== Keep Screen On =====
    
    fun setKeepScreenOn(window: android.view.Window, enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        prefs.edit().putBoolean(PREF_KEEP_SCREEN_ON, enabled).apply()
    }
    
    fun isKeepScreenOnEnabled(): Boolean {
        return prefs.getBoolean(PREF_KEEP_SCREEN_ON, false)
    }
    
    // ===== Screen Timeout Control =====
    
    /**
     * Get current screen timeout in milliseconds
     */
    fun getScreenTimeout(): Int {
        return try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
        } catch (e: Exception) {
            30000 // Default 30 seconds
        }
    }
    
    /**
     * Check if the device is charging
     */
    fun isCharging(): Boolean {
        val intent = context.registerReceiver(null, 
            android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                status == android.os.BatteryManager.BATTERY_STATUS_FULL
    }
    
    /**
     * Get battery percentage
     */
    fun getBatteryPercentage(): Int {
        val intent = context.registerReceiver(null,
            android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100 / scale) else -1
    }
    
    // ===== Wake Lock =====
    
    private var wakeLock: PowerManager.WakeLock? = null
    
    fun acquireWakeLock() {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "VLauncher:SmartFeatures"
        )
        wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes max
    }
    
    fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }
}

/**
 * Device Admin receiver for screen lock
 */
class LauncherDeviceAdmin : android.app.admin.DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
    }
    
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
    }
}
