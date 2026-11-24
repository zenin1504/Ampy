package com.zenin.ampy.ui.screen

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.zenin.ampy.data.model.BatteryInfo
import com.zenin.ampy.vm.BatteryInfoViewModel
import com.zenin.ampy.R
import com.zenin.ampy.data.model.BatteryInfoType
import com.zenin.ampy.data.util.getBoolString
import com.zenin.ampy.nativelib.AmpyStatsNative
import com.zenin.ampy.nativelib.PowerStats
import com.zenin.ampy.ui.components.AppScaffold
import com.zenin.ampy.ui.components.CardWithPowerChart
import com.zenin.ampy.ui.components.CompactBatteryReadings
import com.zenin.ampy.ui.components.DeviceInfoCard
import com.zenin.ampy.ui.components.PowerDataPoint
import com.zenin.ampy.vm.BatteryLogViewModel
import com.zenin.ampy.vm.SettingsViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat

private const val MAX_CHART_POINTS = 36000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dashboard(
    currentTitle: String,
    batteryInfoViewModel: BatteryInfoViewModel,
    settingsViewModel: SettingsViewModel,
    batteryLogViewModel: BatteryLogViewModel,
    onOpenLogcatDetails: () -> Unit
) {
    AppScaffold(title = currentTitle, isTopLevel = true)
    {
        DashBoardContent(batteryInfoViewModel, settingsViewModel, batteryLogViewModel, onOpenLogcatDetails)
    }
}

@Composable
fun NormalBatteryCard(info: BatteryInfo) {
    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(
            text = info.customTitle ?: stringResource(info.type.titleRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = info.value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BatteryCardWithButton(
    info: BatteryInfo,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        ) {
            Text(
                text = info.customTitle ?: stringResource(info.type.titleRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = info.value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(modifier = Modifier.padding(horizontal = 4.dp)) {
            FilledTonalIconButton(
                onClick = onClick,
                modifier = Modifier.size(36.dp),
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun BatteryCardWithInfo(
    info: BatteryInfo,
    onShowInfo: () -> Unit
) {
    BatteryCardWithButton(
        info = info,
        icon = ImageVector.vectorResource(id = R.drawable.info_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
        contentDescription = "Show Info",
        onClick = onShowInfo
    )
}

@Composable
fun BatteryCardWithSwap(
    info: BatteryInfo,
    onSwap: () -> Unit
) {
    BatteryCardWithButton(
        info = info,
        icon = ImageVector.vectorResource(id = R.drawable.swap_horiz_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
        contentDescription = "Swap Unit",
        onClick = onSwap
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BatteryCardWithCalibration(
    info: BatteryInfo,
    isDualBatt: Boolean,
    context: Context,
    onToggleDualBat: () -> Unit,
    onShowMultiplierDialog: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
                text = stringResource(id = info.type.titleRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = info.value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Box(modifier = Modifier.padding(horizontal = 4.dp)) {
            FilledTonalIconButton(
                onClick = onShowMultiplierDialog,
                modifier = Modifier.size(36.dp),
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(
                    ImageVector.vectorResource(id = R.drawable.edit_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
                    contentDescription = "Calibrate",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(
                    text = stringResource(R.string.dual_cell),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = getBoolString(isDualBatt, context),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                FilledTonalIconButton(
                    onClick = onToggleDualBat,
                    modifier = Modifier.size(36.dp),
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(
                        ImageVector.vectorResource(id = R.drawable.swap_horiz_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
                        contentDescription = "Toggle Dual Battery",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CycleCountDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.battery_cycle_count)) },
        text = {
            Text(
                text = stringResource(R.string.battery_cycle_count_info),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}
@Composable
fun WrongVoltageDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.battery_voltage)) },
        text = {
            Text(
                text = stringResource(R.string.wrong_battery_voltage_info),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
fun DashBoardContent(
    batteryInfoViewModel: BatteryInfoViewModel,
    settingsViewModel: SettingsViewModel,
    batteryLogViewModel: BatteryLogViewModel,
    onOpenLogcatDetails: () -> Unit
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isMultiply by batteryInfoViewModel.isMultiply.collectAsState()
    val isDualBatt by batteryInfoViewModel.isDualBatt.collectAsState()
    val refreshInterval by settingsViewModel.refreshInterval.collectAsState()
    val isPowerChartExpanded by settingsViewModel.isPowerChartExpanded.collectAsState()
    val selectedMagnitude by batteryInfoViewModel.selectedMagnitude.collectAsState()
    val isCelsius by settingsViewModel.isCelsius.collectAsState()
    val deviceInfo by batteryLogViewModel.deviceInfo.collectAsState()
    val batteryCapacityMah by batteryLogViewModel.batteryCapacityMah.collectAsState()
    var showMultiplierDialog by remember { mutableStateOf(false) }
    var showCycleCountDialog by remember { mutableStateOf(false) }
    var showWrongVoltageDialog by remember { mutableStateOf(false) }
    val batteryInfoList = remember { mutableStateListOf<BatteryInfo>() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val powerDataPoints = remember { mutableStateListOf<PowerDataPoint>() }
    var powerStats by remember { mutableStateOf<PowerStats?>(null) }
    val statsHandle = remember { AmpyStatsNative.createStats(MAX_CHART_POINTS) }
    var chartStartTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    DisposableEffect(statsHandle) {
        onDispose {
            AmpyStatsNative.destroyStats(statsHandle)
        }
    }

    LaunchedEffect(lifecycleOwner, isCelsius) {
        lifecycleOwner.lifecycle
            .repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Reset power chart data when entering foreground
                powerDataPoints.clear()
                AmpyStatsNative.resetStats(statsHandle)
                powerStats = null
                chartStartTime = System.currentTimeMillis()

                while (true) {
                    val displayList = batteryInfoViewModel.getDisplayBatteryInfo()

                    // Collect power data and update the native rolling summary.
                    powerStats = collectPowerDataForChart(
                        displayList,
                        powerDataPoints,
                        chartStartTime,
                        statsHandle
                    )

                    batteryInfoList.clear()
                    batteryInfoList.addAll(displayList)
                    delay(refreshInterval.toLong())
                }
            }
    }

    LaunchedEffect(Unit) {
        batteryInfoViewModel.saveCycleCount()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.padding(horizontal = 8.dp)) {
            LazyColumn (
                state = listState,
                modifier = Modifier.fillMaxWidth()
            ){
                item {
                    DeviceInfoCard(
                        deviceInfo = deviceInfo,
                        capacityMah = batteryCapacityMah,
                        onViewDetails = onOpenLogcatDetails
                    )
                }
                item {
                    Text(
                        text = stringResource(R.string.battery_readings),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, top = 10.dp, bottom = 8.dp)
                    )
                }
                item {
                    CompactBatteryReadings(
                        items = batteryInfoList.filter {
                            it.type != BatteryInfoType.CURRENT && it.type != BatteryInfoType.POWER
                        },
                        modifier = Modifier.padding(vertical = 4.dp),
                        onSwapTemperature = {
                            settingsViewModel.setIsCelsius(!isCelsius)
                        },
                        onShowCycleInfo = { showCycleCountDialog = true },
                        onShowVoltageInfo = { showWrongVoltageDialog = true }
                    )
                }
                item {
                    val currentInfo = batteryInfoList.find { it.type == BatteryInfoType.CURRENT }
                    if (currentInfo != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BatteryCardWithCalibration(
                                    info = currentInfo,
                                    isDualBatt = isDualBatt,
                                    context = context,
                                    onToggleDualBat = { batteryInfoViewModel.setDualBatt(!isDualBatt) },
                                    onShowMultiplierDialog = { showMultiplierDialog = true }
                                )
                            }
                        }
                    }
                }
                item {
                    val powerInfo = batteryInfoList.find { it.type == BatteryInfoType.POWER }
                    if (powerInfo != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            CardWithPowerChart(
                                info = powerInfo,
                                powerData = powerDataPoints.toList(),
                                onResetData = {
                                    powerDataPoints.clear()
                                    AmpyStatsNative.resetStats(statsHandle)
                                    powerStats = null
                                    chartStartTime = System.currentTimeMillis()
                                },
                                isExpanded = isPowerChartExpanded,
                                isCelsius = isCelsius,
                                powerStats = powerStats,
                                onChartExpand = {
                                    settingsViewModel.setPowerChartExpanded(!isPowerChartExpanded)
                                }
                            )
                        }
                    }
                }

            }
        }

    }

    if (showCycleCountDialog) {
        CycleCountDialog {
            showCycleCountDialog = false
        }
    }

    if (showWrongVoltageDialog) {
        WrongVoltageDialog {
            showWrongVoltageDialog = false
        }
    }

    if (showMultiplierDialog) {
        AlertDialog(
            onDismissRequest = { showMultiplierDialog = false },
            title = { Text(stringResource(R.string.calibrate_via_multiplier)) },
            text = {
                Column (modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.battery_current_info))
                    MultiplierSelector(
                        isMultiply = isMultiply,
                        onMultiplyChange = { batteryInfoViewModel.setMultiplierPrefs(it, selectedMagnitude) },
                        selectedMagnitude = selectedMagnitude,
                        onMagnitudeChange = { batteryInfoViewModel.setMultiplierPrefs(isMultiply, it) }
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showMultiplierDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            },
            dismissButton = {
                Button(onClick = {
                    batteryInfoViewModel.setMultiplierPrefs(true, 0)
                    showMultiplierDialog = false
                }) {
                    Text(stringResource(R.string.reset))
                }

            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplierSelector(
    isMultiply: Boolean,
    onMultiplyChange: (Boolean) -> Unit,
    selectedMagnitude: Int,
    onMagnitudeChange: (Int) -> Unit
) {
    val magnitudeOptions = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9) // 10, 100, 1000, 10000 etc
    val label = if (isMultiply) stringResource(R.string.multiply) else stringResource(R.string.divide)
    var isExpanded by remember { mutableStateOf(false) }

    Column {
        // Multiply/Divide RadioButton
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = isMultiply, onClick = { onMultiplyChange(true) })
            Text(stringResource(R.string.multiply))
            Spacer(Modifier.width(16.dp))
            RadioButton(selected = !isMultiply, onClick = { onMultiplyChange(false) })
            Text(stringResource(R.string.divide))
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Choose multiplier in dropdown menu
        ExposedDropdownMenuBox(
            expanded = isExpanded,
            onExpandedChange = { isExpanded = it }
        ) {
            TextField(
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                    .fillMaxWidth(),
                readOnly = true,
                value = "10^$selectedMagnitude",
                onValueChange = {},
                label = { Text(stringResource(R.string.multiplier, label)) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
                },
            )
            ExposedDropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false }
            ) {
                magnitudeOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text("10^$option") },
                        onClick = {
                            onMagnitudeChange(option)
                            isExpanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}

private fun collectPowerDataForChart(
    displayList: List<BatteryInfo>,
    powerDataPoints: MutableList<PowerDataPoint>,
    chartStartTime: Long,
    statsHandle: Long,
    maxDataPoints: Int = MAX_CHART_POINTS
): PowerStats? {
    val powerInfo = displayList.find { it.type == BatteryInfoType.POWER }
    val tempInfo = displayList.find { it.type == BatteryInfoType.TEMP }

    return powerInfo?.let { power ->
        try {
            val nf = NumberFormat.getInstance()
            val powerValue = nf.parse(power.value)?.toFloat() ?: 0f
            val tempValue = tempInfo?.value?.let { nf.parse(it)?.toFloat() } ?: 0f
            val currentTime = System.currentTimeMillis()
            val absolutePower = kotlin.math.abs(powerValue)

            powerDataPoints.add(PowerDataPoint(
                timestamp = currentTime - chartStartTime,
                power = absolutePower,
                temperature = tempValue
            ))

            // Keep only last N data points
            if (powerDataPoints.size > maxDataPoints) {
                powerDataPoints.removeAt(0)
            }

            AmpyStatsNative.pushStats(statsHandle, absolutePower) ?: run {
                // Kotlin fallback for devices where libampy_stats.so is unavailable.
                PowerStats(
                    average = powerDataPoints.sumOf { it.power.toDouble() }
                        .toFloat() / powerDataPoints.size,
                    peak = powerDataPoints.maxOf { it.power },
                    samples = powerDataPoints.size
                )
            }
        } catch (e: Exception) {
            // Ignore parsing errors
            null
        }
    }
}
