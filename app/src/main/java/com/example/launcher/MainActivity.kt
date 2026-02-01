package com.example.launcher

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.WallpaperManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.launcher.model.AppCategory
import com.example.launcher.model.AppModel
import com.example.launcher.ui.AppAdapter
import com.example.launcher.ui.FlowerGridView
import com.example.launcher.ui.HomeViewModel
import com.example.launcher.utils.ThemeEngine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var themeEngine: ThemeEngine
    private lateinit var gestureDetector: GestureDetector
    
    // UI Components
    private lateinit var clockWidget: TextView
    private lateinit var dateWidget: TextView
    private lateinit var searchBar: EditText
    private lateinit var flowerGridContainer: FrameLayout
    private lateinit var flowerGridView: FlowerGridView
    private lateinit var appDrawerContainer: FrameLayout
    private lateinit var drawerAppGrid: RecyclerView
    private lateinit var categoryContainer: LinearLayout
    private lateinit var widgetContainer: com.example.launcher.ui.FluidWidgetLayout
    private lateinit var widgetManager: com.example.launcher.utils.WidgetManager
    
    private var isDrawerOpen = false
    private val clockHandler = Handler(Looper.getMainLooper())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make status bar transparent
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        
        setContentView(R.layout.activity_main)
        
        // Initialize
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        themeEngine = ThemeEngine(this)
        widgetManager = com.example.launcher.utils.WidgetManager(this)
        
        // Extract colors from wallpaper
        extractWallpaperColors()
        
        // Setup UI
        initializeViews()
        setupGestureDetector()
        setupSearch()
        setupCategoryChips()
        setupFlowerGrid()
        setupAppDrawer()
        setupWidgets()
        
        // Start clock
        updateClockAndDate()
        startClockUpdates()
        
        // Observe apps
        observeApps()
    }
    
    private fun initializeViews() {
        clockWidget = findViewById(R.id.clockWidget)
        dateWidget = findViewById(R.id.dateWidget)
        searchBar = findViewById(R.id.searchBar)
        flowerGridContainer = findViewById(R.id.flowerGridContainer)
        appDrawerContainer = findViewById(R.id.appDrawerContainer)
        drawerAppGrid = findViewById(R.id.drawerAppGrid)
        categoryContainer = findViewById(R.id.categoryContainer)
        widgetContainer = findViewById(R.id.widgetContainer)
        
        // Create and add FlowerGridView programmatically
        flowerGridView = FlowerGridView(this)
        flowerGridContainer.addView(flowerGridView)
        
        // Long press clock for settings
        clockWidget.setOnLongClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        }
        
        // Long press on widget container to add widget
        widgetContainer.setOnLongClickListener {
            openWidgetPicker()
            true
        }
    }
    
    private fun extractWallpaperColors() {
        try {
            val wallpaperManager = WallpaperManager.getInstance(this)
            val wallpaperDrawable = wallpaperManager.drawable
            val theme = themeEngine.extractColorsFromDrawable(wallpaperDrawable)
            applyTheme(theme)
        } catch (e: Exception) {
            // Use default colors
        }
    }
    
    private fun applyTheme(theme: ThemeEngine.AmbientTheme) {
        // Apply text colors
        clockWidget.setTextColor(theme.textColor)
        dateWidget.setTextColor(theme.textColor)
        searchBar.setHintTextColor(if (theme.isDark) 0x80FFFFFF.toInt() else 0x80000000.toInt())
        searchBar.setTextColor(theme.textColor)
        
        // Tint glass backgrounds
        val glassColor = themeEngine.getGlassColor(30) // 12% alpha
        val glassStroke = if (theme.isDark) 0x33FFFFFF else 0x33000000
        
        // Update search bar background
        val searchBg = searchBar.parent as? View
        searchBg?.background?.setTint(glassColor)
        
        // Update category text colors
        for (i in 0 until categoryContainer.childCount) {
            val chip = categoryContainer.getChildAt(i)
            val nameView = chip.findViewById<TextView>(R.id.categoryName)
            val iconView = chip.findViewById<android.widget.ImageView>(R.id.categoryIcon)
            
            nameView.setTextColor(theme.textColor)
            iconView.setColorFilter(theme.textColor)
        }
        
        // Add pulsing neon glow to clock
        startNeonPulseAnimation()
    }
    
    private fun startNeonPulseAnimation() {
        val pulseAnim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.neon_pulse)
        clockWidget.startAnimation(pulseAnim)
    }
    
    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                searchBar.requestFocus()
                // Haptic feedback
                window.decorView.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                return true
            }
            
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                
                // Lower velocity threshold (was 1500) and added distance check
                val distanceY = e2.y - e1.y
                val velocityThreshold = 800f
                val distanceThreshold = 100f
                
                if (velocityY < -velocityThreshold && distanceY < -distanceThreshold && !isDrawerOpen) {
                    openAppDrawer()
                    return true
                }
                if (velocityY > velocityThreshold && distanceY > distanceThreshold && isDrawerOpen) {
                    closeAppDrawer()
                    return true
                }
                return false
            }
        })
        
        val rootLayout = findViewById<View>(R.id.rootLayout)
        rootLayout.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            // Need to return false to let clicks pass through, but true if handled
            false 
        }
    }
    
    private fun setupSearch() {
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.searchApps(s.toString())
                if (s?.isNotEmpty() == true && !isDrawerOpen) {
                    openAppDrawer()
                }
            }
        })
    }
    
    private fun setupCategoryChips() {
        val categories = listOf(
            null to "All",
            AppCategory.COMMUNICATION to "Chat",
            AppCategory.INTERNET to "Web",
            AppCategory.GAMES to "Games",
            AppCategory.MEDIA to "Media",
            AppCategory.UTILITIES to "Tools",
            AppCategory.SETTINGS to "Settings"
        )
        
        categories.forEach { (category, name) ->
            val chipView = layoutInflater.inflate(R.layout.item_category, categoryContainer, false)
            val nameView = chipView.findViewById<TextView>(R.id.categoryName)
            val iconView = chipView.findViewById<android.widget.ImageView>(R.id.categoryIcon)
            
            nameView.text = name
            iconView.setImageResource(getCategoryIcon(category))
            
            chipView.setOnClickListener {
                viewModel.selectCategory(category)
                if (!isDrawerOpen) openAppDrawer()
            }
            
            categoryContainer.addView(chipView)
        }
    }
    
    private fun getCategoryIcon(category: AppCategory?): Int {
        return when (category) {
            null -> R.drawable.ic_category_home
            AppCategory.COMMUNICATION -> R.drawable.ic_category_communication
            AppCategory.INTERNET -> R.drawable.ic_category_internet
            AppCategory.GAMES -> R.drawable.ic_category_games
            AppCategory.MEDIA -> R.drawable.ic_category_media
            AppCategory.UTILITIES -> R.drawable.ic_category_utilities
            AppCategory.SETTINGS -> R.drawable.ic_category_settings
            else -> R.drawable.ic_category_home
        }
    }
    
    private fun setupFlowerGrid() {
        flowerGridView.setOnAppClickListener { app ->
            launchApp(app)
        }
        
        flowerGridView.setOnAppLongClickListener { app ->
            showAppContextMenu(app)
        }
    }
    
    private fun showAppContextMenu(app: AppModel) {
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle(app.label)
            .setItems(arrayOf("App Info", "Uninstall", "Hide App")) { _, which ->
                when (which) {
                    0 -> { // App Info
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = android.net.Uri.parse("package:${app.packageName}")
                        startActivity(intent)
                    }
                    1 -> { // Uninstall
                        val intent = Intent(Intent.ACTION_DELETE)
                        intent.data = android.net.Uri.parse("package:${app.packageName}")
                        startActivity(intent)
                    }
                    2 -> { // Hide App
                        val prefsManager = com.example.launcher.utils.PreferencesManager(this)
                        prefsManager.hideApp(app.packageName)
                        Toast.makeText(this, "${app.label} hidden", Toast.LENGTH_SHORT).show()
                        viewModel.refreshApps()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }
    
    private fun setupAppDrawer() {
        drawerAppGrid.layoutManager = GridLayoutManager(this, 4)
        
        val adapter = AppAdapter(emptyList()) { app ->
            launchApp(app)
        }
        drawerAppGrid.adapter = adapter
        
        // Pull down to close
        val drawerHandle = findViewById<View>(R.id.drawerHandle)
        drawerHandle.setOnClickListener {
            closeAppDrawer()
        }
    }
    
    private fun observeApps() {
        lifecycleScope.launch {
            viewModel.apps.collect { apps ->
                // Update flower grid with first 7 apps
                flowerGridView.setApps(apps.take(7))
                
                // Update drawer
                (drawerAppGrid.adapter as? AppAdapter)?.updateApps(apps)
            }
        }
    }
    
    private fun openAppDrawer() {
        if (isDrawerOpen) return
        isDrawerOpen = true
        
        appDrawerContainer.visibility = View.VISIBLE
        
        // Spring Animation
        androidx.dynamicanimation.animation.SpringAnimation(
            appDrawerContainer, 
            androidx.dynamicanimation.animation.SpringAnimation.TRANSLATION_Y, 
            0f
        ).apply {
            spring.dampingRatio = androidx.dynamicanimation.animation.SpringForce.DAMPING_RATIO_NO_BOUNCY
            spring.stiffness = androidx.dynamicanimation.animation.SpringForce.STIFFNESS_LOW
            start()
        }
    }
    
    private fun closeAppDrawer() {
        if (!isDrawerOpen) return
        isDrawerOpen = false
        
        // Spring Animation
        androidx.dynamicanimation.animation.SpringAnimation(
            appDrawerContainer, 
            androidx.dynamicanimation.animation.SpringAnimation.TRANSLATION_Y, 
            appDrawerContainer.height.toFloat()
        ).apply {
            spring.dampingRatio = androidx.dynamicanimation.animation.SpringForce.DAMPING_RATIO_NO_BOUNCY
            spring.stiffness = androidx.dynamicanimation.animation.SpringForce.STIFFNESS_LOW
            addEndListener { _, _, _, _ ->
                if (!isDrawerOpen) appDrawerContainer.visibility = View.GONE
            }
            start()
        }
    }
    
    private fun launchApp(app: AppModel) {
        val intent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (intent != null) {
            // Scale down animation would go here
            startActivity(intent)
        }
    }
    
    private fun updateClockAndDate() {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        val now = Date()
        
        clockWidget.text = timeFormat.format(now)
        dateWidget.text = dateFormat.format(now)
    }
    
    private fun startClockUpdates() {
        clockHandler.postDelayed(object : Runnable {
            override fun run() {
                updateClockAndDate()
                clockHandler.postDelayed(this, 60000) // Update every minute
            }
        }, 60000)
    }
    
    
    private fun setupWidgets() {
        widgetManager.startListening()
    }
    
    private fun openWidgetPicker() {
        try {
            val pickIntent = widgetManager.getPickWidgetIntent()
            @Suppress("DEPRECATION")
            startActivityForResult(pickIntent, com.example.launcher.utils.WidgetManager.REQUEST_PICK_APPWIDGET)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open widget picker", Toast.LENGTH_SHORT).show()
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                com.example.launcher.utils.WidgetManager.REQUEST_PICK_APPWIDGET -> {
                    val appWidgetId = data?.getIntExtra(
                        android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,
                        -1
                    ) ?: -1
                    
                    if (appWidgetId != -1) {
                        // Create and add widget
                        val widgetView = widgetManager.createWidgetView(appWidgetId)
                        if (widgetView != null) {
                            // Add to center of screen with default size
                            val x = (widgetContainer.width / 2 - 200).toFloat()
                            val y = (widgetContainer.height / 2 - 150).toFloat()
                            widgetContainer.addWidget(widgetView, x, y, 400f, 300f)
                        }
                    }
                }
            }
        }
    }
    
    override fun onStart() {
        super.onStart()
        widgetManager.startListening()
    }
    
    override fun onStop() {
        super.onStop()
        widgetManager.stopListening()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        clockHandler.removeCallbacksAndMessages(null)
        widgetManager.stopListening()
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isDrawerOpen) {
            closeAppDrawer()
        }
        // Don't call super - behave like home
    }
}
