package com.vertigo.launcher.data

import android.content.Context
import android.content.SharedPreferences
import com.vertigo.launcher.logic.HolidayApi
import com.vertigo.launcher.logic.PublicHoliday
import com.vertigo.launcher.logic.WikipediaApi
import com.vertigo.launcher.logic.WikipediaResponse
import com.vertigo.launcher.logic.WikiInsightItem
import com.vertigo.launcher.logic.WikiEvent
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class NeuralInsightRepository(private val context: Context) {
    private val holidayApi: HolidayApi
    private val wikiApi: WikipediaApi
    private val prefs: SharedPreferences = context.getSharedPreferences("neural_insights_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "V-Launcher/1.0 (contact: vertigo@example.com)")
                    .build()
                chain.proceed(request)
            }
            .build()

        val holidayRetrofit = Retrofit.Builder()
            .baseUrl("https://date.nager.at/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        holidayApi = holidayRetrofit.create(HolidayApi::class.java)

        val wikiRetrofit = Retrofit.Builder()
            .baseUrl("https://en.wikipedia.org/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        wikiApi = wikiRetrofit.create(WikipediaApi::class.java)
    }

    suspend fun getHolidayForToday(countryCode: String = "KE"): String? = withContext(Dispatchers.IO) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val year = Calendar.getInstance().get(Calendar.YEAR)
        
        // 1. Check cache first
        val cachedHolidaysJson = prefs.getString("holidays_$year", null)
        val holidays = if (cachedHolidaysJson != null) {
            gson.fromJson(cachedHolidaysJson, Array<PublicHoliday>::class.java).toList()
        } else {
            // 2. Fetch from API if not cached
            try {
                val fetched = holidayApi.getHolidays(year, countryCode)
                prefs.edit().putString("holidays_$year", gson.toJson(fetched)).apply()
                fetched
            } catch (e: Exception) {
                emptyList()
            }
        }

        holidays.find { it.date == today }?.name
    }

    suspend fun getDailyInsights(interests: Map<String, Boolean>): List<WikiInsightItem> = withContext(Dispatchers.IO) {
        val todayKey = SimpleDateFormat("MM_dd", Locale.getDefault()).format(Date())
        val cacheKey = "wiki_cache_$todayKey"
        
        // 1. Try to load from cache first
        val cachedJson = prefs.getString(cacheKey, null)
        val response: WikipediaResponse? = if (cachedJson != null) {
            try {
                gson.fromJson(cachedJson, WikipediaResponse::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }

        val finalResponse = response ?: try {
            // 2. Fetch from API if not cached or cache invalid
            val fetched = wikiApi.getOnThisDay(
                String.format("%02d", Calendar.getInstance().get(Calendar.MONTH) + 1),
                String.format("%02d", Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
            )
            // 3. Save to cache
            prefs.edit().putString(cacheKey, gson.toJson(fetched)).apply()
            fetched
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        if (finalResponse == null) {
            return@withContext listOf(WikiInsightItem("Neural Link Error: Check your network connection."))
        }

        // 4. Process and Filter the response (Cached or Fresh)
        val allEvents = (finalResponse.selected ?: emptyList()) + 
                       (finalResponse.events ?: emptyList()) + 
                       (finalResponse.births ?: emptyList())

        val filteredInsights = mutableListOf<WikiInsightItem>()
        
        // Keyword Maps for the Neural Filter
        val keywords = mapOf(
            "Tech" to listOf("computer", "software", "internet", "silicon", "chip", "digital", "network", "web", "programming"),
            "Space" to listOf("nasa", "orbit", "satellite", "planet", "moon", "rocket", "galaxy", "telescope", "apollo", "mission"),
            "Science" to listOf("discovered", "theory", "formula", "particle", "virus", "gene", "element", "physics", "biology", "chemistry"),
            "Innovators" to listOf("inventor", "scientist", "engineer", "founder", "pioneer", "physicist", "chemist"),
            "History" to listOf("war", "treaty", "king", "queen", "president", "empire", "battle", "independence", "signed")
        )

        // Helper to construct WikiInsightItem from WikiEvent
        fun mapToInsight(event: WikiEvent): WikiInsightItem {
            val textStr = "In ${event.year ?: ""}: ${event.text}"
            val firstPage = event.pages?.find { it.thumbnail?.source != null } ?: event.pages?.firstOrNull()
            return WikiInsightItem(
                text = textStr,
                imageUrl = firstPage?.thumbnail?.source,
                pageTitle = firstPage?.titles?.normalized,
                extract = firstPage?.extract
            )
        }

        // Dynamic Filtering
        for (item in allEvents) {
            if (filteredInsights.size >= 5) break
            
            val text = item.text.lowercase()
            var matchesAnyActivePref = false
            
            for ((category, enabled) in interests) {
                if (enabled) {
                    val categoryKeywords = keywords[category] ?: emptyList()
                    if (categoryKeywords.any { text.contains(it) }) {
                        matchesAnyActivePref = true
                        break
                    }
                }
            }

            if (matchesAnyActivePref) {
                filteredInsights.add(mapToInsight(item))
            }
        }

        // Fallback: If filter is too strict, just show 2-3 significant items
        if (filteredInsights.size < 2) {
            finalResponse.selected?.take(3)?.forEach { item ->
                val textStr = "In ${item.year ?: ""}: ${item.text}"
                val alreadyAdded = filteredInsights.any { it.text == textStr }
                if (!alreadyAdded) {
                    filteredInsights.add(mapToInsight(item))
                }
            }
        }

        filteredInsights
    }

    /**
     * Calculates the current Moon phase for the "And Beyond" feature.
     */
    fun getCosmicInsight(): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        // Simple Moon Phase Algorithm (Lilly-Lockhart)
        var lp = 2551443
        val now = Calendar.getInstance()
        val newMoon = Calendar.getInstance().apply { set(1970, 0, 7, 20, 35, 0) }
        val phase = ((now.timeInMillis - newMoon.timeInMillis) / 1000) % lp
        val age = phase / (24 * 3600)
        
        return when {
            age < 1.84566 -> "🌑 NEW MOON"
            age < 5.53699 -> "🌒 WAXING CRESCENT"
            age < 9.22831 -> "🌓 FIRST QUARTER"
            age < 12.9196 -> "🌔 WAXING GIBBOUS"
            age < 16.6110 -> "🌕 FULL MOON"
            age < 20.3023 -> "🌖 WANING GIBBOUS"
            age < 23.9936 -> "🌗 LAST QUARTER"
            age < 27.6849 -> "🌘 WANING CRESCENT"
            else -> "🌑 NEW MOON"
        }
    }
}
