package com.example.launcher.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Scroller
import kotlin.math.abs

/**
 * DesktopPager - Multi-page desktop with smooth scrolling
 */
class DesktopPager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private val scroller = Scroller(context)
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var initialTouchX = 0f
    private var isDragging = false
    private var currentPage = 0
    
    private val pages = mutableListOf<FrameLayout>()
    private var pageWidth = 0
    
    // Page indicators
    var onPageChanged: ((Int) -> Unit)? = null
    
    // Transition effects
    var transitionEffect: TransitionEffect = TransitionEffect.SLIDE
    
    enum class TransitionEffect {
        SLIDE, CUBE, STACK, FADE, ZOOM
    }
    
    companion object {
        private const val SCROLL_THRESHOLD = 100
        private const val FLING_THRESHOLD = 500f
    }
    
    init {
        // Enable clipping for effects
        clipChildren = false
        clipToPadding = false
    }
    
    /**
     * Add a new page
     */
    fun addPage(): FrameLayout {
        val page = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        pages.add(page)
        addView(page)
        requestLayout()
        return page
    }
    
    /**
     * Remove a page
     */
    fun removePage(index: Int) {
        if (index in pages.indices && pages.size > 1) {
            val page = pages.removeAt(index)
            removeView(page)
            
            if (currentPage >= pages.size) {
                currentPage = pages.size - 1
            }
            
            requestLayout()
            scrollToPage(currentPage, false)
        }
    }
    
    /**
     * Get current page index
     */
    fun getCurrentPage(): Int = currentPage
    
    /**
     * Get page count
     */
    fun getPageCount(): Int = pages.size
    
    /**
     * Get page at index
     */
    fun getPage(index: Int): FrameLayout? = pages.getOrNull(index)
    
    /**
     * Scroll to a specific page
     */
    fun scrollToPage(page: Int, animate: Boolean = true) {
        val targetPage = page.coerceIn(0, pages.size - 1)
        val targetX = targetPage * pageWidth
        
        if (animate) {
            val dx = targetX - scrollX
            scroller.startScroll(scrollX, 0, dx, 0, 300)
            invalidate()
        } else {
            scrollTo(targetX, 0)
        }
        
        if (currentPage != targetPage) {
            currentPage = targetPage
            onPageChanged?.invoke(currentPage)
        }
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        
        pageWidth = width
        
        val childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        val childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        
        pages.forEach { page ->
            page.measure(childWidthSpec, childHeightSpec)
        }
        
        setMeasuredDimension(width, height)
    }
    
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var left = 0
        
        pages.forEach { page ->
            page.layout(left, 0, left + pageWidth, b - t)
            left += pageWidth
        }
    }
    
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = ev.x
                lastTouchY = ev.y
                initialTouchX = ev.x
                isDragging = false
                scroller.abortAnimation()
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(ev.x - lastTouchX)
                val dy = abs(ev.y - lastTouchY)
                
                // Start dragging if horizontal movement is significant
                if (dx > dy && dx > 10) {
                    isDragging = true
                    return true
                }
            }
        }
        return false
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                scroller.abortAnimation()
                lastTouchX = event.x
                initialTouchX = event.x
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (lastTouchX - event.x).toInt()
                lastTouchX = event.x
                
                // Ensure we don't scroll past edges
                val newScrollX = scrollX + dx
                val maxScroll = (pages.size - 1) * pageWidth
                
                if (newScrollX in 0..maxScroll) {
                    scrollBy(dx, 0)
                }
                
                // Apply transition effect
                applyTransitionEffect()
                
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val dx = event.x - initialTouchX
                val velocity = dx / (System.currentTimeMillis() - event.downTime) * 1000
                
                val targetPage = when {
                    velocity < -FLING_THRESHOLD -> currentPage + 1
                    velocity > FLING_THRESHOLD -> currentPage - 1
                    dx < -SCROLL_THRESHOLD -> currentPage + 1
                    dx > SCROLL_THRESHOLD -> currentPage - 1
                    else -> currentPage
                }
                
                scrollToPage(targetPage)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.currX, scroller.currY)
            applyTransitionEffect()
            postInvalidateOnAnimation()
        }
    }
    
    private fun applyTransitionEffect() {
        val scrollProgress = scrollX.toFloat() / pageWidth
        
        pages.forEachIndexed { index, page ->
            val pageOffset = index - scrollProgress
            
            when (transitionEffect) {
                TransitionEffect.SLIDE -> {
                    // Default slide, no extra effect
                    page.translationX = 0f
                    page.scaleX = 1f
                    page.scaleY = 1f
                    page.alpha = 1f
                    page.rotationY = 0f
                }
                TransitionEffect.CUBE -> {
                    page.pivotX = if (pageOffset < 0) pageWidth.toFloat() else 0f
                    page.pivotY = page.height / 2f
                    page.rotationY = pageOffset * -45f
                    page.alpha = 1f - abs(pageOffset) * 0.3f
                }
                TransitionEffect.STACK -> {
                    if (pageOffset >= 0) {
                        page.translationX = 0f
                        page.scaleX = 1f - pageOffset * 0.1f
                        page.scaleY = 1f - pageOffset * 0.1f
                        page.alpha = 1f - pageOffset * 0.5f
                    } else {
                        page.translationX = -pageOffset * pageWidth
                        page.scaleX = 1f
                        page.scaleY = 1f
                        page.alpha = 1f
                    }
                }
                TransitionEffect.FADE -> {
                    page.translationX = 0f
                    page.alpha = 1f - abs(pageOffset).coerceAtMost(1f)
                }
                TransitionEffect.ZOOM -> {
                    val scale = 1f - abs(pageOffset) * 0.2f
                    page.scaleX = scale.coerceAtLeast(0.8f)
                    page.scaleY = scale.coerceAtLeast(0.8f)
                    page.alpha = 1f - abs(pageOffset) * 0.4f
                }
            }
        }
    }
    
    /**
     * Set transition effect
     */
    fun setTransition(effect: TransitionEffect) {
        transitionEffect = effect
        applyTransitionEffect()
    }
}
