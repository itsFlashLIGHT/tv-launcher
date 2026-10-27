package com.flashlight.tvlauncher.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flashlight.tvlauncher.R
import com.flashlight.tvlauncher.data.NetworkKind
import com.flashlight.tvlauncher.ui.theme.PillShape
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Ряд быстрых плиток: сеть, входы, таймер сна, системные настройки */
@Composable
fun QuickTilesRow(
    network: NetworkKind,
    sleepTimerEnd: Long,
    onNetwork: () -> Unit,
    onInputs: () -> Unit,
    onSleepTimer: () -> Unit,
    onSystemSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeFormat = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val sleepActive = sleepTimerEnd > System.currentTimeMillis()

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        QuickTile(
            icon = R.drawable.ic_wifi,
            title = stringResource(R.string.tile_network),
            status = stringResource(
                when (network) {
                    NetworkKind.Wifi -> R.string.tile_network_wifi
                    NetworkKind.Ethernet -> R.string.tile_network_ethernet
                    NetworkKind.Other -> R.string.tile_network_other
                    NetworkKind.None -> R.string.tile_network_none
                },
            ),
            onClick = onNetwork,
        )
        QuickTile(
            icon = R.drawable.ic_input,
            title = stringResource(R.string.tile_inputs),
            status = stringResource(R.string.tile_inputs_status),
            onClick = onInputs,
        )
        QuickTile(
            icon = R.drawable.ic_timer,
            title = stringResource(R.string.tile_sleep),
            status = if (sleepActive) {
                val end = Instant.ofEpochMilli(sleepTimerEnd).atZone(ZoneId.systemDefault())
                stringResource(R.string.tile_sleep_until, end.format(timeFormat))
            } else {
                stringResource(R.string.tile_sleep_off)
            },
            active = sleepActive,
            onClick = onSleepTimer,
        )
        QuickTile(
            icon = R.drawable.ic_settings,
            title = stringResource(R.string.tile_settings),
            status = stringResource(R.string.tile_settings_status),
            onClick = onSystemSettings,
        )
    }
}

@Composable
private fun QuickTile(
    @DrawableRes icon: Int,
    title: String,
    status: String,
    onClick: () -> Unit,
    active: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(PillShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (active) colors.primary.copy(alpha = 0.35f) else colors.surface.copy(alpha = 0.6f),
            contentColor = colors.onSurface,
            focusedContainerColor = colors.onSurface,
            focusedContentColor = colors.surface,
        ),
    ) {
        Row(
            Modifier.padding(start = 20.dp, end = 28.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge)
                Text(status, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
