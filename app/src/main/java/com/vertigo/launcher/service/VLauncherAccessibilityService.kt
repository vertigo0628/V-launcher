package com.vertigo.launcher.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.abs

class VLauncherAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var bottomZone: View? = null
    private var leftZone: View? = null
    private var rightZone: View? = null
    
    private var floatingOverlay: FloatingTerminalOverlay? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        setupZones()
        refreshFloatingButton()
    }

    private fun setupZones() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // --- Bottom Zone (Home/Recents) ---
        bottomZone = createZoneView()
        val bottomParams = createParams(WindowManager.LayoutParams.MATCH_PARENT, 15, Gravity.BOTTOM)
        setupBottomGestures(bottomZone!!)
        
        // --- Left Zone (Back) ---
        leftZone = createZoneView()
        val leftParams = createParams(15, WindowManager.LayoutParams.MATCH_PARENT, Gravity.START)
        setupSideGestures(leftZone!!, isLeft = true)
        
        // --- Right Zone (Back) ---
        rightZone = createZoneView()
        val rightParams = createParams(15, WindowManager.LayoutParams.MATCH_PARENT, Gravity.END)
        setupSideGestures(rightZone!!, isLeft = false)

        try {
            windowManager?.addView(bottomZone, bottomParams)
            windowManager?.addView(leftZone, leftParams)
            windowManager?.addView(rightZone, rightParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createZoneView() = View(this).apply {
        setBackgroundColor(0x01000000)
    }

    private fun createParams(w: Int, h: Int, gravity: Int) = WindowManager.LayoutParams(
        w, h,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply {
        this.gravity = gravity
    }

    private fun setupBottomGestures(view: View) {
        var startY = 0f
        var isHolding = false
        val holdRunnable = Runnable {
            isHolding = true
            performRecents()
        }

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    isHolding = false
                    v.postDelayed(holdRunnable, 350)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.removeCallbacks(holdRunnable)
                    if (!isHolding && (startY - event.rawY) > 30) performHome()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.removeCallbacks(holdRunnable)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupSideGestures(view: View, isLeft: Boolean) {
        var startX = 0f
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val deltaX = if (isLeft) event.rawX - startX else startX - event.rawX
                    if (deltaX > 40) performBack()
                    true
                }
                else -> false
            }
        }
    }

    private fun internalRefreshFloatingButton() {
        val prefs = com.vertigo.launcher.utils.StorageHelper.getSafeDefaultSharedPreferences(this)
        val enabled = prefs.getBoolean("floating_assistant_enabled", false)
        
        if (enabled) {
            if (floatingOverlay == null) {
                windowManager?.let { wm ->
                    floatingOverlay = FloatingTerminalOverlay(this, wm)
                }
            }
            if (floatingOverlay?.isVisible() == false) {
                floatingOverlay?.show()
            }
        } else {
            floatingOverlay?.hide()
            floatingOverlay = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bottomZone?.let { windowManager?.removeView(it) }
        leftZone?.let { windowManager?.removeView(it) }
        rightZone?.let { windowManager?.removeView(it) }
        floatingOverlay?.hide()
        floatingOverlay = null
        instance = null
    }

    override fun onUnbind(intent: Intent?): Boolean {
        floatingOverlay?.hide()
        floatingOverlay = null
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used for gestures
    }

    override fun onInterrupt() {
        // Not used
    }

    companion object {
        private var instance: VLauncherAccessibilityService? = null

        fun performHome() {
            instance?.performGlobalAction(GLOBAL_ACTION_HOME)
        }

        fun performRecents() {
            instance?.performGlobalAction(GLOBAL_ACTION_RECENTS)
        }

        fun performBack() {
            instance?.performGlobalAction(GLOBAL_ACTION_BACK)
        }
        
        fun refreshFloatingButton() {
            instance?.internalRefreshFloatingButton()
        }
        
        fun isEnabled(): Boolean = instance != null
    }
}
