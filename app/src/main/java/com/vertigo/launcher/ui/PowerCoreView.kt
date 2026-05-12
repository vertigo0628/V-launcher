package com.vertigo.launcher.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

class PowerCoreView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var batteryLevel: Int = 0
    private var isCharging: Boolean = false
    
    // Paints
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 20f
        color = 0x3300F0FF.toInt() // Dim neon cyan
    }
    
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 20f
        strokeCap = Paint.Cap.ROUND
        color = 0xFF00F0FF.toInt() // Neon cyan
        
        // Add glow
        setShadowLayer(20f, 0f, 0f, 0xFF00F0FF.toInt())
    }
    
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x1A00F0FF.toInt() // Inner glow
    }
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 64f
        textAlign = Paint.Align.CENTER
        setShadowLayer(10f, 0f, 0f, 0xFF00F0FF.toInt())
        typeface = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)
    }
    
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xB3FFFFFF.toInt() // 70% white
        textSize = 32f
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
    }
    
    private val rect = RectF()
    private var progressSweep = 0f
    private var rotationAnim = 0f
    
    init {
        // Hardware acceleration needed for shadowLayer
        setLayerType(LAYER_TYPE_HARDWARE, null)
        
        // Ambient rotation animation
        startPulseAnimation()
    }
    
    private fun startPulseAnimation() {
        ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 10000
            repeatCount = ValueAnimator.INFINITE
            interpolator = android.view.animation.LinearInterpolator()
            addUpdateListener { 
                rotationAnim = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
    
    fun setBatteryLevel(level: Int, charging: Boolean) {
        this.batteryLevel = level
        this.isCharging = charging
        
        // Update color based on level
        val color = when {
            level < 20 -> 0xFFFF003C.toInt() // Neon Red
            charging -> 0xFF00FF9D.toInt()   // Neon Green
            else -> 0xFF00F0FF.toInt()       // Neon Cyan
        }
        
        progressPaint.color = color
        progressPaint.setShadowLayer(20f, 0f, 0f, color)
        corePaint.color = (color and 0x00FFFFFF) or 0x1A000000 // Keep alpha low
        textPaint.setShadowLayer(10f, 0f, 0f, color)
        
        // Animate progress
        animateProgress(level.toFloat())
    }
    
    private fun animateProgress(target: Float) {
        ValueAnimator.ofFloat(progressSweep, target).apply {
            duration = 1000
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                progressSweep = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val center = width / 2f
        val radius = (width - 40f) / 2f // Padding for stroke/glow
        
        rect.set(center - radius, center - radius, center + radius, center + radius)
        
        // Draw Core Background
        canvas.drawCircle(center, center, radius - 20f, corePaint)
        
        // Draw Track
        canvas.drawArc(rect, 135f, 270f, false, trackPaint)
        
        // Draw Progress
        // Map 0-100 to 0-270 degrees
        val sweep = (progressSweep / 100f) * 270f
        canvas.drawArc(rect, 135f, sweep, false, progressPaint)
        
        // Draw Text
        canvas.drawText("$batteryLevel%", center, center + 20f, textPaint)
        
        val statusText = if (isCharging) "CHARGING" else "POWER"
        canvas.drawText(statusText, center, center + 70f, labelPaint)
        
        // Draw decorative rotating ring
        if (isCharging) {
            canvas.save()
            canvas.rotate(rotationAnim, center, center)
            val decorPaint = Paint(trackPaint).apply { strokeWidth = 5f; alpha = 100 }
            canvas.drawCircle(center, center, radius + 10f, decorPaint)
            // Dashed ring logic could be added here
            canvas.restore()
        }
    }
}
