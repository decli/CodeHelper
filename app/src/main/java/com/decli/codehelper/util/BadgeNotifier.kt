package com.decli.codehelper.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.decli.codehelper.MainActivity
import com.decli.codehelper.R
import com.decli.codehelper.data.SettingsRepository
import com.decli.codehelper.data.SmsRepository
import kotlinx.coroutines.flow.first

object BadgeNotifier {
    private const val TAG = "BadgeNotifier"
    private const val CHANNEL_ID = "pickup_code_badge_number_v2"
    private const val CHANNEL_NAME = "取件码角标"
    private const val LEGACY_NOTIFICATION_ID = 1001
    private const val LEGACY_SECONDARY_NOTIFICATION_ID = 1003
    private const val NOTIFICATION_ID = 1002
    private const val ALARM_REQUEST_CODE = 2001

    private const val XIAOMI_BADGE_ACTION = "android.intent.action.APPLICATION_MESSAGE_UPDATE"
    private const val XIAOMI_BADGE_EXTRA_COMPONENT =
        "android.intent.extra.update_application_component_name"
    private const val XIAOMI_BADGE_EXTRA_TEXT =
        "android.intent.extra.update_application_message_text"

    private const val SAMSUNG_BADGE_ACTION = "android.intent.action.BADGE_COUNT_UPDATE"

    // HyperOS / MIUI 已知的桌面包名（中国版与全球版）。
    // Android 14+ 隐式广播被限制，需要显式 setPackage 才能被 manifest 注册的 receiver 接收。
    private val KNOWN_LAUNCHER_PACKAGES = listOf(
        "com.miui.home",
        "com.mi.android.globallauncher",
        "com.miui.miuilite",
        "com.miui.mihome",
        "com.miui.mihome2",
    )

    suspend fun refreshBadgeFromSms(context: Context) {
        val appContext = context.applicationContext
        val settingsRepository = SettingsRepository(appContext)
        val refreshMinutes = settingsRepository.badgeRefreshMinutesFlow.first()

        if (!hasSmsPermission(appContext)) {
            clearBadge(appContext)
            scheduleNextBadgeRefresh(appContext, refreshMinutes)
            return
        }

        val extractorSettings = settingsRepository.extractorSettingsFlow.first()
        val pickedUpItems = settingsRepository.pickedUpItemsFlow.first()
        val filterWindow = settingsRepository.selectedFilterFlow.first()
        val pendingCount = SmsRepository(appContext.contentResolver)
            .loadPickupCodes(
                filterWindow = filterWindow,
                promptKeywords = extractorSettings.promptKeywords,
                advancedRules = extractorSettings.advancedRules,
                pickedUpKeys = pickedUpItems,
                includePickedUp = false,
            )
            .sumOf { item -> item.codeCount }

        updateBadge(appContext, pendingCount)
        scheduleNextBadgeRefresh(appContext, refreshMinutes)
    }

    @SuppressLint("MissingPermission")
    fun updateBadge(context: Context, pendingCount: Int) {
        val appContext = context.applicationContext
        val notificationManager = NotificationManagerCompat.from(appContext)
        if (pendingCount <= 0 || !hasNotificationPermission(appContext) || !notificationManager.areNotificationsEnabled()) {
            clearBadge(appContext)
            return
        }

        ensureChannel(appContext)
        val launchIntent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
            ?: Intent(appContext, MainActivity::class.java)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val contentIntent = PendingIntent.getActivity(
            appContext,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("还有 $pendingCount 个未取件码")
            .setContentText("打开取件码助手查看")
            .setContentIntent(contentIntent)
            .setNumber(pendingCount)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        applyMiuiBadgeCount(notification, pendingCount)
        notificationManager.cancel(LEGACY_NOTIFICATION_ID)
        notificationManager.cancel(LEGACY_SECONDARY_NOTIFICATION_ID)
        runCatching {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
        sendXiaomiBadgeBroadcast(appContext, pendingCount)
    }

    fun clearBadge(context: Context) {
        val appContext = context.applicationContext
        val notificationManager = NotificationManagerCompat.from(appContext)
        notificationManager.cancel(LEGACY_NOTIFICATION_ID)
        notificationManager.cancel(LEGACY_SECONDARY_NOTIFICATION_ID)
        notificationManager.cancel(NOTIFICATION_ID)
        sendXiaomiBadgeBroadcast(appContext, 0)
    }

    fun scheduleNextBadgeRefresh(context: Context, refreshMinutes: Int) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        val triggerAtMillis = System.currentTimeMillis() + refreshMinutes.coerceIn(5, 120) * 60_000L
        runCatching {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                badgeRefreshIntent(appContext),
            )
        }
    }

    fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    private fun hasSmsPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS,
        ) == PackageManager.PERMISSION_GRANTED

    private fun ensureChannel(context: Context) {
        val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            setShowBadge(true)
            setSound(null, null)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun launcherComponent(context: Context): ComponentName? =
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.component

    private fun activeHomeLauncherPackage(context: Context): String? {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return runCatching {
            context.packageManager
                .resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo
                ?.packageName
        }.getOrNull()
    }

    private fun candidateLauncherPackages(context: Context): List<String> {
        val resolved = activeHomeLauncherPackage(context)
        val all = LinkedHashSet<String>()
        if (resolved != null) all.add(resolved)
        all.addAll(KNOWN_LAUNCHER_PACKAGES)
        val installed = context.packageManager.getInstalledPackages(0).map { it.packageName }
        return all.filter { it in installed }
    }

    // MIUI / HyperOS 桌面不读取 Notification.number / setBadgeIconType。
    // 数字角标走的是私有广播 APPLICATION_MESSAGE_UPDATE，需要主动发送。
    // Android 14+ 限制隐式广播投递到 manifest 注册的 receiver，所以必须显式 setPackage。
    private fun sendXiaomiBadgeBroadcast(context: Context, count: Int) {
        val component = launcherComponent(context) ?: return
        val targets = candidateLauncherPackages(context)
        if (targets.isEmpty()) {
            Log.w(TAG, "no launcher package resolved; skip badge broadcast")
            return
        }
        val componentName = "${component.packageName}/${component.className}"
        val text = if (count <= 0) "" else count.toString()

        for (pkg in targets) {
            val miuiIntent = Intent(XIAOMI_BADGE_ACTION).apply {
                setPackage(pkg)
                putExtra(XIAOMI_BADGE_EXTRA_COMPONENT, componentName)
                putExtra(XIAOMI_BADGE_EXTRA_TEXT, text)
            }
            runCatching { context.sendBroadcast(miuiIntent) }
                .onFailure { Log.w(TAG, "miui badge broadcast to $pkg failed: ${it.message}") }

            val samsungIntent = Intent(SAMSUNG_BADGE_ACTION).apply {
                setPackage(pkg)
                putExtra("badge_count_package_name", component.packageName)
                putExtra("badge_count_class_name", component.className)
                putExtra("badge_count", count.coerceAtLeast(0))
            }
            runCatching { context.sendBroadcast(samsungIntent) }
                .onFailure { Log.w(TAG, "samsung badge broadcast to $pkg failed: ${it.message}") }
        }
        Log.i(TAG, "badge broadcast count=$count targets=$targets")
    }

    // 旧版 MIUI 通过 Notification.extraNotification 读取 messageCount。
    // 在 HyperOS 上反射可能失败，但失败无副作用。
    private fun applyMiuiBadgeCount(notification: Notification, count: Int) {
        runCatching {
            val field = notification.javaClass.getDeclaredField("extraNotification")
            field.isAccessible = true
            val extra = field.get(notification) ?: return@runCatching
            val setter = extra.javaClass.getDeclaredMethod(
                "setMessageCount",
                Int::class.javaPrimitiveType,
            )
            setter.isAccessible = true
            setter.invoke(extra, count)
        }
    }

    private fun badgeRefreshIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            Intent(context, BadgeRefreshReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
