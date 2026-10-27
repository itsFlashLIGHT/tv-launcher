package com.flashlight.tvlauncher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flashlight.tvlauncher.data.AppEntry
import com.flashlight.tvlauncher.data.AppRepository
import com.flashlight.tvlauncher.data.City
import com.flashlight.tvlauncher.data.SettingsStore
import com.flashlight.tvlauncher.data.Weather
import com.flashlight.tvlauncher.data.WeatherRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val weatherRepository = WeatherRepository()
    private val settings = SettingsStore(application)

    /** null — список ещё загружается */
    val apps: StateFlow<List<AppEntry>?> = repository.changes()
        .onStart { emit(Unit) }
        .conflate()
        .map { repository.loadApps() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val city: StateFlow<City?> = settings.city
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** null — погода ещё не загружена; при ошибке сети остаётся последнее значение */
    val weather: StateFlow<Weather?> = settings.city
        .flatMapLatest(::weatherUpdates)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectCity(city: City) = settings.setCity(city)

    /** Пустой список и при отсутствии совпадений, и при ошибке сети */
    suspend fun searchCities(query: String): List<City> =
        runCatching { weatherRepository.searchCities(query) }.getOrDefault(emptyList())

    private fun weatherUpdates(city: City): Flow<Weather> = flow {
        while (true) {
            val result = runCatching { weatherRepository.current(city) }
            result.getOrNull()?.let { emit(it) }
            delay(if (result.isSuccess) WEATHER_REFRESH_MS else WEATHER_RETRY_MS)
        }
    }

    private companion object {
        const val WEATHER_REFRESH_MS = 30 * 60_000L
        const val WEATHER_RETRY_MS = 20_000L
    }
}
