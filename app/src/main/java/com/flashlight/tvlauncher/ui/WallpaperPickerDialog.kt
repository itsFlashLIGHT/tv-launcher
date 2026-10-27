package com.flashlight.tvlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ListItem
import androidx.tv.material3.Text
import com.flashlight.tvlauncher.R
import com.flashlight.tvlauncher.ui.theme.WallpaperPreset

/** Выбор встроенных обоев; тема лаунчера перекрашивается вслед за ними */
@Composable
fun WallpaperPickerDialog(
    current: WallpaperPreset,
    onSelect: (WallpaperPreset) -> Unit,
    onDismiss: () -> Unit,
) {
    val currentFocus = remember { FocusRequester() }

    LauncherDialog(title = stringResource(R.string.wallpaper_picker_title), onDismiss = onDismiss) {
        WallpaperPreset.entries.forEach { preset ->
            ListItem(
                selected = preset == current,
                onClick = { onSelect(preset) },
                headlineContent = { Text(stringResource(preset.title)) },
                leadingContent = {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(preset.blobs)),
                    )
                },
                modifier = if (preset == current) Modifier.focusRequester(currentFocus) else Modifier,
            )
        }
    }

    LaunchedEffect(Unit) {
        withFrameNanos {}
        runCatching { currentFocus.requestFocus() }
    }
}
