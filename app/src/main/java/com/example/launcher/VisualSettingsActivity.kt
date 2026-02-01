package com.example.launcher

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.launcher.utils.BlurManager
import com.example.launcher.utils.IconCustomizer

class VisualSettingsActivity : AppCompatActivity() {
    
    private lateinit var iconCustomizer: IconCustomizer
    private lateinit var blurManager: BlurManager
    
    private lateinit var iconPackSpinner: Spinner
    private lateinit var shapeContainer: LinearLayout
    private lateinit var iconSizeSeekBar: SeekBar
    private lateinit var blurSeekBar: SeekBar
    private lateinit var blurValueText: TextView
    private lateinit var showLabelsSwitch: Switch
    private lateinit var forceAdaptiveSwitch: Switch
    private lateinit var amoledSwitch: Switch
    private lateinit var previewContainer: FrameLayout
    
    private lateinit var btnChangeWallpaper: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visual_settings)
        
        iconCustomizer = IconCustomizer(this)
        blurManager = BlurManager(this)
        
        initViews()
        setupWallpaperButton()
        setupIconPackSpinner()
        setupShapeSelector()
        setupSeekBars()
        setupSwitches()
        updatePreview()
    }
    
    private fun initViews() {
        btnChangeWallpaper = findViewById(R.id.btnChangeWallpaper)
        iconPackSpinner = findViewById(R.id.iconPackSpinner)
        shapeContainer = findViewById(R.id.shapeContainer)
        iconSizeSeekBar = findViewById(R.id.iconSizeSeekBar)
        blurSeekBar = findViewById(R.id.blurSeekBar)
        blurValueText = findViewById(R.id.blurValueText)
        showLabelsSwitch = findViewById(R.id.showLabelsSwitch)
        forceAdaptiveSwitch = findViewById(R.id.forceAdaptiveSwitch)
        amoledSwitch = findViewById(R.id.amoledSwitch)
        previewContainer = findViewById(R.id.previewContainer)
    }
    
    private fun setupWallpaperButton() {
        btnChangeWallpaper.setOnClickListener {
            try {
                // Try to launch specific wallpaper chooser
                val intent = android.content.Intent(android.content.Intent.ACTION_SET_WALLPAPER)
                startActivity(android.content.Intent.createChooser(intent, "Select Wallpaper"))
            } catch (e: Exception) {
                // Fallback to generic image picker
                Toast.makeText(this, "Opening gallery...", Toast.LENGTH_SHORT).show()
                val intent = android.content.Intent(android.content.Intent.ACTION_PICK)
                intent.type = "image/*"
                startActivity(intent)
            }
        }
    }
    
    private fun setupIconPackSpinner() {
        val packs = mutableListOf("System Default")
        packs.addAll(iconCustomizer.getInstalledIconPacks().map { it.name })
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, packs)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        iconPackSpinner.adapter = adapter
        
        // Set current selection
        val currentPack = iconCustomizer.getCurrentIconPack()
        if (currentPack != null) {
            val index = iconCustomizer.getInstalledIconPacks().indexOfFirst { it.packageName == currentPack }
            if (index >= 0) iconPackSpinner.setSelection(index + 1)
        }
        
        iconPackSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (position == 0) {
                    iconCustomizer.setIconPack(null)
                } else {
                    val pack = iconCustomizer.getInstalledIconPacks().getOrNull(position - 1)
                    iconCustomizer.setIconPack(pack?.packageName)
                }
                updatePreview()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupShapeSelector() {
        val shapes = iconCustomizer.getAvailableShapes()
        val currentShape = iconCustomizer.getIconShape()
        
        for ((shapeId, shapeName) in shapes) {
            val button = Button(this).apply {
                text = shapeName
                setBackgroundColor(
                    if (shapeId == currentShape) 0xFF00F0FF.toInt() else 0x33FFFFFF
                )
                setTextColor(if (shapeId == currentShape) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
                setPadding(24, 16, 24, 16)
                
                setOnClickListener {
                    iconCustomizer.setIconShape(shapeId)
                    updateShapeButtons(shapeId)
                    updatePreview()
                }
            }
            
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 16, 0)
            shapeContainer.addView(button, params)
        }
    }
    
    private fun updateShapeButtons(selectedShape: String) {
        val shapes = iconCustomizer.getAvailableShapes()
        for (i in 0 until shapeContainer.childCount) {
            val button = shapeContainer.getChildAt(i) as? Button ?: continue
            val shapeId = shapes.getOrNull(i)?.first ?: continue
            
            button.setBackgroundColor(
                if (shapeId == selectedShape) 0xFF00F0FF.toInt() else 0x33FFFFFF
            )
            button.setTextColor(
                if (shapeId == selectedShape) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            )
        }
    }
    
    private fun setupSeekBars() {
        // Icon size (0=Tiny, 1=Small, 2=Normal, 3=Large, 4=Huge)
        val sizeMultipliers = listOf(
            IconCustomizer.SIZE_TINY,
            IconCustomizer.SIZE_SMALL,
            IconCustomizer.SIZE_NORMAL,
            IconCustomizer.SIZE_LARGE,
            IconCustomizer.SIZE_HUGE
        )
        
        val currentMultiplier = iconCustomizer.getIconSizeMultiplier()
        val currentSizeIndex = sizeMultipliers.indexOfFirst { it == currentMultiplier }.takeIf { it >= 0 } ?: 2
        iconSizeSeekBar.progress = currentSizeIndex
        
        iconSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    iconCustomizer.setIconSizeMultiplier(sizeMultipliers[progress])
                    updatePreview()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Blur intensity
        blurSeekBar.progress = 15
        blurSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                blurValueText.text = progress.toString()
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    
    private fun setupSwitches() {
        showLabelsSwitch.isChecked = iconCustomizer.shouldShowLabels()
        showLabelsSwitch.setOnCheckedChangeListener { _, isChecked ->
            iconCustomizer.setShowLabels(isChecked)
        }
        
        forceAdaptiveSwitch.isChecked = iconCustomizer.shouldForceAdaptive()
        forceAdaptiveSwitch.setOnCheckedChangeListener { _, isChecked ->
            iconCustomizer.setForceAdaptive(isChecked)
            updatePreview()
        }
        
        // AMOLED mode - toggle pure black bg
        amoledSwitch.setOnCheckedChangeListener { _, isChecked ->
            val prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
            prefs.edit().putBoolean("amoled_mode", isChecked).apply()
        }
    }
    
    private fun updatePreview() {
        previewContainer.removeAllViews()
        
        // Show sample app icons with current settings
        val samplePackages = listOf(
            "com.android.settings",
            "com.android.chrome", 
            "com.google.android.gm"
        )
        
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
        }
        
        val iconSize = iconCustomizer.getScaledIconSize(48)
        
        for (pkg in samplePackages) {
            try {
                var icon = packageManager.getApplicationIcon(pkg)
                
                // Apply shape
                val shapedBitmap = iconCustomizer.applyShape(icon, iconSize)
                
                val imageView = ImageView(this).apply {
                    setImageBitmap(shapedBitmap)
                    layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                        setMargins(16, 16, 16, 16)
                    }
                }
                
                row.addView(imageView)
            } catch (e: Exception) {
                // Skip if app not found
            }
        }
        
        previewContainer.addView(row)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        blurManager.destroy()
    }
}
