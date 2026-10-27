package com.flashlight.tvlauncher.ui.theme

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.flashlight.tvlauncher.R

/**
 * Встроенные обои. Каждые задают цвета пятен и «семя» — цвет, из которого
 * строится вся тема лаунчера (см. [colorSchemeFor]).
 */
enum class WallpaperPreset(
    @StringRes val title: Int,
    val seed: Color,
    /** Насыщенность темы: 1 — яркая, ближе к 0 — почти серая */
    val chroma: Float,
    val base: Color,
    val blobs: List<Color>,
) {
    Twilight(
        R.string.wallpaper_twilight,
        seed = Color(0xFF8E7CC3), chroma = 1f, base = Color(0xFF0F0D1A),
        blobs = listOf(Color(0xFF5B45B0), Color(0xFF1F7A72), Color(0xFF8A3D4F)),
    ),
    Ocean(
        R.string.wallpaper_ocean,
        seed = Color(0xFF4FA3D9), chroma = 1f, base = Color(0xFF08111E),
        blobs = listOf(Color(0xFF1E5AA8), Color(0xFF0E8C8C), Color(0xFF2B3F8F)),
    ),
    Forest(
        R.string.wallpaper_forest,
        seed = Color(0xFF6FBF73), chroma = 0.8f, base = Color(0xFF0B130E),
        blobs = listOf(Color(0xFF2E7D4F), Color(0xFF7A8C2B), Color(0xFF1F5E5B)),
    ),
    Sunset(
        R.string.wallpaper_sunset,
        seed = Color(0xFFFF8A65), chroma = 1f, base = Color(0xFF1A0E0C),
        blobs = listOf(Color(0xFFB5452F), Color(0xFFC27A1A), Color(0xFF7A2E5C)),
    ),
    Graphite(
        R.string.wallpaper_graphite,
        seed = Color(0xFF90A4AE), chroma = 0.15f, base = Color(0xFF0F1113),
        blobs = listOf(Color(0xFF3A4750), Color(0xFF54606B), Color(0xFF2B3238)),
    ),
    ;

    companion object {
        fun fromName(name: String): WallpaperPreset = entries.find { it.name == name } ?: Twilight
    }
}
