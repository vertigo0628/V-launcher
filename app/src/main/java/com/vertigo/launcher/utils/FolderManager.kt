package com.vertigo.launcher.utils

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * FolderManager - Manages app folders
 */
class FolderManager(context: Context) {
    
    private val prefs: SharedPreferences = 
        StorageHelper.getSafeSharedPreferences(context, "folder_prefs")
    
    companion object {
        private const val PREF_FOLDERS = "folders"
    }
    
    data class Folder(
        val id: String,
        var name: String,
        var iconColor: Int,
        var apps: MutableList<String>,
        var gridSize: Int = 3, // 3x3, 4x4, etc.
        var x: Int = 0, // Position in home grid
        var y: Int = 0
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("id", id)
                put("name", name)
                put("iconColor", iconColor)
                put("apps", JSONArray(apps))
                put("gridSize", gridSize)
                put("x", x)
                put("y", y)
            }
        }
        
        companion object {
            fun fromJson(json: JSONObject): Folder {
                val appsArray = json.optJSONArray("apps") ?: JSONArray()
                val apps = mutableListOf<String>()
                for (i in 0 until appsArray.length()) {
                    apps.add(appsArray.getString(i))
                }
                
                return Folder(
                    id = json.optString("id"),
                    name = json.optString("name", "Folder"),
                    iconColor = json.optInt("iconColor", 0xFF00F0FF.toInt()),
                    apps = apps,
                    gridSize = json.optInt("gridSize", 3),
                    x = json.optInt("x", 0),
                    y = json.optInt("y", 0)
                )
            }
        }
    }
    
    private var foldersCache: MutableList<Folder>? = null
    
    /**
     * Get all folders
     */
    fun getFolders(): List<Folder> {
        if (foldersCache == null) loadFolders()
        return foldersCache!!.toList()
    }
    
    /**
     * Get a folder by ID
     */
    fun getFolder(id: String): Folder? {
        if (foldersCache == null) loadFolders()
        return foldersCache!!.find { it.id == id }
    }
    
    /**
     * Get folder containing an app
     */
    fun getFolderForApp(packageName: String): Folder? {
        if (foldersCache == null) loadFolders()
        return foldersCache!!.find { packageName in it.apps }
    }
    
    /**
     * Check if app is in any folder
     */
    fun isAppInFolder(packageName: String): Boolean {
        return getFolderForApp(packageName) != null
    }
    
    private fun loadFolders() {
        val json = prefs.getString(PREF_FOLDERS, null)
        
        if (json == null) {
            foldersCache = mutableListOf()
        } else {
            try {
                val array = JSONArray(json)
                foldersCache = mutableListOf()
                for (i in 0 until array.length()) {
                    foldersCache!!.add(Folder.fromJson(array.getJSONObject(i)))
                }
            } catch (e: Exception) {
                foldersCache = mutableListOf()
            }
        }
    }
    
    private fun saveFolders() {
        val array = JSONArray()
        foldersCache?.forEach { array.put(it.toJson()) }
        prefs.edit().putString(PREF_FOLDERS, array.toString()).apply()
    }
    
    /**
     * Create a new folder
     */
    fun createFolder(name: String, initialApps: List<String> = emptyList(), color: Int = 0xFF00F0FF.toInt()): Folder {
        if (foldersCache == null) loadFolders()
        
        val id = "folder_${System.currentTimeMillis()}"
        val folder = Folder(id, name, color, initialApps.toMutableList())
        
        foldersCache!!.add(folder)
        saveFolders()
        
        return folder
    }
    
    /**
     * Create folder by merging two apps (drag & drop)
     */
    fun createFolderFromApps(app1: String, app2: String, name: String = "Folder"): Folder {
        // Remove from any existing folders first
        removeAppFromFolders(app1)
        removeAppFromFolders(app2)
        
        return createFolder(name, listOf(app1, app2))
    }
    
    /**
     * Update folder properties
     */
    fun updateFolder(id: String, name: String? = null, color: Int? = null, gridSize: Int? = null) {
        if (foldersCache == null) loadFolders()
        
        val folder = foldersCache!!.find { it.id == id } ?: return
        name?.let { folder.name = it }
        color?.let { folder.iconColor = it }
        gridSize?.let { folder.gridSize = it.coerceIn(2, 5) }
        
        saveFolders()
    }
    
    /**
     * Delete a folder (apps return to main grid)
     */
    fun deleteFolder(id: String) {
        if (foldersCache == null) loadFolders()
        foldersCache!!.removeAll { it.id == id }
        saveFolders()
    }
    
    /**
     * Add an app to a folder
     */
    fun addAppToFolder(folderId: String, packageName: String) {
        if (foldersCache == null) loadFolders()
        
        // Remove from any other folder first
        removeAppFromFolders(packageName)
        
        val folder = foldersCache!!.find { it.id == folderId } ?: return
        if (packageName !in folder.apps) {
            folder.apps.add(packageName)
            saveFolders()
        }
    }
    
    /**
     * Remove an app from a folder
     */
    fun removeAppFromFolder(folderId: String, packageName: String) {
        if (foldersCache == null) loadFolders()
        
        val folder = foldersCache!!.find { it.id == folderId } ?: return
        folder.apps.remove(packageName)
        
        // Delete folder if empty
        if (folder.apps.isEmpty()) {
            deleteFolder(folderId)
        } else if (folder.apps.size == 1) {
            // Optionally delete folder if only 1 app left
            deleteFolder(folderId)
        } else {
            saveFolders()
        }
    }
    
    /**
     * Remove app from all folders
     */
    fun removeAppFromFolders(packageName: String) {
        if (foldersCache == null) loadFolders()
        
        foldersCache!!.forEach { folder ->
            folder.apps.remove(packageName)
        }
        
        // Clean up empty folders
        foldersCache!!.removeAll { it.apps.isEmpty() || it.apps.size == 1 }
        saveFolders()
    }
    
    /**
     * Reorder apps within a folder
     */
    fun reorderAppsInFolder(folderId: String, orderedApps: List<String>) {
        if (foldersCache == null) loadFolders()
        
        val folder = foldersCache!!.find { it.id == folderId } ?: return
        folder.apps.clear()
        folder.apps.addAll(orderedApps)
        saveFolders()
    }
    
    /**
     * Move folder position
     */
    fun moveFolder(folderId: String, x: Int, y: Int) {
        if (foldersCache == null) loadFolders()
        
        val folder = foldersCache!!.find { it.id == folderId } ?: return
        folder.x = x
        folder.y = y
        saveFolders()
    }
}
