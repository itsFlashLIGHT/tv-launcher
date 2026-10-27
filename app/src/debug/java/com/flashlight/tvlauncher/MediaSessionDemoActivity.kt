package com.flashlight.tvlauncher

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle

/**
 * Только для debug-сборки: фиктивный «плеер» с медиасессией, чтобы проверить
 * виджет «Сейчас играет» без настоящего музыкального приложения.
 * Запуск: adb shell am start -n com.flashlight.tvlauncher/.MediaSessionDemoActivity
 */
class MediaSessionDemoActivity : Activity() {
    private lateinit var session: MediaSession
    private var track = 0
    private var playing = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = MediaSession(this, "demo").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() = update(play = true)
                override fun onPause() = update(play = false)
                override fun onSkipToNext() = update(next = 1)
                override fun onSkipToPrevious() = update(next = -1)
            })
            isActive = true
        }
        update()
    }

    override fun onDestroy() {
        session.release()
        super.onDestroy()
    }

    private fun update(play: Boolean = playing, next: Int = 0) {
        playing = play
        track = (track + next + TRACKS.size) % TRACKS.size
        val (title, artist) = TRACKS[track]
        session.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, cover(track))
                .build(),
        )
        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS,
                )
                .setState(if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED, 0L, 1f)
                .build(),
        )
    }

    private fun cover(index: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val colors = COVER_COLORS[index % COVER_COLORS.size]
        val paint = Paint().apply { shader = LinearGradient(0f, 0f, 256f, 256f, colors.first, colors.second, Shader.TileMode.CLAMP) }
        Canvas(bitmap).drawRect(0f, 0f, 256f, 256f, paint)
        return bitmap
    }

    private companion object {
        val TRACKS = listOf(
            "Тестовый трек" to "Демо-исполнитель",
            "Второй трек с очень длинным названием" to "Другой исполнитель",
        )
        val COVER_COLORS = listOf(
            Color.rgb(255, 138, 101) to Color.rgb(122, 46, 92),
            Color.rgb(79, 163, 217) to Color.rgb(14, 140, 140),
        )
    }
}
