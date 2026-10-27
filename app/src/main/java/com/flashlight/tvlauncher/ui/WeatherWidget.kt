package com.flashlight.tvlauncher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flashlight.tvlauncher.data.Weather
import com.flashlight.tvlauncher.ui.theme.ScallopShape
import kotlin.math.PI

private val WeatherShape = ScallopShape(lobes = 4, depth = 0.07f, rotation = PI.toFloat() / 4 + 0.15f)

/**
 * Погода в стиле Pixel: фигура-«печенька», температура справа сверху,
 * иконка слева снизу. Пока данных нет, показывает «--°». По OK — [onClick].
 */
@Composable
fun WeatherWidget(
    weather: Weather?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.White,
    textColor: Color = Color(0xFF1D1B20),
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(WeatherShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = containerColor,
            focusedContainerColor = containerColor,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, MaterialTheme.colorScheme.primary), shape = WeatherShape),
        ),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), 16.dp),
        ),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val side = minOf(maxWidth, maxHeight)
            Text(
                text = weather?.let { "${it.temperature}°" } ?: "--°",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = side * 0.17f, end = side * 0.13f),
                color = textColor,
                fontSize = with(LocalDensity.current) { (side * 0.30f).toSp() },
                fontWeight = FontWeight.Normal,
                maxLines = 1,
            )
            if (weather != null) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = side * 0.14f, bottom = side * 0.14f)
                        .size(side * 0.38f),
                ) {
                    WeatherIcon(weather.condition, weather.isDay)
                }
            }
        }
    }
}
