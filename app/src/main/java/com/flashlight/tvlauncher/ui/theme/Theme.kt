package com.flashlight.tvlauncher.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/** Овал (stadium) — основная форма всех карточек и виджетов */
val PillShape = RoundedCornerShape(percent = 50)

private val LauncherColors = darkColorScheme(
    primary = Color(0xFFB9A6FF),
    onPrimary = Color(0xFF241A4D),
    secondary = Color(0xFF7FD1C7),
    tertiary = Color(0xFFFFB4A2),
    background = Color(0xFF0F0D1A),
    surface = Color(0xFF1E1B2E),
    onSurface = Color(0xFFE7E1F5),
    surfaceVariant = Color(0xFF2C2840),
    onSurfaceVariant = Color(0xFFCAC3DC),
    border = Color(0xFFB9A6FF),
)

@Composable
fun TvLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LauncherColors, content = content)
}
