package com.flashlight.tvlauncher.ui

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

/** Встроенные обои: медленно дрейфующие цветные пятна на тёмном фоне; без [animated] — неподвижные */
@Composable
fun Wallpaper(animated: Boolean, modifier: Modifier = Modifier) {
    if (animated) {
        val transition = rememberInfiniteTransition(label = "wallpaper")
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Reverse),
            label = "phase",
        )
        WallpaperCanvas({ phase }, modifier)
    } else {
        WallpaperCanvas({ 0.5f }, modifier)
    }
}

/** [phaseProvider] читается внутри отрисовки: анимация перерисовывает холст без рекомпозиции */
@Composable
private fun WallpaperCanvas(phaseProvider: () -> Float, modifier: Modifier) {
    Canvas(modifier.fillMaxSize()) {
        val phase = phaseProvider()
        val w = size.width
        val h = size.height
        drawRect(Color(0xFF0F0D1A))
        drawBlob(Color(0xFF5B45B0), Offset(w * (0.15f + 0.2f * phase), h * 0.2f), w * 0.55f)
        drawBlob(Color(0xFF1F7A72), Offset(w * (0.9f - 0.15f * phase), h * 0.85f), w * 0.5f)
        drawBlob(Color(0xFF8A3D4F), Offset(w * 0.6f, h * (0.1f + 0.3f * phase)), w * 0.35f)
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
