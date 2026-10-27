package com.flashlight.tvlauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.flashlight.tvlauncher.ui.theme.scallopPath
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Аналоговые часы в стиле Pixel: волнистый циферблат, дата сверху,
 * день недели по дуге слева. Размер задаётся через [modifier].
 */
@Composable
fun AnalogClockWidget(
    modifier: Modifier = Modifier,
    // Светлая палитра Material 3 (baseline light): белый циферблат на тёмных обоях
    faceColor: Color = Color.White,
    numeralColor: Color = Color(0xFF49454F),
    tickColor: Color = Color(0xFFCAC4D0),
    labelColor: Color = Color(0xFF6750A4),
    hourHandColor: Color = Color(0xFF6750A4),
    minuteHandColor: Color = Color(0xFF9A82DB),
    secondHandColor: Color = Color(0xFFE8735C),
) {
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(1_000L - System.currentTimeMillis() % 1_000L)
        }
    }
    val locale = Locale.getDefault()
    val dateFormat = remember(locale) { DateTimeFormatter.ofPattern("d MMMM", locale) }
    val dayFormat = remember(locale) { DateTimeFormatter.ofPattern("EEEE", locale) }
    val measurer = rememberTextMeasurer(cacheSize = 64)

    Canvas(modifier) {
        val radius = min(size.width, size.height) / 2f

        drawPath(scallopPath(center, radius, lobes = 12, depth = 0.035f), faceColor)

        val labelStyle = TextStyle(
            color = labelColor,
            fontSize = (radius * 0.13f).toSp(),
            fontWeight = FontWeight.Medium,
        )
        val labelRadius = radius * 0.80f
        val dateArc = drawArcText(measurer, now.format(dateFormat), labelStyle, centerAngle = 0f, radius = labelRadius)
        val dayArc = drawArcText(
            measurer,
            now.format(dayFormat).replaceFirstChar { it.titlecase(locale) },
            labelStyle,
            centerAngle = -62f,
            radius = labelRadius,
        )

        // Минутные риски, кроме часовых отметок и мест под подписями
        for (minute in 0 until 60) {
            if (minute % 5 == 0) continue
            val angle = normalize(minute * 6f)
            if (angle in dateArc.widen(3f) || angle in dayArc.widen(3f)) continue
            val rad = angle.toRadians()
            drawLine(
                color = tickColor,
                start = center + polar(rad, radius * 0.70f),
                end = center + polar(rad, radius * 0.74f),
                strokeWidth = radius * 0.012f,
                cap = StrokeCap.Round,
            )
        }

        val numeralStyle = TextStyle(color = numeralColor, fontSize = (radius * 0.17f).toSp())
        for (hour in 1..12) {
            val layout = measurer.measure(hour.toString(), numeralStyle)
            val position = center + polar((hour * 30f).toRadians(), radius * 0.55f)
            drawText(layout, topLeft = position - Offset(layout.size.width / 2f, layout.size.height / 2f))
        }

        val hourAngle = (now.hour % 12 + now.minute / 60f) * 30f
        val minuteAngle = (now.minute + now.second / 60f) * 6f
        drawHand(hourAngle, length = radius * 0.38f, width = radius * 0.075f, color = hourHandColor, tail = radius * 0.05f)
        drawHand(minuteAngle, length = radius * 0.58f, width = radius * 0.055f, color = minuteHandColor, tail = radius * 0.05f)
        rotate(now.second * 6f, pivot = center) {
            drawLine(
                color = secondHandColor,
                start = center + Offset(0f, radius * 0.12f),
                end = center - Offset(0f, radius * 0.66f),
                strokeWidth = radius * 0.012f,
                cap = StrokeCap.Round,
            )
        }
        drawCircle(secondHandColor, radius = radius * 0.035f, center = center)
    }
}

/**
 * Рисует текст по дуге, буквы идут по часовой стрелке.
 * Углы в градусах от 12 часов по часовой; возвращает занятый диапазон углов.
 */
private fun DrawScope.drawArcText(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    centerAngle: Float,
    radius: Float,
): ClosedFloatingPointRange<Float> {
    val layouts = text.map { measurer.measure(it.toString(), style) }
    val sweeps = layouts.map { Math.toDegrees(it.size.width.toDouble() / radius).toFloat() }
    val start = centerAngle - sweeps.sum() / 2
    var angle = start
    layouts.forEachIndexed { i, layout ->
        rotate(angle + sweeps[i] / 2, pivot = center) {
            drawText(
                layout,
                topLeft = Offset(center.x - layout.size.width / 2f, center.y - radius - layout.size.height / 2f),
            )
        }
        angle += sweeps[i]
    }
    return start..angle
}

/** Стрелка-пилюля, повёрнутая на [angle] градусов от 12 часов */
private fun DrawScope.drawHand(angle: Float, length: Float, width: Float, color: Color, tail: Float) {
    rotate(angle, pivot = center) {
        drawRoundRect(
            color = color,
            topLeft = Offset(center.x - width / 2, center.y - length),
            size = Size(width, length + tail),
            cornerRadius = CornerRadius(width / 2),
        )
    }
}

/** Точка на окружности: угол в радианах от 12 часов по часовой */
private fun polar(angle: Float, r: Float) = Offset(r * sin(angle), -r * cos(angle))

private fun Float.toRadians() = this * PI.toFloat() / 180f

/** Угол в диапазон -180..180 */
private fun normalize(angle: Float) = ((angle + 180f) % 360f + 360f) % 360f - 180f

private fun ClosedFloatingPointRange<Float>.widen(by: Float) = (start - by)..(endInclusive + by)
