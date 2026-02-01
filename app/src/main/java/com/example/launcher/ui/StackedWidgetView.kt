package com.example.launcher.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.core.view.GestureDetectorCompat

/**
 * StackedWidgetView - Allows stacking multiple widgets in the same space
 * User can swipe left/right to switch between widgets
 */
class StackedWidgetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private val widgets = mutableListOf<View>()
    private var currentIndex = 0
    private var offsetX = 0f
    
    // Neon indicator paint
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00F0FF.toInt() // Neon cyan
    }
    private val indicatorInactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x4000F0FF.toInt() // Dim cyan
    }
    
    private val gestureDetector: GestureDetectorCompat
    
    init {
        gestureDetector = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                
                val dx = e2.x - e1.x
                val velocityThreshold = 500f
                val distanceThreshold = 50f
                
                if (velocityX < -velocityThreshold && dx < -distanceThreshold) {
                    // Swipe left - next widget
                    showNext()
                    return true
                }
                if (velocityX > velocityThreshold && dx > distanceThreshold) {
                    // Swipe right - previous widget
                    showPrevious()
                    return true
                }
                return false
            }
            
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                // Pass click to current widget
                getCurrentWidget()?.performClick()
                return true
            }
        })
    }
    
    fun addStackedWidget(widget: View) {
        widgets.add(widget)
        addView(widget)
        
        // Hide all except current
        updateWidgetVisibility()
        requestLayout()
    }
    
    fun removeStackedWidget(widget: View) {
        val index = widgets.indexOf(widget)
        widgets.remove(widget)
        removeView(widget)
        
        if (currentIndex >= widgets.size && widgets.isNotEmpty()) {
            currentIndex = widgets.size - 1
        }
        updateWidgetVisibility()
        requestLayout()
    }
    
    fun getCurrentWidget(): View? {
        return if (currentIndex in widgets.indices) widgets[currentIndex] else null
    }
    
    fun getWidgetCount(): Int = widgets.size
    
    fun showNext() {
        if (widgets.size <= 1) return
        animateToIndex((currentIndex + 1) % widgets.size)
    }
    
    fun showPrevious() {
        if (widgets.size <= 1) return
        animateToIndex(if (currentIndex > 0) currentIndex - 1 else widgets.size - 1)
    }
    
    private fun animateToIndex(newIndex: Int) {
        val oldWidget = getCurrentWidget()
        currentIndex = newIndex
        val newWidget = getCurrentWidget()
        
        // Fade animation
        oldWidget?.animate()
            ?.alpha(0f)
            ?.setDuration(150)
            ?.withEndAction { oldWidget.visibility = View.GONE }
            ?.start()
        
        newWidget?.let {
            it.visibility = View.VISIBLE
            it.alpha = 0f
            it.animate()
                .alpha(1f)
                .setDuration(150)
                .setInterpolator(OvershootInterpolator(1.2f))
                .start()
        }
        
        // Haptic feedback
        performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
        
        invalidate()
    }
    
    private fun updateWidgetVisibility() {
        widgets.forEachIndexed { index, widget ->
            widget.visibility = if (index == currentIndex) View.VISIBLE else View.GONE
            widget.alpha = if (index == currentIndex) 1f else 0f
        }
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        
        // Measure all widgets to fill the container
        widgets.forEach { widget ->
            val childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
            val childHeightSpec = MeasureSpec.makeMeasureSpec(height - INDICATOR_HEIGHT.toInt(), MeasureSpec.EXACTLY)
            widget.measure(childWidthSpec, childHeightSpec)
        }
        
        setMeasuredDimension(width, height)
    }
    
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        widgets.forEach { widget ->
            widget.layout(0, 0, r - l, b - t - INDICATOR_HEIGHT.toInt())
        }
    }
    
    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        
        // Draw page indicators at bottom
        if (widgets.size > 1) {
            drawPageIndicators(canvas)
        }
    }
    
    private fun drawPageIndicators(canvas: Canvas) {
        val indicatorRadius = 6f
        val indicatorSpacing = 20f
        val totalWidth = widgets.size * indicatorSpacing
        val startX = (width - totalWidth) / 2f + indicatorRadius
        val y = height - INDICATOR_HEIGHT / 2f
        
        widgets.forEachIndexed { index, _ ->
            val x = startX + index * indicatorSpacing
            val paint = if (index == currentIndex) indicatorPaint else indicatorInactivePaint
            canvas.drawCircle(x, y, indicatorRadius, paint)
        }
    }
    
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return widgets.size > 1
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }
    
    companion object {
        private const val INDICATOR_HEIGHT = 24f
    }
}
