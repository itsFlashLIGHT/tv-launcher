package com.flashlight.tvlauncher.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.json.JSONObject

/** Настройки лаунчера в SharedPreferences */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val city: Flow<City> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_CITY) trySend(readCity())
        }
        trySend(readCity())
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun setCity(city: City) {
        val json = JSONObject()
            .put("name", city.name)
            .put("region", city.region)
            .put("latitude", city.latitude)
            .put("longitude", city.longitude)
        prefs.edit { putString(KEY_CITY, json.toString()) }
    }

    private fun readCity(): City {
        val json = prefs.getString(KEY_CITY, null)?.let(::JSONObject) ?: return DEFAULT_CITY
        return City(
            name = json.getString("name"),
            region = json.optString("region"),
            latitude = json.getDouble("latitude"),
            longitude = json.getDouble("longitude"),
        )
    }

    private companion object {
        const val KEY_CITY = "city"
        val DEFAULT_CITY = City("Арсеньев", "Приморский Край, Россия", 44.15695, 133.27332)
    }
}
