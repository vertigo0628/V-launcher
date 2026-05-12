package com.vertigo.launcher.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherRepository {
    private val api: WeatherApi
    
    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            
        api = retrofit.create(WeatherApi::class.java)
    }
    
    suspend fun getCurrentWeather(lat: Double, long: Double): WeatherResponse? {
        return try {
            api.getWeather(lat, long)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
