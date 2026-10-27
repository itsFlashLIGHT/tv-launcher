package com.flashlight.tvlauncher.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Волнистый круг («печенька» Material 3): радиус колеблется по косинусу.
 * [lobes] — число волн, [depth] — их глубина в долях радиуса, [rotation] — поворот в радианах.
 */
fun scallopPath(center: Offset, radius: Float, lobes: Int, depth: Float, rotation: Float = 0f): Path {
    val path = Path()
    val steps = 360
    for (i in 0..steps) {
        val t = 2 * PI.toFloat() * i / steps
        val r = radius * (1 - depth + depth * cos(lobes * (t - rotation)))
        val x = center.x + r * sin(t)
        val y = center.y - r * cos(t)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

class ScallopShape(
    private val lobes: Int,
    private val depth: Float,
    private val rotation: Float = 0f,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(scallopPath(size.center, size.minDimension / 2f, lobes, depth, rotation))
}
