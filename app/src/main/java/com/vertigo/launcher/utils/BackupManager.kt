package com.vertigo.launcher.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * BackupManager - Backup and restore launcher settings
 */
class BackupManager(private val context: Context) {
    
    companion object {
        private const val BACKUP_VERSION = 1
        
        // Preferences to backup
        private val PREFS_TO_BACKUP = listOf(
            "launcher_prefs",
            "theme_prefs",
            "icon_prefs",
            "category_prefs",
            "folder_prefs",
            "gesture_prefs",
            "dock_prefs",
            "smart_widget_prefs"
        )
    }
    
    data class BackupInfo(
        val fileName: String,
        val timestamp: Long,
        val version: Int,
        val size: Long
    )
    
    /**
     * Get backup directory
     */
    private fun getBackupDir(): File {
        val dir = File(context.getExternalFilesDir(null), "backups")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    
    /**
     * Create a backup of all settings
     */
    fun createBackup(customName: String? = null): File? {
        try {
            val timestamp = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
            val fileName = customName ?: "vlauncher_backup_${dateFormat.format(Date(timestamp))}.json"
            
            val backup = JSONObject().apply {
                put("version", BACKUP_VERSION)
                put("timestamp", timestamp)
                put("appVersion", getAppVersion())
                
                // Backup each preference file
                val prefsData = JSONObject()
                PREFS_TO_BACKUP.forEach { prefName ->
                    val prefs = StorageHelper.getSafeSharedPreferences(context, prefName)
                    val prefJson = backupPreferences(prefs)
                    prefsData.put(prefName, prefJson)
                }
                put("preferences", prefsData)
                
                // Backup additional data
                put("dockApps", backupDockApps())
                put("hiddenApps", backupHiddenApps())
                put("categories", backupCategories())
                put("folders", backupFolders())
            }
            
            val file = File(getBackupDir(), fileName)
            file.writeText(backup.toString(2))
            
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Restore settings from backup
     */
    fun restoreBackup(backupFile: File): Boolean {
        try {
            val content = backupFile.readText()
            val backup = JSONObject(content)
            
            val version = backup.optInt("version", 0)
            if (version > BACKUP_VERSION) {
                // Backup from newer version, might not be compatible
                return false
            }
            
            // Restore preferences
            val prefsData = backup.optJSONObject("preferences")
            prefsData?.keys()?.forEach { prefName ->
                if (prefName in PREFS_TO_BACKUP) {
                    val prefJson = prefsData.getJSONObject(prefName)
                    val prefs = StorageHelper.getSafeSharedPreferences(context, prefName)
                    restorePreferences(prefs, prefJson)
                }
            }
            
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * List available backups
     */
    fun listBackups(): List<BackupInfo> {
        return getBackupDir().listFiles { file ->
            file.extension == "json" && file.name.startsWith("vlauncher_backup")
        }?.map { file ->
            try {
                val content = file.readText()
                val json = JSONObject(content)
                BackupInfo(
                    fileName = file.name,
                    timestamp = json.optLong("timestamp", file.lastModified()),
                    version = json.optInt("version", 0),
                    size = file.length()
                )
            } catch (e: Exception) {
                BackupInfo(file.name, file.lastModified(), 0, file.length())
            }
        }?.sortedByDescending { it.timestamp } ?: emptyList()
    }
    
    /**
     * Delete a backup
     */
    fun deleteBackup(fileName: String): Boolean {
        val file = File(getBackupDir(), fileName)
        return file.exists() && file.delete()
    }
    
    private fun backupPreferences(prefs: SharedPreferences): JSONObject {
        val json = JSONObject()
        prefs.all.forEach { (key, value) ->
            when (value) {
                is String -> json.put(key, value)
                is Int -> json.put(key, value)
                is Long -> json.put(key, value)
                is Float -> json.put(key, value.toDouble())
                is Boolean -> json.put(key, value)
                is Set<*> -> json.put(key, org.json.JSONArray(value))
            }
        }
        return json
    }
    
    private fun restorePreferences(prefs: SharedPreferences, json: JSONObject) {
        val editor = prefs.edit().clear()
        
        json.keys().forEach { key ->
            when (val value = json.get(key)) {
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Double -> editor.putFloat(key, value.toFloat())
                is Boolean -> editor.putBoolean(key, value)
                is org.json.JSONArray -> {
                    val set = mutableSetOf<String>()
                    for (i in 0 until value.length()) {
                        set.add(value.getString(i))
                    }
                    editor.putStringSet(key, set)
                }
            }
        }
        
        editor.apply()
    }
    
    private fun backupDockApps(): org.json.JSONArray {
        val prefs = StorageHelper.getSafeSharedPreferences(context, "dock_prefs")
        val apps = prefs.getString("dock_apps", null)
        return if (apps != null) org.json.JSONArray(apps.split(",")) else org.json.JSONArray()
    }
    
    private fun backupHiddenApps(): org.json.JSONArray {
        val prefs = StorageHelper.getSafeSharedPreferences(context, "launcher_prefs")
        val apps = prefs.getStringSet("hidden_apps", emptySet()) ?: emptySet()
        return org.json.JSONArray(apps.toList())
    }
    
    private fun backupCategories(): JSONObject? {
        val prefs = StorageHelper.getSafeSharedPreferences(context, "category_prefs")
        val json = prefs.getString("categories", null)
        return if (json != null) JSONObject().put("data", json) else null
    }
    
    private fun backupFolders(): JSONObject? {
        val prefs = StorageHelper.getSafeSharedPreferences(context, "folder_prefs")
        val json = prefs.getString("folders", null)
        return if (json != null) JSONObject().put("data", json) else null
    }
    
    private fun getAppVersion(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }
}
