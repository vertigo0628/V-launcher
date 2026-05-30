package com.vertigo.launcher.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import com.vertigo.launcher.utils.StorageHelper

class WeatherRepository(private val context: android.content.Context) {
    private val api: WeatherApi
    private val prefs = StorageHelper.getSafeSharedPreferences(context, "weather_cache")
    private val gson = com.google.gson.Gson()
    
    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            
        api = retrofit.create(WeatherApi::class.java)
    }
    
    suspend fun getCurrentWeather(lat: Double, long: Double): WeatherResponse? {
        return try {
            val fetched = api.getWeather(lat, long)
            // Save to cache on success
            prefs.edit().putString("last_weather", gson.toJson(fetched)).apply()
            fetched
        } catch (e: Exception) {
            e.printStackTrace()
            // Return from cache on failure
            val cachedJson = prefs.getString("last_weather", null)
            if (cachedJson != null) {
                try {
                    gson.fromJson(cachedJson, WeatherResponse::class.java)
                } catch (jsonEx: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }
}
