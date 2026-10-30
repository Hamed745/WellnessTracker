package com.example.welnesstracker.network

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

sealed class WeatherState {
    object NoCity : WeatherState()
    object Loading : WeatherState()
    object CityNotFound : WeatherState()
    object Error : WeatherState()
    data class Success(val tempC: Int, val code: Int, val city: String) : WeatherState()
}

/** Free Open-Meteo APIs (no API key needed). Must be called off the main thread. */
object WeatherService {

    fun fetch(city: String): WeatherState {
        return try {
            val query = URLEncoder.encode(city, "UTF-8")
            val geo = JSONObject(
                httpGet("https://geocoding-api.open-meteo.com/v1/search?name=$query&count=1&format=json")
            )
            val results = geo.optJSONArray("results")
            if (results == null || results.length() == 0) return WeatherState.CityNotFound

            val place = results.getJSONObject(0)
            val lat = place.getDouble("latitude")
            val lon = place.getDouble("longitude")

            val forecast = JSONObject(
                httpGet(
                    "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                        "&current=temperature_2m,weather_code"
                )
            )
            val current = forecast.getJSONObject("current")
            WeatherState.Success(
                tempC = Math.round(current.getDouble("temperature_2m")).toInt(),
                code = current.getInt("weather_code"),
                city = city
            )
        } catch (e: Exception) {
            WeatherState.Error
        }
    }

    private fun httpGet(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "GET"
            if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
