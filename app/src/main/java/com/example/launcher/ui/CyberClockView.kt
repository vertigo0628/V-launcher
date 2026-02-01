package com.example.launcher.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.View
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CyberClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var timeText = "12:00"
    private var dateText = "SAT, FEB 1"
    private var seconds = 0
    private var is24Hour = false
    
    // Paints
    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00F0FF.toInt() // Neon Cyan
        textSize = 160f
        textAlign = Paint.Align.CENTER
        style = Paint.Style.FILL
        setShadowLayer(20f, 0f, 0f, 0xFF00F0FF.toInt())
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    
    private val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF006E.toInt() // Neon Pink
        textSize = 42f
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.2f
        setShadowLayer(10f, 0f, 0f, 0xFFFF006E.toInt())
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    
    // Tech decorations
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = 0x4D00F0FF.toInt() // Dim Cyan
    }
    
    private val secondsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
        color = 0xFFBD00FF.toInt() // Neon Purple
        setShadowLayer(15f, 0f, 0f, 0xFFBD00FF.toInt())
    }
    
    private val bgLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x33FFFFFF.toInt()
    }

    private val calendar = Calendar.getInstance()
    private val rectF = RectF()

    init {
        // Force software layer for shadows if needed, but hardware usually works for simple text/shapes
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun setTime(timestamp: Long) {
        calendar.timeInMillis = timestamp
        val date = Date(timestamp)
        
        // Format Time
        is24Hour = DateFormat.is24HourFormat(context)
        val format = if (is24Hour) "HH:mm" else "hh:mm"
        timeText = SimpleDateFormat(format, Locale.getDefault()).format(date)
        
        // Format Date
        dateText = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(date).uppercase()
        
        // Seconds (0-59)
        seconds = calendar.get(Calendar.SECOND)
        
        invalidate()
    }
    
    // Allow updating just color theme
    fun setColors(primary: Int, secondary: Int, tertiary: Int) {
        timePaint.color = primary
        timePaint.setShadowLayer(20f, 0f, 0f, primary)
        
        datePaint.color = secondary
        datePaint.setShadowLayer(10f, 0f, 0f, secondary)
        
        secondsPaint.color = tertiary
        secondsPaint.setShadowLayer(15f, 0f, 0f, tertiary)
        
        circlePaint.color = (primary and 0x00FFFFFF) or 0x4D000000 // Alpha adjusted
        
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val cx = width / 2f
        val cy = height / 2f
        val radius = width.coerceAtMost(height) / 2.2f
        
        // 1. Draw Tech Circle/Ring
        // Rotate ring slightly based on seconds for dynamic feel? No, keep static base, dynamic arc.
        
        // Background Ring
        canvas.drawCircle(cx, cy, radius, circlePaint)
        
        // Seconds Arc
        rectF.set(cx - radius, cy - radius, cx + radius, cy + radius)
        // -90 to start at top. Sweep is seconds * 6 degrees.
        val sweepAngle = seconds * 6f
        canvas.drawArc(rectF, -90f, sweepAngle, false, secondsPaint)
        
        // 2. Decorative ticks
        for (i in 0 until 12) {
            val angle = Math.toRadians(i * 30.0 - 90.0)
            val startX = (cx + (radius - 20) * Math.cos(angle)).toFloat()
            val startY = (cy + (radius - 20) * Math.sin(angle)).toFloat()
            val endX = (cx + radius * Math.cos(angle)).toFloat()
            val endY = (cy + radius * Math.sin(angle)).toFloat()
            canvas.drawLine(startX, startY, endX, endY, circlePaint)
        }
        
        // 3. Time Text
        // Center vertically.
        // Measure text height to center properly
        val timeMetrics = timePaint.fontMetrics
        val timeHeight = timeMetrics.descent - timeMetrics.ascent
        val dateMetrics = datePaint.fontMetrics
        
        // Draw Time roughly in center
        canvas.drawText(timeText, cx, cy + timeHeight / 4, timePaint)
        
        // 4. Date Text (below time)
        canvas.drawText(dateText, cx, cy + timeHeight / 2 + 50f, datePaint)
        
        // 5. Tech line decoration
        canvas.drawRect(cx - 100f, cy + timeHeight / 2 + 70f, cx + 100f, cy + timeHeight / 2 + 74f, bgLinePaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Enforce a good aspect ratio or minimum size
        val desiredW = 600
        val desiredH = 600
        
        val width = resolveSize(desiredW, widthMeasureSpec)
        val height = resolveSize(desiredH, heightMeasureSpec)
        
        setMeasuredDimension(width, height)
    }
}
