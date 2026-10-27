package com.flashlight.tvlauncher.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/** Овал (stadium) — основная форма всех карточек и виджетов */
val PillShape = RoundedCornerShape(percent = 50)

/** Акценты для светлых виджетов (белый циферблат часов): тёмные тона того же оттенка */
@Immutable
data class LauncherAccents(
    val clockLabel: Color,
    val clockHourHand: Color,
    val clockMinuteHand: Color,
    val clockSecondHand: Color,
)

val LocalLauncherAccents = staticCompositionLocalOf { accentsFor(WallpaperPreset.Twilight) }

@Composable
fun TvLauncherTheme(
    preset: WallpaperPreset = WallpaperPreset.Twilight,
    content: @Composable () -> Unit,
) {
    val colorScheme = remember(preset) { colorSchemeFor(preset) }
    val accents = remember(preset) { accentsFor(preset) }
    CompositionLocalProvider(LocalLauncherAccents provides accents) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}

/**
 * Тёмная схема Material из цвета обоев: оттенок берётся у семени,
 * светлота фиксирована по ролям (упрощённые «тона» Material You).
 */
fun colorSchemeFor(preset: WallpaperPreset): ColorScheme {
    val tones = Tones(preset)
    return darkColorScheme(
        primary = tones.primary(0.75f, 0.80f),
        onPrimary = tones.primary(0.50f, 0.18f),
        secondary = tones.primary(0.35f, 0.75f),
        tertiary = tones.tertiary(0.70f, 0.78f),
        background = preset.base,
        surface = tones.primary(0.25f, 0.13f),
        onSurface = tones.primary(0.30f, 0.92f),
        surfaceVariant = tones.primary(0.20f, 0.21f),
        onSurfaceVariant = tones.primary(0.15f, 0.80f),
        border = tones.primary(0.75f, 0.80f),
    )
}

fun accentsFor(preset: WallpaperPreset): LauncherAccents {
    val tones = Tones(preset)
    return LauncherAccents(
        clockLabel = tones.primary(0.45f, 0.45f),
        clockHourHand = tones.primary(0.45f, 0.45f),
        clockMinuteHand = tones.primary(0.55f, 0.70f),
        clockSecondHand = tones.tertiary(0.75f, 0.62f),
    )
}

private class Tones(private val preset: WallpaperPreset) {
    private val hue: Float = FloatArray(3).also { ColorUtils.colorToHSL(preset.seed.toArgb(), it) }[0]

    fun primary(saturation: Float, lightness: Float) = hsl(hue, saturation, lightness)

    /** Третичный цвет — сдвиг оттенка на 60°, как у tertiary в Material */
    fun tertiary(saturation: Float, lightness: Float) = hsl((hue + 60f) % 360f, saturation, lightness)

    private fun hsl(h: Float, s: Float, l: Float) =
        Color(ColorUtils.HSLToColor(floatArrayOf(h, (s * preset.chroma).coerceIn(0f, 1f), l)))
}
