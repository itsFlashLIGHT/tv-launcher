package com.flashlight.tvlauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Immutable
data class AppEntry(
    val label: String,
    val component: ComponentName,
    /** LEANBACK_LAUNCHER для TV-приложений, LAUNCHER для мобильных */
    val category: String,
    val banner: ImageBitmap?,
    val icon: ImageBitmap,
) {
    val packageName: String get() = component.packageName
    val key: String get() = component.flattenToString()
}

class AppRepository(private val context: Context) {
    private val packageManager = context.packageManager
    private val launcherApps = context.getSystemService(LauncherApps::class.java)

    /** Сигнал при установке, удалении или обновлении любого приложения */
    fun changes(): Flow<Unit> = callbackFlow {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String, user: UserHandle) {
                trySend(Unit)
            }

            override fun onPackageRemoved(packageName: String, user: UserHandle) {
                trySend(Unit)
            }

            override fun onPackageChanged(packageName: String, user: UserHandle) {
                trySend(Unit)
            }

            override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
                trySend(Unit)
            }

            override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
                trySend(Unit)
            }
        }
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        awaitClose { launcherApps.unregisterCallback(callback) }
    }

    suspend fun loadApps(): List<AppEntry> = withContext(Dispatchers.IO) {
        // TV-версия приложения важнее мобильной: у неё есть баннер и интерфейс под пульт
        val candidates = query(Intent.CATEGORY_LEANBACK_LAUNCHER) + query(Intent.CATEGORY_LAUNCHER)
        val seenPackages = HashSet<String>()
        candidates
            .filter { (info, _) ->
                val packageName = info.activityInfo.packageName
                packageName != context.packageName && seenPackages.add(packageName)
            }
            .map { (info, category) -> toEntry(info, category) }
            .sortedBy { it.label.lowercase() }
    }

    private fun query(category: String): List<Pair<ResolveInfo, String>> =
        packageManager
            .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0)
            .map { it to category }

    private fun toEntry(info: ResolveInfo, category: String): AppEntry {
        val activity = info.activityInfo
        val banner = activity.loadBanner(packageManager) ?: activity.applicationInfo.loadBanner(packageManager)
        val bannerBitmap = banner?.let { drawable ->
            val width = BANNER_WIDTH_PX
            val aspect = if (drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0) {
                drawable.intrinsicHeight.toFloat() / drawable.intrinsicWidth
            } else {
                9f / 16f
            }
            drawable.toBitmap(width, (width * aspect).roundToInt()).asImageBitmap()
        }
        return AppEntry(
            label = info.loadLabel(packageManager).toString(),
            component = ComponentName(activity.packageName, activity.name),
            category = category,
            banner = bannerBitmap,
            icon = info.loadIcon(packageManager).toBitmap(ICON_SIZE_PX, ICON_SIZE_PX).asImageBitmap(),
        )
    }

    private companion object {
        const val BANNER_WIDTH_PX = 480
        const val ICON_SIZE_PX = 192
    }
}

fun Context.launchApp(app: AppEntry) {
    val intent = Intent(Intent.ACTION_MAIN)
        .addCategory(app.category)
        .setComponent(app.component)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    runCatching { startActivity(intent) }
}

fun Context.openAppInfo(app: AppEntry) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", app.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}
