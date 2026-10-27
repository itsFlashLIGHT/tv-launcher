package com.flashlight.tvlauncher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.flashlight.tvlauncher.data.Weather
import com.flashlight.tvlauncher.ui.theme.WallpaperPreset
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val DRIFT_INTERVAL_MS = 60_000L

/**
 * Заставка: приглушённые обои, крупные часы и погода. Блок раз в минуту
 * плавно смещается в случайную точку, чтобы картинка не выжигала экран.
 */
@Composable
fun ScreensaverContent(
    preset: WallpaperPreset,
    weather: Weather?,
    showWeather: Boolean,
    modifier: Modifier = Modifier,
) {
    val drift by produceState(Offset.Zero) {
        while (true) {
            delay(DRIFT_INTERVAL_MS)
            value = Offset(Random.nextFloat() * 2 - 1, Random.nextFloat() * 2 - 1)
        }
    }
    val driftX by animateFloatAsState(drift.x, tween(4_000), label = "driftX")
    val driftY by animateFloatAsState(drift.y, tween(4_000), label = "driftY")

    Box(modifier.fillMaxSize().background(Color.Black)) {
        Wallpaper(preset, animated = true, Modifier.graphicsLayer { alpha = 0.45f })
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .align(Alignment.Center)
                    .offset(x = maxWidth * 0.2f * driftX, y = maxHeight * 0.15f * driftY),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnalogClockWidget(Modifier.size(360.dp))
                if (showWeather && weather != null) {
                    Spacer(Modifier.width(56.dp))
                    WeatherWidget(weather, onClick = {}, Modifier.size(240.dp))
                }
            }
        }
    }
}
