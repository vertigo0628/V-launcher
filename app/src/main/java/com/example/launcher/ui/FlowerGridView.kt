package com.example.launcher.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import com.example.launcher.model.AppModel
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * FlowerGridView - Smart Launcher 6 style organic bubble cluster
 * Apps arranged in an organic flower pattern that looks natural, not grid-like
 */
class FlowerGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val apps = mutableListOf<AppModel>()
    
    // Icon sizes - larger for better visibility
    private val centerIconSize = 88.dpToPx()
    private val primaryIconSize = 76.dpToPx()
    private val secondaryIconSize = 66.dpToPx()
    private val tertiaryIconSize = 56.dpToPx()
    
    // Neon theme paints
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = 0xFF00F0FF.toInt() // Neon cyan
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x3300F0FF.toInt() // Lighter neon cyan glow
    }
    private val outerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x1A00F0FF.toInt() // Subtle outer glow
        maskFilter = android.graphics.BlurMaskFilter(20f, android.graphics.BlurMaskFilter.Blur.NORMAL)
    }
    
    // Second ring uses pink accent
    private val borderPaintSecondary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = 0xFFFF006E.toInt() // Neon pink
    }
    private val glowPaintSecondary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x33FF006E.toInt() // Neon pink glow
    }
    
    // Third ring uses purple
    private val borderPaintTertiary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = 0xFFBD00FF.toInt() // Neon purple
    }
    private val glowPaintTertiary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x26BD00FF.toInt() // Neon purple glow
    }
    
    private var centerX = 0f
    private var centerY = 0f
    private var selectedIndex = -1
    
    private var onAppClickListener: ((AppModel) -> Unit)? = null
    private var onAppLongClickListener: ((AppModel) -> Unit)? = null
    
    // Store icon positions and their sizes
    private data class IconPosition(val rect: RectF, val size: Float, val ring: Int)
    private val iconPositions = mutableListOf<IconPosition>()
    
    fun setApps(appList: List<AppModel>) {
        apps.clear()
        apps.addAll(appList.take(37)) // Max 37 apps (1 + 6 + 12 + 18 pattern)
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
        calculatePositions()
    }

    private fun calculatePositions() {
        iconPositions.clear()
        
        if (apps.isEmpty()) return
        
        // Ring 0: Center icon (largest)
        var idx = 0
        if (idx < apps.size) {
            val halfIcon = centerIconSize / 2f
            iconPositions.add(IconPosition(
                RectF(centerX - halfIcon, centerY - halfIcon, centerX + halfIcon, centerY + halfIcon),
                centerIconSize,
                0
            ))
            idx++
        }
        
        // Ring 1: 6 icons around center (primary size)
        val ring1Radius = centerIconSize * 0.55f + primaryIconSize * 0.55f + 8.dpToPx()
        val ring1Offsets = listOf(0f, 2f, -3f, 1f, -2f, 3f) 
        for (i in 0 until 6) {
            if (idx >= apps.size) break
            val angle = Math.toRadians((60.0 * i) - 90 + ring1Offsets[i])
            val x = centerX + (ring1Radius * cos(angle)).toFloat()
            val y = centerY + (ring1Radius * sin(angle)).toFloat()
            val halfIcon = primaryIconSize / 2f
            
            iconPositions.add(IconPosition(
                RectF(x - halfIcon, y - halfIcon, x + halfIcon, y + halfIcon),
                primaryIconSize,
                1
            ))
            idx++
        }
        
        // Ring 2: 12 icons in outer ring (secondary size)
        val ring2Radius = ring1Radius + primaryIconSize * 0.55f + secondaryIconSize * 0.55f + 6.dpToPx()
        for (i in 0 until 12) {
            if (idx >= apps.size) break
            val angle = Math.toRadians((30.0 * i) - 90) // 30 degree intervals
            val x = centerX + (ring2Radius * cos(angle)).toFloat()
            val y = centerY + (ring2Radius * sin(angle)).toFloat()
            val halfIcon = secondaryIconSize / 2f
            
            iconPositions.add(IconPosition(
                RectF(x - halfIcon, y - halfIcon, x + halfIcon, y + halfIcon),
                secondaryIconSize,
                2
            ))
            idx++
        }
        
        // Ring 3: 18 icons in outermost ring (tertiary size)
        val ring3Radius = ring2Radius + secondaryIconSize * 0.55f + tertiaryIconSize * 0.55f + 4.dpToPx()
        for (i in 0 until 18) {
            if (idx >= apps.size) break
            val angle = Math.toRadians((20.0 * i) - 90) // 20 degree intervals (360/18)
            val x = centerX + (ring3Radius * cos(angle)).toFloat()
            val y = centerY + (ring3Radius * sin(angle)).toFloat()
            val halfIcon = tertiaryIconSize / 2f
            
            iconPositions.add(IconPosition(
                RectF(x - halfIcon, y - halfIcon, x + halfIcon, y + halfIcon),
                tertiaryIconSize,
                3
            ))
            idx++
        }
        
        // Ring 4: 24 icons (outermost)
        val ring4Radius = ring3Radius + tertiaryIconSize + 4.dpToPx()
        for (i in 0 until 24) {
            if (idx >= apps.size) break
            val angle = Math.toRadians((15.0 * i) - 90) // 15 degree intervals (360/24)
            val x = centerX + (ring4Radius * cos(angle)).toFloat()
            val y = centerY + (ring4Radius * sin(angle)).toFloat()
            val halfIcon = tertiaryIconSize / 2f
            
            iconPositions.add(IconPosition(
                RectF(x - halfIcon, y - halfIcon, x + halfIcon, y + halfIcon),
                tertiaryIconSize,
                4
            ))
            idx++
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw in reverse order so center is on top
        for (i in iconPositions.indices.reversed()) {
            if (i >= apps.size) continue
            
            val pos = iconPositions[i]
            val app = apps[i]
            
            // Select paints based on ring
            val (borderP, glowP) = when (pos.ring) {
                0 -> Pair(borderPaint, glowPaint)
                1 -> Pair(borderPaint, glowPaint)
                2 -> Pair(borderPaintSecondary, glowPaintSecondary)
                else -> Pair(borderPaintTertiary, glowPaintTertiary)
            }
            
            // Draw outer glow (only for center and ring 1)
            if (pos.ring <= 1) {
                val glowRadius = pos.size / 2f + 12.dpToPx()
                canvas.drawCircle(pos.rect.centerX(), pos.rect.centerY(), glowRadius, outerGlowPaint)
            }
            
            // Draw glass background circle
            val circleRadius = pos.size / 2f + 6.dpToPx()
            canvas.drawCircle(pos.rect.centerX(), pos.rect.centerY(), circleRadius, glowP)
            canvas.drawCircle(pos.rect.centerX(), pos.rect.centerY(), circleRadius, borderP)
            
            // Draw icon scaled down to fit in circle
            val drawSize = pos.size * iconScaleFactor
            val halfDrawSize = drawSize / 2f
            val cx = pos.rect.centerX()
            val cy = pos.rect.centerY()
            
            app.icon.setBounds(
                (cx - halfDrawSize).toInt(),
                (cy - halfDrawSize).toInt(),
                (cx + halfDrawSize).toInt(),
                (cy + halfDrawSize).toInt()
            )
            app.icon.draw(canvas)
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                selectedIndex = findTouchedIcon(event.x, event.y)
                if (selectedIndex >= 0) {
                    performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                    animatePress(selectedIndex, true)
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (selectedIndex >= 0 && selectedIndex < apps.size) {
                    animatePress(selectedIndex, false)
                    if (findTouchedIcon(event.x, event.y) == selectedIndex) {
                        performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                        onAppClickListener?.invoke(apps[selectedIndex])
                    }
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
        // Check from front to back (center first)
        for (i in iconPositions.indices) {
            if (i >= apps.size) continue
            
            val pos = iconPositions[i]
            val touchRadius = pos.size / 2f + 10.dpToPx()
            
            val dx = x - pos.rect.centerX()
            val dy = y - pos.rect.centerY()
            if (dx * dx + dy * dy <= touchRadius * touchRadius) {
                return i
            }
        }
        return -1
    }
    
    private fun animatePress(index: Int, pressed: Boolean) {
        val scale = if (pressed) 0.92f else 1.0f
        animate()
            .scaleX(scale)
            .scaleY(scale)
            .setDuration(150)
            .setInterpolator(OvershootInterpolator(1.5f))
            .start()
    }
    
    private fun Int.dpToPx(): Float {
        return this * context.resources.displayMetrics.density
    }
}
