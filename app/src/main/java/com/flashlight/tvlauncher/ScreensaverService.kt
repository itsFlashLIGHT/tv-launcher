package com.flashlight.tvlauncher

import android.service.dreams.DreamService
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.flashlight.tvlauncher.data.HomePrefs
import com.flashlight.tvlauncher.data.MediaSessions
import com.flashlight.tvlauncher.data.SettingsStore
import com.flashlight.tvlauncher.data.Weather
import com.flashlight.tvlauncher.data.WeatherRepository
import com.flashlight.tvlauncher.ui.ScreensaverContent
import com.flashlight.tvlauncher.ui.theme.TvLauncherTheme
import com.flashlight.tvlauncher.ui.theme.WallpaperPreset
import kotlinx.coroutines.flow.first

/**
 * Системная заставка Android (Daydream) с теми же часами, погодой и треком, что и встроенная.
 * Выбирается в настройках Android или командой
 * `adb shell settings put secure screensaver_components com.flashlight.tvlauncher/.ScreensaverService`.
 */
class ScreensaverService : DreamService(), LifecycleOwner, SavedStateRegistryOwner {
    // DreamService не даёт Compose владельцев жизненного цикла — заводим свои
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true

        val settings = SettingsStore(this)
        val weatherRepository = WeatherRepository()
        val mediaSessions = MediaSessions(this)
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ScreensaverService)
            setViewTreeSavedStateRegistryOwner(this@ScreensaverService)
            setContent {
                val prefs by settings.homePrefs.collectAsStateWithLifecycle(HomePrefs())
                val weather by produceState<Weather?>(null) {
                    value = runCatching { weatherRepository.current(settings.city.first()) }.getOrNull()
                }
                val nowPlaying by mediaSessions.nowPlaying().collectAsStateWithLifecycle(null)
                val preset = WallpaperPreset.fromName(prefs.wallpaper)
                TvLauncherTheme(preset) {
                    ScreensaverContent(
                        preset = preset,
                        weather = weather,
                        showWeather = prefs.showWeather,
                        nowPlaying = nowPlaying.takeIf { prefs.showNowPlaying },
                        nightDim = prefs.screensaverNightDim,
                    )
                }
            }
        }
        setContentView(view)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onDreamingStopped() {
        // Система может вызвать это уже после onDestroy — из DESTROYED переходить некуда
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
        super.onDreamingStopped()
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }
}
