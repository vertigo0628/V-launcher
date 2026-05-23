package com.vertigo.launcher.model

import android.graphics.drawable.Drawable

class AppModel(
    val label: String,
    val packageName: String,
    private val iconLoader: () -> Drawable,
    val category: AppCategory,
    val badgeCount: Int = 0
) {
    val icon: Drawable by lazy { iconLoader() }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppModel) return false
        return packageName == other.packageName &&
               label == other.label &&
               category == other.category &&
               badgeCount == other.badgeCount
    }

    override fun hashCode(): Int {
        var result = label.hashCode()
        result = 31 * result + packageName.hashCode()
        result = 31 * result + category.hashCode()
        result = 31 * result + badgeCount
        return result
    }

    override fun toString(): String {
        return "AppModel(label='$label', packageName='$packageName', category=$category, badgeCount=$badgeCount)"
    }
}

enum class AppCategory {
    COMMUNICATION,
    INTERNET,
    GAMES,
    MEDIA,
    UTILITIES,
    SETTINGS,
    SYSTEM,
    OTHER
}
