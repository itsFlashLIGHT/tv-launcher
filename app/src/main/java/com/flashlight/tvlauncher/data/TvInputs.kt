package com.flashlight.tvlauncher.data

import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import androidx.compose.runtime.Immutable

/** Физический вход телевизора: HDMI, композитный и т. п. */
@Immutable
data class TvInput(val id: String, val label: String)

/** Входы-«проходы» (passthrough); пустой список, если ТВ-фреймворка нет, как на эмуляторе */
fun Context.passthroughInputs(): List<TvInput> {
    val manager = getSystemService(TvInputManager::class.java) ?: return emptyList()
    return runCatching {
        manager.tvInputList
            .filter { it.isPassthroughInput && !it.isHidden(this) }
            .map { TvInput(it.id, it.loadLabel(this).toString()) }
    }.getOrDefault(emptyList())
}

/** Просит систему (приложение «ТВ» прошивки) переключиться на вход */
fun Context.openTvInput(input: TvInput) {
    val intent = Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(input.id))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}
