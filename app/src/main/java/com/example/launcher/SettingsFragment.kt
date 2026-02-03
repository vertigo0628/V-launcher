package com.example.launcher

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.ListPreference
import com.example.launcher.utils.ThemeManager
import com.example.launcher.utils.IconCustomizer
import com.example.launcher.utils.PreferencesManager

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Use default SharedPreferences (matches HomeViewModel's PreferenceManager.getDefaultSharedPreferences)
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        
        setupThemePreference()
        setupGridSizePreference()
        setupWallpaperPreference()
        setupIconPackPreference()
        setupHiddenAppsPreference()
        setupSystemPreferences()
    }
    
    private fun setupThemePreference() {
        findPreference<ListPreference>("theme")?.setOnPreferenceChangeListener { _, newValue ->
            // ThemeManager.setTheme(newValue as String) - automatic updates via SharedPreferences listener in MainActivity
            // But we might need to recreate this activity too
            activity?.recreate()
            true
        }
    }
    
    private fun setupGridSizePreference() {
        findPreference<ListPreference>("grid_size")?.setOnPreferenceChangeListener { _, newValue ->
            val size = (newValue as String).toInt()
            // PreferencesManager writes to the same shared prefs file "launcher_prefs"?
            // We need to ensure consistency. 
            // Our PreferenceManager uses "launcher_prefs", so standard PreferenceFragment will write to it correctly 
            // if we set sharedPreferencesName = "launcher_prefs" which we did.
            
            Toast.makeText(context, "Grid size updated", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private val wallpaperLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            showWallpaperTargetDialog(uri)
        }
    }
    
    private fun showWallpaperTargetDialog(uri: android.net.Uri) {
        val options = arrayOf("Home Screen", "Lock Screen", "Home & Lock Screens")
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Set Wallpaper")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> setWallpaper(uri, android.app.WallpaperManager.FLAG_SYSTEM)
                    1 -> setWallpaper(uri, android.app.WallpaperManager.FLAG_LOCK)
                    2 -> setWallpaper(uri, android.app.WallpaperManager.FLAG_SYSTEM or android.app.WallpaperManager.FLAG_LOCK)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setWallpaper(uri: android.net.Uri, flags: Int) {
        try {
            val context = requireContext()
            val inputStream = context.contentResolver.openInputStream(uri)
            // Use API 24+ method to set with flags
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.app.WallpaperManager.getInstance(context).setStream(inputStream, null, true, flags)
            } else {
                // Fallback for older devices (sets both usually)
                android.app.WallpaperManager.getInstance(context).setStream(inputStream)
            }
            
            val message = when (flags) {
                android.app.WallpaperManager.FLAG_SYSTEM -> "Home screen wallpaper updated"
                android.app.WallpaperManager.FLAG_LOCK -> "Lock screen wallpaper updated"
                else -> "Wallpaper updated"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to set wallpaper: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private val storagePermissionLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openGalleryPicker()
        } else {
            Toast.makeText(requireContext(), "Permission denied to read storage", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkStoragePermissionAndOpenGallery() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            openGalleryPicker()
        } else {
            storagePermissionLauncher.launch(permission)
        }
    }

    private fun openGalleryPicker() {
        wallpaperLauncher.launch("image/*")
    }

    private fun setupWallpaperPreference() {
        findPreference<Preference>("wallpaper_picker")?.setOnPreferenceClickListener {
            val options = arrayOf("System Wallpapers", "Pick from Gallery/Files")
            
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Change Wallpaper")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            try {
                                val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                                startActivity(Intent.createChooser(intent, "Select Wallpaper"))
                            } catch (e: Exception) {
                                Toast.makeText(requireContext(), "System wallpaper picker not found", Toast.LENGTH_SHORT).show()
                            }
                        }
                        1 -> {
                            checkStoragePermissionAndOpenGallery()
                        }
                    }
                }
                .show()
            true
        }
    }
    
    private fun setupIconPackPreference() {
        val iconPackPref = findPreference<Preference>("icon_pack")
        val customizer = IconCustomizer(requireContext())
        val currentPack = customizer.getCurrentIconPack() ?: "Default"
        iconPackPref?.summary = if (currentPack == "Default") "System Default" else currentPack
        
        iconPackPref?.setOnPreferenceClickListener {
            showIconPackDialog(customizer, iconPackPref)
            true
        }
    }
    
    private fun showIconPackDialog(customizer: IconCustomizer, pref: Preference) {
        val packs = customizer.getInstalledIconPacks()
        val names = mutableListOf("System Default")
        names.addAll(packs.map { it.name })
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Select Icon Pack")
            .setItems(names.toTypedArray()) { _, which ->
                if (which == 0) {
                    customizer.setIconPack(null)
                    pref.summary = "System Default"
                } else {
                    val pack = packs[which - 1]
                    customizer.setIconPack(pack.packageName)
                    pref.summary = pack.name
                }
                Toast.makeText(context, "Icon pack updated", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
    
    private fun setupHiddenAppsPreference() {
        findPreference<Preference>("manage_hidden_apps")?.setOnPreferenceClickListener {
            showHiddenAppsDialog()
            true
        }
    }
    
    private fun showHiddenAppsDialog() {
        val context = requireContext()
        val prefsManager = PreferencesManager(context)
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        
        val allApps = packageManager.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .map { it.activityInfo.packageName to it.loadLabel(packageManager).toString() }
            .sortedBy { it.second }
        
        val hiddenApps = prefsManager.getHiddenApps()
        val appNames = allApps.map { it.second }.toTypedArray()
        val checkedItems = allApps.map { hiddenApps.contains(it.first) }.toBooleanArray()
        
        android.app.AlertDialog.Builder(context)
            .setTitle("Hidden Apps")
            .setMultiChoiceItems(appNames, checkedItems) { _, which, isChecked ->
                val packageName = allApps[which].first
                if (isChecked) {
                    prefsManager.hideApp(packageName)
                } else {
                    prefsManager.unhideApp(packageName)
                }
            }
            .setPositiveButton("Done", null)
            .show()
    }
    
    private fun setupSystemPreferences() {
        findPreference<Preference>("default_launcher")?.setOnPreferenceClickListener {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            true
        }
        
        findPreference<Preference>("notification_access")?.setOnPreferenceClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            true
        }
    }
}
