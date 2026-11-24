package com.zenin.ampy.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zenin.ampy.R
import com.zenin.ampy.ui.components.AppScaffold
import com.zenin.ampy.ui.components.BackIcon
import com.zenin.ampy.ui.components.DeviceInfoCard
import com.zenin.ampy.ui.components.getListItemShape
import com.zenin.ampy.vm.BatteryLogViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalSupportLogcat(
    navController: NavController,
    batteryLogViewModel: BatteryLogViewModel,
    currentTitle: String
) {
    AppScaffold(
        currentTitle,
        navigationIcon = { BackIcon(navController) }
    ) {
        UniversalSupportLogcatScreen(batteryLogViewModel)
    }
}

@Composable
fun UniversalSupportLogcatScreen(
    batteryLogViewModel: BatteryLogViewModel
) {
    val deviceInfo by batteryLogViewModel.deviceInfo.collectAsState()
    val logMap by batteryLogViewModel.latestLog.collectAsState()
    val capacityMah by batteryLogViewModel.batteryCapacityMah.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        DeviceInfoCard(deviceInfo = deviceInfo, capacityMah = capacityMah)

        Spacer(Modifier.height(12.dp))

        val entries = logMap.orEmpty().entries.toList()
        if (entries.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = stringResource(R.string.waiting_for_battery_data_from_logcat),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            entries.forEachIndexed { index, entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = getListItemShape(index, entries.size)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = entry.key,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(0.85f)
                        )
                        Text(
                            text = entry.value,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.weight(1.15f)
                        )
                    }
                }
            }
        }
    }
}
