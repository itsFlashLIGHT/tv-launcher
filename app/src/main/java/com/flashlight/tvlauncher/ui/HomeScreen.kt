package com.flashlight.tvlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flashlight.tvlauncher.HomeApps
import com.flashlight.tvlauncher.HomeViewModel
import com.flashlight.tvlauncher.R
import com.flashlight.tvlauncher.data.AppEntry
import com.flashlight.tvlauncher.data.launchApp
import com.flashlight.tvlauncher.data.openAppInfo
import com.flashlight.tvlauncher.data.uninstallApp
import com.flashlight.tvlauncher.ui.theme.PillShape

private const val GRID_COLUMNS = 5

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val weather by viewModel.weather.collectAsStateWithLifecycle()
    val city by viewModel.city.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var cityPickerOpen by remember { mutableStateOf(false) }
    var menuApp by remember { mutableStateOf<AppEntry?>(null) }
    var hiddenAppsOpen by remember { mutableStateOf(false) }
    var movingKey by remember { mutableStateOf<String?>(null) }
    // Увеличивается, когда фокус надо вернуть на первое приложение (например, сфокусированное скрыли)
    var refocusToken by remember { mutableIntStateOf(0) }

    // Домашний экран: «Назад» никуда не ведёт, а в режиме перемещения — завершает его
    BackHandler {}
    BackHandler(enabled = movingKey != null) { movingKey = null }

    if (cityPickerOpen) {
        CityPickerDialog(
            currentCity = city,
            onSearch = viewModel::searchCities,
            onSelect = {
                viewModel.selectCity(it)
                cityPickerOpen = false
            },
            onDismiss = { cityPickerOpen = false },
        )
    }

    menuApp?.let { app ->
        AppActionsDialog(
            app = app,
            isFavorite = apps?.isFavorite(app) == true,
            onAction = { action ->
                menuApp = null
                when (action) {
                    AppAction.Move -> movingKey = app.key
                    AppAction.ToggleFavorite -> viewModel.toggleFavorite(app)
                    AppAction.Hide -> {
                        viewModel.hide(app)
                        refocusToken++
                    }
                    AppAction.Info -> context.openAppInfo(app)
                    AppAction.Uninstall -> context.uninstallApp(app)
                }
            },
            onDismiss = { menuApp = null },
        )
    }

    val hiddenApps = apps?.hidden.orEmpty()
    if (hiddenAppsOpen && hiddenApps.isNotEmpty()) {
        HiddenAppsDialog(
            apps = hiddenApps,
            onUnhide = viewModel::unhide,
            onDismiss = { hiddenAppsOpen = false },
        )
    }
    LaunchedEffect(hiddenApps.isEmpty()) {
        // Вернули последнее скрытое приложение: кнопка «Скрытые» исчезла вместе с фокусом
        if (hiddenAppsOpen && hiddenApps.isEmpty()) {
            hiddenAppsOpen = false
            refocusToken++
        }
    }

    Box(Modifier.fillMaxSize()) {
        Wallpaper()
        // Отступы 48/27 dp — безопасная зона телевизора (overscan)
        Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 27.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnalogClockWidget(Modifier.size(190.dp))
                Spacer(Modifier.width(28.dp))
                WeatherWidget(weather, onClick = { cityPickerOpen = true }, Modifier.size(170.dp))
            }
            Spacer(Modifier.height(12.dp))
            val home = apps
            if (home != null && home.favorites.isEmpty() && home.others.isEmpty() && home.hidden.isEmpty()) {
                Text(
                    text = stringResource(R.string.apps_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (home != null) {
                AppGrid(
                    apps = home,
                    movingKey = movingKey,
                    refocusToken = refocusToken,
                    onOpen = context::launchApp,
                    onMenu = { menuApp = it },
                    onMove = viewModel::move,
                    onMoveDone = { movingKey = null },
                    onShowHidden = { hiddenAppsOpen = true },
                )
            }
        }

        if (movingKey != null) {
            Text(
                text = stringResource(R.string.move_hint),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 27.dp)
                    .clip(PillShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun AppGrid(
    apps: HomeApps,
    movingKey: String?,
    refocusToken: Int,
    onOpen: (AppEntry) -> Unit,
    onMenu: (AppEntry) -> Unit,
    onMove: (AppEntry, Int) -> Unit,
    onMoveDone: () -> Unit,
    onShowHidden: () -> Unit,
) {
    val firstItemFocus = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    val firstKey = (apps.favorites.firstOrNull() ?: apps.others.firstOrNull())?.key
    val hasFavorites = apps.favorites.isNotEmpty()

    // Сетка держит позицию по ключу первого видимого элемента; без этого при появлении
    // раздела «Избранное» его заголовок уезжал бы за верхний край
    LaunchedEffect(hasFavorites) { gridState.scrollToItem(0) }
    val movingApp = movingKey?.let { key -> (apps.favorites + apps.others).find { it.key == key } }

    val tile: @Composable LazyGridItemScope.(AppEntry) -> Unit = { app ->
        val focusModifier = if (app.key == firstKey) Modifier.focusRequester(firstItemFocus) else Modifier
        AppTile(
            app = app,
            onClick = { if (movingKey == null) onOpen(app) },
            onLongClick = { if (movingKey == null) onMenu(app) },
            modifier = focusModifier.animateItem(),
            moving = app.key == movingKey,
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        state = gridState,
        // В режиме перемещения стрелки двигают приложение, а не фокус
        modifier = Modifier.onPreviewKeyEvent { event ->
            val app = movingApp ?: return@onPreviewKeyEvent false
            when (event.key) {
                Key.DirectionLeft -> if (event.type == KeyEventType.KeyDown) onMove(app, -1)
                Key.DirectionRight -> if (event.type == KeyEventType.KeyDown) onMove(app, 1)
                Key.DirectionUp -> if (event.type == KeyEventType.KeyDown) onMove(app, -GRID_COLUMNS)
                Key.DirectionDown -> if (event.type == KeyEventType.KeyDown) onMove(app, GRID_COLUMNS)
                // Выходим по отпусканию OK: иначе KeyUp долетит до карточки и запустит приложение
                Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> if (event.type == KeyEventType.KeyUp) onMoveDone()
                else -> return@onPreviewKeyEvent false
            }
            true
        },
        // Запас по краям, чтобы увеличение и свечение фокуса не обрезались
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (hasFavorites) {
            sectionHeader("header_favorites") { stringResource(R.string.favorites_title) }
            appItems(apps.favorites, tile)
        }
        sectionHeader("header_apps") { stringResource(R.string.apps_title) }
        appItems(apps.others, tile)
        if (apps.hidden.isNotEmpty()) {
            item(key = "hidden_button", span = { GridItemSpan(maxLineSpan) }) {
                Box {
                    Surface(
                        onClick = onShowHidden,
                        shape = ClickableSurfaceDefaults.shape(PillShape),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.hidden_apps_button, apps.hidden.size),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(refocusToken) {
        withFrameNanos {}
        runCatching { firstItemFocus.requestFocus() }
    }
}

private fun LazyGridScope.sectionHeader(key: String, title: @Composable () -> String) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) {
        Text(
            text = title(),
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun LazyGridScope.appItems(apps: List<AppEntry>, tile: @Composable LazyGridItemScope.(AppEntry) -> Unit) {
    items(apps, key = { it.key }) { app -> tile(app) }
}
