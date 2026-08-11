package com.agrogestao.pro.data.weather

import android.content.Context
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

class WeatherPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("weather_preferences", Context.MODE_PRIVATE)

    fun hasConsent(ownerUserId: String): Boolean =
        ownerUserId.isNotBlank() && preferences.getBoolean(key(ownerUserId, "consent"), false)

    fun grantConsent(ownerUserId: String) {
        if (ownerUserId.isNotBlank()) preferences.edit().putBoolean(key(ownerUserId, "consent"), true).apply()
    }

    fun revokeConsent(ownerUserId: String) {
        if (ownerUserId.isBlank()) return
        preferences.edit()
            .putBoolean(key(ownerUserId, "consent"), false)
            .remove(key(ownerUserId, "forecast"))
            .apply()
    }

    fun save(ownerUserId: String, forecast: WeatherForecast) {
        if (ownerUserId.isBlank() || !hasConsent(ownerUserId)) return
        preferences.edit().putString(key(ownerUserId, "forecast"), forecast.toJson().toString()).apply()
    }

    fun read(ownerUserId: String): WeatherForecast? {
        if (!hasConsent(ownerUserId)) return null
        val serialized = preferences.getString(key(ownerUserId, "forecast"), null) ?: return null
        return runCatching { JSONObject(serialized).toForecast() }.getOrNull()
    }

    private fun key(ownerUserId: String, suffix: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(ownerUserId.toByteArray())
        val ownerKey = digest.take(12).joinToString("") { "%02x".format(it) }
        return "${ownerKey}_$suffix"
    }

    private fun WeatherForecast.toJson() = JSONObject().apply {
        put("requested", requestedMunicipality)
        put("resolved", resolvedLocation)
        put("latitude", latitude)
        put("longitude", longitude)
        put("fetched_at", fetchedAtEpochMillis)
        put("days", JSONArray().apply {
            days.forEach { day ->
                put(JSONObject().apply {
                    put("date", day.date)
                    put("code", day.weatherCode)
                    put("min", day.minTemperatureC)
                    put("max", day.maxTemperatureC)
                    put("rain", day.precipitationMm)
                    put("gust", day.maxWindGustKmh)
                })
            }
        })
    }

    private fun JSONObject.toForecast(): WeatherForecast {
        val dayArray = getJSONArray("days")
        return WeatherForecast(
            requestedMunicipality = getString("requested"),
            resolvedLocation = getString("resolved"),
            latitude = getDouble("latitude"),
            longitude = getDouble("longitude"),
            fetchedAtEpochMillis = getLong("fetched_at"),
            days = List(dayArray.length()) { index ->
                dayArray.getJSONObject(index).let { day ->
                    WeatherDay(
                        date = day.getString("date"),
                        weatherCode = day.getInt("code"),
                        minTemperatureC = day.getDouble("min"),
                        maxTemperatureC = day.getDouble("max"),
                        precipitationMm = day.getDouble("rain"),
                        maxWindGustKmh = day.getDouble("gust")
                    )
                }
            }.take(10)
        ).also { require(it.days.isNotEmpty()) }
    }
}
