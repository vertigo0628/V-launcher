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
    
    enum class DiurnalPhase { 
        H00, H01, H02, H03, H04, H05, H06, H07, 
        H08, H09, H10, H11, H12, H13, H14, H15, 
        H16, H17, H18, H19, H20, H21, H22, H23 
    }

    fun getDiurnalPhase(): DiurnalPhase {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return DiurnalPhase.values()[hour]
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
            THEME_MIDNIGHT -> 0xFF0A1628.toInt()
            THEME_AMOLED -> 0xFF000000.toInt()
            THEME_DIURNAL -> {
                when (getDiurnalPhase()) {
                    DiurnalPhase.H00 -> 0xFF050510.toInt() // Witching hour midnight
                    DiurnalPhase.H01 -> 0xFF080210.toInt() // Deep abyss
                    DiurnalPhase.H02 -> 0xFF0A0B1A.toInt() // Cold space
                    DiurnalPhase.H03 -> 0xFF0B1424.toInt() // Pre-dawn chill
                    DiurnalPhase.H04 -> 0xFF0E1A2D.toInt() // Nautical twilight
                    DiurnalPhase.H05 -> 0xFF14243B.toInt() // Blue hour
                    DiurnalPhase.H06 -> 0xFF1B2228.toInt() // Sunrise edge
                    DiurnalPhase.H07 -> 0xFF162520.toInt() // Morning dew green
                    DiurnalPhase.H08 -> 0xFF182932.toInt() // Crisp morning cyan
                    DiurnalPhase.H09 -> 0xFF152A3F.toInt() // Bright morning sky
                    DiurnalPhase.H10 -> 0xFF1A1A32.toInt() // Mid-morning violet tint
                    DiurnalPhase.H11 -> 0xFF241525.toInt() // Almost noon magenta depth
                    DiurnalPhase.H12 -> 0xFF0F1A2C.toInt() // High noon deep blue
                    DiurnalPhase.H13 -> 0xFF181525.toInt() // Post-noon slate
                    DiurnalPhase.H14 -> 0xFF121E1B.toInt() // Afternoon jungle dark
                    DiurnalPhase.H15 -> 0xFF1A1810.toInt() // Golden hour approach (warm dark)
                    DiurnalPhase.H16 -> 0xFF241410.toInt() // Late afternoon copper
                    DiurnalPhase.H17 -> 0xFF281116.toInt() // Sunset crimson base
                    DiurnalPhase.H18 -> 0xFF220D22.toInt() // Purple dusk
                    DiurnalPhase.H19 -> 0xFF180A2B.toInt() // Twilight indigo
                    DiurnalPhase.H20 -> 0xFF100B24.toInt() // Evening void
                    DiurnalPhase.H21 -> 0xFF0C101A.toInt() // Nightfall navy
                    DiurnalPhase.H22 -> 0xFF081216.toInt() // Late night teal
                    DiurnalPhase.H23 -> 0xFF09060B.toInt() // Approaching midnight
                }
            }
            else -> 0xFF1E1E1E.toInt() // THEME_DARK
        }
    }
    
    fun getTextColor(): Int {
        return when (getCurrentTheme()) {
            THEME_MIDNIGHT -> 0xFFE0E8FF.toInt()
            THEME_AMOLED -> 0xFFFFFFFF.toInt()
            THEME_DIURNAL -> 0xFFFFFFFF.toInt() // Keep text bright for readability
            else -> 0xFFEEEEEE.toInt()
        }
    }
    
    fun getSecondaryTextColor(): Int {
        return when (getCurrentTheme()) {
            THEME_MIDNIGHT -> 0xFF7B8DAA.toInt()
            THEME_AMOLED -> 0xFFAAAAAA.toInt()
            THEME_DIURNAL -> {
                when (getDiurnalPhase()) {
                    DiurnalPhase.H00, DiurnalPhase.H01, DiurnalPhase.H02, DiurnalPhase.H03 -> 0xFFB39DDB.toInt()
                    DiurnalPhase.H04, DiurnalPhase.H05, DiurnalPhase.H06 -> 0xFF90CAF9.toInt()
                    DiurnalPhase.H07, DiurnalPhase.H08, DiurnalPhase.H09 -> 0xFF80DEEA.toInt()
                    DiurnalPhase.H10, DiurnalPhase.H11, DiurnalPhase.H12 -> 0xFFFFF59D.toInt()
                    DiurnalPhase.H13, DiurnalPhase.H14, DiurnalPhase.H15 -> 0xFFFFCC80.toInt()
                    DiurnalPhase.H16, DiurnalPhase.H17, DiurnalPhase.H18 -> 0xFFF48FB1.toInt()
                    DiurnalPhase.H19, DiurnalPhase.H20, DiurnalPhase.H21 -> 0xFFCE93D8.toInt()
                    DiurnalPhase.H22, DiurnalPhase.H23 -> 0xFF9FA8DA.toInt()
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
        return getThemeColors().first
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
        // Wild, 24-hour shifting multi-color gradients
        return when (getDiurnalPhase()) {
            // Midnight to 3AM: Bio-luminescent dark vibes
            DiurnalPhase.H00 -> Triple(0xFF00FFCC.toInt(), 0xFF7C4DFF.toInt(), 0xFFFF00FF.toInt()) // Cyber-witch
            DiurnalPhase.H01 -> Triple(0xFFFF0055.toInt(), 0xFF4A148C.toInt(), 0xFF00BFFF.toInt()) // Blood & Ice
            DiurnalPhase.H02 -> Triple(0xFF00E5FF.toInt(), 0xFF1DE9B6.toInt(), 0xFF651FFF.toInt()) // Deep sea glow
            DiurnalPhase.H03 -> Triple(0xFF536DFE.toInt(), 0xFF8C9EFF.toInt(), 0xFF18FFFF.toInt()) // Cold plasma
            
            // 4AM to 7AM: Dawn awakening (vibrant, explosive transitions)
            DiurnalPhase.H04 -> Triple(0xFFFF3D00.toInt(), 0xFFFFAB40.toInt(), 0xFF651FFF.toInt()) // Solar flare over dark
            DiurnalPhase.H05 -> Triple(0xFFFF1744.toInt(), 0xFFFF9100.toInt(), 0xFF00E5FF.toInt()) // Electric sunrise
            DiurnalPhase.H06 -> Triple(0xFFFFC400.toInt(), 0xFFFF3D00.toInt(), 0xFFD500F9.toInt()) // Golden explosion
            DiurnalPhase.H07 -> Triple(0xFF00E676.toInt(), 0xFF1DE9B6.toInt(), 0xFFFFEA00.toInt()) // Morning dew neon
            
            // 8AM to 11AM: High energy mornings
            DiurnalPhase.H08 -> Triple(0xFF18FFFF.toInt(), 0xFF64DD17.toInt(), 0xFFFFEA00.toInt()) // Tropical sprint
            DiurnalPhase.H09 -> Triple(0xFF2979FF.toInt(), 0xFF00E5FF.toInt(), 0xFF1DE9B6.toInt()) // Clear blue hyper
            DiurnalPhase.H10 -> Triple(0xFFD500F9.toInt(), 0xFF2979FF.toInt(), 0xFF00E5FF.toInt()) // Ultraviolet morning
            DiurnalPhase.H11 -> Triple(0xFFFF1744.toInt(), 0xFFD500F9.toInt(), 0xFF651FFF.toInt()) // Magenta rush
            
            // Noon to 3PM: Intense peaks
            DiurnalPhase.H12 -> Triple(0xFFFFEA00.toInt(), 0xFFFF3D00.toInt(), 0xFF2979FF.toInt()) // Zenith crash
            DiurnalPhase.H13 -> Triple(0xFF00E5FF.toInt(), 0xFF76FF03.toInt(), 0xFFFF3D00.toInt()) // Glitch afternoon
            DiurnalPhase.H14 -> Triple(0xFFFF9100.toInt(), 0xFFD500F9.toInt(), 0xFF00E5FF.toInt()) // Vaporwave heat
            DiurnalPhase.H15 -> Triple(0xFF651FFF.toInt(), 0xFFFF1744.toInt(), 0xFFFFEA00.toInt()) // Retro sunset early
            
            // 4PM to 7PM: Sunsets and golden hours gone wild
            DiurnalPhase.H16 -> Triple(0xFFFF3D00.toInt(), 0xFFFFC400.toInt(), 0xFFF50057.toInt()) // Liquid magma
            DiurnalPhase.H17 -> Triple(0xFFF50057.toInt(), 0xFF651FFF.toInt(), 0xFFFF9100.toInt()) // Purple horizon sky
            DiurnalPhase.H18 -> Triple(0xFFD500F9.toInt(), 0xFF2979FF.toInt(), 0xFFFF1744.toInt()) // Neon dusk
            DiurnalPhase.H19 -> Triple(0xFF651FFF.toInt(), 0xFF00BFFF.toInt(), 0xFF1DE9B6.toInt()) // Bioluminescent twilight
            
            // 8PM to 11PM: Nightlife electric
            DiurnalPhase.H20 -> Triple(0xFF00E5FF.toInt(), 0xFFD500F9.toInt(), 0xFFFFEA00.toInt()) // Arcade carpet
            DiurnalPhase.H21 -> Triple(0xFFFF006E.toInt(), 0xFF3A86FF.toInt(), 0xFF8338EC.toInt()) // Synthwave night
            DiurnalPhase.H22 -> Triple(0xFF76FF03.toInt(), 0xFF00E5FF.toInt(), 0xFF651FFF.toInt()) // Toxic glow
            DiurnalPhase.H23 -> Triple(0xFFF50057.toInt(), 0xFF00E5FF.toInt(), 0xFF000000.toInt()) // Laser grid
        }
    }
}
