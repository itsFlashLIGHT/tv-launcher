package com.flashlight.tvlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.flashlight.tvlauncher.R
import com.flashlight.tvlauncher.data.City
import com.flashlight.tvlauncher.ui.theme.PillShape
import kotlinx.coroutines.delay

/** Окно выбора города для погоды: поле поиска и список найденных городов */
@Composable
fun CityPickerDialog(
    currentCity: City?,
    onSearch: suspend (String) -> List<City>,
    onSelect: (City) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<City>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    val fieldFocus = remember { FocusRequester() }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = emptyList()
            return@LaunchedEffect
        }
        searching = true
        try {
            delay(SEARCH_DEBOUNCE_MS)
            results = onSearch(query.trim())
        } finally {
            searching = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        val colors = MaterialTheme.colorScheme
        Column(
            Modifier
                .width(560.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(colors.surface)
                .padding(28.dp),
        ) {
            Text(
                text = stringResource(R.string.city_picker_title),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(20.dp))
            SearchField(query, onQueryChange = { query = it }, Modifier.focusRequester(fieldFocus))
            Spacer(Modifier.height(12.dp))

            val status = when {
                query.isBlank() -> currentCity?.let { stringResource(R.string.city_picker_current, it.name) }
                searching -> stringResource(R.string.city_picker_searching)
                results.isEmpty() -> stringResource(R.string.city_picker_not_found)
                else -> null
            }
            if (status != null) {
                Text(
                    text = status,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }

            LazyColumn(Modifier.heightIn(max = 300.dp)) {
                items(results, key = { "${it.latitude},${it.longitude}" }) { city ->
                    ListItem(
                        selected = false,
                        onClick = { onSelect(city) },
                        headlineContent = { Text(city.name) },
                        supportingContent = { Text(city.region) },
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        withFrameNanos {}
        runCatching { fieldFocus.requestFocus() }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            // Текстовое поле само съедает «вниз» с пульта — переводим фокус к результатам вручную
            .onPreviewKeyEvent { event ->
                event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionDown &&
                    focusManager.moveFocus(FocusDirection.Down)
            }
            .clip(PillShape)
            .background(colors.surfaceVariant)
            .border(2.dp, if (focused) colors.primary else Color.Transparent, PillShape)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(color = colors.onSurface),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.moveFocus(FocusDirection.Down) }),
        decorationBox = { innerTextField ->
            Box {
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.city_picker_hint),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
                innerTextField()
            }
        },
    )
}

private const val SEARCH_DEBOUNCE_MS = 400L
