package com.flashlight.tvlauncher.data

import android.util.Log
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CancellationException
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

/**
 * Погода и поиск городов через Open-Meteo: бесплатно, без ключа API.
 * Если прогноз Open-Meteo недоступен (у некоторых провайдеров заблокирован его адрес),
 * погода берётся у MET Norway — тоже бесплатно и без ключа.
 */
class WeatherRepository {
    suspend fun current(city: City): Weather =
        try {
            openMeteo(city)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "Open-Meteo недоступен, пробуем MET Norway", e)
            try {
                metNorway(city)
            } catch (fallback: Exception) {
                if (fallback !is CancellationException) Log.w(TAG, "MET Norway недоступен", fallback)
                throw fallback
            }
        }

    private suspend fun openMeteo(city: City): Weather {
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

    /** https://api.met.no/weatherapi/locationforecast/2.0/documentation */
    private suspend fun metNorway(city: City): Weather {
        // MET Norway просит не больше 4 знаков после запятой в координатах
        val data = getJson(
            "https://api.met.no/weatherapi/locationforecast/2.0/compact" +
                "?lat=${"%.4f".format(Locale.US, city.latitude)}" +
                "&lon=${"%.4f".format(Locale.US, city.longitude)}",
        ).getJSONObject("properties").getJSONArray("timeseries").getJSONObject(0).getJSONObject("data")
        val symbol = data.optJSONObject("next_1_hours")?.optJSONObject("summary")?.optString("symbol_code")
        return Weather(
            temperature = data.getJSONObject("instant").getJSONObject("details")
                .getDouble("air_temperature").roundToInt(),
            condition = symbol?.let(::conditionForSymbol) ?: WeatherCondition.Cloudy,
            isDay = symbol?.endsWith("_night") != true,
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
        // MET Norway отклоняет запросы без User-Agent, по которому можно связаться с автором
        connection.setRequestProperty("User-Agent", USER_AGENT)
        try {
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TAG = "WeatherRepository"
        const val TIMEOUT_MS = 10_000
        const val USER_AGENT = "tv-launcher github.com/itsFlashLIGHT/tv-launcher"

        /**
         * Символы MET Norway, например «lightrainshowers_day»:
         * https://github.com/metno/weathericons/tree/main/weather
         */
        fun conditionForSymbol(symbol: String): WeatherCondition {
            val name = symbol.substringBefore('_')
            return when {
                "thunder" in name -> WeatherCondition.Thunderstorm
                "snow" in name || "sleet" in name -> WeatherCondition.Snow
                name == "lightrain" -> WeatherCondition.Drizzle
                "rain" in name -> WeatherCondition.Rain
                name == "fog" -> WeatherCondition.Fog
                name == "clearsky" -> WeatherCondition.Clear
                name == "fair" || name == "partlycloudy" -> WeatherCondition.PartlyCloudy
                else -> WeatherCondition.Cloudy
            }
        }

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
