package com.example.launcher.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette

/**
 * ThemeEngine - Extracts colors from wallpaper and generates ambient theme
 * Smart Launcher 6 style automatic theming
 */
class ThemeEngine(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    
    companion object {
        const val PREF_PRIMARY = "ambient_primary"
        const val PREF_SECONDARY = "ambient_secondary"
        const val PREF_TERTIARY = "ambient_tertiary"
        const val PREF_TEXT_COLOR = "text_color"
        const val PREF_IS_DARK = "is_dark_wallpaper"
        
        // Default colors
        const val DEFAULT_PRIMARY = 0xFF6366F1.toInt()
        const val DEFAULT_SECONDARY = 0xFF8B5CF6.toInt()
        const val DEFAULT_TERTIARY = 0xFFEC4899.toInt()
    }
    
    data class AmbientTheme(
        val primary: Int,
        val secondary: Int,
        val tertiary: Int,
        val textColor: Int,
        val isDark: Boolean
    )
    
    fun extractColorsFromDrawable(drawable: Drawable?): AmbientTheme {
        if (drawable == null) return getDefaultTheme()
        
        val bitmap = when (drawable) {
            is BitmapDrawable -> drawable.bitmap
            else -> return getDefaultTheme()
        }
        
        return extractColorsFromBitmap(bitmap)
    }
    
    fun extractColorsFromBitmap(bitmap: Bitmap): AmbientTheme {
        val palette = Palette.from(bitmap).generate()
        
        val vibrant = palette.vibrantSwatch?.rgb ?: DEFAULT_PRIMARY
        val muted = palette.mutedSwatch?.rgb ?: DEFAULT_SECONDARY
        val darkVibrant = palette.darkVibrantSwatch?.rgb ?: DEFAULT_TERTIARY
        
        // Determine if wallpaper is dark
        val dominantColor = palette.dominantSwatch?.rgb ?: Color.BLACK
        val luminance = ColorUtils.calculateLuminance(dominantColor)
        val isDark = luminance < 0.5
        
        val textColor = if (isDark) Color.WHITE else Color.BLACK
        
        val theme = AmbientTheme(
            primary = vibrant,
            secondary = muted,
            tertiary = darkVibrant,
            textColor = textColor,
            isDark = isDark
        )
        
        // Save to preferences
        saveTheme(theme)
        
        return theme
    }
    
    private fun saveTheme(theme: AmbientTheme) {
        prefs.edit()
            .putInt(PREF_PRIMARY, theme.primary)
            .putInt(PREF_SECONDARY, theme.secondary)
            .putInt(PREF_TERTIARY, theme.tertiary)
            .putInt(PREF_TEXT_COLOR, theme.textColor)
            .putBoolean(PREF_IS_DARK, theme.isDark)
            .apply()
    }
    
    fun getSavedTheme(): AmbientTheme {
        return AmbientTheme(
            primary = prefs.getInt(PREF_PRIMARY, DEFAULT_PRIMARY),
            secondary = prefs.getInt(PREF_SECONDARY, DEFAULT_SECONDARY),
            tertiary = prefs.getInt(PREF_TERTIARY, DEFAULT_TERTIARY),
            textColor = prefs.getInt(PREF_TEXT_COLOR, Color.WHITE),
            isDark = prefs.getBoolean(PREF_IS_DARK, true)
        )
    }
    
    private fun getDefaultTheme() = AmbientTheme(
        primary = DEFAULT_PRIMARY,
        secondary = DEFAULT_SECONDARY,
        tertiary = DEFAULT_TERTIARY,
        textColor = Color.WHITE,
        isDark = true
    )
    
    /**
     * Generate glass color based on ambient theme
     */
    fun getGlassColor(alpha: Int = 26): Int {
        val theme = getSavedTheme()
        return ColorUtils.setAlphaComponent(theme.primary, alpha)
    }
    
    /**
     * Generate complementary accent color
     */
    fun getAccentColor(): Int {
        val theme = getSavedTheme()
        val hsv = FloatArray(3)
        Color.colorToHSV(theme.primary, hsv)
        hsv[0] = (hsv[0] + 180) % 360 // Complementary hue
        return Color.HSVToColor(hsv)
    }
}
