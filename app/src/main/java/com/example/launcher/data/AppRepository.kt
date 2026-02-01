package com.example.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.launcher.model.AppCategory
import com.example.launcher.model.AppModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    suspend fun getInstalledApps(): List<AppModel> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        val resolveInfos = packageManager.queryIntentActivities(intent, 0)
        val prefsManager = com.example.launcher.utils.PreferencesManager(context)
        val hiddenApps = prefsManager.getHiddenApps()
        
        resolveInfos.mapNotNull { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo
            if (activityInfo.packageName == context.packageName) return@mapNotNull null // Exclude self
            
            // Filter out hidden apps
            if (hiddenApps.contains(activityInfo.packageName)) return@mapNotNull null

            val label = resolveInfo.loadLabel(packageManager).toString()
            val packageName = activityInfo.packageName
            val icon = resolveInfo.loadIcon(packageManager)
            
            // Basic categorization logic based on flags or package name (simplified for now)
            val category = categorizeApp(activityInfo.applicationInfo)

            AppModel(label, packageName, icon, category)
        }.sortedBy { it.label }
    }

    private fun categorizeApp(applicationInfo: ApplicationInfo): AppCategory {
        // In a real app, this would use a more sophisticated method, possibly an API or a local DB
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
}
