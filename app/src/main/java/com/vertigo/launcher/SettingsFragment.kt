package com.vertigo.launcher

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.ListPreference
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.vertigo.launcher.utils.ThemeManager
import com.vertigo.launcher.utils.IconCustomizer
import com.vertigo.launcher.utils.PreferencesManager
import androidx.preference.SwitchPreferenceCompat
import com.vertigo.launcher.logic.AppCommander
import com.vertigo.launcher.service.VLauncherAccessibilityService

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Use Device Protected storage to match StorageHelper (Direct Boot safe)
        preferenceManager.setStorageDeviceProtected()
        preferenceManager.sharedPreferencesName = "launcher_prefs"
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        
        setupThemePreference()
        setupGridSizePreference()
        setupWallpaperPreference()
        setupIconPackPreference()
        setupHiddenAppsPreference()
        setupSystemPreferences()
        setupNeuralNavPreference()
        setupAiBrainPreference()
        setupShizukuPreferences()
        setupReignitePreference()
        setupKillSwitchPreference()
        setupFloatingAssistantPreference()
    }

    private fun setupReignitePreference() {
        findPreference<Preference>("reignite_services")?.setOnPreferenceClickListener {
            // 1. Prompt Notification Access (for Music)
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            
            // 2. Prompt Shizuku (for Boost)
            com.vertigo.launcher.utils.ShizukuSetup.requestPermissionIfNeeded()
            
            Toast.makeText(context, "Please re-authorize both services for Vertigo Launcher", Toast.LENGTH_LONG).show()
            true
        }
    }

    private fun setupFloatingAssistantPreference() {
        findPreference<SwitchPreferenceCompat>("floating_assistant_enabled")
            ?.setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean

                // Commit the new value NOW — before refreshFloatingButton() reads it,
                // because SwitchPreference hasn't written it yet at this point.
                com.vertigo.launcher.utils.StorageHelper
                    .getSafeDefaultSharedPreferences(requireContext())
                    .edit()
                    .putBoolean("floating_assistant_enabled", enabled)
                    .commit()  // synchronous commit so the read below sees it

                if (enabled && !VLauncherAccessibilityService.isEnabled()) {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    Toast.makeText(context, "Please enable 'V-launcher' in Accessibility Settings first", Toast.LENGTH_LONG).show()
                } else {
                    // Service is alive — refresh the overlay on main thread
                    VLauncherAccessibilityService.refreshFloatingButton()
                    val msg = if (enabled) "Floating Assistant ON" else "Floating Assistant OFF"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
                true
            }
    }

    private fun setupNeuralNavPreference() {
        val pref = findPreference<SwitchPreferenceCompat>("neural_nav_enabled")
        
        // Only show this emergency bypass for Xiaomi/POCO/Redmi devices
        val manufacturer = android.os.Build.MANUFACTURER.lowercase()
        val brand = android.os.Build.BRAND.lowercase()
        val isRestrictedDevice = manufacturer.contains("xiaomi") || brand.contains("xiaomi") || 
                                 manufacturer.contains("poco") || brand.contains("poco") ||
                                 manufacturer.contains("redmi") || brand.contains("redmi")

        if (!isRestrictedDevice) {
            pref?.isVisible = false
            return
        }

        pref?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            lifecycleScope.launch {
                if (enabled) {
                    AppCommander.hideNavigationBar()
                    if (!VLauncherAccessibilityService.isEnabled()) {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        Toast.makeText(context, "Please enable 'V-launcher' in Accessibility", Toast.LENGTH_LONG).show()
                    }
                } else {
                    AppCommander.showNavigationBar()
                }
            }
            true
        }
    }
    
    private fun setupShizukuPreferences() {
        findPreference<Preference>("shizuku_manager")?.setOnPreferenceClickListener {
            com.vertigo.launcher.utils.ShizukuSetup.requestPermissionIfNeeded()
            Toast.makeText(context, "Shizuku permission requested", Toast.LENGTH_SHORT).show()
            true
        }
        
        findPreference<Preference>("shizuku_permission_manager")?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), PermissionManagerActivity::class.java))
            true
        }
        
        findPreference<Preference>("shizuku_frozen_manager")?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), FrozenManagerActivity::class.java))
            true
        }
    }

    private fun setupKillSwitchPreference() {
        findPreference<Preference>("system_kill_switch")?.setOnPreferenceClickListener {
            if (!com.vertigo.launcher.utils.ShizukuShell.hasPermission()) {
                com.vertigo.launcher.utils.ShizukuSetup.requestPermissionIfNeeded()
                Toast.makeText(context, "Shizuku authorization required", Toast.LENGTH_SHORT).show()
                return@setOnPreferenceClickListener true
            }

            android.app.AlertDialog.Builder(requireContext())
                .setTitle("⚠️ Activate Kill Switch?")
                .setMessage("This will forcefully stop all background and user applications, freeing up memory like a reboot. Unsaved progress will be lost.")
                .setPositiveButton("Purge System") { _, _ ->
                    lifecycleScope.launch {
                        Toast.makeText(context, "Executing System Kill Switch...", Toast.LENGTH_LONG).show()
                        val result = AppCommander.killAllApps(requireContext())
                        if (result.isSuccess) {
                            Toast.makeText(context, "💀 System Purged successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Execution failed: ${result.stderr}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }
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
            .setTitle("System Storage")
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
        
        findPreference<Preference>("miui_autostart")?.setOnPreferenceClickListener {
            try {
                val intent = Intent()
                intent.component = android.content.ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "This option is intended for MIUI devices.", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }
    
    private fun setupAiBrainPreference() {
        val aiBrainPref = findPreference<Preference>("ollama_model_select")
        val aiUrlPref = findPreference<Preference>("ollama_base_url")
        val prefs = com.vertigo.launcher.utils.StorageHelper.getSafeDefaultSharedPreferences(requireContext())
        
        val currentModel = prefs.getString("ollama_model_select", "llama3.2:1b")
        aiBrainPref?.summary = currentModel
        
        aiBrainPref?.setOnPreferenceClickListener {
            val baseUrl = prefs.getString("ollama_base_url", "http://127.0.0.1:11434") ?: "http://127.0.0.1:11434"
            
            // Show a "Loading..." dialog while we fetch from the configured host
            val builder = android.app.AlertDialog.Builder(requireContext())
            builder.setTitle("AI Brain Model")
            builder.setMessage("Fetching models from $baseUrl...")
            builder.setCancelable(false)
            val loadingDialog = builder.create()
            loadingDialog.show()
            
            val ollamaClient = com.vertigo.launcher.logic.OllamaClient()
            
            // Launch coroutine to fetch models
            viewLifecycleOwner.lifecycleScope.launch {
                val result = ollamaClient.getAvailableModels(baseUrl)
                loadingDialog.dismiss()
                
                if (result.isSuccess) {
                    val models = result.getOrNull() ?: emptyList()
                    if (models.isEmpty()) {
                        Toast.makeText(requireContext(), "No models found at $baseUrl", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    
                    val modelArray = models.toTypedArray()
                    val currentIndex = models.indexOf(currentModel).takeIf { it >= 0 } ?: 0
                    
                    android.app.AlertDialog.Builder(requireContext())
                        .setTitle("Select AI Model")
                        .setSingleChoiceItems(modelArray, currentIndex) { dialog, which ->
                            val selectedModel = modelArray[which]
                            prefs.edit().putString("ollama_model_select", selectedModel).apply()
                            aiBrainPref.summary = selectedModel
                            Toast.makeText(requireContext(), "Model changed to $selectedModel", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    val error = result.exceptionOrNull()
                    Toast.makeText(requireContext(), "Failed to connect to $baseUrl\n${error?.message ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                }
            }
            true
        }
    }
}
