package com.flashlight.tvlauncher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.flashlight.tvlauncher.R
import com.flashlight.tvlauncher.data.NowPlaying
import com.flashlight.tvlauncher.data.Weather
import com.flashlight.tvlauncher.ui.theme.LocalLauncherAccents
import com.flashlight.tvlauncher.ui.theme.PillShape
import com.flashlight.tvlauncher.ui.theme.WallpaperPreset
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.random.Random

private const val DRIFT_INTERVAL_MS = 60_000L
private const val NIGHT_CHECK_MS = 60_000L
private const val NIGHT_START_HOUR = 23
private const val NIGHT_END_HOUR = 7
private const val NIGHT_ALPHA = 0.35f

/**
 * Заставка: приглушённые обои, крупные часы и погода, под ними — играющий трек.
 * Блок раз в минуту плавно смещается в случайную точку, чтобы картинка не выжигала экран.
 * С [nightDim] ночью (23:00–7:00) всё становится заметно темнее.
 */
@Composable
fun ScreensaverContent(
    preset: WallpaperPreset,
    weather: Weather?,
    showWeather: Boolean,
    nowPlaying: NowPlaying?,
    nightDim: Boolean,
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

    val isNight by produceState(isNightNow()) {
        while (true) {
            value = isNightNow()
            delay(NIGHT_CHECK_MS)
        }
    }
    val brightness by animateFloatAsState(if (nightDim && isNight) NIGHT_ALPHA else 1f, tween(3_000), label = "night")

    // Фон чёрный, поэтому прозрачность содержимого и есть яркость
    Box(modifier.fillMaxSize().background(Color.Black)) {
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = brightness }) {
            Wallpaper(preset, animated = true, Modifier.graphicsLayer { alpha = 0.45f })
            BoxWithConstraints(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .align(Alignment.Center)
                        .offset(x = maxWidth * 0.2f * driftX, y = maxHeight * 0.12f * driftY),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnalogClockWidget(Modifier.size(360.dp))
                        if (showWeather && weather != null) {
                            Spacer(Modifier.width(56.dp))
                            WeatherWidget(weather, onClick = {}, Modifier.size(240.dp))
                        }
                    }
                    // Место под трек держим всегда, чтобы часы не прыгали, когда музыка включается
                    Box(Modifier.padding(top = 40.dp).height(88.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.animation.AnimatedVisibility(nowPlaying != null && nowPlaying.isPlaying, enter = fadeIn(), exit = fadeOut()) {
                            nowPlaying?.let { ScreensaverTrack(it) }
                        }
                    }
                }
            }
        }
    }
}

/** Плашка трека: только показывает, управлять им с заставки нельзя */
@Composable
private fun ScreensaverTrack(nowPlaying: NowPlaying) {
    val accent = LocalLauncherAccents.current.clockHourHand
    Row(
        Modifier
            .clip(PillShape)
            .background(Color.White.copy(alpha = 0.12f))
            .padding(start = 12.dp, end = 32.dp, top = 12.dp, bottom = 12.dp)
            .widthIn(max = 640.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(32.dp)).background(accent.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center,
        ) {
            val art = nowPlaying.art
            if (art != null) {
                Image(art, contentDescription = null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(painterResource(R.drawable.ic_music_note), null, Modifier.size(32.dp), tint = Color.White)
            }
        }
        Column(Modifier.padding(start = 20.dp)) {
            Text(
                text = nowPlaying.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(nowPlaying.artist, nowPlaying.appLabel).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun isNightNow(): Boolean {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return hour >= NIGHT_START_HOUR || hour < NIGHT_END_HOUR
}
