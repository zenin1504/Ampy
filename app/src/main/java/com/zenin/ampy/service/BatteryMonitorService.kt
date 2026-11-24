package com.zenin.ampy.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.zenin.ampy.ZeninMainActivity
import com.zenin.ampy.R
import com.zenin.ampy.data.repository.BatteryInfoRepository
import com.zenin.ampy.data.repository.PrefsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BatteryMonitorService : Service() {
    companion object {
        private const val ACTION_STOP = "com.zenin.ampy.ACTION_STOP"
        private const val LEGACY_CHANNEL_ID = "battery_monitor"
        private const val LEGACY_EVENT_CHANNEL_ID = "battery_alerts"
        private const val EVENT_CHANNEL_ID = "battery_alerts_v2"
        private const val CHARGING_NOTIF_ID = 1002
        private const val FULL_NOTIF_ID = 1003
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val updateScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var batteryRepo: BatteryInfoRepository
    private lateinit var notificationManager: NotificationManager
    private val prefsRepo by lazy { PrefsRepository(applicationContext) }
    private var updateJob: Job? = null

    @Volatile
    private var currentIntervalMs = 1000

    // New channel so the lock-screen visibility is applied to existing installs.
    private val channelId = "battery_monitor_v2"
    private val notifId = 1001

    override fun onCreate() {
        super.onCreate()
        batteryRepo = BatteryInfoRepository(applicationContext)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        createEventChannel()

        // A foreground service must publish its notification immediately.
        // Android may restart us with a null Intent after killing the process,
        // so this cannot be left to onStartCommand only.
        startForegroundCompat(buildNotification(getString(R.string.initializing)))

        serviceScope.launch {
            prefsRepo.refreshInterval.collect { newInterval ->
                currentIntervalMs = newInterval
            }
        }
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                notifId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(notifId, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopMonitor()
            return START_NOT_STICKY
        }

        // Keep a manifest-registered watchdog armed. If an OEM ROM kills the
        // process despite START_STICKY, the receiver restores the service.
        MonitorWatchdog.schedulePeriodic(applicationContext)
        startUpdating()
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Called when the user swipes the app out of Recents. Schedule a fast
        // restart instead of relying on OEM-specific START_STICKY behaviour.
        MonitorWatchdog.restartSoon(applicationContext)
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        stopUpdating()
        updateScope.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun stopMonitor() {
        stopUpdating()
        updateScope.cancel()
        serviceScope.cancel()
        MonitorWatchdog.cancel(applicationContext)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopUpdating() {
        updateJob?.cancel()
        updateJob = null
    }

    private fun createNotificationChannel() {
        // Channel settings are immutable after creation; remove the old one.
        notificationManager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        val chan = NotificationChannel(
            channelId,
            getString(R.string.battery_monitor),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            // Show the full notification content on the lock screen.
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(chan)
    }

    private fun createEventChannel() {
        // Importance is immutable once a channel exists, so use a new ID for
        // the alarm-strength settings and remove the previous one.
        notificationManager.deleteNotificationChannel(LEGACY_EVENT_CHANNEL_ID)
        val chan = NotificationChannel(
            EVENT_CHANNEL_ID,
            getString(R.string.battery_alerts),
            NotificationManager.IMPORTANCE_MAX
        ).apply {
            description = getString(R.string.battery_alerts)
            setShowBadge(true)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 200, 300)
            setSound(
                android.net.Uri.parse("content://settings/system/notification_sound"),
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            // Show full content even when "hide sensitive" is on.
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(chan)
    }

    private fun postEventNotification(id: Int, title: String, text: String) {
        val openIntent = Intent(this, ZeninMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this, 2, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        // Full-screen intent makes the alert appear on the lock screen like an
        // alarm/call, even on ROMs that suppress ordinary heads-up alerts.
        val fullScreenPending = PendingIntent.getActivity(
            this, 3, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, EVENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_battery_saver_24)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setFullScreenIntent(fullScreenPending, true)
            .build()
        try {
            notificationManager.notify(id, notification)
        } catch (e: Exception) {
            // Never let a transient notification error stop the monitor.
        }
    }

    /** Reads the raw charging state without relying on localized strings. */
    private fun readChargeState(): Pair<Int, Int> {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        return status to level
    }

    private fun buildNotification(content: String): Notification {
        val openIntent = Intent(this, ZeninMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, BatteryMonitorService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.baseline_battery_saver_24)
            .setContentTitle(getString(R.string.battery_monitor))
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content)) // allow more lines
            .setContentIntent(openPending)
            .addAction(
                R.drawable.speed_24dp_1f1f1f_fill1_wght400_grad0_opsz24,
                getString(R.string.stop), stopPending
            )
            .setOngoing(true)       // ongoing notification
            .setOnlyAlertOnce(true) // silently update the notification
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // visible on lock screen
            .build()
    }

    private suspend fun fetchBatteryStatus(): String = withContext(Dispatchers.IO) {
        val allInfos = batteryRepo.getAvailableBatteryInfo()
        val visibleTypes = prefsRepo.visibleEntriesFlow.first()
        val filtered = if (visibleTypes.isEmpty()) {
            allInfos
        } else {
            allInfos.filter { info ->
                info.type in visibleTypes
            }
        }
        filtered.joinToString("\n") { info ->
            val label = getString(info.type.titleRes)
            "$label: ${info.value}"
        }
    }

    private fun startUpdating() {
        stopUpdating()
        updateJob = updateScope.launch {
            var lastStatus = -1
            var lastWasFull = false
            while (isActive) {
                // Fire real alerts on charge transitions so they show on the
                // lock screen. The monitor notification itself is low + silent.
                val chargeState = try {
                    readChargeState()
                } catch (e: Exception) {
                    null
                }
                if (chargeState != null) {
                    val status = chargeState.first
                    val level = chargeState.second
                    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    val isFull = status == BatteryManager.BATTERY_STATUS_FULL ||
                        (isCharging && level >= 100)

                    if (isCharging && !isFull && lastStatus != BatteryManager.BATTERY_STATUS_CHARGING) {
                        postEventNotification(
                            CHARGING_NOTIF_ID,
                            getString(R.string.charging_started),
                            getString(R.string.charging_started_text, level)
                        )
                    }
                    if (isFull && !lastWasFull) {
                        postEventNotification(
                            FULL_NOTIF_ID,
                            getString(R.string.fully_charged),
                            getString(R.string.fully_charged_text, level)
                        )
                    }
                    lastWasFull = isFull
                    lastStatus = status
                }

                // A single failed read must never kill the notification loop.
                val statusText = try {
                    fetchBatteryStatus()
                } catch (e: Exception) {
                    getString(R.string.monitor_update_failed)
                }
                withContext(Dispatchers.Main) {
                    try {
                        notificationManager.notify(notifId, buildNotification(statusText))
                    } catch (e: Exception) {
                        // Transient notification errors should not stop updates.
                    }
                }
                delay(currentIntervalMs.coerceAtLeast(250).toLong())
            }
        }
    }
}
