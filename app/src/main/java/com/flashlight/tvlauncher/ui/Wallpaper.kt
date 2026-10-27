package com.flashlight.tvlauncher.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.flashlight.tvlauncher.ui.theme.WallpaperPreset

/**
 * Встроенные обои [preset]: медленно дрейфующие цветные пятна на тёмном фоне; без [animated] — неподвижные.
 * При смене обоев цвета плавно перетекают.
 */
@Composable
fun Wallpaper(preset: WallpaperPreset, animated: Boolean, modifier: Modifier = Modifier) {
    val colorSpec = tween<Color>(durationMillis = 800)
    val base by animateColorAsState(preset.base, colorSpec, label = "base")
    val blob1 by animateColorAsState(preset.blobs[0], colorSpec, label = "blob1")
    val blob2 by animateColorAsState(preset.blobs[1], colorSpec, label = "blob2")
    val blob3 by animateColorAsState(preset.blobs[2], colorSpec, label = "blob3")
    val colors = { listOf(base, blob1, blob2, blob3) }

    if (animated) {
        val transition = rememberInfiniteTransition(label = "wallpaper")
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Reverse),
            label = "phase",
        )
        WallpaperCanvas(colors, { phase }, modifier)
    } else {
        WallpaperCanvas(colors, { 0.5f }, modifier)
    }
}

/** Цвета и фаза читаются внутри отрисовки: анимации перерисовывают холст без рекомпозиции */
@Composable
private fun WallpaperCanvas(colorsProvider: () -> List<Color>, phaseProvider: () -> Float, modifier: Modifier) {
    Canvas(modifier.fillMaxSize()) {
        val (base, blob1, blob2, blob3) = colorsProvider()
        val phase = phaseProvider()
        val w = size.width
        val h = size.height
        drawRect(base)
        drawBlob(blob1, Offset(w * (0.15f + 0.2f * phase), h * 0.2f), w * 0.55f)
        drawBlob(blob2, Offset(w * (0.9f - 0.15f * phase), h * 0.85f), w * 0.5f)
        drawBlob(blob3, Offset(w * 0.6f, h * (0.1f + 0.3f * phase)), w * 0.35f)
        // Затемнение снизу, чтобы подписи под карточками читались
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))))
    }
}

private fun DrawScope.drawBlob(color: Color, center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(listOf(color.copy(alpha = 0.75f), Color.Transparent), center, radius),
        radius = radius,
        center = center,
    )
}
