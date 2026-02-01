package com.example.launcher.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import com.example.launcher.R
import com.example.launcher.model.AppModel
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * FlowerGridView - Smart Launcher 6 style hexagonal app arrangement
 * Center app with 6 surrounding apps in a flower pattern
 */
class FlowerGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val apps = mutableListOf<AppModel>()
    private val iconSize = 64.dpToPx()
    private val iconPadding = 8.dpToPx()
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = 0x33FFFFFF
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x1AFFFFFF
    }
    
    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f
    private var selectedIndex = -1
    
    private var onAppClickListener: ((AppModel) -> Unit)? = null
    private var onAppLongClickListener: ((AppModel) -> Unit)? = null
    
    private val iconRects = mutableListOf<RectF>()
    
    fun setApps(appList: List<AppModel>) {
        apps.clear()
        apps.addAll(appList.take(7)) // Max 7 apps (1 center + 6 surrounding)
        calculatePositions()
        invalidate()
    }
    
    fun setOnAppClickListener(listener: (AppModel) -> Unit) {
        onAppClickListener = listener
    }
    
    fun setOnAppLongClickListener(listener: (AppModel) -> Unit) {
        onAppLongClickListener = listener
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        radius = min(w, h) / 2f - iconSize - iconPadding
        calculatePositions()
    }
    
    private fun calculatePositions() {
        iconRects.clear()
        
        if (apps.isEmpty()) return
        
        // Center icon
        val halfIcon = iconSize / 2f
        iconRects.add(RectF(
            centerX - halfIcon,
            centerY - halfIcon,
            centerX + halfIcon,
            centerY + halfIcon
        ))
        
        // Surrounding icons (6 positions in hexagonal pattern)
        val surroundingRadius = radius * 0.7f
        for (i in 1 until minOf(apps.size, 7)) {
            val angle = Math.toRadians((60.0 * (i - 1)) - 90) // Start from top
            val x = centerX + (surroundingRadius * cos(angle)).toFloat()
            val y = centerY + (surroundingRadius * sin(angle)).toFloat()
            
            iconRects.add(RectF(
                x - halfIcon,
                y - halfIcon,
                x + halfIcon,
                y + halfIcon
            ))
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        for (i in apps.indices) {
            if (i >= iconRects.size) break
            
            val rect = iconRects[i]
            val app = apps[i]
            
            // Draw glass background circle
            val circleRadius = iconSize / 2f + 8.dpToPx()
            canvas.drawCircle(rect.centerX(), rect.centerY(), circleRadius, glowPaint)
            canvas.drawCircle(rect.centerX(), rect.centerY(), circleRadius, borderPaint)
            
            // Draw icon
            app.icon.setBounds(
                rect.left.toInt(),
                rect.top.toInt(),
                rect.right.toInt(),
                rect.bottom.toInt()
            )
            app.icon.draw(canvas)
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                selectedIndex = findTouchedIcon(event.x, event.y)
                if (selectedIndex >= 0) {
                    animatePress(selectedIndex, true)
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (selectedIndex >= 0 && selectedIndex < apps.size) {
                    animatePress(selectedIndex, false)
                    onAppClickListener?.invoke(apps[selectedIndex])
                }
                selectedIndex = -1
            }
            MotionEvent.ACTION_CANCEL -> {
                if (selectedIndex >= 0) {
                    animatePress(selectedIndex, false)
                }
                selectedIndex = -1
            }
        }
        return super.onTouchEvent(event)
    }
    
    private fun findTouchedIcon(x: Float, y: Float): Int {
        for (i in iconRects.indices) {
            val rect = iconRects[i]
            val expandedRect = RectF(
                rect.left - 16,
                rect.top - 16,
                rect.right + 16,
                rect.bottom + 16
            )
            if (expandedRect.contains(x, y)) {
                return i
            }
        }
        return -1
    }
    
    private fun animatePress(index: Int, pressed: Boolean) {
        val scale = if (pressed) 0.9f else 1f
        animate()
            .scaleX(scale)
            .scaleY(scale)
            .setDuration(100)
            .setInterpolator(OvershootInterpolator())
            .start()
    }
    
    private fun Int.dpToPx(): Float {
        return this * context.resources.displayMetrics.density
    }
}
