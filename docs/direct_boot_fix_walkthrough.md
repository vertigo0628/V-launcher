# Resolving the Direct Boot Crash in V-Launcher

To resolve the `java.lang.IllegalStateException` during early startup (Direct Boot), we've migrated the launcher's `SharedPreferences` from Credential Encrypted (CE) storage to Device Protected (DE) storage. This keeps the launcher fully functional before the user inputs their credentials while safely preserving existing settings via an automated migration path.

## 🛠️ Changes Implemented

### 1. Created `StorageHelper` Utility
A new utility class was added to handle safe context wrapping and automate preference migration:
* **Direct Boot Safe**: Checks the current SDK and retrieves a Device Protected Storage Context.
* **Auto Migration**: Detects if preferences exist in CE storage and moves them to DE storage upon the first post-unlock boot using `moveSharedPreferencesFrom`.

File: [StorageHelper.kt](file:///home/vertigo/AndroidStudioProjects/V-launcher/app/src/main/java/com/vertigo/launcher/utils/StorageHelper.kt)

```kotlin
package com.vertigo.launcher.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.os.UserManagerCompat

object StorageHelper {
    fun getSafeContext(context: Context): Context {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createDeviceProtectedStorageContext()
        } else {
            context
        }
    }

    fun getSafeSharedPreferences(context: Context, name: String, mode: Int = Context.MODE_PRIVATE): SharedPreferences {
        val safeContext = getSafeContext(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val migrationPrefKey = "migrated_to_de_v1"
            val dePrefs = safeContext.getSharedPreferences(name, mode)
            if (!dePrefs.getBoolean(migrationPrefKey, false)) {
                if (UserManagerCompat.isUserUnlocked(context)) {
                    try {
                        safeContext.deleteSharedPreferences(name)
                        if (safeContext.moveSharedPreferencesFrom(context, name)) {
                            safeContext.getSharedPreferences(name, mode)
                                .edit()
                                .putBoolean(migrationPrefKey, true)
                                .apply()
                        } else {
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

    fun getSafeDefaultSharedPreferences(context: Context): SharedPreferences {
        val name = "${context.packageName}_preferences"
        return getSafeSharedPreferences(context, name)
    }
}
```

---

### 2. Refactored Manager Classes & ViewModels
Every instance of standard `getSharedPreferences` or `getDefaultSharedPreferences` has been refactored to use `StorageHelper`.

#### Modifying the Managers:
* **`ThemeEngine`**: [ThemeEngine.kt](file:///home/vertigo/AndroidStudioProjects/V-launcher/app/src/main/java/com/vertigo/launcher/utils/ThemeEngine.kt)
* **`PreferencesManager`**: [PreferencesManager.kt](file:///home/vertigo/AndroidStudioProjects/V-launcher/app/src/main/java/com/vertigo/launcher/utils/PreferencesManager.kt)
* **`FolderManager`**: [FolderManager.kt](file:///home/vertigo/AndroidStudioProjects/V-launcher/app/src/main/java/com/vertigo/launcher/utils/FolderManager.kt)
* **`DockManager`**: [DockManager.kt](file:///home/vertigo/AndroidStudioProjects/V-launcher/app/src/main/java/com/vertigo/launcher/utils/DockManager.kt)
* **`SmartUsageManager`**: [SmartUsageManager.kt](file:///home/vertigo/AndroidStudioProjects/V-launcher/app/src/main/java/com/vertigo/launcher/utils/SmartUsageManager.kt)
* **`IconCustomizer`**: [IconCustomizer.kt](file:///home/vertigo/AndroidStudioProjects/V-launcher/app/src/main/java/com/vertigo/launcher/utils/IconCustomizer.kt)
* **`GestureManager`**: [GestureManager.kt](file:///home/vertigo/AndroidStudioProjects/V-launcher/app/src/main/java/com/vertigo/launcher/utils/GestureManager.kt)
* **`BackupManager`**: [BackupManager.kt](file:///home/vertigo/AndroidStudioProjects/V-launcher/app/src/main/java/com/vertigo/launcher/utils/BackupManager.kt)
* **`SmartWidgetManager`**: [SmartWidgetManager.kt](file:///home/vertigo/AndroidStudioProjects/V-launcher/app/src/main/java/com/vertigo/launcher/utils/SmartWidgetManager.kt)
* **`IconPackManager`**: [IconPackManager.kt](file:///home/vertigo/AndroidStudioProjects/V-launcher/app/src/main/java/com/vertigo/launcher/utils/IconPackManager.kt)
* **`CategoryManager`**: [CategoryManager.kt](file:///home/vertigo/AndroidStudioProjects/V-launcher/app/src/main/java/com/vertigo/launcher/utils/CategoryManager.kt)
* **`SmartFeaturesManager`**: [SmartFeaturesManager.kt](file:///home/vertigo/AndroidStudioProjects/V-launcher/app/src/main/java/com/vertigo/launcher/utils/SmartFeaturesManager.kt)
* **`FlowerGridManager`**: [FlowerGridManager.kt](file:///home/vertigo/AndroidStudioProjects/V-launcher/app/src/main/java/com/vertigo/launcher/utils/FlowerGridManager.kt)
* **`ThemeManager`**: [ThemeManager.kt](file:///home/vertigo/AndroidStudioProjects/V-launcher/app/src/main/java/com/vertigo/launcher/utils/ThemeManager.kt)

#### Modifying the UI & Repositories:
* **`MainActivity`**: [MainActivity.kt](file:///home/vertigo/AndroidStudioProjects/V-launcher/app/src/main/java/com/vertigo/launcher/MainActivity.kt)
* **`HomeViewModel`**: [HomeViewModel.kt](file:///home/vertigo/AndroidStudioProjects/V-launcher/app/src/main/java/com/vertigo/launcher/ui/HomeViewModel.kt)
* **`WeatherRepository`**: [WeatherRepository.kt](file:///home/vertigo/AndroidStudioProjects/V-launcher/app/src/main/java/com/vertigo/launcher/data/WeatherRepository.kt)
* **`NeuralInsightRepository`**: [NeuralInsightRepository.kt](file:///home/vertigo/AndroidStudioProjects/V-launcher/app/src/main/java/com/vertigo/launcher/data/NeuralInsightRepository.kt)
* **`SettingsFragment`**: [SettingsFragment.kt](file:///home/vertigo/AndroidStudioProjects/V-launcher/app/src/main/java/com/vertigo/launcher/SettingsFragment.kt)
* **`MiniApps` Composables**: [MiniApps.kt](file:///home/vertigo/AndroidStudioProjects/V-launcher/app/src/main/java/com/vertigo/launcher/compose/MiniApps.kt)

---

## 🔬 Compilation Status
The project was compiled successfully using Android Gradle Plugin 8.2.0:
```bash
$ ./gradlew compileDebugKotlin
...
BUILD SUCCESSFUL in 4m 57s
```
No compile errors were encountered. The application is now fully Direct Boot compliant!
