package com.vertigo.launcher.utils

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat

class IconPackManager(private val context: Context) {
    
    private val prefs = StorageHelper.getSafeSharedPreferences(context, "launcher_prefs")
    private var iconPackPackageName: String? = null
    private var iconPackResources: Resources? = null
    
    companion object {
        const val PREF_ICON_PACK = "icon_pack"
    }
    
    init {
        loadIconPack()
    }
    
    fun getAvailableIconPacks(): List<Pair<String, String>> {
        val iconPacks = mutableListOf<Pair<String, String>>()
        iconPacks.add("default" to "Default")
        
        val pm = context.packageManager
        val intent = android.content.Intent("org.adw.launcher.THEMES")
        
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
        for (info in resolveInfos) {
            iconPacks.add(info.activityInfo.packageName to info.loadLabel(pm).toString())
        }
        
        return iconPacks
    }
    
    fun setIconPack(packageName: String) {
        prefs.edit().putString(PREF_ICON_PACK, packageName).apply()
        loadIconPack()
    }
    
    private fun loadIconPack() {
        iconPackPackageName = prefs.getString(PREF_ICON_PACK, "default")
        
        if (iconPackPackageName != "default" && iconPackPackageName != null) {
            try {
                iconPackResources = context.packageManager.getResourcesForApplication(iconPackPackageName!!)
            } catch (e: Exception) {
                iconPackResources = null
            }
        } else {
            iconPackResources = null
        }
    }
    
    fun getIconForPackage(packageName: String, defaultIcon: Drawable): Drawable {
        if (iconPackResources == null) return defaultIcon
        
        try {
            // Try to find icon by package name
            val iconName = packageName.replace(".", "_")
            val iconId = iconPackResources?.getIdentifier(iconName, "drawable", iconPackPackageName)
            
            if (iconId != null && iconId != 0) {
                return ResourcesCompat.getDrawable(iconPackResources!!, iconId, null) ?: defaultIcon
            }
        } catch (e: Exception) {
            // Return default icon if there's an error
        }
        
        return defaultIcon
    }
}
