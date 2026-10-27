package com.flashlight.tvlauncher.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Таймер сна. Обычное приложение не может выключить телевизор, поэтому лаунчер
 * становится администратором устройства и по таймеру вызывает [DevicePolicyManager.lockNow] —
 * экран гаснет, даже если плеер держит его включённым.
 */
class SleepTimer(private val context: Context) {
    private val settings = SettingsStore(context)
    private val policyManager = context.getSystemService(DevicePolicyManager::class.java)
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    val adminComponent = ComponentName(context, SleepAdminReceiver::class.java)

    val isAdminActive: Boolean get() = policyManager.isAdminActive(adminComponent)

    /** Возвращает false, если нет прав администратора: таймер тогда не заводится */
    fun start(minutes: Int): Boolean {
        if (!isAdminActive) return false
        val end = System.currentTimeMillis() + minutes * 60_000L
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, alarmIntent())
        } else {
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, end, WINDOW_MS, alarmIntent())
        }
        settings.setSleepTimerEnd(end)
        return true
    }

    fun cancel() {
        alarmManager.cancel(alarmIntent())
        settings.setSleepTimerEnd(0L)
    }

    /**
     * Системное окно выдачи прав; false, если на прошивке его нет.
     * [activity] — именно активность: окно отказывается открываться в новой задаче (FLAG_ACTIVITY_NEW_TASK)
     */
    fun requestAdmin(activity: Context, explanation: String): Boolean {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, explanation)
        return runCatching { activity.startActivity(intent) }.isSuccess
    }

    private fun alarmIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, SleepTimerReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        /** Если точные будильники запрещены — допуск в минуту */
        const val WINDOW_MS = 60_000L
    }
}

/** Срабатывание таймера сна: гасим экран */
class SleepTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        SettingsStore(context).setSleepTimerEnd(0L)
        val policyManager = context.getSystemService(DevicePolicyManager::class.java)
        runCatching { policyManager.lockNow() }
    }
}

/** Администратор устройства с единственной политикой force-lock (см. res/xml/device_admin.xml) */
class SleepAdminReceiver : DeviceAdminReceiver()
