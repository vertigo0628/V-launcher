package com.vertigo.launcher

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.vertigo.launcher.utils.ThemeManager

class LauncherSettingsActivity : AppCompatActivity() {
    
    private lateinit var themeManager: ThemeManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        themeManager = ThemeManager(this)
        
        // Apply theme colors to window
        val bgColor = themeManager.getBackgroundColor()
        window.decorView.setBackgroundColor(bgColor)
        
        // Host the preference fragment
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settingsConfigContainer, SettingsFragment())
                .commit()
        }
        
        // Apply Window Insets to the container
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settingsConfigContainer)) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
