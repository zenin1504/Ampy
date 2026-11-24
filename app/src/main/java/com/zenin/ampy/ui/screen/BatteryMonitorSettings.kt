package com.zenin.ampy.ui.screen

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.zenin.ampy.R
import com.zenin.ampy.ui.components.AppScaffold
import com.zenin.ampy.ui.components.BackIcon
import com.zenin.ampy.ui.components.getListItemShape
import com.zenin.ampy.vm.BatteryMonitorSettingsViewModel

@Composable
fun BatteryMonitorSettings(
    navController: NavController,
    batteryMonVM: BatteryMonitorSettingsViewModel,
    currentTitle: String,
) {
    AppScaffold(currentTitle,
        navigationIcon = { BackIcon(navController)
        },
    ) {
        BatteryMonitorSettingsContent(batteryMonVM)
    }
}
@Composable
fun BatteryMonitorSettingsContent(
    batteryMonVM: BatteryMonitorSettingsViewModel
) {
    val items      by batteryMonVM.availableEntries.collectAsState()
    val visibleSet by batteryMonVM.visibleEntries.collectAsState()
    val currentSelection = visibleSet.ifEmpty { items.toSet() }
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val exactAlarmLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        batteryMonVM.onKeepAliveSettingsReturned()
    }

    Column(
        Modifier
            .padding(horizontal = 8.dp)
            .verticalScroll(scrollState)
    ) {
        Row (modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(stringResource(R.string.please_select_the_entries_you_want_to_show_in_the_battery_monitor))
        }
        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = getListItemShape(0, 1),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.keep_monitor_alive),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.exact_alarm_info),
                    style = MaterialTheme.typography.bodySmall
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = "package:${context.packageName}".toUri()
                        }
                        try {
                            exactAlarmLauncher.launch(intent)
                        } catch (e: Exception) {
                            // Some ROMs do not provide this settings screen.
                        }
                    }) {
                        Text(stringResource(R.string.open_exact_alarm_settings))
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        items.forEachIndexed { index, entry ->
            val checked = currentSelection.contains(entry)
            val onCheckedChange: (Boolean) -> Unit = { on ->
                val newSet = if (visibleSet.isEmpty()) {
                    val allSet = items.toSet()
                    if (on) allSet + entry else allSet - entry
                } else {
                    if (on) visibleSet + entry else visibleSet - entry
                }
                batteryMonVM.setVisibleEntries(newSet)
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = getListItemShape(index, items.size),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCheckedChange(!checked) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = onCheckedChange
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(entry.titleRes))
                }
            }
            Spacer(Modifier.height(2.dp))
        }
        Spacer(Modifier.height(20.dp))

    }
}
