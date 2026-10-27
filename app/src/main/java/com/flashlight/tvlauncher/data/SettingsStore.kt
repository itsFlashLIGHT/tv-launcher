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

/** Настройки вида главного экрана */
@Immutable
data class HomePrefs(
    val columns: Int = 5,
    val showClock: Boolean = true,
    val showWeather: Boolean = true,
    /** Медленный дрейф обоев; на слабом телевизоре можно выключить */
    val animatedWallpaper: Boolean = true,
    /** Имя встроенных обоев (WallpaperPreset); от них же строится тема */
    val wallpaper: String = "Twilight",
    /** Через сколько минут простоя показать заставку; 0 — не показывать */
    val screensaverMinutes: Int = 10,
)

/** Настройки лаунчера в SharedPreferences */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val city: Flow<City> = observe(setOf(KEY_CITY), ::readCity)

    val appLayout: Flow<AppLayout> = observe(setOf(KEY_FAVORITES, KEY_HIDDEN, KEY_ORDER), ::readAppLayout)

    val homePrefs: Flow<HomePrefs> =
        observe(
            setOf(KEY_COLUMNS, KEY_SHOW_CLOCK, KEY_SHOW_WEATHER, KEY_ANIMATED_WALLPAPER, KEY_WALLPAPER, KEY_SCREENSAVER),
            ::readHomePrefs,
        )

    fun updateHomePrefs(transform: (HomePrefs) -> HomePrefs) {
        val home = transform(readHomePrefs())
        prefs.edit {
            putInt(KEY_COLUMNS, home.columns)
            putBoolean(KEY_SHOW_CLOCK, home.showClock)
            putBoolean(KEY_SHOW_WEATHER, home.showWeather)
            putBoolean(KEY_ANIMATED_WALLPAPER, home.animatedWallpaper)
            putString(KEY_WALLPAPER, home.wallpaper)
            putInt(KEY_SCREENSAVER, home.screensaverMinutes)
        }
    }

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

    private fun readHomePrefs(): HomePrefs {
        val defaults = HomePrefs()
        return HomePrefs(
            columns = prefs.getInt(KEY_COLUMNS, defaults.columns),
            showClock = prefs.getBoolean(KEY_SHOW_CLOCK, defaults.showClock),
            showWeather = prefs.getBoolean(KEY_SHOW_WEATHER, defaults.showWeather),
            animatedWallpaper = prefs.getBoolean(KEY_ANIMATED_WALLPAPER, defaults.animatedWallpaper),
            wallpaper = prefs.getString(KEY_WALLPAPER, null) ?: defaults.wallpaper,
            screensaverMinutes = prefs.getInt(KEY_SCREENSAVER, defaults.screensaverMinutes),
        )
    }

    private fun readStringList(key: String): List<String> {
        val array = prefs.getString(key, null)?.let(::JSONArray) ?: return emptyList()
        return (0 until array.length()).map(array::getString)
    }

    private companion object {
        const val KEY_CITY = "city"
        const val KEY_FAVORITES = "favorites"
        const val KEY_HIDDEN = "hidden"
        const val KEY_ORDER = "order"
        const val KEY_COLUMNS = "columns"
        const val KEY_SHOW_CLOCK = "show_clock"
        const val KEY_SHOW_WEATHER = "show_weather"
        const val KEY_ANIMATED_WALLPAPER = "animated_wallpaper"
        const val KEY_WALLPAPER = "wallpaper"
        const val KEY_SCREENSAVER = "screensaver_minutes"
        val DEFAULT_CITY = City("Арсеньев", "Приморский Край, Россия", 44.15695, 133.27332)
    }
}
