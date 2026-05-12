package com.vertigo.launcher.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import org.xmlpull.v1.XmlPullParser

/**
 * IconCustomizer - Handles icon packs, sizing, shapes, and adaptive icons
 */
class IconCustomizer(private val context: Context) {
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("icon_prefs", Context.MODE_PRIVATE)
    
    private var iconPackResources: Resources? = null
    private var iconPackPackage: String? = null
    private val iconMappings = mutableMapOf<String, String>() // Component -> Icon name
    
    companion object {
        // Icon shapes
        const val SHAPE_CIRCLE = "circle"
        const val SHAPE_SQUARE = "square"
        const val SHAPE_ROUNDED_SQUARE = "rounded_square"
        const val SHAPE_SQUIRCLE = "squircle"
        const val SHAPE_TEARDROP = "teardrop"
        const val SHAPE_HEXAGON = "hexagon"
        
        // Prefs keys
        private const val PREF_ICON_PACK = "icon_pack"
        private const val PREF_ICON_SHAPE = "icon_shape"
        private const val PREF_ICON_SIZE = "icon_size"
        private const val PREF_SHOW_LABELS = "show_labels"
        private const val PREF_LABEL_SIZE = "label_size"
        private const val PREF_FORCE_ADAPTIVE = "force_adaptive"
        
        // Size multipliers
        const val SIZE_TINY = 0.6f
        const val SIZE_SMALL = 0.8f
        const val SIZE_NORMAL = 1.0f
        const val SIZE_LARGE = 1.2f
        const val SIZE_HUGE = 1.4f
    }
    
    // ===== Icon Pack Management =====
    
    fun getInstalledIconPacks(): List<IconPackInfo> {
        val packs = mutableListOf<IconPackInfo>()
        val pm = context.packageManager
        
        // Common icon pack intents
        val intents = listOf(
            Intent("org.adw.launcher.THEMES"),
            Intent("com.gau.go.launcherex.theme"),
            Intent("com.novalauncher.THEME"),
            Intent("com.teslacoilsw.launcher.THEME"),
            Intent("com.anddoes.launcher.THEME"),
            Intent("org.adw.launcher.icons.ACTION_PICK_ICON")
        )
        
        val foundPackages = mutableSetOf<String>()
        
        for (intent in intents) {
            val activities = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            for (resolveInfo in activities) {
                val packageName = resolveInfo.activityInfo.packageName
                if (packageName !in foundPackages) {
                    foundPackages.add(packageName)
                    try {
                        val appInfo = pm.getApplicationInfo(packageName, 0)
                        val name = pm.getApplicationLabel(appInfo).toString()
                        val icon = pm.getApplicationIcon(packageName)
                        packs.add(IconPackInfo(packageName, name, icon))
                    } catch (e: Exception) {
                        // Skip invalid packs
                    }
                }
            }
        }
        
        return packs
    }
    
    data class IconPackInfo(
        val packageName: String,
        val name: String,
        val icon: Drawable
    )
    
    fun setIconPack(packageName: String?) {
        prefs.edit().putString(PREF_ICON_PACK, packageName).apply()
        loadIconPack(packageName)
    }
    
    fun getCurrentIconPack(): String? {
        return prefs.getString(PREF_ICON_PACK, null)
    }
    
    private fun loadIconPack(packageName: String?) {
        iconMappings.clear()
        iconPackResources = null
        iconPackPackage = null
        
        if (packageName == null) return
        
        try {
            val pm = context.packageManager
            iconPackResources = pm.getResourcesForApplication(packageName)
            iconPackPackage = packageName
            
            // Parse appfilter.xml to get icon mappings
            parseAppFilter(packageName)
        } catch (e: Exception) {
            // Failed to load icon pack
        }
    }
    
    private fun parseAppFilter(packageName: String) {
        try {
            val res = iconPackResources ?: return
            val resId = res.getIdentifier("appfilter", "xml", packageName)
            if (resId == 0) return
            
            val parser = res.getXml(resId)
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")
                    if (component != null && drawable != null) {
                        // Parse ComponentInfo{pkg/class}
                        val match = Regex("ComponentInfo\\{(.+)/(.+)\\}").find(component)
                        if (match != null) {
                            val key = "${match.groupValues[1]}/${match.groupValues[2]}"
                            iconMappings[key] = drawable
                        }
                    }
                }
                parser.next()
            }
        } catch (e: Exception) {
            // Failed to parse appfilter
        }
    }
    
    fun getIconForPackage(packageName: String, activityName: String?): Drawable? {
        val res = iconPackResources ?: return null
        val pkg = iconPackPackage ?: return null
        
        // Try exact match first
        val key = "$packageName/${activityName ?: ""}"
        val iconName = iconMappings[key] ?: iconMappings[packageName]
        
        if (iconName != null) {
            try {
                val resId = res.getIdentifier(iconName, "drawable", pkg)
                if (resId != 0) {
                    return res.getDrawable(resId, null)
                }
            } catch (e: Exception) {
                // Icon not found
            }
        }
        
        return null
    }
    
    // ===== Icon Shape =====
    
    fun setIconShape(shape: String) {
        prefs.edit().putString(PREF_ICON_SHAPE, shape).apply()
    }
    
    fun getIconShape(): String {
        return prefs.getString(PREF_ICON_SHAPE, SHAPE_CIRCLE) ?: SHAPE_CIRCLE
    }
    
    fun applyShape(icon: Drawable, size: Int): Bitmap {
        val bitmap = drawableToBitmap(icon, size)
        return applyShapeToBitmap(bitmap, getIconShape())
    }
    
    fun applyShapeToBitmap(source: Bitmap, shape: String): Bitmap {
        val size = source.width.coerceAtLeast(source.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val path = getShapePath(shape, size)
        
        canvas.drawPath(path, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        
        val left = (size - source.width) / 2f
        val top = (size - source.height) / 2f
        canvas.drawBitmap(source, left, top, paint)
        
        return output
    }
    
    private fun getShapePath(shape: String, size: Int): Path {
        val path = Path()
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        
        when (shape) {
            SHAPE_CIRCLE -> {
                path.addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW)
            }
            SHAPE_SQUARE -> {
                path.addRect(rect, Path.Direction.CW)
            }
            SHAPE_ROUNDED_SQUARE -> {
                val radius = size * 0.2f
                path.addRoundRect(rect, radius, radius, Path.Direction.CW)
            }
            SHAPE_SQUIRCLE -> {
                // Superellipse approximation
                val radius = size * 0.35f
                path.addRoundRect(rect, radius, radius, Path.Direction.CW)
            }
            SHAPE_TEARDROP -> {
                val radius = size * 0.4f
                path.addRoundRect(
                    rect,
                    floatArrayOf(radius, radius, radius, radius, radius, radius, 0f, 0f),
                    Path.Direction.CW
                )
            }
            SHAPE_HEXAGON -> {
                val cx = size / 2f
                val cy = size / 2f
                val r = size / 2f
                path.moveTo(cx + r, cy)
                for (i in 1..6) {
                    val angle = Math.toRadians((60.0 * i) - 30)
                    path.lineTo(
                        (cx + r * kotlin.math.cos(angle)).toFloat(),
                        (cy + r * kotlin.math.sin(angle)).toFloat()
                    )
                }
                path.close()
            }
            else -> {
                path.addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW)
            }
        }
        
        return path
    }
    
    // ===== Icon Sizing =====
    
    fun setIconSizeMultiplier(multiplier: Float) {
        prefs.edit().putFloat(PREF_ICON_SIZE, multiplier.coerceIn(SIZE_TINY, SIZE_HUGE)).apply()
    }
    
    fun getIconSizeMultiplier(): Float {
        return prefs.getFloat(PREF_ICON_SIZE, SIZE_NORMAL)
    }
    
    fun getScaledIconSize(baseSize: Int): Int {
        return (baseSize * getIconSizeMultiplier()).toInt()
    }
    
    // ===== Label Settings =====
    
    fun setShowLabels(show: Boolean) {
        prefs.edit().putBoolean(PREF_SHOW_LABELS, show).apply()
    }
    
    fun shouldShowLabels(): Boolean {
        return prefs.getBoolean(PREF_SHOW_LABELS, true)
    }
    
    fun setLabelSize(size: Float) {
        prefs.edit().putFloat(PREF_LABEL_SIZE, size.coerceIn(8f, 16f)).apply()
    }
    
    fun getLabelSize(): Float {
        return prefs.getFloat(PREF_LABEL_SIZE, 12f)
    }
    
    // ===== Adaptive Icons =====
    
    fun setForceAdaptive(force: Boolean) {
        prefs.edit().putBoolean(PREF_FORCE_ADAPTIVE, force).apply()
    }
    
    fun shouldForceAdaptive(): Boolean {
        return prefs.getBoolean(PREF_FORCE_ADAPTIVE, false)
    }
    
    fun createAdaptiveIcon(icon: Drawable, backgroundColor: Int = 0xFFFFFFFF.toInt()): Drawable {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && icon is AdaptiveIconDrawable) {
            return icon // Already adaptive
        }
        
        // Create fake adaptive icon with background
        val size = 108 // Standard adaptive icon size
        val foregroundSize = 72
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Draw background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        bgPaint.color = backgroundColor
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint)
        
        // Draw foreground icon centered
        val left = (size - foregroundSize) / 2
        val top = (size - foregroundSize) / 2
        icon.setBounds(left, top, left + foregroundSize, top + foregroundSize)
        icon.draw(canvas)
        
        return BitmapDrawable(context.resources, bitmap)
    }
    
    private fun drawableToBitmap(drawable: Drawable, size: Int): Bitmap {
        if (drawable is BitmapDrawable) {
            return Bitmap.createScaledBitmap(drawable.bitmap, size, size, true)
        }
        
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bitmap
    }
    
    fun getAvailableShapes(): List<Pair<String, String>> {
        return listOf(
            SHAPE_CIRCLE to "Circle",
            SHAPE_SQUARE to "Square", 
            SHAPE_ROUNDED_SQUARE to "Rounded Square",
            SHAPE_SQUIRCLE to "Squircle",
            SHAPE_TEARDROP to "Teardrop",
            SHAPE_HEXAGON to "Hexagon"
        )
    }
}
