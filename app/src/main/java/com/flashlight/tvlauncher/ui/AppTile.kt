package com.flashlight.tvlauncher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flashlight.tvlauncher.data.AppEntry
import com.flashlight.tvlauncher.ui.theme.PillShape

/** Овальная карточка приложения: баннер TV-приложения или иконка, подпись снизу */
@Composable
fun AppTile(
    app: AppEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Режим перемещения: карточка крупнее и с рамкой другого цвета */
    moving: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f)
                .onFocusChanged { focused = it.hasFocus },
            shape = ClickableSurfaceDefaults.shape(PillShape),
            scale = ClickableSurfaceDefaults.scale(focusedScale = if (moving) 1.14f else 1.08f),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = colors.surfaceVariant.copy(alpha = 0.7f),
                focusedContainerColor = colors.surfaceVariant,
            ),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(
                    BorderStroke(if (moving) 3.dp else 2.dp, if (moving) colors.tertiary else colors.primary),
                    shape = PillShape,
                ),
            ),
            glow = ClickableSurfaceDefaults.glow(
                focusedGlow = Glow((if (moving) colors.tertiary else colors.primary).copy(alpha = 0.5f), 16.dp),
            ),
        ) {
            if (app.banner != null) {
                Image(
                    bitmap = app.banner,
                    contentDescription = app.label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = app.icon,
                        contentDescription = app.label,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelLarge,
            color = if (focused) colors.onSurface else colors.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
