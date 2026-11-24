package com.zenin.ampy.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.zenin.ampy.data.repository.PrefsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Restores the battery monitor service after the process was killed, the app
 * was updated, the screen was turned on, power was plugged in, or the device
 * booted. It never starts the service unless the user left the monitor on.
 *
 * Registered in the manifest (not dynamically), so it also works while the
 * app process is not running.
 */
class MonitorRestartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in HANDLED_ACTIONS) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = PrefsRepository(appContext)
                if (!prefs.monitorEnabled.first()) {
                    // Make sure no leftover alarm keeps waking the device.
                    MonitorWatchdog.cancel(appContext)
                    return@launch
                }

                when (action) {
                    MonitorWatchdog.ACTION_RESTART_MONITOR -> {
                        // Arm the next safety check before trying to restart.
                        MonitorWatchdog.reschedulePeriodic(appContext)
                        startMonitor(appContext)
                    }
                    else -> {
                        // A plain broadcast may be blocked from starting an FGS
                        // on Android 12+. The exact alarm retry is the fallback.
                        MonitorWatchdog.schedulePeriodic(appContext)
                        MonitorWatchdog.restartSoon(appContext)
                        if (action in IMMEDIATE_START_ACTIONS) {
                            startMonitor(appContext)
                        }
                    }
                }
            } catch (e: Exception) {
                // Never let a watchdog broadcast crash the app process.
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun startMonitor(context: Context) {
        try {
            ContextCompat.startForegroundService(
                context,
                Intent(context, BatteryMonitorService::class.java)
            )
        } catch (e: Exception) {
            // Android 12+ can refuse background FGS starts. The watchdog will
            // retry from the next exact alarm.
        }
    }

    companion object {
        private val HANDLED_ACTIONS = setOf(
            MonitorWatchdog.ACTION_RESTART_MONITOR,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_SCREEN_ON,
            Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED
        )

        private val IMMEDIATE_START_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
    }
}
