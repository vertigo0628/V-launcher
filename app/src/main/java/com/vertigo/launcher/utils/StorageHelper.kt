package com.vertigo.launcher.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.os.UserManagerCompat

object StorageHelper {
    /**
     * Returns a context suitable for accessing preferences/files.
     * During Direct Boot (device locked), returns a device-protected storage context.
     * When unlocked, we also prefer to use the device-protected storage context for all custom launcher settings
     * to keep them consistent and accessible, but we migrate any existing CE preferences to DE storage.
     */
    fun getSafeContext(context: Context): Context {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createDeviceProtectedStorageContext()
        } else {
            context
        }
    }

    /**
     * Gets SharedPreferences in a Direct-Boot safe manner, migrating old CE preferences to DE if possible.
     */
    fun getSafeSharedPreferences(context: Context, name: String, mode: Int = Context.MODE_PRIVATE): SharedPreferences {
        val safeContext = getSafeContext(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val migrationPrefKey = "migrated_to_de_v1"
            val dePrefs = safeContext.getSharedPreferences(name, mode)
            if (!dePrefs.getBoolean(migrationPrefKey, false)) {
                if (UserManagerCompat.isUserUnlocked(context)) {
                    try {
                        // Delete any temporary clean DE file created during Direct Boot
                        // to ensure moveSharedPreferencesFrom successfully moves the user settings.
                        safeContext.deleteSharedPreferences(name)
                        if (safeContext.moveSharedPreferencesFrom(context, name)) {
                            safeContext.getSharedPreferences(name, mode)
                                .edit()
                                .putBoolean(migrationPrefKey, true)
                                .apply()
                        } else {
                            // If move failed (e.g. source file doesn't exist), mark as migrated so we don't retry
                            dePrefs.edit().putBoolean(migrationPrefKey, true).apply()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("StorageHelper", "Failed to migrate shared preferences $name", e)
                    }
                }
            }
        }
        return safeContext.getSharedPreferences(name, mode)
    }

    /**
     * Gets default SharedPreferences name in a Direct-Boot safe manner.
     */
    fun getSafeDefaultSharedPreferences(context: Context): SharedPreferences {
        val name = "${context.packageName}_preferences"
        return getSafeSharedPreferences(context, name)
    }
}
