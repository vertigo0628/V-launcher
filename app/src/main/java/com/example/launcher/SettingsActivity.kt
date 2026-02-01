package com.example.launcher

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.launcher.utils.ThemeManager

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var themeManager: ThemeManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        themeManager = ThemeManager(this)
        
        // Theme Selection
        val themeSpinner = findViewById<Spinner>(R.id.themeSpinner)
        val themes = arrayOf("Light", "Dark", "AMOLED Black")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, themes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        themeSpinner.adapter = adapter
        
        // Set current theme
        val currentTheme = when (themeManager.getCurrentTheme()) {
            ThemeManager.THEME_LIGHT -> 0
            ThemeManager.THEME_DARK -> 1
            ThemeManager.THEME_AMOLED -> 2
            else -> 0
        }
        themeSpinner.setSelection(currentTheme)
        
        themeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val theme = when (position) {
                    0 -> ThemeManager.THEME_LIGHT
                    1 -> ThemeManager.THEME_DARK
                    2 -> ThemeManager.THEME_AMOLED
                    else -> ThemeManager.THEME_LIGHT
                }
                themeManager.setTheme(theme)
                recreate() // Restart activity to apply theme
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // Grid Size SeekBar
        val gridSizeSeekBar = findViewById<SeekBar>(R.id.gridSizeSeekBar)
        val gridSizeValue = findViewById<TextView>(R.id.gridSizeValue)
        val prefsManager = com.example.launcher.utils.PreferencesManager(this)
        
        gridSizeSeekBar.progress = prefsManager.getGridSize() - 3 // 0-3 maps to 3-6
        gridSizeValue.text = prefsManager.getGridSize().toString()
        
        gridSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val size = progress + 3 // Convert 0-3 to 3-6
                gridSizeValue.text = size.toString()
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val size = (seekBar?.progress ?: 1) + 3
                prefsManager.setGridSize(size)
                Toast.makeText(this@SettingsActivity, "Grid size will apply on restart", Toast.LENGTH_SHORT).show()
            }
        })
        
        // Hidden Apps Management
        val manageHiddenAppsBtn = findViewById<Button>(R.id.manageHiddenAppsBtn)
        manageHiddenAppsBtn.setOnClickListener {
            // Show dialog with list of all apps to hide/unhide
            showHiddenAppsDialog()
        }
        
        // Notification Access Button
        val notificationBtn = findViewById<Button>(R.id.notificationAccessBtn)
        notificationBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        
        // Icon Pack Selector
        val selectIconPackBtn = findViewById<Button>(R.id.selectIconPackBtn)
        selectIconPackBtn.setOnClickListener {
            showIconPackDialog()
        }
        
        // Wallpaper Picker
        val wallpaperPickerBtn = findViewById<Button>(R.id.wallpaperPickerBtn)
        wallpaperPickerBtn.setOnClickListener {
            startActivity(Intent(Intent.ACTION_SET_WALLPAPER))
        }
        
        // Default Launcher Button
        val defaultLauncherBtn = findViewById<Button>(R.id.defaultLauncherBtn)
        defaultLauncherBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        }
        
        // Back Button
        val backBtn = findViewById<Button>(R.id.backBtn)
        backBtn.setOnClickListener {
            finish()
        }
        
        // Apply Window Insets
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settingsMainLayout)) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Apply theme colors
        applyTheme()
    }
    
    private fun applyTheme() {
        val mainLayout = findViewById<LinearLayout>(R.id.settingsMainLayout)
        mainLayout.setBackgroundColor(themeManager.getBackgroundColor())
        
        // Apply text colors
        findViewById<TextView>(R.id.settingsTitle).setTextColor(themeManager.getTextColor())
        findViewById<TextView>(R.id.themeLabel).setTextColor(themeManager.getTextColor())
    }
    
    private fun showHiddenAppsDialog() {
        val prefsManager = com.example.launcher.utils.PreferencesManager(this)
        val packageManager = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        
        val allApps = packageManager.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .map { it.activityInfo.packageName to it.loadLabel(packageManager).toString() }
            .sortedBy { it.second }
        
        val hiddenApps = prefsManager.getHiddenApps()
        val appNames = allApps.map { it.second }.toTypedArray()
        val checkedItems = allApps.map { hiddenApps.contains(it.first) }.toBooleanArray()
        
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Hidden Apps")
        builder.setMultiChoiceItems(appNames, checkedItems) { _, which, isChecked ->
            val packageName = allApps[which].first
            if (isChecked) {
                prefsManager.hideApp(packageName)
            } else {
                prefsManager.unhideApp(packageName)
            }
        }
        builder.setPositiveButton("Done") { dialog, _ ->
            dialog.dismiss()
            // Notify user to refresh launcher
            Toast.makeText(this, "Changes will apply on launcher restart", Toast.LENGTH_LONG).show()
        }
        builder.show()
    }
    
    private fun showIconPackDialog() {
        // Use IconCustomizer which supports more packs (Nova, standard, etc.)
        val iconCustomizer = com.example.launcher.utils.IconCustomizer(this)
        val availablePacks = iconCustomizer.getInstalledIconPacks()
        
        // Add "System Default" option
        val names = mutableListOf("System Default")
        names.addAll(availablePacks.map { it.name })
        
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Select Icon Pack")
        builder.setItems(names.toTypedArray()) { dialog, which ->
            if (which == 0) {
                iconCustomizer.setIconPack(null)
            } else {
                val selectedPack = availablePacks[which - 1]
                iconCustomizer.setIconPack(selectedPack.packageName)
            }
            Toast.makeText(this, "Icon pack updated! Restarting launcher...", Toast.LENGTH_SHORT).show()
            
            // Restart launcher to apply immediately
            // We can't easily restart another activity, but the MainActivity onResume fix handles it
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }
}
