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
        
        // Extract colors from wallpaper
        extractWallpaperColors()
        
        // Setup UI
        initializeViews()
        setupGestureDetector()
        setupSearch()
        setupCategoryChips()
        setupFlowerGrid()
        setupAppDrawer()
        
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
        
        // Create and add FlowerGridView programmatically
        flowerGridView = FlowerGridView(this)
        flowerGridContainer.addView(flowerGridView)
        
        // Long press clock for settings
        clockWidget.setOnLongClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        }
    }
    
    private fun extractWallpaperColors() {
        try {
            val wallpaperManager = WallpaperManager.getInstance(this)
            val wallpaperDrawable = wallpaperManager.drawable
            themeEngine.extractColorsFromDrawable(wallpaperDrawable)
        } catch (e: Exception) {
            // Use default colors
        }
    }
    
    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                searchBar.requestFocus()
                return true
            }
            
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 != null && velocityY < -1500 && !isDrawerOpen) {
                    openAppDrawer()
                    return true
                }
                if (e1 != null && velocityY > 1500 && isDrawerOpen) {
                    closeAppDrawer()
                    return true
                }
                return false
            }
        })
        
        val rootLayout = findViewById<View>(R.id.rootLayout)
        rootLayout.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
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
            // Show context menu
        }
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
        appDrawerContainer.animate()
            .translationY(0f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
    
    private fun closeAppDrawer() {
        if (!isDrawerOpen) return
        isDrawerOpen = false
        
        appDrawerContainer.animate()
            .translationY(appDrawerContainer.height.toFloat())
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                appDrawerContainer.visibility = View.GONE
            }
            .start()
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
    
    override fun onDestroy() {
        super.onDestroy()
        clockHandler.removeCallbacksAndMessages(null)
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isDrawerOpen) {
            closeAppDrawer()
        }
        // Don't call super - behave like home
    }
}
