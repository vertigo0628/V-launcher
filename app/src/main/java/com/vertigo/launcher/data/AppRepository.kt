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
        // System apps check first
        if ((applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
            (applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
            // But if it's explicitly a game, still call it a game
            if ((applicationInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0) {
                return AppCategory.GAMES
            }
            return AppCategory.SYSTEM
        }

        if ((applicationInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0) {
            return AppCategory.GAMES
        }

        val packageName = applicationInfo.packageName.lowercase()
        return when {
            packageName.contains("messaging") || packageName.contains("sms") || packageName.contains("whatsapp") || packageName.contains("telegram") -> AppCategory.COMMUNICATION
            packageName.contains("chrome") || packageName.contains("browser") || packageName.contains("firefox") -> AppCategory.INTERNET
            packageName.contains("camera") || packageName.contains("music") || packageName.contains("video") || packageName.contains("gallery") || packageName.contains("photos") || packageName.contains("youtube") -> AppCategory.MEDIA
            packageName.contains("settings") || packageName.contains("config") -> AppCategory.SETTINGS
            else -> AppCategory.UTILITIES
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
