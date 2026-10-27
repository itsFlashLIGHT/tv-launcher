package com.flashlight.tvlauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ListItem
import androidx.tv.material3.Text
import com.flashlight.tvlauncher.R
import com.flashlight.tvlauncher.data.AppEntry

enum class AppAction { Move, ToggleFavorite, Hide, Info, Uninstall }

/** Меню по долгому нажатию на приложение */
@Composable
fun AppActionsDialog(
    app: AppEntry,
    isFavorite: Boolean,
    onAction: (AppAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val actions = buildList {
        add(AppAction.Move to R.string.action_move)
        add(AppAction.ToggleFavorite to if (isFavorite) R.string.action_unfavorite else R.string.action_favorite)
        add(AppAction.Hide to R.string.action_hide)
        add(AppAction.Info to R.string.action_info)
        if (!app.isSystem) add(AppAction.Uninstall to R.string.action_uninstall)
    }
    val firstFocus = remember { FocusRequester() }

    LauncherDialog(title = app.label, onDismiss = onDismiss) {
        actions.forEachIndexed { index, (action, label) ->
            ListItem(
                selected = false,
                onClick = { onAction(action) },
                headlineContent = { Text(stringResource(label)) },
                modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
            )
        }
    }

    LaunchedEffect(Unit) {
        withFrameNanos {}
        runCatching { firstFocus.requestFocus() }
    }
}

/** Список скрытых приложений; OK возвращает приложение на главный экран */
@Composable
fun HiddenAppsDialog(
    apps: List<AppEntry>,
    onUnhide: (AppEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    LauncherDialog(title = stringResource(R.string.hidden_apps_title), onDismiss = onDismiss) {
        LazyColumn(Modifier.heightIn(max = 360.dp)) {
            items(apps, key = { it.key }) { app ->
                ListItem(
                    selected = false,
                    onClick = { onUnhide(app) },
                    headlineContent = { Text(app.label) },
                    supportingContent = { Text(stringResource(R.string.hidden_apps_unhide)) },
                    leadingContent = { Image(app.icon, contentDescription = null, Modifier.size(32.dp)) },
                )
            }
        }
    }
}
