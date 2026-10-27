package com.flashlight.tvlauncher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.flashlight.tvlauncher.R
import com.flashlight.tvlauncher.data.TvInput

/** Минуты таймера сна на выбор */
private val SLEEP_CHOICES = listOf(15, 30, 45, 60, 90, 120)

@Composable
fun TvInputsDialog(
    inputs: List<TvInput>,
    onSelect: (TvInput) -> Unit,
    onDismiss: () -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    LauncherDialog(title = stringResource(R.string.inputs_title), onDismiss = onDismiss) {
        if (inputs.isEmpty()) {
            Text(
                text = stringResource(R.string.inputs_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        inputs.forEachIndexed { index, input ->
            ListItem(
                selected = false,
                onClick = { onSelect(input) },
                headlineContent = { Text(input.label) },
                modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
            )
        }
    }
    LaunchedEffect(inputs.isNotEmpty()) {
        if (inputs.isEmpty()) return@LaunchedEffect
        withFrameNanos {}
        runCatching { firstFocus.requestFocus() }
    }
}

/** Выбор таймера сна; [onSelect] получает минуты, 0 — выключить таймер */
@Composable
fun SleepTimerDialog(
    active: Boolean,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    val options = buildList {
        if (active) add(0)
        addAll(SLEEP_CHOICES)
    }
    LauncherDialog(title = stringResource(R.string.sleep_title), onDismiss = onDismiss) {
        options.forEachIndexed { index, minutes ->
            ListItem(
                selected = false,
                onClick = { onSelect(minutes) },
                headlineContent = {
                    Text(
                        if (minutes == 0) {
                            stringResource(R.string.sleep_cancel)
                        } else {
                            stringResource(R.string.sleep_minutes, minutes)
                        },
                    )
                },
                modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
            )
        }
    }
    LaunchedEffect(Unit) {
        withFrameNanos {}
        runCatching { firstFocus.requestFocus() }
    }
}
