package com.flashlight.tvlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.flashlight.tvlauncher.HomeViewModel
import com.flashlight.tvlauncher.R
import com.flashlight.tvlauncher.data.AppEntry
import com.flashlight.tvlauncher.data.launchApp
import com.flashlight.tvlauncher.data.openAppInfo

private const val GRID_COLUMNS = 5

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Домашний экран: «Назад» никуда не ведёт
    BackHandler {}

    Box(Modifier.fillMaxSize()) {
        Wallpaper()
        // Отступы 48/27 dp — безопасная зона телевизора (overscan)
        Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 27.dp)) {
            ClockWidget()
            Spacer(Modifier.height(28.dp))
            Text(
                text = stringResource(R.string.apps_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            when (val list = apps) {
                null -> Unit
                emptyList<AppEntry>() -> Text(
                    text = stringResource(R.string.apps_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> AppGrid(
                    apps = list,
                    onOpen = context::launchApp,
                    onLongPress = context::openAppInfo,
                )
            }
        }
    }
}

@Composable
private fun AppGrid(
    apps: List<AppEntry>,
    onOpen: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit,
) {
    val firstItemFocus = remember { FocusRequester() }

    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        // Запас по краям, чтобы увеличение и свечение фокуса не обрезались
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        itemsIndexed(apps, key = { _, app -> app.key }) { index, app ->
            AppTile(
                app = app,
                onClick = { onOpen(app) },
                onLongClick = { onLongPress(app) },
                modifier = if (index == 0) Modifier.focusRequester(firstItemFocus) else Modifier,
            )
        }
    }

    LaunchedEffect(Unit) {
        withFrameNanos {}
        runCatching { firstItemFocus.requestFocus() }
    }
}
