package com.vertigo.launcher.utils

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * CategoryManager - Manages editable app categories
 */
class CategoryManager(context: Context) {
    
    private val prefs: SharedPreferences = 
        StorageHelper.getSafeSharedPreferences(context, "category_prefs")
    
    companion object {
        private const val PREF_CATEGORIES = "categories"
        private const val PREF_APP_CATEGORIES = "app_categories"
        
        // Default category icons
        val DEFAULT_CATEGORIES = listOf(
            Category("all", "All", "ic_category_all", 0xFF00F0FF.toInt()),
            Category("games", "Games", "ic_category_games", 0xFFFF006E.toInt()),
            Category("social", "Social", "ic_category_social", 0xFF00F0FF.toInt()),
            Category("productivity", "Work", "ic_category_work", 0xFFBF00FF.toInt()),
            Category("media", "Media", "ic_category_media", 0xFFFF006E.toInt()),
            Category("utilities", "Utilities", "ic_category_utilities", 0xFF00F0FF.toInt()),
            Category("other", "Other", "ic_category_other", 0xFFBF00FF.toInt())
        )
    }
    
    data class Category(
        val id: String,
        var name: String,
        var iconName: String,
        var color: Int,
        var order: Int = 0,
        var isVisible: Boolean = true
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("id", id)
                put("name", name)
                put("iconName", iconName)
                put("color", color)
                put("order", order)
                put("isVisible", isVisible)
            }
        }
        
        companion object {
            fun fromJson(json: JSONObject): Category {
                return Category(
                    id = json.optString("id"),
                    name = json.optString("name"),
                    iconName = json.optString("iconName", "ic_category_other"),
                    color = json.optInt("color", 0xFFFFFFFF.toInt()),
                    order = json.optInt("order", 0),
                    isVisible = json.optBoolean("isVisible", true)
                )
            }
        }
    }
    
    private var categoriesCache: MutableList<Category>? = null
    private var appCategoriesCache: MutableMap<String, String>? = null
    
    /**
     * Get all categories
     */
    fun getCategories(): List<Category> {
        if (categoriesCache == null) {
            loadCategories()
        }
        return categoriesCache!!.filter { it.isVisible }.sortedBy { it.order }
    }
    
    /**
     * Get all categories including hidden
     */
    fun getAllCategories(): List<Category> {
        if (categoriesCache == null) {
            loadCategories()
        }
        return categoriesCache!!.sortedBy { it.order }
    }
    
    private fun loadCategories() {
        val json = prefs.getString(PREF_CATEGORIES, null)
        
        if (json == null) {
            // Use defaults
            categoriesCache = DEFAULT_CATEGORIES.mapIndexed { index, cat ->
                cat.copy(order = index)
            }.toMutableList()
            saveCategories()
        } else {
            try {
                val array = JSONArray(json)
                categoriesCache = mutableListOf()
                for (i in 0 until array.length()) {
                    categoriesCache!!.add(Category.fromJson(array.getJSONObject(i)))
                }
            } catch (e: Exception) {
                categoriesCache = DEFAULT_CATEGORIES.toMutableList()
            }
        }
    }
    
    private fun saveCategories() {
        val array = JSONArray()
        categoriesCache?.forEach { array.put(it.toJson()) }
        prefs.edit().putString(PREF_CATEGORIES, array.toString()).apply()
    }
    
    /**
     * Add a new category
     */
    fun addCategory(name: String, iconName: String = "ic_category_other", color: Int = 0xFFFFFFFF.toInt()): Category {
        if (categoriesCache == null) loadCategories()
        
        val id = "custom_${System.currentTimeMillis()}"
        val order = categoriesCache!!.maxOfOrNull { it.order }?.plus(1) ?: 0
        val category = Category(id, name, iconName, color, order)
        
        categoriesCache!!.add(category)
        saveCategories()
        
        return category
    }
    
    /**
     * Update a category
     */
    fun updateCategory(id: String, name: String? = null, iconName: String? = null, color: Int? = null) {
        if (categoriesCache == null) loadCategories()
        
        val category = categoriesCache!!.find { it.id == id } ?: return
        name?.let { category.name = it }
        iconName?.let { category.iconName = it }
        color?.let { category.color = it }
        
        saveCategories()
    }
    
    /**
     * Delete a category (moves apps to "other")
     */
    fun deleteCategory(id: String) {
        if (categoriesCache == null) loadCategories()
        
        // Move all apps in this category to "other"
        getAppsInCategory(id).forEach { packageName ->
            setAppCategory(packageName, "other")
        }
        
        categoriesCache!!.removeAll { it.id == id }
        saveCategories()
    }
    
    /**
     * Hide/show a category
     */
    fun setCategoryVisibility(id: String, visible: Boolean) {
        if (categoriesCache == null) loadCategories()
        
        categoriesCache!!.find { it.id == id }?.isVisible = visible
        saveCategories()
    }
    
    /**
     * Reorder categories
     */
    fun reorderCategories(orderedIds: List<String>) {
        if (categoriesCache == null) loadCategories()
        
        orderedIds.forEachIndexed { index, id ->
            categoriesCache!!.find { it.id == id }?.order = index
        }
        saveCategories()
    }
    
    // ===== App-Category Assignments =====
    
    /**
     * Get category for an app
     */
    fun getAppCategory(packageName: String): String {
        if (appCategoriesCache == null) loadAppCategories()
        return appCategoriesCache!![packageName] ?: autoClassifyApp(packageName)
    }
    
    /**
     * Set category for an app
     */
    fun setAppCategory(packageName: String, categoryId: String) {
        if (appCategoriesCache == null) loadAppCategories()
        appCategoriesCache!![packageName] = categoryId
        saveAppCategories()
    }
    
    /**
     * Get all apps in a category
     */
    fun getAppsInCategory(categoryId: String): List<String> {
        if (appCategoriesCache == null) loadAppCategories()
        return appCategoriesCache!!.filter { it.value == categoryId }.keys.toList()
    }
    
    private fun loadAppCategories() {
        val json = prefs.getString(PREF_APP_CATEGORIES, null)
        
        if (json == null) {
            appCategoriesCache = mutableMapOf()
        } else {
            try {
                val obj = JSONObject(json)
                appCategoriesCache = mutableMapOf()
                obj.keys().forEach { key ->
                    appCategoriesCache!![key] = obj.getString(key)
                }
            } catch (e: Exception) {
                appCategoriesCache = mutableMapOf()
            }
        }
    }
    
    private fun saveAppCategories() {
        val obj = JSONObject()
        appCategoriesCache?.forEach { (key, value) ->
            obj.put(key, value)
        }
        prefs.edit().putString(PREF_APP_CATEGORIES, obj.toString()).apply()
    }
    
    /**
     * Auto-classify app based on package name patterns
     */
    private fun autoClassifyApp(packageName: String): String {
        return when {
            // Games
            packageName.contains("game", ignoreCase = true) ||
            packageName.contains("play", ignoreCase = true) -> "games"
            
            // Social
            packageName.contains("facebook") ||
            packageName.contains("twitter") ||
            packageName.contains("instagram") ||
            packageName.contains("whatsapp") ||
            packageName.contains("telegram") ||
            packageName.contains("snapchat") ||
            packageName.contains("tiktok") ||
            packageName.contains("messenger") -> "social"
            
            // Productivity
            packageName.contains("docs") ||
            packageName.contains("office") ||
            packageName.contains("sheets") ||
            packageName.contains("drive") ||
            packageName.contains("calendar") ||
            packageName.contains("mail") ||
            packageName.contains("gmail") -> "productivity"
            
            // Media
            packageName.contains("music") ||
            packageName.contains("video") ||
            packageName.contains("youtube") ||
            packageName.contains("spotify") ||
            packageName.contains("netflix") ||
            packageName.contains("camera") ||
            packageName.contains("gallery") ||
            packageName.contains("photos") -> "media"
            
            // Utilities
            packageName.contains("settings") ||
            packageName.contains("calculator") ||
            packageName.contains("clock") ||
            packageName.contains("flashlight") ||
            packageName.contains("file") -> "utilities"
            
            else -> "other"
        }
    }
}
