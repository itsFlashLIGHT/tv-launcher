package com.flashlight.tvlauncher.data

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.roundToInt

enum class WeatherCondition { Clear, PartlyCloudy, Cloudy, Fog, Drizzle, Rain, Snow, Thunderstorm }

@Immutable
data class Weather(
    val temperature: Int,
    val condition: WeatherCondition,
    val isDay: Boolean,
)

@Immutable
data class City(
    val name: String,
    /** Регион и страна, например «Приморский Край, Россия» */
    val region: String,
    val latitude: Double,
    val longitude: Double,
)

/** Погода и поиск городов через Open-Meteo: бесплатно, без ключа API */
class WeatherRepository {
    suspend fun current(city: City): Weather {
        val current = getJson(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=${city.latitude}&longitude=${city.longitude}" +
                "&current=temperature_2m,weather_code,is_day&timezone=auto",
        ).getJSONObject("current")
        return Weather(
            temperature = current.getDouble("temperature_2m").roundToInt(),
            condition = conditionFor(current.getInt("weather_code")),
            isDay = current.getInt("is_day") == 1,
        )
    }

    suspend fun searchCities(query: String): List<City> {
        val json = getJson(
            "https://geocoding-api.open-meteo.com/v1/search" +
                "?name=${URLEncoder.encode(query, "UTF-8")}&count=10" +
                "&language=${Locale.getDefault().language}",
        )
        val results = json.optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).map { i ->
            val item = results.getJSONObject(i)
            City(
                name = item.getString("name"),
                region = listOf(item.optString("admin1"), item.optString("country"))
                    .filter { it.isNotEmpty() }
                    .joinToString(", "),
                latitude = item.getDouble("latitude"),
                longitude = item.getDouble("longitude"),
            )
        }
    }

    private suspend fun getJson(url: String): JSONObject = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        try {
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000

        /** Коды WMO: https://open-meteo.com/en/docs */
        fun conditionFor(code: Int): WeatherCondition = when (code) {
            0 -> WeatherCondition.Clear
            1, 2 -> WeatherCondition.PartlyCloudy
            3 -> WeatherCondition.Cloudy
            45, 48 -> WeatherCondition.Fog
            in 51..57 -> WeatherCondition.Drizzle
            in 61..67, in 80..82 -> WeatherCondition.Rain
            in 71..77, 85, 86 -> WeatherCondition.Snow
            in 95..99 -> WeatherCondition.Thunderstorm
            else -> WeatherCondition.Cloudy
        }
    }
}
