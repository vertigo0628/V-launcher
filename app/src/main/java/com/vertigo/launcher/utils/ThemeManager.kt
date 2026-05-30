package com.vertigo.launcher.utils

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

class ThemeManager(context: Context) {
    
    private val prefs: SharedPreferences = 
        StorageHelper.getSafeSharedPreferences(context, "launcher_prefs")
    
    companion object {
        const val PREF_THEME = "theme"
        const val THEME_MIDNIGHT = "midnight"
        const val THEME_DARK = "dark"
        const val THEME_AMOLED = "amoled"
        const val THEME_DIURNAL = "diurnal"
    }
    
    enum class DiurnalPhase { MORNING, AFTERNOON, EVENING, NIGHT }

    fun getDiurnalPhase(): DiurnalPhase {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 6..11 -> DiurnalPhase.MORNING     // 6:00 AM - 11:59 AM
            in 12..16 -> DiurnalPhase.AFTERNOON  // 12:00 PM - 4:59 PM
            in 17..19 -> DiurnalPhase.EVENING    // 5:00 PM - 7:59 PM
            else -> DiurnalPhase.NIGHT           // 8:00 PM - 5:59 AM
        }
    }
    
    fun getCurrentTheme(): String {
        val theme = prefs.getString(PREF_THEME, THEME_DARK) ?: THEME_DARK
        return if (theme == "light") THEME_DARK else theme
    }
    
    fun setTheme(theme: String) {
        prefs.edit().putString(PREF_THEME, theme).apply()
    }
    
    fun getBackgroundColor(): Int {
        return when (getCurrentTheme()) {
            THEME_MIDNIGHT -> 0xFF0A1628.toInt() // Deep navy blue
            THEME_AMOLED -> 0xFF000000.toInt()
            THEME_DIURNAL -> {
                when (getDiurnalPhase()) {
                    DiurnalPhase.MORNING -> 0xFF0E1C22.toInt()   // Dawn dark teal
                    DiurnalPhase.AFTERNOON -> 0xFF0F1A2C.toInt() // Cyber midday navy
                    DiurnalPhase.EVENING -> 0xFF1D1420.toInt()   // Twilight sunset violet
                    DiurnalPhase.NIGHT -> 0xFF08080A.toInt()     // AMOLED midnight
                }
            }
            else -> 0xFF1E1E1E.toInt() // THEME_DARK
        }
    }
    
    fun getTextColor(): Int {
        return when (getCurrentTheme()) {
            THEME_MIDNIGHT -> 0xFFE0E8FF.toInt() // Soft blue-white
            THEME_AMOLED -> 0xFFFFFFFF.toInt()
            THEME_DIURNAL -> {
                when (getDiurnalPhase()) {
                    DiurnalPhase.MORNING -> 0xFFE0F2F1.toInt()
                    DiurnalPhase.AFTERNOON -> 0xFFE0F7FA.toInt()
                    DiurnalPhase.EVENING -> 0xFFFCE4EC.toInt()
                    DiurnalPhase.NIGHT -> 0xFFFFFFFF.toInt()
                }
            }
            else -> 0xFFEEEEEE.toInt()
        }
    }
    
    fun getSecondaryTextColor(): Int {
        return when (getCurrentTheme()) {
            THEME_MIDNIGHT -> 0xFF7B8DAA.toInt() // Muted blue-gray
            THEME_AMOLED -> 0xFFAAAAAA.toInt()
            THEME_DIURNAL -> {
                when (getDiurnalPhase()) {
                    DiurnalPhase.MORNING -> 0xFF80CBC4.toInt()
                    DiurnalPhase.AFTERNOON -> 0xFF80DEEA.toInt()
                    DiurnalPhase.EVENING -> 0xFFF48FB1.toInt()
                    DiurnalPhase.NIGHT -> 0xFFB39DDB.toInt()
                }
            }
            else -> 0xFF999999.toInt()
        }
    }

    fun getAccentColor(): Int {
        val theme = getCurrentTheme()
        if (theme != THEME_DIURNAL) {
            return when (theme) {
                THEME_MIDNIGHT -> 0xFF00C8FF.toInt()
                THEME_AMOLED -> 0xFFFFFFFF.toInt()
                else -> 0xFF00F0FF.toInt()
            }
        }
        return when (getDiurnalPhase()) {
            DiurnalPhase.MORNING -> 0xFFFFB300.toInt()
            DiurnalPhase.AFTERNOON -> 0xFF00F0FF.toInt()
            DiurnalPhase.EVENING -> 0xFFFF4081.toInt()
            DiurnalPhase.NIGHT -> 0xFFB388FF.toInt()
        }
    }

    fun getThemeColors(): Triple<Int, Int, Int> {
        val theme = getCurrentTheme()
        if (theme != THEME_DIURNAL) {
            val primary = when (theme) {
                THEME_MIDNIGHT -> 0xFF00C8FF.toInt()
                THEME_AMOLED -> 0xFFFFFFFF.toInt()
                else -> 0xFF00F0FF.toInt()
            }
            return Triple(primary, 0xFFFF006E.toInt(), 0xFFBD00FF.toInt())
        }
        return when (getDiurnalPhase()) {
            DiurnalPhase.MORNING -> Triple(0xFFFFB300.toInt(), 0xFFFF7043.toInt(), 0xFFFFCA28.toInt())
            DiurnalPhase.AFTERNOON -> Triple(0xFF00F0FF.toInt(), 0xFFFF006E.toInt(), 0xFFBD00FF.toInt())
            DiurnalPhase.EVENING -> Triple(0xFFFF4081.toInt(), 0xFFE040FB.toInt(), 0xFF7C4DFF.toInt())
            DiurnalPhase.NIGHT -> Triple(0xFFB388FF.toInt(), 0xFFE040FB.toInt(), 0xFF311B92.toInt())
        }
    }
}
