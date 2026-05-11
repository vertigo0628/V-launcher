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
    
    private var holidayText: String? = null
    
    // Base colors (will be updated dynamically)
    private var primaryColor = 0xFF00F0FF.toInt()   // Neon Cyan
    private var secondaryColor = 0xFFFF006E.toInt() // Neon Pink
    private var tertiaryColor = 0xFFBD00FF.toInt()  // Neon Purple
    
    // Paints - sizes will be set in onSizeChanged
    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryColor
        textAlign = Paint.Align.CENTER
        style = Paint.Style.FILL
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    
    private val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryColor
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.15f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val holidayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = tertiaryColor
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.1f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    
    // Tech decorations
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = (primaryColor and 0x00FFFFFF) or 0x4D000000
    }
    
    private val secondsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = tertiaryColor
    }
    
    private val bgLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x33FFFFFF.toInt()
    }

    private val calendar = Calendar.getInstance()
    private val rectF = RectF()
    
    // Cached dimensions
    private var viewRadius = 0f
    private var timeTextSize = 0f
    private var dateTextSize = 0f
    private var holidayTextSize = 0f

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        // Calculate sizes based on view dimensions
        val minDimension = w.coerceAtMost(h).toFloat()
        
        // Text sizes proportional to view size
        timeTextSize = minDimension * 0.22f
        dateTextSize = minDimension * 0.06f
        holidayTextSize = minDimension * 0.04f
        
        // Circle radius leaves room for text
        viewRadius = minDimension / 2.5f
        
        // Update paint sizes
        timePaint.textSize = timeTextSize
        timePaint.setShadowLayer(timeTextSize * 0.1f, 0f, 0f, primaryColor)
        
        datePaint.textSize = dateTextSize
        datePaint.setShadowLayer(dateTextSize * 0.2f, 0f, 0f, secondaryColor)

        holidayPaint.textSize = holidayTextSize
        holidayPaint.setShadowLayer(holidayTextSize * 0.3f, 0f, 0f, tertiaryColor)
        
        // Stroke widths proportional to size
        circlePaint.strokeWidth = minDimension * 0.008f
        secondsPaint.strokeWidth = minDimension * 0.015f
        secondsPaint.setShadowLayer(minDimension * 0.03f, 0f, 0f, tertiaryColor)
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

        // Holiday Check
        holidayText = com.example.launcher.logic.HolidayManager.getHoliday(calendar)
        
        // Seconds (0-59)
        seconds = calendar.get(Calendar.SECOND)
        
        invalidate()
    }
    
    // Allow updating colors from theme
    fun setColors(primary: Int, secondary: Int, tertiary: Int) {
        primaryColor = primary
        secondaryColor = secondary
        tertiaryColor = tertiary
        
        timePaint.color = primary
        timePaint.setShadowLayer(timeTextSize * 0.1f, 0f, 0f, primary)
        
        datePaint.color = secondary
        datePaint.setShadowLayer(dateTextSize * 0.2f, 0f, 0f, secondary)

        holidayPaint.color = tertiary
        holidayPaint.setShadowLayer(holidayTextSize * 0.3f, 0f, 0f, tertiary)
        
        secondsPaint.color = tertiary
        secondsPaint.setShadowLayer(viewRadius * 0.08f, 0f, 0f, tertiary)
        
        circlePaint.color = (primary and 0x00FFFFFF) or 0x4D000000
        
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val cx = width / 2f
        val cy = height / 2f
        
        // 1. Draw Tech Circle/Ring
        canvas.drawCircle(cx, cy, viewRadius, circlePaint)
        
        // Seconds Arc
        rectF.set(cx - viewRadius, cy - viewRadius, cx + viewRadius, cy + viewRadius)
        val sweepAngle = seconds * 6f
        canvas.drawArc(rectF, -90f, sweepAngle, false, secondsPaint)
        
        // 2. Decorative ticks (12 hour marks)
        val tickLength = viewRadius * 0.08f
        for (i in 0 until 12) {
            val angle = Math.toRadians(i * 30.0 - 90.0)
            val startX = (cx + (viewRadius - tickLength) * Math.cos(angle)).toFloat()
            val startY = (cy + (viewRadius - tickLength) * Math.sin(angle)).toFloat()
            val endX = (cx + viewRadius * Math.cos(angle)).toFloat()
            val endY = (cy + viewRadius * Math.sin(angle)).toFloat()
            canvas.drawLine(startX, startY, endX, endY, circlePaint)
        }
        
        // 3. Time Text (centered vertically with slight offset up)
        val timeMetrics = timePaint.fontMetrics
        val timeY = cy - (timeMetrics.ascent + timeMetrics.descent) / 2f - dateTextSize * 0.3f
        canvas.drawText(timeText, cx, timeY, timePaint)
        
        // 4. Date Text (below time, inside circle)
        val dateY = timeY + timeTextSize * 0.6f
        canvas.drawText(dateText, cx, dateY, datePaint)

        // Holiday Text (above time if exists)
        holidayText?.let {
            val holidayY = timeY - timeTextSize * 0.7f
            canvas.drawText(it, cx, holidayY, holidayPaint)
        }
        
        // 5. Decorative line under date
        val lineY = dateY + dateTextSize * 0.5f
        val lineWidth = viewRadius * 0.6f
        canvas.drawRect(cx - lineWidth, lineY, cx + lineWidth, lineY + 2f, bgLinePaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Try to be square, respecting constraints
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        
        // Desired minimum for readability
        val minSize = (context.resources.displayMetrics.density * 150).toInt()
        
        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> widthSize.coerceAtMost(heightSize.coerceAtLeast(minSize))
            else -> minSize
        }
        
        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> heightSize.coerceAtMost(widthSize.coerceAtLeast(minSize))
            else -> minSize
        }
        
        // Keep it square-ish
        val size = width.coerceAtMost(height)
        setMeasuredDimension(width, size)
    }
}
