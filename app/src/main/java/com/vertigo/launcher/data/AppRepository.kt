package com.vertigo.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.vertigo.launcher.model.AppCategory
import com.vertigo.launcher.model.AppModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "AppRepository"

class AppRepository(private val context: Context) {
    private val preferencesManager = com.vertigo.launcher.utils.PreferencesManager(context)

    suspend fun getInstalledApps(): List<AppModel> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager

        // Primary path: queryIntentActivities with minimal flags (0)
        // Wrapped in try-catch to survive TransactionTooLargeException / DeadObjectException
        try {
            getAppsViaIntentQuery(packageManager)
        } catch (e: Exception) {
            Log.e(TAG, "queryIntentActivities failed (likely Binder buffer overflow), falling back", e)
            getAppsViaInstalledApplications(packageManager)
        }
    }

    /**
     * Primary path: query for LAUNCHER activities.
     * Icon loading is deferred (loaded per-item, not in the Binder parcel).
     */
    private fun getAppsViaIntentQuery(packageManager: PackageManager): List<AppModel> {
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        // Use flag 0 — the lightest possible query to minimise Binder payload
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)

        return resolveInfos.mapNotNull { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo
            if (activityInfo.packageName == context.packageName) return@mapNotNull null

            val packageName = activityInfo.packageName
            val label = try {
                resolveInfo.loadLabel(packageManager).toString()
            } catch (e: Exception) {
                packageName
            }

            val category = categorizeApp(activityInfo.applicationInfo)
            AppModel(
                label = label,
                packageName = packageName,
                iconLoader = {
                    com.vertigo.launcher.utils.PerformanceOptimizer.getCachedIcon(packageName) {
                        try {
                            resolveInfo.loadIcon(packageManager)
                        } catch (e: Exception) {
                            packageManager.defaultActivityIcon
                        }
                    } ?: packageManager.defaultActivityIcon
                },
                category = category
            )
        }.sortedBy { it.label }
    }

    /**
     * Fallback path: uses getInstalledApplications which returns a smaller parcel
     * (no ResolveInfo overhead). Only includes apps that have a launcher intent.
     */
    private fun getAppsViaInstalledApplications(packageManager: PackageManager): List<AppModel> {
        Log.w(TAG, "Using fallback getInstalledApplications path")
        val applications = try {
            packageManager.getInstalledApplications(0)
        } catch (e: Exception) {
            Log.e(TAG, "Even getInstalledApplications failed", e)
            return emptyList()
        }

        return applications.mapNotNull { appInfo ->
            if (appInfo.packageName == context.packageName) return@mapNotNull null

            // Only include apps that have a launchable activity
            val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
                ?: return@mapNotNull null

            val packageName = appInfo.packageName
            val label = try {
                appInfo.loadLabel(packageManager).toString()
            } catch (e: Exception) {
                packageName
            }

            val category = categorizeApp(appInfo)
            AppModel(
                label = label,
                packageName = packageName,
                iconLoader = {
                    com.vertigo.launcher.utils.PerformanceOptimizer.getCachedIcon(packageName) {
                        try {
                            appInfo.loadIcon(packageManager)
                        } catch (e: Exception) {
                            packageManager.defaultActivityIcon
                        }
                    } ?: packageManager.defaultActivityIcon
                },
                category = category
            )
        }.sortedBy { it.label }
    }

    private fun categorizeApp(applicationInfo: ApplicationInfo): AppCategory {
        val packageName = applicationInfo.packageName
        
        // 1. Check custom overrides from PreferencesManager first
        preferencesManager.getAppCategoryOverride(packageName)?.let { overrideStr ->
            try {
                return AppCategory.valueOf(overrideStr)
            } catch (e: Exception) {
                // Ignore and fall through if invalid
            }
        }

        // 2. System apps check
        val isSystem = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                (applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        
        if (isSystem) {
            // But if it's explicitly a game, still call it a game
            if ((applicationInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0) {
                return AppCategory.GAMES
            }
            return AppCategory.SYSTEM
        }

        if ((applicationInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0) {
            return AppCategory.GAMES
        }

        // 3. Native category mapping (Android 8.0 / API 26+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            when (applicationInfo.category) {
                ApplicationInfo.CATEGORY_GAME -> return AppCategory.GAMES
                ApplicationInfo.CATEGORY_AUDIO,
                ApplicationInfo.CATEGORY_VIDEO,
                ApplicationInfo.CATEGORY_IMAGE -> return AppCategory.MEDIA
                ApplicationInfo.CATEGORY_SOCIAL -> return AppCategory.COMMUNICATION
                ApplicationInfo.CATEGORY_NEWS -> return AppCategory.INTERNET
                ApplicationInfo.CATEGORY_MAPS,
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> return AppCategory.UTILITIES
            }
        }

        // 4. Custom Keyword Ruleset matching (package name checks)
        val pkgLower = packageName.lowercase()
        return when {
            // Communication
            pkgLower.contains("message") || pkgLower.contains("sms") || pkgLower.contains("whatsapp") || 
            pkgLower.contains("telegram") || pkgLower.contains("signal") || pkgLower.contains("messenger") || 
            pkgLower.contains("discord") || pkgLower.contains("slack") || pkgLower.contains("viber") || 
            pkgLower.contains("line") || pkgLower.contains("skype") || pkgLower.contains("dialer") || 
            pkgLower.contains("contacts") || pkgLower.contains("phone") || pkgLower.contains("mail") || 
            pkgLower.contains("outlook") || pkgLower.contains("gmail") || pkgLower.contains("chat") ||
            pkgLower.contains("wechat") -> AppCategory.COMMUNICATION

            // Internet
            pkgLower.contains("chrome") || pkgLower.contains("browser") || pkgLower.contains("firefox") || 
            pkgLower.contains("opera") || pkgLower.contains("safari") || pkgLower.contains("edge") || 
            pkgLower.contains("brave") || pkgLower.contains("duckduckgo") || pkgLower.contains("pinterest") || 
            pkgLower.contains("reddit") || pkgLower.contains("twitter") || pkgLower.contains("tiktok") || 
            pkgLower.contains("instagram") || pkgLower.contains("facebook") || pkgLower.contains("tumblr") -> AppCategory.INTERNET

            // Games
            pkgLower.contains("game") || pkgLower.contains("arcade") || pkgLower.contains("puzzle") || 
            pkgLower.contains("action") || pkgLower.contains("rpg") || pkgLower.contains("simulation") || 
            pkgLower.contains("strategy") || pkgLower.contains("board") || pkgLower.contains("card") || 
            pkgLower.contains("angrybirds") || pkgLower.contains("pubg") || pkgLower.contains("clash") || 
            pkgLower.contains("roblox") || pkgLower.contains("minecraft") || pkgLower.contains("sudoku") ||
            pkgLower.contains("chess") || pkgLower.contains("solitaire") -> AppCategory.GAMES

            // Media
            pkgLower.contains("camera") || pkgLower.contains("music") || pkgLower.contains("video") || 
            pkgLower.contains("gallery") || pkgLower.contains("photos") || pkgLower.contains("youtube") || 
            pkgLower.contains("spotify") || pkgLower.contains("netflix") || pkgLower.contains("player") || 
            pkgLower.contains("recorder") || pkgLower.contains("vlc") || pkgLower.contains("hulu") || 
            pkgLower.contains("disney") || pkgLower.contains("twitch") || pkgLower.contains("tv") || 
            pkgLower.contains("fm") || pkgLower.contains("radio") || pkgLower.contains("sound") ||
            pkgLower.contains("deezer") || pkgLower.contains("primevideo") -> AppCategory.MEDIA

            // Settings
            pkgLower.contains("settings") || pkgLower.contains("config") || pkgLower.contains("setup") || 
            pkgLower.contains("preference") || pkgLower.contains("control") || pkgLower.contains("customizer") -> AppCategory.SETTINGS

            // System
            pkgLower.contains("system") || pkgLower.contains("android") || pkgLower.contains("launcher") || 
            pkgLower.contains("keyboard") || pkgLower.contains("inputmethod") || pkgLower.contains("provider") || 
            pkgLower.contains("service") || pkgLower.contains("packageinstaller") -> AppCategory.SYSTEM

            // Utilities
            pkgLower.contains("calculator") || pkgLower.contains("calendar") || pkgLower.contains("clock") || 
            pkgLower.contains("notes") || pkgLower.contains("weather") || pkgLower.contains("file") || 
            pkgLower.contains("document") || pkgLower.contains("pdf") || pkgLower.contains("wallet") || 
            pkgLower.contains("pay") || pkgLower.contains("map") || pkgLower.contains("drive") || 
            pkgLower.contains("office") || pkgLower.contains("scan") || pkgLower.contains("keeps") ||
            pkgLower.contains("weather") || pkgLower.contains("compass") || pkgLower.contains("tasks") -> AppCategory.UTILITIES

            else -> AppCategory.OTHER
        }
    }
    suspend fun getFrozenApps(): List<AppModel> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val applications = try {
            // MATCH_DISABLED_COMPONENTS includes apps disabled via 'pm disable-user'
            packageManager.getInstalledApplications(PackageManager.MATCH_DISABLED_COMPONENTS)
        } catch (e: Exception) {
            Log.e(TAG, "getFrozenApps failed", e)
            return@withContext emptyList()
        }

        applications.mapNotNull { appInfo ->
            if (appInfo.packageName == context.packageName) return@mapNotNull null
            
            // Only include if it's currently disabled
            if (appInfo.enabled) return@mapNotNull null

            val packageName = appInfo.packageName
            val label = try {
                appInfo.loadLabel(packageManager).toString()
            } catch (e: Exception) {
                packageName
            }

            val category = categorizeApp(appInfo)
            AppModel(
                label = label,
                packageName = packageName,
                iconLoader = {
                    com.vertigo.launcher.utils.PerformanceOptimizer.getCachedIcon(packageName) {
                        try {
                            appInfo.loadIcon(packageManager)
                        } catch (e: Exception) {
                            packageManager.defaultActivityIcon
                        }
                    } ?: packageManager.defaultActivityIcon
                },
                category = category
            )
        }.sortedBy { it.label }
    }
}
