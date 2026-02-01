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
    
    // Icon sizes - standardized for evenness
    private val centerIconSize = 72.dpToPx()
    private val primaryIconSize = 64.dpToPx() // Ring 1
    private val secondaryIconSize = 64.dpToPx() // Ring 2 - Same size
    private val tertiaryIconSize = 64.dpToPx() // Ring 3 - Same size
    private val iconScaleFactor = 0.72f // Scale icons to fit circle
    
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
    
    // Edit Mode
    private var isEditMode = false
    
    fun toggleEditMode() {
        isEditMode = !isEditMode
        invalidate()
    }
    
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

    private var globalScale = 1.0f
    
    private fun calculatePositions() {
        iconPositions.clear()
        
        if (apps.isEmpty()) return
        
        // Determine how many rings we are using based on app count
        // Ring 0: 1 app (1 total)
        // Ring 1: +6 apps (7 total)
        // Ring 2: +12 apps (19 total)
        // Ring 3: +18 apps (37 total)
        val maxRing = when {
            apps.size > 19 -> 3
            apps.size > 7 -> 2
            apps.size > 1 -> 1
            else -> 0
        }
        
        // Calculate required radius at scale 1.0
        // usage: Spacing > size for no overlap. 
        // 1.35 gives ~35% gap between centers relative to size, ensuring separation.
        val baseSpacing = primaryIconSize * 1.35f
        
        // The farthest edge is roughly at: (maxRing * spacing) + (iconSize / 2)
        // We add some padding factor just to be safe
        val requiredRadius = (maxRing * baseSpacing) + (primaryIconSize / 2f) + 16.dpToPx()
        
        // Available space
        val availableRadius = min(width / 2f, height / 2f)
        
        // Calculate scale (max 1.0, shrink if needed)
        globalScale = if (availableRadius > 0 && requiredRadius > 0) {
            min(1.0f, availableRadius / requiredRadius)
        } else {
            1.0f
        }
        
        // Apply scale to spacing
        val spacing = baseSpacing * globalScale
        val vSpacing = spacing * 0.866f // sqrt(3)/2
        
        var iter = apps.iterator()
        
        // Center icon (0, 0)
        if (iter.hasNext()) {
            addIconAt(0f, 0f, centerIconSize * globalScale, 0)
            iter.next()
        }
        
        // Generate rings
        var ring = 1
        while (iter.hasNext()) {
            // Start walking the hexagonal perimeter from the top
            for (side in 0 until 6) {
                for (step in 0 until ring) {
                    if (!iter.hasNext()) break
                    
                    // Angle for "side" start and end
                    val angle1Deg = -90.0 + 60 * side
                    val angle2Deg = -90.0 + 60 * (side + 1)
                    
                    val angle1 = Math.toRadians(angle1Deg)
                    val angle2 = Math.toRadians(angle2Deg)
                    
                    // Corner positions
                    val cornerRadius = ring * spacing
                    
                    val x1 = (cornerRadius * cos(angle1)).toFloat()
                    val y1 = (cornerRadius * sin(angle1)).toFloat()
                    
                    val x2 = (cornerRadius * cos(angle2)).toFloat()
                    val y2 = (cornerRadius * sin(angle2)).toFloat()
                    
                    // Interpolate between corners
                    val fraction = step.toFloat() / ring.toFloat()
                    val px = x1 + (x2 - x1) * fraction
                    val py = y1 + (y2 - y1) * fraction
                    
                    // Use larger icons for inner rings
                    val rawSize = if (ring <= 1) primaryIconSize else 
                               if (ring == 2) secondaryIconSize else tertiaryIconSize
                    
                    addIconAt(px, py, rawSize * globalScale, ring)
                    iter.next()
                }
                if (!iter.hasNext()) break
            }
            
            ring++
        }
    }
    
    private fun addIconAt(relX: Float, relY: Float, size: Float, ring: Int) {
        val halfSize = size / 2f
        iconPositions.add(IconPosition(
            RectF(centerX + relX - halfSize, centerY + relY - halfSize, 
                  centerX + relX + halfSize, centerY + relY + halfSize),
            size,
            ring
        ))
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
            
            // Edit Mode Visual Cue: Red pulsing border
            val activeBorderPaint = if (isEditMode) borderPaintSecondary else borderP
            
            canvas.drawCircle(pos.rect.centerX(), pos.rect.centerY(), circleRadius, glowP)
            canvas.drawCircle(pos.rect.centerX(), pos.rect.centerY(), circleRadius, activeBorderPaint)
            
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
