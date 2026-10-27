package com.flashlight.tvlauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/** Что сейчас играет в каком-либо приложении */
@Immutable
data class NowPlaying(
    val packageName: String,
    val appLabel: String,
    val title: String,
    val artist: String?,
    val art: ImageBitmap?,
    val isPlaying: Boolean,
    /** Для кнопок управления; сравнивается по ссылке */
    val controller: MediaController,
)

/**
 * Пустой сервис: системе нужен включённый слушатель уведомлений,
 * чтобы выдать лаунчеру чужие медиасессии.
 */
class MediaListenerService : NotificationListenerService()

/** Медиасессии других приложений (Яндекс Музыка, YouTube и т. д.) */
class MediaSessions(private val context: Context) {
    val listenerComponent = ComponentName(context, MediaListenerService::class.java)

    /** Выдан ли доступ к уведомлениям; без него сессии не видны */
    fun hasAccess(): Boolean =
        context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

    /** Доступ проверяется раз в несколько секунд: его выдают в настройках или по ADB */
    fun accessChanges(): Flow<Boolean> = flow {
        while (true) {
            emit(hasAccess())
            delay(ACCESS_POLL_MS)
        }
    }.distinctUntilChanged()

    /** Текущий трек; null — ничего не играет или нет доступа */
    fun nowPlaying(): Flow<NowPlaying?> = flow {
        while (!hasAccess()) {
            emit(null)
            delay(ACCESS_POLL_MS)
        }
        emitAll(activeSessions())
    }.distinctUntilChanged()

    /** Системный экран доступа к уведомлениям; false, если на прошивке его нет */
    fun openAccessSettings(activity: Context): Boolean =
        runCatching { activity.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }.isSuccess

    private fun activeSessions(): Flow<NowPlaying?> = callbackFlow {
        val manager = context.getSystemService(MediaSessionManager::class.java)
        val handler = Handler(Looper.getMainLooper())
        var controllers = emptyList<MediaController>()
        // Обёртку обложки переиспользуем, пока приложение отдаёт тот же Bitmap: иначе лишние перерисовки
        var artSource: Bitmap? = null
        var art: ImageBitmap? = null

        fun publish() {
            val controller = pick(controllers)
            val metadata = controller?.metadata
            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            if (controller == null || title.isNullOrBlank()) {
                trySend(null)
                return
            }
            val bitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            if (bitmap !== artSource) {
                artSource = bitmap
                art = bitmap?.asImageBitmap()
            }
            trySend(
                NowPlaying(
                    packageName = controller.packageName,
                    appLabel = appLabel(controller.packageName),
                    title = title,
                    artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                        ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE),
                    art = art,
                    isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING,
                    controller = controller,
                ),
            )
        }

        val controllerCallback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
            override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
            override fun onSessionDestroyed() = publish()
        }

        fun attach(list: List<MediaController>?) {
            controllers.forEach { it.unregisterCallback(controllerCallback) }
            controllers = list.orEmpty()
            controllers.forEach { it.registerCallback(controllerCallback, handler) }
            publish()
        }

        val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener(::attach)
        attach(manager.getActiveSessions(listenerComponent))
        manager.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent, handler)
        awaitClose {
            manager.removeOnActiveSessionsChangedListener(sessionsListener)
            controllers.forEach { it.unregisterCallback(controllerCallback) }
        }
    }

    /** Играющая сессия важнее остановленной, Яндекс Музыка — важнее остальных */
    private fun pick(controllers: List<MediaController>): MediaController? {
        val withMetadata = controllers.filter { it.metadata != null }
        val playing = withMetadata.filter { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        return (playing.ifEmpty { withMetadata })
            .sortedByDescending { isYandexMusic(it.packageName, appLabel(it.packageName)) }
            .firstOrNull()
    }

    private fun appLabel(packageName: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
    }.getOrDefault(packageName)

    private companion object {
        const val ACCESS_POLL_MS = 5_000L
    }
}

/** Открывает плеер: его собственную активность сессии или просто приложение */
fun Context.openPlayer(nowPlaying: NowPlaying) {
    val sessionActivity = nowPlaying.controller.sessionActivity
    if (sessionActivity != null && runCatching { sessionActivity.send() }.isSuccess) return
    val launch = packageManager.getLeanbackLaunchIntentForPackage(nowPlaying.packageName)
        ?: packageManager.getLaunchIntentForPackage(nowPlaying.packageName)
        ?: return
    runCatching { startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

fun MediaController.togglePlayPause() {
    if (playbackState?.state == PlaybackState.STATE_PLAYING) transportControls.pause() else transportControls.play()
}

/** Известные пакеты Яндекс Музыки; на прошивках Яндекс ТВ имя может отличаться — тогда ищем по названию */
private val YANDEX_MUSIC_PACKAGES = setOf("ru.yandex.music", "com.yandex.music", "com.yandex.tv.music")
private val YANDEX_MUSIC_LABEL = Regex("(яндекс|yandex)\\s*(музык|music)", RegexOption.IGNORE_CASE)

fun isYandexMusic(packageName: String, label: String): Boolean =
    packageName in YANDEX_MUSIC_PACKAGES || YANDEX_MUSIC_LABEL.containsMatchIn(label)

/** Яндекс Музыка среди установленных приложений, если она есть */
fun List<AppEntry>.findYandexMusic(): AppEntry? = find { isYandexMusic(it.packageName, it.label) }
