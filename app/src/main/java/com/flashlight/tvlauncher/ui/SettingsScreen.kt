package com.flashlight.tvlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import com.flashlight.tvlauncher.R
import com.flashlight.tvlauncher.data.City
import com.flashlight.tvlauncher.data.HomePrefs
import com.flashlight.tvlauncher.data.openSystemSettings
import com.flashlight.tvlauncher.ui.theme.WallpaperPreset

private val COLUMN_CHOICES = listOf(4, 5, 6)

/** Минуты простоя до заставки; 0 — выключена */
private val SCREENSAVER_CHOICES = listOf(0, 5, 10, 15, 30)

/** Экран настроек лаунчера; «Назад» возвращает на главный экран */
@Composable
fun SettingsScreen(
    city: City?,
    prefs: HomePrefs,
    hiddenCount: Int,
    mediaAccess: Boolean,
    onRequestMediaAccess: () -> Unit,
    onPickCity: () -> Unit,
    onPickWallpaper: () -> Unit,
    onPreviewScreensaver: () -> Unit,
    onPrefsChange: ((HomePrefs) -> HomePrefs) -> Unit,
    onShowHidden: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull()
            .orEmpty()
    }
    val firstItemFocus = remember { FocusRequester() }

    BackHandler(onBack = onClose)

    Row(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 48.dp, vertical = 27.dp),
    ) {
        Column(Modifier.weight(0.4f).padding(top = 24.dp, end = 48.dp)) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.settings_version, version),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyColumn(
            Modifier.weight(0.6f),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item { SectionTitle(R.string.settings_section_weather) }
            item {
                ValueItem(
                    title = R.string.settings_city,
                    value = city?.name.orEmpty(),
                    onClick = onPickCity,
                    modifier = Modifier.focusRequester(firstItemFocus),
                )
            }

            item { SectionTitle(R.string.settings_section_look) }
            item {
                ValueItem(
                    title = R.string.settings_wallpaper,
                    value = stringResource(WallpaperPreset.fromName(prefs.wallpaper).title),
                    onClick = onPickWallpaper,
                )
            }
            item {
                SwitchItem(
                    title = R.string.settings_animated_wallpaper,
                    checked = prefs.animatedWallpaper,
                    description = R.string.settings_animated_wallpaper_hint,
                ) {
                    onPrefsChange { it.copy(animatedWallpaper = !it.animatedWallpaper) }
                }
            }

            item { SectionTitle(R.string.settings_section_home) }
            item {
                SwitchItem(R.string.settings_show_clock, prefs.showClock) {
                    onPrefsChange { it.copy(showClock = !it.showClock) }
                }
            }
            item {
                SwitchItem(R.string.settings_show_weather, prefs.showWeather) {
                    onPrefsChange { it.copy(showWeather = !it.showWeather) }
                }
            }
            item {
                SwitchItem(R.string.settings_show_quick_tiles, prefs.showQuickTiles) {
                    onPrefsChange { it.copy(showQuickTiles = !it.showQuickTiles) }
                }
            }
            item {
                ValueItem(R.string.settings_columns, prefs.columns.toString()) {
                    onPrefsChange { it.copy(columns = nextColumns(it.columns)) }
                }
            }

            item { SectionTitle(R.string.settings_section_now_playing) }
            item {
                SwitchItem(R.string.settings_show_now_playing, prefs.showNowPlaying) {
                    onPrefsChange { it.copy(showNowPlaying = !it.showNowPlaying) }
                }
            }
            item {
                ValueItem(
                    title = R.string.settings_media_access,
                    value = stringResource(if (mediaAccess) R.string.settings_media_access_on else R.string.settings_media_access_off),
                    onClick = onRequestMediaAccess,
                )
            }

            item { SectionTitle(R.string.settings_section_screensaver) }
            item {
                val minutes = prefs.screensaverMinutes
                ValueItem(
                    title = R.string.settings_screensaver_after,
                    value = if (minutes == 0) {
                        stringResource(R.string.settings_screensaver_off)
                    } else {
                        stringResource(R.string.settings_screensaver_minutes, minutes)
                    },
                ) {
                    onPrefsChange { it.copy(screensaverMinutes = nextChoice(SCREENSAVER_CHOICES, it.screensaverMinutes)) }
                }
            }
            item {
                SwitchItem(R.string.settings_screensaver_night_dim, prefs.screensaverNightDim) {
                    onPrefsChange { it.copy(screensaverNightDim = !it.screensaverNightDim) }
                }
            }
            item { ValueItem(R.string.settings_screensaver_preview, value = "", onClick = onPreviewScreensaver) }

            item { SectionTitle(R.string.settings_section_apps) }
            item {
                ValueItem(
                    title = R.string.settings_hidden_apps,
                    value = hiddenCount.toString(),
                    onClick = onShowHidden,
                    enabled = hiddenCount > 0,
                )
            }

            item { SectionTitle(R.string.settings_section_system) }
            item { ValueItem(R.string.settings_android, value = "", onClick = context::openSystemSettings) }
        }
    }

    LaunchedEffect(Unit) {
        withFrameNanos {}
        runCatching { firstItemFocus.requestFocus() }
    }
}

/** 4 → 5 → 6 → 4 */
private fun nextColumns(current: Int): Int = nextChoice(COLUMN_CHOICES, current)

/** Следующее значение по кругу; неизвестное текущее — первое значение */
private fun nextChoice(choices: List<Int>, current: Int): Int =
    choices[(choices.indexOf(current) + 1) % choices.size]

@Composable
private fun SectionTitle(@StringRes title: Int) {
    Text(
        text = stringResource(title),
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun ValueItem(
    @StringRes title: Int,
    value: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ListItem(
        selected = false,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        headlineContent = { Text(stringResource(title)) },
        trailingContent = { if (value.isNotEmpty()) Text(value) },
    )
}

@Composable
private fun SwitchItem(
    @StringRes title: Int,
    checked: Boolean,
    @StringRes description: Int? = null,
    onToggle: () -> Unit,
) {
    ListItem(
        selected = false,
        onClick = onToggle,
        headlineContent = { Text(stringResource(title)) },
        supportingContent = description?.let { { Text(stringResource(it)) } },
        // Переключатель только показывает состояние: нажатие обрабатывает вся строка
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
    )
}
