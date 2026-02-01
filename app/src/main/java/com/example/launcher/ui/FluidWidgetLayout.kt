package com.example.launcher.ui

import android.animation.ValueAnimator
import android.appwidget.AppWidgetHostView
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children

/**
 * FluidWidgetLayout - Smart Launcher 6 inspired free-form widget positioning
 * Widgets can be placed anywhere, resized freely without grid constraints
 */
class FluidWidgetLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private val widgets = mutableListOf<WidgetInfo>()
    private val resizeHandlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x80FFFFFF.toInt()
        style = Paint.Style.FILL
    }
    private val resizeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    
    private var draggedWidget: WidgetInfo? = null
    private var resizingWidget: WidgetInfo? = null
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var resizeHandle: ResizeHandle = ResizeHandle.NONE
    
    data class WidgetInfo(
        val view: View,
        var x: Float,
        var y: Float,
        var width: Float,
        var height: Float,
        var isSelected: Boolean = false
    )
    
    enum class ResizeHandle {
        NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
        TOP, BOTTOM, LEFT, RIGHT
    }
    
    fun addWidget(widget: View, x: Float, y: Float, width: Float, height: Float) {
        val info = WidgetInfo(widget, x, y, width, height)
        widgets.add(info)
        addView(widget)
        requestLayout()
    }
    
    fun removeWidget(widget: View) {
        widgets.removeAll { it.view == widget }
        removeView(widget)
        requestLayout()
    }
    
    fun clearSelection() {
        widgets.forEach { it.isSelected = false }
        invalidate()
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        
        // Measure each widget
        widgets.forEach { info ->
            val widgetWidthSpec = MeasureSpec.makeMeasureSpec(info.width.toInt(), MeasureSpec.EXACTLY)
            val widgetHeightSpec = MeasureSpec.makeMeasureSpec(info.height.toInt(), MeasureSpec.EXACTLY)
            info.view.measure(widgetWidthSpec, widgetHeightSpec)
        }
        
        setMeasuredDimension(width, height)
    }
    
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        widgets.forEach { info ->
            info.view.layout(
                info.x.toInt(),
                info.y.toInt(),
                (info.x + info.width).toInt(),
                (info.y + info.height).toInt()
            )
        }
    }
    
    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        
        // Draw resize handles for selected widgets
        widgets.filter { it.isSelected }.forEach { info ->
            drawResizeHandles(canvas, info)
        }
    }
    
    private fun drawResizeHandles(canvas: Canvas, info: WidgetInfo) {
        val handleSize = 24f
        val rect = RectF(info.x, info.y, info.x + info.width, info.y + info.height)
        
        // Draw border
        canvas.drawRect(rect, resizeBorderPaint)
        
        // Draw corner handles
        drawHandle(canvas, rect.left, rect.top, handleSize)
        drawHandle(canvas, rect.right, rect.top, handleSize)
        drawHandle(canvas, rect.left, rect.bottom, handleSize)
        drawHandle(canvas, rect.right, rect.bottom, handleSize)
        
        // Draw edge handles
        drawHandle(canvas, rect.centerX(), rect.top, handleSize)
        drawHandle(canvas, rect.centerX(), rect.bottom, handleSize)
        drawHandle(canvas, rect.left, rect.centerY(), handleSize)
        drawHandle(canvas, rect.right, rect.centerY(), handleSize)
    }
    
    private fun drawHandle(canvas: Canvas, x: Float, y: Float, size: Float) {
        canvas.drawCircle(x, y, size / 2, resizeHandlePaint)
        canvas.drawCircle(x, y, size / 2, resizeBorderPaint)
    }
    
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                val x = ev.x
                val y = ev.y
                
                // Check if touching a resize handle
                widgets.filter { it.isSelected }.forEach { info ->
                    val handle = getResizeHandle(info, x, y)
                    if (handle != ResizeHandle.NONE) {
                        resizingWidget = info
                        resizeHandle = handle
                        lastTouchX = x
                        lastTouchY = y
                        return true
                    }
                }
                
                // Check if touching a widget
                widgets.reversed().forEach { info ->
                    if (x >= info.x && x <= info.x + info.width &&
                        y >= info.y && y <= info.y + info.height) {
                        draggedWidget = info
                        lastTouchX = x
                        lastTouchY = y
                        
                        // Select this widget
                        clearSelection()
                        info.isSelected = true
                        invalidate()
                        return true
                    }
                }
                
                // Clicked outside all widgets - clear selection
                clearSelection()
            }
        }
        return false
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY
                
                resizingWidget?.let { info ->
                    handleResize(info, dx, dy)
                    lastTouchX = event.x
                    lastTouchY = event.y
                    requestLayout()
                    return true
                }
                
                draggedWidget?.let { info ->
                    info.x += dx
                    info.y += dy
                    
                    // Clamp to bounds
                    info.x = info.x.coerceIn(0f, (width - info.width).toFloat())
                    info.y = info.y.coerceIn(0f, (height - info.height).toFloat())
                    
                    lastTouchX = event.x
                    lastTouchY = event.y
                    requestLayout()
                    return true
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                draggedWidget = null
                resizingWidget = null
                resizeHandle = ResizeHandle.NONE
            }
        }
        return true
    }
    
    private fun handleResize(info: WidgetInfo, dx: Float, dy: Float) {
        val minSize = 100f
        
        when (resizeHandle) {
            ResizeHandle.BOTTOM_RIGHT -> {
                info.width = (info.width + dx).coerceAtLeast(minSize)
                info.height = (info.height + dy).coerceAtLeast(minSize)
            }
            ResizeHandle.BOTTOM_LEFT -> {
                val newWidth = info.width - dx
                if (newWidth >= minSize) {
                    info.x += dx
                    info.width = newWidth
                }
                info.height = (info.height + dy).coerceAtLeast(minSize)
            }
            ResizeHandle.TOP_RIGHT -> {
                info.width = (info.width + dx).coerceAtLeast(minSize)
                val newHeight = info.height - dy
                if (newHeight >= minSize) {
                    info.y += dy
                    info.height = newHeight
                }
            }
            ResizeHandle.TOP_LEFT -> {
                val newWidth = info.width - dx
                val newHeight = info.height - dy
                if (newWidth >= minSize) {
                    info.x += dx
                    info.width = newWidth
                }
                if (newHeight >= minSize) {
                    info.y += dy
                    info.height = newHeight
                }
            }
            ResizeHandle.RIGHT -> {
                info.width = (info.width + dx).coerceAtLeast(minSize)
            }
            ResizeHandle.LEFT -> {
                val newWidth = info.width - dx
                if (newWidth >= minSize) {
                    info.x += dx
                    info.width = newWidth
                }
            }
            ResizeHandle.BOTTOM -> {
                info.height = (info.height + dy).coerceAtLeast(minSize)
            }
            ResizeHandle.TOP -> {
                val newHeight = info.height - dy
                if (newHeight >= minSize) {
                    info.y += dy
                    info.height = newHeight
                }
            }
            else -> {}
        }
    }
    
    private fun getResizeHandle(info: WidgetInfo, x: Float, y: Float): ResizeHandle {
        val hitRadius = 40f
        val rect = RectF(info.x, info.y, info.x + info.width, info.y + info.height)
        
        // Check corners first
        if (isNear(x, rect.left, hitRadius) && isNear(y, rect.top, hitRadius)) return ResizeHandle.TOP_LEFT
        if (isNear(x, rect.right, hitRadius) && isNear(y, rect.top, hitRadius)) return ResizeHandle.TOP_RIGHT
        if (isNear(x, rect.left, hitRadius) && isNear(y, rect.bottom, hitRadius)) return ResizeHandle.BOTTOM_LEFT
        if (isNear(x, rect.right, hitRadius) && isNear(y, rect.bottom, hitRadius)) return ResizeHandle.BOTTOM_RIGHT
        
        // Check edges
        if (isNear(y, rect.top, hitRadius) && x >= rect.left && x <= rect.right) return ResizeHandle.TOP
        if (isNear(y, rect.bottom, hitRadius) && x >= rect.left && x <= rect.right) return ResizeHandle.BOTTOM
        if (isNear(x, rect.left, hitRadius) && y >= rect.top && y <= rect.bottom) return ResizeHandle.LEFT
        if (isNear(x, rect.right, hitRadius) && y >= rect.top && y <= rect.bottom) return ResizeHandle.RIGHT
        
        return ResizeHandle.NONE
    }
    
    private fun isNear(value: Float, target: Float, radius: Float): Boolean {
        return Math.abs(value - target) <= radius
    }
}
