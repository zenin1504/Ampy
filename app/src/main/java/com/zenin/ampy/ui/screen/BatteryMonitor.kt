package com.zenin.ampy.ui.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import com.zenin.ampy.ui.components.CustomSwitch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.zenin.ampy.R
import com.zenin.ampy.ui.components.AppScaffold
import com.zenin.ampy.ui.components.CardGroup
import com.zenin.ampy.ui.components.CardGroupTitle
import com.zenin.ampy.vm.BatteryMonitorSettingsViewModel

@Composable
fun BatteryMonitor(currentTitle: String, navController: NavController, battMonVM: BatteryMonitorSettingsViewModel) {
    AppScaffold(currentTitle, isTopLevel = true) {
        BatteryMonitorContent(navController, battMonVM)
    }
}

@Composable
fun BatteryMonitorContent(
    navController: NavController,
    battMonVM: BatteryMonitorSettingsViewModel
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .verticalScroll(scrollState)
    ) {
        CardGroup(
            title = { CardGroupTitle(text = stringResource(R.string.general)) },
            content = listOf(
                {
                    ListItem(
                        modifier = Modifier.clickable { navController.navigate("batt_mon_settings") },
                        headlineContent = { Text(text = stringResource(R.string.battery_monitor_entry_settings_list_item_title), style = MaterialTheme.typography.bodyLarge) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(id = R.drawable.battery_profile_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
                                contentDescription = null,
                            )
                        }
                    )
                }
            )
        )
        Spacer(Modifier.height(16.dp))
        CardGroup(
            title = { CardGroupTitle(text = stringResource(R.string.notification)) },
            content = listOf(
                {
                    ListItem(
                        headlineContent = { BatteryMonitorSwitch(battMonVM) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(id = R.drawable.notifications_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
                                contentDescription = null,
                            )
                        }
                    )
                }
            )
        )
        Spacer(Modifier.height(16.dp))
        CardGroup(
            title = { CardGroupTitle(text = stringResource(R.string.floating_window)) },
            content = listOf(
                {
                    ListItem(
                        modifier = Modifier.clickable { navController.navigate("floating_window_settings") },
                        headlineContent = { Text(text = stringResource(R.string.floating_window_settings_list_item_title), style = MaterialTheme.typography.bodyLarge) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(id = R.drawable.mobile_gear_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
                                contentDescription = null,
                            )
                        }
                    )
                },
                {
                    ListItem(
                        headlineContent = { FloatingWindowSwitch(battMonVM) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(id = R.drawable.pip_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
                                contentDescription = null,
                            )
                        }
                    )
                }
            )
        )
        Spacer(Modifier.height(16.dp))
        CardGroup(
            title = { CardGroupTitle(text = stringResource(R.string.tips)) },
            content = listOf(
                {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(R.string.disable_battery_optimization),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        },
                        leadingContent = {
                            Icon(
                                painter = painterResource(id = R.drawable.info_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
                                contentDescription = null,
                            )
                        }
                    )
                }
            )
        )
    }
}

@Composable
fun FloatingWindowSwitch(battMonVM: BatteryMonitorSettingsViewModel) {
    val context = LocalContext.current
    val isFloatingWindowOn by battMonVM.isFloatingWindowOn.collectAsState()

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (Settings.canDrawOverlays(context)) {
            battMonVM.startFloatingWindow()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.overlay_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = context.getString(R.string.show_in_floating_window), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        CustomSwitch(
            checked = isFloatingWindowOn,
            onCheckedChange = { on ->
                if (on) {
                    if (!Settings.canDrawOverlays(context)) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            "package:${context.packageName}".toUri()
                        )
                        overlayPermissionLauncher.launch(intent)
                    } else {
                        battMonVM.startFloatingWindow()
                    }
                } else {
                    battMonVM.stopFloatingWindow()
                }
            }
        )
    }
}

private fun startMonitorWithKeepAlive(
    context: Context,
    battMonVM: BatteryMonitorSettingsViewModel,
    launcher: ActivityResultLauncher<Intent>
) {
    battMonVM.startMonitor()
    // Exact alarms keep the watchdog working through Doze and allow the
    // system to restart the foreground service after an OEM kill.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        !battMonVM.canScheduleExactAlarms()
    ) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = "package:${context.packageName}".toUri()
        }
        try {
            launcher.launch(intent)
        } catch (e: Exception) {
            // Some ROMs do not implement this settings screen.
        }
    }
}

@Composable
fun BatteryMonitorSwitch(battMonVM: BatteryMonitorSettingsViewModel) {
    val context = LocalContext.current
    val isMonitoring by battMonVM.isMonitoring.collectAsState()

    val keepAliveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        battMonVM.onKeepAliveSettingsReturned()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            startMonitorWithKeepAlive(context, battMonVM, keepAliveLauncher)
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.notification_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
            battMonVM.stopMonitor()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = context.getString(R.string.show_in_notification_bar), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        CustomSwitch(
            checked = isMonitoring,
            onCheckedChange = { on ->
                if (on) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        when {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED -> {
                                startMonitorWithKeepAlive(context, battMonVM, keepAliveLauncher)
                            }
                            else -> {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    } else {
                        startMonitorWithKeepAlive(context, battMonVM, keepAliveLauncher)
                    }
                } else {
                    battMonVM.stopMonitor()
                }
            }
        )
    }
}
