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
        preferenceManager.sharedPreferencesName = "launcher_prefs"
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

    private fun setupWallpaperPreference() {
        findPreference<Preference>("wallpaper_picker")?.setOnPreferenceClickListener {
            startActivity(Intent(Intent.ACTION_SET_WALLPAPER))
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
