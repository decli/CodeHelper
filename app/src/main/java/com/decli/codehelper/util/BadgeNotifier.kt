package com.decli.codehelper.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.decli.codehelper.MainActivity
import com.decli.codehelper.R
import com.decli.codehelper.data.SettingsRepository
import com.decli.codehelper.data.SmsRepository
import kotlinx.coroutines.flow.first

object BadgeNotifier {
    private const val CHANNEL_ID = "pickup_code_badge_number_v2"
    private const val CHANNEL_NAME = "取件码角标"
    private const val LEGACY_NOTIFICATION_ID = 1001
    private const val NOTIFICATION_ID_PRIMARY = 1002
    private const val NOTIFICATION_ID_SECONDARY = 1003
    private const val ALARM_REQUEST_CODE = 2001
    private const val BADGE_STATE_PREFS = "badge_notification_state"
    private const val NEXT_NOTIFICATION_ID_KEY = "next_notification_id"

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
            // Keep it clearable; Xiaomi excludes ongoing notifications from icon badge counts.
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationId = nextNotificationId(appContext)
        clearInactiveBadgeNotifications(appContext, notificationId)
        runCatching {
            notificationManager.notify(notificationId, notification)
        }
    }

    fun clearBadge(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context.applicationContext)
        notificationManager.cancel(LEGACY_NOTIFICATION_ID)
        notificationManager.cancel(NOTIFICATION_ID_PRIMARY)
        notificationManager.cancel(NOTIFICATION_ID_SECONDARY)
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

    private fun nextNotificationId(context: Context): Int {
        val preferences = context.getSharedPreferences(BADGE_STATE_PREFS, Context.MODE_PRIVATE)
        val currentId = preferences.getInt(NEXT_NOTIFICATION_ID_KEY, NOTIFICATION_ID_PRIMARY)
            .takeIf { it == NOTIFICATION_ID_PRIMARY || it == NOTIFICATION_ID_SECONDARY }
            ?: NOTIFICATION_ID_PRIMARY
        val nextId = if (currentId == NOTIFICATION_ID_PRIMARY) {
            NOTIFICATION_ID_SECONDARY
        } else {
            NOTIFICATION_ID_PRIMARY
        }
        preferences.edit().putInt(NEXT_NOTIFICATION_ID_KEY, nextId).apply()
        return currentId
    }

    private fun clearInactiveBadgeNotifications(context: Context, activeNotificationId: Int) {
        val notificationManager = NotificationManagerCompat.from(context.applicationContext)
        notificationManager.cancel(LEGACY_NOTIFICATION_ID)
        if (activeNotificationId != NOTIFICATION_ID_PRIMARY) {
            notificationManager.cancel(NOTIFICATION_ID_PRIMARY)
        }
        if (activeNotificationId != NOTIFICATION_ID_SECONDARY) {
            notificationManager.cancel(NOTIFICATION_ID_SECONDARY)
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
