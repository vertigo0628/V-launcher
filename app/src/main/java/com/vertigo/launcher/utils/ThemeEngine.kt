package com.vertigo.launcher.utils

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette

/**
 * ThemeEngine - Extracts colors from wallpaper and generates ambient theme.
 * Supports manual presets and automatic wallpaper-driven color extraction.
 */
class ThemeEngine(private val context: Context) {

    private val prefs = StorageHelper.getSafeSharedPreferences(context, "theme_prefs")

    companion object {
        const val PREF_PRIMARY    = "ambient_primary"
        const val PREF_SECONDARY  = "ambient_secondary"
        const val PREF_TERTIARY   = "ambient_tertiary"
        const val PREF_TEXT_COLOR = "text_color"
        const val PREF_IS_DARK    = "is_dark_wallpaper"
        const val PREF_PRESET     = "theme_preset"

        const val DEFAULT_PRIMARY   = 0xFF6366F1.toInt()
        const val DEFAULT_SECONDARY = 0xFF8B5CF6.toInt()
        const val DEFAULT_TERTIARY  = 0xFFEC4899.toInt()
    }

    // ─── Legacy AmbientTheme (used by VisualSettings etc.) ────────────────────
    data class AmbientTheme(
        val primary:   Int,
        val secondary: Int,
        val tertiary:  Int,
        val textColor: Int,
        val isDark:    Boolean
    )

    // ─── Compose-friendly DynamicThemeColors ─────────────────────────────────
    data class DynamicThemeColors(
        val primary:   ComposeColor,
        val secondary: ComposeColor,
        val accent:    ComposeColor
    )

    // ─── Presets ──────────────────────────────────────────────────────────────
    enum class ThemePreset { NEON_CYAN, NEON_PURPLE, NEON_PINK, AUTO }

    fun getThemePreset(preset: ThemePreset): DynamicThemeColors = when (preset) {
        ThemePreset.NEON_CYAN   -> DynamicThemeColors(ComposeColor(0xFF00F0FF), ComposeColor(0xFF0060FF), ComposeColor(0xFF00F0FF))
        ThemePreset.NEON_PURPLE -> DynamicThemeColors(ComposeColor(0xFFAC00FF), ComposeColor(0xFF6600CC), ComposeColor(0xFFAC00FF))
        ThemePreset.NEON_PINK   -> DynamicThemeColors(ComposeColor(0xFFFF006E), ComposeColor(0xFFCC0055), ComposeColor(0xFFFF006E))
        ThemePreset.AUTO        -> extractWallpaperColors() ?: getThemePreset(ThemePreset.NEON_CYAN)
    }

    /** Extract dominant vibrant color from the current wallpaper via Palette API. */
    fun extractWallpaperColors(): DynamicThemeColors? {
        return try {
            val wm = WallpaperManager.getInstance(context)
            val drawable = wm.drawable ?: return null
            val bitmap = when (drawable) {
                is BitmapDrawable -> drawable.bitmap
                else -> {
                    val bmp = Bitmap.createBitmap(
                        drawable.intrinsicWidth.coerceAtLeast(1),
                        drawable.intrinsicHeight.coerceAtLeast(1),
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = android.graphics.Canvas(bmp)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bmp
                }
            }
            val palette = Palette.from(bitmap).maximumColorCount(8).generate()
            val vibrant = palette.vibrantSwatch?.rgb ?: palette.lightVibrantSwatch?.rgb ?: DEFAULT_PRIMARY
            val muted   = palette.mutedSwatch?.rgb   ?: palette.darkMutedSwatch?.rgb   ?: DEFAULT_SECONDARY
            val accent  = palette.lightVibrantSwatch?.rgb ?: palette.vibrantSwatch?.rgb ?: DEFAULT_TERTIARY
            DynamicThemeColors(ComposeColor(vibrant), ComposeColor(muted), ComposeColor(accent))
        } catch (e: Exception) { null }
    }

    // ─── Drawable / Bitmap helpers ────────────────────────────────────────────
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
        val vibrant    = palette.vibrantSwatch?.rgb    ?: DEFAULT_PRIMARY
        val muted      = palette.mutedSwatch?.rgb      ?: DEFAULT_SECONDARY
        val darkVibrant = palette.darkVibrantSwatch?.rgb ?: DEFAULT_TERTIARY
        val dominant   = palette.dominantSwatch?.rgb   ?: Color.BLACK
        val luminance  = ColorUtils.calculateLuminance(dominant)
        val isDark     = luminance < 0.5
        val textColor  = if (isDark) Color.WHITE else Color.BLACK
        val theme = AmbientTheme(vibrant, muted, darkVibrant, textColor, isDark)
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

    fun getSavedTheme(): AmbientTheme = AmbientTheme(
        primary   = prefs.getInt(PREF_PRIMARY, DEFAULT_PRIMARY),
        secondary = prefs.getInt(PREF_SECONDARY, DEFAULT_SECONDARY),
        tertiary  = prefs.getInt(PREF_TERTIARY, DEFAULT_TERTIARY),
        textColor = prefs.getInt(PREF_TEXT_COLOR, Color.WHITE),
        isDark    = prefs.getBoolean(PREF_IS_DARK, true)
    )

    private fun getDefaultTheme() = AmbientTheme(DEFAULT_PRIMARY, DEFAULT_SECONDARY, DEFAULT_TERTIARY, Color.WHITE, true)

    fun getGlassColor(alpha: Int = 26): Int = ColorUtils.setAlphaComponent(getSavedTheme().primary, alpha)

    fun getAccentColor(): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(getSavedTheme().primary, hsv)
        hsv[0] = (hsv[0] + 180) % 360
        return Color.HSVToColor(hsv)
    }
}
