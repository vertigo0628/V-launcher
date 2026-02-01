package com.example.launcher.ui

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout

/**
 * PopupWidgetDialog - Shows a widget in a popup overlay
 * Tap on app icon to show widget in a floating popup
 */
class PopupWidgetDialog(
    context: Context,
    private val widgetView: View,
    private val anchorX: Int,
    private val anchorY: Int
) : Dialog(context) {

    private lateinit var container: FrameLayout
    
    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setupDialog()
    }
    
    private fun setupDialog() {
        container = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            
            // Neon border background
            setBackgroundResource(android.R.color.transparent)
        }
        
        // Add widget to container
        if (widgetView.parent != null) {
            (widgetView.parent as? ViewGroup)?.removeView(widgetView)
        }
        container.addView(widgetView)
        
        setContentView(container)
        
        // Configure window
        window?.let { w ->
            w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            w.setDimAmount(0.4f)
            
            val params = w.attributes
            params.gravity = Gravity.TOP or Gravity.START
            params.x = anchorX
            params.y = anchorY
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
            w.attributes = params
        }
        
        // Dismiss on outside touch
        setCanceledOnTouchOutside(true)
    }
    
    override fun show() {
        super.show()
        
        // Animate in
        container.scaleX = 0.8f
        container.scaleY = 0.8f
        container.alpha = 0f
        
        container.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(200)
            .setInterpolator(OvershootInterpolator(1.5f))
            .start()
    }
    
    override fun dismiss() {
        // Animate out
        container.animate()
            .scaleX(0.8f)
            .scaleY(0.8f)
            .alpha(0f)
            .setDuration(150)
            .withEndAction { super.dismiss() }
            .start()
    }
    
    companion object {
        /**
         * Show a popup widget anchored to a specific position
         */
        fun show(context: Context, widget: View, anchorX: Int, anchorY: Int): PopupWidgetDialog {
            val dialog = PopupWidgetDialog(context, widget, anchorX, anchorY)
            dialog.show()
            return dialog
        }
    }
}
