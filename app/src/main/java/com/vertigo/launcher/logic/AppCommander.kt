package com.vertigo.launcher.logic

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.vertigo.launcher.utils.ShizukuShell

/**
 * AppCommander — Shizuku-powered app management operations.
 * 
 * Every method here executes shell commands at ADB privilege level,
 * enabling operations that no regular launcher can perform.
 */
object AppCommander {

    private const val TAG = "AppCommander"

    // ─── App Lifecycle Control ───────────────────────────────────────────

    /** Freeze (disable) an app — it vanishes from the system without uninstalling */
    suspend fun freezeApp(packageName: String): ShizukuShell.CommandResult {
        Log.d(TAG, "Freezing: $packageName")
        return ShizukuShell.exec("pm disable-user --user 0 $packageName")
    }

    /** Unfreeze (re-enable) a previously frozen app */
    suspend fun unfreezeApp(packageName: String): ShizukuShell.CommandResult {
        Log.d(TAG, "Unfreezing: $packageName")
        return ShizukuShell.exec("pm enable $packageName")
    }

    /** Force stop an app immediately — no confirmation dialog */
    suspend fun forceStop(packageName: String): ShizukuShell.CommandResult {
        Log.d(TAG, "Force stopping: $packageName")
        return ShizukuShell.exec("am force-stop $packageName")
    }

    /** Clear all data for an app (equivalent to Settings → Clear Data) */
    suspend fun clearData(packageName: String): ShizukuShell.CommandResult {
        Log.d(TAG, "Clearing data: $packageName")
        return ShizukuShell.exec("pm clear $packageName")
    }

    /** Silently uninstall an app without any confirmation prompt */
    suspend fun silentUninstall(packageName: String): ShizukuShell.CommandResult {
        Log.d(TAG, "Silent uninstalling: $packageName")
        return ShizukuShell.exec("pm uninstall $packageName")
    }

    /** Silently install an APK file without prompts */
    suspend fun silentInstall(apkPath: String): ShizukuShell.CommandResult {
        Log.d(TAG, "Silent installing: $apkPath")
        return ShizukuShell.exec("pm install -r \"$apkPath\"")
    }

    // ─── App State Queries ───────────────────────────────────────────────

    /** Check if an app is currently frozen (disabled) */
    suspend fun isFrozen(packageName: String): Boolean {
        val result = ShizukuShell.exec("pm list packages -d")
        return result.isSuccess && result.stdout.contains("package:$packageName")
    }

    /** Get list of all frozen (disabled) packages */
    suspend fun getFrozenApps(): List<String> {
        val result = ShizukuShell.exec("pm list packages -d")
        if (!result.isSuccess) return emptyList()
        return result.stdout.lines()
            .filter { it.startsWith("package:") }
            .map { it.removePrefix("package:") }
    }

    /** Get storage usage info for an app (code + data + cache sizes) */
    suspend fun getAppStorageInfo(packageName: String): AppStorageInfo {
        val result = ShizukuShell.exec("dumpsys package $packageName | grep -E 'codePath|dataDir|cacheDir'")
        return AppStorageInfo(
            codePath = extractField(result.stdout, "codePath="),
            dataDir = extractField(result.stdout, "dataDir="),
            cacheDir = extractField(result.stdout, "cacheDir=")
        )
    }

    // ─── Process Management ──────────────────────────────────────────────

    /** Kill all background processes for an app */
    suspend fun killBackground(packageName: String): ShizukuShell.CommandResult {
        return ShizukuShell.exec("am kill $packageName")
    }

    /** Force stop all user-installed apps and user-facing system apps (Kill Switch) */
    suspend fun killAllApps(context: Context): ShizukuShell.CommandResult {
        Log.d(TAG, "Kill Switch: Gathering packages to stop")
        val pm = context.packageManager
        
        // Exclude critical packages to avoid crashing the system UI, Shizuku, or the launcher itself
        val excludedPackages = setOf(
            context.packageName,
            "rikka.app.shizuku",
            "rikka.app.shizuku.manager",
            "com.android.systemui",
            "android",
            "com.google.android.inputmethod.latin",
            "com.android.inputmethod.latin",
            "com.google.android.providers.media",
            "com.android.providers.media",
            "com.android.providers.media.module"
        )
        
        val packagesToKill = mutableSetOf<String>()
        
        try {
            // 1. Get all installed packages
            val installedPackages = pm.getInstalledPackages(0)
            for (pkgInfo in installedPackages) {
                val appInfo = pkgInfo.applicationInfo ?: continue
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isSystemUpdate = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                
                // If it's a user app (not system, or updated system app), we can kill it
                if (!isSystemApp || isSystemUpdate) {
                    packagesToKill.add(pkgInfo.packageName)
                }
            }
            
            // 2. Get all launchable packages (including user-facing system apps like YouTube, Chrome, Maps, etc.)
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val launchableActivities = pm.queryIntentActivities(mainIntent, 0)
            for (resolveInfo in launchableActivities) {
                val pkgName = resolveInfo.activityInfo.packageName
                packagesToKill.add(pkgName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying packages for kill switch", e)
        }
        
        // Apply exclusions
        val finalPackages = packagesToKill.filter { pkg ->
            pkg !in excludedPackages && !pkg.contains("shizuku") && !pkg.contains("launcher")
        }
        
        if (finalPackages.isEmpty()) {
            return ShizukuShell.CommandResult(0, "No packages to kill", "")
        }
        
        Log.d(TAG, "Force stopping ${finalPackages.size} packages via shell loop")
        
        // Construct a single shell script command executing force-stop on all packages
        val sb = StringBuilder()
        for (pkg in finalPackages) {
            sb.append("am force-stop ").append(pkg).append("; ")
        }
        
        return ShizukuShell.exec(sb.toString())
    }

    /** Get running services for an app */
    suspend fun getRunningServices(packageName: String): List<String> {
        val result = ShizukuShell.exec("dumpsys activity services $packageName")
        if (!result.isSuccess) return emptyList()
        return result.stdout.lines()
            .filter { it.contains("ServiceRecord") }
            .map { it.trim() }
    }

    // ─── System Utilities ────────────────────────────────────────────────

    /** Trigger a system garbage collection hint */
    suspend fun trimMemory(): ShizukuShell.CommandResult {
        return ShizukuShell.exec("am send-trim-memory --user 0 $(am get-current-user) COMPLETE")
    }

    /** Xiaomi Fix: Force Full Screen Gestures to stay ON */
    suspend fun forceGestures(): ShizukuShell.CommandResult {
        return ShizukuShell.exec("settings put secure force_fsg_nav_bar 1")
    }

    /** Global Fix: Hide system navigation bar entirely using immersive mode policy */
    suspend fun hideNavigationBar(): ShizukuShell.CommandResult {
        ShizukuShell.exec("settings put global policy_control immersive.navigation=*")
        return ShizukuShell.exec("settings put global force_fsg_nav_bar 1")
    }

    /** Restore system navigation bar visibility */
    suspend fun showNavigationBar(): ShizukuShell.CommandResult {
        ShizukuShell.exec("settings put global policy_control null")
        return ShizukuShell.exec("settings put global force_fsg_nav_bar 0")
    }

    /** Take a screenshot and save to a path */
    suspend fun screenshot(outputPath: String): ShizukuShell.CommandResult {
        return ShizukuShell.exec("screencap -p $outputPath")
    }

    /** Simulate an input tap at coordinates */
    suspend fun inputTap(x: Int, y: Int): ShizukuShell.CommandResult {
        return ShizukuShell.exec("input tap $x $y")
    }

    // ─── Permission Management ──────────────────────────────────────────
    
    /** Grant a permission to an app */
    suspend fun grantPermission(packageName: String, permission: String): ShizukuShell.CommandResult {
        Log.d(TAG, "Granting $permission to $packageName")
        return ShizukuShell.exec("pm grant $packageName $permission")
    }

    /** Revoke a permission from an app */
    suspend fun revokePermission(packageName: String, permission: String): ShizukuShell.CommandResult {
        Log.d(TAG, "Revoking $permission from $packageName")
        return ShizukuShell.exec("pm revoke $packageName $permission")
    }

    /** Get list of permissions for an app and their states */
    suspend fun getAppPermissions(packageName: String): List<AppPermission> {
        val result = ShizukuShell.exec("dumpsys package $packageName")
        if (!result.isSuccess) return emptyList()
        
        val permissions = mutableListOf<AppPermission>()
        var inRequestedPermissions = false
        var inInstallPermissions = false
        var inRuntimePermissions = false
        
        val grantedPermissions = mutableSetOf<String>()
        
        // First pass: find granted permissions
        result.stdout.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.endsWith(": granted=true")) {
                grantedPermissions.add(trimmed.substringBefore(":"))
            } else if (trimmed.contains("=true")) {
                // Handle different dumpsys formats
                val name = trimmed.substringBefore("=").substringAfterLast(" ")
                grantedPermissions.add(name)
            }
        }
        
        // Second pass: find all requested permissions
        result.stdout.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed == "requested permissions:") {
                inRequestedPermissions = true
            } else if (inRequestedPermissions && (trimmed.endsWith(":") || trimmed.isEmpty())) {
                inRequestedPermissions = false
            } else if (inRequestedPermissions && trimmed.contains("android.permission.")) {
                val permName = trimmed.removePrefix("android.permission.")
                val fullName = "android.permission.$permName"
                permissions.add(AppPermission(fullName, permName, grantedPermissions.contains(fullName)))
            }
        }
        
        return permissions.distinctBy { it.fullName }
    }

    data class AppPermission(
        val fullName: String,
        val displayName: String,
        val isGranted: Boolean
    )

    // ─── Data Classes ────────────────────────────────────────────────────

    data class AppStorageInfo(
        val codePath: String,
        val dataDir: String,
        val cacheDir: String
    )

    private fun extractField(text: String, prefix: String): String {
        return text.lines()
            .find { it.trim().startsWith(prefix) }
            ?.substringAfter(prefix)
            ?.trim()
            ?: "Unknown"
    }
}
