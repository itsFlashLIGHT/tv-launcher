package com.flashlight.tvlauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.flashlight.tvlauncher.data.WeatherCondition

private val SunLight = Color(0xFFFFD54F)
private val SunDark = Color(0xFFFF9800)
private val MoonColor = Color(0xFFFFE082)
private val CloudFill = Color(0xFFF1F5FB)
private val CloudOutline = Color(0xFF7FA7E0)
private val RainColor = Color(0xFF4A8FE7)
private val SnowColor = Color(0xFF9CC3F5)
private val FogColor = Color(0xFF9AA4B2)

/** Цветная иконка погоды, нарисованная кодом; занимает весь [modifier] */
@Composable
fun WeatherIcon(condition: WeatherCondition, isDay: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        val s = size.minDimension
        when (condition) {
            WeatherCondition.Clear ->
                if (isDay) drawSun(Offset(s * 0.5f, s * 0.5f), s * 0.36f) else drawMoon(Offset(s * 0.5f, s * 0.5f), s * 0.38f)
            WeatherCondition.PartlyCloudy -> {
                if (isDay) drawSun(Offset(s * 0.38f, s * 0.36f), s * 0.28f) else drawMoon(Offset(s * 0.38f, s * 0.36f), s * 0.28f)
                drawCloud(Rect(s * 0.18f, s * 0.42f, s * 0.98f, s * 0.92f))
            }
            WeatherCondition.Cloudy -> drawCloud(Rect(s * 0.04f, s * 0.22f, s * 0.96f, s * 0.80f))
            WeatherCondition.Fog -> {
                drawCloud(Rect(s * 0.08f, s * 0.08f, s * 0.92f, s * 0.58f))
                for (i in 0..2) {
                    val y = s * (0.68f + i * 0.12f)
                    drawLine(FogColor, Offset(s * (0.14f + i * 0.06f), y), Offset(s * (0.86f - i * 0.06f), y), s * 0.06f, StrokeCap.Round)
                }
            }
            WeatherCondition.Drizzle -> {
                drawCloud(Rect(s * 0.08f, s * 0.06f, s * 0.92f, s * 0.60f))
                drawDrops(s, count = 2)
            }
            WeatherCondition.Rain -> {
                drawCloud(Rect(s * 0.08f, s * 0.06f, s * 0.92f, s * 0.60f))
                drawDrops(s, count = 3)
            }
            WeatherCondition.Snow -> {
                drawCloud(Rect(s * 0.08f, s * 0.06f, s * 0.92f, s * 0.60f))
                for (i in 0..2) {
                    drawCircle(SnowColor, s * 0.055f, Offset(s * (0.3f + i * 0.2f), s * (0.76f + (i % 2) * 0.12f)))
                }
            }
            WeatherCondition.Thunderstorm -> {
                drawCloud(Rect(s * 0.08f, s * 0.06f, s * 0.92f, s * 0.60f))
                drawBolt(s)
            }
        }
    }
}

private fun DrawScope.drawSun(center: Offset, radius: Float) {
    drawCircle(Brush.radialGradient(listOf(SunLight, SunDark), center, radius * 1.2f), radius, center)
}

/** Полумесяц: круг минус смещённый круг */
private fun DrawScope.drawMoon(center: Offset, radius: Float) {
    val disc = Path().apply { addOval(Rect(center, radius)) }
    val bite = Path().apply { addOval(Rect(center + Offset(radius * 0.55f, -radius * 0.35f), radius * 0.85f)) }
    drawPath(Path.combine(PathOperation.Difference, disc, bite), MoonColor)
}

/** Облако: объединение двух кругов и скруглённого основания, с контуром */
private fun DrawScope.drawCloud(bounds: Rect) {
    val w = bounds.width
    val h = bounds.height
    val base = Path().apply {
        addRoundRect(RoundRect(bounds.left, bounds.top + h * 0.45f, bounds.right, bounds.bottom, CornerRadius(h * 0.275f)))
    }
    val small = Path().apply { addOval(Rect(Offset(bounds.left + w * 0.33f, bounds.top + h * 0.52f), h * 0.36f)) }
    val big = Path().apply { addOval(Rect(Offset(bounds.left + w * 0.62f, bounds.top + h * 0.46f), h * 0.44f)) }
    val cloud = Path.combine(PathOperation.Union, Path.combine(PathOperation.Union, base, small), big)
    drawPath(cloud, CloudFill)
    drawPath(cloud, CloudOutline, style = Stroke(width = h * 0.07f))
}

private fun DrawScope.drawDrops(s: Float, count: Int) {
    for (i in 0 until count) {
        val x = s * (0.5f + (i - (count - 1) / 2f) * 0.22f)
        drawLine(RainColor, Offset(x, s * 0.70f), Offset(x - s * 0.06f, s * 0.90f), s * 0.06f, StrokeCap.Round)
    }
}

private fun DrawScope.drawBolt(s: Float) {
    val bolt = Path().apply {
        moveTo(s * 0.56f, s * 0.58f)
        lineTo(s * 0.40f, s * 0.80f)
        lineTo(s * 0.52f, s * 0.80f)
        lineTo(s * 0.44f, s * 0.98f)
        lineTo(s * 0.66f, s * 0.72f)
        lineTo(s * 0.54f, s * 0.72f)
        lineTo(s * 0.62f, s * 0.58f)
        close()
    }
    drawPath(bolt, SunDark)
}
