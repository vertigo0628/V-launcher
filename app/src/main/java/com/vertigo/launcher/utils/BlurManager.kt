package com.vertigo.launcher.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.view.View
import android.widget.ImageView

/**
 * BlurManager - Handles blur effects for Android 12+ (RenderEffect) and fallback for older versions
 */
class BlurManager(private val context: Context) {
    
    companion object {
        const val BLUR_RADIUS_MIN = 1f
        const val BLUR_RADIUS_MAX = 25f
        const val BLUR_RADIUS_DEFAULT = 15f
    }
    
    private var renderScript: RenderScript? = null
    
    init {
        // Initialize RenderScript for older devices
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            try {
                @Suppress("DEPRECATION")
                renderScript = RenderScript.create(context)
            } catch (e: Exception) {
                // RenderScript not available
            }
        }
    }
    
    /**
     * Apply blur to a View (Android 12+ only with RenderEffect)
     */
    fun applyBlurToView(view: View, radius: Float = BLUR_RADIUS_DEFAULT) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blurRadius = radius.coerceIn(BLUR_RADIUS_MIN, BLUR_RADIUS_MAX)
            view.setRenderEffect(
                RenderEffect.createBlurEffect(
                    blurRadius,
                    blurRadius,
                    Shader.TileMode.CLAMP
                )
            )
        }
        // For older versions, blur must be applied to bitmaps manually
    }
    
    /**
     * Remove blur from a View
     */
    fun removeBlurFromView(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.setRenderEffect(null)
        }
    }
    
    /**
     * Apply blur to an ImageView's drawable (works on all API levels)
     */
    fun applyBlurToImageView(imageView: ImageView, radius: Float = BLUR_RADIUS_DEFAULT) {
        val drawable = imageView.drawable ?: return
        val blurredBitmap = blurDrawable(drawable, radius)
        imageView.setImageBitmap(blurredBitmap)
    }
    
    /**
     * Blur a drawable and return as bitmap
     */
    fun blurDrawable(drawable: Drawable, radius: Float = BLUR_RADIUS_DEFAULT): Bitmap {
        val bitmap = drawableToBitmap(drawable)
        return blurBitmap(bitmap, radius)
    }
    
    /**
     * Blur a bitmap using RenderScript (older) or software fallback
     */
    fun blurBitmap(bitmap: Bitmap, radius: Float = BLUR_RADIUS_DEFAULT): Bitmap {
        val blurRadius = radius.coerceIn(BLUR_RADIUS_MIN, BLUR_RADIUS_MAX)
        
        // Create mutable copy
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Use software blur for bitmaps on Android 12+
            return softwareBlur(result, blurRadius)
        }
        
        // Use RenderScript on older devices
        renderScript?.let { rs ->
            return renderScriptBlur(rs, result, blurRadius)
        }
        
        // Fallback to software blur
        return softwareBlur(result, blurRadius)
    }
    
    @Suppress("DEPRECATION")
    private fun renderScriptBlur(rs: RenderScript, bitmap: Bitmap, radius: Float): Bitmap {
        try {
            val input = Allocation.createFromBitmap(rs, bitmap)
            val output = Allocation.createTyped(rs, input.type)
            val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
            
            script.setRadius(radius)
            script.setInput(input)
            script.forEach(output)
            output.copyTo(bitmap)
            
            input.destroy()
            output.destroy()
            script.destroy()
        } catch (e: Exception) {
            // Fallback if RenderScript fails
            return softwareBlur(bitmap, radius)
        }
        
        return bitmap
    }
    
    /**
     * Software-based box blur (for API 31+ or fallback)
     */
    private fun softwareBlur(bitmap: Bitmap, radius: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val r = radius.toInt().coerceIn(1, 25)
        
        // Horizontal pass
        val temp = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sumR = 0
                var sumG = 0
                var sumB = 0
                var sumA = 0
                var count = 0
                
                for (dx in -r..r) {
                    val nx = x + dx
                    if (nx in 0 until width) {
                        val pixel = pixels[y * width + nx]
                        sumA += (pixel shr 24) and 0xFF
                        sumR += (pixel shr 16) and 0xFF
                        sumG += (pixel shr 8) and 0xFF
                        sumB += pixel and 0xFF
                        count++
                    }
                }
                
                temp[y * width + x] = ((sumA / count) shl 24) or
                        ((sumR / count) shl 16) or
                        ((sumG / count) shl 8) or
                        (sumB / count)
            }
        }
        
        // Vertical pass
        for (x in 0 until width) {
            for (y in 0 until height) {
                var sumR = 0
                var sumG = 0
                var sumB = 0
                var sumA = 0
                var count = 0
                
                for (dy in -r..r) {
                    val ny = y + dy
                    if (ny in 0 until height) {
                        val pixel = temp[ny * width + x]
                        sumA += (pixel shr 24) and 0xFF
                        sumR += (pixel shr 16) and 0xFF
                        sumG += (pixel shr 8) and 0xFF
                        sumB += pixel and 0xFF
                        count++
                    }
                }
                
                pixels[y * width + x] = ((sumA / count) shl 24) or
                        ((sumR / count) shl 16) or
                        ((sumG / count) shl 8) or
                        (sumB / count)
            }
        }
        
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
    
    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
    
    fun destroy() {
        @Suppress("DEPRECATION")
        renderScript?.destroy()
        renderScript = null
    }
}
