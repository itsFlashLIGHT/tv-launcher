package com.flashlight.tvlauncher.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flashlight.tvlauncher.R
import com.flashlight.tvlauncher.data.NowPlaying
import com.flashlight.tvlauncher.ui.theme.LocalLauncherAccents
import com.flashlight.tvlauncher.ui.theme.PillShape

private val CardShape = RoundedCornerShape(40.dp)
private val ArtShape = RoundedCornerShape(28.dp)
private val TextDark = Color(0xFF1D1B20)
private val TextMuted = Color(0xFF49454F)

/**
 * «Сейчас играет»: обложка, трек и кнопки управления. Если ничего не играет,
 * а Яндекс Музыка установлена, — предлагает открыть её ([onOpenYandexMusic] не null).
 */
@Composable
fun NowPlayingWidget(
    nowPlaying: NowPlaying?,
    onOpenPlayer: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onOpenYandexMusic: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val accent = LocalLauncherAccents.current.clockHourHand

    Row(
        modifier
            .clip(CardShape)
            .background(Color.White)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Обложка — кнопка «открыть плеер»
        Surface(
            onClick = { if (nowPlaying != null) onOpenPlayer() else onOpenYandexMusic?.invoke() },
            modifier = Modifier.fillMaxHeight().width(138.dp),
            shape = ClickableSurfaceDefaults.shape(ArtShape),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = accent.copy(alpha = 0.15f),
                focusedContainerColor = accent.copy(alpha = 0.25f),
            ),
        ) {
            val art = nowPlaying?.art
            if (art != null) {
                Image(art, contentDescription = null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_music_note), null, Modifier.size(48.dp), tint = accent)
                }
            }
        }

        Column(Modifier.padding(start = 16.dp).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
            Text(
                text = nowPlaying?.appLabel ?: stringResource(R.string.now_playing_idle_app),
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = nowPlaying?.title ?: stringResource(R.string.now_playing_idle),
                style = MaterialTheme.typography.titleMedium,
                color = TextDark,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            nowPlaying?.artist?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.weight(1f))
            if (nowPlaying != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    MediaButton(R.drawable.ic_skip_previous, R.string.now_playing_previous, onPrevious)
                    MediaButton(
                        icon = if (nowPlaying.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                        description = if (nowPlaying.isPlaying) R.string.now_playing_pause else R.string.now_playing_play,
                        onClick = onPlayPause,
                    )
                    MediaButton(R.drawable.ic_skip_next, R.string.now_playing_next, onNext)
                }
            } else if (onOpenYandexMusic != null) {
                Surface(
                    onClick = onOpenYandexMusic,
                    shape = ClickableSurfaceDefaults.shape(PillShape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = accent.copy(alpha = 0.15f),
                        contentColor = TextDark,
                        focusedContainerColor = accent,
                        focusedContentColor = Color.White,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.now_playing_open_yandex_music),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaButton(@DrawableRes icon: Int, description: Int, onClick: () -> Unit) {
    val accent = LocalLauncherAccents.current.clockHourHand
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        colors = IconButtonDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = TextDark,
            focusedContainerColor = accent,
            focusedContentColor = Color.White,
        ),
    ) {
        Icon(painterResource(icon), contentDescription = stringResource(description), Modifier.size(24.dp))
    }
}
