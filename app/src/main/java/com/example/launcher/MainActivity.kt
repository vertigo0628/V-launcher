package com.example.launcher

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.WallpaperManager
import android.content.Intent
import android.content.Context
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
    private lateinit var gestureManager: com.example.launcher.utils.GestureManager
    private lateinit var dockManager: com.example.launcher.utils.DockManager
    
    // Neural Hub
    private lateinit var neuralHubRoot: android.view.ViewGroup
    private lateinit var powerCoreView: com.example.launcher.ui.PowerCoreView
    private lateinit var systemMonitor: com.example.launcher.utils.SystemMonitor
    private lateinit var usageManager: com.example.launcher.utils.SmartUsageManager
    private var isHubOpen = false
    
    // UI Components
    private lateinit var clockWidget: com.example.launcher.ui.CyberClockView
    private lateinit var alarmWidget: TextView
    private lateinit var alarmContainer: View
    private lateinit var searchBar: EditText
    private lateinit var flowerGridContainer: FrameLayout
    private lateinit var flowerGridView: FlowerGridView
    private lateinit var appDrawerContainer: FrameLayout
    private lateinit var drawerAppGrid: RecyclerView
    private lateinit var categoryContainer: LinearLayout
    private lateinit var widgetContainer: com.example.launcher.ui.FluidWidgetLayout
    private lateinit var widgetManager: com.example.launcher.utils.WidgetManager
    private lateinit var dockContainer: LinearLayout
    private lateinit var dockAppsContainer: LinearLayout
    
    private var isDrawerOpen = false
    private val clockHandler = Handler(Looper.getMainLooper())
    
    private lateinit var themeManager: com.example.launcher.utils.ThemeManager
    private val prefsListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "theme", "grid_size", "icon_pack", "amoled_mode" -> {
                recreate()
            }
        }
    }
    
    // Neural Hub Realtime Updates
    private val hubUpdateHandler = Handler(Looper.getMainLooper())
    private val hubUpdateRunnable = object : Runnable {
        override fun run() {
            if (isHubOpen) {
                updateNeuralHubVitals()
                hubUpdateHandler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make status bar transparent
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        
        // Initialize managers
        themeManager = com.example.launcher.utils.ThemeManager(this)
        
        // Register prefs listener
        getSharedPreferences("launcher_prefs", android.content.Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(prefsListener)
        getSharedPreferences("theme_prefs", android.content.Context.MODE_PRIVATE) // Old file used by some toggles
            .registerOnSharedPreferenceChangeListener(prefsListener)

        setContentView(R.layout.activity_main)
        
        // Initialize
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        themeEngine = ThemeEngine(this)
        widgetManager = com.example.launcher.utils.WidgetManager(this)
        gestureManager = com.example.launcher.utils.GestureManager(this)
        dockManager = com.example.launcher.utils.DockManager(this)
        systemMonitor = com.example.launcher.utils.SystemMonitor(this)
        usageManager = com.example.launcher.utils.SmartUsageManager(this)
        
        // Apply Theme Background
        applyTheme()

        // Extract colors from wallpaper
        extractWallpaperColors()
        
        // Setup UI
        initializeViews()
        setupNeuralHub() // Initialize Hub
        continueSetup()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh colors in case wallpaper changed
        extractWallpaperColors()
    }

    private fun applyTheme() {
        // Apply background color from ThemeManager
        // If AMOLED mode via VisualSettings is on, force black
        val prefs = getSharedPreferences("theme_prefs", android.content.Context.MODE_PRIVATE)
        val amoledMode = prefs.getBoolean("amoled_mode", false)
        
        // Use transparent background to show system wallpaper by default
        // Only use solid black if AMOLED mode is explicitly enabled
        val bgColor = if (amoledMode) {
            android.graphics.Color.BLACK
        } else {
            android.graphics.Color.TRANSPARENT
        }
        
        window.decorView.setBackgroundColor(bgColor)
    }

    private fun continueSetup() {
        setupGestureDetector()
        setupSearch()
        setupCategoryChips()
        setupFlowerGrid()
        setupAppDrawer()
        setupWidgets()
        setupDock()
        
        // Start clock
        updateClockAndDate()
        startClockUpdates()
        
        // Observe apps
        observeApps()
    }



    private fun initializeViews() {
        clockWidget = findViewById(R.id.clockWidget)
        alarmWidget = findViewById(R.id.alarmWidget)
        alarmContainer = findViewById(R.id.alarmContainer)
        searchBar = findViewById(R.id.searchBar)
        flowerGridContainer = findViewById(R.id.flowerGridContainer)
        appDrawerContainer = findViewById(R.id.appDrawerContainer)
        drawerAppGrid = findViewById(R.id.drawerAppGrid)
        categoryContainer = findViewById(R.id.categoryContainer)
        widgetContainer = findViewById(R.id.widgetContainer)
        dockContainer = findViewById(R.id.dockContainer)
        dockAppsContainer = findViewById(R.id.dockAppsContainer)
        
        // Setup Dock Shortcuts
        dockContainer.findViewById<View>(R.id.btnDockNeural).setOnClickListener {
            openNeuralHub()
        }
        dockContainer.findViewById<View>(R.id.btnDockSettings).setOnClickListener {
            startActivity(Intent(this, LauncherSettingsActivity::class.java))
        }
        dockContainer.findViewById<View>(R.id.btnDockEdit).setOnClickListener {
            Toast.makeText(this, "Edit Mode Active", Toast.LENGTH_SHORT).show()
            flowerGridView.toggleEditMode()
        }

        // Apply WindowInsets to avoid system bar overlap
        val rootLayout = findViewById<View>(R.id.rootLayout)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            
            // Apply top padding for status bar (Clock container)
            findViewById<View>(R.id.clockContainer).setPadding(
                0, systemBars.top, 0, 0
            )
            
            // Apply bottom margin/padding for navigation bar
            // We apply it to dockContainer's layout params or padding
            val params = dockContainer.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.bottomMargin = systemBars.bottom
            dockContainer.layoutParams = params
            
            // Also update app drawer padding
            drawerAppGrid.setPadding(
                drawerAppGrid.paddingLeft,
                drawerAppGrid.paddingTop,
                drawerAppGrid.paddingRight,
                systemBars.bottom + 16.dpToPx() // 16dp extra for aesthetics
            )
            
            insets
        }
        
        // Create and add FlowerGridView programmatically
        flowerGridView = FlowerGridView(this)
        flowerGridContainer.addView(flowerGridView)
        
        // Long press clock for options
        clockWidget.setOnLongClickListener {
            val popup = android.widget.PopupMenu(this, clockWidget)
            popup.menu.add("Settings")
            popup.menu.add("Wallpaper")
            popup.menu.add("Neural Hub")
            popup.menu.add("Edit Layout")
            
            popup.setOnMenuItemClickListener { item ->
                when (item.title) {
                    "Settings" -> startActivity(Intent(this, LauncherSettingsActivity::class.java))
                    "Wallpaper" -> {
                        try {
                            val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                            startActivity(Intent.createChooser(intent, "Select Wallpaper"))
                        } catch (e: Exception) {
                            Toast.makeText(this, "Wallpaper picker not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "Neural Hub" -> openNeuralHub()
                    "Edit Layout" -> {
                        // Enter edit mode visual cue
                        Toast.makeText(this, "Edit Mode Active", Toast.LENGTH_SHORT).show()
                        flowerGridView.toggleEditMode()
                    }
                }
                true
            }
            popup.show()
            true
        }
        
        // Long press on widget container to add widget
        widgetContainer.setOnLongClickListener {
            openWidgetPicker()
            true
        }
    }
    
    private fun setupNeuralHub() {
        val rootLayout = findViewById<android.widget.FrameLayout>(R.id.rootLayout)
        
        // Inflate Neural Hub layout and add it to root
        val inflater = android.view.LayoutInflater.from(this)
        neuralHubRoot = inflater.inflate(R.layout.layout_neural_hub, rootLayout, false) as android.view.ViewGroup
        
        // Initially hide off-screen (left)
        neuralHubRoot.visibility = View.INVISIBLE // Start invisible to avoid flicker
        rootLayout.addView(neuralHubRoot)
        
        // Wait for layout to measure width
        neuralHubRoot.post {
            neuralHubRoot.translationX = -neuralHubRoot.width.toFloat()
            neuralHubRoot.visibility = View.VISIBLE
        }
        
        // Bind views
        powerCoreView = neuralHubRoot.findViewById(R.id.powerCore)
        
        // Back behavior (tap empty space or drag handle to close)
        neuralHubRoot.setOnClickListener {
             // Close on tap outside logic if needed, but for now full screen overlay
        }
        
        // Drag handle listener (simple click to close for now)
        neuralHubRoot.findViewById<View>(R.id.dragHandle).setOnClickListener {
            closeNeuralHub()
        }
    }
    
    private fun openNeuralHub() {
        if (isHubOpen) return
        isHubOpen = true
        
        // Update data
        updateNeuralHubVitals()
        
        // Slide in Hub
        neuralHubRoot.animate()
            .translationX(0f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()
            
        // Parallax & Fade Home Content
        val homeViews = listOf(flowerGridContainer, dockContainer, searchBar, clockWidget, widgetContainer)
        homeViews.forEach { view ->
            view.animate()
                .translationX(200f) // Parallax shift
                .alpha(0.3f)        // Dim background
                .setDuration(300)
                .start()
        }
        
        // Start Realtime Updates
        hubUpdateHandler.post(hubUpdateRunnable)
    }
    
    private fun closeNeuralHub() {
        if (!isHubOpen) return
        isHubOpen = false
        
        // Stop Realtime Updates
        hubUpdateHandler.removeCallbacks(hubUpdateRunnable)
        
        // Slide out Hub
        neuralHubRoot.animate()
            .translationX(-neuralHubRoot.width.toFloat())
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()
            
        // Restore Home Content
        val homeViews = listOf(flowerGridContainer, dockContainer, searchBar, clockWidget, widgetContainer)
        homeViews.forEach { view ->
            view.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }
    
    private fun updateNeuralHubVitals() {
        // Battery
        val battery = systemMonitor.getBatteryInfo()
        powerCoreView.setBatteryLevel(battery.level, battery.isCharging)
        
        // RAM
        val ram = systemMonitor.getRamInfo()
        val ramProgress = neuralHubRoot.findViewById<android.widget.ProgressBar>(R.id.ramProgress)
        val ramText = neuralHubRoot.findViewById<TextView>(R.id.ramText)
        ramProgress.progress = ram.percentUsed
        ramText.text = "${formatSize(ram.used)} / ${formatSize(ram.total)}"
        
        // Storage
        val storage = systemMonitor.getStorageInfo()
        val storageProgress = neuralHubRoot.findViewById<android.widget.ProgressBar>(R.id.storageProgress)
        val storageText = neuralHubRoot.findViewById<TextView>(R.id.storageText)
        storageProgress.progress = storage.percentUsed
        storageText.text = "${formatSize(storage.total - storage.available)} / ${formatSize(storage.total)}"
        
        // Smart Suggestions
        updateQuickSuggestions()
    }
    
    private fun updateQuickSuggestions() {
        val suggestedPackages = usageManager.getSuggestedApps(5)
        
        // Convert packages to AppModels
        val suggestedApps = suggestedPackages.mapNotNull { pkg ->
            try {
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                val label = packageManager.getApplicationLabel(appInfo).toString()
                val icon = packageManager.getApplicationIcon(appInfo)
                // AppModel(label, packageName, icon, category)
                AppModel(label, pkg, icon, AppCategory.OTHER)
            } catch (e: Exception) {
                null
            }
        }
        
        val suggestionsList = neuralHubRoot.findViewById<RecyclerView>(R.id.suggestionsList)
        if (suggestedApps.isNotEmpty()) {
            suggestionsList.visibility = View.VISIBLE
            neuralHubRoot.findViewById<View>(R.id.suggestionsHeader).visibility = View.VISIBLE
            
            val adapter = AppAdapter(suggestedApps) { app ->
                launchApp(app)
                closeNeuralHub()
            }
            suggestionsList.adapter = adapter
        } else {
            suggestionsList.visibility = View.GONE
            neuralHubRoot.findViewById<View>(R.id.suggestionsHeader).visibility = View.GONE
        }
    }
    
    private fun formatSize(bytes: Long): String {
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return String.format("%.1f GB", gb)
    }
    
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
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
        clockWidget.setColors(theme.textColor, theme.primary, theme.secondary)
        // dateWidget.setTextColor(theme.textColor) // Removed
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
                executeGestureAction(gestureManager.getGestureAction(com.example.launcher.utils.GestureManager.GESTURE_DOUBLE_TAP))
                return true
            }
            
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                
                val distanceY = e2.y - e1.y
                val distanceX = e2.x - e1.x
                val velocityThreshold = 800f
                val distanceThreshold = 100f
                
                // Determine swipe direction
                if (kotlin.math.abs(distanceY) > kotlin.math.abs(distanceX)) {
                    // Vertical swipe
                    if (velocityY < -velocityThreshold && distanceY < -distanceThreshold) {
                        // Swipe up
                        executeGestureAction(gestureManager.getGestureAction(com.example.launcher.utils.GestureManager.GESTURE_SWIPE_UP))
                        return true
                    }
                    if (velocityY > velocityThreshold && distanceY > distanceThreshold) {
                        // Swipe down
                        if (isDrawerOpen) {
                            closeAppDrawer()
                        } else {
                            executeGestureAction(gestureManager.getGestureAction(com.example.launcher.utils.GestureManager.GESTURE_SWIPE_DOWN))
                        }
                        return true
                    }
                } else {
                    // Horizontal swipe
                    if (velocityX < -velocityThreshold && distanceX < -distanceThreshold) {
                        executeGestureAction(gestureManager.getGestureAction(com.example.launcher.utils.GestureManager.GESTURE_SWIPE_LEFT))
                        return true
                    }
                    if (velocityX > velocityThreshold && distanceX > distanceThreshold) {
                        // Swipe Right -> Open Neural Hub (Minus 1 Screen)
                        openNeuralHub()
                        return true
                    }
                }
                return false
            }
            
            override fun onLongPress(e: MotionEvent) {
                window.decorView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                executeGestureAction(gestureManager.getGestureAction(com.example.launcher.utils.GestureManager.GESTURE_LONG_PRESS))
            }
        })
        
        val rootLayout = findViewById<View>(R.id.rootLayout)
        rootLayout.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false 
        }
    }
    
    private fun executeGestureAction(action: String) {
        when (action) {
            com.example.launcher.utils.GestureManager.ACTION_NONE -> { }
            com.example.launcher.utils.GestureManager.ACTION_OPEN_DRAWER -> openAppDrawer()
            com.example.launcher.utils.GestureManager.ACTION_OPEN_SEARCH -> {
                searchBar.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(searchBar, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
            com.example.launcher.utils.GestureManager.ACTION_LOCK_SCREEN -> lockScreen()
            com.example.launcher.utils.GestureManager.ACTION_OPEN_NOTIFICATIONS -> openNotifications()
            com.example.launcher.utils.GestureManager.ACTION_OPEN_QUICK_SETTINGS -> openQuickSettings()
            com.example.launcher.utils.GestureManager.ACTION_OPEN_SETTINGS -> {
                startActivity(Intent(this, LauncherSettingsActivity::class.java))
            }
            com.example.launcher.utils.GestureManager.ACTION_OPEN_HIDDEN_APPS -> showHiddenApps()
            com.example.launcher.utils.GestureManager.ACTION_TOGGLE_WIDGETS -> toggleWidgetVisibility()
            else -> {
                // Check if it's an app launch action
                if (action.startsWith(com.example.launcher.utils.GestureManager.ACTION_OPEN_APP)) {
                    val packageName = action.removePrefix(com.example.launcher.utils.GestureManager.ACTION_OPEN_APP)
                    launchAppByPackage(packageName)
                }
            }
        }
    }
    
    private fun lockScreen() {
        try {
            val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
            devicePolicyManager.lockNow()
        } catch (e: Exception) {
            Toast.makeText(this, "Screen lock requires device admin permission", Toast.LENGTH_SHORT).show()
        }
    }
    
    @Suppress("DEPRECATION")
    private fun openNotifications() {
        try {
            val statusBarService = getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val expand = statusBarManager.getMethod("expandNotificationsPanel")
            expand.invoke(statusBarService)
        } catch (e: Exception) {
            // Fallback
        }
    }
    
    @Suppress("DEPRECATION")
    private fun openQuickSettings() {
        try {
            val statusBarService = getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val expand = statusBarManager.getMethod("expandSettingsPanel")
            expand.invoke(statusBarService)
        } catch (e: Exception) {
            // Fallback
        }
    }
    
    private fun showHiddenApps() {
        val prefsManager = com.example.launcher.utils.PreferencesManager(this)
        
        val pinDialog = com.example.launcher.ui.PinDialog(
            this,
            prefsManager,
            onSuccess = {
                showHiddenAppsDialog(prefsManager)
            }
        )
        pinDialog.show()
    }
    
    private fun showHiddenAppsDialog(prefsManager: com.example.launcher.utils.PreferencesManager) {
        val hiddenApps = prefsManager.getHiddenApps().toList()
        
        if (hiddenApps.isEmpty()) {
            Toast.makeText(this, "No hidden apps", Toast.LENGTH_SHORT).show()
            return
        }
        
        val appNames = hiddenApps.mapNotNull { pkg ->
            try {
                packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(pkg, 0)
                ).toString()
            } catch (e: Exception) {
                null
            }
        }.toTypedArray()
        
        android.app.AlertDialog.Builder(this, R.style.NeonAlertDialog)
            .setTitle("Hidden Apps")
            .setItems(appNames) { _, which ->
                val packageName = hiddenApps[which]
                showHiddenAppOptions(packageName, prefsManager)
            }
            .setNegativeButton("Close", null)
            .show()
    }
    
    private fun showHiddenAppOptions(packageName: String, prefsManager: com.example.launcher.utils.PreferencesManager) {
        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (e: Exception) {
            packageName
        }
        
        android.app.AlertDialog.Builder(this, R.style.NeonAlertDialog)
            .setTitle(appName)
            .setItems(arrayOf("Launch", "Unhide")) { _, which ->
                when (which) {
                    0 -> launchAppByPackage(packageName)
                    1 -> {
                        prefsManager.unhideApp(packageName)
                        viewModel.refreshApps()
                        Toast.makeText(this, "$appName unhidden", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun toggleWidgetVisibility() {
        widgetContainer.visibility = if (widgetContainer.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }
    
    private fun launchAppByPackage(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            usageManager.logAppLaunch(packageName)
            startActivity(intent)
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
    
    private fun setupAppDrawer() {
        drawerAppGrid.layoutManager = GridLayoutManager(this, 4)
        
        val adapter = AppAdapter(emptyList()) { app ->
            launchApp(app)
        }
        
        // Add long press listener to adapter
        adapter.setOnItemLongClickListener { app ->
            showDrawerAppContextMenu(app)
            true
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
            launch {
                viewModel.apps.collect { apps ->
                    // Update drawer
                    (drawerAppGrid.adapter as? AppAdapter)?.updateApps(apps)
                }
            }
            
            launch {
                viewModel.flowerApps.collect { flowerApps ->
                    // Update flower grid
                    flowerGridView.setApps(flowerApps)
                }
            }
        }
    }

    private fun showAppContextMenu(app: AppModel) {
        val dialog = android.app.AlertDialog.Builder(this, R.style.NeonAlertDialog)
            .setTitle(app.label)
            .setItems(arrayOf("Remove from Home", "App Info", "Uninstall", "Hide App")) { _, which ->
                when (which) {
                    0 -> viewModel.removeFromGrid(app) // Remove from Home
                    1 -> { // App Info
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = android.net.Uri.parse("package:${app.packageName}")
                        startActivity(intent)
                    }
                    2 -> { // Uninstall
                        val intent = Intent(Intent.ACTION_DELETE)
                        intent.data = android.net.Uri.parse("package:${app.packageName}")
                        startActivity(intent)
                    }
                    3 -> { // Hide App
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
    
    private fun showDrawerAppContextMenu(app: AppModel) {
        val dialog = android.app.AlertDialog.Builder(this, R.style.NeonAlertDialog)
            .setTitle(app.label)
            .setItems(arrayOf("Add to Home", "App Info", "Uninstall", "Hide App")) { _, which ->
                when (which) {
                    0 -> { // Add to Home
                        viewModel.addToGrid(app)
                        Toast.makeText(this, "Added to Home Screen", Toast.LENGTH_SHORT).show()
                        closeAppDrawer()
                    }
                    1 -> { // App Info
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = android.net.Uri.parse("package:${app.packageName}")
                        startActivity(intent)
                    }
                    2 -> { // Uninstall
                        val intent = Intent(Intent.ACTION_DELETE)
                        intent.data = android.net.Uri.parse("package:${app.packageName}")
                        startActivity(intent)
                    }
                    3 -> { // Hide App
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
            usageManager.logAppLaunch(app.packageName)
            startActivity(intent)
        }
    }
    
    private fun updateClockAndDate() {
        // CyberClockView handles formatting internally
        clockWidget.setTime(System.currentTimeMillis())
        
        // Update next alarm less frequently or check roughly
        // Ideally we shouldn't check alarm manager every second, but for now we'll allow it or debounce
        if (System.currentTimeMillis() % 60000 < 1000) {
            updateNextAlarm()
        }
    }
    
    private fun updateNextAlarm() {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val nextAlarm = alarmManager.nextAlarmClock
            
            if (nextAlarm != null) {
                val alarmTime = Date(nextAlarm.triggerTime)
                val alarmFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                alarmWidget.text = "Alarm · ${alarmFormat.format(alarmTime)}"
                alarmContainer.visibility = View.VISIBLE
            } else {
                alarmContainer.visibility = View.GONE
            }
        } catch (e: Exception) {
            alarmContainer.visibility = View.GONE
        }
    }
    
    private fun startClockUpdates() {
        clockHandler.post(object : Runnable {
            override fun run() {
                updateClockAndDate()
                clockHandler.postDelayed(this, 1000) // Update every second for smooth ring
            }
        })
    }
    
    private fun setupDock() {
        if (!dockManager.isDockEnabled()) {
            dockContainer.visibility = View.GONE
            return
        }
        
        dockContainer.visibility = View.VISIBLE
        populateDock()
    }
    
    private fun populateDock() {
        if (!::dockAppsContainer.isInitialized) return
        
        dockAppsContainer.removeAllViews()
        
        val dockApps = dockManager.getDockApps()
        val showLabels = dockManager.showDockLabels()
        
        // If no dock apps saved, use default frequently used apps
        val appsToShow = if (dockApps.isEmpty()) {
            getDefaultDockApps()
        } else {
            dockApps
        }
        
        for (packageName in appsToShow) {
            val appInfo = try {
                packageManager.getApplicationInfo(packageName, 0)
            } catch (e: Exception) {
                continue
            }
            
            val itemView = layoutInflater.inflate(R.layout.item_dock_app, dockAppsContainer, false)
            val iconView = itemView.findViewById<android.widget.ImageView>(R.id.dockAppIcon)
            val labelView = itemView.findViewById<TextView>(R.id.dockAppLabel)
            
            iconView.setImageDrawable(packageManager.getApplicationIcon(appInfo))
            labelView.text = packageManager.getApplicationLabel(appInfo)
            labelView.visibility = if (showLabels) View.VISIBLE else View.GONE
            
            itemView.setOnClickListener {
                launchAppByPackage(packageName)
            }
            
            itemView.setOnLongClickListener {
                showDockAppContextMenu(packageName, it)
                true
            }
            
            dockAppsContainer.addView(itemView)
        }
    }
    
    private fun getDefaultDockApps(): List<String> {
        // Common default apps
        val defaults = listOf(
            "com.android.dialer",
            "com.google.android.dialer", 
            "com.android.messaging",
            "com.google.android.apps.messaging",
            "com.android.chrome",
            "com.google.android.gm",
            "com.android.camera",
            "com.google.android.camera"
        )
        
        // Filter to only installed apps
        return defaults.filter { pkg ->
            try {
                packageManager.getApplicationInfo(pkg, 0)
                true
            } catch (e: Exception) {
                false
            }
        }.take(dockManager.getDockSize())
    }
    
    private fun showDockAppContextMenu(packageName: String, anchor: View) {
        val popup = android.widget.PopupMenu(this, anchor)
        popup.menu.add("Remove from Dock")
        popup.menu.add("App Info")
        
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Remove from Dock" -> {
                    dockManager.removeFromDock(packageName)
                    populateDock()
                }
                "App Info" -> {
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = android.net.Uri.parse("package:$packageName")
                    startActivity(intent)
                }
            }
            true
        }
        popup.show()
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
        
        // Unregister prefs listener
        getSharedPreferences("launcher_prefs", android.content.Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(prefsListener)
        widgetManager.stopListening()
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        if (isDrawerOpen) {
            closeAppDrawer()
        }
        // Don't call super - behave like home
    }
}
