package com.example.launcher.model

import android.graphics.drawable.Drawable

data class AppModel(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    val category: AppCategory,
    val badgeCount: Int = 0
)

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
