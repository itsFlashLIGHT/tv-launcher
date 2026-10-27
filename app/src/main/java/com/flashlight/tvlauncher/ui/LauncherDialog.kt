package com.flashlight.tvlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.flashlight.tvlauncher.R

/** Общая оболочка диалогов лаунчера: скруглённая карточка с заголовком; «Назад» закрывает */
@Composable
fun LauncherDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .width(560.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(28.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(20.dp))
            content()
        }
    }
}

/**
 * Просьба выдать разрешение: объяснение и кнопка [onAllow] (обычно системный экран).
 * Если системного экрана на прошивке нет, передайте [adbCommand] — покажем команду для компьютера.
 */
@Composable
fun PermissionDialog(
    text: String,
    adbCommand: String?,
    onAllow: () -> Unit,
    onDismiss: () -> Unit,
) {
    val allowFocus = remember { FocusRequester() }
    LauncherDialog(title = stringResource(R.string.permission_title), onDismiss = onDismiss) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        if (adbCommand != null) {
            Text(
                text = stringResource(R.string.permission_adb_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = adbCommand,
                modifier = Modifier.padding(vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        ListItem(
            selected = false,
            onClick = onAllow,
            headlineContent = { Text(stringResource(R.string.permission_allow)) },
            modifier = Modifier.focusRequester(allowFocus),
        )
    }
    LaunchedEffect(Unit) {
        withFrameNanos {}
        runCatching { allowFocus.requestFocus() }
    }
}
