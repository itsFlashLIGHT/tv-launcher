package com.flashlight.tvlauncher.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flashlight.tvlauncher.R
import com.flashlight.tvlauncher.ui.theme.PillShape

/** Плашка «нет интернета»: по OK открывает настройки сети */
@Composable
fun OfflineBanner(onOpenNetworkSettings: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onOpenNetworkSettings,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(PillShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = colors.errorContainer.copy(alpha = 0.85f),
            contentColor = colors.onErrorContainer,
            focusedContainerColor = colors.errorContainer,
            focusedContentColor = colors.onErrorContainer,
        ),
    ) {
        Row(
            Modifier.padding(start = 20.dp, end = 28.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_wifi_off), contentDescription = null, Modifier.size(28.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(stringResource(R.string.offline_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.offline_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onErrorContainer.copy(alpha = 0.8f),
                )
            }
            Spacer(Modifier.width(32.dp))
            Text(stringResource(R.string.offline_action), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Пустое состояние вместо сетки: иконка, заголовок и подсказка, что делать */
@Composable
fun EmptyState(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    @StringRes text: Int,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxWidth().padding(top = 24.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, Modifier.size(56.dp), tint = colors.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(title), style = MaterialTheme.typography.headlineSmall, color = colors.onSurface)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(text),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
