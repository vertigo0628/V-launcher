package com.example.launcher

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.launcher.model.AppModel
import com.example.launcher.ui.AppAdapter
import com.example.launcher.ui.HomeViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: AppAdapter
    private lateinit var gestureDetector: android.view.GestureDetector
    private lateinit var widgetHost: android.appwidget.AppWidgetHost
    private lateinit var widgetManager: android.appwidget.AppWidgetManager
    
    companion object {
        const val REQUEST_PICK_APPWIDGET = 1001
        const val REQUEST_CREATE_APPWIDGET = 1002
        const val HOST_ID = 1024
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        
        // Setup gesture detector
        gestureDetector = android.view.GestureDetector(this, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: android.view.MotionEvent): Boolean {
                // Double tap to focus search
                val searchBar = findViewById<android.widget.EditText>(R.id.searchBar)
                searchBar.requestFocus()
                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(searchBar, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                return true
            }
            
            override fun onFling(
                e1: android.view.MotionEvent?,
                e2: android.view.MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                // Swipe up gesture (can be expanded for app drawer)
                if (e1 != null && e2.y < e1.y && Math.abs(velocityY) > 1000) {
                    // Placeholder: Could open separate app drawer here
                    return true
                }
                return false
            }
        })
        
        // Apply gesture to main layout
        val mainLayout = findViewById<android.view.View>(R.id.mainLayout)
        mainLayout?.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
        
        // Initialize widget host
        widgetHost = android.appwidget.AppWidgetHost(this, HOST_ID)
        widgetManager = android.appwidget.AppWidgetManager.getInstance(this)
        widgetHost.startListening()
        
        // Long-press on widgets area to add widget
        val widgetContainer = findViewById<android.widget.FrameLayout>(R.id.widgetContainer)
        widgetContainer.setOnLongClickListener {
            pickWidget()
            true
        }

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        val categoryRecyclerView = findViewById<RecyclerView>(R.id.categoryRecyclerView)
        categoryRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        
        categoryRecyclerView.adapter = com.example.launcher.ui.CategoryAdapter { category ->
            viewModel.selectCategory(category)
        }

        val recyclerView = findViewById<RecyclerView>(R.id.appRecyclerView)
        val prefsManager = com.example.launcher.utils.PreferencesManager(this)
        recyclerView.layoutManager = GridLayoutManager(this, prefsManager.getGridSize())
        
        adapter = AppAdapter(emptyList()) { app ->
            launchApp(app)
        }
        recyclerView.adapter = adapter
        
        val searchBar = findViewById<android.widget.EditText>(R.id.searchBar)
        searchBar.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.searchApps(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        lifecycleScope.launch {
            viewModel.apps.collect { apps ->
                adapter.updateApps(apps)
            }
        }
        
        // Initialize and update clock/date widgets
        updateClockAndDate()
        startClockUpdates()
        
        // Apply theme
        applyTheme()
        
        // Add long-press on clock to open settings
        val clockWidget = findViewById<android.widget.TextView>(R.id.clockWidget)
        clockWidget.setOnLongClickListener {
            startActivity(android.content.Intent(this, SettingsActivity::class.java))
            true
        }
    }
    
    private fun applyTheme() {
        val themeManager = com.example.launcher.utils.ThemeManager(this)
        
        val mainLayout = findViewById<android.view.View>(R.id.mainLayout)
        mainLayout.setBackgroundColor(themeManager.getBackgroundColor())
        
        val clockWidget = findViewById<android.widget.TextView>(R.id.clockWidget)
        val dateWidget = findViewById<android.widget.TextView>(R.id.dateWidget)
        
        clockWidget.setTextColor(themeManager.getTextColor())
        dateWidget.setTextColor(themeManager.getSecondaryTextColor())
    }
    
    private fun updateClockAndDate() {
        val clockWidget = findViewById<android.widget.TextView>(R.id.clockWidget)
        val dateWidget = findViewById<android.widget.TextView>(R.id.dateWidget)
        
        val currentTime = java.util.Calendar.getInstance()
        val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val dateFormat = java.text.SimpleDateFormat("EEEE, MMMM d", java.util.Locale.getDefault())
        
        clockWidget.text = timeFormat.format(currentTime.time)
        dateWidget.text = dateFormat.format(currentTime.time)
    }
    
    private fun startClockUpdates() {
        // Update clock every minute
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                updateClockAndDate()
                handler.postDelayed(this, 60000) // Update every 60 seconds
            }
        }
        handler.post(runnable)
    }

    private fun launchApp(app: AppModel) {
        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            Toast.makeText(this, "Cannot launch app", Toast.LENGTH_SHORT).show()
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Do nothing on back press to behave like a launcher home screen
    }
    
    override fun onDestroy() {
        super.onDestroy()
        widgetHost.stopListening()
    }
    
    private fun pickWidget() {
        val appWidgetId = widgetHost.allocateAppWidgetId()
        val pickIntent = Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_PICK)
        pickIntent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET)
    }
    
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_PICK_APPWIDGET -> {
                    configureWidget(data)
                }
                REQUEST_CREATE_APPWIDGET -> {
                    createWidget(data)
                }
            }
        } else if (resultCode == RESULT_CANCELED && data != null) {
            val appWidgetId = data.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            if (appWidgetId != -1) {
                widgetHost.deleteAppWidgetId(appWidgetId)
            }
        }
    }
    
    private fun configureWidget(data: Intent?) {
        val extras = data?.extras
        val appWidgetId = extras?.getInt(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        val appWidgetInfo = widgetManager.getAppWidgetInfo(appWidgetId)
        
        if (appWidgetInfo?.configure != null) {
            val intent = Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
            intent.component = appWidgetInfo.configure
            intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            startActivityForResult(intent, REQUEST_CREATE_APPWIDGET)
        } else {
            createWidget(data)
        }
    }
    
    private fun createWidget(data: Intent?) {
        val extras = data?.extras
        val appWidgetId = extras?.getInt(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        
        if (appWidgetId == -1) return
        
        val appWidgetInfo = widgetManager.getAppWidgetInfo(appWidgetId)
        val hostView = widgetHost.createView(this, appWidgetId, appWidgetInfo)
        hostView.setAppWidget(appWidgetId, appWidgetInfo)
        
        val widgetContainer = findViewById<android.widget.FrameLayout>(R.id.widgetContainer)
        widgetContainer.removeAllViews()
        widgetContainer.addView(hostView)
        
        Toast.makeText(this, "Widget added", Toast.LENGTH_SHORT).show()
    }
}
