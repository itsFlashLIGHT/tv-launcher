package com.flashlight.tvlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flashlight.tvlauncher.HomeApps
import com.flashlight.tvlauncher.HomeViewModel
import com.flashlight.tvlauncher.R
import com.flashlight.tvlauncher.data.AppEntry
import com.flashlight.tvlauncher.data.HomePrefs
import com.flashlight.tvlauncher.data.NetworkKind
import com.flashlight.tvlauncher.data.TvInput
import com.flashlight.tvlauncher.data.openPlayer
import com.flashlight.tvlauncher.data.togglePlayPause
import com.flashlight.tvlauncher.data.openNetworkSettings
import com.flashlight.tvlauncher.data.openSystemSettings
import com.flashlight.tvlauncher.data.openTvInput
import com.flashlight.tvlauncher.data.passthroughInputs
import com.flashlight.tvlauncher.data.launchApp
import com.flashlight.tvlauncher.data.openAppInfo
import com.flashlight.tvlauncher.data.uninstallApp
import com.flashlight.tvlauncher.ui.theme.PillShape
import com.flashlight.tvlauncher.ui.theme.TvLauncherTheme
import com.flashlight.tvlauncher.ui.theme.WallpaperPreset
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val prefs by viewModel.homePrefs.collectAsStateWithLifecycle()
    // Тема строится из цвета обоев: смена обоев перекрашивает весь лаунчер
    TvLauncherTheme(WallpaperPreset.fromName(prefs.wallpaper)) {
        HomeContent(viewModel)
    }
}

@Composable
private fun HomeContent(viewModel: HomeViewModel) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val weather by viewModel.weather.collectAsStateWithLifecycle()
    val city by viewModel.city.collectAsStateWithLifecycle()
    val prefs by viewModel.homePrefs.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val preset = WallpaperPreset.fromName(prefs.wallpaper)
    var settingsOpen by remember { mutableStateOf(false) }
    var wallpaperPickerOpen by remember { mutableStateOf(false) }
    var cityPickerOpen by remember { mutableStateOf(false) }
    val network by viewModel.network.collectAsStateWithLifecycle()
    val sleepTimerEnd by viewModel.sleepTimerEnd.collectAsStateWithLifecycle()
    // null — окно входов закрыто; иначе список, считанный в момент открытия
    var tvInputs by remember { mutableStateOf<List<TvInput>?>(null) }
    var sleepDialogOpen by remember { mutableStateOf(false) }
    var sleepAdminDialogOpen by remember { mutableStateOf(false) }
    var sleepAdminSystemDialogMissing by remember { mutableStateOf(false) }
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val mediaAccess by viewModel.mediaAccess.collectAsStateWithLifecycle()
    val yandexMusic by viewModel.yandexMusic.collectAsStateWithLifecycle()
    var mediaAccessDialogOpen by remember { mutableStateOf(false) }
    var mediaAccessSystemScreenMissing by remember { mutableStateOf(false) }
    var menuApp by remember { mutableStateOf<AppEntry?>(null) }
    var hiddenAppsOpen by remember { mutableStateOf(false) }
    var movingKey by remember { mutableStateOf<String?>(null) }
    // Куда вернуть фокус: последнее открытое приложение, null — первое в сетке.
    // Фокус переставляется, когда увеличивается refocusToken
    var focusKey by rememberSaveable { mutableStateOf<String?>(null) }
    var refocusToken by remember { mutableIntStateOf(0) }
    // Переставлять фокус при возвращении, только если уходили в приложение из сетки
    // (или это первый запуск); после плиток и виджетов фокус остаётся, где был
    var refocusOnReturn by remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Плавное появление при каждом возвращении на главный экран (из приложения или при запуске).
    // Ждём загрузки списка приложений, чтобы не показать пустой экран
    val appear = remember { Animatable(0f) }
    LaunchedEffect(apps != null) {
        if (apps == null) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            if (refocusOnReturn) {
                refocusOnReturn = false
                refocusToken++
            }
            appear.snapTo(0f)
            appear.animateTo(1f, tween(durationMillis = 450, easing = FastOutSlowInEasing))
        }
    }

    // «Нет сети» показываем не сразу: при старте и переподключении Wi-Fi сеть пропадает на секунду
    val offline by produceState(false, network) {
        value = if (network == NetworkKind.None) {
            delay(2_000)
            true
        } else {
            false
        }
    }

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
                        focusKey = null
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
            focusKey = null
            refocusToken++
        }
    }

    if (wallpaperPickerOpen) {
        WallpaperPickerDialog(
            current = preset,
            onSelect = { selected ->
                viewModel.updateHomePrefs { it.copy(wallpaper = selected.name) }
                wallpaperPickerOpen = false
            },
            onDismiss = { wallpaperPickerOpen = false },
        )
    }

    tvInputs?.let { inputs ->
        TvInputsDialog(
            inputs = inputs,
            onSelect = {
                tvInputs = null
                context.openTvInput(it)
            },
            onDismiss = { tvInputs = null },
        )
    }

    if (sleepDialogOpen) {
        SleepTimerDialog(
            active = sleepTimerEnd > System.currentTimeMillis(),
            onSelect = { minutes ->
                sleepDialogOpen = false
                when {
                    minutes == 0 -> viewModel.sleepTimer.cancel()
                    // Без прав администратора таймер бесполезен: сначала объясняем и просим их
                    !viewModel.sleepTimer.start(minutes) -> sleepAdminDialogOpen = true
                }
            },
            onDismiss = { sleepDialogOpen = false },
        )
    }

    if (sleepAdminDialogOpen) {
        val explanation = stringResource(R.string.sleep_admin_text)
        PermissionDialog(
            text = explanation,
            adbCommand = if (sleepAdminSystemDialogMissing) {
                "adb shell dpm set-active-admin ${viewModel.sleepTimer.adminComponent.flattenToShortString()}"
            } else {
                null
            },
            onAllow = {
                if (viewModel.sleepTimer.requestAdmin(context, explanation)) {
                    sleepAdminDialogOpen = false
                } else {
                    sleepAdminSystemDialogMissing = true
                }
            },
            onDismiss = { sleepAdminDialogOpen = false },
        )
    }

    if (mediaAccessDialogOpen) {
        PermissionDialog(
            text = stringResource(R.string.media_access_text),
            adbCommand = if (mediaAccessSystemScreenMissing) {
                "adb shell cmd notification allow_listener ${viewModel.mediaSessions.listenerComponent.flattenToString()}"
            } else {
                null
            },
            onAllow = {
                if (viewModel.mediaSessions.openAccessSettings(context)) {
                    mediaAccessDialogOpen = false
                } else {
                    mediaAccessSystemScreenMissing = true
                }
            },
            onDismiss = { mediaAccessDialogOpen = false },
        )
    }

    // Заставка по простою: таймер идёт, только пока главный экран на переднем плане
    // и не открыт ни один диалог (нажатия в диалогах сюда не доходят)
    var screensaverOn by remember { mutableStateOf(false) }
    var lastInteraction by remember { mutableLongStateOf(0L) }
    // Кнопка, которой закрыли заставку: её отпускание не должно дойти до карточки
    var swallowKeyUp by remember { mutableStateOf<Key?>(null) }
    val anyDialogOpen = cityPickerOpen || wallpaperPickerOpen || menuApp != null || hiddenAppsOpen ||
        tvInputs != null || sleepDialogOpen || sleepAdminDialogOpen || mediaAccessDialogOpen
    LaunchedEffect(lastInteraction, prefs.screensaverMinutes, anyDialogOpen, screensaverOn) {
        if (prefs.screensaverMinutes == 0 || anyDialogOpen || screensaverOn) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(prefs.screensaverMinutes * 60_000L)
            screensaverOn = true
        }
    }

    val widgets: @Composable () -> Unit = {
        Column {
            HomeWidgets(
                prefs = prefs,
                weatherContent = {
                    WeatherWidget(weather, onClick = { cityPickerOpen = true }, Modifier.size(170.dp), offline = offline)
                },
                nowPlayingContent = if (prefs.showNowPlaying && mediaAccess && (nowPlaying != null || yandexMusic != null)) {
                    {
                        val playing = nowPlaying
                        NowPlayingWidget(
                            nowPlaying = playing,
                            onOpenPlayer = { playing?.let(context::openPlayer) },
                            onPrevious = { playing?.controller?.transportControls?.skipToPrevious() },
                            onPlayPause = { playing?.controller?.togglePlayPause() },
                            onNext = { playing?.controller?.transportControls?.skipToNext() },
                            onOpenYandexMusic = yandexMusic?.let { app -> { context.launchApp(app) } },
                            modifier = Modifier.width(340.dp).height(170.dp),
                        )
                    }
                } else {
                    null
                },
                onOpenSettings = { settingsOpen = true },
            )
            if (prefs.showQuickTiles) {
                QuickTilesRow(
                    network = network,
                    sleepTimerEnd = sleepTimerEnd,
                    onNetwork = context::openNetworkSettings,
                    onInputs = { tvInputs = context.passthroughInputs() },
                    onSleepTimer = { sleepDialogOpen = true },
                    onSystemSettings = context::openSystemSettings,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }
            if (offline) {
                OfflineBanner(context::openNetworkSettings, Modifier.padding(top = 20.dp))
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                lastInteraction = System.currentTimeMillis()
                when {
                    screensaverOn && event.type == KeyEventType.KeyDown -> {
                        screensaverOn = false
                        swallowKeyUp = event.key
                        true
                    }
                    event.type == KeyEventType.KeyUp && event.key == swallowKeyUp -> {
                        swallowKeyUp = null
                        true
                    }
                    else -> screensaverOn
                }
            },
    ) {
        if (screensaverOn) {
            // Заставке нужен фокус: без сфокусированного элемента нажатия не доходят даже до корня
            val screensaverFocus = remember { FocusRequester() }
            ScreensaverContent(
                preset = preset,
                weather = weather,
                showWeather = prefs.showWeather,
                nowPlaying = nowPlaying.takeIf { prefs.showNowPlaying },
                nightDim = prefs.screensaverNightDim,
                modifier = Modifier.focusRequester(screensaverFocus).focusable(),
            )
            LaunchedEffect(Unit) {
                withFrameNanos {}
                runCatching { screensaverFocus.requestFocus() }
            }
        } else {
            Wallpaper(preset, animated = prefs.animatedWallpaper)

            if (settingsOpen) {
                SettingsScreen(
                    city = city,
                    prefs = prefs,
                    hiddenCount = hiddenApps.size,
                mediaAccess = mediaAccess,
                onRequestMediaAccess = {
                    mediaAccessSystemScreenMissing = false
                    mediaAccessDialogOpen = true
                },
                    onPickCity = { cityPickerOpen = true },
                    onPickWallpaper = { wallpaperPickerOpen = true },
                    onPreviewScreensaver = { screensaverOn = true },
                    onPrefsChange = viewModel::updateHomePrefs,
                    onShowHidden = { hiddenAppsOpen = true },
                    onClose = { settingsOpen = false },
                )
            } else {
                // Отступы 48/27 dp — безопасная зона телевизора (overscan)
                Column(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = appear.value
                            translationY = (1f - appear.value) * 24.dp.toPx()
                        }
                        .padding(horizontal = 48.dp, vertical = 27.dp),
                ) {
                    val home = apps
                    if (home != null && home.favorites.isEmpty() && home.others.isEmpty() && home.hidden.isEmpty()) {
                        widgets()
                        EmptyState(R.drawable.ic_apps, R.string.apps_empty, R.string.apps_empty_text)
                    } else if (home != null) {
                        AppGrid(
                            apps = home,
                            columns = prefs.columns,
                            header = widgets,
                            movingKey = movingKey,
                            focusKey = focusKey,
                            refocusToken = refocusToken,
                            onOpen = { app ->
                                focusKey = app.key
                                refocusOnReturn = true
                                context.launchApp(app)
                            },
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
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppGrid(
    apps: HomeApps,
    columns: Int,
    /** Виджеты над сеткой: прокручиваются вместе с приложениями */
    header: @Composable () -> Unit,
    movingKey: String?,
    /** Приложение, на которое ставится фокус при каждом [refocusToken]; null или его нет — первое */
    focusKey: String?,
    refocusToken: Int,
    onOpen: (AppEntry) -> Unit,
    onMenu: (AppEntry) -> Unit,
    onMove: (AppEntry, Int) -> Unit,
    onMoveDone: () -> Unit,
    onShowHidden: () -> Unit,
) {
    val targetFocus = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    val hasFavorites = apps.favorites.isNotEmpty()
    val hasApps = hasFavorites || apps.others.isNotEmpty()
    // Индексы в сетке: сначала виджеты, затем «Избранное» с заголовком, затем «Приложения» с заголовком
    val favoritesStart = 2
    val othersStart = if (hasFavorites) favoritesStart + apps.favorites.size + 1 else 2
    val targetIndex = focusKey?.let { key ->
        apps.favorites.indexOfFirst { it.key == key }.takeIf { it >= 0 }?.let { favoritesStart + it }
            ?: apps.others.indexOfFirst { it.key == key }.takeIf { it >= 0 }?.let { othersStart + it }
    }
    // Если запомненного приложения уже нет (скрыли, удалили) — первое в сетке
    val targetKey = if (targetIndex != null) focusKey else (apps.favorites.firstOrNull() ?: apps.others.firstOrNull())?.key

    // Сетка держит позицию по ключу первого видимого элемента; без этого при появлении
    // раздела «Избранное» его заголовок уезжал бы за верхний край
    LaunchedEffect(hasFavorites) { gridState.scrollToItem(0) }
    val movingApp = movingKey?.let { key -> (apps.favorites + apps.others).find { it.key == key } }

    val tile: @Composable LazyGridItemScope.(AppEntry) -> Unit = { app ->
        val focusModifier = if (app.key == targetKey) Modifier.focusRequester(targetFocus) else Modifier
        AppTile(
            app = app,
            onClick = { if (movingKey == null) onOpen(app) },
            onLongClick = { if (movingKey == null) onMenu(app) },
            modifier = focusModifier.animateItem(),
            moving = app.key == movingKey,
        )
    }

    // На TV Compose по умолчанию тянет фокус к трети экрана и уводит виджеты за верхний край;
    // нам нужна минимальная прокрутка — ровно чтобы карточка стала видна
    CompositionLocalProvider(LocalBringIntoViewSpec provides MinimalBringIntoViewSpec) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = gridState,
            // В режиме перемещения стрелки двигают приложение, а не фокус
            modifier = Modifier.onPreviewKeyEvent { event ->
                val app = movingApp ?: return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> if (event.type == KeyEventType.KeyDown) onMove(app, -1)
                    Key.DirectionRight -> if (event.type == KeyEventType.KeyDown) onMove(app, 1)
                    Key.DirectionUp -> if (event.type == KeyEventType.KeyDown) onMove(app, -columns)
                    Key.DirectionDown -> if (event.type == KeyEventType.KeyDown) onMove(app, columns)
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
            item(key = "widgets", span = { GridItemSpan(maxLineSpan) }) { header() }
            if (hasFavorites) {
                sectionHeader("header_favorites") { stringResource(R.string.favorites_title) }
                appItems(apps.favorites, tile)
            }
            if (hasApps) {
                sectionHeader("header_apps") { stringResource(R.string.apps_title) }
                appItems(apps.others, tile)
            } else {
                // Кнопка «Скрытые» здесь же, под текстом: отдельным элементом она уехала бы за экран
                // и осталась несозданной — фокусу было бы некуда встать
                item(key = "all_hidden", span = { GridItemSpan(maxLineSpan) }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        EmptyState(R.drawable.ic_apps, R.string.apps_all_hidden, R.string.apps_all_hidden_text)
                        HiddenAppsButton(apps.hidden.size, onShowHidden, Modifier.focusRequester(targetFocus))
                    }
                }
            }
            if (hasApps && apps.hidden.isNotEmpty()) {
                item(key = "hidden_button", span = { GridItemSpan(maxLineSpan) }) {
                    Box { HiddenAppsButton(apps.hidden.size, onShowHidden) }
                }
            }
        }
    }

    val currentTargetIndex by rememberUpdatedState(targetIndex)
    LaunchedEffect(refocusToken) {
        // Ленивая сетка не создаёт карточки за экраном — сперва прокручиваем к нужной
        val index = currentTargetIndex
        when {
            index == null -> gridState.scrollToItem(0)
            gridState.layoutInfo.visibleItemsInfo.none { it.index == index } -> gridState.scrollToItem(index)
        }
        withFrameNanos {}
        runCatching { targetFocus.requestFocus() }
    }
}

@Composable
private fun HiddenAppsButton(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(PillShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        ),
    ) {
        Text(
            text = stringResource(R.string.hidden_apps_button, count),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
        )
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

/** Верхний ряд: часы и погода (если включены в настройках) и кнопка настроек справа */
@Composable
private fun HomeWidgets(
    prefs: HomePrefs,
    weatherContent: @Composable () -> Unit,
    /** null — виджет «Сейчас играет» не показывается */
    nowPlayingContent: (@Composable () -> Unit)?,
    onOpenSettings: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (prefs.showClock) {
            AnalogClockWidget(Modifier.size(190.dp))
            Spacer(Modifier.width(28.dp))
        }
        if (prefs.showWeather) {
            weatherContent()
            Spacer(Modifier.width(28.dp))
        }
        nowPlayingContent?.invoke()
        Spacer(Modifier.weight(1f))
        SettingsButton(onOpenSettings, Modifier.align(Alignment.Top))
    }
}

@Composable
private fun SettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.size(56.dp),
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = colors.surface.copy(alpha = 0.6f),
            contentColor = colors.onSurface,
            focusedContainerColor = colors.onSurface,
            focusedContentColor = colors.surface,
        ),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = stringResource(R.string.settings_title),
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private val MinimalBringIntoViewSpec = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val trailingEdge = offset + size
        return when {
            offset >= 0 && trailingEdge <= containerSize -> 0f
            // Элемент больше видимой области — не дёргаем
            offset < 0 && trailingEdge > containerSize -> 0f
            offset < 0 -> offset
            else -> trailingEdge - containerSize
        }
    }
}
