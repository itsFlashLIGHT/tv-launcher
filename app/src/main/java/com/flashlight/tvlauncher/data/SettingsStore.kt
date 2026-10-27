package com.flashlight.tvlauncher.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.json.JSONArray
import org.json.JSONObject

/** Пользовательская раскладка приложений; всё хранится по имени пакета */
@Immutable
data class AppLayout(
    /** Избранное в порядке показа */
    val favorites: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    /** Порядок остальных приложений; кого нет в списке — в конце по алфавиту */
    val order: List<String> = emptyList(),
)

/** Настройки лаунчера в SharedPreferences */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val city: Flow<City> = observe(setOf(KEY_CITY), ::readCity)

    val appLayout: Flow<AppLayout> = observe(setOf(KEY_FAVORITES, KEY_HIDDEN, KEY_ORDER), ::readAppLayout)

    fun setCity(city: City) {
        val json = JSONObject()
            .put("name", city.name)
            .put("region", city.region)
            .put("latitude", city.latitude)
            .put("longitude", city.longitude)
        prefs.edit { putString(KEY_CITY, json.toString()) }
    }

    fun updateAppLayout(transform: (AppLayout) -> AppLayout) {
        val layout = transform(readAppLayout())
        prefs.edit {
            putString(KEY_FAVORITES, JSONArray(layout.favorites).toString())
            putString(KEY_HIDDEN, JSONArray(layout.hidden.toList()).toString())
            putString(KEY_ORDER, JSONArray(layout.order).toString())
        }
    }

    private fun <T> observe(keys: Set<String>, read: () -> T): Flow<T> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key in keys) trySend(read())
        }
        trySend(read())
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    private fun readCity(): City {
        val json = prefs.getString(KEY_CITY, null)?.let(::JSONObject) ?: return DEFAULT_CITY
        return City(
            name = json.getString("name"),
            region = json.optString("region"),
            latitude = json.getDouble("latitude"),
            longitude = json.getDouble("longitude"),
        )
    }

    private fun readAppLayout() = AppLayout(
        favorites = readStringList(KEY_FAVORITES),
        hidden = readStringList(KEY_HIDDEN).toSet(),
        order = readStringList(KEY_ORDER),
    )

    private fun readStringList(key: String): List<String> {
        val array = prefs.getString(key, null)?.let(::JSONArray) ?: return emptyList()
        return (0 until array.length()).map(array::getString)
    }

    private companion object {
        const val KEY_CITY = "city"
        const val KEY_FAVORITES = "favorites"
        const val KEY_HIDDEN = "hidden"
        const val KEY_ORDER = "order"
        val DEFAULT_CITY = City("Арсеньев", "Приморский Край, Россия", 44.15695, 133.27332)
    }
}
