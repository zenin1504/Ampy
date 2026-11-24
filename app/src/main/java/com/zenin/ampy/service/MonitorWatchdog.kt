package com.zenin.ampy.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock

/**
 * Watchdog for the battery monitor notification.
 *
 * The monitor is a foreground service, but OEM ROMs can still kill the process
 * while the device is idle. A manifest-registered alarm asks
 * [MonitorRestartReceiver] to bring it back as long as the user left the
 * monitor enabled.
 *
 * Exact alarms are used when the user has granted `SCHEDULE_EXACT_ALARM`: they
 * are not deferred by Doze and allow a background FGS restart. Each firing
 * arms the next one, so the watchdog cannot drift into a permanently deferred
 * repeating alarm.
 */
object MonitorWatchdog {
    const val ACTION_RESTART_MONITOR = "com.zenin.ampy.RESTART_MONITOR"

    private const val REQUEST_PERIODIC = 2002
    private const val REQUEST_SOON = 2003
    private const val FIRST_CHECK_MS = 60_000L
    private const val PERIODIC_INTERVAL_MS = 15 * 60_000L
    private const val SOON_DELAY_MS = 1_500L

    private fun pendingIntent(context: Context, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, MonitorRestartReceiver::class.java).apply {
                action = ACTION_RESTART_MONITOR
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun alarmManager(context: Context): AlarmManager? =
        context.getSystemService(AlarmManager::class.java)

    /** True when exact alarms can be used to restart the monitor during Doze. */
    fun canScheduleExactAlarms(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager(context)?.canScheduleExactAlarms() == true

    private fun schedule(context: Context, requestCode: Int, delayMs: Long) {
        val alarmManager = alarmManager(context) ?: return
        val triggerTime = SystemClock.elapsedRealtime() + delayMs
        val intent = pendingIntent(context, requestCode)
        try {
            if (canScheduleExactAlarms(context)) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerTime,
                    intent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerTime,
                    intent
                )
            }
        } catch (e: SecurityException) {
            // Some ROMs deny exact alarms even when the API reports otherwise.
            alarmManager.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerTime,
                intent
            )
        }
    }

    /** Initial check after the user turns the monitor on. */
    fun schedulePeriodic(context: Context) =
        schedule(context, REQUEST_PERIODIC, FIRST_CHECK_MS)

    /** Called by the receiver after each successful check. */
    fun reschedulePeriodic(context: Context) =
        schedule(context, REQUEST_PERIODIC, PERIODIC_INTERVAL_MS)

    /** Called after task removal, reboot, screen-on, or plug/unplug events. */
    fun restartSoon(context: Context) =
        schedule(context, REQUEST_SOON, SOON_DELAY_MS)

    /** Called when the user turns the monitor off. */
    fun cancel(context: Context) {
        val alarmManager = alarmManager(context) ?: return
        alarmManager.cancel(pendingIntent(context, REQUEST_PERIODIC))
        alarmManager.cancel(pendingIntent(context, REQUEST_SOON))
    }
}
