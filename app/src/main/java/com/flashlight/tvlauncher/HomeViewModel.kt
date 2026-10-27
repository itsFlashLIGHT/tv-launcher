package com.flashlight.tvlauncher

import android.app.Application
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flashlight.tvlauncher.data.AppEntry
import com.flashlight.tvlauncher.data.AppLayout
import com.flashlight.tvlauncher.data.AppRepository
import com.flashlight.tvlauncher.data.City
import com.flashlight.tvlauncher.data.HomePrefs
import com.flashlight.tvlauncher.data.MediaSessions
import com.flashlight.tvlauncher.data.NowPlaying
import com.flashlight.tvlauncher.data.findYandexMusic
import com.flashlight.tvlauncher.data.NetworkKind
import com.flashlight.tvlauncher.data.SleepTimer
import com.flashlight.tvlauncher.data.networkKind
import com.flashlight.tvlauncher.data.SettingsStore
import com.flashlight.tvlauncher.data.Weather
import com.flashlight.tvlauncher.data.WeatherRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/** Приложения, разложенные по разделам главного экрана */
@Immutable
data class HomeApps(
    val favorites: List<AppEntry>,
    val others: List<AppEntry>,
    val hidden: List<AppEntry>,
) {
    fun isFavorite(app: AppEntry) = favorites.any { it.packageName == app.packageName }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val weatherRepository = WeatherRepository()
    private val settings = SettingsStore(application)
    val sleepTimer = SleepTimer(application)
    val mediaSessions = MediaSessions(application)

    private val installedApps: Flow<List<AppEntry>> = repository.changes()
        .onStart { emit(Unit) }
        .conflate()
        .map { repository.loadApps() }

    /** null — список ещё загружается */
    val apps: StateFlow<HomeApps?> = combine(installedApps, settings.appLayout, ::arrange)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val city: StateFlow<City?> = settings.city
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val homePrefs: StateFlow<HomePrefs> = settings.homePrefs
        .stateIn(viewModelScope, SharingStarted.Eagerly, HomePrefs())

    fun updateHomePrefs(transform: (HomePrefs) -> HomePrefs) = settings.updateHomePrefs(transform)

    val network: StateFlow<NetworkKind> = application.networkKind()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NetworkKind.None)

    /** Что играет в других приложениях; null — ничего или нет доступа */
    val nowPlaying: StateFlow<NowPlaying?> = mediaSessions.nowPlaying()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Выдан ли доступ к медиасессиям (через слушатель уведомлений) */
    val mediaAccess: StateFlow<Boolean> = mediaSessions.accessChanges()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Установленная Яндекс Музыка, включая скрытую с главного экрана */
    val yandexMusic: StateFlow<AppEntry?> = apps
        .map { home -> home?.let { (it.favorites + it.others + it.hidden).findYandexMusic() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Время срабатывания таймера сна; 0 — выключен */
    val sleepTimerEnd: StateFlow<Long> = settings.sleepTimerEnd
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    /** null — погода ещё не загружена; при ошибке сети остаётся последнее значение */
    val weather: StateFlow<Weather?> = settings.city
        .flatMapLatest(::weatherUpdates)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectCity(city: City) = settings.setCity(city)

    /** Пустой список и при отсутствии совпадений, и при ошибке сети */
    suspend fun searchCities(query: String): List<City> =
        runCatching { weatherRepository.searchCities(query) }
            .onFailure { if (it !is CancellationException) Log.w(TAG, "Поиск города не удался", it) }
            .getOrDefault(emptyList())

    fun toggleFavorite(app: AppEntry) = settings.updateAppLayout { layout ->
        val favorites = if (app.packageName in layout.favorites) {
            layout.favorites - app.packageName
        } else {
            layout.favorites + app.packageName
        }
        layout.copy(favorites = favorites)
    }

    fun hide(app: AppEntry) = settings.updateAppLayout { layout ->
        layout.copy(hidden = layout.hidden + app.packageName, favorites = layout.favorites - app.packageName)
    }

    fun unhide(app: AppEntry) = settings.updateAppLayout { layout ->
        layout.copy(hidden = layout.hidden - app.packageName)
    }

    /**
     * Сдвигает приложение внутри его раздела на [offset] позиций
     * (±1 — влево/вправо, ±число колонок — вверх/вниз).
     */
    fun move(app: AppEntry, offset: Int) {
        val installed = apps.value?.let { it.favorites + it.others + it.hidden } ?: return
        settings.updateAppLayout { layout ->
            // Раскладываем заново по свежей раскладке: apps.value может отставать при быстрых нажатиях
            val current = arrange(installed, layout)
            val inFavorites = current.isFavorite(app)
            val section = (if (inFavorites) current.favorites else current.others).map { it.packageName }
            val from = section.indexOf(app.packageName)
            if (from < 0) return@updateAppLayout layout
            val to = (from + offset).coerceIn(0, section.lastIndex)
            val reordered = section.toMutableList().apply { add(to, removeAt(from)) }
            if (inFavorites) layout.copy(favorites = reordered) else layout.copy(order = reordered)
        }
    }

    private fun arrange(installed: List<AppEntry>, layout: AppLayout): HomeApps {
        val byPackage = installed.associateBy { it.packageName }
        val favorites = layout.favorites
            .filter { it !in layout.hidden }
            .mapNotNull { byPackage[it] }
        val favoritePackages = favorites.mapTo(HashSet()) { it.packageName }
        val rank = layout.order.withIndex().associate { (index, packageName) -> packageName to index }
        // Сортировка стабильная: приложения без позиции остаются по алфавиту в конце
        val others = installed
            .filter { it.packageName !in layout.hidden && it.packageName !in favoritePackages }
            .sortedBy { rank[it.packageName] ?: Int.MAX_VALUE }
        val hidden = installed.filter { it.packageName in layout.hidden }
        return HomeApps(favorites, others, hidden)
    }

    private fun weatherUpdates(city: City): Flow<Weather> = flow {
        while (true) {
            val result = runCatching { weatherRepository.current(city) }
            result.getOrNull()?.let { emit(it) }
            delay(if (result.isSuccess) WEATHER_REFRESH_MS else WEATHER_RETRY_MS)
        }
    }

    private companion object {
        const val TAG = "HomeViewModel"
        const val WEATHER_REFRESH_MS = 30 * 60_000L
        const val WEATHER_RETRY_MS = 20_000L
    }
}
